import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class GappyWordsRule extends Rule
{
    List<String> pos
    List<String> words

    GappyWordsRule(List<Map> tokens, Cue cue)
    {
        pos = cue.pos
        words = cue.token_indicies.collect { tokens[it].word.toLowerCase() }
    }

    @Override
    List<Cue> match(List<Map> tokens)
    {
        def matches = []

        // This isn't gonna work for us...
        if (words.size() == 2 && (words[0].equalsIgnoreCase(words[1]))) return matches

        def word_i = 0
        def indicies = []

        for (int i = 0; i < tokens.size(); ++i) {
            if ((pos[word_i] == tokens[i].pos) && (words[word_i].equalsIgnoreCase(tokens[i].word))) {
                indicies << i
                if (++word_i == words.size()) {
                    def cue_words = indicies.collect { tokens[it].word }
                    matches.add(new Cue(Cue.CueType.MULTIWORD_GAPPY, indicies, cue_words, pos))
                    word_i = 0
                    indicies = []
                }
            }
        }

        matches
    }
}
