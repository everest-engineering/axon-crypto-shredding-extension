package engineering.everest.axon.cryptoshredding;

import engineering.everest.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.axon.cryptoshredding.persistence.SecretKeyRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@AutoConfigureEmbeddedDatabase(refresh = AFTER_EACH_TEST_METHOD, type = POSTGRES)
@DataJpaTest
@EnableAutoConfiguration
@ComponentScan(basePackages = "engineering.everest.axon.cryptoshredding")
@ContextConfiguration(classes = { TestsJpaConfig.class })
@Execution(SAME_THREAD)
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
