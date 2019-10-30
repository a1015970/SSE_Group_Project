import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CreateEncryptedVotesFile {
	
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

	
	// to create a file of fake votes we need:
	// 1) a ballot paper
	// 2) N fake authentication tokens - to hash
	// 3) N fake verification codes - to hash and to encrypt with RSA
	// 4) N fake votes - to encrypt using AES, using the fake verification code
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		Random rand = new Random();
		final int numVotes = 10000;
		
		BallotPaper bp = new BallotPaper("\\Users\\Chris\\git\\SSE_Group_Project\\SenateCandidates2016RandomOrder.csv", "SA");

		long[] authTokens = new long[numVotes];
		long[] verifCodes = new long[numVotes];
		for (int i = 0; i < numVotes; i++) {
			authTokens[i] = 1000000000000000L + i;
			verifCodes[i] = rand.nextLong();
		}
		
		ArrayList<byte[]> authTokenHashes = new ArrayList<byte[]>(numVotes);
		ArrayList<byte[]> verifCodeHashes = new ArrayList<byte[]>(numVotes);
		ArrayList<byte[]> verifCodeEncrypted = new ArrayList<byte[]>(numVotes);
		ArrayList<byte[]> votesEncrypted = new ArrayList<byte[]>(numVotes);
		byte[] prevTime = longToBytes(java.lang.System.currentTimeMillis());
		Key key = CryptoEngine.getVotePublicKey();
		for (int i = 0; i < numVotes; i++) {
			authTokenHashes.add(i, CryptoEngine.hashSHA256(longToBytes(authTokens[i])));
			verifCodeHashes.add(i, CryptoEngine.hashSHA256(longToBytes(verifCodes[i])));
			Vote v = Vote.getRandomVote(bp);
			votesEncrypted.add(i, CryptoEngine.encryptAES(v.getBytes(), new String(longToBytes(verifCodes[i]))));
			// verifCodeAndTimeStamps contains 3 pieces of information - key and two timestamps
			byte[] verifCodeAndTimeStamps = new byte[8*3];
			byte[] thisTime = longToBytes(java.lang.System.currentTimeMillis());
			byte[] verifCode = longToBytes(verifCodes[i]);
			for (int j = 0; j < 8; j++) {
				verifCodeAndTimeStamps[j] = verifCode[j];
				verifCodeAndTimeStamps[j+8] = prevTime[j];
				verifCodeAndTimeStamps[j+16] = thisTime[j];
			}
			verifCodeEncrypted.add(i, CryptoEngine.encryptRSA(verifCodeAndTimeStamps, key));
			prevTime = thisTime;
			if (i%1000 == 0) {
				System.out.println("Processed " + i + " of " + numVotes);
			}
			/*
			// small in-place test a debug
			Key privKey = CryptoEngine.getVotePrivateKey();
			byte[] codeDec = CryptoEngine.decryptRSA(verifCodeEncrypted.get(i), privKey);
			long tmp = bytesToLong(codeDec);
			byte[] testDecrypt = CryptoEngine.decryptAES(votesEncrypted.get(i), new String(longToBytes(tmp)));
			System.out.println(authTokenHashes.get(i).length);
			System.out.println(verifCodeHashes.get(i).length);
			System.out.println(verifCodeEncrypted.get(i).length);
			System.out.println(votesEncrypted.get(i).length);
			*/
		}

		// now write them out to file
		String outputFile = "EncryptedVoteRecord.dat";
		System.out.println("writing " + outputFile);

		try {
			OutputStream outputStream = new FileOutputStream(outputFile);
			for (int i = 0; i < numVotes; i++) {
				outputStream.write(authTokenHashes.get(i));
				outputStream.write(verifCodeHashes.get(i));
				outputStream.write(verifCodeEncrypted.get(i));
				outputStream.write(votesEncrypted.get(i));
			}
			outputStream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		System.out.println("Finished! ");

	}

}
