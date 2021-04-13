(ns c3kit.swingstage.css
  (:require [clojure.java.io :as io]
            [garden.core :as garden]
            [cleancoders.util :as util]
            [clojure.tools.namespace.reload :as reload]
            [clojure.tools.namespace.dir :as track]
            [clojure.pprint :refer [pprint]]))

(defmacro print-exec-time
  [tag expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str ~tag ": " (/ (double (- (. System (nanoTime)) start#)) 1000000000.0) " secs"))
     ret#))

(def config
  {:source-dir  "dev/cleancoders/styles"
   :ns          'cleancoders.styles.main
   :output-file "resources/public/css/cleancoders.css"
   :flags       {:pretty-print? true
                 :vendors       ["webkit" "moz" "o"]}})

(defn- generate []
  (print-exec-time "generating css"
    (let [output (:output-file config)
          css-var (ns-resolve (the-ns (:ns config)) 'screen)
          css (garden/css (:flags config) @css-var)]
      (println (str "css: writing " (count css) " bytes to " output))
      (spit (io/file output) css)))
  (println))

(defn handle-error [error]
  (let [emap (Throwable->map error)]
    (println "ERROR ---------------------------------------------")
    (println "Cause: " (:cause emap))
    (pprint (:via emap))
    (println "---------------------------------------------------")
    (println)))

(defn auto-generate []
  (loop [tracker {} last-mod-time 0]
    (let [tracker (track/scan tracker (:source-dir config))
          mod-time (:clojure.tools.namespace.dir/time tracker)
          change? (> mod-time last-mod-time)
          to-load (seq (:clojure.tools.namespace.track/load tracker))]
      (if (and change? to-load)
        (let [_ (println "reloading: " to-load)
              tracker (print-exec-time "reloading time" (reload/track-reload tracker))]
          (if-let [error (:clojure.tools.namespace.reload/error tracker)]
            (handle-error error)
            (generate))
          (recur tracker mod-time))
        (do
          (Thread/sleep 1000)
          (recur tracker mod-time))))))

(defn -main [& args]
  (let [once-or-auto (or (first args) "auto")]
    (assert (#{"once" "auto"} once-or-auto) (str "Unrecognized build frequency: " once-or-auto ". Must be 'once' or 'auto'"))
    (util/establish-path (:output-file config))
    (println "Compiling CSS:" once-or-auto)
    (if (= "once" once-or-auto)
      (do (require (:ns config))
          (generate))
      (auto-generate))))
