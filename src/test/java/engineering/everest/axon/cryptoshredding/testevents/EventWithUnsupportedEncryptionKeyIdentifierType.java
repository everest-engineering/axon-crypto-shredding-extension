package engineering.everest.axon.cryptoshredding.testevents;

import engineering.everest.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.Data;

@Data
public class EventWithUnsupportedEncryptionKeyIdentifierType {
    @EncryptionKeyIdentifier
    private float keyIdentifier = 42.0f;
    @EncryptedField
    private String dummy = "dummy";
}
