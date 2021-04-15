(ns c3kit.scaffold.prime-factors-spec
  (:require-macros [speclj.core :refer [describe before context it should should= should-contain should-not-contain
                                        with-stubs stub should-have-invoked should-not-have-invoked xit should-not=]])
  (:require
    [c3kit.scaffold.prime-factors :refer [prime-factors-of]]
    [speclj.core]))

(defn mersenne [n]
  (int (dec (Math/pow 2 n))))

(defn check [n factors]
  (it (str "of " n) (should= factors (prime-factors-of n))))

(describe "Prime factors"

  (check 1 [])
  (check 2 [2])
  (check 3 [3])
  (check 4 [2 2])
  (check 5 [5])
  (check 6 [2 3])
  (check 7 [7])
  (check 8 [2 2 2])
  (check 9 [3 3])
  (check 10 [2 5])
  (check 12 [2 2 3])
  (check 18 [2 3 3])
  (check 25 [5 5])
  (check 1369 [37 37])
  (check (* 2 3 5 7 11 17 37) [2 3 5 7 11 17 37])
  (check (mersenne 17) [(mersenne 17)])
  (check (mersenne 19) [(mersenne 19)])
  (check (mersenne 31) [(mersenne 31)])

  )

