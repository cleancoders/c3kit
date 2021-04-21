(ns c3kit.apron.cursor
  #?(:clj (:import (clojure.lang IDeref IAtom IRef IAtom2))))

(defn- do-swap!
  ([base path f] (-> (swap! base update-in path f) (get-in path)))
  ([base path f x] (-> (swap! base update-in path f x) (get-in path)))
  ([base path f x y] (-> (swap! base update-in path f x y) (get-in path)))
  ([base path f x y more] (-> (swap! base update-in path (fn [v] (apply f v x y more))) (get-in path))))

(defn- swap-vals-result [path result]
  (let [[o n] result] [(get-in o path) (get-in n path)]))

(defn- do-reset! [base path new-value]
  (swap! base assoc-in path new-value)
  new-value)

(defn- to-string [this path] (str "#<Cursor: " (pr-str @this) " @" (pr-str path) ">"))

#?(:clj
   (deftype Cursor [base path]

     IDeref
     (deref [_] (get-in @base path))

     IAtom
     (swap [_ f] (do-swap! base path f))
     (swap [_ f x] (do-swap! base path f x))
     (swap [_ f x y] (do-swap! base path f x y))
     (swap [_ f x y more] (do-swap! base path f x y more))
     (reset [_ new-value] (do-reset! base path new-value))

     IAtom2
     (swapVals [_ f] (swap-vals-result path (swap-vals! base update-in path f)))
     (swapVals [_ f x] (swap-vals-result path (swap-vals! base update-in path f x)))
     (swapVals [_ f x y](swap-vals-result path (swap-vals! base update-in path f x y)))
     (swapVals [_ f x y more] (swap-vals-result path (swap-vals! base update-in path (fn [v] (apply f v x y more)))))
     (resetVals [_ new-value] (swap-vals-result path (swap-vals! base assoc-in path new-value)))

     Object
     (toString [this] (to-string this path))

     IRef
     (setValidator [_ v] (.setValidator base v))
     (getValidator [_] (.getValidator base))
     (getWatches [_] (.getWatches base))
     (addWatch [this key f] (.addWatch base [path key] (fn [k r o n] (f key this (get-in o path) (get-in n path)))))
     (removeWatch [_ key] (.removeWatch base [path key]))
     )

   :cljs
   (deftype Cursor [base path]
     IAtom

     IDeref
     (-deref [_] (get-in @base path))

     IReset
     (-reset! [_ new-value] (do-reset! base path new-value))

     ISwap
     (-swap! [a f] (do-swap! base path f))
     (-swap! [a f x] (do-swap! base path f x))
     (-swap! [a f x y] (do-swap! base path f x y))
     (-swap! [a f x y more] (do-swap! base path f x y more))

     IPrintWithWriter
     (-pr-writer [_ writer opts]
       (-write writer "#<Cursor: ")
       (pr-writer (get-in @base path) writer opts)
       (-write writer " @")
       (pr-writer path writer opts)
       (-write writer ">"))

     IWatchable
     (-notify-watches [_ oldval newval] (-notify-watches base oldval newval))
     (-add-watch [this key f] (-add-watch base [path key] (fn [k r o n] (f key this (get-in o path) (get-in n path)))))
     (-remove-watch [_ key] (-remove-watch base [path key]))
     ))

#?(:clj (defmethod clojure.core/print-method Cursor [cursor writer]
          (.write writer "#<Cursor: ")
          (.write writer (pr-str @cursor))
          (.write writer " @")
          (.write writer (pr-str (.path cursor)))
          (.write writer ">")))


(defn cursor [a path]
  (if (seq path)
    (Cursor. a path)
    a))

