
(ns c3kit.apron.legend
  (:require
    [c3kit.apron.schema :as schema]
    ))

(def retract {:kind (schema/kind :db/retract) :id schema/id})

(def ^:dynamic index {})

(defn init!
  [schemas]
  #?(:clj (alter-var-root #'index (fn [_] schemas))
     :cljs (set! index schemas)))

(defn for-kind [kind]
  (or (get index kind)
      (throw (ex-info (str "Missing legend for kind: " (pr-str kind)) {:kind kind}))))

(defn present! [entity]
  (when entity
    (schema/present! (-> (:kind entity) for-kind) entity)))

(defn coerce! [entity]
  (when entity
    (schema/coerce! (-> (:kind entity) for-kind) entity)))

(defn conform! [entity]
  (when entity
    (schema/conform! (-> (:kind entity) for-kind) entity)))


