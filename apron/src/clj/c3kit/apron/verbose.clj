(ns c3kit.apron.verbose
  (:require
    [c3kit.apron.log :as log]
    ))

(declare ^:dynamic *buffer*)

(def endl (System/getProperty "line.separator"))
(def spaces (repeat " "))
(def indents (repeat "    "))

(defn- << [& values]
  (doseq [value values]
    (.append *buffer* value)))

(defn- left-col [value width]
  (let [space-count (- width (count (str value)))]
    (apply str value (take space-count spaces))))

(defn- indentation [indent]
  (let [n (if (< indent 0) 0 indent)]
    (apply str (take n indents))))

(declare make-pretty)

(defn- <<-map [the-map indent]
  (if (< (count the-map) 2)
    (<< the-map)
    (do
      (<< "{")
      (let [key-map (reduce #(assoc %1 (str %2) %2) {} (keys the-map))
            keys (sort (keys key-map))
            key-lengths (map count keys)
            max-key-length (apply max key-lengths)
            left-width (+ 2 max-key-length)]
        (doseq [key keys]
          (<< endl (indentation indent) (left-col key left-width))
          (make-pretty (get the-map (get key-map key)) indent)))
      (<< "}"))))

(defn- make-pretty
  ([thing] (make-pretty thing 0))
  ([thing indent]
   (cond
     (map? thing) (<<-map thing (inc indent))
     (nil? thing) (<< "nil")
     :else (<< thing))))

(defn pretty-map [value]
  (binding [*buffer* (StringBuffer.)]
    (make-pretty value)
    (.toString *buffer*)))

;; --  chee.pretty-map


(defn make-body-printable [body]
  (cond
    (string? body)
    (let [size (count body)]
      (if (< 500 size)
        (str size " chars of body")
        body))

    (map? body)
    (if-let [payload (:payload body)]
      (let [printable (pr-str payload)]
        (if (< 100 (count printable))
          (if (map? payload)
            (assoc body :payload (str "< map with keys: " (keys payload) ", values elided >"))
            (assoc body :payload (str "< " (count payload) " entities elided >")))
          (assoc body :payload printable)))
      body)))


(def request-count (atom 0))

(defn wrap-verbose
  "Prints request and response maps to STDOUT in a human readable format."
  [handler]
  (fn [request]
    (let [request-id (swap! request-count inc)]
      (log/info
        "REQUEST " request-id " ========================================================================================"
        endl
        (pretty-map request)
        endl)
      (let [response (handler request)]
        (log/info
          "RESPONSE " request-id " ========================================================================================"
          endl
          (pretty-map (update response :body make-body-printable))
          endl)
        response))))


;(defn wrap-development [handler]
;  (-> handler
;      wrap-verbose))
