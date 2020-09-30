package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class EncrypterDecrypterFactory {

    private final SecureRandom secureRandom;

    public EncrypterDecrypterFactory() {
        this.secureRandom = new SecureRandom();
    }

    public Base64EncodingEncrypter createEncrypter() {
        return new Base64EncodingEncrypter(secureRandom);
    }

    public Base64EncodingDecrypter createDecrypter() {
        return new Base64EncodingDecrypter(secureRandom);
    }
}
