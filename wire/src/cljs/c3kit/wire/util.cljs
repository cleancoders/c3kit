(ns c3kit.wire.util
  (:require
    [c3kit.apron.log :as log]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [goog.dom :as dom]
    [goog.dom.forms :as form]
    ))

(defn errors->strings [errors]
  (map
    (fn [[field error]]
      (str (name field) " " error))
    errors))

(defn +class-if
  ([condition class-name] (+class-if {} condition class-name))
  ([attributes condition class-name]
   (let [attributes (or attributes {})
         class-name (cljs.core/name class-name)]
     (if condition
       (if-let [class-value (:class attributes)]
         (assoc attributes :class (str class-value " " class-name))
         (assoc attributes :class class-name))
       attributes))))

(defn ->css-class [& classes] (str/join " " (remove nil? (flatten classes))))

(def id-counter (cljs.core/atom 0))
(defn uid []
  (let [result (str "A_" @id-counter)]
    (swap! id-counter inc)
    result))

(defn with-react-keys [col]
  (doall
    (map
      (fn [[n i]]
        (if (satisfies? IWithMeta n)
          (let [m (meta n)]
            (if (:key m)
              n
              (with-meta n (assoc m :key i))))
          (with-meta [:span n] {:key i})))
      (partition 2 (interleave col (range))))))

(defn keyed-list [& args] (with-react-keys args))

(def next-key
  (let [k (atom 0)]
    (fn [] (swap! k inc))))

(defn with-nested-react-keys [tree]
  (walk/postwalk
    (fn [node]
      (if (vector? node)
        (with-meta node (assoc (meta node) :key (next-key)))
        node))
    tree))

(defn atom-observer
  "Used to keep track of cursor state.
  Usage: (defonce flash (reagent/cursor (a/atom-observer ratom) [:flash]))"
  [state]
  (fn
    ([path] (get-in @state path))
    ([path value]
     (log/debug "updating atom:" path value)
     (swap! state assoc-in path value))))

(defn watch-atom [key atom]
  (add-watch atom key (fn [k r o n] (log/debug (str "updating atom " key ": " n)))))

(defn form-data
  "Given an if of a form element, loads all the inputs and returns a map {:keyword value}."
  [form]
  (let [form-map (form/getFormDataMap (dom/getElement (name form)))]
    (reduce
      (fn [result key]
        (let [value (.get form-map key)]
          (if (and value (= 1 (count value)))
            (assoc result (keyword key) (first value))
            (assoc result (keyword key) value))))
      {}
      (.getKeys form-map))))
