(ns c3kit.wire.spec-helperc
  #?(:cljs (:require-macros [speclj.core :refer [-fail -to-s around]]))
  (:require
    #?(:clj  [speclj.core :refer :all]
       :cljs [speclj.core])
    ))

#?(:clj (defmacro stub-now [time]
          `(around [it#]
                   (with-redefs [c3kit.apron.time/now (stub :now {:return ~time})]
                     (it#)))))

#?(:clj (defmacro should-select
                  "Asserts the selector finds a node"
          [selector]
          `(let [value# ~selector
                 node# (c3kit.wire.spec-helper/select value#)]
             (when-not node#
               (-fail (str "Expected selector to find node: " (-to-s value#)))))))

#?(:clj (defmacro should-not-select
                  "Asserts the selector does not find a node"
          [selector]
          `(let [value# ~selector
                 node# (c3kit.wire.spec-helper/select value#)]
             (when node#
               (-fail (str "Expected selector NOT to find node: " (-to-s value#)))))))

#?(:clj (defmacro should-have-invoked-webs
                  "Asserts the invocation of ws/call!"
          ([id] `(should= ~id (c3kit.wire.spec-helper/last-ws-call-id)))
          ([id params]
           `(do (should= ~id (c3kit.wire.spec-helper/last-ws-call-id))
                (should= ~params (c3kit.wire.spec-helper/last-ws-call-params))))
          ([id params handler]
           `(do (should= ~id (c3kit.wire.spec-helper/last-ws-call-id))
                (should= ~params (c3kit.wire.spec-helper/last-ws-call-params))
                (should= ~handler (c3kit.wire.spec-helper/last-ws-call-handler))))))
