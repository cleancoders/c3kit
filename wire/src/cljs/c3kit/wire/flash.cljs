(ns c3kit.wire.flash
  (:require
    [c3kit.apron.corec :refer [conjv]]
    [c3kit.apron.log :as log]
    [c3kit.wire.flashc :as flashc]
    [c3kit.wire.js :as wjs]
    [c3kit.wire.util :as util]
    [reagent.core :as reagent]
    ))

(def flash-timeout-millis (atom 5000))

(def state (reagent/atom {}))

(defn clear! [] (reset! state {}))

(defn first-msg [] (-> @state first flashc/text))
(defn last-msg [] (-> @state last flashc/text))
(defn all-msg [] (->> @state (map :text)))

(defn flash= [f1 f2] (= (flashc/id f1) (flashc/id f2)))

(defn- do-flash-remove [flashes flash]
  (remove (partial flash= flash) flashes))

(defn remove! [flash]
  (swap! state do-flash-remove flash))

(defn active? [flash]
  (if (some (partial flash= flash) @state) true false))

(defn add-no-dups [flashes flash]
  (if (some (partial flash= flash) flashes)
    flashes
    (conjv flashes flash)))

(defn add! [f]
  (let [f (flashc/conform! f)]
    (log/debug "adding flash: " f)
    (swap! state add-no-dups f)
    (when-not (:persist f)
      (wjs/timeout @flash-timeout-millis #(remove! f)))
    f))

(defn add-success! [msg] (add! (flashc/success msg)))
(defn add-warn! [msg] (add! (flashc/warn msg)))
(defn add-error! [msg] (add! (flashc/error msg)))

(defn- flash-message [flash]
  (let [text (flashc/text flash)]
    [:div.flash-message {:class (flashc/flash-class flash)}
     [:div.container
      [:p [:span {:on-click #(remove! flash)} "✕"]
       [:span.flash-message-text (if (seq? text) (util/with-react-keys text) text)]]]]))

(defn flash-root []
  (when-let [flashes (seq @state)]
    [:div.flash-root
     (for [f flashes]
       ^{:key (flashc/id f)} [flash-message f])]))
