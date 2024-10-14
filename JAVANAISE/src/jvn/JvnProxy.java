package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import irc.Sentence;

public class JvnProxy implements InvocationHandler {
	private Serializable myObject;
	private static JvnObject obj;
	
	private JvnProxy(Serializable obj) { 
		this.myObject = obj; 
	}

	public static Serializable newInstance(JvnLocalServer js, Serializable object) throws JvnException {
		obj = js.jvnLookupObject("IRC");

		if (obj == null) {
			System.out.println("Objet non trouvé");
			obj = js.jvnCreateObject(object);
			obj.jvnUnLock();
			js.jvnRegisterObject("IRC", obj);
		}
		return (Serializable) java.lang.reflect.Proxy.newProxyInstance(object.getClass().getClassLoader(), object.getClass().getInterfaces(), new JvnProxy(object));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object result = null;
		if(method.isAnnotationPresent(Annotation.class)) {
			Annotation annotation = method.getAnnotation(Annotation.class);

			if(annotation.methodName().equals("write")) {
				obj.jvnLockWrite();
				System.out.println("Appel à la méthode write pour écrire : " + ((String) args[0]));		
				result = method.invoke((Sentence) obj.jvnGetSharedObject(), args);	
				obj.jvnUnLock();
			}
			else if(annotation.methodName().equals("read")) {
				obj.jvnLockRead();
				System.out.println("Appel à la méthode read pour lire : " + ((Sentence) obj.jvnGetSharedObject()).read());
				result = method.invoke((Sentence) obj.jvnGetSharedObject(), args);
				obj.jvnUnLock();
			}
		}
		return result;
	}
}
