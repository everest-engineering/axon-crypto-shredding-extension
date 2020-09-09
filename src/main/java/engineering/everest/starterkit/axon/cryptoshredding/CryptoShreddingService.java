package engineering.everest.starterkit.axon.cryptoshredding;

import engineering.everest.starterkit.axon.cryptoshredding.encryption.AesKeyGenerator;
import engineering.everest.starterkit.axon.cryptoshredding.encryption.Base64EncodingAesEncrypter;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.SecretKeyRepository;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class CryptoShreddingService {

    private final SecretKeyRepository secretKeyRepository;
    private final AesKeyGenerator secretKeyGenerator;

    public CryptoShreddingService(SecretKeyRepository secretKeyRepository, AesKeyGenerator secretKeyGenerator) {
        this.secretKeyRepository = secretKeyRepository;
        this.secretKeyGenerator = secretKeyGenerator;
    }

    public Optional<SecretKey> getOrCreateSecretKeyUnlessDeleted(String keyId) {
        var optionalSecretKey = secretKeyRepository.findById(keyId);
        var secretKey = optionalSecretKey.isEmpty()
                ? secretKeyRepository.create(keyId, secretKeyGenerator.generateKey())
                : optionalSecretKey.get().getKey();
        return Optional.ofNullable(secretKey);
    }

    public Optional<SecretKey> getExistingSecretKey(String keyId) {
        var optionalPersistableEncryptionKey = secretKeyRepository.findById(keyId);
        var secretKey = optionalPersistableEncryptionKey
                .orElseThrow(() -> new MissingEncryptionKeyRecordException(keyId))
                .getKey();

        return Optional.ofNullable(secretKey);
    }

    public Base64EncodingAesEncrypter createEncrypter(SecretKey secretKey) {
        return new Base64EncodingAesEncrypter(); // TODO
    }

    public Base64EncodingAesEncrypter createDecrypter(SecretKey secretKey) {
        return new Base64EncodingAesEncrypter(); // TODO
    }

    public void deleteSecretKey(String keyType, String keyId) {
        // TODO
    }
}
