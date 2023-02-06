package engineering.everest.axon.cryptoshredding.serialization;

import engineering.everest.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Converts a field tagged with {@code @EncryptionKeyIdentifier} into a string for serialisation. <br>
 * Converters for custom types can be registered using the builder.
 */
public class KeyIdentifierToStringConverter {

    private final Map<Class<?>, Function<Object, String>> customKeyIdentifierStringConverters;

    public KeyIdentifierToStringConverter() {
        customKeyIdentifierStringConverters = new HashMap<>();
    }

    private KeyIdentifierToStringConverter(Builder builder) {
        this.customKeyIdentifierStringConverters = builder.customKeyIdentifierStringConverters;
    }

    public static KeyIdentifierToStringConverter.Builder builder() {
        return new KeyIdentifierToStringConverter.Builder();
    }

    public String convertToString(Object object) {
        if (object instanceof String || object instanceof UUID || object instanceof Long || object instanceof Integer) {
            return object.toString();
        }
        if (customKeyIdentifierStringConverters.containsKey(object.getClass())) {
            return customKeyIdentifierStringConverters.get(object.getClass()).apply(object);
        }
        throw new UnsupportedEncryptionKeyIdentifierTypeException(object.toString());
    }

    public static class Builder {
        private final Map<Class<?>, Function<Object, String>> customKeyIdentifierStringConverters;

        public Builder() {
            this.customKeyIdentifierStringConverters = new HashMap<>();
        }

        public Builder customConverter(Class clazz, Function<Object, String> conversionFunction) {
            customKeyIdentifierStringConverters.put(clazz, conversionFunction);
            return this;
        }

        public KeyIdentifierToStringConverter build() {
            return new KeyIdentifierToStringConverter(this);
        }
    }
}
