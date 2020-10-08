package engineering.everest.starterkit.axon.cryptoshredding.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that a field in an Axon message payload (e.g., an event)
 * contains sensitive information that must be encrypted.
 *
 * <p>A corresponding {@code @EncryptedKeyIdentifier} must be present to
 * identify the correct encryption key for the message.
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD})
public @interface EncryptedField {
    String keyType() default "";
}
