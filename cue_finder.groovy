#!/usr/bin/env groovy

data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

//train_file = new File(data_dir, 'sample.train.gappy.txt')
train_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

test_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-test-16032012')

test1_file = new File(test_dir, 'SEM-2012-SharedTask-CD-SCO-test-cardboard.txt')
test2_file = new File(test_dir, 'SEM-2012-SharedTask-CD-SCO-test-circle.txt')

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

finder = new CueFinder()

finder.add_to_lexicon(train_file)
finder.add_to_lexicon(dev_file)
//finder.add_to_lexicon(test1_file)
//finder.add_to_lexicon(test2_file)

//(finder.lexicon.keySet() as List).sort().each { println it }

//println finder.lexicon["burned"]
println finder.lexicon.size()

finder.train(instances)

AffixRule.suffixes.each { print it ; print ' ' } ; println()

if (false) {

    finder.rules.values().each { println() ; println it }

    instances.each { instance ->
        def matches = finder.rules.values().collectMany { rule -> rule.match(instance.tokens) } as Set<Cue>

        def multiword_indicies = matches.collectMany { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) ? cue.token_indicies : [] }

        if (multiword_indicies) {
            matches = matches.grep { Cue cue -> (cue.type == Cue.CueType.MULTIWORD_CONTIGUOUS) || !multiword_indicies.intersect(cue.token_indicies) }
        }

    //    if (!(matches == instance.gold) || (instance.gold.grep { it.type == Cue.CueType.MULTIWORD_CONTIGUOUS })) {
//        if (!(matches == instance.gold)) {
        if (!(matches.containsAll(instance.gold))) {
            println matches
            println()
            println instance.gold
            println()
            println instance.tokens

            println('---')
        }
    }

}

//finder.find_cues_to_conll(train_file, sys_train_cues_conll_file)
//finder.find_cues_to_conll(dev_file, sys_dev_cues_conll_file)

finder.find_cues_to_conll(dev_file, dev_cue_output)
finder.find_cues_to_conll(train_file, train_cue_output)
