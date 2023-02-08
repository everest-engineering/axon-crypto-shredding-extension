package engineering.everest.axon.cryptoshredding.encryption;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class DefaultAesKeyGenerator implements engineering.everest.axon.cryptoshredding.encryption.KeyGenerator {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private final KeyGenerator keygenerator;

    public DefaultAesKeyGenerator() throws NoSuchAlgorithmException {
        this.keygenerator = KeyGenerator.getInstance(ALGORITHM);
        this.keygenerator.init(KEY_SIZE, new SecureRandom());
    }

    public SecretKey generateKey() {
        return keygenerator.generateKey();
    }
}
