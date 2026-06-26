package org.springblade.test;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.util.encoders.Hex;
import org.springblade.core.tool.utils.SM2Util;
import org.springblade.core.tool.utils.StringPool;

/**
 * signKey generator
 *
 * @author Chill
 */
public class Sm2KeyGenerator {

	public static void main(String[] args) {
		System.out.println("================ blade.auth configuration is as follows =================");
		AsymmetricCipherKeyPair keyPair = SM2Util.generateKeyPair();
		String publicKey = SM2Util.getPublicKeyString(keyPair);
		String privateKey = SM2Util.getPrivateKeyString(keyPair);
		System.out.println("#blade configuration \n" +
			"blade:\n" +
			"  auth:\n" +
			"    public-key: " + publicKey + "\n" +
			"    private-key: " + privateKey);
		System.out.println("=======================================================");
		System.out.println(StringPool.EMPTY);
		System.out.println("============== saber website.js configuration is as follows ===============");
		System.out.println("//saber configuration\n" +
			"auth: {\n" +
			"  publicKey: '" + publicKey + "',\n" +
			"}");
		System.out.println("=======================================================");
		System.out.println(StringPool.EMPTY);
		System.out.println("============== Password:[admin] encryption workflow is as follows ================");
		String password = "admin";
		byte[] encryptedData = SM2Util.encrypt(password, publicKey);
		String decryptedText = SM2Util.decrypt(encryptedData, privateKey);
		System.out.println("Before encryption:" + password);
		System.out.println("After encryption:" + Hex.toHexString(encryptedData));
		System.out.println("Decrypted:" + decryptedText);
		System.out.println("Please note: This ciphertext is the password parameter for calling the token interface after frontend encryption");
		System.out.println("=======================================================");

	}

}
