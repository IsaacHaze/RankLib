/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.features;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.SparseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.utilities.FileUtils;

public class FeatureManager {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		List<String> rankingFiles = new ArrayList<String>();
		String outputDir = "";
		boolean shuffle = false;
		int nFold = 0;
		float tvs = -1;//train-validation split in each fold
		
		if(args.length < 3)
		{
			System.out.println("Usage: java -cp bin/RankLib.jar ciir.umass.edu.features.FeatureManager <Params>");
			System.out.println("Params:");
			System.out.println("\t-input <file>\t\tSource data (ranked lists)");
			System.out.println("\t-output <dir>\t\tThe output directory");
			
			System.out.println("");
			System.out.println("  [+] Shuffling");
			System.out.println("\t-shuffle\t\tCreate a copy of the input file in which the ordering of all ranked lists (e.g. queries) is randomized.");
			System.out.println("\t\t\t\t(the order among objects (e.g. documents) within each ranked list is certainly unchanged).");
			
			//System.out.println("");
			System.out.println("  [+] k-fold Partitioning (sequential split)");
			System.out.println("\t-k <fold>\t\tThe number of folds");
			System.out.println("\t[ -tvs <x \\in [0..1]> ] Train-validation split ratio (x)(1.0-x)");
			
			System.out.println("");
			System.out.println("  NOTE: If both -shuffle and -k are specified, the input data will be shuffled and then sequentially partitioned.");
			System.out.println("");
			return;
		}
		
		for(int i=0;i<args.length;i++)
		{
			if(args[i].compareTo("-input")==0)
				rankingFiles.add(args[++i]);
			else if(args[i].compareTo("-k")==0)
				nFold = Integer.parseInt(args[++i]);
			else if(args[i].compareTo("-shuffle")==0)
				shuffle = true;
			else if(args[i].compareTo("-tvs")==0)
				tvs = Float.parseFloat(args[++i]);
			else if(args[i].compareTo("-output")==0)
				outputDir = FileUtils.makePathStandard(args[++i]);
		}		
	
