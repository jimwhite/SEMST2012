
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

    static String tokens_to_sexpstring(List tokens)
    {
        def sb = new StringBuilder(tokens.size() * 20)

        tokens.each {
            it.with {
                sb.append(syntax.replace('*', " ($pos ${(word == lemma) ? word : "($word $lemma)"}) "))
            }
        }

        sb.toString()
    }

}
