package vote;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import javax.crypto.Cipher;

// class representing a single vote in the Australian Senate elections
public class Vote {
	public enum voteTypes {ABOVE_LINE, BELOW_LINE, NOT_DEFINED};
	public voteTypes voteType;
	int numCandidates;
	int numParties;
	public int[] preferencesAbove;
	public int[] preferencesBelow;
	Key privateKey;
	Key publicKey;
	public byte[] encryptedVote;
	
	
	public Vote(int numCandidates, int numParties) {
		assert (numCandidates  <= 254);
		assert (numParties <= 254);
		this.numCandidates = numCandidates;
		this.numParties = numParties;
		voteType = voteTypes.NOT_DEFINED;
		preferencesAbove = new int[0];
		preferencesBelow = new int[0];
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
		} else {
			numPreferences = preferencesBelow.length;
		}
		byte[] theseBytes = new byte[numPreferences+1];
		if (voteType == voteTypes.ABOVE_LINE) {
			theseBytes[0] = 1;
		} else {
			theseBytes[0] = 2;
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
	
	
	public byte[] encrypt() {
		encryptedVote = new byte[0];
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(4096);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			privateKey = keyPair.getPrivate();
			publicKey = keyPair.getPublic();
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			encryptedVote = cipher.doFinal(getBytes());
		} catch (Exception e) {
			
		}
		return encryptedVote;
	}
	
	public void decrypt() {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decoded = cipher.doFinal(encryptedVote);
			for (int i : decoded) {
				System.out.println(i);
			}
		} catch (Exception e) {
			
		}
	}


	public static void main(String[] args) {
		Vote v = new Vote(10,5);
		int[] votes = {1,3,5,7};
		v.voteType = voteTypes.ABOVE_LINE;
		v.preferencesAbove = votes;
		v.encrypt();
		System.out.println(v.encryptedVote.length);
		v.decrypt();
		System.out.println("Encrypted Text");
		System.out.println(Base64.getEncoder().encodeToString(v.encryptedVote));
		System.out.println("public key");
		System.out.println(Base64.getEncoder().encodeToString(v.publicKey.getEncoded()));
		System.out.println("private key");
		System.out.println("-----BEGIN RSA PRIVATE KEY-----");
		System.out.println(Base64.getEncoder().encodeToString(v.privateKey.getEncoded()));
		System.out.println("-----END RSA PRIVATE KEY-----");
	}
}
