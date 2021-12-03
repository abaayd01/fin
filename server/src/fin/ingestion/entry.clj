(ns fin.ingestion.entry
  (:require
    [fin.ingestion.grpc-client :refer [make-grpc-client ingest-transactions]]
    [fin.ingestion.repository :refer [make-ingress-repository latest-transactions]]
    [fin.ingestion.ingestion-service :refer [make-core-ingestion-service ingest-new-transactions]]

    [com.stuartsierra.component :as component])
  (:import
    [grpc.core IngestTransactionsRequest]))

(defn create-system []
  (-> (component/system-map
        :ingress-repository (make-ingress-repository)
        :ingestion-service (make-core-ingestion-service)
        :grpc-client (make-grpc-client {:port 50055}))
      (component/system-using
        {:ingestion-service {:ingress-repository :ingress-repository
                             :grpc-client        :grpc-client}})))

(defonce the-system nil)

(defn init []
  (alter-var-root
    #'the-system
    (constantly (create-system))))

(defn start []
  (alter-var-root #'the-system component/start))

(defn stop []
  (alter-var-root #'the-system #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(comment
  (ingest-new-transactions (:ingestion-service the-system))
  (stop)
  (go)
  (latest-transactions (:ingress-repository the-system))

  (ingest-transactions
    (:grpc-client the-system)
    [{:date          "2020-01-01T00:00:00Z"
      :description   "test123"
      :amount        "12.34"
      :accountNumber "1234"
      :source        "5678"}])

  (let [stub (:stub (:grpc-client the-system))
        req  (let [builder      (IngestTransactionsRequest/newBuilder)
                   transaction1 (-> (grpc.core.Transaction/newBuilder)
                                    (.setDescription "test1")
                                    .build)

                   transaction2 (-> (grpc.core.Transaction/newBuilder)
                                    (.setDescription "test2")
                                    .build)]
               (-> (.addAllTransactions builder [transaction1 transaction2])
                   .build))]
    (.ingestTransactions stub req))

  (let [builder     (IngestTransactionsRequest/newBuilder)
        transaction (-> (grpc.core.Transaction/newBuilder)
                        (.setDescription "test")
                        .build)]
    (-> (.addAllTransactions builder [transaction])
        .build))
  )