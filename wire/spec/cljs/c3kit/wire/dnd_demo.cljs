(ns c3kit.wire.dnd-demo
		(:require
			[c3kit.apron.log :as log]
			[c3kit.wire.dragndrop2 :as dnd]
			[c3kit.wire.js :as wjs]
			[reagent.core :as reagent]
			[reagent.dom :as dom]
			[c3kit.apron.corec :as ccc]))

(def rainbow-state (reagent/atom {:first-color :red}))
(def colors (reagent/atom {:red {:name "red" :color "red" :next :orange} :orange {:name "orange" :color "orange" :next :yellow} :yellow {:name "yellow" :color "yellow" :next :green} :green {:name "green" :color "green" :next :blue} :blue {:name "blue" :color "blue" :next :indigo} :indigo {:name "indigo" :color "indigo" :next :violet} :violet {:name "violet" :color "blueviolet"}}))

(defn get-color-order [color]
		(loop [color          color
									ordered-colors []]
				(if-not (:next color)
						(conj ordered-colors color)
						(recur (get @colors (keyword (:next color))) (conj ordered-colors color)))))

(defn move-first-element [source-key target-key source-color target-color]
		(let [updated-colors (-> @colors
																									(assoc-in [target-key :next] source-key)
																									(assoc source-key (assoc source-color :next (:next target-color))))]
				(swap! colors merge updated-colors)
				(swap! rainbow-state assoc :first-color (:next source-color))))

