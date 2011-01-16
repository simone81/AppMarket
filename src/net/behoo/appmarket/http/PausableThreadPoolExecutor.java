package net.behoo.appmarket.http;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableThreadPoolExecutor extends ScheduledThreadPoolExecutor {
	private boolean isPaused = false;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	   
	public PausableThreadPoolExecutor(int corePoolSize) {
		super(corePoolSize);
	}

	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		pauseLock.lock();
		try {
			while (isPaused) unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}

	public void pause() {
		pauseLock.lock();
	    try {
	    	isPaused = true;
	    } finally {
	    	pauseLock.unlock();
	    }
	}

	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
}
