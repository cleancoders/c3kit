(ns c3kit.wire.dragndrop-spec
  (:require-macros [c3kit.wire.spec-helperc :refer [should-select]]
                   [speclj.core :refer [around before context describe it
                                        should-contain should-have-invoked should-not-be-nil should-not-contain should-not-have-invoked
                                        should-not= should= stub with-stubs]])
  (:require
    [c3kit.apron.log :as log]
    [c3kit.wire.dragndrop2 :as sut]
    [c3kit.wire.fake-hiccup :as fake-hiccup]
    [c3kit.wire.js :as wjs]
    [c3kit.wire.spec-helper :as helper]
    [reagent.core :as reagent]
    [speclj.stub :as stub]))

(def blank-dnd (sut/context))
(def pets ["brusly" "cheddar"])
(def treats ["bone" "catnip"])
(def state (atom {}))

(defn drag-start [{:keys [source-key]}] (swap! state assoc :last-call :drag-start :source-key source-key) "on-drag-start")
(defn drag-over [{:keys [target-key]}] (swap! state assoc :last-call :drag-over :target-key target-key) "on-drag-over")
(defn drag-out [_] (reset! state {:last-call :drag-out}) "on-drag-out")
(defn drop! [{:keys [target-key]}] (swap! state assoc :drop target-key) "on-drag-drop")
(defn drag-end [_] (swap! state assoc :last-call :drag-end) "on-drag-end")
(defn fake-hiccup [_] [:div {:id "dragging-treat"} "give the dog a bone"])

(def dnd (sut/context))

(defn refresh-dnd [] (-> (sut/context)
                         (sut/add-group :pet)
                         (sut/add-group :pet)
                         (sut/drag-from-to :treat :pet)
                         (sut/on-drag-start :treat drag-start)
                         (sut/on-drop :pet drop!)
                         (sut/on-drag-end :treat drag-end)
                         (sut/on-drag-over :treat drag-over)
                         (sut/on-drag-over :pet drag-over)
                         (sut/on-drag-out :treat drag-out)
                         (sut/on-drag-out :pet drag-out)
                         (sut/set-drag-class :treat "drag-bone")))

