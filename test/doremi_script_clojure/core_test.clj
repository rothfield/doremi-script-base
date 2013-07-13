(ns doremi_script_clojure.core-test
	(:require [clojure.test :refer :all ]
		[clojure.pprint :refer :all ]
		[doremi_script_clojure.core :refer :all ]
		[clojure.walk :refer :all ]
		[instaparse.core :as insta]
		))
(defn slurp-fixture [file-name]
    (slurp (clojure.java.io/resource 
					 (str "doremi_script_clojure/fixtures/" file-name))))
  
(def my-get-parser 
  (insta/parser
    (slurp (clojure.java.io/resource "doremi_script_clojure/doremiscript.ebnf")))
  )

(defn good-parse
	"Returns true if parse succeded and the list of expected values are in the 
	flattened parse tree"
	[txt start expected]
	(println "Testing <<\n" txt ">>/n" "with start " start " and expected " expected)
	(let [ result 
		(my-get-parser txt :total false
			:start start)
		flattened (flatten result)]
		(letfn [(my-helper [x]
			(let [found (some #(= x %) flattened)]
				(if (nil? found) 
					(do
						(println "Parsing " txt " with start " start "; Didn't find " x " in flattened result ") 
						(pprint result)
						)
					found) 
				))]
		(if (insta/failure? result)
			(do (println "Parsing " txt " with start " start "; Failed. Result is:")
				(pprint result)
				false
				)
			(do
				(pprint result)
				(every? #(true? %) (map my-helper expected))
				))
		)))

(deftest yesterday_no_chords
	(let [txt2 (slurp 
		(clojure.java.io/resource 
			"doremi_script_clojure/fixtures/yesterday_no_chords.doremiscript.txt"))
						txt (slurp-fixture "yesterday_no_chords.doremiscript.txt") 
						 ]

	(pprint txt)
	(is (good-parse txt :COMPOSITION ["Yesterday"]))
	))

(deftest composition-with-attributes-lyrics-and-sargam-section
	(let [txt (slurp 
		(clojure.java.io/resource 
			"doremi_script_clojure/fixtures/georgia.doremiscript.txt"))]
	(pprint txt)
	(is (good-parse txt :COMPOSITION ["Georgia",:UPPER_OCTAVE_DOT,".",:TALA]))
	))



(deftest lower-octave-line
	(is (good-parse ". : _" :LOWER_OCTAVE_LINE [:LOWER_OCTAVE_DOT :KOMMAL_INDICATOR ])))
		

(deftest dot
	(is (good-parse "." :DOT [:DOT "."]))
	(is (good-parse "*" :DOT [:DOT "*"]))
	(is (good-parse "*" :LOWER_OCTAVE_DOT [:DOT "*"]))
	(is (good-parse "." :LOWER_OCTAVE_DOT [:DOT "."]))
	(is (good-parse "*" :UPPER_OCTAVE_DOT [:DOT "*"]))
	(is (good-parse "." :UPPER_OCTAVE_DOT [:DOT "."]))
		)


(deftest sargam-pitch-can-include-left-slur
	(is (good-parse "(S" :SARGAM_PITCH [:BEGIN_SLUR :S ]))
		)
(deftest lower-octave-dot
	(is (good-parse "." :LOWER_OCTAVE_DOT ["."])))
		
(deftest kommal_underscore
	(is (good-parse "_" :KOMMAL_INDICATOR ["_"])))
		


(deftest lower-lower-octave
	(is (good-parse ":" :LOWER_LOWER_OCTAVE_SYMBOL [":"])))
		

(defn parse-fails? [text-to-parse starting-production]
	(let [result (my-get-parser text-to-parse :start starting-production)]
		(insta/failure? result)
		)
	)

(deftest sargam-notes
	(let [txt "SrRgGmMPdDnNSbS#R#G#MP#D#N#"]
	(is (good-parse txt :BEAT [:S :Sb :N :Nsharp])))
)

(deftest composition
	(let 
		[txt "foo:bar\ncat:dog\n\n | S R G R |\n"]
	(is (good-parse txt :COMPOSITION ["foo" "bar" "cat" "dog"])))
)

(deftest syllable
	(is (good-parse "foo" :SYLLABLE  ["foo"])))


(deftest lyrics-section1
	(let [txt "  Georgia georgia\nNo peace I find ba-by"]
	     (is (good-parse txt :LYRICS_SECTION   [:HYPHENATED_SYLLABLE ])))
    )

(deftest upper-octave-line-item
	(let [txt "."
		result (my-get-parser txt :total true :start :UPPER_OCTAVE_LINE_ITEM )
		flattened (flatten result)]
		(pprint result)
		(is (some #(= "." %) flattened))
		))
(deftest upper-octave-line
	(let [txt ". + 0 2 3"
		result (my-get-parser txt :total true :start :UPPER_OCTAVE_LINE )
		flattened (flatten result)]
		(pprint result)
		(is (some #(= "+" %) flattened))
		(is (some #(= "2" %) flattened))
		))







(deftest sargam-ornament
	(is (good-parse "PMDP" :SARGAM_ORNAMENT  [:P :D])))

(deftest alternate_ending
	(is (good-parse "1._____" :ALTERNATE_ENDING_INDICATOR  []))
	(is (good-parse "3_____" :ALTERNATE_ENDING_INDICATOR  [])))
 
  

(deftest syllable-with-hyphen
	(is (good-parse "foo-   bar baz-" :LYRICS_LINE ["foo-"])))


(deftest composition-attributes-and-sargam-sections
	(let [txt "foo:bar  \ndog:cat    \n\n | S R G | "
		result (my-get-parser txt :total false :start :COMPOSITION )
		flattened (flatten result)]
		(pprint result)
		(is (some #(= :G %) flattened))
		))
(deftest composition-two-attribute-sections
	(let [txt "foo:bar  \ndog:cat    \n\nhat:bat"
		result (my-get-parser txt :total true :start :COMPOSITION )
		flattened (flatten result)]
		(pprint result)
		(is (some #(= :ATTRIBUTE_LINE %) flattened))
		))

(deftest composition-one-attribute-no-eol
	(let [result (my-get-parser "foo:bar   " :start :COMPOSITION )
		flattened (flatten result)]
		(pprint result)
    ;(println flattened)
    (is (some #(= :ATTRIBUTE_LINE %) flattened))
    )
	)
(deftest section-two-attributes-no-eol
	(let [result (my-get-parser "foo:bar  \ndog:cat    " :total true :start :SECTION )
		flattened (flatten result)]
		(pprint result)
    ;(println flattened)
    (is (some #(= :ATTRIBUTE_SECTION %) flattened))
    (is (some #(= :SECTION %) flattened))
    )
	)

(deftest sargam-section
	(let [result (my-get-parser "  | S R G - | " :start :SARGAM_SECTION )
		flattened (flatten result)]
    ;(println result)
    ;(println flattened)
    (is (some #(= :S %) flattened))
    (is (some #(= :MEASURE %) flattened))
    (is (some #(= :R %) flattened))
    (is (some #(= :G %) flattened))
    )
	)
(deftest attribute-line
	(is (good-parse "foo:bar" :ATTRIBUTE_LINE ["foo" "bar" ]))
	(is (good-parse "foo : bar" :ATTRIBUTE_LINE ["foo" "bar" ]))
	(is (good-parse "foo : bar    " :ATTRIBUTE_LINE ["foo" "bar" ]))
)
(deftest attribute-section
	(let [result (my-get-parser "foo:bar\ncat:dog" :start :ATTRIBUTE_SECTION )
		flattened (flatten result)]
    ;(println result)
    ;(println flattened)
    (is (some #(= "foo" %) flattened))
    (is (some #(= "bar" %) flattened))
    (is (some #(= "cat" %) flattened))
    (is (some #(= "dog" %) flattened))
    )
	)

(deftest attributes
	(is (good-parse "foo:bar" :ATTRIBUTE_LINE ["foo" "bar"])))

(deftest line-number
	(let [result (my-get-parser "1)" :start :LINE_NUMBER )
	flattened (flatten result)]
    ;(println flattened)
    (is (some #(= "1" %) flattened))
    (is (some #(= :LINE_NUMBER %) flattened))
    )
	)
(deftest sargam-line-2
	(let [result (my-get-parser "1) | S- Rgm |" :start :SARGAM_LINE )
	flattened (flatten result)]
    ;(println result)
    (is (some #(= :S %) flattened))
    (is (some #(= :R %) flattened))
    (is (= 2 (count (filter #(= :BEAT %) flattened))) "beat count off")
    )
	)
(deftest sargam-line-simple
	(is (good-parse "| S R |" :SARGAM_LINE [:S :R :BEAT ])))




(deftest beat-can-be-delimited-with-angle-brackets
	(is (good-parse "<S>" :BEAT_DELIMITED [:S :BEAT_DELIMITED ])))

(deftest beat-can-be-delimited-with-angle-brackets-more-than-one-note-with-spaces
	(is (good-parse "<S r>" :BEAT_DELIMITED [:S :r :BEAT_DELIMITED ])))

(deftest sargam-pitch-can-include-right-slur
	(is (good-parse "S)" :SARGAM_PITCH [:S :END_SLUR ])))

(deftest parses-double-barline
	(is (good-parse "||" :DOUBLE_BARLINE [:DOUBLE_BARLINE ])))

(deftest doesnt-parse-single-barline-when-it-sees-double-barline
	(is (parse-fails? "||" :SINGLE_BARLINE ))
		"")

(deftest test-left-repeat
	(is (good-parse "|:" :LEFT_REPEAT [:LEFT_REPEAT ])))

(deftest test-final-barline
	(is (good-parse "|]" :FINAL_BARLINE [:FINAL_BARLINE ])))

(deftest test-reverse-final-barline
	(is (good-parse "[|" :REVERSE_FINAL_BARLINE [:REVERSE_FINAL_BARLINE ])))


(deftest test-right-repeat
	(is (good-parse ":|" :RIGHT_REPEAT [:RIGHT_REPEAT ])))


(deftest test-tala
  (let [start :TALA
        items "+2034567"]
	     (is (good-parse "+" :TALA ["+"]))
	     (is (good-parse "0" :TALA ["0"]))
	     (is (good-parse "2" :TALA ["2"]))
   )
  )
 

(deftest dash
	(is (good-parse "-" :DASH [:DASH ])))

(deftest test-repeat-symbol
	(is (good-parse "%" :REPEAT_SYMBOL [:REPEAT_SYMBOL ])) "")

(defn test-some
	[]
	(sargam-pitch-can-include-left-slur)
	(composition-with-attributes-lyrics-and-sargam-section)
	(dash)
	(attributes)
	(parses-double-barline)
	(test-final-barline)
	(test-repeat-symbol)
	(syllable-with-hyphen)
	(beat-can-be-delimited-with-angle-brackets-more-than-one-note-with-spaces)
	(sargam-pitch-can-include-right-slur)
	(sargam-line-simple)
	(beat-can-be-delimited-with-angle-brackets)
	)

;(run-tests)
; (composition-with-attributes-lyrics-and-sargam-section)

(println "loaded core-test")
;  (test-some)
;  (sargam-line-simple)
;  (test-tala)
;  	(composition-with-attributes-lyrics-and-sargam-section)
;  (dot)
;  (yesterday_no_chords)
;  (attribute-line)
;  
;(composition-with-attributes-lyrics-and-sargam-section)
;(yesterday_no_chords)
;(dash)
;(sargam-notes)
;(composition)
(lyrics-section1)
