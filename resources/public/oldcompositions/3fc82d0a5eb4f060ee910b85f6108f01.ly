#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
SSSS RRRRR gggggg
%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
\cadenzaOn
  c'16[ c'16 c'16 c'16] \times 4/5{ d'16[ d'16 d'16 d'16 d'16] }  \times 4/6{ ef'16[ ef'16 ef'16 ef'16 ef'16 ef'16] }  \bar "" \break 
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
}
}
\midi {
\context {
\Score
tempoWholesPerMinute = #(ly:make-moment 200 4)
}
}
}