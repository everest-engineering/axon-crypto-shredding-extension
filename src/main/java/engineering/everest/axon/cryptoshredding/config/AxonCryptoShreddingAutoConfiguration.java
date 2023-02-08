package engineering.everest.axon.cryptoshredding.config;

import engineering.everest.axon.cryptoshredding.CryptoShreddingKeyService;
import engineering.everest.axon.cryptoshredding.encryption.DefaultAesEncrypterDecrypterFactory;
import engineering.everest.axon.cryptoshredding.encryption.DefaultAesKeyGenerator;
import engineering.everest.axon.cryptoshredding.encryption.EncrypterDecrypterFactory;
import engineering.everest.axon.cryptoshredding.encryption.KeyGenerator;
import engineering.everest.axon.cryptoshredding.persistence.SecretKeyRepository;
import engineering.everest.axon.cryptoshredding.serialization.DefaultValueProvider;
import engineering.everest.axon.cryptoshredding.serialization.KeyIdentifierToStringConverter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;

@Configuration
@AutoConfigureBefore(name = { "org.axonframework.springboot.autoconfig.AxonAutoConfiguration" })
public class AxonCryptoShreddingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EncrypterDecrypterFactory.class)
    public EncrypterDecrypterFactory encrypterDecrypterFactory() {
        return new DefaultAesEncrypterDecrypterFactory();
    }

    @Bean
    @ConditionalOnMissingBean(KeyGenerator.class)
    public KeyGenerator keyGenerator() throws NoSuchAlgorithmException {
        return new DefaultAesKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(DefaultValueProvider.class)
    public DefaultValueProvider defaultValueProvider() {
        return new DefaultValueProvider();
    }

    @Bean
    @ConditionalOnMissingBean(KeyIdentifierToStringConverter.class)
    public KeyIdentifierToStringConverter keyIdentifierToStringConverter() {
        return new KeyIdentifierToStringConverter();
    }

    @Bean
    @ConditionalOnMissingBean(CryptoShreddingKeyService.class)
    public CryptoShreddingKeyService cryptoShreddingKeyService(SecretKeyRepository secretKeyRepository,
                                                               KeyGenerator keyGenerator) {
        return new CryptoShreddingKeyService(secretKeyRepository, keyGenerator);
    }
}
