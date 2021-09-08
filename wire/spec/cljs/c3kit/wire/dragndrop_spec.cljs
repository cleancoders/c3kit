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

(defn test-content []
  [:div
   [:div
    [:h1 "Test Page"]
    [demo/rainbow-demo]]])

(def dnd (-> (sut/context)
           (sut/add-group :color)
           (sut/add-group :color-drop)
           (sut/drag-from-to :color :color-drop)
           (sut/on-drag-start :color (fn [dnd] (assoc dnd :on-drag-start true)))
           ;(sut/drag-fake-hiccup-fn :color color-drag-fake-hiccup)
           (sut/on-drop :color-drop (fn [dnd] (assoc dnd :on-drop true)))
           (sut/on-drag-end :color (fn [dnd] (assoc dnd :on-drag-end true)))
           (sut/on-drag-over :color #(println "color drag-over"))
           (sut/on-drag-over :color-drop (fn [dnd] (assoc dnd :on-drag-over true)))
           (sut/on-drag-out :color #(println "color drag-out"))
           (sut/on-drag-out :color-drop (fn [dnd] (assoc dnd :on-drag-out true)))
           (sut/set-drag-class :color "dragging-color")))

(describe "Drag and Drop"
  (helper/with-root-dom)
  (before (helper/render [test-content]
      ))

  (it "registers an unknown group"
    (sut/register dnd :not-a-color :dog)
    (should-be-nil (get-in dnd [:groups :color :members :dog]))
    (should-be-nil (get-in dnd [:groups :not-a-color :members :dog]))
    )

  (it "registers a draggable"
    (sut/register dnd :color :cerulean)
    (should= :cerulean (first (get-in @dnd [:groups :color :members])))
    (should= #{:color-drop} (get-in @dnd [:groups :color :targets]))
    ;(should= :cerulean (get-in @dnd [:groups :color]))
    )


  )


;; TODO - MDM: Work within scrolling
;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: custom fake hiccup for dragged node - when drag starts use 'this' as fake-hiccup - look at original dnd for this
;; TODO - MDM: Same-list, reordering
;; TODO - MDM: Multiple list, transferring and ordering
