package org.mcwonderland.uhc.update;

import org.mcwonderland.uhc.WonderlandUHC;

import java.io.File;

public class OldMenusCheck implements Updater {

    @Override
    public void check(String oldVer, String newVer) {
        if (newVer.startsWith("3")) {
            moveOldMenuYaml();
        }
    }

    private void moveOldMenuYaml() {
        File file = new File(WonderlandUHC.getInstance().getDataFolder(), "menus.yml");

        if (!file.exists())
            return;

        File backup = new File(WonderlandUHC.getInstance().getDataFolder(), "舊版文件備份/menus.yml");
        File backupDirectory = backup.getParentFile();
        if (backupDirectory != null)
            backupDirectory.mkdirs();

        file.renameTo(backup);
    }

}
