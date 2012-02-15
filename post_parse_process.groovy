data_dir = new File('data')

scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev.txt')

plain_text_file = new File(data_dir, 'dev_sentences.txt')

pet_dir = new File(data_dir, "erg.1111.${plain_text_file.name - /.txt/}.12-02-12.pet")

println pet_dir.name

println pet_dir.listFiles().sort { it.name as Integer }

headline_pattern = /^\[(\d+)\]\s+\((\d+)\s+of\s+(\d+)\)\s+\{(\d+)\}\s+`(.*)'$/

assert ((/[1] (1 of 1) {1} `1 . The Singular Experience of Mr . John Scott Eccles'/ =~ headline_pattern).matches())

dmrs_first_line_pattern = /\s*<\s*[dD][mM][rR][sS].*$/

assert ((/<dmrs cfrom='-1' cto='-1'>/ =~ dmrs_first_line_pattern).matches())

report_file = new File('output.html')

report_file.withPrintWriter { printer ->
    new groovy.xml.MarkupBuilder(printer).html {
        body {
            dev_file.withReader { reader ->
                def delimitedReader = new BlankLineTerminatedReader(reader)

                def INTERWORDSEP = ' '

                def sentence_count = 0
                def negation_count = 0
                def missing_export_file_count = 0

                while (delimitedReader.next()) {
                    List<String> lines = delimitedReader.readLines()
                    List<Map> words = lines.collect {
                        def columns = it.split(/\t/)
                        def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns
                        def labels = columns[7..-1]
                        [chap_name:chap_name, sent_indx:sent_indx as Integer, tok_indx:tok_indx as Integer, word:word, lemma:lemma, pos:pos, syntax:syntax, labels:labels]
                    }
                    
                    def plain_text = words.collect { it.word }.join(INTERWORDSEP)

                    words.inject(0) { i, word -> word.cfrom = i ; word.cto = i + word.word.length() ; word.cto + INTERWORDSEP.length() }

                    // Zero-based sentence index used by CoNLL.
                    def sentence_index = (words[0].sent_indx as Integer)

                    // One-based sentence number used by PET.
                    def sentence_number = ++sentence_count

                    p "$sentence_number : $plain_text"

                    def export_file = new File(pet_dir, sentence_number as String)

                    if (!export_file.exists()) {
                        p "No file exported for sentence # ${sentence_number}"
                        ++missing_export_file_count
                        continue
                    }

                    // Any negation labels?
                    if (words[0].labels.size() < 3) continue

                    ++negation_count

                    def pet_plain_text = get_plain_text_from_export_file(export_file)

                    if (plain_text != pet_plain_text) {
                        p "Mismatch btw plain text source and pet's version for sentence # ${sentence_number}:"
                        p pet_plain_text
                    }

                    def dmrs = get_dmrs_from_export_file(export_file)

                    def node_map = [:]

                    // Can't use collectEntries with XmlSlurper apparently (Groovy 1.8.6).
                    dmrs.node.list().each {
                        def nodeprops = new Expando(id:it.@nodeid.toString().intern(), cfrom:(it.@cfrom).toString() as Integer, cto:(it.@cto.toString()) as Integer) ///, links:[:], revlinks:[:])

//                        node_map['toString'] = { it.id }

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

                    table(border:1) {
                        tr { words.each { td(it.word) } }
                        tr { words.each { td(it.lemma) } }
                        tr { words.each { td(it.pos) } }
                        tr { words.each { word -> td(valign:'top') { word.labels.each { p(it) } } } }
                        def word_nodes = node_map.values().groupBy { it.word?.tok_indx }
//                        tr { words.size().times { i -> td(word_nodes[i]?.realpred?.grep { it }?.join(' ') ?: '') } }
//                        tr { words.size().times { i -> td(word_nodes[i]?.gpred?.grep { it }?.join(' ') ?: '') } }
                        tr {
                            words.size().times { i ->
                                td(valign:'top') {
                                    (word_nodes[i]?.realpred).each { if (it) p(it.toString()) }
                                    (word_nodes[i]?.gpred).each { if (it) p(it) }
                                }
                            }
                        }
                    }

                }

                h2 "Summary"
                p "Input had ${sentence_count} sentences of which ${missing_export_file_count} (${missing_export_file_count/sentence_count}) had no parse and ${negation_count} were marked as having negation."
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
