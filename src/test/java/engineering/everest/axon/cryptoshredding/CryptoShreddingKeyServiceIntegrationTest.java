package engineering.everest.axon.cryptoshredding;

import engineering.everest.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.axon.cryptoshredding.persistence.SecretKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ComponentScan(basePackages = "engineering.everest.axon.cryptoshredding")
@EntityScan(basePackages = "engineering.everest.axon.cryptoshredding.persistence")
@EnableJpaRepositories(basePackages = "engineering.everest.axon.cryptoshredding.persistence")
@ContextConfiguration(classes = TestsJpaConfig.class)
class CryptoShreddingKeyServiceIntegrationTest {

    @Autowired
    private SecretKeyRepository secretKeyRepository;
    @Autowired
    private CryptoShreddingKeyService cryptoShreddingKeyService;

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillCreateSecretKeyOnFirstGet() {
        var secretKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(generateKeyId());

        assertEquals("AES", secretKey.orElseThrow().getAlgorithm());
    }

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillReturnEmptyOptional_WhenKeyHasBeenDeleted() {
        var keyId = generateKeyId();
        cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);
        cryptoShreddingKeyService.deleteSecretKey(keyId);

        assertTrue(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId).isEmpty());
    }

    @Test
    void getExistingSecretKey_WillRetrievePreviouslyCreatedKey() {
        var keyId = generateKeyId();
        var secretKey1 = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);
        var secretKey2 = cryptoShreddingKeyService.getExistingSecretKey(keyId);

        assertEquals(secretKey1.orElseThrow(), secretKey2.orElseThrow());
    }

    @Test
    void getExistingSecretKey_WillFailWhenKeyNotCreated() {

        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingKeyService.getExistingSecretKey(generateKeyId()));
    }

    @Test
    void getExistingSecretKey_WillReturnEmptyOptional_WhenKeyPreviouslyCreatedAndDeleted() {
        var keyId = generateKeyId();
        cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);
        cryptoShreddingKeyService.deleteSecretKey(keyId);

        assertEquals(Optional.empty(), cryptoShreddingKeyService.getExistingSecretKey(keyId));
    }

    @Test
    void keysAreIdempotentlyDeleted() {
        var keyId = generateKeyId();
        cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);
        cryptoShreddingKeyService.deleteSecretKey(keyId);
        cryptoShreddingKeyService.deleteSecretKey(keyId);

        assertTrue(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId).isEmpty());
    }

    @Test
    void deletingANonExistentKey_WillFail() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingKeyService.deleteSecretKey(
            new TypeDifferentiatedSecretKeyId("does not exist", "")));
    }

    private TypeDifferentiatedSecretKeyId generateKeyId() {
        return new TypeDifferentiatedSecretKeyId(randomUUID().toString(), "");
    }
}
