# Step 23 Message Format Migration 盤點

盤點日期：2026-05-18

本文件只盤點 Step 23 的訊息格式邊界，不代表已完成程式碼遷移。Step 23 的目標是集中 `LegacyComponentSerializer`、`&` / `§` 舊色碼、Adventure `Component` 與 placeholder replacement 的使用邊界；不是改寫 command、menu、scoreboard 或 login gate 的業務流程。

## 搜尋基準

已執行：

```bash
rg -n "LegacyComponentSerializer" src/main/java src/test/java
rg -n "PluginText\.colorize" src/main/java src/test/java
rg -n -m 40 "&[0-9A-Fa-fK-Ok-oRrXx]|§[0-9A-Fa-fK-Ok-oRrXx]|\{[A-Za-z0-9_+\-]+\}" src/main/resources/messages.yml src/main/resources/items.yml src/main/resources/gui.yml src/main/resources/scoreboards.yml src/main/resources/commands.yml src/main/resources/broadcasts.yml
```

目前 `LegacyComponentSerializer` 有 49 個命中，分布在 22 個 Java 檔案。`PluginText.colorize(...)` 有 12 個直接呼叫。resources 仍大量使用 legacy `&` 色碼與 `{placeholder}`，因此 Step 23 不可直接改成 MiniMessage-only。

## 進度紀錄

### 2026-05-18 第一刀：集中基礎 text boundary

已完成：

- `PluginText` 新增 legacy string -> Adventure `Component`、nullable legacy string -> nullable `Component`、`Component` -> legacy string 的薄轉換方法。
- `Chat` 不再持有自己的 `LegacyComponentSerializer` 或重複 `&` 轉色邏輯，改委派 `PluginText`；`sendConversing(...)` 仍維持 raw legacy string 輸出。
- `PluginPlayers` 的 `kick(...)` 與 action bar 轉換改委派 `PluginText`；排程、多行 kick 與 action bar 行為未改。
- 新增 `PluginTextTest` 覆蓋 ampersand legacy color、nullable component 與 component serialize。

未處理：

