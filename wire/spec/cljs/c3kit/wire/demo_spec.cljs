(ns c3kit.wire.demo-spec
		(:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
																																								should-not= should-have-invoked after before before-all with-stubs with around
																																								stub should-contain should-not-contain should]])
		(:require [c3kit.wire.dnd-demo :as sut]
												[c3kit.wire.spec-helper :as helper]))


(describe "Dragon Drops"
		(context "re-order list"
				(before (reset! sut/colors {:red {:name "red" :color "red" :next :orange} :orange {:name "orange" :color "orange" :next :yellow} :yellow {:name "yellow" :color "yellow" :next :green} :green {:name "green" :color "green" :next :blue} :blue {:name "blue" :color "blue" :next :indigo} :indigo {:name "indigo" :color "indigo" :next :violet} :violet {:name "violet" :color "blueviolet"}})
						(reset! sut/rainbow-state {:first-item :red}))

				(it "moves red after orange (move first element down one)"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :red))
						(sut/update-order {:source-key :red :target-key :orange})
						(should= :orange (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= {:name "orange" :color "orange" :next :red} (first colors))
								(should= {:name "red" :color "red" :next :yellow} (second colors))))

				(it "moves orange after blue"
						(sut/color-drag-started {:source-key :orange})
						;(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :orange))
						(sut/update-order {:source-key :orange :target-key :blue})
						(should= :red (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= ["red" "yellow" "green" "blue" "orange" "indigo" "violet"] (map :name colors))))

				(it "moves orange to last"
						(sut/color-drag-started {:source-key :orange})
						;(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :orange))
						(sut/update-order {:source-key :orange :target-key :violet})
						(should= :red (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= ["red" "yellow" "green" "blue" "indigo" "violet" "orange"] (map :name colors))))

				(it "moves red to last"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :red))
						(sut/update-order {:source-key :red :target-key :violet})
						(should= :orange (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= ["orange" "yellow" "green" "blue" "indigo" "violet" "red"] (map :name colors))))

				(it "moves blue to first"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :blue))
						(sut/update-order {:source-key :blue :target-key :before})
						(should= :blue (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= ["blue" "red" "orange" "yellow" "green" "indigo" "violet"] (map :name colors))))

				(it "moves violet to first"
						(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :violet))
						(sut/update-order {:source-key :violet :target-key :before})
						(should= :violet (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= ["violet" "red" "orange" "yellow" "green" "blue" "indigo"] (map :name colors))))

				(it "moves indigo before orange"
						(sut/color-drag-started {:source-key :indigo})
						;(swap! sut/rainbow-state assoc :dragged-color (get @sut/colors :indigo))
						(sut/update-order {:source-key :indigo :target-key :red})
						(should= :red (:first-item @sut/rainbow-state))
						(let [colors (sut/get-color-order (get @sut/colors (:first-item @sut/rainbow-state)) sut/colors)]
								(should= ["red" "indigo" "orange" "yellow" "green" "blue" "violet"] (map :name colors))))
				)

		(context "Monster Jam"
				(before (reset! sut/teams-state {:trucks {:first-item "one"} :team {:first-item nil}})
						(reset! sut/all-trucks [{:name "one" :owner :trucks :next "two"}
																														{:name "two" :owner :trucks :next "three"}
																														{:name "three" :owner :trucks :next "four"}
																														{:name "four" :owner :trucks}]))

				(it "gets item order"
						(let [order (sut/get-jam-order :trucks)]
								(should= ["one" "two" "three" "four"] (map :name order))))

				(it "starts a drag with first element"
						(let [one (sut/get-truck-by-owner "one" @sut/all-trucks)]
								(sut/truck-drag-started {:source-key "one"})
								(should-not-contain one @sut/all-trucks)
								(should= "two" (get-in @sut/teams-state [:trucks :first-item]))))

				(it "starts a drag with not-first element"
						(let [two (sut/get-truck-by-owner "two" @sut/all-trucks)]
								(sut/truck-drag-started {:source-key "two"})
								(should-not-contain two @sut/all-trucks)
								(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
								(should= "three" (:next (sut/get-truck-by-owner "one" @sut/all-trucks)))))

				(context "update truck order"
						(it "moves to nil drop point"
								(sut/truck-drag-started {:source-key "two"})
								(sut/update-truck-order {:source-key "two" :target-key nil})
								(sut/truck-drag-end sut/teams-dnd)
								(should-be-nil (:original-state @sut/teams-state))
								(should-be-nil (:dragging @sut/teams-state))
								(should-be-nil (:hover @sut/teams-state))
								(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
								(let [items (sut/get-jam-order :trucks)]
										(should= ["one" "two" "three" "four"] (map :name items))))

						(context "moves within same list"
								(it "moves one after two (move first element down one)"
										(sut/truck-drag-started {:source-key "one"})
										(sut/update-truck-order {:source-key "one" :target-key "two"})
										(should= "two" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["two" "one" "three" "four"] (map :name items))))

								(it "moves two after three"
										(sut/truck-drag-started {:source-key "two"})
										(sut/update-truck-order {:source-key "two" :target-key "three"})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["one" "three" "two" "four"] (map :name items))))

								(it "moves two to last"
										(sut/truck-drag-started {:source-key "two"})
										(sut/update-truck-order {:source-key "two" :target-key "four"})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["one" "three" "four" "two"] (map :name items))))

								(it "moves one to last"
										(sut/truck-drag-started {:source-key "one"})
										(sut/update-truck-order {:source-key "one" :target-key "four"})
										(should= "two" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["two" "three" "four" "one"] (map :name items))))

								(it "moves three to first"
										(sut/truck-drag-started {:source-key "three"})
										(sut/update-truck-order {:source-key "three" :target-key :before-trucks})
										(should= "three" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["three" "one" "two" "four"] (map :name items))))

								(it "moves four to first"
										(sut/truck-drag-started {:source-key "four"})
										(sut/update-truck-order {:source-key "four" :target-key :before-trucks})
										(should= "four" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["four" "one" "two" "three"] (map :name items))))

								(it "moves three before two"
										(sut/truck-drag-started {:source-key "three"})
										(sut/update-truck-order {:source-key "three" :target-key "one"})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-jam-order :trucks)]
												(should= ["one" "three" "two" "four"] (map :name items))))
								)

						(context "moves to a different list"
								(it "moves two as first element of second list"
										(sut/truck-drag-started {:source-key "two"})
										(sut/update-truck-order {:source-key "two" :target-key :team})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(should= "two" (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-jam-order :trucks)
																team-items  (sut/get-jam-order :team)
																two (sut/get-truck-by-owner "two" @sut/all-trucks)]
												(println "team-items: " team-items)
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["two"] (map :name team-items))))

								(it "moves two to end of populated second list"
										(swap! sut/all-trucks conj {:name "seven" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] "seven")
										(sut/truck-drag-started {:source-key "two"})
										(sut/update-truck-order {:source-key "two" :target-key :team})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(should= "seven" (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-jam-order :trucks)
																team-items  (sut/get-jam-order :team)
																two (sut/get-truck-by-owner "two" @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["seven" "two"] (map :name team-items))))

								(it "moves two before existing first element in second list"
										(swap! sut/all-trucks conj {:name "seven" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] "seven")
										(sut/truck-drag-started {:source-key "two"})
										(sut/update-truck-order {:source-key "two" :target-key :before-team})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(should= "two" (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-jam-order :trucks)
																team-items  (sut/get-jam-order :team)
																two (sut/get-truck-by-owner "two" @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["two" "seven"] (map :name team-items))))

								(it "moves two between two existing elements in second list"
										(swap! sut/all-trucks conj {:name "seven" :owner :team :next "eight"})
										(swap! sut/all-trucks conj {:name "eight" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] "seven")
										(sut/truck-drag-started {:source-key "two"})
										(sut/update-truck-order {:source-key "two" :target-key "seven"})
										(should= "one" (get-in @sut/teams-state [:trucks :first-item]))
										(should= "seven" (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-jam-order :trucks)
																team-items  (sut/get-jam-order :team)
																two (sut/get-truck-by-owner "two" @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["seven" "two" "eight"] (map :name team-items))))

								(it "moves first element of list 1 to first element of list 2"
										(sut/truck-drag-started {:source-key "one"})
										(sut/update-truck-order {:source-key "one" :target-key :team})
										(should= "two" (get-in @sut/teams-state [:trucks :first-item]))
										(should= "one" (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-jam-order :trucks)
																team-items  (sut/get-jam-order :team)
																one (sut/get-truck-by-owner "one" @sut/all-trucks)]
												(should= ["two" "three" "four"] (map :name truck-items))
												(should= :team (:owner one))
												(should= ["one"] (map :name team-items))))

								)
						)
				)

		)