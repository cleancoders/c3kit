(ns c3kit.wire.dragndrop2
		(:import [goog History])
		(:require
			[c3kit.apron.log :as log]
			[c3kit.wire.dnd-mobile-patch]
			[goog.dom :as dom]
			[goog.events.EventHandler]
			[goog.fx.DragDropGroup]
			[goog.fx.DragDrop]
			[c3kit.wire.js :as wjs]
			[c3kit.apron.corec :as ccc]
			))

(def drag-threshold 5)

(def dnd-structure
		{:groups      {:group-key-1 {:members    {:member-key-1 {:node                 "dom node"
																																																											:draggable-mousedown  "mousedown listener to start drag"
																																																											:draggable-touchstart "touchstart listener to start drag"
																																																											:droppable-mouseenter "mouseenter listener to dragover"
																																																											:droppable-mouseleave "mouseleave listener to end dragover"
																																																											:droppable-touchend   "touchend listener to end drag"}}
																															:targets    #{"target group keys"}
																															:drag-class "css class that will be added to drag-node when being dragged"
																															:listeners  {:drag-start ["drag event handlers"]
																																												:drag       ["drag event handlers"]
																																												:drag-over  ["drag event handlers"]
																																												:drag-out   ["drag event handlers"]
																																												:drop       ["drag event handlers"]
																																												:drag-end   ["drag event handlers"]}}}
			:maybe-drag  {:start-position      [1 2]
																	:group               "group key"
																	:member              "member key"
																	:node                "member dom node"
																	:document            "document dom node"
																	:mouse-up-listener   "a js event handler"
																	:move-listener       "a js event handler"
																	:touch-end-listener  "a js event handler"
																	:touch-move-listener "a js event handler"}
			:active-drag {:group        "group key"
																	:member       "member key"
																	:node         "member dom node"
																	:drag-node    "dom node being dragged"
																	:offset       "[x y] offset between cursor and drag-node"
																	:document     "document dom node"
																	:end-listener "a js event handler"
																	:drop-target  ["group key" "member key"]
																	}})

(defn get-group [dnd group]
		(if-let [group (get-in @dnd [:groups] group)]
				group
				(throw (ex-info (str "DnD group missing: " group) {:group group :dnd dnd}))))

(defn prevent-default [event] (.preventDefault event))

(defn drag-event
		([source-group source-key source-node event]
			{:source-group  source-group
				:source-key    source-key
				:source-node   source-node
				:browser-event (prevent-default event)})
		([source-group source-key source-node target-group target-key target-node event]
			(assoc (drag-event source-group source-key source-node event)
					:target-group target-group
					:target-key target-key
					:target-node target-node)))

(defn dispatch-event [dnd group type event]
		(loop [listeners (get-in @dnd [:groups group :listeners type])]
				(if-let [listener (first listeners)]
						(if (= false (listener event))
								false
								(recur (rest listeners)))
						true)))

(defn update-drag-node-position [dnd js-event]
		(let [{:keys [drag-node offset]} (:active-drag @dnd)
								drag-style (wjs/node-style drag-node)
								[x y] (wjs/e-coordinates js-event)
								[offset-x offset-y] offset]
				(wjs/o-set drag-style "left" (str (- x offset-x) "px"))
				(wjs/o-set drag-style "top" (str (- y offset-y) "px"))))

(defn handle-drag [dnd js-event]
		(let [state @dnd
								{:keys [group member node]} (:active-drag state)]
				(when (dispatch-event dnd group :drag (drag-event group member node js-event))
						(update-drag-node-position dnd js-event))))

(defn- dispatch-drag-over-out [dnd group member node target-group target-member target-node js-event event-type]
		(let [event (drag-event group member node target-group target-member target-node js-event)]
				(and (dispatch-event dnd group event-type event)
						(dispatch-event dnd target-group event-type event))))

