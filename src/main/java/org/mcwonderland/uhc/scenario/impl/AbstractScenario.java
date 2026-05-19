package org.mcwonderland.uhc.scenario.impl;

import org.mcwonderland.uhc.platform.event.PluginEvents;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.api.event.scenario.ScenarioDisabledEvent;
import org.mcwonderland.uhc.api.event.scenario.ScenarioEnabledEvent;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.util.GameUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

@Setter
public abstract class AbstractScenario implements Scenario {

    @Setter(AccessLevel.PACKAGE)
    private String name;
    private ItemStack icon;

    private boolean enabled = false;
    private Collection<Listener> listeners;


    public AbstractScenario(
            String name,
            ItemStack icon) {
        this.name = name;
        this.icon = icon;
    }


    @Override
    public final void reload() {
        this.listeners = this.initListeners();

        if (this instanceof Listener)
            this.listeners.add(( Listener ) this);

        onReload();
    }

    @Override
    public final void enable() {
        onEnable();
        this.enabled = true;

        if (GameUtils.isGameStarted())
            this.listeners.forEach(PluginEvents::registerEvents);

        PluginEvents.callEvent(new ScenarioEnabledEvent(this));
    }

    @Override
    public final void disable() {
        onDisable();
        this.enabled = false;
        this.listeners.forEach(HandlerList::unregisterAll);
        PluginEvents.callEvent(new ScenarioDisabledEvent(this));
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }


    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final ItemStack getIcon() {
        return icon;
    }

    @Override
    public String getFancyName() {
        if (icon == null || icon.getItemMeta() == null)
            return name;

        Component displayName = icon.getItemMeta().displayName();
        return displayName == null ? name : PluginText.toMiniMessageString(displayName);
    }

    protected void onReload() {

    }

    protected void onEnable() {

    }

    protected void onDisable() {

    }

    protected Collection<Listener> initListeners() {
        return Lists.newArrayList();
    }
}
