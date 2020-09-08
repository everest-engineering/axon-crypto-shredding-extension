package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Component
public class AesKeyGenerator {
    public static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private final SecureRandom securerandom;
    private final KeyGenerator keygenerator;

    public AesKeyGenerator(SecureRandom securerandom) throws NoSuchAlgorithmException {
        this.securerandom = securerandom;
        this.keygenerator = KeyGenerator.getInstance(ALGORITHM);
        this.keygenerator.init(KEY_SIZE, this.securerandom);
    }

    public SecretKey generateKey() {
        return keygenerator.generateKey();
    }
}
