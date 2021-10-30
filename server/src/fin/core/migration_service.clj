(ns fin.core.migration-service
  (:require
    [fin.core.domain.transaction :refer [invert-transaction]]
    [fin.core.repository :as repository]
    [fin.core.domain.transaction-service :as ts]))

(defprotocol MigrationService
  (find-all-transactions [this])
  (update-transaction-if-is-internal! [this transaction-id]))

(defrecord MySqlMigrationService [repository]
  MigrationService
  (find-all-transactions [this]
    (repository/find-all-transactions (:repository this)))

  (update-transaction-if-is-internal! [this transaction-id]
    (let [original-transaction (repository/find-transaction-by-id (:repository this) transaction-id)
          matched-transaction  (repository/find-matching-transaction (:repository this) (invert-transaction original-transaction))
          is-internal          (ts/is-internal? original-transaction matched-transaction)]
      (when matched-transaction
        (repository/mark-transaction-as-internal! (:repository this) matched-transaction))

      (when is-internal
        (repository/mark-transaction-as-internal! (:repository this) original-transaction)))))

(defn make-mysql-migration-service []
  (map->MySqlMigrationService {}))
