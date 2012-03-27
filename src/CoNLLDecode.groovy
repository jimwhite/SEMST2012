
public class CoNLLDecode
{
    static List<Map> decode_lines_to_tokens(List <String> lines)
    {
        lines.collect { decode_line_to_token(it) }
    }

static def decode_line_to_token(String line)
{
    def columns = line.split(/\t/)
    def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns
    def labels = (columns.size() > 7) ? columns[7..-1].collect { it.trim() } : []

    [chap_name:chap_name, sent_indx:sent_indx as Integer, tok_indx:tok_indx as Integer, word:word, lemma:lemma, pos:pos, syntax:syntax, labels:labels]
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

    def affixes = ['un', 'im', 'dis', 'ir', 'in', 'less', 'lessly']

def convert_to_cue_data(File conll_infile, File mallet_outfile)
{
    def neg_only = false

    mallet_outfile.withPrintWriter { printer ->

        conll_infile.withReader { reader ->
            def delimitedReader = new BlankLineTerminatedReader(reader)

            while (delimitedReader.next()) {
                // Can't use this because DGM.getFile(BufferedReader) uses read not readLine.
                // println delimitedReader.text
                // But readLines does.
                List<String> lines = delimitedReader.readLines()

                def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

                tokens.eachWithIndex { token, token_i ->
                    String id = token.with { chap_name + '/' + sent_indx + '/' + tok_indx }

//                    def negation_labels = token.labels.collate(1, 3)

                    def cue_label = '_'

                    def negated_scope_count = (token.labels.size() / 3) as Integer

                    negated_scope_count.times { scope_i ->
                        def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                        if (scope_labels[0] != '_') {
                            cue_label = (token.word == scope_labels[0]) ? '+' : '!'
                        }
                    }

                    def features = ['word_' + token.word.toLowerCase()
                                    , 'lemma_' + token.lemma.toLowerCase()
                                    , 'pos_' + token.pos
                            ]

                    affixes.each {
                        if ((token.word.startsWith(it) || token.word.endsWith(it)) && (token.word.length() > it.length()))
                            features += 'affix_' + it
                    }
                    
                    features.add(cue_label)

                    if (cue_label || !neg_only) {
                        printer.println features.join(' ')
                    }
                }
                
                printer.println()
            }
        }
    }
}

    def convert_to_cue_maxent_data(File conll_infile, File mallet_outfile)
    {
        def neg_only = false

        def affixes = ['un', 'im', 'dis', 'ir', 'in', 'less', 'lessly']

        mallet_outfile.withPrintWriter { printer ->

            conll_infile.withReader { reader ->
                def delimitedReader = new BlankLineTerminatedReader(reader)

                while (delimitedReader.next()) {
                    // Can't use this because DGM.getFile(BufferedReader) uses read not readLine.
                    // println delimitedReader.text
                    // But readLines does.
                    List<String> lines = delimitedReader.readLines()

                    def tokens = CoNLLDecode.decode_lines_to_tokens(lines)

                    tokens.eachWithIndex { token, token_i ->
                        String id = token.with { chap_name + '/' + sent_indx + '/' + tok_indx }

                        def negation_labels = token.labels.collate(1, 3)

                        def cue_label = '_pos_'

                        def negated_scope_count = (token.labels.size() / 3) as Integer

                        negated_scope_count.times { scope_i ->
                            def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                            if (scope_labels[0] != '_') cue_label = scope_labels[0]
                        }

                        def features = [:]

                        features['word_' + token.word.toLowerCase()] = 1
                        features['lemma_' + token.lemma] = 1
                        features['pos_' + token.pos] = 1

                        affixes.each { if ((token.word.startsWith(it) || token.word.endsWith(it)) && (token.word.length() > it.length())) features['affix_' + it] = 1 }

                        if (cue_label || !neg_only) {
                            printer.println id + '\t' + cue_label + '\t' + features.collect { k, v -> k + '\t' + v }.join('\t')
                        }
                    }
                }
            }
        }
    }

def convert_to_trees(File dev_file, File out_file)
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

                    def scope_labels = tokens.collect { it.labels[(scope_i * 3)..<((scope_i + 1) * 3)] }
                    
                    def start_scope_x = scope_labels.findIndexOf { it[1] != '_' }
                    def end_scope_x = scope_labels.findLastIndexOf { it[1] != '_' }

                    tokens.eachWithIndex { token, token_i ->
                        String id = token.with { chap_name + '/' + sent_indx + '/' + scope_i + '/' + tok_indx }

                        token.with {
//                            def xsyntax = syntax.replace('(', '<').replace(')', '>')
//                            def left_syntax = xsyntax.substring(0, syntax.indexOf('*')+1)
//                            def right_syntax = xsyntax.substring(syntax.indexOf('*'))

                            def sys_scope_label = scope_labels[token_i][1] == '_' ? '-' : '+'

//                            if (token_i == start_scope_x) {
//                                sys_scope_label = '['
//                            } else if (token_i == end_scope_x) {
//                                sys_scope_label = ']'
//                            } else if ((sys_scope_label == '-') && (token_i > start_scope_x) && (token_i < end_scope_x)) {
//                                sys_scope_label = '_'
//                            }
                            
                            sexp.append(syntax.replace('*', " (token $id ${sexp_escape(pos)} ${sexp_escape(word)} ${sexp_escape(lemma)} ${scope_labels[token_i][0]} ${sys_scope_label} ${scope_labels[token_i][2] == '_' ? '-' : '+'} $tok_indx ${sexp_brackets(syntax)}) "))
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

static def sexp_escape(String s)
{
    s = s.replace("(", "-LRB-")
    s = s.replace(")", "-RRB-")
    s
}

    static def sexp_brackets(String s)
    {
        s.replace("(", "[").replace(")", "]")
    }

    def convert_scope_to_conll(File scope_file, File in_file, File out_file)
    {
        out_file.withWriter { printer ->
            in_file.withReader { reader ->
                scope_file.withReader { label_reader ->

                    def delimitedReader = new BlankLineTerminatedReader(reader)

                    while (delimitedReader.next()) {
                        List<String> lines = delimitedReader.readLines()
                        //     def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns

                        def negated_scope_count = (CoNLLDecode.decode_line_to_token(lines[0]).labels.size() / 3) as Integer

//                        if (negated_scope_count) {
//                            println negated_scope_count
//                            println lines
//                            println '-----'
//                        }

                        def sys_labels = [:].withDefault { [:] }

                        negated_scope_count.times { scope_i ->
                            lines.size().times { token_i ->
                                def label = label_reader.readLine().trim()
                                if (!(label in ['+', '-', '!', '[', '_', ']'])) println "Unexpected label: $label $scope_i $token_i ${lines[0]}"
                                sys_labels[scope_i][token_i] = label
                            }
                            def bl = label_reader.readLine().trim()
                            if (bl != "") {
                                println "Label file out of sync! Expected blank line.  Got '$bl'"
//                                throw new FooException()
                            }
                        }

                        lines.eachWithIndex { line, token_i ->
                            def token = CoNLLDecode.decode_line_to_token(line)

                            def negated_scope_count_i = (token.labels.size() / 3) as Integer

                            assert negated_scope_count == negated_scope_count_i

                            token.with { printer.print ([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }

                            if (negated_scope_count) {
                                negated_scope_count.times { scope_i ->
                                    def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

//                            String id = token.with { chap_name + '/' + sent_indx + '/' + scope_i + '/' + tok_indx }
                                    def label = sys_labels[scope_i][token_i] in ['-', '_'] ? '_' : token.word

//                                    label = label.replaceAll(/\./, '%')

                                    if (sys_labels[scope_i][token_i] == '!') {
                                        // For cues we get the scope token (if any) from the cue finder.
                                        label = scope_labels[1]
//                                        if (scope_labels[0] == "less") {
//                                            label = token.word.replaceAll(/less(?:ness)?(?:ly)?$/, "")
//                                        } else {
//                                            label = token.word.replaceFirst("^${scope_labels[0]}", "")
//                                        }
//                                        if (!label) label = '_'
                                    }

                                    printer.print '\t'
                                    printer.print ([scope_labels[0], label, scope_labels[2]].join('\t'))
//                            printer.print scope_labels.join('\t')
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
        }
    }

    def all_tokens_for_events = true

    def convert_event_to_conll(Boolean training, File event_file, File in_file, File out_file)
    {
        out_file.withWriter { printer ->
            in_file.withReader { reader ->
                event_file.withReader { label_reader ->

                    def delimitedReader = new BlankLineTerminatedReader(reader)

                    while (delimitedReader.next()) {
                        List<String> lines = delimitedReader.readLines()
                        //     def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns

                        def tokens = decode_lines_to_tokens(lines)

                        def negated_scope_count = (tokens[0].labels.size() / 3) as Integer

                        def sys_labels = [:].withDefault { [:].withDefault { '_' } }

//                        negated_scope_count.times { scope_i ->
//                            tokens.size().times { token_i ->
//                                def label = label_reader.readLine().trim()
//                                if (!(label in ['+', '-', '!', '[', '_', ']'])) println "Unexpected label: $label $scope_i $token_i ${lines[0]}"
//                                sys_labels[scope_i][token_i] = label
//                            }
//                            def bl = label_reader.readLine().trim()
//                            if (bl != "") println "Label file out of sync! Expected blank line.  Got '$bl'"
//                        }

                        negated_scope_count.times { scope_i ->
                            def can_has_event = true
                            
                            if (training) {
                                can_has_event = false

                                tokens.eachWithIndex { token, token_i ->
                                    def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]
                                    if (scope_labels[2] != '_') { can_has_event = true }
                                }
                            }

                            if (all_tokens_for_events || can_has_event) {
                                def has_scope = false
                                tokens.eachWithIndex { token, token_i ->
                                    def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]
                                    if (all_tokens_for_events || (scope_labels[1] != '_')) {
                                        has_scope = true
                                        def label = label_reader.readLine().trim()
                                        if (!(label in ['+', '-', '!', '[', '_', ']'])) println "Unexpected label: $label $scope_i $token_i ${lines[0]}"
                                        sys_labels[scope_i][token_i] = label
                                    }
                                }
                            
                                if (has_scope) {
                                    def bl = label_reader.readLine().trim()
                                    if (bl != "") println "Label file out of sync! Expected blank line.  Got '$bl'"
                                }
                            }
                        }

                        tokens.eachWithIndex { token, token_i ->
                            def negated_scope_count_i = (token.labels.size() / 3) as Integer

                            assert negated_scope_count == negated_scope_count_i

                            token.with { printer.print ([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }

                            if (negated_scope_count) {
                                negated_scope_count.times { scope_i ->
                                    def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                                    def label = sys_labels[scope_i][token_i] in ['-', '_'] ? '_' : token.word

                                    if (scope_labels[0] != '_') {
                                        // For cues we get the scope token (if any) from the cue finder.
                                        label = scope_labels[1]
//                                        if (scope_labels[0] == "less") {
//                                            label = token.word.replaceAll(/less(?:ness)?(?:ly)?$/, "")
//                                        } else {
//                                            label = token.word.replaceFirst("^${scope_labels[0]}", "")
//                                        }
//                                        if (!label) label = '_'
                                    }

                                    printer.print '\t'
                                    printer.print ([scope_labels[0], scope_labels[1], label].join('\t'))
//                            printer.print scope_labels.join('\t')
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
        }
    }

    def convert_cue_to_conll(File event_file, File in_file, File out_file)
    {
        out_file.withWriter { printer ->
            in_file.withReader { reader ->
                event_file.withReader { label_reader ->

                    def delimitedReader = new BlankLineTerminatedReader(reader)

                    while (delimitedReader.next()) {
                        List<String> lines = delimitedReader.readLines()
                        //     def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns

                        def negated_scope_count = (CoNLLDecode.decode_line_to_token(lines[0]).labels.size() / 3) as Integer

                        def sys_labels = [:].withDefault { [:] }

                        negated_scope_count.times { scope_i ->
                            lines.size().times { token_i ->
                                def label = label_reader.readLine().trim()
                                if (!(label in ['+', '-', '!', '[', '_', ']'])) println "Unexpected label: $label $scope_i $token_i ${lines[0]}"
                                sys_labels[scope_i][token_i] = label
                            }
                            def bl = label_reader.readLine().trim()
                            if (bl != "") println "Label file out of sync! Expected blank line.  Got '$bl'"
                        }

                        lines.eachWithIndex { line, token_i ->
                            def token = CoNLLDecode.decode_line_to_token(line)

                            def negated_scope_count_i = (token.labels.size() / 3) as Integer

                            assert negated_scope_count == negated_scope_count_i

                            token.with { printer.print ([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }

                            if (negated_scope_count) {
                                negated_scope_count.times { scope_i ->
                                    def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]

                                    def label = '_'
                                    
                                    if (sys_labels[scope_i][token_i] == '+') {
                                        label = token.word
                                    } else if (sys_labels[scope_i][token_i] == '!') {
                                        affixes.each {
                                            if (token.word.startsWith(it) || token.word.endsWith(it)) {
                                                label = it
                                            }
                                        }
                                        if (label == '_') label = 'XX'
                                    }

                                    printer.print '\t'
                                    printer.print ([label, scope_labels[1], scope_labels[2]].join('\t'))
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
        }
    }

    def tree_to_scope_sequence(File tree_infile, File outfile)
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
//                        println cue_path
                        if (cue_path) {
                            print_scope_sequence(sexp, cue_path, [], ['cpr_root'], printer)
                            printer.println()
                        }
                    }
                }
            }
        }
    }

    def print_scope_sequence(Object tree, List cue_up_path, List cue_down_path, List cue_path_rel, PrintWriter printer)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                def cue = cue_up_path[0]

                def up_path = cue_up_path.tail().collect { it[0] }.join('_')
                def down_path = cue_down_path.collect { it[0] }.join('_')

                def label = (tree[5] == '_') ? tree[6] : '!'
                
//                def up_coordination = find_coordination(cue_up_path)
//                def down_coordination = find_coordination(cue_down_path)

                def instance = [
                        'pos_' + tree[2]
//                        , 'pos1_' + tree[2][0]
                        , 'word_' + tree[3].toLowerCase()
                        , 'lemma_' + tree[4].toLowerCase()
//                        , 'reld=' + (((tree[8] as Integer) - (cue[8] as Integer)) < 0 ? 'L' : 'R')
                        , 'dist=' + ((tree[8] as Integer) - (cue[8] as Integer))
                        , 'cue_word_' + cue[3].toLowerCase()
                        , 'cue_lemma_' + cue[4].toLowerCase()
                        , 'cue_pos_' + cue[2]
//                        , 'cue_pos1_' + cue[2][0]
                        , 'in=' + (tree[9] == '*')
                        , 'up_' + up_path
                        , 'down_' + down_path
// No Help                , 'synl_' + tree[9].substring(0, tree[9].indexOf('*')+1)
// No Help                , 'synr_' + tree[9].substring(tree[9].indexOf('*'))
//                        , 'cc_up=' + !up_coordination.isEmpty()
//                        , 'cc_down=' + !down_coordination.isEmpty()
                        , label
                ]
                
                if (tree[5] != '_')  instance = ["is_cue"] + instance

                instance = cue_path_rel + instance

                // cc.mallet.fst.SimpleTagger says "whitespace" but what they mean is space.
//                    printer.println (instance.join('\t'))
                printer.println (instance.join(' '))

//                throw new FooException()
            } else {
                if (tree.is(cue_up_path[-1])) {
                    cue_up_path = cue_up_path[0..-2]

                    def cue_sibling_x = tree.tail().indexOf(cue_up_path[-1])
                    
                    tree.tail().eachWithIndex { child, x ->
//                        print_scope_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0], 'cpr_dx_' + (x - cue_sibling_x)], printer)
//                        print_scope_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0] + '_' + (x - cue_sibling_x)], printer)
// Not better             print_scope_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0], 'cprd_' + cue_rel_distance(x, cue_sibling_x)], printer)
                        print_scope_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0] + '_' + cue_rel_distance(x, cue_sibling_x)], printer)
                    }
                } else {
                    cue_down_path = cue_down_path + [tree]

                    tree.tail().each {
                        print_scope_sequence(it, cue_up_path, cue_down_path, cue_path_rel, printer)
                    }
                }

            }
        }
    }

     String cue_rel_distance(x, cue_sibling_x)
    {
        def d = x - cue_sibling_x
//        return x < cue_sibling_x ? 'L' : 'R'
//        (d < -2) ? 'L' : (d > 2) ? 'R' : d
        (cue_sibling_x ? 'X' : 'H') + d
    }

    def tree_to_event_sequence(Boolean training, File tree_infile, File outfile)
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
//                        println cue_path
                        if (cue_path && (all_tokens_for_events || !training || find_event_tokens(sexp))) {
                            print_event_sequence(sexp, cue_path, [], ['cpr_root'], printer)
                            if (all_tokens_for_events || training || find_scope_tokens(sexp)) { printer.println() }
                        }
                    }
                }
            }
        }
    }

    def print_event_sequence(Object tree, List cue_up_path, List cue_down_path, List cue_path_rel, PrintWriter printer)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                def cue = cue_up_path[0]

                def up_path = cue_up_path.tail().collect { it[0] }.join('_')
                def down_path = cue_down_path.collect { it[0] }.join('_')

                def label = tree[7]

//                def up_coordination = find_coordination(cue_up_path)
//                def down_coordination = find_coordination(cue_down_path)

                def instance = [
                        'pos_' + tree[2]
//                        , 'pos1_' + tree[2][0]
                        , 'word_' + tree[3].toLowerCase()
                        , 'lemma_' + tree[4].toLowerCase()
//                        , 'reld=' + (((tree[8] as Integer) - (cue[8] as Integer)) < 0 ? 'L' : 'R')
                        , 'dist=' + ((tree[8] as Integer) - (cue[8] as Integer))
                        , 'cue_word_' + cue[3].toLowerCase()
                        , 'cue_lemma_' + cue[4].toLowerCase()
                        , 'cue_pos_' + cue[2]
//                        , 'cue_pos1_' + cue[2][0]
                        , 'in=' + (tree[9] == '*')
                        , 'up_' + up_path
                        , 'down_' + down_path
//                        , 'scope=' + ((tree[5] == '_') ? tree[6] : '!')
//                        , 'cc_up=' + !up_coordination.isEmpty()
//                        , 'cc_down=' + !down_coordination.isEmpty()
                        , label
                ]

                if (tree[5] != '_')  instance = ["is_cue"] + instance

                instance = cue_path_rel + instance

                // cc.mallet.fst.SimpleTagger says "whitespace" but what they mean is space.
//                    printer.println (instance.join('\t'))
                if (all_tokens_for_events || (tree[6] == '+')) { printer.println (instance.join(' ')) }

//                throw new FooException()
            } else {
                if (tree.is(cue_up_path[-1])) {
                    cue_up_path = cue_up_path[0..-2]

                    def cue_sibling_x = tree.tail().indexOf(cue_up_path[-1])

                    tree.tail().eachWithIndex { child, x ->
//                        print_scope_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0], 'cpr_dx_' + (x - cue_sibling_x)], printer)
                        print_event_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0] + '_' + (x - cue_sibling_x)], printer)
//                        print_event_sequence(child, cue_up_path, cue_down_path, ['cpr_' + tree[0] + '_' + cue_rel_distance(x, cue_sibling_x)], printer)
                    }
                } else {
                    cue_down_path = cue_down_path + [tree]

                    tree.tail().each {
                        print_event_sequence(it, cue_up_path, cue_down_path, cue_path_rel, printer)
                    }
                }

            }
        }
    }

    // Non-terimnal nodes on path (if any) that contain an immediate CC child token node.
    List find_coordination(List path)
    {
        path.collectMany { (it[0] == "token" ? [] : (it.tail().find { it[0] == "token" && it[2] == "CC" } ? [it] : []))}
    }

def tree_to_mallet(File tree_infile, File outfile)
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
//                        println cue_path
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

    List find_scope_tokens(tree)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                (tree[6] == '+') ? [tree] : []
            } else {
                tree.tail().collectMany { find_scope_tokens(it) }
            }
        } else {
            []
        }
    }

    List find_event_tokens(tree)
    {
        if (tree instanceof List) {
            if (tree[0] == 'token') {
                (tree[7] == '+') ? [tree] : []
            } else {
                tree.tail().collectMany { find_event_tokens(it) }
            }
        } else {
            []
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
