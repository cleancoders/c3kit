(ns c3kit.bucket.hashid-spec
  (:require
    [c3kit.bucket.hashid :as sut]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it should= should-contain should-throw before]]))

(def hashid-fns (sut/hashid-fns "test" 8))
(def id->hash (:id->hash hashid-fns))
(def hash->id (:hash->id hashid-fns))

(describe "Hashid"

  (it "encodes and decodes ids"
    (should= 123 (hash->id (id->hash 123)))
    (should= 321 (hash->id (id->hash 321)))
    (should= 277076930200755 (hash->id (id->hash 277076930200755)))
    (should= 277076930200756 (hash->id (id->hash 277076930200756))))

  (it "hashid hard coded values to make sure they're the same in clj/cljs"
    (should= "wedgpzLR" (id->hash 1))
    (should= 1 (hash->id "wedgpzLR"))
    (should= "qOd64WwJ" (id->hash 42))
    (should= 42 (hash->id "qOd64WwJ"))
    ;; MDM - seems to break down at values much higher than below.  This is plenty big for datomic ids as fas as I've seen.
    (should= "kV41EmPmEJe" (id->hash 999999999999999))
    (should= 999999999999999 (hash->id "kV41EmPmEJe")))

  (it "crashing case"
    (let [hash-fns (sut/hashid-fns "abcdefg" 30) ;; magic combo
          ->id (:hash->id hash-fns)]
      (should= nil (->id "blah"))))

  )

