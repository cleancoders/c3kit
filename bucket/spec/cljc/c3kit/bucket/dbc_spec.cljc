(ns c3kit.bucket.dbc-spec
  (:require
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it xit should= should-contain
                                                      should-not-contain should-throw should-be-a with
                                                      should-not= before should should-not should-not-throw
                                                      focus-it]]
    [c3kit.bucket.db :as db]
    [c3kit.apron.log :as log]
    [c3kit.apron.time :as time :refer [seconds ago from-now]]
    [c3kit.apron.schema :as s]
    [c3kit.bucket.spec-helper :as helper]
    ))

(def bibelot
  {:kind  (s/kind :bibelot)
   :id    s/id
   :name  {:type :string}
   :size  {:type :long}
   :color {:type :string}})

(def gewgaw
  {:kind  (s/kind :gewgaw)
   :id    s/id
   :name  {:type :string}
   :thing {:type :ref}})

(def doodad
  {:kind    (s/kind :doodad)
   :id      s/id
   :names   {:type [:string]}
   :numbers {:type [:long]}
   :things  {:type [:ref]}})

(def thingy
  {:kind (s/kind :thingy)
   :id   s/id
   :foo  {:type :string}
   :bar  {:type :ref}
   :fizz {:type :long}
   :bang {:type :keyword}})

(def tempy
  {:kind (s/kind :tempy)
   :id   s/id
   :when {:type :instant}})

(def child :undefined)
(def original :undefined)

