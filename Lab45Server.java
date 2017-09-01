 	  /*
	  *   Program Name:     Lab 4-5                                
	  *                                                             
	  *   Student Name:     Huy Bui, Anh Vu	                    
	  *   Semester:         Spring, 2016			               
	  *   Class-Section:    CoSc20203						        
	  *   Instructor:       Dr. Dick Rinewalt  	                
	  *                                                            
	  *   Program Overview:                                        
	  *     This program creates a chat program. Many clients 	     
	  *     can connect to the server to chat 						 
	  *                                                             
	  *   Features:                                                
	  *     1. Clients can choose an user name which is displayed in
	  * 		the conversation	   								
	  *	 2. There is a list of online clients available for both 
	  *	 	the server and clients								 
	  *	 3. The server can choose to kick a client out of server 
	  *	 4. Quitting the GUI would also close the streams and/or 
	  *	    log out												 
	  *	 5. The server(admin) can also send messages to clients	 
	  * 	 6. There are notifications whenever a client logs 		 
	  * 		in/out or is kicked									
	  *															 
	  *   Note: We use a Message object instead of sending Strings 
      *															
	  */
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;

public class Lab45Server extends JFrame implements ActionListener, ListSelectionListener{
	final static int PORT = 5259;
	DefaultListModel<String> model = new DefaultListModel<String>();
	JList<String> clients=new JList<String>(model);
	JTextArea texts= new JTextArea(15,10);
	JTextField typing = new JTextField(20);;
	JButton sendB= new JButton("Send");
	JButton kickB = new JButton("Kick");
	JScrollPane scrollText= new JScrollPane();
	JScrollPane scrollList = new JScrollPane();
	JLabel messageL = new JLabel("Message");
	
	ObjectInputStream inMes;
	ObjectOutputStream writer;
	
//collection of online connections
	ArrayList<connection> users = new ArrayList<connection>();
	
	public Lab45Server() {
//set up the GUI
		setLayout(new BorderLayout());
		clients.setFixedCellWidth(100);
		clients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(clients,BorderLayout.EAST);
		JPanel westPanel = new JPanel(new BorderLayout());
		texts.setEditable(false);
		texts.setLineWrap(true);
		texts.setWrapStyleWord(true);
		scrollText.getViewport().add(texts);	
		westPanel.add(scrollText,BorderLayout.CENTER);
		kickB.setEnabled(false);
		JPanel interactP = new JPanel();		
		interactP.add(messageL); interactP.add(typing); interactP.add(sendB); interactP.add(kickB);
		westPanel.add(interactP, BorderLayout.SOUTH);
		add(westPanel,BorderLayout.CENTER);
		setSize(550,350);
		setTitle("Server");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
//set up the listeners
		sendB.addActionListener(this);
		clients.addListSelectionListener(this);
		kickB.addActionListener(this);
	}
	public static void main(String[] args){
		Lab45Server display = new Lab45Server();
		display.setVisible(true);
		display.run();			
	}
	public void run() {
		System.out.println("Create Server");
		ServerSocket serverSocket=null;
		Socket clientSocket=null;
		try {
			serverSocket = new ServerSocket(PORT);
			while (true){
					clientSocket = serverSocket.accept(); //accept clients
					users.add(new connection(clientSocket)); //each connection object will handle a client
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			}			
	}
	public class connection{
		Socket clientSock;
		ObjectOutputStream writer;
		ObjectInputStream inMes;
		String name;
		
		public connection(Socket socket) throws IOException{
			//wrap streams with object reader
			clientSock=socket;
			writer = new ObjectOutputStream(clientSock.getOutputStream());
			writer.flush();
			inMes = new ObjectInputStream(clientSock.getInputStream());
			
//create a thread to handler client connection
			Thread handler = new Thread(){
				public void run(){
					try{
						Message in;
						
//each type of the message would do something different
						while ((in=(Message) inMes.readObject())!=null){
							
//type 3 = client log out
							if (in.type==3) {
								System.out.println(in.user+" logged out");
								texts.append(name+" JUST LOGGED OUT \n");
								sendMessage(in);
								users.remove(connection.this);
								sendList();
								sendAll(new Message(in.user,6));
								break;  
							}
							
//type 1 = client send regular message
							if (in.type==1) {
								sendAll(in);
								texts.append(in.user + ": "+in.message+"\n");
							}
							
//type 2 = client log in
							if (in.type==2){
								System.out.println(in.user+" logged in");
								texts.append(in.user+" JUST LOGGED IN \n");
								name=in.user;
								sendList();
								sendAll(in);
							}
						}
//close all streams and sockets
						clientSock.close();
						writer.close();
						inMes.close();
						clientSock=null;
						writer=null;
						inMes=null;
					}
					catch(IOException ex) {
						System.out.println("Stream is closed");
					}
					catch(ClassNotFoundException ex){ex.printStackTrace();}
				}
			};
			handler.start(); //start thread
		}
		
//send message to one client (the current connection object)
		public void sendMessage(Message mess) throws IOException{
			writer.writeObject(mess);
			writer.flush();
		}
	}
	
//send message to all clients in the collection
	public void sendAll(Message mess) throws IOException{
		for (connection user: users){
			user.writer.writeObject(mess);
			user.writer.flush();
		}
	}
	public void actionPerformed(ActionEvent ae){
		try {
//server send message to all clients			
			if (ae.getSource()==sendB) {
				sendAll(new Message(typing.getText(),"SERVER",1));			
				texts.append("SERVER: "+typing.getText()+"\n");
				typing.setText("");
			}
//kick the selected client. message type 5
			if (ae.getSource()==kickB) {
//notify the selected client
				connection connect = users.get(clients.getSelectedIndex());
				connect.sendMessage(new Message(5));
				System.out.println(connect.name+" has been kicked");
				texts.append(connect.name+" HAS BEEN KICKED \n");
				users.remove(connect);
//notify others				
				sendAll(new Message(connect.name, 7));
				
//close all streams and socket
				connect.clientSock.close();
				connect.writer.close();
				connect.inMes.close();
				connect.clientSock=null;
				connect.writer=null;
				connect.inMes=null;
				sendList();
			}
		} catch(IOException ex){ System.out.println("Stream is closed");}
	}
//send the current clients list to all clients. invoked every time the list is changed. message type 4 
	public void sendList() throws IOException {
		String[] names = new String[users.size()];
		model.removeAllElements();
		for (int i=0; i <names.length;i++) {
			names[i]=(users.get(i).name);
			model.addElement(names[i]);
		}	
		sendAll(new Message(names,4));
	}
	public void valueChanged(ListSelectionEvent se) {
		if (clients.isSelectionEmpty()) kickB.setEnabled(false);
		else kickB.setEnabled(true);
	}

}
