(ns c3kit.wire.ajax
  (:require
    [c3kit.apron.log :as log]
    [c3kit.apron.util :as util]
    [c3kit.apron.utilc :as utilc]
    [c3kit.wire.api :as api]
    [c3kit.wire.apic :as apic]
    [c3kit.wire.flash :as flash]
    [c3kit.wire.flashc :as flashc]
    [ring.util.response :as response]
    ))

(defn response [body] (response/response body))

(defn ok
  ([] (response (apic/ok)))
  ([payload] (response (apic/ok payload)))
  ([payload msg] (response (apic/ok payload msg))))

(defn fail
  ([] (response (apic/fail)))
  ([payload] (response (apic/fail payload)))
  ([payload msg] (response (apic/fail payload msg))))

(defn error
  ([] (response (apic/error)))
  ([payload] (response (apic/error payload)))
  ([payload msg] (response (apic/error payload msg))))

(defn redirect
  ([uri] (response (apic/redirect uri)))
  ([uri msg] (response (apic/redirect uri msg))))

(defn flash-success [response msg] (update response :body #(apic/flash-success % msg)))
(defn flash-warn [response msg] (update response :body #(apic/flash-warn % msg)))
(defn flash-error [response msg] (update response :body #(apic/flash-error % msg)))

(defn validation-errors-response [entity]
  (response (api/validation-errors-response entity)))

(defn maybe-validation-errors [entity]
  (when-let [r (api/maybe-validation-errors entity)]
    (response r)))

(defn payload [response] (-> response :body :payload))
(defn status [response] (-> response :body :status))
(defn flash [response] (-> response :body :flash))
(defn first-flash [response] (-> response :body :flash first))
(defn first-flash-text [response] (-> response first-flash flashc/text))

(defn api-not-found-handler [request] (fail (:uri request) (str "API not found: " (:uri request))))

(defn default-ajax-ex-handler [request ex]
  (log/error ex)
  ;(errors/send-error-email request e)
  (error nil "Our apologies. An error occurred and we have been notified."))

(defn wrap-catch-api-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (if-let [ex-handler (util/config-value (:ajax-on-ex @api/config))]
          (ex-handler request e)
          (default-ajax-ex-handler request e))))))

(defn wrap-transfer-flash-to-api [handler]
  (fn [request]
    (when-let [response (handler request)]
      (if-let [messages (and (map? (:body response)) (flash/messages response))]
        (-> response
            flash/clear-messages
            (assoc-in [:body :flash] messages))
        response))))

(defn wrap-api-transit-response [handler]
  (fn [request]
    (when-let [response (handler request)]
      (let [json-response (update response :body utilc/->transit)]
        (if (contains? (:headers response) "Content-Type")
          json-response
          (response/content-type json-response "application/transit+json; charset=utf-8"))))))

(defn wrap-add-api-version [handler]
  (fn [request]
    (when-let [response (handler request)]
      (assoc-in response [:body :version] (api/version)))))

(defn wrap-ajax [handler]
  (-> handler
      wrap-catch-api-errors
      ;wrap-transfer-flash-to-api
      wrap-add-api-version
      wrap-api-transit-response))




