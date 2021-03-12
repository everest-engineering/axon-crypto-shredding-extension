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
public class EventWithDifferentiatedKeyType {

    @EncryptionKeyIdentifier(keyType = "some-tag")
    private long aLongIdThatNeedsAdditionalType;
    @EncryptedField(keyType = "some-tag")
    private String aStringField;    
}
