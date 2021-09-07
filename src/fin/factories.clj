(ns fin.factories
  (:require
    [fin.schemas :as s]
    [clojure.string :as string]))

(defmacro factory
  [schema-name]
  (let [sym-name (symbol (string/replace (str schema-name) #".*/" ""))]
    `(defn ~sym-name
       ([]
        (~sym-name {}))
       ([overrides#]
        (merge (malli.generator/generate ~schema-name) overrides#)))))

(factory s/Transaction)
