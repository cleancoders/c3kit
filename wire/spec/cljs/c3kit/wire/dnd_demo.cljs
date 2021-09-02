(ns c3kit.wire.dnd-demo
		(:require
			[c3kit.apron.log :as log]
			[c3kit.wire.dragndrop2 :as dnd]
			[c3kit.wire.js :as wjs]
			[reagent.core :as reagent]
			[reagent.dom :as dom]
			[c3kit.apron.corec :as ccc]))

(def teams-state (reagent/atom {:trucks {:first-item "Megalodon" :before :before-trucks} :team {:first-item nil :before :before-team}}))
(def lists (reagent/atom {:trucks :before-trucks :team :before-team}))
(def all-trucks (reagent/atom [{:name "Megalodon" :owner :trucks :next "El Toro Loco"}
																															{:name "El Toro Loco" :owner :trucks :next "Grave Digger"}
																															{:name "Grave Digger" :owner :trucks :next "EarthShaker"}
																															{:name "EarthShaker" :owner :trucks :next "Son-uva Digger"}
																															{:name "Son-uva Digger" :owner :trucks :next "Ice Cream Man"}
																															{:name "Ice Cream Man" :owner :trucks :next "Hurricane Force"}
																															{:name "Hurricane Force" :owner :trucks :next "Monster Mutt Rottweiler"}
																															{:name "Monster Mutt Rottweiler" :owner :trucks :next "Blaze"}
																															{:name "Blaze" :owner :trucks :next "Dragon"}
																															{:name "Dragon" :owner :trucks}]))

