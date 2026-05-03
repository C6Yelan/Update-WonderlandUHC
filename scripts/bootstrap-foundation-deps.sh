#!/usr/bin/env bash
# [維護] 準備 lib-foundation 需要的本機 Maven 相容依賴與別名。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_M2_REPO="${LOCAL_M2_REPO:-$ROOT_DIR/.m2-local}"
USER_M2_REPO="$HOME/.m2/repository"
TMP_DIR="${TMPDIR:-/tmp}/foundation-aliases.$$"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$TMP_DIR" "$LOCAL_M2_REPO"

ALIAS_SPECS=(
  "AuthMe:5.6.0-SNAPSHOT-2530"
  "CitizensAPI:2.0.29-b839"
  "CMI-API:9.0.0.0"
  "DiscordSRV:1.26.0-SNAPSHOT-0824719"
  "EssentialsX:2.20.0-SNAPSHOT-1375"
  "FactionsUUID:1.6.9.5-U0.6.11-b287"
  "mcMMO:2.1.213"
  "Multiverse-Core:4.3.2-SNAPSHOT-870"
  "PlaceholderAPI:2.11.1"
  "ProtocolLib:5.0.0-SNAPSHOT-586"
  "Residence:5.0.1.6"
  "SimpleClans:2.16.2-SNAPSHOT-283"
  "Towny:0.98.1.12"
  "TownyChat:0.96"
  "WorldEdit:7.2.10"
  "WorldGuard:7.0.7"
)

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
}

alias_jar_path() {
  local artifact_id="$1"
  local version="$2"
  echo "$LOCAL_M2_REPO/org/mineacademy/plugin/$artifact_id/$version/${artifact_id}-${version}.jar"
}

alias_installed() {
  local artifact_id="$1"
  local version="$2"
  [[ -f "$(alias_jar_path "$artifact_id" "$version")" ]]
}

copy_alias_from_user_repo() {
  local artifact_id="$1"
  local version="$2"
  local source_dir="$USER_M2_REPO/org/mineacademy/plugin/$artifact_id/$version"
  local target_dir="$LOCAL_M2_REPO/org/mineacademy/plugin/$artifact_id/$version"

  if [[ -d "$target_dir" || ! -d "$source_dir" ]]; then
    return 0
  fi

  mkdir -p "$(dirname "$target_dir")"
  cp -a "$source_dir" "$target_dir"
}

copy_installed_aliases() {
  local spec artifact_id version
  for spec in "${ALIAS_SPECS[@]}"; do
    IFS=':' read -r artifact_id version <<<"$spec"
    copy_alias_from_user_repo "$artifact_id" "$version"
  done
}

all_aliases_installed() {
  local spec artifact_id version
  for spec in "${ALIAS_SPECS[@]}"; do
    IFS=':' read -r artifact_id version <<<"$spec"
    if ! alias_installed "$artifact_id" "$version"; then
      return 1
    fi
  done

  return 0
}

extract_snapshot_jar_version() {
  local metadata="$1"
  local compact version
  compact=$(tr '\n' ' ' <<<"$metadata")
  version=$(sed -n 's:.*<snapshotVersion><extension>jar</extension><value>\([^<]*\)</value>.*:\1:p' <<<"$compact")

  if [[ -n "$version" ]]; then
    echo "$version"
    return 0
  fi

  version=$(rg -o '<value>[^<]+' <<<"$compact" | head -n1 | sed 's/<value>//')
  echo "$version"
}

download_file() {
  local output="$1"
  local url="$2"
  echo "Downloading $(basename "$output")"
  curl -sSL --fail -o "$output" "$url"
}

install_alias() {
  local artifact_id="$1"
  local version="$2"
  local jar_path="$3"

  echo "Installing alias org.mineacademy.plugin:${artifact_id}:${version}"
  mvn -q -Dmaven.repo.local="$LOCAL_M2_REPO" install:install-file \
    -Dfile="$jar_path" \
    -DgroupId=org.mineacademy.plugin \
    -DartifactId="$artifact_id" \
    -Dversion="$version" \
    -Dpackaging=jar \
    -DgeneratePom=true >/dev/null
}

