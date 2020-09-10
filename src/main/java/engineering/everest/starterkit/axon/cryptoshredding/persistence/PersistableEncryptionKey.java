package engineering.everest.starterkit.axon.cryptoshredding.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "eventencryptionkeys")
public class PersistableEncryptionKey {
    @Id
    private String id;
    @Lob
    private byte[] key;
    private String algorithm;
}
