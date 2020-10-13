package engineering.everest.starterkit.axon.cryptoshredding.encryption;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;

@Configuration
public class EncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EncrypterDecrypterFactory.class)
    public EncrypterDecrypterFactory encrypterDecrypterFactory() {
        return new DefaultAesEncrypterDecrypterFactory();
    }

    @Bean
    @ConditionalOnMissingBean(engineering.everest.starterkit.axon.cryptoshredding.encryption.KeyGenerator.class)
    public KeyGenerator keyGenerator() throws NoSuchAlgorithmException {
        return new DefaultAesKeyGenerator();
    }
}
