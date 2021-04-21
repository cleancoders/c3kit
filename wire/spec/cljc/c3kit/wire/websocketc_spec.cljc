(ns c3kit.wire.websocketc-spec
  #?(:clj (:import (java.util.concurrent ScheduledExecutorService Future)))
  (:require
    [speclj.core #?(:clj :refer :cljs :refer-macros)
     [context describe it should= should-contain should-not-contain should-throw should with-stubs stub with
      before should-have-invoked should-not-have-invoked should-not-throw around should-not= should-be-a]]
    #?(:clj [org.httpkit.server :as httpkit])
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.log :as log #?(:clj :refer :cljs :refer-macros) [capture-logs]]
    [c3kit.apron.time :as time :refer [seconds ago]]
    [c3kit.wire.spec-helperc #?(:clj :refer :cljs :refer-macros) [stub-now]]
    [c3kit.wire.websocketc :as sut]
    [speclj.stub :as stub]
    [c3kit.apron.utilc :as utilc]))

(def state :undefined-state)
(def request :undefined-request)
(def now :undefined-now)
(def message-handler-result (atom nil))
(def send!-result (atom true))

(defn pings []
  (let [sends (stub/invocations-of :socket/send!)
        messages (map #(sut/unpack (second %)) sends)]
    (count (filter #(= :ws/ping (:kind %)) messages))))

(defn cursor-hack [state id]
  ;; MDM - Cursive (IDE) complains about (sut/-connection-cursor state #?(:clj id))
  #?(:clj  (sut/-connection-cursor state id)
     :cljs (sut/-connection-cursor state)))

(describe "Websocket common"

  (with-stubs)
  (with now (time/now))
  (stub-now @now)
  (with state (sut/create (stub :message-handler {:invoke (fn [_] @message-handler-result)})))
  #?(:clj (with request {:params {:connection-id "client-123" :csrf-token "csrf-blah"} :session/key "csrf-blah"}))
  (around [it]
    (with-redefs [sut/-create-timeout! (stub :sut/create-timeout {:return :fake-timeout})
                  sut/-cancel-timeout! (stub :sut/cancel-timeout)
                  #?(:clj httpkit/send! :cljs sut/-socket-send!) (stub :socket/send! {:invoke (fn [& _] @send!-result)})
                  #?(:cljs js/setTimeout) #?(:cljs (stub :js/setTimeout {:return :fake-timeout}))
                  #?(:clj sut/-schedule-with-delay) #?(:clj (stub :-schedule-with-delay {:return :fake-delay-task}))
                  #?(:clj sut/-new-scheduler) #?(:clj (stub :-new-scheduler {:return :fake-scheduler}))]
      (capture-logs
        (it))))

  (context "message anatomy"

    (it "minimal request"
      (should= {:request-id 123 :kind :foo}
               (sut/request 123 :foo)))

    (it "request with params"
      (should= {:request-id 123 :kind :foo :params [:stuff]}
               (sut/request 123 :foo [:stuff])))

    (it "request with params"
      (should= {:request-id 123 :kind :foo :params [:stuff]}
               (sut/request 123 :foo [:stuff])))

    (it "request needing response"
      (should= {:request-id 123 :kind :foo :params [:stuff] :reply? true}
               (sut/request 123 :foo [:stuff] true)))

    (it "minimal response"
      (should= {:response-id 321
                :payload     :stuff}
               (sut/response 321 :stuff)))

    (it "request? / response?"
      (let [request (sut/request 111 :bar :params true)
            response (sut/response 222 :payload)]
        (should= true (sut/request? request))
        (should= true (sut/response? response))
        (should= false (sut/request? response))
        (should= false (sut/response? request))))

    )

  (context "connection"

    (it "minimal"
      (should= {:id              "some client id"
                :socket          :socket
                :request-counter 0
                :open?           false}
               (sut/connection "some client id" :socket)))

    (context "connection-request!"

      (it "no callback"
        (let [conn-atom (atom (sut/connection "ABC" :socket))
              request (sut/connection-request! @state conn-atom :foo "param" nil)]
          (should= 1 (:request-id request))
          (should= :foo (:kind request))
          (should= "param" (:params request))
          (should-not-contain :reply? request)
          (should= 1 (:request-counter @conn-atom))
          (should-not-contain :responders @conn-atom)))

      (it "with callback"
        (let [conn-atom (atom (sut/connection "ABC" :socket))
              request (sut/connection-request! @state conn-atom :foo "param" :a-responder)]
          (should= 1 (:request-id request))
          (should= :foo (:kind request))
          (should= "param" (:params request))
          (should= true (:reply? request))
          (should= 1 (:request-counter @conn-atom))
          (let [[responder-fn timeout] (get-in @conn-atom [:responders 1])]
            (should= :a-responder responder-fn)
            (should-not= nil timeout))))
      )

    (context "connection-responder!"

      (it "removes pending responders from connection"
        (let [conn-atom (atom (sut/connection "ABC" :socket))
              _ (sut/connection-request! @state conn-atom :foo "param" :a-responder)
              [responder-fn timeout] (sut/connection-responder! conn-atom 1)]
          (should= :a-responder responder-fn)
          (should-not-contain 1 (:responders @conn-atom))))
      )

    )

  (context "creation"

    (it "bare defaults"
      (should-contain :message-handler @@state)
      #?(:cljs (should-contain :connection @@state))
      #?(:clj (should= {} (:connections @@state)))
      #?(:clj (should= :fake-scheduler (:scheduler @@state))))

    (it "request-timeout"
      (should= 5000 (:request-timeout @@state))
      (should= 1234 (:request-timeout @(sut/create :blah :request-timeout 1234))))

    (it "on-data"
      (should= nil (:on-data @@state))
      (should= "on-data" (:on-data @(sut/create :blah :on-data "on-data"))))

    (it "atom-fn"
      (let [state (sut/create :blah :atom-fn #(atom (assoc % :foo :bar)))]
        (should= :bar (:foo @state))))

    (it "ping interval"
      (should= 30 (:ping-interval @@state))
      (should= 123 (:ping-interval @(sut/create :blah :ping-interval 123))))

    (it "ping task"
      (should-contain :ping-task @@state)
      #?(:clj (should= :fake-delay-task (:ping-task @@state))
         :cljs (should= :fake-timeout (:ping-task @@state))))

    (it "nil ping-interval"
      (let [state (sut/create :blah :ping-interval nil)]
        (should= nil (:ping-interval @state))
        (should-not-contain :ping-task @state)))

    )


  (context "receive message -"

    (before (reset! message-handler-result nil)
            (reset! send!-result true))
    #?(:clj  (before (sut/-open-connection! @state @request "conn-abc" "a-socket")
                     (reset! stub/*stubbed-invocations* []))
       :cljs (before (sut/-add-connection! @state "path" "csrf-123" "conn-abc" "a-socket")
                     (swap! @state assoc-in [:connection :open?] true)))

    (it "request with message-handler missing"
      (swap! @state assoc :message-handler nil)
      (should-not-throw
        #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/request 123 :test "params")))
           :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/request 123 :test "params"))}))))
      (should-not-have-invoked :message-handler))

    (it "request with missing connection"
      #?(:cljs (swap! @state assoc :connection nil))
      #?(:clj  (sut/-data-received @state "MISSING" "a-socket" (utilc/->edn (sut/request 123 :test "params")))
         :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/request 123 :test "params"))})))
      (should-not-have-invoked :message-handler))

    (it "request delegated to handler"
      #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/request 123 :test "params")))
         :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/request 123 :test "params"))})))
      (should-have-invoked :message-handler)
      (let [[message] (stub/last-invocation-of :message-handler)]
        (should= :test (:kind message))
        (should= "params" (:params message))
        (should= "conn-abc" (:connection-id message))
        (should= 123 (:request-id message))
        #?(:clj (should= @request (:request message)))))

    (it "request with response not requested"
      #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/request 123 :test "params" false)))
         :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/request 123 :test "params" false))})))
      (should-not-have-invoked :socket/send!))

    (it "request with response requested"
      (reset! message-handler-result "some-result")
      #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/request 123 :test "params" true)))
         :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/request 123 :test "params" true))})))
      (should-have-invoked :socket/send!)
      (should= ["a-socket" (utilc/->edn (sut/response 123 "some-result"))] (stub/last-invocation-of :socket/send!)))

    (it "request with response : send! failed"
      (reset! send!-result false)
      #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/request 123 :test "params" true)))
         :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/request 123 :test "params" true))})))
      (should-not-contain "conn-abc" (:connections @@state)))

    (it "response matched up to responder"
      #?(:clj  (sut/call! @state "conn-abc" :test "params" (stub :test-responder))
         :cljs (sut/call! @state :test "params" (stub :test-responder)))
      (let [[socket message-str] (stub/last-invocation-of :socket/send!)
            message (sut/unpack message-str)
            request-id (:request-id message)]
        #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/response request-id "a payload")))
           :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/response request-id "a payload"))}))))
      (should-have-invoked :test-responder {:with ["a payload"]}))

    (it "response with missing responder"
      #?(:clj  (sut/call! @state "conn-abc" :test "params" (stub :test-responder))
         :cljs (sut/call! @state :test "params" (stub :test-responder)))
      (should-not-throw
        #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" (utilc/->edn (sut/response 999 "a payload")))
           :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn (sut/response 999 "a payload"))}))))
      (should-not-have-invoked :test-responder))

    (it "non-edn data - no on-data handler"
      (should-not-throw #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" :blah)
                           :cljs (sut/-data-received @state (clj->js {:data :blah}))))
      (should-not-have-invoked :message-handler))

    (it "non-edn data - on-data set"
      (swap! @state assoc :on-data (stub :on-data))
      (should-not-throw #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" :blah)
                           :cljs (sut/-data-received @state (clj->js {:data :blah}))))
      (should-not-have-invoked :message-handler)
      (should-have-invoked :on-data))

    (context "timeout!"

      (it "removes the responder"
        (let [conn-atom (atom (sut/connection "ABC" :socket))
              request (sut/connection-request! @state conn-atom :foo "param" :a-responder)
              request-id (:request-id request)]
          (sut/-timeout! @state conn-atom request-id)
          (should-not-contain request-id (:responders @conn-atom))))

      (it "sends internal message"
        (let [conn-atom (atom (sut/connection "ABC" :socket))
              request (sut/connection-request! @state conn-atom :foo "param" :a-responder)
              request-id (:request-id request)]
          (sut/-timeout! @state conn-atom request-id)
          (should-have-invoked :message-handler)
          (let [[message] (stub/last-invocation-of :message-handler)]
            (should= :ws/timeout (:kind message))
            (should= request-id (:params message)))))

      (it "responses kill the timeout"
        #?(:clj  (sut/call! @state "conn-abc" :test "params" (stub :test-responder))
           :cljs (sut/call! @state :test "params" (stub :test-responder)))
        (let [[_ message-str] (stub/last-invocation-of :socket/send!)
              message (sut/unpack message-str)
              response (sut/response (:request-id message) "a payload")]
          #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" response)
             :cljs (sut/-data-received @state (clj->js {:data (utilc/->edn response)})))
          (should-have-invoked :sut/cancel-timeout {:with [:fake-timeout]})))

      )

    (it "records activity"
      #?(:clj  (sut/-data-received @state "conn-abc" "a-socket" "blah")
         :cljs (sut/-data-received @state (clj->js {:data "blah"})))
      (should= @now (get-in @@state #?(:clj  [:connections "conn-abc" :last-active-at]
                                       :cljs [:connection :last-active-at]))))

    )

  (context "call! -"

    (before (reset! send!-result true))
    #?(:clj  (before (sut/-open-connection! @state @request "conn-abc" "a-socket"))
       :cljs (before (sut/-add-connection! @state "path" "csrf-123" "conn-abc" "a-socket")
                     (swap! @state assoc-in [:connection :open?] true)))

    (it "connection missing - just return false"
      #?(:cljs (swap! @state assoc :connection nil))
      (let [result (sut/call! @state #?(:clj "missing-connection") :test)]
        (should= false result)))

    (it "socket missing - just return false"
      #?(:clj  (swap! @state assoc-in [:connections "conn-abc" :socket] nil)
         :cljs (swap! @state assoc-in [:connection :socket] nil))
      (let [result (sut/call! @state #?(:clj "conn-abc") :test)]
        (should= false result)))

    (it "connection closed"
      #?(:clj  (swap! @state assoc-in [:connections "conn-abc" :open?] false)
         :cljs (swap! @state assoc-in [:connection :open?] false))
      (let [result (sut/call! @state #?(:clj "missing-connection") :test)]
        (should= false result)))

    ;; in js, socket.send does't return so this is Java only
    #?(:clj (it "send! failed because connection closed"
              (reset! send!-result false)
              (should= false (sut/call! @state #?(:clj "conn-abc") :test))
              (should-not-contain "conn-abc" (:connections @@state))))

    (it "simple"
      (should= true (sut/call! @state #?(:clj "conn-abc") :test))
      (should-have-invoked :socket/send!)
      (let [[socket message-str] (stub/last-invocation-of :socket/send!)
            message (sut/unpack message-str)]
        (should= "a-socket" socket)
        (should= :test (:kind message))
        (should= nil (:params message))
        (should-not-contain :reply? message))
      (should-not-contain :responders #?(:clj  (get-in @@state [:connections "conn-abc"])
                                         :cljs (:connection @@state))))

    (it "with params"
      (should= true (sut/call! @state #?(:clj "conn-abc") :test "params"))
      (should-have-invoked :socket/send!)
      (let [[socket message-str] (stub/last-invocation-of :socket/send!)
            message (sut/unpack message-str)]
        (should= "a-socket" socket)
        (should= :test (:kind message))
        (should= "params" (:params message))
        (should-not-contain :reply? message))
      (should-not-contain :responders #?(:clj  (get-in @@state [:connections "conn-abc"])
                                         :cljs (:connection @@state))))

    (it "with callback handler"
      (should= true (sut/call! @state #?(:clj "conn-abc") :test "params" :foo))
      (should-have-invoked :socket/send!)
      (let [[socket message-str] (stub/last-invocation-of :socket/send!)
            message (sut/unpack message-str)]
        (should= "a-socket" socket)
        (should= :test (:kind message))
        (should= true (:reply? message))
        (let [connection (get-in @@state #?(:clj [:connections "conn-abc"] :cljs [:connection]))]
          (should-contain :responders connection)
          (should= :foo (first (get-in connection [:responders (:request-id message)]))))))

    (it "sent message activity time"
      (sut/call! @state #?(:clj "conn-abc") :test)
      (should= @now (get-in @@state #?(:clj  [:connections "conn-abc" :last-active-at]
                                       :cljs [:connection :last-active-at]))))

    )

  (context "keep alive ping"

    (before (reset! send!-result true))
    #?(:clj  (before (sut/-open-connection! @state @request "conn-abc" "a-socket"))
       :cljs (before (sut/-add-connection! @state "path" "csrf-123" "conn-abc" "a-socket")
                     (swap! @state assoc-in [:connection :open?] true)))

    (it "activity?"
      (let [connection (atom (sut/connection "id" "a-socket"))
            moment (-> 5 seconds ago)]
        (should= false (sut/-activity-since? connection moment))
        (sut/-activity! connection)
        (should= true (sut/-activity-since? connection moment))
        (swap! connection assoc :last-active-at (-> 6 seconds ago))
        (should= false (sut/-activity-since? connection moment))))

    (it "ping"
      #?(:clj  (sut/-ping! @state (sut/-connection-cursor @state "conn-abc"))
         :cljs (sut/-ping! @state (sut/-connection-cursor @state)))
      (should-have-invoked :socket/send!)
      (let [[_ message-str] (stub/last-invocation-of :socket/send!)
            message (sut/unpack message-str)]
        (should= :ws/ping (:kind message))
        (should= nil (:params message))
        (should-not-contain :reply? message)))

    (it "ping on inactivity"
      (sut/-activity! (cursor-hack @state "conn-abc") (-> 2 seconds ago))
      (sut/-ping-inactive-connections! @state)
      (should= 0 (pings))

      (sut/-activity! (cursor-hack @state "conn-abc") (-> 31 seconds ago))
      (sut/-ping-inactive-connections! @state)
      (should= 1 (pings)))

    (it "doesn't ping closed connections"
      (let [connection (cursor-hack @state "conn-abc")]
        (sut/-activity! connection (-> 31 seconds ago))
        (swap! connection assoc :open? false)
        (with-redefs [sut/-do-call! (stub :do-call!)]
          (sut/-ping-inactive-connections! @state)
          (should-not-have-invoked :do-call!))))

    (it "ping interval"
      (swap! @state assoc :ping-interval 123)
      (with-redefs [sut/-activity-since? (stub :activity-since? {:return true})]
        (sut/-ping-inactive-connections! @state)
        (let [[_ moment] (stub/last-invocation-of :activity-since?)
              seconds-ago (int (/ (time/millis-between (time/now) moment) 1000))]
          (should= 123 seconds-ago))))

    (it "nil ping interval"
      (swap! @state assoc :ping-interval nil)
      (with-redefs [sut/-activity-since? (stub :activity-since? {:return true})]
        (sut/-ping-inactive-connections! @state)
        (should-not-have-invoked :activity-since?)))

    )

  #?(:clj
     (context "server"

       (with request {:params {:connection-id "client-123" :csrf-token "csrf-blah"} :session/key "csrf-blah"})
       (around [it] (with-redefs [httpkit/send! (stub :httpkit/send! {:invoke (fn [& _] @send!-result)})] (it)))
       (before (reset! message-handler-result nil)
               (reset! send!-result true))

       (context "ring handler"

         (it "invalid csrf"
           (let [response1 (sut/handler @state (assoc-in @request [:params :csrf-token] nil))
                 response2 (sut/handler @state (assoc-in @request [:params :csrf-token] "wrong"))]
             (should= 403 (:status response1))
             (should= 403 (:status response2))
             (should= "Invalid anti-forgery token" (:body response1))
             (should= "Invalid anti-forgery token" (:body response2))))

         (it "invalid connection-id"
           (let [response (sut/handler @state (assoc-in @request [:params :connection-id] nil))]
             (should= 403 (:status response))
             (should= "Invalid connection id" (:body response))))

         (it "socket response"
           (with-redefs [httpkit/as-channel (stub :httpkit/as-channel {:return :channel})]
             (let [response (sut/handler @state @request)
                   [ch-request options] (stub/last-invocation-of :httpkit/as-channel)]
               (should= :channel response)
               (should= @request ch-request)
               ;(should-contain :init options)
               (should-contain :on-receive options)
               ;(should-contain :on-ping options)
               (should-contain :on-close options)
               (should-contain :on-open options))))
         )

       (context "on-open"

         (it "adds connection"
           (sut/-open-connection! @state @request "conn-abc" "a-socket")
           (should-contain "conn-abc" (:connections @@state))
           (should= "conn-abc" (get-in @@state [:connections "conn-abc" :id]))
           (should= "a-socket" (get-in @@state [:connections "conn-abc" :socket]))
           (should= @request (get-in @@state [:connections "conn-abc" :request]))
           (should= true (get-in @@state [:connections "conn-abc" :open?])))

         (it "sends internal :ws/open"
           (sut/-open-connection! @state @request "conn-abc" "a-socket")
           (should= [{:request-id nil :kind :ws/open :params nil :connection-id "conn-abc" :request @request}]
                    (stub/last-invocation-of :message-handler)))

         (it "says hello to client"
           (sut/-open-connection! @state @request "conn-abc" "a-socket")
           (should-have-invoked :httpkit/send!)
           (let [[socket message-str] (stub/last-invocation-of :httpkit/send!)
                 message (sut/unpack message-str)]
             (should= "a-socket" socket)
             (should= :ws/hello (:kind message))))

         )

       (context "on-close"

         (it "removes connection"
           (sut/-open-connection! @state @request "conn-abc" "a-socket")
           (sut/-channel-on-close @state "conn-abc" "a-socket" "a status")
           (should-not-contain "conn-abc" (:connections @@state)))

         (it "sends internal :ws/close"
           (sut/-open-connection! @state @request "conn-abc" "a-socket")
           (sut/-channel-on-close @state "conn-abc" "a-socket" "a status")
           (should= [{:request-id nil :kind :ws/close :params nil :connection-id "conn-abc" :request @request}]
                    (stub/last-invocation-of :message-handler)))

         )

       (context "close"

         (around [it] (with-redefs [httpkit/close (stub :httpkit/close)] (it)))

         (it "missing socket"
           (should-not-throw (sut/close! @state "blah"))
           (should-not-have-invoked :httpkit/close))

         (it "with socket"
           (sut/-open-connection! @state @request "conn-abc" "a-socket")
           (sut/close! @state "conn-abc")
           (should-have-invoked :httpkit/close {:with ["a-socket"]}))

         )

       )

     :cljs
     (context "client"

       (around [it] (with-redefs [sut/-socket-send! (stub :socket/send! {:invoke (fn [& _] @send!-result)})] (it)))

       (context "connection uri"

         (it "not secure"
           (let [location (clj->js {:host "site.com" :protocol "http:"})]
             (should= "ws://site.com/path?connection-id=conn-abc&csrf-token=csrf-123"
                      (sut/-connection-uri location "/path" "conn-abc" "csrf-123"))))

         (it "secure"
           (let [location (clj->js {:host "site.com:443" :protocol "https:"})]
             (should= "wss://site.com:443/path2?connection-id=conn-xyz&csrf-token=csrf-987"
                      (sut/-connection-uri location "/path2" "conn-xyz" "csrf-987"))))

         )


       (context "on-open"

         (it "adds connection"
           (sut/-handle-open @state (clj->js {}))
           (should= true (get-in @@state [:connection :open?]))
           (should-have-invoked :message-handler)
           (should= :ws/open (-> (stub/last-invocation-of :message-handler) first :kind)))

         (it "sends internal :ws/open"
           (sut/-handle-open @state (clj->js {}))
           (should= [{:request-id nil :kind :ws/open :params nil :connection-id nil}]
                    (stub/last-invocation-of :message-handler)))

         )

       (context "on-close"

         (it "removes connection"
           (sut/-handle-open @state (clj->js {}))
           (sut/-handle-close @state (clj->js {}))
           (should= false (get-in @@state [:connection :open?])))

         (it "sends internal :ws/close"
           (sut/-handle-close @state (clj->js {}))
           (should= [{:request-id nil :kind :ws/close :params nil :connection-id nil}]
                    (stub/last-invocation-of :message-handler)))

         )

       (context "on-error"

         (it "sends to handler"
           (sut/-handle-error @state (clj->js {:error "blah"}))
           (should= [{:request-id nil :kind :ws/error :params {:error "blah"} :connection-id nil}]
                    (stub/last-invocation-of :message-handler)))

         )


       ;(context "connect!"
       ;
       ;  (with state (sut/create-client "path" "csrf-123" :test))
       ;  (around [it] (with-redefs [sut/-create-socket (stub :-create-socket {:return (clj->js {:socket true})})] (it)))
       ;
       ;  (it "success"
       ;    (sut/connect! @state)
       ;    (prn "(get-in @state [:connection :socket]): " (get-in @state [:connection :socket]))
       ;    (should= "a-socket" (get-in @state [:connection :socket]))
       ;    (should-not= nil (get-in @state [:connection :id]))
       ;
       ;    )
       ;
       ;  )

       )
     )
  )
