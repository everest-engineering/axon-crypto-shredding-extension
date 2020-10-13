package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import javax.crypto.SecretKey;

public interface KeyGenerator {
    SecretKey generateKey();
}
