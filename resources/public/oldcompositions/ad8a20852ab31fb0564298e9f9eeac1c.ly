#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
SSSSSSSSS   RRRRRR
%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
\cadenzaOn
  \times 8/9{ c'32[ c'32 c'32 c'32 c'32 c'32 c'32 c'32 c'32] }  \times 4/6{ d'16[ d'16 d'16 d'16 d'16 d'16] }  \bar "" \break 
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