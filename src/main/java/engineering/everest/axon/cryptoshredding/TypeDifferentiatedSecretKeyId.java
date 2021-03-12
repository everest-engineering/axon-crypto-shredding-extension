package engineering.everest.axon.cryptoshredding;

import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Secret key identifier that includes a {@code keyType} parameter. This is used to differentiate between identifiers
 * when key uniqueness cannot be globally guaranteed (such as when using monotonically increasing integers).
 *
 * @see EncryptionKeyIdentifier
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public
class TypeDifferentiatedSecretKeyId implements Serializable {
    private String keyId;
    private String keyType;
}