(defn move-down-list [source-key target-key source-color target-color]
		(let [source-previous (first (filter #(= source-key (:next %)) (vals @colors)))
								updated-colors  (-> @colors
																										(assoc-in [(keyword (:name source-previous)) :next] (:next source-color))
																										(assoc source-key (assoc source-color :next (:next target-color)))
																										(assoc-in [target-key :next] source-key))]
				(swap! rainbow-state assoc :colors updated-colors)
				(swap! colors merge updated-colors)))

(defn move-to-first [source-key source-color]
		(let [first-color     (:first-color @rainbow-state)
								source-previous (first (filter #(= source-key (:next %)) (vals @colors)))
								updated-colors  (-> @colors
																										(assoc-in [(keyword (:name source-previous)) :next] (:next source-color))
																										(assoc source-key (assoc source-color :next first-color)))]
				(swap! colors merge updated-colors)
				(swap! rainbow-state assoc :first-color source-key)))

(defn update-order [{:keys [source-key target-key]}]
		(let [source-color (:dragged-color @rainbow-state)
								target-color (get @colors target-key)]
				(cond (= (:first-color @rainbow-state) source-key) (move-first-element source-key target-key source-color target-color)
						(= :before target-key) (move-to-first source-key source-color)
						:else (move-down-list source-key target-key source-color target-color))))

(defn color-drag-started [{:keys [source-key] :as drag}]
		(let [source-color    (get @colors source-key)
								source-previous (first (filter #(= source-key (:next %)) (vals @colors)))]
				(swap! rainbow-state assoc :dragging source-key :dragged-color (get @colors source-key) :hover nil :original-state {:colors @colors :first-color (:first-color @rainbow-state)})
				(if source-previous
						(swap! colors assoc-in [(keyword (:name source-previous)) :next] (:next source-color))
						(swap! rainbow-state assoc :first-color (:next source-color)))
				(swap! colors dissoc source-key)))

(defn color-drag-end [] (swap! rainbow-state dissoc :dragging :hover :colors))

(defn color-drop [{:keys [source-key target-key] :as stuff}]
		(println "color drop")
		(update-order stuff))

(defn drag-over-color [{:keys [source-key target-key] :as drag}]
		(swap! rainbow-state assoc :drop-color target-key)
		)

(defn drag-out-color [_]
		(swap! rainbow-state dissoc :drop-color)
		)

(defn color-drag-fake-hiccup [node]
		(let [width (.-clientWidth node)
								color (get @colors (:dragging @rainbow-state))]
				[:div {:id "dragged-color" :style (str "width: " width "px;")}
					[:div {:class "dragging-color" :style {:background-color "white" :text (:color color)}}]]
				))

(def rainbow-dnd (-> (dnd/context)
																			(dnd/add-group :color)
																			(dnd/add-group :color)
																			(dnd/drag-from-to :color :color)
																			(dnd/on-drag-start :color color-drag-started)
																			(dnd/drag-fake-hiccup-fn :color color-drag-fake-hiccup)
																			;(dnd/on-drag :color move-ball)
																			(dnd/on-drop :color color-drop)
																			(dnd/on-drag-end :color color-drag-end)
																			(dnd/on-drag-over :color #(println "color drag-over"))
																			(dnd/on-drag-over :color drag-over-color)
																			(dnd/on-drag-out :color #(println "color drag-out"))
																			(dnd/on-drag-out :color drag-out-color)
																			(dnd/set-drag-class :color "dragging-color")))

(defn color-wrapper [color]
		(let [color-name (:name color)
								wrapper-id (str "-color-wrapper-" color-name)
								color-id   (str "-color-" color-name)]
				[:div.-color-wrapper {:id             wrapper-id :key wrapper-id
																										:style          {:display "flex" :background-color (:color color)}
																										:on-mouse-enter #(when-not (:dragging @rainbow-state) (swap! rainbow-state assoc :hover color-name))
																										:on-mouse-leave #(swap! rainbow-state dissoc :hover)
																										:class          (when (= color-name (:hover @rainbow-state)) "grab")
																										:ref            (dnd/register rainbow-dnd :color (keyword color-name))
																										}
					[:li
						[:div {:id color-id :key color-id}
							[:<>
								[:span {:class "-color color-name"} [:span color-name]]]]]]))

(defn rainbow-demo []
		[:div#rainbow-demo.demo-container
			[:h2 "Rainbow Demo"]
			[:p "Change the order of the rainbow"]
			[:div {:style {:display "flex"}}
				[:div.rainbow-scroller
					(let [colors (seq (get-color-order (get @colors (keyword (:first-color @rainbow-state)))))]
							[:ol {:id "-colors"}
								[:div#-before {:style {:height "50px"} :ref (dnd/register rainbow-dnd :color :before)}]
								(ccc/for-all [color colors]
										(color-wrapper color))
								[:div#-after {:style {:height "50px"} :ref (dnd/register rainbow-dnd :color :after)}]])]]])


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
(defn drag-over [{:keys [target-key]}] (println "target-key: " target-key) (swap! golf-state assoc :drop-hole target-key))
(defn drag-out [_] (swap! golf-state dissoc :drop-hole))

(defn golf-drag-fake-hiccup [node]
		(let [width (.-clientWidth node)]
				(println "width: " width)
				[:div {:id "dragged-ball" :style (str "width: " width "px;")}
					[:div {:class "dragging-ball"}]]
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
							[:div.golf-ball {:style          (:ball-location @golf-state)
																								:on-mouse-enter #(swap! golf-state assoc :hover :ball-1)
																								:on-mouse-leave #(swap! golf-state dissoc :hover)
																								:class          (when (= :ball-1 (:hover @golf-state)) "grab")
																								:ref            (dnd/register golf-dnd :ball :ball-1)}])
					(println "@golf-state: " @golf-state)
					[:div.golf-hole {:style          (:hole-location @golf-state)
																						:class          (when (= :hole-1 (:drop-hole @golf-state)) "hole-hover")
																						:on-mouse-enter #(println "hole mouse enter")
																						:ref            (dnd/register golf-dnd :hole :hole-1)}]]
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
			[:div
				[:h1 "Drag & Drop Demo"]
				[golf-demo]]
			[:div
				[:h1 "Colors of the Rainbow"]
				[rainbow-demo]]])

(defn ^:export init []
		(dom/render [content] (wjs/element-by-id "main"))
		(log/info "Demo initialized"))
