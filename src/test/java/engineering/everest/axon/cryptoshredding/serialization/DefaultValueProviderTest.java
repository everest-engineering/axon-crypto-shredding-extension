package engineering.everest.axon.cryptoshredding.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultValueProviderTest {

    private DefaultValueProvider defaultValueProvider;

    @BeforeEach
    void setUp() {
        defaultValueProvider = new DefaultValueProvider();
    }

    @ParameterizedTest
    @MethodSource("testValuesForDefaultValue")
    void defaultValue_WillReturnExpectedDefault(Class<?> clazz, Object expectedDefault) {
        assertEquals(expectedDefault, defaultValueProvider.defaultValue(clazz));
    }

    @Test
    void defaultValue_WillReturnDefaultForRegisteredCustomType() {
        var customDefaultValueProvider = DefaultValueProvider.builder()
            .customType(UUID.class, UUID.fromString("deadbeef-dead-beef-dead-beef00000000"))
            .build();

        assertEquals(UUID.fromString("deadbeef-dead-beef-dead-beef00000000"),
            customDefaultValueProvider.defaultValue(UUID.class));
    }

    @Test
    void defaultValue_WillFail_WhenClassIsNull() {
        var exception = assertThrows(IllegalArgumentException.class, () -> defaultValueProvider.defaultValue(null));
        assertEquals("Default value for null class doesn't make sense", exception.getMessage());
    }

    private static Stream<Arguments> testValuesForDefaultValue() {
        return Stream.of(
            Arguments.of(Boolean.class, null),
            Arguments.of(boolean.class, false),
            Arguments.of(char.class, '\0'),
            Arguments.of(Character.class, null),
            Arguments.of(byte.class, (byte) 0),
            Arguments.of(Byte.class, null),
            Arguments.of(short.class, (short) 0),
            Arguments.of(Short.class, null),
            Arguments.of(int.class, 0),
            Arguments.of(Integer.class, null),
            Arguments.of(long.class, 0L),
            Arguments.of(Long.class, null),
            Arguments.of(float.class, 0f),
            Arguments.of(Float.class, null),
            Arguments.of(double.class, 0d),
            Arguments.of(Double.class, null));
    }
}
