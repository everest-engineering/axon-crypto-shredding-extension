package engineering.everest.starterkit.axon.cryptoshredding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.EncryptionKeyDeletedException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingSerializedEncryptionKeyIdentifierException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;
import lombok.extern.log4j.Log4j2;
import org.axonframework.serialization.Converter;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Defaults.defaultValue;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@Log4j2
public class CryptoShreddingEventSerializer implements Serializer {

    private final Serializer wrappedSerializer;
    private final CryptoShreddingService cryptoShreddingService;
    private final ObjectMapper objectMapper;

    public CryptoShreddingEventSerializer(Serializer wrappedSerializer,
                                          CryptoShreddingService cryptoShreddingService,
                                          ObjectMapper objectMapper) {
        this.wrappedSerializer = wrappedSerializer;
        this.cryptoShreddingService = cryptoShreddingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> SerializedObject<T> serialize(Object object, Class<T> expectedRepresentation) {
        var fields = List.of(object.getClass().getDeclaredFields());
        var encryptedFields = getEncryptedFields(fields);
        if (encryptedFields.isEmpty()) {
            return wrappedSerializer.serialize(object, expectedRepresentation);
        }

        var encryptionKey = retrieveOrCreateEncryptionKeyForSerialization(object, fields);
        var encryptedObject = encryptAnnotatedFields(object, encryptedFields, encryptionKey);
        return wrappedSerializer.serialize(encryptedObject, expectedRepresentation);
    }

    @Override
    public <T> boolean canSerializeTo(Class<T> expectedRepresentation) {
        return wrappedSerializer.canSerializeTo(expectedRepresentation);
    }

    @Override
    public <S, T> T deserialize(SerializedObject<S> serializedObject) {
        Class<?> classToDeserialize = getClassToDeserialize(serializedObject);
        var fields = List.of(classToDeserialize.getDeclaredFields());
        var encryptedFields = getEncryptedFields(fields);
        if (encryptedFields.isEmpty()) {
            return wrappedSerializer.deserialize(serializedObject);
        }

        var encryptedSerializedType = new SimpleSerializedType(HashMap.class.getCanonicalName(), null);
        var encryptedSerializedObject = new SimpleSerializedObject<>((String) serializedObject.getData(), String.class, encryptedSerializedType);
        Map<String, Object> encryptedMappedObject = wrappedSerializer.deserialize(encryptedSerializedObject);
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(encryptedMappedObject);
        var optionalEncryptionKey = retrieveEncryptionKeyForDeserialization(encryptedMappedObject, serializedFieldNameMapping, fields);

        var mappedObject = optionalEncryptionKey
                .map(secretKey -> decryptAnnotatedFields(encryptedMappedObject, serializedFieldNameMapping, encryptedFields, secretKey))
                .orElseGet(() -> applyEncryptedFieldDefaults(encryptedMappedObject, serializedFieldNameMapping, encryptedFields));
        return (T) objectMapper.convertValue(mappedObject, classToDeserialize);
    }

    @Override
    public Class classForType(SerializedType type) {
        return wrappedSerializer.classForType(type);
    }

    @Override
    public SerializedType typeForClass(Class type) {
        return wrappedSerializer.typeForClass(type);
    }

    @Override
    public Converter getConverter() {
        return wrappedSerializer.getConverter();
    }

    private List<Field> getEncryptedFields(List<Field> fields) {
        return fields.stream()
                .filter(field -> field.getAnnotation(EncryptedField.class) != null)
                .collect(toList());
    }

    private SecretKey retrieveOrCreateEncryptionKeyForSerialization(Object object, List<Field> fields) {
        var optionalField = findOptionalEncryptionKeyIdentifierField(fields);
        if (optionalField.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotation();
        }

        var encryptionKeyIdentifier = extractEncryptionKeyIdentifier(object, optionalField.get());
        var encryptionKey = cryptoShreddingService.getOrCreateSecretKeyUnlessDeleted(encryptionKeyIdentifier);
        return encryptionKey.orElseThrow(() -> new EncryptionKeyDeletedException(encryptionKeyIdentifier));
    }

    private Optional<SecretKey> retrieveEncryptionKeyForDeserialization(Map<String, Object> encryptedMappedObject,
                                                                        Map<String, String> serializedFieldNameMapping,
                                                                        List<Field> fields) {
        var optionalField = findOptionalEncryptionKeyIdentifierField(fields);
        if (optionalField.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotation();
        }

        var encryptionKeyIdentifierKey = serializedFieldNameMapping.get(optionalField.get().getName().toLowerCase());
        if (encryptionKeyIdentifierKey == null) {
            throw new MissingSerializedEncryptionKeyIdentifierException();
        }

        var encryptionKeyIdentifier = encryptedMappedObject.get(encryptionKeyIdentifierKey).toString();
        if (encryptionKeyIdentifier == null || encryptionKeyIdentifier.isBlank()) {
            throw new MissingSerializedEncryptionKeyIdentifierException();
        }
        return cryptoShreddingService.getExistingSecretKey(encryptionKeyIdentifier);
    }

    private Optional<Field> findOptionalEncryptionKeyIdentifierField(List<Field> fields) {
        return fields.stream()
                .filter(field -> field.getAnnotation(EncryptionKeyIdentifier.class) != null)
                .findFirst();
    }

    private String extractEncryptionKeyIdentifier(Object object, Field encryptionKeyIdentifierField) {
        encryptionKeyIdentifierField.setAccessible(true);
        try {
            return convertToString(encryptionKeyIdentifierField.get(object));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertToString(Object object) {
        if (object instanceof String || object instanceof UUID || object instanceof Long || object instanceof Integer) {
            return object.toString();
        }
        throw new UnsupportedEncryptionKeyIdentifierTypeException(object.toString());
    }

    private Object encryptAnnotatedFields(Object object, List<Field> encryptedFields, SecretKey encryptionKey) {
        var mappedObject = objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(mappedObject);
        var base64EncodingAesEncrypter = cryptoShreddingService.createEncrypter(encryptionKey);

        encryptedFields.forEach(field -> {
            var fieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var serializedClearText = wrappedSerializer.serialize(mappedObject.get(fieldKey), String.class);
            mappedObject.put(fieldKey, base64EncodingAesEncrypter.encryptAndEncode(serializedClearText.getData()));
        });

        return mappedObject;
    }

    private Map<String, String> buildFieldNamingSerializationStrategyIndependentMapping(Map<String, Object> mappedObject) {
        return mappedObject.keySet().stream()
                .collect(toMap(String::toLowerCase, fieldName -> fieldName));
    }

    private Map<String, Object> decryptAnnotatedFields(Map<String, Object> encryptedMappedObject,
                                                       Map<String, String> serializedFieldNameMapping,
                                                       List<Field> encryptedFields,
                                                       SecretKey encryptionKey) {
        var base64EncodingAesEncrypter = cryptoShreddingService.createDecrypter(encryptionKey);

        encryptedFields.forEach(field -> {
            var serializedFieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var cleartextSerializedFieldValue = base64EncodingAesEncrypter.decryptBase64Encoded((String) encryptedMappedObject.get(serializedFieldKey));
            var deserializedFieldValue = wrappedSerializer.deserialize(
                    new SimpleSerializedObject<>(cleartextSerializedFieldValue, String.class, Object.class.getCanonicalName(), null));
            encryptedMappedObject.put(serializedFieldKey, deserializedFieldValue);
        });

        return encryptedMappedObject;
    }

    private Map<String, Object> applyEncryptedFieldDefaults(Map<String, Object> encryptedMappedObject,
                                                            Map<String, String> serializedFieldNameMapping,
                                                            List<Field> encryptedFields) {
        encryptedFields.forEach(field -> {
            var serializedFieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            encryptedMappedObject.put(serializedFieldKey, defaultValue(field.getType()));
        });

        return encryptedMappedObject;
    }

    private <S> Class<?> getClassToDeserialize(SerializedObject<S> serializedObject) {
        Class<?> classToDeserialize;
        try {
            classToDeserialize = Class.forName(serializedObject.getType().getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return classToDeserialize;
    }
}
