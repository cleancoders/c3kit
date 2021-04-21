(ns c3kit.wire.flash-spec
  (:require
    [c3kit.wire.flash :as sut]
    [c3kit.wire.flashc :as flashc]
    [speclj.core :refer :all]
    ))

(describe "Flash"

          (it "success message"
              (let [request (sut/success {} "Nice one!")
                    flashes (-> request :flash :messages)
                    flash (first flashes)]
                (should= 1 (count flashes))
                (should= true (flashc/success? flash))
                (should= "Nice one!" (flashc/text flash))))

          (it "error message"
              (let [request (sut/error {} "Oh Noez!")
                    flashes (-> request :flash :messages)
                    flash (first flashes)]
                (should= 1 (count flashes))
                (should= true (flashc/error? flash))
                (should= "Oh Noez!" (flashc/text flash))))

          )
