(ns fin.core.application.grpc.server
  (:require
    [fin.core.domain.application-service :as aps]
    [grpc.utils :refer [build-response build-grpc-service]]

    [com.stuartsierra.component :as component]
    [tick.core :as t])
  (:import
    [io.grpc ServerBuilder]
    [grpc.core CoreServiceGrpc$CoreServiceImplBase IngestTransactionsResponse]))

(defn- ->Date [str]
  (-> (t/zoned-date-time str)
      t/instant
      t/date))

(defn- ->Transaction [req]
  {:description      (:description req)
   :amount           (bigdec (:amount req))
   :transaction-date (->Date (:date req))
   :account-number   (:accountNumber req)
   :source           (:source req)})

(defn ingest-transactions-handler [application-service]
  (fn [req]
    (let [transactions (map (comp ->Transaction bean) (:transactionsList req))]
      (aps/ingest-transactions application-service transactions))
    (build-response
      IngestTransactionsResponse
      {})))

(defn core-service [application-service]
  (build-grpc-service
    CoreServiceGrpc$CoreServiceImplBase
    {:ingestTransactions (ingest-transactions-handler application-service)}))

(defrecord Server [application-service config]
  component/Lifecycle
  (start [system]
    (assoc system :server (-> (ServerBuilder/forPort (:port config))
                              (.addService (core-service (:application-service system)))
                              .build
                              .start)))
  (stop [system]
    (.shutdown (:server system))
    (assoc system :server nil)))

(defn make-server [config]
  (map->Server {:config config}))
