package HDS;

import java.util.ArrayList;

import Message.FileCorruptMessage;
import Message.Message;

public class Dados {

	//Algoritmo
	private int wts = 0;
	private ArrayList<FileCorruptMessage> acklist = new ArrayList<>();
	private int rid = 0;
	private ArrayList<Message> readlist = new ArrayList<>();
	private ArrayList<Boolean> checkCorrupt = new ArrayList<>();

	//Algoritmo
	
	
	public int getWts() {
		return wts;
	}
	public void setWts(int wts) {
		this.wts = wts;
	}
	public ArrayList<FileCorruptMessage> getAcklist() {
		return acklist;
	}
	public void setAcklist(ArrayList<FileCorruptMessage> acklist) {
		this.acklist = acklist;
	}
	public int getRid() {
		return rid;
	}
	public void setRid(int rid) {
		this.rid = rid;
	}
	public ArrayList<Message> getReadlist() {
		return readlist;
	}
	public void setReadlist(ArrayList<Message> readlist) {
		this.readlist = readlist;
	}
	public ArrayList<Boolean> getCheckCorrupt() {
		return checkCorrupt;
	}

	public synchronized void setCheckCorrupt(Boolean b) {
		checkCorrupt.add(b);
	}
	
}
