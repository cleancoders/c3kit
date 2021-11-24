(ns c3kit.wire.demo-spec
  (:require-macros [speclj.core :refer [describe context it should-not-be-nil should-be-nil should= should-not
                                        should-not= should-have-invoked after before before-all with-stubs with around
                                        stub should-contain should-not-contain should]]
                   [c3kit.wire.spec-helperc :refer [should-select should-not-select should-have-invoked-ws]])
  (:require [c3kit.wire.spec-helper :as helper]
            [c3kit.wire.dnd-demo :as sut]
            [clojure.string :as string]))

(def colors [{:id :red :name "red" :color "red" :owner :colors :next :orange} {:id :orange :name "orange" :color "orange" :owner :colors :next :yellow} {:id :yellow :name "yellow" :color "yellow" :owner :colors :next :green} {:id :green :name "green" :color "green" :owner :colors :next :blue} {:id :blue :name "blue" :color "blue" :owner :colors :next :indigo} {:id :indigo :name "indigo" :color "indigo" :owner :colors :next :violet} {:id :violet :name "violet" :color "blueviolet" :owner :colors}])
(def monster-trucks [{:id :megalodon :name "Megalodon" :owner :trucks :next :el-toro-loco}
                     {:id :el-toro-loco :name "El Toro Loco" :owner :trucks :next :grave-digger}
                     {:id :grave-digger :name "Grave Digger" :owner :trucks :next :earth-shaker}
                     {:id :earth-shaker :name "EarthShaker" :owner :trucks :next :son-uva-digger}
                     {:id :son-uva-digger :name "Son-uva Digger" :owner :trucks :next :ice-cream-man}
                     {:id :ice-cream-man :name "Ice Cream Man" :owner :trucks :next :hurricane-force}
                     {:id :hurricane-force :name "Hurricane Force" :owner :trucks :next :monster-mutt-rottweiler}
                     {:id :monster-mutt-rottweiler :name "Monster Mutt Rottweiler" :owner :trucks :next :blaze}
                     {:id :blaze :name "Blaze" :owner :trucks :next :dragon}
                     {:id :dragon :name "Dragon" :owner :trucks}])

