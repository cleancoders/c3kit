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

(def server (app/resolution :ws/server))

(defn connected-ids [] (set (keys (:connections @@server))))

(def get-handler (delay #(wsc/handler @server %)))
(def post-handler (delay #(wsc/handler @server %)))

(defn- log-message [{:keys [kind connection-id]}]
  (log/trace (str "websocket call: " kind " client: " connection-id))
  {})

(defn pong [_] (apic/ok :pong))

(defn unhandled-message [{:keys [kind] :as msg}]
  (log/warn "Unhandled websocket event:" kind)
  (throw (ex-info (str "Unsupported websocket Call: " kind) msg)))

(defn default-on-connection-closed [{:keys [connection-id]}]
  (log/warn (str "UNHANDLED websocket connection closed: " connection-id))
  (apic/ok))

(defn close! [cid] #_(@send-fn cid [:chsk/close]))

(def default-handlers {:ws/open  'c3kit.wire.websocket/log-message
                       :ws/close 'c3kit.wire.websocket/default-on-connection-closed
                       :ws/ping  'c3kit.wire.websocket/pong
                       })

(def handler-cache (atom {}))

(defn resolve-handler [id]
  (if-let [app-actions (util/var-value (:ws-handlers @api/config))]
    (when-let [action-sym (or (get app-actions id) (get default-handlers id))]
      (util/resolve-var action-sym))
    (do (log/warn "app-handler-var has not been set")
        (when-let [action-sym (get default-handlers id)]
          (util/resolve-var action-sym)))))

(defn cached-resolve-handler [id]
  (if-let [action (get @handler-cache id)]
    action
    (when-let [action (resolve-handler id)]
      (swap! handler-cache assoc id action)
      action)))

(def handler-resolver (atom cached-resolve-handler))
(defn development! []
  (log/warn "websocket in development mode, not caching handlers!!!")
  (reset! handler-resolver resolve-handler))

(defn push! [client-id event params]
  (wsc/call! @server client-id event params)
  )

(defn dispatch-message [msg]
  (if-let [action (@handler-resolver (:kind msg))]
    (action msg)
    (unhandled-message msg)))

(defn wrap-log-message [handler]
  (fn [msg]
    (log/info "websocket event" (:kind msg) (:params msg))
    (let [result (handler msg)]
      (log/trace "websocket handler result: " result)
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
        (.printStackTrace e)
        (log/error e)
        (apic/error nil (.getMessage e))))))


(defn wrap-add-api-version [handler]
  (fn [msg]
    (let [result (handler msg)]
      (assoc result :version (api/version)))))

(def message-handler (-> dispatch-message
                         wrap-catch-errors
                         wrap-log-message
                         wrap-add-api-version
                         ))

(defn refresh-handler []
  (let [refresh! (util/resolve-var 'c3kit.apron.refresh/refresh!)]
    (fn [msg]
      (refresh!)
      (let [handler (util/resolve-var 'c3kit.wire.websocket/message-handler)]
        (handler msg)))))

(defn start [app]
  (log/report "Starting websocket")
  (when (app/development?) (development!))
  (let [handler (if (app/development?) (refresh-handler) message-handler)
        server (wsc/create handler)]
    (assoc app :ws/server server)))

(defn stop [app]
  (log/report "Stopping websocket")
  (dissoc app :ws/server))

(def service (app/service 'c3kit.wire.websocket/start 'c3kit.wire.websocket/stop))