(defn end-drag [dnd js-event]
		(let [{:keys [group member node drag-node document drag-listener end-listener drop-target]} (:active-drag @dnd)
								drag-end-event (drag-event group member node js-event)]
				(wjs/remove-listener document "mousemove" drag-listener)
				(wjs/remove-listener document "touchmove" drag-listener)
				(wjs/remove-listener document "mouseup" end-listener)
				(wjs/remove-listener document "touchend" end-listener)
				(wjs/node-remove-child (wjs/doc-body document) drag-node)
				(when drop-target
						(let [[target-group target-member target-node] drop-target
												drop-event (drag-event group member node target-group target-member target-node js-event)]
								(dispatch-event dnd target-group :drop drop-event)))
				(dispatch-event dnd group :drag-end drag-end-event)
				(swap! dnd dissoc :active-drag)))

(defn append-dragger [doc drag-node drag-class drag-style]
		(wjs/node-id= drag-node "_dragndrop-drag-node_")
		(wjs/o-set drag-style "position" "absolute")
		(wjs/o-set drag-style "pointer-events" "none")            ;; allow wheel events to scroll containers, but prevents mouse-over
		(when drag-class (wjs/node-add-class drag-node drag-class))
		(wjs/node-append-child (wjs/doc-body doc) drag-node))

(defn add-doc-listeners [doc drag-handler end-handler]
		(wjs/add-listener doc "mousemove" drag-handler)
		(wjs/add-listener doc "touchmove" drag-handler)
		(wjs/add-listener doc "mouseup" end-handler)
		(wjs/add-listener doc "touchend" end-handler))

(defn start-drag [dnd group member node js-event]
		(when (dispatch-event dnd group :drag-start (drag-event group member node js-event))
				(let [drag-handler (partial handle-drag dnd)
										end-handler  (partial end-drag dnd)
										doc          (wjs/document node)
										drag-class   (get-in @dnd [:groups group :drag-class])
										drag-node    (wjs/node-clone node true)
										drag-style   (wjs/node-style drag-node)
										dnd-state    @dnd
										scroll-x     (.-scrollX js/window)
										scroll-y     (.-scrollY js/window)
										[start-x start-y] (-> dnd-state :maybe-drag :start-position)
										[node-x node-y _ _] (wjs/node-bounds node)
										offset       [(- start-x node-x scroll-x) (- start-y node-y scroll-y)]
										active-drag  {:group         group
																								:member        member
																								:node          node
																								:drag-node     drag-node
																								:offset        offset
																								:document      doc
																								:drag-listener drag-handler
																								:end-listener  end-handler
																								}]
						(append-dragger doc drag-node drag-class drag-style)
						(add-doc-listeners doc drag-handler end-handler)
						(swap! dnd assoc :active-drag active-drag)
						(update-drag-node-position dnd js-event)))
		(wjs/nod js-event)
		)

(defn end-maybe-drag [dnd _]
		(when-let [maybe-drag (:maybe-drag @dnd)]
				(let [{:keys [document node mouse-up-listener move-listener]} maybe-drag]
						(wjs/remove-listener node "mousemove" move-listener)
						(wjs/remove-listener node "mouseout" move-listener)
						(wjs/remove-listener document "mouseup" mouse-up-listener))
				(swap! dnd dissoc :maybe-drag)))

(defn touch-move [dnd group member node js-event]
		(println "touch-move!")
		(let [[start-x start-y] (-> @dnd :maybe-drag :start-position)
								touches          (first (.-touches js-event))
								[x y] [(.-clientX touches) (.-clientY touches)]
								;[x y] (wjs/e-coordinates js-event)
								distance         (+ (js/Math.abs (- x start-x)) (js/Math.abs (- y start-y)))
								above-threshold? (> distance drag-threshold)
								touch-out?       (= (-> @dnd :maybe-drag :node) (wjs/e-target js-event))]
				(println "[start-x start-y]: " [start-x start-y])
				(println "[x y] distance above-threshold? touch-out?: " [x y] distance above-threshold? touch-out?)
				(when (or above-threshold? touch-out?)
						(do (start-drag dnd group member node js-event)
								(end-maybe-drag dnd nil))
						(wjs/nod js-event))))

