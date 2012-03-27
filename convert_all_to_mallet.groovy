#!/usr/bin/env groovy

data_dir = new File('data')
scope_dir = new File(data_dir, 'SEM-2012-SharedTask-CD-SCO-09032012b')

train_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-training-09032012.txt')
//train_file = new File(scope_dir, 'coordinated_train.txt')
dev_file = new File(scope_dir, 'SEM-2012-SharedTask-CD-SCO-dev-09032012.txt')

train_cues_file = new File(data_dir, 'train.cues.txt')
train_cues_vector_file = new File(data_dir, 'train.cues.vectors')
dev_cues_file = new File(data_dir, 'dev.cues.txt')

cue_classifier = new File(data_dir, 'cue.classifier')

train_cue_output = new File(data_dir, 'output.train.cue.txt')
dev_cue_output = new File(data_dir, 'output.dev.cue.txt')

train_trees_file = new File(data_dir, 'train.trees.txt')
dev_trees_file = new File(data_dir, 'dev.trees.txt')

//train_scope_vectors = new File(data_dir, 'train.scope.vectors')
train_scope_file = new File(data_dir, 'train.scope.txt')
dev_scope_file = new File(data_dir, 'dev.scope.txt')

train_scope_output = new File(data_dir, "output.train.scope.txt")
dev_scope_output = new File(data_dir, "output.dev.scope.txt")

scope_classifier = new File(data_dir, "scope.model")

train_event_file = new File(data_dir, 'train.event.txt')
dev_event_file = new File(data_dir, 'dev.event.txt')

train_event_output = new File(data_dir, "output.train.event.txt")
dev_event_output = new File(data_dir, "output.dev.event.txt")

event_classifier = new File(data_dir, "event.model")

sys_train_file = new File(data_dir, "output.training.conll.txt")
sys_dev_file = new File(data_dir, "output.dev.conll.txt")

def decoder = new CoNLLDecode()

prepare_data = true

if (prepare_data) {
    decoder.convert_to_cue_data(train_file, train_cues_file)
    decoder.convert_to_cue_data(dev_file, dev_cues_file)

    decoder.convert_to_trees(train_file, train_trees_file)
    decoder.convert_to_trees(dev_file, dev_trees_file)

//    decoder.tree_to_mallet(train_trees_file, train_scope_file)
//    decoder.tree_to_mallet(dev_trees_file, dev_scope_file)

    decoder.tree_to_scope_sequence(train_trees_file, train_scope_file)
    decoder.tree_to_scope_sequence(dev_trees_file, dev_scope_file)

    decoder.tree_to_event_sequence(true, train_trees_file, train_event_file)
    decoder.tree_to_event_sequence(false, dev_trees_file, dev_event_file)
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

//def train_scope_vectors_process = ["mallet", "import-file", "--input", train_scope_file
//        , "--output", train_scope_vectors].execute()
//println "train_scope_vectors_process = ${train_scope_vectors_process.waitFor()}"
//
//def train_scope_classifier_process = ["mallet", "train-classifier", "--input", train_scope_vectors
//        , "--trainer", "MaxEnt", "--output-classifier", scope_classifier].execute()
//println "train_scope_classifier_process = ${train_scope_classifier_process.waitFor()}"
//
//def classify_train_scope_process = ["mallet", "classify-file", "--input", train_scope_file
//        , "--classifier", scope_classifier, "--output", train_scope_output].execute()
//println "classify_train_scope_process = ${classify_train_scope_process.waitFor()}"
//
//def classify_dev_scope_process = ["mallet", "classify-file", "--input", dev_scope_file
//        , "--classifier", scope_classifier, "--output", dev_scope_output].execute()
//println "classify_dev_scope_process = ${classify_dev_scope_process.waitFor()}"
//

return 0

println simple_train_simple_tagger(train_scope_file, scope_classifier)

println simple_simple_tagger(scope_classifier, train_scope_file, train_scope_output)
println simple_simple_tagger(scope_classifier, dev_scope_file, dev_scope_output)

def simple_train_simple_tagger(File input_file, File model_file)
{
    // cc.mallet.fst.SimpleTagger --train true --model-file nouncrf  sample
    def proc = ["java", "-cp", "/opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar"
                , "cc.mallet.fst.SimpleTagger", "--train", true, "--threads", 8, "--model-file", model_file, input_file].execute()


    proc.consumeProcessOutput(System.out, System.err)

    return proc.waitFor()
}

def simple_simple_tagger(File model_file, File input_file, File output_file)
{
    // cc.mallet.fst.SimpleTagger --train true --model-file nouncrf  sample
    def proc = ["java", "-cp", "/opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar"
            , "cc.mallet.fst.SimpleTagger", "--model-file", model_file, input_file].execute()

    output_file.withWriter { output ->
        proc.consumeProcessOutput(output, System.err)
    }

    return proc.waitFor()
}

decoder.convert_scope_to_conll(train_scope_output, train_file, sys_train_file)
decoder.convert_scope_to_conll(dev_scope_output, dev_file, sys_dev_file)
