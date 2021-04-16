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

  )
