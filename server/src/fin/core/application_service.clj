(ns fin.core.application-service
  (:require
    [fin.core.domain.transaction-summary-service :as tss]
    [fin.core.repository :as repository]))

(defprotocol ApplicationService
  (find-all-categories [this])
  (find-transaction-by-id [this transaction-id])
  (find-transactions-between-dates [this from to])
  (get-transaction-summary [this from to])
  (tag-transaction-with-categories [this transaction categories])
  (create-transaction [this description amount transaction-date]))

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
        categories))))

(defn make-application-service []
  (map->CoreApplicationService {}))
