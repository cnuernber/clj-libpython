(ns libpython-clj2.python.io-redirect
  (:require [libpython-clj2.python.class :as py-class]
            [libpython-clj2.python.ffi :as py-ffi]
            [libpython-clj2.python.bridge-as-python :as py-bridge-py]
            [libpython-clj2.python.base :as py-base]
            [libpython-clj2.python.protocols :as py-proto])
  (:import [java.io Writer]))


(defn self->writer
  ^Writer [self]
  (deref (py-bridge-py/py-self->jvm-obj self)))


(def writer-cls*
  (py-bridge-py/pydelay
   (py-class/create-class
    "jvm_io_bridge"
    nil
    {"__init__" (py-class/make-tuple-instance-fn
                 (fn [self jvm-handle]
                   (py-proto/set-attr! self "jvm_handle" jvm-handle)
                   nil))
     "write" (py-class/make-tuple-instance-fn
              (fn [self & args]
                (when (seq args)
                  (.write (self->writer self) (str
                                               (py-base/->jvm
                                                (first args)))))
                (py-ffi/py-none))
              {:arg-converter identity})
     "flush" (py-class/make-tuple-instance-fn
              (constantly (py-ffi/py-none)))
     "isatty" (py-class/make-tuple-instance-fn
               (constantly (py-ffi/py-false)))})))


(defn setup-std-writer
  [writer-var sys-mod-attname]
  (assert (instance? Writer (deref writer-var)))
  (py-ffi/with-gil
    (let [sys-module (py-ffi/import-module "sys")
          std-out-writer (@writer-cls*
                          (py-bridge-py/make-jvm-object-handle writer-var))]
      (py-proto/set-attr! sys-module sys-mod-attname std-out-writer)
      :ok)))


(defn redirect-io!
  []
  (setup-std-writer #'*err* "stderr")
  (setup-std-writer #'*out* "stdout"))
