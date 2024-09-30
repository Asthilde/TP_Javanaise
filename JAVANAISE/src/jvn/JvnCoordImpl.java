/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;
import java.rmi.registry.*;
import java.rmi.server.*;


public class JvnCoordImpl 	
extends UnicastRemoteObject 
implements JvnRemoteCoord{


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static int objectId;
	private static HashMap<Integer, JvnObject> objectsIdMap;
	private static HashMap<String, Integer> objectsNameMap;
	private static HashMap<Integer, HashMap<JvnRemoteServer, LockState>> objectsLockMap;


	/**
	 * Default constructor
	 * @throws JvnException
	 **/
	private JvnCoordImpl() throws Exception {
		// to be completed
		if(objectId == 0) {
			objectId = 1;
		}
		if(objectsIdMap == null) {
			objectsIdMap = new HashMap<Integer, JvnObject>();
		}
		if(objectsNameMap == null) {
			objectsNameMap = new HashMap<String, Integer>();
		}
		if(objectsLockMap == null) {
			objectsLockMap = new HashMap<Integer, HashMap<JvnRemoteServer, LockState>>();
		}
	}

	/**
	 *  Allocate a NEW JVN object id (usually allocated to a 
	 *  newly created JVN object)
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public int jvnGetObjectId()
			throws java.rmi.RemoteException,jvn.JvnException {
		// to be completed 
		JvnCoordImpl.objectId += 1;
		return JvnCoordImpl.objectId-1;
	}

	/**
	 * Associate a symbolic name with a JVN object
	 * @param jon : the JVN object name
	 * @param jo  : the JVN object 
	 * @param joi : the JVN object identification
	 * @param js  : the remote reference of the JVNServer
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public void jvnRegisterObject(String jon, JvnObject jo, int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException,jvn.JvnException{
		// to be completed 
		if(!JvnCoordImpl.objectsIdMap.containsKey(joi)) {
			JvnCoordImpl.objectsIdMap.put(joi, jo);
		}
		if(!JvnCoordImpl.objectsNameMap.containsKey(jon)) {
			JvnCoordImpl.objectsNameMap.put(jon, joi);
		}
		if(!JvnCoordImpl.objectsLockMap.containsKey(joi)) {
			HashMap<JvnRemoteServer, LockState> serverLock = new HashMap<JvnRemoteServer, LockState>();
			serverLock.put(js, LockState.W);
			JvnCoordImpl.objectsLockMap.put(joi, serverLock);
		}
	}

	/**
	 * Get the reference of a JVN object managed by a given JVN server 
	 * @param jon : the JVN object name
	 * @param js : the remote reference of the JVNServer
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
			throws java.rmi.RemoteException,jvn.JvnException{
		// to be completed 
		if(JvnCoordImpl.objectsNameMap.containsKey(jon)) {
			Integer objId = JvnCoordImpl.objectsNameMap.get(jon);
			HashMap<JvnRemoteServer, LockState> serversMap = objectsLockMap.get(objId);
			if(serversMap != null && !serversMap.containsKey(js)) {
				serversMap.put(js, LockState.NL);
				objectsLockMap.put(objId,serversMap);
			}
			return JvnCoordImpl.objectsIdMap.get(objId);
		}
		else { 
			throw new JvnException("L'objet n'est pas référencé dans le coordinateur");
		}
	}

	/**
	 * Get a Read lock on a JVN object managed by a given JVN server 
	 * @param joi : the JVN object identification
	 * @param js  : the remote reference of the server
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException, JvnException
	 **/
	public Serializable jvnLockRead(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException{
		// to be completed
		JvnObject objFound = JvnCoordImpl.objectsIdMap.get(joi); 
		if(objFound != null)
		{
			HashMap<JvnRemoteServer, LockState> currentLocks = objectsLockMap.get(joi);
			if(currentLocks == null) {
				HashMap<JvnRemoteServer, LockState> lockStateMap = new HashMap<JvnRemoteServer, LockState> ();
				lockStateMap.put(js, LockState.R);
				objectsLockMap.put(joi,lockStateMap);
				return LockState.R;
			}
			else if(currentLocks.get(js) == LockState.NL || currentLocks.get(js) == null) {
				if(currentLocks.containsValue(LockState.W)) {
					for(Map.Entry<JvnRemoteServer, LockState> server : currentLocks.entrySet()) {
						if(server.getValue() == LockState.W) {
							try {
								objectsIdMap.get(joi).wait();
								JvnObject newObject = (JvnObject) server.getKey().jvnInvalidateWriterForReader(joi);
								objectsIdMap.put(joi, newObject);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							currentLocks.put(server.getKey(), LockState.R);
							currentLocks.put(js, LockState.R);
							objectsLockMap.put(joi,currentLocks);
							return LockState.R;
						}
					}
				}
				else {
					currentLocks.put(js, LockState.R);
					objectsLockMap.put(joi,currentLocks);
					return LockState.R;
				}
			}
		}
		throw new JvnException("L'objet n'est pas référencé dans le coordinateur");
	}

	/**
	 * Get a Write lock on a JVN object managed by a given JVN server 
	 * @param joi : the JVN object identification
	 * @param js  : the remote reference of the server
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException, JvnException
	 **/
	public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException{
		// to be completed
		JvnObject objFound = JvnCoordImpl.objectsIdMap.get(joi); 
		if(objFound != null)
		{
			HashMap<JvnRemoteServer, LockState> currentLocks = objectsLockMap.get(joi);
			if(currentLocks == null) {
				HashMap<JvnRemoteServer, LockState> lockStateMap = new HashMap<JvnRemoteServer, LockState> ();
				lockStateMap.put(js, LockState.W);
				objectsLockMap.put(joi,lockStateMap);
				return LockState.W;
			}
			else if(currentLocks.get(js) == LockState.NL || currentLocks.get(js) == null) {
				if(currentLocks.containsValue(LockState.W)) {
					for(Map.Entry<JvnRemoteServer, LockState> server : currentLocks.entrySet()) {
						if(server.getValue() == LockState.W) {
							// TODO
							try {
								objectsIdMap.get(joi).wait();
								JvnObject newObject = (JvnObject) server.getKey().jvnInvalidateWriter(joi);
								objectsIdMap.put(joi, newObject);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							currentLocks.remove(server.getKey());
							currentLocks.put(js, LockState.W);
							objectsLockMap.put(joi,currentLocks);
							return LockState.W;
						}
					}
				}
				else if(currentLocks.containsValue(LockState.R)) {
					for(Map.Entry<JvnRemoteServer, LockState> server : currentLocks.entrySet()) {
						if(server.getValue() == LockState.R && server.getKey() != js) {
							try {
								// Potentiellement erreur si itération sur un truc supprimé
								objectsIdMap.get(joi).wait();
								server.getKey().jvnInvalidateReader(joi);
								currentLocks.remove(server.getKey());
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					currentLocks.put(js, LockState.W);
					objectsLockMap.put(joi,currentLocks);
					return LockState.W;
				}
				else {
					currentLocks.put(js, LockState.W);
					objectsLockMap.put(joi,currentLocks);
					return LockState.W;
				}
			}
		}
		throw new JvnException("L'objet n'est pas référencé dans le coordinateur");
	}

	/**
	 * A JVN server terminates
	 * @param js  : the remote reference of the server
	 * @throws java.rmi.RemoteException, JvnException
	 **/
	public void jvnTerminate(JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		// to be completed
		if(objectsLockMap.isEmpty()) {
			throw new JvnException("Le serveur JVN n'est pas connu par coordinateur");
		}
		else {
			for(Map.Entry<Integer, HashMap<JvnRemoteServer, LockState>> objectsMap : objectsLockMap.entrySet()) {
				for(Map.Entry<JvnRemoteServer, LockState> server : objectsMap.getValue().entrySet()) {
					if(server.getKey() == js) {
						if(server.getValue() == LockState.R) {
							// TODO
							// Wait for the resource after write has been finished
							server.getKey().jvnInvalidateReader(objectsMap.getKey());
						}
						else if(server.getValue() == LockState.W) {
							// TODO
							// Wait for the resource after write has been finished
							server.getKey().jvnInvalidateWriter(objectsMap.getKey());
						}
						objectsMap.getValue().remove(server.getKey());
					}
				}
			}
		}
	}

	public static void main (String[] args) {
		try {
			JvnCoordImpl coordinator = new JvnCoordImpl();
			JvnCoordImpl coordStub = (JvnCoordImpl) UnicastRemoteObject.exportObject(coordinator, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Coordinator", coordStub);
            System.out.println("Coordinator ready");

		} catch(Exception e) {
			System.err.println(e.toString());
			e.getStackTrace();
		}
	}
}


