#!/bin/bash

CLASSPATH=out/production/SEMST2012:src ./cue_finder.groovy 
#CLASSPATH=out/production/SEMST2012:src ./cue_finder.groovy data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt
#CLASSPATH=out/production/SEMST2012:src ./cue_finder.groovy data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt

CLASSPATH=out/production/SEMST2012:src ./convert_conll_to_mallet.groovy data/output.train.cue.txt
CLASSPATH=out/production/SEMST2012:src ./convert_conll_to_mallet.groovy data/output.dev.cue.txt
  
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/scope.model data/scope.output.train.cue.txt >data/output.train.scope.txt
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/scope.model data/scope.output.dev.cue.txt >data/output.dev.scope.txt

CLASSPATH=out/production/SEMST2012:src ./convert_scope_to_conll.groovy data/output.train.cue.txt data/output.train.scope.txt data/sys0.training.conll.txt
CLASSPATH=out/production/SEMST2012:src ./convert_scope_to_conll.groovy data/output.dev.cue.txt data/output.dev.scope.txt data/sys0.dev.conll.txt
  
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/event.model data/event.output.train.cue.txt >data/output.train.event.txt
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/event.model data/event.output.dev.cue.txt >data/output.dev.event.txt

CLASSPATH=out/production/SEMST2012:src ./convert_event_to_conll.groovy data/sys0.training.conll.txt data/output.train.event.txt data/sys.training.conll.txt
CLASSPATH=out/production/SEMST2012:src ./convert_event_to_conll.groovy data/sys0.dev.conll.txt data/output.dev.event.txt data/sys.dev.conll.txt
  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/sys.training.conll.txt  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/sys.dev.conll.txt
