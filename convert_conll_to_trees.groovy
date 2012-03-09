
data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-22022012')

dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-22022012.txt')

//dev_file = new File('docs/annotated.txt')

// mallet import-file --input data/train.mallet.txt --output data/train.vectors
// mallet train-classifier --input data/train.vectors --training-portion 0.9 --trainer MaxEnt

out_file = new File(data_dir, 'train.trees.txt')

def neg_only = true

def affixes = ['un', 'im', 'dis', 'ir', 'in']

out_file.withWriter { printer ->

    dev_file.withReader { reader ->
        def delimitedReader = new BlankLineTerminatedReader(reader)

        while (delimitedReader.next()) {
            // Can't use this because DGM.getFile(BufferedReader) uses read not readLine.
            // println delimitedReader.text
            // But readLines does.
            List<String> lines = delimitedReader.readLines()

            def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

            def negated_scope_count = (tokens[0].labels.size() / 3) as Integer

            negated_scope_count.times { scope_i ->
                def sexp = new StringBuilder(tokens.size() * 20)

                tokens.each { token ->
                    String id = token.with { chap_name + '/' + sent_indx + '/' + scope_i + '/' + tok_indx }

                    def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                    token.with {
                        sexp.append(syntax.replace('*', " (token $id ${sexp_escape(pos)} ${sexp_escape(word)} ${sexp_escape(lemma)} ${scope_labels[0]} ${scope_labels[1] == '_' ? '-' : '+'} ${scope_labels[2] == '_' ? '-' : '+'} $tok_indx ) "))
                    }
                    
//                    def cue_label = null
//                    if (negation_labels.size()) {
//                        cue_label = negation_labels[0].find { it != '_' & it != '***' }
//                    }
//
//                    def features = [:]
//
//                    features['word_' + token.word.toLowerCase()] = 1
//                    features['lemma_' + token.lemma] = 1
//                    features['pos_' + token.pos] = 1
//
//                    affixes.each { if (token.word.startsWith(it) && (token.word.length() > it.length())) features['affix_' + it] = 1 }
//
//                    if (cue_label || !neg_only) {
//                        printer.println id + '\t' + (cue_label ? cue_label.toLowerCase() : '_pos_') + '\t' + features.collect { k, v -> k + '\t' + v }.join('\t')
//                    }
                }

                printer.println sexp
            }
        }
    }

}

def sexp_escape(String s)
{
    s = s.replace("(", "-LRB-")
    s = s.replace(")", "-RRB-")
    s
}
