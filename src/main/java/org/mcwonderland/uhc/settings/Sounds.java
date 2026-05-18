package org.mcwonderland.uhc.settings;

import org.mcwonderland.uhc.platform.sound.PluginSound;

public class Sounds extends PluginStaticConfig {

    public static void load() {
        loadStaticConfiguration(Sounds.class, UHCFiles.SOUNDS);
    }

    public static class Commands {
        public static PluginSound SPEC_TOGGLE;
        public static PluginSound RESPAWN;
        public static PluginSound SET_SPAWN;
        public static PluginSound STAFF_ON;
        public static PluginSound STAFF_OFF;
        public static PluginSound TOP_KILLS;
        public static PluginSound TEAM_CHAT_ON;
        public static PluginSound TEAM_CHAT_OFF;
        public static PluginSound TEAM_CREATED;
        public static PluginSound TEAM_INFO;
        public static PluginSound SEND_COORDS;
    }

    public static class Host {
        public static PluginSound START_CREATING_WORLD;
        public static PluginSound WORLD_CREATED;
        public static PluginSound INVENTORY_EDITED;
        public static PluginSound GOLDEN_HEAD_CREATED;
        public static PluginSound CLEAR_ENABLED_SCENARIOS;
        public static PluginSound SCENARIO_TOGGLED;
    }

    public static class Game {
        public static PluginSound INVINCIBLE_END;
        public static PluginSound DEATH;
        public static PluginSound ITEM_DISABLED;
        public static PluginSound CANT_JOIN_NETHER;
        public static PluginSound WIN;
    }

    public static class Countdown {

        public static class Lobby {
            public static PluginSound START;
            public static PluginSound TICK;
            public static PluginSound RUN;
        }

        public static class Start {
            public static PluginSound TICK;
            public static PluginSound RUN;
        }

        public static class Border {
            public static PluginSound TICK;
            public static PluginSound RUN;
        }

        public static class Damage {
            public static PluginSound TICK;
            public static PluginSound RUN;
        }

        public static class Pvp {
            public static PluginSound TICK;
            public static PluginSound RUN;
        }

        public static class FinalHeal {
            public static PluginSound TICK;
            public static PluginSound RUN;
        }

        public static class NetherClose {
            public static PluginSound TICK;
            public static PluginSound RUN;
        }
    }

    public static class Tutorial {
        public static PluginSound FINISHED;
        public static PluginSound NEXT_SECTION;
        public static PluginSound CANCELLED;
    }

}
