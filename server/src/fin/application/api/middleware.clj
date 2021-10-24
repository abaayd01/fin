(ns fin.application.api.middleware
  (:require
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [reitit.ring.coercion :as reitit-coercion]
    [reitit.ring.middleware.exception :as reitit-exception]
    [reitit.ring.middleware.muuntaja
     :refer [format-negotiate-middleware
             format-request-middleware
             format-response-middleware]]
    [reitit.ring.middleware.parameters
     :refer [parameters-middleware]]
    [ring.middleware.keyword-params
     :refer [wrap-keyword-params]
     :rename {wrap-keyword-params keywordize-param-keys-middleware}]))

(defn cors-middleware
  "Add Access-Control-Allow-Origin header"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (update-in response [:headers] merge {"Access-Control-Allow-Origin" "*"}))))

(defn combine-params-middleware
  "Smush :params and :body-params into :params for convenience"
  [handler]
  (fn [{:keys [params body-params] :as request}]
    (handler (assoc request :params (merge params body-params)))))

(defn make-inject-application-service-middleware
  "Inject a repo-registry component into the request map under the keyword :repo-registry.

  The repo-registry component being a stuartsierra/component 'component' which is a
  registry of other repository components."
  [application-service]
  (fn [handler]
    (fn [request]
      (handler (assoc request :application-service application-service)))))

(defn format-response-body-keys-as-snake-case-middleware
  "Convert kebab case keys to snake case keys in the response body."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc response :body (cske/transform-keys csk/->snake_case_keyword (:body response))))))

(defn make-pipeline
  [application-service]
  [(make-inject-application-service-middleware application-service)
   parameters-middleware
   keywordize-param-keys-middleware
   format-negotiate-middleware
   format-request-middleware
   combine-params-middleware
   format-response-middleware
   cors-middleware
   reitit-exception/exception-middleware
   reitit-coercion/coerce-exceptions-middleware
   reitit-coercion/coerce-request-middleware
   reitit-coercion/coerce-response-middleware
   format-response-body-keys-as-snake-case-middleware])
