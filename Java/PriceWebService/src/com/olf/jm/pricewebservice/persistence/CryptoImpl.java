package com.olf.jm.pricewebservice.persistence;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.olf.jm.pricewebservice.model.CryptoException;
import com.olf.jm.pricewebservice.model.CryptoInterface;
import com.openlink.util.logging.PluginLog;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;


/*
 * History:
 * 2015-11-02	V1.0	jwaechter	- initial version
 */

/**
 * Implementation of the crypto interface to 
 * @author jwaechter
 *  */
public class CryptoImpl implements CryptoInterface {

	private static final char[] PASSWORD = "thisisapasswordusedinthedefaultclass0000".toCharArray();
	
    private static final byte[] SALT = { 
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, 
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, 
    }; 
    
	public String encrypt(String data) {
		
		SecretKeyFactory keyFactory;
		try {
			keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		} catch (NoSuchAlgorithmException e) {			
			String message = "Error creating the security key factory";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} 
		SecretKey key;
		try {
			key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
		} catch (InvalidKeySpecException e) {
			String message = "Error creating the security key";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} 
		
	    // get cipher object for password-based encryption
	    Cipher pbeCipher;
		try {
			pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		} catch (NoSuchAlgorithmException e) {
			String message = "Error creating cipher no such algorithm";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} catch (NoSuchPaddingException e) {
			String message = "Error creating cipher";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		}
	    
	    // initialize cipher for encryption, without supplying
	    // any parameters. Here, "myKey" is assumed to refer 
	    // to an already-generated key.
	    try {
			pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 24));
		} catch (InvalidKeyException e1) {
			String message = "Error initialsing cipher invalid key";
			PluginLog.error(message);
			throw new CryptoException(message, e1);
		} catch (InvalidAlgorithmParameterException e1) {
			String message = "Error initialising cipher invalid algorithm";
			PluginLog.error(message);
			throw new CryptoException(message, e1);
		} 
        	    
	    // encrypt some data and store away ciphertext
	    // for later decryption
	    byte[] cipherText;
		try {
			cipherText = pbeCipher.doFinal(data.getBytes());
		} catch (IllegalBlockSizeException e) {
			String message = "Error encrypting data (Block size)";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} catch (BadPaddingException e) {
			String message = "Error encrypting data (Padding)";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		}
	    
		return new sun.misc.BASE64Encoder().encode(cipherText);
	}

	public String decrypt(String data) {
		
		byte[] dec;
		
		try {
			dec = new sun.misc.BASE64Decoder().decodeBuffer(data);
		} catch (IOException e) {
			String message = "IOexception decoding input buffer (Not a Base64 input string?)";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		}
        SecretKeyFactory keyFactory;
		try {
			keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		} catch (NoSuchAlgorithmException e) {
			String message = "Could not find decoding algorithm";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} 
        SecretKey key;
		try {
			key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
		} catch (InvalidKeySpecException e) {
			String message = "Invalid Key Spec";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} 
        Cipher pbeCipher;
		try {
			pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		} catch (NoSuchAlgorithmException e) {
			String message = "Decoding Algorithm not found";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} catch (NoSuchPaddingException e) {
			String message = "Could not find decoding padding";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} 
        try {
			pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 24));
		} catch (InvalidKeyException e) {
			String message = "Invalid key spec";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} catch (InvalidAlgorithmParameterException e) {
			String message = "Invalid algorithm parameter";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} 
		
		String password;
		try {
			password = new String(pbeCipher.doFinal(dec));
		} catch (IllegalBlockSizeException e) {
			String message = "Error decrypting password: illegal block size";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		} catch (BadPaddingException e) {
			String message = "Bad Padding while decrypting password";
			PluginLog.error(message);
			throw new CryptoException(message, e);
		}
        return password;
	}
}
