package engineering.everest.starterkit.axon.cryptoshredding;

import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.SecretKeyRepository;
import org.junit.jupiter.api.BeforeEach;
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
@ComponentScan(basePackages = "engineering.everest.starterkit.axon.cryptoshredding")
@EntityScan(basePackages = "engineering.everest.starterkit.axon.cryptoshredding.persistence")
@EnableJpaRepositories(basePackages = "engineering.everest.starterkit.axon.cryptoshredding.persistence")
@ContextConfiguration(classes = TestsJpaConfig.class)
class CryptoShreddingServiceIntegrationTest {

    private static final String SECRET_KEY_ID = "secret-key-id";

    @Autowired
    private SecretKeyRepository secretKeyRepository;
    @Autowired
    private CryptoShreddingService cryptoShreddingService;

    @BeforeEach
    void setUp() {

    }

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillCreateSecretKeyOnFirstGet() {
        var secretKey = cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);

        assertEquals("AES", secretKey.get().getAlgorithm());
    }

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillReturnEmptyOptional_WhenKeyHasBeenDeleted() {
        cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        cryptoShreddingService.deleteSecretKey(SECRET_KEY_ID);

        assertTrue(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID).isEmpty());
    }

    @Test
    void getExistingSecretKey_WillRetrievePreviouslyCreatedKey() {
        var secretKey1 = cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        var secretKey2 = cryptoShreddingService.getExistingSecretKey(SECRET_KEY_ID);

        assertEquals(secretKey1.get(), secretKey2.get());
    }

    @Test
    void getExistingSecretKey_WillFailWhenKeyNotCreated() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingService.getExistingSecretKey(SECRET_KEY_ID));
    }

    @Test
    void getExistingSecretKey_WillReturnEmptyOptional_WhenKeyPreviouslyCreatedAndDeleted() {
        cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        cryptoShreddingService.deleteSecretKey(SECRET_KEY_ID);

        assertEquals(Optional.empty(), cryptoShreddingService.getExistingSecretKey(SECRET_KEY_ID));
    }

    @Test
    void keysAreIdempotentlyDeleted() {
        cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID);
        cryptoShreddingService.deleteSecretKey(SECRET_KEY_ID);
        cryptoShreddingService.deleteSecretKey(SECRET_KEY_ID);

        assertTrue(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(SECRET_KEY_ID).isEmpty());
    }

    @Test
    void deletingANonExistentKey_WillFail() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingService.deleteSecretKey(SECRET_KEY_ID));
    }
}
