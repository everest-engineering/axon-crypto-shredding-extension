package engineering.everest.starterkit.axon.cryptoshredding.persistence;

import engineering.everest.starterkit.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.crypto.SecretKey;

@Repository
public interface SecretKeyRepository extends JpaRepository<PersistableSecretKey, TypeDifferentiatedSecretKeyId> {

    default SecretKey create(TypeDifferentiatedSecretKeyId keyId, SecretKey key) {
        save(new PersistableSecretKey(keyId, key.getEncoded(), key.getAlgorithm()));
        return key;
    }
}
