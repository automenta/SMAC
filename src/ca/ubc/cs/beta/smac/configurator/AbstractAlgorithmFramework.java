package ca.ubc.cs.beta.smac.configurator;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
import ca.ubc.cs.beta.aclib.configspace.tracking.ParamConfigurationOriginTracker;
import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
import ca.ubc.cs.beta.aclib.eventsystem.events.AutomaticConfiguratorEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.AutomaticConfigurationEnd;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.ChallengeStartEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.model.ModelBuildEndEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.model.ModelBuildStartEvent;
import ca.ubc.cs.beta.aclib.eventsystem.events.state.StateRestoredEvent;
import ca.ubc.cs.beta.aclib.exceptions.DeveloperMadeABooBooException;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.exceptions.OutOfTimeException;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aclib.misc.MapList;
import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aclib.misc.watch.StopWatch;
import ca.ubc.cs.beta.aclib.objectives.RunObjective;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.random.RandomUtil;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
import ca.ubc.cs.beta.aclib.runhistory.RunHistoryHelper;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistoryWrapper;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.smac.SMACOptions;
import ca.ubc.cs.beta.aclib.state.StateDeserializer;
import ca.ubc.cs.beta.aclib.state.StateFactory;
import ca.ubc.cs.beta.aclib.state.StateSerializer;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.termination.CompositeTerminationCondition;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;
import ca.ubc.cs.beta.aclib.termination.standard.ConfigurationSpaceExhaustedCondition;
import ca.ubc.cs.beta.aclib.trajectoryfile.TrajectoryFileEntry;


public class AbstractAlgorithmFramework {

	
	
	/**
	 * Run History class
	 * Should only be modified by restoreState, and not by run()
	 */
	protected ThreadSafeRunHistory runHistory;

	protected final long applicationStartTime = System.currentTimeMillis();

	protected final ParamConfigurationSpace configSpace;
	
	protected final double cutoffTime;
	
	protected final List<ProblemInstance> instances;
	
	protected final TargetAlgorithmEvaluator tae;
	
	/**
	 * Stores our configuration
	 */
	protected final SMACOptions options;
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
		
	private final StateFactory stateFactory;
	
	//private final FileWriter trajectoryFileWriter;
	//private final FileWriter trajectoryFileWriterCSV;
	
	private int iteration = 0;
	protected ParamConfiguration incumbent = null;
	
	private final int MAX_RUNS_FOR_INCUMBENT;
	
	
	private final List<TrajectoryFileEntry> tfes = new ArrayList<TrajectoryFileEntry>();
	
	
	
	protected InstanceSeedGenerator instanceSeedGen;
	
	private final ParamConfiguration initialIncumbent;

	private final EventManager evtManager;

	protected SeedableRandomPool pool;
	
	private final CompositeTerminationCondition termCond;
	protected final ParamConfigurationOriginTracker configTracker;
	
	private final InitializationProcedure initProc; 
	
	
	/**
	 * Controls whether the shutdown hook should attempt to write the state to disk
	 * 
	 * By default it is false, and then after the initialization phase we set it to true.
	 * Once we write the final state to disk (either because we are done or CRASHED), we set it back to false
	 * 
	 * The shutdown hook then checks this value and writes to disk if true.
	 * 
	 * 
	 */
	private final AtomicBoolean shouldWriteStateOnCrash = new AtomicBoolean(false);
	
	private final AlgorithmExecutionConfig execConfig;
	
