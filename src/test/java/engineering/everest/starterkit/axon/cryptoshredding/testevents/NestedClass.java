package engineering.everest.starterkit.axon.cryptoshredding.testevents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NestedClass {
    private String stringInANestedClass;
    private Object someObject;
}
