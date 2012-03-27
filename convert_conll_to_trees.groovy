
data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012')

train_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

// mallet import-file --input data/train.mallet.txt --output data/train.vectors
// mallet train-classifier --input data/train.vectors --training-portion 0.9 --trainer MaxEnt

train_out_file = new File(data_dir, 'train.trees.txt')
dev_out_file = new File(data_dir, 'dev.trees.txt')

decoder = new CoNLLDecode()

decoder.convert_to_trees(train_file, train_out_file)

decoder.convert_to_trees(dev_file, dev_out_file)

