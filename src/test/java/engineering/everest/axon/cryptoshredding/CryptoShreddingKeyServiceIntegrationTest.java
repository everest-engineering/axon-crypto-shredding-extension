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

    private static final TypeDifferentiatedSecretKeyId SECRET_KEY_ID = new TypeDifferentiatedSecretKeyId("secret-key-id", "");

    @Autowired
    private SecretKeyRepository secretKeyRepository;
    @Autowired
    private CryptoShreddingKeyService cryptoShreddingKeyService;

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillCreateSecretKeyOnFirstGet() {
        var secretKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);

        assertEquals("AES", secretKey.get().getAlgorithm());
    }

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillReturnEmptyOptional_WhenKeyHasBeenDeleted() {
        cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        cryptoShreddingKeyService.deleteSecretKey(SECRET_KEY_ID);

        assertTrue(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID).isEmpty());
    }

    @Test
    void getExistingSecretKey_WillRetrievePreviouslyCreatedKey() {
        var secretKey1 = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        var secretKey2 = cryptoShreddingKeyService.getExistingSecretKey(SECRET_KEY_ID);

        assertEquals(secretKey1.get(), secretKey2.get());
    }

    @Test
    void getExistingSecretKey_WillFailWhenKeyNotCreated() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingKeyService.getExistingSecretKey(SECRET_KEY_ID));
    }

    @Test
    void getExistingSecretKey_WillReturnEmptyOptional_WhenKeyPreviouslyCreatedAndDeleted() {
        cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        cryptoShreddingKeyService.deleteSecretKey(SECRET_KEY_ID);

        assertEquals(Optional.empty(), cryptoShreddingKeyService.getExistingSecretKey(SECRET_KEY_ID));
    }

    @Test
    void keysAreIdempotentlyDeleted() {
        cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        cryptoShreddingKeyService.deleteSecretKey(SECRET_KEY_ID);
        cryptoShreddingKeyService.deleteSecretKey(SECRET_KEY_ID);

        assertTrue(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID).isEmpty());
    }

    @Test
    void deletingANonExistentKey_WillFail() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingKeyService.deleteSecretKey(SECRET_KEY_ID));
    }
}
