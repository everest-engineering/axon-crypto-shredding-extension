package engineering.everest.axon.cryptoshredding;

import engineering.everest.axon.cryptoshredding.encryption.KeyGenerator;
import engineering.everest.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.axon.cryptoshredding.persistence.PersistableSecretKey;
import engineering.everest.axon.cryptoshredding.persistence.SecretKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoShreddingKeyServiceTest {

    @Mock
    private SecretKeyRepository secretKeyRepository;

    @Mock
    private KeyGenerator secretKeyGenerator;

    private CryptoShreddingKeyService cryptoShreddingKeyService;

    @BeforeEach
    void setup() {
        cryptoShreddingKeyService = new CryptoShreddingKeyService(secretKeyRepository, secretKeyGenerator);
    }

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillCreateSecretKeyOnFirstGet() {
        var keyId = generateKeyId();
        var expectedKey = mock(SecretKey.class);
        when(secretKeyRepository.findById(keyId)).thenReturn(Optional.empty());
        when(secretKeyGenerator.generateKey()).thenReturn(expectedKey);

        var actualKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);

        verify(secretKeyRepository).create(keyId, expectedKey);
        assertTrue(actualKey.isPresent());
        assertEquals(expectedKey, actualKey.get());
    }

    @Test
    void getExistingSecretKey_WillRetrievePreviouslyCreatedKey() {
        var keyId = generateKeyId();
        var existingKey = new PersistableSecretKey(keyId, "test key".getBytes(), "test algo");
        when(secretKeyRepository.findById(keyId)).thenReturn(Optional.of(existingKey));

        var actualKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);

        verify(secretKeyRepository, never()).create(any(), any());
        assertTrue(actualKey.isPresent());
        assertArrayEquals(existingKey.getKey(), actualKey.get().getEncoded());
        assertEquals(existingKey.getAlgorithm(), actualKey.get().getAlgorithm());
    }

    @Test
    void getOrCreateSecretKeyUnlessDeleted_WillReturnEmptyOptional_WhenKeyHasBeenDeleted() {
        var keyId = generateKeyId();
        var existingKey = new PersistableSecretKey(keyId, null, null);
        when(secretKeyRepository.findById(keyId)).thenReturn(Optional.of(existingKey));

        var actualKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(keyId);

        verify(secretKeyRepository, never()).create(any(), any());
        assertFalse(actualKey.isPresent());
    }

    @Test
    void getExistingSecretKey_WillFailWhenKeyNotCreated() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingKeyService.getExistingSecretKey(generateKeyId()));
    }

    @Test
    void secretKeyExists_WillReturnTrueIfKeyExists() {
        TypeDifferentiatedSecretKeyId keyId = generateKeyId();
        when(secretKeyRepository.findById(keyId))
            .thenReturn(Optional.of(mock(PersistableSecretKey.class)));

        assertTrue(cryptoShreddingKeyService.secretKeyExists(keyId));
    }

    @Test
    void secretKeyExists_WillReturnFalseIfKeyNeverCreated() {
        assertFalse(cryptoShreddingKeyService.secretKeyExists(generateKeyId()));
    }

    @Test
    void secretKeyExists_WillReturnTrueIfKeyCreated() {
        var keyId = generateKeyId();
        when(secretKeyRepository.findById(keyId)).thenReturn(Optional.of(mock(PersistableSecretKey.class)));

        assertTrue(cryptoShreddingKeyService.secretKeyExists(keyId));
    }

    @Test
    void deletingANonExistentKey_WillFail() {
        assertThrows(MissingEncryptionKeyRecordException.class, () -> cryptoShreddingKeyService.shredSecretKey(
            new TypeDifferentiatedSecretKeyId("does not exist", "")));
    }

    @Test
    void shredSecretKeyNullifiesKeyAndAlgorithm() {
        var keyId = generateKeyId();
        var existingKey = new PersistableSecretKey(keyId, "test key".getBytes(), "test algo");
        when(secretKeyRepository.findById(keyId)).thenReturn(Optional.of(existingKey));

        cryptoShreddingKeyService.shredSecretKey(keyId);

        assertNull(existingKey.getAlgorithm());
        assertNull(existingKey.getKey());
        verify(secretKeyRepository).save(existingKey);
    }

    private TypeDifferentiatedSecretKeyId generateKeyId() {
        return new TypeDifferentiatedSecretKeyId(randomUUID().toString(), "");
    }
}
