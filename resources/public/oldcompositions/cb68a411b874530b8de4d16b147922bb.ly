#(ly:set-option 'midi-extension "mid")
\version "2.12.3"
\include "english.ly"
\header{
title = "John's tune"

}
%{

%}
melody = {
\time 3/4
\clef treble
\key c \phrygian
\cadenzaOn


 \times 2/3{ c'4[ e'8] }  f'8[ g'8] \break

}


text = \lyricmode {
    
}

\score{
\transpose c' fs'
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