(ns fin.components.transaction-repository
  (:require
    [fin.protocols :as p]
    [fin.queries :as queries]
    [fin.schemas :refer [Transaction]]

    [malli.core :as m]))

(defn insert-transaction!
  [{:keys [db table-name]} transaction]
  (queries/insert! db table-name transaction))
(m/=> insert-transaction!
      [:=> [:cat
            [:map
             [:db any?]
             [:table-name keyword?]]
            Transaction]
       :any])

(defn update-transaction!
  [{:keys [db table-name]} updated-transaction]
  (queries/update! db table-name updated-transaction))
(m/=> update-transaction!
      [:=> [:cat
            [:map
             [:db any?]
             [:table-name keyword?]]
            Transaction]
       :any])

(defrecord TransactionRepository [db table-name]
  p/IRepository
  (find-by-id [this id]
    (queries/find-by-id (:db this) table-name id))

  p/ITransactionRepository
  (find-between-dates [this from to]
    (queries/find-between-dates
      (:db this) table-name :transaction_date from to))

  (find-where [this where-clauses]
    (queries/find-where (:db this) table-name where-clauses))

  (insert-transaction! [this transaction]
    (insert-transaction! this transaction))

  (update-transaction! [this updated-transaction]
    (update-transaction! this updated-transaction))
  )

(defn make-transaction-repository [table-name]
  (map->TransactionRepository {:table-name table-name}))
