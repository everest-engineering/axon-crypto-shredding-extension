package engineering.everest.axon.cryptoshredding.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MockitoExtension.class)
class DefaultDefaultAesEncrypterDecrypterFactoryTest {

    private DefaultAesEncrypterDecrypterFactory defaultAesEncrypterDecrypterFactory;

    @BeforeEach
    void setUp() {
        defaultAesEncrypterDecrypterFactory = new DefaultAesEncrypterDecrypterFactory();
    }

    @Test
    void createEncrypterReturnsNewInstances() {
        assertNotEquals(defaultAesEncrypterDecrypterFactory.createEncrypter(), defaultAesEncrypterDecrypterFactory.createEncrypter());
    }

    @Test
    void createDecrypterReturnsNewInstances() {
        assertNotEquals(defaultAesEncrypterDecrypterFactory.createDecrypter(), defaultAesEncrypterDecrypterFactory.createDecrypter());
    }
}
