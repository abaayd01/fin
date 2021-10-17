(ns fin.view-models)

(defn ->transaction
  [transaction]
  (merge
    (select-keys
      transaction
      [:id :description :amount :transaction_date])
    {:categories (map #(select-keys % [:name :id]) (:categories transaction))}))