#!/usr/bin/env groovy

data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

train_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

train_cues_file = new File(data_dir, 'train.cues.txt')
dev_cues_file = new File(data_dir, 'dev.cues.txt')

train_trees_file = new File(data_dir, 'train.trees.txt')
dev_trees_file = new File(data_dir, 'dev.trees.txt')

train_scope_file = new File(data_dir, 'train.scope.txt')
train_scope_vectors = new File(data_dir, 'train.scope.vectors')
dev_scope_file = new File(data_dir, 'dev.scope.txt')

train_scope_output = new File(data_dir, "output.train.scope.txt")
dev_scope_output = new File(data_dir, "output.dev.scope.txt")

sys_train_file = new File(data_dir, "output.training.conll.txt")
sys_dev_file = new File(data_dir, "output.dev.conll.txt")

/*
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/output.training.conll.txt
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/output.dev.conll.txt
 */

convert_to_conll(train_scope_output, train_file, sys_train_file)
convert_to_conll(dev_scope_output, dev_file, sys_dev_file)

def convert_to_conll(File scope_file, File in_file, File out_file)
{
    out_file.withWriter { printer ->
        in_file.withReader { reader ->
            scope_file.withReader { label_reader ->

            def delimitedReader = new BlankLineTerminatedReader(reader)

            while (delimitedReader.next()) {
                List<String> lines = delimitedReader.readLines()
                //     def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns

                def negated_scope_count = (CoNLLDecode.decode_line_to_token(lines[0]).labels.size() / 3) as Integer
                
                def sys_labels = [:].withDefault { [:] }
                
                negated_scope_count.times { scope_i ->
                    lines.size().times { token_i ->
                        def label = label_reader.readLine().trim()
                        if (!(label in ['+', '-', '!'])) println "Unexpected label: $label $scope_i $token_i ${lines[0]}"
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

//                            String id = token.with { chap_name + '/' + sent_indx + '/' + scope_i + '/' + tok_indx }
                            def label = sys_labels[scope_i][token_i] == '+' ? token.word : '_'

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

def xconvert_to_conll(File scope_file, File in_file, File out_file)
{
    out_file.withWriter { printer ->
        in_file.withReader { reader ->
            scope_file.withReader { label_reader ->
                def delimitedReader = new BlankLineTerminatedReader(reader)

                while (delimitedReader.next()) {
                    List<String> lines = delimitedReader.readLines()
                    //     def (chap_name, sent_indx, tok_indx, word, lemma, pos, syntax) = columns

                    lines.each {
                        def token = CoNLLDecode.decode_line_to_token(it)

                        def negated_scope_count = (token.labels.size() / 3) as Integer

                        token.with { printer.print ([chap_name, sent_indx, tok_indx, word, lemma, pos, syntax].join('\t')) }

                        if (negated_scope_count) {
                            negated_scope_count.times { scope_i ->
                                def scope_labels = token.labels[(scope_i * 3)..<((scope_i + 1) * 3)]
                                
                                def raw_label = label_reader.readLine().trim()
                                
                                if (!raw_label in ['+', '-']) println "Unexpected label: $raw_label"

                                String id = token.with { chap_name + '/' + sent_indx + '/' + scope_i + '/' + tok_indx }
                                def label = raw_label == '+' ? token.word : '_'

                                printer.print '\t'
                                printer.print ([scope_labels[0], label, scope_labels[2]].join('\t'))
    //                            printer.print scope_labels.join('\t')

                                def bl = label_reader.readLine().trim()
                                if (bl != "") println "Label file out of sync! Expected blank line.  Got '$bl'"
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

//new File('data/dev.scope.txt').eachLine {
//    def fields = it.split(/\s+/)
//    println "${sys_label[fields[0]]} $it"
//}

