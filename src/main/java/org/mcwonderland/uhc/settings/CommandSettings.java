package org.mcwonderland.uhc.settings;

import java.util.List;

public class CommandSettings extends PluginStaticConfig {

    public static void load() {
        loadStaticConfiguration(CommandSettings.class, UHCFiles.COMMANDS);
    }

    public static String CANT_EXECUTE_SELF;
    public static String INVALID_ARGUMENT;
    public static String LABEL_USAGE;
    public static String NO_CONSOLE;
    public static String RELOAD_FAIL;
    public static String RELOAD_SUCCESS;

    public static class Uhc {
        public static class Choose {
            public static List<String> KICK_INIT_MSG;
        }

        public static class Regen {
            public static List<String> CREATING_WORLD;
        }

        public static class SetHost {
            public static String HOST_CHANGED;
        }

        public static class SpecToggle {
            public static String ONLY_FOR_DEFAULT_SPECTATE_MODE;
            public static String GAMEMODE_TOGGLED;
        }
    }

    public static class Whitelist {
        public static String NAME_REQUIRED;
        public static String ADDED, REMOVED;
        public static String ALREADY_ADDED, ALREADY_REMOVED;
        public static String CLEARED;
        public static List<String> LIST;
    }

    public static class GiveAll {
        public static String INVALID_ITEM, INVALID_AMOUNT;
        public static String GIVEN;
    }

    public static class Border {
        public static String ONLY_NUMBER_BETWEEN;
        public static String ONLY_IN_TIMER_MODE;
    }

    public static class Respawn {
        public static String IS_PLAYING;
        public static String BROADCAST;
        public static List<String> RESPAWNED;
    }

    public static class SetSpawn {
        public static String SPAWN_SAVED;
    }

    public static class SendCoords {
        public static String FORMAT;
    }

    public static class Team {
        public static String ALREADY_HAS_ONE;
        public static String NOT_OWNER;
        public static String PLAYER_NOT_IN_TEAM;
        public static String PLAYER_HAS_NO_TEAM;
        public static String YOU_DONT_HAVE_TEAM;

        public static class Invite {
            public static String CLICK_HERE;
            public static String ALREADY_IN_YOUR_TEAM;
            public static List<String> INVITED;
            public static List<String> INVITATION_MESSAGES;
        }

        public static class Join {
            public static String NO_INVITATION;
        }

        public static class Public {
            public static String OPENED, CLOSED;
        }

        public static class Kick {
            public static List<String> KICKED;
        }

        public static class Create {
            public static List<String> CREATED;
        }

        public static class Chat {
            public static String CANT_USE;
            public static String JOINED, QUITTED;
        }
    }

    public static class Config {
        public static List<String> MESSAGES;
    }

    public static class TopKills {
        public static List<String> MESSAGES;
    }

    public static class TeamList {
        public static List<String> MESSAGES;
    }
}
