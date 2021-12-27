(ns c3kit.wire.flash-spec
  (:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before with-stubs with around
                                        should-contain should-not-contain should should-not-have-invoked stub]]
                   [c3kit.wire.spec-helperc :refer [should-select should-not-select]])
  (:require
    [c3kit.wire.js :as cc]
    [c3kit.wire.flash :as sut]
    [c3kit.wire.flashc :as flashc]
    [c3kit.wire.spec-helper :as helper]
    ))

(describe "Flash"

  (helper/with-root-dom)
  (before (reset! sut/state {})
          (helper/render [sut/flash-root]))

  (it "adds success"
    (sut/add-success! "Huray!")
    (should= 1 (count @sut/state))
    (should (flashc/success? (first @sut/state)))
    (should= "Huray!" (flashc/text (first @sut/state)))
    (should-not= nil (flashc/id (first @sut/state))))

  (it "adds error"
    (sut/add-error! "Oh Noez!")
    (should= 1 (count @sut/state))
    (should= true (flashc/error? (first @sut/state)))
    (should= "Oh Noez!" (flashc/text (first @sut/state)))
    (should-not= nil (flashc/id (first @sut/state))))

  (it "adds info"
    (sut/add-warn! "Did you know...")
    (should= 1 (count @sut/state))
    (should= false (flashc/error? (first @sut/state)))
    (should= false (flashc/success? (first @sut/state)))
    (should= true (flashc/warn? (first @sut/state)))
    (should= "Did you know..." (flashc/text (first @sut/state)))
    (should-not= nil (flashc/id (first @sut/state))))

  (it "shows all flashes"
    (sut/add-success! "m1")
    (sut/add-success! "m2")
    (sut/add-success! "m3")
    (should= ["m1" "m2" "m3"] (sut/all-msg))
    (should= "m1" (sut/first-msg))
    (should= "m3" (sut/last-msg))

    (helper/render [sut/flash-root])
    (let [results (helper/select-all ".flash-message")]
      (should= 3 (count results))
      (doseq [div results]
        (should= "flash-message success" (helper/class-name div)))
      (should= "m1" (helper/html (nth results 0) ".flash-message-text"))
      (should= "m2" (helper/html (nth results 1) ".flash-message-text"))
      (should= "m3" (helper/html (nth results 2) ".flash-message-text"))))

  (it "shows success flashes"
    (sut/add-success! "m1")
    (helper/flush)
    (should-select ".success")
    (should-not-select ".error"))

  (it "shows flash with string type"
    (sut/add! {:level "success" :text "m1"})
    (helper/flush)
    (should-contain "success" (helper/class-name ".flash-message")))

  (it "shows error flashes"
    (sut/add-error! "m1")
    (helper/flush)
    (should-contain "error" (helper/class-name ".flash-message")))

  (it "shows warn flashes"
    (sut/add-warn! "m1")
    (helper/flush)
    (should-contain "warn" (helper/class-name ".flash-message")))

  (it "clicking the X button"
    (sut/add-error! "m1")
    (helper/flush)
    (helper/click! "p > span")
    (should= [] @sut/state))

  ;(it "touch the X button"
  ;  (sut/add-error! "m1")
  ;  (helper/flush)
  ;  (helper/touch-start! "p > span")
  ;  (should= [] @sut/state))

  (context "adding"

    (it "stored into order"
      (let [f1 (sut/add-success! "Hello")
            f2 (sut/add-success! "Bye")]
        (should= [f1 f2] @sut/state)))

    (it "active?"
      (let [f1 (sut/add-success! "Hello")
            f2 (sut/add-success! "Bye")]
        (should (sut/active? f1))
        (should (sut/active? f2))))

    (it "active?"
      (let [f1 (sut/add-success! "Hello")]
        (sut/add! f1)
        (should (sut/active? f1))
        (should= 1 (count @sut/state))
        (should= [f1] @sut/state)))
    )

  (context "timeout"

    (with-stubs)
    (around [it] (with-redefs [cc/timeout (stub :timeout)] (it)))

    (it "is added when persist is false"
      (sut/add-success! "Hello")
      (should-have-invoked :timeout))

    (it "is not added when persist is true"
      (sut/add! (flashc/create :success "Hello" true))
      (should-not-have-invoked :timeout))

    )
  )
