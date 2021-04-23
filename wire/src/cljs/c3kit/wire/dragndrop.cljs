(ns c3kit.wire.dragndrop
  (:require
    [c3kit.wire.dnd-mobile-patch]
    [goog.fx.DragDropGroup]
    [goog.fx.DragDrop]
    [goog.events :as events]
    [goog.dom :as dom]))

(defn get-group [dnd group]
  (if-let [group (get @dnd group)]
    group
    (throw (ex-info (str "DnD group missing: " group) {:group group :dnd dnd}))))

(defn register [dnd group key id]
  (fn [e]
    (if e
      (.addItem (get-group dnd group) e {:group group :key key})
      (.removeItem (get-group dnd group) id))))

(defn context [] (atom {}))

(defn add-group [dnd group-key]
  (let [dnd-group (goog.fx.DragDropGroup.)]
    (.init dnd-group)
    (swap! dnd assoc group-key dnd-group)
    dnd))

(defn drag-from-to [dnd from-key to-key]
  (let [from (get-group dnd from-key)
        to (get-group dnd to-key)]
    (.addTarget from to))
  dnd)

(defn- one-item-listener [listener]
  (fn [e]
    (let [data (.-data (.-dragSourceItem e))]
      (listener (:group data) (:key data)))))

(defn- two-item-listener [listener]
  (fn [e]
    (let [source-data (.-data (.-dragSourceItem e))
          target-data (.-data (.-dropTargetItem e))]
      (listener (:group source-data) (:key source-data) (:group target-data) (:key target-data)))))

(defn on-drag-start [dnd group-key listener]
  (events/listen (get-group dnd group-key) "dragstart" (one-item-listener listener) false)
  dnd)

(defn on-drag-end [dnd group-key listener]
  (events/listen (get-group dnd group-key) "dragend" (one-item-listener listener) false)
  dnd)

(defn on-drag-over [dnd group-key listener]
  (events/listen (get-group dnd group-key) "dragover" (two-item-listener listener) false)
  dnd)

(defn on-drag-out [dnd group-key listener]
  (events/listen (get-group dnd group-key) "dragout" (two-item-listener listener) false)
  dnd)

(defn on-drop [dnd group-key listener]
  (events/listen (get-group dnd group-key) "drop" (two-item-listener listener) false)
  dnd)

(defn set-drag-class [dnd group-key classname]
  (.setDragClass (get-group dnd group-key) classname)
  dnd)

(declare fake-hiccup->dom)

(defn- vector->html [hiccup]
  (if (= :<> (first hiccup))
    (map fake-hiccup->dom (rest hiccup))
    (let [tag-name (name (first hiccup))
          options (when (map? (second hiccup)) (clj->js (second hiccup)))
          remaining (if options (drop 2 hiccup) (rest hiccup))
          children (map fake-hiccup->dom remaining)]
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

