(ns c3kit.bucket.bg-spec
  (:require
    [c3kit.apron.log :as log]
    [c3kit.apron.time :as time :refer [seconds hours ago from-now]]
    [c3kit.bucket.bg :as bg]
    [c3kit.bucket.bg-schema :as bg-schema]
    [c3kit.bucket.db :as db]
    [c3kit.bucket.spec-helper :as helper]
    [speclj.core :refer :all]
    ))

(def app (atom {}))
(def counter (atom 0))
(defn one-upper [_ _] (swap! counter inc))

(log/off!)

(describe "Background"

  (helper/with-db-schemas [bg-schema/bg-task])

  (around [it]
    (with-redefs [bg/background (delay (:background @app))]
      (it)))
  (before (reset! app {})
          (reset! counter 0)
          (swap! app bg/start))
  (after (swap! app bg/stop))

  (it "starts"
    (should-contain :background @app)
    (should-contain :executor @(:background @app)))

  (it "executor accessor"
    (should= (:executor @(:background @app)) (bg/executor)))

  (it "stops"
    (let [executor (bg/executor)]
      (should= false (.isShutdown executor))

      (swap! app bg/stop)

      (should= true (.isShutdown executor))
      (should= nil (:background @app))))

  (it "schedules a task"
    (let [task (bg/schedule :one-upper 5000 one-upper)]
      (should= task (:one-upper @(:background @app)))
      (should= task (bg/task :one-upper))))

  (it "scheduled task executes immediately if never run"
    (should= 0 @counter)
    (bg/schedule :one-upper 5000 one-upper)
    (Thread/sleep 20)
    (should= 1 @counter)
    (let [bg-task (db/ffind-by :bg-task :key :one-upper)]
      (should-not-be-nil bg-task)
      (should (time/between? (:last-ran-at bg-task) (-> 1 seconds ago) (time/now)))))

  (it "scheduled task doesn't execute immediately if it's NOT due"
    (should= 0 @counter)
    (db/tx :kind :bg-task :key :one-upper :last-ran-at (-> 1 seconds ago))
    (bg/schedule :one-upper 5000 one-upper)
    (Thread/sleep 10)
    (should= 0 @counter))

  (it "scheduled task doesn't execute immediately if it IS due"
    (should= 0 @counter)
    (db/tx :kind :bg-task :key :one-upper :last-ran-at (-> 6 seconds ago))
    (bg/schedule :one-upper (-> 5 seconds) one-upper)
    (Thread/sleep 10)
    (should= 1 @counter))

  (it "executes periodically"
    (should= 0 @counter)
    (bg/schedule :one-upper 1 one-upper)
    (Thread/sleep 10)
    (should (> @counter 1))
    (should (< @counter 16)))

  (it "updates :last-ran-at in record"
    (bg/schedule :one-upper 1 one-upper)
    (let [record (db/ffind-by :bg-task :key :one-upper)]
      (Thread/sleep 10)
      (should (time/after? (:last-ran-at (db/reload record)) (:last-ran-at record)))))

  (it "cancelling a task"
    (let [task (bg/schedule :one-upper 100 one-upper)]
      (should= false (.isCancelled task))

      (bg/cancel-task :one-upper)
      (should= true (.isCancelled task))))

  (it "errors are logged"
    (log/capture-logs
      (bg/schedule :trouble-maker 5000 (fn [_ _] (throw (ex-info "blah" {}))))
      (Thread/sleep 10))
    (let [logs (seq (filter #(= :error (:level %)) (log/parse-captured-logs)))]
      (should-not-be-nil logs)
      (should= "Background Error:" (:message (first logs)))
      (should= "clojure.lang.ExceptionInfo: blah {}" (:message (second logs)))))
  )

