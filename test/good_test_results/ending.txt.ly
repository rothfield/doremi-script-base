#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{


}
%{

%}
melody = {
\clef treble
\key c \major
\cadenzaOn


  c'4^"1.____" r4 r4 r4 \bar "||"   d'4^"2.____" r4 r4 r4 
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