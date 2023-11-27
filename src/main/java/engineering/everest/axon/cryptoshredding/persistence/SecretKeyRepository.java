package engineering.everest.axon.cryptoshredding.persistence;

import engineering.everest.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;

import javax.crypto.SecretKey;
import java.util.Optional;

public interface SecretKeyRepository {
    PersistableSecretKey create(TypeDifferentiatedSecretKeyId keyId, SecretKey key);

    Optional<PersistableSecretKey> findById(TypeDifferentiatedSecretKeyId keyId);

    PersistableSecretKey save(PersistableSecretKey key);
}
