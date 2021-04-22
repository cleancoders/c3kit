(ns c3kit.bucket.db
"A Simple in-memory database.
 Data pulled from the server is stored here to be easily retrieved some time later."
  (:require
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.legend :as legend]
    [c3kit.apron.log :as log]
    [c3kit.apron.utilc :as utilc]
    [c3kit.bucket.dbc :as dbc]
    ))

(def ^:private db (atom {:all {}}))
(defn replace-db-atom! [new-atom]
  (reset! new-atom @db)
  (set! db new-atom))

(def ^:private id-source (atom 1000))

(defn- gen-id [] (swap! id-source inc))
(defn- ensure-id [e] (if (:id e) e (assoc e :id (gen-id))))
(defn clear! [] (reset! db {:all {}}))

(defn- merge-with-original [updated original]
       (if original
         (let [retracted-keys (doall (filter #(= nil (get updated %)) (keys original)))
               merged (merge original updated)]
              (apply dissoc merged retracted-keys))
         updated))

(defn- retract-entity [db entity]
       (let [id (:id entity)
             kind (get-in db [:all id :kind])]
            (-> db
                (update :all dissoc id)
                (update kind dissoc id))))

(defn- install-entity [db e]
       (assert (:id e) (str "entity missing id!: " e))
       (let [original (get-in db [:all (:id e)])
             e (-> (utilc/keywordize-kind e)
                   (merge-with-original original)
                   legend/coerce!)]
            (-> db
                (update :all assoc (:id e) e)
                (update (:kind e) assoc (:id e) e))))

(defn- tx-entity [db e]
       (if (dbc/retract? e)
         (retract-entity db e)
         (install-entity db e)))

(defn- field-matches? [e [k v]]
       (let [ev (get e k)]
            (cond
              (sequential? ev) (some #(= v %) ev)
              (set? ev) (get ev v)
              :else (= v ev))))

(defn- entity-matches? [spec e]
       (every? (partial field-matches? e) spec))

;; db api -----------------------------------

(defn squuid [] (random-uuid))

(defn tempid [] (gen-id))

(defn entity [id]
      (cond
        (nil? id) nil
        (map? id) (get-in @db [:all (:id id)])
        :else (get-in @db [:all id])))

(defn entity! [id] (dbc/entity! (entity id) id))
(defn entity-of-kind! [kind id] (dbc/entity-of-kind! (entity id) kind id))
(defn entity-of-kind [kind id] (dbc/entity-of-kind (entity id) kind))
(defn reload [e] (when-let [id (:id e)] (entity id)))

(defn- tx-result [entity]
       (if (dbc/retract? entity)
         {:kind :db/retract :id (:id entity)}
         (reload entity)))

(defn tx [& args]
      (let [e (ccc/->options args)]
           (when (seq e)
                 (let [e (ensure-id e)]
                      (swap! db tx-entity e)
                      (tx-result e)))))

(defn tx* [entities]
      (let [entities (map ensure-id (remove nil? entities))]
           (swap! db (fn [db] (reduce #(tx-entity %1 %2) db entities)))
           (map tx-result entities)))

(defn count-all
      ([kind attr] (count-all kind))                            ;; MDM - compatibility with datomic db
      ([kind] (count (get @db kind))))

(defn find-all
      ([kind attr] (find-all kind))                             ;; MDM - compatibility with datomic db
      ([kind] (vals (get @db kind))))

(defn find-by [kind & kvs]
      ;(assert (even? (count kvs)) "Each attribute must have a value")
      (let [kv-pairs (partition 2 kvs)
            kinds (vals (get @db kind))]
           (assert (every? keyword? (map first kv-pairs)) "Attributes must be keywords")
           (filter (partial entity-matches? kv-pairs) kinds)))

(defn count-by [kind & kv]
      (count (apply find-by kind kv)))

(defn ffind-by [kind & kvs]
      (first (apply find-by kind kvs)))

(defn retract [id-or-entity]
      (if-let [entity (entity id-or-entity)]
              (tx (assoc entity :kind :db/retract))
              (log/warn "Attempt to retract missing entity: " id-or-entity)))

