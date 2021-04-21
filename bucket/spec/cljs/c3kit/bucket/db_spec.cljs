(ns c3kit.bucket.db-spec
  (:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before with-stubs with around
                                        should-contain should-not-contain should-throw should should-not-throw]])
  (:require [c3kit.bucket.db :as db]
            [c3kit.bucket.dbc-spec :as dbc-spec]
            [c3kit.bucket.spec-helper :as helper]))

(describe "DB"

  (context "CRUD"
    (helper/with-db-schemas [dbc-spec/bibelot])

    (it "kind string is converted to keyword"
      (let [e (db/tx {:kind "bibelot" :size 1})]
        (should= :bibelot (:kind e))))

    (it "tx nil values"
      (let [bang1 (db/tx :kind :bibelot :size 1 :name "bibby")]
        (should= (dissoc bang1 :size) (db/tx bang1 :size nil))
        (should= (dissoc bang1 :name) (db/tx (dissoc bang1 :name)))))
    )

  (context "find-all"

    (helper/with-db-schemas [dbc-spec/bibelot])
    (before (db/tx :kind :bibelot :name "john" :color "red" :size 1)
            (db/tx :kind :bibelot :name "paul" :color "green" :size 2)
            (db/tx :kind :bibelot :name "george" :color "blue" :size 3))

    (it "doesn't required attribute"
      (let [all (db/find-all :bibelot)]
        (should= 3 (count all))
        (should= #{1 2 3} (set (map :size all)))))
    )
  )
