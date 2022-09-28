package engineering.everest.axon.cryptoshredding.testevents;

import engineering.everest.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWithMultipleUntaggedEncryptionKeyIdentifierAnnotations {

    public static EventWithMultipleUntaggedEncryptionKeyIdentifierAnnotations createTestInstance() {
        return EventWithMultipleUntaggedEncryptionKeyIdentifierAnnotations.builder()
            .keyIdentifier("key-identifier")
            .keyIdentifier2("key-identifier-2")
            .fieldForFirstKey("I am a string")
            .fieldForSecondKey("I'm not the other string")
            .build();
    }

    @EncryptionKeyIdentifier
    private String keyIdentifier;
    @EncryptionKeyIdentifier
    private String keyIdentifier2;
    @EncryptedField
    private String fieldForFirstKey;
    @EncryptedField
    private String fieldForSecondKey;
}