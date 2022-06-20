(ns c3kit.apron.util-spec
  (:import (java.io ByteArrayInputStream))
  (:require
    [c3kit.apron.util :as sut]
    [speclj.core :refer :all]
    [c3kit.apron.log :as log]))

(def foo "Foo")

(describe "util"

  (it "resolve-var"
    (should-throw (deref (sut/resolve-var 'foo/bar)))
    (should= "Foo" (deref (sut/resolve-var 'c3kit.apron.util-spec/foo))))

  (context "var-value"

    (around [it] (log/capture-logs (it)))

    (it "nil"
      (should= nil (sut/var-value nil))
      (should= "" (log/captured-logs-str)))

    (it "missing ns"
      (should= nil (sut/var-value 'foo/bar))
      (should= "Unable to resolve var: foo/bar java.io.FileNotFoundException: Could not locate foo__init.class, foo.clj or foo.cljc on classpath."
               (log/captured-logs-str)))

    (it "missing var"
      (should= nil (sut/var-value 'c3kit.apron.util-spec/bar))
      (should= "Unable to resolve var: c3kit.apron.util-spec/bar java.lang.Exception: No such var c3kit.apron.util-spec/bar"
               (log/captured-logs-str)))

    (it "success"
      (should= "Foo" (sut/var-value 'c3kit.apron.util-spec/foo))
      (should= "" (log/captured-logs-str)))

    )

  (context "config value"

    (it "nil"
      (should= nil (sut/config-value nil)))

    (it "value"
      (should= :foo (sut/config-value :foo)))

    (it "sym"
      (should= "Foo" (sut/config-value 'c3kit.apron.util-spec/foo)))

    )

  (it "md5"
    (should= "8622b9718771d75e07734684d6efa1dd" (sut/md5 "I'm a little teapot")))

  (it "stream->md5"
    (should= "8622b9718771d75e07734684d6efa1dd"
             (sut/stream->md5 (ByteArrayInputStream. (.getBytes "I'm a little teapot" "UTF-8")))))

  )
