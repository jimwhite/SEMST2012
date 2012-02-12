Universe   = vanilla
Environment=LOGONROOT=~/Projects/logon;PATH=/usr/kerberos/bin:/usr/local/bin:/bin:/usr/bin:/opt/git/bin:/opt/scripts:/condor/bin
Executable  = /home2/jimwhite/Projects/gondor.school/SEMST2012/parse.sh
Arguments   = --binary --erg+tnt -best 1 --export --text /home2/jimwhite/Projects/gondor.school/SEMST2012/data/dev50_sentences.txt
Log         = parse.log
Output 		= parse.out
Error	    = parse.err
Queue
