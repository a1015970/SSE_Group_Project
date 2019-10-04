

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// class representing a single vote in the Australian Senate elections
public class Vote {
	public enum voteTypes {ABOVE_LINE, BELOW_LINE, NOT_DEFINED};
	public voteTypes voteType;
	public int[] preferencesAbove;
	public int[] preferencesBelow;

	public BallotPaper ballotPaper;
	
	// constructors
	public Vote(BallotPaper ballotPaper) {
		this.ballotPaper = ballotPaper;
		voteType = voteTypes.NOT_DEFINED;
		preferencesAbove = new int[0];
		preferencesBelow = new int[0];
	}

	public Vote(byte[] encryptedVote, Key privateKey, BallotPaper ballotPaper) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		this.ballotPaper = ballotPaper;
		voteType = voteTypes.NOT_DEFINED;
		preferencesAbove = new int[0];
		preferencesBelow = new int[0];
		// decrypt vote
		//Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		//cipher.init(Cipher.DECRYPT_MODE, privateKey);
		//byte[] decoded = cipher.doFinal(encryptedVote);
		byte[] decoded = CryptoEngine.decryptRSA(encryptedVote, privateKey);
		for (int i : decoded) {
			System.out.println(i);
		}
		// create new Vote object
		if (decoded[0] == 1) {
			this.voteType = voteTypes.ABOVE_LINE;
			this.preferencesAbove = new int[decoded.length-1];
			for (int i = 1; i < decoded.length; i++) {
				this.preferencesAbove[i-1] = decoded[i];
			}
		} else if (decoded[0] == 2) {
			this.voteType = voteTypes.BELOW_LINE;
			this.preferencesBelow = new int[decoded.length-1];
			for (int i = 1; i < decoded.length; i++) {
				this.preferencesBelow[i-1] = decoded[i];
			}
		} else {
			this.voteType = voteTypes.NOT_DEFINED;
		}
	}


	
	// convert this Vote to a compact Byte array
	public byte[] getBytes() {
		if (voteType == voteTypes.NOT_DEFINED) {
			byte[] theseBytes = {-1};
			return theseBytes;
		}
		int numPreferences;
		if (voteType == voteTypes.ABOVE_LINE) {
			numPreferences = preferencesAbove.length;
		} else if (voteType == voteTypes.BELOW_LINE) {
			numPreferences = preferencesBelow.length;
		} else {
			numPreferences = 0;
		}
		byte[] theseBytes = new byte[numPreferences+1];
		if (voteType == voteTypes.ABOVE_LINE) {
			theseBytes[0] = 1;
		} else if (voteType == voteTypes.BELOW_LINE) {
			theseBytes[0] = 2;
		} else {
			theseBytes[0] = -1;
		}
		for (int i = 0; i < numPreferences; i++) {
			if (voteType == voteTypes.ABOVE_LINE) {
				theseBytes[i+1] = (byte) preferencesAbove[i];
			} else {
				theseBytes[i+1] = (byte) preferencesBelow[i];
			}
		}
		return theseBytes;
	}
	
	
	public byte[] encrypt(Key publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		//byte[] encryptedVote = new byte[0];
		//Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		//cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		//encryptedVote = cipher.doFinal(getBytes());
		//return encryptedVote;
		return CryptoEngine.encryptRSA(getBytes(), publicKey);
	}
	

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(4096);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		Key privateKey = keyPair.getPrivate();
		Key publicKey = keyPair.getPublic();
		Vote v = new Vote(null);
		int[] votes = {1,3,5,7};
		v.voteType = voteTypes.ABOVE_LINE;
		v.preferencesAbove = votes;
		byte[] encryptedVote = v.encrypt(publicKey);
		System.out.println(encryptedVote.length);
		Vote newVote = new Vote(encryptedVote, privateKey, null);
		System.out.println("Encrypted Text");
		System.out.println(Base64.getEncoder().encodeToString(encryptedVote));
		System.out.println("public key");
		System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
		System.out.println("private key");
		System.out.println("-----BEGIN RSA PRIVATE KEY-----");
		System.out.println(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
		System.out.println("-----END RSA PRIVATE KEY-----");
	}
}
