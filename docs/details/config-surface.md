# WonderlandUHC 設定檔載入面細節說明

整理日期：2026-05-23

這份文件說明 WonderlandUHC 目前有哪些設定檔、由哪些程式讀取、哪些屬於 jar 預設 resource、哪些屬於運行資料。它不是開服設定教學，而是給維護者在修改 YAML、config parser、文字格式或 cache 時判斷影響範圍。

## 設定檔分類

| 類型 | 檔案 | 維護定位 |
| --- | --- | --- |
| 靜態設定 | `settings.yml`、`messages.yml`、`commands.yml`、`sounds.yml` | 啟動 / reload 時由 static config class 載入。 |
| 顯示設定 | `gui.yml`、`items.yml`、`scoreboards.yml`、`scenarios.yml`、`broadcasts.yml` | 由各功能讀取，控制 GUI、item、scoreboard、scenario 與 Discord 公告。 |
| 座標資料 | `spawns.yml` | 運行期間儲存大廳 spawn。 |
| 運行資料 | `cache.db`、`savedgames.db`、`stats.yml` | 伺服器運行後產生或更新，不是 jar 預設 resource。 |
| manifest | `plugin.yml` | Paper plugin metadata、commands、permissions、dependency。 |

## Resource 建立

啟動時 `PluginBootstrap.loadFiles()` 會讀 `UHCFiles.getFileNames()`，並對每個檔案呼叫 `saveResourceIfMissing(...)`。

會自動補出的預設 resource：

- `broadcasts.yml`
- `commands.yml`
- `items.yml`
- `gui.yml`
- `messages.yml`
- `scenarios.yml`
- `scoreboards.yml`
- `settings.yml`
- `spawns.yml`
- `sounds.yml`

注意：

1. 只有檔案不存在時才會從 jar 複製。
2. 伺服器資料夾已存在同名檔案時，不會自動覆蓋。
3. 新增預設設定檔時，若希望首次啟動自動產生，必須加入 `UHCFiles` 的 settings file 清單。
4. `cache.db`、`savedgames.db`、`stats.yml` 不在這個預設 resource 清單中。

## Static Config 載入

目前 static config 類別包含：

| 類別 | 檔案 |
| --- | --- |
| `Settings` | `settings.yml` |
| `Messages` | `messages.yml` |
| `CommandSettings` | `commands.yml` |
| `Sounds` | `sounds.yml` |

它們都透過 `PluginStaticConfig.loadStaticConfiguration(...)` 載入。

主要規則：

1. public static 且非 final 的欄位會被視為 config field。
2. 欄位名稱會轉成 YAML path，例如 `PRE_START_TIME` -> `Pre_Start_Time`。
3. 巢狀 class 會變成 YAML section，例如 `Settings.Game.PRE_START_TIME` -> `Game.Pre_Start_Time`。
4. 支援型別包含 `String`、`Integer`、`Boolean`、`List<String>`、`List<Integer>`、enum、`PluginSound`。
5. 若欄位載入後仍是 null，會丟出 missing configuration error。
6. enum 解析會忽略大小寫，並把空白或 `-` 視為 `_`。

維護意義：

- 新增 static config field 時，預設 YAML 必須同步新增。
- 移除 field 時，也應檢查 YAML 是否還有無用預設值。
- 改 enum 名稱時，要考慮舊設定檔可能無法解析。
- `init()` 可用於特殊轉換或修正值，但不應藏太多業務流程。

## `settings.yml`

`settings.yml` 是全域預設與部分運行行為設定。

主要區塊：

- `Sounds`
- `Restart_Cmd`
- `Border`
- `Team`
- `CombatRelog`
- `Spectator`
- `Misc`
- `Game`
- `CenterCleaner`
- `Practice`
- `DiscordVoice`

維護注意：

1. `Settings` 類別中的 static field 是啟動後直接讀取的全域設定。
2. `Game` 區塊會影響 timer、teleport、freeze、UHC 世界名稱等核心流程。
3. `CenterCleaner` 修改後，通常需要跑選圖 / 跑圖流程驗證。
4. `DiscordVoice` 修改後，需要 DiscordSRV ready 才能完整驗證。

已移除的舊設定：

- `ChunkLoading`
- `Misc.Always_Day`
- `Misc.No_Fire_Tick`
- `Misc.Anti_Rain`

這些欄位目前不在 `Settings` 類別中，預設 `settings.yml` 也不再提供。若舊伺服器殘留這些 YAML key，目前程式不會讀取它們；不要為了相容舊檔案重新新增 runtime 行為。

## 本場遊戲設定快照

`UHCGameSettings` 是本場比賽設定快照，不等同於 `settings.yml` 全域 static config。

它會出現在：

- `Game.getSettings()`。
- `cache.db` 的 `Settings`。
- `savedgames.db`。
- host GUI 修改後的運行中設定。

維護注意：

1. 修改 host GUI 設定時，通常是在改 `UHCGameSettings`，不一定是在改 `settings.yml`。
2. 需要跨重啟保留的本場設定，要確認 `UHCGameSettings.toMap()` 與 `fromSection()` 都支援。
3. 新增本場設定時，需同步檢查 cache、saved games、clone、GUI 與預設 fallback。

## 文字設定

主要文字檔：

- `messages.yml`
- `commands.yml`
- `gui.yml`
- `items.yml`
- `scoreboards.yml`
- `scenarios.yml`
- `broadcasts.yml`
- `settings.yml` 中少量玩家可見名稱。

正式文字格式是 MiniMessage。

維護注意：

