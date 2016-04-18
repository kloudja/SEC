package Message;

import java.security.PublicKey;

public class FSInitMessage extends Message{

	private String id;
	private PublicKey publicKey;
	public FSInitMessage(String id, PublicKey publicKey) {
		this.id = id;
		this.publicKey = publicKey;
	}
	
	public String getId(){
		return id;
	}
	
	public PublicKey getPublicKey() {
		return publicKey;
	}
}
