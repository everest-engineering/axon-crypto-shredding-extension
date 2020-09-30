package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MockitoExtension.class)
class EncrypterDecrypterFactoryTest {

    private EncrypterDecrypterFactory encrypterDecrypterFactory;

    @BeforeEach
    void setUp() {
        encrypterDecrypterFactory = new EncrypterDecrypterFactory();
    }

    @Test
    void createEncrypterReturnsNewInstances() {
        assertNotEquals(encrypterDecrypterFactory.createEncrypter(), encrypterDecrypterFactory.createEncrypter());
    }

    @Test
    void createDecrypterReturnsNewInstances() {
        assertNotEquals(encrypterDecrypterFactory.createDecrypter(), encrypterDecrypterFactory.createDecrypter());
    }
}