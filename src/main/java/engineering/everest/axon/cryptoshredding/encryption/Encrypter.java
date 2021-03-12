package engineering.everest.axon.cryptoshredding.encryption;

import javax.crypto.SecretKey;

public interface Encrypter {
    byte[] encrypt(SecretKey secretKey, String cleartext);
}
