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
		} else if (lockState == LockState.WC) {
			lockState = LockState.RWC;
		} else if (lockState == LockState.NL) {
			lockState = LockState.R;
			object = js.jvnLockRead(id);
			System.out.println("Je reçois l'objet " + object.toString());
		}
	}

	@Override
	public synchronized void jvnLockWrite() throws JvnException {
		if (lockState == LockState.W || lockState == LockState.WC || lockState == LockState.RWC) {
			lockState = LockState.W;
		} else if (lockState == LockState.R || lockState == LockState.RC || lockState == LockState.NL) {
			lockState = LockState.W;
			object = js.jvnLockWrite(id);
			System.out.println("Je reçois l'objet " + object.toString());

		}
	}

	@Override
	public synchronized void jvnUnLock() throws JvnException {
		System.out.print("Je suis unlock, ");
		if (lockState == LockState.NL) {
			this.notifyAll();
		} else if (lockState == LockState.W) {
			lockState = LockState.WC;
			this.notifyAll();
		} else if (lockState == LockState.R) {
			lockState = LockState.RC;
			this.notifyAll();
		}
		System.out.println("Mon état est : " + lockState);
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
	public synchronized void jvnInvalidateReader() throws JvnException {
		if (lockState == LockState.W || lockState == LockState.WC || lockState == LockState.RWC) {
			throw new JvnException("L'objet n'est pas verrouillé en lecture sur cette machine !");
		}
		System.out.println("Je reçois invalidateReader en tant qu'objet, mon état est : " + this.lockState);
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
			throw new JvnException("L'objet n'est pas verrouillé en écriture sur cette machine !");
		}
		System.out.println("Je reçois invalidateWriter en tant qu'objet, mon état est : " + this.lockState);
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
			throw new JvnException("L'objet n'est pas verrouillé en écriture sur cette machine !");
		}
		System.out.println("Je reçois invalidateWriterForReader en tant qu'objet, mon état est : " + this.lockState);
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