(defn test-content []
  [:div
   [:div
    [:h1 "treats"]
    [:ol#-treats
     [:li#-bone.treat {:style nil :ref (sut/register dnd :treat "bone")}]
     [:li#-catnip {:ref (sut/register dnd :treat "catnip")}]
     [:li#-brusly.pet {:ref (sut/register dnd :pet "brusly")}]
     [:li#-cheddar.pet {:ref (sut/register dnd :pet "cheddar")}]
     ]]])

(def draggers (reagent/atom nil))
(def bone (reagent/track #(when @draggers (get-in @draggers ["bone"]))))
(def catnip (reagent/track #(when @draggers (get-in @draggers ["catnip"]))))
(def bone-node (reagent/track #(when @bone (:node @bone))))
(def catnip-node (reagent/track #(when @bone (:node @catnip))))
(def droppers (reagent/atom nil))
(def brusly (reagent/track #(when @droppers (get-in @droppers ["brusly"]))))
(def brusly-node (reagent/track #(when @brusly (:node @brusly))))

(defn get-doc-listeners [kind]
  (->> (stub/invocations-of kind)
       (filter #(= (wjs/document @bone-node) (first %)))
       (map #(second %))))

(defn get-listeners [kind] (map #(second %) (stub/invocations-of kind)))

(describe "Drag and Drop"
  (with-stubs)
  (helper/with-root-dom)
  (before (reset! blank-dnd {}))
  (around [it] (with-redefs [wjs/add-listener (stub :add-listener)
                             wjs/remove-listener (stub :remove-listener)
                             wjs/nod (stub :nod)]
                 (log/capture-logs (it))))

  (context "invalid dnd"
    (it "registers an unknown group"
      ((sut/register dnd :a-box "is not a group") "a node")
      (should-not-have-invoked :add-listener)
      (should= "registering to unknown group: :a-box is not a group" (log/captured-logs-str))
      (swap! dnd update-in [:groups] dissoc :not-a-pet)))

  (context "valid dnd"
    (it "add-group"
      (sut/add-group blank-dnd :pet)
      (should= [:pet] (keys (get-in @blank-dnd [:groups]))))

    (it "drags from and to"
      (sut/add-group blank-dnd :pet)
      (sut/add-group blank-dnd :treat)
      (sut/drag-from-to blank-dnd :treat :pet)
      (should= #{:pet} (get-in @blank-dnd [:groups :treat :targets])))

    (it "sets drag class"
      (sut/set-drag-class blank-dnd :treat "pet")
      (should= "pet" (get-in @blank-dnd [:groups :treat :drag-class])))

    (it "has fake hiccup fn"
      (sut/add-group blank-dnd :pet)
      (sut/add-group blank-dnd :treat)
      (sut/drag-from-to blank-dnd :treat :pet)
      (sut/drag-fake-hiccup-fn blank-dnd :treat fake-hiccup)
      (should-not= nil (get-in @blank-dnd [:groups :treat :hiccup])))

    (context "dnd behaviors"
      (before
        (reset! dnd @(refresh-dnd))
        (helper/render [test-content]))

      (it "structure"
        (should-select "#-bone")
        (should-select "#-catnip")
        (should-select "#-brusly"))

      (context "dnd registration"
        (it "draggables have drag listeners"
          (let [registrations      (stub/invocations-of :add-listener)
                registration-types (map #(second %) registrations)]
            (should-contain "mousedown" registration-types)
            (should-contain drag-start (get-in @dnd [:groups :treat :listeners :drag-start]))
            (should-contain drag-over (get-in @dnd [:groups :treat :listeners :drag-over]))
            (should-contain drag-out (get-in @dnd [:groups :treat :listeners :drag-out]))
            (should-contain drag-end (get-in @dnd [:groups :treat :listeners :drag-end]))
            (should= "drag-bone" (get-in @dnd [:groups :treat :drag-class]))))

        (it "droppables have drop listeners"
          (let [registrations      (stub/invocations-of :add-listener)
                registration-types (map #(second %) registrations)]
            (should-contain "mouseenter" registration-types)
            (should-contain "mouseleave" registration-types)
            (should-contain drag-out (get-in @dnd [:groups :treat :listeners :drag-out]))
            (should-contain drag-over (get-in @dnd [:groups :pet :listeners :drag-over]))
            (should-contain drag-out (get-in @dnd [:groups :pet :listeners :drag-out]))
            (should-contain drop! (get-in @dnd [:groups :pet :listeners :drop]))))

        (it "registers draggables & droppables"
          (let [draggables [:node :draggable-mousedown]
                droppables [:node :droppable-mouseenter :droppable-mouseleave]]
            (should-have-invoked :add-listener {:times 6})
            (should= treats (keys (get-in @dnd [:groups :treat :members])))
            (should= #{:pet} (get-in @dnd [:groups :treat :targets]))
            (should= pets (keys (get-in @dnd [:groups :pet :members])))
            (doseq [treat treats]
              (should= draggables (keys (get-in @dnd [:groups :treat :members treat]))))
            (doseq [pet pets]
              (should= droppables (keys (get-in @dnd [:groups :pet :members pet]))))
            ))
        )
      (context "dnd actions"
        (before (reset! draggers (get-in @dnd [:groups :treat :members]))
                (reset! droppers (get-in @dnd [:groups :pet :members])))

        (context "draggable-mouse-down"
          (it "right mouse-down"
            (let [cnt-add-listeners (count (stub/invocations-of :add-listener))
                  mousedown         (:draggable-mousedown @bone)]
              (mousedown (clj->js {:button 1}))
              (should= 0 (- (count (stub/invocations-of :add-listener)) cnt-add-listeners))
              (should-not-contain :maybe-drag @dnd)))

          (it "left mouse-down"
            (let [before-listeners (get-listeners :add-listener)
                  mousedown        (:draggable-mousedown @bone)
                  _                (mousedown (clj->js {:button 0 :clientX 0 :clientY 0}))
                  after-listeners  (get-listeners :add-listener)]
              (should= 3 (- (count after-listeners) (count before-listeners)))
              (should-contain "mousemove" after-listeners)
              (should-contain "mouseout" after-listeners)
              (should-contain "mouseup" after-listeners)
              (should-contain :maybe-drag @dnd)
              (should= "bone" (get-in @dnd [:maybe-drag :member]))
              (should= [0 0] (get-in @dnd [:maybe-drag :start-position]))))
          )

        (context "dragging"
          (before
            (let [mousedown (:draggable-mousedown @bone)]
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
              (with-redefs [sut/-start-drag (stub :start-drag)]
                (let [mousemove-handler (get-in @dnd [:maybe-drag :move-listener])]
                  (mousemove-handler (clj->js {:clientX 2 :clientY 2}))
                  (should-not-have-invoked :start-drag)
                  (should-contain :maybe-drag @dnd))))

            (it "valid movement - above threshold"
              (with-redefs [sut/-start-drag (stub :start-drag)]
                (let [mousemove-handler (get-in @dnd [:maybe-drag :move-listener])]
                  (mousemove-handler (clj->js {:clientX 10 :clientY 10}))
                  (should-have-invoked :start-drag)
                  (should-have-invoked :nod)
                  (should-not-contain :maybe-drag @dnd))))

            (it "valid movement - mouseout"
              (with-redefs [sut/-start-drag (stub :start-drag)]
                (let [mousemove-handler (get-in @dnd [:maybe-drag :move-listener])]
                  (mousemove-handler (clj->js {:type "mouseout" :target @bone-node}))
                  (should-have-invoked :nod)
                  (should-have-invoked :start-drag)
                  (should-not-contain :maybe-drag @dnd))))
            )

          (it "not start-drag with false dispatch event"
            (with-redefs [sut/-dispatch-event (stub :dispatch {:return false})]
              (sut/-start-drag dnd :treat "bone" @bone-node (clj->js {}))
              (should-not-contain :active-drag @dnd)
              (should-not-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))

          (it "start drag"
            (with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
              (sut/-start-drag dnd :treat "bone" @bone-node (clj->js {:target @bone-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}))
              (let [doc-listeners (get-doc-listeners :add-listener)
                    node          (get-in @dnd [:active-drag :drag-node])
                    node-style    (wjs/node-style node)]
                (should-contain :active-drag @dnd)
                (should= 3 (count doc-listeners))
                (should-contain "mousemove" doc-listeners)
                (should= [-10 -10] (get-in @dnd [:active-drag :offset]))
                (should= "10px" (wjs/o-get node-style "left"))
                (should= "10px" (wjs/o-get node-style "top"))
                (should= :drag-start (:last-call @state))
                (should= "bone" (:source-key @state))
                (should= "_dragndrop-drag-node_" (helper/id node))
                (should-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))
                (should= "_dragndrop-drag-node_" (last (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))))

          (context "drag node"
            (it "is cloned when no hiccup-fn"
              (with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})
                            fake-hiccup/->dom (stub :fake-hiccup-dom)]
                (sut/-start-drag dnd :treat "bone" @bone-node (clj->js {:target @bone-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}))
                (let [node (get-in @dnd [:active-drag :drag-node])]
                  (should-not-have-invoked :fake-hiccup-dom)
                  (should-not= "give the dog a bone" (helper/html node)))))

            (it "is created when hiccup-fn - when node has classes"
              (with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
                (sut/drag-fake-hiccup-fn dnd :treat fake-hiccup)
                (should-not-be-nil (get-in @dnd [:groups :treat :hiccup]))
                (sut/-start-drag dnd :treat "bone" @bone-node (clj->js {:target @bone-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}))
                (let [node (get-in @dnd [:active-drag :drag-node])]
                  (should= "_dragndrop-drag-node_" (helper/id node))
                  (should-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))
                  (should-contain "drag-bone" (clojure.string/split (wjs/node-classes node) #" "))
                  (should-contain "treat" (clojure.string/split (wjs/node-classes node) #" "))
                  (should= "give the dog a bone" (helper/html node)))))

            (it "is created when hiccup-fn - when node has no classes"
              (with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
                (sut/drag-fake-hiccup-fn dnd :treat fake-hiccup)
                (should-not-be-nil (get-in @dnd [:groups :treat :hiccup]))
                (sut/-start-drag dnd :treat "catnip" @catnip-node (clj->js {:target @catnip-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}))
                (let [node (get-in @dnd [:active-drag :drag-node])]
                  (should= "_dragndrop-drag-node_" (helper/id node))
                  (should-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))
                  (should-contain "drag-bone" (clojure.string/split (wjs/node-classes node) #" "))
                  (should-not-contain "treat" (clojure.string/split (wjs/node-classes node) #" "))
                  (should= "give the dog a bone" (helper/html node))
                  )))
            )

          (it "mouse-enter no active drag"
            (let [mouseenter (:droppable-mouseenter @brusly)]
              (mouseenter (clj->js {}))
              (should= nil (get-in @dnd [:active-drag :drop-target]))
              ))

          (it "mouse-leave no active drag"
            (let [mouseleave (:droppable-mouseleave @brusly)]
              (should= nil (mouseleave (clj->js {})))))

          (context "active-drag"
            (before (with-redefs [wjs/node-bounds (stub :bounds {:return [10 10]})]
                      (let [mousemove (get-in @dnd [:maybe-drag :move-listener])]
                        (mousemove (clj->js {:type "mouseout" :target @bone-node :scrollX 0 :scrollY 0}))
                        )))

            (it "handles the drag"
              (let [drag-handler (get-in @dnd [:active-drag :drag-listener])]
                (drag-handler (clj->js {:target @bone-node :clientX 10 :clientY 15}))
                (let [node-style (wjs/node-style (get-in @dnd [:active-drag :drag-node]))]
                  (should= [-10 -10] (get-in @dnd [:active-drag :offset]))
                  (should= "20px" (wjs/o-get node-style "left"))
                  (should= "25px" (wjs/o-get node-style "top")))))

            (it "droppable-mouse-enter"
              (let [mouseenter (:droppable-mouseenter @brusly)]
                (mouseenter (clj->js {}))
                (should= [:pet "brusly" @brusly-node] (get-in @dnd [:active-drag :drop-target]))
                (should= :drag-over (:last-call @state))
                (should= "brusly" (:target-key @state))))

            (it "droppable-mouse-leave"
              (let [mouseenter (:droppable-mouseenter @brusly)
                    mouseleave (:droppable-mouseleave @brusly)]
                (mouseenter (clj->js {}))
                (mouseleave (clj->js {}))
                (should= :drag-out (:last-call @state))
                (should= nil (:target-key @state))))

            (it "end-drag - no target"
              (let [mouseup (get-in @dnd [:active-drag :end-listener])]
                (mouseup (clj->js {}))
                (should= nil (:drop! @state))))

            (it "end-drag - with target"
              (let [mouseenter           (:droppable-mouseenter @brusly)
                    mouseup              (get-in @dnd [:active-drag :end-listener])
                    rmv-listeners-before (get-doc-listeners :remove-listener)]
                (mouseenter (clj->js {}))
                (mouseup (clj->js {}))
                (should= 2 (- (count (get-doc-listeners :remove-listener)) (count rmv-listeners-before)))
                (should= "brusly" (:drop @state))
                (should= :drag-end (:last-call @state))
                (should-not-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))
            )
          )
        )
      )
    )
  )

;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: Warning on configuring groups that don't exist
;; TODO - MDM: Warning on register for duplicate keys (or missing keys on deregister)
;; TODO - MDM: configurable drag-threshold
