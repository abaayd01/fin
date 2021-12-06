(ns fin.core.domain.application-service
  (:require
    [fin.core.domain.transaction :refer [set-is-internal map->Transaction]]
    [fin.core.domain.transaction-service :as ts]
    [fin.core.domain.transaction-summary-service :as tss]
    [fin.core.domain.repository :as repository]

    [tick.core :as t]))

(defprotocol ApplicationService
  (find-all-categories [this])
  (find-transaction-by-id [this transaction-id])
  (find-transactions-between-dates [this from to])
  (get-transaction-summary [this from to])
  (tag-transaction-with-categories [this transaction categories])
  (create-transaction [this params])
  (ingest-transactions [this transactions]))

(defn- -create-transaction
  "Creates a transaction respecting all invariants"
  [this params]
  (let [base-transaction     (map->Transaction params)
        matching-transaction (repository/find-matching-transaction (:repository this) base-transaction)]
    (when (nil? matching-transaction)
      (let [matching-inverse-transaction (repository/find-matching-inverse-transaction (:repository this) base-transaction)
            transaction                  (set-is-internal base-transaction (ts/is-internal? base-transaction matching-inverse-transaction))]
        (when matching-inverse-transaction
          (repository/mark-transaction-as-internal!
            (:repository this)
            matching-inverse-transaction))

        (repository/create-transaction!
          (:repository this)
          transaction)))))

(defn- -dangerously-create-transaction
  "Creates a transaction without respecting the 'duplicate records in the database' invariant.
  It will still respect the invariant of 'matching inverse transactions'."
  [repo params]
  (let [base-transaction             (map->Transaction params)
        matching-inverse-transaction (repository/find-matching-inverse-transaction repo base-transaction)
        transaction                  (set-is-internal base-transaction (ts/is-internal? base-transaction matching-inverse-transaction))]

    (when matching-inverse-transaction
      (repository/mark-transaction-as-internal!
        repo
        matching-inverse-transaction))

    (repository/create-transaction!
      repo
      transaction)))

(defn- compare-by [g f coll]
  (when (seq coll)
    (reduce (fn [cur-lowest cur-val]
              (if (g (f cur-lowest) (f cur-val))
                cur-lowest
                cur-val))
            coll)))

(defn- min-by-transaction-date
  [transactions]
  (compare-by t/< :transaction-date transactions))

(defn- max-by-transaction-date
  [transactions]
  (compare-by t/> :transaction-date transactions))

(defn- strip-extra-keys
  [transaction]
  (select-keys transaction [:transaction-date
                            :amount
                            :description]))

(defn- contains-txn-match [txn txns]
  (let [slim-txn (strip-extra-keys txn)]
    (some (fn [cur-txn] (= slim-txn (strip-extra-keys cur-txn))) txns)))

(defn- diff-transactions
  [a b]
  (filter (fn [txn] (not (contains-txn-match txn b))) a))

(defn- txns-to-insert
  [repo ingress-txns]
  (let [min-date (:transaction-date (min-by-transaction-date ingress-txns))
        max-date (:transaction-date (max-by-transaction-date ingress-txns))
        db-txns  (repository/find-transactions-between-dates repo min-date max-date)]
    (diff-transactions ingress-txns db-txns)))

(defrecord CoreApplicationService [repository]
  ApplicationService
  (find-all-categories [this]
    (repository/find-all-categories (:repository this)))

  (find-transaction-by-id [this transaction-id]
    (repository/find-transaction-by-id (:repository this) transaction-id))

  (find-transactions-between-dates [this from to]
    (repository/find-transactions-between-dates (:repository this) from to))

  (get-transaction-summary
    [this from to]
    (->> (repository/find-transactions-between-dates (:repository this) from to)
         (tss/calculate-transaction-summary from to)))

  (tag-transaction-with-categories
    [this transaction-id categories]
    (let [transaction (repository/find-transaction-by-id
                        (:repository this)
                        transaction-id)]
      (repository/tag-transaction-with-categories!
        (:repository this)
        transaction
        categories)))

  (create-transaction
    [this params]
    (-create-transaction this params))

  (ingest-transactions
    [this transactions]
    (doseq [txn (txns-to-insert (:repository this) transactions)]
      (-dangerously-create-transaction (:repository this) txn))))

(defn make-application-service []
  (map->CoreApplicationService {}))
