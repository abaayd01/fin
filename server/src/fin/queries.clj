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
  (first (p/query
           db
           (sql/format
             {:select :*
              :from   table-name
              :where  [:= :id id]
              :limit  1}))))

(defn- concat-keywords
  ([a b] (concat-keywords a b "."))
  ([a b sep] (keyword (str (name a) sep (name b)))))

(defn hydrate-many-to-many-association
  [db
   records
   {:keys [association-table-name
           join-table-name
           fk-join-base
           fk-join-association]}]
  (let [joined-records            (p/query
                                    db
                                    (sql/format
                                      {:select [(concat-keywords association-table-name :* "/") fk-join-base]
                                       :from   association-table-name
                                       :join   [join-table-name
                                                [:=
                                                 (concat-keywords join-table-name fk-join-association)
                                                 (concat-keywords association-table-name :id)]]
                                       :where  [:in fk-join-base (map :id records)]}))
        associations-by-record-id (group-by fk-join-base joined-records)]
    (map
      (fn [record]
        (let [associated-records (get associations-by-record-id (:id record))]
          (assoc record association-table-name associated-records)))
      records)))

(defn deep-query
  [db
   data
   {:keys [many-to-many-associations]}]
  (let [records (p/query db (sql/format data))]
    (reduce
      (fn [acc many-to-many-association]
        (hydrate-many-to-many-association
          db
          acc
          many-to-many-association))
      records
      many-to-many-associations)))

(defn deep-find-by-id
  [db
   base-table-name
   id
   {:keys [many-to-many-associations]}]
  (let [record (find-by-id db base-table-name id)]
    (first
      (reduce
        (fn [acc many-to-many-association]
          (hydrate-many-to-many-association
            db
            acc
            many-to-many-association))
        [record]
        many-to-many-associations))))