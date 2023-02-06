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
public class EventWithCustomTypeAsEncryptedField {

    public static EventWithCustomTypeAsEncryptedField createTestInstance() {
        return new EventWithCustomTypeAsEncryptedField("key-identifier", new CustomType("original encrypted value"));
    }

    public static EventWithCustomTypeAsEncryptedField createCryptoShreddedUnencryptedTestInstance() {
        return new EventWithCustomTypeAsEncryptedField("key-identifier", new CustomType("custom default value"));
    }

    @EncryptionKeyIdentifier
    private String aCustomTypeUsedAsKeyIdentifier;

    @EncryptedField
    private CustomType aCustomTypeFieldWrappingAString;
}