build_empty_jar() {
  local jar_path="$1"
  local empty_dir="$TMP_DIR/empty-dir"
  mkdir -p "$empty_dir"
  jar cf "$jar_path" -C "$empty_dir" .
}

build_cmi_stub() {
  local spigot_api_jar="$1"
  local src_dir="$TMP_DIR/cmi-src"
  local classes_dir="$TMP_DIR/cmi-classes"
  local jar_path="$TMP_DIR/cmi-api-stub.jar"

  mkdir -p "$src_dir/com/Zrips/CMI/Containers"
  mkdir -p "$src_dir/com/Zrips/CMI/Modules/TabList"
  mkdir -p "$src_dir/com/Zrips/CMI"
  mkdir -p "$classes_dir"

  cat >"$src_dir/com/Zrips/CMI/CMI.java" <<'EOF'
package com.Zrips.CMI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.TabList.TabListManager;

public final class CMI {
    private static final CMI INSTANCE = new CMI();
    private final PlayerManager playerManager = new PlayerManager();
    private final TabListManager tabListManager = new TabListManager();

    public static CMI getInstance() {
        return INSTANCE;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public static final class PlayerManager {
        private final Map<UUID, CMIUser> users = new HashMap<>();

        public CMIUser getUser(Player player) {
            return player == null ? null : getUser(player.getUniqueId());
        }

        public CMIUser getUser(UUID uuid) {
            if (uuid == null) {
                return null;
            }
            return users.computeIfAbsent(uuid, id -> new CMIUser(id.toString()));
        }

        public CMIUser getUser(String name) {
            if (name == null) {
                return null;
            }
            return new CMIUser(name);
        }

        public Map<UUID, CMIUser> getAllUsers() {
            return users;
        }
    }
}
EOF

  cat >"$src_dir/com/Zrips/CMI/Containers/CMIUser.java" <<'EOF'
package com.Zrips.CMI.Containers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CMIUser {
    private final String name;
    private boolean vanished;
    private boolean afk;
    private Long mutedUntil = 0L;
    private boolean god;
    private final Set<UUID> ignores = new HashSet<>();
    private String nickName;

    public CMIUser(String name) {
        this.name = name;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public boolean isAfk() {
        return afk;
    }

    public Long getMutedUntil() {
        return mutedUntil;
    }

    public boolean isGod() {
        return god;
    }

    public void setGod(boolean god) {
        this.god = god;
    }

    public void addIgnore(UUID uuid, boolean saveNow) {
        ignores.add(uuid);
    }

    public void removeIgnore(UUID uuid) {
        ignores.remove(uuid);
    }

    public boolean isIgnoring(UUID uuid) {
        return ignores.contains(uuid);
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName, boolean update) {
        this.nickName = nickName;
    }

    public void updateDisplayName() {
    }

    public String getName() {
        return name;
    }
}
EOF

  cat >"$src_dir/com/Zrips/CMI/Modules/TabList/TabListManager.java" <<'EOF'
package com.Zrips.CMI.Modules.TabList;

public class TabListManager {
    public boolean isUpdatesOnNickChange() {
        return false;
    }

    public void updateTabList(int ticks) {
    }
}
EOF

  javac -source 8 -target 8 -cp "$spigot_api_jar" -d "$classes_dir" $(find "$src_dir" -name '*.java')
  jar cf "$jar_path" -C "$classes_dir" .
  echo "$jar_path"
}

build_factionsuuid_stub() {
  local spigot_api_jar="$1"
  local src_dir="$TMP_DIR/factions-src"
  local classes_dir="$TMP_DIR/factions-classes"
  local jar_path="$TMP_DIR/factionsuuid-stub.jar"

  mkdir -p "$src_dir/com/massivecraft/factions"
  mkdir -p "$classes_dir"

  cat >"$src_dir/com/massivecraft/factions/FPlayer.java" <<'EOF'
package com.massivecraft.factions;

public class FPlayer {
    public String getName() {
        return null;
    }
}
EOF

  cat >"$src_dir/com/massivecraft/factions/FLocation.java" <<'EOF'
package com.massivecraft.factions;

import org.bukkit.Location;

public class FLocation {
    public FLocation(Location location) {
    }
}
EOF

  cat >"$src_dir/com/massivecraft/factions/Board.java" <<'EOF'
package com.massivecraft.factions;

public class Board {
    private static final Board INSTANCE = new Board();

    public static Board getInstance() {
        return INSTANCE;
    }

    public Object getFactionAt(FLocation location) {
        return null;
    }
}
EOF

  javac -source 8 -target 8 -cp "$spigot_api_jar" -d "$classes_dir" $(find "$src_dir" -name '*.java')
  jar cf "$jar_path" -C "$classes_dir" .
  echo "$jar_path"
}

resolve_spigot_api_jar() {
  local existing
  existing=$(find "$LOCAL_M2_REPO/org/spigotmc/spigot-api/1.19.2-R0.1-SNAPSHOT" -name 'spigot-api-*.jar' 2>/dev/null | head -n 1 || true)
  if [[ -n "$existing" ]]; then
    echo "$existing"
    return 0
  fi

  existing=$(find "$HOME/.m2/repository/org/spigotmc/spigot-api/1.19.2-R0.1-SNAPSHOT" -name 'spigot-api-*.jar' 2>/dev/null | head -n 1 || true)
  if [[ -n "$existing" ]]; then
    echo "$existing"
    return 0
  fi

  local metadata version
  metadata=$(curl -sSL --fail "https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.19.2-R0.1-SNAPSHOT/maven-metadata.xml")
  version=$(extract_snapshot_jar_version "$metadata")
  if [[ -z "$version" ]]; then
    echo "Failed to resolve spigot-api snapshot version" >&2
    exit 1
  fi

  local out="$TMP_DIR/spigot-api.jar"
  download_file "$out" "https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.19.2-R0.1-SNAPSHOT/spigot-api-${version}.jar"
  echo "$out"
}

main() {
  require_cmd curl
  require_cmd mvn
  require_cmd javac
  require_cmd jar

  echo "Using local Maven repository: $LOCAL_M2_REPO"

  copy_installed_aliases
  if all_aliases_installed; then
    echo "All Foundation compatibility aliases already installed."
    return 0
  fi

  download_file "$TMP_DIR/authme.jar" "https://repo.codemc.io/repository/maven-releases/fr/xephi/authme/5.6.0-beta1/authme-5.6.0-beta1.jar"

  local citizens_meta citizens_ver
  citizens_meta=$(curl -sSL --fail "https://maven.citizensnpcs.co/repo/net/citizensnpcs/citizensapi/2.0.29-SNAPSHOT/maven-metadata.xml")
  citizens_ver=$(extract_snapshot_jar_version "$citizens_meta")
  if [[ -z "$citizens_ver" ]]; then
    echo "Failed to resolve citizensapi snapshot version" >&2
    exit 1
  fi
  download_file "$TMP_DIR/citizensapi.jar" "https://maven.citizensnpcs.co/repo/net/citizensnpcs/citizensapi/2.0.29-SNAPSHOT/citizensapi-${citizens_ver}.jar"

  download_file "$TMP_DIR/discordsrv.jar" "https://nexus.scarsz.me/content/groups/public/com/discordsrv/discordsrv/1.26.0/discordsrv-1.26.0.jar"

  local essentials_meta essentials_ver
  essentials_meta=$(curl -sSL --fail "https://repo.essentialsx.net/snapshots/net/essentialsx/EssentialsX/2.20.0-SNAPSHOT/maven-metadata.xml")
  essentials_ver=$(extract_snapshot_jar_version "$essentials_meta")
  if [[ -z "$essentials_ver" ]]; then
    echo "Failed to resolve EssentialsX snapshot version" >&2
    exit 1
  fi
  download_file "$TMP_DIR/essentialsx.jar" "https://repo.essentialsx.net/snapshots/net/essentialsx/EssentialsX/2.20.0-SNAPSHOT/EssentialsX-${essentials_ver}.jar"

  download_file "$TMP_DIR/mcmmo.jar" "https://nexus.neetgames.com/repository/maven-releases/com/gmail/nossr50/mcMMO/mcMMO/2.1.200/mcMMO-2.1.200.jar"
  download_file "$TMP_DIR/multiverse.jar" "https://repo.glaremasters.me/repository/onarandombox/com/onarandombox/multiversecore/Multiverse-Core/2.5/Multiverse-Core-2.5.jar"
  download_file "$TMP_DIR/placeholderapi.jar" "https://repo.extendedclip.com/content/repositories/placeholderapi/me/clip/placeholderapi/2.11.5/placeholderapi-2.11.5.jar"
  download_file "$TMP_DIR/protocollib.jar" "https://repo.dmulloy2.net/repository/public/com/comphenix/protocol/ProtocolLib/5.3.0/ProtocolLib-5.3.0.jar"
  download_file "$TMP_DIR/residence.jar" "https://jitpack.io/com/github/Zrips/Residence/6.0.0.1/Residence-6.0.0.1.jar"
  download_file "$TMP_DIR/towny.jar" "https://repo.glaremasters.me/repository/towny/com/palmergames/bukkit/towny/towny/0.98.1.12/towny-0.98.1.12.jar"
  download_file "$TMP_DIR/worldedit.jar" "https://maven.enginehub.org/repo/com/sk89q/worldedit/worldedit-core/7.2.10/worldedit-core-7.2.10.jar"
  download_file "$TMP_DIR/worldguard.jar" "https://maven.enginehub.org/repo/com/sk89q/worldguard/worldguard-core/7.0.5/worldguard-core-7.0.5.jar"

  local spigot_api_jar cmi_stub_jar factionsuuid_stub_jar
  spigot_api_jar=$(resolve_spigot_api_jar)
  cmi_stub_jar=$(build_cmi_stub "$spigot_api_jar")
  factionsuuid_stub_jar=$(build_factionsuuid_stub "$spigot_api_jar")

  local empty_stub_jar="$TMP_DIR/empty-stub.jar"
  build_empty_jar "$empty_stub_jar"

  install_alias "AuthMe" "5.6.0-SNAPSHOT-2530" "$TMP_DIR/authme.jar"
  install_alias "CitizensAPI" "2.0.29-b839" "$TMP_DIR/citizensapi.jar"
  install_alias "CMI-API" "9.0.0.0" "$cmi_stub_jar"
  install_alias "DiscordSRV" "1.26.0-SNAPSHOT-0824719" "$TMP_DIR/discordsrv.jar"
  install_alias "EssentialsX" "2.20.0-SNAPSHOT-1375" "$TMP_DIR/essentialsx.jar"
  install_alias "FactionsUUID" "1.6.9.5-U0.6.11-b287" "$factionsuuid_stub_jar"
  install_alias "mcMMO" "2.1.213" "$TMP_DIR/mcmmo.jar"
  install_alias "Multiverse-Core" "4.3.2-SNAPSHOT-870" "$TMP_DIR/multiverse.jar"
  install_alias "PlaceholderAPI" "2.11.1" "$TMP_DIR/placeholderapi.jar"
  install_alias "ProtocolLib" "5.0.0-SNAPSHOT-586" "$TMP_DIR/protocollib.jar"
  install_alias "Residence" "5.0.1.6" "$TMP_DIR/residence.jar"
  install_alias "SimpleClans" "2.16.2-SNAPSHOT-283" "$empty_stub_jar"
  install_alias "Towny" "0.98.1.12" "$TMP_DIR/towny.jar"
  install_alias "TownyChat" "0.96" "$empty_stub_jar"
  install_alias "WorldEdit" "7.2.10" "$TMP_DIR/worldedit.jar"
  install_alias "WorldGuard" "7.0.7" "$TMP_DIR/worldguard.jar"

  echo "Foundation compatibility aliases are installed into $LOCAL_M2_REPO."
}

main "$@"
