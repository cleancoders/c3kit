(ns c3kit.wire.fake-hiccup-spec
  (:require-macros [speclj.core :refer [describe context it should=]])
  (:require [c3kit.wire.fake-hiccup :as sut]
            [goog.dom :as dom]))

(defn test-content []
  [:div
   [:div
    [:h1 "treats"]
    [:ol#-treats
     [:li#-bone.treat {:style nil}]
     [:li#-catnip]
     [:li#-brusly.pet]
     [:li#-cheddar.pet]]]])

(defn get-outer-html [hiccup] (dom/getOuterHtml (sut/vector->html hiccup)))

(describe "Fake Hiccup"
  (it "nil hiccup results in nil"
    (should= nil (sut/->dom nil)))
  (it "renders vector function with no arguments"
    (should= (get-outer-html (test-content))
             (get-outer-html [test-content])))
  (it "renders function component no arguments"
    (should= (get-outer-html (test-content))
             (get-outer-html [(fn [] test-content)])))
  (it "renders function component two arguments"
    (let [component (fn [_ _]
                      (fn [name age] [:div [:p name] [:p age]]))]
      (should= (get-outer-html [:div [:p "Lucky"] [:p 3]])
               (get-outer-html [component "Lucky" 3]))))
  (it "renders vector function with one argument"
    (should= (get-outer-html [:h1 "Chocolate"])
             (get-outer-html [(fn [a] [:h1 a]) "Chocolate"])))
  (it "ignores nil hiccup values"
    (should= (get-outer-html [:h1 "Hello!"])
             (get-outer-html [:h1 nil "Hello!"])))

  (context "tag name"
    (it "h1 without classes or ids"
      (should= "h1" (sut/->tag-name :h1)))
    (it "div without classes or ids"
      (should= "div" (sut/->tag-name :div)))
    (it "an id"
      (should= "h1" (sut/->tag-name :h1#some-id)))
    (it "a class"
      (should= "h1" (sut/->tag-name :h1.some-class)))
    (it "an id, then a class"
      (should= "h1" (sut/->tag-name :h1#-some-id.some-class)))
    (it "a class, then an id"
      (should= "h1" (sut/->tag-name :h1.some-class#some-id))))

  (context "options"
    (for [hiccup [nil [] [:h1] [:h1 "Hello, world!"]]]
      (it (str "are non-existent with hiccup: " (or hiccup "nil"))
        (should= {} (sut/hiccup-options hiccup))))
    (for [options [{}
                   {:class "hello"}
                   {:id "world"}]]
      (it (str "results in " options)
        (should= options (sut/hiccup-options [:h1 options]))))
    (it "results in one id from tag name"
      (should= {:id "some-id"} (sut/hiccup-options [:h1#some-id])))
    (it "results in one class from tag name"
      (should= {:class "some-class"} (sut/hiccup-options [:h1.some-class])))
    (it "results in id from tag and class from options"
      (should= {:class "some-class" :id "some-id"} (sut/hiccup-options [:h1#some-id {:class "some-class"}])))
    (it "results in class from tag and id from options"
      (should= {:class "some-class" :id "some-id"} (sut/hiccup-options [:h1.some-class {:id "some-id"}])))
    (it "results in multiple classes and ids from options"
      (should= {:class "class-1 class-2 another-class"
                :id    "id-1 id-2 another-id"}
               (sut/hiccup-options [:h1#id-1#id-2.class-1.class-2#another-id.another-class])))
    (it "merges tag options and vector options"
      (should= {:class "hello world goodbye everyone"
                :id    "vec-id another-id chicken pollo"}
               (sut/hiccup-options [:h1#vec-id#another-id.hello.world
                                    {:id    "chicken pollo"
                                     :class "goodbye everyone"}]))))

  (context "child elements"
    (it "are non-existent when there is no content"
      (should= [] (sut/child-elements [:h1])))
    (it "are non-existent when there are vector options"
      (should= [] (sut/child-elements [:h1 {:id "hello"}])))
    (it "excludes tag and options"
      (should= [[:h2 "Chicken"] [:h3 "Beef"]]
               (sut/child-elements [:h1 {:id "goodbye"}
                                    [:h2 "Chicken"]
                                    [:h3 "Beef"]])))
    (it "is a single-element list when text is the only other item"
      (should= ["Frogs"] (sut/child-elements [:h1 "Frogs"])))
    (it "is a two-element list when there are other child elements"
      (should= ["Element one" [:h2 "Element 2"]]
               (sut/child-elements [:h1 "Element one"
                                    [:h2 "Element 2"]])))))
