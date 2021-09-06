(ns c3kit.wire.demo-spec
		(:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
																																								should-not= should-have-invoked after before before-all with-stubs with around
																																								stub should-contain should-not-contain should]])
		(:require [c3kit.wire.dragndrop2 :as dnd]
												[c3kit.wire.spec-helper]
												[c3kit.wire.dnd-demo :as sut]))

(def colors [{:id :red :name "red" :color "red" :owner :colors :next :orange} {:id :orange :name "orange" :color "orange" :owner :colors :next :yellow} {:id :yellow :name "yellow" :color "yellow" :owner :colors :next :green} {:id :green :name "green" :color "green" :owner :colors :next :blue} {:id :blue :name "blue" :color "blue" :owner :colors :next :indigo} {:id :indigo :name "indigo" :color "indigo" :owner :colors :next :violet} {:id :violet :name "violet" :color "blueviolet" :owner :colors}])

(describe "Dragon Drops"
		(context "re-order list"
				(before (reset! sut/colors colors)
						(reset! sut/rainbow-state {:colors {:first-item :red}}))

				(it "moves to nil drop point"
						(sut/color-drag-started {:source-key :red})
						(sut/color-drag-end sut/rainbow-dnd)
						(should-be-nil (:original-state @sut/rainbow-state))
						(should-be-nil (:dragging @sut/rainbow-state))
						(should-be-nil (:hover @sut/rainbow-state))
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "orange" "yellow" "green" "blue" "indigo" "violet"] (map :name colors))))

				(it "moves red before yellow (move first element down one)"
						(sut/color-drag-started {:source-key :red})
						(sut/update-order {:source-key :red :target-key :yellow} sut/rainbow-state sut/colors)
						(should= :orange (get-in @sut/rainbow-state [:colors :first-item]) #_(dnd/get-first-element sut/rainbow-state @sut/colors :colors))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["orange" "red" "yellow" "green" "blue" "indigo" "violet"] (map :name colors))))

				(it "moves orange before blue"
						(sut/color-drag-started {:source-key :orange})
						(sut/update-order {:source-key :orange :target-key :blue} sut/rainbow-state sut/colors)
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "yellow" "green" "orange" "blue" "indigo" "violet"] (map :name colors))))

				(it "moves orange to last"
						(sut/color-drag-started {:source-key :orange})
						(sut/update-order {:source-key :orange :target-key :after} sut/rainbow-state sut/colors)
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "yellow" "green" "blue" "indigo" "violet" "orange"] (map :name colors))))

				(it "moves red to last"
						(sut/color-drag-started {:source-key :red})
						(sut/update-order {:source-key :red :target-key :after} sut/rainbow-state sut/colors)
						(should= :orange (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["orange" "yellow" "green" "blue" "indigo" "violet" "red"] (map :name colors))))

				(it "moves blue to first"
						(sut/color-drag-started {:source-key :blue})
						(sut/update-order {:source-key :blue :target-key :red} sut/rainbow-state sut/colors)
						(should= :blue (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["blue" "red" "orange" "yellow" "green" "indigo" "violet"] (map :name colors))))

				(it "moves violet to first"
						(sut/color-drag-started {:source-key :violet})
						(sut/update-order {:source-key :violet :target-key :red} sut/rainbow-state sut/colors)
						(should= :violet (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["violet" "red" "orange" "yellow" "green" "blue" "indigo"] (map :name colors))))

				(it "moves indigo before orange"
						(sut/color-drag-started {:source-key :indigo})
						(sut/update-order {:source-key :indigo :target-key :orange} sut/rainbow-state sut/colors)
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (dnd/get-items-order @sut/colors :colors (dnd/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "indigo" "orange" "yellow" "green" "blue" "violet"] (map :name colors))))
				)

		(context "Monster Jam"
				(before (reset! sut/teams-state {:trucks {:first-item :one} :team {:first-item nil}})
						(reset! sut/all-trucks [{:id :one :name "one" :owner :trucks :next :two}
																														{:id :two :name "two" :owner :trucks :next :three}
																														{:id :three :name "three" :owner :trucks :next :four}
																														{:id :four :name "four" :owner :trucks}]))

				(it "gets item order"
						(let [first-element-id (dnd/get-first-element-id sut/teams-state :trucks)
												first-element (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks)
												order (dnd/get-items-order @sut/all-trucks :trucks first-element)]
								(should= :one first-element-id)
								(should= {:id :one :name "one" :owner :trucks :next :two} first-element)
								(should= ["one" "two" "three" "four"] (map :name order))))

				(it "starts a drag with first element"
						(let [one (dnd/get-element-by-id :one @sut/all-trucks)]
								(sut/truck-drag-started {:source-key :one})
								(should-not-contain one @sut/all-trucks)
								(should= :two (get-in @sut/teams-state [:trucks :first-item]))))

				(it "starts a drag with not-first element"
						(let [two (dnd/get-element-by-id :two @sut/all-trucks)]
								(sut/truck-drag-started {:source-key :two})
								(should-not-contain two @sut/all-trucks)
								(should= :one (get-in @sut/teams-state [:trucks :first-item]))
								(should= :three (:next (dnd/get-element-by-id :one @sut/all-trucks)))))

				(context "update truck order"
						(it "moves to nil drop point"
								(sut/truck-drag-started {:source-key :two})
								(sut/truck-drag-end sut/teams-dnd)
								(should-be-nil (:original-state @sut/teams-state))
								(should-be-nil (:dragging @sut/teams-state))
								(should-be-nil (:hover @sut/teams-state))
								(should= :one (get-in @sut/teams-state [:trucks :first-item]))
								(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
										(should= ["one" "two" "three" "four"] (map :name items))))

						(context "moves within same list"
								(it "moves one before three (move first element down one)"
										(sut/truck-drag-started {:source-key :one})
										(sut/update-order {:source-key :one :target-key :three} sut/teams-state sut/all-trucks)
										(should= :two (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["two" "one" "three" "four"] (map :name items))))

								(it "moves two before four"
										(sut/truck-drag-started {:source-key :two})
										(sut/update-order {:source-key :two :target-key :four} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["one" "three" "two" "four"] (map :name items))))

								(it "moves two to last"
										(sut/truck-drag-started {:source-key :two})
										(sut/update-order {:source-key :two :target-key :after-trucks} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["one" "three" "four" "two"] (map :name items))))

								(it "moves one to last"
										(sut/truck-drag-started {:source-key :one})
										(sut/update-order {:source-key :one :target-key :after-trucks} sut/teams-state sut/all-trucks)
										(should= :two (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["two" "three" "four" "one"] (map :name items))))

								(it "moves three to first"
										(sut/truck-drag-started {:source-key :three})
										(sut/update-order {:source-key :three :target-key :one} sut/teams-state sut/all-trucks)
										(should= :three (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["three" "one" "two" "four"] (map :name items))))

								(it "moves four to first"
										(sut/truck-drag-started {:source-key :four})
										(sut/update-order {:source-key :four :target-key :one} sut/teams-state sut/all-trucks)
										(should= :four (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["four" "one" "two" "three"] (map :name items))))

								(it "moves three before two"
										(sut/truck-drag-started {:source-key :three})
										(sut/update-order {:source-key :three :target-key :two} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["one" "three" "two" "four"] (map :name items))))
								)

						(context "moves to a different list"
								(it "moves two as first element of second list"
										(sut/truck-drag-started {:source-key :two})
										(sut/update-order {:source-key :two :target-key :team} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(should= :two (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (dnd/get-items-order @sut/all-trucks :team (dnd/get-first-element sut/teams-state @sut/all-trucks :team))
																two         (dnd/get-element-by-id :two @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["two"] (map :name team-items))))

								(it "moves two to end of populated second list"
										(swap! sut/all-trucks conj {:id :seven :name "seven" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] :seven)
										(sut/truck-drag-started {:source-key :two})
										(sut/update-order {:source-key :two :target-key :team} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(should= :seven (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (dnd/get-items-order @sut/all-trucks :team (dnd/get-first-element sut/teams-state @sut/all-trucks :team))
																two         (dnd/get-element-by-id :two @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["seven" "two"] (map :name team-items))))

								(it "moves two before existing first element in second list"
										(swap! sut/all-trucks conj {:id :seven :name "seven" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] :seven)
										(sut/truck-drag-started {:source-key :two})
										(sut/update-order {:source-key :two :target-key :seven} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(should= :two (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (dnd/get-items-order @sut/all-trucks :team (dnd/get-first-element sut/teams-state @sut/all-trucks :team))
																two         (dnd/get-element-by-id :two @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["two" "seven"] (map :name team-items))))

								(it "moves two between two existing elements in second list"
										(swap! sut/all-trucks conj {:id :seven :name "seven" :owner :team :next :eight})
										(swap! sut/all-trucks conj {:id :eight :name "eight" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] :seven)
										(sut/truck-drag-started {:source-key :two})
										(sut/update-order {:source-key :two :target-key :eight} sut/teams-state sut/all-trucks)
										(should= :one (get-in @sut/teams-state [:trucks :first-item]))
										(should= :seven (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (dnd/get-items-order @sut/all-trucks :team (dnd/get-first-element sut/teams-state @sut/all-trucks :team))
																two         (dnd/get-element-by-id :two @sut/all-trucks)]
												(should= ["one" "three" "four"] (map :name truck-items))
												(should= :team (:owner two))
												(should= ["seven" "two" "eight"] (map :name team-items))))

								(it "moves first element of list 1 to first element of list 2"
										(sut/truck-drag-started {:source-key :one})
										(sut/update-order {:source-key :one :target-key :team} sut/teams-state sut/all-trucks)
										(should= :two (get-in @sut/teams-state [:trucks :first-item]))
										(should= :one (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (dnd/get-items-order @sut/all-trucks :trucks (dnd/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (dnd/get-items-order @sut/all-trucks :team (dnd/get-first-element sut/teams-state @sut/all-trucks :team))
																one         (dnd/get-element-by-id :one @sut/all-trucks)]
												(should= ["two" "three" "four"] (map :name truck-items))
												(should= :team (:owner one))
												(should= ["one"] (map :name team-items))))

								)
						)
				)

		)