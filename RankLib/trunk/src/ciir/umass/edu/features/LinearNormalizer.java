package ciir.umass.edu.features;

import java.util.Arrays;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;

/**
 * @author Laura Dietz, vdang
 */
public class LinearNormalizer extends Normalizer {
	@Override
    public void normalize(RankList rl)
	{
		if(rl.size() == 0)
		{
			System.out.println("Error in LinearNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}
		int nFeature = rl.get(0).getFeatureCount();
        int[] fids = new int[nFeature];
        for(int i=1;i<=nFeature;i++)
        	fids[i] = i;
        normalize(rl, fids);
    }
	@Override
    public void normalize(RankList rl, int[] fids)
	{
		if(rl.size() == 0)
		{
			System.out.println("Error in LinearNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}
		
        float[] min = new float[fids.length];
        float[] max = new float[fids.length];
        Arrays.fill(min, 0);
        Arrays.fill(max, 0);
        for(int i=0;i<rl.size();i++)
        {
            DataPoint dp = rl.get(i);
            for(int j=0;j<fids.length;j++)
            {
                min[j] = Math.min(min[j],dp.getFeatureValue(fids[j]));
                max[j] = Math.max(max[j],dp.getFeatureValue(fids[j]));
            }
        }
        for(int i=0;i<rl.size();i++)
        {
            DataPoint dp = rl.get(i);
            for(int j=0;j<fids.length;j++)
            {
                float value = (dp.getFeatureValue(fids[j]) - min[j]) / (max[j] - min[j]);
                dp.setFeatureValue(fids[j], value);
            }
        }
    }
    public String name()
    {
        return "linear";
    }
}
