package txnflow.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import txnflow.auth_service.entity.AppUser;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByKeycloakUserId(String keycloakUserId);
}