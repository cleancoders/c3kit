(ns c3kit.wire.ajax-spec
  (:require
    [c3kit.apron.log :as log]
    [c3kit.wire.ajax :as sut]
    [c3kit.wire.api :as api]
    [c3kit.wire.flashc :as flashc]
    [speclj.core :refer :all]
    ))

(def user :undefined)
(def config :undefined)

(describe "Ajax"


  (context "middleware"

    (it "transfers flash messages"
      (let [handler  (fn [r] (sut/ok :foo "Yipee!"))
            wrapped  (sut/wrap-transfer-flash-to-api handler)
            response (wrapped {})]
        (should= nil (-> response :flash :messages))
        (should= "Yipee!" (-> response :body :flash first flashc/text))
        (should= true (-> response :body :flash first flashc/success?))))

    (it "adds version to response"
      (api/configure! :version "123")
      (let [handler  #(sut/ok %)
            wrapped  (sut/wrap-add-api-version handler)
            response (wrapped :foo)]
        (should= "123" (-> response :body :version))))

    )

  (context "on-error"

    (with-stubs)

    (it "default"
      (api/configure! :ajax-on-ex nil)
      (with-redefs [c3kit.wire.ajax/default-ajax-ex-handler (stub :ex-handler)]
        (let [wrapped (sut/wrap-catch-api-errors (fn [r] (throw (Exception. "test"))))]
          (wrapped {:method :test})))
      (should-have-invoked :ex-handler))

    (it "default handler"
      (api/configure! :ajax-on-ex 'c3kit.wire.ajax/default-ajax-ex-handler)
      (log/capture-logs
        (let [wrapped  (sut/wrap-catch-api-errors (fn [r] (throw (Exception. "test"))))
              response (wrapped {:method :test})]
          (should= 200 (:status response))
          (should= :error (sut/status response))
          (should= "Our apologies. An error occurred and we have been notified." (-> response :body :flash first :text))
          (should= :error (-> response :body :flash first :level))))
      (should= "java.lang.Exception: test" (log/captured-logs-str)))

    (it "customer handler fn"
      (api/configure! :ajax-on-ex (stub :custom-ex-handler))
      (let [wrapped (sut/wrap-catch-api-errors (fn [r] (throw (Exception. "test"))))]
        (wrapped {:method :test}))
      (should-have-invoked :custom-ex-handler))

    )

  (context "helpers"

    (it "success"
      (let [response (sut/ok :foo)]
        (should= 200 (:status response))
        (should= 0 (-> response :flash :messages count))
        (should= {:status :ok :payload :foo} (:body response))))

    (it "success with flash"
      (let [response (sut/ok :bar "Cool beans!")]
        (should= 200 (:status response))
        (should= :success (-> response :body :flash first flashc/level))
        (should= "Cool beans!" (-> response :body :flash first flashc/text))
        (should= :ok (-> response sut/status))
        (should= :bar (-> response sut/payload))))

    (it "fail"
      (let [response (sut/fail :fuzz-balz "Oh Noez!")]
        (should= 200 (:status response))
        (should= :error (-> response :body :flash first flashc/level))
        (should= "Oh Noez!" (-> response :body :flash first flashc/text))
        (should= :fail (-> response sut/status))
        (should= :fuzz-balz (-> response sut/payload))))
    )

  (context "flash"

    (it "success"
      (let [response (sut/flash-success (sut/ok) "hello")
            flash (-> response :body :flash first)]
        (should= "hello" (flashc/text flash))
        (should= :success (flashc/level flash))))

    (it "warn"
      (let [response (sut/flash-warn (sut/ok) "hello")
            flash (-> response :body :flash first)]
        (should= "hello" (flashc/text flash))
        (should= :warn (flashc/level flash))))

    (it "error"
      (let [response (sut/flash-error (sut/ok) "hello")
            flash (-> response :body :flash first)]
        (should= "hello" (flashc/text flash))
        (should= :error (flashc/level flash))))

    )
  )

