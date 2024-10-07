package Compteur;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class Compteur implements java.io.Serializable {
	int comp;
	
	public Compteur () {
		comp = 0;
	}
	
	public void incrementCompteur() {
		comp += 1;
	}
	
	public void decrementCompteur() {
		comp -= 1;
	}
	
	public int getCompteur() {
		return comp;
	}

}