(describe "Dragon Drops"
  (before (reset! sut/golf-state (assoc (sut/golf-locations) :score 0))
          (reset! sut/rainbow-state {:colors {:first-item :red}})
          (reset! sut/colors colors)
          (reset! sut/monster-jam-state {:trucks {:first-item :megalodon} :team {:first-item nil}})
          (reset! sut/monster-trucks monster-trucks))

  (context "content"
    (helper/with-root-dom)
    (before
      (helper/render [sut/content])
      (helper/flush))

    (it "shows demo items"
      (should-select "#golf-demo")
      (should-select "#rainbow-demo")
      (should-select "#team-demo"))

    (it "golf shows a ball and a hole"
      (should-select "#ball-1")
      (should-select "#ball-2")
      (should-select "#hole-1")
      (should-select "#hole-2")
      (should= [:ball :hole] (keys (get-in @sut/golf-dnd [:groups]))))

    (for [color @sut/colors]
      (it (str "rainbow shows colors: " (:name color))
        (should-select (str "#-color-" (:name color)))))

    (for [truck @sut/monster-trucks]
      (it (str "monster trucks shows: " (:name truck))
        (should-select (str "#-truck-" (-> (:name truck) string/lower-case (string/replace #"\s" "-"))))))
    )

  (context "golf"

    (it "grabs a ball on hover"
      (helper/mouse-enter! "#ball-1")
      (helper/flush)
      (should= :ball-1 (:hover @sut/golf-state))
      (should= "golf-ball grab" (helper/class-name "#ball-1"))
      (helper/mouse-leave! "#ball-1")
      (helper/flush)
      (should-be-nil (:hover @sut/golf-state))
      (should= "golf-ball" (helper/class-name "#ball-1")))

    (it "balls are draggable"
      (let [draggables [:node :draggable-mousedown]]
        (should= [:ball-1 :ball-2] (keys (get-in @sut/golf-dnd [:groups :ball :members])))
        (should= draggables (keys (get-in @sut/golf-dnd [:groups :ball :members :ball-1])))
        (should= draggables (keys (get-in @sut/golf-dnd [:groups :ball :members :ball-2])))))

    (it "holes are droppable"
      (let [droppables [:node :droppable-mouseenter :droppable-mouseleave]]
        (should= [:hole-1 :hole-2] (keys (get-in @sut/golf-dnd [:groups :hole :members])))
        (should= droppables (keys (get-in @sut/golf-dnd [:groups :hole :members :hole-1])))
        (should= droppables (keys (get-in @sut/golf-dnd [:groups :hole :members :hole-2])))))

    (it "drags a ball to a hole"
      (swap! sut/golf-dnd assoc :source-key :ball-1)
      (sut/golf-drag-started @sut/golf-dnd)
      (should-be-nil (:hover @sut/golf-state))
      (should= :ball-1 (:dragging @sut/golf-state))
      (swap! sut/golf-dnd assoc :target-key :hole-1)
      (sut/drag-over @sut/golf-dnd)
      (should= :hole-1 (:drop-hole @sut/golf-state))
      )

    (it "drags ball to hole then out of hole"
      (swap! sut/golf-dnd assoc :source-key :ball-1)
      (swap! sut/golf-dnd assoc :target-key :hole-1)
      (let [golf-locations [(:ball-location @sut/golf-state) (:hole-location @sut/golf-state)]]
        (sut/drag-over @sut/golf-dnd)
        (should= :hole-1 (:drop-hole @sut/golf-state))
        (sut/drag-out @sut/golf-dnd)
        (should= nil (:drop-hole @sut/golf-state))
        (sut/golf-drag-end)
        (should= golf-locations [(:ball-location @sut/golf-state) (:hole-location @sut/golf-state)])
        (should= 0 (:score @sut/golf-state))
        (map #(should-be-nil (% @sut/golf-state)) [:dragging :hover :drop-hole])))

    (it "drops ball-1 in hole-1"
      (swap! sut/golf-dnd assoc :source-key :ball-1 :target-key :hole-1)
      (let [golf-locations [(:ball-location @sut/golf-state) (:hole-location @sut/golf-state)]]
        (sut/drag-over @sut/golf-dnd)
        (should= :hole-1 (:drop-hole @sut/golf-state))
        (sut/golf-drop @sut/golf-dnd)
        (sut/golf-drag-end)
        (should-not= golf-locations [(:ball-location @sut/golf-state) (:hole-location @sut/golf-state)])
        (should= 1 (:score @sut/golf-state))
        (map #(should-be-nil (% @sut/golf-state)) [:dragging :hover :drop-hole])))

    (it "drops ball-2 in hole-2"
      (swap! sut/golf-dnd assoc :source-key :ball-2 :target-key :hole-2)
      (let [golf-locations [(:ball-location @sut/golf-state) (:hole-location @sut/golf-state)]]
        (sut/drag-over @sut/golf-dnd)
        (should= :hole-2 (:drop-hole @sut/golf-state))
        (sut/golf-drop @sut/golf-dnd)
        (sut/golf-drag-end)
        (should-not= golf-locations [(:ball-location @sut/golf-state) (:hole-location @sut/golf-state)])
        (should= 1 (:score @sut/golf-state))
        (map #(should-be-nil (% @sut/golf-state)) [:dragging :hover :drop-hole])))
    )


  (context "lists"
    (context "Monster Jam - multiple lists"
      (before (reset! sut/monster-trucks [{:id :megalodon :name "Megalodon" :owner :trucks :next :son-uva-digger}
                                          {:id :son-uva-digger :name "Son-Uva-Digger" :owner :trucks :next :dragon}
                                          {:id :dragon :name "Dragon" :owner :trucks :next :grave-digger}
                                          {:id :grave-digger :name "Grave Digger" :owner :trucks}]))

      (it "gets item order"
        (let [first-element-id (sut/get-first-element-id sut/monster-jam-state :trucks)
              first-element    (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks)
              order            (sut/get-items-order @sut/monster-trucks :trucks first-element)]
          (should= :megalodon first-element-id)
          (should= {:id :megalodon :name "Megalodon" :owner :trucks :next :son-uva-digger} first-element)
          (should= ["Megalodon" "Son-Uva-Digger" "Dragon" "Grave Digger"] (map :name order))))

      (it "starts a drag with first element"
        (let [one (sut/get-element-by-id :megalodon @sut/monster-trucks)]
          (sut/truck-drag-started {:source-key :megalodon})
          (should-not-contain one @sut/monster-trucks)
          (should= :son-uva-digger (get-in @sut/monster-jam-state [:trucks :first-item]))))

      (it "starts a drag with not-first element"
        (let [two (sut/get-element-by-id :son-uva-digger @sut/monster-trucks)]
          (sut/truck-drag-started {:source-key :son-uva-digger})
          (should-not-contain two @sut/monster-trucks)
          (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
          (should= :dragon (:next (sut/get-element-by-id :megalodon @sut/monster-trucks)))))

      (context "update truck order"
        (it "moves to nil drop point"
          (sut/truck-drag-started {:source-key :son-uva-digger})
          (sut/truck-drag-end sut/monster-jam-dnd)
          (should-be-nil (:original-state @sut/monster-jam-state))
          (should-be-nil (:dragging @sut/monster-jam-state))
          (should-be-nil (:hover @sut/monster-jam-state))
          (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
          (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
            (should= ["Megalodon" "Son-Uva-Digger" "Dragon" "Grave Digger"] (map :name items))))

        (context "moves within same list"
          (it "moves one before three (move first element down one)"
            (sut/truck-drag-started {:source-key :megalodon})
            (sut/update-order {:source-key :megalodon :target-key :dragon} sut/monster-jam-state sut/monster-trucks)
            (should= :son-uva-digger (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Son-Uva-Digger" "Megalodon" "Dragon" "Grave Digger"] (map :name items))))

          (it "moves two before four"
            (sut/truck-drag-started {:source-key :son-uva-digger})
            (sut/update-order {:source-key :son-uva-digger :target-key :grave-digger} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Megalodon" "Dragon" "Son-Uva-Digger" "Grave Digger"] (map :name items))))

          (it "moves two to last"
            (sut/truck-drag-started {:source-key :son-uva-digger})
            (sut/update-order {:source-key :son-uva-digger :target-key :after-trucks} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Megalodon" "Dragon" "Grave Digger" "Son-Uva-Digger"] (map :name items))))

          (it "moves one to last"
            (sut/truck-drag-started {:source-key :megalodon})
            (sut/update-order {:source-key :megalodon :target-key :after-trucks} sut/monster-jam-state sut/monster-trucks)
            (should= :son-uva-digger (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Son-Uva-Digger" "Dragon" "Grave Digger" "Megalodon"] (map :name items))))

          (it "moves three to first"
            (sut/truck-drag-started {:source-key :dragon})
            (sut/update-order {:source-key :dragon :target-key :megalodon} sut/monster-jam-state sut/monster-trucks)
            (should= :dragon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Dragon" "Megalodon" "Son-Uva-Digger" "Grave Digger"] (map :name items))))

          (it "moves four to first"
            (sut/truck-drag-started {:source-key :grave-digger})
            (sut/update-order {:source-key :grave-digger :target-key :megalodon} sut/monster-jam-state sut/monster-trucks)
            (should= :grave-digger (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Grave Digger" "Megalodon" "Son-Uva-Digger" "Dragon"] (map :name items))))

          (it "moves three before two"
            (sut/truck-drag-started {:source-key :dragon})
            (sut/update-order {:source-key :dragon :target-key :son-uva-digger} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (let [items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))]
              (should= ["Megalodon" "Dragon" "Son-Uva-Digger" "Grave Digger"] (map :name items))))
          )

        (context "moves to a different list"
          (it "moves two as first element of second list"
            (sut/truck-drag-started {:source-key :son-uva-digger})
            (sut/update-order {:source-key :son-uva-digger :target-key :team} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (should= :son-uva-digger (get-in @sut/monster-jam-state [:team :first-item]))
            (let [truck-items    (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))
                  team-items     (sut/get-items-order @sut/monster-trucks :team (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :team))
                  son-uva-digger (sut/get-element-by-id :son-uva-digger @sut/monster-trucks)]
              (should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
              (should= :team (:owner son-uva-digger))
              (should= ["Son-Uva-Digger"] (map :name team-items))))

          (it "moves two to end of populated second list"
            (swap! sut/monster-trucks conj {:id :earth-shaker :name "Earth Shaker" :owner :team})
            (swap! sut/monster-jam-state assoc-in [:team :first-item] :earth-shaker)
            (sut/truck-drag-started {:source-key :son-uva-digger})
            (sut/update-order {:source-key :son-uva-digger :target-key :team} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (should= :earth-shaker (get-in @sut/monster-jam-state [:team :first-item]))
            (let [truck-items    (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))
                  team-items     (sut/get-items-order @sut/monster-trucks :team (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :team))
                  son-uva-digger (sut/get-element-by-id :son-uva-digger @sut/monster-trucks)]
              (should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
              (should= :team (:owner son-uva-digger))
              (should= ["Earth Shaker" "Son-Uva-Digger"] (map :name team-items))))

          (it "moves two before existing first element in second list"
            (swap! sut/monster-trucks conj {:id :earth-shaker :name "Earth Shaker" :owner :team})
            (swap! sut/monster-jam-state assoc-in [:team :first-item] :earth-shaker)
            (sut/truck-drag-started {:source-key :son-uva-digger})
            (sut/update-order {:source-key :son-uva-digger :target-key :earth-shaker} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (should= :son-uva-digger (get-in @sut/monster-jam-state [:team :first-item]))
            (let [truck-items    (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))
                  team-items     (sut/get-items-order @sut/monster-trucks :team (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :team))
                  son-uva-digger (sut/get-element-by-id :son-uva-digger @sut/monster-trucks)]
              (should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
              (should= :team (:owner son-uva-digger))
              (should= ["Son-Uva-Digger" "Earth Shaker"] (map :name team-items))))

          (it "moves two between two existing elements in second list"
            (swap! sut/monster-trucks conj {:id :earth-shaker :name "Earth Shaker" :owner :team :next :monster-mutt-rottweiler})
            (swap! sut/monster-trucks conj {:id :monster-mutt-rottweiler :name "Monster Mutt Rottweiler" :owner :team})
            (swap! sut/monster-jam-state assoc-in [:team :first-item] :earth-shaker)
            (sut/truck-drag-started {:source-key :son-uva-digger})
            (sut/update-order {:source-key :son-uva-digger :target-key :monster-mutt-rottweiler} sut/monster-jam-state sut/monster-trucks)
            (should= :megalodon (get-in @sut/monster-jam-state [:trucks :first-item]))
            (should= :earth-shaker (get-in @sut/monster-jam-state [:team :first-item]))
            (let [truck-items    (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))
                  team-items     (sut/get-items-order @sut/monster-trucks :team (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :team))
                  son-uva-digger (sut/get-element-by-id :son-uva-digger @sut/monster-trucks)]
              (should= ["Megalodon" "Dragon" "Grave Digger"] (map :name truck-items))
              (should= :team (:owner son-uva-digger))
              (should= ["Earth Shaker" "Son-Uva-Digger" "Monster Mutt Rottweiler"] (map :name team-items))))

          (it "moves first element of list 1 to first element of list 2"
            (sut/truck-drag-started {:source-key :megalodon})
            (sut/update-order {:source-key :megalodon :target-key :team} sut/monster-jam-state sut/monster-trucks)
            (should= :son-uva-digger (get-in @sut/monster-jam-state [:trucks :first-item]))
            (should= :megalodon (get-in @sut/monster-jam-state [:team :first-item]))
            (let [truck-items (sut/get-items-order @sut/monster-trucks :trucks (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :trucks))
                  team-items  (sut/get-items-order @sut/monster-trucks :team (sut/get-first-element sut/monster-jam-state @sut/monster-trucks :team))
                  megalodon   (sut/get-element-by-id :megalodon @sut/monster-trucks)]
              (should= ["Son-Uva-Digger" "Dragon" "Grave Digger"] (map :name truck-items))
              (should= :team (:owner megalodon))
              (should= ["Megalodon"] (map :name team-items))))

          (it "only allows 5 elements on team"
            (reset! sut/monster-trucks [{:id :megalodon :name "Megalodon" :owner :trucks}
                                        {:id :earth-shaker :name "Earth Shaker" :owner :team :next :son-uva-digger}
                                        {:id :son-uva-digger :name "Son-Uva-Digger" :owner :team :next :dragon}
                                        {:id :dragon :name "Dragon" :owner :team :next :monster-mutt-rottweiler}
                                        {:id :monster-mutt-rottweiler :name "Monster Mutt Rottweiler" :owner :team :next :grave-digger}
                                        {:id :grave-digger :name "Grave Digger" :owner :team}])
            (swap! sut/monster-jam-state assoc-in [:team :first-item] :earth-shaker))

          )
        )
      (context "dragon drop"

        (it "grabs a truck on hover"
          (helper/mouse-enter! "#-truck-wrapper-megalodon")
          (helper/flush)
          (should= :megalodon (:hover @sut/monster-jam-state))
          (should= "-truck-wrapper grab" (helper/class-name "#-truck-wrapper-megalodon"))
          (helper/mouse-leave! "#-truck-wrapper-megalodon")
          (helper/flush)
          (should-be-nil (:hover @sut/monster-jam-state))
          (should= "-truck-wrapper" (helper/class-name "#-truck-wrapper-megalodon")))

        (it "trucks are draggable"
          (let [draggables [:node :draggable-mousedown :droppable-mouseenter :droppable-mouseleave]]
            (should= #{:megalodon :grave-digger :son-uva-digger :dragon :after-trucks} (set (keys (get-in @sut/monster-jam-dnd [:groups :truck :members]))))
            (for [truck (map :id @sut/monster-trucks)]
              (should= draggables (keys (get-in @sut/monster-jam-dnd [:groups :truck :members truck]))))))

        (it "trucks are droppable"
          (let [droppables [:node :draggable-mousedown :droppable-mouseenter :droppable-mouseleave]]
            (should= #{:megalodon :grave-digger :son-uva-digger :dragon :after-trucks} (set (keys (get-in @sut/monster-jam-dnd [:groups :truck :members]))))
            (should= #{:team} (set (keys (get-in @sut/monster-jam-dnd [:groups :truck-drop :members]))))
            (for [truck (map :id @sut/monster-trucks)]
              (should= droppables (keys (get-in @sut/monster-jam-dnd [:groups :truck :members truck]))))
            ))

        (it "drags a megalodon to a monster-mutt-rottweiler"
          (swap! sut/monster-jam-dnd assoc :source-key :megalodon)
          (sut/truck-drag-started @sut/monster-jam-dnd)
          (should-be-nil (:hover @sut/monster-jam-state))
          (should= :megalodon (:dragging @sut/monster-jam-state))
          (swap! sut/monster-jam-dnd assoc :target-key :monster-mutt-rottweiler)
          (sut/drag-over-truck @sut/monster-jam-dnd)
          (should= :monster-mutt-rottweiler (:drop-truck @sut/monster-jam-state))
          )

        (it "drags megalodon to monster-mutt-rottweiler then out of monster-mutt-rottweiler"
          (swap! sut/monster-jam-dnd assoc :source-key :megalodon)
          (swap! sut/monster-jam-dnd assoc :target-key :monster-mutt-rottweiler)
          (sut/drag-over-truck @sut/monster-jam-dnd)
          (should= :monster-mutt-rottweiler (:drop-truck @sut/monster-jam-state))
          (sut/drag-out-truck @sut/monster-jam-dnd)
          (should= nil (:drop-truck @sut/monster-jam-state))
          (sut/truck-drag-end @sut/monster-jam-dnd)
          (map #(should-be-nil (% @sut/monster-jam-state)) [:dragging :hover :drop-truck]))

        (it "drops megalodon on monster-mutt-rottweiler"
          (swap! sut/monster-jam-dnd assoc :source-key :megalodon :target-key :monster-mutt-rottweiler)
          (sut/drag-over-truck @sut/monster-jam-dnd)
          (should= :monster-mutt-rottweiler (:drop-truck @sut/monster-jam-state))
          (sut/truck-drop @sut/monster-jam-dnd)
          (sut/truck-drag-end @sut/monster-jam-dnd)
          (map #(should-be-nil (% @sut/monster-jam-state)) [:dragging :hover :drop-truck]))

        (it "drops grave-digger on son-uva-digger"
          (swap! sut/monster-jam-dnd assoc :source-key :grave-digger :target-key :son-uva-digger)
          (sut/drag-over-truck @sut/monster-jam-dnd)
          (should= :son-uva-digger (:drop-truck @sut/monster-jam-state))
          (sut/truck-drop @sut/monster-jam-dnd)
          (sut/truck-drag-end @sut/monster-jam-dnd)
          (map #(should-be-nil (% @sut/monster-jam-state)) [:dragging :hover :drop-truck]))
        )
      )

    (context "Rainbow - single list"
      (before (reset! sut/colors colors)
              (reset! sut/rainbow-state {:colors {:first-item :red}}))

      (context "re-order list"
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

      (context "dragon drop"

        (it "grabs a color on hover"
          (helper/mouse-enter! "#-color-wrapper-red")
          (helper/flush)
          (should= :red (:hover @sut/rainbow-state))
          (should= "-color-wrapper grab" (helper/class-name "#-color-wrapper-red"))
          (helper/mouse-leave! "#-color-wrapper-red")
          (helper/flush)
          (should-be-nil (:hover @sut/rainbow-state))
          (should= "-color-wrapper" (helper/class-name "#-color-wrapper-red"))
          )

        (it "colors are draggable"
          (let [draggables [:node :draggable-mousedown :droppable-mouseenter :droppable-mouseleave]]
            (should= [:red :orange :yellow :green :blue :indigo :violet :after] (keys (get-in @sut/rainbow-dnd [:groups :color :members])))
            (should= draggables (keys (get-in @sut/rainbow-dnd [:groups :color :members :red])))
            (should= draggables (keys (get-in @sut/rainbow-dnd [:groups :color :members :violet])))))

        (it "colors are droppable"
          (let [droppables [:node :draggable-mousedown :droppable-mouseenter :droppable-mouseleave]]
            (should= [:red :orange :yellow :green :blue :indigo :violet :after] (keys (get-in @sut/rainbow-dnd [:groups :color :members])))
            (should= droppables (keys (get-in @sut/rainbow-dnd [:groups :color :members :indigo])))
            (should= droppables (keys (get-in @sut/rainbow-dnd [:groups :color :members :orange])))))

        (it "drags a red to a indigo"
          (swap! sut/rainbow-dnd assoc :source-key :red)
          (sut/color-drag-started @sut/rainbow-dnd)
          (should-be-nil (:hover @sut/rainbow-state))
          (should= :red (:dragging @sut/rainbow-state))
          (swap! sut/rainbow-dnd assoc :target-key :indigo)
          (sut/drag-over-color @sut/rainbow-dnd)
          (should= :indigo (:drop-color @sut/rainbow-state))
          )

        (it "drags red to indigo then out of indigo"
          (swap! sut/rainbow-dnd assoc :source-key :red)
          (swap! sut/rainbow-dnd assoc :target-key :indigo)
          (sut/drag-over-color @sut/rainbow-dnd)
          (should= :indigo (:drop-color @sut/rainbow-state))
          (sut/drag-out-color @sut/rainbow-dnd)
          (should= nil (:drop-color @sut/rainbow-state))
          (sut/color-drag-end @sut/rainbow-dnd)
          (map #(should-be-nil (% @sut/rainbow-state)) [:dragging :hover :drop-color]))

        (it "drops red in indigo"
          (swap! sut/rainbow-dnd assoc :source-key :red :target-key :indigo)
          (sut/drag-over-color @sut/rainbow-dnd)
          (should= :indigo (:drop-color @sut/rainbow-state))
          (sut/color-drop @sut/rainbow-dnd)
          (sut/color-drag-end @sut/rainbow-dnd)
          (map #(should-be-nil (% @sut/rainbow-state)) [:dragging :hover :drop-color]))

        (it "drops violet in orange"
          (swap! sut/rainbow-dnd assoc :source-key :violet :target-key :orange)
          (sut/drag-over-color @sut/rainbow-dnd)
          (should= :orange (:drop-color @sut/rainbow-state))
          (sut/color-drop @sut/rainbow-dnd)
          (sut/color-drag-end @sut/rainbow-dnd)
          (map #(should-be-nil (% @sut/rainbow-state)) [:dragging :hover :drop-color])))

      )
    )
  )
