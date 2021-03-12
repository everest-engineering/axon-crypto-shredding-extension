package engineering.everest.axon.cryptoshredding.annotations;

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
 *
 * @see EncryptionKeyIdentifier
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD})
public @interface EncryptedField {
    /**
     * Returns the type of key. This value is optional if keys are guaranteed to be globally unique.
     *
     * @return a string that, when combined with a key identifier, uniquely identifies an encryption key
     */
    String keyType() default "";
}