	public AbstractAlgorithmFramework(SMACOptions smacOptions, AlgorithmExecutionConfig execConfig, List<ProblemInstance> instances, TargetAlgorithmEvaluator algoEval, StateFactory stateFactory, ParamConfigurationSpace configSpace, InstanceSeedGenerator instanceSeedGen, ParamConfiguration initialIncumbent, EventManager manager, ThreadSafeRunHistory rh, SeedableRandomPool pool, CompositeTerminationCondition termCond, ParamConfigurationOriginTracker originTracker, InitializationProcedure initProc )
	{
		this.instances = instances;
		this.cutoffTime = smacOptions.scenarioConfig.algoExecOptions.cutoffTime;
		this.options = smacOptions;
				
		this.execConfig = execConfig;
		
		this.tae = algoEval;
		this.stateFactory = stateFactory;
		this.configSpace = configSpace;
		this.runHistory = rh;
		this.instanceSeedGen = instanceSeedGen;
		
		
		this.initialIncumbent = initialIncumbent;
		this.evtManager = manager;
		this.pool = pool;
		
		this.termCond = termCond;
		if(initialIncumbent.isForbiddenParamConfiguration())
		{
			throw new ParameterException("Initial Incumbent specified is forbidden: " + this.initialIncumbent.getFormattedParamString(StringFormat.NODB_SYNTAX));
		}
		
		
	
		long time = System.currentTimeMillis();
		Date d = new Date(time);
		DateFormat df = DateFormat.getDateTimeInstance();	
		log.info("Automatic Configuration Start Time is {}", df.format(d));				
		
		//=== Clamp # runs for incumbent to # of available seeds.
		if(instanceSeedGen.getInitialInstanceSeedCount() < options.maxIncumbentRuns)
		{
			log.info("Clamping number of runs to {} due to lack of instance/seeds pairs", instanceSeedGen.getInitialInstanceSeedCount());
			MAX_RUNS_FOR_INCUMBENT = instanceSeedGen.getInitialInstanceSeedCount();
		}  else
		{
			MAX_RUNS_FOR_INCUMBENT=smacOptions.maxIncumbentRuns;
			log.info("Maximimum Number of Runs for the Incumbent Initialized to {}", MAX_RUNS_FOR_INCUMBENT);
		}
		
		TerminationCondition cond = new ConfigurationSpaceExhaustedCondition(configSpace,MAX_RUNS_FOR_INCUMBENT);
		cond.registerWithEventManager(evtManager);
		
		termCond.addCondition(cond);
		//=== Initialize trajectory file.
		/*
		try {
			String outputFileName = options.scenarioConfig.outputDirectory + File.separator + runGroupName + File.separator +"traj-run-" + options.seedOptions.numRun + ".txt";
			this.trajectoryFileWriter = new FileWriter(new File(outputFileName));
			log.info("Trajectory File Writing To: {}", outputFileName);
			String outputFileNameCSV = options.scenarioConfig.outputDirectory + File.separator + runGroupName + File.separator +"traj-run-" + options.seedOptions.numRun + ".csv";
			this.trajectoryFileWriterCSV = new FileWriter(new File(outputFileNameCSV));
			log.info("Trajectory File Writing To: {}", outputFileNameCSV);
			
			
			
			trajectoryFileWriter.write(runGroupName + ", " + options.seedOptions.numRun + "\n");
			trajectoryFileWriterCSV.write(runGroupName + ", " + options.seedOptions.numRun + "\n");		
		} catch (IOException e) {
			
			throw new IllegalStateException("Could not create trajectory file: " , e);
		}*/
		this.configTracker = originTracker;
		this.initProc = initProc;
		
		if(options.saveRunsEveryIteration && options.scenarioConfig.algoExecOptions.cutoffTime <= 600)
		{
			log.warn("Saving runs every iteration is discouraged for small cap times and may cause a significant amount of overhead due to file I/O.");
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{

			@Override
			public void run() {
				if(shouldWriteStateOnCrash.get())
				{
					log.info("Making best attempt to save state. This state may be dirty, in that it was taken in the middle of an iteration and consequently may not be restorable. It may also be corrupt depending on the exact reason we are shutting down.");
					saveState("SHUTDOWN",true);
				} else
				{
					log.debug("State Saved Already, Skipping Shutdown Version");
				}
			}
			
		}));
	}

	
	
	public ParamConfiguration getInitialIncumbent()
	{
		return initialIncumbent;
	}
	
	
	public ParamConfiguration getIncumbent()
	{
		return incumbent;
	}
	
	private static final String OBJECT_MAP_POOL_KEY = "POOL";
	private static final String OBJECT_MAP_INSTANCE_SEED_GEN_KEY = "INSTANCE_SEED_GEN";
	public void restoreState(StateDeserializer sd)
	{
		
		try 
		{
		log.info("Restoring State");
		
		
		
		int myIteration = sd.getIteration();
		if(myIteration >= 0)
		{
			iteration = myIteration;
		} else
		{
			log.info("No iteration info found it state file, staying at iteration 0");
		}
		
		
		runHistory = new ThreadSafeRunHistoryWrapper(sd.getRunHistory());
		
		Map<String, Serializable> map = sd.getObjectStateMap();
		
			
		if(map.get(OBJECT_MAP_POOL_KEY) != null)
		{
			this.pool = (SeedableRandomPool) map.get(OBJECT_MAP_POOL_KEY);
		} else
		{
			log.info("Incomplete state detected using existing Random Pool object");
		}
		
		
		if(map.get(OBJECT_MAP_INSTANCE_SEED_GEN_KEY) != null)
		{
			this.instanceSeedGen = (InstanceSeedGenerator) map.get(OBJECT_MAP_INSTANCE_SEED_GEN_KEY);
		} else
		{
			log.info("Incomplete state detected using existing instance seed generator");
		}
		
		
		if(this.pool == null)
		{
			throw new IllegalStateException("The pool we restored was null, this state file cannot be restored in SMAC");
		}
		
		if(this.instanceSeedGen == null)
		{
			throw new IllegalStateException("The instance seed generator we restored was null, this state file cannot be restored in SMAC");
		}
		incumbent = sd.getIncumbent();
		if(incumbent == null)
		{
			incumbent = this.initialIncumbent;
		}
		
		Set<ProblemInstanceSeedPair> allPisps = new HashSet<ProblemInstanceSeedPair>();
		for(AlgorithmRun run : runHistory.getAlgorithmRuns())
		{
			ProblemInstanceSeedPair pisp = run.getRunConfig().getProblemInstanceSeedPair();
			allPisps.add(run.getRunConfig().getProblemInstanceSeedPair());
			log.debug("Blacklisting problem instance seed pair: {} ", pisp);
			this.instanceSeedGen.take(pisp.getInstance(), pisp.getSeed());
		}
		
		if(runHistory.getAlgorithmInstanceSeedPairsRan(incumbent).size() != allPisps.size())
		{
			throw new ParameterException("Incumbent has been run on "+ runHistory.getAlgorithmInstanceSeedPairsRan(incumbent).size()+ " problem instance seed pair(s), but there have been a total of "+  allPisps.size() +" run. This generally means the state data used to restore SMAC needs to be repaired to preserve this invariant. Please run the state data through the state-merge utility to repair this invariant");
		}
		
		log.info("Incumbent Set To {}",incumbent);
		
		tae.seek(runHistory.getAlgorithmRuns());
		
		for(AlgorithmRun run: runHistory.getAlgorithmRuns())
		{
			termCond.notifyRun(run);
		}
		
		
		this.fireEvent(new StateRestoredEvent(termCond, this.iteration, this.runHistory, this.incumbent));

		log.info("Restored to Iteration {}", iteration);
		
		} catch(RuntimeException e)
		{
			tae.notifyShutdown();
			throw e;
		}
		
	}
	
	
	protected boolean shouldSave() 
	{
		return true;
	}

	
	private void saveState()
	{
		//The math is only true on perfect powers of 2. 
		saveState("it",(((iteration - 1) & iteration) == 0) || options.saveRunsEveryIteration);
	}

	private synchronized void saveState(String id, boolean saveFullState) {
		StateSerializer state = stateFactory.getStateSerializer(id, iteration);
		
		//state.setPRNG(RandomPoolType.SEEDABLE_RANDOM_SINGLETON, SeedableRandomSingleton.getRandom());
		//state.setPRNG(RandomPoolType.PARAM_CONFIG, configSpacePRNG);
		
		Map<String, Serializable> objMap = new HashMap<String, Serializable>();
		objMap.put(OBJECT_MAP_POOL_KEY,  this.pool);
		objMap.put(OBJECT_MAP_INSTANCE_SEED_GEN_KEY, this.instanceSeedGen);
		
		state.setObjectStateMap(objMap);
		if(saveFullState)
		{	
			//Only save run history on perfect powers of 2.
			state.setRunHistory(runHistory);
		} 
		//state.setInstanceSeedGenerator(runHistory.getInstanceSeedGenerator());
		state.setIncumbent(incumbent);
		state.save();
		
	}
		
	
	
	
	//Last runtime that we saw
	boolean outOfTime = false;
	/**
	 * Function that determines whether we should stop processing or not
	 * @param iteration - number of iterations we have done
	 * @param nextRunTime - the time the next run takes
	 * @return
	 */
	protected boolean have_to_stop(int iteration)
	{
		outOfTime = true;
		outOfTime = termCond.haveToStop();
		
		return outOfTime;
	}
	
	
	public void logIncumbent()
	{
		logIncumbent(-1);
	}
	
	
	
	
	/**
	 * Logs the incumbent 
	 * @param iteration
	 */
	public void logIncumbent(int iteration)
	{
		
		if (iteration > 0)
		{
			Object[] arr = {iteration, runHistory.getThetaIdx(incumbent), incumbent};		
			log.info("At end of iteration {}, incumbent is {} ({}) ",arr);
		} else
		{
			log.info("Incumbent currently is: {} ({}) ", runHistory.getThetaIdx(incumbent), incumbent);
		}				
		//writeIncumbent();
		
	}
	

	public int getIteration()
	{
		return iteration;
	}
	
	private void fireEvent(AutomaticConfiguratorEvent evt)
	{
		this.evtManager.fireEvent(evt);
		this.evtManager.flush();
		
	}
	
	
	/**
	 * Actually performs the Automatic Configuration
	 */
	public void run()
	{
		try {
			try {
				if(pool == null) { throw new IllegalStateException("pool is null, this was unexpected"); }
				if(iteration == 0)
				{ 

					incumbent = initialIncumbent;
					log.info("Initial Incumbent set as Incumbent: {}", incumbent);
					iteration = 0;
					
					log.debug("Initialization Procedure Started");
					initProc.run();
					log.debug("Initialization Procedure Completed");
					
					incumbent =initProc.getIncumbent(); 
					logConfiguration("New Incumbent", incumbent);
					updateIncumbentCost();
					logIncumbent(iteration);
				} else
				{
					//We are restoring state
				}
				fireEvent(new IncumbentPerformanceChangeEvent(termCond, currentIncumbentCost, incumbent ,runHistory.getTotalNumRunsOfConfig(incumbent), this.initialIncumbent));
				/**
				 * Main Loop
				 */
				
				try{
					while(!have_to_stop(iteration+1))
					{
						shouldWriteStateOnCrash.set(true);
						if(shouldSave()) saveState();
						
						
						runHistory.incrementIteration();
						iteration++;
						log.info("Starting Iteration {}", iteration);
						
						fireEvent(new ModelBuildStartEvent(termCond));
						
						StopWatch t = new AutoStartStopWatch();
						learnModel(runHistory, configSpace);
						log.info("Model Learn Time: {} (s)", t.time() / 1000.0);
						
						fireEvent(new ModelBuildEndEvent(termCond, getModel()));
						ArrayList<ParamConfiguration> challengers = new ArrayList<ParamConfiguration>();
						challengers.addAll(selectConfigurations());
						

						double learnModelTime = t.stop()/1000.0;
						
						double intensifyTime = Math.ceil( learnModelTime) * (options.intensificationPercentage / (1.0-options.intensificationPercentage));
						
						intensify(challengers, intensifyTime);
						
						logIncumbent(iteration);
						
					} 
				} catch(OutOfTimeException e){
					// We're out of time.
					logIncumbent(iteration);
				}
				
				
				saveState("it", true);
				shouldWriteStateOnCrash.set(false);
				
				log.info("SMAC Completed");
				
				if(options.stateOpts.cleanOldStatesOnSuccess)
				{
					log.info("Cleaning old states");
					stateFactory.purgePreviousStates();
				}
				
			} catch(RuntimeException e)
			{
				try{
					saveState("CRASH",true);
					shouldWriteStateOnCrash.set(false);
				} catch(RuntimeException e2)
				{
					log.error("SMAC has encountered an exception, and encountered another exception while trying to save the local state. NOTE: THIS PARTICULAR ERROR DID NOT CAUSE SMAC TO FAIL, the original culprit follows further below. (This second error is potentially another / seperate issue, or a disk failure of some kind.) When submitting bug/error reports, please include enough context for *BOTH* exceptions \n  ", e2);
					throw e;
				}
				throw e;
			}
		} finally
		{
			fireEvent(new AutomaticConfigurationEnd(termCond, incumbent, currentIncumbentCost));
			tae.notifyShutdown();
		}
	}
	
	
	
	double timedOutRunCost;
	
	

	protected void learnModel(RunHistory runHistory, ParamConfigurationSpace configSpace) {
	}

	
	public void logIncumbentPerformance(SortedMap<TrajectoryFileEntry, Double> tfePerformance)
	{
		TrajectoryFileEntry tfe = null;
		double testSetPerformance = Double.POSITIVE_INFINITY;
		
		//=== We want the last TFE
		
		ParamConfiguration lastIncumbent = null;
		double lastEmpericalPerformance = Double.POSITIVE_INFINITY;
		
		double lastTestSetPerformance = Double.POSITIVE_INFINITY;
		for(Entry<TrajectoryFileEntry, Double> ents : tfePerformance.entrySet())
		{
			
			
			tfe = ents.getKey();
			double empiricalPerformance = tfe.getEmpericalPerformance();
			
			testSetPerformance = ents.getValue();
			double tunerTime = tfe.getTunerTime();
			ParamConfiguration formerIncumbent = tfe.getConfiguration();
			
			
			if(formerIncumbent.equals(lastIncumbent) && empiricalPerformance == lastEmpericalPerformance && lastTestSetPerformance == testSetPerformance)
			{
				continue;
			} else
			{
				lastIncumbent = formerIncumbent;
				lastEmpericalPerformance = empiricalPerformance;
				lastTestSetPerformance = testSetPerformance;
			}
			
			
			
			if(Double.isInfinite(testSetPerformance))
			{
				Object[] args2 = {runHistory.getThetaIdx(formerIncumbent), formerIncumbent, tunerTime, empiricalPerformance }; 
				log.info("Total Objective of Incumbent {} ({}) at time {} on training set: {}", args2 );
			} else
			{
				Object[] args2 = {runHistory.getThetaIdx(formerIncumbent), formerIncumbent, tunerTime, empiricalPerformance, testSetPerformance };
				log.info("Total Objective of Incumbent {} ({}) at time {} on training set: {}; on test set: {}", args2 );
			}
			
			
		}
		
	}
	
	/**
	 * 
	 * @param tfePerformance
	 */
	public void logSMACResult(SortedMap<TrajectoryFileEntry, Double> tfePerformance)
	{
		
		

		TrajectoryFileEntry tfe = null;
		double testSetPerformance = Double.POSITIVE_INFINITY;
		
		//=== We want the last TFE
		tfe = tfePerformance.lastKey();
		testSetPerformance = tfePerformance.get(tfe);
		
		if(tfe != null)
		{
			if(!tfe.getConfiguration().equals(incumbent))
			{
				throw new IllegalStateException("Last TFE should be Incumbent");
			}
		}
		
		
		ProblemInstanceSeedPair pisp =  runHistory.getAlgorithmInstanceSeedPairsRan(incumbent).iterator().next();
	
		RunConfig runConfig = new RunConfig(pisp, cutoffTime, incumbent, execConfig);
	
		String cmd = tae.getManualCallString(runConfig);
		Object[] args = {runHistory.getThetaIdx(incumbent), incumbent, cmd };
	

		log.info("**********************************************");
		
		if(Double.isInfinite(testSetPerformance))
		{
			Object[] args2 = { runHistory.getThetaIdx(incumbent), incumbent, runHistory.getEmpiricalCost(incumbent, runHistory.getUniqueInstancesRan(), cutoffTime) }; 
			log.info("Total Objective of Final Incumbent {} ({}) on training set: {}", args2 );
		} else
		{
			Object[] args2 = { runHistory.getThetaIdx(incumbent), incumbent,runHistory.getEmpiricalCost(incumbent, runHistory.getUniqueInstancesRan(), cutoffTime), testSetPerformance };
			log.info("Total Objective of Final Incumbent {} ({}) on training set: {}; on test set: {}", args2 );
		}
		
		log.info("Sample Call for Final Incumbent {} ({}) \n{} ",args);
		log.info("Complete Configuration (no inactive conditionals):{}", incumbent.getFormattedParamString(StringFormat.STATEFILE_SYNTAX_NO_INACTIVE));
		log.info("Complete Configuration (including inactive conditionals):{}", incumbent.getFormattedParamString(StringFormat.STATEFILE_SYNTAX));
		
	}

	
	protected Object getModel()
	{
		return null;
	}
	private int selectionCount =0;
	
	protected List<ParamConfiguration> selectConfigurations()
	{
		ParamConfiguration config = configSpace.getRandomConfiguration(pool.getRandom("ROAR_RANDOM_CONFIG"));
		log.debug("Selecting a random configuration {}", config);
		configTracker.addConfiguration(config, "RANDOM", "SelectionCount="+selectionCount);
		return Collections.singletonList(config);
	}
	/**
	 * Intensification
	 * @param challengers - List of challengers we should check against
	 * @param timeBound  - Amount of time we are allowed to run against (seconds)
	 */
	private void intensify(List<ParamConfiguration> challengers, double timeBound) 
	{
		double initialTime = runHistory.getTotalRunCost();
		log.info("Calling intensify with {} challenger(s)", challengers.size());
		for(int i=0; i < challengers.size(); i++)
		{
			double timeUsed = runHistory.getTotalRunCost() - initialTime;
			if( timeUsed > timeBound && i > 1)
			{
				log.info("Out of time for intensification timeBound: {} (s); used: {}  (s)", timeBound, timeUsed );
				break;
			} else
			{
				
				log.info("Intensification timeBound: {} (s); used: {}  (s)", timeBound, timeUsed);
			}
			challengeIncumbent(challengers.get(i));
		}
	}

	
	/**
	 * Counter that controls number of attempts for challenge Incumbent to not hit the limit before giving up
	 */
	
	private void challengeIncumbent(ParamConfiguration challenger)
	{
		fireEvent(new ChallengeStartEvent(termCond, challenger));
		this.challengeIncumbent(challenger, true);
	}
	
	/**
	 * Challenges an incumbent
	 * 
	 * 
	 * @param challenger - challenger we are running with
	 * @param runIncumbent - whether we should run the incumbent before hand 
	 */
	private void challengeIncumbent(ParamConfiguration challenger, boolean runIncumbent) {
		//=== Perform run for incumbent unless it has the maximum #runs.
		
		if(runIncumbent)
		{
			if (runHistory.getTotalNumRunsOfConfig(incumbent) < MAX_RUNS_FOR_INCUMBENT){
				log.debug("Performing additional run with the incumbent ");
				ProblemInstanceSeedPair pisp = RunHistoryHelper.getRandomInstanceSeedWithFewestRunsFor(runHistory,instanceSeedGen, incumbent, instances, pool.getRandom("CHALLENGE_INCUMBENT_INSTANCE_SELECTION"),options.deterministicInstanceOrdering);
				RunConfig incumbentRunConfig = getRunConfig(pisp, cutoffTime,incumbent);
				evaluateRun(incumbentRunConfig);
				updateIncumbentCost();
				//fireEvent(new IncumbentChangeEvent(termCond,  runHistory.getEmpiricalCost(incumbent, new HashSet<ProblemInstance>(instances) , cutoffTime), incumbent,runHistory.getTotalNumRunsOfConfig(incumbent)));
				fireEvent(new IncumbentPerformanceChangeEvent(termCond, currentIncumbentCost, incumbent ,runHistory.getTotalNumRunsOfConfig(incumbent),incumbent));
				
				
				
				
				if(options.alwaysRunInitialConfiguration && !incumbent.equals(initialIncumbent))
				{
					Object[] args = { runHistory.getThetaIdx(initialIncumbent), initialIncumbent,  runHistory.getThetaIdx(incumbent), incumbent }; 
					log.info("Trying challenge with initial configuration {} ({}) first (current incumbent {} ({})", args);
					challengeIncumbent(initialIncumbent, false);
					log.info("Challenge with initial configuration done");
				}
				
				
			} else
			{
				log.debug("Already have performed max runs ({}) for incumbent" , MAX_RUNS_FOR_INCUMBENT);
			}
			
		}
		
		
		if(challenger.equals(incumbent))
		{
			Object[] args = { runHistory.getThetaIdx(challenger), challenger,  runHistory.getThetaIdx(incumbent), incumbent };
			log.info("Challenger {} ({}) is equal to the incumbent {} ({}); not evaluating it further ", args);
			return;
		}
		
		
		
		
		int N=options.initialChallengeRuns;
		while(true){
			/*
			 * Get all the <instance,seed> pairs the incumbent has run (get them in a set).
			 * Then remove all the <instance,seed> pairs the challenger has run on from that set.
			 */
			Set<ProblemInstanceSeedPair> sMissing = new HashSet<ProblemInstanceSeedPair>( runHistory.getAlgorithmInstanceSeedPairsRan(incumbent) );
			sMissing.removeAll( runHistory.getAlgorithmInstanceSeedPairsRan(challenger) );

			List<ProblemInstanceSeedPair> aMissing = new ArrayList<ProblemInstanceSeedPair>();
			aMissing.addAll(sMissing);
			
			//DO NOT SHUFFLE AS MATLAB DOESN'T
			int runsToMake = Math.min(N, aMissing.size());
			if (runsToMake == 0){
		        log.info("Aborting challenge of incumbent. Incumbent has " + runHistory.getTotalNumRunsOfConfig(incumbent) + " runs, challenger has " + runHistory.getTotalNumRunsOfConfig(challenger) + " runs, and the maximum runs for any config is set to " + MAX_RUNS_FOR_INCUMBENT + ".");
		        return;
			} else
			{
			
			}
			Collections.sort(aMissing);
			
			//=== Sort aMissing in the order that we want to evaluate <instance,seed> pairs.
			int[] permutations = RandomUtil.getPermutation(aMissing.size(), 0, pool.getRandom("CHALLENGE_INCUMBENT_SHUFFLE"));
			RandomUtil.permuteList(aMissing, permutations);
			aMissing = aMissing.subList(0, runsToMake);
			
			//=== Only bother with this loop if tracing is enabled (facilitates stepping through the code).
			if(log.isTraceEnabled())
			{
				for(ProblemInstanceSeedPair pisp : aMissing)
				{
					log.trace("Missing Problem Instance Seed Pair {}", pisp);
				}
			}
		
			log.trace("Permuting elements according to {}", Arrays.toString(permutations));
			
			//TODO: refactor adaptive capping.
			double bound_inc = Double.POSITIVE_INFINITY;
			Set<ProblemInstance> missingInstances = null;
			Set<ProblemInstance> missingPlusCommon = null;
			if(options.adaptiveCapping)
			{
				missingInstances = new HashSet<ProblemInstance>();
				
				for(int i=0; i < runsToMake; i++)
				{
					//int index = permutations[i];
					missingInstances.add(aMissing.get(i).getInstance());
				}
				missingPlusCommon = new HashSet<ProblemInstance>();
				missingPlusCommon.addAll(missingInstances);
				Set<ProblemInstance> piCommon = runHistory.getInstancesRan(incumbent);
				piCommon.retainAll( runHistory.getInstancesRan( challenger ));
				missingPlusCommon.addAll(piCommon);
				
				bound_inc = runHistory.getEmpiricalCost(incumbent, missingPlusCommon, cutoffTime) + Math.pow(10, -3);
			}
			Object[] args2 = { N,  runHistory.getThetaIdx(challenger)!=-1?" " + runHistory.getThetaIdx(challenger):"" , challenger, bound_inc } ;
			log.info("Performing up to {} run(s) for challenger{} ({}) up to a total bound of {} ", args2);
			
			List<RunConfig> runsToEval = new ArrayList<RunConfig>(options.scenarioConfig.algoExecOptions.taeOpts.maxConcurrentAlgoExecs); 
			
			if(options.adaptiveCapping && incumbentImpossibleToBeat(challenger, aMissing.get(0), aMissing, missingPlusCommon, cutoffTime, bound_inc))
			{
				log.info("Challenger cannot beat incumbent => scheduling empty run");
				runsToEval.add(getBoundedRunConfig(aMissing.get(0), 0, challenger));
				if (runsToMake != 1){
					throw new IllegalStateException("Error in empty run scheduling: empty runs should only be scheduled in first iteration of intensify.");
				}
			} else
			{
				for (int i = 0; i < runsToMake ; i++) {
					//Runs to make and aMissing should be the same size
					//int index = permutations[i];
					
					RunConfig runConfig;
					ProblemInstanceSeedPair pisp = aMissing.get(0);
					if(options.adaptiveCapping)
					{
						double capTime = computeCap(challenger, pisp, aMissing, missingPlusCommon, cutoffTime, bound_inc);
						if(capTime < cutoffTime)
						{
							if(capTime <= BEST_POSSIBLE_VALUE)
							{
								//If we are here abort runs
								break;
							}
							runConfig = getBoundedRunConfig(pisp, capTime, challenger);
						} else
						{
							runConfig = getRunConfig(pisp, cutoffTime, challenger);
						}
					} else
					{
						runConfig = getRunConfig(pisp, cutoffTime, challenger);
					}
					
					runsToEval.add(runConfig);
					
					sMissing.remove(pisp);
					aMissing.remove(0);
					if(runsToEval.size() == options.scenarioConfig.algoExecOptions.taeOpts.maxConcurrentAlgoExecs )
					{
						evaluateRun(runsToEval);
						runsToEval.clear();
					} 
				}
			}
			if(runsToEval.size() > 0)
			{
				evaluateRun(runsToEval);
				runsToEval.clear();
			}
						
			//=== Get performance of incumbent and challenger on their common instances.
			Set<ProblemInstance> piCommon = runHistory.getInstancesRan(incumbent);
			piCommon.retainAll( runHistory.getInstancesRan( challenger ));
			
			double incCost = runHistory.getEmpiricalCost(incumbent, piCommon,cutoffTime);
			double chalCost = runHistory.getEmpiricalCost(challenger, piCommon, cutoffTime);
			
			Object args[] = {piCommon.size(), runHistory.getUniqueInstancesRan().size(), runHistory.getThetaIdx(challenger), challenger.getFriendlyIDHex(), chalCost,runHistory.getThetaIdx(incumbent),  incumbent, incCost  };
			log.info("Based on {} common runs on (up to) {} instances, challenger {} ({})  has a lower bound {} and incumbent {} ({}) has obj {}",args);
			
			//=== Decide whether to discard challenger, to make challenger incumbent, or to continue evaluating it more.		
			if (incCost + Math.pow(10, -6)  < chalCost){
				log.info("Challenger {} ({}) is worse; aborting its evaluation",  runHistory.getThetaIdx(challenger), challenger );
				configTracker.addConfiguration(challenger, "Challenge-Round-" + runHistory.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(challenger), "Continue=False","IncumbentCost=" + incCost , "ChallengeCost=" + chalCost);
				
				break;
			} else if (sMissing.isEmpty())
			{	
				if(chalCost < incCost - Math.pow(10,-6))
				{
					configTracker.addConfiguration(challenger, "Final-Challenge-Round", "NewIncumbent=True","IncumbentCost=" + incCost , "ChallengeCost=" + chalCost);
					changeIncumbentTo(challenger);
				} else
				{
					configTracker.addConfiguration(challenger, "Final-Challenge-Round", "NewIncumbent=False","IncumbentCost=" + incCost , "ChallengeCost=" + chalCost);
					log.info("Challenger {} ({}) has all the runs of the incumbent, but did not outperform it", runHistory.getThetaIdx(challenger), challenger );
					
				}
				
				break;
			} else
			{
				configTracker.addConfiguration(challenger, "Challenge-Round-" + runHistory.getTotalNumRunsOfConfig(challenger), "Continue=True","IncumbentCost=" + incCost , "ChallengeCost=" + chalCost,"RunsNeededLeft="+(runHistory.getTotalNumRunsOfConfig(incumbent)-runHistory.getTotalNumRunsOfConfig(challenger)));
				N *= 2;
				Object[] args3 = { runHistory.getThetaIdx(challenger), challenger, N};
				log.trace("Increasing additional number of runs for challenger {} ({}) to : {} ", args3);
			}
		}
	}
	
	

	private void logConfiguration(String type, ParamConfiguration challenger) {
		
		
		ProblemInstanceSeedPair pisp =  runHistory.getAlgorithmInstanceSeedPairsRan(incumbent).iterator().next();
		
		RunConfig config = new RunConfig(pisp, cutoffTime, challenger, execConfig);
		
		String cmd = tae.getManualCallString(config);
		Object[] args = { type, runHistory.getThetaIdx(challenger), challenger, cmd };
		log.info("Sample Call for {} {} ({}) \n{} ",args);
		
	}



	private static double currentIncumbentCost;

	private void changeIncumbentTo(ParamConfiguration challenger) {
		ParamConfiguration oldIncumbent = incumbent;
		incumbent = challenger;
		updateIncumbentCost();
		log.info("Incumbent Changed to: {} ({})", runHistory.getThetaIdx(challenger), challenger );
		logConfiguration("New Incumbent", challenger);		
		fireEvent(new IncumbentPerformanceChangeEvent(termCond, currentIncumbentCost, incumbent ,runHistory.getTotalNumRunsOfConfig(incumbent),oldIncumbent));

	}

	private double computeCap(ParamConfiguration challenger, ProblemInstanceSeedPair pisp, List<ProblemInstanceSeedPair> aMissing, Set<ProblemInstance> instanceSet, double cutofftime, double bound_inc)
	{
		if(incumbentImpossibleToBeat(challenger, pisp, aMissing, instanceSet, cutofftime, bound_inc))
		{
			return UNCOMPETITIVE_CAPTIME;
		}
		return computeCapBinSearch(challenger, pisp, aMissing, instanceSet, cutofftime, bound_inc, BEST_POSSIBLE_VALUE, cutofftime);
	}
	
	private boolean incumbentImpossibleToBeat(ParamConfiguration challenger, ProblemInstanceSeedPair pisp, List<ProblemInstanceSeedPair> aMissing, Set<ProblemInstance> instanceSet, double cutofftime, double bound_inc)
	{
		return (lowerBoundOnEmpiricalPerformance(challenger, pisp, aMissing, instanceSet, cutofftime, BEST_POSSIBLE_VALUE) > bound_inc);
	}
	
	private static final double BEST_POSSIBLE_VALUE = 0;
	private static final double UNCOMPETITIVE_CAPTIME = 0;
	private double computeCapBinSearch(ParamConfiguration challenger, ProblemInstanceSeedPair pisp, List<ProblemInstanceSeedPair> aMissing, Set<ProblemInstance> missingInstances, double cutofftime, double bound_inc,  double lowerBound, double upperBound)
	{
	
		if(upperBound - lowerBound < Math.pow(10,-6))
		{
			double capTime = upperBound + Math.pow(10, -3);
			return capTime * options.capSlack + options.capAddSlack;
		}
		
		double mean = (upperBound + lowerBound)/2;
		
		double predictedPerformance = lowerBoundOnEmpiricalPerformance(challenger, pisp, aMissing, missingInstances, cutofftime, mean); 
		if(predictedPerformance < bound_inc)
		{
			return computeCapBinSearch(challenger, pisp, aMissing, missingInstances, cutofftime, bound_inc, mean, upperBound);
		} else
		{
			return computeCapBinSearch(challenger, pisp, aMissing, missingInstances, cutofftime, bound_inc, lowerBound, mean);
		}
	}

	
	private double lowerBoundOnEmpiricalPerformance(ParamConfiguration challenger, ProblemInstanceSeedPair pisp, List<ProblemInstanceSeedPair> aMissing, Set<ProblemInstance> instanceSet, double cutofftime, double probe)
	{
		
		Map<ProblemInstance, Map<Long, Double>> hallucinatedValues = new HashMap<ProblemInstance, Map<Long, Double>>();
		
		for(ProblemInstanceSeedPair missingPisp : aMissing)
		{
			ProblemInstance pi = missingPisp.getInstance();
		
			if(hallucinatedValues.get(pi) == null)
			{
				hallucinatedValues.put(pi, new HashMap<Long, Double>());
			}
			
			double hallucinatedValue = BEST_POSSIBLE_VALUE;
			
			if(pisp.equals(missingPisp))
			{
				hallucinatedValue = probe;
			}
			
			
			
			hallucinatedValues.get(pi).put(missingPisp.getSeed(), hallucinatedValue);
			
		}
		return runHistory.getEmpiricalCost(challenger, instanceSet, cutofftime, hallucinatedValues);
	}
	private  void updateIncumbentCost() {
		
		currentIncumbentCost = runHistory.getEmpiricalCost(incumbent, new HashSet<ProblemInstance>(instances), cutoffTime);
		log.debug("Incumbent Cost now: {}", currentIncumbentCost);
	}


	
	/**
	 * Gets a runConfiguration for the given parameters
	 * @param pisp
	 * @param cutofftime
	 * @param configuration
	 * @return
	 */
	protected RunConfig getRunConfig(ProblemInstanceSeedPair pisp, double cutofftime, ParamConfiguration configuration)
	{
		
		RunConfig rc =  new RunConfig(pisp, cutofftime, configuration, execConfig);
		log.trace("RunConfig generated {}", rc);
		return rc;
	}
	
	
	private RunConfig getBoundedRunConfig(
			ProblemInstanceSeedPair pisp, double capTime,
			ParamConfiguration challenger) {
		
		RunConfig rc =  new RunConfig(pisp, capTime, challenger, execConfig );
		log.trace("RunConfig generated {}", rc);
		return rc;
	}
	
	
	
	/**
	 * 
	 * @return the input parameter (unmodified, simply for syntactic convience)
	 */
	protected List<AlgorithmRun> updateRunHistory(List<AlgorithmRun> runs)
	{
		for(AlgorithmRun run : runs)
		{
			try {
					runHistory.append(run);
			} catch (DuplicateRunException e) {
				//We are trying to log a duplicate run
				throw new IllegalStateException(e);
			}
		}
		return runs;
	}
	
	/**
	 * Evaluates a single run, and updates our runHistory
	 * @param runConfig
	 * @return
	 */
	protected List<AlgorithmRun> evaluateRun(RunConfig runConfig)
	{
		return evaluateRun(Collections.singletonList(runConfig));
	}
	
	/**
	 * Evaluates a list of runs and updates our runHistory
	 * @param runConfigs
	 * @return
	 */
	protected List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs)
	{
		if (have_to_stop(iteration)){
			log.info("Cannot schedule any more runs, out of time");
			throw new OutOfTimeException();
		} 
	
		log.info("Iteration {}: Scheduling {} run(s):", iteration,  runConfigs.size());
		for(RunConfig rc : runConfigs)
		{
			Object[] args = { iteration, runHistory.getThetaIdx(rc.getParamConfiguration())!=-1?" "+runHistory.getThetaIdx(rc.getParamConfiguration()):"", rc.getParamConfiguration(), rc.getProblemInstanceSeedPair().getInstance().getInstanceID(),  rc.getProblemInstanceSeedPair().getSeed(), rc.getCutoffTime()};
			log.info("Iteration {}: Scheduling run for config{} ({}) on instance {} with seed {} and captime {}", args);
		}
		
		List<AlgorithmRun> completedRuns = tae.evaluateRun(runConfigs);
		
		for(AlgorithmRun run : completedRuns)
		{
			RunConfig rc = run.getRunConfig();
			Object[] args = { iteration,  runHistory.getThetaIdx(rc.getParamConfiguration())!=-1?" "+runHistory.getThetaIdx(rc.getParamConfiguration()):"", rc.getParamConfiguration(), rc.getProblemInstanceSeedPair().getInstance().getInstanceID(),  rc.getProblemInstanceSeedPair().getSeed(), rc.getCutoffTime(), run.getRunResult(), options.scenarioConfig.runObj.getObjective(run), run.getWallclockExecutionTime()};

			log.info("Iteration {}: Completed run for config{} ({}) on instance {} with seed {} and captime {} => Result: {}, response: {}, wallclock time: {} seconds", args);
		}
		
		
		
		updateRunHistory(completedRuns);
		return completedRuns;
	}


	public double getEmpericalPerformance(ParamConfiguration config) {
		Set<ProblemInstance> pis = new HashSet<ProblemInstance>();
		pis.addAll(instances);
		return runHistory.getEmpiricalCost(config, pis, cutoffTime);
	}
	
	public List<TrajectoryFileEntry> getTrajectoryFileEntries()
	{
		return Collections.unmodifiableList(tfes);
	}
	
	public String getTerminationReason()
	{
		return termCond.getTerminationReason();
	}
	
}
