package engineering.everest.axon.cryptoshredding.persistence;

import engineering.everest.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "cryptoshreddingkeys")
public class PersistableSecretKey {
    @EmbeddedId
    private TypeDifferentiatedSecretKeyId id;
    @Lob
    private byte[] key;
    private String algorithm;
}
