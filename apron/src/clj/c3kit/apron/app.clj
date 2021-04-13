(ns c3kit.apron.app
  (:import (clojure.lang IDeref))
  (:require
    [c3kit.apron.log :as log]))

;; MDM - This file should never change mid-process. Therefore is has no reason to reload (wrap-refresh).
;; Application objects that should persist through reloads, like the database connection,
;; can be stored here. The resolution fn offers convenience to retrieve the app values.
(defonce app {:api-version "no-api-version"})

(defn resolve-var
  "Given the symbol of a fully qualified var name, load the namespace and return the var.
  Throws an exception if the var doesnt exist.  Deref the var to get it's value.
  Use to decouple code; dynamically load code."
  [var-sym]
  (let [ns-sym (symbol (namespace var-sym))
        var-sym (symbol (name var-sym))]
    (require ns-sym)
    (if-let [var (ns-resolve (the-ns ns-sym) var-sym)]
      var
      (throw (Exception. (str "No such var " (name ns-sym) "/" (name var-sym)))))))

(defn resolution
  "Returns a deref-able pointer to values stored in app.
  An exception is throw when app does not have a value for the key."
  [key]
  (if (vector? key)
    (reify IDeref
      (deref [_]
        (if-let [value (get-in app key)]
          value
          (throw (Exception. (str "Unresolved app component: " key))))))
    (reify IDeref
      (deref [_]
        (if-let [value (get app key)]
          value
          (throw (Exception. (str "Unresolved app component: " key))))))))

(defn service
  "The start and stop symbols must point to functions that:
    1) start/stop the 'service'
    2) add/remove data from app"
  [start-sym stop-sym] {:start start-sym :stop stop-sym})

(defn- start-service! [service]
  (when-let [start-fn-sym (:start service)]
    (when-let [start-fn (resolve-var start-fn-sym)]
      (alter-var-root #'app start-fn))))

(defn- stop-service! [service]
  (when-let [stop-fn-sym (:stop service)]
    (when-let [stop-fn (resolve-var stop-fn-sym)]
      (alter-var-root #'app stop-fn))))

(defn start! [services]
  (log/with-level :info
                  (log/info ">>>>> Starting App >>>>>")
                  (doseq [service services] (start-service! service))
                  (log/info "<<<<< App Started <<<<<")))

(defn stop! [services]
  (log/with-level :info
                  (log/info ">>>>> Stopping App >>>>>")
                  (doseq [service (reverse services)] (stop-service! service))
                  (log/info "<<<<< App Stopped <<<<<")))

(def env-keys ["c3.env" "C3_ENV"])

(defn find-env
  "Look for the env value in the system properties, OS environment variables, or default to development"
  ([] (find-env env-keys))
  ([env-keys] (let [[p-name e-name] env-keys] (find-env p-name e-name)))
  ([property-name env-name] (or (System/getProperty property-name) (System/getenv env-name) "development")))

(defn start-env
  "To be used as in a start service fn."
  ([app] (let [[p-name e-name] env-keys] (start-env p-name e-name app)))
  ([property-name env-name app] (assoc app :env (find-env property-name env-name))))

(defn stop-env
  "To be used as in a stop service fn."
  [app] (dissoc app :env))

(defn set-env! [env] (alter-var-root #'app assoc :env env))

(def env (resolution :env))




