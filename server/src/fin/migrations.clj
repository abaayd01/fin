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
  (migratus/destroy config "create transactions table unique index")

  (migratus/migrate config)

  (migratus/rollback config)

  (migratus/completed-list config)

  (migratus/create config "create transactions table unique index")

  (migratus/create config "create categories table")

  (migratus/create config "seed categories table" :edn)
  )
