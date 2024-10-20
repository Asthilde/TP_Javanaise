package jvn;

public class JvnLockException extends Exception {
	private static final long serialVersionUID = 5L;
	String message;
  
	public JvnLockException() {
	}
	
	public JvnLockException(String message) {
		this.message = message;
	}	
  
	public String getMessage(){
		return message;
	}

}
