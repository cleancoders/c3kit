(ns c3kit.bucket.db-spec
  (:require
    [c3kit.bucket.db :as db]
    [c3kit.bucket.dbc-spec :as dbc-spec]
    [c3kit.bucket.spec-helper :as helper]
    [speclj.core :refer :all]
    ))

(describe "DB"

  (context "schema"

    (context "attributes"

      (it "simple string"
        (let [attribute (db/build-attribute :foo [:name :string])]
          ;(should-contain :db/id attribute)
          (should= :foo/name (:db/ident attribute))
          (should= :db.type/string (:db/valueType attribute))
          (should= :db.cardinality/one (:db/cardinality attribute))
          ;(should= :db.part/db (:db.install/_attribute attribute))
          ))

      (it "long"
        (let [attribute (db/build-attribute :foo [:names :long :many])]
          (should= :db.type/long (:db/valueType attribute))))

      (it "keyword-ref"
        (let [attribute (db/build-attribute :foo [:temper :kw-ref :many])]
          (should= :db.type/ref (:db/valueType attribute))))

      (it "many strings"
        (let [attribute (db/build-attribute :foo [:names :string :many])]
          (should= :db.cardinality/many (:db/cardinality attribute))))

      (it "indexed"
        (let [attribute (db/build-attribute :foo [:names :string])]
          (should= false (:db/index attribute))
          (let [attribute (db/build-attribute :foo [:names :string :index])]
            (should= true (:db/index attribute)))))

      (it "uniqueness"
        (let [attribute (db/build-attribute :foo [:name :string])]
          (should= nil (:db/unique attribute)))
        (let [attribute (db/build-attribute :foo [:name :string :unique-value])]
          (should= :db.unique/value (:db/unique attribute)))
        (let [attribute (db/build-attribute :foo [:name :string :unique-identity])]
          (should= :db.unique/identity (:db/unique attribute))))

      (it "component"
        (let [attribute (db/build-attribute :foo [:name :ref])]
          (should= false (:db/isComponent attribute)))
        (let [attribute (db/build-attribute :foo [:name :ref :component])]
          (should= true (:db/isComponent attribute))))

      (it "history"
        (let [attribute (db/build-attribute :foo [:name :ref])]
          (should= false (:db/noHistory attribute)))
        (let [attribute (db/build-attribute :foo [:name :ref :no-history])]
          (should= true (:db/noHistory attribute))))

      (it "fulltext"
        (let [attribute (db/build-attribute :foo [:name :string])]
          (should= false (:db/fulltext attribute)))
        (let [attribute (db/build-attribute :foo [:name :string :fulltext])]
          (should= true (:db/fulltext attribute))))
      )

    (it "entity"
      (let [schema (db/build-schema :bar [[:fizz :string] [:bang :long]])]
        (should= 2 (count schema))
        (should= :bar/fizz (:db/ident (first schema)))
        (should= :db.type/string (:db/valueType (first schema)))
        (should= :bar/bang (:db/ident (second schema)))
        (should= :db.type/long (:db/valueType (second schema)))))

    (it "enum schema"
      (let [schema (db/build-enum-schema :thing [:foo :bar])]
        (should= 2 (count schema))
        (should-contain {:db/ident :thing/foo} schema)
        (should-contain {:db/ident :thing/bar} schema)))
    )

  (context "partition"

    (it "default"
      (reset! db/config {})
      (should= :db.part/user (db/partition-name)))

    (it "in config"
      (reset! db/config {:partition :test})
      (should= :test (db/partition-name)))

    (it "schema"
      (reset! db/config {:partition :test})
      (should= [{:db/id "test", :db/ident :test} [:db/add :db.part/db :db.install/partition "test"]] (db/partition-schema))
      (should= [{:db/id "newbie", :db/ident :newbie} [:db/add :db.part/db :db.install/partition "newbie"]] (db/partition-schema :newbie)))

    )

  (context "CRUD"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "loading from string key"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})
            loaded (db/entity (str (:id saved)))]
        (should= :bibelot (:kind loaded))
        (should= "thingy" (:name loaded))
        (should= (:id loaded) (:id saved))))

    (it "loading from datomic entity"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})
            loaded (db/entity (db/datomic-entity saved))]
        (should= :bibelot (:kind loaded))
        (should= "thingy" (:name loaded))
        (should= (:id loaded) (:id saved))))
    )

  (context "searching"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "find all requires an attribute"
      (should-throw (db/find-all :bibelot)))

    )

  (context "transactions"

    (helper/with-db-schemas [dbc-spec/gewgaw])

    (it "async save"
      (let [g1 {:kind :gewgaw :name "1"}
            g2 {:kind :gewgaw :name "2"}
            result @(db/atx* [g1 g2])]
        (should= "1" (:name (db/reload (first result))))
        (should= "2" (:name (db/reload (second result))))))
    )

  (context "history"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "of entity"
      (let [biby (db/tx :kind :bibelot :name "Biby" :size 1 :color "blue")
            biby (db/tx biby :size 2)
            biby (db/tx biby :color "green")
            biby (db/tx biby :size 3 :color "red")
            history (db/history biby)]
        (should= 4 (count history))
        (doseq [h history]
          (should (:db/tx h))
          (should (:db/instant h)))
        (should= {:name "Biby" :size 1 :color "blue"} (select-keys (nth history 0) [:name :size :color]))
        (should= {:name "Biby" :size 2 :color "blue"} (select-keys (nth history 1) [:name :size :color]))
        (should= {:name "Biby" :size 2 :color "green"} (select-keys (nth history 2) [:name :size :color]))
        (should= {:name "Biby" :size 3 :color "red"} (select-keys (nth history 3) [:name :size :color]))))

    )
  )

