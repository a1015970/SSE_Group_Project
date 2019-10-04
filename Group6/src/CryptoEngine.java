import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CryptoEngine {
	
	static Cipher encodeRsaCipher = null; // we want to re-use Ciphers
	static Key keyEncodeRSA = null;
	static Cipher decodeRsaCipher = null; // we want to re-use Ciphers
	static Key keyDecodeRSA = null;
	
	static Key votePublicKey = null;
	static Key votePrivateKey = null;
	

	// encode a byte array using RSA
	public static byte[] encryptRSA(byte[] plaintext, Key publicKey) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (encodeRsaCipher == null) {
			encodeRsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		}
		if ((keyEncodeRSA == null) || (keyEncodeRSA != publicKey)) {
			// need to initialise/reinitialise Cipher
			encodeRsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			keyEncodeRSA = publicKey;
		}
		byte[] encrypted = encodeRsaCipher.doFinal(plaintext);
		return encrypted;
	}
	
	// decode a byte array using RSA
	public static byte[] decryptRSA(byte[] encrypted, Key privateKey) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (decodeRsaCipher == null) {
			decodeRsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		}
		if ((keyDecodeRSA == null) || (keyDecodeRSA != privateKey)) {
			decodeRsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
			keyDecodeRSA = privateKey;
		}
		byte[] plaintext = decodeRsaCipher.doFinal(encrypted);
		return plaintext;
	}
	
	
	// calculate SHA256 hash of input
	public static byte[] hashSHA256(byte[] input) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");  
        return md.digest(input);  
	}
	
	// calculate SHA256 hash of input after "iterations" hash iterations
	public static byte[] hashSHA256(byte[] input, int iterations) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] output = input.clone();
		for (int i = 0; i < iterations; i++) {
			output = md.digest(output);
		}
        return output;  
	}
	
	// return the keys for encrypting and decrypting vote.
	// These should really be read from a file - this is an interim method
	public static Key getVotePublicKey() throws NoSuchAlgorithmException {
		if (votePublicKey == null) {
			generateVoteKeys();
		}
		return votePublicKey;
	}
	
	public static Key getVotePrivateKey() throws NoSuchAlgorithmException {
		if (votePrivateKey == null) {
			generateVoteKeys();
		}
		return votePrivateKey;
	}
	
	static void generateVoteKeys() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(4096);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		votePrivateKey = keyPair.getPrivate();
		votePublicKey = keyPair.getPublic();
	}
	
	
}
