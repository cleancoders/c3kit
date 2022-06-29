(ns c3kit.bucket.migrate
  (:refer-clojure :exclude [remove])
  (:require
    [c3kit.apron.app :as app]
    [c3kit.apron.log :as log]
    [c3kit.apron.util :as util]
    [c3kit.bucket.db :as db]
    [clojure.string :as str]
    [datomic.api :as api]
    ))

(def applied-migrations #{})

(defn schema-attr-id [key]
  (first (map first (api/q '[:find ?e
                             :in $ ?ident
                             :where [?e :db/ident ?ident]] (db/db) key))))

(defn attr-exists? [key]
  (not (nil? (schema-attr-id key))))

(def migration (db/build-schema :migration [[:name :string :unique]]))

(defn add [new type & spec]
  (if (attr-exists? new)
    (log/warn "\tadd: SKIPPING " new)
    (do
      (log/info (str "\tadding " new))
      (let [kind      (keyword (namespace new))
            attr-name (keyword (name new))
            attr      (db/build-attribute kind (concat [attr-name type] spec))]
        (db/transact! [attr])))))

(defn remove [attr]
  (if-let [attr-id (schema-attr-id attr)]
    (do
      (log/info (str "\tremoving " attr " (" attr-id ")"))
      (let [new-name (keyword "garbage" (str (namespace attr) "." (name attr) "_" (System/currentTimeMillis)))]
        (db/transact! [{:db/id attr-id :db/ident new-name}])))
    (log/warn "\tremove: MISSING " attr)))

(defn rename [old new]
  (when (attr-exists? new)
    (log/warn "\trename: new name EXISTS " new)
    (remove new))
  (if-let [old-id (schema-attr-id old)]
    (do (log/info (str "\trenaming " old " to " new))
        (db/transact! [{:db/id old-id :db/ident new}]))
    (log/warn "\trename FAILED: MISSING " old)))

(defn entity-values [e-id attr]
  (let [e  (api/entity (db/db) e-id)
        v  (get e attr)
        vs (if (set? v) v [v])]
    (map #(or (:db/id %) %) vs)))

(defn retract-datoms [attr]
  (->> (api/q '[:find ?e :in $ ?attr :where [?e ?attr]] (db/db) attr)
       (map first)                                          ;; entity ids
       (mapcat (fn [id] (map #(vector id %) (entity-values id attr))))
       (map (fn [[id v]] [:db/retract id attr v]))))

(defn retract-attribute-values [attr]
  (when-let [attr-id (schema-attr-id attr)]
    (log/info (str "\tretracting all values for " attr " (" attr-id ")"))
    (doall (->> (retract-datoms attr)
                (partition-all 100)
                (map db/transact!)))))

(defn retract-remove [attr]
  (retract-attribute-values attr)
  (remove attr))

(defn init-migration-list []
  (when-not (attr-exists? :migration/name)
    (log/info "\tadding migration attribute")
    (db/transact! migration))
  (let [all-migrations (db/find-all :migration :name)]
    (alter-var-root #'applied-migrations into (map :name all-migrations))))

(defn available-migrations [config]
  (->> (util/clj-files-in (:migration-dir config))
       (map util/filename->ns)
       (map #(str/split % #"\."))
       (map last)
       (filter #(re-matches #"m[0-9]{8}.*" %))
       sort))

(defn migrate! [ns-prefix migration]
  (try
    (let [migration-var (util/resolve-var (symbol (str ns-prefix "." (name migration)) "migrate"))]
      (log/info migration "-" (-> migration-var meta :doc))
      (if (get applied-migrations migration)
        (log/info "\t-> already applied")
        (do
          (log/info "\t-> NEW! Applying now...")
          (@migration-var)
          (db/tx {:kind :migration :name migration}))))
    (catch Exception e
      (log/error migration "FAILED")
      (throw e))))

(def db-partitions
  '[:find ?ident :where [:db.part/db :db.install/partition ?p]
    [?p :db/ident ?ident]])

(defn init-partition []
  (let [partition-name (db/partition-name)]
    (when-not (contains? (db/q db-partitions) [partition-name])
      (log/info "\tadding partition:" partition-name)
      (db/transact! (db/partition-schema)))))

(defn init []
  (app/start! [db/service])
  (init-partition)
  (init-migration-list))

(defn- db-entity-schema [schema]
  (for [[key spec] (seq (dissoc schema :kind :id :*))]
    (let [type (:type spec)
          db   (:db spec)
          [type db] (if (sequential? type) [(first type) (conj db :many)] [type db])]
      (concat [key type] db))))

(defn- db-enum-schema [schema] (:values schema))

(defn db-schema
  "converts the schema into format usable by datomic c3kit.bucket.db"
  [schema]
  (cond
    (:kind schema) (db-entity-schema schema)
    (:enum schema) (db-enum-schema schema)
    :else (throw (ex-info "Invalid schema" schema))))

(defn ->db-schema
  "Convert c3kit.apron.schema format into datomic transact forms"
  [schema]
  (if-let [enum (:enum schema)]
    (db/build-enum-schema enum (db-schema schema))
    (let [kind (get-in schema [:kind :value])]
      (assert kind (str "kind missing: " schema))
      (assert (keyword? kind) (str "kind must be keyword: " kind))
      (db/build-schema kind (db-schema schema)))))

(defn -main [& args]
  (try
    (let [config (db/read-config)]
      (init)

      (log/info!)

      (when-not (contains? (set args) "init")
        (log/info "Running Migrations")

        (doseq [migration (available-migrations config)]
          (migrate! (:migration-ns-prefix config) migration)))

      (let [full-schema-var (:full-schema config)
            _               (when-not full-schema-var (throw (Exception. ":full-schema missing from datomic.edn")))
            full-schema     @(util/resolve-var full-schema-var)
            schema          (mapcat ->db-schema full-schema)]
        (log/info "Applying full schema. " (count schema) " attributes")
        @(db/transact! schema))
      (log/info (str "Migration complete. " (count (db/current-schema)) " attributes found in schema.")))

    (System/exit 0)

    (catch Exception e
      (log/error e)
      (System/exit -1))))

