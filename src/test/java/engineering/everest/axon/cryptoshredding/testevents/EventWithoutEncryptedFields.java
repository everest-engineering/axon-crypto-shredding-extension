package engineering.everest.axon.cryptoshredding.testevents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

import java.util.UUID;

@Revision("0")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventWithoutEncryptedFields {

    public static EventWithoutEncryptedFields createTestInstance() {
        return new EventWithoutEncryptedFields(
                "Default string",
                42,
                9600L,
                123.456789012345f,
                UUID.fromString("deadbeef-dead-beef-dead-beef00000042"),
                98765432L,
                65535,
                "I am a byte array".getBytes());
    }

    private String aStringField;
    private int aPrimitiveIntegerField;
    private long aPrimitiveLongField;
    private float aPrimitiveFloatField;
    private UUID aUUIDField;
    private Long aLongField;
    private Integer anIntegerField;
    private byte[] aByteArrayField;
}
