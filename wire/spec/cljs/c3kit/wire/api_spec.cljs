(ns c3kit.wire.api-spec
  (:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before with-stubs with around
                                        should-contain should-not-contain stub should-not-have-invoked should-have-invoked]])
  (:require
    [c3kit.wire.api :as sut]
    [c3kit.wire.flash :as flash]
    [c3kit.wire.flashc :as flashc]
    [c3kit.wire.js :as cc]
    [speclj.core]
    ))

(def handler :undefined)
(def call :undefined)

(describe "API"

  (with-stubs)

  (context "handling success responses"

    (with handler (stub :handler))
    (with call {:handler @handler :options {}})
    (around [it] (with-redefs [sut/new-version! (stub :new-version!)] (it)))

    (it "ok status invokes handler"
      (let [response {:status :ok :uri "/somewhere" :payload :payload}]
        (sut/handle-api-response response @call)
        (should-have-invoked :handler {:with [:payload]})))

    (it "fail status does NOT invokes handler"
      (let [response {:status :fail :uri "/somewhere"}]
        (sut/handle-api-response response @call)
        (should-not-have-invoked :handler)))

    (it "error status does NOT invokes handler"
      (let [response {:status :error :uri "/somewhere"}]
        (sut/handle-api-response response @call)
        (should-not-have-invoked :handler)))

    (it "flash"
      (let [flash-msg (flashc/warn "blah")
            response  {:status :ok :uri "/somewhere" :flash [flash-msg]}]
        (sut/handle-api-response response @call)
        (should-contain flash-msg @flash/state)))

    (it "redirect"
      (sut/configure! :version "a-version" :redirect-fn (stub :custom-redirect))
      (let [response {:status :redirect :uri "/somewhere"}]
        (sut/handle-api-response response @call)
        (should-not-have-invoked :handler)
        (should-have-invoked :custom-redirect {:with ["/somewhere"]})))

    (it "redirect - :no-redirect"
      (with-redefs [cc/redirect! (stub :goto!)]
        (let [response {:status :redirect :uri "/somewhere"}]
          (sut/handle-api-response response (assoc-in @call [:options :no-redirect] true))
          (should-not-have-invoked :handler)
          (should-not-have-invoked :goto! {:with ["/somewhere"]}))))

    (it ":after-all option"
      (let [response      {:status :ok :uri "/somewhere"}
            after-handler (stub :after)]
        (sut/handle-api-response response (assoc-in @call [:options :after-all] after-handler))
        (should-have-invoked :after)))

    (it "removed server-down flash"
      (flash/add! sut/server-down-flash)
      (sut/handle-api-response {:status :ok} @call)
      (should= false (flash/active? sut/server-down-flash)))

    (it "version not current"
      (sut/configure! :version "new")
      (sut/handle-api-response {:status :ok :version "old"} @call)
      (should-have-invoked :new-version!))

    (it "version current"
      (sut/configure! :version "new")
      (sut/handle-api-response {:status :ok :version "new"} @call)
      (should-not-have-invoked :new-version!)

      (sut/handle-api-response {:status :ok :version nil} @call)
      (should-not-have-invoked :new-version!))

    (it ":on-fail option"
      (let [call (assoc-in @call [:options :on-fail] (stub :on-fail))]
        (sut/handle-api-response {:status :ok} call)
        (should-not-have-invoked :on-fail)

        (sut/handle-api-response {:status :fail} call)
        (should-have-invoked :on-fail)))

    (it ":on-error option"
      (let [call (assoc-in @call [:options :on-error] (stub :on-error))]
        (sut/handle-api-response {:status :ok} call)
        (should-not-have-invoked :on-error)

        (sut/handle-api-response {:status :error} call)
        (should-have-invoked :on-error)))
    )

  )
