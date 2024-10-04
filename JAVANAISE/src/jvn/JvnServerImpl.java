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
		try {
	        // Libérer toutes les ressources détenues par ce serveur JVN
	        for (Map.Entry<Integer, JvnObject> entry : objectStore.entrySet()) {
	            JvnObject obj = entry.getValue();
	            synchronized (obj) {
	                if (obj.jvnGetState() == LockState.R || obj.jvnGetState() == LockState.W|| obj.jvnGetState() == LockState.RC|| obj.jvnGetState() == LockState.RWC|| obj.jvnGetState() == LockState.Wc) {
	                    obj.jvnChangeState(LockState.NL);
	                }
	            }
	        }

	        // Notifier le coordinateur que le serveur se termine
	        coordinator.jvnTerminate(js);

	    } catch (RemoteException e) {
	        throw new JvnException("Erreur lors de la terminaison : " + e.getMessage());
	    }
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		// to be completed     
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
	public JvnObject jvnLookupObject(String jon) throws JvnException {
	    Integer id = nameRegistry.get(jon);
	    if (id == null) {
	        throw new JvnException("Objet non trouvé pour le nom : " + jon);
	    }

	    JvnObject localObject = objectStore.get(id);

	    if (localObject == null) {
	        // Si l'objet n'est pas présent dans le magasin local
	        try {
	            localObject = coordinator.jvnLookupObject(jon, js);
	            if (localObject != null) {
	                // Ajouter l'objet mis à jour dans le magasin local
	                objectStore.put(id, localObject);
	            } else {
	                throw new JvnException("Le coordinateur n'a pas pu retourner l'objet pour le nom : " + jon);
	            }
	        } catch (Exception e) {
	            throw new JvnException("Erreur lors de la récupération de l'objet du coordinateur : " + e.getMessage());
	        }
	    } else {
	        // Vérifier si l'état nécessite une version plus récente
	        if (localObject.jvnGetState() == LockState.NL) {
	            try {
	                JvnObject updatedObject = coordinator.jvnLookupObject(jon, js);
	                if (updatedObject != null) {
	                    // Mise à jour de l'objet dans le magasin local
	                    objectStore.put(id, updatedObject);
	                    localObject = updatedObject;
	                }
	            } catch (Exception e) {
	                throw new JvnException("Erreur lors de la récupération de l'objet mis à jour du coordinateur : " + e.getMessage());
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
		// to be completed 
	   // Obtenir l'objet localement à partir du store
	    JvnObject obj = objectStore.get(joi);
	    if (obj == null) {
	        throw new JvnException("Objet non trouvé");
	    }

	    if (obj.jvnGetState() == LockState.NL) {
	        try {
	            Serializable newState = coordinator.jvnLockRead(joi, js);
	            String objectName = null;

	            // Récupérer le nom de l'objet à partir de nameRegistry
	            for (Map.Entry<String, Integer> entry : nameRegistry.entrySet()) {
	                if (entry.getValue().equals(joi)) {
	                    objectName = entry.getKey();
	                    break;
	                }
	            }

	            obj = coordinator.jvnLookupObject(objectName, js);
	            if (obj != null) {
	                objectStore.put(joi, obj);
	            }
	            obj.jvnChangeState(newState);
	        } catch (Exception e) {
	            throw new JvnException("Erreur lors de la demande de verrou de lecture au coordinateur : " + e.getMessage());
	        }
	    } else if (obj.jvnGetState() == LockState.RC || obj.jvnGetState() == LockState.R) {
	        obj.jvnChangeState(LockState.R);
	    } else if (obj.jvnGetState() == LockState.WC || obj.jvnGetState() == LockState.RWC) {
	        obj.jvnChangeState(LockState.RWC);
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

	    if (obj.jvnGetState() == LockState.NL) {
	        try {
	            Serializable newState = coordinator.jvnLockWrite(joi, js);
	            String objectName = null;

	            for (Map.Entry<String, Integer> entry : nameRegistry.entrySet()) {
	                if (entry.getValue().equals(joi)) {
	                    objectName = entry.getKey();
	                    break;
	                }
	            }

	            obj = coordinator.jvnLookupObject(objectName, js);
	            if (obj != null) {
	                objectStore.put(joi, obj);
	            }
	            obj.jvnChangeState(newState);
	        } catch (Exception e) {
	            throw new JvnException("Erreur lors de la demande de verrou d'écriture au coordinateur : " + e.getMessage());
	        }
	    }else if(obj.jvnGetState() == LockState.R || obj.jvnGetState() == LockState.RC) {
	    	try {
	            Serializable newState = coordinator.jvnLockWrite(joi, js);
	            obj.jvnChangeState(newState);
	        } catch (Exception e) {
	            throw new JvnException("Erreur lors de la demande de verrou d'écriture au coordinateur : " + e.getMessage());
	        }
	    }
	    else if (obj.jvnGetState() == LockState.RWC || obj.jvnGetState() == LockState.WC) {
	        obj.jvnChangeState(LockState.W);
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
	        while (obj.jvnGetState() == LockState.R || obj.jvnGetState() == LockState.RC || obj.jvnGetState() == LockState.RWC) {
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

 
