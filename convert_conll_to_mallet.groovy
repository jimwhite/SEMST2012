#!/usr/bin/env groovy

data_dir = new File('data')

infile = new File(args[0])

trees_file = new File(data_dir, 'trees.' + infile.name)

scope_file = new File(data_dir, 'scope.' + infile.name)

scope_output = new File(data_dir, "output.scope." + infile.name)

scope_classifier = new File(data_dir, "scope.model")

event_file = new File(data_dir, 'event.' + infile.name)

event_output = new File(data_dir, "output.event." + infile.name)

event_classifier = new File(data_dir, "event.model")

sys_conll_file = new File(data_dir, "conll." + infile.name)

def decoder = new CoNLLDecode()

decoder.convert_to_trees(infile, trees_file)

decoder.tree_to_scope_sequence(trees_file, scope_file)

decoder.tree_to_event_sequence(/*args[1] as Boolean*/false, trees_file, event_file)
