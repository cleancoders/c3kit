(ns c3kit.core-spec
  (:require [speclj.core :refer :all]
            [c3kit.core :as sut]))


(describe "c3kit core"

          (it "foo"
              (should= :foo (sut/foo)))
          )
