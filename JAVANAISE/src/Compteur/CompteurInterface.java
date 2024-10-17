package Compteur;

import jvn.Annotation;

public interface CompteurInterface extends java.io.Serializable {
	
	@Annotation(methodName="write")
	public int incrementCompteur();
	
	@Annotation(methodName="write")
	public int decrementCompteur();
	
	@Annotation(methodName="read")
	public int getCompteur();
}
