/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

import java.util.Arrays;

/**
 * @author vdang
 * 
 * This class implements objects to be ranked. In the context of Information retrieval, each instance is a query-url pair represented by a n-dimentional feature vector.
 * It should be general enough for other ranking applications as well (not limited to just IR I hope). 
 */
public class DataPoint {
	public static float UNKNOWN = -1000000;
	public static int MAX_FEATURE = 51;
	public static int FEATURE_INCREASE = 10;
	
	public static int featureCount = 0;
	
	//attributes
	protected float label = 0.0f;//[ground truth] the real label of the data point (e.g. its degree of relevance according to the relevance judgment)
	protected String id = "";//id of this datapoint (e.g. query-id)
	protected float[] fVals = null;//fVals[0] is un-used. Feature id MUST start from 1
	protected String description = "";
	
	//internal to learning procedures
	protected double cached = -1.0;//the latest evaluation score of the learned model on this data point
	
	protected String getKey(String pair)
	{
		return pair.substring(0, pair.indexOf(":"));
	}
	protected String getValue(String pair)
	{
		return pair.substring(pair.lastIndexOf(":")+1);
	}	
	protected DataPoint()
	{		
	}
	public DataPoint(DataPoint dp)
	{
		label = dp.label;
		id = dp.id;
		description = dp.description;
		cached = dp.cached;
		fVals = new float[dp.fVals.length];
		System.arraycopy(dp.fVals, 0, fVals, 0, dp.fVals.length);
	}
	/**
	 * The input must have the form: 
	 * @param text
	 */
	public DataPoint(String text)
	{
		parse(text);
	}
	protected int parse(String text)
	{
		int nonZeroCount = 0;
		fVals = new float[MAX_FEATURE];
		Arrays.fill(fVals, UNKNOWN);
		int lastFeature = -1;
		try {
			int idx = text.indexOf("#");
			if(idx != -1)
			{
				description = text.substring(idx);
				text = text.substring(0, idx).trim();//remove the comment part at the end of the line
			}
			String[] fs = text.split(" ");
			label = Float.parseFloat(fs[0]);
			if(label < 0)
			{
				System.out.println("Relevance label cannot be negative. System will now exit.");
				System.exit(1);
			}
			id = getValue(fs[1]);
			String key = "";
			String val = "";
			for(int i=2;i<fs.length;i++)
			{
				key = getKey(fs[i]);
				val = getValue(fs[i]);
				int f = Integer.parseInt(key);
				if(f >= MAX_FEATURE)
				{
					while(f >= MAX_FEATURE)
						MAX_FEATURE += FEATURE_INCREASE;
					float[] tmp = new float [MAX_FEATURE];
					System.arraycopy(fVals, 0, tmp, 0, fVals.length);
					Arrays.fill(tmp, fVals.length, MAX_FEATURE, UNKNOWN);
					fVals = tmp;
				}
				fVals[f] = Float.parseFloat(val);
				if(fVals[f] != 0 && fVals[f] != UNKNOWN)
					nonZeroCount++;
				if(f > featureCount)//#feature will be the max_id observed
					featureCount = f;
				if(f > lastFeature)//note than lastFeature is the max_id observed for this current data point, whereas featureCount is the max_id observed on the entire dataset
					lastFeature = f;
			}
			//shrink fVals
			float[] tmp = new float[lastFeature+1];
			System.arraycopy(fVals, 0, tmp, 0, lastFeature+1);
			fVals = tmp;
		}
		catch(Exception ex)
		{
			System.out.println("Error in DataPoint::parse(): " + ex.toString());
			System.exit(1);
		}
		return nonZeroCount;
	}
	
	public String getID()
	{
		return id;
	}
	public void setID(String id)
	{
		this.id = id;
	}
	public float getLabel()
	{
		return label;
	}
	public void setLabel(float label)
	{
		this.label = label;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public void setCached(double c)
	{
		cached = c;
	}
	public double getCached()
	{
		return cached;

	}
	public void resetCached()
	{
		cached = -100000000.0f;;
	}
	
	public static int getFeatureCount()
	{
		return featureCount;
	}
	
	//Need overriding in sub-classes
	public float getFeatureValue(int fid)
	{
		if(fid <= 0 || fid >= fVals.length)
		{
			System.out.println("Error in DataPoint::getFeatureValue(): requesting unspecified feature, fid=" + fid);
			System.out.println("System will now exit.");
			System.exit(1);
		}
		if(fVals[fid] == UNKNOWN)//value for unspecified feature is 0
			return 0;
		return fVals[fid];
	}
	public void setFeatureValue(int fid, float fval) 
	{
		if(fid <= 0 || fid >= fVals.length)
		{
			System.out.println("Error in DataPoint::setFeatureValue(): feature (id=" + fid + ") not found.");
			System.exit(1);
		}
		fVals[fid] = fval;
	}	
	public String toString()
	{
		String output = ((int)label) + " " + "qid:" + id + " ";
		for(int i=1;i<fVals.length;i++)
			if(fVals[i] != UNKNOWN)
				output += i + ":" + fVals[i] + ((i==fVals.length-1)?"":" ");
		output += " " + description;
		return output;
	}
	public void setFeatureVector(float[] fVals)
	{
		this.fVals = fVals;
	}
	public float[] getFeatureVector()
	{
		return fVals;
	}
}