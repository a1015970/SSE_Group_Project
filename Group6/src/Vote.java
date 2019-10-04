

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
	
	// these fields are used when tallying votes
	int[] convertedPreferenceBelowLine;
	int currentPreference = 0;
	double transferValue = 1.0;
	
	// constructors
	public Vote(BallotPaper ballotPaper) {
		this.ballotPaper = ballotPaper;
		voteType = voteTypes.NOT_DEFINED;
		preferencesAbove = new int[0];
		preferencesBelow = new int[0];
	}

	// constructor for Vote from encrypted vote
	public Vote(byte[] encryptedVote, Key privateKey, BallotPaper ballotPaper) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		this.ballotPaper = ballotPaper;
		voteType = voteTypes.NOT_DEFINED;
		preferencesAbove = new int[0];
		preferencesBelow = new int[0];
		// decrypt vote
		byte[] decoded = CryptoEngine.decryptRSA(encryptedVote, privateKey);
		//for (int i : decoded) {
		//	System.out.println(i);
		//}
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
		return CryptoEngine.encryptRSA(getBytes(), publicKey);
	}
	

	// methods used when tallying vote
	
	// convert above line votes to below line votes
	public void convertToBelowLineVote() throws PartyNotFoundException {
		if (voteType == voteTypes.NOT_DEFINED) {
			convertedPreferenceBelowLine = new int[1];
			convertedPreferenceBelowLine[0] = 0;
			return;
		}
		if (voteType == voteTypes.BELOW_LINE) {
			convertedPreferenceBelowLine = preferencesBelow;
			return;
		}
		// handle above line
		ArrayList<Integer> prefBelow = new ArrayList<Integer>();
		for (int pref : preferencesAbove) {
			Party party = ballotPaper.getPartyById(pref);
			for (Candidate c: party.candidates) {
				prefBelow.add(c.uid);
			}
		}
		convertedPreferenceBelowLine = new int[prefBelow.size()];
		for (int i = 0; i < prefBelow.size(); i++) {
			convertedPreferenceBelowLine[i] = prefBelow.get(i);
		}
	}
	
	// return this vote's current preferred candidate id, potentially after transferring from previous candidates
	public int getCurrentPreferredCandidateId() {
		return convertedPreferenceBelowLine[currentPreference];
	}
	
	// get the current value of this vote, a fraction between 0 and 1
	public double getCurrentTransferValue() {
		return transferValue;
	}
	
	// when this vote is transferred with fractional carry-over value, multiply current value by that fraction
	public void reduceTransferValue(double transferFraction) {
		transferValue = transferValue * transferFraction;
	}
	
	// when this vote cannot be counted for the current candidate:
	//  -- when they are elected and need to transfer excess votes
	//  -- when the delegate has excluded them
	//  -- when they are the candidate with the lowest number of votes
	// move on to this vote's next candidate
	// if we are at the last preference, set it to id == 0, indicating an exhausted vote
	public void transferVoteToNextPreference() {
		if (currentPreference == convertedPreferenceBelowLine.length-1) {
			convertedPreferenceBelowLine[currentPreference] = 0;
		} else {
			currentPreference++;
		}
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Key privateKey = CryptoEngine.getVotePrivateKey();
		Key publicKey = CryptoEngine.getVotePublicKey();
		Vote v = new Vote(null);
		int[] votes = {1,3,5,7};
		v.voteType = voteTypes.ABOVE_LINE;
		v.preferencesAbove = votes;
		byte[] encryptedVote = v.encrypt(publicKey);
		System.out.println("Length of encrypted vote: " + encryptedVote.length);
		Vote newVote = new Vote(encryptedVote, privateKey, null);
		System.out.println("Encrypted Text:");
		System.out.println(Base64.getEncoder().encodeToString(encryptedVote));
		System.out.println("public key:");
		System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
		System.out.println("private key:");
		System.out.println("-----BEGIN RSA PRIVATE KEY-----");
		System.out.println(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
		System.out.println("-----END RSA PRIVATE KEY-----");
	}
}
