import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
	}

}
