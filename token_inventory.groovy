data_dir = new File('data')

scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev.txt')
training_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training.txt')

input_file = training_file
input_file = new File(data_dir, 'eval-2012-02-20/wisteria2.all')

input_basename = input_file.name - /.txt/

//plain_text_file = new File(data_dir, 'dev_sentences.txt')
//
//pet_dir = new File(data_dir, "erg.1111.${plain_text_file.name - /.txt/}.12-02-12.pet")
//
//println pet_dir.name

//println pet_dir.listFiles().sort { it.name as Integer }

headline_pattern = /^\[(\d+)\]\s+\((\d+)\s+of\s+(\d+)\)\s+\{(\d+)\}\s+`(.*)'$/

assert ((/[1] (1 of 1) {1} `1 . The Singular Experience of Mr . John Scott Eccles'/ =~ headline_pattern).matches())

dmrs_first_line_pattern = /\s*<\s*[dD][mM][rR][sS].*$/

assert ((/<dmrs cfrom='-1' cto='-1'>/ =~ dmrs_first_line_pattern).matches())

report_file = new File(input_basename + '-tokens.html')

report_file.withPrintWriter { printer ->
    new groovy.xml.MarkupBuilder(printer).html {
        body {
            h1 input_file.name
            input_file.withReader { reader ->
                def delimitedReader = new BlankLineTerminatedReader(reader)

                def INTERWORDSEP = ' '

                def sentence_count = 0
                def negated_sentence_count = 0
                def missing_export_file_count = 0
                
//                def token_inventory = [:].withDefault { 0 }
                def token_inventory = [:].withDefault { [] }

                def realpred_to_negation_cues = [:].withDefault { [] }
                def gpred_to_negation_cues = [:].withDefault { [] }
                def negation_cues = [:].withDefault { [count:0, realpreds:[:].withDefault { 0 }, gpreds:[:].withDefault { 0 }]}

                while (delimitedReader.next()) {
                    List<String> lines = delimitedReader.readLines()
                    List<Map> words = lines.collect {
                        def columns = it.split(/\t/)
                        def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns
                        def labels = columns.size() < 8 ? [] : columns[7..-1].collect { it.trim() }
                        [chap_name:chap_name, sent_indx:sent_indx as Integer, tok_indx:tok_indx as Integer, word:word, lemma:lemma, pos:pos, syntax:syntax, labels:labels]
                    }

                    def negated_scope_count = (words[0].labels.size() / 3) as Integer
                    
                    def plain_text = words.collect { it.word }.join(INTERWORDSEP)

                    words.inject(0) { i, word -> word.cfrom = i ; word.cto = i + word.word.length() ; word.cto + INTERWORDSEP.length() }
                    
                    // Zero-based sentence index used by CoNLL.
                    def sentence_index = (words[0].sent_indx as Integer)

                    // One-based sentence number used by PET.
                    def sentence_number = ++sentence_count

//                    words.each { if (!(it.word =~ /^[-\p{L}]+$/ || it.word =~ /^\p{N}+$/)) token_inventory[it.word] = token_inventory[it.word] + 1 }
                    words.each { if (!(it.word =~ /^[-\p{L}]+$/ || it.word =~ /^\p{N}+$/)) token_inventory[it.word] << "${words[0].chap_name} s${words[0].sent_indx} : $plain_text".toString() }
                }

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
