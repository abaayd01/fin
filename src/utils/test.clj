(ns utils.test
  (:require [clojure.test :refer [is]]))

(defmacro it
  [msg form]
  `(is ~form ~msg))
