(ns fin.core.domain.transaction)

(defrecord Transaction
  [id
   transaction-date
   amount
   description
   categories])