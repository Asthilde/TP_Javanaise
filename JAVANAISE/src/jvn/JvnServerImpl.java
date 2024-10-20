/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import irc.Sentence;

import java.io.*;



public class JvnServerImpl 	
extends UnicastRemoteObject 
implements JvnLocalServer, JvnRemoteServer{

	private JvnRemoteCoord coordinator; // Ref coord
	private HashMap<Integer, JvnObject> objectStore; // To store JVN objects
	private HashMap<String, Integer> nameRegistry; // To store names

	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;

	/**
	 * Default constructor
	 * @throws JvnException
	 **/
	private JvnServerImpl() throws Exception {
		super();
		try {
			Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Port to be corrected if necessary
			coordinator = (JvnRemoteCoord) registry.lookup("Coordinator");
			objectStore = new HashMap<Integer, JvnObject>();
			nameRegistry = new HashMap<String, Integer>();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					jvnTerminate();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}));
		} catch (Exception e) {
			throw new JvnException("Error connecting to coordinator : " + e.getMessage());
		}
	}
	

	/**
	 * Static method allowing an application to get a reference to 
	 * a JVN server instance
	 * @throws JvnException
	 **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null) {
			synchronized (JvnServerImpl.class) {
				if (js == null) { // Double-check locking
					try {
						js = new JvnServerImpl();
					} catch (Exception e) {
						System.err.println("Error initializing JVN server: " + e.getMessage());
						return null;
					}
				}
			}
		}
		return js;
	}


	/**
	 * The JVN service is not used anymore
	 * @throws JvnException
	 **/
	public void jvnTerminate()
			throws jvn.JvnException {
		try {
			for (Map.Entry<Integer, JvnObject> entry : objectStore.entrySet()) {
				JvnObject obj = entry.getValue();
				synchronized (obj) {
					if(((LockState) obj.jvnGetState()) == LockState.R || ((LockState) obj.jvnGetState()) == LockState.W) {
						obj.wait();
						obj.jvnChangeState(LockState.NL);
					} else {
						obj.jvnChangeState(LockState.NL);
					}
				}
			}
			nameRegistry.clear();
			objectStore.clear();
			coordinator.jvnTerminate(js);
		} catch (RemoteException e) {
			throw new JvnException("Termination error : " + e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	} 

	/**
	 * creation of a JVN object
	 * @param o : the JVN object state
	 * @throws JvnException
	 **/
	public JvnObject jvnCreateObject(Serializable o)
			throws jvn.JvnException { 
		JvnObject jvnObj = null;
		try {
			jvnObj = new JvnObjectImpl(o, coordinator.jvnGetObjectId(), js);
			objectStore.put(jvnObj.jvnGetObjectId(), jvnObj);
			jvnObj.jvnLockWrite();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return jvnObj; 
	}

	/**
	 *  Associate a symbolic name with a JVN object
	 * @param jon : the JVN object name
	 * @param jo : the JVN object 
	 * @throws JvnException
	 **/
	public void jvnRegisterObject(String jon, JvnObject jo) throws JvnException {
		int id = jo.jvnGetObjectId();
		if (nameRegistry.containsKey(jon)) {
			throw new JvnException("Registered name");
		}
		nameRegistry.put(jon, id);
		try {
			coordinator.jvnRegisterObject(jon, jo, id, js);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (JvnException e) {
			e.printStackTrace();
		}
	}




	/**
	 * Provide the reference of a JVN object beeing given its symbolic name
	 * @param jon : the JVN object name
	 * @return the JVN object 
	 * @throws JvnException
	 **/
	public JvnObject jvnLookupObject(String jon) throws JvnException {   
		Integer id = nameRegistry.get(jon);
		JvnObject localObject = null;

		if (id != null) {
			localObject = objectStore.get(id);
		}

		if (localObject == null) {
			try {
				localObject = coordinator.jvnLookupObject(jon, js);
				if(localObject == null) {
					return null;
				}
				localObject = new JvnObjectImpl(((Sentence)localObject.jvnGetSharedObject()), localObject.jvnGetObjectId(), js);
				localObject.jvnChangeState(LockState.NL);
				objectStore.put(localObject.jvnGetObjectId(), localObject);
				nameRegistry.put(jon, localObject.jvnGetObjectId());
			} catch (Exception e) {
				throw new JvnException("Error retrieving coordinator object :" + e.getMessage());
			}
		} else {
			if (localObject.jvnGetState() == LockState.NL) {
				try {
					JvnObject updatedObject = coordinator.jvnLookupObject(jon, js);
					if (updatedObject != null) {
						localObject = new JvnObjectImpl(((Sentence)updatedObject.jvnGetSharedObject()), updatedObject.jvnGetObjectId(), js);
						localObject.jvnChangeState(LockState.NL);
						objectStore.put(id, localObject);
					}
				} catch (Exception e) {
					throw new JvnException("Error retrieving updated object from coordinator : " + e.getMessage());
				}
			}
		}
		return localObject;
	}


	/**
	 * Get a Read lock on a JVN object 
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockRead(int joi)
			throws JvnException {
		JvnObject obj = objectStore.get(joi);
		if (obj == null) {
			throw new JvnException("Object not found in machine");
		}
		synchronized (obj) {
			try {
				JvnObject updatedObject = (JvnObject) coordinator.jvnLockRead(joi, js);
				return updatedObject.jvnGetSharedObject();
			} catch (Exception e) {
				throw new JvnException("Error when requesting a read lock from the coordinator : " + e.getMessage());
			}
		}
	}

	/**
	 * Get a Write lock on a JVN object 
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws  JvnException
	 **/
	public Serializable jvnLockWrite(int joi)
			throws JvnException {
		JvnObject obj = objectStore.get(joi);
		if (obj == null) {
			throw new JvnException("Object not found in machine");
		}
		synchronized (obj) {
			try {
				JvnObject updatedObject = (JvnObject) coordinator.jvnLockWrite(joi, js);
				return updatedObject.jvnGetSharedObject();
			} catch (Exception e) {
				throw new JvnException("Error when requesting a write lock from the coordinator : " + e.getMessage());
			}
		}
	}	


	/**
	 * Invalidate the Read lock of the JVN object identified by id 
	 * called by the JvnCoord
	 * @param joi : the JVN object id
	 * @return void
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public void jvnInvalidateReader(int joi)
			throws java.rmi.RemoteException,jvn.JvnException {
		JvnObject obj = objectStore.get(joi);
		if (obj == null) {
			throw new JvnException("Object not found for ID : " + joi);
		}
		synchronized (obj) {
			obj.jvnInvalidateReader();
			obj.notify();
		}
	};


	/**
	 * Invalidate the Write lock of the JVN object identified by id 
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriter(int joi)
			throws java.rmi.RemoteException,jvn.JvnException { 

		JvnObject obj = objectStore.get(joi);
		if (obj == null) {
			throw new JvnException("Object not found for ID : " + joi);
		}
		synchronized (obj) {
			while(obj.jvnInvalidateWriter() != LockState.NL) {
				try {
					obj.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			obj.notify();
		}
		return obj;
	};

	/**
	 * Reduce the Write lock of the JVN object identified by id 
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriterForReader(int joi)
			throws java.rmi.RemoteException,jvn.JvnException { 
		JvnObject obj = objectStore.get(joi);
		if (obj == null) {
			throw new JvnException("Object not found for ID : " + joi);
		}
		synchronized (obj) {
			while(obj.jvnInvalidateWriterForReader() != LockState.R && obj.jvnInvalidateWriterForReader() != LockState.RC && obj.jvnInvalidateWriterForReader() != LockState.NL) {
				try {
					obj.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			obj.notify();
		}
		return obj;
	};

}


