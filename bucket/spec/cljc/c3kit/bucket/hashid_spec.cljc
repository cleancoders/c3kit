(ns c3kit.bucket.hashid-spec
  (:require
    [c3kit.apron.hashid :as sut]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it should= should-contain should-throw]]))

(describe "Hashid"

  (it "encodes and decodes ids"
    (should= 123 (sut/hash->id (sut/id->hash 123)))
    (should= 321 (sut/hash->id (sut/id->hash 321)))
    (should= 277076930200755 (sut/hash->id (sut/id->hash 277076930200755)))
    (should= 277076930200756 (sut/hash->id (sut/id->hash 277076930200756))))

  (it "hashid hard coded values to make sure they're the same in clj/cljs"
    (should= "BVyGj6x2" (sut/id->hash 1))
    (should= 1 (sut/hash->id "BVyGj6x2"))
    (should= "7ayR3Jzb" (sut/id->hash 42))
    (should= 42 (sut/hash->id "7ayR3Jzb"))
    ;; MDM - seems to break down at values much higher than below.  This is plenty big for datomic ids as fas as I've seen.
    (should= "jREgpG5GpaB" (sut/id->hash 999999999999999))
    (should= 999999999999999 (sut/hash->id "jREgpG5GpaB")))

  )

