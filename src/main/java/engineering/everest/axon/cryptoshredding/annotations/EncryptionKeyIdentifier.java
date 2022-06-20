package engineering.everest.axon.cryptoshredding.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks the field in an Axon message (e.g., event) that identifies the encryption key used to protect sensitive fields.
 *
 * @see EncryptedField
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.FIELD })
public @interface EncryptionKeyIdentifier {
    /**
     * Returns the type of key. This value is optional if keys are guaranteed to be globally unique.
     *
     * @return a string that, when combined with a key identifier, uniquely identifies an encryption key
     */
    String keyType() default "";

    /**
     * Returns an arbitrary tag used to assign fields to their encryption keys. This value is optional if only a single key identifier is
     * present in a payload. The tag is used to assign an encryption key to one or more fields when multiple key identifiers exist for a
     * payload.
     *
     * @return a string that assigns the encryption key identifier to one or more fields in a payload.
     */
    String tag() default "";
}
