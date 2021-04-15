(ns c3kit.apron.util-spec
  (:import (java.io ByteArrayInputStream))
  (:require
    [c3kit.apron.util :as sut]
    [speclj.core :refer :all]))

(def foo "Foo")

(describe "util"

  (it "resolve-var"
    (should= "Foo" (deref (sut/resolve-var 'c3kit.apron.util-spec/foo))))

  (it "md5"
    (should= "8622b9718771d75e07734684d6efa1dd" (sut/md5 "I'm a little teapot")))

  (it "stream->md5"
    (should= "8622b9718771d75e07734684d6efa1dd"
             (sut/stream->md5 (ByteArrayInputStream. (.getBytes "I'm a little teapot" "UTF-8")))))

  (it "->edn"
    (should= "[1 2 3]" (sut/->edn [1 2 3])))

  (it "<-edn"
    (should= [1 2 3] (sut/<-edn "[1 2 3]")))

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
