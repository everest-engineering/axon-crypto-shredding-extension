package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DefaultAesEncrypterDecrypterIntegrationTest {

    private static final String TINY_MESSAGE = "42";
    private static final String PLAIN_TEXT_MESSAGE = "The quick brown fox jumped ship. This is a long payload by design. " +
            "Please keep it such so that we have confidence in our ability to handle long messages. Lorem ipsum dolor sit amet, " +
            "consectetur adipiscing elit. Nunc sit amet nulla id lacus vulputate fringilla at sed est. Sed viverra rhoncus " +
            "justo, id sagittis purus tristique sit amet. Sed ornare porta magna, ut egestas leo accumsan vel. Class aptent " +
            "taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Proin eu est ullamcorper, pharetra " +
            "ex quis, scelerisque ligula. Pellentesque porta metus ut nulla volutpat fringilla. Mauris dapibus vel orci sit amet " +
            "consequat. Curabitur scelerisque augue nec aliquam ultrices. Nulla pellentesque, eros et tincidunt porta, nibh " +
            "eros eleifend odio, ut varius turpis eros at sapien. Praesent sed massa vel purus faucibus pellentesque placerat " +
            "sit amet ante. Cras suscipit fermentum congue. Aenean congue felis est, sit amet convallis diam lacinia eget. " +
            "Donec at blandit est.\n" +
            "\n" +
            "Nam ac velit eu ligula semper laoreet. Cras ut justo nec neque vestibulum tempus. Phasellus id scelerisque risus, " +
            "vitae malesuada nibh. Donec lacus lorem, molestie eget erat sed, bibendum commodo mauris. Cras ultricies volutpat " +
            "eros. Ut ante sapien, tincidunt at augue nec, vestibulum congue urna. Fusce at congue orci, vel tempor justo. " +
            "Nullam vitae pretium erat. Ut ut diam risus. Maecenas mauris ligula, pretium ac lectus vitae, tincidunt venenatis mi. " +
            "Sed odio nisi, placerat id lectus non, condimentum ultrices arcu.";

    private DefaultAesKeyGenerator defaultAesKeyGenerator;
    private DefaultAesEncrypter base64EncodingDefaultAesEncrypter;
    private DefaultAesDecrypter defaultAesDecrypter;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        var secureRandom = new SecureRandom();
        defaultAesKeyGenerator = new DefaultAesKeyGenerator();
        base64EncodingDefaultAesEncrypter = new DefaultAesEncrypter(secureRandom);
        defaultAesDecrypter = new DefaultAesDecrypter(secureRandom);
    }

    @Test
    void willDecryptItsOwnEncryptedMessages() {
        var secretKey = defaultAesKeyGenerator.generateKey();
        var encodedCipherText = base64EncodingDefaultAesEncrypter.encrypt(secretKey, PLAIN_TEXT_MESSAGE);
        var decodedPlainText = defaultAesDecrypter.decrypt(secretKey, encodedCipherText);

        assertEquals(PLAIN_TEXT_MESSAGE, decodedPlainText);
    }

    @Test
    void paddingWorksForTinyMessages() {
        var secretKey = defaultAesKeyGenerator.generateKey();
        var encodedCipherText = base64EncodingDefaultAesEncrypter.encrypt(secretKey, TINY_MESSAGE);
        var decodedPlainText = defaultAesDecrypter.decrypt(secretKey, encodedCipherText);

        assertEquals(TINY_MESSAGE, decodedPlainText);
    }

    @Test
    void encrypt_WillFail_WhenEncryptionKeyIsInvalid() {
        SecretKeySpec invalidAlgorithm = new SecretKeySpec("blah".getBytes(), "invalid algorithm");
        assertThrows(RuntimeException.class, () -> base64EncodingDefaultAesEncrypter.encrypt(invalidAlgorithm, PLAIN_TEXT_MESSAGE), PLAIN_TEXT_MESSAGE);
    }

    @Test
    void decrypt_WillFail_WhenDecryptionKeyIsInvalid() {
        var encodedCipherText = base64EncodingDefaultAesEncrypter.encrypt(defaultAesKeyGenerator.generateKey(), PLAIN_TEXT_MESSAGE);

        assertThrows(RuntimeException.class, () -> defaultAesDecrypter.decrypt(defaultAesKeyGenerator.generateKey(), encodedCipherText));
    }
}
