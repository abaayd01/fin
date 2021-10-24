(ns fin.infrastructure.persistence.mysql-repository
  (:require
    [fin.core.repository :as r]
    [fin.infrastructure.persistence.db :as db]
    [honey.sql :as sql]))

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
      :transactions/categories
      (distinct
        (concat
          (:transactions/categories transaction)
          (map :categories/name matched-patterns))))))

(defn- assoc-categories
  [transaction categories-by-txn-id]
  (let [categories (map :categories/name (get categories-by-txn-id (:transactions/id transaction)))]
    (assoc transaction :transactions/categories categories)))

(defn- find-transactions-between-dates
  [db from to]
  (let [txn-rows             (db/execute!
                               db
                               (sql/format
                                 {:select   :*
                                  :from     :transactions
                                  :where    [:between :transaction_date from to]
                                  :order-by [[:transaction_date :desc]]}))

        txn-categories       (when (seq txn-rows)
                               (db/execute!
                                 db
                                 (sql/format
                                   {:select [:categories/name :transactions_categories.transaction_id]
                                    :from   :transactions_categories
                                    :join   [:categories [:= :transactions_categories.category_id :categories.id]]
                                    :where  [:in :transactions_categories.transaction_id (map :transactions/id txn-rows)]})))

        categories-by-txn-id (group-by :transactions_categories/transaction_id txn-categories)

        category-patterns    (db/execute!
                               db
                               (sql/format
                                 {:select [:transaction_category_patterns/* :categories/name]
                                  :from   :transaction_category_patterns
                                  :join   [:categories [:= :transaction_category_patterns.category_id :categories.id]]}))

        transactions         (->> txn-rows
                                  (map #(assoc-categories % categories-by-txn-id))
                                  (map #(apply-category-patterns % category-patterns)))]
    transactions))

(defrecord MySqlRepository [db]
  r/Repository
  (find-all-categories
    [this]
    (db/execute!
      (:db this)
      (sql/format
        {:select :*
         :from   :categories})))

  (find-transactions-between-dates
    [this from to]
    (find-transactions-between-dates (:db this) from to)))

(defn make-repository []
  (map->MySqlRepository {}))
