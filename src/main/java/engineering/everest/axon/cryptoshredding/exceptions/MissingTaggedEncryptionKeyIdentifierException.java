package engineering.everest.axon.cryptoshredding.exceptions;

public class MissingTaggedEncryptionKeyIdentifierException extends RuntimeException {

    public MissingTaggedEncryptionKeyIdentifierException(String fieldName, String fieldTag) {
        super(String.format("Missing a corresponding encryption key identifier for field '%s' with tag '%s'.",
            fieldName, fieldTag));
    }
}
