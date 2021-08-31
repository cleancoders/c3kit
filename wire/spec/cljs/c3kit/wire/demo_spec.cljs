(ns c3kit.wire.demo-spec
		(:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
																																								should-not= should-have-invoked after before before-all with-stubs with around
																																								stub should-contain should-not-contain should]])
		(:require [c3kit.wire.dnd-demo :as sut]
												[c3kit.wire.spec-helper :as helper]))


(describe "Dragon Drops"
		(context "re-order list"
				(before (reset! sut/colors {:red {:name "red" :color "red" :next :orange} :orange {:name "orange" :color "orange" :next :yellow} :yellow {:name "yellow" :color "yellow" :next :green} :green {:name "green" :color "green" :next :blue} :blue {:name "blue" :color "blue" :next :indigo} :indigo {:name "indigo" :color "indigo" :next :violet} :violet {:name "violet" :color "blueviolet"}})
						(reset! sut/rainbow-state {:first-color :red}))

				(it "moves red after orange (move first element down one)"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :red))
						(sut/update-order {:source-key :red :target-key :orange})
						(should= :orange (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= {:name "orange" :color "orange" :next :red} (first colors))
								(should= {:name "red" :color "red" :next :yellow} (second colors))))

				(it "moves orange after blue"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :orange))
						(sut/update-order {:source-key :orange :target-key :blue})
						(should= :red (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= ["red" "yellow" "green" "blue" "orange" "indigo" "violet"] (map :name colors))))

				(it "moves orange to last"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :orange))
						(sut/update-order {:source-key :orange :target-key :violet})
						(should= :red (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= ["red" "yellow" "green" "blue" "indigo" "violet" "orange"] (map :name colors))))

				(it "moves red to last"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :red))
						(sut/update-order {:source-key :red :target-key :violet})
						(should= :orange (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= ["orange" "yellow" "green" "blue" "indigo" "violet" "red"] (map :name colors))))

				(it "moves blue to first"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :blue))
						(sut/update-order {:source-key :blue :target-key :before})
						(should= :blue (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= ["blue" "red" "orange" "yellow" "green" "indigo" "violet"] (map :name colors))))

				(it "moves violet to first"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :violet))
						(sut/update-order {:source-key :violet :target-key :before})
						(should= :violet (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= ["violet" "red" "orange" "yellow" "green" "blue" "indigo"] (map :name colors))))

				(it "moves indigo before orange"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :indigo))
						(sut/update-order {:source-key :indigo :target-key :red})
						(should= :red (:first-color @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-color @sut/rainbow-state)))]
								(should= ["red" "indigo" "orange" "yellow" "green" "blue" "violet"] (map :name colors))))
				)

		)