package ca.ubc.cs.beta.aeatk.concurrent;

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
public class DebugReentrantReadWriteLock extends ReentrantReadWriteLock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public DebugReentrantReadWriteLock()
	{
		super();
	}
	
	public DebugReentrantReadWriteLock(boolean fair)
	{
		super(fair);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder(super.toString());
		
		//sb.append("\n").append(this.readLock().)
		return sb.toString();
	}
}
