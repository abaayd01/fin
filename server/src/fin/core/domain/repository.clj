(ns fin.core.domain.repository)

(defprotocol Repository
  (create-transaction-category-pattern! [this transaction-category-pattern])
  (find-all-categories [this])
  (find-all-transactions [this])
  (find-transaction-by-id [this transaction-id])
  (find-matching-inverse-transaction [this transaction])
  (find-matching-transaction [this transaction])
  (find-transactions-between-dates [this from to])
  (find-categories-for-transaction [this transaction-id])
  (tag-transaction-with-categories! [this transaction categories])
  (create-transaction! [this transaction])
  (mark-transaction-as-internal! [this transaction]))