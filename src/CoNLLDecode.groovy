
class CoNLLDecode
{
    static def decode_lines_to_tokens(List <String> lines)
    {
        lines.collect {
            def columns = it.split(/\t/)
            def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns
            def labels = columns[7..-1].collect { it.trim() }

            [chap_name:chap_name, sent_indx:sent_indx as Integer, tok_indx:tok_indx as Integer, word:word, lemma:lemma, pos:pos, syntax:syntax, labels:labels]
        }
    }

    static def remove_word_markup(String word)
    {
        // The Gutenberg Project puts underscores around a word for emphasis.
        // I don't require the pairing to be in a single token because *SEM-ST splits up some words.
        while ((word.length() > 1) && word.startsWith('_')) { word = word.substring(1) }
        while ((word.length() > 1) && word.endsWith('_')) { word = word.substring(0, word.length() - 1) }

        word
    }

    static def clitics = ["'s", "n't", "'ve", "'re", "'ll", "'d", "'m"] as Set<String>

    static def tokens_to_text(List tokens)
    {
        tokens.inject(new StringBuilder(tokens.size() << 4)) { StringBuilder sb, token ->
            // Remove any funkiness before we start treating the token like it is a word or part of one.
            def word = remove_word_markup(token.word)

            // If we're not the first word then we'll need a separator.
            if (sb.length()) {
                // If we're a clitic or abbreviation then we don't want a separator.
                sb.append(clitics.contains(word) ? '' : ' ')
            }

            token.cfrom = sb.length()
            sb.append(word)
            token.cto = sb.length()

            sb
        }.toString()
    }

    static String xtokens_to_sexpstring(List tokens)
    {
        def sb = new StringBuilder(tokens.size() * 20)

        tokens.each {
            it.with {
                sb.append(syntax.replace('*', " ($pos ${(word == lemma) ? word : "($word $lemma)"}) "))
            }
        }

        sb.toString()
    }

    static String tokens_to_sexpstring(List tokens)
    {
        def sb = new StringBuilder(tokens.size() * 20)

        tokens.each {
            it.with {
                sb.append(syntax.replace('*', " (token $tok_indx $pos $word $lemma '$labels' ) "))
            }
        }

        sb.toString()
    }

    static convert_to_trees(File dev_file, File out_file)
    {
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
    }

    def sexp_escape(String s)
    {
        s = s.replace("(", "-LRB-")
        s = s.replace(")", "-RRB-")
        s
    }

    def tree_to_mallet(File outfile, File tree_infile)
    {
        outfile.withPrintWriter { printer ->
            tree_infile.withReader { reader ->
                def line = new StringBuilder()

                def cint

                while ((cint = reader.read()) >= 0) {
                    Character c = cint
                    if (c == '(') {
                        def sexp = readSexpList(reader)

                        def cue_path = path_to_cue(sexp)
                        println cue_path
                        if (cue_path) printInstances(sexp, cue_path, [], printer)
        //                writer.println()
                    }
                }
            }
        }
    }

    def printInstances(Object tree, List cue_up_path, List cue_down_path, PrintWriter printer)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                if (tree[5] == '_') {
                    def cue = cue_up_path[0]

                    def up_path = cue_up_path.tail().collect { it[0] }.join('_')
                    def down_path = cue_down_path.collect { it[0] }.join('_')

                    def instance = [tree[1], tree[6]
                            , 'pos_' + tree[2], 1, 'word_' + tree[3], 1, 'lemma_' + tree[4], 1
                            , 'distance', (tree[8] as Integer) - (cue[8] as Integer)  // Good for ~2% acc
                            , 'cue_word_' + cue[3].toLowerCase(), 1, 'cue_lemma_' + cue[4], 1, 'cue_pos_' + cue[2], 1
                            , 'up_' + up_path, 1    // Good for ~6% acc
                            , 'down_' + down_path, 1  // Hurts ~4% when up_ is present.
                    ]
                    printer.println (instance.join('\t'))
                }
            } else {
                if (tree.is(cue_up_path[-1])) {
                    cue_up_path = cue_up_path[0..-2]
                } else {
                    cue_down_path = cue_down_path + [tree]
                }

                tree.each {
                    printInstances(it, cue_up_path, cue_down_path, printer)
                }
            }
        }
    }

    List path_to_cue(Object tree)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                (tree[5] == '_') ? null : [tree]
            } else {
                def path = null
                def parent = tree
                while (tree) {
                    path = path_to_cue(tree.head())
                    if (path) {
                        path.add(parent)
                        break
                    }
                    tree = tree.tail()
                }

                path
            }
        } else {
            null
        }
    }


    List find_cue(Object tree)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                (tree[5] == '_') ? null : tree
            } else {
                def cue = null

                while (tree) {
                    cue = find_cue(tree.head())
                    if (cue) break
                    tree = tree.tail()
                }

                cue
            }
        } else {
            null
        }
    }

    def printTree(Object tree, IndentWriter writer)
    {
        if (tree instanceof List) {
            writer.print "("
            def indent = writer + 1
            def head = tree.head()
            if (head instanceof String) {
                indent.print head + ' '
            } else {
                printTree(head, indent)
            }
            def tail = tree.tail()
            tail.each { if ((head != 'token') && (tail.size() > 1)) indent.println() ; printTree(it, indent) }
            indent.print ")"
        } else {
            writer.print " " + tree
//        writer.print " '$tree'"
        }
    }

    def readSexpList(Reader reader)
    {
        // This grammar has single quotes in token names.
//    final tokenDelimiters = "\"''()\t\r\n "
//    final tokenDelimiters = "\"()\t\r\n "
        // No quoted strings at all for these s-exprs.
        final tokenDelimiters = "()\t\r\n "

        def stack = []
        def sexps = []

        def cint = reader.read()

        loop:
        while (cint >= 0) {
            Character c = cint as Character
            switch (c) {

                case ')' :
                    if (stack.size() < 1) break loop
                    def t = stack.pop()
                    t << sexps
                    sexps = t
                    cint = reader.read()
                    break

                case '(':

                    stack.push(sexps)
                    sexps = []
                    cint = reader.read()
                    break

//            case "'":
//        case '"':
//                def delimiter = c
//                def string = new StringBuilder()
//                string.append(c)
//                while ((c = reader.read()) >= 0) { string.append(c) ; if (c == delimiter) break }
//                sexps << string.toString()
//                cint = reader.read()
//                break

                default:
                    if (c.isWhitespace()) {
                        cint = reader.read()
                    } else {
                        def token = new StringBuilder()
                        token.append(c)
                        while ((cint = reader.read()) >= 0) {
                            if (tokenDelimiters.indexOf(cint) >= 0) break
                            token.append(cint as Character)
                        }
                        sexps << token.toString()
                    }
            }
        }

        return sexps
    }
}
