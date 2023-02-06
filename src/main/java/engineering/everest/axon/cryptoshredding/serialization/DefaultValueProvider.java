package engineering.everest.axon.cryptoshredding.serialization;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.FALSE;

/**
 * Provides default values for fields that can no longer be decrypted. <br>
 * Support for custom types can be registered using the builder.
 */
public class DefaultValueProvider {

    private static final Double DOUBLE_0 = 0d;
    private static final Float FLOAT_0 = 0f;

    private final Map<Class<?>, Object> customDefaultValueTypes;

    public DefaultValueProvider() {
        this.customDefaultValueTypes = new HashMap<>();
    }

    private DefaultValueProvider(Builder builder) {
        this.customDefaultValueTypes = builder.customDefaultValueTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T defaultValue(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Default value for null class doesn't make sense");
        }

        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) {
                return (T) FALSE;
            } else if (clazz == char.class) {
                return (T) Character.valueOf('\0');
            } else if (clazz == byte.class) {
                return (T) Byte.valueOf((byte) 0);
            } else if (clazz == short.class) {
                return (T) Short.valueOf((short) 0);
            } else if (clazz == int.class) {
                return (T) Integer.valueOf(0);
            } else if (clazz == long.class) {
                return (T) Long.valueOf(0L);
            } else if (clazz == float.class) {
                return (T) FLOAT_0;
            } else if (clazz == double.class) {
                return (T) DOUBLE_0;
            }
        }

        return (T) customDefaultValueTypes.get(clazz);
    }

    public static class Builder {
        private final Map<Class<?>, Object> customDefaultValueTypes;

        public Builder() {
            this.customDefaultValueTypes = new HashMap<>();
        }

        public <T> Builder customType(Class<T> clazz, Object defaultValue) {
            customDefaultValueTypes.put(clazz, defaultValue);
            return this;
        }

        public DefaultValueProvider build() {
            return new DefaultValueProvider(this);
        }
    }
}
