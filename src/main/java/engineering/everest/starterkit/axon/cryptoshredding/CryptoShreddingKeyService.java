package engineering.everest.starterkit.axon.cryptoshredding;

import engineering.everest.starterkit.axon.cryptoshredding.encryption.AesKeyGenerator;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.PersistableEncryptionKey;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.SecretKeyRepository;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Optional;

@Component
public class CryptoShreddingKeyService {

    private final SecretKeyRepository secretKeyRepository;
    private final AesKeyGenerator secretKeyGenerator;

    public CryptoShreddingKeyService(SecretKeyRepository secretKeyRepository, AesKeyGenerator secretKeyGenerator) {
        this.secretKeyRepository = secretKeyRepository;
        this.secretKeyGenerator = secretKeyGenerator;
    }

    public Optional<SecretKey> getOrCreateSecretKeyUnlessDeleted(String keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            return Optional.of(secretKeyRepository.create(keyId, secretKeyGenerator.generateKey()));
        }
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey);
    }

    public Optional<SecretKey> getExistingSecretKey(String keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            throw new MissingEncryptionKeyRecordException(keyId);
        }
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey);
    }

    public void deleteSecretKey(String keyId) {
        var optionalSecretKey = secretKeyRepository.findById(keyId);
        var secretKey = optionalSecretKey.orElseThrow(() -> new MissingEncryptionKeyRecordException(keyId));
        if (secretKey.getKey() != null || secretKey.getAlgorithm() != null) {
            secretKey.setAlgorithm(null);
            secretKey.setKey(null);
            secretKeyRepository.save(secretKey);
        }
    }

    private Optional<SecretKey> createSecretKeyOrEmptyOptional(Optional<PersistableEncryptionKey> optionalPersistableSecretKey) {
        var persistableSecretKey = optionalPersistableSecretKey.get();
        if (persistableSecretKey.getAlgorithm() == null || persistableSecretKey.getKey() == null) {
            return Optional.empty();
        }
        return Optional.of(new SecretKeySpec(persistableSecretKey.getKey(), persistableSecretKey.getAlgorithm()));
    }
}
