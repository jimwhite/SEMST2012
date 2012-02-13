
data_dir = new File('data')

scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev.txt')

plain_text_file = new File(data_dir, 'dev_sentences.txt')

pet_dir = new File(data_dir, "erg.1111.${plain_text_file.name - /.txt/}.12-02-12.pet")

println pet_dir.name

println pet_dir.listFiles().sort { it.name as Integer }

headline_pattern = /^\[(\d+)\]\s+\((\d+)\s+of\s+(\d+)\)\s+\{(\d+)\}\s+`(.*)'$/

assert ((/[1] (1 of 1) {1} `1 . The Singular Experience of Mr . John Scott Eccles'/ =~ headline_pattern).matches())


dev_file.withReader { reader ->
    def delimitedReader = new BlankLineTerminatedReader(reader)
    
    def sentence_counter = 0
    def missing_export_file_count = 0

    while (delimitedReader.next()) {
        List<String> lines = delimitedReader.readLines()
        List<String[]> words = lines.collect { it.split(/\t/) }

        // Zero-based sentence index used by CoNLL.
        def sentence_index = (words[0][1] as Integer)

        // One-based sentence number used by PET.
        def sentence_number = ++sentence_counter
//        def sentence_number = sentence_index + 1

        def plain_text = column2string(words, 3)

//        def sexp_str = columns2sexpstring(words)
//        println sexp_str


        def export_file = new File(pet_dir, sentence_number as String)

        if (!export_file.exists()) {
//            println "Sentence ${sentence_index}: ${plain_text}"
//            println "No file exported for sentence ${sentence_index}: ${plain_text}"
            ++missing_export_file_count
            continue
        }
        
        def pet_plain_text = get_plain_text_from_export_file(export_file)
        
        if (plain_text != pet_plain_text) {
            println "Sentence ${sentence_index}: ${plain_text}"
            println "Mismatch btw plain text source and pet's version for sentence ${sentence_index}: ${pet_plain_text}"
        }

//        def dmrs = get_dmrs_from_export_file(export_file)

    }
    
    println "Input had ${sentence_counter} sentences of which ${missing_export_file_count} (${missing_export_file_count/sentence_counter}) had no parse."
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

String column2string(List<String[]> words, int col, joiner = ' ')
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
