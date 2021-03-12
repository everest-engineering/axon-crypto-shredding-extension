package engineering.everest.axon.cryptoshredding.exceptions;

public class UnsupportedEncryptionKeyIdentifierTypeException extends RuntimeException {

    public UnsupportedEncryptionKeyIdentifierTypeException(String identifierType) {
        super(String.format("Unsupported encryption key identifier type '%s'", identifierType));
    }
}
