(ns grpc.utils
  (:require [camel-snake-kebab.core :as csk]))

(defmacro build-grpc-service
  [service-class
   service-definition]
  (let [method-definitions
        (map
          (fn [[k v]]
            (let [request          (gensym)
                  responseObserver (gensym)]
              `(~(symbol k)
                 [~request ~responseObserver]
                 (let [response# (~v (~bean ~request))]
                   (.onNext ~responseObserver response#)
                   (.onCompleted ~responseObserver)))))
          service-definition)]
    `(proxy [~service-class]
            []
       ~@method-definitions)))

(defmacro new-builder [c]
  `(~(symbol (str c "/newBuilder"))))

(defn make-setter-fns [m]
  (map (fn [[k v]] `(~(symbol (str ".set" (csk/->PascalCase (name k)))) ~v)) m))

(defmacro apply-setter-calls [builder m]
  (let [setter-fns (make-setter-fns m)]
    `(-> ~builder
         ~@setter-fns)))

(defmacro build-response [c m]
  `(-> (new-builder ~c)
       (apply-setter-calls ~m)
       .build))

(defmacro build-request [c m]
  `(-> (new-builder ~c)
       (apply-setter-calls ~m)
       .build))

(defn- dissoc-*-bytes-keys [m]
  (let [keys-to-dissoc (map keyword (filter #(re-matches #".*Bytes" %) (map name (keys m))))]
    (apply dissoc m keys-to-dissoc)))

(defn response->map [response]
  (dissoc-*-bytes-keys
    (dissoc
      (bean response)
      :serializedSize
      :initialized
      :descriptorForType
      :parserForType
      :allFields
      :class
      :defaultInstanceForType
      :unknownFields
      :initializationErrorString)))
