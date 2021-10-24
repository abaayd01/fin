(ns fin.core.repository)

(defprotocol Repository
  (create-transaction-category-pattern! [this transaction-category-pattern])
  (find-all-categories [this])
  (find-transaction-by-id [this transaction-id])
  (find-transactions-between-dates [this from to])
  (find-categories-for-transaction [this transaction-id])
  (add-category-to-transaction! [this transaction-id category-id])
  (remove-category-from-transaction! [this transaction-id category-id]))