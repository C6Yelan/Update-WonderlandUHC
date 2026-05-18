package org.mcwonderland.uhc.game;

import org.mcwonderland.uhc.platform.event.PluginEvents;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.mcwonderland.uhc.api.event.GameChangeSettingsEvent;
import org.mcwonderland.uhc.application.match.EndMatchUseCase;
import org.mcwonderland.uhc.application.match.MatchTransition;
import org.mcwonderland.uhc.application.match.MatchTransitionResult;
import org.mcwonderland.uhc.application.match.MatchTransitionUseCase;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.core.match.LegacyMatchSettingsMapper;
import org.mcwonderland.uhc.core.match.MatchRepository;
import org.mcwonderland.uhc.core.match.MatchSettings;
import org.mcwonderland.uhc.core.match.UhcMatch;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.state.GameState;
import org.mcwonderland.uhc.game.state.playing.PlayingState;
import org.mcwonderland.uhc.game.state.preparing.PreparingState;
import org.mcwonderland.uhc.game.state.starting.PreStartState;
import org.mcwonderland.uhc.game.state.starting.TeleportingState;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mineacademy.fo.collection.PlayerCollection;

import java.util.*;


@Getter
@Setter
public class Game {
    @Getter
    private static Game game = new Game();

    @Getter(AccessLevel.NONE)
    private final MatchRepository matchRepository = new MatchRepository();
    @Getter(AccessLevel.NONE)
    private final MatchTransitionUseCase matchTransitionUseCase = new MatchTransitionUseCase();
    @Getter(AccessLevel.NONE)
    private final EndMatchUseCase endMatchUseCase = new EndMatchUseCase();
    private UHCGameSettings settings = UHCGameSettings.defaultSettings();
    private String host = "";
    private int allPlayers;
    private int currentBorder = 0;
    private MatchCenter matchCenter = createDefaultMatchCenter();
    private boolean centerCleaner;
    private boolean damageEnabled, finalHealEnabled, pvpEnabled;

    private PlayerCollection whiteList = new PlayerCollection();
    private Map<UUID, InvinciblePlayer> invinciblePlayers = new HashMap<>();

    //new
    private Queue<GameState> states = new LinkedList<>();
    private GameState currentState;

    private Game() {
        this.states.add(new PreparingState(StateName.WAITING));
        this.states.add(new TeleportingState(StateName.TELEPORTING));
        this.states.add(new PreStartState(StateName.PRE_START));
        this.states.add(new PlayingState(StateName.PLAYING));

        this.currentState = this.states.remove();
        this.currentState.init();
        this.matchRepository.setActiveMatch(UhcMatch.create(LegacyMatchSettingsMapper.fromGameSettings(this.settings)));
    }

    public static UHCGameSettings getSettings() {
        return game.settings;
    }

    public static void changeSettings(UHCGameSettings newSettings) {
        MatchSettings matchSettings = LegacyMatchSettingsMapper.fromGameSettings(newSettings);

        game.settings = newSettings;
        game.matchCenter = game.withCurrentBorderSize(game.matchCenter);
        game.getActiveMatch().updateSettings(matchSettings);

        PluginEvents.callEvent(new GameChangeSettingsEvent(newSettings));
    }

    public MatchCenter getMatchCenter() {
        if (matchCenter == null)
            matchCenter = createDefaultMatchCenter();

        return matchCenter;
    }

    public void setMatchCenter(MatchCenter matchCenter) {
        this.matchCenter = matchCenter == null ? createDefaultMatchCenter() : matchCenter;
    }

    private MatchCenter withCurrentBorderSize(MatchCenter center) {
        MatchCenter current = center == null ? createDefaultMatchCenter() : center;
        return new MatchCenter(current.getX(), current.getZ(), currentInitialBorderSize());
    }

    private MatchCenter createDefaultMatchCenter() {
        return new MatchCenter(0, 0, currentInitialBorderSize());
    }

    private int currentInitialBorderSize() {
        if (settings == null || settings.getBorderSettings() == null || settings.getBorderSettings().getInitialBorder() == null)
            return 2000;

        return settings.getBorderSettings().getInitialBorder();
    }

    public UhcMatch getActiveMatch() {
        return this.matchRepository.getActiveMatch();
    }

    public void nextState() {
        MatchTransition transition = LegacyGameStateTransitions.transitionFor(this.currentState.getName());

        this.currentState.end();
        this.currentState = states.remove();
        this.currentState.init();

        MatchTransitionResult transitionResult = this.matchTransitionUseCase.apply(this.getActiveMatch(), transition);

        if (!transitionResult.isSuccess())
            throw new IllegalStateException(transitionResult.getFailureReason());

        GameTimerRunnable.totalSecond = 0;
        GameTimerRunnable.tick = 0;

        this.currentState.start();
    }

    public void endMatch() {
        MatchTransitionResult transitionResult = this.endMatchUseCase.end(this.getActiveMatch());

        if (!transitionResult.isSuccess())
            throw new IllegalStateException(transitionResult.getFailureReason());
    }

    public StateName getCurrentStateName() {
        return this.currentState.getName();
    }
}
