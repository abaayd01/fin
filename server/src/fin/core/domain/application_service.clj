(ns fin.core.domain.application-service
  (:require
    [fin.core.domain.transaction :refer [set-is-internal map->Transaction]]
    [fin.core.domain.transaction-service :as ts]
    [fin.core.domain.transaction-summary-service :as tss]
    [fin.core.domain.repository :as repository]))

(defprotocol ApplicationService
  (find-all-categories [this])
  (find-transaction-by-id [this transaction-id])
  (find-transactions-between-dates [this from to])
  (get-transaction-summary [this from to])
  (tag-transaction-with-categories [this transaction categories])
  (create-transaction [this params])
  (dangerously-create-transaction [this params]))

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

(defn -dangerously-create-transaction
  "Creates a transaction without respecting the 'duplicate records in the database' invariant.
  It will still respect the invariant of 'matching inverse transactions'."
  [this params]
  (let [base-transaction             (map->Transaction params)
        matching-inverse-transaction (repository/find-matching-inverse-transaction (:repository this) base-transaction)
        transaction                  (set-is-internal base-transaction (ts/is-internal? base-transaction matching-inverse-transaction))]

    (when matching-inverse-transaction
      (repository/mark-transaction-as-internal!
        (:repository this)
        matching-inverse-transaction))

    (repository/create-transaction!
      (:repository this)
      transaction)))

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

  (dangerously-create-transaction
    [this params]
    (-dangerously-create-transaction this params)))

(defn make-application-service []
  (map->CoreApplicationService {}))
