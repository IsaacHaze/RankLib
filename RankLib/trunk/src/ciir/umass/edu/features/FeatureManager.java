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
			System.out.println("Usage: java -cp bin/RankLib.jar ciir.umass.edu.feature.FeatureManager <Params>");
			System.out.println("Params:");
			System.out.println("\t-input <file>\t\tSource data (ranked lists)");
			System.out.println("\t-k <fold>\t\tThe number of folds");
			System.out.println("\t-output <dir>\t\tThe output directory");
			System.out.println("\t[ -shuffle] \t\tShuffle the order of the input ranked lists prior to k-fold data generation");
			System.out.println("\t[ -tvs <x \\in [0..1]> ] Train-validation split ratio (x)(1.0-x)");
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
		
		List<RankList> samples = FeatureManager.split(rankingFiles, shuffle, nFold, tvs, outputDir);
		if(shuffle && rankingFiles.size() > 0)
			FeatureManager.save(samples, outputDir + rankingFiles.get(0) + ".shuffled");
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
			RankList rl = new RankList();
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
					qp = new DataPoint(content);

				if(lastID.compareTo("")!=0 && lastID.compareTo(qp.getID())!=0)
				{
					if(!mustHaveRelDoc || hasRel)
						samples.add(rl);
					rl = new RankList();
					hasRel = false;
				}
				
				if(qp.getLabel() > 0)
					hasRel = true;
				lastID = qp.getID();
				rl.add(qp);
				countEntries++;
			}
			if(rl.size() > 0 && (!mustHaveRelDoc || hasRel))
				samples.add(rl);
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
	 * Read features specified in an input feature file. 
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
	 * Generate k-fold training and test data from a single input sample file.
	 * @param rankingFile
	 * @param shuffle Whether to shuffle the order of the ranked list before splitting
	 * @param nFold
	 * @param outputDir
	 * @return The input ranking (might be shuffled if shuffle=true) for k-fold data generation.
	 */
	public static List<RankList> split(String rankingFile, boolean shuffle, int nFold, String outputDir)
	{
		return split(rankingFile, shuffle, nFold, -1, outputDir);
	}
	/**
	 * Generate k-fold training, validation and test data from a single input sample file.
	 * @param rankingFile
	 * @param shuffle Whether to shuffle the order of the ranked list before splitting
	 * @param nFold
	 * @param tvs Train/validation split ratio
	 * @param outputDir
	 * @return The input ranking (might be shuffled if shuffle=true) for k-fold data generation.
	 */
	public static List<RankList> split(String rankingFile, boolean shuffle, int nFold, float tvs, String outputDir)
	{
		List<String> l = new ArrayList<String>();
		l.add(rankingFile);
		return split(l, shuffle, nFold, tvs, outputDir);		
	}
	/**
	 * Generate k-fold training and test data from multiple input files. All input ranked lists are merged into one 
	 * before the k-fold data generation begins.
	 * @param rankingFiles
	 * @param shuffle Whether to shuffle the order of the ranked list before splitting
	 * @param nFold
	 * @param outputDir
	 * @return The merged input ranking (might be shuffled if shuffle=true) for k-fold data generation.
	 */
	public static List<RankList> split(List<String> rankingFiles, boolean shuffle, int nFold, String outputDir)
	{
		return split(rankingFiles, shuffle, nFold, -1, outputDir);
	}
	/**
	 * Generate k-fold training, validation and test data from multiple input files. All input ranked lists are merged into one 
	 * before the k-fold data generation begins.
	 * @param rankingFiles
	 * @param shuffle Whether to shuffle the order of the ranked list before splitting
	 * @param nFold
	 * @param tvs Train/validation split ratio
	 * @param outputDir
	 * @return The merged input ranking (might be shuffled if shuffle=true) for k-fold data generation.
	 */
	public static List<RankList> split(List<String> rankingFiles, boolean shuffle, int nFold, float tvs, String outputDir)
	{
		outputDir = FileUtils.makePathStandard(outputDir);
		List<RankList> samples = readInput(rankingFiles);
		if(shuffle)
			Collections.shuffle(samples);
		
		String outputFN = FileUtils.getFileName(rankingFiles.get(0));
		List<List<RankList>> trains = new ArrayList<List<RankList>>();
		List<List<RankList>> tests = new ArrayList<List<RankList>>();
		List<List<RankList>> valis = new ArrayList<List<RankList>>();
		prepareCV(samples, nFold, tvs, trains, valis, tests);
		try{
			System.out.print("Saving... ");
			for(int i=0;i<trains.size();i++)
			{
				save(trains.get(i), outputDir + "f" + (i+1) + ".train." + outputFN);
				save(tests.get(i), outputDir + "f" + (i+1) + ".test." + outputFN);
				if(tvs > 0)
					save(valis.get(i), outputDir + "f" + (i+1) + ".validation." + outputFN);
			}
			System.out.println("[Done]");
		}
		catch(Exception ex)
		{
			System.out.println("Error in FeatureManager::split(): " + ex.toString());
			System.exit(1);
		}
		return samples;
	}
	
	/**
	 * Obtain all features present in a sample set. 
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
		DataPoint dp = samples.get(0).get(0);
		int fc = dp.getFeatureCount();
		int[] features = new int[fc];
		for(int i=0;i<fc;i++)
			features[i] = i+1;
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
		/*int[][] folds = new int[][]{
				{102, 106, 124, 134, 159, 160, 116, 139, 133},
				{107, 117, 168, 121, 120, 123, 114, 146, 138},
				{110, 149, 158, 118, 167, 103, 108, 164},
				{122, 136, 111, 151, 155, 104, 119, 170},
				{154, 162, 153, 115, 112, 157, 113, 109}
		};
		
		List<List<Integer>> trainSamplesIdx = new ArrayList<List<Integer>>();
		for(int f=0;f<nFold;f++)
			trainSamplesIdx.add(new ArrayList<Integer>());
		
		for(int i=0;i<samples.size();i++)
		{
			int qid = Integer.parseInt(samples.get(i).getID());
			int f = -1;
			for(int j=0;j<folds.length&&f==-1;j++)
			{
				for(int k=0;k<folds[j].length&&f==-1;k++)
					if(qid == folds[j][k])
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
