(ns fin.core
  (:gen-class)
  (:require
    [fin.components.db :refer [make-db]]
    [fin.components.malli-instrumenter :refer [make-malli-instrumenter]]
    [fin.components.transaction-category-pattern-repository :refer [make-transaction-category-pattern-repository]]
    [fin.components.transaction-repository :refer [make-transaction-repository]]
    [fin.components.category-repository :refer [make-category-repository]]
    [fin.handlers :as handlers]
    [fin.middleware :as middleware]
    [fin.protocols :as p]
    [fin.schemas :as s]

    [clojure.tools.namespace.repl :refer [refresh]]
    [com.stuartsierra.component :as component]
    [muuntaja.core :as muuntaja-core]
    [next.jdbc.connection :as connection]
    [reitit.coercion.malli :as rcm]
    [reitit.ring :as ring]
    [ring.adapter.jetty :as jetty]
    [tick.core :as t])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn app
  "Returns the web handler fn as a closure over a system"
  [repo-registry]
  (ring/ring-handler
    (ring/router
      ["/api"
       {:muuntaja   (muuntaja-core/create)
        :middleware (middleware/make-pipeline repo-registry)
        :coercion   (rcm/create
                      {;; set of keys to include in error messages
                       :error-keys       #{#_:type :coercion #_:in #_:schema #_:value :errors :humanized #_:transformed}
                       ;; validate request & response
                       :validate         true
                       ;; top-level short-circuit to disable request & response coercion
                       :enabled          true
                       ;; strip-extra-keys (effects only predefined transformers)
                       :strip-extra-keys true
                       ;; add/set default values
                       :default-values   true
                       ;; malli options
                       :options          nil})}

       ["/transaction-summary"
        {:name       ::get-transaction-summary
         :handler    handlers/get-transaction-summary
         :parameters {:query [:map
                              [:from s/zoned-date-time-string?]
                              [:to s/zoned-date-time-string?]]}}]

       ["/transactions/:id"
        {:name       ::transactions
         :handler    handlers/show-transaction
         :parameters {:path [:map
                             [:id int?]]}}]

       ["/transactions/:transaction_id/categories"
        {:name       ::transaction-categories
         :put        handlers/set-categories-for-transaction
         :parameters {:path [:map
                             [:transaction_id int?]]
                      :body [:map
                             [:category_ids [:sequential int?]]]}}]

       ["/categories"
        {:name    ::categories
         :handler handlers/index-categories}]

       ["/transaction-category-patterns"
        {:name       ::transaction-category-patterns
         :post       handlers/create-transaction-category-pattern
         :parameters {:body [:map
                             [:category_id int?]
                             [:pattern string?]]}}]])
    (ring/routes
      (ring/create-default-handler))))

(defrecord Server [repo-registry options]
  component/Lifecycle
  (start [this]
    (assoc this :server (jetty/run-jetty (app repo-registry) options)))

  (stop [this]
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-server [options]
  (map->Server {:options options}))

(defrecord RepoRegistry
  [transaction-repository
   category-repository]
  component/Lifecycle
  (start [this]
    (assoc this :repo-registry {:transaction-repository transaction-repository
                                :category-repository    category-repository}))

  (stop [this]
    (assoc this :repo-registry nil)))

(defn make-repo-registry []
  (map->RepoRegistry {}))

(def ^:private db-spec {:dbtype "mysql" :dbname "fin" :username "root" :password "password"})

(defn create-system [{:keys [db-spec server-options]}]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db)
        :transaction-repository (make-transaction-repository :transactions)
        :category-repository (make-category-repository :categories)
        :transaction-category-pattern-repository (make-transaction-category-pattern-repository)
        :repo-registry (make-repo-registry)
        :malli-instrumenter (make-malli-instrumenter)
        :server (make-server server-options))
      (component/system-using
        {:db                                      {:ds :ds}
         :transaction-repository                  {:db :db}
         :category-repository                     {:db :db}
         :transaction-category-pattern-repository {:db :db}
         :repo-registry                           {:transaction-repository                  :transaction-repository
                                                   :category-repository                     :category-repository
                                                   :transaction-category-pattern-repository :transaction-category-pattern-repository}
         :server                                  {:repo-registry :repo-registry}})))

(defonce the-system nil)

(defn init []
  (alter-var-root #'the-system (constantly
                                 (create-system {:db-spec        db-spec
                                                 :server-options {:port  4000
                                                                  :join? false}
                                                 :handler        app}))))

(defn start []
  (alter-var-root #'the-system component/start))

(defn stop []
  (alter-var-root #'the-system #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'fin.core/go))

(comment
  (start)
  (stop)
  (go)
  (reset)
  )
