(ns doremi_script_clojure.core
  (:gen-class
    :methods [#^{:static true}[myparse [String] String]
              #^{:static true}[json_text_to_lilypond[String] String]])
  (:require	
    [instaparse.core :as insta]

    [doremi_script_clojure.semantic_analyzer :refer [transform-parse-tree]]
    [doremi_script_clojure.to_lilypond :refer [to-lilypond]]
    [clojure.java.io :refer [input-stream resource]]
    [clojure.data.json :as json]
    [clojure.pprint :refer [pprint]] 
    [clojure.walk :refer [postwalk]]
    ))



(def doremi-script-parser  
  (insta/parser
    (slurp (resource "doremiscript.ebnf")) :total true))

(defn doremi-text->parse-tree[txt]
  (doremi-script-parser txt))

(defn doremi-script-text->parsed-doremi-script[txt]
  (transform-parse-tree (doremi-text->parse-tree txt) txt))

(defn slurp-fixture[file-name]
  (slurp (resource 
           (str "fixtures/" file-name))))

(def yesterday (slurp-fixture "yesterday.txt"))

(defn to-string[doremi-data]
  (let [
                            default-formatter (fn default-formatter[x]
                                            (array-map       :type (:my_type x)
                                                   :source (:source x)
                                                ))
        postwalk-fn (fn to-string-postwalker[node]
                      (let [ my-type (if (:is_barline node)
                                       :barline
                                       ;; else
                                       (:my_type node))
                            default-val (default-formatter node)
                            ]
                        ;; (println my-type)
                        (case my-type
                          :beat
                          (array-map :type :beat :subdivisions (:subdivisions node) :items (:items node))
                          :measure
                          (array-map :type my-type 
                                      :beat_count (:beat_count node) 
                                      :items (:items node))
                          :composition
                          (array-map :type my-type 
                                      :source (:source node)
                                      :lines (:lines node))
                          :sargam_line
                          (array-map :type my-type 
                                      :source (:source node)
                                      :items (:items node))
                          :pitch
                          (array-map :type my-type 
                                      :value (:value node)
                                      :octave (:octave node)
                                     :syllable (:syllable node)
                                     )
                          :dash
                          :dash
                          ;; (default-formatter node)
                          :barline
                          (array-map :type my-type 
                                      :source (:source node)
                                     )
                          node
                          )))
        ]
    (postwalk postwalk-fn doremi-data)
    ))

(defn pprint-results[x]
  (if (:my_type x)
    (with-out-str (json/pprint x))
    (with-out-str (pprint x))))

(defn get-stdin[]
  (with-open [rdr (java.io.BufferedReader. *in* )]
    (let [seq  (line-seq rdr)
          zz (count seq)]
      (apply str (clojure.string/join "\n" seq)))))

(defn -json_text_to_lilypond[txt]
  "Takes parsed doremi-script json data as text"
  "Returns lilypond text"
  (to-lilypond (json/read-str txt)))

(defn doremi-json-to-lilypond[x]
  ""
  (to-lilypond x))

(comment
  (-json_text_to_lilypond "{}"))



(defn -myparse[txt1]
  (try
    (let [
          txt (clojure.string/replace txt1 "\r\n" "\n")
          x (transform-parse-tree (doremi-text->parse-tree  txt)
                                  txt)
          ]
      ;;(println "x is\n" x)
      ;;(println "class of x is:" (class x))
      (pprint-results x))
    (catch Exception e (str "Error:" (.getMessage e)))
    )) 

(defn main-json[txt]
  (pprint-results 
    (transform-parse-tree (doremi-text->parse-tree  txt)
                          txt)))

(defn doremi-text->json-data[txt]
   (let [txt1 (clojure.string/replace txt "\r\n" "\n")
         parse-tree (doremi-text->parse-tree txt1)
         ]
     (transform-parse-tree parse-tree txt1)
  ))

(defn doremi-text->lilypond[txt]
   (let [txt1 (clojure.string/replace txt "\r\n" "\n")
         parse-tree (doremi-text->parse-tree txt1)
         json-data (transform-parse-tree parse-tree txt1)
         ]
     (to-lilypond json-data)
  ))

;;(pprint (doremi-text->lilypond "S"))

(defn usage[]
   (println "Usage: pipe std in as follows: \"SRG\" |  java -jar doremi-script-standalone.jar > my.ly to produce lilypond output. Or --json to produce doremi-script json format. Or --ly to produce lilypond output.") 
  )


(defn -main[& args]

  "Read from stdin. Writes results to stdout"
  "Command line params: --json returns doremi json data"
  "--ly returns lilypond data"
  "defaults to lilypond"
  (try
    (let [
          txt1 (get-stdin)
          txt (clojure.string/replace txt1 "\r\n" "\n")
          x (transform-parse-tree (doremi-text->parse-tree  txt)
                                  txt)
          ]
  (cond 
    (= (first args) "--json")
      (println (pprint-results x))
    (or (= (first args) "--ly") (empty? args)) 
     (println (to-lilypond x))
    true
     (println (usage))
      )
      )
    (catch Exception e (str "Error:" (.getMessage e)))
    ))




