(ns c3kit.bucket.db-spec
  (:import (java.util Date)
           (clojure.lang ExceptionInfo))
  (:require
    [c3kit.bucket.db :as db]
    [c3kit.bucket.dbc-spec :as dbc-spec]
    [c3kit.bucket.spec-helper :as helper]
    [c3kit.apron.time :as time :refer [seconds ago]]
    [speclj.core :refer :all]
    [c3kit.apron.log :as log]))

(def biby :undefined)
(defn sleep [entity] (Thread/sleep 10) entity)

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

  (context "tx-form"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "update"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})
            id    (:id saved)
            form  (db/tx-form (assoc saved :name "blah"))]
        (should= [id [{:bibelot/name "blah", :db/id id}]] form)))

    (it "insert "
      (let [id (db/tempid)]
        (with-redefs [db/tempid (fn [] id)]
          (let [form (db/tx-form {:kind :bibelot :name "thingy"})]
            (should= [id [{:bibelot/name "thingy", :db/id id}]] form)))))

    (it "retract"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})
            id    (:id saved)
            form  (db/tx-form (with-meta saved {:retract true}))]
        (should= [id [[:db.fn/retractEntity id]]] form)))

    (it "cas"
      (let [saved  (db/tx {:kind :bibelot :name "thingy"})
            id     (:id saved)
            update (assoc saved :name "blah")
            cas    (db/cas update {:name "thingy"})
            form   (db/tx-form cas)]
        (should= [id [[:db/cas id :bibelot/name "thingy" "blah"]]] form)))

    )

  (context "where-clause"

    (it "="
      (should= [['?e :foo/bar 123]] (db/where-clause :foo/bar 123))
      (should= [['?e :fizz/bang "whoosh"]] (db/where-clause :fizz/bang "whoosh")))

    (it "not"
      (should= ['(not [?e :foo/bar 123])] (db/where-clause :foo/bar ['not 123]))
      (should= ['(not [?e :fizz/bang "whoosh"])] (db/where-clause :fizz/bang ['not "whoosh"]))
      (should= ['[?e :fizz/bang]] (db/where-clause :fizz/bang ['not nil])))

    (it ">"
      (with-redefs [gensym (fn [prefix] (symbol prefix))]
        (should= '[[?e :foo/bar ?bar]
                   [(> ?bar 123)]]
                 (db/where-clause :foo/bar ['> 123]))))
    (it "<"
      (with-redefs [gensym (fn [prefix] (symbol prefix))]
        (should= '[[?e :foo/bar ?bar]
                   [(< ?bar 123)]]
                 (db/where-clause :foo/bar ['< 123]))))

    (it ">="
      (with-redefs [gensym (fn [prefix] (symbol prefix))]
        (should= '[[?e :foo/bar ?bar]
                   [(>= ?bar 123)]]
                 (db/where-clause :foo/bar ['>= 123]))))
    (it "<="
      (with-redefs [gensym (fn [prefix] (symbol prefix))]
        (should= '[[?e :foo/bar ?bar]
                   [(<= ?bar 123)]]
                 (db/where-clause :foo/bar ['<= 123]))))

    (it "or"
      (should= '[(or [?e :foo/bar 123]
                     [?e :foo/bar 321])]
               (db/where-clause :foo/bar [123 321])))

    (it "explicit or"
      (should= '[(or [?e :foo/bar 123]
                     [?e :foo/bar 321])]
               (db/where-clause :foo/bar ['or 123 321])))
    )


  (context "CRUD"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "loading from string key"
      (let [saved  (db/tx {:kind :bibelot :name "thingy"})
            loaded (db/entity (str (:id saved)))]
        (should= :bibelot (:kind loaded))
        (should= "thingy" (:name loaded))
        (should= (:id loaded) (:id saved))))

    (it "loading from datomic entity"
      (let [saved  (db/tx {:kind :bibelot :name "thingy"})
            loaded (db/entity (db/datomic-entity saved))]
        (should= :bibelot (:kind loaded))
        (should= "thingy" (:name loaded))
        (should= (:id loaded) (:id saved))))
    )

  (context "searching"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "find all requires an attribute"
      (should-throw (db/find-all :bibelot)))

    (it "1 nil attr"
      (log/capture-logs
        (db/tx {:kind :bibelot :name "thingy" :color nil :size 123})
        (should= [] (db/find-by :bibelot :color nil))
        (should-contain "search for nil value (:bibelot :color), returning no results." (log/captured-logs-str))))

    )

  (context "counting"

    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "1 nil attr"
      (log/capture-logs
        (db/tx {:kind :bibelot :name "thingy" :color nil :size 123})
        (should= 0 (db/count-by :bibelot :color nil))
        (should-contain "search for nil value (:bibelot :color), returning no results." (log/captured-logs-str))))
    )

  (context "transactions"

    (helper/with-db-schemas [dbc-spec/gewgaw])

    (it "async save"
      (let [g1     {:kind :gewgaw :name "1"}
            g2     {:kind :gewgaw :name "2"}
            result @(db/atx* [g1 g2])]
        (should= "1" (:name (db/reload (first result))))
        (should= "2" (:name (db/reload (second result))))))

    (it "cas success"
      (let [g1 (db/tx {:kind :gewgaw :name "1"})
            update (assoc g1 :name "2")
            cas-update (db/cas update :name "1")
            updated (db/tx cas-update)]
        (should= (db/reload g1) updated)
        (should= "2" (:name updated))))

    (it "cas with multiple attrs"
      (let [g1 (db/tx {:kind :gewgaw :name "1" :thing 123})
            update (assoc g1 :name "2" :thing 321)
            cas-update (db/cas update :name "1" :thing 123)
            updated (db/tx cas-update)]
        (should= (db/reload g1) updated)
        (should= "2" (:name updated))
        (should= 321 (:thing updated))))

    (it "cas failure"
      (let [g1 (db/tx {:kind :gewgaw :name "1"})
            update (assoc g1 :name "2")
            cas-update (db/cas update :name "WRONG")]
        (try
          (db/tx cas-update)
          (should-fail "tx should have failed")
          (catch Exception e
            (should= true (db/cas-ex? e))))))
    )

  (context "history"

    (helper/with-db-schemas [dbc-spec/bibelot])
    (with biby (-> (db/tx :kind :bibelot :name "Biby" :size 1 :color "blue")
                   sleep
                   (db/tx :size 2)
                   sleep
                   (db/tx :color "green")
                   sleep
                   (db/tx :size 3 :color "red")))

    (it "of entity"
      (let [history (db/history @biby)]
        (should= 4 (count history))
        (doseq [h history]
          (should (:db/tx h))
          (should (:db/instant h)))
        (should= {:name "Biby" :size 1 :color "blue"} (select-keys (nth history 0) [:name :size :color]))
        (should= {:name "Biby" :size 2 :color "blue"} (select-keys (nth history 1) [:name :size :color]))
        (should= {:name "Biby" :size 2 :color "green"} (select-keys (nth history 2) [:name :size :color]))
        (should= {:name "Biby" :size 3 :color "red"} (select-keys (nth history 3) [:name :size :color]))))

    (it "created-at"
      (let [moment (db/created-at @biby)
            now    (time/now)]
        (should-be-a Date moment)
        (should (time/after? moment (-> 1 seconds ago)))
        (should (time/before? moment now))))

    (it "updated-at"
      (Thread/sleep 10)
      (let [updated (db/tx @biby :size 4)
            moment  (db/updated-at updated)
            _       (Thread/sleep 10)
            now     (time/now)]
        (should-be-a Date moment)
        (should (time/after? moment (-> 1 seconds ago)))
        (should (time/before? moment now))
        (should (time/after? moment (db/created-at updated)))))

    (it "with-timestamps"
      (let [updated (db/tx @biby :size 4)
            result  (db/with-timestamps updated)]
        (should= (db/created-at updated) (:db/created-at result))
        (should= (db/updated-at updated) (:db/updated-at result))))
    )
  )
