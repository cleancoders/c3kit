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
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "orange" "yellow" "green" "blue" "indigo" "violet"] (map :name colors))))

				(it "moves red before yellow (move first element down one)"
						(sut/color-drag-started {:source-key :red})
						(sut/update-order {:source-key :red :target-key :yellow} sut/rainbow-state sut/colors)
						(should= :orange (get-in @sut/rainbow-state [:colors :first-item]) #_(sut/get-first-element sut/rainbow-state @sut/colors :colors))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["orange" "red" "yellow" "green" "blue" "indigo" "violet"] (map :name colors))))

				(it "moves orange before blue"
						(sut/color-drag-started {:source-key :orange})
						(sut/update-order {:source-key :orange :target-key :blue} sut/rainbow-state sut/colors)
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "yellow" "green" "orange" "blue" "indigo" "violet"] (map :name colors))))

				(it "moves orange to last"
						(sut/color-drag-started {:source-key :orange})
						(sut/update-order {:source-key :orange :target-key :after} sut/rainbow-state sut/colors)
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "yellow" "green" "blue" "indigo" "violet" "orange"] (map :name colors))))

				(it "moves red to last"
						(sut/color-drag-started {:source-key :red})
						(sut/update-order {:source-key :red :target-key :after} sut/rainbow-state sut/colors)
						(should= :orange (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["orange" "yellow" "green" "blue" "indigo" "violet" "red"] (map :name colors))))

				(it "moves blue to first"
						(sut/color-drag-started {:source-key :blue})
						(sut/update-order {:source-key :blue :target-key :red} sut/rainbow-state sut/colors)
						(should= :blue (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["blue" "red" "orange" "yellow" "green" "indigo" "violet"] (map :name colors))))

				(it "moves violet to first"
						(sut/color-drag-started {:source-key :violet})
						(sut/update-order {:source-key :violet :target-key :red} sut/rainbow-state sut/colors)
						(should= :violet (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["violet" "red" "orange" "yellow" "green" "blue" "indigo"] (map :name colors))))

				(it "moves indigo before orange"
						(sut/color-drag-started {:source-key :indigo})
						(sut/update-order {:source-key :indigo :target-key :orange} sut/rainbow-state sut/colors)
						(should= :red (get-in @sut/rainbow-state [:colors :first-item]))
						(let [colors (sut/get-items-order @sut/colors :colors (sut/get-first-element sut/rainbow-state @sut/colors :colors))]
								(should= ["red" "indigo" "orange" "yellow" "green" "blue" "violet"] (map :name colors))))
				)

		(context "Monster Jam"
				(before (reset! sut/teams-state {:trucks {:first-item :megalodon} :team {:first-item nil}})
						(reset! sut/all-trucks [{:id :megalodon :name "Megalodon" :owner :trucks :next :son-uva-digger}
																														{:id :son-uva-digger :name "Son-Uva-Digger" :owner :trucks :next :dragon}
																														{:id :dragon :name "Dragon" :owner :trucks :next :grave-digger}
																														{:id :grave-digger :name "Grave Digger" :owner :trucks}]))

				(it "gets item order"
						(let [first-element-id (sut/get-first-element-id sut/teams-state :trucks)
												first-element (sut/get-first-element sut/teams-state @sut/all-trucks :trucks)
												order (sut/get-items-order @sut/all-trucks :trucks first-element)]
								(should= :megalodon first-element-id)
								(should= {:id :megalodon :name "Megalodon" :owner :trucks :next :son-uva-digger} first-element)
								(should= ["Megalodon" "Son-Uva-Digger" "Dragon" "Grave Digger"] (map :name order))))

				(it "starts a drag with first element"
						(let [one (sut/get-element-by-id :megalodon @sut/all-trucks)]
								(sut/truck-drag-started {:source-key :megalodon})
								(should-not-contain one @sut/all-trucks)
								(should= :son-uva-digger (get-in @sut/teams-state [:trucks :first-item]))))

				(it "starts a drag with not-first element"
						(let [two (sut/get-element-by-id :son-uva-digger @sut/all-trucks)]
								(sut/truck-drag-started {:source-key :son-uva-digger})
								(should-not-contain two @sut/all-trucks)
								(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
								(should= :dragon (:next (sut/get-element-by-id :megalodon @sut/all-trucks)))))

				(context "update truck order"
						(it "moves to nil drop point"
								(sut/truck-drag-started {:source-key :son-uva-digger})
								(sut/truck-drag-end sut/teams-dnd)
								(should-be-nil (:original-state @sut/teams-state))
								(should-be-nil (:dragging @sut/teams-state))
								(should-be-nil (:hover @sut/teams-state))
								(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
								(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
										(should= ["Megalodon" "Son-Uva-Digger" "Dragon" "Grave Digger"] (map :name items))))

						(context "moves within same list"
								(it "moves one before three (move first element down one)"
										(sut/truck-drag-started {:source-key :megalodon})
										(sut/update-order {:source-key :megalodon :target-key :dragon} sut/teams-state sut/all-trucks)
										(should= :son-uva-digger (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Son-Uva-Digger" "Megalodon" "Dragon" "Grave Digger"] (map :name items))))

								(it "moves two before four"
										(sut/truck-drag-started {:source-key :son-uva-digger})
										(sut/update-order {:source-key :son-uva-digger :target-key :grave-digger} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Megalodon" "Dragon" "Son-Uva-Digger" "Grave Digger"] (map :name items))))

								(it "moves two to last"
										(sut/truck-drag-started {:source-key :son-uva-digger})
										(sut/update-order {:source-key :son-uva-digger :target-key :after-trucks} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Megalodon" "Dragon" "Grave Digger" "Son-Uva-Digger"] (map :name items))))

								(it "moves one to last"
										(sut/truck-drag-started {:source-key :megalodon})
										(sut/update-order {:source-key :megalodon :target-key :after-trucks} sut/teams-state sut/all-trucks)
										(should= :son-uva-digger (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Son-Uva-Digger" "Dragon" "Grave Digger" "Megalodon"] (map :name items))))

								(it "moves three to first"
										(sut/truck-drag-started {:source-key :dragon})
										(sut/update-order {:source-key :dragon :target-key :megalodon} sut/teams-state sut/all-trucks)
										(should= :dragon (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Dragon" "Megalodon" "Son-Uva-Digger" "Grave Digger"] (map :name items))))

								(it "moves four to first"
										(sut/truck-drag-started {:source-key :grave-digger})
										(sut/update-order {:source-key :grave-digger :target-key :megalodon} sut/teams-state sut/all-trucks)
										(should= :grave-digger (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Grave Digger" "Megalodon" "Son-Uva-Digger" "Dragon"] (map :name items))))

								(it "moves three before two"
										(sut/truck-drag-started {:source-key :dragon})
										(sut/update-order {:source-key :dragon :target-key :son-uva-digger} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(let [items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))]
												(should= ["Megalodon" "Dragon" "Son-Uva-Digger" "Grave Digger"] (map :name items))))
								)

						(context "moves to a different list"
								(it "moves two as first element of second list"
										(sut/truck-drag-started {:source-key :son-uva-digger})
										(sut/update-order {:source-key :son-uva-digger :target-key :team} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(should= :son-uva-digger (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (sut/get-items-order @sut/all-trucks :team (sut/get-first-element sut/teams-state @sut/all-trucks :team))
																son-uva-digger         (sut/get-element-by-id :son-uva-digger @sut/all-trucks)]
												(should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
												(should= :team (:owner son-uva-digger))
												(should= ["Son-Uva-Digger"] (map :name team-items))))

								(it "moves two to end of populated second list"
										(swap! sut/all-trucks conj {:id :earth-shaker :name "Earth Shaker" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] :earth-shaker)
										(sut/truck-drag-started {:source-key :son-uva-digger})
										(sut/update-order {:source-key :son-uva-digger :target-key :team} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(should= :earth-shaker (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (sut/get-items-order @sut/all-trucks :team (sut/get-first-element sut/teams-state @sut/all-trucks :team))
																son-uva-digger         (sut/get-element-by-id :son-uva-digger @sut/all-trucks)]
												(should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
												(should= :team (:owner son-uva-digger))
												(should= ["Earth Shaker" "Son-Uva-Digger"] (map :name team-items))))

								(it "moves two before existing first element in second list"
										(swap! sut/all-trucks conj {:id :earth-shaker :name "Earth Shaker" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] :earth-shaker)
										(sut/truck-drag-started {:source-key :son-uva-digger})
										(sut/update-order {:source-key :son-uva-digger :target-key :earth-shaker} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(should= :son-uva-digger (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (sut/get-items-order @sut/all-trucks :team (sut/get-first-element sut/teams-state @sut/all-trucks :team))
																son-uva-digger         (sut/get-element-by-id :son-uva-digger @sut/all-trucks)]
												(should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
												(should= :team (:owner son-uva-digger))
												(should= ["Son-Uva-Digger" "Earth Shaker"] (map :name team-items))))

								(it "moves two between two existing elements in second list"
										(swap! sut/all-trucks conj {:id :earth-shaker :name "Earth Shaker" :owner :team :next :monster-mutt-rottweiler})
										(swap! sut/all-trucks conj {:id :monster-mutt-rottweiler :name "Monster Mutt Rottweiler" :owner :team})
										(swap! sut/teams-state assoc-in [:team :first-item] :earth-shaker)
										(sut/truck-drag-started {:source-key :son-uva-digger})
										(sut/update-order {:source-key :son-uva-digger :target-key :monster-mutt-rottweiler} sut/teams-state sut/all-trucks)
										(should= :megalodon (get-in @sut/teams-state [:trucks :first-item]))
										(should= :earth-shaker (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (sut/get-items-order @sut/all-trucks :team (sut/get-first-element sut/teams-state @sut/all-trucks :team))
																son-uva-digger         (sut/get-element-by-id :son-uva-digger @sut/all-trucks)]
												(should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
												(should= :team (:owner son-uva-digger))
												(should= ["Earth Shaker" "Son-Uva-Digger" "Monster Mutt Rottweiler"] (map :name team-items))))

								(it "moves first element of list 1 to first element of list 2"
										(sut/truck-drag-started {:source-key :megalodon})
										(sut/update-order {:source-key :megalodon :target-key :team} sut/teams-state sut/all-trucks)
										(should= :son-uva-digger (get-in @sut/teams-state [:trucks :first-item]))
										(should= :megalodon (get-in @sut/teams-state [:team :first-item]))
										(let [truck-items (sut/get-items-order @sut/all-trucks :trucks (sut/get-first-element sut/teams-state @sut/all-trucks :trucks))
																team-items  (sut/get-items-order @sut/all-trucks :team (sut/get-first-element sut/teams-state @sut/all-trucks :team))
																megalodon         (sut/get-element-by-id :megalodon @sut/all-trucks)]
												(should= ["Son-Uva-Digger" "Dragon" "Grave Digger"] (map :name truck-items))
												(should= :team (:owner megalodon))
												(should= ["Megalodon"] (map :name team-items))))

								(it "only allows 5 elements on team"
										(reset! sut/all-trucks [{:id :megalodon :name "Megalodon" :owner :trucks}
																																		{:id :earth-shaker :name "Earth Shaker" :owner :team :next :son-uva-digger}
																																		{:id :son-uva-digger :name "Son-Uva-Digger" :owner :team :next :dragon}
																																		{:id :dragon :name "Dragon" :owner :team :next :monster-mutt-rottweiler}
																																		{:id :monster-mutt-rottweiler :name "Monster Mutt Rottweiler" :owner :team :next :grave-digger}
																																		{:id :grave-digger :name "Grave Digger" :owner :team}])
										(swap! sut/teams-state assoc-in [:team :first-item] :earth-shaker))

								)
						)
				)

		)