(ns fin.handlers
  (:require
    [clojure.math.numeric-tower :as math]
    [fin.protocols :as p]
    [tick.core :as t]))

(defn- sum-txns-to-in-and-out [txns]
  (let [{:keys [:credits :debits]}
        (group-by #(if (pos? %) :credits :debits) (map :amount txns))]
    {:in  (reduce + credits)
     :out (math/abs (reduce + debits))}))

(defn- is-internal-txn [txn txns]
  (let [txn-to-match  {:amount           (* -1 (:amount txn))
                       :transaction_date (:transaction_date txn)}
        set-slim-txns (set (map #(select-keys % [:amount :transaction_date]) txns))]
    (set-slim-txns txn-to-match)))

(defn- sum-txns-to-in-and-out-ext [txns]
  (let [ext-txns (filter (fn [txn] (not (is-internal-txn txn txns))) txns)
        {in-ext :in out-ext :out} (sum-txns-to-in-and-out ext-txns)]
    {:in-ext  in-ext
     :out-ext out-ext}))

(defn get-transaction-summary [req]
  (let [from       (t/zoned-date-time (get-in req [:params :from]))
        to         (t/zoned-date-time (get-in req [:params :to]))
        repo       (get-in req [:repo-registry :transaction-repository])
        txns       (p/find-between-dates repo from to)

        in-out     (sum-txns-to-in-and-out txns)
        ext-in-out (sum-txns-to-in-and-out-ext txns)


        stats      (merge in-out ext-in-out {:delta (- (:in in-out) (:out in-out))})]
    {:status 200
     :body   {:stats stats
              :transactions txns}}))

(comment
  )