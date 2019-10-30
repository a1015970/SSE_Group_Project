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
	
	public static long bytesToLong(byte[] b) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (b[i] & 0xFF);
	    }
	    return result;
	}
	public static byte[] longToBytes(long l) {
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
	    return result;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		BallotPaper bp = new BallotPaper("\\Users\\Chris\\git\\SSE_Group_Project\\SenateCandidates2016RandomOrder.csv", "SA");
		
		ArrayList<byte[]> evr = readEncryptedVotesFile("EncryptedVoteRecord.dat");
		System.out.println("Read votes :" + evr.size());
		
		Key key = CryptoEngine.getVotePrivateKey();
		/*
		for (int i = 0; i < evr.size(); i++) {
			ArrayList<byte[]> fields = divideEncryptedVoteRecord(evr.get(i));
			byte[] decryptedVerificationCode = CryptoEngine.decryptRSA(fields.get(2), key);
			if (i%100 == 0) {
				System.out.println("Processed " + i + " of " + evr.size());
			}
		}
		System.out.println("Successfully decrypted verification codes");
		*/
		System.out.println("Verifying vote ...");
		//verifyEncryptedVotes(evr, key);
		System.out.println("Successfully verified vote");
		
		System.out.println("Decrypting vote ...");
		ArrayList<Vote> votes = decryptVotes(evr, key, bp);
		System.out.println("Successfully decrypted vote");
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
		offset += 512;
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
		System.out.println("File successfully read.");
		return encryptedVotes;
	}

	// this checks two things about a bunch of encrypted vote records:
	// 1) that the AuthTokenHash belongs to an authorized voter
	// 2) that the VerificationCodeEncrypted timestamps are in order
	public static boolean verifyEncryptedVotes(ArrayList<byte[]> votes, Key privateKey) {
		// re-create fake authTokens
		final int numVotes = 10000;
		long[] authTokens = new long[numVotes];
		for (int i = 0; i < numVotes; i++) {
			authTokens[i] = 1000000000000000L + i;
		}
		// re-create hashes of auth tokens
		ArrayList<byte[]> authTokenHashesRef = new ArrayList<byte[]>(numVotes);
		for (int i = 0; i < numVotes; i++) {
			try {
				authTokenHashesRef.add(i, CryptoEngine.hashSHA256(longToBytes(authTokens[i])));
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Get the fields we need from the encrypted votes
		long prevTime = 0;
		for (int i = 0; i < numVotes; i++) {
			ArrayList<byte[]> fields = divideEncryptedVoteRecord(votes.get(i));
			byte[] authTokenHash = fields.get(0);
			// for these fake auth token hashes, they're in the right order so just compare to the correct one
			if (! java.util.Arrays.equals(authTokenHashesRef.get(i), authTokenHash)) {
				System.out.println("Unknown authorization token hash at index " + i);
				System.out.println(CryptoEngine.byteArrayToHex(authTokenHash));
				System.out.println(CryptoEngine.byteArrayToHex(authTokenHashesRef.get(i)));
				return false;
			}
			byte[] verificationCodeDecrypted = new byte[24];
			try {
				verificationCodeDecrypted = CryptoEngine.decryptRSA(fields.get(2), privateKey);
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException|BadPaddingException|NoSuchAlgorithmException|NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] prevTimeBytes = new byte[8];
			byte[] thisTimeBytes = new byte[8];
			for (int j = 0; j < 8; j++) {
				prevTimeBytes[j] = verificationCodeDecrypted[j+8];
				thisTimeBytes[j] = verificationCodeDecrypted[j+16];
			}
			if (i==0) {
				prevTime = bytesToLong(thisTimeBytes);
				continue; // can't compare timestamp on first iteration
			}
			if (bytesToLong(prevTimeBytes) != prevTime) {
				System.out.println("Timestamp mismatch at index " + i);
				System.out.println(prevTime);
				System.out.println(bytesToLong(prevTimeBytes));
				return false;
			}
			prevTime = bytesToLong(thisTimeBytes);
		}
		return true;
	}
	
	// actually decrypt the votes
	public static ArrayList<Vote> decryptVotes(ArrayList<byte[]> encryptedVotes, Key privateKey, BallotPaper ballotPaper) {
		final int numVotes = 10000;
		ArrayList<Vote> votes = new ArrayList<Vote>();
		
		// iterate over array
		// decrypting verification code
		// using verification code to get AES key
		// use AES key to decrypt actual vote as byte array
		// create Vote object from byte array
		
		for (int i = 0; i < numVotes; i++) {
			// get verification code
			ArrayList<byte[]> fields = divideEncryptedVoteRecord(encryptedVotes.get(i));
			byte[] verificationCodeDecrypted = new byte[24];
			try {
				verificationCodeDecrypted = CryptoEngine.decryptRSA(fields.get(2), privateKey);
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException|BadPaddingException|NoSuchAlgorithmException|NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] verifCode = new byte[8];
			for (int j = 0; j < 8; j++) {
				verifCode[j] = verificationCodeDecrypted[j];
			}
			// get password from verification code
			long verifCodeLong = bytesToLong(verifCode);
			// decrypt vote record
			byte[] voteBytes = null;
			try {
				voteBytes = CryptoEngine.decryptAES(fields.get(3), new String(longToBytes(verifCodeLong)));
			} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
					| NoSuchPaddingException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
				e.printStackTrace();
				System.out.println("Verification code is " + bytesToLong(verifCode));
				System.out.println("Index is " + i);
			}
			// create Vote object from vote array
			Vote v = new Vote(voteBytes, ballotPaper);
			votes.add(v);
		}
		return votes;
	}
}
