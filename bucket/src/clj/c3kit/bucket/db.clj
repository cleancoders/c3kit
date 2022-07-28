(ns c3kit.bucket.db
  (:refer-clojure :exclude [update])
  (:require
    [c3kit.apron.app :as app]
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.log :as log]
    [c3kit.apron.util :as util]
    [c3kit.bucket.dbc :as dbc]
    [clojure.set :as set]
    [clojure.string :as str]
    [datomic.api :as api]))

(defn connect [uri]
  (api/create-database uri)
  (api/connect uri))

(defn read-config [] (util/read-edn-resource "config/datomic.edn"))

(def config (atom {}))

(defn start [app]
  (swap! config merge (read-config))
  (let [uri (:uri @config)]
    (log/info "Connecting to datomic at: " uri)
    (assoc app :datomic-connection (connect uri))))

(defn stop [app]
  (log/info "Connection to datomic discarded")
  (dissoc app :datomic-connection))

(def service (app/service 'c3kit.bucket.db/start 'c3kit.bucket.db/stop))

(defonce connection (app/resolution! :datomic-connection))

(defn partition-name [] (or (:partition @config) :db.part/user))

(defn partition-schema
  "Return transact-able form to add a partition with name"
  ([] (partition-schema (partition-name)))
  ([partition-name]
   [{:db/id (name partition-name) :db/ident (keyword partition-name)}
    [:db/add :db.part/db :db.install/partition (name partition-name)]]))

(defn- apply-uniqueness [schema options]
  (if (:unique-value options)
    (assoc schema :db/unique :db.unique/value)
    (if (:unique-identity options)
      (assoc schema :db/unique :db.unique/identity)
      schema)))

(defn build-attribute [kind [attr-name type & spec]]
  (let [options (set spec)
        type    (if (= :kw-ref type) :ref type)]
    (->
      {
       :db/ident       (keyword (name kind) (name attr-name))
       :db/valueType   (keyword "db.type" (name type))
       :db/cardinality (if (contains? options :many) :db.cardinality/many :db.cardinality/one)
       :db/index       (if (:index options) true false)
       :db/isComponent (if (:component options) true false)
       :db/noHistory   (if (:no-history options) true false)
       :db/fulltext    (if (:fulltext options) true false)
       }
      (apply-uniqueness options))))

