(ns c3kit.wire.spec-helper
  (:refer-clojure :exclude [flush])
  (:require-macros [speclj.core :refer [around before should-have-invoked should= stub with-stubs]])
  (:require
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.log :as log]
    [c3kit.wire.ajax :as ajax]
    [c3kit.wire.js :as wjs]
    [c3kit.wire.websocket :as ws]
    [cljs.pprint :as pp]
    [cljsjs.react.dom.test-utils]                           ;; Brings in js/ReactTestUtils
    [clojure.string :as str]
    [reagent.core :as reagent]
    [reagent.dom :as dom]
    [speclj.core]
    [speclj.stub :as stub]
    ))

(log/warn!)

(def pprint pp/pprint)

(def ^:private render-roots (atom []))

(defn- unmount-render-roots []
  (doseq [root @render-roots]
    (dom/unmount-component-at-node root))
  (reset! render-roots []))

(defn reset-dom! [content]
  (let [body (.-body js/document)]
    (unmount-render-roots)
    (set! (.-innerHTML body) content)))

(defn with-clean-dom
  ([] (with-clean-dom []))
  ([content] (before (reset-dom! content))))

(defn with-root-dom [] (with-clean-dom "<div id='root'/>"))

(defn select
  ([selector] (select js/document selector))
  ([root selector]
   (assert root (str "select: can't select inside nil nodes. " selector))
   (assert (string? selector) (str "select: selector must be a string!: " selector))
   (.querySelector root selector)))

