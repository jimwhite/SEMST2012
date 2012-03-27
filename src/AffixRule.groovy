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
    
//    Map lexicon = [:]
    static Map<String, Integer> suffixes = [:].withDefault { 0 }

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
        tokens.collectMany { Map token ->
            if (token.pos[0] == pos[0]) {
                def m = pattern.matcher(token.word)
                if (m.matches()) {
//                        if (gold_positive.contains(token.word.toLowerCase()) || lexicon.containsKey(word_root.toLowerCase() + ':' + token.pos[0])) {
//                        if (gold_positive.contains(token.word.toLowerCase()) || lexicon.containsKey(word_root.toLowerCase())) {
                    def word_root = m.group(prefix ? 2 : 1)
                    def word_affix = m.group(prefix ? 1 : 2)
                    if (!gold_negative.contains(token.word.toLowerCase()) && (gold_positive.contains(token.word.toLowerCase()) || gold_positive.contains(token.word.toLowerCase() - ~/ly/) || ok_root(word_root))) {
//                        if (lexicon.containsKey(word_root.toLowerCase())) {
//                            println "${token.word} $word_affix $word_root"
                        [new Cue(Cue.CueType.AFFIX, [token.tok_indx], [word_affix], [pos], [word_root])]
                    } else {
//                            println "${token.word} $word_affix $word_root"
                        []
                    }
                } else {
                    []
                }
            } else {
                []
            }
        }
    }

    def ok_root(String root)
    {
//        println root
//        (root.length() > 3) && !(root.contains("-"))
//        false
        
//        if (root.startsWith("burn")) { println root ; println CueFinder.lexicon[root] ; println lexicon.size()}
//        ((root.length() > 3) && CueFinder.lexicon.containsKey(root.toLowerCase()))
        root = root.toLowerCase()
//        !gold_negative.contains(root) && CueFinder.lexicon.containsKey(root)
//        (pos[0][0] == 'J' || pos[0][0] == 'R') && (CueFinder.lexicon.containsKey(root) || (prefix && suffixes.any { CueFinder.lexicon.containsKey(root + it) }))
//        (pos[0][0] == 'J') && (CueFinder.lexicon.containsKey(root) || (prefix && suffixes.any { CueFinder.lexicon.containsKey(root + it) }))
        (pos[0][0] == 'J') && (CueFinder.lexicon.containsKey(root) || (prefix && suffixes.grep { it.value > 2 }.any { CueFinder.lexicon.containsKey(root + it.key) }))
    }

    void addPositive(List<Map> tokens, Cue cue)
    {
        String word = tokens[cue.token_indicies[0]].word.toLowerCase()
        
//        if (!word in gold_positive) {
        if (!gold_positive.contains(word)) {
            if (prefix) {
                def m = pattern.matcher(word)
                if (m.matches()) {
                    def negated = m.group(2)
                    CueFinder.lexicon.each { w, k ->
                        if (w.length() > 3 && negated.startsWith(w) && (negated.length() > w.length() + 1)) {
                            def suffix = negated.substring(w.length())
//                            println "$suffix $word $negated $w"
                            suffixes[suffix] = suffixes[suffix] + 1
                        }
                    }
                } else {
                    println word
                    println this
                }
            }

            gold_positive.add(word)

            if (gold_negative.contains(word)) { gold_negative.remove(word) }
        }
    }

    void addNegative(List<Map> tokens, Cue cue)
    {
        def word = tokens[cue.token_indicies[0]].word.toLowerCase()
        if (!gold_positive.contains(word)) { gold_negative.add(word) }
    }
}
