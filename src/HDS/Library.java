package HDS;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import static javax.xml.bind.DatatypeConverter.printHexBinary;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.print.DocFlavor.SERVICE_FORMATTED;

import Block.ContentHashBlock;
import Message.*;

public class Library {

	private Socket socket;
	private ObjectOutputStream objectOutputStream;
	private ObjectInputStream objectInputStream;
	private String id;
	private KeyPair keyPair;
	private Cipher cipher;
	private MessageDigest messageDigest;
	private ArrayList<ContentHashBlock> contentHashBlocks = new ArrayList<>();
	private ArrayList<Socket_OOS_OIS> sockets = new ArrayList<>();
	private ArrayList<Boolean> checkCorrupt = new ArrayList<>();

	public ArrayList<Boolean> getCheckCorrupt() {
		return checkCorrupt;
	}

	public synchronized void setCheckCorrupt(Boolean b) {
		checkCorrupt.add(b);
	}

	public Library() throws UnknownHostException, IOException, InterruptedException {
		for(int i = 0; i < Server.NMR_SERVERS; i++){
		socket = new Socket("localhost", Server.PORT+i);
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectInputStream = new ObjectInputStream(socket.getInputStream());
		Socket_OOS_OIS soo = new Socket_OOS_OIS(socket, objectOutputStream, objectInputStream);
		sockets.add(soo);
		
		System.out.println("adicionado um novo socket");
		System.out.println("existem " + sockets.size() + " sockets");
		}
	}

	public String Fsinit() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {

		// generate an RSA key
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024);
		keyPair = keyGen.generateKeyPair();

		// get an RSA cipher object and print the provider
		cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		messageDigest = MessageDigest.getInstance("SHA-1");

		byte[] bytesID = generateHash(keyPair.getPublic().toString().getBytes());
		id = printHexBinary(bytesID);
		FSInitMessage message = new FSInitMessage(id, keyPair.getPublic());
		broadcastToServers(message);
//		objectOutputStream.writeObject(message);
//		objectOutputStream.flush();
//		objectOutputStream.reset();

