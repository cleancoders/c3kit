(ns c3kit.wire.flash
  (:require
    [c3kit.apron.corec :refer [conjv]]
    [c3kit.wire.flashc :as flashc]
    [hiccup.element :as elem]))

(defn include [request flash] (update-in request [:flash :messages] conjv flash))

(defn success [request text] (include request (flashc/success text)))
(defn success* [response col msg-fn] (reduce #(success %1 (msg-fn %2)) response col))

(defn warn [request text] (include request (flashc/warn text)))
(defn warn* [response col msg-fn] (reduce #(warn %1 (msg-fn %2)) response col))

(defn error [request text] (include request (flashc/error text)))
(defn error* [response col msg-fn] (reduce #(error %1 (msg-fn %2)) response col))


(defn clear-messages [response] (update response :flash dissoc :messages))

(defn copy [response request] (assoc response :flash (:flash request)))

(defn messages [response] (-> response :flash :messages))
(defn first-msg [response] (-> response :flash :messages first))
(defn first-msg-text [response] (-> response :flash :messages first flashc/text))
(defn first-msg-level [response] (-> response :flash :messages first flashc/level))
(defn all-msg-text [response] (map flashc/text (-> response :flash :messages)))


(defn- flash-class [flash] (if (flashc/error? flash) "error" "success"))

(def flash-js
  "function removeFlash(id) {
    var f = document.getElementById(id);
    return f.parentNode.removeChild(f);
   }

   function timeoutFlash(id) {
    setTimeout(function(){ removeFlash(id); }, 8000);
   }")

(defn- flash-message [flash]
  [:div.flash-message {:class (flash-class flash) :id (:id flash)}
   (elem/javascript-tag (str "timeoutFlash('" (:id flash) "');"))
   [:div.container
    [:p [:span {:onclick (str "removeFlash('" (:id flash) "');")} "✕"]
     [:span (flashc/text flash)]]]])

(defn flash-root [request]
  (when-let [flashes (messages request)]
    [:div.flash-root
     (for [f flashes] (flash-message f))]))

