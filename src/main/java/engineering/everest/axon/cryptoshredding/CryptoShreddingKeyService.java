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

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

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
    @Transactional(propagation = NOT_SUPPORTED)
    public Optional<SecretKey> getOrCreateSecretKeyUnlessDeleted(TypeDifferentiatedSecretKeyId keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            LOGGER.trace("Creating crypto shredding key {}", keyId.toString());
            var secretKey = secretKeyGenerator.generateKey();
            secretKeyRepository.create(keyId, secretKey);
            return Optional.of(secretKey);
        }
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey.get());
    }

    /**
     * Check if a secret key exists
     *
     * @param  keyId that uniquely identifies the key
     * @return       true if the key was previously created <b>even if it has been deleted</b>, false if it was never created
     */
    public boolean secretKeyExists(TypeDifferentiatedSecretKeyId keyId) {
        return secretKeyRepository.findById(keyId).isPresent();
    }

    /**
     * Retrieve an existing secret key.
     *
     * @param  keyId that uniquely identifies the key
     * @return       an optional secret key which will be empty if the key previously existed but has been deleted
     */
    @Transactional(propagation = NOT_SUPPORTED)
    public Optional<SecretKey> getExistingSecretKey(TypeDifferentiatedSecretKeyId keyId) {
        var optionalPersistableSecretKey = secretKeyRepository.findById(keyId);
        if (optionalPersistableSecretKey.isEmpty()) {
            throw new MissingEncryptionKeyRecordException(keyId.getKeyId(), keyId.getKeyType());
        }
        LOGGER.trace("Retrieved crypto shredding key {}", keyId.toString());
        return createSecretKeyOrEmptyOptional(optionalPersistableSecretKey.get());
    }

    /**
     * Shred a secret key, rendering all fields protected by this key inaccessible.
     * <p>
     * <b>The encryption key table should not modified directly</b>.
     *
     * @param keyId that uniquely identifies the key
     */
    @Transactional(propagation = NOT_SUPPORTED)
    public void shredSecretKey(TypeDifferentiatedSecretKeyId keyId) {
        var optionalSecretKey = secretKeyRepository.findById(keyId);
        var secretKey = optionalSecretKey.orElseThrow(() -> new MissingEncryptionKeyRecordException(keyId.getKeyId(), keyId.getKeyType()));
        if (secretKey.getKey() != null || secretKey.getAlgorithm() != null) {
            secretKey.setAlgorithm(null);
            secretKey.setKey(null);
            secretKeyRepository.save(secretKey);
            LOGGER.trace("Permanently deleted crypto shredding key {}", keyId.toString());
        }
    }

    private Optional<SecretKey> createSecretKeyOrEmptyOptional(PersistableSecretKey persistableSecretKey) {
        if (persistableSecretKey.getAlgorithm() == null || persistableSecretKey.getKey() == null) {
            return Optional.empty();
        }
        return Optional.of(new SecretKeySpec(persistableSecretKey.getKey(), persistableSecretKey.getAlgorithm()));
    }
}
