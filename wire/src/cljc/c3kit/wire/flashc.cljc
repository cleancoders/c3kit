(ns c3kit.wire.flashc
  #?(:clj  (:import (java.util UUID)))
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.schema :as schema])
  )

(def flash-schema
  {:level   {:type :keyword}
   :text    {:type :ignore}
   :persist {:type :boolean}
   :id      {:type   :string
             :coerce #(or % (str (ccc/new-uuid)))}})

(defn conform! [flash]
  (schema/conform! flash-schema flash))

(defn create
  ([class txt] (create class txt false))
  ([class txt persist] {:level class :text txt :id (str (ccc/new-uuid)) :persist persist}))
(defn error [txt] (create :error txt))
(defn warn [txt] (create :warn txt))
(defn success [txt] (create :success txt))

(defn success? [flash] (= :success (:level flash)))
(defn warn? [flash] (= :warn (:level flash)))
(defn error? [flash] (= :error (:level flash)))

(defn text [flash] (:text flash))
(defn level [flash] (:level flash))
(defn id [flash] (:id flash))

(defn flash-class [flash]
  (cond (error? flash) "error"
        (warn? flash) "warn"
        :else "success"))
