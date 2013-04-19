package ciir.umass.edu.eval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import ciir.umass.edu.stats.RandomPermutationTest;
import ciir.umass.edu.utilities.FileUtils;
import ciir.umass.edu.utilities.SimpleMath;

public class Analyzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Analyzer a = new Analyzer();
		List<String> l = new ArrayList<String>();
		//a.compare("output/", "ca.feature.base");
		
		HashMap<String, Double> m1 = a.read("output/ca.feature.base");
		HashMap<String, Double> m2 = a.read("output/ca.feature.base.everything");
		RandomPermutationTest randomizedTest = new RandomPermutationTest();
		System.out.println("p-value = " + randomizedTest.test(m1, m2));
	}

	class Result {
		int status = 0;//success
		int win = 0;
		int loss = 0;
		int[] countByImpRange = null;
	}
	
	private static double[] improvementRatioThreshold = new double[]{-1, -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1000};
	private int zero = 4;
	public int locateSegment(double value)
	{
		if(value > 0)
		{
			for(int i=zero;i<improvementRatioThreshold.length;i++)
				if(value <= improvementRatioThreshold[i])
					return i;
		}
		else if(value < 0)
		{
			for(int i=0;i<=zero;i++)
				if(value < improvementRatioThreshold[i])
					return i;
		}
		return -1;
	}
	
	public HashMap<String, Double> read(String filename)
	{
		HashMap<String, Double> performance = new HashMap<String, Double>();		
		try {
			String content = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			while((content = in.readLine()) != null)
			{
				content = content.trim();
				if(content.length() == 0)
					continue;
				
				//expecting: id [space]* metric-text [space]* performance
				while(content.indexOf("  ") != -1)
					content = content.replace("  ", " ");
				content = content.replace(" ", "\t");
				String[] s = content.split("\t");
				String measure = s[0];
				String id = s[1];
				double p = Double.parseDouble(s[2]);
				performance.put(id, p);
			}
			in.close();
			System.out.println("Reading " + filename + "... " + performance.size() + " ranked lists [Done]");
		}
		catch(Exception ex)
		{
			System.out.println("Error in Analyzer::read(): " + ex.toString());
			System.exit(1);
		}
		return performance;
	}
	
	public void compare(String directory, String baseFile)
	{
		directory = FileUtils.makePathStandard(directory);
		List<String> targets = FileUtils.getAllFiles2(directory);
		for(int i=0;i<targets.size();i++)
		{
			if(targets.get(i).compareTo(baseFile) == 0)
			{
				targets.remove(i);
				i--;
			}
			else
				targets.set(i, directory+targets.get(i));
		}
		compare(directory+baseFile, targets);
	}
	public void compare(String baseFile, List<String> targetFiles)
	{
		HashMap<String, Double> base = read(baseFile);
		List<HashMap<String, Double>> targets = new ArrayList<HashMap<String, Double>>();
		for(int i=0;i<targetFiles.size();i++)
		{
			HashMap<String, Double> hm = read(targetFiles.get(i));
			targets.add(hm);
		}
		Result[] rs = compare(base, targets);
		
		System.out.println("");
		System.out.println("System\tPerformance\tImprovement\tWin\tLoss");
		System.out.println(FileUtils.getFileName(baseFile) + " [baseline]\t" + SimpleMath.round(base.get("all").doubleValue(), 4));
		for(int i=0;i<rs.length;i++)
		{
			if(rs[i].status == 0)
			{
				double delta = targets.get(i).get("all").doubleValue() - base.get("all").doubleValue();
				String msg = FileUtils.getFileName(targetFiles.get(i)) + "\t" + SimpleMath.round(targets.get(i).get("all").doubleValue(), 4) + "\t" + ((delta>0)?"+":"") + SimpleMath.round(delta, 4) + "\t" + rs[i].win + "\t" + rs[i].loss;
				System.out.println(msg);
			}
			else
				System.out.println("[" + targetFiles.get(i) + "] skipped: contains different ranked lists (ids) compared to the baseline");
		}
		
		/*for(int i=0;i<targets.size();i++)
		{
			double delta = targets.get(i).get("all").doubleValue() - base.get("all").doubleValue();
			System.out.println(delta);
			System.out.println("Win/Loss: " + win[i] + "/" + loss[i]);
			//for(int j=0;j<changes[i].length;j++)
				//System.out.print(changes[i][j] + "\t");
			//System.out.println("");
			System.out.println("-----------------------------------------------");
		}*/
	}
	
	public Result compare(HashMap<String, Double> base, HashMap<String, Double> target)
	{
		Result r = new Result();
		if(base.size() != target.size())
		{
			r.status = -1;
			return r;
		}
		
		r.countByImpRange = new int[improvementRatioThreshold.length];
		Arrays.fill(r.countByImpRange, 0);
		for(String key: base.keySet())
		{
			if(!target.containsKey(key))
			{
				r.status = -2;
				return r;
			}
			if(key.compareTo("all") == 0)
				continue;
			double p = base.get(key).doubleValue();
			double pt = target.get(key).doubleValue();
			if(pt > p)
				r.win++;
			else if(pt < p)
				r.loss++;
			double change = pt - p;
			if(change != 0)
				r.countByImpRange[locateSegment(change)]++;
		}
		return r;
	}
	public Result[] compare(HashMap<String, Double> base, List<HashMap<String, Double>> targets)
	{
		//comparative statistics
		Result[] rs = new Result[targets.size()];
		for(int i=0;i<targets.size();i++)
			rs[i] = compare(base, targets.get(i));			
		return rs;
	}
}
