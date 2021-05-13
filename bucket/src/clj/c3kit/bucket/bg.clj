(ns c3kit.bucket.bg
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit ScheduledFuture))
  (:require
    [c3kit.apron.app :as app]
    [c3kit.apron.log :as log]
    [c3kit.apron.time :as time]
    [c3kit.bucket.db :as db]
    ))

(defn start [app]
  (log/info "Starting background manager")
  (let [executor (ScheduledThreadPoolExecutor. 3)]
    (assoc app :background (atom {:executor executor}))))

(defonce background (app/resolution :background))

(defn ^ScheduledThreadPoolExecutor executor [] (when @background (:executor @@background)))

(defn stop [app]
  (when-let [executor (executor)]
    (log/info "Shutting down background manager")
    (.shutdownNow executor)
    (.awaitTermination executor 5 TimeUnit/SECONDS))
  (dissoc app :background))

(def service (app/service 'c3kit.bucket.bg/start 'c3kit.bucket.bg/stop))

(defn task [key] (get @@background key))

(defn- new-task-record! [key] (db/tx :kind :bg-task :key key :last-ran-at time/epoch))

(defn- wrap-task [record task]
  (fn []
    (try
      (let [record (db/reload record)
            now (time/now)]
        (task (:last-ran-at record) now)
        (db/tx record :last-ran-at now))
      (catch Exception e
        (log/error "Background Error:")
        (log/error e)))))

(defn ^ScheduledFuture schedule [key period ^Runnable task]
  (log/info "Scheduling task: " key period)
  (let [record (or (db/ffind-by :bg-task :key key) (new-task-record! key))
        millis-since-ran (time/millis-between (time/now) (:last-ran-at record))
        initial-delay (max 0 (- period millis-since-ran))
        wrapped (wrap-task record task)
        executor (executor)
        scheduled (.scheduleAtFixedRate executor wrapped initial-delay period TimeUnit/MILLISECONDS)]
    (swap! @background assoc key scheduled)
    scheduled))

(defn cancel-task [key]
  (log/info "Cancelling task: " key)
  (when-let [bg-resolution @background]
    (when-let [^ScheduledFuture task (get @bg-resolution key)]
      (.cancel task false))))

(defn start-scheduled-tasks [tasks app]
  (reduce
    (fn [app [key period task-fn]]
      (assoc app key (schedule key period task-fn)))
    app tasks))

(defn stop-scheduled-tasks [tasks app]
  (reduce
    (fn [app [key _ _]]
      (cancel-task key)
      (dissoc app key))
    app tasks))
