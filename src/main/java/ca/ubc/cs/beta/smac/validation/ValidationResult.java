package ca.ubc.cs.beta.smac.validation;

import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

import java.util.List;

public class ValidationResult {

	private Double performance;
	private List<ProblemInstanceSeedPair> pisps;

	public ValidationResult(Double performance, List<ProblemInstanceSeedPair> pisps)
	{
		this.performance = performance;
		this.pisps = pisps;
	}

	public Double getPerformance() {
		return performance;
	}

	public List<ProblemInstanceSeedPair> getPISPS() {
		return pisps;
	}
	
	
}
