#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
title = ""
composer = ""
  tagline = ""  % removed 
}
%{
 S S- S-S SSS SSSS S--R--G 
 %}
  
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c \major
\autoBeamOn  
\cadenzaOn
 c'4 c'4  \times2/3 { c'4  c'8 } \times2/3 { c'8 c'8 c'8 } c'16[ c'16 c'16 c'16] \times2/7 { c'8.   d'8.   e'16 } \break

}

text = \lyricmode {
  
}

\score{

<<
  \new Voice = "one" {
    \melody
  }
  \new Lyrics \lyricsto "one" \text
>>
\layout {
  \context {
       \Score
    \remove "Bar_number_engraver"
  } 
  }
\midi { 
  \context {
    \Score
    tempoWholesPerMinute = #(ly:make-moment 200 4)
   }
 }
}
