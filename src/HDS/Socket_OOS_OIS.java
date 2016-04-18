package HDS;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Socket_OOS_OIS {

	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	

	public Socket_OOS_OIS(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
		super();
		this.socket = socket;
		this.oos = oos;
		this.ois = ois;
	}
	

	public ObjectOutputStream getOos() {
		return oos;
	}


	public ObjectInputStream getOis() {
		return ois;
	}
	
	public Socket getSocket() {
		return socket;
	}
}
