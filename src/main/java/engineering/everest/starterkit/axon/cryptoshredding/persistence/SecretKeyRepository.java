package engineering.everest.starterkit.axon.cryptoshredding.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.crypto.SecretKey;

@Repository
public interface SecretKeyRepository extends JpaRepository<PersistableEncryptionKey, String> {

    default SecretKey create(String secretKeyIdentifier, SecretKey key) {
        save(new PersistableEncryptionKey(secretKeyIdentifier, key));
        return key;
    }
}
