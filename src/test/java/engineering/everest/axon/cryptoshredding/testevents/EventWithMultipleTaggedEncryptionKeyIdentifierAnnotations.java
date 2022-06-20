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
public class EventWithMultipleTaggedEncryptionKeyIdentifierAnnotations {

    public static EventWithMultipleTaggedEncryptionKeyIdentifierAnnotations createTestInstance() {
        return EventWithMultipleTaggedEncryptionKeyIdentifierAnnotations.builder()
            .keyIdentifier("key-identifier")
            .keyIdentifier2("key-identifier-2")
            .fieldForFirstKey("I am a string")
            .fieldForSecondKey("I'm not the other string")
            .build();
    }

    @EncryptionKeyIdentifier
    private String keyIdentifier;
    @EncryptionKeyIdentifier(tag = "secondKeyTag")
    private String keyIdentifier2;
    @EncryptedField
    private String fieldForFirstKey;
    @EncryptedField(tag = "secondKeyTag")
    private String fieldForSecondKey;
}
