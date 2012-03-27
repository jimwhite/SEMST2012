class CueFinder
{
    Map<Rule, Rule> rules = [:]
    
    static Map<String, Integer> lexicon = [:].withDefault { 0 }
//    static Map<String, Integer> suffixes = [:].withDefault { 0 }

    def train(List<Map> instances)
    {
        instances.each { instance ->
            instance.gold.each { cue ->
                def rule = Rule.ruleForCue(instance.tokens, cue)
                if (rule instanceof AffixRule && rules.containsKey(rule)) {
                    rules[rule].addPositive(instance.tokens, cue)
                } else {
                    rules[rule] = rule
                }
            }
        }

        println instances.size()
        println rules.size()

        instances.each { instance ->
            def tokens = instance.tokens
            if (!tokens) println instance
            def matches = rules.values().collectMany { it.match(tokens) }

            matches.each { Cue cue ->
                if ((cue.type == Cue.CueType.AFFIX) && !(instance.gold.contains(cue))) {
//            println "Negative case ${cue}"
                    rules[Rule.ruleForCue(instance.tokens, cue)].addNegative(instance.tokens, cue)
                }
            }
        }
        
//        rules.each { rule -> if (rule instanceof  AffixRule) rule.lexicon = lexicon }
    }
    
    def add_to_lexicon(File conll_file)
    {
        conll_file.withReader { reader ->
            def delimitedReader = new BlankLineTerminatedReader(reader)

            while (delimitedReader.next()) {
                List<String> lines = delimitedReader.readLines()

                def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

                tokens.each { Map token ->
//                    def word = (token.word.toLowerCase()) + ':' + token.pos[0]
                    String word = (token.word.toLowerCase())
                    lexicon[word] = lexicon[word] + 1
//                    def lemma = (token.lemma.toLowerCase()) + ':' + token.pos[0]
                    String lemma = (token.lemma.toLowerCase())
                    lexicon[word] = lexicon[lemma] + 1

//                    if (word != lemma && word.contains(lemma)) {
//                        def suffix = word.substring(word.indexOf(lemma) + lemma.length())
//                        if (suffix) {
//                            suffixes[suffix] = suffixes[suffix] + 1
//                        }
//                    }
                }
            }
        }
    }

    def find_cues_to_conll(File infile, File outfile)
    {
        outfile.withPrintWriter { printer ->
            infile.withReader { reader ->
                def delimitedReader = new BlankLineTerminatedReader(reader)
                while (delimitedReader.next()) {
                    List<String> lines = delimitedReader.readLines()

                    def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

                    def matches = rules.values().collectMany { rule -> rule.match(tokens) } as Set<Cue>

                    def multiword_indicies = matches.collectMany { Cue cue -> cue.isMultiword() ? cue.token_indicies : [] }

                    if (multiword_indicies) {
                        matches = matches.grep { Cue cue -> cue.isMultiword() || !multiword_indicies.intersect(cue.token_indicies) }
                    }

                    tokens.eachWithIndex { token, token_i ->
                        token.with { printer.print ([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }

                        if (matches) {
                            matches.each { Cue cue ->
                                printer.print '\t'
                                if (token_i in cue.token_indicies) {
                                    printer.print([cue.conll_cue_value(token_i), cue.conll_scope_value(token_i), '_'].join('\t'))
                                } else {
                                    printer.print(['_', '_', '_'].join('\t'))
                                }
                            }
                            printer.println()
                        }  else {
                            printer.println('\t***')
                        }
                    }
                    
                    printer.println()
                }
            }
        }
    }


}
