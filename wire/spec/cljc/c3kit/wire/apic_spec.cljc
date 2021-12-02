(ns c3kit.wire.apic-spec
  (:require
    [c3kit.wire.apic :as sut]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it should= should-contain should-throw
                                                      should should-not]]))

(describe "API Common"

  (context "schema"

    (it "status is keyword"
      (let [response (sut/conform-response {:status "foo"})]
        (should= :foo (:status response))))

    (it "fields"
      (should-contain :status sut/response-schema)
      (should-contain :flash sut/response-schema)
      (should-contain :payload sut/response-schema)
      (should-contain :uri sut/response-schema))

    (it "ok"
      (should= {:status :ok} (sut/ok))
      (should= {:status :ok :payload :foo} (sut/ok :foo)))

    (it "ok with flash"
      (let [response (sut/ok :bar "Yeah!")]
        (should= {:status :ok :payload :bar} (dissoc response :flash))
        (should= :success (sut/flash-level response 0))
        (should= "Yeah!" (sut/flash-text response 0))))

    (it "fail"
      (should= {:status :fail} (sut/fail))
      (should= {:status :fail :payload :foo} (sut/fail :foo)))

    (it "fail with flash"
      (let [response (sut/fail :bar "No!")]
        (should= {:status :fail :payload :bar} (dissoc response :flash))
        (should= :error (sut/flash-level response 0))
        (should= "No!" (sut/flash-text response 0))))

    (it "error"
      (should= {:status :error} (sut/error))
      (should= {:status :error :payload :foo} (sut/error :foo)))

    (it "error with flash"
      (let [response (sut/error :bar "No!")]
        (should= {:status :error :payload :bar} (dissoc response :flash))
        (should= :error (sut/flash-level response 0))
        (should= "No!" (sut/flash-text response 0))))

    (it "redirect"
      (should= {:status :redirect :uri "/path"} (sut/redirect "/path")))

    (it "redirect with flash"
      (let [response (sut/redirect "/path" "No!")]
        (should= {:status :redirect :uri "/path"} (dissoc response :flash))
        (should= :warn (sut/flash-level response 0))
        (should= "No!" (sut/flash-text response 0))))

    (it "error?"
      (should (sut/error? (sut/error)))
      (should-not (sut/error? (sut/fail)))
      (should-not (sut/error? (sut/ok)))
      (should-not (sut/error? (sut/redirect "/path"))))

    (it "fail?"
      (should (sut/fail? (sut/fail)))
      (should-not (sut/fail? (sut/error)))
      (should-not (sut/fail? (sut/ok)))
      (should-not (sut/fail? (sut/redirect "/path"))))

    (it "ok?"
      (should (sut/ok? (sut/ok)))
      (should-not (sut/ok? (sut/fail)))
      (should-not (sut/ok? (sut/error)))
      (should-not (sut/ok? (sut/redirect "/path"))))

    (it "redirect?"
      (should (sut/redirect? (sut/redirect "/path")))
      (should-not (sut/redirect? (sut/fail)))
      (should-not (sut/redirect? (sut/ok)))
      (should-not (sut/redirect? (sut/error ))))

    )

  )
