(ns c3kit.wire.dragndrop-spec
		(:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
																																								should-not= should-have-invoked after before with-stubs with around
																																								should-contain should-not-contain should should-not-have-invoked stub]]
																			[c3kit.wire.spec-helperc :refer [should-select should-not-select]])
		(:require
			[c3kit.wire.dragndrop2 :as sut]
			[c3kit.wire.spec-helper :as helper]
			[c3kit.apron.log :as log]
			[c3kit.wire.dnd-demo :as demo]
			))


(def blank-dnd (sut/context))
(def colors [{:id "Tails" :name "Tails"} {:id "Brusly" :name "Brusly"} {:id "Cheddar" :name "Cheddar"}])

(def dnd (-> (sut/context)
											(sut/add-group :type)
											(sut/add-group :pet)
											(sut/drag-from-to :type :pet)
											(sut/on-drag-start :type (fn [] "on-drag-start"))
											(sut/on-drop :pet (fn [] "on-drop"))
											(sut/on-drag-end :type (fn [] "on-drag-end"))
											(sut/on-drag-over :type #(println "breed drag-over"))
											(sut/on-drag-over :pet (fn [] "on-drag-over"))
											(sut/on-drag-out :type #(println "breed drag-out"))
											(sut/on-drag-out :pet (fn [] "on-drag-out"))
											(sut/set-drag-class :type "dragging-breed")))

(defn test-content []
		[:div
			[:div
				[:h1 "Pets"]
				[:div#-tails {:ref (sut/register dnd :pet "Tails")}]
				[:div#-brusly {:ref (sut/register dnd :pet "Brusly")}]
				[:div#-cheddar {:ref (sut/register dnd :pet "Cheddar")}]
				[:div#-rottie {:ref (sut/register dnd :type "Rottweiler")}]
				[:div#-cat {:ref (sut/register dnd :type "Cat")}]
				]])

(describe "Drag and Drop"
		(helper/with-root-dom)
		(before (helper/render [test-content])
				(helper/flush))

		(before (reset! blank-dnd {}))

		(around [it] (with-redefs []
																	(log/capture-logs
																			(it))))

		(it "add-group"
				(sut/add-group blank-dnd :pet)
				(should= [:pet] (keys (get-in @blank-dnd [:groups]))))

		(it "drags from and to"
				(sut/add-group blank-dnd :type)
				(sut/add-group blank-dnd :pet)
				(sut/drag-from-to blank-dnd :type :pet)
				(should= #{:pet} (get-in @blank-dnd [:groups :type :targets])))

		(it "listeners"
				(let [drag-start-listener (first (get-in @dnd [:groups :type :listeners :drag-start]))
										drop-listener       (first (get-in @dnd [:groups :pet :listeners :drop]))
										drag-over-listener  (first (get-in @dnd [:groups :pet :listeners :drag-over]))
										drag-out-listener   (first (get-in @dnd [:groups :pet :listeners :drag-out]))
										drag-end-listener   (first (get-in @dnd [:groups :type :listeners :drag-end]))]
						(should= "on-drag-start" (drag-start-listener))
						(should= "on-drop" (drop-listener))
						(should= "on-drag-over" (drag-over-listener))
						(should= "on-drag-out" (drag-out-listener))
						(should= "on-drag-end" (drag-end-listener))))

		(it "sets drag class"
				(sut/set-drag-class blank-dnd :type "color")
				(should= "color" (get-in @blank-dnd [:groups :type :drag-class])))

		(it "registers an unknown group"
				((sut/register dnd :not-a-pet "blue") [:div#-not-a-pet {:ref (sut/register dnd :not-a-pet "blue")}])
				(should= "registering to unknown group: :not-a-pet blue" (log/captured-logs-str)))

		(it "registers draggables & droppables"
				(let [draggables [:node :draggable-mousedown :draggable-touchstart]
										droppables [:node :droppable-mouseenter :droppable-mouseleave :droppable-touchend]]
						(for [color (map :id colors)]
								(should= draggables (keys (get-in @dnd [:groups :type :members color]))))
						(for [color (map :id colors)]
								(should= droppables (keys (get-in @dnd [:groups :pet :members color]))))
						(should= ["Tails" "Brusly" "Cheddar"] (keys (get-in @dnd [:groups :pet :members])))
						(should= #{:pet} (get-in @dnd [:groups :type :targets]))
						(should= ["Rottweiler" "Cat"] (keys (get-in @dnd [:groups :type :members])))))

		(it "draggables have drag listeners"
				(let [listeners (get-in @dnd [:groups :type :listeners])]
						(should-contain :drag-start listeners)
						(should-contain :drag-over listeners)
						(should-contain :drag-out listeners)
						(should-contain :drag-end listeners)))

		(it "droppables have drop listeners"
				(let [listeners (get-in @dnd [:groups :pet :listeners])]
						(should-contain :drop listeners)
						(should-contain :drag-over listeners)
						(should-contain :drag-out listeners)))
		)




;; TODO - MDM: Work within scrolling
;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: custom fake hiccup for dragged node - when drag starts use 'this' as fake-hiccup - look at original dnd for this
;; TODO - MDM: Same-list, reordering
;; TODO - MDM: Multiple list, transferring and ordering
