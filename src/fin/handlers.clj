(ns fin.handlers
  (:require
    [clojure.math.numeric-tower :as math]
    [fin.protocols :as p]
    [fin.schemas :refer [Response TransactionSummary]]
    [malli.core :as m]
    [tick.core :as t]))

(defn- sum-txns-to-in-and-out [txns]
  (let [{:keys [:credits :debits]}
        (group-by #(if (pos? %) :credits :debits) (map :amount txns))]
    {:in  (reduce + credits)
     :out (math/abs (reduce + debits))}))

(defn get-transaction-summary [req]
  (let [from (t/zoned-date-time (get-in req [:params :from]))
        to   (t/zoned-date-time (get-in req [:params :to]))
        repo (get-in req [:repo-registry :transaction-repository])
        body (sum-txns-to-in-and-out (p/find-between-dates repo from to))]
    {:status 200
     :body   body}))
(m/=> get-transaction-summary [:=> [:cat :any] (Response TransactionSummary)])

(comment
  )