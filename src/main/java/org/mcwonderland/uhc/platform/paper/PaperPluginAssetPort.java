package org.mcwonderland.uhc.platform.paper;

import org.mcwonderland.uhc.port.PluginAssetPort;
import org.mcwonderland.uhc.util.Extra;

public final class PaperPluginAssetPort implements PluginAssetPort {

    @Override
    public void registerRecipes() {
        Extra.createHead();
    }
}
