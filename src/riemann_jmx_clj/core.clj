(ns riemann-jmx-clj.core
  (:require [clojure.java.jmx :as jmx]
            [clj-yaml.core :as yaml]
            [riemann.client :as riemann]
            [clojure.pprint :refer (pprint)]
            [clojure.string :as str])
  (:gen-class))

(defn- get-riemann-connection-helper
  [host port]
  (doto (riemann/tcp-client :host host :port port)
    (riemann/connect-client)))

(let [get-riemann-connection-helper (memoize get-riemann-connection-helper)]
  (defn get-riemann-connection
    ([host]
     (get-riemann-connection-helper host 5555))
    ([host port]
     (get-riemann-connection-helper host port))))

(defn run-queries
  "Takes a parsed yaml config and runs the contained queries, returning a seq of Riemann events."
  [yaml]
  (let [{:keys [jmx queries]} yaml]
    (->> (jmx/with-connection jmx
           (doall
             (for [{:keys [obj attr tags service]} queries
                   name (jmx/mbean-names obj)
                   attr attr ]
               {:service (if service (str service \. attr) (str (.getCanonicalName ^javax.management.ObjectName name) \. attr))
                :host (if (:event_host jmx)
                        (:event_host jmx)
                        (:host jmx))
                :state "ok"
                :metric (if (re-find #"\." attr)
                          ((jmx/read name ((str/split attr #"\.") 0)) (keyword ((str/split attr #"\.") 1)) )
                          (jmx/read name ((str/split attr #"\.") 0))) 
                :tags tags})))
         (mapcat (fn [{:keys [service metric] :as event}]
                   (if (map? metric)
                     (for [[k v] metric]
                       (assoc event
                              :service (str service \.(name k))
                              :metric v))
                     [event]))))))

(defn run-configuration
  "Takes a parsed yaml config, runs the queries, and posts the results to riemann"
  [yaml]
  (let [{{:keys [host port]} :riemann} yaml
        conn (if port
               (get-riemann-connection host port)
               (get-riemann-connection host))
        events (run-queries yaml)]
    (print ".")
    (flush)
    (riemann/send-events conn events)))

(defn munge-credentials
  "Takes a parsed yaml config and, if it has jmx username & password,
   configures the jmx environment map properly. If only a username or
   password is set, exits with an error"
  [config]
  (let [{:keys [host port username password]} (:jmx config)]
    (when (and username (not password))
      (println "Provided username but no password.")
      (System/exit 1))
    (when (and password (not username))
      (println "Provided password but no username")
      (System/exit 1))
    (if (or username password)
      (assoc config :jmx {:host host :port port :environment {"jmx.remote.credentials" (into-array String [username password])}})
      config)))

(defn start-config
  "Takes a path to a yaml config, parses it, and runs it in a loop"
  [config]
  (let [yaml (yaml/parse-string (slurp config))
        munged (munge-credentials yaml)]
    (pprint munged)
    (future
      (while true
        (try
          (run-configuration munged)   
          (catch Exception e
            (.printStackTrace e))
          (finally (Thread/sleep (* 1000 (-> yaml :riemann :interval)))))))))

(defn -main
  [& args]
  (doseq [arg args]
    (start-config arg)
    (println "Started monitors")))
