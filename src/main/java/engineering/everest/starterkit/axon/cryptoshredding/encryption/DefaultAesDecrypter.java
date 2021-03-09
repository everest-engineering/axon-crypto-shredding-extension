package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static javax.crypto.Cipher.DECRYPT_MODE;

class DefaultAesDecrypter implements Decrypter {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int INITIALIZATION_VECTOR_LENGTH_BYTES = 12;
    private static final int AUTHENTICATION_TAG_SIZE_BITS = 128;

    private final SecureRandom secureRandom;

    public DefaultAesDecrypter(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String decrypt(SecretKey secretKey, byte[] initializationVectorAndCipherText) {
        try {
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(DECRYPT_MODE, secretKey, new GCMParameterSpec(AUTHENTICATION_TAG_SIZE_BITS, initializationVectorAndCipherText, 0, INITIALIZATION_VECTOR_LENGTH_BYTES), secureRandom);
            return new String(cipher.doFinal(initializationVectorAndCipherText, INITIALIZATION_VECTOR_LENGTH_BYTES, initializationVectorAndCipherText.length - INITIALIZATION_VECTOR_LENGTH_BYTES));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
