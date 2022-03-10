(ns c3kit.wire.spec-helper-spec
  (:require-macros
    [c3kit.wire.spec-helperc :refer [should-select should-not-select should-have-invoked-ws]]
    [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                         should-not= should-have-invoked after before before-all with-stubs with around
                         stub should-contain should-not-contain should]]
    )
  (:require
    [c3kit.wire.js :as wjs]
    [c3kit.wire.spec-helper :as sut]
    [reagent.core :as reagent]
    ))

(def ratom (reagent/atom {}))

(defn content []
  [:div
   [:input#-text-input
    {:on-key-down  #(swap! ratom assoc :key-down (wjs/e-key-code %))
     :on-key-up    #(swap! ratom assoc :key-up (wjs/e-key-code %))
     :on-key-press #(swap! ratom assoc :key-press (wjs/e-key-code %))}]])

(describe "Spec Helpers"

  (context "Keyboard Events"
    (sut/with-root-dom)
    (before
      (reset! ratom {})
      (sut/render [content])
      (sut/flush))

    (it "simulates a key down event"
      (sut/key-down! "#-text-input" wjs/ESC)
      (should= wjs/ESC (:key-down @ratom)))

    (it "simulates a key up event"
      (sut/key-up! "#-text-input" wjs/ENTER)
      (should= wjs/ENTER (:key-up @ratom)))

    (it "simulates a key press event"
      (sut/key-press! "#-text-input" wjs/ENTER)
      (should= wjs/ENTER (:key-press @ratom)))

    )
  )