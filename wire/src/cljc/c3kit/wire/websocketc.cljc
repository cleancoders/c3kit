(ns c3kit.wire.websocketc
  #?(:clj (:import (java.util.concurrent Executors ScheduledExecutorService TimeUnit Future)))
  (:require
    [c3kit.apron.log :as log]
    #?(:clj [org.httpkit.server :as httpkit])
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.utilc :as util]
    [c3kit.apron.cursor :refer [cursor]]
    [c3kit.apron.time :as time :refer [seconds ago]]))

(defn request
  ([id kind] {:request-id id :kind kind})
  ([id kind params] (assoc (request id kind) :params params))
  ([id kind params reply?] (if reply? (assoc (request id kind params) :reply? true) (request id kind params))))

(defn response [id data] {:response-id id :payload data})

(defn request? [message] (and (map? message) (contains? message :request-id)))
(defn response? [message] (and (map? message) (contains? message :response-id)))

(defn connection
  "Returns a map holding connection data.  A connection will contain, but is not limited to, these keys:

  :id               - a unique string id for the connection
  :socket           - network communication channel
  :request-counter  - to generate next request id in sequence for this connection
  :open?            - boolean
  :responders       - (maybe) map of request-id -> (fn [payload]) to handle responses
  :last-active-at   - time of last send or receive of data"
  [id socket] {:id              id
               :socket          socket
               :request-counter 0
               :open?           false})


