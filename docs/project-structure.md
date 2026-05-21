# WonderlandUHC 新版本專案檔案架構摘要

整理日期：2026-05-21

這份文件是新版本 WonderlandUHC 的檔案架構入口摘要，目標是讓委託人、原作者與後續維護者快速理解專案目前怎麼分層、主要檔案放在哪裡，以及未來新增功能時應該優先放到哪一類目錄。

## 整體方向

新版本的檔案架構不是只為了升級到 Paper `1.21.11`，也同時把舊專案中混在一起的責任逐步拆開。

主要方向：

1. 純 UHC 規則與 Paper / Bukkit API 分開。
2. 比賽流程集中到 application service / use case。
3. Paper、LuckPerms、Chunky、DiscordSRV 等外部依賴集中在邊界層。
4. 原版插件中已穩定運作的流程可以保留，但新功能不應繼續擴散舊耦合。
5. 指令、GUI、scenario、scoreboard、設定檔各自維持清楚責任。

## 根目錄總覽

| 路徑 | 用途 |
| --- | --- |
| `src/main/java/` | 插件主要 Java 程式碼。 |
| `src/main/resources/` | `plugin.yml` 與預設設定檔，正式格式以 Paper `1.21.11` 與 MiniMessage 為準。 |
| `src/test/java/` | 單元測試與不需啟動 Paper 的行為測試。 |
| `build.gradle`、`gradlew`、`gradle/` | Gradle 建置設定與 wrapper。 |
| `.github/` | GitHub issue template。 |
| `docs/` | 正式說明文件，根目錄以摘要文件為主。 |
| `docs/details/` | 細節說明文件，用來補充摘要文件省略的規則、範例與排查方式。 |
| `libs/` | 本地編譯依賴，目前用來讓 Gradle 封裝時找到 Chunky API；內容不會打包進插件 jar。 |

## Java Package 摘要

| Package | 定位 |
| --- | --- |
| `org.mcwonderland.uhc.core` | 純 UHC 規則與狀態模型，不依賴 Bukkit / Paper。 |
| `org.mcwonderland.uhc.application` | 比賽流程、世界流程、登入權限、border 等 use case。 |
| `org.mcwonderland.uhc.port` | 平台能力介面，例如 scheduler、world、asset、event publisher。 |
| `org.mcwonderland.uhc.platform` | Paper / Bukkit / Adventure 的具體 adapter。 |
| `org.mcwonderland.uhc.integration` | LuckPerms、Chunky、DiscordSRV 等外部插件整合。 |
| `org.mcwonderland.uhc.bootstrap` | 啟動組裝、依賴狀態報告、功能註冊與資源載入。 |
| `org.mcwonderland.uhc.game` | 目前仍承接原版插件的大量遊戲流程與狀態。 |
| `org.mcwonderland.uhc.command` | `/uhc`、`/team` 等指令入口。 |
| `org.mcwonderland.uhc.menu` | 主持設定、隊伍設定、scenario、scoreboard 等 inventory GUI。 |
| `org.mcwonderland.uhc.scenario` | 各 UHC scenario 的註冊、設定、listener 與實作。 |
| `org.mcwonderland.uhc.scoreboard` | sidebar、below-name health、tab line、theme 與顯示 fallback。 |
| `org.mcwonderland.uhc.settings` | `settings.yml`、`messages.yml`、`commands.yml` 等靜態設定載入。 |
| `org.mcwonderland.uhc.storage`、`stats` | runtime data、saved games、stats 等資料讀寫。 |
| `org.mcwonderland.uhc.model` | 跨功能資料模型與輔助流程，例如 broadcast、death message、freeze、tutorial。 |
| `org.mcwonderland.uhc.tools`、`practice` | 玩家工具、staff / spectator hotbar 與練習模式。 |
| `org.mcwonderland.uhc.api` | 對內 / 對外事件、enum 與少量公開 API object。 |
| `org.mcwonderland.uhc.util` | 共用輔助工具，例如玩家、世界、物品、文字、音效、時間格式與區域選取等 helper。 |

## Resources 摘要

| 檔案 | 用途 |
| --- | --- |
| `plugin.yml` | Bukkit / Paper plugin manifest、commands、permissions、hard / soft dependencies。 |
| `settings.yml` | 遊戲主要設定預設值。 |
| `messages.yml`、`commands.yml` | 玩家可見訊息與指令文字，正式格式使用 MiniMessage。 |
| `gui.yml`、`items.yml` | GUI、按鈕、lore、hotbar item 顯示。 |
| `scoreboards.yml` | scoreboard theme、sidebar line、below-name 顯示設定。 |
| `scenarios.yml` | scenario 顯示名稱、說明與 scenario-specific config。 |
| `sounds.yml` | GUI、指令、流程音效設定，使用 Paper `1.21.11` 可用 sound key。 |
| `broadcasts.yml`、`spawns.yml` | Discord / 遊戲公告格式與 spawn 記錄格式。 |

注意：`cache.db`、`savedgames.db`、`stats.yml` 這類 runtime data 不應作為 jar 預設 resource 打包。

## 新功能放置原則

1. 純規則或狀態判斷優先放 `core`。
2. 比賽流程與跨入口 use case 優先放 `application`。
3. Paper / Bukkit 具體操作放 `platform`。
4. 外部插件 API 放 `integration`。
5. 指令入口放 `command`，GUI 入口放 `menu`，但兩者都不應承擔大型流程。
6. scenario 行為放 `scenario`，跨 scenario 規則才抽 shared helper。
7. 原版插件既有流程可留在 `game`，但新功能不要新增 Foundation / NMS 類耦合。
8. 只有出現實際共用需求時才新增 port / service，避免為單一使用點過度抽象。

## 目前過渡狀態

目前專案已完成主要升級與舊版相容清理，但檔案架構仍保留部分原版插件的既有形狀：

- `game` 仍是目前實際遊戲流程的核心區域。
- `command`、`menu`、`scenario` 仍有部分既有流程寫法，例如入口層直接操作遊戲狀態、設定或事件細節；目前已完成 Paper `1.21.11` 相容整理，後續只在有實際維護收益時再逐步拆分。
- `core` / `application` / `port` / `platform` 是後續新增功能與逐步整理時的主要方向。
- `util` 仍保留多種共用 helper；後續只在有實際維護收益時逐步搬移，不為了目錄漂亮而重構。

後續判斷是否要移動檔案時，應優先看行為風險、測試成本與是否真的降低維護負擔，不以「清空舊 package」當作目標。

更完整的 package 邊界、資料夾定位、新功能放置判斷與設定檔載入面，見 `docs/details/project-structure.md` 與 `docs/details/config-surface.md`。
