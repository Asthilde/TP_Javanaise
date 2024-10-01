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
import java.io.*;



public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{
	
	private JvnRemoteCoord coordinator; // Ref coord
	private HashMap<Integer, JvnObject> objectStore; // Pour stocker les objets JVN
    private HashMap<String, Integer> nameRegistry; // Pour stocker les noms
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;

  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();
		// to be completed
	    try {
	        Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Port a corriger si besoin
	        coordinator = (JvnRemoteCoord) registry.lookup("Coord");
	        objectStore = new HashMap<>();
            nameRegistry = new HashMap<>();
	    } catch (Exception e) {
	        throw new JvnException("Erreur lors de la connexion au coordinateur : " + e.getMessage());
	    }
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.JvnException {
    // to be completed 
		// Libérer toutes les ressources du serveur JVN
	    // Notifier le coordinateur de la terminaison
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		// to be completed 
//		// Appel du coordinateur pour obtenir un ID
//	    int id = coordinator.jvnGetObjectId();
//	    JvnObject newObject = new // ne sais pas quelle methode appelee
////        objectStore.put(id, newObject);
//        return newObject;
//	    
//	    
		JvnObject jvnObj = null;
		try {
			jvnObj = (JvnObject) o;
			objectStore.put(coordinator.jvnGetObjectId(), jvnObj);
			jvnObj.jvnLockRead();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
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
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException {
		// to be completed 
		int id = jo.jvnGetObjectId();
        if (nameRegistry.containsKey(jon)) {
            throw new JvnException("Nom enregistré");
        }
        nameRegistry.put(jon, id);
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
    // to be completed 
		Integer id = nameRegistry.get(jon);
        if (id == null) {
            throw new JvnException("Objet non trouvé pour le nom : " + jon);
        }
        return objectStore.get(id);
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
		// to be completed 
	   JvnObject obj = objectStore.get(joi);
       if (obj == null) {
           throw new JvnException("Objet non trouvé");
       }
       if(obj.jvnGetState()== LockState.NL) {
    	   try {
               Serializable newState = coordinator.jvnLockRead(joi, js);
               String objectName=null;
               for (Map.Entry<String, Integer> entry : nameRegistry.entrySet()) {
                   if (entry.getValue().equals(joi)) {
                       objectName = entry.getKey();
                       break;
                   }
               }
               obj= coordinator.jvnLookupObject(objectName, js);
               obj.jvnChangeState(newState);
           } catch (Exception e) {
               throw new JvnException("Erreur lors de la demande de verrou de lecture au coordinateur : " + e.getMessage());
           }
       }
       return obj.jvnGetState();

	}	
	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
		// to be completed 
	   JvnObject obj = objectStore.get(joi);
       if (obj == null) {
           throw new JvnException("Objet non trouvé");
       }
       
       if(obj.jvnGetState() == LockState.R || obj.jvnGetState() == LockState.RC || obj.jvnGetState()== LockState.NL) {
    	   try {
               Serializable newState = coordinator.jvnLockWrite(joi, js);
               String objectName = null;
               for (Map.Entry<String, Integer> entry : nameRegistry.entrySet()) {
                   if (entry.getValue().equals(joi)) {
                       objectName = entry.getKey();
                       break;
                   }
               }
               obj= coordinator.jvnLookupObject(objectName, js);
               obj.jvnChangeState(newState);
           } catch (Exception e) {
               throw new JvnException("Erreur lors de la demande de verrou d'écriture au coordinateur : " + e.getMessage());
           }
       }
       return obj.jvnGetState();
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
		// to be completed 
	  JvnObject obj = objectStore.get(joi);
	    if (obj == null) {
	        throw new JvnException("Objet non trouvé pour ID : " + joi);
	    }

	    synchronized (obj) {
	        while (obj.jvnGetState() == LockState.R || obj.jvnGetState() == LockState.RC) {
	            try {
	                obj.wait(); // Wait the unlockReader
	            } catch (InterruptedException e) {
	                throw new JvnException("Interruption pendant l'attente de la libération des verrous de lecture");
	            }
	        }

	        obj.jvnChangeState(LockState.NL);
	    }
	    obj.notifyAll();

	};
	
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException { 
		// to be completed 
	  JvnObject obj = objectStore.get(joi);
	    if (obj == null) {
	        throw new JvnException("Objet non trouvé pour ID : " + joi);
	    }

	    synchronized (obj) {
	        while (obj.jvnGetState() == LockState.W || obj.jvnGetState() == LockState.WC || obj.jvnGetState() == LockState.RWC) {
	            try {
	                obj.wait(); // Wait the unlockWriter
	            } catch (InterruptedException e) {
	                throw new JvnException("Interruption pendant l'attente de la libération des verrous de lecture");
	            }
	        }
	        if(obj.jvnGetState() == LockState.W || obj.jvnGetState() == LockState.WC) {
	        obj.jvnChangeState(LockState.NL);
	        } else {
		        obj.jvnChangeState(LockState.R);
	        }
	    }
	    return obj.jvnGetState();
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException { 
		// to be completed 
	   JvnObject obj = objectStore.get(joi);
	    if (obj == null) {
	        throw new JvnException("Objet non trouvé pour ID : " + joi);
	    }

	    synchronized (obj) {
	        while (obj.jvnGetState() == LockState.W || obj.jvnGetState() == LockState.WC || obj.jvnGetState() == LockState.RWC) {
	            try {
	                obj.wait(); // Wait the unlockReader
	            } catch (InterruptedException e) {
	                throw new JvnException("Interruption pendant l'attente de la libération des verrous de lecture");
	            }
	        }
		        obj.jvnChangeState(LockState.R);
	        }
	    return obj.jvnGetState();
	 };

}

 
