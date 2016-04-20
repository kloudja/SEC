package Block;
import java.awt.List;
import java.security.PublicKey;
import java.util.ArrayList;

public class PublicKeyBlock extends Block{
	
	private PublicKey publicKey;
	private ArrayList<String> contentHashBlockId = new ArrayList();
	private byte[] signature;
	private int wts;

	public PublicKeyBlock (String id, PublicKey publicKey) {
		super(id);
		this.publicKey = publicKey;
	}
	
	public PublicKey getPublicKey() {
		return publicKey;
	}

	public ArrayList<String> getContentFiles() {
		return contentHashBlockId;
	}

	public void setContentFiles(ArrayList<String> contentFiles) {
		this.contentHashBlockId = contentFiles;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public ArrayList<String> getContentHashBlockId() {
		return contentHashBlockId;
	}

	public void setContentHashBlockId(ArrayList<String> contentHashBlockId) {
		this.contentHashBlockId = contentHashBlockId;
	}

	public int getWts() {
		return wts;
	}

	public void setWts(int wts) {
		this.wts = wts;
	}
}