package org.mcwonderland.uhc.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtraTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void deleteWorldRemovesLegacyAndPaper26WorldStorage() throws IOException {
        File serverRoot = temporaryFolder.newFolder("server");
        File legacyWorld = createFile(serverRoot, "uhc_world/region/r.0.0.mca");
        File paper26World = createFile(serverRoot, "world/dimensions/minecraft/uhc_world/region/r.0.0.mca");

        Extra.deleteWorld(serverRoot, "uhc_world");

        assertFalse(legacyWorld.getParentFile().getParentFile().exists());
        assertFalse(paper26World.getParentFile().getParentFile().exists());
        assertTrue(new File(serverRoot, "world/dimensions/minecraft").isDirectory());
    }

    @Test
    public void deleteWorldUsesConfiguredLevelNameForPaper26WorldStorage() throws IOException {
        File serverRoot = temporaryFolder.newFolder("server");
        Files.writeString(new File(serverRoot, "server.properties").toPath(), "level-name=main_world\n", StandardCharsets.UTF_8);
        File paper26World = createFile(serverRoot, "main_world/dimensions/minecraft/uhc_world/region/r.0.0.mca");

        Extra.deleteWorld(serverRoot, "uhc_world");

        assertFalse(paper26World.getParentFile().getParentFile().exists());
        assertTrue(new File(serverRoot, "main_world/dimensions/minecraft").isDirectory());
    }

    private static File createFile(File root, String relativePath) throws IOException {
        File file = new File(root, relativePath);
        assertTrue(file.getParentFile().mkdirs());
        assertTrue(file.createNewFile());
        return file;
    }
}
