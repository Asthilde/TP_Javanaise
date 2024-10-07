package Compteur;

import jvn.JvnException;
import jvn.JvnObject;
import jvn.JvnServerImpl;

public class Client {
	boolean createObject;
	public JvnObject obj;
	public JvnServerImpl js;
	
	public Client() {
		createObject = false;
		js = JvnServerImpl.jvnGetServer();
	}
	
	public Client(boolean createPower) {
		this();
		createObject = createPower;
	}
	
}