1. 不要新增舊 Bukkit 色碼作為新預設格式。
2. placeholder 使用 `{name}`，不是 `<name>`。
3. 玩家或外部輸入通常會被 escape，不應被當成 MiniMessage tag。
4. Discord `Formatting` 不是 Minecraft 顏色顯示面，會移除 Minecraft 顏色後送出。

完整文字格式見 `docs/details/text-format.md`。

## GUI 與 Item

`gui.yml` 與 `items.yml` 主要由：

- `PluginMenuSection`
- `PluginItems`
- `UHCTool`

讀取。

GUI section 通常需要：

- `Title`
- `Rows`
- `Buttons.<Name>.Slot`
- `Buttons.<Name>.Type`
- `Buttons.<Name>.Name`
- `Buttons.<Name>.Lore`

item 通常需要：

- `Type`
- `Name`
- `Lore`
- `Slot`

`PluginItems` 會把 material name 正規化：

- 轉大寫。
- 空白與 `-` 轉 `_`。
- 移除前綴 `minecraft:`。

維護注意：

1. `Slot` 缺少或不是數字時，GUI 會丟錯。
2. `Type` 缺少或 material 不存在時，item 建立會丟錯。
3. GUI 修改不只看 YAML，也要確認對應 menu class 的 button key 是否一致。

## Scoreboard

`scoreboards.yml` 由 `SidebarTheme.loadThemes()` 載入。

每個 top-level key 是一個 theme。theme 內常見 section：

- `Lobby`
- `Starting`
- `Spectator_Solo`
- `Spectator_Teams`
- `Staff_Solo`
- `Staff_Teams`
- `Player_Solo`
- `Player_Teams`

維護注意：

1. 目前預設檔只保留 `Default` 區塊；host GUI 不再提供多風格切換。
2. 修改 scoreboard placeholder 時，要同步檢查 line 類別與 `GamePlaceholderReplacer`。
3. `SidebarTheme.defaultTheme()` 仍取第一個 top-level theme，因此至少要有一個 top-level theme。
4. scoreboard text 是可重新載入流程的一部分，修改後應確認 reload 或重啟後有重新載入。

## Scenario 設定

`scenarios.yml` 由 scenario 系統讀取。

常見欄位：

- `Type`
- `Name`
- `Description`
- scenario-specific config。

`ScenarioConfig` 會：

1. 依 scenario name 找 YAML section。
2. 讀取顯示 material、名稱與描述。
3. 將有 `@FilePath` 的 scenario 欄位依型別注入。
4. 支援 `String`、`Integer`、`Boolean`、`Material`、`PluginSound`、部分 collection。

新增 `@FilePath` 欄位時，要同步新增 YAML 預設值。Material 不存在時會在載入或使用時丟錯。

## Broadcast 設定

`broadcasts.yml` 主要由 broadcast sender 讀取。

Discord 目前使用：

- `Discord.Channel_Ids`
- `Discord.Invalid_Channel`
- `Discord.Formatting`

流程：

1. 讀取 formatting list。
2. 套用 WonderlandUHC placeholder。
3. 移除 Minecraft 顏色。
4. 透過 DiscordSRV / JDA 發送。

維護注意：

1. `Invalid_Channel` 是 Minecraft 端錯誤訊息，可用 MiniMessage。
2. `Discord.Formatting` 是 Discord 文字內容，不應依賴 MiniMessage 顏色。
3. mention 行為由 DiscordSRV / JDA 處理，修改時需小心 allowed mentions。

## 運行資料

| 檔案 | 來源 | 用途 |
| --- | --- | --- |
| `cache.db` | `WorldLoadingCacheStore` | 世界載入狀態、host、本場設定、`MatchCenter`。 |
| `savedgames.db` | `SavedGameSettingsStore` | 主持人保存的設定組。 |
| `stats.yml` | `StatsStorageYaml` | 玩家統計資料。 |
| `spawns.yml` | `UHCSpawn` / `Spawns` | 大廳 spawn。 |

維護注意：

1. 不要把運行資料放進 jar 預設 resource。
2. 修改資料格式時，要考慮既有伺服器上的舊檔案。
3. `cache.db` 格式變更會影響 `/uhc choose` 後重啟接續。
4. `WorldLoadingCacheStore` 已有部分舊 enum tag migration，新增 migration 時要保持保守。

## Reload 行為

`WonderlandUHC.reload()` 會：

1. 取消 scheduler task。
2. reload scenario 運行狀態。
3. reload saved game settings cache。
4. 重新執行可重新載入流程。

可重新載入流程會：

1. `loadFiles()`。
2. `loadStaticConfiguration()`。
3. `checkDependencies()`。
4. `loadScoreboardThemes()`。
5. `Spawns.reload()`。
6. reload stats storage。
7. 若世界載入狀態是 `DONE`，啟動 scoreboard updater 與運行任務。

修改 world loading、game state 或外部 plugin hook 後，不應只靠 `/uhc reload`，仍應用完整重啟驗證。

## 新增設定檢查表

新增設定時，至少確認：

1. 預設 YAML 有值。
2. Java loader 有欄位或明確讀取邏輯。
3. 型別與 YAML 寫法一致。
4. 缺少舊設定時是否需要 fallback 或 migration。
5. 若要保存到本場設定，`UHCGameSettings.toMap()` 與 `fromSection()` 已更新。
6. 若要顯示在 GUI，`gui.yml` key 與 menu class key 一致。
7. 若是文字，placeholder 與 MiniMessage 規則正確。
8. 封裝與啟動測試通過。

驗證方式見 `docs/details/verification.md`。
