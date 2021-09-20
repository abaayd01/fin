(ns fin.middleware
  (:require
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

(defn make-inject-repo-registry-middleware
  "Inject a repo-registry component into the request map under the keyword :repo-registry.

  The repo-registry component being a stuartsierra/component 'component' which is a
  registry of other repository components."
  [repo-registry]
  (fn [handler]
    (fn [request]
      (handler (assoc request :repo-registry repo-registry)))))

(defn make-pipeline [repo-registry]
  [(make-inject-repo-registry-middleware repo-registry)
   parameters-middleware
   keywordize-param-keys-middleware
   format-negotiate-middleware
   format-request-middleware
   combine-params-middleware
   format-response-middleware
   reitit-exception/exception-middleware
   reitit-coercion/coerce-request-middleware
   reitit-coercion/coerce-response-middleware])