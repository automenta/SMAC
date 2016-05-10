package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.mechanism;


import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.encoding.EncodingMechanism;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

@ThreadSafe
public class TCPMechanism {


	private final Logger log = LoggerFactory.getLogger(getClass());
	private final EncodingMechanism enc;
	
	public TCPMechanism(EncodingMechanism enc) {
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
	public AlgorithmRunResult evaluateRun(AlgorithmRunConfiguration rc, String remoteHost, int remotePort) 
	{

		try {
			Socket clientSocket = new Socket(remoteHost, remotePort);
			try 
			{
				BufferedOutputStream bout = new BufferedOutputStream(clientSocket.getOutputStream()); 
				StopWatch watch = new AutoStartStopWatch();
				
				bout.write(enc.getOutputBytes(rc));
				bout.flush();
		
				return enc.getInputBytes(rc, clientSocket.getInputStream(), watch);
			} finally
			{
				clientSocket.close();
			}
		} catch (IOException e) {
			log.error("Error creating socket, trying connection again in 10 seconds",e);
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				throw new TargetAlgorithmAbortException(e1);
			}
			
			return evaluateRun(rc,  remoteHost, remotePort);
		}
	}

}
