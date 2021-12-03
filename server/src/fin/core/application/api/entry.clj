(ns fin.core.application.api.entry
  (:require
    [fin.core.application.api.middleware :as middleware]
    [fin.core.application.api.handlers :as handlers]

    [com.stuartsierra.component :as component]
    [malli.core :as m]
    [muuntaja.core :as muuntaja-core]
    [reitit.coercion.malli :as rcm]
    [reitit.ring :as ring]
    [ring.adapter.jetty :as jetty]))

(def zoned-date-time-string?
  (m/-simple-schema
    {:type            :zoned-date-time-string
     :pred            (fn [in]
                        (try (let [[_ month day hrs mins secs _]
                                   (->> in
                                        (re-matches #"(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}).(\d{3})Z")
                                        rest
                                        (map #(Integer/parseInt %)))]
                               (and (and (> month 0) (<= month 12))
                                    (and (> day 0) (<= day 31))
                                    (and (>= hrs 0) (<= hrs 24))
                                    (and (>= mins 0) (<= mins 60))
                                    (and (>= secs 0) (<= secs 60))))

                             (catch Exception _ false)))
     :type-properties {:error/message "invalid zoned date time string"}}))

(defn app
  "Returns the web handler fn as a closure over a system"
  [application-service]
  (ring/ring-handler
    (ring/router
      ["/api"
       {:muuntaja   (muuntaja-core/create)
        :middleware (middleware/make-pipeline application-service)
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

       ["/categories"
        {:name    ::categories
         :handler handlers/get-categories}]

       ["/transaction-summary"
        {:name       ::transaction-summary
         :handler    handlers/get-transaction-summary
         :parameters {:query [:map
                              [:from zoned-date-time-string?]
                              [:to zoned-date-time-string?]]}}]

       ["/transactions"
        ["/:transaction-id"
         {:name       ::show-transaction
          :handler    handlers/get-transaction
          :parameters {:path [:map
                              [:transaction-id int?]]}}]

        ["/:transaction-id/tag"
         {:name       ::tag-transaction
          :put        handlers/tag-transaction
          :parameters {:path [:map
                              [:transaction-id int?]]
                       :body [:map
                              [:categories
                               [:sequential string?]]]}}]]])

    (ring/routes
      (ring/create-default-handler))))

(defrecord API [application-service options]
  component/Lifecycle
  (start [this]
    (assoc this :api (jetty/run-jetty (app application-service) options)))

  (stop [this]
    (.stop (:api this))
    (assoc this :api nil)))

(defn make-api [options]
  (map->API {:options options}))
