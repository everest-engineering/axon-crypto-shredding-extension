package engineering.everest.axon.cryptoshredding.encryption;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultAesKeyGeneratorTest {

    public DefaultAesKeyGenerator defaultAesKeyGenerator;

    @Test
    public void keyGeneratorUsesDefaultAESAlgorithm() throws NoSuchAlgorithmException {
        defaultAesKeyGenerator = new DefaultAesKeyGenerator();

        var key = defaultAesKeyGenerator.generateKey();

        assertEquals("AES", key.getAlgorithm());
    }
}
