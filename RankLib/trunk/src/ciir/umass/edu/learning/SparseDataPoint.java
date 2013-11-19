/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

public class SparseDataPoint extends DataPoint {

	public static void main(String[] args)
	{
		SparseDataPoint spd = new SparseDataPoint("3 qid:5 1:3");
		spd.zero = new short[]{1, 4, 6, 8, 10};
		System.out.println(spd.locate(11));
	}
	
	public short[] zero = null;
	public short locate(int fid)
	{
		if(zero == null)//this feature vector has no zero entries
			return 0;
		
		int l = 0;
		int r = zero.length-1;
		do{
			int m = (l+r)/2;
			if(fid > zero[m])
				l = m+1;
			else if(fid < zero[m])
				r = m-1;
			else //locate the missing feature
				return -1;
		}while(l <= r);
		if(r >= zero.length-1)
			return (short)zero.length;
		return (short)(r+1);
	}
	public SparseDataPoint(String text)
	{
		int nonZeroCount = parse(text);
		if(nonZeroCount < getFeatureCount())
		{
			zero = new short[getFeatureCount()-nonZeroCount];		
			float[] tmp = new float[nonZeroCount];
			short nzc = 0;
			short zc = 0;
			for(short fid=1;fid<fVals.length;fid++)
			{
				if(fVals[fid] != 0 && fVals[fid] != UNKNOWN)
					tmp[nzc++] = fVals[fid];
				else
					zero[zc++] = fid;
			}
			fVals = tmp;			
		}
		else
		{
			float[] tmp = new float[fVals.length-1];
			System.arraycopy(fVals, 1, tmp, 0, tmp.length);
			fVals = tmp;
		}
	}
	
	public float getFeatureValue(int fid)
	{
		if(fid <= 0 || fid > getFeatureCount())
		{
			System.out.println("Error in SparseDataPoint::getFeatureValue(): requesting invalid feature, fid=" + fid);
			System.exit(1);
		}
		short r = locate(fid);
		if(r == -1)
			return 0;
		return fVals[fid-1-r];
	}
	public void setFeatureValue(int fid, float fval) 
	{
		if(fid <= 0 || fid > getFeatureCount())
		{
			System.out.println("Error in SparseDataPoint::setFeatureValue(): feature (id=" + fid + ") not found.");
			System.exit(1);
		}
		short r = locate(fid);
		fVals[fid-1-r] = fval;
	}
	public String toString()
	{
		String output = label + " " + "id:" + id + " ";
		int j=0;
		for(int i=1;i<=getFeatureCount();i++)
		{
			if(zero[j] != i)
				output += i + ":" + fVals[i] + ((i==fVals.length-1)?"":" ");
			else
				j++;
		}
		output += " " + description;
		return output;
	}
	/**
	 * NOTE that @fVals only contains non-zero features
	 */
	public void setFeatureVector(float[] fVals)
	{
		this.fVals = fVals;
	}
	/**
	 * NOTE that the returned vector only contains non-zero features
	 */
	public float[] getFeatureVector()
	{
		return fVals;
	}
}