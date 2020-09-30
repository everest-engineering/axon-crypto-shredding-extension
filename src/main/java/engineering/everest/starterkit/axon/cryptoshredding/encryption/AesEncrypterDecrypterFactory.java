package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AesEncrypterDecrypterFactory {

    private final SecureRandom secureRandom;

    public AesEncrypterDecrypterFactory() {
        this.secureRandom = new SecureRandom();
    }

    public AesEncrypter createEncrypter() {
        return new AesEncrypter(secureRandom);
    }

    public AesDecrypter createDecrypter() {
        return new AesDecrypter(secureRandom);
    }
}
