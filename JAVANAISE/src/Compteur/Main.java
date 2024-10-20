package Compteur;


public class Main {
	public static void main(String argv[]) {
		try {
			Client c1 = new Client();
			Client c2 = new Client(true);
			
			//Création de l'objet compteur
			c2.obj = c2.js.jvnCreateObject(new Compteur());
			c2.obj.jvnUnLock();
			c2.js.jvnRegisterObject("Compteur", c2.obj);
			
			//Ecriture de c2 sur Compteur
			System.out.println("Ecriture sur le compteur depuis c2");
			c2.obj.jvnLockWrite();
			((Compteur) (c2.obj.jvnGetSharedObject())).incrementCompteur();
			c2.obj.jvnUnLock();
			System.out.println("Etat du compteur : " + ((Compteur) (c2.obj.jvnGetSharedObject())).getCompteur());
			
			//Récupération du Compteur sur c1
			System.out.println("Récupération du compteur depuis c1");
			c1.obj = c1.js.jvnLookupObject("Compteur");
			c1.obj.jvnLockRead();
			System.out.println("Etat du compteur : " + ((Compteur) (c1.obj.jvnGetSharedObject())).getCompteur());
			
			//Tentative de lecture du Compteur sur c2
			System.out.println("Lecture du compteur depuis c2");
			c2.obj.jvnLockRead();
			System.out.println("Etat du compteur : " + ((Compteur) (c2.obj.jvnGetSharedObject())).getCompteur());
			
			//Tentative d'écriture du Compteur sur c2
			System.out.println("Ecriture du compteur depuis c2");
			c2.obj.jvnLockWrite();
			((Compteur) (c2.obj.jvnGetSharedObject())).incrementCompteur();
			System.out.println("Etat du compteur : " + ((Compteur) (c2.obj.jvnGetSharedObject())).getCompteur());
			
			//Tentative d'écriture du Compteur sur c2
			System.out.println("Ecriture du compteur depuis c1");
			c1.obj.jvnLockWrite();
			System.out.println("Demande du verrou en écriture depuis c1");
			Thread.sleep(3000);
			System.out.println("Relachement du verrou en écriture depuis c2");
			c2.obj.jvnUnLock();
			((Compteur) (c1.obj.jvnGetSharedObject())).incrementCompteur();
			System.out.println("Etat du compteur : " + ((Compteur) (c1.obj.jvnGetSharedObject())).getCompteur());
			
		} catch (Exception e) {
			System.out.println("Compteur problem : " + e.getMessage());
		}
	}
}
