#!/usr/bin/env groovy

data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

train_file = new File(scope_dir, 'combined.txt')

train_cues_file = new File(data_dir, 'train.cues.txt')
train_cues_vector_file = new File(data_dir, 'train.cues.vectors')

cue_classifier = new File(data_dir, 'cue.classifier')

train_cue_output = new File(data_dir, 'output.train.cue.txt')

train_trees_file = new File(data_dir, 'train.trees.txt')

train_scope_file = new File(data_dir, 'train.scope.txt')

train_scope_output = new File(data_dir, "output.train.scope.txt")

scope_classifier = new File(data_dir, "scope.model")

train_event_file = new File(data_dir, 'train.event.txt')

train_event_output = new File(data_dir, "output.train.event.txt")

event_classifier = new File(data_dir, "event.model")

sys_train_file = new File(data_dir, "output.training.conll.txt")

def decoder = new CoNLLDecode()

decoder.convert_to_cue_data(train_file, train_cues_file)

decoder.convert_to_trees(train_file, train_trees_file)

decoder.tree_to_scope_sequence(train_trees_file, train_scope_file)

decoder.tree_to_event_sequence(true, train_trees_file, train_event_file)

