package org.mcwonderland.uhc.update;

import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;

import java.io.File;

public class OldMenusCheck implements Updater {

    @Override
    public void check(String oldVer, String newVer) {
        if (newVer.startsWith("3")) {
            moveOldMenuYaml();
        }
    }

    private void moveOldMenuYaml() {
        File file = LegacyFoundationAdapter.getFile("menus.yml");

        if (file.exists())
            file.renameTo(LegacyFoundationAdapter.getOrMakeFile("舊版文件備份/menus.yml"));
    }

}
