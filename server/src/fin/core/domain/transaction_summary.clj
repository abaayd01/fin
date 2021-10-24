(ns fin.core.domain.transaction-summary)

(defrecord TransactionSummary
  [in out delta from to])
