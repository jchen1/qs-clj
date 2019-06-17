(defproject qs-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[bidi "2.1.6"]
                 [cheshire "5.8.1"]
                 [clj-http "3.10.0"]
                 [com.stuartsierra/component "0.4.0"]
                 [com.stuartsierra/component.repl "0.2.0"]
                 [environ "1.1.0"]
                 [org.clojure/clojure "1.10.0"]
                 [ring "1.7.1"]]
  :plugins [[lein-dotenv "1.0.0"]]
  :repl-options {:init-ns user})