- menu / item / join / quit / login / MOTD 的 serializer 使用點。
- `Component` -> legacy string 相容點，例如 golden head 判斷、scenario fancy name、broadcast item list。
- clickable command component。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (32.115s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。

### 2026-05-18 第二刀：presentation boundary 委派

已完成：

- `PluginMenu` menu title 改走 `PluginText.toComponent(...)`。
- `UHCLoginEvent` login kick message 改走 `PluginText.toComponent(...)`；`LoginListener` 的 fallback kick message 也改直接走 `PluginText`，不再保留 `UHCLoginEvent.toComponent(...)` 作為第二個轉換入口。
- `UHCJoinEvent` join message 改走 `PluginText.toNullableComponent(...)`，保留 null join message 行為。
- `MotdListener` MOTD 改走 `PluginText.toComponent(...)`。
- `LobbyQuitListener` 與 `RolePlayerEvents` 的 quit message 改走 `PluginText.toComponent(...)`。

未處理：

- `QuitListener` 仍保留 serialize -> colorize -> deserialize 的特殊後處理語意，下一刀前需單獨判斷。
- item display name / lore 與 golden head 判斷。
- `Component` -> legacy string 相容點，例如 scenario fancy name、broadcast item list。
- clickable command component。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (21.640s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 命中降為 33 個。

### 2026-05-18 第三刀：item / recipe display name 委派

已完成：

- `PluginItems` 的 item display name 與 lore component 轉換改走 `PluginText.toComponent(...)`。
- `PluginItems` 保留 name 預設 `&r&f`、lore 預設 `&7`、lore 內嵌換行拆行與 item material alias 行為。
- `Extra#createHead()` 的 golden head recipe display name 改走 `PluginText.toComponent(...)`。

未處理：

- `GoldenHeadListener` 的 `Component` -> legacy string 比對邏輯，因為這是 golden head 功能判斷，不混入本刀。
- `MainSettingsMenu` inventory editor 的 golden head display name 與 clickable command component。
- scenario fancy name、broadcast item list。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (16.492s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 命中降為 29 個。

### 2026-05-18 第四刀：Component -> legacy string 相容點委派

已完成：

- `AbstractScenario#getName()` 的 scenario fancy name 仍從 item display name 取得玩家可見名稱，但 `Component` -> legacy string 轉換改走 `PluginText.toLegacyString(...)`。
- `GamePlaceholderReplacer` 的 custom item list / broadcast placeholder display name 轉換改走 `PluginText.toLegacyString(...)`。
- `GoldenHeadListener` 的 golden head item display name 轉換改走 `PluginText.toLegacyString(...)`。
- golden head 判斷仍維持 `name.equalsIgnoreCase(Settings.Misc.GOLDEN_HEAD_NAME)`，不改 `Settings.Misc.GOLDEN_HEAD_NAME` 格式、不改 recipe display name 來源，也不改功能判斷語意。

未處理：

- `QuitListener` 的 serialize -> colorize -> deserialize 特殊後處理語意。
- `CombatRelog` villager custom name、`UHCTeam` backpack title、`OreAlert` legacy string 輸出。
- `MainSettingsMenu` inventory editor 的 golden head display name 與 clickable command component。
- `/team invite`、`/uhc regen` 等 clickable command component。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (21.912s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 23 行，剩餘 10 個 Java 檔案。

### 2026-05-18 第五刀：clickable command component 委派

已完成：

- `/team invite` 的邀請點擊訊息與 hover text 改用 `PluginText.toComponent(...)` 建立，保留 `/team join <player>` run command、placeholder replacement 與既有訊息來源。
- `/uhc regen` 的 CenterCleaner confirm / skip 點擊訊息與 hover text 改用 `PluginText.toComponent(...)` 建立，保留 confirm / skip / seed 流程。
- `MainSettingsMenu` inventory editor 的 `/tohead` 與 `/finish` 點擊訊息改用 `PluginText.toComponent(...)` 建立，保留 input session、背包保存與 game mode 還原流程。
- 本刀沒有新增 clickable command helper 或 command framework，只移除這三處本地 `legacyAmpersand()` serializer。

未處理：

- `MainSettingsMenu` inventory editor 的 golden head display name 仍保留 `LEGACY_SECTION`，需和 `Settings.Misc.GOLDEN_HEAD_NAME` 行為一起確認。
- `OreAlert` 仍保留 legacy ampersand serializer，因為它是 staff alert 的 legacy string 輸出形狀，不是 clickable command。
- `QuitListener` 的 serialize -> colorize -> deserialize 特殊後處理語意。
- `CombatRelog` villager custom name、`UHCTeam` backpack title。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (27.381s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 18 行，剩餘 8 個 Java 檔案。

### 2026-05-18 第六刀：MainSettingsMenu golden head display name 委派

已完成：

- `MainSettingsMenu` inventory editor 的 golden head display name 改用 `PluginText.toComponent(Settings.Misc.GOLDEN_HEAD_NAME)` 建立。
- `MainSettingsMenu` 已移除 `LegacyComponentSerializer` import 與本地 `LEGACY_SECTION` 欄位。
- 本刀保留 `/tohead` input、`Material.GOLDEN_APPLE` 檢查、`Settings.Misc.GOLDEN_HEAD_NAME` 來源、背包保存與 game mode 還原流程。

未處理：

- `OreAlert` legacy ampersand serializer，因為它是 staff alert 的 legacy string 輸出形狀。
- `QuitListener` 的 serialize -> colorize -> deserialize 特殊後處理語意。
- `CombatRelog` villager custom name、`UHCTeam` backpack title。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (32.551s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 16 行，剩餘 7 個 Java 檔案。

### 2026-05-18 第七刀：relog / backpack component title 委派

已完成：

- `CombatRelog` villager custom name 改用 `PluginText.toComponent(UHCTeam.getTeam(player).getPrefix() + player.getName())` 建立。
- `UHCTeam` backpack inventory title 改用 `PluginText.toComponent(this.name)` 建立。
- 本刀保留 combat relog entity 產生、no-AI / silent、裝備複製、背包大小、team name 來源與 team runtime 流程。
- `CombatRelog` 與 `UHCTeam` 已移除本地 `LegacyComponentSerializer` import。

未處理：

- `OreAlert` legacy ampersand serializer，因為它是 staff alert 的 legacy string 輸出形狀。
- `QuitListener` 的 serialize -> colorize -> deserialize 特殊後處理語意。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (42.537s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 12 行，剩餘 5 個 Java 檔案。

### 2026-05-18 第八刀：OreAlert legacy ampersand string 委派

已完成：

- `PluginText` 新增 `toLegacyAmpersandString(Component)`，集中 legacy ampersand string serialize 邊界。
- `OreAlert#colorizedName()` 改用 `PluginText.toLegacyAmpersandString(...)`，保留 staff alert 需要的 `&` 色碼輸出形狀。
- `OreAlert` 已移除本地 `LegacyComponentSerializer` 與 `LEGACY_AMPERSAND` 欄位。
- 新增 `PluginTextTest#toLegacyAmpersandStringSerializesComponent()` 覆蓋 ampersand serialize。

未處理：

- `QuitListener` 的 serialize -> colorize -> deserialize 特殊後處理語意。
- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (40.712s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 11 行，剩餘 4 個 Java 檔案。

### 2026-05-18 第九刀：QuitListener quit message 後處理委派

已完成：

- `QuitListener` 保留原本 serialize -> colorize -> deserialize 的後處理語意，但改成委派 `PluginText.toLegacyString(...)` 與 `PluginText.toComponent(...)`。
- `QuitListener` 已移除本地 `LegacyComponentSerializer` import。
- 本刀保留 `onPlayerQuit(e)` 呼叫順序、null quit message 行為與 quit event 流程。

未處理：

- scoreboard。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (24.164s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 8 行，剩餘 3 個 Java 檔案。

### 2026-05-18 第十刀：scoreboard component 轉換委派

已完成：

- `SimpleSidebar` 的 scoreboard title、line prefix 與 line suffix component 轉換改用 `PluginText.toComponent(...)`。
- `SimpleScores` 的 team prefix、below-name health objective、below-name heart display 與 tab health objective component 轉換改用 `PluginText.toComponent(...)`。
- `SimpleSidebar` 保留 `§` entry code、15 行 entry、16 / 32 字元切割、hex color carry 與 legacy color carry 邏輯。
- `SimpleScores` 保留 team color fail-safe、team prefix truncate、below-name heart 與 tab health objective 流程。
- `SimpleSidebar` 與 `SimpleScores` 已移除本地 `LegacyComponentSerializer` import；`LegacyComponentSerializer` 目前只保留在 `PluginText` 這個 text boundary。

未處理：

- README / DEVELOPMENT 的訊息格式策略說明。

驗證：

- `bash scripts/package-plugin-1.21.sh` 通過。
- `bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build` 通過。
- Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done (37.644s)!`。
- console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`
- 本次啟動期間 `latest.log` 未命中 `ERROR`、`Exception`、`NoClassDefFoundError`、`NoSuchMethodError` 或 `ClassCastException`。
- `LegacyComponentSerializer` production/test source 的 `rg -n` 命中降為 3 行，剩餘 1 個 Java 檔案：`PluginText`。

## 目前格式策略判斷

- 建議 Step 23 第一階段維持 legacy-compatible config：既有 `&c`、`§c`、換行與 `{placeholder}` 都要繼續可讀。
- 不建議第一刀導入 MiniMessage；若要支援 MiniMessage，應先定義 legacy / MiniMessage 共存規則與 fallback，而不是邊改邊猜。
- `LegacyComponentSerializer` 不應全域刪除；它可以存在於 `platform/text` 這類明確邊界，但不應散在 command、event、scoreboard 與 game logic。
- `PluginText` 已是目前舊色碼與 placeholder helper，最小改法是先把 Adventure `Component` 轉換也集中到這裡，不新增大型 message registry。

## 使用點分類

### A. 建議先集中成正式 text 邊界

| 檔案 | 現況 | 建議 |
| --- | --- | --- |
| `src/main/java/org/mcwonderland/uhc/platform/text/PluginText.java` | 已處理 `&` -> `§`、strip colors、placeholder replacement、時間格式。尚未提供 `Component` 轉換 API。 | 第一刀補上薄方法，例如 legacy string -> `Component`、nullable legacy string -> nullable `Component`、`Component` -> legacy string。 |
| `src/main/java/org/mcwonderland/uhc/util/Chat.java` | 自己持有 `LegacyComponentSerializer` 與 `&` 轉色邏輯，和 `PluginText.colorize(...)` 重複。 | 改成委派 `PluginText`；保留 `sendConversing` 的 raw legacy string 行為。 |
| `src/main/java/org/mcwonderland/uhc/platform/player/PluginPlayers.java` | `kick` / action bar 有自己的 `toComponent` 和 `&` 轉色邏輯。 | 改成委派 `PluginText`；驗收 kick 多行與 action bar 顏色。 |
| `src/main/java/org/mcwonderland/uhc/platform/menu/PluginMenu.java` | menu title 用本地 serializer 轉 `Component`。 | 改成委派 `PluginText`；驗收主要 host menu title。 |
| `src/main/java/org/mcwonderland/uhc/platform/item/PluginItems.java` | item name / lore 用 serializer + `PluginText.colorize(...)`。 | 改成委派 `PluginText`；保留 name 預設 `&r&f`、lore 預設 `&7` 的語意。 |
| `src/main/java/org/mcwonderland/uhc/game/state/share/login/UHCLoginEvent.java` | login kick message 用本地 static `toComponent`。 | 改成委派 `PluginText`；保留 Step 22 驗收過的 kick 文字與顏色。 |
| `src/main/java/org/mcwonderland/uhc/game/state/share/join/UHCJoinEvent.java` | join message nullable `String` -> nullable `Component`。 | 改成委派 `PluginText`；保留 null join message 行為。 |

### B. Component 輸出邊界，可第二批處理

| 檔案 | 現況 | 建議 |
| --- | --- | --- |
| `src/main/java/org/mcwonderland/uhc/game/state/share/MotdListener.java` | MOTD 先 `PluginText.colorize(...)` 再本地 deserialize。 | 改走集中入口；驗收 server list MOTD。 |
| `src/main/java/org/mcwonderland/uhc/game/state/share/LobbyQuitListener.java` | lobby quit message 自行 deserialize。 | 改走集中入口；驗收 lobby 離線訊息。 |
| `src/main/java/org/mcwonderland/uhc/game/state/share/QuitListener.java` | 已委派 `PluginText.toLegacyString(...)` 與 `PluginText.toComponent(...)`，保留 serialize -> colorize -> deserialize 後處理語意。 | 本檔已無 serializer；保留對下游 listener 設好的 quit message 再補 legacy color 的行為。 |
| `src/main/java/org/mcwonderland/uhc/game/player/role/player/RolePlayerEvents.java` | playing quit / combat relog disconnect message 自行 deserialize。 | 改走集中入口；驗收遊戲中離線訊息與 combat relog。 |
| `src/main/java/org/mcwonderland/uhc/game/CombatRelog.java` | relog villager custom name 已改用 `PluginText.toComponent(...)`。 | 保留 combat relog entity flow；本檔已無 serializer。 |
| `src/main/java/org/mcwonderland/uhc/game/UHCTeam.java` | backpack inventory title 已改用 `PluginText.toComponent(...)`。 | 保留 team name 與 backpack runtime flow；本檔已無 serializer。 |
| `src/main/java/org/mcwonderland/uhc/util/Extra.java` | `Extra#createHead()` 的 golden head display name 已改用 `PluginText.toComponent(...)`。 | 保留 `Settings.Misc.GOLDEN_HEAD_NAME` 來源與 golden head 判斷相容。 |
| `src/main/java/org/mcwonderland/uhc/menu/impl/host/MainSettingsMenu.java` | inventory editor click text 與 golden head display name 已改用 `PluginText.toComponent(...)`。 | 保留 input session、背包保存與 `Settings.Misc.GOLDEN_HEAD_NAME` 語意；本檔已無 serializer。 |

### C. Component -> legacy string 相容點，不能盲目刪

| 檔案 | 現況 | 建議 |
| --- | --- | --- |
| `src/main/java/org/mcwonderland/uhc/scenario/impl/AbstractScenario.java` | scenario icon display name serialize 回 legacy string，作為 fancy name。 | 保留「從 item display name 取玩家可見名稱」語意，改由 `PluginText` 集中 serialize。 |
| `src/main/java/org/mcwonderland/uhc/model/GamePlaceholderReplacer.java` | item display name serialize 後放入 broadcast / placeholder 輸出。 | 改由 `PluginText` 集中 serialize；驗收 custom item list 顯示。 |
| `src/main/java/org/mcwonderland/uhc/game/state/playing/listener/GoldenHeadListener.java` | item display name serialize 後和 `Settings.Misc.GOLDEN_HEAD_NAME` 比對。 | 這是功能判斷，不只是顯示；必須和 golden head 建立處一起改、一起測。 |
| `src/main/java/org/mcwonderland/uhc/game/player/staff/OreAlert.java` | `NamedTextColor` component 已透過 `PluginText.toLegacyAmpersandString(...)` serialize 成 legacy ampersand string。 | 保留 staff ore alert 需要 legacy string 的輸出形狀；本檔已無 serializer。 |

### D. Clickable command component，不應先重構成通用 framework

| 檔案 | 現況 | 建議 |
| --- | --- | --- |
| `src/main/java/org/mcwonderland/uhc/command/team/InviteCommand.java` | 已改用 `PluginText.toComponent(...)` 建立可點擊 `/team join` 訊息與 hover。 | 保留 team invite flow，不再新增 command helper。 |
| `src/main/java/org/mcwonderland/uhc/command/uhc/RegenWorldCommand.java` | 已改用 `PluginText.toComponent(...)` 建立 CenterCleaner confirm / skip clickable prompt。 | 保留 confirm / skip / seed 流程，不擴大成 command framework 重設計。 |
| `src/main/java/org/mcwonderland/uhc/menu/impl/host/MainSettingsMenu.java` | inventory editor 的 clickable `/tohead` / `/finish` component 與 golden head display name 都已改用 `PluginText.toComponent(...)`。 | 保留 inventory editor session 流程，不再處理本檔 serializer。 |

### E. Scoreboard 高風險區，需獨立切片

| 檔案 | 現況 | 建議 |
| --- | --- | --- |
| `src/main/java/org/mcwonderland/uhc/scoreboard/SimpleSidebar.java` | scoreboard title、line prefix 與 suffix component 轉換已委派 `PluginText.toComponent(...)`；`§` entry code、16 / 32 字元切割與顏色延續保留本檔。 | 本檔已無 serializer；若後續要改 scoreboard 行為，需另開功能切片。 |
| `src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java` | team prefix、below-name heart、tab health objective component 轉換已委派 `PluginText.toComponent(...)`。 | 本檔已無 serializer；team color fail-safe 與 objective flow 保留。 |

## Config 與資源檔狀態

以下檔案仍使用 legacy `&` 色碼與 `{placeholder}`，Step 23 必須保留讀取相容：

- `src/main/resources/messages.yml`
- `src/main/resources/commands.yml`
- `src/main/resources/gui.yml`
- `src/main/resources/items.yml`
- `src/main/resources/scoreboards.yml`
- `src/main/resources/broadcasts.yml`

這代表 README / DEVELOPMENT 的最終策略應寫成「目前支援 legacy `&` / `§` 格式，並集中於 text boundary 解析」，除非後續明確決定支援 MiniMessage。

## 建議實作順序

1. 第一刀：只擴充 `PluginText` 與改 `Chat` / `PluginPlayers` 委派，消除重複色碼轉換，不改玩家可見文字。
2. 第二刀：處理 menu / join / quit / login / MOTD 這類 presentation boundary。
3. 第三刀：處理 item / recipe display name 與 lore。
4. 第四刀：處理 Component -> legacy string 相容點，尤其 golden head 判斷、scenario fancy name、broadcast item list。
5. 第五刀：處理 clickable command component，只抽薄 helper，不重寫 command flow。
6. 第六刀：處理 `MainSettingsMenu` golden head display name，移除本檔剩餘 serializer。
7. 第七刀：處理 `CombatRelog` villager custom name 與 `UHCTeam` backpack title，移除這兩檔本地 serializer。
8. 第八刀：處理 `OreAlert` legacy string 輸出，保留 `&` 色碼輸出形狀。
9. 第九刀：處理 `QuitListener` 的 serialize -> colorize -> deserialize 特殊後處理語意，需單獨記錄。
10. 第十刀：處理 scoreboard；保留 entry code、長度切割與顏色延續邏輯，獨立驗收。
11. 最後更新 README / DEVELOPMENT 的訊息格式策略。

## 驗收清單

每個程式碼切片後依專案規則執行：

```bash
bash scripts/package-plugin-1.21.sh
bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build
```

伺服器仍使用測試服資料夾內的 `start.bat` 啟動。至少抽樣：

- login gate kick message：白名單、等待 host、滿員、遊戲中加入。
- join / quit / combat relog disconnect message。
- `/team invite` clickable message 與 hover。
- `/uhc regen` CenterCleaner clickable prompt。
- host menu title、inventory editor clickable `/tohead` / `/finish`。
- item display name / lore：hotbar tools、scenario icon、golden head。
- scoreboard title、line、team prefix、below-name heart、tab health。
- MOTD。

## Overthinking 停損線

以下不屬於第一階段，遇到時應停止並討論：

- 新增全域 message registry 或 message key framework。
- 自動把所有 YAML 轉成 MiniMessage-only。
- 重新設計 command、menu、scoreboard、login gate 或 team flow。
- 為了消除 `LegacyComponentSerializer` 名稱而破壞舊設定相容。
