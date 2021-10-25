(ns fin.infrastructure.persistence.mysql-repository
  (:require
    [fin.core.repository :as r]
    [fin.core.domain.transaction :refer [map->Transaction]]
    [fin.infrastructure.persistence.db :as db]

    [clojure.data :refer [diff]]
    [honey.sql :as sql]))

(defn map-keys [m f]
  (into {} (map (fn [[k v]] {k (f v)})) m))

(defn make-transaction [m]
  (map->Transaction {:id               (:transactions/id m)
                     :description      (:transactions/description m)
                     :amount           (:transactions/amount m)
                     :transaction-date (:transactions/transaction_date m)
                     :categories       (:categories m)}))

(defn make-category [m]
  (cond
    (:categories/name m) (:categories/name m)
    (:name m) (:name m)))

(defn- find-all-categories
  [db]
  (->> (db/execute!
         db
         (sql/format
           {:select :*
            :from   :categories}))
       (map make-category)))

(defn- apply-category-patterns
  [transaction category-patterns]
  (let [matched-patterns
        (filter
          #(re-find
             (re-pattern (str "(?i)" (:transaction_category_patterns/pattern %)))
             (:transactions/description transaction))
          category-patterns)]
    (assoc
      transaction
      :categories
      (distinct
        (concat
          (:categories transaction)
          (map :categories/name matched-patterns))))))

(defn- assoc-categories
  [transaction categories-by-txn-id]
  (let [categories (get categories-by-txn-id (:transactions/id transaction))]
    (assoc transaction :categories categories)))

(defn- find-transaction-by-id
  [db transaction-id]
  (let [txn-row     (first (db/execute!
                             db
                             (sql/format
                               {:select :*
                                :from   :transactions
                                :where  [:= :transactions.id transaction-id]})))
        categories  (map make-category
                         (db/execute!
                           db
                           (sql/format
                             {:select [:categories/name :transactions_categories.transaction_id]
                              :from   :transactions_categories
                              :join   [:categories [:= :transactions_categories.category_id :categories.id]]
                              :where  [:= :transactions_categories.transaction_id transaction-id]})))
        transaction (assoc txn-row :categories categories)]
    (make-transaction transaction)))

(defn- find-transactions-between-dates
  [db from to]
  (let [txn-rows             (db/execute!
                               db
                               (sql/format
                                 {:select   :*
                                  :from     :transactions
                                  :where    [:between :transaction_date from to]
                                  :order-by [[:transaction_date :desc]]}))

        category-rows        (when (seq txn-rows)
                               (db/execute!
                                 db
                                 (sql/format
                                   {:select [:categories/name :transactions_categories.transaction_id]
                                    :from   :transactions_categories
                                    :join   [:categories [:= :transactions_categories.category_id :categories.id]]
                                    :where  [:in :transactions_categories.transaction_id (map :transactions/id txn-rows)]})))

        categories-by-txn-id (-> (group-by :transactions_categories/transaction_id category-rows)
                                 (map-keys (partial map make-category)))

        category-patterns    (db/execute!
                               db
                               (sql/format
                                 {:select [:transaction_category_patterns/* :categories/name]
                                  :from   :transaction_category_patterns
                                  :join   [:categories [:= :transaction_category_patterns.category_id :categories.id]]}))

        transactions         (->> txn-rows
                                  (map #(assoc-categories % categories-by-txn-id))
                                  (map #(apply-category-patterns % category-patterns)))]
    (map make-transaction transactions)))

(defn- get-category-rows [db categories]
  (when (seq categories)
    (db/execute!
      db
      (sql/format
        {:select :*
         :from   :categories
         :where  [:in :categories/name categories]}))))

(defn- remove-categories-from-transaction!
  [db transaction categories]
  (let [category-ids   (map :categories/id (get-category-rows db categories))
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
  (let [category-ids   (map :categories/id (get-category-rows db categories))
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

(defn- tag-transaction-with-categories!
  [db transaction categories]
  (let [{existing-categories :categories} transaction
        [to-remove to-add] (into [] (map (partial filter seq)) (diff existing-categories categories))]
    (remove-categories-from-transaction! db transaction to-remove)
    (add-categories-for-transaction! db transaction to-add)
    (find-transaction-by-id db (:id transaction))))

(defrecord MySqlRepository [db]
  r/Repository
  (find-all-categories [this]
    (find-all-categories (:db this)))

  (find-transaction-by-id [this transaction-id]
    (find-transaction-by-id (:db this) transaction-id))

  (find-transactions-between-dates [this from to]
    (find-transactions-between-dates (:db this) from to))

  (tag-transaction-with-categories! [this transaction categories]
    (tag-transaction-with-categories! (:db this) transaction categories)))

(defn make-repository []
  (map->MySqlRepository {}))
