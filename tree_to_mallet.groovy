#!/usr/bin/env groovy

// James White UW ID 1138573 Net ID jimwhite
// Convert CoNLL negation sexp to Mallet instances


/*
 mallet import-file --input data/train.scope.txt --output data/train.scope.vectors
 mallet train-classifier --input data/train.scope.vectors --training-portion 0.9 --trainer MaxEnt

 mallet import-file --input data/split_train.up_scope.txt --output data/split_train.up_scope.vectors
 mallet import-file --input data/split_test.up_scope.txt --output data/split_test.up_scope.vectors --use-pipe-from data/split_train.up_scope.vectors
 mallet train-classifier --input data/split_train.up_scope.vectors --trainer MaxEnt --output-classifier up_scope_max_ent.classifier
 mallet classify-file --input data/split_test.up_scope.txt --output - --classifier up_scope_max_ent.classifier
  */

new File('data/train.scope.txt').withPrintWriter { printer ->
    new File('data/train.trees.txt').withReader { reader ->
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


def xprintInstances(Object tree, List cue, PrintWriter printer)
{
    if (tree instanceof List) {
        if (tree[0] == 'token') {
            if (tree[5] == '_') {
                def features = [:]

                def instance = [tree[1], tree[6]
                        , 'pos_' + tree[2], 1, 'word_' + tree[3], 1, 'lemma_' + tree[4], 1
                        , 'cue_word_' + cue[3].toLowerCase(), 1, 'cue_lemma_' + cue[4], 1, 'cue_pos_' + cue[2], 1
                        , 'distance', (tree[8] as Integer) - (cue[8] as Integer)  // Good for ~2% acc
                ]
                printer.println (instance.join('\t'))
            }
        } else {
            tree.each { printInstances(it, cue, printer) }
        }
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


