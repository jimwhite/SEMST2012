import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph

data_dir = new File('data')

scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-22022012')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-22022012.txt')

plain_text_file = new File(data_dir, 'training_sentences.txt')

pet_dir = new File(data_dir, "erg.1111.training_sentences.12-02-26.pet")

//pet_dir = new File(data_dir, "erg.1111.${plain_text_file.name - /.txt/}.12-02-26.pet")
//println pet_dir.name

//println pet_dir.listFiles().sort { it.name as Integer }

headline_pattern = /^\[(\d+)\]\s+\((\d+)\s+of\s+(\d+)\)\s+\{(\d+)\}\s+`(.*)'$/

assert ((/[1] (1 of 1) {1} `1 . The Singular Experience of Mr . John Scott Eccles'/ =~ headline_pattern).matches())

dmrs_first_line_pattern = /\s*<\s*[dD][mM][rR][sS].*$/

assert ((/<dmrs cfrom='-1' cto='-1'>/ =~ dmrs_first_line_pattern).matches())

report_file = new File("output_${plain_text_file.name - /.txt/}.html")

report_file.withPrintWriter { printer ->
    new groovy.xml.MarkupBuilder(printer).html {
        body {
            h1 pet_dir.name
            dev_file.withReader { reader ->
                def delimitedReader = new BlankLineTerminatedReader(reader)

                def INTERWORDSEP = ' '

                def sentence_count = 0
                def negated_sentence_count = 0
                def missing_export_file_count = 0
                
                def token_inventory = [:].withDefault { [] }

                def realpred_to_negation_cues = [:].withDefault { [] }
                def gpred_to_negation_cues = [:].withDefault { [] }
                def negation_cues = [:].withDefault { [count:0, realpreds:[:].withDefault { 0 }, gpreds:[:].withDefault { 0 }]}

                while (delimitedReader.next()) {
                    List<String> lines = delimitedReader.readLines()
                    List<Map> words = lines.collect {
                        def columns = it.split(/\t/)
                        def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns
                        def labels = columns[7..-1].collect { it.trim() }
                        [chap_name:chap_name, sent_indx:sent_indx as Integer, tok_indx:tok_indx as Integer, word:word, lemma:lemma, pos:pos, syntax:syntax, labels:labels]
                    }

                    def negated_scope_count = (words[0].labels.size() / 3) as Integer
                    
                    def plain_text = words.collect { it.word }.join(INTERWORDSEP)

                    words.inject(0) { i, word -> word.cfrom = i ; word.cto = i + word.word.length() ; word.cto + INTERWORDSEP.length() }
                    
                    // Zero-based sentence index used by CoNLL.
                    def sentence_index = (words[0].sent_indx as Integer)

                    // One-based sentence number used by PET.
                    def sentence_number = ++sentence_count

                    p "${words[0].chap_name} s${words[0].sent_indx} (#$sentence_number) : $plain_text"

                    def export_file = new File(pet_dir, sentence_number as String)

                    if (!export_file.exists()) {
                        p "No file exported for sentence # ${sentence_number}${negated_scope_count ? " with $negated_scope_count negations" : ''}"
                        ++missing_export_file_count
                        continue
                    }

                    // Any negation labels?
                    if (negated_scope_count < 1) continue

                    ++negated_sentence_count

                    def pet_plain_text = get_plain_text_from_export_file(export_file)

                    if (plain_text != pet_plain_text) {
                        p "Mismatch btw plain text source and pet's version for sentence # ${sentence_number}:"
                        p pet_plain_text
                    }

                    def dmrs = get_dmrs_from_export_file(export_file)

                    def node_map = [:]

                    def tg = new TinkerGraph()

                    // Can't use collectEntries with XmlSlurper apparently (Groovy 1.8.6).
                    dmrs.node.list().each {
                        def nodeprops = new Expando(id:it.@nodeid.toString().intern(), cfrom:(it.@cfrom).toString() as Integer, cto:(it.@cto.toString()) as Integer) ///, links:[:], revlinks:[:])

                        def v = tg.addVertex(it.@nodeid)
                        v.setProperty('cfrom', (it.@cfrom).toString() as Integer)
                        v.setProperty('cto', (it.@cto).toString() as Integer)

                        nodeprops.words = words.findAll { it.cfrom >= nodeprops.cfrom && it.cto <= nodeprops.cto }

                        nodeprops.word = nodeprops.words.find { it.cfrom == nodeprops.cfrom && it.cto == nodeprops.cto }

                        def realpred = it.realpred
                        if (realpred.size()) {
                            nodeprops.realpred = [lemma: realpred.@lemma.toString() ?: null, pos: realpred.@pos.toString() ?: null]
                            def sense = realpred.@sense.toString()
                            if (sense) nodeprops.realpred.sense = sense
                        }

                        def gpred = it.'gpred'
                        if (gpred.size()) {
                            nodeprops.gpred = gpred.text()
                        }

                        node_map[nodeprops.id] = nodeprops
                    }

                    def links = dmrs.link.list().collect {
                        def fromid = it.@from.toString()
                        def toid = it.@to.toString()
                        def rargname = it.rargname.text()
                        def post = it.post.text()

                        [rargname:rargname, post:post, from:node_map[fromid], to:node_map[toid]]
                    }

                    negated_scope_count.times { negation_indx ->
                        // Group nodes according to the index of their first token.
                        def tok_indx_to_nodes = node_map.values().groupBy { it.words ? it.words[0].tok_indx : -1}
                        
                        if (tok_indx_to_nodes[-1]) {
                            println "Nodes with no token:"
                            tok_indx_to_nodes[-1].each { println it }
                            tok_indx_to_nodes.remove(-1)
                        }
                        
                        // Group the nodes for each token according to the number of tokens spanned.
                        def tok_indx_to_node_map = (Map<Integer, List>) tok_indx_to_nodes.collectEntries { x, nodes -> [x, nodes.groupBy { it.words.size() }]}

                        def negation_cue = words.findAll { it.labels[negation_indx * 3] != '_' }
//                        if (negation_cue.size() == 1) negation_cue = negation_cue[0]

                        if (negation_cue) {
                            def cue_key = negation_cue.collect { it.word.toLowerCase() }
                            def stats = negation_cues[cue_key]
                            stats.count = stats.count + 1
                            negation_cue.each { cue ->
                                // Sort the node list so it will (better) match the order displayed.
                                def nodes = tok_indx_to_nodes[cue.tok_indx]
                                if (nodes) {
                                    nodes = nodes.sort { it.words.size() }
                                    (nodes.realpred).each {
                                        realpred_to_negation_cues[it] = realpred_to_negation_cues[it] + [cue_key]
                                        if (it) stats.realpreds[it] = stats.realpreds[it] + 1
                                    }
                                    (nodes.gpred).each {
                                        gpred_to_negation_cues[it] = gpred_to_negation_cues[it] + [cue_key]
                                        if (it) stats.gpreds[it] = stats.gpreds[it] + 1
                                    }
                                }
                            }
//                            negation_cues[cue_key] = stats
                        } else {
                            p { b("MISSING NEGATION CUE") }
                        }

                        table(border:1) {
                            tr { words.each { td(it.word) } }
                            tr { words.each { td(it.lemma) } }
                            tr { words.each { td(it.pos) } }
                            tr { words.each { word -> td(valign:'top') { word.labels[(negation_indx * 3)..<((negation_indx + 1) * 3)].each { p(it) } } } }
//                        tr { words.size().times { i -> td(word_nodes[i]?.realpred?.grep { it }?.join(' ') ?: '') } }
//                        tr { words.size().times { i -> td(word_nodes[i]?.gpred?.grep { it }?.join(' ') ?: '') } }
                            while (tok_indx_to_node_map.size()) {
                                tr {
                                    def col_indx = 0
                                    def span_dx = 0
                                    while (col_indx + span_dx < words.size()) {
                                        def token_node_map = tok_indx_to_node_map[col_indx + span_dx]
                                        if (token_node_map) {
                                            // We'll take the shortest spans first.
                                            def min_span = token_node_map.keySet().min()
                                            def nodes = token_node_map[min_span] as List
                                            token_node_map.remove(min_span)

                                            // If this token has no more nodes, remove it so we'll know when were done.
                                            if (token_node_map.size() < 1) {
                                                tok_indx_to_node_map.remove(col_indx + span_dx)
                                            }

                                            if (span_dx > 0) {
                                                td(colspan:span_dx)
                                                col_indx += span_dx
                                            }

                                            span_dx = nodes[0].words.size()
                                            td(valign:'top', colspan:span_dx) {
                                                nodes.each { node ->
                                                    if (node.realpred) {
                                                        p("${node.id}:${node.realpred}")
                                                    }

                                                    if (node.gpred) {
                                                        p("${node.id}:${node.gpred}")

                                                    }

                                                    def arcs = links.grep { it.from.is (node) }

                                                    if (arcs) {
                                                        ul {
                                                            arcs.each { link -> li "${link.rargname} ${link.post} ${link.to.id}" }
                                                        }
                                                    }
//                                            p(node.id + (node.realpred ? ' ' + node.realpred.toString() : '') + (node.gpred ? ' ' + node.gpred : ''))
//                                            node.realpred.each { if (it) p(it.toString()) }
//                                            node.gpred.each { if (it) p(it) }
                                                }
                                            }
                                            col_indx += span_dx
                                            span_dx = 0
                                        } else {
                                            span_dx += 1
                                        }
                                    }
                                    if (span_dx > 0) {
                                        td(colspan:span_dx)
                                    }
                                }
                            }
                            
//                            def multiword_nodes = (node_map.values() as List).grep { it.words.size() > 1 }.sort { (it.words.token_indx).min() }
//                            multiword_nodes.each { node ->
//                                tr {
//                                    def first_word = node.words[0]
//                                        if (first_word.tok_indx > 0) {
//                                            td(colspan:(first_word.tok_indx))
//                                        }
//                                        td(colspan:node.words.size()) {
//                                            if (node.realpred) {
//                                                p("${node.id}:${node.realpred}")
//                                            }
//
//                                            if (node.gpred) {
//                                                p("${node.id}:${node.gpred}")
//                                            }
//
//                                            def arcs = links.grep { it.from.is (node) }
//
//                                            if (arcs) {
//                                                ul {
//                                                    arcs.each { link -> li "${link.rargname} ${link.post} ${link.to.id}" }
//                                                }
//                                            }
//                                        }
//                                        if (first_word.tok_indx + node.words.size() < words.size()) {
//                                            td(colspan:words.size() - (first_word.tok_indx + node.words.size()))
//                                        }
//                                    }
//                            }
                        }
                    }

                }

                h2 "Summary"
                p "Corpus ${pet_dir.name} had ${sentence_count} sentences of which ${missing_export_file_count} (${missing_export_file_count/sentence_count}) had no parse and ${negated_sentence_count} were marked as having negation."

                h3 "Negation cue to (single token) predicate"
                table(border:1) {
                    negation_cues.sort { -it.value.count }.each {
                        def token = it.key
                        def count = it.value.count
                        def realpreds = it.value.realpreds
                        def gpreds = it.value.gpreds
                        tr {
                            td(token)
                            td(count)
                            td(realpreds.size() ? realpreds.toString() : '')
                            td(gpreds.size() ? gpreds.toString() : '')
                        }
                    }
                }

                h3 "realpred to negation cues"
                table(border:1) {
                    realpred_to_negation_cues.each { pred, tokens ->
                        tr {
                            td(pred.toString())
                            td(tokens.size())
                            td(tokens.countBy { it }.toString())
                        }
                    }
                }

                h3 "gpred to negation cues"
                table(border:1) {
                    gpred_to_negation_cues.each { pred, tokens ->
                        tr {
                            td(pred.toString())
                            td(tokens.size())
                            td(tokens.countBy { it }.toString())
                        }
                    }
                }
                
//                def negation_cue_to_pred = [:].withDefault { [] }
//                realpred_to_negation_cues.each { pred, tokens -> tokens.each { negation_cue_to_pred[it] = negation_cue_to_pred[it] + [pred ?: 'none'] }}
//                gpred_to_negation_cues.each { pred, tokens -> tokens.each { negation_cue_to_pred[it] = negation_cue_to_pred[it] + [pred ?: 'none'] }}

                if (token_inventory.size()) {
                    h3 'Tokens other than /^[-\\p{L}]+$/ || /^\\p{N}+$/'
                    table(border:1) {
//                    token_inventory.entrySet().sort { a, b -> b.value.size() <=> a.value.size() ?: a.key <=> b.key }.each { e ->
                        token_inventory.entrySet().sort { a, b -> a.value.size() <=> b.value.size() ?: a.key <=> b.key }.each { e ->
                            tr {
                                td(align:'center', e.key)
                                td(align:'right', e.value.size())
                                if (e.value.size() > 40) {
                                    td()
                                } else {
                                    td {
                                        e.value.unique().each { p(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



String get_plain_text_from_export_file(File pet_export_file)
{
    def result = null

    pet_export_file.withReader {
        def delimitedReader = new BlankLineTerminatedReader(it)

        while (delimitedReader.hasNext()) {
            delimitedReader.next()
            List<String> lines = delimitedReader.readLines()

            if (lines.size()) {
//                println (lines[0])

                def m = lines[0] =~ headline_pattern

                if (m.matches()) {
                    def (_, pet_sentence_number, parse_i, parse_n, somenumber, pet_plain_text) = m[0]

                    result = pet_plain_text

                    break
                }
            }
        }
    }

    result
}

def get_dmrs_from_export_file(File pet_export_file)
{
    def result = null

    pet_export_file.withReader {
        def delimitedReader = new BlankLineTerminatedReader(it)

        while (delimitedReader.hasNext()) {
            delimitedReader.next()
            List<String> lines = delimitedReader.readLines()

            if (lines.size()) {
//                println (lines[0])

                def m = lines[0] =~ dmrs_first_line_pattern

                if (m.matches()) {
                    def dmrs = new XmlSlurper().parseText(lines.join('\n'))

                    result = dmrs
                    break
                }
            }
        }
    }

    result
}

String column2string(List words, col, joiner = ' ')
{
    words.collect { it[col] }.join(joiner)
}

String columns2sexpstring(List<String[]> words)
{
    def b = new StringBuilder(words.size() * 20)

    words.each { columns ->
        def (chap, line, i, word, lemma, pos, tree) = columns
        b.append(tree.replace('*', " ($pos ${(word == lemma) ? word : "($word $lemma)"}) "))
    }

    b
}
