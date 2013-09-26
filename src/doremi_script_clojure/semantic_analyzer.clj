(ns doremi_script_clojure.semantic_analyzer
  "Semantic analysis is the activity of a compiler to determine what the types of various values are, how those types interact in expressions, and whether those interactions are semantically reasonable. "
  (:require	
    ;;  [doremi_script_clojure.test-helper :refer :all ]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]] 
    [clojure.walk :refer [postwalk-demo postwalk postwalk-replace keywordize-keys]]
    [clojure.string :refer [lower-case]]
    [instaparse.core :as insta]
    [clojure.data.json :as json]
    ))
(comment
  ;; to use in repl:
  ;; cpr runs current file in vim
  (use 'doremi_script_clojure.semantic_analyzer :reload) (ns doremi_script_clojure.semantic_analyzer) 
  (use 'clojure.stacktrace) 
  (print-stack-trace *e)
  (use 'doremi_script_clojure.test-helper :reload)  ;; to reload the grammar
  (print-stack-trace *e)
  (pst)
  )

(defn p[] (println "************************"))
(def yesterday (slurp (io/resource "fixtures/yesterday.txt")))

(def doremi-script-parser  
  (insta/parser
    (slurp (io/resource "doremiscript.ebnf"))))

(defn start-index[z]
  "Looks up starting index of the node from the node's 
  metadata. instaparse adds the metadata in the parsing process"
  "Returns the character position in the source code where the object starts"
  (first (instaparse.core/span z))    
  )

(def unit-of-rhythm
  #{ :pitch  :dash}
  )

(def sargam-pitch-to-source
  (array-map
    :Sb "Sb" 
    :Ssharp "S#"
    :Rsharp "R#" 
    :Gsharp "G#" 
    :Psharp "P#" 
    :Dsharp "D#" 
    :Nsharp "N#" 
    :Pb "Pb" 
    :S "S" 
    :r "r" 
    :R "R" 
    :g "g" 
    :G "G" 
    :m "m" 
    :M "M" 
    :P "P" 
    :d "d" 
    :D "D" 
    :n "n" 
    :N "N" 
    ))

(def to-normalized-pitch
  {
   :S "C"
   :r "Db"
   :R "D"
   :g "Eb"
   :G "E"
   :m "F"
   :M "F#"
   :P "G"
   :d "Ab"
   :D "A"
   :n "Bb"
   :N "B"
   :Sb "Cb" 
   :Ssharp "C#"
   :Rsharp "R#" 
   :Gsharp "E#" 
   :Psharp "G#" 
   :Pb "Gb" 
   :Dsharp "A#" 
   :Nsharp "B#" 
   })


(defn my-seq[x]
  "seq through the data structure, which is like"
  " {:items [ {:items [1 2 3]} 2 3]}"
  "Don't include items like {:items [1 2 3]} "
  "just [1 2 3]"
  (filter #(not (:items %))
          (tree-seq
            (fn branch?[x] (or (vector? x) (map? x))) 
            (fn children[y] 
              (cond 
                (and (map? y) (:items y)) 
                (:items y)
                (vector? y)
                (rest y)))
            x)))

(defn line-column-map [my-map line]
  "my-map is a map from column number -> list of nodes
  Add nodes from line to the map"
  (reduce (fn [accumulator node]
            ;;(pprint "line-column-map")
            ;;(pprint node)
            ;;(pprint line)
            (let [column (- (:_start_index node) 
                            (:_start_index line))]

              (assoc accumulator
                     column 
                     (conj (get accumulator column) node)
                     )))
          my-map (filter #(:_start_index %) (my-seq line))
          )) 


(defn rationalize-new-lines[txt]
  (clojure.string/replace txt #"\r\n" "\n")
  )

(defn get-source[node txt]
  (if  (instaparse.core/span node)
    (apply subs txt (instaparse.core/span node))
    node
    ))

(defn run-through-parser[txt]
  (doremi-script-parser txt))

(defn json-key-fn[x]
  (let [y (name x)]
    (if (= \_ (first y))
      (subs y 1)
      y)))

(defn pp-to-json[x]
  "For debugging, pretty print json output. Not useable"
  (json/pprint :key-fn json-key-fn)) 

(defn- my-to-json[x]
  "returns json/text version of parse tree. It is a string"
  (json/write-str x :key-fn json-key-fn))

(defn- update-sargam-pitch-node [pitch nodes]
  (if false (do
              (println "update-sargam-pitch-node********")
              (pprint pitch) (println "nodes") (pprint nodes) (println "end nodes")
              (println "********")))
  (let [
        upper-dots (count (filter #(= (:_my_type %) :upper_octave_dot) nodes))
        lower-dots (count (filter #(= (:_my_type %) :lower_octave_dot) nodes))
        upper-upper-dots (count (filter #(= (:_my_type %) :upper_upper_octave_symbol) nodes))
        lower-lower-dots (count (filter #(= (:_my_type %) :lower_lower_octave_symbol) nodes))
        octave (+ upper-dots (- lower-dots) (* -2 lower-lower-dots) (* 2 upper-upper-dots))
        ]
    (merge pitch 
           {
            ;; TODO: review which nodes gets added to attributes
            :attributes 
            (into [] (concat (:attributes pitch) 
                             (filter #(#{:begin_slur 
                                         :end_slur
                                         :chord_symbol
                                         :ending
                                         :mordent} (:_my_type %)) nodes)))
            :octave octave
            :syllable (first (keep #(if (= (:_my_type %) :syllable)  (:_source %)) nodes))
            :chord (first (keep #(if (= (:_my_type %) :chord_symbol)  (:_source %)) nodes))
            :ornament nil 
            :tala (first (keep #(if (= (:_my_type %) :tala)  (:_source %)) nodes))
            }
           )))

(defn collapse-sargam-section [sargam-section txt]
  ;;  (pprint sargam-section) (println "************^^^^")
  "main logic related to lining up columns is here"
  "Deals with the white-space significant aspect of doremi-script"
  "given a section like

  .
  S
  Hi

  "
  "Returns the sargam-line with the associated objects in the same column attached
  to the corresponding pitches/items on main line. The dot becomes an upper octave of S and 
  Hi becomes a syllable for S"
  "Assign attributes to the main line(sargam_line) from the lower and upper lines using 
  column numbers. Returns a sargam-line"
  (assert (= (:_my_type sargam-section) :sargam_section))
  (assert (string? txt))
  (let [
        sargam-line (some #(if (= (:_my_type %) :sargam_line) %)
                          (:items sargam-section))
        column-map (reduce line-column-map {}  (:items sargam-section))
        line-starts (map :_start_index (:items sargam-section))
        line-start-for  (fn line-start-for-fn[column] 
                          (last (filter (fn[x] (>= column x)) line-starts)) )
        column-for-node (fn[node]
                          (- (:_start_index node) (line-start-for (:_start_index node))))
        postwalk-fn (fn sargam-section-postwalker[node]
                      "TODO: in progress- rewrite for new version!!copied from old"
                      (cond
                        (not (map? node))
                        node
                        (not (:_my_type node))
                        node
                        true
                        (let [
                              column (column-for-node node)
                              nodes (get column-map column) 
                              my-type (:_my_type node)
                              source (:_source  node)
                              ]
                          (cond
                            (= my-type :pitch)
                            (update-sargam-pitch-node node nodes)
                            true
                            node))))
        ]
    (assert (= :sargam_line (:_my_type sargam-line)))
    (assert (map? column-map))
    (postwalk postwalk-fn sargam-section)
    ))
(defn make-sorted-map[node]
  (cond 
    ;;    true
    ;;   node
    (and (map? node) (= (into (hash-set) (keys node)) #{:numerator :denominator}))
    node
    (map? node)
    (into (sorted-map) node)
    true
    node))

(defn make-maps-sorted[x]
  (into (sorted-map) (postwalk make-sorted-map x)))

(defn backwards-comparator[k1 k2]
  (compare k2 k1))
;;(def pitch-counter (atom -1))


;; Notes on dash, dashes handling
;;
;; Rule 1: Dashes at beginning of beat should be tied to previous note in current line
;; the previous pitch should be marked :tied => true
;; The dash should be marked
;; dash_to_tie: true,
;; pitch_to_use_for_tie: <pitch> 
;;
;;
;;
;; example 1:  S---
;; dashes will look like
;; { my_type: 'dash', source: '-' } { my_type: 'dash', source: '-' }
;;
;; example 2: -S
;; In this case the dash isn't tied to any previous note
;; dashes will look like
;; { my_type: 'dash',numerator: 1, denominator: 2, dash_to_tie: false,rest: true }
;; example 3:
;;  S --R
;                  [ { my_type: 'dash',
;                      source: '-',
;                      numerator: 2,
;                      denominator: 3,
;                      dash_to_tie: true,
;                      pitch_to_use_for_tie: 
;                       { my_type: 'pitch',
;                         normalized_pitch: 'C',
;                         attributes: [],
;                         pitch_source: 'S',
;                         source: 'S',
;                         column_offset: 0,
;                         octave: 0,
;                         numerator: 1,
;                         denominator: 1,
;                         tied: true,
;                         fraction_array: 
;                          [ { numerator: 1, denominator: 1 },
;                            { numerator: 2, denominator: 3 } ],
;                         fraction_total: { numerator: 5, denominator: 3 },
;                         column: 0 },
;                      column: 2 },
;                    { my_type: 'dash', source: '-', column: 3 },
;;
(defn main-walk[node txt]
  ;; (pprint node)
  (cond
    (not (vector? node))
    node
    true
    (let [
          my-key (first node)
          ;; zzz (println "main-walk, my-key =>" my-key)
          my-map (array-map :_my_type (keyword (lower-case (name my-key)))
                            :_source (get-source node txt)
                            :_start_index (start-index node) 
                            )
          ;;zz (pprint "&&&&&&&&")
          ;;z (pprint my-key)
          node2 (if (and (vector? (second node)) 
                         (keyword? (first (second node)))
                         (.endsWith (name (first (second node))) "ITEMS"))
                  (merge {:items (subvec  (second node) 1) } my-map)
                  ;; else
                  node)
          ]
      (cond
        ;;[:BEGIN_SLUR_SARGAM_PITCH
        ;;         [:BEGIN_SLUR "("]
        ;;        [:SARGAM_PITCH [:SARGAM_MUSICAL_CHAR [:S]]]]
        (= :CHORD_SYMBOL my-key)
        my-map
        (#{:BEGIN_SLUR_SARGAM_PITCH} my-key)
        (let [
              [_ begin-slur my-pitch2] node
              my-pitch (merge my-pitch2
                              {:column_offset 1
                               :_source (:_source my-map)
                               })
              ]
          (if false (do
                      (pprint my-map)
                      (pprint "******************")
                      (println "aa")
                      (pprint node)
                      )
            ;; add begin slur to attributes
            (assoc my-pitch 
                   :attributes
                   (conj (into [] (:attributes my-pitch)) begin-slur))))
        (#{:UPPER_OCTAVE_LINE} my-key)

        (merge  my-map (array-map :items (subvec node 1)) )
        (= :SYLLABLE my-key)
        ;;  (do ;(println my-map)
        ;(println "syllable case")
        my-map 
        (= :COMPOSITION my-key)
        (let [ sections 
              (filter #(= :sargam_section (:_my_type %))  (:items node2))
              lines
              (into [] (map  (fn[x] (some #(if (= :sargam_line (:_my_type %))
                                             %) 
                                          (:items x))) sections))
              ] 
          (merge {:lines  lines 
                  :warnings []
                  :id 999
                  :notes_used ""
                  :force_sargam_chars_hash {}
                  :time_signature "4/4"
                  :mode "major"
                  :key "C"
                  :author ""
                  :apply_hyphenated_lyrics false
                  :title ""
                  :filename "untitled"
                  } 
                 my-map))
        (#{:PITCH_WITH_DASHES :DASHES} my-key)
        ;; Handles things like S--  ---   
        ;; --
        ;; The first item is significant and will get counted rhythmically
        ;; returns [ pitch dash dash ] or [dash dash dash]
        (let [
              ;; dfadfadsf (println "my-key" my-key)
              all-items (subvec node2 1)
              main-item (first all-items)
              ;; zzzzzz (println "node2-->")
              ;; adfadf (pprint node2)
              ;; zz (println "main-item is:")
              ;;; zzz (pprint main-item)
              rest-items (subvec all-items 1)
              ;; following line bombs
              rest-items2 (into [] (map #(assoc % :ignore true) rest-items))
              ;; ddsadfafd  (println "rest-items-->\n\n")
              ;; fadfakkk (pprint rest-items)
              ;; ddsadfafd  (println "rest-items^^^\n\n")
              main-item2 (assoc main-item  :numerator 
                                (count 
                                  (filter (fn[x] (#{:pitch :dash} (:_my_type x)))
                                          all-items)))]
          (into [] (concat [main-item2] rest-items2)))
        (= :BEAT my-key)
        (let [
              ;; [ [pitch dash dash] pitch pitch ] => [pitch dash dash pitch pitch]
              ;; apply concat to get rid of pitch with dashes' array
              my-fun (fn[z]
                       (apply concat (into [] (map (fn[x] (if (vector? x) x (vector x))) z))))
              items2 (into [] (my-fun (:items node2)))  ;; TODO: ugly
              subdivisions 
              (count (filter (fn[x] (unit-of-rhythm (:_my_type x))) 
                             items2))
              my-beat (assoc node2 :items items2 :_subdivisions subdivisions)
              ]
          ;;;
          ;;;(pprint my-beat)
          (postwalk (fn postwalk-in-beat[z] 
                      ;;(println "postwalk-in-beat -z ---------->")
                      ;;(println "subdivisions are " subdivisions)
                      ;;(if (= 0 subdivisions)
                      ;; (do
                      ;; (println "0 subdivisions. my-beat is : ")
                      ;; (pprint my-beat)))
                      ;;(pprint z)
                      ;;(assert (not (= 0 subdivisions)))
                      ;;(println "z is")
                      ;;(pprint z)
                      ;; (pprint "z is")
                      ;; (println "postwalk-in-beat -z ---------->")
                      ;; (pprint z)
                      ;;(println "\n\n")
                      (cond 
                        (= :beat (:_my_type z))
                        z
                        ;; (assoc z :items (into [] (apply concat (:items z))))
                        (not (#{:pitch :dash} (:_my_type z)))
                        z
                        (:ignore z)
                        z
                        (not (:numerator z))
                        z
                        true 
                        (do
                          ;;(println "post-walk in beat, z is --->")
                          ;;  (pprint z)
                          (let [my-ratio (/ (:numerator z) subdivisions)
                                ;; zz (println "my-ratio is" my-ratio)
                                frac 
                                (if (= (class my-ratio) java.lang.Long)
                                  (sorted-map-by backwards-comparator  :numerator 1 
                                                 :denominator 1) 
                                  ;; else 
                                  (sorted-map-by  backwards-comparator 
                                                 :numerator (numerator my-ratio)
                                                 :denominator (denominator my-ratio)))
                                ]
                            (assoc z 
                                   :denominator subdivisions
                                   :fraction_array 
                                   [ frac ])))))
                    my-beat))
(#{:MORDENT :UPPER_UPPER_OCTAVE_SYMBOL :LOWER_OCTAVE_DOT :LOWER_LOWER_OCTAVE_SYMBOL :UPPER_OCTAVE_DOT :LINE_NUMBER :BEGIN_SLUR :END_SLUR} my-key)
my-map
(= :DASH my-key)
;; TODO: I think not needed now
my-map
;; (merge my-map (sorted-map :numerator 1))
(= :BARLINE my-key)
(merge  my-map (sorted-map :_my_type (keyword (keyword (lower-case (name (get-in node [1 0])))))
                           :is_barline true))
(= :SARGAM_LINE my-key)
;; Here is a good place to handle ties/dashes/rests
;; Number the significant pitches and dashes in this line, starting with 0
;; NEEDS WORK
;; Given S- -R
;; we want to tie the dash at beginning of beat 2 to S
;; In general, a dash at the beginning of a beat will always be tied to the previous dash or
;; pitch, except if the very first beat starts with a dash
;;  S- -- --  
;;  |  |  |    

(let [

      line node2
      pitch-counter (atom -1)
      significant? (fn significant2[x]
                     "don't number dashes such as the last 2 of S---"
                     (and (map? x) (#{:pitch :dash} (:_my_type x))
                          (not (:ignore x))))
      line2 (postwalk (fn add-pitch-counters[z] (cond
                                                  (not (significant? z))
                                                  z
                                                  true
                                                  (do
                                                    (swap! pitch-counter inc)
                                                    (assoc z :pitch-counter @pitch-counter))))
                      line)
      pitches (into []  (filter 
                          significant? (my-seq line2) ))
      line3 (postwalk (fn line3-postwalk[z]
                      ;;  (println "**z ===> " z)
                        (cond  
                          (= :beat (:_my_type z))
                          (let [ 
                              beat-counter (atom -1)
                              pitches-in-beat (into []  (filter 
                              significant? (my-seq z) ))
                                ]
                            (postwalk (fn[a]
                                        (cond (significant? a)
                                              (do
                                             (swap! beat-counter inc)
                                             (assoc a :beat-counter @beat-counter))
                                             true
                                             a 
                                        ))  z)
                            ;;(println "beat case")
                            ;;(println "d is" pitches-in-beat)
                             )
                          true
                          z)) line2)
      ] 
  (postwalk (fn walk-line-tieing-dashes-and-pitches[zzz] 
              "if item is dash at beginning of line, set dash_to_tie false and rest true
              if item is dash (not at beginning of line) and line starts with a pitch"
              ;;; (println "***zzz, significant?" zzz (significant? zzz))
              (cond 
                (not (significant? zzz))
                zzz
                true
                (let
                  [
                   my-key (:_my_type zzz) 
                   prev-item (last (filter #(and (significant? %)
                                                  (< (:pitch-counter %) (:pitch-counter zzz))) pitches))
                   next-item  (first (filter #(and (significant? %)
                                                  (> (:pitch-counter zzz) (:pitch-counter %))) pitches))
                   ]
                  (cond (and (= 0 @pitch-counter)
                             (= my-key :dash))  ;; dash is first item in line
                        (assoc zzz 
                               :dash_to_tie false
                               :rest true )
                        (and (= :dash my-key) (= 0 (:beat-counter zzz))
                             prev-item)
                        (do (println "dash case, prev-item is " prev-item "current is" zzz)
                            (assoc zzz 
                               :dash_to_tie true
                               :pitch_to_use_for_tie prev-item))
                        (= :pitch my-key) 
                        (let []
                          (println "pitch case")
                          (println "prev-pitch" prev-item)
                          (assoc zzz :tied true)
                        )
                        ))

                ))
            line3))
(= :SARGAM_SECTION my-key)
(let [collapsed
      (collapse-sargam-section 
        (merge (sorted-map :items (subvec node 1)) my-map)
        txt)]
  ;;(pprint collapsed)
  collapsed
  )
(= :SARGAM_PITCH my-key)
(let [
      sarg  (some #(if (= (first %) :SARGAM_MUSICAL_CHAR) (first (second %))) (rest node))
      ]
  ;; (swap! pitch-counter inc)
  (merge 
    my-map
    (sorted-map  
      ;; :_pitch_counter @pitch-counter
      :_my_type :pitch
      :numerator 1  ;;; numerator and denominator may get updated later!
      :denominator 1
      :column_offset 0  ;; may get updated
      :normalized_pitch (sarg to-normalized-pitch)
      :pitch_source (sarg sargam-pitch-to-source)
      )))
true
node2
)))) 



(defn my-seq2[x]
  (tree-seq (fn branch?[node]
              true)
            (fn children[node]
              (cond 
                (and (map? node) (:items node))
                (:items node)
                (and (map? node) (:lines node))
                (:lines node)
                (vector? node)
                identity))
            x))

(defn doremi-script-parse[txt]
  "parse the text"
  ;;(println "processing")
  ;;(println txt)
  (let [
        parse-tree (run-through-parser txt)
        parse-tree2 (postwalk (fn[node] (main-walk node txt)) parse-tree)
        ]
    (make-maps-sorted parse-tree2) 
    ))

(def t1 "Dm7\nS")
;; (def t2 "S--S --R-")
;;(def t2 "Srgm")
;(def t2 "---S r-")
(def t2 "S--r-g-- -S")
(def t2 "S--r-g-- -S")
(def t2 "S-\n\n-R\n\nS- -R")
(pprint (run-through-parser t2))

(pprint (doremi-script-parse t2))

;;(def z1 (doremi-script-parse t2))
;;(pprint (run-through-parser yesterday))
;;(doremi-script-parse yesterday)
;;(pprint (doremi-script-parse yesterday))
;;(pprint (run-through-parser ":\n*\nS\n.\n:\nHi"))
(defn doremi-script-to-json[txt]
  (my-to-json (doremi-script-parse txt)))

