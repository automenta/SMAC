package ca.ubc.cs.beta.smac.matlab.helper;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.configspace.ParamFileHelper;
import ca.ubc.cs.beta.aclib.misc.math.ArrayMathOps;
import ca.ubc.cs.beta.aclib.misc.random.SeedableRandomSingleton;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.seedgenerator.RandomInstanceSeedGenerator;
import ca.ubc.cs.beta.models.fastrf.RandomForest;
import ca.ubc.cs.beta.models.fastrf.RegtreeBuildParams;
import ec.util.MersenneTwister;

public class StaticMethodWrapper implements Serializable{

	private static InstanceSeedGenerator instanceSeedGenerator;
	private static int instanceCount = 0;
	
	
	
	public ParamConfiguration getRandomConfiguration(ParamConfigurationSpace p)
	{
	
		return p.getRandomConfiguration();
	}
	
	public ParamConfigurationSpace getParamFileParser(String s, long seedForRandomSampling)
	{
		return ParamFileHelper.getParamFileParser(s, seedForRandomSampling);
	}
	
	public ParamConfigurationSpace getParamFileParser(File f, long seedForRandomSampling)
	{
		return ParamFileHelper.getParamFileParser(f, seedForRandomSampling);
	}
	
	public ParamConfiguration fromString(ParamConfigurationSpace configSpace, String paramString)
	{
		return configSpace.getConfigurationFromString(paramString, ParamConfiguration.StringFormat.STATEFILE_SYNTAX);
	}
	public void reinitSeed()
	{
		ParamFileHelper.clear();
		SeedableRandomSingleton.reinit();
	}
	
	public void setSeedOrInit(long seed)
	{
		ParamFileHelper.clear();
		SeedableRandomSingleton.setSeed(seed);
		
		SeedableRandomSingleton.reinit();
		if(seed != SeedableRandomSingleton.getSeed())
		{
			throw new IllegalStateException("Seed could not be changed, perhaps you need to run clear java");
		}
		
		instanceSeedGenerator = new RandomInstanceSeedGenerator(instanceCount, seed);
		
	}
	
	public Random getTreeRandom()
	{
		return new MersenneTwister(24);
	}
	public Random getRandomSingleton()
	{
		return SeedableRandomSingleton.getRandom();
	}
	
	/**
	 * Matlab wants a permutation of numbers (1,n) we have a permutation from (0,n) so we translate to (1,n)
	 * @param n
	 * @return
	 */
	public int[] getPermutation(int n)
	{
		return SeedableRandomSingleton.getPermutation(n, 1);
	}
	
	public int getNextSeed(int instanceID)
	{
		return instanceSeedGenerator.getNextSeed(instanceID);
	}
	
	public void setInstanceCount(int instanceCount)
	{
		StaticMethodWrapper.instanceCount= instanceCount;
	}
	
	public RandomForest getRandomForest(int numTrees, double[][] allTheta, double[][] allX, int[][] theta_inst_idxs, double[] y, RegtreeBuildParams params)
	{
		return RandomForest.learnModel(numTrees, allTheta, allX, theta_inst_idxs, y, params);
	}
	
	public int matlabHashCode(double[][] matrix)
	{
		return ArrayMathOps.matlabHashCode(matrix);
	}
	
	public String toString()
	{
		return "Hello, is it me you're looking for?";
	}
	
}