import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


// Perform a test of the tally system, using pre-generated fake vote data
// see README.TXT for more details
public class TallyTest {
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		BallotPaper bp = new BallotPaper("SenateCandidates2016RandomOrder.csv", "SA");
		
		ArrayList<byte[]> evr = EncryptedVoteRecord.readEncryptedVotesFile("EncryptedVoteRecord.dat");
		System.out.println("Read votes :" + evr.size());
		
		Key key = CryptoEngine.getVotePrivateKey();
		System.out.println("Verifying vote ...");
		EncryptedVoteRecord.verifyEncryptedVotes(evr, key);
		System.out.println("Successfully verified vote");
		
		System.out.println("Decrypting vote ...");
		ArrayList<Vote> votes = EncryptedVoteRecord.decryptVotes(evr, key, bp);
		System.out.println("Successfully decrypted vote");
		
		System.out.println("Starting tally ...");
		Tally.ballotPaper = bp;
		Tally.voteList = votes;
		Tally.numCandidatesToElect = 6; //6 for half senate, 12 for full senate
		Tally.tallyVotes();
		System.out.println("Tally completed");

	}

}
