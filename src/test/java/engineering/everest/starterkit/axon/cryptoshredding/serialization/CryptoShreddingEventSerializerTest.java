package engineering.everest.starterkit.axon.cryptoshredding.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.EncryptionKeyRepository;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.PersistableEncryptionKey;
import org.axonframework.serialization.Converter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.axonframework.serialization.json.JacksonSerializer.defaultSerializer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoShreddingEventSerializerTest {

    private static final String REVISION_NUMBER = "0";
    private static final String KEY_IDENTIFIER = "key-identifier";
    private static final String EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON = "{\"anIntegerField\":65535,\"astringField\":\"Default string\",\"aprimitiveIntegerField\":42,\"aprimitiveLongField\":9600,\"aprimitiveFloatField\":123.45679,\"auuidfield\":\"deadbeef-dead-beef-dead-beef00000042\",\"alongField\":98765432,\"abyteArrayField\":\"SSBhbSBhIGJ5dGUgYXJyYXk=\"}";
    private static final String ENCRYPTION_KEY = "test-key";

    private CryptoShreddingEventSerializer cryptoShreddingEventSerializerWithMock;
    private CryptoShreddingEventSerializer jsonCryptoShreddingEventSerializer;

    // TODO test with XML serializer

    @Mock
    private Serializer mockWrappedSerializer;
    @Mock
    private EncryptionKeyGenerator encryptionKeyGenerator;
    @Mock
    private EncryptionKeyRepository encryptionKeyRepository;

    @BeforeEach
    void setUp() {
        cryptoShreddingEventSerializerWithMock = new CryptoShreddingEventSerializer(mockWrappedSerializer, encryptionKeyGenerator, encryptionKeyRepository, new ObjectMapper());
        jsonCryptoShreddingEventSerializer = new CryptoShreddingEventSerializer(defaultSerializer(), encryptionKeyGenerator, encryptionKeyRepository, new ObjectMapper());
    }

    @Test
    void serialize_WillNotEncodeEventsWithoutAnnotations() {
        var serializedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithoutEncryptedFields.createTestInstance(), String.class);
        assertEquals(EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON, serializedEvent.getData());
    }

    @Test
    void serialize_WillGenerateNewKeyAndEncodeAnnotatedFields() {
        when(encryptionKeyGenerator.generateKey()).thenReturn(ENCRYPTION_KEY);
        when(encryptionKeyRepository.create(KEY_IDENTIFIER, ENCRYPTION_KEY)).thenReturn(ENCRYPTION_KEY);

        var serializedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), String.class);
    }

    // TODO: test that we fail when encrypted field annotations are present but we don't have a key identifier annotation

    // TODO: will encode using previously created key

    @Test
    void deserialize_WillNotDecodeEventsWithoutAnnotations() {
        var serializedObject = new SimpleSerializedObject<>(EVENT_WITHOUT_ANNOTATIONS_SERIALIZED_JSON, String.class, new SimpleSerializedType(EventWithoutEncryptedFields.class.getCanonicalName(), REVISION_NUMBER));
        var result = jsonCryptoShreddingEventSerializer.deserialize(serializedObject);
    }

    @Test
    void deserialize_WillDeserializeUnencryptedEventsCreatedByThisSerializer() {
        var serializedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithoutEncryptedFields.createTestInstance(), String.class);
        var serializedType = new SimpleSerializedType(EventWithoutEncryptedFields.class.getCanonicalName(), REVISION_NUMBER);
        EventWithoutEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(
                new SimpleSerializedObject<>(serializedEvent.getData(), String.class, serializedType));
        assertEquals(EventWithoutEncryptedFields.createTestInstance(), deserialized);
    }

    @Test
    void deserialize_WillDecryptEventsSerializedByJsonCryptoShreddingSerializer_WhenEventContainsEncryptedFields() {
        when(encryptionKeyGenerator.generateKey()).thenReturn(ENCRYPTION_KEY);
        when(encryptionKeyRepository.create(KEY_IDENTIFIER, ENCRYPTION_KEY)).thenReturn(ENCRYPTION_KEY);
        when(encryptionKeyRepository.findById(KEY_IDENTIFIER)).thenReturn(Optional.of(new PersistableEncryptionKey(KEY_IDENTIFIER, ENCRYPTION_KEY)));

        var serializedEvent = jsonCryptoShreddingEventSerializer.serialize(EventWithEncryptedFields.createTestInstance(), String.class);
        var serializedType = new SimpleSerializedType(EventWithEncryptedFields.class.getCanonicalName(), REVISION_NUMBER);
        EventWithEncryptedFields deserialized = jsonCryptoShreddingEventSerializer.deserialize(
                new SimpleSerializedObject<>(serializedEvent.getData(), String.class, serializedType));
        assertEquals(EventWithEncryptedFields.createTestInstance(), deserialized);
    }

    @Test
    void canSerializeTo_WillBeDelegated() {
        cryptoShreddingEventSerializerWithMock.canSerializeTo(String.class);

        verify(mockWrappedSerializer).canSerializeTo(String.class);
    }

    @Test
    void classForType_WillBeDelegated() {
        var type = new SimpleSerializedType("object-type", "revision-C");
        cryptoShreddingEventSerializerWithMock.classForType(type);

        verify(mockWrappedSerializer).classForType(type);
    }

    @Test
    void typeForClass_WillBeDelegated() {
        cryptoShreddingEventSerializerWithMock.typeForClass(String.class);

        verify(mockWrappedSerializer).typeForClass(String.class);
    }

    @Test
    void getConverter_WillBeDelegate() {
        Converter converter = mock(Converter.class);
        when(mockWrappedSerializer.getConverter()).thenReturn(converter);

        assertEquals(converter, cryptoShreddingEventSerializerWithMock.getConverter());
    }
}
