#!/bin/bash

CLASSPATH=out/production/SEMST2012:src ./cue_finder.groovy 
#CLASSPATH=out/production/SEMST2012:src ./cue_finder.groovy data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt
#CLASSPATH=out/production/SEMST2012:src ./cue_finder.groovy data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt

mv data/output.train.cue.txt data/sys0.train.conll.txt
mv data/output.dev.cue.txt data/sys0.dev.conll.txt

CLASSPATH=out/production/SEMST2012:src ./convert_conll_to_mallet.groovy data/sys0.train.conll.txt
CLASSPATH=out/production/SEMST2012:src ./convert_conll_to_mallet.groovy data/sys0.dev.conll.txt
  
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/scope.model data/scope.sys0.train.conll.txt >data/sys1.train.scope.txt
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/scope.model data/scope.sys0.dev.conll.txt >data/sys1.dev.scope.txt

CLASSPATH=out/production/SEMST2012:src ./convert_scope_to_conll.groovy data/sys0.train.conll.txt data/sys1.train.scope.txt data/sys1.train.conll.txt
CLASSPATH=out/production/SEMST2012:src ./convert_scope_to_conll.groovy data/sys0.dev.conll.txt data/sys1.dev.scope.txt data/sys1.dev.conll.txt
  
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/event.model data/event.sys0.train.conll.txt >data/sys2.train.event.txt
java -d32 -Xmx800m -cp /opt/local/share/java/mallet-2.0.7/dist/mallet.jar:/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar cc.mallet.fst.SimpleTagger --model-file data/event.model data/event.sys0.dev.conll.txt >data/sys2.dev.event.txt

CLASSPATH=out/production/SEMST2012:src ./convert_event_to_conll.groovy data/sys1.train.conll.txt data/sys2.train.event.txt data/sys.train.conll.txt
CLASSPATH=out/production/SEMST2012:src ./convert_event_to_conll.groovy data/sys1.dev.conll.txt data/sys2.dev.event.txt data/sys.dev.conll.txt
  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/sys.train.conll.txt  
./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/sys.dev.conll.txt
