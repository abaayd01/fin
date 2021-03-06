(ns fin.components.transaction-repository
  (:require
    [fin.protocols :as p]
    [fin.queries :as queries]
    [fin.schemas :refer [Transaction]]

    [honey.sql :as sql]
    [malli.core :as m]))

(def associations
  {:many-to-many-associations [{:association-table-name :categories
                                :join-table-name        :transactions_categories
                                :fk-join-base           :transaction_id
                                :fk-join-association    :category_id}]})

(defn insert-transaction!
  [{:keys [db table-name]} transaction]
  (queries/insert! db table-name transaction))
(m/=> insert-transaction!
      [:=> [:cat
            [:map
             [:db any?]
             [:table-name keyword?]]
            Transaction]
       :any])

(defn update-transaction!
  [{:keys [db table-name]} updated-transaction]
  (queries/update! db table-name updated-transaction))
(m/=> update-transaction!
      [:=> [:cat
            [:map
             [:db any?]
             [:table-name keyword?]]
            Transaction]
       :any])

(defn apply-category-patterns
  [category-patterns transaction]
  (let [matched-patterns
        (filter
          #(re-find
             (re-pattern (str "(?i)" (:pattern %)))
             (:description transaction))
          category-patterns)]
    (assoc
      transaction
      :categories
      (concat
        (:categories transaction)
        (distinct (map :category matched-patterns))))))

(defn find-all-category-patterns [db]
  (map
    (fn [m]
      (-> m
          (assoc :category {:id   (:category_id m)
                            :name (:category_name m)})
          (dissoc :category_id :category_name)))
    (p/query
      db
      (sql/format
        {:select [:transaction_category_patterns/*
                  [:categories/name :category_name]
                  [:categories/id :category_id]]
         :from   :transaction_category_patterns
         :join   [:categories [:= :transaction_category_patterns.category_id :categories.id]]}))))

(defrecord TransactionRepository [db table-name]
  p/IRepository
  (find-by-id [this id]
    (let [record            (queries/deep-find-by-id
                              (:db this)
                              :transactions
                              id
                              associations)
          category-patterns (find-all-category-patterns (:db this))]
      (->> record
           (apply-category-patterns category-patterns))))

  (find-where [this where-clauses]
    (queries/find-where (:db this) table-name where-clauses))

  (insert! [this transaction]
    (insert-transaction! this transaction))

  (update! [this updated-transaction]
    (update-transaction! this updated-transaction))

  p/ITransactionRepository
  (find-between-dates [this from to]
    (let [records           (queries/deep-query
                              (:db this)
                              {:select   :*
                               :from     :transactions
                               :where    [:between :transaction_date from to]
                               :order-by [[:transaction_date :desc]]}
                              associations)
          category-patterns (find-all-category-patterns (:db this))]
      (->> records
           (map #(apply-category-patterns category-patterns %)))))

  (categories-for-transaction [this transaction_id]
    (p/query
      (:db this)
      (sql/format
        {:select :categories/*
         :from   :categories
         :join   [:transactions_categories
                  [:= :transactions_categories.category_id :categories.id]]
         :where  [:= :transactions_categories.transaction_id transaction_id]})))

  (add-category-to-transaction! [this transaction_id category_id]
    (queries/insert!
      (:db this)
      :transactions_categories
      {:transaction_id transaction_id
       :category_id    category_id}))

  (remove-category-from-transaction! [this transaction_id category_id]
    (p/query
      (:db this)
      (sql/format
        {:delete-from :transactions_categories
         :where       [:and
                       [:= :transaction_id transaction_id]
                       [:= :category_id category_id]]})))
  )

(defn make-transaction-repository [table-name]
  (map->TransactionRepository {:table-name table-name}))

(comment
  )