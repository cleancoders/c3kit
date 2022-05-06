(ns c3kit.apron.corec-spec
  (:require
    [c3kit.apron.corec :as ccc]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it should= should-be-nil]]))

(describe "Core Common"

  ;(it "encodes and decodes ids"
  ;    (should= 123 (ccc/hash->id (ccc/id->hash 123)))
  ;    (should= 321 (ccc/hash->id (ccc/id->hash 321)))
  ;    (should= 277076930200755 (ccc/hash->id (ccc/id->hash 277076930200755)))
  ;    (should= 277076930200756 (ccc/hash->id (ccc/id->hash 277076930200756))))

  ;(it "hashid hard coded values to make sure they're the same in clj/cljs"
  ;    (should= "BVyGj6x2" (ccc/id->hash 1))
  ;    (should= 1 (ccc/hash->id "BVyGj6x2"))
  ;    (should= "7ayR3Jzb" (ccc/id->hash 42))
  ;    (should= 42 (ccc/hash->id "7ayR3Jzb"))
  ;    ;; MDM - seems to break down at values much higher than below.  This is plenty big for datomic ids as fas as I've seen.
  ;    (should= "jREgpG5GpaB" (ccc/id->hash 999999999999999))
  ;    (should= 999999999999999 (ccc/hash->id "jREgpG5GpaB")))


  (context "nand"
    (it "no arguments" (should= false (ccc/nand)))
    (it "one falsy argument" (should= true (ccc/nand nil)))
    (it "one truthy argument" (should= false (ccc/nand 1)))
    (it "two truthy arguments" (should= false (ccc/nand 1 2)))
    (it "two falsy arguments" (should= true (ccc/nand false nil)))
    (it "three truthy arguments" (should= false (ccc/nand 1 2 3)))
    (it "three falsy arguments" (should= true (ccc/nand false nil (not true))))
    (it "two truthy and one falsy argument" (should= true (ccc/nand 1 2 false)))
    (it "truthy then falsy argument" (should= true (ccc/nand 1 nil)))
    (it "falsy then truthy argument" (should= true (ccc/nand nil 1)))
    (it "lazy evaluation on the first falsy value" (should= true (ccc/nand nil (/ 1 0))))
    (it "evaluates each form exactly once"
      (let [flag (atom false)]
        (should= false (ccc/nand (swap! flag not) true))
        (should= true @flag))))

  (context "nor"
    (it "no arguments" (should= true (ccc/nor)))
    (it "one falsy argument" (should= true (ccc/nor nil)))
    (it "one truthy argument" (should= false (ccc/nor 1)))
    (it "two truthy arguments" (should= false (ccc/nor 1 2)))
    (it "two falsy arguments" (should= true (ccc/nor false nil)))
    (it "three truthy arguments" (should= false (ccc/nor 1 2 3)))
    (it "three falsy arguments" (should= true (ccc/nor false nil (not true))))
    (it "two truthy and one falsy argument" (should= false (ccc/nor 1 2 false)))
    (it "truthy then falsy argument" (should= false (ccc/nor 1 nil)))
    (it "falsy then truthy argument" (should= false (ccc/nor nil 1)))
    (it "lazy evaluation on the first truthy value" (should= false (ccc/nor 1 (/ 1 0))))
    (it "evaluates each form exactly once"
      (let [flag (atom true)]
        (should= true (ccc/nor (swap! flag not) nil))
        (should= false @flag))))

  (context "xor"
    (it "no arguments" (should-be-nil (ccc/xor)))
    (it "one nil argument" (should-be-nil (ccc/xor nil)))
    (it "one false argument" (should-be-nil (ccc/xor false)))
    (it "one truthy argument" (should= 1 (ccc/xor 1)))
    (it "nil then false arguments" (should-be-nil (ccc/xor nil false)))
    (it "false then nil arguments" (should-be-nil (ccc/xor false nil)))
    (it "two truthy arguments" (should-be-nil (ccc/xor true 1)))
    (it "falsy then truthy arguments" (should= 1 (ccc/xor false 1)))
    (it "truthy then falsy arguments" (should= 1 (ccc/xor 1 false)))
    (it "truthy, falsy, then truthy arguments" (should-be-nil (ccc/xor 1 nil 2)))
    (it "lazy evaluation on the second truthy value" (should-be-nil (ccc/xor 1 2 (/ 1 0))))
    (it "four arguments with one truthy value" (should= 4 (ccc/xor nil false nil 4)))
    (it "four arguments with two truthy values" (should= nil (ccc/xor nil false 3 4)))
    (it "evaluates each form exactly once"
      (let [flag (atom 0)]
        (should= 1 (ccc/xor (swap! flag inc) nil nil))
        (should= 1 @flag))))

  (context "->options"

    (it "nil -> {}"
      (should= {} (ccc/->options [nil]))
      )

    )

  (it "new-uuid"
    (should= 10 (->> (repeatedly ccc/new-uuid)
                     (take 10)
                     set
                     count)))

  (it "conjv"
    (let [result (ccc/conjv (list 1 2 3) 4)]
      (should= [1 2 3 4] result)
      (should= true (vector? result))))

  (it "concatv"
    (let [result (ccc/concatv (list 1 2 3) (list 4))]
      (should= [1 2 3 4] result)
      (should= true (vector? result))))

  (it "dissocv"
    (let [result (ccc/dissocv [1 2 3] 1)]
      (should= [1 3] result)
      (should= true (vector? result))))

  (it "assocv"
    (let [result (ccc/assocv [1 2 3] 1 :foo)]
      (should= [1 :foo 2 3] result)
      (should= true (vector? result))))

  (it "removev"
    (let [result (ccc/removev even? (list 1 2 3 4))]
      (should= [1 3] result)
      (should= true (vector? result))))

  (it "removev="
    (let [result (ccc/removev= (list 1 2 3 4) 2)]
      (should= [1 3 4] result)
      (should= true (vector? result))))

  (context "ffilter"
    (for [coll [nil [] [nil]]]
      (it (str "is nil when " (pr-str coll))
        (should-be-nil (ccc/ffilter any? coll))))
    (it "single-element collection"
      (should= 1 (ccc/ffilter any? [1])))
    (it "two-element collection"
      (should= 2 (ccc/ffilter any? [2 1])))
    (it "first item of filtered result"
      (should= :a (ccc/ffilter identity [nil false :a :b])))
    (it "first item of no results"
      (should-be-nil (ccc/ffilter number? [nil false :a :b]))))

  (context "rsort"
    (it "a nil collection"
      (should= [] (ccc/rsort nil)))
    (it "an empty collection"
      (should= [] (ccc/rsort [])))
    (it "a single-element collection"
      (should= [1] (ccc/rsort [1])))
    (it "an already reverse-sorted collection"
      (should= [5 4 3 2 1] (ccc/rsort [5 4 3 2 1])))
    (it "a regular-sorted collection"
      (should= [5 4 3 2 1] (ccc/rsort [1 2 3 4 5])))
    (it "a shuffled collection"
      (should= [5 4 3 2 1] (ccc/rsort [4 5 1 3 2])))
    (it "by custom compare function"
      (should= [[1 5] [2 4] [3 3] [4 2] [5 1]]
               (ccc/rsort
                 (fn [x y] (compare (second x) (second y)))
                 [[5 1] [4 2] [3 3] [2 4] [1 5]]))))

  (context "rsort"
    (it "a nil collection"
      (should= [] (ccc/rsort-by :x nil)))
    (it "an empty collection"
      (should= [] (ccc/rsort-by :x [])))
    (it "a single-element collection"
      (should= [{:x 1}] (ccc/rsort-by :x [{:x 1}])))
    (it "an already reverse-sorted collection"
      (let [coll [{:x 5} {:x 4} {:x 3} {:x 2} {:x 1}]]
        (should= (reverse (sort-by :x coll)) (ccc/rsort-by :x coll))))
    (it "a regular-sorted collection"
      (let [coll [{:x 1} {:x 2} {:x 3} {:x 4} {:x 5}]]
        (should= (reverse (sort-by :x coll)) (ccc/rsort-by :x coll))))
    (it "a shuffled collection"
      (let [coll [{:x 4} {:x 5} {:x 1} {:x 3} {:x 2}]]
        (should= (reverse (sort-by :x coll)) (ccc/rsort-by :x coll))))
    (it "by custom compare function"
      (let [coll [{:a [5 1]} {:a [4 2]} {:a [3 3]} {:a [2 4]} {:a [1 5]}]
            compare-fn (fn [x y] (compare (second x) (second y)))]
        (should= (reverse (sort-by :a compare-fn coll))
                 (ccc/rsort-by :a compare-fn coll)))))

  (it "formats"
    (should= "Number 9" (ccc/formats "Number %s" 9)))

  (it "remove-nils"
    (should= {:a 1} (ccc/remove-nils {:a 1 :b nil})))

  (it "ex?"
    (should= false (ccc/ex? "Not an exception"))
    (should= true (ccc/ex? #?(:clj (Exception. "yup") :cljs (js/Error. "yup")))))

  )
