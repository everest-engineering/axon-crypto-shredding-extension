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

import static java.lang.System.arraycopy;
import static javax.crypto.Cipher.ENCRYPT_MODE;

class DefaultAesEncrypter implements Encrypter {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int INITIALIZATION_VECTOR_LENGTH_BYTES = 12;
    private static final int AUTHENTICATION_TAG_SIZE_BITS = 128;

    private final SecureRandom secureRandom;

    public DefaultAesEncrypter(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public byte[] encrypt(SecretKey secretKey, String cleartext) {
        try {
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            byte[] initializationVector = createInitializationVector();
            cipher.init(ENCRYPT_MODE, secretKey, new GCMParameterSpec(AUTHENTICATION_TAG_SIZE_BITS, initializationVector), secureRandom);
            return concatInitializationVectorAndCipherText(initializationVector, cipher.doFinal(cleartext.getBytes()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createInitializationVector() {
        byte[] initializationVector = new byte[INITIALIZATION_VECTOR_LENGTH_BYTES];
        secureRandom.nextBytes(initializationVector);
        return initializationVector;
    }

    private byte[] concatInitializationVectorAndCipherText(byte[] initializationVector, byte[] cipherText) {
        byte[] initializationVectorWithCipherText = new byte[initializationVector.length + cipherText.length];
        arraycopy(initializationVector, 0, initializationVectorWithCipherText, 0, INITIALIZATION_VECTOR_LENGTH_BYTES);
        arraycopy(cipherText, 0, initializationVectorWithCipherText, initializationVector.length, cipherText.length);
        return initializationVectorWithCipherText;
    }
}
