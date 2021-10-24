(ns fin.core.repository)

(defprotocol Repository
  (create-transaction-category-pattern! [system transaction-category-pattern])
  (find-all-categories [system])
  (find-transaction-by-id [system transaction-id])
  (find-transactions-between-dates [system from to])
  (find-categories-for-transaction [system transaction-id])
  (add-category-to-transaction! [system transaction-id category-id])
  (remove-category-from-transaction! [system transaction-id category-id]))