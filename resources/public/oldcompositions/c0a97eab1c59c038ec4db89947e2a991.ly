#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
SSSS rrr | ggg
%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
\cadenzaOn
  c'16[ c'16 c'16 c'16] \times 2/3{ df'8[ df'8 df'8] }  \bar "|"  \times 2/3{ ef'8[ ef'8 ef'8] }  \bar "" \break 
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