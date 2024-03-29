package engineering.everest.axon.cryptoshredding.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.axon.cryptoshredding.CryptoShreddingKeyService;
import engineering.everest.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import engineering.everest.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import engineering.everest.axon.cryptoshredding.encryption.EncrypterDecrypterFactory;
import engineering.everest.axon.cryptoshredding.exceptions.DuplicateEncryptionKeyIdentifierFieldTagException;
import engineering.everest.axon.cryptoshredding.exceptions.EncryptionKeyDeletedException;
import engineering.everest.axon.cryptoshredding.exceptions.MissingEncryptionKeyIdentifierAnnotationException;
import engineering.everest.axon.cryptoshredding.exceptions.MissingSerializedEncryptionKeyIdentifierFieldException;
import engineering.everest.axon.cryptoshredding.exceptions.MissingTaggedEncryptionKeyIdentifierException;
import lombok.extern.slf4j.Slf4j;
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

import static java.util.stream.Collectors.toMap;

/**
 * A wrapper around the existing Axon serializers that intercepts fields annotated with {@code @EncryptedField}, encrypting them with
 * symmetric keys that are generated and stored alongside the Axon event log and saga store. Encryption keys are identified via the
 * {@code @EncryptionKeyIdentifier} annotation. This annotation accepts an optional keyType parameter that is used to differentiate between
 * identifiers when key uniqueness cannot be globally guaranteed (such as when using monotonically increasing integers).
 * <p>
 * A 256-bit AES (symmetric) key is generated for each {identifier, keyType} tuple. Each field annotated with {@code @EncryptedField} is
 * encrypted using an initialisation vector unique to that field. This initialisation vector is stored as part of the serialised field
 * payload.
 */
@Slf4j
public class CryptoShreddingSerializer implements Serializer {

    private final Serializer wrappedSerializer;
    private final CryptoShreddingKeyService cryptoShreddingKeyService;
    private final EncrypterDecrypterFactory encrypterDecrypterFactory;
    private final ObjectMapper objectMapper;
    private final DefaultValueProvider defaultValueProvider;
    private final KeyIdentifierToStringConverter keyIdentifierToStringConverter;

    public CryptoShreddingSerializer(@Qualifier("eventSerializer") Serializer wrappedSerializer,
                                     CryptoShreddingKeyService cryptoShreddingKeyService,
                                     EncrypterDecrypterFactory encrypterDecrypterFactory,
                                     ObjectMapper objectMapper,
                                     DefaultValueProvider defaultValueProvider,
                                     KeyIdentifierToStringConverter keyIdentifierToStringConverter) {
        this.wrappedSerializer = wrappedSerializer;
        this.cryptoShreddingKeyService = cryptoShreddingKeyService;
        this.encrypterDecrypterFactory = encrypterDecrypterFactory;
        this.objectMapper = objectMapper;
        this.defaultValueProvider = defaultValueProvider;
        this.keyIdentifierToStringConverter = keyIdentifierToStringConverter;
    }

    @Override
    public <T> SerializedObject<T> serialize(Object object, Class<T> expectedRepresentation) {
        var fields = List.of(object.getClass().getDeclaredFields());
        var encryptedFields = getEncryptedFields(fields);
        if (encryptedFields.isEmpty()) {
            return wrappedSerializer.serialize(object, expectedRepresentation);
        }

        var fieldToSecretKeyMapping = retrieveOrCreateSecretKeysForSerialization(object, fields);
        var encryptedMappedObject = mapAndEncryptAnnotatedFields(object, encryptedFields, fieldToSecretKeyMapping);
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
        var encryptedSerializedObject = new SimpleSerializedObject<>(
            serializedObject.getData(), serializedObject.getContentType(), encryptedSerializedType);
        Map<String, Object> encryptedMappedObject = wrappedSerializer.deserialize(encryptedSerializedObject);
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(encryptedMappedObject);
        var fieldTagToSecretKeyMapping = retrieveSecretKeysForDeserialization(encryptedMappedObject, serializedFieldNameMapping, fields);

        var mappedObject = decryptAnnotatedFields(encryptedMappedObject, serializedFieldNameMapping,
            encryptedFields, fieldTagToSecretKeyMapping);
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
            .toList();
    }

    private Map<String, SecretKey> retrieveOrCreateSecretKeysForSerialization(Object object, List<Field> fields) {
        var secretKeyIdentifierFields = findSecretKeyIdentifierFields(fields);
        if (secretKeyIdentifierFields.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotationException();
        }

        Map<String, SecretKey> fieldTagToSecretKeyMapping = new HashMap<>();
        secretKeyIdentifierFields.forEach(field -> {
            var secretKeyIdentifier = extractSecretKeyIdentifier(object, field);
            var optionalSecretKey = cryptoShreddingKeyService.getOrCreateSecretKeyUnlessDeleted(secretKeyIdentifier);
            var fieldTag = field.getAnnotation(EncryptionKeyIdentifier.class).tag();
            if (fieldTagToSecretKeyMapping.containsKey(fieldTag)) {
                throw new DuplicateEncryptionKeyIdentifierFieldTagException(field.getName(), fieldTag);
            }
            fieldTagToSecretKeyMapping.put(fieldTag, optionalSecretKey.orElseThrow(
                () -> new EncryptionKeyDeletedException(secretKeyIdentifier.getKeyId(), secretKeyIdentifier.getKeyType())));
        });

        return fieldTagToSecretKeyMapping;
    }

