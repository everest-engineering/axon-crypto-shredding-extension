package engineering.everest.axon.cryptoshredding.testevents;

import engineering.everest.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

@Revision("0")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventWithCustomTypeForKeyIdentifier {

    public static EventWithCustomTypeForKeyIdentifier createTestInstance() {
        return new EventWithCustomTypeForKeyIdentifier(new CustomType("key-identifier"), "encrypted field");
    }

    @EncryptionKeyIdentifier
    private CustomType aCustomTypeUsedAsKeyIdentifier;
    @EncryptedField
    private String aStringField;
}
