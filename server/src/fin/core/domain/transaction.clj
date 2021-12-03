(ns fin.core.domain.transaction)

(defrecord Transaction
  [id
   transaction-date
   amount
   description
   categories
   account-number
   is-internal])

(defn set-is-internal [transaction is-internal]
  (assoc transaction :is-internal is-internal))