(ns c3kit.apron.utilc-spec
  (:require
    [c3kit.apron.utilc :as sut]
    [c3kit.apron.corec :as ccc]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it should= should-contain should-throw]]))

(describe "Util common"


  (context "end"

    (it "->edn"
      (should= "[1 2 3]" (sut/->edn [1 2 3])))

    (it "<-edn"
      (should= [1 2 3] (sut/<-edn "[1 2 3]")))
    )

  (context "map manipulation"

    (it "keywordize kind"
      (should= {:kind :foo :val 1} (sut/keywordize-kind {:kind "foo" :val 1}))
      (should= {:kind :foo :val 1} (sut/keywordize-kind {:kind :foo :val 1}))
      (should-throw (sut/keywordize-kind {:missing "kind"}))
      (should-throw (sut/keywordize-kind {:kind 123})))

    (it "index-by-id"
      (let [a {:id 123 :name "a"}
            b {:id 456 :name "b"}]
        (should= {123 a 456 b} (sut/index-by-id [b a]))))
    )

  (context "transit"

    (it "uuid"
      (let [uuid         (ccc/new-uuid)
            uuid-transit (sut/->transit uuid)]
        (should= uuid (sut/<-transit uuid-transit))))

    (it "uuid in map"
      ;#uuid "53060bf1-971a-4d18-80fc-92a3112afd6e"
      (let [uuid (sut/->uuid-or-nil "53060bf1-971a-4d18-80fc-92a3112afd6e")
            data {:uuid uuid}
            trs  (sut/->transit data)]
        (should= data (sut/<-transit trs))))
    )

  (context "json"

    (it "->json"
      (should= "{\"a\":123,\"b\":\"hello\",\"c\":[1,2,3],\"d\":{\"e\":\"f\"},\"g\":321}"
               (sut/->json {:a  123
                            :b  "hello"
                            :c  [1 2 3]
                            :d  {:e "f"}
                            "g" 321})))

    (it "<-json"
      (should= {"a"  123
                "b"  "hello"
                "c"  [1 2 3]
                "d"  {"e" "f"}
                "g" 321}
               (sut/<-json "{\"a\":123,\"b\":\"hello\",\"c\":[1,2,3],\"d\":{\"e\":\"f\"},\"g\":321}")))

    (it "<-json: keyword keys"
      (should= {:a  123
                :b  "hello"
                :c  [1 2 3]
                :d  {:e "f"}
                :g 321}
               (sut/<-json-kw "{\"a\":123,\"b\":\"hello\",\"c\":[1,2,3],\"d\":{\"e\":\"f\"},\"g\":321}")))
    )

  (context "csv"

    (it "no rows"
      (should= "" (sut/->csv [])))

    (it "with rows"
      (should= (str "A,B,C\r\n"
                    "1,2,3\r\n"
                    "a,b,c")
               (sut/->csv [["A" "B" "C"]
                           [1 2 3]
                           ['a 'b 'c]])))

    (it "with comma in value"
      (should= (str "A,B\r\n"
                    "\"a, ok\",A-OK")
               (sut/->csv [["A" "B"]
                           ["a, ok" "A-OK"]])))

    (it "with \" in value"
      (should= (str "A,B\r\n"
                    "\"\"\"a\"\"\",\"\"\"b\"")
               (sut/->csv [["A" "B"]
                           ["\"a\"" "\"b"]])))
    )

  (it "->filename"
    (should= "foo" (sut/->filename "foo"))
    (should= "foo_bar" (sut/->filename "foo bar"))
    (should= "foos_bar" (sut/->filename "foo's bar"))
    (should= "Mr_foo" (sut/->filename "Mr. foo"))
    (should= "foo_bar" (sut/->filename "foo-bar"))
    (should= "foo" (sut/->filename "foo/\\<>:\"|?*[]"))
    (should= "foo.bar" (sut/->filename "foo" "bar")))

  )
