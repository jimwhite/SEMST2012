import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode(includes="type,token_indicies")
class Cue //implements Comparable
{
//    @Override
//    int compareTo(Object o)
//    {
//        if (!(this.equals(o)) && o instanceof Cue) {
//            return ((token_indicies <=> o.token_indicies) ?: type <=> o.type)
//        }
//
//        return 0
//    }

    enum CueType { WORD, AFFIX, MULTIWORD_CONTIGUOUS, MULTIWORD_GAPPY }
    
    CueType type

//    List<Map> tokens
    List<Integer> token_indicies
    List<String> cue
    List<String> pos

    Cue(List token_label_pairs)
    {
//        println token_label_pairs
//        this.tokens = token_label_pairs.collect { (Map) it[0] }
        token_indicies = token_label_pairs.collect { it[0].tok_indx }
        pos = token_label_pairs.collect { it[0].pos }
        cue = token_label_pairs.collect { it[1].toLowerCase() }

        if (token_label_pairs.size() == 1) {
            if (cue[0].equalsIgnoreCase(token_label_pairs[0][0].word)) {
                type = CueType.WORD
            } else {
                type = CueType.AFFIX
            }
        } else {
            token_label_pairs.each {
                if (!(it[0].word.equalsIgnoreCase(it[1]))) println "Bad multiword cue ${token_label_pairs}"
            }
            
//            def contiguous = tokens.tail().inject(tokens[0], { t0, t1 -> (t0 && t0.tok_indx + 1 == t1.tok_indx) ? t1 : null }) != null
            def contiguous = token_label_pairs.tail().inject(token_label_pairs.head()[0], { t0, tp -> (t0 && t0.tok_indx + 1 == tp[0].tok_indx) ? tp[0] : null }) != null

            type = contiguous ? CueType.MULTIWORD_CONTIGUOUS : CueType.MULTIWORD_GAPPY
        }

//        cue = cue*.toLowerCase()
    }
    
    Cue(Cue.CueType type, List<Integer> token_indicies, List<String> cue, List<String> pos)
    {
        this.type = type
        this.token_indicies = token_indicies
        this.cue = cue
        this.pos = pos
    }
    
    String conll_cue_value(Integer token_i)
    {
        cue[token_indicies.indexOf(token_i)]
    }
}
