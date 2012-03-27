#!/usr/bin/env groovy

data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

train_file = args.size() < 3 ? new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt') : new File(args[0])
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

train_scope_output = args.size() < 3 ? new File(data_dir, "output.train.scope.txt") : new File(args[1])
dev_scope_output = new File(data_dir, "output.dev.scope.txt")

sys_train_file = args.size() < 3 ? new File(data_dir, "output.training.conll.txt") : new File(args[2])
sys_dev_file = new File(data_dir, "output.dev.conll.txt")

/*
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/output.training.conll.txt
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/output.dev.conll.txt
 */

def decoder = new CoNLLDecode()

//println "convert_scope_to_conll($train_scope_output, $train_file, $sys_train_file)"
decoder.convert_scope_to_conll(train_scope_output, train_file, sys_train_file)

if (args.size() < 3) decoder.convert_scope_to_conll(dev_scope_output, dev_file, sys_dev_file)
