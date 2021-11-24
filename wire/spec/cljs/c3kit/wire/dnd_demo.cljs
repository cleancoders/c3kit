(ns c3kit.wire.dnd-demo
  (:require
    [c3kit.apron.log :as log]
    [c3kit.wire.dragndrop2 :as dnd]
    [c3kit.wire.js :as wjs]
    [reagent.core :as reagent]
    [reagent.dom :as dom]
    [c3kit.apron.corec :as ccc]
    [clojure.string :as string]))

(defn get-element-by-id [id items] (first (filter #(or (= id (keyword (:id %))) (= id (:id %))) items)))
(defn get-first-element-id [state owner] (get-in @state [owner :first-item]))
(defn get-first-element [state items owner] (-> (get-first-element-id state owner) (get-element-by-id items)))
(defn get-elements-by-owner [owner items] (filter #(= owner (:owner %)) items))

(defn get-items-order [items owner first-item]
  (let [items (get-elements-by-owner owner items)]
    (loop [item          first-item
           ordered-items []]
      (if-not (:next item)
        (seq (conj ordered-items item))
        (recur (get-element-by-id (:next item) items) (conj ordered-items item))))))

(defn remove-element
  [elements element] (remove #(= (:id element) (:id %)) elements))

(defn update-elements [element update-fn elements]
  (if-not element
    elements
    (-> (remove-element elements element) (conj (update-fn)))))


(def monster-jam-state (reagent/atom {:trucks {:first-item :megalodon} :team {:first-item nil}}))
(def monster-trucks (reagent/atom [{:id :megalodon :name "Megalodon" :owner :trucks :next :el-toro-loco}
                                   {:id :el-toro-loco :name "El Toro Loco" :owner :trucks :next :grave-digger}
                                   {:id :grave-digger :name "Grave Digger" :owner :trucks :next :earth-shaker}
                                   {:id :earth-shaker :name "EarthShaker" :owner :trucks :next :son-uva-digger}
                                   {:id :son-uva-digger :name "Son-uva Digger" :owner :trucks :next :ice-cream-man}
                                   {:id :ice-cream-man :name "Ice Cream Man" :owner :trucks :next :hurricane-force}
                                   {:id :hurricane-force :name "Hurricane Force" :owner :trucks :next :monster-mutt-rottweiler}
                                   {:id :monster-mutt-rottweiler :name "Monster Mutt Rottweiler" :owner :trucks :next :blaze}
                                   {:id :blaze :name "Blaze" :owner :trucks :next :dragon}
                                   {:id :dragon :name "Dragon" :owner :trucks}]))

(defn move-element-in-list [source-key source-element target-element state-atom items-atom]
  (let [first-element-name (get-first-element-id state-atom (:owner source-element))
        target-key         (:id target-element)
        target-previous    (first (filter #(or (= (keyword target-key) (:next %)) (= target-key (:next %))) @items-atom))
        updated-elements   (->> @items-atom
                                (update-elements source-element #(assoc source-element :next target-key :owner (:owner target-element)))
                                (update-elements target-previous #(assoc target-previous :next source-key)))]
    (when (or (= target-key first-element-name) (= (keyword target-key) first-element-name))
      (swap! state-atom assoc-in [(:owner source-element) :first-item] source-key))
    (reset! items-atom updated-elements)))

(defn move-element-to-last [source-key source-element state-atom items-atom]
  (let [last-element     (last (get-items-order @items-atom (:owner source-element) (get-first-element state-atom @items-atom (:owner source-element))))
        updated-elements (->> @items-atom
                              (update-elements source-element #(dissoc source-element :next))
                              (update-elements last-element #(assoc last-element :next source-key)))]
    (reset! items-atom updated-elements)
    (when-not last-element (swap! state-atom assoc-in [(:owner source-element) :first-item] source-key))))

(defn move-to-new-owner [source-key target-key source-element target-element state-atom]
  (let [new-owner      (if target-element (:owner target-element) target-key)
        first-element  (get-first-element monster-jam-state @monster-trucks new-owner)
        updated-source (-> source-element (assoc :owner new-owner) (dissoc :next))]
    (if (or (nil? target-element) (nil? first-element))
      (move-element-to-last source-key updated-source monster-jam-state monster-trucks)
      (move-element-in-list source-key updated-source target-element monster-jam-state monster-trucks))))

(defn return-to-original-state [state-atom items-atom]
  (let [{:keys [items state]} (:original-state @state-atom)]
    (reset! state-atom state)
    (reset! items-atom items)))

(defn update-order [{:keys [source-key target-key]} state-atom items-atom]
  (let [source-element (:dragged-element @state-atom)
        target-element (get-element-by-id target-key @items-atom)
        after-key      (keyword (str "after-" (apply str (rest (str (:owner source-element))))))
        ]
    (cond (or (= :after target-key) (= after-key target-key)) (move-element-to-last source-key source-element state-atom items-atom)
          (not= (:owner source-element) (:owner target-element)) (move-to-new-owner source-key target-key source-element target-element state-atom)
          :else (move-element-in-list source-key source-element target-element state-atom items-atom))))

(defn update-previous [previous-element source-element items-atom]
  (let [updated-previous (assoc previous-element :next (:next source-element))
        updated-elements (-> @items-atom
                             (remove-element previous-element)
                             (conj updated-previous))]
    (reset! items-atom updated-elements)))

(defn truck-drag-started [{:keys [source-key]}]
  (let [source-truck    (get-element-by-id source-key @monster-trucks)
        first-in-list?  (= source-key (get-first-element-id monster-jam-state (:owner source-truck)))
        source-previous (first (filter #(= (:id source-truck) (:next %)) @monster-trucks))]
    (swap! monster-jam-state assoc :dragging source-key :dragged-element source-truck :hover nil :original-state {:items @monster-trucks :state @monster-jam-state})
    (if first-in-list?
      (swap! monster-jam-state assoc-in [(:owner source-truck) :first-item] (:next source-truck))
      (update-previous source-previous source-truck monster-trucks))
    (swap! monster-trucks #(remove-element @monster-trucks source-truck))))

(defn truck-drag-end [{:keys [source-key]}]
  (when (empty? (filter #(= source-key (:id %)) @monster-trucks))
    (return-to-original-state monster-jam-state monster-trucks))
  (swap! monster-jam-state dissoc :dragging :hover :original-state))

(defn truck-drop [dnd]
  (update-order dnd monster-jam-state monster-trucks)
  (swap! monster-jam-state dissoc :drop-truck))

(defn drag-over-truck [{:keys [target-key]}]
  (swap! monster-jam-state assoc :drop-truck target-key))

(defn drag-out-truck [_]
  (swap! monster-jam-state dissoc :drop-truck))

(defn truck-drag-fake-hiccup [node]
  (let [width (.-clientWidth node)
        truck (:dragged-element @monster-jam-state)]
    (println "truck: " truck)
    (println "(:name truck): " (:name truck))
    #_[:div.-color-wrapper {:id "dragged-color"}
       [:<>
        [:span.color {:class "-color" :style {:background-color (:color color)}}
         (:name color)]]]
    [:div.-truck-wrapper {:id "dragged-truck" :style {:width width}}
     (:name truck)]
    ))

(def monster-jam-dnd (-> (dnd/context)
                         (dnd/add-group :truck)
                         (dnd/add-group :truck-drop)
                         (dnd/drag-from-to :truck :truck)
                         (dnd/drag-from-to :truck :truck-drop)
                         (dnd/on-drag-start :truck truck-drag-started)
                         (dnd/drag-fake-hiccup-fn :truck truck-drag-fake-hiccup)
                         (dnd/on-drop :truck truck-drop)
                         (dnd/on-drop :truck-drop truck-drop)
                         (dnd/on-drag-end :truck truck-drag-end)
                         (dnd/on-drag-over :truck #(println "truck drag-over"))
                         (dnd/on-drag-over :truck drag-over-truck)
                         (dnd/on-drag-over :truck-drop #(println "list drag-over"))
                         (dnd/on-drag-over :truck-drop drag-over-truck)
                         (dnd/on-drag-out :truck #(println "truck drag-out"))
                         (dnd/on-drag-out :truck drag-out-truck)
                         (dnd/on-drag-out :truck-drop #(println "list dragout"))
                         (dnd/on-drag-out :truck-drop drag-out-truck)
                         (dnd/set-drag-class :truck "dragging-truck")))

(defn truck-content [truck]
  (let [truck-id (str "-truck-" (-> (:name truck) string/lower-case (string/replace #"\s" "-")))]
    [:li.truck {:id "-truck" :style {:background-color (if (= :trucks (:owner truck)) "lightblue" "rgb(245, 183, 177)")}}
     [:div {:id truck-id :key truck-id}
      [:<>
       [:span {:class "-item item-name"} [:span (:name truck)]]]]]))

(defn truck-wrapper [truck]
  (let [truck-name (:name truck)
        truck-id   (:id truck)
        wrapper-id (str "-truck-wrapper-" (-> truck-name string/lower-case (string/replace #"\s" "-")))]
    [:div.-truck-wrapper {:id             wrapper-id
                          :key            wrapper-id
                          :on-mouse-enter #(when-not (:dragging @monster-jam-state) (swap! monster-jam-state assoc :hover (:id truck)))
                          :on-mouse-leave #(swap! monster-jam-state dissoc :hover)
                          :class          (when (= truck-id (:hover @monster-jam-state)) "grab")
                          :ref            (dnd/register monster-jam-dnd :truck truck-id)
                          }
     (when (= (:id truck) (:drop-truck @monster-jam-state))
       [:div {:id "-placeholder" :style {:height "50px" :background-color "white"}}])
     [truck-content truck]]))

(defn list-items []
  [:div#-trucks.list-scroller
   (let [first-truck (get-first-element monster-jam-state @monster-trucks :trucks)
         trucks      (get-items-order @monster-trucks :trucks first-truck)]
     [:ol#-trucks.trucks
      (ccc/for-all [truck trucks]
                   (truck-wrapper truck))
      [:div#-after
       (if (= :after-trucks (:drop-truck @monster-jam-state))
         {:style {:height "50px"} :ref (dnd/register monster-jam-dnd :truck :after-trucks)}
         {:style {:height "1px"} :ref (dnd/register monster-jam-dnd :truck :after-trucks)})]])])

(defn maybe-show-drop-box [elements]
  (when (< (count elements) 5)
    [:div.-placeholder-wrapper {:id    "-wrapper-end"
                                :key   "-wrapper-end"
                                :style {:height "500px" :background-color "white"}
                                :ref   (dnd/register monster-jam-dnd :truck-drop :team)
                                }
     (when (= :team (:drop-truck @monster-jam-state))
       [:div {:id "-placeholder" :style {:height "50px" :background-color "white"}}])
     [:li.truck {:id "-truck" :style {:background-color "white"}}
      [:div {:id "-team-after" :key "-team-after"}
       [:<>
        [:span {:class "-item item-name"} [:span "Drop a Monster Truck Here"]]]]]]))

(defn show-team []
  [:div#-team.list-scroller {:ref (dnd/register monster-jam-dnd :truck-drop :team)}
   (let [first-truck    (get-first-element monster-jam-state @monster-trucks :team)
         ordered-trucks (when first-truck (get-items-order @monster-trucks :team first-truck))]
     [:ol#-team.colors
      (when ordered-trucks
        (ccc/for-all [truck ordered-trucks]
                     (truck-wrapper truck)))
      [maybe-show-drop-box ordered-trucks]])])

(defn team-demo []
  [:div#team-demo.demo-container
   [:h2 "Monster Jam Demo"]
   [:p "Select 5 to Build Your Jam Team"]
   [:div#lists.list-container
    [:div.team-container
     [:h3 (str "Jam Team - " (count (filter #(= :team (:owner %)) @monster-trucks)) " Monster Trucks")]
     [show-team]]
    [:div#-trucks.team-container
     [:h3 "Monster Trucks"]
     [list-items]]]])




(def rainbow-state (reagent/atom {:colors {:first-item :red}}))
(def colors (reagent/atom [{:id :red :name "red" :color "red" :owner :colors :next :orange} {:id :orange :name "orange" :color "orange" :owner :colors :next :yellow} {:id :yellow :name "yellow" :color "yellow" :owner :colors :next :green} {:id :green :name "green" :color "green" :owner :colors :next :blue} {:id :blue :name "blue" :color "blue" :owner :colors :next :indigo} {:id :indigo :name "indigo" :color "indigo" :owner :colors :next :violet} {:id :violet :name "violet" :color "blueviolet" :owner :colors}]))

(defn color-drag-started [{:keys [source-key]}]
  (let [source-element  (get-element-by-id source-key @colors)
        first-in-list?  (= source-key (get-in @rainbow-state [(:owner source-element) :first-item]))
        source-previous (first (filter #(= source-key (:next %)) @colors))
        ]
    (swap! rainbow-state assoc :dragging source-key :dragged-element source-element :hover nil :original-state {:items @colors :state @rainbow-state})
    (if first-in-list?
      (swap! rainbow-state assoc-in [(:owner source-element) :first-item] (:next source-element))
      (update-previous source-previous source-element colors))
    (swap! colors #(remove-element @colors source-element))))

(defn color-drag-end [{:keys [source-key]}]
  (when (empty? (filter #(= source-key (:id %)) @colors))
    (return-to-original-state rainbow-state colors))
  (swap! rainbow-state dissoc :dragging :hover :original-state))

(defn color-drop [dnd]
  (update-order dnd rainbow-state colors)
  (swap! rainbow-state dissoc :drop-color :dragged-element))

(defn drag-over-color [{:keys [target-key]}]
  (swap! rainbow-state assoc :drop-color target-key))

(defn drag-out-color [_]
  (swap! rainbow-state dissoc :drop-color))

(defn color-drag-fake-hiccup [node]
  (let [color (:dragged-element @rainbow-state)]
    [:div.-color-wrapper {:id "dragged-color"}
     [:<>
      [:span.color {:class "-color" :style {:background-color (:color color)}}
       (:name color)]]]
    ))

(def rainbow-dnd (-> (dnd/context)
                     (dnd/add-group :color)
                     (dnd/drag-from-to :color :color)
                     (dnd/on-drag-start :color color-drag-started)
                     (dnd/drag-fake-hiccup-fn :color color-drag-fake-hiccup)
                     (dnd/on-drop :color color-drop)
                     (dnd/on-drag-end :color color-drag-end)
                     (dnd/on-drag-over :color #(println "color drag-over"))
                     (dnd/on-drag-over :color drag-over-color)
                     (dnd/on-drag-out :color #(println "color drag-out"))
                     (dnd/on-drag-out :color drag-out-color)
                     (dnd/set-drag-class :color "dragging-color")))

(defn color-content [color]
  (let [color-id (str "-color-" (:name color))]
    [:li.color {:id "-color" :style {:background-color (:color color)}}
     [:div {:id color-id :key color-id}
      [:<>
       [:span {:class "-color item-name"} [:span (:name color)]]]]]))

(defn color-wrapper [color]
  (let [color-name (:name color)
        wrapper-id (str "-color-wrapper-" color-name)]
    [:div.-color-wrapper {:id             wrapper-id
                          :key            wrapper-id
                          :on-mouse-enter #(when-not (:dragging @rainbow-state) (swap! rainbow-state assoc :hover (:id color)))
                          :on-mouse-leave #(swap! rainbow-state dissoc :hover)
                          :class          (when (= (:id color) (:hover @rainbow-state)) "grab")
                          :ref            (dnd/register rainbow-dnd :color (:id color))
                          }
     (when (= (:id color) (:drop-color @rainbow-state))
       [:div {:id "-placeholder" :style {:height "50px" :background-color "white"}}])
     [color-content color]]))


(defn rainbow-demo []
  [:div#rainbow-demo.demo-container
   [:h2 "Rainbow Demo"]
   [:p "Change the order of the rainbow"]
   [:div
    [:div.list-scroller
     (let [first-color (get-first-element rainbow-state @colors :colors)
           colors      (get-items-order @colors :colors first-color)]
       [:ol#-colors.colors
        (ccc/for-all [color colors]
                     (color-wrapper color))
        [:div#-after
         {:style {:height "100px"} :ref (dnd/register rainbow-dnd :color :after)}]
        ])]]])


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

(defn golf-drag-fake-hiccup [node]
  (let [width (.-clientWidth node)]
    (println "HICCUP!!!!")
    (println "(.-id node): " (.-id node))
    [:div {:id "dragging-ball" :style {:width width :background-color "blueviolet"}}]
    ))

(def golf-dnd (-> (dnd/context)
                  (dnd/add-group :ball)
                  (dnd/add-group :hole)
                  (dnd/drag-from-to :ball :hole)
                  (dnd/on-drag-start :ball golf-drag-started)
                  (dnd/drag-fake-hiccup-fn :ball golf-drag-fake-hiccup)
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
       [:div#ball-1.golf-ball {:style          (:ball-location @golf-state)
                               :on-mouse-enter #(swap! golf-state assoc :hover :ball-1)
                               :on-mouse-leave #(swap! golf-state dissoc :hover)
                               :class          (when (= :ball-1 (:hover @golf-state)) "grab")
                               :ref            (dnd/register golf-dnd :ball :ball-1)}])
     [:div#hole-1.golf-hole {:style          (:hole-location @golf-state)
                             :class          (when (= :hole-1 (:drop-hole @golf-state)) "hole-hover")
                             :on-mouse-enter #(println "hole mouse enter")
                             :ref            (dnd/register golf-dnd :hole :hole-1)}]]
    [:div.golf-scroller
     [:div.golf-green
      (when-not (= :ball-2 (:dragging @golf-state))
        [:div#ball-2.golf-ball {:style          (:ball-location @golf-state)
                                :on-mouse-enter #(swap! golf-state assoc :hover :ball-2)
                                :on-mouse-leave #(swap! golf-state dissoc :hover)
                                :class          (when (= :ball-2 (:hover @golf-state)) "grab")
                                :ref            (dnd/register golf-dnd :ball :ball-2)}])
      [:div#hole-2.golf-hole {:style (:hole-location @golf-state)
                              :class (when (= :hole-2 (:drop-hole @golf-state)) "hole-hover")
                              :ref   (dnd/register golf-dnd :hole :hole-2)}]]]]])

(defn content []
  [:div
   [:div
    [:h1 "Drag & Drop Demo"]
    [golf-demo]]
   [:br]
   [:div.list-container
    [:div.rainbow-container
     [rainbow-demo]]
    [:div.teams-container
     [team-demo]]]])

(defn ^:export init []
  (dom/render [content] (wjs/element-by-id "main"))
  (log/info "Demo initialized"))
