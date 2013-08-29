//package ca.ubc.cs.beta.smac.executors;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.lang.management.ManagementFactory;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Queue;
//import java.util.Set;
//import java.util.SortedMap;
//import java.util.TreeMap;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
//import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration.StringFormat;
//import ca.ubc.cs.beta.aclib.configspace.tracking.ParamConfigurationOriginTracker;
//import ca.ubc.cs.beta.aclib.configspace.tracking.RealParamConfigurationOriginTracker;
//import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
//import ca.ubc.cs.beta.aclib.configspace.ParamFileHelper;
//import ca.ubc.cs.beta.aclib.eventsystem.EventHandler;
//import ca.ubc.cs.beta.aclib.eventsystem.EventManager;
//import ca.ubc.cs.beta.aclib.eventsystem.events.ac.AutomaticConfigurationEnd;
//import ca.ubc.cs.beta.aclib.eventsystem.events.ac.ChallengeStartEvent;
//import ca.ubc.cs.beta.aclib.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
//import ca.ubc.cs.beta.aclib.eventsystem.events.basic.AlgorithmRunCompletedEvent;
//import ca.ubc.cs.beta.aclib.eventsystem.events.model.ModelBuildEndEvent;
//import ca.ubc.cs.beta.aclib.eventsystem.events.model.ModelBuildStartEvent;
//import ca.ubc.cs.beta.aclib.eventsystem.handlers.LogRuntimeStatistics;
//import ca.ubc.cs.beta.aclib.eventsystem.handlers.ParamConfigurationIncumbentChangerOriginTracker;
//import ca.ubc.cs.beta.aclib.eventsystem.handlers.ParamConfigurationOriginLogger;
//import ca.ubc.cs.beta.aclib.exceptions.FeatureNotFoundException;
//import ca.ubc.cs.beta.aclib.exceptions.StateSerializationException;
//import ca.ubc.cs.beta.aclib.exceptions.TrajectoryDivergenceException;
//import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
//import ca.ubc.cs.beta.aclib.initialization.InitializationProcedure;
//import ca.ubc.cs.beta.aclib.initialization.classic.ClassicInitializationProcedure;
//
//import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
//import ca.ubc.cs.beta.aclib.misc.returnvalues.ACLibReturnValues;
//import ca.ubc.cs.beta.aclib.misc.spi.SPIClassLoaderHelper;
//import ca.ubc.cs.beta.aclib.misc.version.VersionTracker;
//import ca.ubc.cs.beta.aclib.model.builder.HashCodeVerifyingModelBuilder;
//import ca.ubc.cs.beta.aclib.objectives.OverallObjective;
//import ca.ubc.cs.beta.aclib.objectives.RunObjective;
//import ca.ubc.cs.beta.aclib.options.AbstractOptions;
//import ca.ubc.cs.beta.aclib.options.docgen.OptionsToLaTeX;
//import ca.ubc.cs.beta.aclib.options.scenario.ScenarioOptions;
//import ca.ubc.cs.beta.aclib.probleminstance.InstanceListWithSeeds;
//import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
//import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceHelper;
//import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceOptions.TrainTestInstances;
//import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
//import ca.ubc.cs.beta.aclib.random.SeedableRandomPoolConstants;
//import ca.ubc.cs.beta.aclib.runhistory.NewRunHistory;
//import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
//import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
//import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistoryWrapper;
//import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;
//import ca.ubc.cs.beta.aclib.smac.ExecutionMode;
//import ca.ubc.cs.beta.aclib.smac.SMACOptions;
//import ca.ubc.cs.beta.aclib.state.StateDeserializer;
//import ca.ubc.cs.beta.aclib.state.StateFactory;
//import ca.ubc.cs.beta.aclib.state.legacy.LegacyStateFactory;
//import ca.ubc.cs.beta.aclib.state.nullFactory.NullStateFactory;
//import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
//import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
//import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.helpers.TargetAlgorithmEvaluatorNotifyTerminationCondition;
//import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;
//import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
//import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
//import ca.ubc.cs.beta.aclib.termination.CompositeTerminationCondition;
//import ca.ubc.cs.beta.aclib.termination.TerminationCondition;
//import ca.ubc.cs.beta.aclib.termination.standard.ConfigurationSpaceExhaustedCondition;
//import ca.ubc.cs.beta.aclib.trajectoryfile.TrajectoryFileEntry;
//import ca.ubc.cs.beta.aclib.trajectoryfile.TrajectoryFileLogger;
//import ca.ubc.cs.beta.smac.configurator.AbstractAlgorithmFramework;
//import ca.ubc.cs.beta.smac.configurator.SequentialModelBasedAlgorithmConfiguration;
//import ca.ubc.cs.beta.smac.handler.ChallengePredictionHandler;
//import ca.ubc.cs.beta.smac.validation.Validator;
//
//import com.beust.jcommander.JCommander;
//import com.beust.jcommander.Parameter;
//import com.beust.jcommander.ParameterException;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.Marker;
//import org.slf4j.MarkerFactory;
//
//@SuppressWarnings("unused")
//@Deprecated
//public class AutomaticConfigurator 
//{
//
//	
//	private static Logger log;
//	private static Marker exception;
//	private static Marker stackTrace;
//	
//	private static String logLocation = "<NO LOG LOCATION SPECIFIED, FAILURE MUST HAVE OCCURED EARLY>";
//	
//	/*
//	private static List<ProblemInstance> instances;
//	private static List<ProblemInstance> testInstances;
//	
//	private static InstanceSeedGenerator instanceSeedGen;
//	private static InstanceSeedGenerator testInstanceSeedGen;
//	
//	
//	
//	private static String instanceFileAbsolutePath;
//	private static String instanceFeatureFileAbsolutePath;
//	*/
//	
//	
//	private static InstanceListWithSeeds trainingILWS;
//	private static InstanceListWithSeeds testingILWS;
//	
//	
//	private static Map<String,  AbstractOptions> taeOptions;
//	private static SeedableRandomPool pool;
//	
//	private static String outputDir;
//	
//	/**
//	 * Executes SMAC then exits the JVM {@see System.exit()}
//	 *  
//	 * @param args string arguments
//	 */
//	public static void main(String[] args)
//	{
//		int returnValue = oldMain(args);
//		
//		if(log != null)
//		{
//			log.info("Returning with value: {}",returnValue);
//		}
//		
//		System.exit(returnValue);
//	}
//	
//	
//	/**
//	 * Executes SMAC according to the given arguments
//	 * @param args 	string input arguments
//	 * @return return value for operating system
//	 */
//	public static int oldMain(String[] args)
//	{
//		/*
//		 * WARNING: DO NOT LOG ANYTHING UNTIL AFTER WE HAVE PARSED THE CLI OPTIONS
//		 * AS THE CLI OPTIONS USE A TRICK TO ALLOW LOGGING TO BE CONFIGURABLE ON THE CLI
//		 * IF YOU LOG PRIOR TO IT ACTIVATING, IT WILL BE IGNORED 
//		 */
//		try {
//			SMACOptions options = parseCLIOptions(args);
//			
//			
//			log.info("Automatic Configurator Started");
//			
//			
//			
//			
//		
//			/*
//			 * Build the Serializer object used in the model 
//			 */
//			StateFactory restoreSF = options.getRestoreStateFactory(outputDir);
//			
//			
//			AlgorithmExecutionConfig execConfig = options.getAlgorithmExecutionConfig();
//		
//			ParamConfigurationSpace configSpace = execConfig.getParamFile();
//			
//			log.info("Configuration Space Size is less than or equal to {} ", configSpace.getUpperBoundOnSize());
//			
//			StateFactory sf = options.getSaveStateFactory(outputDir);
//			
//			
//			List<ProblemInstance> instances = trainingILWS.getInstances();
//			InstanceSeedGenerator instanceSeedGen = trainingILWS.getSeedGen();
//			
//			options.checkProblemInstancesCompatibleWithVerifySAT(instances);
//			
//			ParamConfiguration initialIncumbent = configSpace.getConfigurationFromString(options.initialIncumbent, StringFormat.NODB_SYNTAX, pool.getRandom(SeedableRandomPoolConstants.INITIAL_INCUMBENT_SELECTION));
//		
//			
//			if(!initialIncumbent.equals(configSpace.getDefaultConfiguration()))
//			{
//				log.info("Initial Incumbent set to \"{}\" ", initialIncumbent.getFormattedParamString(StringFormat.NODB_SYNTAX));
//			} else
//			{
//				log.info("Initial Incumbent is the default \"{}\" ", initialIncumbent.getFormattedParamString(StringFormat.NODB_SYNTAX));
//			}
//			
//			
//			TargetAlgorithmEvaluator tae = options.scenarioConfig.algoExecOptions.taeOpts.getTargetAlgorithmEvaluator(execConfig, taeOptions, outputDir, options.seedOptions.numRun);
//			
//
//			if(options.modelHashCodeFile != null)
//			{
//				log.info("Algorithm Execution will verify model Hash Codes");
//				parseModelHashCodes(options.modelHashCodeFile);
//			}
//			
//			EventManager eventManager = new EventManager();
//			
//			AbstractAlgorithmFramework smac;
//	
//			ThreadSafeRunHistory rh = new ThreadSafeRunHistoryWrapper(new NewRunHistory(options.scenarioConfig.intraInstanceObj, options.scenarioConfig.interInstanceObj, options.scenarioConfig.runObj));
//			
//			CompositeTerminationCondition termCond = options.scenarioConfig.limitOptions.getTerminationConditions();
//			
//			
//			
//			LogRuntimeStatistics logRT = new LogRuntimeStatistics(rh, termCond, execConfig.getAlgorithmCutoffTime());
//			TrajectoryFileLogger tLog = new TrajectoryFileLogger(rh, termCond, outputDir +  File.separator + "traj-run-" + options.seedOptions.numRun, initialIncumbent);
//			
//			try {
//			termCond.registerWithEventManager(eventManager);
//			eventManager.registerHandler(ModelBuildStartEvent.class, logRT);
//			eventManager.registerHandler(IncumbentPerformanceChangeEvent.class,logRT);
//			eventManager.registerHandler(AlgorithmRunCompletedEvent.class, logRT);
//			eventManager.registerHandler(AutomaticConfigurationEnd.class, logRT);
//			
//			eventManager.registerHandler(IncumbentPerformanceChangeEvent.class, tLog);
//			eventManager.registerHandler(AutomaticConfigurationEnd.class, tLog);
//			
//			ParamConfigurationOriginTracker configTracker = options.trackingOptions.getTracker(eventManager, initialIncumbent, outputDir, rh, execConfig, options.seedOptions.numRun);
//			
//			
//			TargetAlgorithmEvaluator acTae = new TargetAlgorithmEvaluatorNotifyTerminationCondition(tae, eventManager, termCond, true);
//			
//			
//			InitializationProcedure initProc;
//			
//			switch(options.initializationMode)
//			{
//				case CLASSIC:
//					initProc = new ClassicInitializationProcedure(rh, initialIncumbent, acTae, options.classicInitModeOpts, instanceSeedGen, instances, options.maxIncumbentRuns, termCond, options.scenarioConfig.algoExecOptions.cutoffTime, pool, options.deterministicInstanceOrdering);
//					break;
//				case ITERATIVE_CAPPING:
//				default:
//					throw new IllegalStateException("Not Implemented currently");
//			}
//			
//			
//			switch(options.execMode)
//			{
//				case ROAR:
//
//					smac = new AbstractAlgorithmFramework(options,instances,acTae,sf, configSpace, instanceSeedGen, initialIncumbent, eventManager, rh, pool, runGroupName, termCond, configTracker, initProc);
//					
//					break;
//				case SMAC:
//
//					smac = new SequentialModelBasedAlgorithmConfiguration(options, instances, acTae, options.expFunc.getFunction(),sf, configSpace, instanceSeedGen, initialIncumbent, eventManager, rh,pool, runGroupName, termCond, configTracker, initProc);
//
//					
//					break;
//				default:
//					throw new IllegalArgumentException("Execution Mode Specified is not supported");
//			}
//			
//			if(options.trackingOptions.configTracking && options.execMode.equals(ExecutionMode.SMAC))
//			{
//				ChallengePredictionHandler cph = new ChallengePredictionHandler(smac, configTracker);
//				eventManager.registerHandler(ModelBuildStartEvent.class, cph);
//				eventManager.registerHandler(ModelBuildEndEvent.class, cph);
//				eventManager.registerHandler(ChallengeStartEvent.class, cph);
//				
//			}
//			
//			
//			
//			
//			
//			
//			options.saveContextWithState(configSpace, trainingILWS, sf);
//						
//			if(options.stateOpts.restoreIteration != null)
//			{
//				restoreState(options, restoreSF, smac, configSpace, instances, execConfig, rh);
//			}
//			
//			
//				
//				smac.run();
//				log.info("SMAC Termination Reason: {}",smac.getTerminationReason() );
//			} finally
//			{
//				tae.notifyShutdown();
//			}
//			
//			pool.logUsage();
//			
//			List<TrajectoryFileEntry> tfes = tLog.getTrajectoryFileEntries();
//			SortedMap<TrajectoryFileEntry, Double> performance;
//			if(options.doValidation)
//			{
//			
//				//Don't use the same TargetAlgorithmEvaluator as above as it may have runhashcode and other crap that is probably not applicable for validation
//				
//				if(options.validationOptions.maxTimestamp == -1)
//				{
//					options.validationOptions.maxTimestamp = options.scenarioConfig.limitOptions.tunerTimeout;
//				}
//				
//				options.scenarioConfig.algoExecOptions.taeOpts.trackRunsScheduled = false;
//				
//				TargetAlgorithmEvaluator validatingTae =TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(options.scenarioConfig.algoExecOptions.taeOpts, execConfig, false, taeOptions);
//				try {
//					
//					List<ProblemInstance> testInstances = testingILWS.getInstances();
//					InstanceSeedGenerator testInstanceSeedGen = testingILWS.getSeedGen();
//					
//					performance  = (new Validator()).validate(testInstances,options.validationOptions,options.scenarioConfig.algoExecOptions.cutoffTime, testInstanceSeedGen, validatingTae, outputDir, options.scenarioConfig.runObj, options.scenarioConfig.intraInstanceObj, options.scenarioConfig.interInstanceObj, tfes, options.seedOptions.numRun,true);
//				} finally
//				{
//					validatingTae.notifyShutdown();
//				}
//				
//			} else
//			{
//				performance = new TreeMap<TrajectoryFileEntry, Double>();
//				performance.put(tfes.get(tfes.size()-1), Double.POSITIVE_INFINITY);
//				
//			}
//			
//			
//			
//			
//			smac.logIncumbentPerformance(performance);
//
//			smac.logSMACResult(performance);
//			
//			logRT.logLastRuntimeStatistics();
//			
//			eventManager.shutdown();
//			log.info("SMAC Termination Reason: {}",smac.getTerminationReason() );
//			log.info("SMAC Completed Successfully. Log: " + logLocation);
//			
//			
//			return ACLibReturnValues.SUCCESS;
//		} catch(Throwable t)
//		{
//			System.out.flush();
//			System.err.flush();
//			
//			System.err.println("Error occured running SMAC ( " + t.getClass().getSimpleName() + " : "+ t.getMessage() +  ")\nError Log: " + logLocation);
//			System.err.flush();
//			
//				if(log != null)
//				{
//					
//					log.error(exception, "Message: {}",t.getMessage());
//					
//					
//					if(t instanceof ParameterException)
//					{
//						log.info("Don't forget that some options are set by default from files in ~/.aclib/");
//						log.debug("Exception stack trace", t);
//						
//						
//					} else if(t instanceof TargetAlgorithmAbortException)
//					{
//						log.error("A serious problem occured during target algorithm execution and we are aborting execution ",t );
//						
//						
//						log.error("We tried to call the target algorithm wrapper, but this call failed.");
//						log.error("The problem is (most likely) somewhere in the wrapper.");
//						log.error("There is also possibly additional error information above (in this log)");
//						log.error("The easiest way to debug this problem is to manually execute the call we tried and see why it did not return the correct result");
//						log.error("The required syntax is something like \"Final Result for ParamILS: x,x,x,x,x\".);");
//						log.error("Specifically the regex we are matching is {}", CommandLineAlgorithmRun.AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
//					}	else
//					{
//						log.info("Maybe try running in DEBUG mode if you are missing information");
//						
//						log.error(exception, "Exception:{}", t.getClass().getCanonicalName());
//						StringWriter sWriter = new StringWriter();
//						PrintWriter writer = new PrintWriter(sWriter);
//						t.printStackTrace(writer);
//						log.error(stackTrace, "StackTrace:{}",sWriter.toString());
//					}
//						
//					log.info("Exiting SMAC with failure. Log: " + logLocation);
//					log.info("For a list of available commands use:  --help");
//					log.info("Please see above for the available options. Further information is available in the following documents:");
//					log.info("- The FAQ (doc/faq.pdf) contains commonly asked questions regarding troubleshooting, and usage.");
//					log.info("- The Quickstart Guide (doc/quickstart.pdf) gives a simple example for getting up and running.");
//					log.info("- The Manual (doc/manual.pdf) contains detailed information on file format semantics.");
//
//					
//					
//					t = t.getCause();
//				} else
//				{
//					if(t instanceof ParameterException )
//					{
//						
//						System.err.println(t.getMessage());
//						t.printStackTrace();
//					} else
//					{
//						t.printStackTrace();
//					}
//					
//				}
//		
//				
//				if(t instanceof ParameterException)
//				{
//					return ACLibReturnValues.PARAMETER_EXCEPTION;
//				}
//				
//				if(t instanceof StateSerializationException)
//				{
//					return ACLibReturnValues.SERIALIZATION_EXCEPTION;
//				}
//				
//				if(t instanceof TrajectoryDivergenceException)
//				{
//					return ACLibReturnValues.TRAJECTORY_DIVERGENCE;
//				}
//				
//				return ACLibReturnValues.OTHER_EXCEPTION;
//		}
//		
//		
//	}
//	
//
//	
//
//	
//	private static void restoreState(SMACOptions options, StateFactory sf, AbstractAlgorithmFramework smac,  ParamConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfig execConfig, RunHistory rh) {
//		
//		if(options.stateOpts.restoreIteration < 0)
//		{
//			throw new ParameterException("Iteration must be a non-negative integer");
//		}
//		
//		StateDeserializer sd = sf.getStateDeserializer("it", options.stateOpts.restoreIteration, configSpace, instances, execConfig, rh);
//		
//		smac.restoreState(sd);
//	}
//
//	private static String runGroupName = "DEFAULT";
//	
//	/**
//	 * Parsers Command Line Arguments and returns a options object
//	 * @param args
//	 * @return
//	 */
//	private static SMACOptions parseCLIOptions(String[] args) throws ParameterException, IOException
//	{
//		//DO NOT LOG UNTIL AFTER WE PARSE CONFIG OBJECT
//		
//		SMACOptions options = new SMACOptions();
//		taeOptions = options.scenarioConfig.algoExecOptions.taeOpts.getAvailableTargetAlgorithmEvaluators();
//		JCommander jcom = JCommanderHelper.getJCommanderAndCheckForHelp(args, options, taeOptions);
//		
//		jcom.setProgramName("smac");
//		
//		try {
//			
//			
//			//JCommanderHelper.parse(com, args);
//			try {
//				try {
//				
//				
//				args = processScenarioStateRestore(args);
//				jcom.parse(args);
//				} finally
//				{
//					runGroupName = options.runGroupOptions.getFailbackRunGroup();
//				}
//				
//				
//
//				if(options.adaptiveCapping == null)
//				{
//					switch(options.scenarioConfig.runObj)
//					{
//					case RUNTIME:
//						options.adaptiveCapping = true;
//						break;
//						
//					case QUALITY:
//						options.adaptiveCapping = false;
//						break;
//						
//					default:
//						//You need to add something new here
//						throw new IllegalStateException("Not sure what to default too");
//					}
//				}
//				
//				if(options.randomForestOptions.logModel == null)
//				{
//					switch(options.scenarioConfig.runObj)
//					{
//					case RUNTIME:
//						options.randomForestOptions.logModel = true;
//						break;
//					case QUALITY:
//						options.randomForestOptions.logModel = false;
//					}
//				}
//				
//				
//				
//				
//				
//				
//				runGroupName = options.getRunGroupName(taeOptions.values());
//				//File outputDir = new File(options.scenarioConfig.outputDirectory);
//				
//				
//				String runGroupName = options.getRunGroupName(taeOptions.values());;
//				/*
//				 * Build the Serializer object used in the model 
//				 */
//				outputDir = options.getOutputDirectory(runGroupName);
//			
//				File outputDirFile = new File(outputDir);
//				
//				if(!outputDirFile.exists())
//				{
//					boolean result = outputDirFile.mkdirs();
//					
//					if(!result)
//					{
//						throw new ParameterException("Could not create all folders necessary for output directory: " + outputDir);
//					}
//				}
//				
//				
//			} finally
//			{
//				
//				options.logOptions.initializeLogging(outputDir, options.seedOptions.numRun);
//				AutomaticConfigurator.logLocation = options.logOptions.getLogLocation(outputDir,options.seedOptions.numRun);
//				
//				log = LoggerFactory.getLogger(AutomaticConfigurator.class);
//				
//				exception = MarkerFactory.getMarker("EXCEPTION");
//				stackTrace = MarkerFactory.getMarker("STACKTRACE");
//				
//				VersionTracker.setClassLoader(SPIClassLoaderHelper.getClassLoader());
//				VersionTracker.logVersions();
//				
//				for(String name : jcom.getParameterFilesToRead())
//				{
//					log.info("Parsing (default) options from file: {} ", name);
//				}
//				
//			}
//			
//			log.trace("Command Line Options Parsed");
//			
//			
//			
//			
//			validateObjectiveCombinations(options.scenarioConfig, options.adaptiveCapping);
//			
//			JCommanderHelper.logCallString(args, AutomaticConfigurator.class);
//			
//		
//			
//			
//			 
//			 if(log.isDebugEnabled())
//			 {
//				Map<String, String> env = new TreeMap<String, String>(System.getenv());
//					
//				StringBuilder sb = new StringBuilder();
//				 for (String envName : env.keySet()) {
//					 sb.append(envName).append("=").append(env.get(envName)).append("\n");
//					 
//			           
//			        }
//				
//				
//					 
//				 log.debug("==========Enviroment Variables===========\n{}", sb.toString());
//				 
//				 
//				 Map<Object,Object > props = new TreeMap<Object, Object>(System.getProperties());
//				 sb = new StringBuilder();
//				 for (Entry<Object, Object> ent : props.entrySet())
//				 {
//					 
//					 sb.append(ent.getKey().toString()).append("=").append(ent.getValue().toString()).append("\n");
//					 
//			           
//			     }
//				
//				 String hostname = "[UNABLE TO DETERMINE HOSTNAME]";
//				try {
//					hostname = InetAddress.getLocalHost().getHostName();
//				} catch(UnknownHostException e)
//				{ //If this fails it's okay we just use it to output to the log
//					
//				}
//				
//				log.debug("Hostname:{}", hostname);
//				log.debug("==========System Properties==============\n{}", sb.toString() );
//			 }
//			
//			JCommanderHelper.logConfiguration(jcom);
//			pool = options.seedOptions.getSeedableRandomPool();
//			
//			
//			
//			
//			TrainTestInstances tti = options.getTrainingAndTestProblemInstances(pool);
//			trainingILWS = tti.getTrainingInstances();
//			testingILWS = tti.getTestInstances();
//		
//			
//			
//			try {
//				//We don't handle this more gracefully because this seems like a super rare incident.
//				if(ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled())
//				{
//					log.debug("JVM Supports CPU Timing Measurements");
//				} else
//				{
//					log.warn("This Java Virtual Machine has CPU Time Measurements disabled, tunerTimeout will not contain any SMAC Execution Time.");
//				}
//			} catch(UnsupportedOperationException e)
//			{
//				log.warn("This Java Virtual Machine does not support CPU Time Measurements, tunerTimeout will not contain any SMAC Execution Time Information (http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/ThreadMXBean.html#setThreadCpuTimeEnabled(boolean))");
//			}
//			
//			if(options.seedOptions.numRun + options.seedOptions.seedOffset < 0)
//			{
//				log.warn("NumRun {} plus Seed Offset {} should be positive, things may not seed correctly",options.seedOptions.numRun, options.seedOptions.seedOffset );
//			}
//			return options;
//		} catch(IOException e)
//		{
//			throw e;
//			
//		} catch(ParameterException e)
//		{
//		
//			
//			throw e;
//		}
//	}
//	
//
//	
//	private static String[] processScenarioStateRestore(String[] args) {
//		
//		
//		ArrayList<String> inputArgs = new ArrayList<String>(Arrays.asList(args));
//		
//		
//		ListIterator<String> inputIt =  inputArgs.listIterator();
//		
//		
//		Iterator<String> firstPass = inputArgs.iterator();
//		
//		
//		boolean foundIteration = false;
//		while(firstPass.hasNext())
//		{
//			String arg = firstPass.next();
//			if(arg.trim().equals("--restoreIteration") || arg.trim().equals("--restoreStateIteration"))
//			{
//				if(firstPass.hasNext())
//				{
//					foundIteration= true;
//				}
//			}
//		}
//		while(inputIt.hasNext())
//		{
//			String input = inputIt.next();
//			
//			if(input.trim().equals("--restoreScenario"))
//			{
//				if(!inputIt.hasNext())
//				{
//					throw new ParameterException("Failed to parse argument --restoreScenario expected 1 more argument");
//				} else
//				{
//					String dir = inputIt.next();
//					
//					
//					inputIt.add("--restoreStateFrom");
//					inputIt.add(dir);
//					if(!foundIteration)
//					{
//						inputIt.add("--restoreIteration");
//						inputIt.add(String.valueOf(Integer.MAX_VALUE));
//					}
//					inputIt.add("--scenarioFile");
//					inputIt.add(dir + File.separator + "scenario.txt");
//					inputIt.add("--instanceFeatureFile");
//					inputIt.add(dir + File.separator + "instance-features.txt");
//					inputIt.add("--instanceFile");
//					inputIt.add(dir + File.separator + "instances.txt");
//					inputIt.add("--paramFile");
//					inputIt.add(dir + File.separator + "param-file.txt");
//					inputIt.add("--testInstanceFile");
//					inputIt.add(dir + File.separator + "instances.txt");
//					
//				}
//				
//				
//			}
//			
//		}
//		
//		return inputArgs.toArray(new String[0]);
//	}
//
//
//
//
//	/**
//	 * Validates the various objective functions and ensures that they are legal together
//	 * @param scenarioConfig
//	 */
//	private static void validateObjectiveCombinations(
//			ScenarioOptions scenarioConfig, boolean adaptiveCapping) {
//
//		switch(scenarioConfig.interInstanceObj)
//		{
//			case MEAN:
//				//Okay
//				break;
//			default:
//				throw new ParameterException("Model does not currently support an inter-instance objective other than " +  OverallObjective.MEAN);
//				
//		}
//		
//		
//		
//		
//		switch(scenarioConfig.runObj)
//		{
//			case RUNTIME:
//				break;
//			
//			case QUALITY:
//				if(!scenarioConfig.intraInstanceObj.equals(OverallObjective.MEAN))
//				{
//					throw new ParameterException("To optimize quality you MUST use an intra-instance objective of " + OverallObjective.MEAN);
//				}
//				
//				if(adaptiveCapping)
//				{
//					throw new ParameterException("You can only use Adaptive Capping when using " + RunObjective.RUNTIME + " as an objective");
//				}
//				
//		}
//	}
//
//
//
//	
//	
//	private static Pattern modelHashCodePattern = Pattern.compile("^(Preprocessed|Random) Forest Built with Hash Code:\\s*\\d+?\\z");
//	
//	
//	private static void parseModelHashCodes(File modelHashCodeFile) {
//		log.info("Model Hash Code File Passed {}", modelHashCodeFile.getAbsolutePath());
//		Queue<Integer> modelHashCodeQueue = new LinkedList<Integer>();
//		Queue<Integer> preprocessedHashCodeQueue = new LinkedList<Integer>();
//		
//		BufferedReader bin = null;
//		try {
//			try{
//				bin = new BufferedReader(new FileReader(modelHashCodeFile));
//			
//				String line;
//				int hashCodeCount=0;
//				int lineCount = 1;
//				while((line = bin.readLine()) != null)
//				{
//					
//					Matcher m = modelHashCodePattern.matcher(line);
//					if(m.find())
//					{
//						Object[] array = { ++hashCodeCount, lineCount, line};
//						log.debug("Found Model Hash Code #{} on line #{} with contents:{}", array);
//						boolean preprocessed = line.substring(0,1).equals("P");
//						
//						int colonIndex = line.indexOf(":");
//						
//						String lineSubStr = line.substring(colonIndex+1).trim();
//						
//						if(!preprocessed)
//						{
//							modelHashCodeQueue.add(Integer.valueOf(lineSubStr));
//						} else
//						{
//							preprocessedHashCodeQueue.add(Integer.valueOf(lineSubStr));
//						}
//						
//					} else
//					{
//						log.trace("No Hash Code found on line: {}", line );
//					}
//					lineCount++;
//				}
//				if(hashCodeCount == 0)
//				{
//					log.warn("Hash Code File Specified, but we found no hash codes");
//				}
//			} finally
//			{
//				if(bin != null) bin.close();
//			}
//		} catch(IOException e)
//		{
//			throw new RuntimeException(e);
//		}
//		
//		//Who ever is looking at this code, I can feel your disgust.
//		HashCodeVerifyingModelBuilder.modelHashes = modelHashCodeQueue;
//		HashCodeVerifyingModelBuilder.preprocessedHashes = preprocessedHashCodeQueue;
//		
//	}
//	
//	
//	
//
//	
//}
