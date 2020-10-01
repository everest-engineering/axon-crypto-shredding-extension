package engineering.everest.starterkit.axon.cryptoshredding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public
class TypeDifferentiatedSecretKeyId implements Serializable {
    private String keyId;
    private String keyType;
}
