#!/usr/bin/env groovy

in_file = new File(args[0])
out_file = new File(args[1])

out_file.withWriter { printer ->
    in_file.withReader { reader ->

        def delimitedReader = new BlankLineTerminatedReader(reader)

        while (delimitedReader.next()) {
            List<String> lines = delimitedReader.readLines()

            def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

            def negated_scope_count = (tokens[0].labels.size() / 3) as Integer

            def first_cues = negated_scope_count ? (0..<negated_scope_count).collect { cues_for_scope(tokens, it).findIndexOf { it != '_' } } : []
            
            def scope_map = negated_scope_count ? [*0..<negated_scope_count].sort { first_cues[it] } : []

            tokens.eachWithIndex { token, token_i ->
                token.with { printer.print([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }

                if (negated_scope_count) {
                    negated_scope_count.times { i ->
                        def scope_i = scope_map[i]
                        def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                        printer.print '\t'
                        printer.print scope_labels.join('\t')
                    }
                    printer.println()
                } else {
                    printer.println "\t***"
                }
            }

            printer.println()
        }
    }
}

List<String> cues_for_scope(List<Map> tokens, scope_i)
{
    tokens.collect { token ->
        def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]
        scope_labels[0]
    }
}
