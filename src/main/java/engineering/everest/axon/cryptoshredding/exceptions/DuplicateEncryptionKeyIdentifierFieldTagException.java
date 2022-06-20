package engineering.everest.axon.cryptoshredding.exceptions;

public class DuplicateEncryptionKeyIdentifierFieldTagException extends RuntimeException {

    public DuplicateEncryptionKeyIdentifierFieldTagException(String fieldName, String fieldTag) {
        super(String.format(
            "Duplicated field tag found for encryption key identifier annotation on '%s' with tag '%s'. "
                + " Use tags to distinguish between multiple encryption key identifiers.",
            fieldName, fieldTag));
    }
}
