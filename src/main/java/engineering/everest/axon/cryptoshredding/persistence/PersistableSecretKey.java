package engineering.everest.axon.cryptoshredding.persistence;

import engineering.everest.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "cryptoshreddingkeys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersistableSecretKey {
    @EmbeddedId
    private TypeDifferentiatedSecretKeyId id;
    @Lob
    private byte[] key;
    private String algorithm;
}
