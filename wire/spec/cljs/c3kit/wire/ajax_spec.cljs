(ns c3kit.wire.ajax-spec
  (:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before with-stubs with around
                                        should-contain should-not-contain stub should-not-have-invoked should-have-invoked]])
  (:require
    [c3kit.apron.log :as log]
    [c3kit.wire.ajax :as sut]
    [c3kit.wire.api :as api]
    [c3kit.wire.js :as cc]
    [c3kit.wire.flash :as flash]
    [c3kit.wire.spec-helper :as helper]
    [speclj.core]
    ))

(def handler :undefined)

(describe "AJAX"

  (with-stubs)

  (context "response"

    (it "server-down?"
      (should= false (sut/server-down? {:status     200
                                        :success    true
                                        :error-code :no-error
                                        :error-text ""}))
      (should= true (sut/server-down? {:status     0
                                       :success    false
                                       :error-code :http-error
                                       :error-text " [0]"}))
      (should= true (sut/server-down? {:status     502
                                       :success    false
                                       :error-code :http-error
                                       :error-text "Bad Gateway [502]"}))
      )

    )

  (context "triage-response"

    (around [it]
      (with-redefs [api/handle-api-response (stub :handle-api-response)
                    sut/handle-server-down (stub :handle-server-down)
                    sut/handle-unknown (stub :handle-unknown)]
        (it)))

    (it "success"
      (sut/triage-response {:error-code :no-error :status 200} {})
      (should-have-invoked :handle-api-response))

    (it "server-down"
      (sut/triage-response {:error-code :http-error :status 0} {})
      (should-have-invoked :handle-server-down))

    (it "unknown"
      (sut/triage-response {:error-code :no-error :status 123} {})
      (should-have-invoked :handle-unknown))

    )

  (context "handle server-down"

    (around [it]
      (with-redefs [cc/timeout (stub :timeout)]
        (log/capture-logs
          (it))))

    (it "flash"
      (should= true (:persist api/server-down-flash))
      (should= false (flash/active? api/server-down-flash))
      (sut/handle-server-down {})
      (should= true (flash/active? api/server-down-flash))
      )

    (it "timeout"
      (sut/handle-server-down {})
      (should-have-invoked :timeout)) ; presumably to re-invoke the api call
    )

  (context "requests"

    (helper/stub-ajax)

    (it "save-destination"
      (sut/save-destination "/foo")
      (should-have-invoked :ajax/post! {:with ["/api/v1/save-destination" {:destination "/foo"} :*]}))

    )

  )
