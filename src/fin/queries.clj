(ns fin.queries
  (:require [fin.protocols :as p]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]))

;; repo level fns
(defn insert!
  [db table-name m]
  (p/query
    db
    (-> (h/insert-into table-name)
        (h/values [m])
        sql/format)))

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
      {:select :*
       :from   table-name
       :where  [:between column-key from to]})))
