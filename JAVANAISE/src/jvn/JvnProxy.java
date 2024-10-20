package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import irc.Sentence;

public class JvnProxy implements InvocationHandler {
	private static JvnObject obj;

	private JvnProxy() { 
	}

	public static Serializable newInstance(JvnLocalServer js, Serializable object) throws JvnException {
		obj = js.jvnLookupObject("IRC");

		if (obj == null) {
			System.out.println("Objet non trouvé");
			obj = js.jvnCreateObject(object);
			obj.jvnUnLock();
			js.jvnRegisterObject("IRC", obj);
		}
		return (Serializable) java.lang.reflect.Proxy.newProxyInstance(object.getClass().getClassLoader(), object.getClass().getInterfaces(), new JvnProxy());
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable, Exception {
		Object result = null;
		if(method.isAnnotationPresent(Annotation.class)) {
			Annotation annotation = method.getAnnotation(Annotation.class);

			if(annotation.methodName().equals("write")) {
				try {
					obj.jvnLockWrite();
				} catch(jvn.JvnLockException e) {
					System.out.println("Le verrou n'est pas relaché par ceux qui le détiennent j'abandonne !");	
					if(method.getReturnType() == Integer.class || method.getReturnType() == int.class || method.getReturnType() == long.class || method.getReturnType() == float.class || method.getReturnType() == long.class) {
						return 0;
					} else if(method.getReturnType() == boolean.class) {
						return false;
					}
					else if(method.getReturnType() == char.class) {
						return '\0' ;
					}
					return null;
				}
				System.out.println("Appel à la méthode write");		
				result = method.invoke(obj.jvnGetSharedObject(), args);	
				System.out.println("Fin d'appel à la méthode write");
				obj.jvnUnLock();
			}
			else if(annotation.methodName().equals("read")) {
				try {
					obj.jvnLockRead();
				} catch(jvn.JvnLockException e) {
					System.out.println("Le verrou n'est pas relaché par ceux qui le détiennent j'abandonne !");	
					if(method.getReturnType() == Integer.class || method.getReturnType() == int.class || method.getReturnType() == long.class || method.getReturnType() == float.class || method.getReturnType() == long.class) {
						return 0;
					} else if(method.getReturnType() == boolean.class) {
						return false;
					}
					else if(method.getReturnType() == char.class) {
						return '\0' ;
					}
					return null;
				}
				System.out.println("Appel à la méthode read");
				result = method.invoke(obj.jvnGetSharedObject(), args);
				System.out.println("Fin d'appel à la méthode read");
				obj.jvnUnLock();
			}
		}
		return result;
	}
}
