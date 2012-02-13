
data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO')

dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev.txt')

//dev_file = new File('docs/annotated.txt')

dev_file.withReader { reader ->
    def delimitedReader = new BlankLineTerminatedReader(reader)
    
    while (delimitedReader.next()) {
        // Can't use this because DGM.getFile(BufferedReader) uses read not readLine.
        // println delimitedReader.text
        // But readLines does.
        List<String> lines = delimitedReader.readLines()
        // println lines

//        def sexp_str = columns2sexpstring(lines)
//        println sexp_str

        println column2string(lines, 3)
    }
}

String column2string(List<String> lines, int col, joiner = ' ')
{
    lines.collect { it.split(/\s+/)[col] }.join(joiner)
}

String columns2sexpstring(List<String> lines)
{
    def b = new StringBuilder(lines.size() * 20)
    
    lines.each { col4word ->
        def fields = col4word.split(/\s+/)
        def (chap, line, i, word, lemma, pos, tree) = fields
        b.append(tree.replace('*', " ($pos ${(word == lemma) ? word : "($word $lemma)"}) "))
        if (word.contains(')') || word.contains(')')) println word
        if (pos.contains(')') || pos.contains(')')) println pos
    }

    b
}
