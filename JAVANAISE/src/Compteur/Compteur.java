package Compteur;

import jvn.*;

public class Compteur implements CompteurInterface {
	int comp;
	
	public Compteur () {
		comp = 0;
	}
	
	@Annotation(methodName="write")
	public int incrementCompteur() {
		comp += 1;
		return comp;
	}
	
	@Annotation(methodName="write")
	public int decrementCompteur() {
		comp -= 1;
		return comp;
	}
	
	@Annotation(methodName="read")
	public int getCompteur() {
		return comp;
	}

}
