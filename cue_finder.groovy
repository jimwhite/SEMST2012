#!/usr/bin/env groovy

train_file = new File(args[0])
dev_file = new File(args[1])
dev_cue_output = new File(args[2])

data_dir = new File('data')

cue_classifier = new File(data_dir, 'cue.classifier')

train_cue_output = new File(data_dir, 'output.train.cue.txt')
dev_cue_output = new File(data_dir, 'output.dev.cue.txt')

instances = []

train_file.withReader { reader ->
    def delimitedReader = new BlankLineTerminatedReader(reader)

    while (delimitedReader.next()) {
        List<String> lines = delimitedReader.readLines()

        def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

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

finder = new CueFinder()

finder.add_to_lexicon(train_file)

println finder.lexicon.size()

finder.train(instances)

println AffixRule.suffixes.entrySet().join(' ')

if (true) {
//    finder.rules.values().each { println() ; println it }

    instances.each { instance ->
        def matches = finder.rules.values().collectMany { rule -> rule.match(instance.tokens) } as Set<Cue>

        def multiword_indicies = matches.collectMany { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) ? cue.token_indicies : [] }

        if (multiword_indicies) {
            matches = matches.grep { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) || !multiword_indicies.intersect(cue.token_indicies) }
        }

//      if (!(matches == instance.gold)) {
//      if (!(matches == instance.gold) || (instance.gold.grep { it.type == Cue.CueType.MULTIWORD_CONTIGUOUS })) {
        if (!(matches.containsAll(instance.gold))) {
            println()
            println('---')
            println matches
            println()
            println instance.gold
            println()
            instance.tokens.each { println it }
        }
    }

}

finder.find_cues_to_conll(dev_file, dev_cue_output)
