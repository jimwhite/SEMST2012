import groovy.transform.EqualsAndHashCode
import java.util.regex.Pattern
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class ContiguousWordsRule extends Rule
{
    List<String> pos
    List<String> words
    
    ContiguousWordsRule(List<Map> tokens, Cue cue)
    {
        pos = cue.pos
        words = cue.token_indicies.collect { tokens[it].word.toLowerCase() }
    }

    @Override
    List<Cue> match(List<Map> tokens)
    {
        def matches = []
        
        def word_i = 0
        
        for (int i = 0; i < tokens.size(); ++i) {
            if ((pos[word_i] == tokens[i].pos) && (words[word_i].equalsIgnoreCase(tokens[i].word))) {
                if (++word_i == words.size()) {
                    matches.add(new Cue(Cue.CueType.MULTIWORD_CONTIGUOUS, [*((i-(word_i-1))..(i))], words, pos))
                    word_i = 0
                }
            } else {
                word_i = 0
            }
        }

        matches
    }
}
