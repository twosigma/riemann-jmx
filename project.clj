(defproject riemann-jmx-clj "0.1.0-SNAPSHOT"
  :description "A JMX connector to riemann"
  :url "https://github.com/twosigma/riemann-jmx"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jmx "0.2.0"]
                 [clj-yaml "0.4.0"]
                 [riemann-clojure-client "0.2.6"]]
  :main riemann-jmx-clj.core)
