#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
S - S

%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
  c'4 r4 c'4 \bar "" \break 
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