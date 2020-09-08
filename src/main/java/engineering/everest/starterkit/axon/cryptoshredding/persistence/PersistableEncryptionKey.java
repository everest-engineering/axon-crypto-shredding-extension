package engineering.everest.starterkit.axon.cryptoshredding.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "event_encryption_keys")
public class PersistableEncryptionKey {
    @Id
    private String id;
    private SecretKey key;
}


