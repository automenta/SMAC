package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;

import java.util.List;

/**
 * Abstract TAE decorator that only requires implementation of the async call.
 * @author afrechet
 */
public abstract class AbstractAsyncTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {

	public AbstractAsyncTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
	}
	
	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}

}
