package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.starterkit.axon.cryptoshredding.CryptoShreddingKeyService;
import engineering.everest.starterkit.axon.cryptoshredding.CryptoShreddingSerializer;
import engineering.everest.starterkit.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.EncryptionKeyDeletedException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingSerializedEncryptionKeyIdentifierFieldException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.SecretKeyRepository;
import engineering.everest.starterkit.axon.cryptoshredding.testevents.EventWithDifferentiatedKeyType;
import engineering.everest.starterkit.axon.cryptoshredding.testevents.EventWithEncryptedFields;
import engineering.everest.starterkit.axon.cryptoshredding.testevents.EventWithMissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.testevents.EventWithUnsupportedEncryptionKeyIdentifierType;
import engineering.everest.starterkit.axon.cryptoshredding.testevents.EventWithoutEncryptedFields;
import org.axonframework.serialization.Converter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoShreddingSerializerTest {

    private static final String REVISION_NUMBER = "0";
    private static final TypeDifferentiatedSecretKeyId KEY_IDENTIFIER = new TypeDifferentiatedSecretKeyId("key-identifier", "");
    private static final String EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON = "{\"anIntegerField\":65535,\"astringField\":\"Default string\",\"aprimitiveIntegerField\":42,\"aprimitiveLongField\":9600,\"aprimitiveFloatField\":123.45679,\"auuidfield\":\"deadbeef-dead-beef-dead-beef00000042\",\"alongField\":98765432,\"abyteArrayField\":\"SSBhbSBhIGJ5dGUgYXJyYXk=\"}";
    private static final SecretKey ENCRYPTION_KEY = new SecretKeySpec("0123456789012345".getBytes(), "AES");

    private CryptoShreddingSerializer cryptoShreddingSerializerWithMock;
    private CryptoShreddingSerializer jsonCryptoShreddingSerializer;
    private DefaultAesEncrypter defaultAesEncrypter;
    private DefaultAesDecrypter defaultAesDecrypter;

    @Mock
    private Serializer mockWrappedSerializer;
    @Mock
    private SecretKeyRepository secretKeyRepository;
    @Mock
    private CryptoShreddingKeyService cryptoShreddingKeyService;
    @Mock
    private DefaultAesEncrypterDecrypterFactory encrypterFactory;

    @BeforeEach
    void setUp() {
        cryptoShreddingSerializerWithMock = new CryptoShreddingSerializer(mockWrappedSerializer, cryptoShreddingKeyService, encrypterFactory, new ObjectMapper());
        jsonCryptoShreddingSerializer = new CryptoShreddingSerializer(JacksonSerializer.defaultSerializer(), cryptoShreddingKeyService, encrypterFactory, new ObjectMapper());
        var secureRandom = new SecureRandom();
        defaultAesEncrypter = new DefaultAesEncrypter(secureRandom);
        defaultAesDecrypter = new DefaultAesDecrypter(secureRandom);
    }

    @Test
    void serialize_WillGenerateNewKeyAndEncodeAnnotatedFields() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);

        jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);

        verify(cryptoShreddingKeyService).getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER);
    }

    @Test
    void serialize_WillReturnASerializedObjectWithTypeOfSerializedClass() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);

        var serialized = jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);

        assertEquals(new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), "0"), serialized.getType());
    }

    @Test
    void serialize_WillFailWhenEncryptionKeyHasBeenDeleted() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.empty());

        assertThrows(EncryptionKeyDeletedException.class,
                () -> jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class));

    }

    @Test
    void serialize_WillFail_WhenEncryptionKeyIdentifierIsNotSupported() {
        assertThrows(UnsupportedEncryptionKeyIdentifierTypeException.class, () ->
                jsonCryptoShreddingSerializer.serialize(new EventWithUnsupportedEncryptionKeyIdentifierType(), byte[].class)
        );
    }

    @Test
    void serialize_WillFail_WhenEncryptionKeyIdentifierAnnotationIsMissing() {
        assertThrows(MissingEncryptionKeyIdentifierAnnotation.class,
                () -> jsonCryptoShreddingSerializer.serialize(new EventWithMissingEncryptionKeyIdentifierAnnotation(), byte[].class));
    }

    @Test
    void deserialize_WillSkipDecryptingUnencryptedEvents() {
        SimpleSerializedType serializedType = new SimpleSerializedType(EventWithoutEncryptedFields.class.getCanonicalName(), REVISION_NUMBER);
        SimpleSerializedObject<byte[]> serializedEvent = new SimpleSerializedObject<>(EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON.getBytes(), byte[].class, serializedType);

        assertEquals(EventWithoutEncryptedFields.createTestInstance(), jsonCryptoShreddingSerializer.deserialize(serializedEvent));
        verify(secretKeyRepository, never()).findById(any(TypeDifferentiatedSecretKeyId.class));
    }

    @Test
    void deserialize_WillDeserializeUnencryptedEventsCreatedByThisSerializer() {
        var serializedEvent = jsonCryptoShreddingSerializer.serialize(EventWithoutEncryptedFields.createTestInstance(), byte[].class);
        var serializedType = new SimpleSerializedType(EventWithoutEncryptedFields.class.getCanonicalName(), REVISION_NUMBER);

        EventWithoutEncryptedFields deserialized = jsonCryptoShreddingSerializer.deserialize(
                new SimpleSerializedObject<>(serializedEvent.getData(), byte[].class, serializedType));

        assertEquals(EventWithoutEncryptedFields.createTestInstance(), deserialized);
        verify(secretKeyRepository, never()).findById(any(TypeDifferentiatedSecretKeyId.class));
    }

    @Test
    void deserialize_WillDecryptEventsSerializedByJsonCryptoShreddingSerializer_WhenEventContainsEncryptedFields() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingKeyService.getExistingSecretKey(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);
        when(encrypterFactory.createDecrypter()).thenReturn(defaultAesDecrypter);

        var serializedAndEncryptedEvent = jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), REVISION_NUMBER));
        EventWithEncryptedFields deserialized = jsonCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);

        assertEquals(EventWithEncryptedFields.createTestInstance(), deserialized);
    }

    @Test
    void deserialize_WillDecryptEventsSerializedByXmlCryptoShreddingSerializer_WhenEventContainsEncryptedFields() {
        var xmlCryptoShreddingSerializer = new CryptoShreddingSerializer(XStreamSerializer.defaultSerializer(), cryptoShreddingKeyService, encrypterFactory, new ObjectMapper());

        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingKeyService.getExistingSecretKey(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);
        when(encrypterFactory.createDecrypter()).thenReturn(defaultAesDecrypter);

        var serializedAndEncryptedEvent = xmlCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), String.class);
        SimpleSerializedObject<String> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), String.class,
                new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), REVISION_NUMBER));
        EventWithEncryptedFields deserialized = xmlCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);

        assertEquals(EventWithEncryptedFields.createTestInstance(), deserialized);
    }

    @Test
    void deserialize_WillReplaceEncryptedFieldsWithDefaultValues_WhenEncryptionKeyHasBeenDeleted() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingKeyService.getExistingSecretKey(KEY_IDENTIFIER)).thenReturn(Optional.empty());
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);

        var serializedAndEncryptedEvent = jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), REVISION_NUMBER));
        EventWithEncryptedFields deserialized = jsonCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);

        assertEquals(EventWithEncryptedFields.createUnencryptedTestInstance(), deserialized);
    }

    @Test
    void deserialize_WillFail_WhenEncryptionKeyIdentifierFieldHasBeenDeletedOrRenamedInSerializedForm() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);

        var serializedAndEncryptedEvent = jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        byte[] mangledPayload = new String(serializedAndEncryptedEvent.getData()).replace("keyIdentifier", "renamed-or-lost!").getBytes();
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent =
                new SimpleSerializedObject<>(mangledPayload, byte[].class,
                        new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), null));

        assertThrows(MissingSerializedEncryptionKeyIdentifierFieldException.class, () -> {
            EventWithEncryptedFields deserialized = jsonCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        });
    }

    @Test
    void deserialize_WillFail_WhenEncryptionKeyIdentifierValueHasBeenDeletedOrRenamedInSerializedForm() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);

        var serializedAndEncryptedEvent = jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        byte[] mangledPayload = new String(serializedAndEncryptedEvent.getData()).replace("key-identifier", "").getBytes();
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent =
                new SimpleSerializedObject<>(mangledPayload, byte[].class,
                        new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), null));

        assertThrows(MissingSerializedEncryptionKeyIdentifierFieldException.class, () -> {
            EventWithEncryptedFields deserialized = jsonCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        });
    }

    @Test
    void deserialize_WillFail_WhenEncryptionKeyIdentifierAnnotationIsMissing() {
        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(KEY_IDENTIFIER)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);

        var serializedAndEncryptedEvent = jsonCryptoShreddingSerializer.serialize(EventWithEncryptedFields.createTestInstance(), byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithMissingEncryptionKeyIdentifierAnnotation.class.getCanonicalName(), REVISION_NUMBER));

        assertThrows(MissingEncryptionKeyIdentifierAnnotation.class, () -> {
            EventWithMissingEncryptionKeyIdentifierAnnotation deserialized = jsonCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);
        });
    }

    @Test
    void keyTypeCanBeUsedToDifferentiateBetweenPrimitiveIdentifiers() {
        var typeDifferentiatedKey = new TypeDifferentiatedSecretKeyId(String.valueOf(1234L), "some-tag");

        when(cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(typeDifferentiatedKey)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(cryptoShreddingKeyService.getExistingSecretKey(typeDifferentiatedKey)).thenReturn(Optional.of(ENCRYPTION_KEY));
        when(encrypterFactory.createEncrypter()).thenReturn(defaultAesEncrypter);
        when(encrypterFactory.createDecrypter()).thenReturn(defaultAesDecrypter);

        var eventWithDifferentiatedKeyType = new EventWithDifferentiatedKeyType(1234L, "field value");
        var serializedAndEncryptedEvent = jsonCryptoShreddingSerializer.serialize(eventWithDifferentiatedKeyType, byte[].class);
        SimpleSerializedObject<byte[]> typeInformationAugmentedEncryptedEvent = new SimpleSerializedObject<>(serializedAndEncryptedEvent.getData(), byte[].class,
                new SimpleSerializedType(EventWithDifferentiatedKeyType.class.getCanonicalName(), REVISION_NUMBER));
        EventWithDifferentiatedKeyType deserialized = jsonCryptoShreddingSerializer.deserialize(typeInformationAugmentedEncryptedEvent);

        assertEquals(eventWithDifferentiatedKeyType, deserialized);
    }

    @Test
    void canSerializeTo_WillBeDelegated() {
        cryptoShreddingSerializerWithMock.canSerializeTo(byte[].class);

        verify(mockWrappedSerializer).canSerializeTo(byte[].class);
    }

    @Test
    void classForType_WillBeDelegated() {
        var type = new SimpleSerializedType("object-type", "revision-C");
        cryptoShreddingSerializerWithMock.classForType(type);

        verify(mockWrappedSerializer).classForType(type);
    }

    @Test
    void typeForClass_WillBeDelegated() {
        cryptoShreddingSerializerWithMock.typeForClass(byte[].class);

        verify(mockWrappedSerializer).typeForClass(byte[].class);
    }

    @Test
    void getConverter_WillBeDelegate() {
        Converter converter = mock(Converter.class);
        when(mockWrappedSerializer.getConverter()).thenReturn(converter);

        assertEquals(converter, cryptoShreddingSerializerWithMock.getConverter());
    }
}