(describe "DB Common"

  (context "CRUD"

    (helper/with-db-schemas [bibelot])

    (it "returns nil on missing id"
      (should= nil (db/entity -1))
      (should= nil (db/entity nil))
      (should= nil (db/entity ""))
      (should= nil (db/entity "-1")))

    (it "tx nil entities"
      (should-not-throw (db/tx nil))
      (should-not-throw (db/tx* [nil])))

    (it "create and read"
      (let [saved  (db/tx {:kind :bibelot :name "thingy"})
            loaded (db/entity (:id saved))]
        (should= :bibelot (:kind loaded))
        (should= "thingy" (:name loaded))
        (should= (:id loaded) (:id saved))))

    (it "entity!"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})]
        (should= saved (db/entity! (:id saved)))
        (should-throw (db/entity! 9999))))

    (it "entity-of-kind"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})]
        (should= saved (db/entity-of-kind :bibelot (:id saved)))
        (should= nil (db/entity-of-kind :other (:id saved)))))

    (it "entity-of-kind!"
      (let [saved (db/tx {:kind :bibelot :name "thingy"})]
        (should= saved (db/entity-of-kind! :bibelot (:id saved)))
        (should-throw (db/entity-of-kind! :other (:id saved)))))

    (it "updating"
      (let [saved   (db/tx {:kind :bibelot :name "thingy"})
            updated (db/tx saved :name "whatsamajigger")
            loaded  (db/entity (:id saved))]
        (should= "whatsamajigger" (:name loaded))
        (should= (:id saved) (:id loaded))
        (should= (:id saved) (:id updated))))

    (it "retracting via metadata"
      (let [saved   (db/tx {:kind :bibelot :name "thingy"})
            updated (db/tx (with-meta saved {:retract true}))]
        (should= nil (db/entity (:id saved)))
        (should= {:kind :db/retract :id (:id saved)} updated)))

    (it "retracting via :kind :db/retract"
      (let [saved   (db/tx {:kind :bibelot :name "thingy"})
            updated (db/tx (assoc saved :kind :db/retract))]
        (should= nil (db/entity (:id saved)))
        (should= {:kind :db/retract :id (:id saved)} updated)))

    (it "retracting when passed an entity"
      (let [saved     (db/tx {:kind :bibelot :name "thingy"})
            retracted (db/retract saved)]
        (should= {:kind :db/retract :id (:id saved)} retracted)
        (should= [] (db/find-by :bibelot :name "thingy"))))

    (it "retracting when passed an id"
      (let [saved     (db/tx {:kind :bibelot :name "thingy"})
            retracted (db/retract (:id saved))]
        (should= {:kind :db/retract :id (:id saved)} retracted)
        (should= [] (db/find-by :bibelot :name "thingy"))))
    )

  (context "find-by"

    (helper/with-db-schemas [bibelot thingy doodad])

    (it "find by attribute"
      (let [saved  (db/tx {:kind :bibelot :name "thingy" :color "blue" :size 123})
            loaded (db/find-by :bibelot :name "thingy")]
        (should= (:id saved) (:id (first loaded)))
        (should= "thingy" (:name (first loaded)))
        (should= "blue" (:color (first loaded)))
        (should= 123 (:size (first loaded)))))

    (it "find by two attribute"
      (let [saved  (db/tx {:kind :bibelot :name "thingy" :color "blue" :size 123})
            loaded (db/find-by :bibelot :name "thingy" :color "blue")]
        (should= (:id saved) (:id (first loaded)))))

    (it "find by three attribute"
      (let [saved  (db/tx {:kind :bibelot :name "thingy" :color "blue" :size 123})
            loaded (db/find-by :bibelot :name "thingy" :color "blue" :size 123)]
        (should= (:id saved) (:id (first loaded)))))

    (it "find-by with 4 attrs"
      (let [bibby (db/tx {:kind :bibelot :name "bibby"})
            saved (db/tx :kind :thingy :foo "paul" :bar (:id bibby) :fizz 2 :bang :paul)]
        (should (db/ffind-by :thingy :foo "paul" :bar (:id bibby) :fizz 2 :bang :paul))
        (should-not (db/ffind-by :thingy :foo "ringo" :bar (:id bibby) :fizz 2 :bang :paul))))

    (it "find by nil"
      (let [b1 (db/tx :kind :bibelot :name "Bee" :size 1)
            b2 (db/tx :kind :bibelot :name "Bee" :color "blue")
            b3 (db/tx :kind :bibelot :size 1 :color "blue")]
        (should= [b1] (db/find-by :bibelot :name "Bee" :color nil))
        (should= [b2] (db/find-by :bibelot :name "Bee" :size nil))
        (should= [b3] (db/find-by :bibelot :color "blue" :name nil))))

    (it "find by not nil"
      (let [b1 (db/tx :kind :bibelot :name "Bee" :size 1)
            b2 (db/tx :kind :bibelot :name "Bee" :color "blue")
            b3 (db/tx :kind :bibelot :size 1 :color "blue")]
        (should= [b2] (db/find-by :bibelot :name "Bee" :color ['not nil]))
        (should= [b1] (db/find-by :bibelot :name "Bee" :size ['not nil]))
        (should= [b2] (db/find-by :bibelot :color "blue" :name ['not nil]))
        (should= [b3] (db/find-by :bibelot :size 1 :color ['not nil]))))

    (it "not"
      (let [b1 (db/tx :kind :bibelot :name "Bee" :color "red" :size 1)
            b2 (db/tx :kind :bibelot :name "Bee" :color "blue" :size 2)
            b3 (db/tx :kind :bibelot :name "Ant" :color "blue" :size 1)]
        (should= [b3] (db/find-by :bibelot :name ['not "Bee"] :size 1))
        (should= [b3] (db/find-by :bibelot :name ['not "Bee"] :color "blue"))
        (should= [b1] (db/find-by :bibelot :name ['not "Ant"] :size 1))
        (should= [b1] (db/find-by :bibelot :name "Bee" :size ['not 2]))
        (should= [b2] (db/find-by :bibelot :name "Bee" :size ['not 1]))
        (should= [b3] (db/find-by :bibelot :size 1 :color ['not "red"]))))

    (it "not multi-value"
      (let [d1 (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            d2 (db/tx {:kind :doodad :names ["foo" "bang"] :numbers [8 43]})]
        (should= [d1] (db/find-by :doodad :names "foo" :numbers ['not 43]))
        (should= [d2] (db/find-by :doodad :names "foo" :numbers ['not 42]))
        (should= [d2] (db/find-by :doodad :names ['not "bar"] :numbers 8))
        (should= [d1] (db/find-by :doodad :names ['not "bang"] :numbers 8))))

    (it "or"
      (let [b1 (db/tx :kind :bibelot :name "Bee" :color "red" :size 1)
            b2 (db/tx :kind :bibelot :name nil :color "blue" :size 2)
            b3 (db/tx :kind :bibelot :name "Ant" :color "blue" :size 1)]
        (should= [b1 b3] (db/find-by :bibelot :name ["Bee" "Ant"]))
        (should= [b1 b3] (db/find-by :bibelot :name #{"Bee" "Ant"}))
        ;(should= [b1 b2 ] (db/find-by :bibelot :name ["Bee" nil]))
        (should= [b3] (db/find-by :bibelot :name ["BLAH" "Ant"]))
        (should= [] (db/find-by :bibelot :name ["BLAH" "ARG"]))
        (should= [] (db/find-by :bibelot :name []))
        (should= [] (db/find-by :bibelot :name [] :size 1))))

    (it "explicit or"
      (let [b1 (db/tx :kind :bibelot :name "Bee" :color "red" :size 1)
            b2 (db/tx :kind :bibelot :name "Bee" :color "blue" :size 2)
            b3 (db/tx :kind :bibelot :name "Ant" :color "blue" :size 1)]
        (should= [b1 b2 b3] (db/find-by :bibelot :name ["Bee" "Ant"]))
        (should= [b3] (db/find-by :bibelot :name ['or "BLAH" "Ant"]))
        (should= [] (db/find-by :bibelot :name ['or "BLAH" "ARG"]))
        (should= [] (db/find-by :bibelot :name ['or ]))
        (should= [] (db/find-by :bibelot :name ['or ] :size 1))))

    (it "or multi-value"
      (let [d1 (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            d2 (db/tx {:kind :doodad :names ["foo" "bang"] :numbers [8 43]})]
        (should= [d1 d2] (db/find-by :doodad :names ["foo" "BLAH"]))
        (should= [d1 d2] (db/find-by :doodad :names ["bar" "bang"]))
        (should= [d1] (db/find-by :doodad :names ["bar" "BLAH"]))
        (should= [] (db/find-by :doodad :names ["ARG" "BLAH"]))))

    (context "<>: "

      (helper/with-db-schemas [bibelot thingy tempy])

      (it "long"
        (let [b1 (db/tx :kind :bibelot :size 1)
              b2 (db/tx :kind :bibelot :size 2)
              b3 (db/tx :kind :bibelot :size 3)]
          (should= [b1 b2 b3] (db/find-by :bibelot :size ['> 0]))
          (should= [b2 b3] (db/find-by :bibelot :size ['> 1]))
          (should= [b3] (db/find-by :bibelot :size ['> 2]))
          (should= [] (db/find-by :bibelot :size ['> 3]))
          (should= [b1 b2 b3] (db/find-by :bibelot :size ['< 4]))
          (should= [b1 b2] (db/find-by :bibelot :size ['< 3]))
          (should= [b1] (db/find-by :bibelot :size ['< 2]))
          (should= [] (db/find-by :bibelot :size ['< 1]))
          (should= [b2 b3] (db/find-by :bibelot :size ['>= 2]))
          (should= [b1 b2] (db/find-by :bibelot :size ['<= 2]))))

      (it "string"
        (let [b1 (db/tx :kind :bibelot :name "foo")]
          (should= [b1] (db/find-by :bibelot :name ['> "a"]))
          (should= [] (db/find-by :bibelot :name ['> "foo"]))
          (should= [] (db/find-by :bibelot :name ['< "foo"]))
          (should= [b1] (db/find-by :bibelot :name ['< "z"]))
          (should= [b1] (db/find-by :bibelot :name ['<= "foo"]))
          (should= [b1] (db/find-by :bibelot :name ['>= "foo"]))
          (should= [] (db/find-by :bibelot :name ['<= "fom"]))
          (should= [] (db/find-by :bibelot :name ['>= "fop"]))))

      (it "ref"
        (let [b1 (db/tx :kind :thingy :bar 123)]
          (should= [b1] (db/find-by :thingy :bar ['> 0]))
          (should= [] (db/find-by :thingy :bar ['> 123]))
          (should= [] (db/find-by :thingy :bar ['< 123]))
          (should= [b1] (db/find-by :thingy :bar ['< 124]))
          (should= [b1] (db/find-by :thingy :bar ['>= 123]))
          (should= [b1] (db/find-by :thingy :bar ['<= 123]))
          (should= [] (db/find-by :thingy :bar ['>= 124]))
          (should= [] (db/find-by :thingy :bar ['<= 122]))))

      (it "date"
        (let [now (time/now)
              b1  (db/tx :kind :tempy :when now)]
          (should= [b1] (db/find-by :tempy :when ['> (-> 1 seconds ago)]))
          (should= [] (db/find-by :tempy :when ['> now]))
          (should= [] (db/find-by :tempy :when ['< now]))
          (should= [b1] (db/find-by :tempy :when ['< (-> 1 seconds from-now)]))
          (should= [b1] (db/find-by :tempy :when ['<= now]))
          (should= [b1] (db/find-by :tempy :when ['>= now]))
          (should= [] (db/find-by :tempy :when ['<= (-> 1 seconds ago)]))
          (should= [] (db/find-by :tempy :when ['>= (-> 1 seconds from-now)]))))
      )
    )

  (context "find-ids-by"

    (helper/with-db-schemas [bibelot])

    (it "find by attribute"
      (let [saved (db/tx {:kind :bibelot :name "thingy" :color "blue" :size 123})]
        (should= [(:id saved)] (db/find-ids-by :bibelot :name "thingy"))
        (should= [] (db/find-ids-by :bibelot :name "blah"))))

    )

  (context "count-by"

    #?(:clj  (helper/with-db-schemas [bibelot thingy])
       :cljs (helper/with-db-schemas [bibelot thingy]))

    (it "count by attribute"
      (let [saved (db/tx {:kind :bibelot :name "thingy" :color "blue" :size 123})]
        (should= 1 (db/count-by :bibelot :name "thingy"))
        (should= 0 (db/count-by :bibelot :name "blah"))))

    (it "count-by with 4 attrs"
      (let [bibby (db/tx {:kind :bibelot :name "bibby"})
            saved (db/tx :kind :thingy :foo "paul" :bar (:id bibby) :fizz 2 :bang :paul)]
        (should= 1 (db/count-by :thingy :foo "paul" :bar (:id bibby) :fizz 2 :bang :paul))
        (should= 0 (db/count-by :thingy :foo "ringo" :bar (:id bibby) :fizz 2 :bang :paul))
        (should= 0 (db/count-by :thingy :foo []))
        (should= 0 (db/count-by :thingy :foo [] :fizz 2))))
    )

  (context "find-all"

    #?(:clj  (helper/with-db-schemas [bibelot])
       :cljs (helper/with-db-schemas [bibelot]))
    (before (db/tx :kind :bibelot :name "john" :color "red" :size 1)
            (db/tx :kind :bibelot :name "paul" :color "green" :size 2)
            (db/tx :kind :bibelot :name "george" :color "blue" :size 3))

    (it "find all bibelot/name"
      (let [all (db/find-all :bibelot :name)]
        (should= 3 (count all))
        (should= #{1 2 3} (set (map :size all)))))

    (it "find all bibelot/color"
      (let [all (db/find-all :bibelot :color)]
        (should= 3 (count all))
        (should= #{1 2 3} (set (map :size all)))))
    )

  (context "count-all"

    #?(:clj  (helper/with-db-schemas [bibelot thingy])
       :cljs (helper/with-db-schemas [bibelot thingy]))
    (before (db/tx :kind :bibelot :name "john" :color "red" :size 1)
            (db/tx :kind :bibelot :name "paul" :color "green" :size 2)
            (db/tx :kind :bibelot :name "george" :color "blue" :size 3))

    (it "find all bibelot/name"
      (should= 0 (db/count-all :thingy :foo))
      (should= 3 (db/count-all :bibelot :name)))
    )


  (context "reference values"

    #?(:clj  (helper/with-db-schemas [bibelot gewgaw])
       :cljs (helper/with-db-schemas [bibelot gewgaw]))

    (it "loading"
      (let [child        (db/tx {:kind :bibelot :name "child" :color "golden"})
            saved        (db/tx {:kind :gewgaw :name "parent" :thing (:id child)})
            loaded       (db/entity (:id saved))
            loaded-child (db/entity (:thing loaded))]
        (should= (:id loaded) (:id saved))
        (should= (:id child) (:id loaded-child))))

    (it "find by attribute"
      (let [child  (db/tx {:kind :bibelot :name "child" :color "golden"})
            saved  (db/tx {:kind :gewgaw :name "parent" :thing (:id child)})
            result (db/find-by :gewgaw :thing (:id child))]
        (should= (:id saved) (:id (first result)))
        (should= "parent" (:name (first result)))))

    (it "pass through loading and saving seamelessly"
      (let [child       (db/tx {:kind :bibelot :name "child" :color "golden"})
            saved       (db/tx {:kind :gewgaw :name "parent" :thing (:id child)})
            loaded      (db/entity (:id saved))
            saved-again (db/tx loaded)]
        (should= (:thing loaded) (:thing saved-again))))

    )

  (context "multiple values"

    #?(:clj  (helper/with-db-schemas [bibelot doodad])
       :cljs (helper/with-db-schemas [bibelot doodad]))

    (it "loading"
      (let [saved  (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            loaded (db/entity (:id saved))]
        (should= (:id loaded) (:id saved))
        (should= #{"foo" "bar"} (set (:names loaded)))
        (should= #{8 42} (set (:numbers loaded)))))

    (it "find by attribute"
      (let [saved  (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            loaded (db/find-by :doodad :names "bar")]
        (should= 1 (count loaded))
        (should= (:id saved) (:id (first loaded)))))

    (it "retracting [string] value"
      (let [saved   (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            updated (db/tx saved :names nil)]
        (should= nil (seq (:names updated)))))

    (it "retracting one value from [string]"
      (let [saved   (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            updated (db/tx saved :names ["foo"])]
        (should= #{"foo"} (set (:names updated)))))

    (it "adding one value to [string]"
      (let [saved   (db/tx {:kind :doodad :names ["foo" "bar"] :numbers [8 42]})
            updated (db/tx saved :names ["foo" "bar" "fizz"])]
        (should= #{"foo" "bar" "fizz"} (set (:names updated)))))

    (it "using refs"
      (let [child1 (db/tx {:kind :bibelot :name "child1" :color "golden"})
            child2 (db/tx {:kind :bibelot :name "child2" :color "silver"})
            saved  (db/tx {:kind :doodad :things [(:id child1) (:id child2)]})
            loaded (db/entity (:id saved))]
        (should= #{(:id child1) (:id child2)} (set (:things saved)))
        (should= #{(:id child1) (:id child2)} (set (:things loaded)))))

    (it "retracting whole [ref] value"
      (let [child1  (db/tx {:kind :bibelot :name "child1" :color "golden"})
            child2  (db/tx {:kind :bibelot :name "child2" :color "silver"})
            saved   (db/tx {:kind :doodad :things [(:id child1) (:id child2)]})
            updated (db/tx saved :things nil)]
        (should= nil (seq (:things updated)))))

    (it "removing one [ref] values"
      (let [child1  (db/tx {:kind :bibelot :name "child1" :color "golden"})
            child2  (db/tx {:kind :bibelot :name "child2" :color "silver"})
            saved   (db/tx {:kind :doodad :things [(:id child1) (:id child2)]})
            updated (db/tx saved :things [(:id child1)])]
        (should= #{(:id child1)} (set (:things updated)))))

    (it "adding one [ref] values"
      (let [child1  (db/tx {:kind :bibelot :name "child1" :color "golden"})
            child2  (db/tx {:kind :bibelot :name "child2" :color "silver"})
            child3  (db/tx {:kind :bibelot :name "child3" :color "bronze"})
            saved   (db/tx {:kind :doodad :things [(:id child1) (:id child2)]})
            updated (db/tx saved :things [(:id child1) (:id child2) (:id child3)])]
        (should= #{(:id child1) (:id child2) (:id child3)} (set (:things updated)))))

    )

  (context "retracting null values"
    #?(:clj  (helper/with-db-schemas [bibelot thingy])
       :cljs (helper/with-db-schemas [bibelot thingy]))

    (with child (db/tx {:kind :bibelot :name "child" :color "golden"}))
    (with original (db/tx {:kind :thingy
                           :foo  "foo"
                           :bar  (:id @child)
                           :fizz 2015
                           :bang :kapow!}))


    (it "can set a string to nil"
      (let [result (db/tx (assoc @original :foo nil))]
        (should= nil (:foo result))
        (should= nil (:foo (db/reload @original)))))

    (it "can set a ref to nil"
      (let [result (db/tx (assoc @original :bar nil))]
        (should= nil (:bar result))
        (should= nil (:bar (db/reload @original)))))

    (it "can set a long to nil"
      (let [result (db/tx (dissoc @original :fizz))]
        (should= nil (:fizz result))
        (should= nil (:fizz (db/reload @original)))))

    (it "can set a keyword to nil"
      (let [result (db/tx (dissoc @original :bang))]
        (should= nil (:bang result))
        (should= nil (:bang (db/reload @original)))))
    )

  (context "transactions"

    #?(:clj  (helper/with-db-schemas [bibelot gewgaw])
       :cljs (helper/with-db-schemas [bibelot gewgaw]))

    (it "save multiple entities at the same time"
      (let [g1     {:kind :gewgaw :name "1"}
            g2     {:kind :gewgaw :name "2"}
            result (db/tx* [g1 g2])]
        (should= "1" (:name (db/reload (first result))))
        (should= "2" (:name (db/reload (second result))))))

    (it "update multiple entities at the same time"
      (let [g1     (db/tx {:kind :gewgaw :name "1"})
            g2     (db/tx {:kind :gewgaw :name "2"})
            result (db/tx* [(assoc g1 :name "one") (dissoc g2 :name)])]
        (should= "one" (:name (db/reload (first result))))
        (should= nil (:name (db/reload (second result))))))

    (it "retract with meta-data on any of the entities"
      (let [g1 (db/tx {:kind :gewgaw :name "1"})
            g2 (db/tx {:kind :gewgaw :name "2"})
            [u1 u2] (db/tx* [(assoc g1 :name "one") (assoc g2 :kind :db/retract)])]
        (should= "one" (:name (db/reload u1)))
        (should= nil (db/reload u2))
        (should= {:kind :db/retract :id (:id g2)} u2)))

    (it "temp-ids are resolved"
      (let [bibby  {:kind :bibelot :name "bibby" :id (db/tempid)}
            gewy   {:kind :gewgaw :thing (:id bibby)}
            result (db/tx* [bibby gewy])
            [saved-bibby saved-gewy] result]
        (should= (:id saved-bibby) (:thing saved-gewy))))
    )
  )
