# WonderlandUHC 外部整合細節說明

整理日期：2026-05-23

這份文件補充 `docs/plugin-dependencies.md`，說明 LuckPerms、Chunky、DiscordSRV 在程式中的整合邊界。重點是給維護者理解依賴如何宣告、如何檢查、缺少時哪些流程會受影響，以及修改外部整合時應該注意哪些風險。

## 依賴宣告位置

| 層面 | 檔案 | 用途 |
| --- | --- | --- |
| Paper 載入順序 | `src/main/resources/plugin.yml` | 宣告 `softdepend`，讓 Paper 優先載入可用外部插件。 |
| 編譯期依賴 | `build.gradle`、`libs/` | 讓 Java 編譯時找得到外部 API。 |
| 啟動時檢查 | `Dependency`、`PluginBootstrap` | 啟動時輸出依賴插件狀態。 |

`plugin.yml` 目前宣告：

```yaml
softdepend: [ LuckPerms, Chunky, DiscordSRV ]
```

`build.gradle` 目前使用：

```gradle
compileOnly 'net.luckperms:api:5.5'
compileOnly 'com.discordsrv:discordsrv:1.30.5'
compileOnly name: 'Chunky-Bukkit-1.4.40'
```

這些都是 `compileOnly`，外部插件本體不會被打包進 WonderlandUHC jar。

## Dependency Report

`Dependency` enum 保存外部插件名稱與下載網址。`PluginBootstrap.checkDependencies()` 啟動時會依序檢查：

1. LuckPerms: runtime required。
2. Chunky: runtime required。
3. DiscordSRV: optional。

狀態意義：

| Console 顯示 | 內部狀態 | 意義 |
| --- | --- | --- |
| `可用` | `Available` | Bukkit plugin 存在且已啟用。 |
| `未啟用` | `Disabled` | optional plugin 不存在或未啟用，對應功能不可用。 |
| `缺少` | `Unavailable` | required plugin 不存在或未啟用，部署狀態不正確。 |

目前檢查只看 `Bukkit.getPluginManager().getPlugin(name)` 與 `plugin.isEnabled()`。

維護注意：

1. `plugin.yml` 的 `softdepend` 用於 Paper 載入順序；`PluginBootstrap.checkDependencies()` 才是 runtime 必要 / 可選判斷來源。
2. 新增外部插件時，應同步更新 `Dependency`、`plugin.yml`、`build.gradle` 與文件。
3. optional plugin 缺少時，入口應有明確停用或錯誤，不應讓流程默默卡住。

## LuckPerms

LuckPerms 是必要依賴，主要用於登入階段權限查詢。

主要位置：

| 類別 | 用途 |
| --- | --- |
| `integration.luckperms.LuckPermsLoginPermissionService` | 使用 LuckPerms API 查詢登入前權限。 |
| `application.login.LoginPermissionService` | 登入權限查詢介面。 |
| `game.state.share.login.LoginListener` | Paper login event 入口。 |
| `game.state.share.login.UHCLoginEvent` | 提供 `hasPermission(...)` 給 login checker。 |
| `PreparingLoginListener`、`PlayingLoginListener`、`WhitelistChecker` | 依世界 / 比賽狀態判斷是否允許登入。 |

`LuckPermsLoginPermissionService` 目前行為：

1. 使用 UUID 與名稱載入 LuckPerms user。
2. 最多等待 `3` 秒。
3. 查詢 permission data。
4. 明確 `TRUE` 時允許。
5. 明確 `FALSE` 且有 node 時拒絕。
6. 未明確設定時，額外檢查 Bukkit OP。
7. 查詢失敗時記錄錯誤並 fail closed。

fail closed 代表該次權限檢查回傳 `false`，避免 LuckPerms 查詢失敗時讓玩家意外 bypass login gate。

主要登入 gate 權限：

| 權限 | 用途 |
| --- | --- |
| `wonderland.uhc.bypass.join.configuring` | 在設定 / 預覽階段進入伺服器。 |
| `wonderland.uhc.bypass.join.full` | 滿人時進入。 |
| `wonderland.uhc.bypass.join.started` | 遊戲開始後進入。 |
| `wonderland.uhc.bypass.join.whitelist` | 插件白名單開啟時進入。 |

維護注意：

