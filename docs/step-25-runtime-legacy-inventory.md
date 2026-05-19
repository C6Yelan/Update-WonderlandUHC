# Step 25 Runtime Legacy Residue Inventory

本文件只盤點 Step 25 的 runtime legacy residue，不代表已開始程式碼遷移。Step 25 的目標是清除仍拖住未來更新的 static facade、state bridge、舊資料格式 fallback 與過渡命名；不是重寫核心比賽流程、設定系統、scoreboard 或 Step 24 的 MiniMessage 結果。

## 掃描基準

目前分支：`update-to-1.21`

掃描指令：

```bash
rg -n "(?i)\blegacy\b|compat|deprecated" src/main/java src/test/java build.gradle scripts
rg -n "Legacy[A-Za-z0-9_]*|[A-Za-z0-9_]*Legacy[A-Za-z0-9_]*|Compat[A-Za-z0-9_]*|[A-Za-z0-9_]*Compat[A-Za-z0-9_]*|Deprecated[A-Za-z0-9_]*|[A-Za-z0-9_]*Deprecated[A-Za-z0-9_]*" src/main/java src/test/java build.gradle scripts
rg -n "Fallback|fallback|alias|ALIASES|legacy|Legacy|bridge|Bridge|facade|Facade|wrapper|Wrapper|mapper|Mapper" src/main/java src/test/java
rg -n "舊版|相容|OldEnchant|Old_Enchant|old enchant|pass/fail|0,0" src/main/resources src/main/java src/test/java docs/steps.md
rg -n "&[0-9A-Fa-fK-Ok-orR]|§[0-9A-Fa-fK-Ok-orR]|LegacyComponentSerializer|toLegacyString|toLegacyAmpersandString|MessageFormatMigration|migrate-message-format" src/main/java src/test/java src/main/resources build.gradle scripts docs
rg -n "org\.mineacademy|Foundation|Datou|DaTou|NMS|PacketListener|PacketEvents|custom-ore-generator|de\.derfrzocker|com\.wimbli|WorldBorder" src/main/java src/test/java src/main/resources build.gradle scripts
```

重點結果：

- `build.gradle` 與 `scripts/` 沒有 Step 25 runtime legacy 命中。
- production / test source 沒有 `Foundation`、`org.mineacademy`、`DatouNMS`、NMS、`PacketListenerAPI`、PacketEvents、`custom-ore-generator`、舊 `com.wimbli.WorldBorder` 依賴殘留。
- repo 預設 `src/main/resources` 沒有 legacy `&` / `§` 訊息色碼命中；Step 24 message-format gate 仍維持乾淨。
- 初次盤點時 runtime legacy residue 主要集中在：
  - `WorldLoadingCacheState`
  - `LegacyGameStateTransitions`
  - `MatchSettingsMapper`
  - 舊 material / sound / color 設定讀取 fallback
  - 未使用的舊 updater / OldEnchant resource 殘留

第一刀進度（2026-05-19）：

- `UHCGameSettingsSaver` 已改名為正式 `SavedGameSettingsCache`，保留既有 `savedgames.db` 讀寫與 per-player memory cache 語意。
- `CacheSaver` 已改名為正式 `WorldLoadingCacheState`，保留既有 `cache.db` 讀寫、loading status、host、settings、match center 與重啟恢復語意。
- `LegacyGameStateTransitions` 已由正式 `MatchTransition.fromSourceState(...)` 取代；`Game.nextState()` 仍保留既有 `GameState` queue，只改 transition 決策來源。
- `LegacyMatchSettingsMapper` 已改名為正式 `MatchSettingsMapper`，保留既有 `UHCGameSettings` 到 `MatchSettings` 欄位映射。
- `CenterCleaner.applyLegacyGeneratorSettings(...)` 已改名為 `applyConfiguredGeneratorSettings(...)`，不改 `WorldCreator.generatorSettings(...)` 套用條件。
- material / sound 設定值 alias 已改成正式 `MATERIAL_NAME_ALIASES` / `SOUND_NAME_ALIASES` 命名；保留既有 data folder 可讀性，不把舊值直接改成啟動錯誤。
- `PluginColor` / `UHCTeam` 的 `§` team prefix 輸出已改用 section-code 命名；保留 Step 21 已接受的玩家可見 team prefix 行為。
- unused `update` package、`Messages.Updater`、`Settings.OldEnchant`、預設 `messages.yml` updater section、預設 `settings.yml` OldEnchant section 與未讀取的 CenterCleaner pass/fail resource 欄位已移除。
- production unused 的 `LegacyMatchStateMapper` 與對應測試已刪除。

## 分類盤點

