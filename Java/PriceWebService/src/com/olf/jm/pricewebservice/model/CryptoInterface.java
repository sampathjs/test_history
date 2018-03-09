package com.olf.jm.pricewebservice.model;

/*
 * History:
 * 2015-11-02	V1.0	jwaechter	- initial version
 */

/**
 * This interface provides basic encryption and decryption functionalities.
 * @author jwaechter
 * @version 1.0
 */
public interface CryptoInterface {
	public abstract String encrypt(String data) throws CryptoException;
	public abstract String decrypt(String data) throws CryptoException;
}
