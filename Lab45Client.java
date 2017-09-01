import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.awt.event.*;

public class Lab45Client extends JFrame implements ActionListener{
	final static int PORT = 5259;
	final static String SERVER = "127.0.0.1";
	DefaultListModel<String> model = new DefaultListModel<String>();
	JList<String> clients=new JList<String>(model);;
	JTextArea texts= new JTextArea(15,10);
	JTextField typing = new JTextField(20);;
	JButton sendB= new JButton("Send");
	JButton connectB= new JButton("Connect");
	JTextField userName= new JTextField(20);
	JScrollPane scrollText= new JScrollPane();
	JLabel messageL = new JLabel("Message");
	JLabel userL = new JLabel("User Name");
	String name;
	
	Socket socket;
	ObjectOutputStream writer;
	ObjectInputStream inMes;
	
	public Lab45Client() {
//set up GUI
		setLayout(new BorderLayout());
		clients.setFixedCellWidth(100);
		add(clients,BorderLayout.EAST);
		JPanel westPanel = new JPanel(new BorderLayout());
		texts.setEditable(false);
		texts.setLineWrap(true);
		texts.setWrapStyleWord(true);
		scrollText.getViewport().add(texts);	
		westPanel.add(scrollText,BorderLayout.CENTER);
		JPanel interactP = new JPanel(new BorderLayout());
		JPanel interNorth = new JPanel();
		typing.setEnabled(false);
		sendB.setEnabled(false);
		interNorth.add(messageL); interNorth.add(typing); interNorth.add(sendB);
		JPanel interSouth = new JPanel();
		interSouth.add(userL); interSouth.add(userName); interSouth.add(connectB);
		interactP.add(interNorth,BorderLayout.NORTH);
		interactP.add(interSouth,BorderLayout.SOUTH);
		westPanel.add(interactP, BorderLayout.SOUTH);
		add(westPanel,BorderLayout.CENTER);
		setSize(550,350);
		setTitle("Client");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//send the "close streams" message to server when the GUI is closed. message type 3
		addWindowListener(new WindowAdapter(){
			 public void windowClosing(WindowEvent windowEvent){
				 try {
					 if (writer!=null){
						writer.writeObject(new Message(typing.getText(),name,3));
						writer.flush();
					 }
				 } catch(IOException ex) {System.out.println("Stream is closed");}
			 }
		});	
//set up listeners
		connectB.addActionListener(this);
		sendB.addActionListener(this);
	}
	public static void main(String[] args){
		Lab45Client display = new Lab45Client();
		display.setVisible(true);
	}
//set up socket and streams. take in user name
	public int setUpNetwork(){
		try{
			if (userName.getText().equals("")) {
				userName.setText("Choose an user name please");
				return -1;
			}
			name = userName.getText();			
			socket = new Socket(SERVER, PORT);
			texts.append("CONNECTED TO SERVER AS " + name +"\n");
//wrap streams with object readers
			writer = new ObjectOutputStream(socket.getOutputStream());
			writer.flush();
			inMes = new ObjectInputStream(socket.getInputStream());
//notify the server about logging in. message type 2
			writer.writeObject(new Message("",name,2));
			writer.flush();
			return 0;
		} catch (IOException ex) {ex.printStackTrace();}
		return 0;
	}
	public void actionPerformed (ActionEvent ae){
		try{
//connect button
			if (ae.getSource()==connectB&&connectB.getText().equals("Connect")) {
				if (setUpNetwork()==-1) return;
				System.out.println("Connection Established");
				connectB.setText("Disconnect");
				userName.setEditable(false);
				sendB.setEnabled(true);
				typing.setEnabled(true);
//create a thread to receive messages from server				
				Thread handler = new Thread(){
					
					public void run(){
						try {							
							Message in;
							while ((in=(Message) inMes.readObject() )!=null){
//type 1 = regular messages
								if (in.type==1) {
									texts.append(in.user + ": "+in.message+"\n");
								}
//type 3 = disconnect
								if (in.type==3) {
									break;
								}
//type 2 = log in
								if (in.type==2) {
									if (!in.user.equals(name)){
										texts.append(in.user+" JUST LOGGED IN \n");
									}
								}
//type 4 = receive user list and update list
								if (in.type==4){
									model.removeAllElements();
									for (int i =0; i<in.names.length; i++) model.addElement(in.names[i]);
								}
//type 5 = kicked
								if (in.type==5){
									texts.append("YOU HAVE BEEN KICKED \n");
									model.removeAllElements();
									connectB.setText("Connect");
									userName.setEditable(true);
									sendB.setEnabled(false);
									typing.setEnabled(false);
									break;
								}
//type 6 = some one else log out
								if (in.type==6) {
									texts.append(in.user+" HAS LOGGED OUT\n");
								}
								if (in.type==7) {
									texts.append(in.user+" HAS BEEN KICKED\n");
								}
							}
//close all streams and sockets
							socket.close();
							writer.close();
							inMes.close();
							socket=null;
							writer=null;
							inMes=null;
						}
						catch (IOException ex) {System.out.println("Stream is closed");}
						catch (ClassNotFoundException ex) {ex.printStackTrace();}
					}
				};
				handler.start();
				return;
			}
//disconnect button. message type 3
			if (ae.getSource()==connectB&&connectB.getText().equals("Disconnect")) {
				writer.writeObject(new Message(typing.getText(),name,3));
				writer.flush();
				model.removeAllElements();
				connectB.setText("Connect");
				userName.setEditable(true);
				sendB.setEnabled(false);
				typing.setEnabled(false);
				texts.append("LOGGED OUT \n");				
			}
//send button. message type 1
			if (ae.getSource()==sendB&&!(typing.getText().equals(""))) {
				writer.writeObject(new Message(typing.getText(),name,1));
				writer.flush();
				typing.setText("");
				return;
			}
		} catch (IOException ex) {System.out.println("Stream is closed");}
	}

}
