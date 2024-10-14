package irc;

import jvn.Annotation;

public interface SentenceInterface extends java.io.Serializable {
	
	@Annotation(methodName="write")
	public boolean write(String text);
	
	@Annotation(methodName="read")
	public String read();
	
}
