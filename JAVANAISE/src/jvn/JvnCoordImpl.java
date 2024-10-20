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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import irc.Sentence;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.*;


public class JvnCoordImpl 	
extends UnicastRemoteObject 
implements JvnRemoteCoord{

	private static final long serialVersionUID = 2L;
	private static int objectId;
	private static HashMap<Integer, JvnObject> objectsIdMap;
	private static HashMap<String, Integer> objectsNameMap;
	private static HashMap<Integer, HashMap<JvnRemoteServer, LockState>> objectsLockMap;
	private static List<JvnRemoteServer> writeAskServers = new ArrayList<JvnRemoteServer>();


	/**
	 * Default constructor
	 * @throws JvnException
	 **/
	private JvnCoordImpl() throws Exception {
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
			throws java.rmi.RemoteException,jvn.JvnException, JvnLockException {
		if(JvnCoordImpl.objectsNameMap.containsKey(jon)) {
			Integer objId = JvnCoordImpl.objectsNameMap.get(jon);
			HashMap<JvnRemoteServer, LockState> serversMap = objectsLockMap.get(objId);
			if(serversMap != null && !serversMap.containsKey(js)) {
				if(serversMap.containsValue(LockState.W)) {
					for(Map.Entry<JvnRemoteServer, LockState> server : serversMap.entrySet()) {
						if(server.getValue() == LockState.W) {
							JvnObject newObject;
							newObject = (JvnObject) server.getKey().jvnInvalidateWriterForReader(objId);
							objectsIdMap.put(objId, newObject);
							serversMap.put(server.getKey(), LockState.R);
							objectsLockMap.put(objId,serversMap);
							break;
						}
					}
				}
				serversMap.put(js, LockState.NL);
				objectsLockMap.put(objId,serversMap);
			}
			return JvnCoordImpl.objectsIdMap.get(objId);
		}
		else { 
			return null;
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
			throws java.rmi.RemoteException, JvnException, JvnLockException{
		JvnObject objFound = JvnCoordImpl.objectsIdMap.get(joi); 
		if(objFound != null)
		{
			HashMap<JvnRemoteServer, LockState> currentLocks = objectsLockMap.get(joi);
			if(currentLocks.get(js) == LockState.NL || currentLocks.get(js) == null) {
				if(currentLocks.containsValue(LockState.W)) {
					for(Map.Entry<JvnRemoteServer, LockState> server : currentLocks.entrySet()) {
						if(server.getValue() == LockState.W) {
							JvnObject newObject = (JvnObject) server.getKey().jvnInvalidateWriterForReader(joi);
							objectsIdMap.put(joi, newObject);
							currentLocks.put(server.getKey(), LockState.R);
							currentLocks.put(js, LockState.R);
							objectsLockMap.put(joi,currentLocks);
							return newObject;
						}
					}
				}
				else {
					currentLocks.put(js, LockState.R);
					objectsLockMap.put(joi,currentLocks);
					return objFound;
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
			throws java.rmi.RemoteException, JvnException, JvnLockException{
		JvnObject objFound = JvnCoordImpl.objectsIdMap.get(joi); 
		if(objFound != null)
		{
			try {
				writeAskServers.add(js);
				while(writeAskServers.size() > 1) {
					js.wait();
				}
				writeAskServers.remove(js);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			HashMap<JvnRemoteServer, LockState> currentLocks = objectsLockMap.get(joi);
			if(currentLocks.get(js) == LockState.NL || currentLocks.get(js) == null || currentLocks.get(js) == LockState.R) {
				if(currentLocks.containsValue(LockState.W)) {
					for(Map.Entry<JvnRemoteServer, LockState> server : currentLocks.entrySet()) {
						if(server.getValue() == LockState.W) {
							JvnObject newObject = (JvnObject) server.getKey().jvnInvalidateWriter(joi);
							objectsIdMap.put(joi, newObject);
							currentLocks.put(server.getKey(), LockState.NL);
							currentLocks.put(js, LockState.W);
							objectsLockMap.put(joi,currentLocks);
							if(writeAskServers.size() > 0) {
								writeAskServers.getFirst().notify();
							}
							return newObject;
						}
					}
				}
				else if(currentLocks.containsValue(LockState.R)) {
					List<JvnRemoteServer> serversTab = new ArrayList<JvnRemoteServer>();
					for(Map.Entry<JvnRemoteServer, LockState> server : currentLocks.entrySet()) {
						if(server.getValue() == LockState.R && server.getKey().hashCode() != js.hashCode()) {
							try {
								server.getKey().jvnInvalidateReader(joi);
							} catch(JvnLockException le) {
								currentLocks.put(js, LockState.NL);
								objectsLockMap.put(joi,currentLocks);
								if(writeAskServers.size() > 0) {
									writeAskServers.getFirst().notify();
								}
								throw le;
							}
							server.getKey().jvnInvalidateReader(joi);
							serversTab.add(server.getKey());
						}
					}

					for(JvnRemoteServer serv : serversTab) {
						currentLocks.put(serv, LockState.NL);
					}
					currentLocks.put(js, LockState.W);
					objectsLockMap.put(joi,currentLocks);

					if(writeAskServers.size() > 0) {
						writeAskServers.getFirst().notify();
					}
					return objFound;
				}
				else {
					currentLocks.put(js, LockState.W);
					objectsLockMap.put(joi,currentLocks);
					if(writeAskServers.size() > 0) {
						writeAskServers.getFirst().notify();
					}
					return objFound;
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
			throws java.rmi.RemoteException, JvnException, JvnLockException {
		if(objectsLockMap.isEmpty()) {
			throw new JvnException("Le serveur JVN n'est pas connu par coordinateur");
		}
		else {
			for(Map.Entry<Integer, HashMap<JvnRemoteServer, LockState>> objectsMap : objectsLockMap.entrySet()) {
				for(Map.Entry<JvnRemoteServer, LockState> server : objectsMap.getValue().entrySet()) {
					if(server.getKey().hashCode() == js.hashCode()) {
						if(server.getValue() == LockState.R) {
							server.getKey().jvnInvalidateReader(objectsMap.getKey());
						}
						else if(server.getValue() == LockState.W) {
							server.getKey().jvnInvalidateWriter(objectsMap.getKey());
						}
						objectsMap.getValue().remove(server.getKey());
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			JvnCoordImpl coordinator = new JvnCoordImpl();

			final Registry[] registryHolder = new Registry[1];

			try {
				registryHolder[0] = LocateRegistry.getRegistry();
				registryHolder[0].list();
			} catch (RemoteException e) {
				registryHolder[0] = LocateRegistry.createRegistry(1099);
			}
			try {
				registryHolder[0].lookup("Coordinator");
				System.out.println("Le coordinateur est déja lié...");
				registryHolder[0].unbind("Coordinator");
			} catch (NotBoundException e) {}
			registryHolder[0].bind("Coordinator", coordinator);
			System.out.println("Coordinateur ready");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


