#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
S - R - | - G


%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
  c'4 r4 d'4 r4 \bar "|"  r4 e'4 \bar "" \break 
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