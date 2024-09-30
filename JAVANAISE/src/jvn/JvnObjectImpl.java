package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {

	private static final long serialVersionUID = 1L;

	private LockState lockState;
	private int id;
	private Serializable object;
	private JvnLocalServer js;

	public JvnObjectImpl(Serializable sharedObject, int id, JvnLocalServer js) {
		lockState = LockState.W;
		this.id = id;
		object = sharedObject;
		this.js = js;
	}

	@Override
	public void jvnLockRead() throws JvnException {
		// TODO Auto-generated method stub
	}

	@Override
	public void jvnLockWrite() throws JvnException {
		// TODO Auto-generated method stub

	}

	@Override
	public void jvnUnLock() throws JvnException {
		// TODO Auto-generated method stub

	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		// TODO Auto-generated method stub
		if (object == null) {
			throw new JvnException("L'objet partagé n'est pas instancié !");
		}
		return object;
	}

	@Override
	public synchronized void jvnInvalidateReader() throws JvnException {
		// TODO Auto-generated method stub
		if(lockState != LockState.R || lockState != LockState.RC || lockState != LockState.RWC) {
			throw new JvnException("L'objet n'est pas verrouillé en lecture sur cette machine !");
		}
		lockState = LockState.NL;
		try {
			this.wait();
		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.notify(); //ou notifyAll
	}

	@Override
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		// TODO Auto-generated method stub
		if(lockState != LockState.W || lockState != LockState.WC || lockState != LockState.RWC) {
			throw new JvnException("L'objet n'est pas verrouillé en écriture sur cette machine !");
		}
		try {
			this.wait();
			// object = ... Mise à jour de l'objet partagé ?
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lockState = LockState.NL;
		this.notify(); //ou notifyAll
		return this.lockState; // ou this.jvnGetSharedObject(); -> Ce qui modifie le coordinateur si c'est le cas !
	}

	@Override
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
		// TODO Auto-generated method stub
		if(lockState != LockState.W || lockState != LockState.WC || lockState != LockState.RWC) {
			throw new JvnException("L'objet n'est pas verrouillé en écriture sur cette machine !");
		}
		try {
			this.wait();
			// object = ... Mise à jour de l'objet partagé ?
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lockState = LockState.RC;
		this.notify(); //ou notifyAll
		return this.lockState; // ou this.jvnGetSharedObject(); -> Ce qui modifie le coordinateur si c'est le cas !
	}
	
	@Override
	public Serializable jvnGetState() throws JvnException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void jvnChangeState(Serializable newState) throws JvnException {
		// TODO Auto-generated method stub
		lockState = (LockState) newState;
	}
}
