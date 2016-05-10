package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.mechanism;


import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.EncodingMechanism;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReverseTCPMechanism {


	private final Logger log = LoggerFactory.getLogger(getClass());
	private EncodingMechanism enc;
	public ReverseTCPMechanism(EncodingMechanism enc) 
	{
		this.enc = enc;
		
	}

	/**
	 * 
	 * @param rc
	 * @param execConfig
	 * @param port
	 * @param remoteAddr
	 * @param udpPacketSize
	 * @return
	 */
	public AlgorithmRunResult evaluateRun(InputStream in, OutputStream out, AlgorithmRunConfiguration rc) throws IOException
	{
		try 
		{
			OutputStream bout = out; 
			
			bout.write(enc.getOutputBytes(rc));
		
			
			bout.flush();
			
			
			//AlgorithmRunResult run = rtcp.evaluateRun(in,out, rc);
			
			StopWatch watch = new AutoStartStopWatch();
		
			return enc.getInputBytes(rc,in, watch);
		
		} finally
		{
			//clientSocket.close();
		}
	}
}