(defn build-schema [kind attribute-specs]
  (vec (map #(build-attribute kind %) attribute-specs)))

(defn build-enum-schema [enum values]
  (mapv (fn [val] {:db/ident (keyword (name enum) (name val))}) values))

(defn db [] (api/db @connection))

(defn db-as-of [t] (api/as-of (db) t))

(defn scope-attribute [kind attr] (keyword (name kind) (name attr)))

(defn scope-attributes [kind attributes]
  (into {}
        (map
          (fn [[k v]] [(scope-attribute kind k) v])
          attributes)))

(defn transact!
  ([transaction] (transact! transaction @connection))
  ([transaction connection]
   (api/transact connection transaction)))

(defn value-or-id [v]
  (if (and (instance? datomic.query.EntityMap v) (contains? v :db/id))
    (:db/id v)
    v))

(defn attributes->entity
  ([attributes id]
   (when (seq attributes)
     (let [kind (namespace (first (first attributes)))]
       (attributes->entity attributes id kind))))
  ([attributes id kind]
   (into {:id id :kind (keyword kind)}
         (map
           (fn [[k v]]
             [(keyword (name k))
              (if (set? v)
                (set (map value-or-id v))
                (value-or-id v))])
           attributes))))

(defn entity [id]
  (cond
    (number? id) (when-let [attributes (seq (api/entity (api/db @connection) id))]
                   (attributes->entity attributes id))
    (nil? id) nil
    (string? id) (when-not (str/blank? id) (entity (Long/parseLong id)))
    :else (attributes->entity (seq id) (:db/id id))))

(defn entity! [id] (dbc/entity! (entity id) id))
(defn entity-of-kind! [kind id] (dbc/entity-of-kind! (entity id) kind id))
(defn entity-of-kind [kind id] (dbc/entity-of-kind (entity id) kind))
(defn reload [e] (when-let [id (:id e)] (entity id)))

(defn q->entities [result] (map #(-> % first entity) result))
(defn q->ids [result] (map first result))

(defn- id-or-val [thing] (or (:db/id thing) thing))

(defn dissoc-nils [entity]
  (apply dissoc entity (filter #(= nil (get entity %)) (keys entity))))

(defn- kind! [entity]
  (or (:kind entity)
      (throw (Exception. (str ":kind missing for " entity)))))

(defn tempid [] (api/tempid (partition-name)))
(def tempid? (comp (fnil neg? 0) :idx))
(def squuid api/squuid)

(defn insert-form [id entity]
  (list (-> entity dissoc-nils (assoc :db/id id))))

(defn- retract-field-forms [id original retracted-keys]
  (reduce (fn [form key]
            (let [o-val (get original key)]
              (if (set? o-val)
                (reduce #(conj %1 [:db/retract id key (id-or-val %2)]) form o-val)
                (conj form [:db/retract id key (id-or-val o-val)]))))
          [] retracted-keys))

(defn- cardinality-many-retract-forms [updated original]
  (reduce (fn [form [key val]]
            (if (or (set? val) (sequential? val))
              (let [id      (:db/id updated)
                    o-val   (set (map id-or-val (get original key)))
                    missing (set/difference o-val (set val))]
                (reduce #(conj %1 [:db/retract id key (id-or-val %2)]) form missing))
              form))
          [] updated))

(defn update-form [id updated]
  (let [original          (into {} (api/entity (api/db @connection) id))
        retracted-keys    (doall (filter #(= nil (get updated %)) (keys original)))
        updated           (-> (apply dissoc updated retracted-keys)
                              dissoc-nils
                              (assoc :db/id id))
        seq-retractions   (cardinality-many-retract-forms updated original)
        field-retractions (retract-field-forms id original retracted-keys)]
    (concat [updated] seq-retractions field-retractions)))

(defn maybe-retract-form [entity]
  (when (dbc/retract? entity)
    (if-let [id (:id entity)]
      (list id (list [:db.fn/retractEntity id]))
      (throw (Exception. "Can't retract entity without an :id")))))

(defn maybe-cas-form [entity]
  (when-let [old-vals (:cas (meta entity))]
    (let [kind (:kind entity)
          id   (:id entity)]
      [id
       (map (fn [[k v]]
              (vector :db/cas id (scope-attribute kind k) v (get entity k)))
            old-vals)])))

(defn tx-entity-form [entity]
  (let [kind (kind! entity)
        id   (or (:id entity) (tempid))
        e    (scope-attributes kind (dissoc entity :kind :id))]
    (if (tempid? id)
      (list id (insert-form id e))
      (list id (update-form id e)))))

(defn tx-form [entity]
  (or (maybe-retract-form entity)
      (maybe-cas-form entity)
      (tx-entity-form entity)))

(defn resolve-id [result id]
  (if (tempid? id)
    (api/resolve-tempid (:db-after result) (:tempids result) id)
    id))

(defn- tx-result [id]
  (if-let [e (entity id)]
    e
    {:kind :db/retract :id id}))

(defn tx
  "Transacts (save, update, or retract) the entity.
  Arguments, assumed to be in key-value pairs, will be merged into the entity prior to saving.
  Retracts entity when it's metadata has :retract."
  [& args]
  (let [e (ccc/->options args)]
    (when (seq e)
      (let [[id form] (tx-form e)
            result @(api/transact @connection form)
            id     (resolve-id result id)]
        (tx-result id)))))

(defn tx*
  "Transact multiple entities, synchronously"
  [entities]
  (let [id-forms (map tx-form (remove nil? entities))
        tx-form  (mapcat second id-forms)
        result   @(api/transact @connection tx-form)
        ids      (map #(resolve-id result (first %)) id-forms)]
    (map tx-result ids)))

(defn atx*
  "Asynchronous transact.  Returns a future containing the transacted entities.
  Ideal for large imports or when multiple entities need to be transacted in a single transaction."
  [entities]
  (let [id-forms (map tx-form entities)
        tx-form  (mapcat second id-forms)
        tx       (api/transact-async @connection tx-form)]
    (future
      (loop [done? (future-done? tx)]
        (if done?
          (let [result @tx
                ids    (map first id-forms)]
            (map #(entity (resolve-id result %)) ids))
          (do
            (Thread/yield)
            (recur (future-done? tx))))))))

(defn- ->attr-kw [kind attr] (keyword (name kind) (name attr)))

(declare where-clause)
(defmulti ^:private seq-where-clause (fn [attr value] (first value)))
(defmethod seq-where-clause 'not [attr [_ value]]
  (if (nil? value)
    (list ['?e attr])
    (list (list 'not ['?e attr value]))))

(defn- simple-where-fn [attr value f-sym]
  (let [attr-sym (gensym (str "?" (name attr)))]
    (list ['?e attr attr-sym]
          [(list f-sym attr-sym value)])))

(defmethod seq-where-clause '> [attr [_ value]] (simple-where-fn attr value '>))
(defmethod seq-where-clause '< [attr [_ value]] (simple-where-fn attr value '<))
(defmethod seq-where-clause '>= [attr [_ value]] (simple-where-fn attr value '>=))
(defmethod seq-where-clause '<= [attr [_ value]] (simple-where-fn attr value '<=))

(defn- or-where-clause [attr values]
  (let [values (set values)]
    (if (seq values)
      (list (cons 'or (mapcat #(where-clause attr %) values)))
      [nil])))

(defmethod seq-where-clause 'or [attr values] (or-where-clause attr (rest values)))
(defmethod seq-where-clause :default [attr values] (or-where-clause attr values))

(defn where-clause [attr value]
  (cond (nil? value) (list [(list 'missing? '$ '?e attr)])
        (set? value) (or-where-clause attr value)
        (sequential? value) (seq-where-clause attr value)
        :else (list ['?e attr value])))

(defn find-where
  "Search for all entities that match the datalog 'where' clause passed in."
  [where]
  (if (some nil? where)
    []
    (-> '[:find ?e :in $ :where]
        (concat where)
        (api/q (db))
        q->entities)))

(defn find-ids-where
  "Search for ids of entities that match the datalog 'where' clause passed in."
  [where]
  (if (some nil? where)
    []
    (-> '[:find ?e :in $ :where]
        (concat where)
        (api/q (db))
        q->ids)))

(defn count-where
  "Count all entities that match the datalog 'where' clause passed in."
  [where]
  (if (some nil? where)
    0
    (-> '[:find (count ?e) :in $ :where]
        (concat where)
        (api/q (db))
        ffirst
        (or 0))))

(defn- do-search
  ([q-fn default kind attr value]
   (if (nil? value)
     (do (log/warn (str "search for nil value (" kind " " attr "), returning no results.")) default)
     (q-fn (where-clause (->attr-kw kind attr) value))))
  ([q-fn _ kind attr1 val1 & pairs]
   (assert (even? (count pairs)) "must provide key value pairs")
   (let [pairs (partition 2 pairs)
         attrs (map #(->attr-kw kind %) (cons attr1 (map first pairs)))
         vals  (cons val1 (map second pairs))]
     (q-fn (mapcat where-clause attrs vals)))))

(defn find-by
  "Searches for all entities where the attribute(s) match the value(s).
  Values matching options:
      value         - (= % value)
      nil           - (nil? %)
      [values]      - (some #(= % value) values)
      ['not value]  - (not (= % value))
      ['> value]    - (> % value)
      ['< value]    - (< % value)
  With a single attr, value is the only matching option, and it must not be nil. Otherwise, no results will be returned
  and a warning will be logged."
  [kind & pairs] (apply do-search find-where [] kind pairs))

(defn find-ids-by
  "Searches for all entity ids where the attribute(s) match the value(s).
  Faster search since no entity data is loaded.
  See find-by for value matching options."
  [kind & pairs] (apply do-search find-ids-where [] kind pairs))

(defn count-by
  "Counts all entities where the attribute(s) match the value(s).
  See find-by for value matching options."
  [kind & pairs] (apply do-search count-where 0 kind pairs))

(defn ffind-by
  "Same as (first (find-by ...))"
  ([kind attr value] (first (find-by kind attr value)))
  ([kind attr1 val1 attr2 val2] (first (find-by kind attr1 val1 attr2 val2)))
  ([kind attr1 val1 attr2 val2 & pairs] (first (apply find-by kind attr1 val1 attr2 val2 pairs))))

(defn q
  "Raw datomic query and request"
  [query & args]
  (apply api/q query (db) args))

(defn find-entities
  "Takes a datalog query and returns realized (de-namespaced) entities."
  [query & args]
  (q->entities (apply q query args)))

(defn count-all
  "Attribute must be a qualified attribute name like :user/email :airport/code
  Returns count of entities where attribute has a value."
  ([kind] (throw (ex-info "an attribute is required when using datomic" {:kind kind})))
  ([kind attr]
   (or (ffirst (api/q '[:find (count ?e)
                        :in $ ?attribute
                        :where [?e ?attribute]] (db) (->attr-kw kind attr)))
       0)))

(defn find-all
  "Attribute must be a qualified attribute name like :user/email :airport/code
  Returns all entities where the value is not empty."
  ([kind] (throw (ex-info "an attribute is required when using datomic" {:kind kind})))
  ([kind attr]
   (q->entities (api/q '[:find ?e
                         :in $ ?attribute
                         :where [?e ?attribute]] (db) (->attr-kw kind attr)))))

(defn ->eid
  "Returns the entity id"
  [id-or-entity]
  (if (number? id-or-entity) id-or-entity (:id id-or-entity)))

(defn retract
  "Basically 'deletes' an entity."
  [id-or-entity]
  (-> (if (number? id-or-entity) {:id id-or-entity} id-or-entity)
      (assoc :kind :db/retract)
      tx))

(defn cas
  "compare-and-swap - Returns entity with CAS metadata.  When transacted, only the specified attributes will be saved.
  If the values don't match, an exception will be thrown.  User cas-ex? to check for CAS conflict.
  Only use with existing entities.
  Do not include the entity along with a CAS version in the same transaction.
  -> https://docs.datomic.com/cloud/transactions/transaction-functions.html#db-cas
  -> https://docs.datomic.com/cloud/best.html#optimistic-concurrency"
  [entity & old-attr-value-pairs]
  (let [old-vals (ccc/->options old-attr-value-pairs)]
    (with-meta entity {:cas old-vals})))

(defn cas-ex? [e] (instance? java.util.concurrent.ExecutionException e))

(defn tx-ids
  "Returns a sorted list of all the transaction ids in which the entity was updated."
  [eid]
  (->> (api/q
         '[:find ?tx
           :in $ ?e
           :where
           [?e _ _ ?tx _]]
         (api/history (db)) eid)
       (sort-by first)
       (map first)))

(defn entity-as-of-tx
  "Loads the entity as it existed when the transaction took place, adding :db/tx (transaction id)
   and :db/instant (date) attributes to the entity."
  [db eid kind txid]
  (let [tx         (api/entity db txid)
        timestamp  (:db/txInstant tx)
        attributes (api/entity (api/as-of db txid) eid)]
    (when (seq attributes)
      (-> attributes
          (attributes->entity eid kind)
          (assoc :db/tx txid :db/instant timestamp)))))

(defn history
  "Returns a list of every version of the entity form creation to current state,
  with :db/tx and :db/instant attributes."
  [entity]
  (let [id   (:id entity)
        kind (:kind entity)]
    (assert id)
    (assert kind)
    (reduce #(conj %1 (entity-as-of-tx (db) id kind %2)) [] (tx-ids (:id entity)))))

(defn created-at
  "Returns the instant (java.util.Date) the entity was created."
  [id-or-entity]
  (let [eid (->eid id-or-entity)]
    (ffirst (api/q '[:find (min ?inst)
                     :in $ ?e
                     :where [?e _ _ ?tx]
                     [?tx :db/txInstant ?inst]] (api/history (db)) eid))))

(defn updated-at
  "Returns the instant (java.util.Date) this entity was last updated."
  [id-or-entity]
  (let [eid (->eid id-or-entity)]
    (ffirst (api/q '[:find (max ?inst)
                     :in $ ?e
                     :where [?e _ _ ?tx]
                     [?tx :db/txInstant ?inst]] (api/history (db)) eid))))

(defn with-timestamps
  "Adds :created-at and :updated-at timestamps to the entity."
  [entity]
  (assoc entity :db/created-at (created-at entity) :db/updated-at (updated-at entity)))

(defn excise!
  "Remove entity from database history."
  [id-or-e]
  (let [id (if-let [id? (:id id-or-e)] id? id-or-e)]
    (transact! [{:db/excise id}])))

(def reserved-attr-nses #{"db" "db.alter" "db.attr" "db.bootstrap" "db.cardinality" "db.entity" "db.excise" "db.fn"
                          "db.install" "db.lang" "db.part" "db.sys" "db.type" "db.unique" "fressian" "deleted" "garbage"})
(defn current-schema
  "Returns a list of all the fully qualified fields in the schema."
  []
  (->> (api/q '[:find ?ident :where [?e :db/ident ?ident]] (db))
       (map first)
       (filter #(not (reserved-attr-nses (namespace %))))
       sort))

(defn garbage-idents
  "Returns a list of all the idents starting with :garbage."
  []
  (filter #(= "garbage" (namespace %)) (map first (api/q '[:find ?ident :where [?e :db/ident ?ident]] (db)))))

(def inspect-table (log/table-spec [":db/ident" 50]
                                   [":db/id" 8]
                                   [":db/valueType" 20]
                                   [":db/cardinality" 22]
                                   [":db/index" 9]
                                   [":db/unique" 20]
                                   [":db/fulltext" 12]))

(defn inspect
  "For use in REPL or development.
  Prints the qualified names of all fields, alphebetically, in the schema."
  []
  ; sample attr
  ;{:db/id 227, :db/ident :aircraft-model/model, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one, :db/index false}
  (let [result    (api/q '[:find ?v :where [_ :db.install/attribute ?v]] (db))
        attrs     (map #(->> % first (api/entity (db)) api/touch) result)
        app-attrs (->> attrs
                       (remove #(reserved-attr-nses (namespace (:db/ident %))))
                       (sort-by #(str (:db/ident %))))]
    (println ((:title-fn inspect-table) "cleancoders DB Attributes"))
    (println (:header inspect-table))
    (doseq [[attr color] (partition 2 (interleave app-attrs (cycle [40 44])))]
      (log/color-pr (format (:format inspect-table)
                            (:db/ident attr)
                            (:db/id attr)
                            (:db/valueType attr)
                            (:db/cardinality attr)
                            (:db/index attr)
                            (or (:db/unique attr) "-")
                            (or (:db/fulltext attr) "-"))
                    color))))

(defn datomic-entity [id-or-e]
  (if-let [id (:id id-or-e)]
    (api/entity (db) id)
    (api/entity (db) id-or-e)))

