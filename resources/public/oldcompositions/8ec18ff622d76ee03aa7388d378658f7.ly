#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
1) | SSSSS RR |SSSSS
%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
\cadenzaOn
  \times 4/5{ c'16[ c'16 c'16 c'16 c'16] }  d'8[ d'8] \bar "|"  \times 4/5{ c'16[ c'16 c'16 c'16 c'16] }  \bar "" \break 
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