package engineering.everest.axon.cryptoshredding.testevents;

import engineering.everest.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

import java.util.Map;
import java.util.UUID;

@Revision("0")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWithEncryptedFields {

    public static EventWithEncryptedFields createTestInstance() {
        return EventWithEncryptedFields.builder()
            .keyIdentifier("key-identifier")
            .aStringField("I am a string")
            .aPrimitiveIntegerField(44)
            .aPrimitiveLongField(9601L)
            .aPrimitiveFloatField(124.56789012345f)
            .aUUIDField(UUID.fromString("deadbeef-dead-beef-dead-beef00000007"))
            .aLongField(98765433L)
            .anIntegerField(65536)
            .encryptedNestedClass(new NestedClass("hey, it's me! - nested string!", "not a good idea to have untyped properties..."))
            .cleartextNestedClass(new NestedClass(null, Map.of("like", "bananas", "dislike", "lychees")))
            .build();
    }

    public static EventWithEncryptedFields createUnencryptedTestInstance() {
        return EventWithEncryptedFields.builder()
            .keyIdentifier("key-identifier")
            .cleartextNestedClass(new NestedClass(null, Map.of("like", "bananas", "dislike", "lychees")))
            .build();
    }

    @EncryptionKeyIdentifier
    private String keyIdentifier;
    @EncryptedField
    private String aStringField;
    @EncryptedField
    private String anEmptyStringField;
    @EncryptedField
    private String aNullStringField;
    @EncryptedField
    private int aPrimitiveIntegerField;
    @EncryptedField
    private long aPrimitiveLongField;
    @EncryptedField
    private float aPrimitiveFloatField;
    @EncryptedField
    private UUID aUUIDField;
    @EncryptedField
    private UUID aNullUUIDField;
    @EncryptedField
    private Long aLongField;
    @EncryptedField
    private Integer anIntegerField;
    @EncryptedField
    private NestedClass encryptedNestedClass;
    private NestedClass cleartextNestedClass;
}
