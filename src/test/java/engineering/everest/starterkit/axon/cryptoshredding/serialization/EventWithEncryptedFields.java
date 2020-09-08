package engineering.everest.starterkit.axon.cryptoshredding.serialization;

import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

import java.util.UUID;

@Revision("0")
@Data
@NoArgsConstructor
@AllArgsConstructor
class EventWithEncryptedFields {

    public static EventWithEncryptedFields createTestInstance() {
        return new EventWithEncryptedFields(
                "key-identifier",
                "Encrypted string",
                44,
                9601L,
                124.56789012345f,
                UUID.fromString("deadbeef-dead-beef-dead-beef00000007"),
                98765433L,
                65536,
                "I am an encrypted byte array".getBytes());
    }

    @EncryptionKeyIdentifier
    private String keyIdentifier;
    @EncryptedField
    private String aStringField;
    @EncryptedField
    private int aPrimitiveIntegerField;
    @EncryptedField
    private long aPrimitiveLongField;
    @EncryptedField
    private float aPrimitiveFloatField;
    @EncryptedField
    private UUID aUUIDField;
    @EncryptedField
    private Long aLongField;
    @EncryptedField
    private Integer anIntegerField;
    @EncryptedField
    private byte[] aByteArrayField;

    // TODO: try out Java time instance
}
