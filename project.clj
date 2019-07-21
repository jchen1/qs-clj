(defproject qs-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[bidi "2.1.6"]
                 [cheshire "5.8.1"]
                 [clj-http "3.10.0"]
                 [clojure.java-time "0.3.2"]
                 [com.datomic/datomic-pro "0.9.5786" :exclusions [org.slf4j/slf4j-nop]]
                 [com.fzakaria/slf4j-timbre "0.3.13"]
                 [com.stuartsierra/component "0.4.0"]
                 [com.stuartsierra/component.repl "0.2.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [environ "1.1.0"]
                 [io.replikativ/hasch "0.3.5"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.500"]
                 [org.eclipse.jetty/jetty-http "9.4.12.v20180830"]
                 [org.slf4j/log4j-over-slf4j "1.7.14"]
                 [org.slf4j/jul-to-slf4j "1.7.14"]
                 [org.slf4j/jcl-over-slf4j "1.7.14"]
                 [potemkin "0.4.5"]
                 [ring "1.7.1"]
                 [ring/ring-codec "1.1.2"]
                 [ring/ring-json "0.4.0"]]
  :plugins [[lein-dotenv "1.0.0"]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :repl-options {:init-ns user})
