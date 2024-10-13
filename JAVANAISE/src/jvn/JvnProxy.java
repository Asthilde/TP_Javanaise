package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import irc.Annotation;
import irc.Sentence;
import irc.SentenceInterface;

public class JvnProxy implements InvocationHandler {
	private JvnObject obj;
	private JvnProxy(JvnObject obj) { this.obj = obj; }
	
	public static SentenceInterface  newInstance(JvnObject object) throws JvnException {
		return (SentenceInterface) java.lang.reflect.Proxy.newProxyInstance(((SentenceInterface) object.jvnGetSharedObject()).getClass().getClassLoader(), ((SentenceInterface) object.jvnGetSharedObject()).getClass().getInterfaces(), new JvnProxy(object));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// TODO Auto-generated method stub
		Object result = null;
		if(method.isAnnotationPresent(Annotation.class)) {
			System.out.println("Je passe ici !");
			Annotation annotation = method.getAnnotation(Annotation.class);
			
			if(annotation.methodName().equals("write")) {
				obj.jvnLockWrite();
				System.out.println("Appel à la méthode write pour écrire : " + ((String) args[0]));
				result = ((Sentence)(obj.jvnGetSharedObject())).write((String) args[0]);
				obj.jvnUnLock();
//				try {
//					return ((Sentence)(obj.jvnGetSharedObject())).write((String) args[0]);
//				} finally {
//					obj.jvnUnLock();
//				}
			}
			else if(annotation.methodName().equals("read")) {
				obj.jvnLockRead();
				System.out.println("Appel à la méthode read pour lire : " + ((Sentence) obj.jvnGetSharedObject()).read());
				result = ((Sentence) obj.jvnGetSharedObject()).read();
				obj.jvnUnLock();
//				try {
//					return ((Sentence) obj.jvnGetSharedObject()).read();	
//				} finally {
//					obj.jvnUnLock();
//				}
			}
		}
		return result;
	}

}
