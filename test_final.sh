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


# ./test_final.sh data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt data/xdev.txt
# ./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/xdev.txt

# ./test_final.sh data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt data/xtrain.txt
# ./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/xtrain.txt

# ./test_final.sh data/SEM-2012-SharedTask-CD-SCO-test-16032012/SEM-2012-SharedTask-CD-SCO-test-cardboard.txt data/run1/sys-cardboard.txt
# ./test_final.sh data/SEM-2012-SharedTask-CD-SCO-test-16032012/SEM-2012-SharedTask-CD-SCO-test-circle.txt data/run1/sys-circle.txt

# CLASSPATH=out/production/SEMST2012:src ./canonicalize_conll.groovy data/run1/sys-cardboard.txt data/sysc-cardboard.txt
# CLASSPATH=out/production/SEMST2012:src ./canonicalize_conll.groovy data/run1/sys-circle.txt data/sysc-circle.txt

# ./eval.cd-sco.pl -g data/run1/sys-cardboard.txt -s data/sysc-cardboard.txt
# ./eval.cd-sco.pl -g data/run1/sys-circle.txt -s data/sysc-circle.txt

#./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-test-GOLD-23032012/SEM-2012-SharedTask-CD-SCO-test-cardboard-GOLD.txt -s data/run1/sys-cardboard.txt
#./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-test-GOLD-23032012/SEM-2012-SharedTask-CD-SCO-test-circle-GOLD.txt -s data/run1/sys-circle.txt

#./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-test-GOLD-23032012/SEM-2012-SharedTask-CD-SCO-test-cardboard-GOLD.txt -s data/white-semst-submission-1-cardboard/closed/scope/sys-cardboard.txt
#./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-test-GOLD-23032012/SEM-2012-SharedTask-CD-SCO-test-circle-GOLD.txt -s data/white-semst-submission-1-circle/closed/scope/sys-circle.txt

#./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-test-GOLD-23032012/SEM-2012-SharedTask-CD-SCO-test-cardboard-GOLD.txt -s data/white-semst-submission-2-cardboard/closed/scope/sys-cardboard.txt
#./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-test-GOLD-23032012/SEM-2012-SharedTask-CD-SCO-test-circle-GOLD.txt -s data/white-semst-submission-2-circle/closed/scope/sys-circle.txt
