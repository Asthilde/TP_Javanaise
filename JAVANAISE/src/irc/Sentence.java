/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;
import jvn.Annotation;


public class Sentence implements SentenceInterface {
	private static final long serialVersionUID = 1L;
	String 	data;

	public Sentence() {
		data = new String("");
	}

	@Annotation(methodName="write")
	public boolean write(String text) {
		data = text;
		return data == text;
	}

	@Annotation(methodName="read")
	public String read() {
		return data;	
	}

}