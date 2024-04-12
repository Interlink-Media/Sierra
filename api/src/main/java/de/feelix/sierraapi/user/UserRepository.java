package de.feelix.sierraapi.user;

import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.Optional;
import java.util.UUID;

/**
 * The UserRepository interface represents a repository for querying SierraUser objects based on different criteria.
 */
public interface UserRepository {

    /**
     * Retrieves a SierraUser object from the repository based on the specified UUID.
     *
     * @param uuid the UUID used to query the user
     * @return an Optional containing the SierraUser object if found, or an empty Optional if not found
     */
    Optional<SierraUser> queryUserByUuid(UUID uuid);

    /**
     * Retrieves a SierraUser object from the repository based on the specified entity ID.
     *
     * @param id the entity ID used to query the user
     * @return an Optional containing the SierraUser object if found, or an empty Optional if not found
     */
    Optional<SierraUser> queryUserByEntityId(int id);

    /**
     * Queries the UserRepository for a SierraUser object based on the given name.
     *
     * @param name the name used to query the user
     * @return an Optional containing the SierraUser object if found, or an empty Optional if not found
     */
    Optional<SierraUser> queryUserByName(String name);
}
