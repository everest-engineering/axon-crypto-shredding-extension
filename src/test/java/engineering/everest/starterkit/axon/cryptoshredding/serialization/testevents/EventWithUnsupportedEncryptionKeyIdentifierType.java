package engineering.everest.starterkit.axon.cryptoshredding.serialization.testevents;

import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.Data;

@Data
public class EventWithUnsupportedEncryptionKeyIdentifierType {
    @EncryptionKeyIdentifier
    private float keyIdentifier = 42.0f;
    @EncryptedField
    private String dummy = "dummy";
}