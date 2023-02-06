package engineering.everest.axon.cryptoshredding.testevents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomType {
    private String wrappedId;
}
