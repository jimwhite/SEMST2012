README.txt for UWashington's submission to *SEM 2012 shared task for resolving the scope of negation.  

There are lots of bits of code laying about but these instructions exercise the ones necessary to produce the reported results.  The mix of Bash and Groovy scripts don't help things either since things would be tidier if they were all in Groovy or Gradle.  The project files are for Intellij IDEA.

A few points that may be relevant:

The code is built specifically around the shared task and the bulk of the code is just doing file format munging.  The data format is based on CoNLL and so there are three task-specific columns: cue, scope, and event.  The system consists of three classifiers in a pipeline and they each take the CoNLL-style file as input then output a new one with their column filled in.  The top-level scripts are "train_final.sh" and "test_final.sh".  

The cue-finder is rather English-oriented and I would be surprised if it provides much help with Chinese.  Although its pattern-learning skeleton might be useful, this was the worst-performing part of the system (and almost the worst in the shared-task, but then it was written on about the last of the seven days ;-).  CRF worked so well for the other stages my thought is that it would probably work pretty well for cue-finding too.

The scope stage has tree-path features generated from the constituency parse supplied with the task data.  I suspect that will need some modification if you use a dependency parse.  That is computed in CoNLLDecode.path_to_cue (called from CoNLLDecode.tree_to_scope_sequence called from convert_conll_to_mallet.groovy) 

In revisiting this system I discovered that the code (CoNLLDecode.print_scope_sequence) that generates the "edge of constituent" features is actually a no-op because it tests for the wrong characters.  The problem is apparent in the MALLET training files for the scope classifier (data/train.scope.txt) where the b= and e= features, which are supposed to flag the edge (beginning or ending respectively) of a constituent, are always false.  I just tried "fixing" this code and find that on the dev set and one of test files I tried it performs slightly worse because even though scope token precision goes up slightly (f 84.85% to 85.31%) it winds up getting one less scope match in both cases (taking scope from f 76.43 down to 75.99).  Since these features make dev perform worse on dev, I would have not used them anyhow if I had found this bug before the shared task submission.  So actually the bug is in the documentation since the system report says I do use these features.  It were ever thus with documentation.

@InProceedings{white:2012:STARSEM-SEMEVAL,
  author    = {White, James Paul},
  title     = {UWashington: Negation Resolution using Machine Learning Methods},
  booktitle = {{*SEM 2012}: The First Joint Conference on Lexical and Computational Semantics -- Volume 1: Proceedings of the main conference and the shared task, and Volume 2: Proceedings of the Sixth International Workshop on Semantic Evaluation {(SemEval 2012)}},
  month     = {7-8 June},
  year      = {2012},
  address   = {Montr\'{e}al, Canada},
  publisher = {Association for Computational Linguistics},
  pages     = {335--339},
  url       = {http://www.aclweb.org/anthology/S12-1044}
}

0) Get the project source.

jim$ git clone https://github.com/jimwhite/SEMST2012.git

jim$ cd SEMST2012

1) Install Groovy and include it in your PATH.  I've used this code with Groovy 1.8.9 but not the 2.x versions yet.

http://groovy.codehaus.org

MacPorts should work fine, but I've not tried it for Groovy.

sudo port install groovy @1.8.9

2) Install MALLET.  MacPorts is a convenient way to do that.

jim$ sudo port install mallet
--->  Fetching archive for mallet
--->  Attempting to fetch mallet-2.0.7_0.darwin_12.noarch.tbz2 from http://packages.macports.org/mallet
--->  Attempting to fetch mallet-2.0.7_0.darwin_12.noarch.tbz2.rmd160 from http://packages.macports.org/mallet
--->  Installing mallet @2.0.7_0
--->  Activating mallet @2.0.7_0
--->  Cleaning mallet
--->  Updating database of binaries: 100.0%
--->  Scanning binaries for linking errors: 100.0%
--->  No broken files found.

