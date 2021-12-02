(ns c3kit.wire.api
  (:require
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.log :as log]
    [c3kit.wire.apic :as apic]
    [c3kit.wire.js :as cc]
    [c3kit.wire.flash :as flash]
    [c3kit.wire.flashc :as flashc]
    ))

(def config (atom {
                   :version            "undefined"
                   :redirect-fn        cc/redirect!
                   :anti-forgery-token nil
                   :csrf-token         nil
                   :ws-on-reconnected  nil
                   :ws-uri-path        "/user/websocket"
                   }))

(defn configure! [& options] (swap! config merge (ccc/->options options)))

(defn- handle-payload [handler payload]
  (try
    (handler payload)
    (catch :default e
      (log/error "AJAX handler error")
      (log/error e)
      (flash/add-error! "Oh no!  I choked on some data.  Doh!"))))

(defn- redirect [uri]
  (when-let [redirect-fn (:redirect-fn @config)]
    (redirect-fn uri)))

(def server-down-flash (flashc/create :warn "Server Maintenance - please wait a moment as we try to reconnect." true))
(def new-version-flash (flashc/create :warn (list "There is a newer version of this app.  Please "
                                                  [:a {:href "#" :on-click #(cc/redirect! (cc/page-href))} "refresh"] ".") true))

(defn new-version! [version]
  (log/warn "new version: " version ", was: " (:version @config))
  (flash/add! new-version-flash))

;; Options:    - extensible
;;  :after-all - a no-arg fn that is always invoked at the end of the entire call process
;;  :no-redirect - when truthy, redirect is ignored
;;  :on-fail - a (fn [payload] ...) that will be invoked when the response status is :fail
;;  :on-error - a (fn [payload] ...) that will be invoked when the response status is :error

(defn handle-api-response [raw-response {:keys [handler options]}]
  (flash/remove! server-down-flash)
  (log/trace "raw response: " raw-response)
  (let [response (apic/conform-response raw-response)
        {:keys [status flash payload version]} response]
    (when (seq flash)
      (doseq [f flash] (flash/add! f)))
    (when (and version (not= version (:version @config)))
      (new-version! version))
    (when (= :ok status)
      (handle-payload handler payload))
    (when (and (= :redirect status) (not (:no-redirect options)))
      (redirect (:uri response)))
    (when (= :fail status)
      (when-let [on-fail (:on-fail options)] (on-fail payload)))
    (when (= :error status)
      (when-let [on-error (:on-error options)] (on-error payload)))
    )
  (when-let [after-all (:after-all options)]
    (after-all)))
