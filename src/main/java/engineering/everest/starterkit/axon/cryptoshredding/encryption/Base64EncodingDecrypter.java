package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import static javax.crypto.Cipher.DECRYPT_MODE;

public class Base64EncodingDecrypter {
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private static final int INITIALIZATION_VECTOR_LENGTH = 16;

    private final SecureRandom secureRandom;

    public Base64EncodingDecrypter(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String decryptBase64Encoded(SecretKey secretKey, String base64EncodedCipherText) {
        try {
            // TODO this doesn't look reentrant
            var initializationVectorAndCipherText = Base64.getDecoder().decode(base64EncodedCipherText);
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(DECRYPT_MODE, secretKey, new IvParameterSpec(initializationVectorAndCipherText, 0, INITIALIZATION_VECTOR_LENGTH), secureRandom);
            return new String(cipher.doFinal(initializationVectorAndCipherText, INITIALIZATION_VECTOR_LENGTH, initializationVectorAndCipherText.length - INITIALIZATION_VECTOR_LENGTH));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
