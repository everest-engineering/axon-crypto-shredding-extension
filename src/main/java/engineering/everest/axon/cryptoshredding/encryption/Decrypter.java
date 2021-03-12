package engineering.everest.axon.cryptoshredding.encryption;

import javax.crypto.SecretKey;

public interface Decrypter {
    String decrypt(SecretKey secretKey, byte[] ciphertext);
}
