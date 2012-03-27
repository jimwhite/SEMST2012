#!/bin/sh

mkdir data/SEM-2012-SharedTask-CD-SCO-09032012c

CLASSPATH=out/production/SEMST2012:src ./canonicalize_conll.groovy data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt data/SEM-2012-SharedTask-CD-SCO-09032012c/SEM-2012-SharedTask-CD-SCO-training-09032012.txt

CLASSPATH=out/production/SEMST2012:src ./canonicalize_conll.groovy data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt data/SEM-2012-SharedTask-CD-SCO-09032012c/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt

./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-training-09032012.txt -s data/SEM-2012-SharedTask-CD-SCO-09032012c/SEM-2012-SharedTask-CD-SCO-training-09032012.txt

./eval.cd-sco.pl -g data/SEM-2012-SharedTask-CD-SCO-09032012b/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/SEM-2012-SharedTask-CD-SCO-09032012c/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt

#CLASSPATH=out/production/SEMST2012:src  ./canonicalize_conll.groovy data/sys.train.conll.txt data/sysc.train.conll.txt
#CLASSPATH=out/production/SEMST2012:src  ./canonicalize_conll.groovy data/sys.dev.conll.txt data/sysc.dev.conll.txt