		if(shuffle || nFold > 0)
		{
			List<RankList> samples = readInput(rankingFiles);
			if(samples.size() == 0)
			{
				System.out.println("Error: The input file is empty.");
				return;
			}
			
			String fn = FileUtils.getFileName(rankingFiles.get(0));
			if(shuffle)
			{
				fn +=  ".shuffled";
				System.out.print("Shuffling... ");
				Collections.shuffle(samples);
				System.out.println("[Done]");
				System.out.print("Saving... ");
				FeatureManager.save(samples, outputDir + fn);
				System.out.println("[Done]");
			}
			if(nFold > 0)
			{
				List<List<RankList>> trains = new ArrayList<List<RankList>>();
				List<List<RankList>> tests = new ArrayList<List<RankList>>();
				List<List<RankList>> valis = new ArrayList<List<RankList>>();
				System.out.println("Partitioning... ");
				prepareCV(samples, nFold, tvs, trains, valis, tests);
				System.out.println("[Done]");
				try{
					for(int i=0;i<trains.size();i++)
					{
						System.out.print("Saving fold " + (i+1) + "/" + nFold + "... ");
						save(trains.get(i), outputDir + "f" + (i+1) + ".train." + fn);
						save(tests.get(i), outputDir + "f" + (i+1) + ".test." + fn);
						if(tvs > 0)
							save(valis.get(i), outputDir + "f" + (i+1) + ".validation." + fn);
						System.out.println("[Done]");
					}					
				}
				catch(Exception ex)
				{
					System.out.println("Error: Cannot save partition data.");				
					System.out.println("Occured in FeatureManager::main(): " + ex.toString());
				}
			}
		}
	}
	
	/**
	 * Read a set of rankings from a single file.
	 * @param inputFile
	 * @return
	 */
	public static List<RankList> readInput(String inputFile)
	{
		return readInput(inputFile, false, false);
	}
	/**
	 * Read a set of rankings from a single file.
	 * @param inputFile
	 * @param mustHaveRelDoc
	 * @param useSparseRepresentation
	 * @return
	 */
	public static List<RankList> readInput(String inputFile, boolean mustHaveRelDoc, boolean useSparseRepresentation)	
	{
		List<RankList> samples = new ArrayList<RankList>();
		int countRL = 0;
		int countEntries = 0;
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "ASCII"));
			
			String lastID = "";
			boolean hasRel = false;
			List<DataPoint> rl = new ArrayList<DataPoint>();
			while((content = in.readLine()) != null)
			{
				content = content.trim();
				if(content.length() == 0)
					continue;
				if(content.indexOf("#")==0)
					continue;
				
				if(countEntries % 10000 == 0)
					System.out.print("\rReading feature file [" + inputFile + "]: " + countRL + "... ");
				
				DataPoint qp = null;
				if(useSparseRepresentation)
					qp = new SparseDataPoint(content);
				else
					qp = new DenseDataPoint(content);

				if(lastID.compareTo("")!=0 && lastID.compareTo(qp.getID())!=0)
				{
					if(!mustHaveRelDoc || hasRel)
						samples.add(new RankList(rl));
					rl = new ArrayList<DataPoint>();
					hasRel = false;
				}
				
				if(qp.getLabel() > 0)
					hasRel = true;
				lastID = qp.getID();
				rl.add(qp);
				countEntries++;
			}
			if(rl.size() > 0 && (!mustHaveRelDoc || hasRel))
				samples.add(new RankList(rl));
			in.close();
			System.out.println("\rReading feature file [" + inputFile + "]... [Done.]            ");
			System.out.println("(" + samples.size() + " ranked lists, " + countEntries + " entries read)");
		}
		catch(Exception ex)
		{
			System.out.println("Error in FeatureManager::readInput(): " + ex.toString());
			System.exit(1);
		}
		return samples;
	}
	/**
	 * Read sets of rankings from multiple files. Then merge them altogether into a single ranking.
	 * @param inputFiles
	 * @return
	 */
	public static List<RankList> readInput(List<String> inputFiles)	
	{
		List<RankList> samples = new ArrayList<RankList>();
		for(int i=0;i<inputFiles.size();i++)
		{
			List<RankList> s = readInput(inputFiles.get(i), false, false);
			samples.addAll(s);
		}
		return samples;
	}
	/**
	 * Read features specified in an input feature file. Expecting one feature per line. 
	 * @param featureDefFile
	 * @return
	 */
	public static int[] readFeature(String featureDefFile)
	{
		int[] features = null;
		List<String> fids = new ArrayList<String>();
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(featureDefFile)));			
			while((content = in.readLine()) != null)
			{
				content = content.trim();
				if(content.length() == 0)
					continue;
				if(content.indexOf("#")==0)
					continue;				
				fids.add(content.split("\t")[0].trim());
			}
			in.close();
			features = new int[fids.size()];
			for(int i=0;i<fids.size();i++)
				features[i] = Integer.parseInt(fids.get(i));
		}
		catch(Exception ex)
		{
			System.out.println("Error in FeatureManager::readFeature(): " + ex.toString());
			System.exit(1);
		}
		return features;
	}
	/**
	 * Obtain all features present in a sample set. 
	 * Important: If your data (DataPoint objects) is loaded by RankLib (e.g. command-line use) or its APIs, there is nothing to watch out for.
	 *            If you create the DataPoint objects yourself, make sure DataPoint.featureCount correctly reflects the total number features present in your dataset.
	 * @param samples
	 * @return
	 */
	public static int[] getFeatureFromSampleVector(List<RankList> samples)
	{
		if(samples.size() == 0)
		{
			System.out.println("Error in FeatureManager::getFeatureFromSampleVector(): There are no training samples.");
			System.exit(1);
		}
		int fc = DataPoint.getFeatureCount();
		int[] features = new int[fc];
		for(int i=1;i<=fc;i++)
			features[i-1] = i;
		return features;
	}
	/**
	 * Split the input sample set into k chunks (folds) of roughly equal size and create train/test data for each fold.
	 * Note that NO randomization is done. If you want to randomly split the data, make sure that you randomize the order 
	 * in the input samples prior to calling this function. 
	 * @param samples
	 * @param nFold
	 * @param trainingData
	 * @param testData
	 */
	public static void prepareCV(List<RankList> samples, int nFold, List<List<RankList>> trainingData, List<List<RankList>> testData)
	{
		prepareCV(samples, nFold, -1, trainingData, null, testData);
	}
	/**
	 * Split the input sample set into k chunks (folds) of roughly equal size and create train/test data for each fold. Then it further splits
	 * the training data in each fold into train and validation. Note that NO randomization is done. If you want to randomly split the data,  
	 * make sure that you randomize the order in the input samples prior to calling this function. 
	 * @param samples
	 * @param nFold
	 * @param tvs Train/validation split ratio
	 * @param trainingData
	 * @param validationData
	 * @param testData
	 */
	public static void prepareCV(List<RankList> samples, int nFold, float tvs, List<List<RankList>> trainingData, List<List<RankList>> validationData, List<List<RankList>> testData)
	{
		/*int[][] testQueries  = new int[][]{
			{67, 125, 78, 96, 174, 112, 62, 92, 19, 181, 89, 126, 104, 111, 41, 170, 131, 59, 162, 83, 58, 70, 161, 4, 184, 80, 109, 132, 168, 49, 197, 50, 176, 200, 53, 103, 199, 25, 94, 66},
			{13, 124, 55, 180, 65, 105, 31, 24, 163, 123, 33, 159, 101, 36, 149, 127, 142, 22, 143, 7, 68, 69, 15, 147, 57, 196, 23, 77, 1, 2, 106, 5, 64, 137, 14, 86, 73, 183, 114, 18},
			{54, 63, 9, 17, 182, 44, 29, 186, 128, 99, 135, 167, 139, 133, 146, 82, 34, 141, 154, 194, 16, 140, 26, 75, 190, 173, 93, 179, 6, 11, 28, 38, 189, 193, 51, 171, 40, 3, 90, 20},
			{178, 98, 130, 37, 172, 165, 85, 122, 115, 117, 153, 46, 30, 152, 138, 79, 81, 95, 91, 187, 100, 110, 56, 169, 175, 157, 87, 160, 43, 47, 88, 27, 155, 195, 129, 45, 21, 145, 8, 121},
			{116, 166, 32, 60, 61, 52, 118, 177, 72, 156, 76, 108, 151, 71, 35, 150, 113, 164, 107, 134, 48, 10, 12, 84, 188, 39, 158, 191, 102, 74, 185, 136, 119, 42, 97, 198, 192, 148, 144, 120}
			 };
		
		List<List<Integer>> trainSamplesIdx = new ArrayList<List<Integer>>();
		for(int f=0;f<nFold;f++)
			trainSamplesIdx.add(new ArrayList<Integer>());
		
		for(int i=0;i<samples.size();i++)
		{
			int qid = Integer.parseInt(samples.get(i).getID());
			int f = -1;
			for(int j=0;j<testQueries.length&&f==-1;j++)
			{
				for(int k=0;k<testQueries[j].length&&f==-1;k++)
					if(qid == testQueries[j][k])
						f = j;
			}
			if(f==-1)
			{
				System.out.println("Error: qid=" + qid);
				System.exit(1);
			}
			trainSamplesIdx.get(f).add(i);
		}*/
		
		List<List<Integer>> trainSamplesIdx = new ArrayList<List<Integer>>();
		int size = samples.size()/nFold;
		int start = 0;
		int total = 0;
		for(int f=0;f<nFold;f++)
		{
			List<Integer> t = new ArrayList<Integer>();
			for(int i=0;i<size && start+i<samples.size();i++)
				t.add(start+i);
			trainSamplesIdx.add(t);
			total += t.size();
			start += size;
		}		
		for(;total<samples.size();total++)
			trainSamplesIdx.get(trainSamplesIdx.size()-1).add(total);
		
		for(int i=0;i<trainSamplesIdx.size();i++)
		{
			System.out.print("\rCreating data for fold-" + (i+1) + "...");
			List<RankList> train = new ArrayList<RankList>();
			List<RankList> test = new ArrayList<RankList>();
			List<RankList> vali = new ArrayList<RankList>();
			//train-test split
			List<Integer> t = trainSamplesIdx.get(i);
			for(int j=0;j<samples.size();j++)
			{
				if(t.contains(j))
					test.add(new RankList(samples.get(j)));
				else
					train.add(new RankList(samples.get(j)));				
			}
			//train-validation split if specified
			if(tvs > 0)
			{
				int validationSize = (int)(train.size()*(1.0-tvs));
				for(int j=0;j<validationSize;j++)
				{
					vali.add(train.get(train.size()-1));
					train.remove(train.size()-1);
				}
			}
			//save them 
			trainingData.add(train);
			testData.add(test);
			if(tvs > 0)
				validationData.add(vali);
		}
		System.out.println("\rCreating data for " + nFold + " folds... [Done]            ");
	}
	/**
	 * Split the input sample set into 2 chunks: one for training and one for either validation or testing
	 * @param sampleFile
	 * @param featureDefFile
	 * @param percentTrain The percentage of data used for training
	 * @param trainingData
	 * @param testData
	 */
	public static void prepareSplit(List<RankList> samples, double percentTrain, List<RankList> trainingData, List<RankList> testData)
	{
		int size = (int) (samples.size() * percentTrain);
		for(int i=0; i<size; i++)
			trainingData.add(new RankList(samples.get(i)));
		for(int i=size; i<samples.size(); i++)
			testData.add(new RankList(samples.get(i)));
	}	
	/**
	 * Save a sample set to file
	 * @param samples
	 * @param outputFile
	 */
	public static void save(List<RankList> samples, String outputFile)
	{
		try{
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
			for(int j=0;j<samples.size();j++)
				save(samples.get(j), out);
			out.close();	
		}
		catch(Exception ex)
		{
			System.out.println("Error in FeatureManager::save(): " + ex.toString());
			System.exit(1);
		}
	}
	/**
	 * Write a ranked list to a file object.
	 * @param r
	 * @param out
	 * @throws Exception
	 */
	private static void save(RankList r, BufferedWriter out) throws Exception
	{
		for(int j=0;j<r.size();j++)
		{
			out.write(r.get(j).toString());
			out.newLine();
		}
	}
}
