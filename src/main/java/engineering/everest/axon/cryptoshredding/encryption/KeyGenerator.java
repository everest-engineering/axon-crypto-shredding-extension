package engineering.everest.axon.cryptoshredding.encryption;

import javax.crypto.SecretKey;

public interface KeyGenerator {
    SecretKey generateKey();
}
