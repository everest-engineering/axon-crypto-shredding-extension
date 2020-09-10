package engineering.everest.starterkit.axon.cryptoshredding.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.starterkit.axon.cryptoshredding.CryptoShreddingEventSerializer;
import engineering.everest.starterkit.axon.cryptoshredding.CryptoShreddingService;
import engineering.everest.starterkit.axon.cryptoshredding.encryption.Base64EncodingAesEncrypter;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.EncryptionKeyDeletedException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingSerializedEncryptionKeyIdentifierException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.SecretKeyRepository;
import engineering.everest.starterkit.axon.cryptoshredding.serialization.testevents.EventWithEncryptedFields;
import engineering.everest.starterkit.axon.cryptoshredding.serialization.testevents.EventWithMissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.serialization.testevents.EventWithUnsupportedEncryptionKeyIdentifierType;
import engineering.everest.starterkit.axon.cryptoshredding.serialization.testevents.EventWithoutEncryptedFields;
import org.axonframework.serialization.Converter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Optional;

import static org.axonframework.serialization.json.JacksonSerializer.defaultSerializer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoShreddingEventSerializerTest {

    private static final String REVISION_NUMBER = "0";
    private static final String KEY_IDENTIFIER = "key-identifier";
    private static final String EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON = "{\"anIntegerField\":65535,\"astringField\":\"Default string\",\"aprimitiveIntegerField\":42,\"aprimitiveLongField\":9600,\"aprimitiveFloatField\":123.45679,\"auuidfield\":\"deadbeef-dead-beef-dead-beef00000042\",\"alongField\":98765432,\"abyteArrayField\":\"SSBhbSBhIGJ5dGUgYXJyYXk=\"}";
    private static final SecretKey ENCRYPTION_KEY = new SecretKeySpec("0123456789012345".getBytes(), "AES");

    private CryptoShreddingEventSerializer cryptoShreddingEventSerializerWithMock;
    private CryptoShreddingEventSerializer jsonCryptoShreddingEventSerializer;

    // TODO test with XML serializer

    @Mock
    private Serializer mockWrappedSerializer;
    @Mock
    private SecretKeyRepository secretKeyRepository;
    @Mock
    private Base64EncodingAesEncrypter base64EncodingAesEncrypter;
    @Mock
    private CryptoShreddingService cryptoShreddingService;

    @BeforeEach
    void setUp() {
        cryptoShreddingEventSerializerWithMock = new CryptoShreddingEventSerializer(mockWrappedSerializer, cryptoShreddingService, new ObjectMapper());
        jsonCryptoShreddingEventSerializer = new CryptoShreddingEventSerializer(defaultSerializer(), cryptoShreddingService, new ObjectMapper());
    }

    @Test
    void serialize_WillGenerateNewKeyAndEncodeAnnotatedFields() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(base64EncodingAesEncrypter);

        jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);

        verify(cryptoShreddingService).getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER);
    }

    @Test
    void serialize_WillReturnASerializedObjectWithTypeOfSerializedClass() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(base64EncodingAesEncrypter);

        var serialized = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        assertEquals(new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), "0"), serialized.getType());
    }

    @Test
    void serialize_WillFailWhenEncryptionKeyHasBeenDeleted() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.empty());

        assertThrows(EncryptionKeyDeletedException.class,
                () -> jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class));

    }

    @Test
    void serialize_WillFail_WhenEncryptionKeyIdentifierIsNotSupported() {
        assertThrows(UnsupportedEncryptionKeyIdentifierTypeException.class, () ->
                jsonCryptoShreddingEventSerializer.serialize(new EventWithUnsupportedEncryptionKeyIdentifierType(), byte[].class)
        );
    }

    @Test
    void serialize_WillFail_WhenEncryptionKeyIdentifierAnnotationIsMissing() {
        assertThrows(MissingEncryptionKeyIdentifierAnnotation.class,
                () -> jsonCryptoShreddingEventSerializer.serialize(new EventWithMissingEncryptionKeyIdentifierAnnotation(), byte[].class));
    }

    @Test
    void deserialize_WillSkipDecryptingUnencryptedEvents() {
        SimpleSerializedType serializedType = new SimpleSerializedType(EventWithoutEncryptedFields.class.getCanonicalName(), REVISION_NUMBER);
        SimpleSerializedObject<byte[]> serializedEvent = new SimpleSerializedObject<>(EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON.getBytes(), byte[].class, serializedType);

        assertEquals(EventWithoutEncryptedFields.createTestInstance(), jsonCryptoShreddingEventSerializer.deserialize(serializedEvent));
        verify(secretKeyRepository, never()).findById(anyString());
    }

    @Test
    void deserialize_WillDeserializeUnencryptedEventsCreatedByThisSerializer() {
        var serializedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithoutEncryptedFields.createTestInstance(), byte[].class);
        var serializedType = new SimpleSerializedType(EventWithoutEncryptedFields.class.getCanonicalName(), REVISION_NUMBER);

        EventWithoutEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(
                new SimpleSerializedObject<>(serializedEvent.getData(), byte[].class, serializedType));

        assertEquals(EventWithoutEncryptedFields.createTestInstance(), deserialized);
        verify(secretKeyRepository, never()).findById(anyString());
    }

    @Test
    void deserialize_WillDecryptEventsSerializedByJsonCryptoShreddingSerializer_WhenEventContainsEncryptedFields() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.getExistingSecretKey(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(new Base64EncodingAesEncrypter());
        when(cryptoShreddingService.createDecrypter(ENCRYPTION_KEY)).thenReturn(new Base64EncodingAesEncrypter());

        var serializedAndEncryptedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), REVISION_NUMBER));
        EventWithEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        assertEquals(EventWithEncryptedFields.createTestInstance(), deserialized);
    }

    @Test
    void deserialize_WillReplaceEncryptedFieldsWithDefaultValues_WhenEncryptionKeyHasBeenDeleted() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.getExistingSecretKey(KEY_IDENTIFIER)).thenReturn(Optional.empty());
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(new Base64EncodingAesEncrypter());

        var serializedAndEncryptedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), REVISION_NUMBER));
        EventWithEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        assertEquals(EventWithEncryptedFields.createUnencryptedTestInstance(), deserialized);
    }

    @Test
    void deserialize_WillFail_WhenEncryptionKeyIdentifierFieldHasBeenDeletedOrRenamedInSerializedForm() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(new Base64EncodingAesEncrypter());

        var serializedAndEncryptedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        byte[] mangledPayload = new String(serializedAndEncryptedEvent.getData()).replace("keyIdentifier", "renamed-or-lost!").getBytes();
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent =
                new SimpleSerializedObject<>(mangledPayload, byte[].class,
                        new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), null));

        assertThrows(MissingSerializedEncryptionKeyIdentifierException.class, () -> {
            EventWithEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        });
    }

    @Test
    void deserialize_WillFail_WhenEncryptionKeyIdentifierValueHasBeenDeletedOrRenamedInSerializedForm() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(new Base64EncodingAesEncrypter());

        var serializedAndEncryptedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        byte[] mangledPayload = new String(serializedAndEncryptedEvent.getData()).replace("key-identifier", "").getBytes();
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent =
                new SimpleSerializedObject<>(mangledPayload, byte[].class,
                        new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), null));

        assertThrows(MissingSerializedEncryptionKeyIdentifierException.class, () -> {
            EventWithEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        });
    }

    @Test
    void deserialize_WillFail_WhenEncryptionKeyIdentifierAnnotationIsMissing() {
        when(cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingService.createEncrypter(ENCRYPTION_KEY)).thenReturn(new Base64EncodingAesEncrypter());

        var serializedAndEncryptedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithMissingEncryptionKeyIdentifierAnnotation.class.getCanonicalName(), REVISION_NUMBER));

        assertThrows(MissingEncryptionKeyIdentifierAnnotation.class, () -> {
            EventWithMissingEncryptionKeyIdentifierAnnotation deserialized = jsonCryptoShreddingEventSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        });
    }

    @Test
    void canSerializeTo_WillBeDelegated() {
        cryptoShreddingEventSerializerWithMock.canSerializeTo(byte[].class);

        verify(mockWrappedSerializer).canSerializeTo(byte[].class);
    }

    @Test
    void classForType_WillBeDelegated() {
        var type = new SimpleSerializedType("object-type", "revision-C");
        cryptoShreddingEventSerializerWithMock.classForType(type);

        verify(mockWrappedSerializer).classForType(type);
    }

    @Test
    void typeForClass_WillBeDelegated() {
        cryptoShreddingEventSerializerWithMock.typeForClass(byte[].class);

        verify(mockWrappedSerializer).typeForClass(byte[].class);
    }

    @Test
    void getConverter_WillBeDelegate() {
        Converter converter = mock(Converter.class);
        when(mockWrappedSerializer.getConverter()).thenReturn(converter);

        assertEquals(converter, cryptoShreddingEventSerializerWithMock.getConverter());
    }
}
