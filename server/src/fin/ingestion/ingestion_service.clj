(ns fin.ingestion.ingestion-service
  (:require
    [fin.ingestion.repository :as r]
    [fin.ingestion.grpc-client :refer [ingest-transactions]]

    [clojure.tools.logging :refer [log]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; protocol definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IngestionService
  (ingest-new-transactions [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal protocol methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- -ingest-new-transactions [ingress-repository grpc-client]
  (let [transactions (r/latest-transactions ingress-repository)]
    (ingest-transactions grpc-client transactions)))

(defrecord CoreIngestionService [ingress-repository application-service]
  IngestionService
  (ingest-new-transactions [this]
    (-ingest-new-transactions (:ingress-repository this) (:grpc-client this))))

(defn make-core-ingestion-service []
  (map->CoreIngestionService {}))