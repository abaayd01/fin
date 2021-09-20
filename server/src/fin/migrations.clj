(ns fin.migrations
  (:require [migratus.core :as migratus]))

(def config {:store                :database
             :migration-dir        "migrations/"
             :migration-table-name "migrations"
             :db                   {:dbtype   "mysql"
                                    :dbname   "fin"
                                    :user     "root"
                                    :password "password"}})

(comment
  (migratus/create config "create transactions table unique index")
  (migratus/destroy config "create transactions table unique index")
  (migratus/migrate config)
  (migratus/up config)
  (migratus/rollback config)
  (migratus/completed-list config)
  )
