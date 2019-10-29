import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class EncryptedVoteRecord {
	
	public byte[] AuthTokenHash = new byte[32]; // SHA256
	public byte[] VerificationCodeHash = new byte[32]; // SHA256
	public byte[] VerificationCodeEncrypted = new byte[512]; // RSA 4096 
	public byte[] VoteEncrypted = new byte[272]; // AES 

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		EncryptedVoteRecord test = new EncryptedVoteRecord();
		BallotPaper bp = new BallotPaper("\\Users\\Chris\\git\\SSE_Group_Project\\SenateCandidates2016RandomOrder.csv", "SA");
		Vote v = new Vote(bp);
		test.VoteEncrypted = CryptoEngine.encryptAES(v.getBytes(), "password");
		test.VoteEncrypted = CryptoEngine.encryptRSA(v.getBytes(), CryptoEngine.getVotePublicKey());
		System.out.println("VoteEncrypted length is " + test.VoteEncrypted.length);
		System.out.println(CryptoEngine.byteArrayToHex(test.VoteEncrypted));
		
		ArrayList<byte[]> evr = readEncryptedVotesFile("EncryptedVoteRecord.dat");
		System.out.println("Read votes :" + evr.size());
		
		Key key = CryptoEngine.getVotePrivateKey();
		for (int i = 0; i < evr.size(); i++) {
			ArrayList<byte[]> fields = divideEncryptedVoteRecord(evr.get(i));
			byte[] decryptedVerificationCode = CryptoEngine.decryptRSA(fields.get(2), key);
			if (i%100 == 0) {
				System.out.println("Processed " + i + " of " + evr.size());
			}
		}
		System.out.println("Successfully decrypted verification codes");

	}
	
	// divide record in the four fields
	public static ArrayList<byte[]> divideEncryptedVoteRecord(byte[] record) {
		byte[] AuthTokenHash = new byte[32]; // SHA256
		byte[] VerificationCodeHash = new byte[32]; // SHA256
		byte[] VerificationCodeEncrypted = new byte[512]; // RSA 4096 
		byte[] VoteEncrypted = new byte[272]; // AES 
		// first 32 bytes are AuthTokenHash
		int offset = 0;
		for (int i = 0; i < 32; i++) {
			AuthTokenHash[i] = record[i+offset];
		}
		offset += 32;
		// next 32 bytes are VerificationCodeHash
		for (int i = 0; i < 32; i++) {
			VerificationCodeHash[i] = record[i+offset];
		}
		offset += 32;
		// next 512 bytes are VerificationCodeEncrypted
		for (int i = 0; i < 512; i++) {
			VerificationCodeEncrypted[i] = record[i+offset];
		}
		// next 272 bytes are VoteEncrypted
		for (int i = 0; i < 272; i++) {
			VoteEncrypted[i] = record[i+offset];
		}
		offset += 272;
		ArrayList<byte[]> out = new ArrayList<byte[]>(4);
		out.add(0,AuthTokenHash);
		out.add(1,VerificationCodeHash);
		out.add(2,VerificationCodeEncrypted);
		out.add(3,VoteEncrypted);
		return out;
	}
	
	// read an encrypted votes file.
	// each record gets returned as a single byte array
	public static ArrayList<byte[]> readEncryptedVotesFile(String filename) {
		final int numVotes = 10000;
		final int len = 32+32+272+512; // size of encrypted vote record in bytes
		ArrayList<byte[]> encryptedVotes = new ArrayList<byte[]>(numVotes);
		System.out.println("reading " + filename);
		
		try {
			InputStream inputStream = new FileInputStream(filename);
			for (int i = 0; i < numVotes; i++) {
				byte[] record = new byte[len];
				record = inputStream.readNBytes(len);
				encryptedVotes.add(i, record);
			}
			inputStream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		System.out.println("Finished! ");
		
		return encryptedVotes;
	}

}
