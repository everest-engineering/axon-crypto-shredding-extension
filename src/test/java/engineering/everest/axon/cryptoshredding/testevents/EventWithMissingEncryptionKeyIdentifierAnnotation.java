package engineering.everest.axon.cryptoshredding.testevents;

import engineering.everest.axon.cryptoshredding.annotations.EncryptedField;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventWithMissingEncryptionKeyIdentifierAnnotation {

    @EncryptedField
    private String dummy = "dummy";
}
