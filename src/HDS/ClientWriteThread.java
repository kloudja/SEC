package HDS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import Message.FileCorruptMessage;
import Message.Message;

public class ClientWriteThread extends Thread {

	private Message message;
	private Socket_OOS_OIS soo;
	private boolean check;
	private Library lib;

	public ClientWriteThread(Message message, Socket_OOS_OIS soo, Library lib) {
		this.message = message;
		this.soo = soo;
		this.lib = lib;
	}

	@Override
	public synchronized void run() {
		super.run();
		boolean check;
		ObjectOutputStream oos = soo.getOos();
		ObjectInputStream ois = soo.getOis();
		try {
			oos.writeObject(message);
			System.out.println("mandeu uma puta duma msg!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileCorruptMessage m = null;
		try {
			m = (FileCorruptMessage) ois.readObject();
			System.out.println("recebi input");
		} catch (ClassNotFoundException | IOException e1) {
			System.out.println("n recebi input");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Verifica se a 1� mensagem que virificou os id's dos Hasblocks est�
		// corrupta
		if (m.isCorrupted()) {
			check = false;
		} else {
			// Verifica se a 2� mensagem que verifica mesmo os HashBlocks est�
			// corrupta
			FileCorruptMessage hashBlockCorruptMessage = null;
			try {
				hashBlockCorruptMessage = (FileCorruptMessage) ois.readObject();
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!hashBlockCorruptMessage.isCorrupted())
				check = true;
			else {
				check = false;
			}

			lib.setCheckCorrupt(check);
			notifyAll();
		}

	}

}
