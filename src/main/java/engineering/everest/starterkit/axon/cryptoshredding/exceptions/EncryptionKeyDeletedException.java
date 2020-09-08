package engineering.everest.starterkit.axon.cryptoshredding.exceptions;

public class EncryptionKeyDeletedException extends RuntimeException {

    public EncryptionKeyDeletedException(String encryptionKeyIdentifier) {
        super(String.format(
                "An event with crypto shredding annotations is being serialized in cleartext because encryption key %s has been deleted.",
                encryptionKeyIdentifier));
    }
}
