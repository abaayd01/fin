(ns fin.core.domain.transaction-summary-service
  (:require [fin.core.domain.transaction-summary :refer [map->TransactionSummary]]
            [clojure.math.numeric-tower :as math]))

(defn calculate-transaction-summary
  [from to transactions]
  (let [in  (->> (map :amount transactions)
                 (filter pos?)
                 (reduce +))
        out (math/abs
              (->> (map :amount transactions)
                   (filter neg?)
                   (reduce +)))]
    (map->TransactionSummary
      {:in    in
       :out   out
       :delta (- in out)
       :from  from
       :to    to})))
