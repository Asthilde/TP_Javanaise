package Compteur;

import java.io.Serializable;
import jvn.Annotation;

public interface CompteurInterface extends Serializable {
	
	@Annotation(methodName="write")
	public int incrementCompteur();
	
	@Annotation(methodName="write")
	public int decrementCompteur();
	
	@Annotation(methodName="read")
	public int getCompteur();
}