(ns fin.ingestion.repository
  (:require
    [grpc.utils :refer [build-request response->map]]

    [clojure.java.shell :refer [sh]]
    [clojure.string :as string]
    [clojure.tools.logging :refer [log]])
  (:import [io.grpc ManagedChannelBuilder]
           [grpc.fin IngressServiceGrpc GetTransactionsRequest]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; protocol definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Repository
  (latest-transactions [system]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal protocol methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- clean-description-str [str]
  (-> str
      string/trim
      (string/replace #"\s+" " ")
      (string/replace #"\"" "")
      string/upper-case))

(defn- ->Transaction [m]
  {:date          (:date m)
   :description   (clean-description-str (:description m))
   :amount        (:amount m)
   :source        (:source m)
   :accountNumber (:accountNumber m)})

(defn- grpc-get-transactions []
  (let [channel          (-> (ManagedChannelBuilder/forAddress "localhost" 50051)
                             .usePlaintext
                             .build)
        stub             (IngressServiceGrpc/newBlockingStub channel)
        raw-response     (-> stub
                             (.getTransactions (build-request GetTransactionsRequest {})))
        raw-transactions (map response->map (:transactionsList (response->map raw-response)))]
    (.shutdown channel)
    (map ->Transaction raw-transactions)))

(defn- sh-with-println [& args]
  (println (apply sh args)))

(defn- eval-with-setup-and-teardown [{:keys [setup-fn teardown-fn]} f]
  (setup-fn)
  (let [ret-val (f)]
    (teardown-fn)
    ret-val))

(def stub-transactions
  [{:date          "2021-10-01T00:00:00Z"
    :description   "DIGITALOCEAN.COM NEW YORK CITY NY"
    :amount        "-7.67"
    :accountNumber "1234"
    :source        "5678"}
   {:date          "2021-11-01T00:00:00Z"
    :description   "new txn"
    :amount        "100"
    :accountNumber "1234"
    :source        "5678"}
   {:date          "2021-10-01T00:00:00Z"
    :description   "inverse txn"
    :amount        "7.67"
    :accountNumber "1234"
    :source        "5678"}
   {:date          "2021-11-01T00:00:00Z"
    :description   "internal inverse -ve"
    :amount        "-10"
    :accountNumber "1234"
    :source        "5678"}
   {:date          "2021-11-01T00:00:00Z"
    :description   "internal inverse +ve"
    :amount        "10"
    :accountNumber "1234"
    :source        "5678"}])

(defn- -latest-transactions
  []
  (->> (eval-with-setup-and-teardown
           {:setup-fn    #(sh-with-println "pm2" "start" "./ingress/index.js" "--wait-ready" "--name=ingress-service")
            :teardown-fn #(sh-with-println "pm2" "stop" "ingress-service")}
           grpc-get-transactions)
         (map ->Transaction)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; record definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord IngressRepository []
  Repository
  (latest-transactions [_]
    (log :info "getting latest transactions")
    (-latest-transactions)))

(defn make-ingress-repository []
  (map->IngressRepository {}))