    private Map<String, Optional<SecretKey>> retrieveSecretKeysForDeserialization(Map<String, Object> encryptedMappedObject,
                                                                                  Map<String, String> serializedFieldNameMapping,
                                                                                  List<Field> fields) {
        var secretKeyIdentifierFields = findSecretKeyIdentifierFields(fields);
        if (secretKeyIdentifierFields.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotationException();
        }

        Map<String, Optional<SecretKey>> fieldTagToSecretKeyMapping = new HashMap<>();
        secretKeyIdentifierFields.forEach(field -> {
            var encryptionKeyIdentifierAnnotation = field.getAnnotation(EncryptionKeyIdentifier.class);
            var secretKeyIdentifierFieldName = serializedFieldNameMapping.get(field.getName().toLowerCase());
            if (secretKeyIdentifierFieldName == null) {
                throw new MissingSerializedEncryptionKeyIdentifierFieldException();
            }

            var secretKeyIdentifier = encryptedMappedObject.get(secretKeyIdentifierFieldName).toString();
            if (secretKeyIdentifier == null || secretKeyIdentifier.isBlank()) {
                throw new MissingSerializedEncryptionKeyIdentifierFieldException();
            }

            var secretKey = cryptoShreddingKeyService.getExistingSecretKey(
                new TypeDifferentiatedSecretKeyId(secretKeyIdentifier, encryptionKeyIdentifierAnnotation.keyType()));
            fieldTagToSecretKeyMapping.put(encryptionKeyIdentifierAnnotation.tag(), secretKey);
        });

        return fieldTagToSecretKeyMapping;
    }

    private List<Field> findSecretKeyIdentifierFields(List<Field> fields) {
        return fields.stream()
            .filter(field -> field.getAnnotation(EncryptionKeyIdentifier.class) != null)
            .toList();
    }

    private TypeDifferentiatedSecretKeyId extractSecretKeyIdentifier(Object object, Field secretKeyIdentifierField) {
        secretKeyIdentifierField.setAccessible(true);
        try {
            return new TypeDifferentiatedSecretKeyId(
                keyIdentifierToStringConverter.convertToString(secretKeyIdentifierField.get(object)),
                secretKeyIdentifierField.getAnnotation(EncryptionKeyIdentifier.class).keyType());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> mapAndEncryptAnnotatedFields(Object object,
                                                             List<Field> encryptedFields,
                                                             Map<String, SecretKey> fieldTagToSecretKeyMapping) {
        var mappedObject = objectMapper.convertValue(object, new TypeReference<HashMap<String, Object>>() {});
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(mappedObject);
        var encrypter = encrypterDecrypterFactory.createEncrypter();

        encryptedFields.forEach(field -> {
            var fieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var fieldTag = field.getAnnotation(EncryptedField.class).tag();
            if (!fieldTagToSecretKeyMapping.containsKey(fieldTag)) {
                throw new MissingTaggedEncryptionKeyIdentifierException(field.getName(), fieldTag);
            }
            var secretKey = fieldTagToSecretKeyMapping.get(fieldTag);
            var serializedClearText = wrappedSerializer.serialize(mappedObject.get(fieldKey), String.class);
            byte[] cipherText = encrypter.encrypt(secretKey, serializedClearText.getData());
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
                                                       Map<String, Optional<SecretKey>> fieldTagToSecretKeyMapping) {
        var decrypter = encrypterDecrypterFactory.createDecrypter();

        encryptedFields.forEach(field -> {
            var fieldTag = field.getAnnotation(EncryptedField.class).tag();
            if (!fieldTagToSecretKeyMapping.containsKey(fieldTag)) {
                throw new MissingTaggedEncryptionKeyIdentifierException(field.getName(), fieldTag);
            }
            var serializedFieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var cipherText = Base64.getDecoder().decode((String) encryptedMappedObject.get(serializedFieldKey));
            var optionalSecretKey = fieldTagToSecretKeyMapping.get(fieldTag);

            if (optionalSecretKey.isPresent()) {
                var cleartextSerializedFieldValue = decrypter.decrypt(optionalSecretKey.get(), cipherText);
                var deserializedFieldValue = wrappedSerializer.deserialize(
                    new SimpleSerializedObject<>(cleartextSerializedFieldValue, String.class, Object.class.getCanonicalName(), null));
                encryptedMappedObject.put(serializedFieldKey, deserializedFieldValue);
            } else {
                encryptedMappedObject.put(serializedFieldKey, defaultValueProvider.defaultValue(field.getType()));
            }
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
