(ns c3kit.apron.corec-spec
  (:require
    [c3kit.apron.corec :as ccc]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it should= should-contain should-throw]]))

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

  (it "formats"
    (should= "Number 9" (ccc/formats "Number %s" 9)))

  (it "remove-nils"
    (should= {:a 1} (ccc/remove-nils {:a 1 :b nil})))

  (it "ex?"
    (should= false (ccc/ex? "Not an exception"))
    (should= true (ccc/ex? #?(:clj (Exception. "yup") :cljs (js/Error. "yup")))))

  )
