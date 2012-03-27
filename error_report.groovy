#!/usr/bin/env groovy

gold_file = new File(args[0])
sys_file = new File(args[1])

new File('data/error_report.html').withWriter {
    new groovy.xml.MarkupBuilder(it).html {
        body {
            gold_file.withReader { gold_reader ->
                sys_file.withReader { sys_reader ->
                    def gold_dr = new BlankLineTerminatedReader(gold_reader)
                    def sys_dr = new BlankLineTerminatedReader(sys_reader)

                    while (gold_dr.next()) {
                        def gold_tokens = CoNLLDecode.decode_lines_to_tokens(gold_dr.readLines())
                        if (!sys_dr.next()) {
                            println "Premature end of sys"
                            break
                        }
                        def sys_tokens = CoNLLDecode.decode_lines_to_tokens(sys_dr.readLines())

                        def gold_negated_scope_count = (gold_tokens[0].labels.size() / 3) as Integer
                        def sys_negated_scope_count = (sys_tokens[0].labels.size() / 3) as Integer

//                        if (gold_tokens.find { it.labels.collate(3).find { it[2] != '_' } } )
//                        if (gold_tokens.collect { it.labels.collate(3).collect { it[0]} } != sys_tokens.collect { it.labels.collate(3).collect { it[0]} })
                        if (gold_tokens.collect { it.labels.collate(3).collect { it[2]} } != sys_tokens.collect { it.labels.collate(3).collect { it[2]} })

                        if (gold_negated_scope_count || sys_negated_scope_count) {
                            table(border: 1) {
                                tr { gold_tokens.each { td(it.word) } }
                                tr { gold_tokens.each { td(it.lemma) } }
                                tr { gold_tokens.each { td(it.pos) } }
                                tr()
                                tr { td "gold" }
                                gold_negated_scope_count.times { scope_i ->
//                                    def labels = gold_tokens.collect { it.labels[(scope_i * 3)..<((scope_i + 1) * 3)] }
//                                    tr { labels.each { label -> td(valign: 'top') { label.each { p(it) } } } }
                                    tr { gold_tokens.each { token -> td(valign: 'top') { token.labels[(scope_i * 3)..<((scope_i + 1) * 3)].each { p(it) } } } }
                                }
                                tr()
                                tr { td "sys" }
                                sys_negated_scope_count.times { scope_i ->
                                    tr { sys_tokens.each { token -> td(valign: 'top') { token.labels[(scope_i * 3)..<((scope_i + 1) * 3)].each { p(it) } } } }
                                }
                            }
                            br()
                        }

//                tokens.eachWithIndex { token, token_i ->
//                    token.with { printer.print([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }
//
//                    if (negated_scope_count) {
//                        negated_scope_count.times { i ->
//                            def scope_i = scope_map[i]
//                            def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]
//
//                            printer.print '\t'
//                            printer.print scope_labels.join('\t')
//                        }
//                        printer.println()
//                    } else {
//                        printer.println "\t***"
//                    }
//                }

                    }
                }
            }
        }
    }
}