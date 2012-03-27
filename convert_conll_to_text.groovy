
data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-22022012')

dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-22022012.txt')

//dev_file = new File('docs/annotated.txt')

dev_file.withReader { reader ->
    def delimitedReader = new BlankLineTerminatedReader(reader)
    
    while (delimitedReader.next()) {
        // Can't use this because DGM.getFile(BufferedReader) uses read not readLine.
        // println delimitedReader.text
        // But readLines does.
        List<String> lines = delimitedReader.readLines()

        println CoNLLDecode.tokens_to_text(CoNLLDecode.decode_lines_to_tokens(lines))
    }
}
