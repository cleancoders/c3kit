(ns c3kit.apron.cljs
  (:import (cljs.closure Inputs Compilable))
  (:require [cljs.build.api :as api]
            [clojure.java.io :as io]
            [c3kit.apron.util :as util]
            [c3kit.apron.app :as app]
            ))

;; MDM - This is copied from scaffold, sad be necessary.  scaffold needs to be a separate library for all the dev deps
;; which we don't want in apron. scaffold depends on apron, so to avoid circular dependency, we copy the code.

(def build-config (atom nil))
(def run-cmd (atom "phantomjs resources/public/specs/speclj.js"))

(defn run-specs [auto?]
  (let [cmd (str @run-cmd (when auto? " auto"))
        process (.exec (Runtime/getRuntime) cmd)
        output (.getInputStream process)
        error (.getErrorStream process)]
    (io/copy output (System/out))
    (io/copy error (System/err))
    (when (not auto?)
      (System/exit (.waitFor process)))))

(defn on-dev-compiled []
  ;; MDM - Touch the js output file so the browser will reload it without hard refresh.
  (.setLastModified (io/file (:output-to @build-config)) (System/currentTimeMillis))
  (run-specs true))

(deftype Sources [build-options]
  Inputs
  (-paths [_] (map io/file (:sources build-options)))
  Compilable
  (-compile [_ opts] (mapcat #(cljs.closure/compile-dir (io/file %) opts) (:sources build-options)))
  (-find-sources [_ opts] (mapcat #(cljs.closure/-find-sources % opts) (:sources build-options))))

(defn auto-run [build-options]
  (while true
    (try
      (api/watch (Sources. build-options) build-options)
      (catch Exception e
        (.printStackTrace e)))))

(defn- resolve-watch-fn [options]
  (if-let [watch-fn-sym (:watch-fn options)]
    (do
      (when-not (symbol? watch-fn-sym) (throw (Exception. ":watch-fn must be a fully qualified symbol")))
      (assoc options :watch-fn (util/resolve-var watch-fn-sym)))
    options))

(defn -main [& args]
  ;; usage:  lein run -m cleancoders.cljs [auto (default)|once] [env (development)]
  (let [once-or-auto (or (first args) "auto")
        config (util/read-edn-resource "config/cljs.edn")
        build-key (keyword (or (second args) (app/find-env (or (:env-keys config) app/env-keys))))]
    (when-let [cmd (:run-cmd config)] (reset! run-cmd cmd))
    (reset! build-config (resolve-watch-fn (get config build-key)))
    (assert (#{"once" "auto"} once-or-auto) (str "Unrecognized build frequency: " once-or-auto ". Must be 'once' or 'auto'"))
    (println "Compiling ClojureScript:" once-or-auto build-key)
    (util/establish-path (:output-to @build-config))
    (io/delete-file ".specljs-timestamp" true)
    (if (= "once" once-or-auto)
      (do
        (api/build (Sources. @build-config) @build-config)
        (when (:specs @build-config)
          (run-specs false)))
      (auto-run @build-config))))
