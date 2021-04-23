(ns c3kit.wire.api
  (:require
    [c3kit.apron.app :as app]
    [c3kit.apron.log :as log]
    [c3kit.apron.schema :as schema]
    [c3kit.wire.apic :as apic]
    [clojure.java.io :as io]
    [clojure.string :as str]
    ))

(defn load-api-version [js-path app]
  (let [url (or (io/resource js-path) (throw (ex-info "No JS file. Did you forget to do 'lein cljs'?" {:js-path js-path})))
        file (io/file (.getFile url))
        timestamp (str (.lastModified file))]
    (log/report "API version: " timestamp)
    (assoc app :api/version timestamp)))

(def version (app/resolution :api/version))

(defn maybe-validation-errors [entity]
  (when-let [error-map (schema/error-message-map entity)]
    (-> (apic/ok {:errors error-map})
        (apic/flash-warn "Validation errors..."))))

(defn maybe-entity-errors [entity]
  (when-let [errors (schema/messages entity)]
    (apic/fail nil (str/join " , " errors))))
