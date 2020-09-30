package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MockitoExtension.class)
class AesEncrypterDecrypterFactoryTest {

    private AesEncrypterDecrypterFactory aesEncrypterDecrypterFactory;

    @BeforeEach
    void setUp() {
        aesEncrypterDecrypterFactory = new AesEncrypterDecrypterFactory();
    }

    @Test
    void createEncrypterReturnsNewInstances() {
        assertNotEquals(aesEncrypterDecrypterFactory.createEncrypter(), aesEncrypterDecrypterFactory.createEncrypter());
    }

    @Test
    void createDecrypterReturnsNewInstances() {
        assertNotEquals(aesEncrypterDecrypterFactory.createDecrypter(), aesEncrypterDecrypterFactory.createDecrypter());
    }
}