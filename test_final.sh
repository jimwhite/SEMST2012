#!/bin/bash

MALLET_DIST=/opt/local/share/java/mallet-2.0.7/dist
export MALLET_CLASSPATH=$MALLET_DIST/mallet.jar:$MALLET_DIST/mallet-deps.jar

export CLASSPATH=out/production/SEMST2012:src

TRAINING_DATA=data/starsem-st-2012-data/cd-sco/corpus/training/SEM-2012-SharedTask-CD-SCO-training-09032012.txt

./cue_finder.groovy "$TRAINING_DATA" $1 data/sys0.dev.conll.txt

./convert_conll_to_mallet.groovy data/sys0.dev.conll.txt false
  
java -d32 -Xmx800m -cp "$MALLET_CLASSPATH" cc.mallet.fst.SimpleTagger --model-file data/scope.model data/scope.sys0.dev.conll.txt >data/sys1.dev.scope.txt

./convert_scope_to_conll.groovy data/sys0.dev.conll.txt data/sys1.dev.scope.txt data/sys1.dev.conll.txt

./convert_conll_to_mallet.groovy data/sys1.dev.conll.txt false
 
java -d32 -Xmx800m -cp "$MALLET_CLASSPATH" cc.mallet.fst.SimpleTagger --model-file data/event.model data/event.sys1.dev.conll.txt >data/sys2.dev.event.txt

./convert_event_to_conll.groovy data/sys1.dev.conll.txt data/sys2.dev.event.txt $2 false
