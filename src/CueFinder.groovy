class CueFinder
{
    Map<Rule, Rule> rules = [:]

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
            def matches = rules.values().collectMany { rule -> rule.match(instance.tokens) }

            matches.each { Cue cue ->
                if ((cue.type == Cue.CueType.AFFIX) && !(instance.gold.contains(cue))) {
//            println "Negative case ${cue}"
                    rules[Rule.ruleForCue(instance.tokens, cue)].addNegative(instance.tokens, cue)
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

                    def multiword_indicies = matches.collectMany { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) ? cue.token_indicies : [] }

                    if (multiword_indicies) {
                        matches = matches.grep { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) || !multiword_indicies.intersect(cue.token_indicies) }
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
