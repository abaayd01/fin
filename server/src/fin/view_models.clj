(ns fin.view-models)

(defn ->category [category] (select-keys category [:name :id]))

(defn ->transaction
  [transaction]
  (merge
    (select-keys
      transaction
      [:id :description :amount :transaction_date])
    {:categories (map ->category (:categories transaction))}))