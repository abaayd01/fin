(ns fin.infrastructure.persistence.mysql-repository
  (:require
    [fin.core.repository :as r]
    [fin.core.domain.transaction :refer [map->Transaction]]
    [fin.infrastructure.persistence.db :as db]
    [fin.utils :refer [map-keys]]

    [clojure.data :refer [diff]]
    [honey.sql :as sql]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transforming methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn row->transaction [row categories]
  (when (:transactions/id row)
    (map->Transaction
      {:id               (:transactions/id row)
       :description      (:transactions/description row)
       :amount           (:transactions/amount row)
       :transaction-date (:transactions/transaction_date row)
       :categories       categories
       :is-internal      (:transactions/is_internal row)})))

(defn transaction->row [transaction]
  {:id               (:id transaction)
   :description      (:description transaction)
   :amount           (:amount transaction)
   :transaction_date (:transaction-date transaction)
   :is_internal      (:is-internal transaction)})

(defn row->category [row]
  (cond
    (:categories/name row) (:categories/name row)
    (:name row) (:name row)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal helper methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- find-matched-categories-for-txn-row
  [txn-row category-patterns]
  (let [matched-patterns
        (filter
          #(re-find
             (re-pattern (str "(?i)" (:transaction_category_patterns/pattern %)))
             (:transactions/description txn-row))
          category-patterns)]
    (map row->category matched-patterns)))

(defn- categories-for-txn-row
  [txn-row categories-by-txn-id category-patterns]
  (let [explicit-categories (get categories-by-txn-id (:transactions/id txn-row))
        matched-categories  (find-matched-categories-for-txn-row txn-row category-patterns)]
    (distinct (concat explicit-categories matched-categories))))

(defn- build-transactions
  [db txn-rows]
  (let [category-rows        (when (seq txn-rows)
                               (db/execute!
                                 db
                                 (sql/format
                                   {:select [:categories/name :transactions_categories.transaction_id]
                                    :from   :transactions_categories
                                    :join   [:categories [:= :transactions_categories.category_id :categories.id]]
                                    :where  [:in :transactions_categories.transaction_id (map :transactions/id txn-rows)]})))

        categories-by-txn-id (-> (group-by :transactions_categories/transaction_id category-rows)
                                 (map-keys (partial map row->category)))

        category-patterns    (db/execute!
                               db
                               (sql/format
                                 {:select [:transaction_category_patterns/* :categories/name]
                                  :from   :transaction_category_patterns
                                  :join   [:categories [:= :transaction_category_patterns.category_id :categories.id]]}))]

    (map
      (fn [row]
        (let [categories (categories-for-txn-row row categories-by-txn-id category-patterns)]
          (row->transaction row categories)))
      txn-rows)))

(defn- find-category-rows [db categories]
  (when (seq categories)
    (db/execute!
      db
      (sql/format
        {:select :*
         :from   :categories
         :where  [:in :categories/name categories]}))))

(defn- remove-categories-from-transaction!
  [db transaction categories]
  (let [category-ids   (map :categories/id (find-category-rows db categories))
        transaction-id (:id transaction)]
    (when (seq category-ids)
      (db/execute!
        db
        (sql/format
          {:delete-from :transactions_categories
           :where       [:and
                         [:in :category_id category-ids]
                         [:= :transaction_id transaction-id]]})))))

(defn- add-categories-for-transaction!
  [db transaction categories]
  (let [category-ids   (map :categories/id (find-category-rows db categories))
        transaction-id (:id transaction)]
    (when (seq category-ids)
      (db/execute!
        db
        (sql/format
          {:insert-into :transactions_categories
           :values      (map
                          (fn [x] {:category_id    x
                                   :transaction_id transaction-id})
                          category-ids)})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; protocol methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- find-all-categories
  [db]
  (->> (db/execute!
         db
         (sql/format
           {:select :*
            :from   :categories}))
       (map row->category)))

(defn- find-all-transactions
  [db]
  (let [txn-rows (db/execute!
                   db
                   (sql/format
                     {:select :*
                      :from   :transactions}))]
    (build-transactions db txn-rows)))

(defn- find-transaction-by-id
  [db transaction-id]
  (let [txn-rows (db/execute!
                   db
                   (sql/format
                     {:select :*
                      :from   :transactions
                      :where  [:= :transactions.id transaction-id]}))]
    (first (build-transactions db txn-rows))))

(defn- find-matching-transaction
  [db transaction]
  (let [txn-rows (db/execute!
                   db
                   (sql/format
                     {:select :*
                      :from   :transactions
                      :where  [:and
                               [:= :transactions.amount (:amount transaction)]
                               [:= :transactions.transaction_date (:transaction-date transaction)]]}))]
    (first (build-transactions db txn-rows))))

(defn- find-transactions-between-dates
  [db from to]
  (let [txn-rows (db/execute!
                   db
                   (sql/format
                     {:select   :*
                      :from     :transactions
                      :where    [:between :transaction_date from to]
                      :order-by [[:transaction_date :desc]]}))]
    (build-transactions db txn-rows)))

(defn- tag-transaction-with-categories!
  [db transaction categories]
  (let [{existing-categories :categories} transaction
        [to-remove to-add] (into [] (map (partial filter seq)) (diff existing-categories categories))]
    (remove-categories-from-transaction! db transaction to-remove)
    (add-categories-for-transaction! db transaction to-add)
    (find-transaction-by-id db (:id transaction))))

(defn- create-transaction!
  [db transaction]
  (db/execute!
    db
    (sql/format
      {:insert-into :transactions
       :values      [(transaction->row transaction)]}))
  transaction)

(defn- mark-transaction-as-internal!
  [db transaction]
  (db/execute!
    db
    (sql/format
      {:update :transactions
       :set    {:is-internal true}
       :where  [:= :transactions/id (:id transaction)]})))

(defrecord MySqlRepository [db]
  r/Repository
  (find-all-categories [this]
    (find-all-categories (:db this)))

  (find-all-transactions [this]
    (find-all-transactions (:db this)))

  (find-transaction-by-id [this transaction-id]
    (find-transaction-by-id (:db this) transaction-id))

  (find-matching-transaction [this transaction]
    (find-matching-transaction (:db this) transaction))

  (find-transactions-between-dates [this from to]
    (find-transactions-between-dates (:db this) from to))

  (tag-transaction-with-categories! [this transaction categories]
    (tag-transaction-with-categories! (:db this) transaction categories))

  (create-transaction! [this transaction]
    (create-transaction! (:db this) transaction))

  (mark-transaction-as-internal! [this transaction]
    (mark-transaction-as-internal! (:db this) transaction)))

(defn make-repository []
  (map->MySqlRepository {}))
