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


  c'4 r4 r4 \bar "|"  d'4 r4 c'4 \bar "|"  b4 r4 r4 \bar "|"  r4 r4 r4 \bar "|"  
 
}


text = \lyricmode {
i- rene good night 
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