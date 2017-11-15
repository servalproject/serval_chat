package org.servalproject.servalchat.views;

import org.servalproject.mid.Serval;
import org.servalproject.servaldna.ServalDInterfaceException;
import org.servalproject.servaldna.meshms.MeshMSException;

import java.io.IOException;

/**
 * Created by jeremy on 15/11/17.
 */

// A simple background worker, with simpler exception handling than using AsncTask
public abstract class BackgroundWorker implements Runnable{
	private final Serval serval;
	private int state = IDLE;
	private Throwable ex;
	static final int IDLE=0;
	static final int RUNNING=1;

	protected BackgroundWorker(){
		this.serval = Serval.getInstance();
	}

	protected abstract void onBackGround() throws Exception;
	protected abstract void onComplete(Throwable t);

	public boolean isRunning(){
		return state == RUNNING;
	}

	protected void rethrow(Throwable t){
		if (t == null)
			return;
		if (t instanceof RuntimeException)
			throw (RuntimeException) t;
		throw new IllegalStateException(t);
	}

	@Override
	public void run() {
		if (state!=RUNNING)
			throw new IllegalStateException();
		if (serval.uiHandler.isOnThread()){
			state = IDLE;
			Throwable e = this.ex;
			ex = null;
			onComplete(e);
		}else{
			try {
				onBackGround();
			}catch (Throwable t){
				ex = t;
			}
			serval.uiHandler.post(this);
		}
	}

	public void execute(){
		if (state != IDLE)
			throw new IllegalStateException();
		state = RUNNING;
		serval.runOnThreadPool(this);
	}
}
