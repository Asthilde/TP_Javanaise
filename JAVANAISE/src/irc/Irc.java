/***
 * Irc class : simple implementation of a chat using JAVANAISE 
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.awt.*;
import java.awt.event.*; 


import jvn.*;
import java.io.*;


public class Irc {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	JvnObject       sentence;


  /**
  * main method
  * create a JVN object nammed IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
	   try {
	        System.out.println("Tentative d'initialisation de JVN...");

		// initialize JVN
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
        System.out.println("JVN initialisé avec succès.");

		
//        // Tentative de création et d'enregistrement de l'objet IRC
//        System.out.println("Création de l'objet IRC...");
//        JvnObject jo = js.jvnCreateObject(new Sentence());
//        if (jo == null) {
//            throw new Exception("L'objet IRC n'a pas pu être créé.");
//        }
//
//        // Enregistrez l'objet
//        js.jvnRegisterObject("IRC", jo);
//        System.out.println("Objet IRC enregistré avec succès.");

        // Maintenant, vous pouvez le rechercher
        System.out.println("Tentative de recherche de l'objet IRC...");
        JvnObject jo = js.jvnLookupObject("IRC");
		   
		if (jo == null) {
		    System.out.println("Objet non trouvé, création de l'objet IRC...");
		    jo = js.jvnCreateObject(new Sentence());
		    if (jo == null) {
		        throw new Exception("L'objet IRC n'a pas pu être créé.");
		    }
		    // Déverrouillez l'objet
		    jo.jvnUnLock();
		    System.out.println("Test unlock");
		    // Enregistrez l'objet
		    js.jvnRegisterObject("IRC", jo);
		    System.out.println("Objet IRC enregistré avec succès.");
		}
		// create the graphical part of the Chat application
		 new Irc(jo);
	   
	   } catch (Exception e) {
		   System.out.println("IRC problem : " + e.getMessage());
	   }
	}

  /**
   * IRC Constructor
   @param jo the JVN object representing the Chat
   **/
	public Irc(JvnObject jo) {
		sentence = jo;
		frame=new Frame();
		frame.setLayout(new GridLayout(1,1));
		text=new TextArea(10,60);
		text.setEditable(false);
		text.setForeground(Color.red);
		frame.add(text);
		data=new TextField(40);
		frame.add(data);
		Button read_button = new Button("read");
		read_button.addActionListener(new readListener(this));
		frame.add(read_button);
		Button write_button = new Button("write");
		write_button.addActionListener(new writeListener(this));
		frame.add(write_button);
		frame.setSize(545,201);
		text.setBackground(Color.black); 
		frame.setVisible(true);
	}
}


 /**
  * Internal class to manage user events (read) on the CHAT application
  **/
 class readListener implements ActionListener {
	Irc irc;
  
	public readListener (Irc i) {
		irc = i;
	}
   
 /**
  * Management of user events
  **/
	public void actionPerformed (ActionEvent e) {
	 try {
		// lock the object in read mode
		irc.sentence.jvnLockRead();
		
		// invoke the method
		String s = ((Sentence)(irc.sentence.jvnGetSharedObject())).read();
		
		// unlock the object
		irc.sentence.jvnUnLock();
		
		// display the read value
		irc.data.setText(s);
		irc.text.append(s+"\n");
	   } catch (JvnException je) {
		   System.out.println("IRC problem : " + je.getMessage());
	   }
	}
}

 /**
  * Internal class to manage user events (write) on the CHAT application
  **/
 class writeListener implements ActionListener {
	Irc irc;
  
	public writeListener (Irc i) {
        	irc = i;
	}
  
  /**
    * Management of user events
   **/
	public void actionPerformed (ActionEvent e) {
	   try {	
		// get the value to be written from the buffer
    String s = irc.data.getText();
	   System.out.println("test lockWrite : " + s );
	   System.out.println("test lockWrite : " + irc.sentence.jvnGetObjectId() );

        	
    // lock the object in write mode
		irc.sentence.jvnLockWrite();
		
		// invoke the method
		((Sentence)(irc.sentence.jvnGetSharedObject())).write(s);
		
		// unlock the object
		irc.sentence.jvnUnLock();
	 } catch (JvnException je) {
		   System.out.println("IRC problem  : " + je.getMessage());
	 }
	}
}