| 類別 | 位置 | 現況 | 建議處理 |
| --- | --- | --- | --- |
| 小型 static facade | `src/main/java/org/mcwonderland/uhc/game/settings/SavedGameSettingsCache.java` | 已由 `UHCGameSettingsSaver` 改名而來；包住 `SavedGameSettingsStore` 與 per-player memory cache。呼叫點仍只有 `WonderlandUHC`、`PluginBootstrap`、`SavedSettingsMenu`。 | 第一刀已處理。不改 `savedgames.db` 格式；後續若要再縮小 static state，應另成獨立小刀。 |
| 大型 static facade | `src/main/java/org/mcwonderland/uhc/game/settings/WorldLoadingCacheState.java` | 已由 `CacheSaver` 改名而來；包住 `WorldLoadingCacheStore`，同時持有 loading status、host、settings、match center。呼叫點橫跨 bootstrap、world selection、pregeneration、login gate、MOTD、hotbar、GUI、scoreboard。 | 第二刀已處理。這刀只改正式命名，不改世界產生 / 重啟 / login gate 語意；後續若要移除 static state，需另成獨立設計。 |
| state bridge | `src/main/java/org/mcwonderland/uhc/game/LegacyGameStateTransitions.java` | 初次盤點時 `Game.nextState()` 用舊 `StateName` 轉 `MatchTransition`，再推進 `UhcMatch`。 | 第三刀已刪除此 class 與測試；`Game.nextState()` 改由 active match state 透過 `MatchTransition.fromSourceState(...)` 取得 transition。 |
| settings mapper | `src/main/java/org/mcwonderland/uhc/core/match/MatchSettingsMapper.java` | 已由 `LegacyMatchSettingsMapper` 改名而來；production 仍由 `Game` 使用，把目前 runtime `UHCGameSettings` 轉成 core `MatchSettings`。 | 第四刀已處理。不改欄位映射。 |
| unused state mapper | `src/main/java/org/mcwonderland/uhc/core/match/LegacyMatchStateMapper.java` | 初次盤點時只被自己的 test 使用，沒有 production call site。 | 第一刀已刪除 class 與 test。 |
| generator method naming | `src/main/java/org/mcwonderland/uhc/game/CenterCleaner.java` `applyConfiguredGeneratorSettings(...)` | 已由 `applyLegacyGeneratorSettings(...)` 改名而來；實際只在 center cleaner 關閉時套用 `Settings.CenterCleaner.GENERATOR_SETTINGS` 到 `WorldCreator.generatorSettings(...)`。 | 第四刀已處理。不改 generator settings 行為。 |
| material alias support | `src/main/java/org/mcwonderland/uhc/platform/item/PluginItems.java`、`src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java` | 已改成正式 `MATERIAL_NAME_ALIASES` 命名；仍接受既有 Material 別名，例如 `WORKBENCH`、`WEB`、`MUSHROOM_SOUP`、`CARROT_STICK`。這是資料值 alias，不是 message format parser。 | 第五刀已處理命名 residue；不移除 alias 行為，避免既有 data folder 舊值直接失效。若未來要移除，需另開行為變更步驟。 |
| sound alias support | `src/main/java/org/mcwonderland/uhc/util/SoundConfigParser.java` | 已改成正式 `SOUND_NAME_ALIASES` 命名；仍接受既有 Sound 別名，例如 `LEVEL_UP`、`ORB_PICKUP`、`NOTE_PLING`。這會影響既有 `sounds.yml` / scenario sound 設定可讀性。 | 第五刀已處理命名 residue；不移除 alias 行為。 |
| section-code color support | `src/main/java/org/mcwonderland/uhc/platform/text/PluginColor.java`、`src/main/java/org/mcwonderland/uhc/game/UHCTeam.java` | `PluginColor` 仍可讀 `&c` / `§c`，`toString()` 輸出 `§` 色碼；`UHCTeam#getPrefix()` 也用 `§l`。Step 21 文件曾明確接受 team prefix / placeholder 的 `§` 色碼輸出。 | 第五刀已改成 section-code 命名，不改玩家可見 team prefix 行為。 |
| Step 24 message literal tests | `src/test/java/org/mcwonderland/uhc/platform/text/PluginTextTest.java` | 測試 `&` 色碼在 `PluginText.toComponent(...)` 中保持 literal，證明 runtime 不再解析 legacy message format。 | 保留。這不是相容層，而是 Step 24 removal gate 的防回歸測試。 |
| accepted removed feature residue | `src/main/java/org/mcwonderland/uhc/settings/Settings.java`、`src/main/resources/settings.yml` | 1.7 舊附魔已被接受移除；先前仍保留 unused settings class 與預設 resource section。 | 第六刀已移除 `Settings.OldEnchant` 與預設 `settings.yml` `OldEnchant` section。既有 data folder 多出的 YAML key 會被 `PluginStaticConfig` 忽略。 |
| unused old updater | `src/main/java/org/mcwonderland/uhc/update/`、`src/main/java/org/mcwonderland/uhc/settings/Messages.java`、`src/main/resources/messages.yml` | `Updaters` / `OldMenusCheck` 只在 update package 內互相引用，沒有 production 啟動 call site；`Messages.Updater` 與預設 `messages.yml` updater section 也沒有 production 使用點。 | 第六刀已刪除 unused `update` package、`Messages.Updater` 與預設 updater messages。 |
| resource-only historical fields | `src/main/resources/settings.yml` CenterCleaner | `Allow_Bad_Biome`、`Range`、`Check_River_In`、`Max_High`、`Bad_Biome_Limit` 是先前 pass/fail 掃描欄位，`Settings.CenterCleaner` 已不讀取。 | 第六刀已從預設 resource 移除未讀取欄位與相容註解；保留仍被讀取的 `Generator_Settings`。 |
| docs historical text | `docs/steps.md`、`docs/message-format-migration.md`、`docs/step-23-message-format-inventory.md` 等 | 大量 `legacy` / `舊版` 是歷史施工紀錄或前步驟決策。 | 不屬於 runtime cleanup。Step 25 只需在文件中標明歷史文字不算 runtime legacy；不要為了搜尋歸零刪歷史紀錄。 |