		return id;
	}

	public synchronized boolean FsWrite(int pos, byte[] content) throws IOException, InvalidKeyException, IllegalBlockSizeException,
	BadPaddingException, ClassNotFoundException {
		byte[] actualContent = content;
		int indexFirstBlockToWrite = pos / Server.BLOCKSIZE;
		int actualBlockIndex = indexFirstBlockToWrite;
		int indexLastBlockToWrite = (pos + content.length) / Server.BLOCKSIZE;
		int initialPosFirstBlock = pos % Server.BLOCKSIZE;
		int finalPosLastBlock = (pos + content.length) % Server.BLOCKSIZE;
		
		int missingBlocksId = 0;
		ContentHashBlock blockToWrite;
		byte[] contentToBlock;
		byte[] blankBlock = new byte[Server.BLOCKSIZE];
		
		while(missingBlocksId < indexFirstBlockToWrite){
			try{//feito desta maneira à cão para resolver o caso de o bloco não existir ainda
				contentHashBlocks.get(missingBlocksId);
			}catch(IndexOutOfBoundsException e){
				blockToWrite = new ContentHashBlock(printHexBinary(generateHash(blankBlock)), 0);
				contentHashBlocks.add(missingBlocksId, blockToWrite);
			}
			missingBlocksId++;
		}
		
		while(actualBlockIndex <= indexLastBlockToWrite){
			try{//feito desta maneira à cão para resolver o caso de o bloco não existir ainda
				blockToWrite = contentHashBlocks.get(actualBlockIndex);
			}catch(IndexOutOfBoundsException e){
				blockToWrite = new ContentHashBlock(printHexBinary(generateHash(content)), 0);
				contentHashBlocks.add(actualBlockIndex,blockToWrite);
			}
			if(actualBlockIndex == indexFirstBlockToWrite){
				if(actualContent.length + initialPosFirstBlock > Server.BLOCKSIZE){
					contentToBlock = Arrays.copyOfRange(actualContent, 0, Server.BLOCKSIZE - initialPosFirstBlock);
					blockToWrite.writeContent(contentToBlock, initialPosFirstBlock);
					//TODO actualizar ID do(s) bloco(s)
					blockToWrite.setBlockId(printHexBinary(generateHash(blockToWrite.getContent())));
					actualContent = Arrays.copyOfRange(actualContent, contentToBlock.length, actualContent.length);
				}
				else{
					contentToBlock = actualContent;
					blockToWrite.writeContent(contentToBlock, initialPosFirstBlock);
					//TODO actualizar ID do(s) bloco(s)
					blockToWrite.setBlockId(printHexBinary(generateHash(blockToWrite.getContent())));
				}
				
			}
			else if(actualBlockIndex == indexLastBlockToWrite){
				blockToWrite.writeContent(actualContent, 0);
				blockToWrite.setBlockId(printHexBinary(generateHash(blockToWrite.getContent())));
			}
			else{
				contentToBlock = Arrays.copyOfRange(actualContent, 0, Server.BLOCKSIZE);
				blockToWrite.writeContent(contentToBlock, 0);
				//TODO actualizar ID do(s) bloco(s)
				blockToWrite.setBlockId(printHexBinary(generateHash(blockToWrite.getContent())));
				actualContent = Arrays.copyOfRange(actualContent, contentToBlock.length, actualContent.length);
			}
			
			actualBlockIndex++;
		}
		
	
		

		ArrayList<String> arrayOfHashIds = generateArrayOfHashIds();//OK
		
		byte[] signatureOfArrayIds = signContent(convertArrayListInBytes(arrayOfHashIds));
		
		

		WriteMessage message = new WriteMessage(contentHashBlocks, arrayOfHashIds, signatureOfArrayIds,
				keyPair.getPublic());
		for(int i = 0; i < Server.NMR_SERVERS; i++){
		new ClientWriteThread(message, sockets.get(i), this).start();
		
		}
		
//		objectOutputStream.writeObject(message);
//		objectOutputStream.flush();
//		objectOutputStream.reset();
		
		
		while(checkCorrupt.size()!=Server.NMR_SERVERS){
			try {
				this.wait();
			} catch (InterruptedException e) {
				System.out.println("catchs");
			}
			System.out.println("recebi uma resposta");
			
		}
		//TODO
		int certos=0;
		int errados=0;
		for(int i = 0; i < checkCorrupt.size(); i++){
			 if(checkCorrupt.get(i)==true){
				 certos++;
			 }
			 else{
				 errados++;
			 } 
		}
		
		if(certos > errados)return true;
		else return false;
		
//		FileCorruptMessage m = (FileCorruptMessage) objectInputStream.readObject();
//		// Verifica se a 1ª mensagem que virificou os id's dos Hasblocks está
//		// corrupta
//		if (m.isCorrupted()) {
//			return false;
//		} else {
//			// Verifica se a 2ª mensagem que verifica mesmo os HashBlocks está
//			// corrupta
//			FileCorruptMessage hashBlockCorruptMessage = (FileCorruptMessage) objectInputStream.readObject();
//			if (!hashBlockCorruptMessage.isCorrupted())
//				return true;
//			else{
//				return false;
//			}
//		}

	}

	private byte[] signContent(byte[] content)
			throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		return cipher.doFinal(generateHash(content));
	}

	private byte[] generateHash(byte[] b) {
		messageDigest.update(b);
		return messageDigest.digest();
	}

	private byte[] convertArrayListInBytes(ArrayList<String> arrayList) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		for (String element : arrayList) {
			out.writeUTF(element);
		}
		return baos.toByteArray();
	}

	public ArrayList<String> generateArrayOfHashIds() {
		ArrayList<String> arrayOfHashIds = new ArrayList<>();
		for (int i = 0; i < contentHashBlocks.size(); i++) {
			arrayOfHashIds.add(contentHashBlocks.get(i).getId());
		}
		return arrayOfHashIds;
	}

	public int FsRead(String id, int pos, int size) throws IOException, ClassNotFoundException {
		//TODO verificar integridade dos metadados
		ReadMessage message = new ReadMessage(id, pos, size);
		objectOutputStream.writeObject(message);
		objectOutputStream.flush();
		objectOutputStream.reset();
		FileCorruptMessage fileCorruptedMessage = (FileCorruptMessage) objectInputStream.readObject();
		if(fileCorruptedMessage.isCorrupted()){
			System.out.println("FILE IS CORRUPTED");
		}
		byte[] content = (byte[]) objectInputStream.readObject();
		return content.length;
	}
	
	private void broadcastToServers(Message message) throws IOException{
		for(int i = 0; i < sockets.size(); i++){
			ObjectOutputStream oos = sockets.get(i).getOos();
			oos.writeObject(message);
			oos.flush();
			oos.reset();
			System.out.println("mensagem enviada para socket:" + sockets.get(i).getSocket());
		}
	}
	
}