(defn unpack [data] (try (util/<-edn data) (catch #?(:clj Exception :cljs :default) _ data)))
(defn pack [message] (util/->edn message))

(declare send-to!)
(declare call!)
(declare ^:private connections)
(declare -close-socket!)

(defn- handle-request [server connection message]
  (if-let [handler (:message-handler @server)]
    (let [handler-msg (assoc message :connection-id (:id @connection))
          handler-msg (if-let [request (:request @connection)] (assoc handler-msg :request request) handler-msg)
          result (handler handler-msg)]
      (when (:reply? handler-msg)
        (let [reply (response (:request-id handler-msg) result)]
          (send-to! server connection (:socket @connection) reply))))
    (log/warn "websocket MISSING :message-handler!")))

(defn- send-internal-message [server connection kind payload]
  (handle-request server connection (request nil kind payload)))

(defn connection-responder! [conn-atom id]
  ;; swap-val! doesn't work in cljs.  Not a problem since cljs is single threaded, but we need to make difference calls.
  #?(:clj  (let [[old _] (swap-vals! conn-atom update :responders dissoc id)]
             (get-in old [:responders id]))
     :cljs (let [old (get-in @conn-atom [:responders id])]
             (swap! conn-atom update :responders dissoc id)
             old)))

(defn -cancel-timeout! [timeout] #?(:clj (.cancel timeout false) :cljs (js/clearTimeout timeout)))

(defn -activity!
  ([connection] (-activity! connection (time/now)))
  ([connection moment] (swap! connection assoc :last-active-at moment)))

(defn -activity-since? [connection moment]
  (if-let [activity (:last-active-at @connection)]
    (time/before? moment activity)
    false))

(defn -handle-response [connection response]
  (if-let [responder (connection-responder! connection (:response-id response))]
    (let [[responder-fn timeout] responder]
      (when timeout (-cancel-timeout! timeout))
      (responder-fn (:payload response)))
    (log/warn "websocket connection MISSING responder:" (:response-id response))))

(defn- handle-data [server data]
  (if-let [data-handler (:on-data @server)]
    (data-handler data)
    (log/warn "websocket UNHANDLED data received:" data)))

(defn- handle-message [server connection data]
  (if @connection
    (let [message (unpack data)]
      (-activity! connection)
      (cond (request? message) (handle-request server connection message)
            (response? message) (-handle-response connection message)
            :else (handle-data server message)))
    (log/warn "websocket MISSING connection:" (pr-str connection))))

(defn -timeout! [server connection request-id]
  (when (connection-responder! connection request-id)
    (log/debug "websocket TIMEOUT: " request-id)
    (send-internal-message server connection :ws/timeout request-id)))

(defn -create-timeout! [server connection request-id timeout-millis]
  #?(:clj  (let [^ScheduledExecutorService scheduler (:scheduler @server)]
             (.schedule scheduler #(-timeout! server connection request-id) timeout-millis TimeUnit/MILLISECONDS))
     :cljs (js/setTimeout #(-timeout! server connection request-id) timeout-millis)))

(defn connection-request! [server conn-atom kind params responder]
  (let [connection (swap! conn-atom (fn [conn]
                                      (let [request-counter (inc (:request-counter conn 0))
                                            conn (assoc conn :request-counter request-counter)]
                                        (if responder
                                          (let [timeout (when-let [timeout-millis (:request-timeout @server)]
                                                          (-create-timeout! server conn request-counter timeout-millis))]
                                            (update conn :responders assoc request-counter [responder timeout]))
                                          conn))))]
    (request (:request-counter connection) kind params (some? responder))))

(defn -do-call! [state connection kind params handler options]
  (let [options (when options (ccc/->options options))]
    (if (:open? @connection)
      (let [message (connection-request! state connection kind params handler)
            socket (:socket @connection)]
        (-activity! connection)
        (if socket
          (send-to! state connection socket message)
          (do (log/warn "websocket send! with missing socket: " message) false)))
      false)))

(defn -ping! [state connection] (-do-call! state connection :ws/ping nil nil nil))

(defn -ping-inactive-connections! [state]
  (when-let [interval (:ping-interval @state)]
    (let [moment (-> interval seconds ago)]
      (doseq [connection (connections state)]
        (when (and @connection (:open? @connection) (not (-activity-since? connection moment)))
          (-ping! state connection))))))

(defn- connection-closed! [server connection]
  (when (:open? @connection)
    (send-internal-message server connection :ws/close nil)
    #?(:clj  (swap! server update :connections dissoc (:id @connection))
       :cljs (swap! connection assoc :open? false))))

#?(:clj
   (do

     (defn -connection-cursor [server connection-id] (cursor server [:connections connection-id]))
     (defn- connections [server] (map (fn [[id _]] (-connection-cursor server id)) (:connections @server)))

     (defn- handle-failed-send! [server connection-id]
       (log/warn "websocket send! FAILED. Connection closed? removing:")
       (connection-closed! server (-connection-cursor server connection-id))
       false)

     (defn- send-to! [server connection socket message]
       (let [connection-id (:id @connection)]
         (if (httpkit/send! socket (pack message))
           true
           (handle-failed-send! server connection-id))))

     (defn- maybe-invalid-csrf-token [request]
       (let [expected (:session/key request)
             actual (-> request :params :csrf-token)]
         (when (or (nil? actual) (not= expected actual))
           {:status 403 :body "Invalid anti-forgery token"})))

     (defn- maybe-invalid-connection-id [connection-id] (when-not connection-id {:status 403 :body "Invalid connection id"}))

     ;(defn -channel-init [server connection-id ch] (log/debug "websocket channel init:" connection-id))
     ;(defn -channel-on-ping [server connection-id ch data] (log/debug "websocket channel on-ping:" connection-id data))

     (defn -data-received [server connection-id socket data]
       (log/debug "websocket channel on-receive:" connection-id data)
       (handle-message server (-connection-cursor server connection-id) data))

     (defn -channel-on-close [server connection-id _ status]
       (log/debug "websocket channel on-close:" connection-id status)
       (connection-closed! server (-connection-cursor server connection-id)))

     (defn -open-connection! [server request connection-id channel]
       (log/debug "websocket channel on-open:" connection-id)
       (let [connection (assoc (connection connection-id channel) :request request :open? true)]
         (swap! server assoc-in [:connections connection-id] connection))
       (send-internal-message server (-connection-cursor server connection-id) :ws/open nil)
       (call! server connection-id :ws/hello))

     (defn -new-scheduler [] (Executors/newScheduledThreadPool 1))

     (defn -schedule-with-delay [scheduler state interval]
       (.scheduleWithFixedDelay scheduler #(-ping-inactive-connections! state) interval interval TimeUnit/SECONDS))

     (defn- add-ping-task! [state interval]
       (let [scheduler (:scheduler @state)
             ping-task (-schedule-with-delay scheduler state interval)]
         (swap! state assoc :ping-task ping-task)))
     )

   ;; ------------------------------------------------------------------------------------------------------------------
   :cljs
   (do

     (defn -connection-uri
       ([path connection-id csrf-token] (-connection-uri (.-location js/window) path connection-id csrf-token))
       ([location path connection-id csrf-token]
        (let [protocol (if (= "https:" (.-protocol location)) "wss:" "ws:")
              host (.-host location)]
          (str protocol "//" host path "?connection-id=" connection-id "&csrf-token=" csrf-token))))

     (defn -connection-cursor [state] (cursor state [:connection]))
     (defn- connections [state] [(-connection-cursor state)])

     (defn -socket-send! [socket data] (.send socket data) true)

     (defn- send-to! [_ connection socket message] (-socket-send! socket (pack message)))

     (defn -data-received [client e]
       (let [data (.-data e)]
         (handle-message client (-connection-cursor client) data)))

     (defn -handle-open [client _]
       (log/debug "websocket opened")
       (swap! client assoc-in [:connection :open?] true)
       (send-internal-message client (-connection-cursor client) :ws/open nil))

     (defn -handle-close [client _]
       (log/debug "websocket close:")
       (swap! client assoc-in [:connection :open?] false)
       (send-internal-message client (-connection-cursor client) :ws/close nil))

     (defn -handle-error [client e]
       (log/debug "websocket error:")
       (send-internal-message client (-connection-cursor client) :ws/error (js->clj e :keywordize-keys true)))

     (defn -add-connection! [client path csrf-token cid socket]
       (swap! client assoc :connection (connection cid socket) :path path :csrf-token csrf-token))

     (defn- -ping-inactive-connections-and-set-timeout! [state interval]
       (-ping-inactive-connections! state)
       (js/setTimeout #(-ping-inactive-connections-and-set-timeout! state interval) interval))

     (defn- add-ping-task! [state interval]
       (let [task #(-ping-inactive-connections-and-set-timeout! state interval)]
         (swap! state assoc :ping-task (js/setTimeout task interval))))

     )
   )

(def ^:private default-options
  {:on-data         nil
   :request-timeout 5000
   :ping-interval   30})

(defn create
  "Returns a atom to hold all the state and configuration to run a websocket client or server.

  message-handler - (fn [REQUEST]) to handle incoming RPC requests.

  REQUEST:
  :request-id    - each connection has it's own sequence starting at 1
  :kind          - used to dispatch behavior (see BUILTIN KINDS below)
  :params        - any clj form (data) needed to support the :kind of request
  :reply?        - true iff a response is expected
  :connection-id - uniquely identifying the connection to the server
  :request       - (server only) the ring request that initiated the websocket connection

  BUILTIN KINDS (values for :kind key in requests)
  :ws/open       - a connection is opened {:params nil}
  :ws/close      - a connection is closed {:params nil}
  :ws/hello      - sent from server to client when a connection is opened {:params nil}
  :ws/error      - an error occurred {:params error-map}
  :ws/timeout    - a request with {:reply? true} did not receive a response within :request-timeout milliseconds

  RESPONSE:
  :response-id   - id corresponding to originating request
  :payload       - any form

  OPTIONS - a map and/or key/value pairs:
  :on-data         	- (fn [data]) when incoming data fails to parse (edn) or satisfy request? or response?
  :request-timeout 	- (default: 5000) milliseconds to wait before cancelling request.  nil -> never timeout.
  :atom-fn      		- (default: core/atom) type of atom to story the state. Maybe reagent/atom.
  :ping-interval	 	- (default: 30) seconds between keep-alive pings on inactive connections. nil -> no pings
  "
  [message-handler & args]
  (let [options (ccc/->options args)
        atom-fn (:atom-fn options atom)
        server (merge default-options
                      (select-keys options (keys default-options))
                      #?(:clj  {:connections {}
                                :scheduler   (-new-scheduler)}
                         :cljs {:connection nil})
                      {:message-handler message-handler})
        state-atom (atom-fn server)]
    (when-let [interval (:ping-interval @state-atom)]
      (add-ping-task! state-atom interval))
    state-atom))

#?(:clj
   (defn handler
     "Ring handler to open websocket connections in the specified server state atom.

     server  - server state atom (from create)
     request - ring request"
     [server request]
     (let [connection-id (-> request :params :connection-id)]
       (or (maybe-invalid-csrf-token request)
           (maybe-invalid-connection-id connection-id)
           (httpkit/as-channel request {:on-receive (partial -data-received server connection-id)
                                        :on-close   (partial -channel-on-close server connection-id)
                                        :on-open    (partial -open-connection! server request connection-id)
                                        ;; Maybe delete the two below.  They don't seem used.
                                        ;:init       (partial -channel-init server connection-id)
                                        ;:on-ping    (partial -channel-on-ping server connection-id)
                                        }))))
   :cljs
   (defn connect!
     "Open a websocket connection to the server.

     client     - client state atom
     path       - URI path to the websocket handler.  The protocol and host are determined by the window location.
     csrf-token - required for security.  Default strategy is session/key from server."
     [client path csrf-token]
     (log/debug "websocket connect!")
     (let [connection-id (str (ccc/new-uuid))
           uri (-connection-uri path connection-id csrf-token)
           socket (new js/WebSocket uri)]
       (-add-connection! client path csrf-token connection-id socket)
       (.addEventListener socket "open" (partial -handle-open client))
       (.addEventListener socket "message" (partial -data-received client))
       (.addEventListener socket "close" (partial -handle-close client))
       (.addEventListener socket "error" (partial -handle-error client)))))

(defn call!
  "Make a websocket RPC.

  state         - client or sever state (from create fn)
  connection-id - (server only) to select the client connection you want to call
  kind          - used to dispatch behavior, typically a keyword
  params        - (optional) any clj form (data) needed to support the :kind of request
  handler       - (optional) (fn [RESPONSE]) invoked when a response to the request is received (reply? implied true)

  OPTIONS - a map and/or key/value pairs:
  "
  #?(:clj  ([state connection-id kind] (call! state connection-id kind nil nil))
     :cljs ([state kind] (call! state kind nil nil)))
  #?(:clj  ([state connection-id kind params] (call! state connection-id kind params nil))
     :cljs ([state kind params] (call! state kind params nil)))
  #?(:clj  ([state connection-id kind params handler & options]
            (-do-call! state (-connection-cursor state connection-id) kind params handler options))
     :cljs ([state kind params handler & options]
            (-do-call! state (-connection-cursor state) kind params handler options))))

#?(:clj  (defn open? [server connection-id] (get-in @server [:connections connection-id :open?]))
   :cljs (defn open? [client] (get-in @client [:connection :open?])))

#?(:clj
   (defn close!
     "Close the connection with connection-id"
     [state connection-id]
     (if-let [socket (get-in @state [:connections connection-id :socket])]
       (httpkit/close socket)
       (log/warn "websocket: attempt to close missing socket: " connection-id)))
   :cljs
   (defn close!
     "Close the connection.
     code   (optional) https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent#status_codes
     reason (optional) A human-readable string explaining why the connection is closing"
     ([state] (.close (get-in @state [:connection :socket])))
     ([state code-or-reason] (.close (get-in @state [:connection :socket]) code-or-reason))
     ([state code reason] (.close (get-in @state [:connection :socket]) code reason))))
