(ns c3kit.wire.websocket-spec
  (:require-macros [speclj.core :refer [describe it should= should-not= after before stub around should-not-have-invoked
                                        should-have-invoked with-stubs context]])
  (:require
    [c3kit.apron.corec :as ccc]
    [c3kit.wire.api :as api]
    [c3kit.wire.websocket :as sut]
    [c3kit.wire.websocketc :as wsc]
    [speclj.core]
    ))

(describe "Websocket"

  (with-stubs)
  (around [it] (with-redefs [sut/make-call! (stub :make-call!)] (it)))

  (it "on-connect callback"
    (sut/call! :some/call {} ccc/noop)
    (should-not-have-invoked :make-call!)
    (sut/message-handler {:kind :ws/open})
    (should-have-invoked :make-call!))

  (it "on-connect invokes immediately if already connected"
    (set! sut/client (atom {:connection {:open? true}}))
    (sut/call! :some/call {} ccc/noop)
    (should-have-invoked :make-call!))

  (context "connecting"

    (around [it]
      (with-redefs [wsc/connect! (stub :wsc/connect!)]
        (api/configure! :ws-on-reconnected (stub :reconnected!))
        (it)))

    (it "reconnects on close"
      (reset! sut/open? true)
      (sut/push-handler {:kind :ws/close})
      (should= false @sut/open?)
      (should-have-invoked :wsc/connect!))

    (it "first connection doesn't tell page reconnected"
      (reset! sut/open? false)
      (reset! sut/reconnection? false)
      (sut/push-handler {:kind :ws/open})
      (should= true @sut/open?)
      (should-not-have-invoked :reconnected!))

    (it "reconnection tells page when reconnected"
      (reset! sut/open? true)
      (reset! sut/reconnection? false)
      (sut/push-handler {:kind :ws/close})
      (sut/push-handler {:kind :ws/open})
      (should-have-invoked :reconnected!))

    )
  )

