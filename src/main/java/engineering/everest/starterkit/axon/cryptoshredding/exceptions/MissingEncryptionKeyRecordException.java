package engineering.everest.starterkit.axon.cryptoshredding.exceptions;

public class MissingEncryptionKeyRecordException extends RuntimeException {

    public MissingEncryptionKeyRecordException(String encryptionKeyIdentifier) {
        super(String.format("Missing record for encryption key %s of type %s", encryptionKeyIdentifier, "TODO"));
    }
}
