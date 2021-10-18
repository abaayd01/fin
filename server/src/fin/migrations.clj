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
  (migratus/migrate config)

  (migratus/rollback config)

  (migratus/completed-list config)

  (migratus/create config "create transactions table unique index")

  (migratus/create config "create categories table")

  (migratus/create config "seed categories table" :edn)

  (migratus/destroy config "create transactions categories table")

  (migratus/create config "create transaction category patterns table")

  (migratus/create config "seed transaction category patterns table" :edn)
  )
