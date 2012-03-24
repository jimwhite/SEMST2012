#!/bin/bash

#mallet import-file --input data/train.cues.txt --output data/train.cues.vectors
#mallet train-classifier --trainer MaxEnt --input data/train.cues.vectors --output-classifier data/cue.classifier
#mallet classify-file --classifier data/cue.classifier --input data/train.cues.txt --output data/output.train.cue.txt
#mallet classify-file --classifier data/cue.classifier --input data/dev.cues.txt --output data/output.dev.cue.txt

CLASSPATH=out/production/SEMST2012:src ./get_crf_labels.groovy data/train.cues.txt >data/train.cues.labels.txt
CLASSPATH=out/production/SEMST2012:src ./get_crf_labels.groovy data/dev.cues.txt >data/dev.cues.labels.txt

java -d32 -Xmx1800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --train true --model-file data/cue.model --threads 8 data/train.cues.txt 2>data/err.txt >data/out.txt

java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/cue.model data/train.cues.txt >data/output.train.cue.txt
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/cue.model data/dev.cues.txt >data/output.dev.cue.txt

CLASSPATH=out/production/SEMST2012:src ./convert_cue_to_conll.groovy
  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/output.training.conll.txt  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/output.dev.conll.txt
