(ns fin.protocols)

(defprotocol IDB
  (query [this sql]))

(defprotocol IRepository
  (find-by-id [this id]))

(defprotocol ITransactionRepository
  (find-between-dates [this from to])
  (insert-transaction! [this transaction]))

(defprotocol IIngressService
  (get-transactions [this]))

(defprotocol IIngressOrchestrator
  (run [this]))