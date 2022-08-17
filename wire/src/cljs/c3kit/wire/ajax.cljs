(ns c3kit.wire.ajax
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.log :as log]
    [c3kit.wire.api :as api]
    [c3kit.wire.flash :as flash]
    [c3kit.wire.js :as cc]
    [cljs-http.client :as http]
    [cljs.core.async :as async]
    [reagent.core :as reagent]
    ))

(declare do-ajax-request)
(defn handle-server-down [ajax-call]
  (log/warn "Appears that server is down.  Will retry after in a moment.")
  (flash/add! api/server-down-flash)
  (cc/timeout 3000 #(do-ajax-request ajax-call)))

(defn handle-http-error [response ajax-call]
  (if-let [handler (:on-http-error (:options ajax-call))]
    (handler response)
    (log/error "Unexpected AJAX response: " response ajax-call)))

(def active-ajax-requests (reagent/atom 0))
(defn activity? [] (not= 0 @active-ajax-requests))

(defn server-down? [response]
  (and (= :http-error (:error-code response))
       (contains? #{0 502} (:status response))))

(defn triage-response [response ajax-call]
  (cond (server-down? response) (handle-server-down ajax-call)
        (= 200 (:status response)) (api/handle-api-response (:body response) ajax-call)
        :else (handle-http-error response ajax-call)))

(defn request-map [{:keys [options params method] :as ajax-call}]
  (let [;; TODO - MDM: This is sloppy. Need away to allow any http-client options to pass through.
        ;; TODO - MDM: We need a way to allow arbitrary authentication schemes to be applied here, not just CSRF.
        request (select-keys options [:headers :with-credentials?])
        request (if-let [csrf (:anti-forgery-token @api/config)] (assoc-in request [:headers "X-CSRF-Token"] csrf) request)]
    (if (:form-data? options)
      (let [form-data (js/FormData.)]
        (doseq [[k v] params]
          (.append form-data (name k) v))
        (assoc request :body form-data))
      (if (= "GET" method)
        (assoc request :query-params params)
        (assoc request :form-params params)))))

(defn- do-ajax-request [{:keys [method method-fn url params] :as ajax-call}]
  (log/debug "<" method url params)
  (go
    (swap! active-ajax-requests inc)
    (let [response (async/<! (method-fn url (request-map ajax-call)))]
      (log/debug ">" method url (:error-code response) (:status response) (:status (:body response)))
      (triage-response response ajax-call)
      (swap! active-ajax-requests dec))))

(defn build-ajax-call [method method-fn url params handler opt-args]
  {:options   (ccc/->options opt-args)
   :method    method
   :method-fn method-fn
   :url       url
   :params    params
   :handler   handler})

;; MDM - do-get and do-post
;; These functions initiate ajax calls to the server and conform to a semi-formal API.
;; Requests are simple: get or post to a URL with query params.
;; Responses from the server are a map described by the response-schema above.
;; Every call to do-get or do-post must include a handler function that takes one argument, the response :payload.  It
;; gets called when a response has :status of :ok. The :payload can be anything.  Client code should know what data
;; to expect based on what it's asking for.
;;
;; Options:    - extensible
;;  *** - see c3kit.wire.api for list of general API options
;;  :on-http-error  - (fn [response]...) invoked when the HTTP status code is unexpected

(defn get! [url params handler & opt-args]
  (do-ajax-request (build-ajax-call "GET" http/get url params handler opt-args)))

(defn post! [url params handler & opt-args]
  (do-ajax-request (build-ajax-call "POST" http/post url params handler opt-args)))

(defn save-destination [dest]
  (post! "/api/v1/save-destination" {:destination dest} #(log/info "destination saved: " %)))

