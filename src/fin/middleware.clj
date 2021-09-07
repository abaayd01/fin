(ns fin.middleware
  (:require
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

(def pipeline
  [parameters-middleware
   keywordize-param-keys-middleware
   format-negotiate-middleware
   format-request-middleware
   combine-params-middleware
   format-response-middleware
   cors-middleware])