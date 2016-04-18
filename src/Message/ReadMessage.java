package Message;

public class ReadMessage extends Message {
	
	private static final long serialVersionUID = 9095442153076363727L;
	private String fileId;
	private int pos;
	private int size; 
	
	public ReadMessage(String fileId, int pos, int size){
		this.fileId = fileId;
		this.pos = pos;
		this.size = size;
	}
	
	public String getFileId(){
		return fileId;
	}
	
	public int getPos(){
		return pos;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}
	
	
	
}
