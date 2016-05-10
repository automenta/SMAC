package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

import java.util.List;


public interface TargetAlgorithmEvaluatorRunObserver {

	/**
	 * Invoked on a best effort basis when new information is available
	 * @param runs
	 */
	public void currentStatus(List<? extends AlgorithmRunResult> runs);
}
