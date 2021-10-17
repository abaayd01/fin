(ns fin.handlers
  (:require
    [fin.view-models :as view-models]
    [fin.protocols :as p]

    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.math.numeric-tower :as math]
    [tick.core :as t])
  (:import [java.io PushbackReader]))

(def ruleset (edn/read (PushbackReader. (io/reader (io/resource "ruleset.edn")))))

(defn is-a-manual-internal-transaction? [txn]
  (some
    (fn [pattern] (re-matches (re-pattern pattern) (:description txn)))
    (:internal-transactions ruleset)))

(defn- sum-txns-to-in-and-out [txns]
  (let [{:keys [:credits :debits]}
        (group-by #(if (pos? %) :credits :debits) (map :amount txns))]
    {:in  (reduce + credits)
     :out (math/abs (reduce + debits))}))

(defn- is-internal-txn [txn txns]
  (let [txn-to-match  {:amount           (* -1 (:amount txn))
                       :transaction_date (:transaction_date txn)}
        set-slim-txns (set (map #(select-keys % [:amount :transaction_date]) txns))]
    (or (set-slim-txns txn-to-match) (is-a-manual-internal-transaction? txn))))

(defn- sum-txns-to-in-and-out-ext [ext-txns]
  (let [{in-ext :in out-ext :out} (sum-txns-to-in-and-out ext-txns)]
    {:in-ext  in-ext
     :out-ext out-ext}))

(defn get-transaction-summary [req]
  (let [from       (t/zoned-date-time (get-in req [:params :from]))
        to         (t/zoned-date-time (get-in req [:params :to]))
        repo       (get-in req [:repo-registry :transaction-repository])
        txns       (p/find-between-dates repo from to)
        ext-txns   (filter (fn [txn] (not (is-internal-txn txn txns))) txns)

        in-out     (sum-txns-to-in-and-out txns)
        ext-in-out (sum-txns-to-in-and-out-ext ext-txns)

        stats      (merge in-out ext-in-out {:delta (- (:in-ext ext-in-out) (:out-ext ext-in-out))})]
    {:status 200
     :body   {:stats        stats
              :transactions (map view-models/->transaction ext-txns)}}))

(defn show-transaction [req]
  (let [{:keys [id]} (get-in req [:parameters :path])
        repo (get-in req [:repo-registry :transaction-repository])
        txn  (p/find-by-id repo id)]
    {:status 200
     :body   (view-models/->transaction txn)}))

(defn add-category-to-transaction
  [req]
  (let [{:keys [transaction_id category_id]} (get-in req [:parameters :path])
        repo (get-in req [:repo-registry :transaction-repository])]

    (p/add-category-to-transaction! repo transaction_id category_id)

    (let [txn (p/find-by-id repo transaction_id)]
      {:status 200
       :body   (view-models/->transaction txn)})))

(defn remove-category-from-transaction
  [req]
  (let [{:keys [transaction_id category_id]} (get-in req [:parameters :path])
        repo (get-in req [:repo-registry :transaction-repository])]

    (p/remove-category-from-transaction! repo transaction_id category_id)

    (let [txn (p/find-by-id repo transaction_id)]
      {:status 200
       :body   (view-models/->transaction txn)})))

(comment
  )
