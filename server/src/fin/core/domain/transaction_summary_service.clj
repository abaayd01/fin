(ns fin.core.domain.transaction-summary-service
  (:require [fin.core.domain.transaction-summary :as ts]
            [clojure.math.numeric-tower :as math]))

(defn calculate-transaction-summary
  [from to transactions]
  (let [in  (->> (map :transactions/amount transactions)
                 (filter pos?)
                 (reduce +))
        out (math/abs
              (->> (map :transactions/amount transactions)
                   (filter neg?)
                   (reduce +)))]
    (ts/map->TransactionSummary
      {:in    in
       :out   out
       :delta (- in out)
       :from  from
       :to    to})))
