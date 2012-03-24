data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

train_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

train_cues_file = new File(data_dir, 'train.cues.txt')
dev_cues_file = new File(data_dir, 'dev.cues.txt')

cue_classifier = new File(data_dir, 'cue.classifier')

train_cue_output = new File(data_dir, 'output.train.cue.txt')
dev_cue_output = new File(data_dir, 'output.dev.cue.txt')

instances = []

train_file.withReader { reader ->
    def delimitedReader = new BlankLineTerminatedReader(reader)

    while (delimitedReader.next()) {
        List<String> lines = delimitedReader.readLines()

        def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

//        Set<Cue> cues = [] as TreeSet
        Set<Cue> cues = []

        def negated_scope_count = (tokens[0].labels.size() / 3) as Integer

        negated_scope_count.times { scope_i ->
            def cue_tokens = []
            tokens.each { token ->
                def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                if (scope_labels[0] != '_') {
                    cue_tokens.add([token, scope_labels[0]])
                }
            }

            cues.add(new Cue(cue_tokens))
        }
        
        instances.add([tokens:tokens, gold:cues, sys:[]])
    }
}

//instances.each { if (it.gold.type.contains(Cue.CueType.MULTIWORD_CONTIGUOUS)) { println it.gold ; println it.tokens; println() } }
//
//println()
//
//instances.each { if (it.gold.type.contains(Cue.CueType.MULTIWORD_GAPPY)) { println it.gold ; println it.tokens; println() } }

Map<Rule, Rule> rules = [:]

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


rules.values().each { println() ; println it }

instances.each { instance ->
    def matches = rules.values().collectMany { rule -> rule.match(instance.tokens) } as Set<Cue>
    
    def multiword_indicies = matches.collectMany { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) ? cue.token_indicies : [] }

    if (multiword_indicies) {
        matches = matches.grep { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) || !multiword_indicies.intersect(cue.token_indicies) }
    }

//    if (!(matches == instance.gold) || (instance.gold.grep { it.type == Cue.CueType.MULTIWORD_CONTIGUOUS })) {
    if (!(matches == instance.gold)) {
        println matches
        println()
        println instance.gold
        println()
        println instance.tokens

        println('---')
    }
}

