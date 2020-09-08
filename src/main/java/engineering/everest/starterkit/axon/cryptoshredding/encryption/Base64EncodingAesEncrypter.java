package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import java.util.Base64;

public class Base64EncodingAesEncrypter {

    public String encryptAndEncode(String cleartext) {
        return Base64.getEncoder().encodeToString(cleartext.getBytes()); // TODO
    }

    public String decryptBase64Encoded(String base64EncodedCipherText) {
        return new String(Base64.getDecoder().decode(base64EncodedCipherText)); // TODO
    }
}
