package engineering.everest.starterkit.axon.cryptoshredding;

import engineering.everest.starterkit.axon.cryptoshredding.encryption.KeyGenerator;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.PersistableSecretKey;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.SecretKeyRepository;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Optional;

/**
 * Service level cryptographic key management.
 */
@Component
public class CryptoShreddingKeyService {
    private static final String DEFAULT_KEY_TYPE = "";

    private final SecretKeyRepository secretKeyRepository;
    private final KeyGenerator secretKeyGenerator;

    public CryptoShreddingKeyService(SecretKeyRepository secretKeyRepository, KeyGenerator secretKeyGenerator) {
        this.secretKeyRepository = secretKeyRepository;
        this.secretKeyGenerator = secretKeyGenerator;
    }

    /**
     * Retrieve a secret key, generating it on first access unless explicitly discarded.
     *
     * @param keyId that uniquely identifies the key
     * @return an optional secret key that will be missing only if the key was deleted.
     */
    public Optional<SecretKey> getOrCreateSecretKeyUnlessDeleted(TypeDifferentiatedSecretKeyId keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            return Optional.of(secretKeyRepository.create(keyId, secretKeyGenerator.generateKey()));
        }
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey);
    }

    /**
     * Retrieve an existing secret key.
     *
     * @param keyId that uniquely identifies the key
     * @return an optional secret key
     */
    public Optional<SecretKey> getExistingSecretKey(TypeDifferentiatedSecretKeyId keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            throw new MissingEncryptionKeyRecordException(keyId.getKeyId(), keyId.getKeyType());
        }
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey);
    }

    /**
     * Delete a secret key, rendering all fields protected by this key inaccessible.
     * <p>
     * <b>The encryption key table should not modified directly</b>.
     *
     * @param keyId that uniquely identifies the key
     */
    public void deleteSecretKey(TypeDifferentiatedSecretKeyId keyId) {
        var optionalSecretKey = secretKeyRepository.findById(keyId);
        var secretKey = optionalSecretKey.orElseThrow(() -> new MissingEncryptionKeyRecordException(keyId.getKeyId(), keyId.getKeyType()));
        if (secretKey.getKey() != null || secretKey.getAlgorithm() != null) {
            secretKey.setAlgorithm(null);
            secretKey.setKey(null);
            secretKeyRepository.save(secretKey);
        }
    }

    private Optional<SecretKey> createSecretKeyOrEmptyOptional(Optional<PersistableSecretKey> optionalPersistableSecretKey) {
        var persistableSecretKey = optionalPersistableSecretKey.orElseThrow();
        if (persistableSecretKey.getAlgorithm() == null || persistableSecretKey.getKey() == null) {
            return Optional.empty();
        }
        return Optional.of(new SecretKeySpec(persistableSecretKey.getKey(), persistableSecretKey.getAlgorithm()));
    }
}
