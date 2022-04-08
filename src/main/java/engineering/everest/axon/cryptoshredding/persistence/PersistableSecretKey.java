package engineering.everest.axon.cryptoshredding.persistence;

import engineering.everest.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import org.hibernate.Hibernate;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import java.util.Objects;

@Entity(name = "cryptoshreddingkeys")
public class PersistableSecretKey {
    @EmbeddedId
    private TypeDifferentiatedSecretKeyId id;
    @Lob
    private byte[] key;
    private String algorithm;

    public PersistableSecretKey() {}

    public PersistableSecretKey(TypeDifferentiatedSecretKeyId id, byte[] key, String algorithm) {
        this.id = id;
        this.key = key;
        this.algorithm = algorithm;
    }

    public TypeDifferentiatedSecretKeyId getId() {
        return id;
    }

    public void setId(TypeDifferentiatedSecretKeyId id) {
        this.id = id;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        PersistableSecretKey that = (PersistableSecretKey) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
