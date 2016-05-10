package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.transform;


import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;
import com.beust.jcommander.ParameterException;

import net.jcip.annotations.ThreadSafe;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TAE Decorator that allows users to supply arbitrary transforms (e.g. square the run time if SAT) 
 * @author Alexandre Fréchette <afrechet@cs.ubc.ca>
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class TransformTargetAlgorithmEvaluatorDecorator extends AbstractForEachRunTargetAlgorithmEvaluatorDecorator {
	
	@SuppressWarnings("unused")
	private final TransformTargetAlgorithmEvaluatorDecoratorOptions options;
	
	//Variables associated to a run we calculate : S run result (SAT=1,UNSAT=-1,other=0), R runtime, Q quality, C cutoff.
	static final String[] variablesnames = {"S","R","Q","C"};
	
	private final Expression SAT_runtime_Expression;
	private final Expression SAT_quality_Expression;
	private final Expression UNSAT_runtime_Expression;
	private final Expression UNSAT_quality_Expression;
	private final Expression TIMEOUT_quality_Expression;
	private final Expression TIMEOUT_runtime_Expression;
	private final Expression other_runtime_Expression;
	private final Expression other_quality_Expression;
	
	private final List<Expression> Expressions;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final AtomicBoolean warningLogged = new AtomicBoolean(false);
	
	public TransformTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae,TransformTargetAlgorithmEvaluatorDecoratorOptions options) {
		
		super(tae);
		
		log.debug("Results from the Target Algorithm Evaluator are being transformed");
		//Get the necessary transforms from the options as Expression.
		try
		{
			this.SAT_runtime_Expression = new ExpressionBuilder(options.SAT_runtime_transform).variables(variablesnames).build();
			this.SAT_quality_Expression = new ExpressionBuilder(options.SAT_quality_transform).variables(variablesnames).build();
			this.UNSAT_runtime_Expression = new ExpressionBuilder(options.UNSAT_runtime_transform).variables(variablesnames).build();
			this.UNSAT_quality_Expression = new ExpressionBuilder(options.UNSAT_quality_transform).variables(variablesnames).build();
			this.TIMEOUT_runtime_Expression = new ExpressionBuilder(options.TIMEOUT_runtime_transform).variables(variablesnames).build();
			this.TIMEOUT_quality_Expression = new ExpressionBuilder(options.TIMEOUT_quality_transform).variables(variablesnames).build();
			this.other_runtime_Expression = new ExpressionBuilder(options.other_runtime_transform).variables(variablesnames).build();
			this.other_quality_Expression = new ExpressionBuilder(options.other_quality_transform).variables(variablesnames).build();
		}
		catch(Exception e) {
			throw new ParameterException("Provided options contains an unExpression string (" + e.getMessage() + ").");
		}
		//} catch (UnknownFunctionException e) {
			//throw new ParameterException("Provided options contains a Expression string with an unknown function ("+e.getMessage()+").");
		//}
		
		Expressions = Arrays.asList(SAT_runtime_Expression,SAT_quality_Expression,
				UNSAT_runtime_Expression,UNSAT_quality_Expression,
				TIMEOUT_quality_Expression,TIMEOUT_runtime_Expression,
				other_runtime_Expression,other_quality_Expression);
		
		this.options = options;
		
	}

	/**
	 * Template method that is invoked with each run that complete
	 * 
	 * @param run process the run
	 * @return run that will replace it in the values returned to the client
	 */
	protected AlgorithmRunResult processRun(AlgorithmRunResult run)
	{
		//Assign the variables according to the current run.
		int S;
		switch(run.getRunStatus())
		{
			case SAT:
				S=1;
				break;
			case UNSAT:
				S=-1;
				break;
			default:
				S=0;
				break;
		}
		double R = run.getRuntime();
		double Q = run.getQuality();
		double C = run.getAlgorithmRunConfiguration().getCutoffTime();
		
		//Set the variables
		for(Expression Expression : Expressions)
		{
			Expression.setVariable("S",S);
			Expression.setVariable("R",R);
			Expression.setVariable("Q",Q);
			Expression.setVariable("C",C);
		}
		
		//Compute modified values.
		double transformedRuntime;
		double transformed_quality;
		
		Expression usedExpression;
		switch(run.getRunStatus())
		{
			case SAT:
				transformedRuntime = SAT_runtime_Expression.evaluate();
				transformed_quality = SAT_quality_Expression.evaluate();
				usedExpression = SAT_runtime_Expression;
				break;
			
			case UNSAT:
				transformedRuntime = UNSAT_runtime_Expression.evaluate();
				transformed_quality = UNSAT_quality_Expression.evaluate();
				usedExpression = UNSAT_runtime_Expression;
				break;
			
			case TIMEOUT:
				transformedRuntime = TIMEOUT_runtime_Expression.evaluate();
				transformed_quality = TIMEOUT_quality_Expression.evaluate();
				usedExpression = TIMEOUT_runtime_Expression;
				break;
				
			default:
				transformedRuntime = other_runtime_Expression.evaluate();
				transformed_quality = other_quality_Expression.evaluate();
				usedExpression = other_runtime_Expression;
				break;
		}
		
		
		if(options.transformValidValuesOnly && transformedRuntime >= run.getAlgorithmRunConfiguration().getCutoffTime())
		{
			return new ExistingAlgorithmRunResult(run.getAlgorithmRunConfiguration(), RunStatus.TIMEOUT, run.getAlgorithmRunConfiguration().getCutoffTime(), run.getRunLength(), transformed_quality, run.getResultSeed());
			
		} else
		{
			if(options.transformValidValuesOnly)
			{
				if(transformedRuntime < 0)
				{
					if(!warningLogged.getAndSet(true))
					{
						log.warn("Transformation of Runtime seems to have resulted in a negative value this is illegal and you should ensure that the result is always above zero, original run: {}, transformed runtime: {}, transformation: \"{}\" ", run, transformedRuntime , usedExpression);
					}
					
					transformedRuntime = 0;
				}
				
			}
			return new ExistingAlgorithmRunResult( run.getAlgorithmRunConfiguration(), run.getRunStatus(), transformedRuntime, run.getRunLength(), transformed_quality, run.getResultSeed());
			
		}
	}
	
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}
