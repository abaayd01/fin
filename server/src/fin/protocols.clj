(ns fin.protocols)

(defprotocol IDB
  (query [this sql]))

(defprotocol IRepository
  (find-all [this])
  (find-by-id [this id])
  (find-where [this where-clauses])
  (insert! [this record])
  (update! [this record]))

(defprotocol ITransactionRepository
  (find-between-dates [this from to])
  (add-category-to-transaction! [this transaction_id category_id])
  (remove-category-from-transaction! [this transaction_id category_id])
  (categories-for-transaction [this transaction_id]))

(defprotocol IIngressService
  (get-transactions [this]))

(defprotocol IIngressOrchestrator
  (run [this]))
