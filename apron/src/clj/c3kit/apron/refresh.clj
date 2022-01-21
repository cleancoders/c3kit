(ns c3kit.apron.refresh
  (:import (java.io File)
           (java.net URL))
  (:require
    [c3kit.apron.app :as app]
    [c3kit.apron.log :as log]
    [c3kit.apron.util :as util]
    [clojure.java.io :as io]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.tools.namespace.file :as file]
    [clojure.tools.namespace.reload :as reload]))

(defonce excludes (atom #{}))
(defonce services (atom []))
(defonce prefix (atom "c3kit"))

(defn init [s ns-prefix exclude-syms]
  (reset! services s)
  (reset! prefix ns-prefix)
  (swap! excludes (fn [exs] (set (concat exs exclude-syms)))))

;; MDM : Copied from clojure.tools.namespace.dir because they're private.

(defn- modified-files [tracker files]
  (filter #(< (:clojure.tools.namespace.dir/time tracker 0) (.lastModified ^File %)) files))

(defn- deleted-files [tracker files]
  (set/difference (:clojure.tools.namespace.dir/files tracker #{}) (set files)))

(defn- update-files [tracker deleted modified]
  (let [now (System/currentTimeMillis)]
    (-> tracker
        (update-in [:clojure.tools.namespace.dir/files] #(if % (apply disj % deleted) #{}))
        (file/remove-files deleted)
        (update-in [:clojure.tools.namespace.dir/files] into modified)
        (file/add-files modified)
        (assoc :clojure.tools.namespace.dir/time now))))

;; MDM : Copied ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

(def clj-extensions [".clj" ".cljc"])

(defn ns-to-filenames
  "Converts the namespace name into a relative path for the corresponding clojure src file."
  ([ns] (ns-to-filenames ns clj-extensions))
  ([ns extensions] (map #(str (apply str (replace {\. \/ \- \_} (name ns))) %) extensions)))

(defn ns-to-file
  "Returns a java.io.File corresponding to the clojure src file for the
  given namespace.  nil is returned if the file is not found in the classpath
  or if the file is not a raw text file."
  ([ns] (ns-to-file ns clj-extensions))
  ([ns extensions]
   (let [relative-filenames (ns-to-filenames ns extensions)
         ^URL url (first (filter identity (map #(io/resource %) relative-filenames)))]
     (if (and url (= "file" (.getProtocol url)))
       (io/file (.getFile url))
       (do
         ;; When run in the same process as cljs, all the cljs namespaced get loaded, but won't be found here.
         ;(log/warn "can't find file for ns: " ns url (pr-str relative-filenames))
         nil)))))

(defn scan [tracker]
  (let [files (->> (all-ns)
                   (map #(.name %))
                   (filter #(str/starts-with? (name %) @prefix))
                   (remove @excludes)
                   (map ns-to-file)
                   (remove nil?))
        deleted (seq (deleted-files tracker files))
        modified (seq (modified-files tracker files))]
    (if (or deleted modified)
      (update-files tracker deleted modified)
      tracker)))


(defn reload [tracker]
  (if-let [to-load (seq (:clojure.tools.namespace.track/load tracker))]
    (do (app/stop! @services)
        (log/info "Reloading:\n\t" (str/join "\n\t" (sort to-load)))
        (let [tracker (reload/track-reload tracker)]
          (app/start! @services)
          tracker))
    tracker))

(defn print-error [tracker]
  (when-let [error (:clojure.tools.namespace.reload/error tracker)]
    (log/report error))
  tracker)

(def lock (Object.))

(def tracker (atom {}))

(defn refresh!
  ([] (locking lock (swap! tracker refresh!)))
  ([tracker]
   (-> tracker
       scan
       reload
       print-error)))

(defn refresh-handler [root-sym]
  (fn [request]
    (refresh!)
    (let [root-handler (util/resolve-var root-sym)]
      (root-handler request))))

