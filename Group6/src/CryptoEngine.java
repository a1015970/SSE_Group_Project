import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
	
	
	// encode a byte array using 256-bit AES, using a password thru PBKDF2
	public static byte[] encryptAES(byte[] plaintext, String password) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		byte[] salt = {1,2,3,4,5,6,7,8,9,0};
		int iterations = 100000; iterations = 1000;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = skf.generateSecret(spec);
        key = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
        byte[] encrypted = c.doFinal(plaintext);
		return encrypted;
	}
	
	// decode a byte array using 256-bit AES
	public static byte[] decryptAES(byte[] encrypted, String password) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		byte[] salt = {1,2,3,4,5,6,7,8,9,0};
		int iterations = 100000; iterations = 1000;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = skf.generateSecret(spec);
        key = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher d = Cipher.getInstance("AES/CBC/PKCS5Padding");
        d.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));
        byte[] decrypted = d.doFinal(encrypted);
        return decrypted;
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
	public static Key getVotePublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		if (votePublicKey == null) {
			getStaticVoteKeys();
			//generateNewVoteKeys();
		}
		return votePublicKey;
	}
	
	public static Key getVotePrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		if (votePrivateKey == null) {
			getStaticVoteKeys();
			//generateNewVoteKeys();
		}
		return votePrivateKey;
	}
	
	// generate random keys. not useful except for testing
	static void generateNewVoteKeys() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(4096);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		votePrivateKey = keyPair.getPrivate();
		votePublicKey = keyPair.getPublic();
	}
	
	// we don't want these hard-coded keys in the final system!
	static void getStaticVoteKeys() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String encodedPublicKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAt43OaPV1Md2zLwZCW6WxUo1QPBaiJVr4bDUsIXoIAufDwHRpxphwI3oJOmX9D6t27B+/f+wWRwVLXY5QXeioEWAoF26u86YZHpusKywkKfC3XtdhPRVLXxYiP71TnrjXn7O9JSzk5F4/vz+qxCIfH569Ggft8HihJV7rVxDDmEJadUf0TMetDU8uPmeLBlkKSiIFJzauyfNOQsTc34eCln6J/pJcEsTUjjQ0NLnPByz1HTzUC/ah15y+Qe8MzdtzwG8n7H+vFWFdGB9hyUcj2VnP81vUZH0lhIIqTBMTsyW45xgsXlmLcAr7tJBE6Roko1nJlM7VlKQTOraNJprv/NirhLQqwBY3YE20cr0tQOPDo8168rNg8jc6sUFwAS+PV9EYf1ajD6jgTpapwIZ9rBuo0uh2M8aDzVfKi5kPBBOkFVoki/mjHNKwDfYa+93onaWkG7UWQbVlKoC8BTJ8/TW+vcfkosdjRtZ+HqJ1EfZKiZxtFA5SF6SryY2Hu2TVD3wNDZSXX5FncNoPr2j8LV4kFlzQBdlXfQ5bT4BEkB/cswtCVRWbJMUx2Eqlg/ucRbZ7Gz8xgmt446CdkP42r/uwuqF6w95nY3uNlZu51Wqd0U6IJTdWoOxKGTwZIIAK5KM05d8PXqnjMtPWe83GHy7aVFvApxj1kBaA8IKOvEcCAwEAAQ==";
		String encodedPrivateKey = "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"; 
		X509EncodedKeySpec specPub = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedPublicKey));
		KeyFactory kfPub = KeyFactory.getInstance("RSA");
	    votePublicKey = kfPub.generatePublic(specPub);
	    PKCS8EncodedKeySpec specPriv = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedPrivateKey));
	    KeyFactory kfPriv = KeyFactory.getInstance("RSA");
	    votePrivateKey =  kfPriv.generatePrivate(specPriv);
	}
	
	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for(byte b: a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}
	
	public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String password = "test";
        String plaintext = "This is the plaintext";
        byte[] enc = CryptoEngine.encryptAES(plaintext.getBytes(), password);
        String dec = new String(CryptoEngine.decryptAES(enc, password));
        System.out.println(plaintext + "  " + dec);
	}
	
}
