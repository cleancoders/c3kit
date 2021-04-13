(ns c3kit.apron.log
  #?(:cljs (:require-macros [c3kit.apron.log :refer [trace debug info warn error fatal report capture-logs]]))
  (:require [clojure.string :as str]
            [taoensso.timbre :as timbre]))

;; MDM - If you're not seeing a log entry, check your Chrome Dev Tools console levels.

; For accurate line numbers in Chrome, add these Blackbox patterns:
; /taoensso/timbre/appenders/core\.js$
; /taoensso/timbre\.js$
; /cljs/core\.js$
;
; See: https://goo.gl/ZejSvR

(def captured-logs (atom []))

#?(:clj (defmacro trace [& args] `(timbre/trace ~@args)))
#?(:clj (defmacro debug [& args] `(timbre/debug ~@args)))
#?(:clj (defmacro info [& args] `(timbre/info ~@args)))
#?(:clj (defmacro warn [& args] `(timbre/warn ~@args)))
#?(:clj (defmacro error [& args] `(timbre/error ~@args)))
#?(:clj (defmacro fatal [& args] `(timbre/fatal ~@args)))
#?(:clj (defmacro report [& args] `(timbre/report ~@args)))
#?(:clj (defmacro capture-logs [& body]
          `(let [original-level# (:level timbre/*config*)]
             (reset! captured-logs [])
             (try
               (timbre/set-level! :trace)
               (with-redefs [timbre/-log! (fn [& args#] (swap! captured-logs conj args#))]
                 ~@body)
               (finally
                 (timbre/set-level! original-level#))))))
#?(:clj (defmacro with-level [level & body] `(timbre/with-level ~level ~@body)))

(defn test-levels [msg]
  (report msg)
  (fatal msg)
  (error msg)
  (warn msg)
  (info msg)
  (debug msg)
  (trace msg))

(defn level [] (:level timbre/*config*))

(defn set-level! [new-level]
  (when-not (= (level) new-level)
    (report (str "Setting log level: " new-level))
    (timbre/set-level! new-level)))

(defn off! [] (set-level! :report))
(defn fatal! [] (set-level! :fatal))
(defn error! [] (set-level! :error))
(defn warn! [] (set-level! :warn))
(defn info! [] (set-level! :info))
(defn debug! [] (set-level! :debug))
(defn all! [] (set-level! :trace))

(defn parse-captured-logs []
  (map
    (fn [[config level ?ns-str ?file ?line msg-type ?err vargs_ ?base-data callsite-id]]
      {:level level :message (apply str @vargs_)})
    @captured-logs))

(defn captured-logs-str []
  (str/join "\n"
            (map #(str/join " " %)
                 (map #(deref (nth % 7)) @captured-logs))))

;; TODO - MDM: Make this portable.  Either replace use of clojure.core/format, or find cljs equiv.
#?(:clj
   (defn table-spec [& cols]
     (let [width (+ (apply + (map second cols)) (count cols))
           format-str (str/join " " (map #(str "%-" (second %) "s") cols))]
       {:cols     cols
        :format   format-str
        :width    width
        :title-fn (fn [title]
                    (let [pad (/ (- width (.length title)) 2)]
                      (str (str/join "" (take pad (repeat " "))) title "\n")))
        :header   (str (apply (partial format format-str) (map first cols)) "\n"
                       (str/join "" (take width (repeat "-"))) "\n")
        })))

(defn color-pr
  "For ANSI color codes: https://en.wikipedia.org/wiki/ANSI_escape_code"
  [message color]
  (println (str "\u001b[" color "m" message "\u001b[0m")))

