package engineering.everest.starterkit.axon.cryptoshredding.encryption;

public interface EncrypterDecrypterFactory {
    Encrypter createEncrypter();

    Decrypter createDecrypter();
}
