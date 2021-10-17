(ns fin.components.category-repository
  (:require
    [fin.protocols :as p]
    [honey.sql :as sql]))

(defrecord CategoryRepository [db table-name]
  p/IRepository
  (find-all [this]
    (p/query
      (:db this)
      (sql/format
        {:select :*
         :from   table-name}))))

(defn make-category-repository [table-name]
  (map->CategoryRepository {:table-name table-name}))
