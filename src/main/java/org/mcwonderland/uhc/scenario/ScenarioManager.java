package org.mcwonderland.uhc.scenario;

import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.impl.block.*;
import org.mcwonderland.uhc.scenario.impl.consume.ScenarioAbsorptionLess;
import org.mcwonderland.uhc.scenario.impl.consume.ScenarioFoodNeophobia;
import org.mcwonderland.uhc.scenario.impl.consume.ScenarioPotionLess;
import org.mcwonderland.uhc.scenario.impl.consume.ScenarioSoup;
import org.mcwonderland.uhc.scenario.impl.damage.*;
import org.mcwonderland.uhc.scenario.impl.death.ScenarioNoClean;
import org.mcwonderland.uhc.scenario.impl.death.ScenarioShiftKill;
import org.mcwonderland.uhc.scenario.impl.death.ScenarioSwapInventory;
import org.mcwonderland.uhc.scenario.impl.death.ScenarioTimeBomb;
import org.mcwonderland.uhc.scenario.impl.disable.ScenarioBowLess;
import org.mcwonderland.uhc.scenario.impl.disable.ScenarioHorseLess;
import org.mcwonderland.uhc.scenario.impl.disable.ScenarioNoEnchant;
import org.mcwonderland.uhc.scenario.impl.disable.ScenarioRodLess;
import org.mcwonderland.uhc.scenario.impl.rush.ScenarioCutClean;
import org.mcwonderland.uhc.scenario.impl.rush.ScenarioFastSmelting;
import org.mcwonderland.uhc.scenario.impl.rush.ScenarioHastyBoys;
import org.mcwonderland.uhc.scenario.impl.special.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ScenarioManager {
    private final Map<String, Scenario> scenarios = new LinkedHashMap<>();
    private final Map<String, ScenarioRegistrationFailure> registrationFailures = new LinkedHashMap<>();
    private final Consumer<ScenarioRegistrationFailure> failureReporter;

    public ScenarioManager() {
        this(ScenarioManager::logRegistrationFailure);
    }

    public ScenarioManager(Consumer<ScenarioRegistrationFailure> failureReporter) {
        this.failureReporter = failureReporter;
    }

    public void register(Scenario scenario) {
        String name = scenario.getName();

        try {
            scenario.reload();
            this.scenarios.put(name, scenario);
            this.registrationFailures.remove(name);
        } catch (RuntimeException | LinkageError ex) {
            isolateScenario(name, scenario, "reload", ex);
        }
    }

    public void unregister(Scenario scenario) {
        this.scenarios.remove(scenario.getName());
        this.registrationFailures.remove(scenario.getName());
    }

    public void reloadAll() {
        new ArrayList<>(scenarios.values()).forEach(scenario -> {
            try {
                scenario.reload();
                this.registrationFailures.remove(scenario.getName());
            } catch (RuntimeException | LinkageError ex) {
                isolateScenario(scenario.getName(), scenario, "reload", ex);
            }
        });
    }

    public Scenario getScenario(ScenarioName name) {
        return scenarios.get(name.capitalize());
    }

    public Scenario getScenario(String name) {
        return scenarios.get(name);
    }

    public List<Scenario> getEnabledScenarios() {
        return getScenarios().stream()
                .filter(Scenario::isEnabled)
                .collect(Collectors.toList());
    }

    public Collection<Scenario> getScenarios() {
        return scenarios.values();
    }

    public Collection<ScenarioRegistrationFailure> getRegistrationFailures() {
        return Collections.unmodifiableCollection(registrationFailures.values());
    }

    public ScenarioRegistrationFailure getRegistrationFailure(String name) {
        return registrationFailures.get(name);
    }

    public boolean isScenarioAvailable(String name) {
        return scenarios.containsKey(name) && !registrationFailures.containsKey(name);
    }

    public boolean toggleScenario(Scenario scenario, boolean enabled) {
        String action = enabled ? "enable" : "disable";

        try {
            scenario.toggleEnabled(enabled);
            return true;
        } catch (RuntimeException | LinkageError ex) {
            isolateScenario(scenario.getName(), scenario, action, ex);
            return false;
        }
    }

    public void reopenEnabledScenarios() {
        new ArrayList<>(getEnabledScenarios()).forEach(scenario -> {
            if (toggleScenario(scenario, false))
                toggleScenario(scenario, true);
        });
    }

    public void registerDefaults() {
        registerDefault(ScenarioName.ABSORPTION_LESS, () -> new ScenarioAbsorptionLess(ScenarioName.ABSORPTION_LESS));
        registerDefault(ScenarioName.BACKPACK, () -> new ScenarioBackPack(ScenarioName.BACKPACK));
        registerDefault(ScenarioName.BENCH_BLITZ, () -> new ScenarioBenchBlitz(ScenarioName.BENCH_BLITZ));
        registerDefault(ScenarioName.BLOOD_DIAMONDS, () -> new ScenarioBloodDiamonds(ScenarioName.BLOOD_DIAMONDS));
        registerDefault(ScenarioName.BOW_LESS, () -> new ScenarioBowLess(ScenarioName.BOW_LESS));
        registerDefault(ScenarioName.CUT_CLEAN, () -> new ScenarioCutClean(ScenarioName.CUT_CLEAN));
        registerDefault(ScenarioName.DAMAGE_DOGERS, () -> new ScenarioDamageDogers(ScenarioName.DAMAGE_DOGERS));
        registerDefault(ScenarioName.DIAMOND_LESS, () -> new ScenarioDiamondLess(ScenarioName.DIAMOND_LESS));
        registerDefault(ScenarioName.DOUBLE_OR_NOTHING, () -> new ScenarioDoubleOrNothing(ScenarioName.DOUBLE_OR_NOTHING));
        registerDefault(ScenarioName.FAST_OBSIDIAN, () -> new ScenarioFastObsidian(ScenarioName.FAST_OBSIDIAN));
        registerDefault(ScenarioName.FAST_SMELTING, () -> new ScenarioFastSmelting(ScenarioName.FAST_SMELTING));
        registerDefault(ScenarioName.FRAGILE_RODS, () -> new ScenarioFragileRods(ScenarioName.FRAGILE_RODS));
        registerDefault(ScenarioName.FIRE_LESS, () -> new ScenarioFireLess(ScenarioName.FIRE_LESS));
        registerDefault(ScenarioName.FOOD_NEOPHOBIA, () -> new ScenarioFoodNeophobia(ScenarioName.FOOD_NEOPHOBIA));
        registerDefault(ScenarioName.GOLD_LESS, () -> new ScenarioGoldLess(ScenarioName.GOLD_LESS));
        registerDefault(ScenarioName.HASTY_BOYS, () -> new ScenarioHastyBoys(ScenarioName.HASTY_BOYS));
        registerDefault(ScenarioName.HORSE_LESS, () -> new ScenarioHorseLess(ScenarioName.HORSE_LESS));
        registerDefault(ScenarioName.IRON_MAN, () -> new ScenarioIronMan(ScenarioName.IRON_MAN));
        registerDefault(ScenarioName.LESS_BOW_DAMAGE, () -> new ScenarioLessBowDamage(ScenarioName.LESS_BOW_DAMAGE));
        registerDefault(ScenarioName.LIMITATIONS, () -> new ScenarioLimitations(ScenarioName.LIMITATIONS));
        registerDefault(ScenarioName.LUCKY_LEAVES, () -> new ScenarioLuckyLeaves(ScenarioName.LUCKY_LEAVES));
        registerDefault(ScenarioName.NO_CLEAN, () -> new ScenarioNoClean(ScenarioName.NO_CLEAN));
        registerDefault(ScenarioName.NO_ENCHANT, () -> new ScenarioNoEnchant(ScenarioName.NO_ENCHANT));
        registerDefault(ScenarioName.NO_FALL, () -> new ScenarioNoFall(ScenarioName.NO_FALL));
        registerDefault(ScenarioName.POTION_LESS, () -> new ScenarioPotionLess(ScenarioName.POTION_LESS));
        registerDefault(ScenarioName.ROD_LESS, () -> new ScenarioRodLess(ScenarioName.ROD_LESS));
        registerDefault(ScenarioName.SHIFT_KILL, () -> new ScenarioShiftKill(ScenarioName.SHIFT_KILL));
        registerDefault(ScenarioName.SILK_WEB, () -> new ScenarioSilkWeb(ScenarioName.SILK_WEB));
        registerDefault(ScenarioName.SOUP, () -> new ScenarioSoup(ScenarioName.SOUP));
        registerDefault(ScenarioName.SWAP_INVENTORY, () -> new ScenarioSwapInventory(ScenarioName.SWAP_INVENTORY));
        registerDefault(ScenarioName.SWITCHEROO, () -> new ScenarioSwitcheroo(ScenarioName.SWITCHEROO));
        registerDefault(ScenarioName.TIMBER, () -> new ScenarioTimber(ScenarioName.TIMBER));
        registerDefault(ScenarioName.TIME_BOMB, () -> new ScenarioTimeBomb(ScenarioName.TIME_BOMB));
        registerDefault(ScenarioName.TRIPLE_ARROW, () -> new ScenarioTripleArrow(ScenarioName.TRIPLE_ARROW));
        registerDefault(ScenarioName.TRIPLE_ORES, () -> new ScenarioTripleOres(ScenarioName.TRIPLE_ORES));
        registerDefault(ScenarioName.VANILLA_PLUS, () -> new ScenarioVanillaPlus(ScenarioName.VANILLA_PLUS));
        registerDefault(ScenarioName.VEIN_MINERS, () -> new ScenarioVeinMiners(ScenarioName.VEIN_MINERS));
        registerDefault(ScenarioName.ARMOR_VS_HEALTH, () -> new ScenarioArmorVsHealth(ScenarioName.ARMOR_VS_HEALTH));
    }

    private void registerDefault(ScenarioName scenarioName, Supplier<Scenario> scenarioFactory) {
        try {
            register(scenarioFactory.get());
        } catch (RuntimeException | LinkageError ex) {
            isolateScenario(scenarioName.capitalize(), null, "construct", ex);
        }
    }

    private void isolateScenario(String name, Scenario scenario, String action, Throwable cause) {
        this.scenarios.remove(name);

        if (scenario != null && scenario.isEnabled()) {
            try {
                scenario.disable();
            } catch (RuntimeException | LinkageError ignored) {
            }
        }

        ScenarioRegistrationFailure failure = new ScenarioRegistrationFailure(name, action, cause);
        this.registrationFailures.put(name, failure);
        reportFailure(failure);
    }

    private void reportFailure(ScenarioRegistrationFailure failure) {
        try {
            failureReporter.accept(failure);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void logRegistrationFailure(ScenarioRegistrationFailure failure) {
        PluginConsole.error(
                failure.getCause(),
                "Scenario '" + failure.getScenarioName() + "' was isolated during " + failure.getAction() + ".",
                "This scenario is unavailable, but plugin startup and other scenarios will continue."
        );
    }
}
