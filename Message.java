import java.io.*;

public class Message implements Serializable{
	String message;
	String user;
	int type;
	String[] names;
	
	public Message(String message, String user, int type){
		this.message=message;
		this.user=user;
		this.type=type;
	}
	public Message(String[] clients, int type){
		names=clients;
		this.type=type;
	}
	public Message(int type){
		this.type=type;
	}
	public Message(String user, int type){
		this.user=user;
		this.type=type;
	}
}
