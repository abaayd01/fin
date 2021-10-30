(ns fin.core.domain.transaction)

(defrecord Transaction
  [id
   transaction-date
   amount
   description
   categories
   is-internal])

(defn invert-transaction [transaction]
  (assoc transaction :amount (* -1 (:amount transaction))))

(defn set-is-internal [transaction is-internal]
  (assoc transaction :is-internal is-internal))