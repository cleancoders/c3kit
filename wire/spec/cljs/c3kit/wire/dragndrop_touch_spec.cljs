(ns c3kit.wire.dragndrop-touch-spec
  (:require-macros [speclj.core :refer [describe context xit it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before with-stubs with around
                                        should-contain should-not-contain should should-not-have-invoked stub]]
                   [c3kit.wire.spec-helperc :refer [should-select should-not-select]])
  (:require
   [c3kit.wire.dragndrop2 :as sut]
   [c3kit.wire.spec-helper :as helper]
   [c3kit.apron.log :as log]
   [c3kit.wire.js :as wjs]
   [reagent.core :as reagent]
   [speclj.stub :as stub]))

(def blank-dnd (sut/context))
(def pets ["brusly" "cheddar"])
(def treats ["bone" "catnip"])
(def state (atom {}))

(defn drag-start [{:keys [source-key]}] (swap! state assoc :last-call :drag-start :source-key source-key) "on-drag-start")
(defn touch-start [{:keys [source-key]}] (swap! state assoc :last-call :touch-start :source-key source-key) "on-touch-start")
(defn drag-over [{:keys [target-key]}] (swap! state assoc :last-call :drag-over :target-key target-key) "on-drag-over")
(defn drag-out [_] (reset! state {:last-call :drag-out}) "on-drag-out")
(defn drop! [{:keys [target-key]}] (swap! state assoc :drop target-key) "on-drag-drop")
(defn drag-end [_] (swap! state assoc :last-call :drag-end) "on-drag-end")
(defn touch-end [_] (swap! state assoc :last-call :touch-end) "on-touch-end")

(defn fake-hiccup [node] [:div {:id "dragging-treat"} "give the dog a bone"])

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
     [:li#-bone.treat {:ref (sut/register dnd :treat "bone") :style {:left 0 :top 0 :width 100 :height 50}}]
     [:li#-catnip {:ref (sut/register dnd :treat "catnip") :style {:left 0 :top 50 :width 100 :height 50}}]
     [:li#-brusly.pet {:ref (sut/register dnd :pet "brusly") :style {:left 0 :top 150 :width 100 :height 50}}]
     [:li#-cheddar.pet {:ref (sut/register dnd :pet "cheddar") :style {:left 0 :top 200 :width 100 :height 50}}]
     ]]])

