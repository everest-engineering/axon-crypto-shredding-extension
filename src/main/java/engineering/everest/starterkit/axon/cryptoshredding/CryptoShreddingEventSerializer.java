package engineering.everest.starterkit.axon.cryptoshredding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import engineering.everest.starterkit.axon.cryptoshredding.encryption.AesEncrypterDecrypterFactory;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.EncryptionKeyDeletedException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingSerializedEncryptionKeyIdentifierException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;
import lombok.extern.log4j.Log4j2;
import org.axonframework.common.ObjectUtils;
import org.axonframework.serialization.Converter;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Defaults.defaultValue;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Log4j2
public class CryptoShreddingEventSerializer implements Serializer {

    private final Serializer wrappedSerializer;
    private final CryptoShreddingKeyService cryptoShreddingKeyService;
    private final AesEncrypterDecrypterFactory aesEncrypterDecrypterFactory;
    private final ObjectMapper objectMapper;

    public CryptoShreddingEventSerializer(@Qualifier("eventSerializer") Serializer wrappedEventSerializer,
                                          CryptoShreddingKeyService cryptoShreddingKeyService,
                                          AesEncrypterDecrypterFactory aesEncrypterDecrypterFactory, ObjectMapper objectMapper) {
        this.wrappedSerializer = wrappedEventSerializer;
        this.cryptoShreddingKeyService = cryptoShreddingKeyService;
        this.aesEncrypterDecrypterFactory = aesEncrypterDecrypterFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> SerializedObject<T> serialize(Object object, Class<T> expectedRepresentation) {
        var fields = List.of(object.getClass().getDeclaredFields());
        var encryptedFields = getEncryptedFields(fields);
        if (encryptedFields.isEmpty()) {
            return wrappedSerializer.serialize(object, expectedRepresentation);
        }

        var secretKey = retrieveOrCreateSecretKeyForSerialization(object, fields);
        var encryptedMappedObject = mapAndEncryptAnnotatedFields(object, encryptedFields, secretKey);
        var serializedObject = wrappedSerializer.serialize(encryptedMappedObject, expectedRepresentation);
        return new SimpleSerializedObject<>(serializedObject.getData(), expectedRepresentation,
                wrappedSerializer.typeForClass(ObjectUtils.nullSafeTypeOf(object)));
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

        var encryptedSerializedType = new SimpleSerializedType(HashMap.class.getCanonicalName(), serializedObject.getType().getRevision());
        var encryptedSerializedObject = new SimpleSerializedObject<>(serializedObject.getData(), serializedObject.getContentType(), encryptedSerializedType);
        Map<String, Object> encryptedMappedObject = wrappedSerializer.deserialize(encryptedSerializedObject);
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(encryptedMappedObject);
        var optionalSecretKey = retrieveSecretKeyForDeserialization(encryptedMappedObject, serializedFieldNameMapping, fields);

        var mappedObject = optionalSecretKey
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

    private SecretKey retrieveOrCreateSecretKeyForSerialization(Object object, List<Field> fields) {
        var optionalField = findOptionalSecretKeyIdentifierField(fields);
        if (optionalField.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotation();
        }

        var secretKeyIdentifier = extractSecretKeyIdentifier(object, optionalField.get());
        var optionalSecretKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(secretKeyIdentifier);
        return optionalSecretKey.orElseThrow(() -> new EncryptionKeyDeletedException(secretKeyIdentifier));
    }

    private Optional<SecretKey> retrieveSecretKeyForDeserialization(Map<String, Object> encryptedMappedObject,
                                                                    Map<String, String> serializedFieldNameMapping,
                                                                    List<Field> fields) {
        var optionalField = findOptionalSecretKeyIdentifierField(fields);
        if (optionalField.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotation();
        }

        var secretKeyIdentifierKey = serializedFieldNameMapping.get(optionalField.get().getName().toLowerCase());
        if (secretKeyIdentifierKey == null) {
            throw new MissingSerializedEncryptionKeyIdentifierException();
        }

        var secretKeyIdentifier = encryptedMappedObject.get(secretKeyIdentifierKey).toString();
        if (secretKeyIdentifier == null || secretKeyIdentifier.isBlank()) {
            throw new MissingSerializedEncryptionKeyIdentifierException();
        }
        return cryptoShreddingKeyService.getExistingSecretKey(secretKeyIdentifier);
    }

    private Optional<Field> findOptionalSecretKeyIdentifierField(List<Field> fields) {
        return fields.stream()
                .filter(field -> field.getAnnotation(EncryptionKeyIdentifier.class) != null)
                .findFirst();
    }

    private String extractSecretKeyIdentifier(Object object, Field secretKeyIdentifierField) {
        secretKeyIdentifierField.setAccessible(true);
        try {
            return convertToString(secretKeyIdentifierField.get(object));
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

    private Map<String, Object> mapAndEncryptAnnotatedFields(Object object, List<Field> encryptedFields, SecretKey secretKey) {
        var mappedObject = objectMapper.convertValue(object, new TypeReference<HashMap<String, Object>>() {
        });
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(mappedObject);
        var base64EncodingAesEncrypter = aesEncrypterDecrypterFactory.createEncrypter();

        encryptedFields.forEach(field -> {
            var fieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var serializedClearText = wrappedSerializer.serialize(mappedObject.get(fieldKey), String.class);
            byte[] cipherText = base64EncodingAesEncrypter.encrypt(secretKey, serializedClearText.getData());
            mappedObject.put(fieldKey, Base64.getEncoder().encodeToString(cipherText));
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
                                                       SecretKey secretKey) {
        var base64EncodingAesEncrypter = aesEncrypterDecrypterFactory.createDecrypter();

        encryptedFields.forEach(field -> {
            var serializedFieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var cipherText = Base64.getDecoder().decode((String)encryptedMappedObject.get(serializedFieldKey));
            var cleartextSerializedFieldValue = base64EncodingAesEncrypter.decrypt(secretKey, cipherText);
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