jim$ find /opt/local/share -name "mallet*"
/opt/local/share/java/mallet-2.0.7
/opt/local/share/java/mallet-2.0.7/dist/mallet-deps.jar
/opt/local/share/java/mallet-2.0.7/dist/mallet.jar

The scripts are set up for MALLET v2.0.7.

3) Download and unpack the data from the shared task.  The download page is at http://www.clips.ua.ac.be/sem2012-st-neg/data.html .
jim$ cd data

jim$ wget http://www.clips.ua.ac.be/sem2012-st-neg/data/starsem-st-2012-data.tar.gz
--2013-07-12 16:35:39--  http://www.clips.ua.ac.be/sem2012-st-neg/data/starsem-st-2012-data.tar.gz
Resolving www.clips.ua.ac.be... 146.175.13.81
Connecting to www.clips.ua.ac.be|146.175.13.81|:80... connected.
HTTP request sent, awaiting response... 200 OK
Length: 4409729 (4.2M) [application/x-gzip]
Saving to: ‘starsem-st-2012-data.tar.gz’

100%[======================================================================================================>] 4,409,729    686KB/s   in 7.2s   

2013-07-12 16:35:47 (602 KB/s) - ‘starsem-st-2012-data.tar.gz’ saved [4409729/4409729]

jim$ tar xf starsem-st-2012-data.tar.gz 

jim$ find *
starsem-st-2012-data
starsem-st-2012-data/.DS_Store
starsem-st-2012-data/169-sem-st-paper.pdf
starsem-st-2012-data/cd-sco
starsem-st-2012-data/cd-sco/.DS_Store
starsem-st-2012-data/cd-sco/corpus
starsem-st-2012-data/cd-sco/corpus/.DS_Store
starsem-st-2012-data/cd-sco/corpus/dev
starsem-st-2012-data/cd-sco/corpus/dev/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt
starsem-st-2012-data/cd-sco/corpus/test
starsem-st-2012-data/cd-sco/corpus/test/SEM-2012-SharedTask-CD-SCO-test-cardboard.txt
starsem-st-2012-data/cd-sco/corpus/test/SEM-2012-SharedTask-CD-SCO-test-circle.txt
starsem-st-2012-data/cd-sco/corpus/test-gold
starsem-st-2012-data/cd-sco/corpus/test-gold/SEM-2012-SharedTask-CD-SCO-test-cardboard-GOLD.txt
starsem-st-2012-data/cd-sco/corpus/test-gold/SEM-2012-SharedTask-CD-SCO-test-circle-GOLD.txt
starsem-st-2012-data/cd-sco/corpus/training
starsem-st-2012-data/cd-sco/corpus/training/SEM-2012-SharedTask-CD-SCO-training-09032012.txt
starsem-st-2012-data/cd-sco/README
starsem-st-2012-data/cd-sco/src
starsem-st-2012-data/cd-sco/src/eval.cd-sco.pl
starsem-st-2012-data/pb-foc
starsem-st-2012-data/pb-foc/corpus
starsem-st-2012-data/pb-foc/corpus/merged
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-de.index
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-de.txt
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-te.index.rand
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-te.txt.rand
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-te.txt.rand.noFocus
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-tr.index
starsem-st-2012-data/pb-foc/corpus/SEM-2012-SharedTask-PB-FOC-tr.txt
starsem-st-2012-data/pb-foc/README
starsem-st-2012-data/pb-foc/src
starsem-st-2012-data/pb-foc/src/make_corpus.sh
starsem-st-2012-data/pb-foc/src/merge.py
starsem-st-2012-data/pb-foc/src/pb-foc_evaluation.py
starsem-st-2012-data/pb-foc/src/pretty_columns.py
starsem-st-2012-data/pb-foc/words
starsem-st-2012-data/pb-foc/words/.DS_Store
starsem-st-2012-data/README
starsem-st-2012-data.tar.gz

jim$ cd ..

