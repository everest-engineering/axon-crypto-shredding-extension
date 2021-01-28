package engineering.everest.starterkit.axon.cryptoshredding.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks the field in an Axon message (e.g., event) that identifies the
 * encryption key used to protect sensitive fields.
 *
 * @see EncryptedField
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD})
public @interface EncryptionKeyIdentifier {
    /**
     * Returns the type of key. This value is optional if keys are guaranteed to be globally unique.
     *
     * @return a string that, when combined with a key identifier, uniquely identifies an encryption key
     */
    String keyType() default "";
}
