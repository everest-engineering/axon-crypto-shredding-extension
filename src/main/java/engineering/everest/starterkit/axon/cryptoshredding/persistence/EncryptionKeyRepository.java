package engineering.everest.starterkit.axon.cryptoshredding.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EncryptionKeyRepository extends JpaRepository<PersistableEncryptionKey, String> {

    default String create(String encryptionKeyIdentifier, String key) {
        save(new PersistableEncryptionKey(encryptionKeyIdentifier, key));
        return key;
    }
}
