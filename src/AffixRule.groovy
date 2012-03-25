import groovy.transform.EqualsAndHashCode
import java.util.regex.Pattern
import groovy.transform.ToString

@ToString
@EqualsAndHashCode(includes="cue_regex,pos")
class AffixRule extends Rule
{
    String cue_regex
    String pos
    Boolean prefix
    Pattern pattern
    
    Set<String> gold_positive = []
    Set<String> gold_negative = []

    AffixRule(List<Map> tokens, Cue cue)
    {
        pos = cue.pos[0]
        def cue_str = cue.cue[0].toLowerCase()

        Map token = tokens[cue.token_indicies[0]]
        String word = token.word.toLowerCase()

        gold_positive.add(word)
        
        if (word.startsWith(cue_str)) {
            prefix = true
            cue_regex = '(?i)^(' + Pattern.quote(cue_str) + ')(.+)$'
        } else {
            prefix = false
            def tail_str = word.substring(word.lastIndexOf(cue_str) + cue_str.length())
            cue_regex = '(?i)^(.+)(' + Pattern.quote(cue_str) + ')' + Pattern.quote(tail_str) + '$'
        }

        pattern = ~cue_regex
    }

    @Override
    List<Cue> match(List<Map> tokens)
    {
        tokens.collectMany { token ->
            if (token.pos == pos) {
                def m = pattern.matcher(token.word)
                if (m.matches()) {
                    if (gold_negative.contains(token.word.toLowerCase())) {
                        []
                    } else {
                        [new Cue(Cue.CueType.AFFIX, [token.tok_indx], [m.group(prefix ? 1 : 2)], [pos], [m.group(prefix ? 2 : 1)])]
                    }
                } else {
                    []
                }
            } else {
                []
            }
        }
    }

    void addPositive(List<Map> tokens, Cue cue)
    {
        gold_positive.add(tokens[cue.token_indicies[0]].word.toLowerCase())
    }

    void addNegative(List<Map> tokens, Cue cue)
    {
        def word = tokens[cue.token_indicies[0]].word.toLowerCase()
        if (!gold_positive.contains(word)) { gold_negative.add(word) }
    }
}
