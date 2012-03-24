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

rules = [] as Set<Rule>

instances.each { instance ->
    instance.gold.each { rules.add(Rule.ruleForCue(instance.tokens, it)) }
}

println instances.size()
println rules.size()

//rules.each { println() ; println it }

instances.each { instance ->
    if (instance.gold) {
        def matches = rules.collectMany { rule -> rule.match(instance.tokens) }
        println matches
        println()
        println instance.gold
        println()
        println instance.tokens
        println('---')
    }
}