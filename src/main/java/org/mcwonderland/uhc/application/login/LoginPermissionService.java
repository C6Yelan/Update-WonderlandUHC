package org.mcwonderland.uhc.application.login;

import org.mcwonderland.uhc.UHCPermission;

public interface LoginPermissionService {

    boolean hasPermission(LoginSubject subject, UHCPermission permission);
}
