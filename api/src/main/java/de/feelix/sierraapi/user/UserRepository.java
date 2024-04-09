package de.feelix.sierraapi.user;

import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<SierraUser> queryUserByUuid(UUID uuid);

    Optional<SierraUser> queryUserByEntityId(int id);

    Optional<SierraUser> queryUserByName(String name);
}
