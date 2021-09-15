(ns fin.components.ingress-service
  (:require
    [fin.protocols :as p]
    [fin.schemas :refer [Transaction Transactions Date]]
    [clojure.java.shell :refer [sh]]
    [clojure.string :as string]
    [malli.core :as m]
    [grpc.fin.IngressService.client :as ingress-service]
    [protojure.grpc.client.providers.http2 :as grpc-http2]
    [tick.core :as t]))

(defn- sh-with-println [& args]
  (println (apply sh args)))

(defn- eval-with-setup-and-teardown [{:keys [setup-fn teardown-fn]} f]
  (setup-fn)
  (let [ret-val (f)]
    (teardown-fn)
    ret-val))

(def ^:private GRPCTransaction
  [:map
   [:description string?]
   [:amount string?]
   [:date string?]])

(defn- grpc-get-transactions
  []
  (-> @(grpc-http2/connect {:uri          "http://localhost:8080"
                            :idle-timeout 60000})
      (ingress-service/getTransactions {})
      deref
      :transactions))
(m/=> grpc-get-transactions [:=> [:cat] [:sequential GRPCTransaction]])

(defn- ->Date [str]
  (-> (t/zoned-date-time str)
      t/instant
      t/date))
(m/=> ->Date [:=> [:cat :string] Date])

(defn- clean-description-str [str]
  (-> str
      string/trim
      (string/replace #"\s+" " ")
      (string/replace #"\"" "")
      string/upper-case))

(defn- ->Transaction [m]
  (merge m
         {:description      (clean-description-str (:description m))
          :amount           (bigdec (:amount m))
          :transaction_date (->Date (:date m))
          :account_number   (:accountNumber m)}))
(m/=> ->Transaction [:=> [:cat :any] Transaction])

(defn get-transactions
  []
  (->> (eval-with-setup-and-teardown
         {:setup-fn    #(sh-with-println "pm2" "start" "./ingress/index.js" "--wait-ready" "--name=ingress-service")
          :teardown-fn #(sh-with-println "pm2" "stop" "ingress-service")}
         grpc-get-transactions)
       (map ->Transaction)))
(m/=> get-transactions [:=> [:cat] Transactions])

(defrecord IngressService []
  p/IIngressService
  (get-transactions [_]
    (get-transactions)))

(defn make-ingress-service []
  (->IngressService))

;; debug
(comment
  )
