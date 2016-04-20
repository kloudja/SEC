package HDS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import Message.FileCorruptMessage;
import Message.Message;
import Message.WriteMessage;

public class ClientWriteThread extends Thread {

	private Message message;
	private Socket_OOS_OIS soo;
	private boolean check;
	private Library lib;
	private Dados dados;

	public ClientWriteThread(Message message, Socket_OOS_OIS soo, Library lib) {
		this.message = message;
		this.soo = soo;
		this.lib = lib;
	}

	public ClientWriteThread(WriteMessage message2, Socket_OOS_OIS socket_OOS_OIS, Library library, Dados dados) {
		this.message = message2;
		this.soo = socket_OOS_OIS;
		this.lib = library;
		this.dados = dados;
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
		// Verifica se a 1ª mensagem que virificou os id's dos Hasblocks está
		// corrupta
		if (m.isCorrupted()) {
			check = false;
		} else {
			// Verifica se a 2ª mensagem que verifica mesmo os HashBlocks está
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

		}
		dados.setCheckCorrupt(check);
	}

	public void ola() {
		System.out.println("olá");
	}

}
