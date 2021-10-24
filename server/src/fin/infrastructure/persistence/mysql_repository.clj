(ns fin.infrastructure.persistence.mysql-repository
  (:require
    [fin.core.repository :as r]
    [fin.infrastructure.persistence.db :as db]
    [honey.sql :as sql]))

(defrecord MySqlRepository [db]
  r/Repository
  (find-all-categories
    [_]
    (db/execute!
      db
      (sql/format
        {:select :*
         :from   :categories})))

  (find-transactions-between-dates
    [_ from to]
    (db/execute!
      db
      (sql/format
        {:select   :*
         :from     :transactions
         :where    [:between :transaction_date from to]
         :order-by [[:transaction_date :desc]]}))))

(defn make-repository []
  (map->MySqlRepository {}))
