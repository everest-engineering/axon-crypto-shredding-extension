package engineering.everest.axon.cryptoshredding.encryption;

public interface EncrypterDecrypterFactory {
    Encrypter createEncrypter();

    Decrypter createDecrypter();
}
