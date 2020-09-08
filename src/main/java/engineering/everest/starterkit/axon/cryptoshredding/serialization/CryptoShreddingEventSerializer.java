package engineering.everest.starterkit.axon.cryptoshredding.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptedField;
import engineering.everest.starterkit.axon.cryptoshredding.annotations.EncryptionKeyIdentifier;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyRecordException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingEncryptionKeyIdentifierAnnotation;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.MissingSerializedEncryptionKeyIdentifierException;
import engineering.everest.starterkit.axon.cryptoshredding.exceptions.UnsupportedEncryptionKeyIdentifierTypeException;
import engineering.everest.starterkit.axon.cryptoshredding.persistence.EncryptionKeyRepository;
import org.axonframework.serialization.Converter;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SerializedType;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class CryptoShreddingEventSerializer implements Serializer {

    private final Serializer wrappedSerializer;
    private final EncryptionKeyGenerator encryptionKeyGenerator;
    private final EncryptionKeyRepository encryptionKeyRepository;
    private final ObjectMapper objectMapper;

    public CryptoShreddingEventSerializer(Serializer wrappedSerializer,
                                          EncryptionKeyGenerator encryptionKeyGenerator,
                                          EncryptionKeyRepository encryptionKeyRepository,
                                          ObjectMapper objectMapper) {
        this.wrappedSerializer = wrappedSerializer;
        this.encryptionKeyGenerator = encryptionKeyGenerator;
        this.encryptionKeyRepository = encryptionKeyRepository;
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
        var objectToSerialise = encryptionKey.isPresent()
                ? encryptAnnotatedFields(object, encryptedFields, encryptionKey.get())
                : object;  // TODO should log an error; we have an event that is being recorded for an entity that has been deleted from the system
        return wrappedSerializer.serialize(objectToSerialise, expectedRepresentation);
    }

    @Override
    public <T> boolean canSerializeTo(Class<T> expectedRepresentation) {
        return wrappedSerializer.canSerializeTo(expectedRepresentation);
    }

    @Override
    public <S, T> T deserialize(SerializedObject<S> serializedObject) {
        List<Field> fields;
        Class<?> classToDeserializeTo;
        try {
            classToDeserializeTo = Class.forName(serializedObject.getType().getName());
            fields = List.of(classToDeserializeTo.getDeclaredFields());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        var encryptedFields = getEncryptedFields(fields);
        if (encryptedFields.isEmpty()) {
            return wrappedSerializer.deserialize(serializedObject);
        }

        var encryptedSerializedType = new SimpleSerializedType(HashMap.class.getCanonicalName(), null);
        var encryptedSerializedObject = new SimpleSerializedObject<>((String) serializedObject.getData(), String.class, encryptedSerializedType);
        Map<String, Object> encryptedMappedObject = wrappedSerializer.deserialize(encryptedSerializedObject);
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(encryptedMappedObject);
        var optionalEncryptionKey = retrieveEncryptionKeyForDeserialization(encryptedMappedObject, serializedFieldNameMapping, fields);
        var decryptedObject = decryptAnnotatedFields(encryptedMappedObject, serializedFieldNameMapping, encryptedFields, optionalEncryptionKey.orElse(null));

        var decryptedEvent = objectMapper.convertValue(decryptedObject, classToDeserializeTo);


//        wrappedSerializer.deserialize(serializedObject.)
        T object = wrappedSerializer.deserialize(serializedObject);

//        var fields = object.getClass().getDeclaredFields();
//        var encryptedFields = getEncryptedFields(fields);
//        if (encryptedFields.isEmpty()) {
//            return object;
//        }
//
//        var encryptionKey = getEncryptionKeyOrApplyMissingRecordStrategy(object, fields, (encryptionKeyIdentifier) -> {
//            // Key was deleted entirely which should never happen as the absence of a record is an indication
//            // that we are yet to store the key
//            throw new MissingEncryptionKeyException(encryptionKeyIdentifier);
//        });
//        encryptionKey.ifPresent(key -> decryptAnnotatedFields(object, encryptedFields, key));
        return object;
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

    private Optional<String> retrieveOrCreateEncryptionKeyForSerialization(Object object, List<Field> fields) {
        var optionalField = findOptionalEncryptionKeyIdentifierField(fields);
        if (optionalField.isEmpty()) {
            return Optional.empty();
        }

        var encryptionKeyIdentifier = extractEncryptionKeyIdentifier(object, optionalField.get());
        var optionalEncryptionKey = encryptionKeyRepository.findById(encryptionKeyIdentifier);
        var encryptionKey = optionalEncryptionKey.isEmpty()
                ? encryptionKeyRepository.create(encryptionKeyIdentifier, encryptionKeyGenerator.generateKey())
                : optionalEncryptionKey.get().getKey();
        return encryptionKey == null ? Optional.empty() : Optional.of(encryptionKey);
    }

    private Optional<String> retrieveEncryptionKeyForDeserialization(Map<String, Object> encryptedMappedObject,
                                                                     Map<String, String> serializedFieldNameMapping,
                                                                     List<Field> fields) {
        var optionalField = findOptionalEncryptionKeyIdentifierField(fields);
        if (optionalField.isEmpty()) {
            throw new MissingEncryptionKeyIdentifierAnnotation();
        }

        var encryptionKeyIdentifierKey = serializedFieldNameMapping.get(optionalField.get().getName().toLowerCase());
        var encryptionKeyIdentifier = encryptedMappedObject.get(encryptionKeyIdentifierKey).toString();
        if (encryptionKeyIdentifier == null) {
            throw new MissingSerializedEncryptionKeyIdentifierException();
        }

        var optionalPersistableEncryptionKey = encryptionKeyRepository.findById(encryptionKeyIdentifier);
        var encryptionKey = optionalPersistableEncryptionKey
                .orElseThrow(() -> new MissingEncryptionKeyRecordException(encryptionKeyIdentifier))
                .getKey();

        return encryptionKey == null ? Optional.empty() : Optional.of(encryptionKey);
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
        if (object instanceof String || object instanceof UUID || object instanceof Long) {
            return object.toString();
        }
        throw new UnsupportedEncryptionKeyIdentifierTypeException(object.toString());
    }

    private Object encryptAnnotatedFields(Object object, List<Field> encryptedFields, String encryptionKey) {
        var mappedObject = objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
        var serializedFieldNameMapping = buildFieldNamingSerializationStrategyIndependentMapping(mappedObject);

        encryptedFields.forEach(field -> {
            var fieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
            var fieldValue = mappedObject.get(fieldKey);
            var serializedFieldValue = wrappedSerializer.serialize(fieldValue, String.class);
            var encodedFieldValue = Base64.getEncoder().encodeToString(serializedFieldValue.getData().getBytes()); // TODO
            mappedObject.put(fieldKey, encodedFieldValue);
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
                                                       String optionalEncryptionKey) {

        encryptedFields.forEach(field -> {
            var serializedFieldKey = serializedFieldNameMapping.get(field.getName().toLowerCase());
//            var objectFieldName = objectFieldNameMapping.get(field.getName().toLowerCase());
            var encryptedFieldValue = (String) encryptedMappedObject.get(serializedFieldKey);
            var decryptedFieldValue = new String(Base64.getDecoder().decode(encryptedFieldValue)); // TODO
//            var tmp = wrappedSerializer.deserialize(fieldValue, String.class);
            encryptedMappedObject.put(serializedFieldKey, decryptedFieldValue);
        });

        return encryptedMappedObject;
    }
}
