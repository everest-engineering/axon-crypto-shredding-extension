package engineering.everest.starterkit.axon.cryptoshredding.serialization.testevents;

import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptedField;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventWithMissingEncryptionKeyIdentifierAnnotation {

    @EncryptedField
    private String dummy = "dummy";
}
