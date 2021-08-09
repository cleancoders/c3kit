(ns c3kit.wire.dnd-demo
  (:require
    [c3kit.apron.log :as log]
    [c3kit.wire.dragndrop2 :as dnd]
    [c3kit.wire.js :as wjs]
    [reagent.core :as reagent]
    [reagent.dom :as dom]
    ))

(defn random-golf-position [] {:left (rand-int 450) :top (rand-int 450)})

(defn golf-locations [] {:ball-location (random-golf-position)
                         :hole-location (random-golf-position)})

(def golf-state (reagent/atom (assoc (golf-locations) :score 0)))

;(defn golf-drag-started [e]
;  (let [[green-x green-y _ _] (wjs/client-bounds (wjs/element-by-id "golf-green"))
;        [_ _ ball-w ball-h] (wjs/client-bounds (wjs/element-by-id "golf-ball"))]
;    (swap! golf-state assoc :offset [(+ green-x (/ ball-w 2)) (+ green-y (/ ball-h 2))])))

;(defn move-ball [{:keys [browser-event]}]
;  (let [[client-x client-y] (wjs/e-coordinates browser-event)]
;    (swap! golf-state (fn [state]
;                        (let [[offset-x offset-y] (:offset state)
;                              left (max 0 (- client-x offset-x))
;                              top (max 0 (- client-y offset-y))]
;                          (assoc state :ball-location {:left left :top top}))))))

;(defn golf-drop [_]
;  (println "drop")
;  (swap! golf-state #(-> %
;                         (merge (golf-locations))
;                         (update :score inc))))

(defn golf-drag-started [e] (swap! golf-state assoc :dragging? true))
(defn move-ball [{:keys [browser-event]}])
(defn show-ball [_] (swap! golf-state dissoc :dragging?))
(defn golf-drop [_]
  (println "drop")
  (swap! golf-state #(-> %
                         (merge (golf-locations))
                         (update :score inc))))

(def golf-dnd (-> (dnd/context)
                  (dnd/add-group :ball)
                  (dnd/add-group :hole)
                  (dnd/drag-from-to :ball :hole)
                  (dnd/on-drag-start :ball golf-drag-started)
                  (dnd/on-drag :ball move-ball)
                  (dnd/on-drop :hole golf-drop)
                  (dnd/on-drag-end :ball show-ball)
                  (dnd/on-drag-over :ball #(println "ball drag-over"))
                  (dnd/on-drag-over :hole #(println "hole drag-over"))
                  (dnd/on-drag-out :ball #(println "ball drag-out"))
                  (dnd/on-drag-out :hole #(println "hole drag-out"))
                  ))

(defn golf-demo []
  [:div#golf-demo.demo-container
   [:h2 "Golf Demo"]
   [:p "Drag the ball into the hole.  Score: " (:score @golf-state)]
   [:div#golf-green
    (when-not (:dragging? @golf-state)
      [:div#golf-ball.golf-ball {:style (:ball-location @golf-state) :ref (dnd/register golf-dnd :ball :the-ball)}])
    [:div#golf-hole {:style (:hole-location @golf-state) :ref (dnd/register golf-dnd :hole :the-hole)
                     :on-mouse-over #(println "mouseover")}]]])

(defn content []
  [:div
   [:h1 "Drag & Drop Demo"]
   [golf-demo]])

(defn ^:export init []
  (dom/render [content] (wjs/element-by-id "main"))
  (log/info "Demo initialized"))
