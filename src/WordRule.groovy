import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class WordRule extends Rule
{
//    {
//        type = Cue.CueType.WORD
//    }
    
    String word
    String pos

    WordRule(List<Map> tokens, Cue cue)
    {
        word = cue.cue[0]
        pos = cue.pos[0]
    }

    @Override
    List<Cue> match(List<Map> tokens)
    {
        tokens.collectMany { ((pos == it.pos) && word.equalsIgnoreCase(it.word)) ? [new Cue(Cue.CueType.WORD, [it.tok_indx], [word], [pos])] : [] }
    }
}
