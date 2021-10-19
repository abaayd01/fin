(ns fin.handlers
  (:require
    [fin.view-models :as view-models]
    [fin.protocols :as p]

    [clojure.data :refer [diff]]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.math.numeric-tower :as math]
    [clojure.tools.logging :refer [log]]
    [tick.core :as t])
  (:import [java.io PushbackReader]))

(def ruleset (edn/read (PushbackReader. (io/reader (io/resource "ruleset.edn")))))

(defn is-a-manual-internal-transaction? [txn]
  (some
    (fn [pattern] (re-matches (re-pattern pattern) (:description txn)))
    (:internal-transactions ruleset)))

(defn- sum-txns-to-in-and-out [txns]
  (let [{:keys [:credits :debits]}
        (group-by #(if (pos? %) :credits :debits) (map :amount txns))]
    {:in  (reduce + credits)
     :out (math/abs (reduce + debits))}))

(defn- is-internal-txn [txn txns]
  (let [txn-to-match  {:amount           (* -1 (:amount txn))
                       :transaction_date (:transaction_date txn)}
        set-slim-txns (set (map #(select-keys % [:amount :transaction_date]) txns))]
    (or (set-slim-txns txn-to-match) (is-a-manual-internal-transaction? txn))))

(defn- sum-txns-to-in-and-out-ext [ext-txns]
  (let [{in-ext :in out-ext :out} (sum-txns-to-in-and-out ext-txns)]
    {:in-ext  in-ext
     :out-ext out-ext}))

(defn get-transaction-summary [req]
  (let [from       (t/zoned-date-time (get-in req [:params :from]))
        to         (t/zoned-date-time (get-in req [:params :to]))
        repo       (get-in req [:repo-registry :transaction-repository])
        txns       (p/find-between-dates repo from to)
        ext-txns   (filter (fn [txn] (not (is-internal-txn txn txns))) txns)

        in-out     (sum-txns-to-in-and-out txns)
        ext-in-out (sum-txns-to-in-and-out-ext ext-txns)

        stats      (merge in-out ext-in-out {:delta (- (:in-ext ext-in-out) (:out-ext ext-in-out))})]
    {:status 200
     :body   {:stats        stats
              :transactions (map view-models/->transaction ext-txns)}}))

(defn show-transaction [req]
  (let [{:keys [id]} (get-in req [:parameters :path])
        repo (get-in req [:repo-registry :transaction-repository])
        txn  (p/find-by-id repo id)]
    {:status 200
     :body   (view-models/->transaction txn)}))

(defn set-categories-for-transaction
  [req]
  (let [{:keys [transaction_id]} (get-in req [:parameters :path])
        {new_category_ids :category_ids} (get-in req [:params])
        repo                 (get-in req [:repo-registry :transaction-repository])
        current_category_ids (map :id (p/categories-for-transaction repo transaction_id))
        [ids_to_add ids_to_remove _] (map vec (diff (set new_category_ids) (set current_category_ids)))]

    (doseq [category_id ids_to_remove]
      (try (p/remove-category-from-transaction! repo transaction_id category_id)
           (catch Exception e
             (log :info (str "could not remove category id " category_id " from transaction id " transaction_id ".n" (.getMessage e))))))

    (doseq [category_id ids_to_add]
      (try
        (p/add-category-to-transaction! repo transaction_id category_id)
        (catch Exception e
          (log :info (str "could not add category id " category_id " from transaction id " transaction_id ".\n" (.getMessage e))))))

    (let [txn (p/find-by-id repo transaction_id)]
      {:status 200
       :body   (view-models/->transaction txn)})))

(defn index-categories [req]
  (let [repo       (get-in req [:repo-registry :category-repository])
        categories (p/find-all repo)]
    {:status 200
     :body   (map view-models/->category categories)}))

(comment
  )