4) Train the classifiers.  The train_final.sh script will do that and it has a script variable TRAINING_DATA to point at the appropriate file.  In the version in Github that is set to just the training file in the corpus.  For the final system reported in the shared task the train and dev files were pasted together to form a combined file for training.

jim$ ./train_final.sh

5) Run the system in the dev data.  The cue finder ignores any labels on the input so it can safely take the dev file (which is also a "gold" file) as-is.

jim$ ./test_final.sh data/starsem-st-2012-data/cd-sco/corpus/dev/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt data/sys-dev.txt

jim$ ./eval.cd-sco.pl -g data/starsem-st-2012-data/cd-sco/corpus/dev/SEM-2012-SharedTask-CD-SCO-dev-09032012.txt -s data/sys-dev.txt

---------------------------+------+--------+------+------+------+---------------+------------+---------
                            | gold | system | tp   | fp   | fn   | precision (%) | recall (%) | F1  (%) 
----------------------------+------+--------+------+------+------+---------------+------------+---------
Cues:                          173 |    163 |  152 |    8 |   21 |         95.00 |      87.86 |   91.29
Scopes(cue match):             168 |    155 |  107 |    5 |   61 |         95.54 |      63.69 |   76.43
Scopes(no cue match):          168 |    155 |  107 |    5 |   61 |         95.54 |      63.69 |   76.43
Scopes(no cue match, no punc): 168 |    155 |  107 |    5 |   61 |         95.54 |      63.69 |   76.43
Scope tokens(no cue match):   1368 |   1253 | 1109 |  137 |  259 |         89.00 |      81.07 |   84.85
Negated(no cue match):         122 |     83 |   50 |   20 |   59 |         71.43 |      45.87 |   55.87
Full negation:                 173 |    163 |   66 |    8 |  107 |         89.19 |      38.15 |   53.44
---------------------------+------+--------+------+------+------+---------------+------------+---------
 # sentences: 787
 # negation sentences: 144
 # negation sentences with errors: 94
 % correct sentences: 87.29
 % correct negation sentences: 34.72
--------------------------------------------------------------------------------------------------------

6) Run the system on the test data.  Again, the system reports are based on combining the "cardboard" and "circle" datasets into one.  Note that the training data is needed here because the cue finder trains its classifier when loaded rather than storing a model file.  

jim$ ./test_final.sh data/starsem-st-2012-data/cd-sco/corpus/test/SEM-2012-SharedTask-CD-SCO-test-cardboard.txt data/sys-cardboard.txt

jim$ ./eval.cd-sco.pl -g data/starsem-st-2012-data/cd-sco/corpus/test-gold/SEM-2012-SharedTask-CD-SCO-test-cardboard-GOLD.txt -s data/sys-cardboard.txt 

---------------------------+------+--------+------+------+------+---------------+------------+---------
                            | gold | system | tp   | fp   | fn   | precision (%) | recall (%) | F1  (%) 
----------------------------+------+--------+------+------+------+---------------+------------+---------
Cues:                          133 |    135 |  121 |   13 |   12 |         90.30 |      90.98 |   90.64
Scopes(cue match):             128 |    130 |   85 |   13 |   43 |         86.73 |      66.41 |   75.22
Scopes(no cue match):          128 |    130 |   85 |   13 |   42 |         86.87 |      67.19 |   75.77
Scopes(no cue match, no punc): 128 |    130 |   85 |   13 |   42 |         86.87 |      67.19 |   75.77
Scope tokens(no cue match):    963 |    937 |  811 |  126 |  152 |         86.55 |      84.22 |   85.37
Negated(no cue match):          87 |     79 |   44 |   33 |   39 |         57.14 |      53.01 |   55.00
Full negation:                 133 |    135 |   50 |   13 |   83 |         79.37 |      37.59 |   51.02
---------------------------+------+--------+------+------+------+---------------+------------+---------
 # sentences: 496
 # negation sentences: 119
 # negation sentences with errors: 76
 % correct sentences: 83.06
 % correct negation sentences: 36.13
--------------------------------------------------------------------------------------------------------
