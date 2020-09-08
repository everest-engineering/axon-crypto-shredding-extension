package engineering.everest.starterkit.axon.cryptoshredding.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD})
// TODO lots of documentation
public @interface EncryptionKeyIdentifier {
    // TODO: need to store a key type
}