(defn draggable-touch-start [dnd group member node js-event]
		(println "touch start!")
		(let [listener       (partial touch-move dnd group member node)
								doc-listener   (partial end-maybe-drag dnd)
								doc            (wjs/document node)
								touches        (first (.-touches js-event))
								start-position [(.-clientX touches) (.-clientY touches)]]
				(println "start-position: " start-position)
				(wjs/add-listener node "touchmove" listener)
				(swap! dnd assoc :maybe-drag {:start-position      start-position
																																		:group               group
																																		:member              member
																																		:node                node
																																		:document            doc
																																		:touch-end-listener  doc-listener
																																		:touch-move-listener listener})))

(defn mouse-move [dnd group member node js-event]
		(let [[start-x start-y] (-> @dnd :maybe-drag :start-position)
								[x y] (wjs/e-coordinates js-event)
								distance         (+ (js/Math.abs (- x start-x)) (js/Math.abs (- y start-y)))
								above-threshold? (> distance drag-threshold)
								mouse-out?       (and (= "mouseout" (wjs/e-type js-event)) (= (-> @dnd :maybe-drag :node) (wjs/e-target js-event)))]
				(when (or above-threshold? mouse-out?)
						(do (start-drag dnd group member node js-event)
								(end-maybe-drag dnd nil))
						(wjs/nod js-event))))

(defn draggable-mouse-down [dnd group member node js-event]
		(when (wjs/e-left-click? js-event)
				(let [listener       (partial mouse-move dnd group member node)
										doc-listener   (partial end-maybe-drag dnd)
										doc            (wjs/document node)
										start-position (wjs/e-coordinates js-event)]
						(wjs/add-listener node "mousemove" listener)
						(wjs/add-listener node "mouseout" listener)
						(wjs/add-listener doc "mouseup" doc-listener)
						(swap! dnd assoc :maybe-drag {:start-position    start-position
																																				:group             group
																																				:member            member
																																				:node              node
																																				:document          doc
																																				:mouse-up-listener doc-listener
																																				:move-listener     listener}))))

(defn maybe-make-draggable! [{:keys [node] :as data} dnd group member]
		(if (seq (get-in @dnd [:groups group :targets]))
				(let [mousedown  (partial draggable-mouse-down dnd group member node)
										touchstart (partial draggable-touch-start dnd group member node)]
						(wjs/add-listener node "mousedown" mousedown)
						(wjs/add-listener node "touchstart" touchstart)
						(assoc data :draggable-mousedown mousedown :draggable-touchstart touchstart))
				data))

(defn droppable-touch-end [dnd target-group target-member target-node js-event]
		(when-let [{:keys [group member node]} (:active-drag @dnd)]
				(when (dispatch-drag-over-out dnd group member node target-group target-member target-node js-event :drag-over)
						(prn "drag-over success")
						(swap! dnd assoc-in [:active-drag :drop-target] [target-group target-member target-node]))))

(defn droppable-mouse-leave [dnd target-group target-member target-node js-event]
		(when-let [{:keys [group member node]} (:active-drag @dnd)]
				(when (dispatch-drag-over-out dnd group member node target-group target-member target-node js-event :drag-out)
						(swap! dnd update :active-drag dissoc :drop-target))))

(defn droppable-mouse-enter [dnd target-group target-member target-node js-event]
		(when-let [{:keys [group member node]} (:active-drag @dnd)]
				(when (dispatch-drag-over-out dnd group member node target-group target-member target-node js-event :drag-over)
						(swap! dnd assoc-in [:active-drag :drop-target] [target-group target-member target-node]))))

