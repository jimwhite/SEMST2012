#!/bin/bash

java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --train true --model-file data/scope.model --threads 8 data/train.scope.txt 2>data/err.txt >data/out.txt

java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/scope.model data/train.scope.txt >data/output.train.scope.txt
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/scope.model data/dev.scope.txt >data/output.dev.scope.txt

CLASSPATH=out/production/SEMST2012:src ./convert_scope_to_conll.groovy
  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/output.training.conll.txt  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/output.dev.conll.txt

  