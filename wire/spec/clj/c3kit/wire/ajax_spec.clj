(ns c3kit.wire.ajax-spec
  (:require
    [c3kit.wire.ajax :as ajax]
    [c3kit.wire.ajax :as sut]
    [c3kit.wire.api :as api]
    [c3kit.wire.apic :as apic]
    [c3kit.wire.flashc :as flashc]
    [speclj.core :refer :all]
    ))

(def user :undefined)
(def config :undefined)

(describe "Ajax"


  (context "middleware"

    (it "transfers flash messages"
      (let [handler (fn [r] (sut/ok :foo "Yipee!"))
            wrapped (sut/wrap-transfer-flash-to-api handler)
            response (wrapped {})]
        (should= nil (-> response :flash :messages))
        (should= "Yipee!" (-> response :body :flash first flashc/text))
        (should= true (-> response :body :flash first flashc/success?))))

    (it "adds version to response"
      (with-redefs [api/version (atom "123")]
        (let [handler #(ajax/ok %)
              wrapped (sut/wrap-add-api-version handler)
              response (wrapped :foo)]
          (should= "123" (-> response :body :version)))))

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
        (should= :ok (-> response ajax/status))
        (should= :bar (-> response ajax/payload))))

    (it "fail"
      (let [response (sut/fail :fuzz-balz "Oh Noez!")]
        (should= 200 (:status response))
        (should= :error (-> response :body :flash first flashc/level))
        (should= "Oh Noez!" (-> response :body :flash first flashc/text))
        (should= :fail (-> response ajax/status))
        (should= :fuzz-balz (-> response ajax/payload))))
    )
  )

