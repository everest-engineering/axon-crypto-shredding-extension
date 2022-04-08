package engineering.everest.axon.cryptoshredding;

import engineering.everest.axon.cryptoshredding.encryption.KeyGenerator;
import engineering.everest.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.axon.cryptoshredding.persistence.PersistableSecretKey;
import engineering.everest.axon.cryptoshredding.persistence.SecretKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Optional;

import static org.springframework.transaction.annotation.Isolation.READ_UNCOMMITTED;

/**
 * Service level cryptographic key management.
 */
@Component
@Slf4j
public class CryptoShreddingKeyService {

    private final SecretKeyRepository secretKeyRepository;
    private final KeyGenerator secretKeyGenerator;

    public CryptoShreddingKeyService(SecretKeyRepository secretKeyRepository, KeyGenerator secretKeyGenerator) {
        this.secretKeyRepository = secretKeyRepository;
        this.secretKeyGenerator = secretKeyGenerator;
    }

    /**
     * Retrieve a secret key, generating it on first access unless explicitly discarded.
     *
     * @param  keyId that uniquely identifies the key
     * @return       an optional secret key that will be missing only if the key was deleted.
     */
    public Optional<SecretKey> getOrCreateSecretKeyUnlessDeleted(TypeDifferentiatedSecretKeyId keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            LOGGER.trace("Creating crypto shredding key {}", keyId);
            return Optional.of(secretKeyRepository.create(keyId, secretKeyGenerator.generateKey()));
        }
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey.get());
    }

    /**
     * Retrieve an existing secret key.
     *
     * @param  keyId that uniquely identifies the key
     * @return       an optional secret key
     */
    @Transactional(isolation = READ_UNCOMMITTED)
    public Optional<SecretKey> getExistingSecretKey(TypeDifferentiatedSecretKeyId keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            throw new MissingEncryptionKeyRecordException(keyId.getKeyId(), keyId.getKeyType());
        }
        LOGGER.trace("Retrieved crypto shredding key {}", keyId);
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey.get());
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
            LOGGER.trace("Permanently deleted crypto shredding key {}", keyId);
        }
    }

    private Optional<SecretKey> createSecretKeyOrEmptyOptional(PersistableSecretKey persistableSecretKey) {
        if (persistableSecretKey.getAlgorithm() == null || persistableSecretKey.getKey() == null) {
            return Optional.empty();
        }
        return Optional.of(new SecretKeySpec(persistableSecretKey.getKey(), persistableSecretKey.getAlgorithm()));
    }
}
