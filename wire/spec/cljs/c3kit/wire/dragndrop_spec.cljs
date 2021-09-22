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
			[c3kit.wire.js :as wjs]))


(def blank-dnd (sut/context))
(def pets [{:id "tails" :name "Tails"} {:id "brusly" :name "Brusly"} {:id "cheddar" :name "Cheddar"} {:id "wasabi" :name "Wasabi"}])

(defn drag-start [] "on-drag-start")
(defn drag-over [] "on-drag-over")
(defn drag-out [] "on-drag-out")
(defn drop! [] "on-drag-drop")
(defn drag-end [] "on-drag-end")

(def dnd
		(-> (sut/context)
				(sut/add-group :pet)
				(sut/add-group :pet-drop)
				(sut/drag-from-to :pet :pet)
				(sut/drag-from-to :pet :pet-drop)
				(sut/on-drag-start :pet drag-start)
				(sut/on-drop :pet drop!)
				(sut/on-drag-end :pet drag-end)
				(sut/on-drag-over :pet drag-over)
				(sut/on-drag-over :pet-drop drag-over)
				(sut/on-drag-out :pet drag-out)
				(sut/on-drag-out :pet-drop drag-out)
				(sut/set-drag-class :pet "dragging-pet")))

(def brusly [:div#-brusly {:ref (sut/register dnd :pet "brusly")}])
(def tails [:div#-tails {:ref (sut/register dnd :pet "tails")}])
(def cheddar [:div#-brusly {:ref (sut/register dnd :pet "cheddar")}])
(def wasabi [:div#-brusly {:ref (sut/register dnd :pet "wasabi")}])
(def pet-drop [:div#-pet-drop {:ref (sut/register dnd :pet-drop "pet-drop")}])

(defn test-content []
		[:div
			[:div
				[:h1 "Pets"]
				tails
				brusly
				cheddar
				wasabi
				pet-drop
				]])

(describe "Drag and Drop"
		(with-stubs)
		(helper/with-root-dom)
		(before
				(reset! blank-dnd {})
				(helper/render [test-content])
				(helper/flush))

		(around [it] (with-redefs []
																	(log/capture-logs
																			(it))))

		(context "create dnd"
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

				(it "draggables have drag listeners"
						(should= [drag-start] (get-in @dnd [:groups :pet :listeners :drag-start]))
						(should-contain drag-over (get-in @dnd [:groups :pet :listeners :drag-over]))
						(should-contain drag-out (get-in @dnd [:groups :pet :listeners :drag-out]))
						(should-contain drag-end (get-in @dnd [:groups :pet :listeners :drag-end]))
						(should-contain drop! (get-in @dnd [:groups :pet :listeners :drop]))
						(should= "dragging-pet" (get-in @dnd [:groups :pet :drag-class])))

				(it "droppables have drop listeners"
						(should-contain drag-over (get-in @dnd [:groups :pet :listeners :drag-over]))
						(should-contain drag-out (get-in @dnd [:groups :pet :listeners :drag-out]))
						(should-contain drag-over (get-in @dnd [:groups :pet-drop :listeners :drag-over]))
						(should-contain drag-out (get-in @dnd [:groups :pet-drop :listeners :drag-out])))
				)

		(context "register"
				(it "registers an unknown group"
						((sut/register dnd :not-a-pet "a box") [:div#-not-a-pet {:ref (sut/register dnd :not-a-pet "box")}])
						(should= "registering to unknown group: :not-a-pet a box" (log/captured-logs-str))
						(swap! dnd update-in [:groups] dissoc :not-a-pet))

				(it "registers draggables & droppables"
						(let [draggables [:node :draggable-mousedown :draggable-touchstart]
												droppables [:node :droppable-mouseenter :droppable-mouseleave :droppable-touchend]]
								(for [pet (map :id pets)]
										(should= draggables (keys (get-in @dnd [:groups :pet :members pet]))))
								(for [pet (map :id pets)]
										(map #(should= droppables (keys (get-in @dnd [:groups % :members pet]))) [:pet :pet-drop]))
								(should= ["tails" "brusly" "cheddar" "wasabi"] (keys (get-in @dnd [:groups :pet :members])))
								(should= #{:pet :pet-drop} (get-in @dnd [:groups :pet :targets]))
								(should= ["pet-drop"] (keys (get-in @dnd [:groups :pet-drop :members])))))
				)

		(context "draggable-mouse-down"
				#_(it "does nothing if not draggable"
						(with-redefs [wjs/e-left-click? (stub :left-click? {:return true})
																				wjs/e-coordinates (stub :coordinate {:return [0 0 0 0]})]
								(sut/draggable-mouse-down dnd :pet-drop "pet-drop" pet-drop "mouse down on droppable")
								(should-not-contain :maybe-drag @dnd)))

				(it "left mouse-down"
						(with-redefs [wjs/e-left-click? (stub :left-click? {:return false})
																				wjs/e-coordinates (stub :coordinate {:return [0 0 0 0]})]
								(let [node (get-in @dnd [:groups :pet :members "brusly" :node])]
										(sut/draggable-mouse-down dnd :pet "brusly" node "the mouse's right button is down")
										(should-not-contain :maybe-drag @dnd)))
						)

				(it "left mouse-down"
						(helper/mouse-down "#-brusly")
						(with-redefs [wjs/e-left-click? (stub :left-click? {:return true})
																				wjs/e-coordinates (stub :coordinate {:return [0 0 0 0]})]
								(let [node (get-in @dnd [:groups :pet :members "brusly" :node])]
										(sut/draggable-mouse-down dnd :pet "brusly" node "the mouse is down")
										(should-contain :maybe-drag @dnd)
										(should= "brusly" (get-in @dnd [:maybe-drag :member]))))
						)
				)
		)

;; TODO - MDM: Work within scrolling
;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: custom fake hiccup for dragged node - when drag starts use 'this' as fake-hiccup - look at original dnd for this
;; TODO - MDM: Same-list, reordering
;; TODO - MDM: Multiple list, transferring and ordering
