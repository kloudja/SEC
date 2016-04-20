package HDS;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.text.AbstractDocument.Content;

import static javax.xml.bind.DatatypeConverter.printHexBinary;
import Block.Block;
import Block.ContentHashBlock;
import Block.PublicKeyBlock;
import Message.FSInitMessage;
import Message.FileCorruptMessage;
import Message.Message;
import Message.ReadMessage;
import Message.WriteMessage;

public class ServerThread extends Thread {
	private Socket socket = null;
	private Server blockServer;
	private String myId;
	private Cipher cipher;
	private MessageDigest messageDigest;
	private PublicKey clientPublicKey;
	private ObjectInputStream objectInputStream;
	private ObjectOutputStream objectOutputStream;

	ServerThread(Socket socket, Server blockServer){
		this.socket = socket;
		this.blockServer = blockServer;
	}

	public void run(){
		Message message = null;
		ArrayList<String> aux;
		try{
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			messageDigest = MessageDigest.getInstance("SHA-1");
			objectInputStream = new ObjectInputStream(socket.getInputStream());
			objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

			while((message = (Message)objectInputStream.readObject()) != null){
				
				 System.out.println("recebi uma cena " + message);
				switch (message.getClass().toString()) {
				
				case "class Message.FSInitMessage":
					FSInitMessage fsInitMessage = (FSInitMessage) message;
					myId = fsInitMessage.getId();
					clientPublicKey = fsInitMessage.getPublicKey();
					PublicKeyBlock publicKeyBlock = new PublicKeyBlock(myId, clientPublicKey);
					blockServer.addBlockToMap(publicKeyBlock);
					break;

				case "class Message.WriteMessage":
					WriteMessage writeMessage = (WriteMessage) message;
					System.out.println("recebi uma msg");
					byte[] originalHash = decipherSignature(writeMessage.getSignatureOfArrayIds(), writeMessage.getPublicKey());
					byte[] newHash = generateHash(convertArrayListInBytes(writeMessage.getArrayOfHashIds()));
					
					//Algoritmo-------------------------------------------------------
					byte[] originalwts = decipherSignature(writeMessage.getWts(), writeMessage.getPublicKey()); 
					PublicKeyBlock block =
							blockServer.getPublicKeyBlockById(printHexBinary
									(generateHash(writeMessage.getPublicKey().toString().getBytes())));
					int wtsRecebido = new BigInteger(originalwts).intValue();
					int wtsAntigo = block.getWts();
					
					if(wtsRecebido > wtsAntigo){
						
						System.out.println("Wts está bacano. Vou escrever no server");
						writeBlocksToServer(
								writeMessage.getBlocks(),
								writeMessage.getArrayOfHashIds(),
								writeMessage.getSignatureOfArrayIds(),
								writeMessage.getPublicKey(),
								wtsRecebido
								);
					
					}
					//Algoritmo---------------------------------------------------------------
					
					
					//Check the integrity of the ArrayList that contains only the Id's of the HashBlocks
					//In case of failing the integrity it will send a corrupt File Message with true [arrayList<String>]
					if(!Arrays.equals(originalHash, newHash)){
						objectOutputStream.writeObject(new FileCorruptMessage(true));
						System.out.println("enviei msg");
						objectOutputStream.flush();
						objectOutputStream.reset();
					}
					//Otherwise, sends the array to the server and send the corrupt File Message with false
					else{
						objectOutputStream.writeObject(new FileCorruptMessage(false));
						System.out.println("enviei msg");
						objectOutputStream.flush();
						objectOutputStream.reset();
						aux = generateArrayOfHashIds(writeMessage.getBlocks());
						System.out.println("NUMERO DE BLOCOS-> " + aux.size());
						//Now check if the ArrayList that contains the HashBlocks were not "edited" [arrayList<HashBlock>]
						//In case of detecting a difference between blocks, send a corrupt File Message with true 
						if(!compareArrayListsString(writeMessage.getArrayOfHashIds(), aux)){
							objectOutputStream.writeObject(new FileCorruptMessage(true));
							System.out.println("enviei msg");
							objectOutputStream.flush();
							objectOutputStream.reset();
						}
						//Otherwise, writeBlocksToServer
						else{
							objectOutputStream.writeObject(new FileCorruptMessage(false));
							System.out.println("enviei msg");
							objectOutputStream.flush();
							objectOutputStream.reset();
							//writeBlocksToServer(writeMessage.getBlocks(), writeMessage.getArrayOfHashIds(), writeMessage.getSignatureOfArrayIds(), writeMessage.getPublicKey());

						}
					}
					break;

				case "class Message.ReadMessage":

					ReadMessage readMessage = (ReadMessage) message;

					String idToRead = readMessage.getFileId();
					PublicKeyBlock publicKeyBlock2 = blockServer.getPublicKeyBlockById(idToRead);

					byte[] toSend = processContent(readMessage.getPos(), readMessage.getSize(), publicKeyBlock2); 
					System.out.println("Is the message to send null? "+ toSend.equals(null));
					objectOutputStream.writeObject(toSend);
					objectOutputStream.flush();
					objectOutputStream.reset();

					//					String tempContent = get(blockToRead.getId());
					//					String contentToSend = tempContent.substring(readMessage.getPos());
					//					objectOutputStream.writeObject(contentToSend);

					break;


				default:
					break;
				}

			}
			socket.close();

		} catch (IOException e){
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/  catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private byte[] processContent(int pos, int size, PublicKeyBlock publicKeyBlock2) throws IOException {
		
		byte[] contentFinal = new byte[0];
		int indexFirstBlockToRead = pos / Server.BLOCKSIZE;
		int actualBlockIndex = indexFirstBlockToRead;
		int indexLastBlockToRead = (pos + size) / Server.BLOCKSIZE;
		int initialPosFirstBlock = pos % Server.BLOCKSIZE;
		int finalPosLastBlock = (pos + size) % Server.BLOCKSIZE;
		ContentHashBlock blockToRead;
		int actualSize = size;
		boolean fileCorrupted = false;
		while(actualBlockIndex <= indexLastBlockToRead){
			try{//feito desta maneira à cão para resolver o caso de o bloco não existir ainda
				
				blockToRead = blockServer.getContentHashBlockId_block().get(publicKeyBlock2.getContentFiles().get(actualBlockIndex));
				if(!checkBlockIntegrity(blockToRead)){
					fileCorrupted = true;
				}
				
			}catch(IndexOutOfBoundsException e){
				objectOutputStream.writeObject(new FileCorruptMessage(fileCorrupted));
				objectOutputStream.flush();
				objectOutputStream.reset();
				return contentFinal;
			}
			if(actualBlockIndex == indexFirstBlockToRead){
				if(size + initialPosFirstBlock > Server.BLOCKSIZE){
					
					
					byte[] contentToAdd = Arrays.copyOfRange(get(blockToRead.getId()), initialPosFirstBlock, Server.BLOCKSIZE);
					actualSize -= contentToAdd.length;
					byte[] finalSize  = new byte[contentFinal.length + contentToAdd.length];
					System.arraycopy(contentFinal, 0, finalSize, 0, contentFinal.length);
					System.arraycopy(contentToAdd, 0, finalSize, contentFinal.length, contentToAdd.length);
					//TODO actualizar ID do(s) bloco(s)
					contentFinal = finalSize;
				}
				else{
					System.out.println(get(blockToRead.getId()).length);
					System.out.println(initialPosFirstBlock);
					int a = initialPosFirstBlock + size;
					System.out.println(a);
					objectOutputStream.writeObject(new FileCorruptMessage(fileCorrupted));
					objectOutputStream.flush();
					objectOutputStream.reset();
					return Arrays.copyOfRange(get(blockToRead.getId()), initialPosFirstBlock, initialPosFirstBlock + size);
				}
				
			}
			else if(actualBlockIndex == indexLastBlockToRead){
				byte[] contentToAdd = Arrays.copyOfRange(get(blockToRead.getId()), 0, actualSize);
				byte[] finalSize  = new byte[contentFinal.length + contentToAdd.length];
				System.arraycopy(contentFinal, 0, finalSize, 0, contentFinal.length);
				System.arraycopy(contentToAdd, 0, finalSize, contentFinal.length, contentToAdd.length);
				objectOutputStream.writeObject(new FileCorruptMessage(fileCorrupted));
				objectOutputStream.flush();
				objectOutputStream.reset();
				return finalSize;
			}
			else{
				byte[] contentToAdd = Arrays.copyOfRange(get(blockToRead.getId()), 0, Server.BLOCKSIZE);
				actualSize -= Server.BLOCKSIZE;
				byte[] finalSize  = new byte[contentFinal.length + contentToAdd.length];
				System.arraycopy(contentFinal, 0, finalSize, 0, contentFinal.length);
				System.arraycopy(contentToAdd, 0, finalSize, contentFinal.length, contentToAdd.length);
				contentFinal = finalSize;
			}
			
			actualBlockIndex++;
			
			
			
		}
		objectOutputStream.writeObject(new FileCorruptMessage(fileCorrupted));
		objectOutputStream.flush();
		objectOutputStream.reset();

		return contentFinal;		
	}

	private byte[] appendByteArray(byte[] content, byte[] temp) {
		byte[] c = new byte[content.length + temp.length];
		System.arraycopy(content, 0, c, 0, content.length);
		System.arraycopy(temp, 0, c, content.length, temp.length);
		return c;
	}

	private byte[] getByteContentOnServer(int indexOfFirstBlock, ArrayList<String> tempListOfBlocks) throws UnsupportedEncodingException {

		String IdfirstBlock = tempListOfBlocks.get(indexOfFirstBlock);
		byte[] firstBlock = blockServer.getContentHashBlockId_block().get(IdfirstBlock).getContent();
		System.out.println("Is content retrieved from server null? " + firstBlock.equals(null));
		System.out.println(new String(firstBlock, "UTF-8"));
		return firstBlock;
	}

	private boolean compareArrayListsString(ArrayList<String> arrayList1, ArrayList<String> arrayList2) {
		for(int i = 0; i < arrayList1.size(); i++){
			if(!arrayList1.get(i).equals(arrayList2.get(i))){
				return false;
			}
		}
		return true;
	}

	private void writeBlocksToServer(ArrayList<ContentHashBlock> arrayOfHashBlocks, ArrayList<String> idHashBlocks, byte[] signature, PublicKey publicKey) {
		for(int i = 0; i < arrayOfHashBlocks.size(); i++){
			blockServer.put_h(arrayOfHashBlocks.get(i));
		}
		//blockServer.getPublicKeyBlock(myId);
		blockServer.put_k(idHashBlocks, signature, publicKey);
	}

	private void writeBlocksToServer(ArrayList<ContentHashBlock> blocks, ArrayList<String> arrayOfHashIds,
			byte[] signatureOfArrayIds, PublicKey publicKey, int wtsRecebido) {

		for(int i = 0; i < blocks.size(); i++){
			blockServer.put_h(blocks.get(i));
		}
		//blockServer.getPublicKeyBlock(myId);
		blockServer.put_k(arrayOfHashIds, signatureOfArrayIds, publicKey, wtsRecebido);
		
	}
	
	
	private byte[] generateHash(byte[] b) {
		messageDigest.update(b);
		return messageDigest.digest();
	}

	private byte[] decipherSignature(byte[] signature, PublicKey publicKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		return cipher.doFinal(signature);
	}

	/*private byte[] convertArrayListInBytes(ArrayList<ContentHashBlock> arrayList) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		for (ContentHashBlock element : arrayList) {
			out.writeObject(element);
		}
		return baos.toByteArray();
	}*/

	private byte[] convertArrayListInBytes(ArrayList<String> arrayList) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		for (String element : arrayList) {
			out.writeUTF(element);
		}
		return baos.toByteArray();
	}

	public ArrayList<String> generateArrayOfHashIds (ArrayList<ContentHashBlock> contentHashBlocks){
		ArrayList<String> arrayOfHashIds =  new ArrayList<>();
		for(int i = 0; i < contentHashBlocks.size(); i++){
			arrayOfHashIds.add(printHexBinary(generateHash(contentHashBlocks.get(i).getContent())));
		}
		return arrayOfHashIds;
	}
	
	public byte[] get(String id){
		return blockServer.getContentHashBlockId_block().get(id).getContent();			

	}
	
	public boolean checkBlockIntegrity(ContentHashBlock block){
		return block.getId().equals(printHexBinary(generateHash(block.getContent())));
	}
}
