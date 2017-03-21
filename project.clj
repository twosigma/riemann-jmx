(defproject riemann-jmx-clj "0.1.0-SNAPSHOT"
  :description "A JMX connector to riemann"
  :url "https://github.com/twosigma/riemann-jmx"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jmx "0.3.3"]
                 [clj-yaml "0.4.0"]
                 [riemann-clojure-client "0.4.4"]]
  :main riemann-jmx-clj.core)
