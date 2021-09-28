(ns c3kit.wire.dragndrop-spec
		(:require-macros [speclj.core :refer [describe context xit it should-not-be-nil should-be-nil should= should-not
																																								should-not= should-have-invoked after before with-stubs with around
																																								should-contain should-not-contain should should-not-have-invoked stub]]
																			[c3kit.wire.spec-helperc :refer [should-select should-not-select]])
		(:require
			[c3kit.wire.dragndrop2 :as sut]
			[c3kit.wire.spec-helper :as helper]
			[c3kit.apron.log :as log]
			[c3kit.wire.dnd-demo :as demo]
			[c3kit.wire.js :as wjs]
			[reagent.dom :as dom]
			[reagent.core :as reagent]
			[speclj.stub :as stub]))

(def blank-dnd (sut/context))
(def pets [{:id "tails" :name "Tails"} {:id "brusly" :name "Brusly"} {:id "cheddar" :name "Cheddar"} {:id "wasabi" :name "Wasabi"}])
(def state (atom {}))

(defn drag-start [{:keys [source-key]}] (swap! state assoc :last-call :drag-start :source-key source-key) "on-drag-start")
(defn drag-over [{:keys [target-key]}] (swap! state assoc :last-call :drag-over :target-key target-key) "on-drag-over")
(defn drag-out [_] (reset! state {:last-call :drag-out}) "on-drag-out")
(defn drop! [{:keys [target-key]}] (swap! state assoc :drop target-key) "on-drag-drop")
(defn drag-end [_] (swap! state assoc :last-call :drag-end) "on-drag-end")
(defn fake-hiccup [] [:div {:id "dragging-pet"}
																						[:div "a dragging pet"]])

(def dnd (sut/context))

(defn refresh-dnd [] (-> (sut/context)
																							(sut/add-group :pet)
																							(sut/add-group :pet-drop)
																							(sut/drag-from-to :pet :pet-drop)
																							(sut/on-drag-start :pet drag-start)
																							(sut/drag-fake-hiccup-fn :pet fake-hiccup)
																							(sut/on-drop :pet-drop drop!)
																							(sut/on-drag-end :pet drag-end)
																							(sut/on-drag-over :pet drag-over)
																							(sut/on-drag-over :pet-drop drag-over)
																							(sut/on-drag-out :pet drag-out)
																							(sut/on-drag-out :pet-drop drag-out)
																							(sut/set-drag-class :pet "dragging-pet")))

