package org.mcwonderland.uhc.application.login;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerLoginConnection;

import java.util.Optional;
import java.util.UUID;

public final class LoginSubject {
    private final UUID uniqueId;
    private final String name;

    private LoginSubject(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public static Optional<LoginSubject> from(PlayerConnection connection) {
        PlayerProfile profile = profileFrom(connection);
        if (profile == null)
            return Optional.empty();

        String name = profile.getName();
        if (name == null || name.isBlank())
            return Optional.empty();

        return Optional.of(new LoginSubject(profile.getId(), name));
    }

    private static PlayerProfile profileFrom(PlayerConnection connection) {
        if (connection instanceof PlayerLoginConnection loginConnection) {
            PlayerProfile profile = loginConnection.getAuthenticatedProfile();
            return profile == null ? loginConnection.getUnsafeProfile() : profile;
        }

        if (connection instanceof PlayerConfigurationConnection configurationConnection)
            return configurationConnection.getProfile();

        return null;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }
}