(defn maybe-make-droppable! [{:keys [node] :as data} dnd group member]
		(if (some #(contains? % group) (map :targets (vals (:groups @dnd))))
				(let [mouseenter (partial droppable-mouse-enter dnd group member node)
										mouseleave (partial droppable-mouse-leave dnd group member node)
										touchend   (partial droppable-touch-end dnd group member node)]
						(wjs/add-listener node "mouseenter" mouseenter)
						(wjs/add-listener node "mouseleave" mouseleave)
						(wjs/add-listener node "touchend" touchend)
						(assoc data :droppable-mouseenter mouseenter :droppable-mouseleave mouseleave :droppable-touchend touchend))
				data))

(defn register [dnd group member]
		(fn [node]
				(when-not (get-in @dnd [:groups group]) (log/warn "registering to unknown group:" group member))
				(if node
						(let [member-data (-> {:node node}
																										(maybe-make-draggable! dnd group member)
																										(maybe-make-droppable! dnd group member))]
								;(when-not node-id (throw (ex-info "registered dragndrop nodes must have an id" {:group group :member member})))
								(swap! dnd assoc-in [:groups group :members member] member-data))
						(let [{:keys [node draggable-mousedown draggable-touchstart droppable-mouseenter droppable-mouseleave droppable-touchend] :as drag}
												(get-in @dnd [:groups group :members member])]
								(when draggable-mousedown (wjs/remove-listener node "mousedown" draggable-mousedown))
								(when draggable-touchstart (wjs/remove-listener node "touchstart" draggable-touchstart))
								(when droppable-mouseenter (wjs/remove-listener node "mouseenter" droppable-mouseenter))
								(when droppable-mouseleave (wjs/remove-listener node "mouseleave" droppable-mouseleave))
								(when droppable-touchend (wjs/remove-listener node "touchend" droppable-touchend))
								(swap! dnd update-in [:groups group :members] dissoc member)))))

(defn context [] (atom {}))

(defn add-group [dnd group-key]
		(swap! dnd assoc-in [:groups group-key] {})
		dnd)

(defn drag-from-to [dnd from-key to-key]
		(swap! dnd update-in [:groups from-key :targets] #(-> (conj % to-key) set))
		dnd)

(defn- add-group-listener! [dnd group type listener]
		(swap! dnd update-in [:groups group :listeners type] ccc/conjv listener)
		dnd)

(defn on-drag-start [dnd group listener] (add-group-listener! dnd group :drag-start listener))
(defn on-drop [dnd group listener] (add-group-listener! dnd group :drop listener))
(defn on-drag [dnd group listener] (add-group-listener! dnd group :drag listener))
(defn on-drag-over [dnd group listener] (add-group-listener! dnd group :drag-over listener))
(defn on-drag-out [dnd group listener] (add-group-listener! dnd group :drag-out listener))
(defn on-drag-end [dnd group listener] (add-group-listener! dnd group :drag-end listener))

(defn set-drag-class [dnd group classname]
		(swap! dnd assoc-in [:groups group :drag-class] classname)
		dnd)

(declare fake-hiccup->dom)

(defn- vector->html [hiccup]
		(if (= :<> (first hiccup))
				(map fake-hiccup->dom (rest hiccup))
				(let [tag-name  (name (first hiccup))
										options   (when (map? (second hiccup)) (clj->js (second hiccup)))
										remaining (if options (drop 2 hiccup) (rest hiccup))
										children  (map fake-hiccup->dom remaining)]
						(dom/createDom tag-name options (clj->js (flatten children))))))

(defn fake-hiccup->dom [hiccup]
		(cond (vector? hiccup) (vector->html hiccup)
				(seq? hiccup) (map fake-hiccup->dom hiccup)
				(string? hiccup) hiccup
				(nil? hiccup) nil
				:else (pr-str hiccup)))

(defn drag-fake-hiccup-fn [dnd group-key fake-hiccup-fn]
		(set! (.-createDragElement (get-group dnd group-key))
				(fn [node] (fake-hiccup->dom (fake-hiccup-fn node))))
		dnd)


