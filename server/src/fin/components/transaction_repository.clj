(ns fin.components.transaction-repository
  (:require
    [fin.protocols :as p]
    [fin.queries :as queries]
    [fin.schemas :refer [Transaction]]

    [malli.core :as m]))

(def associations
  {:many-to-many-associations [{:association-table-name :categories
                                :join-table-name        :transactions_categories
                                :fk-join-base           :transaction_id
                                :fk-join-association    :category_id}]})

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
    (queries/deep-find-by-id
      (:db this)
      :transactions
      id
      associations))

  p/ITransactionRepository
  (find-between-dates [this from to]
    (queries/deep-query
      (:db this)
      {:select   :*
       :from     :transactions
       :where    [:between :transaction_date from to]
       :order-by [[:transaction_date :desc]]}
      associations))

  (find-where [this where-clauses]
    (queries/find-where (:db this) table-name where-clauses))

  (insert-transaction! [this transaction]
    (insert-transaction! this transaction))

  (update-transaction! [this updated-transaction]
    (update-transaction! this updated-transaction))
  )

(defn make-transaction-repository [table-name]
  (map->TransactionRepository {:table-name table-name}))
