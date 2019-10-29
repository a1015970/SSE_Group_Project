

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// class representing a single vote in the Australian Senate elections
@SuppressWarnings("unused")
public class Vote {
	public enum voteTypes {ABOVE_LINE, BELOW_LINE, NOT_DEFINED};
	public voteTypes voteType;
	public int[] preferencesAbove;
	public int[] preferencesBelow;
	public boolean isExhausted;

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
		isExhausted = false;
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
		isExhausted = false;
	}

	@Override
	public String toString() {
		String str = "";
		try {
			if (this.voteType == voteTypes.NOT_DEFINED) {
				return "Vote type - not defined";
			}
			if (this.voteType == voteTypes.ABOVE_LINE) {
				str += "Above line - " + this.preferencesAbove.length + " preferences\n";
				for (int i = 0; i < this.preferencesAbove.length; i++) {
					str += String.valueOf(i) + "  " + ballotPaper.getPartyById(this.preferencesAbove[i]).toString() + "\n";
				}
				return str;
			}
			if (this.voteType == voteTypes.BELOW_LINE) {
				str += "Below line - " + this.preferencesBelow.length + " preferences\n";
				for (int i = 0; i < this.preferencesBelow.length; i++) {
					str += String.valueOf(i) + "  " + ballotPaper.getCandidateById(this.preferencesBelow[i]).toString() + "\n";
				}
				return str;
			}
		} catch (CandidateNotFoundException|PartyNotFoundException e) {
			e.printStackTrace();
		}
		return str; // should never get here!
	}
	
	// return a random vote - used for testing
	// when picking random candidates, generate non-uniform distrubution by
	// picking two random numbers and using the minimum
	public static Vote getRandomVote(BallotPaper ballotPaper) {
		Vote vote = new Vote(ballotPaper);
		Random rand = new Random();
		// 80% above line
		if (rand.nextFloat() < 0.8) {
			vote.voteType = voteTypes.ABOVE_LINE;
			// vote for a random number of parties >=6, <=numParties
			int numAbove = ballotPaper.partyList.size();
			int numVotes = 6 + rand.nextInt(numAbove-6);
			// put party uids in random order, and select first n of them
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < numAbove; i++) {
				list.add(i);
			}
			vote.preferencesAbove = new int[numVotes];
			for (int i = 0; i < numVotes; i++) {
				int pref = rand.nextInt(list.size());
				pref = java.lang.Math.min(pref, rand.nextInt(list.size()));
				pref = java.lang.Math.min(pref, rand.nextInt(list.size()));
				vote.preferencesAbove[i] = list.get(pref);
				list.remove(pref);
			}
		} else {
			vote.voteType = voteTypes.BELOW_LINE;
			// vote for a random number of candidates >=12, <=numCandidates
			int numBelow = ballotPaper.getNumCandidates();
			int numVotes = 12 + rand.nextInt(numBelow-12);
			// put candidate uids in random order, and select first n of them
			List<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < numBelow; i++) {
				list.add(i);
			}
			vote.preferencesBelow = new int[numVotes];
			for (int i = 0; i < numVotes; i++) {
				int pref = rand.nextInt(list.size());
				pref = java.lang.Math.min(pref, rand.nextInt(list.size()));
				pref = java.lang.Math.min(pref, rand.nextInt(list.size()));
				vote.preferencesBelow[i] = list.get(pref);
				list.remove(pref);
			}
		}
		return vote;
	}
	
	// convert this Vote to a compact Byte array
	public byte[] getBytes() {
		byte[] theseBytes = new byte[256];
		if (voteType == voteTypes.NOT_DEFINED) {
			theseBytes[0] = -1;
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
		if (voteType == voteTypes.ABOVE_LINE) {
			theseBytes[0] = 1;
		} else if (voteType == voteTypes.BELOW_LINE) {
			theseBytes[0] = 2;
		} else {
			theseBytes[0] = -1;
		}
		theseBytes[1] = (byte) numPreferences;
		for (int i = 0; i < numPreferences; i++) {
			if (voteType == voteTypes.ABOVE_LINE) {
				theseBytes[i+2] = (byte) preferencesAbove[i];
			} else {
				theseBytes[i+2] = (byte) preferencesBelow[i];
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
		currentPreference = 0;
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
		preferencesBelow = convertedPreferenceBelowLine;
		voteType = voteTypes.BELOW_LINE;
	}
	
	// return this vote's current preferred candidate id, potentially after transferring from previous candidates
	// return -1 if vote is exhausted
	public int getCurrentPreferredCandidateId() {
		if (isExhausted) {
			return -1;
		}
		return convertedPreferenceBelowLine[currentPreference];
	}
	
	// get the current value of this vote, a fraction between 0 and 1
	public double getCurrentTransferValue() {
		if (isExhausted) {
			return 0.0;
		} else {
			return transferValue;
		}
	}
	
	// when this vote is transferred with fractional carry-over value, multiply current value by that fraction
	public void reduceTransferValue(double transferFraction) {
		transferValue = transferValue * transferFraction;
	}
	
	// see if currentPreference is for an excluded candidate
	public boolean isCurrentPreferenceExcluded() {
		return ballotPaper.isCandidateExcluded(convertedPreferenceBelowLine[currentPreference]);
	}
	
	// when this vote cannot be counted for the current candidate:
	//  -- when they are elected and need to transfer excess votes
	//  -- when the delegate has excluded them
	//  -- when they are the candidate with the lowest number of votes
	// move on to this vote's next candidate, skipping over already excluded candidates
	// if we are at the last preference, set isExhausted to true
	public void transferVoteToNextPreference() {
		while (currentPreference < convertedPreferenceBelowLine.length-1) {
			currentPreference++;
			if (!ballotPaper.isCandidateExcluded(convertedPreferenceBelowLine[currentPreference])) {
				// this preference is still valid, so return
				return;
			}
		}
		// if we've got to the end of the preferences, then this vote is exhausted
		isExhausted = true;
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
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
		System.out.println("-----BEGIN RSA PUBLIC KEY-----");
		System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
		System.out.println("-----END RSA PUBLIC KEY-----");
		System.out.println("private key:");
		System.out.println("-----BEGIN RSA PRIVATE KEY-----");
		System.out.println(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
		System.out.println("-----END RSA PRIVATE KEY-----");
	}
}
