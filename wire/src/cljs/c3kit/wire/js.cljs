(ns c3kit.wire.js
  (:import [goog History])
  (:require
    [goog.object :as gobject]
    [c3kit.apron.log :as log]))

(defn e-key-code [e] (-> e .-keyCode))

; Key Codes
(def BACKSPACE 8)
(def TAB 9)
(defn TAB? [e] (= (e-key-code e) TAB))
(def ENTER 13)
(defn ENTER? [e] (= (e-key-code e) ENTER))
(def SHIFT 16)
(def ESC 27)
(defn ESC? [e] (= (e-key-code e) ESC))
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

(defn nip
  "Nip the event in the bud, before it causes any trouble."
  [e]
  (.stopPropagation e))

(defn nix
  "Nix an event: Stop what you're doing and get the hell out."
  [e]
  (nip e)
  (nod e))

(defn ancestor-where [pred node]
  (cond
    (nil? node) nil
    (pred node) node
    :else (recur pred (.-parentElement node))))

(defn timeout [millis f] (js/setTimeout f millis))
(defn clear-timeout [timeout] (js/clearTimeout timeout))
(defn interval [millis f] (js/setInterval f millis))
(defn clear-interval [interval] (js/clearInterval interval))

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

(defn o-get
  ([js-obj key] (gobject/get js-obj key nil))
  ([js-obj key default] (gobject/get js-obj key default)))

(defn o-set [js-obj key value] (gobject/set js-obj key value))

(defn e-target [e] (.-target e))
(defn e-text [e] (-> e .-target .-value))
(defn e-type [e] (-> e .-type))
(defn e-checked? [e] (-> e .-target .-checked))
(defn e-coordinates [e] [(.-clientX e) (.-clientY e)])
(defn e-left-click? [e] (= 0 (o-get e "button")))
(defn e-wheel-click? [e] (= 1 (o-get e "button")))
(defn e-right-click? [e] (= 2 (o-get e "button")))

(defn focus! [node] (when node (.focus node)))

(defn nod-n-do
  "Return function to suppress browser event with nod and call the supplied function with args."
  [a-fn & args]
  (fn [e]
    (nod e)
    (apply a-fn args)))

(defn nip-n-do
  "Return function to suppress browser event with nip and call the supplied function with args."
  [a-fn & args]
  (fn [e]
    (nip e)
    (apply a-fn args)))

(defn nix-n-do
  "Return function to suppress browser event with nix and call the supplied function with args."
  [a-fn & args]
  (fn [e]
    (nix e)
    (apply a-fn args)))


(defn document ([] js/document) ([node] (.-ownerDocument node)))
(defn doc-body [doc] (.-body doc))
(defn doc-ready-state [] (.-readyState js/document))
(defn doc-ready? [] (= "complete" (doc-ready-state)))

(defn window-open [url window-name options-string] (.open js/window url window-name options-string))
(defn window-close! [] (.close js/window))

(defn frame-window [iframe] (.-contentWindow iframe))

(defn page-title [] (.-title js/document))
(defn page-title= [title] (set! (.-title js/document) title))
(defn page-href [] (-> js/window .-location .-href))
(defn page-reload! [] (.reload (.-location js/window)))

(defn node-id [node] (o-get node "id"))
(defn node-id= [node id] (o-set node "id" id))
(defn node-width [node] (.-clientWidth node))
(defn node-height [node] (.-clientHeight node))
(defn node-size [node] [(node-width node) (node-height node)])
(defn node-value [node] (.-value node))
(defn node-parent [node] (.-parentNode node))
(defn node-children [node] (array-seq (.-childNodes node)))
(defn node-clone [node deep?] (.cloneNode node deep?))
(defn node-append-child [node child] (.appendChild node child))
(defn node-remove-child [node child] (.removeChild node child))
(defn node-style [node] (.-style node))
(defn node-add-class [node class] (.add (.-classList node) class))
(defn node-remove-class [node class] (.remove (.-classList node) class))
(defn node-classes [node] (.-className node))
(defn node-bounds [node]
  (let [rect (.getBoundingClientRect node)]
    [(.-x rect) (.-y rect) (.-width rect) (.-height rect)]))

(defn uri-encode [& stuff] (js/encodeURIComponent (apply str stuff)))
(defn post-message [window message target-domain] (.postMessage window (clj->js message) target-domain))
(defn register-post-message-handler [handler] (.addEventListener js/window "message" handler))
(defn register-storage-handler [handler] (.addEventListener js/window "storage" handler))
(defn screen-size [] [(.-width js/screen) (.-height js/screen)])
(defn element-by-id [id] (.getElementById js/document id))
(defn context-2d [canvas] (.getContext canvas "2d"))
(defn set-local-storage [key value] (.setItem js/localStorage key value))
(defn remove-local-storage [key] (.removeItem js/localStorage key))

(defn add-listener
  ([node event listener] (add-listener node event listener nil))
  ([node event listener options]
   (if node
     (.addEventListener node event listener (when options (clj->js options)))
     (log/warn "add-listener to nil node"))))

(defn remove-listener
  ([node event listener] (remove-listener node event listener nil))
  ([node event listener options]
   (if node
     (.removeEventListener node event listener (when options (clj->js options)))
     (log/warn "remove-listener to nil node"))))

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

(defn scroll-into-view [node-id]
  (.scrollIntoView (.getElementById js/document node-id) (clj->js {:behavior "smooth"})))

(defn scroll-to-top [] (.scrollTo js/window (clj->js {:behavior "smooth" :top 0})))
