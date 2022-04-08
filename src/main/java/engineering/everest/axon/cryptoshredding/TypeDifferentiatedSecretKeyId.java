package engineering.everest.axon.cryptoshredding;

import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Secret key identifier that includes a {@code keyType} parameter. This is used to differentiate between identifiers when key uniqueness
 * cannot be globally guaranteed (such as when using monotonically increasing integers).
 *
 * @see EncryptionKeyIdentifier
 */
@Embeddable
public class TypeDifferentiatedSecretKeyId implements Serializable {
    private String keyId;
    private String keyType;

    public TypeDifferentiatedSecretKeyId() {}

    public TypeDifferentiatedSecretKeyId(String keyId, String keyType) {
        this.keyId = keyId;
        this.keyType = keyType;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeDifferentiatedSecretKeyId that = (TypeDifferentiatedSecretKeyId) o;
        return Objects.equals(keyId, that.keyId) && Objects.equals(keyType, that.keyType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyId, keyType);
    }
}
