package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

class DefaultAesKeyGenerator implements engineering.everest.starterkit.axon.cryptoshredding.encryption.KeyGenerator {
    public static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private final SecureRandom securerandom;
    private final KeyGenerator keygenerator;

    public DefaultAesKeyGenerator() throws NoSuchAlgorithmException {
        this.securerandom = new SecureRandom();
        this.keygenerator = KeyGenerator.getInstance(ALGORITHM);
        this.keygenerator.init(KEY_SIZE, this.securerandom);
    }

    public SecretKey generateKey() {
        return keygenerator.generateKey();
    }
}
