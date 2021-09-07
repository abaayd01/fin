(ns fin.components.ingress-orchestrator
  (:require
    [clojure.tools.logging :refer [log]]
    [clojure.set]
    [fin.protocols :as p]
    [fin.schemas :refer [Transaction Transactions]]
    [malli.core :as m]
    [malli.transform :as mt]
    [tick.core :as t]))

(defn- compare-by [g f coll]
  (when (seq coll)
    (reduce (fn [cur-lowest cur-val]
              (if (g (f cur-lowest) (f cur-val))
                cur-lowest
                cur-val))
            coll)))

(def ^:private Transactions->Transaction
  [:=> [:cat Transactions] [:maybe Transaction]])

(defn- min-by-transaction-date
  [transactions]
  (compare-by t/< :transaction_date transactions))
(m/=> min-by-transaction-date Transactions->Transaction)

(defn- max-by-transaction-date
  [transactions]
  (compare-by t/> :transaction_date transactions))
(m/=> max-by-transaction-date Transactions->Transaction)

(defn- strip-extra-keys
  [transaction]
  (m/decode Transaction transaction mt/strip-extra-keys-transformer))
(m/=> strip-extra-keys [:=> [:cat Transaction] Transaction])

(defn- diff-transactions
  [a b]
  (let [a-         (map strip-extra-keys a)
        b-set      (set (map strip-extra-keys b))
        a-not-in-b (filter (fn [el] (not (b-set el))) a-)]
    a-not-in-b))
(m/=> diff-transactions [:=> [:cat Transactions Transactions] Transactions])

(defrecord IngressOrchestrator [transaction-repository ingress-service]
  p/IIngressOrchestrator
  (run [_]
    (let [ingress-txns   (p/get-transactions ingress-service)
          db-txns        (p/find-between-dates
                           transaction-repository
                           (:transaction_date
                             (min-by-transaction-date ingress-txns))
                           (:transaction_date
                             (max-by-transaction-date ingress-txns)))
          txns-to-insert (diff-transactions ingress-txns db-txns)]
      (doseq [txn txns-to-insert]
        (try
          (p/insert-transaction! transaction-repository txn)
          (catch Exception e
            (log :info (str "caught exception: " (.getMessage e))))))

      {:ingress-txns   ingress-txns
       :db-txns        db-txns
       :txns-to-insert txns-to-insert})))

(defn make-ingress-orchestrator []
  (map->IngressOrchestrator {}))
