(ns c3kit.bucket.spec-helper
  (:require
    [c3kit.bucket.db :as db]
    [c3kit.bucket.migrate :as migrate]
    [datomic.api :as datomic]
    [speclj.core :refer :all]
    ))

(defn test-connection [schema]
  (let [uri "datomic:mem://test"]
    (datomic/delete-database uri)
    (let [connection (db/connect uri)]
      @(db/transact! (concat (db/partition-schema :poker) schema) connection)
      connection)))

(defn test-connection-from-schemas [schemas]
  (let [schemas (if (sequential? schemas) (flatten schemas) [schemas])
        schema (mapcat migrate/->db-schema schemas)]
    (test-connection schema)))

(defn with-db-schemas [schemas]
  (around [it]
          (let [connection (delay (test-connection-from-schemas schemas))]
            (with-redefs [c3kit.bucket.db/connection connection]
              (it)))))
