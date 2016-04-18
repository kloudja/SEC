package Message;

import java.security.PublicKey;
import java.util.ArrayList;

import Block.ContentHashBlock;

public class WriteMessage extends Message {
	
	
	private ArrayList<ContentHashBlock> blocks;
	private PublicKey publicKey;
	private byte[] signatureOfArrayIds;
	private ArrayList<String> arrayOfHashIds; 
	
	public WriteMessage(ArrayList<ContentHashBlock> blocks, ArrayList<String> arrayOfHashIds, byte[] signature, PublicKey publicKey){
		this.blocks = blocks;
		this.arrayOfHashIds = arrayOfHashIds;
		this.signatureOfArrayIds = signature;
		this.publicKey = publicKey;
	}
	
	
	public ArrayList<ContentHashBlock> getBlocks() {
		return blocks;
	}


	public void setBlocks(ArrayList<ContentHashBlock> blocks) {
		this.blocks = blocks;
	}


	public byte[] getSignatureOfArrayIds() {
		return signatureOfArrayIds;
	}


	public void setSignatureOfArrayIds(byte[] signatureOfArrayIds) {
		this.signatureOfArrayIds = signatureOfArrayIds;
	}


	public ArrayList<String> getArrayOfHashIds() {
		return arrayOfHashIds;
	}


	public void setArrayOfHashIds(ArrayList<String> arrayOfHashIds) {
		this.arrayOfHashIds = arrayOfHashIds;
	}


	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}


	public PublicKey getPublicKey() {
		return publicKey;
	}
}
