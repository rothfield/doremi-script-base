#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
title = "test semantic analyzer"
composer = ""
  tagline = ""  % removed 
}
%{
 Title: test semantic analyzer

~
+    *
:
.     S
*
Dm7
<gRg>
g
.
hi- 
 %}
  
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c \major
\autoBeamOn  
\cadenzaOn
 ef'''4\mordent^"Dm7"
}

text = \lyricmode {
  hi-
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
