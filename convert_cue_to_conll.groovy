#!/usr/bin/env groovy

data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

train_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

train_cue_output = new File(data_dir, 'output.train.cue.txt')
dev_cue_output = new File(data_dir, 'output.dev.cue.txt')

sys_train_file = new File(data_dir, "output.training.conll.txt")
sys_dev_file = new File(data_dir, "output.dev.conll.txt")

def decoder = new CoNLLDecode()

decoder.convert_cue_to_conll(train_cue_output, train_file, sys_train_file)
decoder.convert_cue_to_conll(dev_cue_output, dev_file, sys_dev_file)
