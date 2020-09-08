package engineering.everest.starterkit.axon.cryptoshredding;

import engineering.everest.starterkit.axon.cryptoshredding.encryption.AesKeyGenerator;
import engineering.everest.starterkit.axon.cryptoshredding.encryption.Base64EncodingAesEncrypter;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.EncryptionKeyRepository;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class CryptoShreddingService {

    private final EncryptionKeyRepository encryptionKeyRepository;
    private final AesKeyGenerator encryptionKeyGenerator;

    public CryptoShreddingService(EncryptionKeyRepository encryptionKeyRepository, AesKeyGenerator encryptionKeyGenerator) {
        this.encryptionKeyRepository = encryptionKeyRepository;
        this.encryptionKeyGenerator = encryptionKeyGenerator;
    }

    public Optional<SecretKey> getOrCreateSecretKeyUnlessDeleted(String keyId) {
        var optionalEncryptionKey = encryptionKeyRepository.findById(keyId);
        var encryptionKey = optionalEncryptionKey.isEmpty()
                ? encryptionKeyRepository.create(keyId, encryptionKeyGenerator.generateKey())
                : optionalEncryptionKey.get().getKey();
        return Optional.ofNullable(encryptionKey);
    }

    public Optional<SecretKey> getExistingSecretKey(String keyId) {
        var optionalPersistableEncryptionKey = encryptionKeyRepository.findById(keyId);
        var encryptionKey = optionalPersistableEncryptionKey
                .orElseThrow(() -> new MissingEncryptionKeyRecordException(keyId))
                .getKey();

        return Optional.ofNullable(encryptionKey);
    }

    public Base64EncodingAesEncrypter createEncrypter(SecretKey encryptionKey) {
        return new Base64EncodingAesEncrypter(); // TODO
    }

    public Base64EncodingAesEncrypter createDecrypter(SecretKey encryptionKey) {
        return new Base64EncodingAesEncrypter(); // TODO
    }

    public void deleteEncryptionKey(String keyType, String keyId) {
        // TODO
    }
}
