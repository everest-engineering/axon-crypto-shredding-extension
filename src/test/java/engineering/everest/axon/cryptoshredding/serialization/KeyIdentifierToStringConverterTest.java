package engineering.everest.axon.cryptoshredding.serialization;

import engineering.everest.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyIdentifierToStringConverterTest {

    private KeyIdentifierToStringConverter keyIdentifierToStringConverter;

    @BeforeEach
    void setUp() {
        keyIdentifierToStringConverter = new KeyIdentifierToStringConverter();
    }

    @ParameterizedTest
    @MethodSource("testValuesForConversion")
    void convertToString_WillConvertStandardTypes(Object keyIdentifierField, String expectedConversion) {
        assertEquals(expectedConversion, keyIdentifierToStringConverter.convertToString(keyIdentifierField));
    }

    @Test
    void convertToString_WillReturnStringForRegisteredCustomType() {
        var customKeyIdentifierToStringConverter = KeyIdentifierToStringConverter.builder()
            .customConverter(CustomIdType.class, object -> String.valueOf(((CustomIdType) object).getWrappedIdentifier()))
            .build();

        assertEquals("888888", customKeyIdentifierToStringConverter.convertToString(new CustomIdType(888888L)));
    }

    @Test
    void convertToString_WillFail_WhenKeyIdentifierTypeNotSupported() {
        assertThrows(UnsupportedEncryptionKeyIdentifierTypeException.class,
            () -> keyIdentifierToStringConverter.convertToString(new CustomIdType(1234L)));
    }

    private static Stream<Arguments> testValuesForConversion() {
        return Stream.of(
            Arguments.of("carbon copy", "carbon copy"),
            Arguments.of(UUID.fromString("deadbeef-dead-beef-dead-beef00000007"), "deadbeef-dead-beef-dead-beef00000007"),
            Arguments.of(42L, "42"),
            Arguments.of(43, "43"));
    }

    @Getter
    @AllArgsConstructor
    private static class CustomIdType {
        private long wrappedIdentifier;
    }
}