(def draggers (reagent/atom nil))
(def bone (reagent/track #(when @draggers (get-in @draggers ["bone"]))))
(def catnip (reagent/track #(when @draggers (get-in @draggers ["catnip"]))))
(def bone-node (reagent/track #(when @bone (:node @bone))))
(def catnip-node (reagent/track #(when @bone (:node @catnip))))
(def droppers (reagent/atom nil))
(def brusly (reagent/track #(when @droppers (get-in @droppers ["brusly"]))))
(def cheddar (reagent/track #(when @droppers (get-in @droppers ["cheddar"]))))
(def brusly-node (reagent/track #(when @brusly (:node @brusly))))
(def cheddar-node (reagent/track #(when @brusly (:node @cheddar))))

(defn get-doc-listeners [kind]
  (->> (stub/invocations-of kind)
    (filter #(= (wjs/document @bone-node) (first %)))
    (map #(second %))))

(defn get-listeners [kind] (map #(second %) (stub/invocations-of kind)))

(describe "MOBILE TOUCH Drag and Drop"
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
      (sut/drag-from-to blank-dnd :treat :pet)
      (should= #{:pet} (get-in @blank-dnd [:groups :treat :targets])))

    (it "sets drag class"
      (sut/set-drag-class blank-dnd :treat "pet")
      (should= "pet" (get-in @blank-dnd [:groups :treat :drag-class])))

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
                registration-types ["mousedown" "mouseenter" "mouseleave" "touchstart" "touchend"]
                resulting-registration-types (map #(second %) registrations)]
            (doseq [type registration-types]
              (should-contain type resulting-registration-types))
            (should-contain drag-start (get-in @dnd [:groups :treat :listeners :drag-start]))
            (should-contain drag-over (get-in @dnd [:groups :treat :listeners :drag-over]))
            (should-contain drag-out (get-in @dnd [:groups :treat :listeners :drag-out]))
            (should-contain drag-end (get-in @dnd [:groups :treat :listeners :drag-end]))
            (should= "drag-bone" (get-in @dnd [:groups :treat :drag-class]))))

        (it "droppables have drop listeners"
          (let [registrations      (stub/invocations-of :add-listener)
                registration-types ["mouseenter" "mouseleave" "touchstart" "touchend" "touchmove"]
                resulting-registration-types (map #(second %) registrations)]
            (doseq [type registration-types]
              (should-contain type resulting-registration-types))
            (should-contain drag-out (get-in @dnd [:groups :treat :listeners :drag-out]))
            (should-contain drag-over (get-in @dnd [:groups :pet :listeners :drag-over]))
            (should-contain drag-out (get-in @dnd [:groups :pet :listeners :drag-out]))
            (should-contain drop! (get-in @dnd [:groups :pet :listeners :drop]))))

        (it "registers draggables & droppables"
          (let [draggables [:node :draggable-mousedown :draggable-touchstart]
                droppables [:node :droppable-mouseenter :droppable-mouseleave :droppable-touchenter :droppable-touchleave :droppable-touchend]]
            (should-have-invoked :add-listener {:times 14})
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

        (context "mobile touch actions"
          (context "touchstart"
            (it "more than single touch"
              (let [cnt-add-listeners (count (stub/invocations-of :add-listener))
                    touchstart       (:draggable-touchstart @catnip)]
                (touchstart (clj->js {:touches [{:clientX 0 :clientY 0} {:clientX 10 :clientY 10}]}))
                (should= 0 (- (count (stub/invocations-of :add-listener)) cnt-add-listeners))
                (should-not-contain :maybe-drag @dnd)
                (should= nil (:kind @dnd))))

            (it "single touch"
              (let [before-listeners (get-listeners :add-listener)
                    touchstart       (:draggable-touchstart @catnip)
                    _                (touchstart (clj->js {:touches [{:clientX 0 :clientY 0}]}))
                    after-listeners  (get-listeners :add-listener)]
                (should= 2 (- (count after-listeners) (count before-listeners)))
                (should-contain "touchmove" after-listeners)
                (should-contain :maybe-drag @dnd)
                (should= :touch (:kind @dnd))
                (should= "catnip" (get-in @dnd [:maybe-drag :member]))
                (should= [0 0] (get-in @dnd [:maybe-drag :start-position]))))
            )
          (context "dragging"
            (before
              (let [touchstart (:draggable-touchstart @catnip)]
                (touchstart (clj->js {:touches (clj->js [{:clientX 0 :clientY 0}])}))))

            (context "maybe drag"
              (it "touchend with no valid movement"
                (let [remove-listeners-before (get-listeners :remove-listener)
                      touchend                 (get-in @dnd [:maybe-drag :touchend-listener])
                      _                       (touchend {})
                      remove-listeners-after  (get-listeners :remove-listener)]
                  (should-not-contain :maybe-drag @dnd)
                  (should= 1 (- (count remove-listeners-after) (count remove-listeners-before)))
                  (should-contain "touchend" remove-listeners-after)))

              (it "movement below threshold"
                (with-redefs [sut/start-drag (stub :start-drag)]
                  (let [touchmove-handler (get-in @dnd [:maybe-drag :touchmove-listener])]
                    (touchmove-handler (clj->js {:touches (clj->js [{:clientX 2 :clientY 2}])}))
                    (should-not-have-invoked :start-drag)
                    (should-contain :maybe-drag @dnd))))

              (it "invalid move - added touch"
                (with-redefs [sut/start-drag (stub :start-drag)]
                  (let [touchmove-handler (get-in @dnd [:maybe-drag :touchmove-listener])]
                    (touchmove-handler (clj->js {:touches (clj->js [{:clientX 10 :clientY 10} {:clientX 11 :clientY 10}])}))
                    (should-not-have-invoked :start-drag)
                    (should-contain :maybe-drag @dnd))))

              (it "valid movement - above threshold"
                (with-redefs [sut/start-drag (stub :start-drag)]
                  (let [touchmove-handler (get-in @dnd [:maybe-drag :touchmove-listener])]
                    (touchmove-handler (clj->js {:changedTouches (clj->js [{:clientX 2 :clientY 10}])}))
                    (should-have-invoked :start-drag)
                    (should-have-invoked :nod)
                    (should-not-contain :maybe-drag @dnd))))
              )

            (it "not start-drag with false dispatch event"
              (with-redefs [sut/dispatch-event (stub :dispatch {:return false})]
                (sut/start-drag dnd :treat "catnip" @catnip-node [sut/handle-touch-drag sut/end-touch-drag] (clj->js {}))
                (should-not-contain :active-drag @dnd)
                (should-not-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))

            (it "start drag"
              (sut/start-drag dnd :treat "catnip" @catnip-node [sut/handle-touch-drag sut/end-touch-drag] (clj->js {:changedTouches (clj->js [{:target @catnip-node :clientX 0 :clientY 0 :scrollX 10 :scrollY 10}])}))
                (let [doc-listeners (get-doc-listeners :add-listener)
                      node-style    (wjs/node-style (get-in @dnd [:active-drag :drag-node]))]
                  (should-contain :active-drag @dnd)
                  (should= 5 (count doc-listeners))
                  (should-contain "touchmove" doc-listeners)
                  ;(should= [-48 -74.875] (get-in @dnd [:active-drag :offset]))
                  ;(should= "10px" (wjs/o-get node-style "left"))
                  ;(should= "10px" (wjs/o-get node-style "top"))
                  (should= :drag-start (:last-call @state))
                  (should= "catnip" (:source-key @state))
                  (should-contain "_dragndrop-drag-node_" (map #(helper/id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))

            (it "touchenter no active drag"
              (let [touchmove (:droppable-touchenter @cheddar)]
                (touchmove (clj->js {}))
                (should= nil (get-in @dnd [:active-drag :drop-target]))
                ))

            (it "touchleave no active drag"
              (let [touchleave (:droppable-touchleave @cheddar)]
                (should= nil (touchleave (clj->js {})))))

            (context "active-drag"
              (before (let [touchmove (get-in @dnd [:maybe-drag :touchmove-listener])]
                        (touchmove (clj->js {:changedTouches (clj->js [{:target @catnip-node :clientX 10 :clientY 15}])}))
                        ))

              (it "handles the drag"
                (let [drag-handler (get-in @dnd [:active-drag :drag-listener])]
                  (drag-handler (clj->js {:changedTouches (clj->js [{:target @catnip-node :clientX 10 :clientY 15}])})
                  (let [node-style (wjs/node-style (get-in @dnd [:active-drag :drag-node]))]
                    ;(should= [-10 -10] (get-in @dnd [:active-drag :offset]))
                    #_(should= "20px" (wjs/o-get node-style "left"))
                    #_(should= "25px" (wjs/o-get node-style "top"))))))

              (context "drag over & out using touchmove listener"
                (it "droppable-touchenter - not in a droppable node"
                  (let [touchenter (:droppable-touchenter @cheddar)]
                    (touchenter (clj->js {:changedTouches (clj->js [{:target @catnip-node :clientX 10 :clientY 15}])}))
                    (should= nil (get-in @dnd [:active-drag :drop-target]))
                    (should= :drag-start (:last-call @state))
                    (should= nil (:target-key @state))))

                (it "droppable-touchenter - do enter!"
                (let [touchenter (:droppable-touchenter @cheddar)]
                  (touchenter (clj->js {:changedTouches (clj->js [{:target @catnip-node :clientX 50 :clientY 225}])}))
                  (should= [:pet "cheddar" @cheddar-node] (get-in @dnd [:active-drag :drop-target]))
                  (should= :drag-over (:last-call @state))
                  (should= "cheddar" (:target-key @state))))

              #_(it "droppable-touch-leave - touchmove listener"
                (let [touchenter (:droppable-mouseenter @cheddar)
                      touchleave (:droppable-mouseleave @cheddar)]
                  (touchenter (clj->js {}))
                  (touchleave (clj->js {}))
                  (should= :drag-out (:last-call @state))
                  (should= nil (:target-key @state))))
               )

              ;(it "end-drag - no target"
              ;  (let [mouseup (get-in @dnd [:active-drag :end-listener])]
              ;    (mouseup (clj->js {}))
              ;    (should= nil (:drop! @state))))
              ;
              ;(it "end-drag - with target"
              ;  (let [mouseenter           (:droppable-mouseenter @cheddar)
              ;        mouseup              (get-in @dnd [:active-drag :end-listener])
              ;        rmv-listeners-before (get-doc-listeners :remove-listener)]
              ;    (mouseenter (clj->js {}))
              ;    (mouseup (clj->js {}))
              ;    (should= 4 (- (count (get-doc-listeners :remove-listener)) (count rmv-listeners-before)))
              ;    (should= "cheddar" (:drop @state))
              ;    (should= :drag-end (:last-call @state))
              ;    (should-not-contain "_dragndrop-drag-node_" (map #(.-id %) (wjs/node-children (wjs/doc-body (wjs/document)))))))
              )
            )
          )
        )
      )
    )
  )

;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: custom fake hiccup for dragged node - when drag starts use 'this' as fake-hiccup - look at original dnd for this
