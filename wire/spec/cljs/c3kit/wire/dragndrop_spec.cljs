(ns c3kit.wire.dragndrop-spec
  (:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before with-stubs with around
                                        should-contain should-not-contain should should-not-have-invoked stub]]
                   [c3kit.wire.spec-helperc :refer [should-select should-not-select]])
  (:require
    [c3kit.wire.dragndrop :as sut]
    [c3kit.wire.spec-helper :as helper]
    ))

(describe "Drag and Drop"



  )


;; TODO - MDM: Work within scrolling
;; TODO - MDM: Add touch listeners so it works on mobile
;; TODO - MDM: custom fake hiccup for dragged node - when drag starts use 'this' as fake-hiccup - look at original dnd for this
;; TODO - MDM: Same-list, reordering
;; TODO - MDM: Multiple list, transferring and ordering
