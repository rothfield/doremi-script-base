(ns doremi_script_clojure.core
  (:gen-class)
  (:import (net.davidashen.text Hyphenator))
  (:require	
    [instaparse.core :as insta]
    [doremi_script_clojure.semantic_analyzer :refer [transform-parse-tree]]
    [clojure.java.io :refer [input-stream resource]]
    [clojure.data.json :as json]
    ))

(def hyphenator 
  (memoize 
    (fn []
      (println "Loading hyphen.tex")
      (let [h (new Hyphenator)]
        (.loadTable 
          h (input-stream (resource "hyphen.tex")))
        h))))

(def hyphenator-splitting-char (char 173))

(defn hyphenate[txt]
  " (hyphenate \"happy birthday\") => 
  (hap- py birth- day)
  "
  (let [hyphenated (.hyphenate (hyphenator) txt)
   hyphenated2 (clojure.string/replace hyphenated (char 173) \-)]
    (re-seq  #"\w+-?" hyphenated2)))

(defn- json-key-fn[x]
  (let [y (name x)]
    (if (= \_ (first y))
      (subs y 1)
      y)))

(defn slurp-fixture [file-name]
  (slurp (resource 
           (str "fixtures/" file-name))))

(def yesterday (slurp-fixture "yesterday.txt"))

(def doremi-script-parser  
  (insta/parser
    (slurp (resource "doremiscript.ebnf"))))

(defn run-through-parser[txt]
  (doremi-script-parser txt))

(defn pp-to-json[x]
  "For debugging, pretty print json output. Not useable"
  (json/pprint :key-fn json-key-fn)) 

(defn my-pp-json[x]
  "very primitive json pretty-printer. Changes dq,dq => dq,newline,dq "
  x
  )
;;(clojure.string/replace x "\",\"" "\",\n\""))

(defn- my-to-json[x]
  "returns json/text version of parse tree. It is a string"
  (my-pp-json (json/write-str x :key-fn json-key-fn)))


(defn -main[& args]
  (let [txt (slurp *in*)]
    (print (my-to-json (transform-parse-tree (run-through-parser txt) txt)))))

