package engineering.everest.axon.cryptoshredding.persistence;

import engineering.everest.axon.cryptoshredding.TypeDifferentiatedSecretKeyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersistableSecretKeyJPARepository extends JpaRepository<PersistableSecretKey, TypeDifferentiatedSecretKeyId> {}