(defn select-all
  ([sel] (select-all js/document sel))
  ([root selector]
   (assert root (str "select-all: can't select inside nil nodes. " selector))
   (assert (string? selector) (str "select-all: selector must be a string!: " selector))
   (let [results (.querySelectorAll root selector)
         slice   #(.call js/Array.prototype.slice %)]
     (into [] (slice results)))))

(defn render
  "Use me to render components for testing.  Using reagent/render directly may work, but is not as good."
  ([component] (render component (select "#root")))
  ([component root]
   (swap! render-roots conj root)

	 ;; TODO - MDM: This should be removed.
   (set! (.-Slider js/window) (clj->js {:default (fn [] nil)}))
   (set! js/jwplayer (fn [elem_id] (clj->js {:setup  ccc/noop
                                             :remove ccc/noop})))
   (try
     (dom/render component root)
     (catch :default e (throw (ex-info "Render Error" {:message e}))))))

(defn flush [] (reagent/flush))

(def simulator (.-Simulate js/ReactTestUtils))

(defn- resolve-node
  ([action thing]
   (if (string? thing)
     (if-let [node (select thing)]
       node
       (throw (ex-info (str action " - can't find node: " thing) {:action action :thing thing})))
     (if thing
       thing
       (throw (ex-info (str action " - node is nil") {:action action :thing thing})))))
  ([action root selector]
   (when (nil? root)
     (throw (ex-info (str action " - root node is nil") {:action action :root root :selector selector})))
   (if-let [node (select root selector)]
     node
     (throw (ex-info (str action " - can't find child node: " selector) {:action action :root root :selector selector})))))

; https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/keyCode
(def keypresses
  {wjs/ESC   (clj->js {:keyCode 27 :key "Escape"})
   wjs/TAB   (clj->js {:keyCode 9, :key "Tab"})
   wjs/ENTER (clj->js {:keyCode 13 :key "Enter"})
   wjs/LEFT  (clj->js {:keyCode 37 :key "ArrowLeft"})
   wjs/UP    (clj->js {:keyCode 38 :key "ArrowUp"})
   wjs/RIGHT (clj->js {:keyCode 39 :key "ArrowRight"})
   wjs/DOWN  (clj->js {:keyCode 40 :key "ArrowDown"})})

(defn key-down
  ([thing key-code]
   ((.-keyDown simulator)
    (resolve-node :key-down thing)
    (get keypresses key-code (clj->js {:keyCode key-code :key (str key-code)}))))
  ([root selector key-code]
   (key-down (resolve-node :key-down root selector) key-code)))

(defn key-down!
  ([thing key-code] (key-down thing key-code) (flush))
  ([root selector key-code] (key-down root selector key-code) (flush)))

(defn key-up
  ([thing key]
   ((.-keyUp simulator) (resolve-node :key-up thing) (get keypresses key)))
  ([root selector key]
   (key-up (resolve-node :key-up root selector) key)))

(defn key-up!
  ([thing key] (key-up thing key) (flush))
  ([root selector key] (key-up root selector key) (flush)))

(defn key-press
  ([thing key]
   ((.-keyPress simulator) (resolve-node :key-press thing) (get keypresses key)))
  ([root selector key]
   (key-press (resolve-node :key-press root selector) key)))

(defn key-press!
  ([thing key] (key-press thing key) (flush))
  ([root selector key] (key-press root selector key) (flush)))

(defn touch-end
  ([thing]
   ((.-touchend simulator) (resolve-node :touchend thing)))
  ([root selector]
   (touch-end (resolve-node :touchend root selector))))

(defn touch-end!
  ([thing] (touch-end thing) (flush))
  ([root selector] (touch-end root selector) (flush)))

(defn touch-start
  ([thing]
   ((.-touchstart simulator) (resolve-node :touchstart thing)))
  ([root selector]
   (touch-start (resolve-node :touchstart root selector))))

(defn touch-start!
  ([thing] (touch-start thing) (flush))
  ([root selector] (touch-start root selector) (flush)))

(defn click
  ([thing]
   ((.-click simulator) (resolve-node :click thing)))
  ([root selector]
   (click (resolve-node :click root selector))))

(defn click!
  ([thing] (click thing) (flush))
  ([root selector] (click root selector) (flush)))

(defn mouse-enter
  ([thing]
   ((.-mouseEnter simulator) (resolve-node :mouse-enter thing)))
  ([root selector]
   (mouse-enter (resolve-node :mouse-enter root selector))))

(defn mouse-enter!
  ([thing] (mouse-enter thing) (flush))
  ([root selector] (mouse-enter root selector) (flush)))

(defn mouse-up
  ([thing]
   ((.-mouseUp simulator) (resolve-node :mouse-up thing)))
  ([root selector]
   (mouse-up (resolve-node :mouse-up root selector))))

(defn mouse-up!
  ([thing] (mouse-up thing) (flush))
  ([root selector] (mouse-up root selector) (flush)))

(defn mouse-move
  ([thing]
   ((.-mouseMove simulator) (resolve-node :mouse-move thing)))
  ([root selector]
   (mouse-move (resolve-node :mouse-move root selector))))

(defn mouse-move!
  ([thing] (mouse-move thing) (flush))
  ([root selector] (mouse-move root selector) (flush)))

(defn mouse-down
  ([thing] (mouse-down thing 0))
  ([thing button]
   (let [node (resolve-node :mouse-down thing)]
     ((.-mouseDown simulator) node (clj->js {:button button :buttons 1}))))
  ([root button selector]
   (mouse-down (resolve-node :mouse-down root selector) button)))

(defn mouse-down!
  ([thing] (mouse-down thing) (flush))
  ([root selector] (mouse-down root selector) (flush)))

(defn mouse-leave
  ([thing]
   ((.-mouseLeave simulator) (resolve-node :mouse-leave thing)))
  ([root selector]
   (mouse-leave (resolve-node :mouse-leave root selector))))

(defn mouse-leave!
  ([thing] (mouse-leave thing) (flush))
  ([root selector] (mouse-leave root selector) (flush)))

(defn change
  ([thing]
   ((.-change simulator) (resolve-node :change thing) {:target thing}))
  ([thing value]
   (let [node (resolve-node :change thing)]
     (set! (.-value node) value)
     (change node)))
  ([root selector value]
   (change (resolve-node :change root selector) value)))

(defn change!
  ([thing value] (change thing value) (flush))
  ([root selector value] (change root selector value) (flush)))

(defn check-box
  ([thing value]
   (let [node (resolve-node :check-box thing)]
     (set! (.-checked node) value)
     (change node)))
  ([root selector value]
   (check-box (resolve-node :check-box root selector) value)))

(defn check-box!
  ([thing value] (check-box thing value) (flush))
  ([root selector value] (check-box root selector value) (flush)))

(defn html!
  "Throws exception if the node doesn't exist."
  ([thing]
   (.-innerHTML (resolve-node :html thing)))
  ([root selector]
   (.-innerHTML (resolve-node :html root selector))))

(defn html
  "Return nil if the node doesn't exist."
  ([] (.-innerHTML (.-body js/document)))
  ([selector-or-elem]
   (cond
     (string? selector-or-elem) (when-let [e (select selector-or-elem)] (.-innerHTML e))
     (nil? selector-or-elem) nil
     :else (.-innerHTML selector-or-elem)))
  ([root selector]
   (when-let [e (select root selector)]
     (.-innerHTML e))))

(defn class-name
  ([thing]
   (.-className (resolve-node :class-name thing)))
  ([root selector]
   (.-className (resolve-node :class-name root selector))))

(defn tag-name
  ([thing]
   (.-tagName (resolve-node :tag-name thing)))
  ([root selector]
   (.-tagName (resolve-node :tag-name root selector))))

(defn href
  ([thing]
   (.-href (resolve-node :href thing)))
  ([root selector]
   (.-href (resolve-node :href root selector))))

(defn id
  ([thing]
   (.-id (resolve-node :id thing)))
  ([root selector]
   (.-id (resolve-node :id root selector))))

(defn value
  ([thing]
   (.-value (resolve-node :value thing)))
  ([root selector]
   (.-value (resolve-node :value root selector))))

(defn checked?
  ([thing]
   (.-checked (resolve-node :checked? thing)))
  ([root selector]
   (.-checked (resolve-node :checked? root selector))))

(defn disabled?
  ([thing]
   (.-disabled (resolve-node :disabled? thing)))
  ([root selector]
   (.-disabled (resolve-node :disabled? root selector))))

(defn readonly?
  ([thing]
   (.-readOnly (resolve-node :readonly? thing)))
  ([root selector]
   (.-readOnly (resolve-node :readonly? root selector))))

(defn src
  ([thing]
   (.-src (resolve-node :src thing)))
  ([root selector]
   (.-src (resolve-node :src root selector))))

(defn alt
  ([thing]
   (.-alt (resolve-node :alt thing)))
  ([root selector]
   (.-alt (resolve-node :alt root selector))))

(defn style
  ([thing]
   (.-style (resolve-node :id thing)))
  ([root selector]
   (.-style (resolve-node :id root selector))))

(defn print-html [] (println "HTML: " (html)))

(defn print-error [e file line col]
  (println "********* JS ERROR *********")
  (println "\t" e)
  (println "\t" (str/join ":" [file line col]))
  (println "****************************"))
(defn print-js-errors [] (set! (.-onerror js/window) print-error))

(defn stub-ajax []
  (around [it]
    (with-redefs [ajax/post! (stub :ajax/post!)
                  ajax/get! (stub :ajax/get!)]
      (it))))

(defn last-ajax-post-url [] (when-let [args (stub/last-invocation-of :ajax/post!)] (first args)))
(defn last-ajax-get-url [] (when-let [args (stub/last-invocation-of :ajax/get!)] (first args)))

(defn last-ajax-post-params [] (when-let [args (stub/last-invocation-of :ajax/post!)] (second args)))
(defn last-ajax-get-params [] (when-let [args (stub/last-invocation-of :ajax/get!)] (second args)))

(defn last-ajax-post-handler [] (when-let [args (stub/last-invocation-of :ajax/post!)] (nth args 2)))
(defn last-ajax-get-handler [] (when-let [args (stub/last-invocation-of :ajax/get!)] (nth args 2)))

(defn last-ajax-post-options [] (when-let [args (stub/last-invocation-of :ajax/post!)] (ccc/->options (drop 3 args))))
(defn last-ajax-get-options [] (when-let [args (stub/last-invocation-of :ajax/get!)] (ccc/->options (drop 3 args))))

(defn stub-ws []
  (around [it]
    (with-redefs [ws/call! (stub :ws/call!)]
      (it))))
(defn last-ws-call-id [] (when-let [args (stub/last-invocation-of :ws/call!)] (first args)))
(defn last-ws-call-params [] (when-let [args (stub/last-invocation-of :ws/call!)] (second args)))
(defn last-ws-call-handler [] (when-let [args (stub/last-invocation-of :ws/call!)] (nth args 2)))
(defn last-ws-call-options [] (when-let [args (stub/last-invocation-of :ws/call!)] (ccc/->options (drop 3 args))))


