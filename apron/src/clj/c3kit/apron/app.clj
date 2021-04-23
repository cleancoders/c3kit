(ns c3kit.apron.app
  (:import (clojure.lang IDeref))
  (:require
    [c3kit.apron.log :as log]
    [c3kit.apron.util :as util]))

;; MDM - This file should never change mid-process. Therefore is has no reason to reload (wrap-refresh).
;; Application objects that should persist through reloads, like the database connection,
;; can be stored here. The resolution fn offers convenience to retrieve the app values.
(defonce app {:api-version "no-api-version"})

(defn resolution
  "Returns a deref-able pointer to values stored in app, may return nil"
  [key]
  (if (vector? key)
    (reify IDeref (deref [_] (get-in app key)))
    (reify IDeref (deref [_] (get app key)))))

(defn resolution!
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
    (when-let [start-fn (util/resolve-var start-fn-sym)]
      (alter-var-root #'app start-fn))))

(defn- stop-service! [service]
  (when-let [stop-fn-sym (:stop service)]
    (when-let [stop-fn (util/resolve-var stop-fn-sym)]
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

(defn development?
  "Return true if env resolves to 'development'"
  [] (or (not @env) (= "development" @env)))

(defn production?
  "Return true if env resolves to 'production'"
  [] (= "production" @env))




