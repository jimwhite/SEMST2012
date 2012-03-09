#!/usr/bin/env groovy

// James White UW ID 1138573 Net ID jimwhite
// Pretty-print s-expressions from input to output

System.out.withWriter {
    IndentWriter writer = new IndentWriter(it)

    System.in.withReader { reader ->
        def line = new StringBuilder()

        def cint

        while ((cint = reader.read()) >= 0) {
            Character c = cint
            if (c == '\n') {
                println line.toString()
                line = new StringBuilder()
            } else if (c == '(') {
                def sexp = readSexpList(reader)

                printTree(sexp, writer)
                writer.println()
            } else {
                line.append(c)
            }
        }
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


