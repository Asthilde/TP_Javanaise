package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject, Serializable {

	private static final long serialVersionUID = 3L;

	private LockState lockState;
	private int id;
	private Serializable object;
	private transient JvnLocalServer js;

	public JvnObjectImpl(Serializable sharedObject, int id, JvnLocalServer js) {
		lockState = LockState.W;
		this.id = id;
		object = sharedObject;
		this.js = js;
	}

	@Override
	public synchronized void jvnLockRead() throws JvnException {
		if (lockState == LockState.R || lockState == LockState.RC || lockState == LockState.W) {
			lockState = LockState.R;
		} else if (lockState == LockState.RC) {
			lockState = LockState.RWC;
		} else if (lockState == LockState.NL) {
			object = js.jvnLockRead(id);
			lockState = LockState.R;
		}
	}

	@Override
	public synchronized void jvnLockWrite() throws JvnException {
		if (lockState == LockState.W || lockState == LockState.WC || lockState == LockState.RWC) {
			lockState = LockState.W;
		} else if (lockState == LockState.R || lockState == LockState.RC || lockState == LockState.NL) {
			object = js.jvnLockWrite(id);
			lockState = LockState.W;
		}
	}

	@Override
	public synchronized void jvnUnLock() throws JvnException {
		if (lockState == LockState.NL) {
			throw new JvnException("The current machine does not hold a lock on the object.");
		} else if (lockState == LockState.W) {
			lockState = LockState.WC;
			this.notify();
		} else if (lockState == LockState.R) {
			lockState = LockState.RC;
			this.notify();
		}
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		return id;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		if (object == null) {
			throw new JvnException("The shared object is not instantiated!");
		}
		return object;
	}

	@Override
	public synchronized void jvnInvalidateReader() throws JvnException {
		if (lockState == LockState.W || lockState == LockState.WC || lockState == LockState.RWC) {
			throw new JvnException("The object is not read-locked on this machine!");
		}
		while (lockState == LockState.R) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		lockState = LockState.NL;
	}

	@Override
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		if (lockState == LockState.R) {
			throw new JvnException("The object is not read-locked on this machine!");
		}
		while (lockState == LockState.W) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		lockState = LockState.NL;
		return lockState;
	}

	@Override
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
		if (lockState == LockState.R) {
			throw new JvnException("The object is not write-locked on this machine!");
		}
		while (lockState == LockState.W) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (lockState == LockState.RWC) {
			lockState = LockState.R;
		} else {
			lockState = LockState.RC;
		}
		return lockState;
	}

	@Override
	public Serializable jvnGetState() throws JvnException {
		return lockState;
	}

	@Override
	public void jvnChangeState(Serializable newState) throws JvnException {
		lockState = (LockState) newState;
	}
}
