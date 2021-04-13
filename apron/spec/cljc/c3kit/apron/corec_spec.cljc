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

  (it "keywordize kind"
    (should= {:kind :foo :val 1} (ccc/keywordize-kind {:kind "foo" :val 1}))
    (should= {:kind :foo :val 1} (ccc/keywordize-kind {:kind :foo :val 1}))
    (should-throw (ccc/keywordize-kind {:missing "kind"}))
    (should-throw (ccc/keywordize-kind {:kind 123})))

  (context "->options"

    (it "nil -> {}"
      (should= {} (ccc/->options [nil]))
      )

    )

  )
