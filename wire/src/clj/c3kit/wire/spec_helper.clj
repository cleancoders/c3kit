(ns c3kit.wire.spec-helper
  (:require
  [c3kit.wire.apic :as apic]
  [c3kit.apron.corec :refer [->options]]
  [c3kit.wire.flashc :as flashc]
  [c3kit.apron.log :as log]
  [c3kit.apron.time :as time]
  [speclj.core :refer :all]
  ))

(log/warn!)

(defmacro stub-now [time]
  `(around [it#]
     (with-redefs [time/now (stub :now {:return ~time})]
       (it#))))

(defmacro should-redirect-to
          [response location]
  `(do
     (should= 302 (:status ~response))
     (should= ~location ((:headers ~response) "Location"))))

(defmacro should-ajax-redirect-to [response location]
  `(do
     (should= :redirect (-> ~response :body :status))
     (should= ~location (-> ~response :body :uri))))

(defmacro should-be-ajax-ok [response message]
  `(do
     (should= :ok (-> ~response :body :status))
     (should= ~message (-> ~response :body apic/first-flash-text))
     (should (-> ~response :body apic/first-flash flashc/success?))))

(defmacro should-be-ajax-fail [response message]
  `(do
     (should= :fail (-> ~response :body :status))
     (should= ~message (-> ~response :body apic/first-flash-text))
     (should (-> ~response :body apic/first-flash flashc/error?))))

(defmacro should-be-ws-fail [response message]
  `(do
     (should= :fail (-> ~response :status))
     (should= ~message (-> ~response apic/first-flash-text))
     (should (-> ~response apic/first-flash flashc/error?))))







