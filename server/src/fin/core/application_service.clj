(ns fin.core.application-service
  (:require
    [fin.core.domain.transaction :refer [invert-transaction set-is-internal map->Transaction]]
    [fin.core.domain.transaction-service :as ts]
    [fin.core.domain.transaction-summary-service :as tss]
    [fin.core.repository :as repository]
    [fin.utils :refer [keyed]]))

(defprotocol ApplicationService
  (find-all-categories [this])
  (find-transaction-by-id [this transaction-id])
  (find-transactions-between-dates [this from to])
  (get-transaction-summary [this from to])
  (tag-transaction-with-categories [this transaction categories])
  (create-transaction [this description amount transaction-date]))

(defn- -create-transaction
  [this description amount transaction-date]
  (let [base-transaction             (map->Transaction (keyed [description amount transaction-date]))
        matching-inverse-transaction (repository/find-matching-transaction (:repository this) (invert-transaction base-transaction))
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
    [this description amount transaction-date]
    (-create-transaction this description amount transaction-date))
  )

(defn make-application-service []
  (map->CoreApplicationService {}))
