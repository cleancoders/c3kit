(ns c3kit.scaffold.css.sample
  (:refer-clojure :exclude [rem])
  (:require
    [garden.def :as garden]
    [garden.units :as units]
    ))

; MDM - This is just some sample garden code to make sure it compiles alright.

(defn px [n] (units/px n))
(defn em [n] (units/em n))
(defn rem [n] (units/rem n))
(defn percent [n] (units/percent n))

(defn font-load [face weight]
  ["@font-face" {
                 :font-family (str "'" face "-" weight "'")}
   {:src         (str "url('/fonts/" face "-" weight ".woff2') format('woff2'),
                       url('/fonts/" face "-" weight ".woff') format('woff')")
    :font-weight "normal"
    :font-style  "normal"
    }]
  )

(defn font-family [face weight]
  (str "'" face "-" weight "', Helvetica, sans-serif"))

(garden/defstyles screen

  [:* :*:before :*:after {
                          :box-sizing "border-box"
                          }]

  ["::selection" {
                  :background-color "#123"
                  :color            "#456"
                  }]

  [:body :html {
                :width  "100%"
                :height "100%"
                }]

  [:html {
          :font-size (px 19)
          }]

  [:body {
          :background-color "#fff"
          :color            "#555"
          :font-family      (font-family "groldroundedslim" "extralight")
          :letter-spacing   (px 0.25)
          :line-height      (rem 1.35)
          }]
  )