## 建議切片順序

1. **Saved game settings facade 小刀**
   - 已處理 `UHCGameSettingsSaver` 與 `LegacyMatchStateMapper` 這類 call site 少或 unused 的項目。
   - 目標是降低命中數，不改玩家可見行為。

2. **World loading cache facade 小刀**
   - 已處理 `CacheSaver`，改名為正式 `WorldLoadingCacheState`。
   - 先保留 static entry，但名稱與職責已轉為正式 runtime state；沒有重新包一層 `Legacy` 替代品。

3. **Game state transition bridge 小刀**
   - 已處理 `LegacyGameStateTransitions`。
   - 只替換 transition 決定方式，不重寫 `GameState` queue 或 match lifecycle。

4. **Mapper / naming cleanup 小刀**
   - 已將 `LegacyMatchSettingsMapper` 改成正式 `MatchSettingsMapper`。
   - 已將 `CenterCleaner.applyLegacyGeneratorSettings(...)` 改成正式 `applyConfiguredGeneratorSettings(...)`。

5. **Alias / section-code naming 小刀**
   - 已將 material / sound alias 常數改成正式 alias 命名。
   - 已將 `PluginColor` / `UHCTeam` 的 `legacy` 命名改成 section-code 命名。
   - 保留既有 data folder 可讀性與 Step 21 team prefix 行為。

6. **Dead old feature/resource cleanup 小刀**
   - 已刪除 `update` package unused old updater。
   - 已刪除 `Settings.OldEnchant` 與預設 `settings.yml` 的 `OldEnchant`。
   - 已移除 CenterCleaner 未讀取的 pass/fail resource 欄位。

7. **舊資料格式 fallback 行為移除決策**
   - `PluginItems` / `ScenarioConfig` material alias。
   - `SoundConfigParser` sound alias。
   - `PluginColor` section-code parse / output。
   - 第五刀已把命名改為正式支援；若仍要移除行為本身，會影響既有 data folder 與玩家可見顏色，需另行確認「舊設定直接失效」是否可接受。

## 目前完成 / 未完成判斷

已完成：

- Step 24 訊息格式 runtime legacy parser / serializer 已清空；目前 source 沒有 `LegacyComponentSerializer`、`PluginText.toLegacyString(...)`、`PluginText.toLegacyAmpersandString(...)`、message migration script 或 Gradle task。
- Foundation / DatouNMS / NMS / Packet / COG / 舊 WorldBorder hard dependency 搜尋 gate 目前乾淨。
- `build.gradle` 與 `scripts/` 沒有 Step 25 runtime legacy residue。

未完成：

- `LegacyGameStateTransitions` 已刪除，transition 決策改由 `MatchTransition.fromSourceState(...)` 承接。
- `UHCGameSettingsSaver` 已改名為正式 `SavedGameSettingsCache`，並保留既有 `savedgames.db` 讀寫語意。
- `LegacyMatchSettingsMapper` 已改名為正式 `MatchSettingsMapper`；`LegacyMatchStateMapper` source/test 已刪除。
- 舊 material / sound 設定值 alias 與 section-code color 行為仍保留，但已轉為正式命名；行為移除需另成決策，不列為 Step 25 必刪項。
- `OldEnchant` 設定、unused old updater、unused updater messages 與 CenterCleaner 未讀取 resource 欄位已清理。

## 風險與停損線

- 不因為 `Legacy` 字樣存在就直接刪 class；接在 runtime lifecycle 上的項目需先替換決策來源或呼叫點。
- 不把原 `CacheSaver` 包成另一個新的全域 facade；若要保留 static 入口，也應明確命名為正式 runtime state，而不是過渡相容層。
- 不在 Step 25 重寫 settings parser、scoreboard renderer、GameState queue 或 match lifecycle。
- material / sound / color alias 是舊資料相容問題；要移除前需先接受既有 data folder 舊值會報錯或顯示不同。
