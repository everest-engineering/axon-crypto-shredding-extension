package engineering.everest.starterkit.axon.cryptoshredding.exceptions;

public class MissingEncryptionKeyRecordException extends RuntimeException {

    public MissingEncryptionKeyRecordException(String secretKeyId, String keyType) {
        super(String.format("Missing record for encryption key %s of type `%s`", secretKeyId, keyType));
    }
}
