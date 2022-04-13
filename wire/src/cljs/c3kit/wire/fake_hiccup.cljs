(ns c3kit.wire.fake-hiccup
  (:require [goog.dom :as dom]
            [clojure.string :as s]))

(defn- attrs->list [selector attributes]
  (->> (get attributes selector)
       (map #(subs % 1))
       (s/join " ")))

(defn- tag-options [tag]
  (let [attributes (->> tag str (re-seq #".+?(?=[#\.]|$)") rest (group-by first))]
    {:id    (attrs->list \# attributes)
     :class (attrs->list \. attributes)}))

(defn- combine-attribute [key & options]
  (let [value (->> options (map key) (s/join " ") s/trim)]
    (when-not (empty? value)
      {key value})))

(defn hiccup-options [[tag options]]
  (let [options (if (map? options) options {})
        tag-options (tag-options tag)]
    (merge options
           (combine-attribute :id tag-options options)
           (combine-attribute :class tag-options options))))

(defn ->tag-name [tag]
  (let [name (name tag)]
    (->> [(s/index-of name "#")
          (s/index-of name ".")
          (count name)]
         (filter pos?)
         (apply min)
         (subs name 0))))

(defn child-elements [hiccup]
  (if (map? (second hiccup))
    (drop 2 hiccup)
    (rest hiccup)))

(declare ->dom)

(defn create-dom-node [[tag :as hiccup]]
  (dom/createDom
    (->tag-name tag)
    (-> hiccup hiccup-options clj->js)
    (->> hiccup child-elements (map ->dom) flatten clj->js)))

(defn component->html [[tag & children]]
  (let [component (apply tag children)]
    (if (fn? component)
      (->dom (vec (concat [component] children)))
      (->dom component))))

(defn vector->html [[tag & children :as hiccup]]
  (cond (= :<> tag) (map ->dom children)
        (fn? tag) (component->html hiccup)
        :else (create-dom-node hiccup)))

(defn ->dom [hiccup]
  (cond (vector? hiccup) (vector->html hiccup)
        (seq? hiccup) (map ->dom hiccup)
        (string? hiccup) hiccup
        (nil? hiccup) nil
        :else (pr-str hiccup)))
