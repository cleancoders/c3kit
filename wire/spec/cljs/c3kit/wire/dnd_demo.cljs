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

(defn golf-drag-started [{:keys [source-key]}] (swap! golf-state assoc :dragging source-key :hover nil))
(defn golf-drag-end [] (swap! golf-state dissoc :dragging :hover :drop-hole))
(defn golf-drop [_]
  (swap! golf-state #(-> %
                         (merge (golf-locations))
                         (update :score inc))))
(defn drag-over [{:keys [target-key]}] (swap! golf-state assoc :drop-hole target-key))
(defn drag-out [_] (swap! golf-state dissoc :drop-hole))

(def golf-dnd (-> (dnd/context)
                  (dnd/add-group :ball)
                  (dnd/add-group :hole)
                  (dnd/drag-from-to :ball :hole)
                  (dnd/on-drag-start :ball golf-drag-started)
                  ;(dnd/on-drag :ball move-ball)
                  (dnd/on-drop :hole golf-drop)
                  (dnd/on-drag-end :ball golf-drag-end)
                  (dnd/on-drag-over :ball #(println "ball drag-over"))
                  (dnd/on-drag-over :hole drag-over)
                  (dnd/on-drag-out :ball #(println "ball drag-out"))
                  (dnd/on-drag-out :hole drag-out)
                  (dnd/set-drag-class :ball "dragging-ball")))

(defn golf-demo []
  [:div#golf-demo.demo-container
   [:h2 "Golf Demo"]
   [:p "Drag the ball into the hole.  Score: " (:score @golf-state)]
   [:div {:style {:display "flex"}}
    [:div.golf-green
     (when-not (= :ball-1 (:dragging @golf-state))
       [:div.golf-ball {:style          (:ball-location @golf-state)
                        :on-mouse-enter #(swap! golf-state assoc :hover :ball-1)
                        :on-mouse-leave #(swap! golf-state dissoc :hover)
                        :class          (when (= :ball-1 (:hover @golf-state)) "grab")
                        :ref            (dnd/register golf-dnd :ball :ball-1)}])
     [:div.golf-hole {:style (:hole-location @golf-state)
                      :class (when (= :hole-1 (:drop-hole @golf-state)) "hole-hover")
                      :on-mouse-enter #(println "hole mouse enter")
                      :ref   (dnd/register golf-dnd :hole :hole-1)}]]
    [:div.golf-scroller
     [:div.golf-green
      (when-not (= :ball-2 (:dragging @golf-state))
        [:div.golf-ball {:style          (:ball-location @golf-state)
                         :on-mouse-enter #(swap! golf-state assoc :hover :ball-2)
                         :on-mouse-leave #(swap! golf-state dissoc :hover)
                         :class          (when (= :ball-2 (:hover @golf-state)) "grab")
                         :ref            (dnd/register golf-dnd :ball :ball-2)}])
      [:div.golf-hole {:style (:hole-location @golf-state)
                       :class (when (= :hole-2 (:drop-hole @golf-state)) "hole-hover")
                       :ref   (dnd/register golf-dnd :hole :hole-2)}]]]]])

(defn content []
  [:div
   [:h1 "Drag & Drop Demo"]
   [golf-demo]])

(defn ^:export init []
  (dom/render [content] (wjs/element-by-id "main"))
  (log/info "Demo initialized"))
