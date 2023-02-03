package engineering.everest.axon.cryptoshredding.encryption;

import java.security.SecureRandom;

public class DefaultAesEncrypterDecrypterFactory implements EncrypterDecrypterFactory {

    private final SecureRandom secureRandom;

    public DefaultAesEncrypterDecrypterFactory() {
        this.secureRandom = new SecureRandom();
    }

    public Encrypter createEncrypter() {
        return new DefaultAesEncrypter(secureRandom);
    }

    public Decrypter createDecrypter() {
        return new DefaultAesDecrypter(secureRandom);
    }
}
