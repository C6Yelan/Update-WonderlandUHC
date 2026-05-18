package org.mcwonderland.uhc.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.Result;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.login.LoginPermissionService;
import org.mcwonderland.uhc.application.login.LoginSubject;
import org.mcwonderland.uhc.platform.console.PluginConsole;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class LuckPermsLoginPermissionService implements LoginPermissionService {
    private static final long USER_LOAD_TIMEOUT_SECONDS = 3L;

    private final LuckPerms luckPerms;

    public LuckPermsLoginPermissionService() {
        this(LuckPermsProvider.get());
    }

    LuckPermsLoginPermissionService(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @Override
    public boolean hasPermission(LoginSubject subject, UHCPermission permission) {
        UUID uniqueId = subject.getUniqueId();
        if (uniqueId == null)
            return false;

        try {
            User user = luckPerms.getUserManager()
                    .loadUser(uniqueId, subject.getName())
                    .orTimeout(USER_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
            QueryOptions queryOptions = luckPerms.getContextManager().getStaticQueryOptions();
            Result<Tristate, Node> permissionResult = user.getCachedData()
                    .getPermissionData(queryOptions)
                    .queryPermission(permission.toString());

            Tristate result = permissionResult.result();
            if (result == Tristate.TRUE)
                return true;

            if (result == Tristate.FALSE && permissionResult.node() != null)
                return false;

            return isOperator(subject) || result.asBoolean();
        } catch (CompletionException exception) {
            logFailure(subject, permission, exception.getCause() == null ? exception : exception.getCause());
            return false;
        } catch (RuntimeException exception) {
            logFailure(subject, permission, exception);
            return false;
        }
    }

    private void logFailure(LoginSubject subject, UHCPermission permission, Throwable throwable) {
        PluginConsole.error(throwable,
                "Failed to query LuckPerms permission '" + permission + "' for " + subject.getName() + ".",
                "Login gate will fail closed for this permission check.");
    }

    private boolean isOperator(LoginSubject subject) {
        UUID uniqueId = subject.getUniqueId();
        String name = subject.getName();

        for (OfflinePlayer operator : Bukkit.getOperators()) {
            if (uniqueId != null && uniqueId.equals(operator.getUniqueId()))
                return true;

            String operatorName = operator.getName();
            if (operatorName != null && operatorName.equalsIgnoreCase(name))
                return true;
        }

        return false;
    }
}