(defn test-content []
		[:div
			[:div
				[:h1 "Pets"]
				[:ol#-pets
					[:li#-brusly {:style nil :ref (sut/register dnd :pet "brusly")}]
					[:li#-tails {:ref (sut/register dnd :pet "tails")}]
					[:li#-cheddar {:ref (sut/register dnd :pet "cheddar")}]
					[:li#-wasabi {:ref (sut/register dnd :pet "wasabi")}]
					[:li#-dog-drop {:ref (sut/register dnd :pet-drop "dog-drop")}]
					[:li#-cat-drop {:ref (sut/register dnd :pet-drop "cat-drop")}]
					]]])

(def dragger (reagent/atom nil))
(def drag-node (reagent/track #(when @dragger (:node @dragger))))
(def dropper (reagent/atom nil))
(def drop-node (reagent/track #(when @dropper (:node @dropper))))

(defn get-doc-listeners [kind]
		(->> (stub/invocations-of kind)
				(filter #(= (wjs/document @drag-node) (first %)))
				(map #(second %))))

(defn get-listeners [kind] (map #(second %) (stub/invocations-of kind)))

(describe "Drag and Drop"
		(with-stubs)
		(helper/with-root-dom)
		(before (reset! blank-dnd {}))
		(around [it] (with-redefs [wjs/add-listener (stub :add-listener)
																													wjs/remove-listener (stub :remove-listener)
																													sut/prevent-default (stub :prevent-default)
																													wjs/nod (stub :nod)]
																	(log/capture-logs (it))))

		(context "invalid dnd"
				(it "registers an unknown group"
						((sut/register dnd :a-box "is not a group") "a node")
						(should-not-have-invoked :add-listener)
						(should= "registering to unknown group: :a-box is not a group" (log/captured-logs-str))
						(swap! dnd update-in [:groups] dissoc :not-a-pet))
				)

		(context "valid dnd"
				(it "add-group"
						(sut/add-group blank-dnd :pet)
						(should= [:pet] (keys (get-in @blank-dnd [:groups]))))

				(it "drags from and to"
						(sut/add-group blank-dnd :pet)
						(sut/add-group blank-dnd :pet)
						(sut/drag-from-to blank-dnd :pet :pet)
						(should= #{:pet} (get-in @blank-dnd [:groups :pet :targets])))

				(it "sets drag class"
						(sut/set-drag-class blank-dnd :pet "pet")
						(should= "pet" (get-in @blank-dnd [:groups :pet :drag-class])))

				(context "dnd behaviors"
						(before
								(reset! dnd @(refresh-dnd))
								(helper/render [test-content]))

						(it "structure"
								(should-select "#-brusly")
								(should-select "#-tails")
								(should-select "#-cheddar")
								(should-select "#-wasabi")
								(should-select "#-dog-drop")
								(should-select "#-cat-drop"))

						(context "dnd registration"
								(it "draggables have drag listeners"
										(let [registrations      (stub/invocations-of :add-listener)
																registration-types (map #(second %) registrations)]
												(should-contain "mousedown" registration-types)
												(should-contain "touchstart" registration-types)
												(should-contain drag-start (get-in @dnd [:groups :pet :listeners :drag-start]))
												(should-contain drag-over (get-in @dnd [:groups :pet :listeners :drag-over]))
												(should-contain drag-out (get-in @dnd [:groups :pet :listeners :drag-out]))
												(should-contain drag-end (get-in @dnd [:groups :pet :listeners :drag-end]))
												(should= "dragging-pet" (get-in @dnd [:groups :pet :drag-class]))))

								(it "droppables have drop listeners"
										(let [registrations      (stub/invocations-of :add-listener)
																registration-types (map #(second %) registrations)]
												(should-contain "mouseenter" registration-types)
												(should-contain "mouseleave" registration-types)
												(should-contain "touchend" registration-types)
												(should-contain drag-out (get-in @dnd [:groups :pet :listeners :drag-out]))
												(should-contain drag-over (get-in @dnd [:groups :pet-drop :listeners :drag-over]))
												(should-contain drag-out (get-in @dnd [:groups :pet-drop :listeners :drag-out]))
												(should-contain drop! (get-in @dnd [:groups :pet-drop :listeners :drop]))))

								(it "registers draggables & droppables"
										(let [draggables [:node :draggable-mousedown :draggable-touchstart]
																droppables [:node :droppable-mouseenter :droppable-mouseleave :droppable-touchend]]
												(should-have-invoked :add-listener {:times 14})
												(should= ["brusly" "tails" "cheddar" "wasabi"] (keys (get-in @dnd [:groups :pet :members])))
												(should= #{:pet-drop} (get-in @dnd [:groups :pet :targets]))
												(should= ["dog-drop" "cat-drop"] (keys (get-in @dnd [:groups :pet-drop :members])))
												(doseq [pet (map :id pets)]
														(should= draggables (keys (get-in @dnd [:groups :pet :members pet]))))
												(doseq [pet-drop ["dog-drop" "cat-drop"]]
														(should= droppables (keys (get-in @dnd [:groups :pet-drop :members pet-drop]))))
												))
								)
						(context "dnd actions"
								(before (reset! dragger (get-in @dnd [:groups :pet :members "brusly"]))
										(reset! dropper (get-in @dnd [:groups :pet-drop :members "dog-drop"])))

								(context "draggable-mouse-down"
										(it "right mouse-down"
												(let [cnt-add-listeners (count (stub/invocations-of :add-listener))
																		mousedown         (:draggable-mousedown @dragger)]
														(mousedown (clj->js {:button 1}))
														(should= 0 (- (count (stub/invocations-of :add-listener)) cnt-add-listeners))
														(should-not-contain :maybe-drag @dnd)))

										(it "left mouse-down"
												(let [before-listeners (get-listeners :add-listener)
																		mousedown        (:draggable-mousedown @dragger)
																		_                (mousedown (clj->js {:button 0 :clientX 0 :clientY 0}))
																		after-listeners  (get-listeners :add-listener)]
														(should= 3 (- (count after-listeners) (count before-listeners)))
														(should-contain "mousemove" after-listeners)
														(should-contain "mouseout" after-listeners)
														(should-contain "mouseup" after-listeners)
														(should-contain :maybe-drag @dnd)
														(should= "brusly" (get-in @dnd [:maybe-drag :member]))
														(should= [0 0] (get-in @dnd [:maybe-drag :start-position]))))
										)

								(context "dragging"
										(before
												(let [mousedown (:draggable-mousedown @dragger)]
														(mousedown (clj->js {:button 0 :clientX 0 :clientY 0}))))

										(context "maybe drag"
												(it "mouse up with no valid movement"
														(let [remove-listeners-before (get-listeners :remove-listener)
																				mouseup                 (get-in @dnd [:maybe-drag :mouse-up-listener])
																				_                       (mouseup {})
																				remove-listeners-after  (get-listeners :remove-listener)]
																(should-not-contain :maybe-drag @dnd)
																(should= 3 (- (count remove-listeners-after) (count remove-listeners-before)))
																(should-contain "mousemove" remove-listeners-after)
																(should-contain "mouseout" remove-listeners-after)
																(should-contain "mouseup" remove-listeners-after)))

												(it "movement below threshold"
														(with-redefs [sut/start-drag (stub :start-drag)]
																(let [mousemove-handler (get-in @dnd [:maybe-drag :move-listener])]
																		(mousemove-handler (clj->js {:clientX 2 :clientY 2}))
																		(should-not-have-invoked :start-drag)
																		(should-contain :maybe-drag @dnd))))

												(it "valid movement - above threshold"
														(with-redefs [sut/start-drag (stub :start-drag)]
																(let [mousemove-handler (get-in @dnd [:maybe-drag :move-listener])]
																		(mousemove-handler (clj->js {:clientX 10 :clientY 10}))
																		(should-have-invoked :start-drag)
																		(should-have-invoked :nod)
																		(should-not-contain :maybe-drag @dnd))))

												(it "valid movement - mouseout"
														(with-redefs [sut/start-drag (stub :start-drag)]
																(let [mousemove-handler (get-in @dnd [:maybe-drag :move-listener])]
																		(mousemove-handler (clj->js {:type "mouseout" :target @drag-node}))
																		(should-have-invoked :nod)
																		(should-have-invoked :start-drag)
																		(should-not-contain :maybe-drag @dnd))))
												)

										(it "not start-drag with false dispatch event"
												(with-redefs [sut/dispatch-event (stub :dispatch {:return false})]
														(sut/start-drag dnd :pet "brusly" @drag-node (clj->js {}))
														(should-not-contain :active-drag @dnd)
														(should-not-contain "_dragndrop-drag-node_" (map #(.-id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))

										(it "start drag"
												(with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
														(sut/start-drag dnd :pet "brusly" @drag-node (clj->js {:target @drag-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}))
														(let [doc-listeners (get-doc-listeners :add-listener)
																				node-style    (wjs/node-style (get-in @dnd [:active-drag :drag-node]))]
																(should-contain :active-drag @dnd)
																(should= 5 (count doc-listeners))
																(should-contain "mousemove" doc-listeners)
																(should= [-10 -10] (get-in @dnd [:active-drag :offset]))
																(should= "10px" (wjs/o-get node-style "left"))
																(should= "10px" (wjs/o-get node-style "top"))
																(should= :drag-start (:last-call @state))
																(should= "brusly" (:source-key @state))
																(should-contain "_dragndrop-drag-node_" (map #(.-id %) (wjs/node-children (wjs/doc-body (wjs/document))))))))

										#_(it "fakes a hiccup"
												(with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
														(sut/start-drag dnd :pet "brusly" @drag-node (clj->js {:target @drag-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}))
														(let [dragger-parent (get-in @dnd [:active-drag :drag-node])
																				dragger (first (wjs/node-children dragger-parent))]
																(println "(wjs/node-children dragger-parent): " (wjs/node-children dragger-parent))
																(println "dragger: " dragger)
																(println "(.-id dragger): " (.-id dragger))
																(should= "dragging-pet" (.-id dragger))
																(should= "dragging-pet" (.-class dragger))
																(should= "a dragging pet" (.-value dragger)))))

										(it "mouse-enter no active drag"
												(let [mouseenter (:droppable-mouseenter @dropper)]
														(mouseenter (clj->js {}))
														(should= nil (get-in @dnd [:active-drag :drop-target]))
														))

										(it "mouse-leave no active drag"
												(let [mouseleave (:droppable-mouseleave @dropper)]
														(should= nil (mouseleave (clj->js {})))))

										(context "active-drag"
												(before (with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
																						(let [mousemove (get-in @dnd [:maybe-drag :move-listener])]
																								(mousemove (clj->js {:type "mouseout" :target @drag-node :scrollX 0 :scrollY 0}))
																								)))

												(it "handles the drag"
														(let [drag-handler (get-in @dnd [:active-drag :drag-listener])]
																(drag-handler (clj->js {:target @drag-node :clientX 10 :clientY 15}))
																(let [node-style (wjs/node-style (get-in @dnd [:active-drag :drag-node]))]
																		(should= [-10 -10] (get-in @dnd [:active-drag :offset]))
																		(should= "20px" (wjs/o-get node-style "left"))
																		(should= "25px" (wjs/o-get node-style "top")))))

												(it "droppable-mouse-enter"
														(let [mouseenter (:droppable-mouseenter @dropper)]
																(mouseenter (clj->js {}))
																(should= [:pet-drop "dog-drop" @drop-node] (get-in @dnd [:active-drag :drop-target]))
																(should= :drag-over (:last-call @state))
																(should= "dog-drop" (:target-key @state))))

												(it "droppable-mouse-leave"
														(let [mouseenter (:droppable-mouseenter @dropper)
																				mouseleave (:droppable-mouseleave @dropper)]
																(mouseenter (clj->js {}))
																(mouseleave (clj->js {}))
																(should= :drag-out (:last-call @state))
																(should= nil (:target-key @state))))

												(it "end-drag - no target"
														(let [mouseup (get-in @dnd [:active-drag :end-listener])]
																(mouseup (clj->js {}))
																(should= nil (:drop! @state))))

														(it "end-drag - with target"
																(let [mouseenter (:droppable-mouseenter @dropper)
																						mouseup    (get-in @dnd [:active-drag :end-listener])
																						rmv-listeners-before (get-doc-listeners :remove-listener)]
																		(mouseenter (clj->js {}))
																		(mouseup (clj->js {}))
																		(should= 4 (- (count (get-doc-listeners :remove-listener)) (count rmv-listeners-before)))
																		(should= "dog-drop" (:drop @state))
																		(should= :drag-end (:last-call @state))
																		(should-not-contain "_dragndrop-drag-node_" (map #(.-id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))
												)
										)
								)
						)
				)
		)

;; TODO - MDM: Work within scrolling
;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: custom fake hiccup for dragged node - when drag starts use 'this' as fake-hiccup - look at original dnd for this
;; TODO - MDM: Same-list, reordering
;; TODO - MDM: Multiple list, transferring and ordering
