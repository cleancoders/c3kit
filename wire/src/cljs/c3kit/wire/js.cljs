(ns c3kit.wire.js
  (:import [goog History]))

; Key Codes
(def BACKSPACE 8)
(def TAB 9)
(defn TAB? [e] (= (.-keyCode e) TAB))
(def ENTER 13)
(defn ENTER? [e] (= (.-keyCode e) ENTER))
(def SHIFT 16)
(def ESC 27)
(defn ESC? [e] (= (.-keyCode e) ESC))
(def SPACE 32)
(def LEFT 37)
(def UP 38)
(def RIGHT 39)
(def DOWN 40)
(def DELETE 46)
(def COMMA 188)

(defn nod
  "Give an event the nod, as if saying: Good job, your work is done."
  [e]
  (.preventDefault e))

(defn nix
  "Nix an event: Stop what you're doing and get the hell out."
  [e]
  (.stopPropagation e)
  (.preventDefault e))

(defn nip
  "Nip the event in the bud, before it causes any trouble."
  [e]
  (.stopPropagation e))

(defn ancestor-where [pred node]
  (cond
    (nil? node) nil
    (pred node) node
    :else (recur pred (.-parentElement node))))

(defn timeout [millis f] (js/setTimeout f millis))
(defn clear-timeout [timeout] (js/clearTimeout timeout))

;(defn navigate!
;  "Use for navigation between rich-client pages.  Passing a full URL will cause error."
;  [path]
;  (accountant/navigate! path))

(defn redirect!
  "Tell the browser to load the given URL, with full HTTP request/response process."
  [url]
  (set! (.-location js/window) url))

;(defn goto! [path]
;  (when path
;    (if (secretary/locate-route path)
;      (navigate! path)
;      (redirect! path))))

(defn print-page [] (.print js/window))

(defn e-target [e] (.-target e))
(defn e-text [e] (-> e .-target .-value))
(defn e-checked? [e] (-> e .-target .-checked))

(defn focus! [node] (when node (.focus node)))

(defn nod-n-do
  "Creates fn to ignore what ever browser even is created and do something else."
  [a-fn & args]
  (fn [e]
    (nod e)
    (apply a-fn args)))

(defn page-title [] (.-title js/document))
(defn page-title= [title] (set! (.-title js/document) title))
(defn doc-ready-state [] (.-readyState js/document))
(defn doc-ready? [] (= "complete" (doc-ready-state)))
(defn page-href [] (-> js/window .-location .-href))
(defn page-reload! [] (.reload (.-location js/window)))
(defn uri-encode [& stuff] (js/encodeURIComponent (apply str stuff)))
(defn open-window [url window-name options-string] (.open js/window url window-name options-string))
(defn frame-window [iframe] (.-contentWindow iframe))
(defn post-message [window message target-domain] (.postMessage window (clj->js message) target-domain))
(defn register-post-message-handler [handler] (.addEventListener js/window "message" handler))
(defn screen-size [] [(.-width js/screen) (.-height js/screen)])
(defn node-width [node] (.-clientWidth node))
(defn node-height [node] (.-clientHeight node))
(defn node-size [node] [(node-width node) (node-height node)])
(defn node-value [node] (.-value node))
(defn element-by-id [id] (.getElementById js/document id))
(defn add-listener [node event listener] (.addEventListener node event listener))
(defn parent-node [node] (.-parentNode node))
(defn child-nodes [node] (array-seq (.-childNodes node)))
(defn context-2d [canvas] (.getContext canvas "2d"))
(defn close-window! [] (.close js/window))

(defn download [url filename]
  (let [a (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) filename)
    (.click a)))

(defn download-data [data content-type filename]
  (let [blob (new js/Blob (clj->js [data]) (clj->js {:type content-type}))
        url (.createObjectURL js/URL blob)]
    (download url filename)
    (timeout 100 #(.revokeObjectURL js/URL url))))

(defn copy-to-clipboard-fallback [text]
  (let [textarea (.createElement js/document "textarea")
        body (.-body js/document)]
    (set! (.-textContent textarea) text)
    (.appendChild body textarea)
    (let [selection (.getSelection js/document)
          range (.createRange js/document)]
      (.selectNode range textarea)
      (.removeAllRanges selection)
      (.addRange selection range)
      (.execCommand js/document "copy")
      (.removeAllRanges selection)
      (.removeChild body textarea))))

(defn copy-to-clipboard [text]
  (if-let [clipboard (.-clipboard js/navigator)]
    (.writeText clipboard text)
    (copy-to-clipboard-fallback text)))

(defn begin-path! [ctx] (.beginPath ctx))
(defn stroke! [ctx] (.stroke ctx))
(defn fill! [ctx] (.fill ctx))
(defn line-width= [ctx w] (set! (.-lineWidth ctx) w))
(defn stroke-color= [ctx color] (set! (.-strokeStyle ctx) color))
(defn fill-color= [ctx color] (set! (.-fillStyle ctx) color))
(defn font= [ctx font] (set! (.-font ctx) font))
(defn text-align= [ctx align] (set! (.-textAlign ctx) align))
(defn close-path! [ctx] (.closePath ctx))
(defn move-to! [ctx [x y]] (.moveTo ctx x y))
(defn line-to! [ctx [x y]] (.lineTo ctx x y))
(defn fill-rect! [ctx [x1 y1] [x2 y2]] (.fillRect ctx x1 y1 x2 y2))
(defn stroke-rect! [ctx [x1 y1] [x2 y2]] (.strokeRect ctx x1 y1 x2 y2))
(defn fill-text! [ctx text [x y]] (.fillText ctx text x y))

