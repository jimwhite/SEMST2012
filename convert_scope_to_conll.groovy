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

def decoder = new CoNLLDecode()

decoder.convert_scope_to_conll(train_scope_output, train_file, sys_train_file)
decoder.convert_scope_to_conll(dev_scope_output, dev_file, sys_dev_file)
