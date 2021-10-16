(ns fin.queries
  (:require [fin.protocols :as p]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [tick.core :as t]))

;; repo level fns
(defn insert!
  [db table-name & vals]
  (p/query
    db
    (-> (h/insert-into table-name)
        (h/values vals)
        sql/format)))

(defn update!
  [db table-name m]
  (p/query
    db
    (-> {:update table-name
         :set    (-> m
                     (dissoc :id)
                     (assoc :updated_at (t/date-time)))
         :where  [:= :id (:id m)]}
        sql/format)))

(defn find-where
  [db table-name where-clauses]
  (p/query
    db
    (sql/format
      {:select :*
       :from   table-name
       :where  where-clauses})))

(defn find-by-id
  [db table-name id]
  (p/query
    db
    (sql/format
      {:select :*
       :from   table-name
       :where  [:= :id id]})))

(defn find-between-dates
  [db table-name column-key from to]
  (p/query
    db
    (sql/format
      {:select   :*
       :from     table-name
       :where    [:between column-key from to]
       :order-by [[column-key :desc]]})))
