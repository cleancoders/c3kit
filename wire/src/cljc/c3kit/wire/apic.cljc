(ns c3kit.wire.apic
  (:require
    [c3kit.apron.corec :as corec]
    [c3kit.apron.log :as log]
    [c3kit.apron.schema :as schema]
    [c3kit.wire.flashc :as flashc]
    ))

(def response-schema
  {:status  {:type :keyword}
   :flash   {:type [:ignore]}
   :payload {:type :ignore}
   :uri     {:type :string}
   :version {:type :string}})

(defn conform-response [response]
  (let [response (schema/conform response-schema response)]
    (if (schema/error? response)
      (do (log/error "Failed to conform response.")
          (doseq [message (schema/messages response)]
            (log/error message))
          {:status :error})
      response)))

(defn flash-success [response msg] (update response :flash corec/conjv (flashc/success msg)))
(defn flash-warn [response msg] (update response :flash corec/conjv (flashc/warn msg)))
(defn flash-error [response msg] (update response :flash corec/conjv (flashc/error msg)))

(defn flash-level [response n] (flashc/level (nth (:flash response) n)))
(defn flash-text [response n] (flashc/text (nth (:flash response) n)))

(defn first-flash [response] (-> response :flash first))
(defn first-flash-text [response] (-> response first-flash flashc/text))

(defn ok
  "The request was processed without a hitch."
  ([] {:status :ok})
  ([payload] {:payload payload :status :ok})
  ([payload msg] (flash-success (ok payload) msg)))

(defn fail
  "The request failed for anticipated reasons."
  ([] {:status :fail})
  ([payload] {:payload payload :status :fail})
  ([payload msg] (flash-error (fail payload) msg)))

(defn error
  "An unexpected exception was thrown while processing the request."
  ([] {:status :error})
  ([payload] {:payload payload :status :error})
  ([payload msg] (flash-error (error payload) msg)))

(defn redirect
  ([uri] {:status :redirect :uri uri})
  ([uri msg] (flash-warn (redirect uri) msg)))

(defn status [response] (:status response))
(defn error? [response] (= :error (:status response)))
(defn ok? [response] (= :ok (:status response)))
(defn fail? [response] (= :fail (:status response)))
(defn redirect? [response] (= :redirect (:status response)))
(defn payload [response] (:payload response))


