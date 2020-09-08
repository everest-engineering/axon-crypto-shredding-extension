package engineering.everest.starterkit.axon.cryptoshredding.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.crypto.SecretKey;

@Repository
public interface EncryptionKeyRepository extends JpaRepository<PersistableEncryptionKey, String> {

    default SecretKey create(String encryptionKeyIdentifier, SecretKey key) {
        save(new PersistableEncryptionKey(encryptionKeyIdentifier, key));
        return key;
    }
}
