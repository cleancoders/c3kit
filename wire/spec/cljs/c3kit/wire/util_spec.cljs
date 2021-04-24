(ns c3kit.wire.util-spec
  (:require-macros [speclj.core :refer [describe context it should= should-not= after before should-contain around with]])
  (:require
    [c3kit.wire.util :as sut]
    [speclj.core]
    ))

(describe "Util"

  (it "converts error map to strings"
    (should= ["email can't be blank"
              "confirm password must match"]
             (sut/errors->strings {:email "can't be blank" :confirm "password must match"})))

  (context "add-class-if"

    (it "changes nothing if condition is false"
      (should= {} (sut/+class-if {} false "foo"))
      (should= {:fizz "bang"} (sut/+class-if {:fizz "bang"} false "foo"))
      (should= {} (sut/+class-if nil false "foo"))
      (should= {} (sut/+class-if false "foo")))

    (it "adds first class"
      (should= {:class "one"} (sut/+class-if true "one"))
      (should= {:class "one" :fizz "bang"}
               (sut/+class-if {:fizz "bang"} true "one")))

    (it "adds additional class"
      (should= {:class "one two"} (sut/+class-if {:class "one"} true "two"))
      (should= {:class "one two" :fizz "bang"}
               (sut/+class-if {:class "one" :fizz "bang"} true "two")))

    )

  (context "uid"

    (it "are all unique"
      (should= 1000 (count (set (repeatedly 1000 sut/uid)))))

    (it "applies unique keys to list of nodes"
      (let [before (list [:a] [:b] [:c])
            after (sut/with-react-keys before)
            keys (map #(-> % meta :key) after)]
        ;(prn "keys: " keys)
        (should= 3 (count (set keys)))))

    (it "applies unique when strings are in list"
      (let [before (list "date")
            after (sut/with-react-keys before)
            keys (map #(-> % meta :key) after)]
        (should= [[:span "date"]] after)
        (should= [0] keys)))
    )

  (it "->css-class"
    (should= "" (sut/->css-class))
    (should= "foo" (sut/->css-class "foo"))
    (should= "foo bar" (sut/->css-class "foo" "bar"))
    (should= "foo bar" (sut/->css-class ["foo" "bar"])))

  )
