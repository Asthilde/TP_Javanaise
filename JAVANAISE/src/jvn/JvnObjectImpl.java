package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject, Serializable {

	private static final long serialVersionUID = 3L;
	
	private static final long lockTimeOut = 5000;

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
	public synchronized void jvnLockRead() throws JvnException, JvnLockException {
		if (lockState == LockState.R || lockState == LockState.RC || lockState == LockState.W) {
			lockState = LockState.R;
		} else if (lockState == LockState.WC) {
			lockState = LockState.RWC;
		} else if (lockState == LockState.NL) {
			object = js.jvnLockRead(id);
			lockState = LockState.R;
		}
	}

	@Override
	public synchronized void jvnLockWrite() throws JvnException, JvnLockException {
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
			this.notifyAll();
		} else if (lockState == LockState.W) {
			lockState = LockState.WC;
			this.notifyAll();
		} else if (lockState == LockState.R) {
			lockState = LockState.RC;
			this.notifyAll();
		}
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		return id;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		if (object == null) {
			throw new JvnException("L'objet partagé n'est pas instancié !");
		}
		return object;
	}

	@Override
	public synchronized void jvnInvalidateReader() throws JvnException, JvnLockException {
		if (lockState == LockState.W || lockState == LockState.WC) {
			throw new JvnException("L'objet n'est pas verrouillé en lecture sur cette machine !");
		}
		while (lockState == LockState.R) {
			try {
				this.wait(lockTimeOut);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new JvnLockException("Verrou pas relaché !");
			}
			if(lockState == LockState.R) {
				throw new JvnLockException("Verrou pas relaché !");
			}
		}
		lockState = LockState.NL;
	}

	@Override
	public synchronized Serializable jvnInvalidateWriter() throws JvnException, JvnLockException {
		if (lockState == LockState.R) {
			throw new JvnException("L'objet n'est pas verrouillé en écriture sur cette machine !");
		}
		while (lockState == LockState.W) {
			try {
				this.wait(lockTimeOut);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(lockState == LockState.W) {
				throw new JvnLockException("Verrou pas relaché !");
			}
		}
		lockState = LockState.NL;
		return lockState;
	}

	@Override
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException, JvnLockException {
		if (lockState == LockState.R) {
			throw new JvnException("L'objet n'est pas verrouillé en écriture sur cette machine !");
		}
		while (lockState == LockState.W) {
			try {
				this.wait(lockTimeOut);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(lockState == LockState.W) {
				throw new JvnLockException("Verrou pas relaché !");
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
