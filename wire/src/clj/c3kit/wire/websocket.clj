(ns c3kit.wire.websocket
  (:import (clojure.lang ExceptionInfo))
  (:require
    [c3kit.apron.app :as app]
    [c3kit.apron.log :as log]
    [c3kit.apron.schema :as schema]
    [c3kit.apron.util :as util]
    [c3kit.wire.api :as api]
    [c3kit.wire.apic :as apic]
    [c3kit.wire.websocketc :as wsc]
    ))

(def nil-app-handlers {})
(def app-handlers-var (atom 'c3kit.wire.websocket/nil-app-handlers))
(defn set-app-handlers-var! [var-sym] (reset! app-handlers-var var-sym))

(def server (app/resolution :ws/server))

(defn connected-ids [] (set (keys (:connections @@server))))

(def get-handler (delay #(wsc/handler @server %)))
(def post-handler (delay #(wsc/handler @server %)))

(defn default-on-connection-closed [uid]
  (log/warn (str "UNHANDLED websocket connection closed: " uid))
  (apic/ok))

(def on-connection-closed-handler (app/resolution :ws/connection-closed-handler))

(defn- log-message [{:keys [kind connection-id]}]
  (log/trace (str "websocket call: " kind " client: " connection-id))
  {})

(defn pong [_] (apic/ok :pong))

(defn unhandled-message [{:keys [kind] :as msg}]
  (log/warn "Unhandled websocket event:" kind)
  (throw (ex-info (str "Unsupported websocket Call: " kind) msg)))

(defn dispatch-closed-connection [connection-id]
  (if-let [handler @on-connection-closed-handler]
    (handler connection-id)
    (default-on-connection-closed connection-id)))

(defn closed-connection-detected [{:keys [connection-id]}] (dispatch-closed-connection connection-id))

(defn close! [cid] #_(@send-fn cid [:chsk/close]))

;(defn client-closed-connection [{:keys [connection-id]}]
;  (close! connection-id)
;  (dispatch-closed-connection connection-id))

(def builtin-actions {:ws/open  'c3kit.wire.websocket/log-message
                      :ws/close 'c3kit.wire.websocket/closed-connection-detected
                      :ws/ping  'c3kit.wire.websocket/pong
                      ;:ws/close 'poker.websocket-custom/client-closed-connection
                      })

(def handler-cache (atom {}))

(defn resolve-handler [id]
  (let [app-actions @(util/resolve-var @app-handlers-var)]
    (when-let [action-sym (or (get builtin-actions id) (get app-actions id))]
      (util/resolve-var action-sym))))

(defn cached-resolve-handler [id]
  (if-let [action (get @handler-cache id)]
    action
    (when-let [action (resolve-handler id)]
      (swap! handler-cache assoc id action)
      action)))

(def handler-resolver (if (app/development?) resolve-handler cached-resolve-handler))

(defn push! [client-id event params]
  (wsc/call! @server client-id event params)
  )

(defn dispatch-message [msg]
  (if-let [action (handler-resolver (:kind msg))]
    (action msg)
    (unhandled-message msg)))

(defn wrap-log-message [handler]
  (fn [msg]
    (log/info "websocket event" (:kind msg) (:params msg))
    (let [result (handler msg)]
      ;(log/info "result: " result)
      result)))

(defn wrap-catch-errors [handler]
  (fn [msg]
    (try
      (handler msg)
      (catch ExceptionInfo exi
        (let [data (ex-data exi)]
          (cond
            (schema/error? data) (apic/error (schema/error-message-map data) (.getMessage exi))
            (:errors data) (apic/error (:errors data) (.getMessage exi))
            :else (apic/error nil (.getMessage exi)))))
      (catch Throwable e
        (log/error e)
        (apic/error nil (.getMessage e))))))


(defn wrap-add-api-version [handler]
  (fn [msg]
    (let [result (handler msg)]
      (assoc result :version @api/version))))

(def message-handler (-> dispatch-message
                         wrap-catch-errors
                         wrap-log-message
                         wrap-add-api-version
                         ))

(defn refresh-handler []
  (let [refresh! (util/resolve-var 'c3kit.wire.refresh/refresh!)]
    (fn [msg]
      (refresh!)
      (let [handler (util/resolve-var 'c3kit.wire.websocket/message-handler)]
        (handler msg)))))

(defn start [app]
  (log/info "Starting custom websocket")
  (let [handler (if (app/development?) (refresh-handler) message-handler)
        server (wsc/create handler)]
    (assoc app :ws/server server)))

(defn stop [app]
  (log/info "Stopping custom websocket")
  (dissoc app :ws/server))



