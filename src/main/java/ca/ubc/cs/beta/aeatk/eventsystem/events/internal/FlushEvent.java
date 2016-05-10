package ca.ubc.cs.beta.aeatk.eventsystem.events.internal;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;

import java.util.concurrent.Semaphore;

/**
 * Internal Event that is used to flush the event queue
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class FlushEvent extends AutomaticConfiguratorEvent
{

	private final Semaphore semaphore;

	public FlushEvent(Semaphore release) {
		this.semaphore = release;
	}
	
	public void releaseSemaphore()
	{
		this.semaphore.release();
	}
	
}