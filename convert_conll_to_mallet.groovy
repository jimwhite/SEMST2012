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

prepare_data = false

if (prepare_data) {
    def decoder = new CoNLLDecode()

    decoder.convert_to_cue_data(train_file, train_cues_file)
    decoder.convert_to_cue_data(dev_file, dev_cues_file)

    decoder.convert_to_trees(train_file, train_trees_file)
    decoder.convert_to_trees(dev_file, dev_trees_file)

    decoder.tree_to_mallet(train_trees_file, train_scope_file)
    decoder.tree_to_mallet(dev_trees_file, dev_scope_file)
}

/*
 mallet import-file --input data/train.mallet.txt --output data/train.vectors
 mallet train-classifier --input data/train.vectors --training-portion 0.9 --trainer MaxEnt

 mallet import-file --input data/train.scope.txt --output data/train.scope.vectors
 mallet train-classifier --input data/train.scope.vectors --training-portion 0.9 --trainer MaxEnt

 mallet import-file --input data/split_train.up_scope.txt --output data/split_train.up_scope.vectors
 mallet import-file --input data/split_test.up_scope.txt --output data/split_test.up_scope.vectors --use-pipe-from data/split_train.up_scope.vectors
 mallet train-classifier --input data/split_train.up_scope.vectors --trainer MaxEnt --output-classifier up_scope_max_ent.classifier
 mallet classify-file --input data/split_test.up_scope.txt --output - --classifier up_scope_max_ent.classifier
  */

def train_scope_vectors_process = ["mallet", "import-file", "--input", train_scope_file
        , "--output", train_scope_vectors].execute()
println "train_scope_vectors_process = ${train_scope_vectors_process.waitFor()}"

scope_classifier = new File(data_dir, "scope_max_ent.classifier")

def train_scope_classifier_process = ["mallet", "train-classifier", "--input", train_scope_vectors
        , "--trainer", "MaxEnt", "--output-classifier", scope_classifier].execute()
println "train_scope_classifier_process = ${train_scope_classifier_process.waitFor()}"

def classify_train_scope_process = ["mallet", "classify-file", "--input", train_scope_file
        , "--classifier", scope_classifier, "--output", train_scope_output].execute()
println "classify_train_scope_process = ${classify_train_scope_process.waitFor()}"

def classify_dev_scope_process = ["mallet", "classify-file", "--input", dev_scope_file
        , "--classifier", scope_classifier, "--output", dev_scope_output].execute()
println "classify_dev_scope_process = ${classify_dev_scope_process.waitFor()}"


