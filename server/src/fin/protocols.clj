(ns fin.protocols)

(defprotocol IDB
  (query [this sql]))

(defprotocol IRepository
  (find-by-id [this id]))

(defprotocol ITransactionRepository
  (find-between-dates [this from to])
  (find-where [this where-clauses])
  (insert-transaction! [this transaction])
  (update-transaction! [this updated-transaction])
  (add-category-to-transaction! [this transaction_id category_id])
  (remove-category-from-transaction! [this transaction_id category_id])
  (get-categories-for-transaction [this transaction_id]))

(defprotocol IIngressService
  (get-transactions [this]))

(defprotocol IIngressOrchestrator
  (run [this]))