(ns fin.components.transaction-category-pattern-repository
  (:require [fin.protocols :as p]
            [honey.sql :as sql]))

(defrecord ITransactionCategoryPatternRepository [db]
  p/IRepository
  (p/insert! [this transaction-category-pattern]
    (p/query
      (:db this)
      (sql/format
        {:insert-into [:transaction_category_patterns]
         :values      [transaction-category-pattern]}))))

(defn make-transaction-category-pattern-repository []
  (map->ITransactionCategoryPatternRepository {}))
