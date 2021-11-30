package engineering.everest.axon.cryptoshredding.exceptions;

public class EncryptionKeyDeletedException extends RuntimeException {

    public EncryptionKeyDeletedException(String secretKeyId, String keyType) {
        super(String.format(
            "An event with crypto shredding annotations is being serialized in cleartext because encryption key %s of type `%s` has been deleted.",
            secretKeyId, keyType));
    }
}
