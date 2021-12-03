(ns fin.ingestion.grpc-client
  (:require
    [grpc.utils :refer [response->map]]

    [com.stuartsierra.component :as component])
  (:import
    [io.grpc ManagedChannelBuilder]
    [grpc.core CoreServiceGrpc IngestTransactionsRequest Transaction]))

(defprotocol GrpcClient
  (ingest-transactions [system transactions]))

(defn build-transaction-dto [transaction]
  (-> (Transaction/newBuilder)
      (.setDate (:date transaction))
      (.setDescription (:description transaction))
      (.setAmount (:amount transaction))
      (.setAccountNumber (:accountNumber transaction))
      (.setSource (:source transaction))
      .build))

(defn build-ingest-transactions-request
  [transactions]
  (let [request-builder  (IngestTransactionsRequest/newBuilder)
        transaction-dtos (map build-transaction-dto transactions)]
    (-> (.addAllTransactions request-builder transaction-dtos)
        .build)))

(defrecord Client [config]
  component/Lifecycle
  (start [system]
    (let [channel (-> (ManagedChannelBuilder/forAddress "localhost" (:port config))
                      .usePlaintext
                      .build)
          stub    (CoreServiceGrpc/newBlockingStub channel)]
      (-> system
          (assoc :channel channel)
          (assoc :stub stub))))

  (stop [system]
    (.shutdown (:channel system))
    (-> system
        (assoc :stub nil)
        (assoc :channel nil)))

  GrpcClient
  (ingest-transactions [system transactions]
    (-> (:stub system)
        (.ingestTransactions (build-ingest-transactions-request transactions))
        response->map)))

(defn make-grpc-client [config]
  (map->Client {:config config}))

(comment
  (let [builder      (IngestTransactionsRequest/newBuilder)
        transaction1 (-> (Transaction/newBuilder)
                         (.setDescription "test1")
                         .build)

        transaction2 (-> (Transaction/newBuilder)
                         (.setDescription "test2")
                         .build)]
    (-> (.addAllTransactions builder [transaction1 transaction2])
        .build))
  )