1. 修改登入限制時，要同時看 `LoadingStatus` 與 `StateName`。
2. 不要把 LuckPerms API 呼叫散落在多個 listener。
3. 若要支援其他權限來源，應從 `LoginPermissionService` 邊界著手。

## Chunky

Chunky 是 softdepend，但啟動檢查會將它視為必要外部插件。

也就是：

- 缺少 Chunky 時，WonderlandUHC 會停止啟用。
- Chunky 可用時，新的 UHC 世界才能進入正式預生成流程。

`libs/Chunky-Bukkit-1.4.40.jar` 只用於本地編譯。實際運行時仍要由伺服器 `plugins/` 內的 Chunky 提供 API。

主要位置：

| 類別 | 用途 |
| --- | --- |
| `port.ChunkPregenerationPort` | WonderlandUHC 對預生成能力的需求介面。 |
| `integration.ChunkPregenerationAdapters` | 依 Chunky 是否 hooked 選擇 adapter。 |
| `integration.chunky.ChunkyPregenerationAdapter` | 實際呼叫 Chunky API。 |
| `application.world.ChunkPregenerationService` | WonderlandUHC 世界預生成流程。 |
| `util.ChunkFiller` | 從世界載入狀態接回預生成流程。 |

缺少 Chunky 時，啟動檢查會先停止 WonderlandUHC；missing adapter 仍保留作為預生成入口的防護，會丟出：

```text
Chunk pregeneration requires Chunky. Install Chunky and restart the server.
```

`ChunkyPregenerationAdapter` 目前呼叫 `ChunkyAPI.startTask(...)`，使用：

- world name。
- shape: `square`。
- center X / Z。
- radius X / Z。
- pattern: `region`。

注意：舊版 `settings.yml` 的 `ChunkLoading` 數值已不再由目前程式讀取。`ChunkPregenerationService` 只依本場初始邊界計算預生成半徑，並使用 `MatchCenter` 作為 Chunky 任務中心。若未來要新增可調 Chunky 參數，應以新的設定名稱、程式欄位與文件一起設計，不要恢復舊 `ChunkLoading` 語意。

## DiscordSRV

DiscordSRV 是 optional integration。

主要功能：

- Discord 開場公告。
- Discord 語音頻道。
- `/reconnect` 將玩家移回大廳或隊伍語音。

主要位置：

| 類別 / 檔案 | 用途 |
| --- | --- |
| `model.broadcast.impl.DiscordBroadcastSender` | Discord 文字公告送出。 |
| `hook.voice.DiscordVoiceHook` | Discord 語音整合。 |
| `hook.voice.TeamVoices` | 隊伍與語音頻道對應。 |
| `FeatureRegistry.setupDiscordVoiceHook()` | DiscordSRV hooked 時才 setup voice hook。 |
| `broadcasts.yml` | Discord channel 與公告格式。 |
| `settings.yml` 的 `DiscordVoice` | Discord 語音設定。 |

DiscordSRV plugin enabled 不代表 JDA 已 ready。語音 hook 會等待 `DiscordSRV.isReady`；文字公告送出時也會檢查 ready 狀態。

維護注意：

1. DiscordSRV 是 softdepend，不要在 plugin enable 早期無條件呼叫 DiscordSRV API。
2. guild、category、lobby voice channel 缺少時，應記錄 console 訊息並停用對應 voice 行為。
3. Discord 公告會移除 Minecraft 顏色，格式細節見 `docs/details/text-format.md`。

## 修改依賴檢查表

新增或調整外部插件整合時，至少檢查：

1. `build.gradle` 是否有正確 `compileOnly`。
2. `plugin.yml` 的 `softdepend` 與 `PluginBootstrap.checkDependencies()` 的 runtime 必要 / 可選判斷是否符合實際需求。
3. `Dependency` enum 是否有 plugin name 與 download URL。
4. `PluginBootstrap.checkDependencies()` 是否會輸出正確狀態。
5. 缺少 optional plugin 時，入口是否有明確停用或錯誤。
6. 外部 API 是否只存在於 `integration`、hook 或明確邊界。
7. package 後 jar 是否沒有誤包外部插件。
8. 啟動 log 是否包含合理的依賴插件狀態。

通用驗證流程見 `docs/details/verification.md`。
