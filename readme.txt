Author:	Van Dang (vdang@cs.umass.edu)
Date:		July, 2012.
Version:	2.1
======================================

WHAT'S NEW
- Add ListNet.
- Add Random Forest.
- With little manual work, it can do BagBoo/Bagging LambaMART too.
- For my personal use only: add support for 
  (1) external relevance judgment file [-qrel]
  (2) output ranking in indri run file format (not exposed via cmd parameters) [-indri][requires doc-ID stored for each feature vector]
  (3) ignore ranked list without any relevant document [-hr]

========================
v 2.0
------
- Add MART
- Add LambdaMART
- Change the calculation of NDCG to the standard version: (2^{rel_i} - 1) / log_{2} (i+1). Therefore, the absolute NDCG score might be slightly lower than before.
- Add zscore normalization.
- Fix the divide-by-zero bug related to the sum normalization ( q->D={d1,d2,...,d_n}; F={f1,f2,...,fm}; fk_di = fk_di / sum_{dj \in D} |fk_dj| ).
(I do not claim that these normalization methods are good -- in fact, I think it's a better idea for you to normalize your own data using your favorate method)
- Add the ability to split the training file to x% train and (100-x)% validation (previous version only allows train/test split, not train/validation).
- Add some minor cmd-line parameters.
- Some cmd-line parameter string have been changed.
- Internal code clean up for slight improvement in efficiency/speed.

========================
v 1.2.1
------
- Fix the error with sparse train/test/validate file (with v 1.1, when we do not specify feature whose value is 0, the system crashes in some cases)
- Speedup RankNet using batch learning + add some tricks (see the LambdaRank paper for details).
- Change default epochs to 50 for RankNet.
- Fix a bug related to RankBoost not dealing properly with features whose values are negative.

========================
v 1.1
------
- Change data types in some classes to reduce the amount of memory use. Thus this version can work with larger dataset.
- Rearrange packages
- Change some functions' name

========================
v 1.0
------
This is the first version of RankLib.

======================================
1. OVERVIEW

RankLib is a library for comparing different ranking algorithms. In the current version:
- Algorithms: RankNet, RankBoost, AdaRank and Coordinate Ascent
- Training data: it allow users to:
   + Specify train/test data separately
   + Automatically does train/test split from a single input file
   + Do k-fold cross validation (only sequential split at the moment, NO RANDOM SPLIT)
   + Allow users to specify validation set to guide the training process. It will pick the model that performs best on the validation data instead of the one on the training data. This is useful for easily overfitted algorithms like RankNet.
- Evaluation metrics: MAP, NDCG@k, DCG@k, P@k, RR@k, BEST@k, ERR@k

===============================================================================================================================================
2. HOW TO USE

2.1. Binary
Usage: java -jar RankLib.jar <Params>
Params:
  [+] Training (+ tuning and evaluation)
	-train <file>		Training data
	-ranker <type>		Specify which ranking algorithm to use
				1: RankNet
				2: RankBoost
				3: AdaRank
				4: Coordinate Ascent
	[ -feature <file> ]	Feature description file: list features to be considered by the learner, each on a separate line
				If not specified, all features will be used.
	[ -metric2t <metric> ]	Metric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, BEST@k, ERR@k (default=ERR@10)
	[ -metric2T <metric> ]	Metric to evaluate on the test data (default to the same as specified for -metric2t)
	[ -tp <x \in [0..1]> ]	Set train-test split to be (x)(1.0-x)
	[ -kcv <k> ]		Specify if you want to perform k-fold cross validation using ONLY the specified training data (default=NoCV)
	[ -validate <file> ]	Specify if you want to tune your system on the validation data (default=unspecified)
				If specified, the final model will be the one that performs best on the validation data
	[ -test <file> ]	Specify if you want to evaluate the trained model on this data (default=unspecified)
	[ -norm ]		Normalize feature vectors (default=false)
	[ -save <model> ]	Save the learned model to the specified file (default=not-save)
	[ -silent ]		Do not print progress messages (which are printed by default)

    [-] RankNet-specific parameters
	[ -epoch <T> ]		The number of epochs to train (default=300)
	[ -layer <layer> ]	The number of hidden layers (default=1)
	[ -node <node> ]	The number of hidden nodes per layer (default=10)

    [-] RankBoost-specific parameters
	[ -round <T> ]		The number of rounds to train (default=300)
	[ -tc <k> ]		The number of threshold candidates to search (default=10)

    [-] AdaRank-specific parameters
	[ -round <T> ]		The number of rounds to train (default=500)
	[ -noeq ]		Train without enqueuing too-strong features (default=unspecified)
	[ -tolerance <t> ]	Tolerance between two consecutive rounds of learning (default=0.0020)
	[ -max ]		The maximum number of times can a feature be consecutively selected without changing performance (default=5)

    [-] Coordinate Ascent-specific parameters
	[ -r <k> ]		The number of random restarts (default=5)
	[ -i <iteration> ]	The number of iterations to search in each dimension (default=25)
	[ -tolerance <t> ]	Performance tolerance between two solutions (default=0.0010)
	[ -reg <slack> ]	Regularization parameter (default=no-regularization)

  [+] Testing previously saved models
	-load <model>		The model to load
	-test <file>		Test data to evaluate the model (specify either this or -rank but not both)
	-rank <file>		Rank the samples in the specified file (specify either this or -test but not both)
	[ -metric2T <metric> ]	Metric to evaluate on the test data (default=ERR@10)
	[ -idv ]		Print score on individual ranked lists in the specified test set
	[ -norm ]		Normalize feature vectors (default=false)

  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
  + NOTE: ALWAYS include -letor if you're doing experiments on LETOR 4.0 dataset.       +
  +       The reason is a relevance degree of 2 in the dataset is actually counted as 3 +
  +       (this is based on the evaluation script they provided). To be consistent      +
  +       with their numbers, this program will change 2 to 3 when it loads the data    +
  +       into memory if the -letor flag is specified.                                  +
  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

2.2. Build
An ant xml config. file is included. Make sure you have ant on your machine. Just type "ant" and you are good to go.

==================================================================
3. FILE FORMAT (TRAIN/TEST/VALIDATION)

The file format of the training and test and validation files is the same as for SVM-Rank (http://www.cs.cornell.edu/People/tj/svm_light/svm_rank.html). This is also the format used in the LETOR datasets. Each of the following lines represents one training example and is of the following format:

<line> .=. <target> qid:<qid> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
<target> .=. <float>
<qid> .=. <positive integer>
<feature> .=. <positive integer>
<value> .=. <float>
<info> .=. <string>

Here's an example: (taken from the SVM-Rank website). Note that everything after "#" are discarded.

3 qid:1 1:1 2:1 3:0 4:0.2 5:0 # 1A
2 qid:1 1:0 2:0 3:1 4:0.1 5:1 # 1B 
1 qid:1 1:0 2:1 3:0 4:0.4 5:0 # 1C
1 qid:1 1:0 2:0 3:1 4:0.3 5:0 # 1D  
1 qid:2 1:0 2:0 3:1 4:0.2 5:0 # 2A  
2 qid:2 1:1 2:0 3:1 4:0.4 5:0 # 2B 
1 qid:2 1:0 2:0 3:1 4:0.1 5:0 # 2C 
1 qid:2 1:0 2:0 3:1 4:0.2 5:0 # 2D  
2 qid:3 1:0 2:0 3:1 4:0.1 5:1 # 3A 
3 qid:3 1:1 2:1 3:0 4:0.3 5:0 # 3B 
4 qid:3 1:1 2:0 3:0 4:0.4 5:1 # 3C 
1 qid:3 1:0 2:1 3:1 4:0.5 5:0 # 3D


