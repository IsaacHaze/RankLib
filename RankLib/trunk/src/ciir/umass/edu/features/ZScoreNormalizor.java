/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.features;

import java.util.Arrays;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;

/**
 * @author vdang
 */
public class ZScoreNormalizor extends Normalizer {
	@Override
	public void normalize(RankList rl) {
		if(rl.size() == 0)
		{
			System.out.println("Error in SumNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}
		int nFeature = DataPoint.getFeatureCount();
		float[] mean = new float[nFeature];
		Arrays.fill(mean, 0);
		for(int i=0;i<rl.size();i++)
		{
			DataPoint dp = rl.get(i);
			for(int j=1;j<=nFeature;j++)
				mean[j-1] += dp.getFeatureValue(j);
		}
		
		for(int j=1;j<=nFeature;j++)
		{
			mean[j-1] = mean[j-1] / rl.size();
			float std = 0;
			for(int i=0;i<rl.size();i++)
			{
				DataPoint p = rl.get(i);
				float x = p.getFeatureValue(j) - mean[j-1];
				std += x*x;
			}
			std = (float) Math.sqrt(std / (rl.size()-1));
			//normalize
			if(std > 0)
			{
				for(int i=0;i<rl.size();i++)
				{
					DataPoint p = rl.get(i);
					float x = (p.getFeatureValue(j) - mean[j-1])/std;//x ~ standard normal (0, 1)
					p.setFeatureValue(j, x);
				}
			}
		}
	}
	@Override
	public void normalize(RankList rl, int[] fids) {
		if(rl.size() == 0)
		{
			System.out.println("Error in SumNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}
		float[] mean = new float[fids.length];
		Arrays.fill(mean, 0);
		for(int i=0;i<rl.size();i++)
		{
			DataPoint dp = rl.get(i);
			for(int j=0;j<fids.length;j++)
				mean[j] += dp.getFeatureValue(fids[j]);
		}
		
		for(int j=0;j<fids.length;j++)
		{
			mean[j] = mean[j] / rl.size();
			float std = 0;
			for(int i=0;i<rl.size();i++)
			{
				DataPoint p = rl.get(i);
				float x = p.getFeatureValue(fids[j]) - mean[j];
				std += x*x;
			}
			std = (float) Math.sqrt(std / (rl.size()-1));
			//normalize
			if(std > 0.0)
			{
				for(int i=0;i<rl.size();i++)
				{
					DataPoint p = rl.get(i);
					float x = (p.getFeatureValue(fids[j]) - mean[j])/std;//x ~ standard normal (0, 1)
					p.setFeatureValue(fids[j], x);
				}
			}
		}
	}
	public String name()
	{
		return "zscore";
	}
}
