package Compteur;

import jvn.*;

public class Main {
	public static void main(String argv[]) {
		try {
			// initialize JVN
			JvnServerImpl js = JvnServerImpl.jvnGetServer();
			CompteurInterface compteur = (CompteurInterface) JvnProxy.newInstance(js, new Compteur());

			int i = 0;
			while(true) {
				System.out.println("Client " + js.hashCode() + " incrémente le compteur");
				compteur.incrementCompteur();
				Thread.sleep((long) Math.random() * 2000);
				System.out.println("Client " + js.hashCode() + " lit le compteur. Compteur : " + compteur.getCompteur());

				if(i % 10 == 0) {
					System.out.println("Client " + js.hashCode() + " décrémente le compteur");
					compteur.decrementCompteur();
				}
				i++;
			}

		} catch (Exception e) {
			System.out.println("Problème avec le compteur : " + e.getMessage());
		}
	}
}
