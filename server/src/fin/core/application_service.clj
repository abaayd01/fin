(ns fin.core.application-service
  (:require
    [fin.core.domain.transaction-summary-service :as tss]
    [fin.core.repository :as repository]))

(defprotocol ApplicationService
  (find-all-categories [this])
  (find-transactions-between-dates [this from to])
  (get-transaction-summary [this from to]))

(defrecord CoreApplicationService [repository]
  ApplicationService
  (find-all-categories [_]
    (repository/find-all-categories repository))

  (find-transactions-between-dates [_ from to]
    (repository/find-transactions-between-dates repository from to))

  (get-transaction-summary
    [_ from to]
    (->> (repository/find-transactions-between-dates repository from to)
         (tss/calculate-transaction-summary from to))))

(defn make-application-service []
  (map->CoreApplicationService {}))