(defn get-truck-by-owner [name items] (first (filter #(= name (:name %)) items)))
(defn get-first-of-list [owner] (get-in @teams-state [owner :first-item]))
(defn get-items-by-owner [owner] (filter #(= owner (:owner %)) @all-trucks))

(defn get-jam-order [owner]
		(let [items (filter #(= owner (:owner %)) @all-trucks)]
				(loop [item          (get-truck-by-owner (get-in @teams-state [owner :first-item]) items)
											ordered-items []]
						(if-not (:next item)
								(seq (conj ordered-items item))
								(recur (get-truck-by-owner (:next item) items) (conj ordered-items item))))))

(defn remove-truck
		([truck] (remove #(= (:name truck) (:name %)) @all-trucks))
		([trucks truck] (remove #(= (:name truck) (:name %)) trucks)))

(defn move-the-first-element [source-key target-key source-truck target-truck]
		(println "move first element")
		(let [updated-trucks (-> @all-trucks
																									(remove-truck target-truck)
																									(conj (assoc target-truck :next source-key))
																									(remove-truck source-truck)
																									(conj (assoc source-truck :next (:next target-truck))))]
				(reset! all-trucks updated-trucks)
				(swap! teams-state assoc-in [(:owner source-truck) :first-item] (:next source-truck))))

(defn move-item-down-list [source-key target-key source-truck target-truck]
		(println "move-down-list")
		(let [updated-trucks (-> @all-trucks
																									(remove-truck source-truck)
																									(conj (assoc source-truck :next (:next target-truck)))
																									(remove-truck target-truck)
																									(conj (assoc target-truck :next source-key)))]
				(reset! all-trucks updated-trucks)))

(defn move-item-to-first [source-key source-truck]
		(println "move to first")
		(let [first-truck-name (get-first-of-list (:owner source-truck))
								;source-previous  (first (filter #(= source-key (:next %)) (get-items-by-owner (:owner source-truck))))
								updated-trucks   (->
																											;(remove-truck source-previous)
																											;(conj (assoc source-previous :next (:next source-truck)))
																											(remove-truck source-truck)
																											(conj (assoc source-truck :next first-truck-name)))]
				(reset! all-trucks updated-trucks)
				(swap! teams-state assoc-in [(:owner source-truck) :first-item] source-key)))

(defn move-to-new-owner [source-key target-key source-truck target-truck]
		(println "changing owners")
		(let [before-key?    (some #(= target-key %) (vals @lists))
								new-owner      (if before-key?
																									(key (first (filter #(= target-key (val %)) @lists)))
																									(if target-truck
																											(:owner target-truck)
																											target-key))
								first-element? (empty? (filter #(= new-owner (:owner %)) @all-trucks))
								target-truck   (if target-truck target-truck (last (get-jam-order new-owner)))
								updated-source (-> source-truck (assoc :owner new-owner) (dissoc :next))
								]
				(cond (or first-element? before-key?) (move-item-to-first source-key updated-source)
						:else (move-item-down-list source-key target-key updated-source target-truck))
				))

(defn return-to-original-state []
		(let [{:keys [items state]} (:original-state @teams-state)]
				(reset! teams-state state)
				(reset! all-trucks items)))

(defn update-truck-order [{:keys [source-key target-key] :as drag}]
		(println "update order")
		(let [source-truck (:dragged-truck @teams-state)
								target-truck (get-truck-by-owner target-key @all-trucks)
								before-key   (keyword (str "before-" (apply str (rest (str (:owner source-truck))))))
								]
				(cond (= before-key target-key) (move-item-to-first source-key source-truck)
						(not (= (:owner source-truck) (:owner target-truck))) (move-to-new-owner source-key target-key source-truck target-truck)
						:else (move-item-down-list source-key target-key source-truck target-truck))))

(defn update-previous [previous-truck source-truck]
		(let [updated-previous (assoc previous-truck :next (:next source-truck))
								updated-trucks   (-> (remove-truck previous-truck)
																											(conj updated-previous))]
				(reset! all-trucks updated-trucks)))

(defn truck-drag-started [{:keys [source-key]}]
		(let [source-truck    (get-truck-by-owner source-key @all-trucks)
								first-in-list?  (= source-key (get-in @teams-state [(:owner source-truck) :first-item]))
								source-previous (first (filter #(= (:name source-truck) (:next %)) @all-trucks))]
				(swap! teams-state assoc :dragging source-key :dragged-truck source-truck :hover nil :original-state {:items @all-trucks :state @teams-state})
				(if first-in-list?
						(swap! teams-state assoc-in [(:owner source-truck) :first-item] (:next source-truck))
						(update-previous source-previous source-truck))
				(swap! all-trucks #(remove-truck source-truck))))

(defn truck-drag-end [{:keys [source-key drop-target] :as drag}]
		(when (empty? (filter #(= source-key (:name %)) @all-trucks))
				(return-to-original-state))
		(swap! teams-state dissoc :dragging :hover :original-state))

(defn truck-drop [drag]
		(println "TRUCK DROP")
		(update-truck-order drag)
		(swap! teams-state dissoc :drop-box))

(defn drag-over-truck [{:keys [source-key target-key] :as drag}]
		(swap! teams-state assoc :drop-box target-key))

(defn drag-out-truck [_]
		(swap! teams-state dissoc :drop-box))

(defn truck-drag-fake-hiccup [node]
		(let [width (.-clientWidth node)
								truck (get @all-trucks (:dragging @teams-state))]
				[:div {:id "dragged-truck" :class "dragging-truck" :style (str "width: " width "px;") :background-color "white" :text (:truck truck)}
					[:div {:class "dragging-truck" :style {:background-color "white" :text (:truck truck)}}]]
				))

(def teams-dnd (-> (dnd/context)
																	(dnd/add-group :truck)
																	(dnd/add-group :list)
																	(dnd/drag-from-to :truck :truck)
																	(dnd/drag-from-to :truck :list)
																	(dnd/on-drag-start :truck truck-drag-started)
																	(dnd/drag-fake-hiccup-fn :truck truck-drag-fake-hiccup)
																	(dnd/on-drop :truck truck-drop)
																	(dnd/on-drop :list truck-drop)
																	(dnd/on-drag-end :truck truck-drag-end)
																	(dnd/on-drag-over :truck #(println "truck drag-over"))
																	(dnd/on-drag-over :truck drag-over-truck)
																	(dnd/on-drag-over :list #(println "list drag-over"))
																	(dnd/on-drag-over :list drag-over-truck)
																	(dnd/on-drag-out :truck #(println "truck drag-out"))
																	(dnd/on-drag-out :truck drag-out-truck)
																	(dnd/on-drag-out :list #(println "list dragout"))
																	(dnd/on-drag-out :list drag-out-truck)
																	(dnd/set-drag-class :truck "dragging-truck")))

(defn truck-content [truck]
		(let [truck-id (str "-truck-" (:name truck))]
				[:li.truck {:id "-truck" :style {:background-color "lightgrey"}}
					[:div {:id truck-id :key truck-id}
						[:<>
							[:span {:class "-item item-name"} [:span (:name truck)]]]]]))

(defn truck-wrapper [truck]
		(let [truck-name (:name truck)
								wrapper-id (str "-truck-wrapper-" truck-name)]
				[:div.-truck-wrapper {:id             wrapper-id
																										:key            wrapper-id
																										:style          (when (= (:name truck) (:drop-box @teams-state)) {:height "100px" :background-color "white"})
																										:on-mouse-enter #(when-not (:dragging @teams-state) (swap! teams-state assoc :hover truck-name))
																										:on-mouse-leave #(swap! teams-state dissoc :hover)
																										:class          (when (= truck-name (:hover @teams-state)) "grab")
																										:ref            (dnd/register teams-dnd :truck truck-name)
																										}
					[truck-content truck]]))

(defn list-items []
		[:div#-trucks.list-scroller
			(let [trucks (get-jam-order :trucks)]
					(println "trucks: " trucks)
					[:ol#-colors.colors
						[:div#-before
							(if (= :before-trucks (:drop-box @teams-state))
									{:style {:height "50px"} :ref (dnd/register teams-dnd :truck :before-trucks)}
									{:style {:height "1px"} :ref (dnd/register teams-dnd :truck :before-trucks)})]
						(ccc/for-all [truck trucks]
								(truck-wrapper truck))
						])])

(defn show-team []
		[:div#-team.list-scroller {:ref (dnd/register teams-dnd :list :team)}
			(let [first-member    (get-first-of-list :team)
									ordered-members (when first-member (get-jam-order :team))]
					[:ol#-team.colors
						[:div#-before
							(if (= :before-team (:drop-box @teams-state))
									{:style {:height "50px"} :ref (dnd/register teams-dnd :truck :before-team)}
									{:style {:height "1px"} :ref (dnd/register teams-dnd :truck :before-team)})]
						(when ordered-members
								(ccc/for-all [member ordered-members]
										(truck-wrapper member)))
						[:li.truck "Drop a Monster Truck here"]])])

(defn team-demo []
		[:div#team-demo.demo-container
			[:h2 "Monster Jam Demo"]
			[:p "Select 5 to Build Your Jam Team"]
			[:div#lists.list-container
				[:div.team-container
					[:h3 (str "Jam Team - " (count (filter #(= :team (:owner %)) @all-trucks)) " Monster Trucks")]
					[show-team]]
				[:div#-trucks.team-container
					[:h3 "Monster Trucks"]
					[list-items]]]])




(def rainbow-state (reagent/atom {:first-item :red}))
(def colors (reagent/atom {:red {:name "red" :color "red" :next :orange} :orange {:name "orange" :color "orange" :next :yellow} :yellow {:name "yellow" :color "yellow" :next :green} :green {:name "green" :color "green" :next :blue} :blue {:name "blue" :color "blue" :next :indigo} :indigo {:name "indigo" :color "indigo" :next :violet} :violet {:name "violet" :color "blueviolet"}}))

(defn get-color-order [item items-atom]
		(loop [item          item
									ordered-items []]
				(if-not (:next item)
						(conj ordered-items item)
						(recur (get @items-atom (keyword (:next item))) (conj ordered-items item)))))

(defn move-first-element [source-key target-key source-color target-color]
		(let [updated-colors (-> @colors
																									(assoc-in [target-key :next] source-key)
																									(assoc source-key (assoc source-color :next (:next target-color))))]
				(swap! colors merge updated-colors)
				(swap! rainbow-state assoc :first-item (:next source-color))))

(defn move-down-list [source-key target-key source-color target-color]
		(let [source-previous (first (filter #(= source-key (:next %)) (vals @colors)))
								updated-colors  (-> @colors
																										;(assoc-in [(keyword (:name source-previous)) :next] (:next source-color))
																										(assoc source-key (assoc source-color :next (:next target-color)))
																										(assoc-in [target-key :next] source-key))]
				(swap! rainbow-state assoc :colors updated-colors)
				(swap! colors merge updated-colors)))

(defn move-to-first [source-key source-color]
		(let [first-color     (:first-item @rainbow-state)
								source-previous (first (filter #(= source-key (:next %)) (vals @colors)))
								updated-colors  (-> @colors
																										(assoc-in [(keyword (:name source-previous)) :next] (:next source-color))
																										(assoc source-key (assoc source-color :next first-color)))]
				(swap! colors merge updated-colors)
				(swap! rainbow-state assoc :first-item source-key)))

(defn update-order [{:keys [source-key target-key]}]
		(let [source-color (:dragged-color @rainbow-state)
								target-color (get @colors target-key)]
				(cond (= (:first-item @rainbow-state) source-key) (move-first-element source-key target-key source-color target-color)
						(= :before target-key) (move-to-first source-key source-color)
						:else (move-down-list source-key target-key source-color target-color))))

(defn color-drag-started [{:keys [source-key] :as drag}]
		(let [source-color    (get @colors source-key)
								source-previous (first (filter #(= source-key (:next %)) (vals @colors)))]
				(swap! rainbow-state assoc :dragging source-key :dragged-color (get @colors source-key) :hover nil :original-state {:items @colors :state @rainbow-state})
				(if source-previous
						(swap! colors assoc-in [(keyword (:name source-previous)) :next] (:next source-color))
						(swap! rainbow-state assoc :first-item (:next source-color)))
				(swap! colors dissoc source-key)))

(defn color-drag-end [{:keys [source-key]}]
		(println "source-key: " source-key)
		(println "(filter #(= source-key (keyword (:name %))) @colors): " (filter #(= source-key (keyword (:name %))) @colors))
		(when (empty? (filter #(= source-key (keyword (:name %))) @colors))
				(return-to-original-state))
		(swap! rainbow-state dissoc :dragging :hover :colors))

(defn color-drop [{:keys [source-key target-key] :as stuff}]
		(println "color drop")
		(swap! rainbow-state dissoc :drop-color)
		(update-order stuff))

(defn drag-over-color [{:keys [source-key target-key] :as drag}]
		(println "drag-over-color: target-key: " target-key)
		(swap! rainbow-state assoc :drop-color target-key))

(defn drag-out-color [_]
		(swap! rainbow-state dissoc :drop-color))

(defn color-drag-fake-hiccup [node]
		(let [width (.-clientWidth node)
								color (get @colors (:dragging @rainbow-state))]
				[:div {:id "dragged-color" :class "dragging-color" :style (str "width: " width "px;") :background-color "white" :text (:color color)}
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
																										:style          (when (= (keyword (:name color)) (:drop-color @rainbow-state)) {:height "100px" :background-color "white"})
																										:on-mouse-enter #(when-not (:dragging @rainbow-state) (swap! rainbow-state assoc :hover color-name))
																										:on-mouse-leave #(swap! rainbow-state dissoc :hover)
																										:class          (when (= color-name (:hover @rainbow-state)) "grab")
																										:ref            (dnd/register rainbow-dnd :color (keyword color-name))
																										}
					[color-content color]]))

(defn rainbow-demo []
		[:div#rainbow-demo.demo-container
			[:h2 "Rainbow Demo"]
			[:p "Change the order of the rainbow"]
			[:div                                                    ;{:style {:display "flex"}}
				[:div.list-scroller
					(let [colors (seq (get-color-order (get @colors (keyword (:first-item @rainbow-state))) colors))]
							[:ol#-colors.colors
								[:div#-before
									(if (= :before (:drop-color @rainbow-state))
											{:style {:height "50px"} :ref (dnd/register rainbow-dnd :color :before)}
											{:style {:height "1px"} :ref (dnd/register rainbow-dnd :color :before)})]
								(ccc/for-all [color colors]
										(color-wrapper color))
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
			[:br]
			[:div.list-container
				[:div.rainbow-container
					[rainbow-demo]]
				[:div.teams-container
					[team-demo]]]])

(defn ^:export init []
		(dom/render [content] (wjs/element-by-id "main"))
		(log/info "Demo initialized"))
