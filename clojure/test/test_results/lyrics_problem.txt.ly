#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{ 
}
%{
| GP - -  - | GR - - - | 
  Geor-gia geor-gia 

 
  
  


%}
melody = {
\once \override Staff.TimeSignature #'stencil = ##f
\clef treble
\key c 
\major
\cadenzaOn
  e'8[ g'8] r4 r4 r4 \bar "|"  e'8[ d'8] r4 r4 r4 \bar "|" \break 
 }
text = \lyricmode {
Geor- gia geor- gia 
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