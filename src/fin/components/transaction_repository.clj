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

(defrecord TransactionRepository [db table-name]
  p/IRepository
  (find-by-id [this id]
    (queries/find-by-id (:db this) table-name id))

  p/ITransactionRepository
  (find-between-dates [this from to]
    (queries/find-between-dates
      (:db this) table-name :transaction_date from to))

  (insert-transaction! [this transaction]
    (insert-transaction! this transaction)))

(defn make-transaction-repository [table-name]
  (map->TransactionRepository {:table-name table-name}))