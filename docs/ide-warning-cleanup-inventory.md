# Step 20 IDE 小型警告盤點

建立日期：2026-05-15

本文件承接 `docs/steps.md` Step 20，用來整理目前 IDE / 靜態分析中低風險、低行為影響的 warning。目的不是把所有 warning 一次清到歸零，而是先把可重現、可機械判斷、低風險的項目挑出來，避免把 deprecated API、Foundation、DatouNMS、登入流程或 team color model 混進同一刀。

## 盤點方式

本輪先以 command line 能重現的檢查為基準，再補一輪文字掃描：

```bash
GRADLE_EXTRA_ARGS='-I /tmp/gradle-xlint-step20.gradle --warning-mode all' bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap
```

其中 `/tmp/gradle-xlint-step20.gradle` 只替 `JavaCompile` 加上：

```groovy
allprojects {
    tasks.withType(JavaCompile).configureEach {
        options.compilerArgs.addAll([
            '-Xlint:all',
            '-Xlint:-deprecation',
            '-Xlint:-processing'
        ])
    }
}
```

結果：

- 封裝與測試通過。
- 產出 jar：`build/libs/WonderlandUHC-1.0.0-alpha-2.jar`。
- 完整 log 暫存於 `/tmp/uhc-step20-xlint.log`。
- 本輪刻意排除 deprecated warning；deprecated API 的分類沿用 `docs/step-19-deprecation-inventory.md`，不在 Step 20 處理。
- Command line warning 總數為 `81`。
- VS Code workspace 目前只有本機未追蹤的 `.vscode/settings.json`，內容包含 `java.compile.nullAnalysis.mode=automatic` 與 `java.configuration.updateBuildConfiguration=interactive`；repo 內沒有可提交、可重現的 VS Code / JDT inspection profile。

## 2026-05-15 第一刀後結果

本輪已完成第一批低風險機械清理：

- 移除 3 個檔內未使用 import。
- 將 6 處 `game.getSettings()` / `e.getGame().getSettings()` 類型的靜態方法呼叫改為 `Game.getSettings()`。
- 沒有修改 Foundation / DatouNMS / deprecated API 的實質行為。

驗證方式：

```bash
GRADLE_EXTRA_ARGS='-I /tmp/gradle-xlint-step20.gradle --warning-mode all' bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap
```

結果：

- 封裝與測試通過。
- 產出 jar：`build/libs/WonderlandUHC-1.0.0-alpha-2.jar`。
- 完整 log 暫存於 `/tmp/uhc-step20-xlint-after-first-cut.log`。
- Command line warning 從 `81` 降為 `75`。
- `static` warning 從 `6` 降為 `0`。

Paper `1.21.11` smoke test：

```bash
WINDOWS_SERVER_DIR='/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11' WINDOWS_SERVER_PORT=25567 bash scripts/deploy-to-windows-server.sh --skip-build
```

接著使用測試服資料夾內的 `start.bat` 啟動。

結果：

- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- Paper `1.21.11-130` 啟動並監聽 `25567`。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (19.494s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 連到 `/dev/null`，無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code 回報第一批

使用者從 VS Code Problems 面板回報 5 個檔案的 `is not used` warning。本批處理原則：

- 不使用 `@SuppressWarnings`。
- 只刪除可確認不影響行為的 dead field、dead local variable 與 duplicate import。
- Foundation `ConfigMenu` 反射使用的 button 欄位不刪。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `api/WonderlandUHCApi.java` | `wonderlandUhc`、`pluginUsingApi` 欄位未使用 | 第一批先刪除兩個 private field；後續 Step 20 cleanup 確認全專案沒有程式碼、resource 或文件宣告使用此空 API 入口後，已單獨刪除整個檔案。 |
| `command/impl/host/RespawnCommand.java` | 重複 `org.mcwonderland.uhc.util.*` import 未使用 | 刪除重複 import；保留原本仍被使用的 wildcard import。 |
| `command/TestCommand.java` | local variable `simplePractice` 未使用 | 刪除只建立但未使用的變數、相關 import，以及已無行為的 `clean` 測試分支。 |
| `game/timer/impl/ScatterHandler.java` | field `cacheTP` 未使用 | 刪除欄位與唯一 assignment。 |
| `menu/impl/game/staff/StaffOptionsMenu.java` | 5 個 button field 未使用 | 不刪。`ConfigMenu` 的 `autoRegisterConfigMenuButtons()` 會透過 reflection 掃描所有 `ConfigMenuButton` 欄位並註冊按鈕；刪除會破壞 menu。 |

驗證：

- `GRADLE_EXTRA_ARGS='-I /tmp/gradle-xlint-step20.gradle --warning-mode all' bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- 完整 log 暫存於 `/tmp/uhc-step20-vscode-batch1-xlint.log`。
- Command line warning 維持 `75`；本批主要是 VS Code / JDT unused warning，不屬 javac `-Xlint` 類型。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (20.787s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code 回報第二批

使用者從 VS Code Problems 面板回報 5 個檔案的 null type safety、deprecated 與 unused warning。本批處理原則：

- 不使用 `@SuppressWarnings`。
- null type safety 僅處理 Guava collection helper 與 JDK collection copy 可等價替換的案例。
- deprecated Bukkit metadata API 不在單一 call site 局部硬改；正確處理點是 `LegacyFoundationAdapter` metadata 邊界。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `game/border/TimerBorder.java` | Guava `Lists.newArrayList` / `Queues.newLinkedBlockingQueue` 造成 null type safety warning | 改用 JDK `new ArrayList<>(...)` 與 `new LinkedBlockingQueue<>(...)`，保留原本 shuffle 後 queue 的行為。 |
| `game/CombatRelog.java` | Guava `Lists.newArrayList` 造成 null type safety warning | 改用 JDK `new ArrayList<>(...)`。 |
| `game/CombatRelog.java` | `MetadataValue` deprecated | 暫緩。這是 Foundation / Bukkit metadata bridge 的相容邊界，若要處理應集中收斂到 `LegacyFoundationAdapter`，不在本批只改一個 call site。 |
| `game/player/role/Role.java` | 重複 `role.models.*` import 未使用 | 刪除重複 import。 |
| `game/player/UHCPlayer.java` | Guava `Sets.newHashSet` 造成 null type safety warning | 改用 JDK `new HashSet<>(...)`。 |
| `game/settings/sub/UHCItemSettings.java` | private helper `getItemArray(...)` 未使用 | 刪除未使用 private helper。 |

驗證：

- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (24.010s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code 回報第三批

使用者從 VS Code Problems 面板回報 5 個檔案的 unused import、unchecked cast、null type safety 與 potential null pointer warning。本批處理原則：

- 不使用 `@SuppressWarnings`。
- 保留既有資料載入、join 行為、block drops 與互動事件流程。
- 僅用等價 collection copy 與 runtime checked cast 消除 IDE warning；`InteractListener` 的 block action null guard 後續判定為過度防禦並已回復。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `game/settings/UHCGameSettings.java` | 重複 `settings.sub.*` import 未使用 | 刪除重複 import。 |
| `game/settings/UHCGameSettings.java` | `Object` to `E` unchecked cast | 改用 `clazz.cast(...)` 取得 deserialize 預設值，保留 `SerializedMap#get` fallback 行為。 |
| `game/state/playing/listener/BlockListener.java` | Guava `Lists.newArrayList` 造成 null type safety warning | 改用 JDK `new ArrayList<>(...)`。 |
| `game/state/playing/listener/InteractListener.java` | `clickedBlock` potential null pointer | 已回復防禦式 `clickedBlock != null` guard；依 Bukkit `RIGHT_CLICK_BLOCK` / `LEFT_CLICK_BLOCK` action contract 視為 IDE false positive，不為此增加額外分支。 |
| `game/state/playing/PlayingState.java` | 重複 `playing.listener.*` import 未使用 | 刪除重複 import。 |
| `game/state/share/join/JoinListener.java` | Guava varargs copy 造成 null type safety warning | 改用 `new ArrayList<>(Arrays.asList(joinBehaviors))`，保留可變 list 行為。 |

驗證：

- `git diff --check` 通過。
- 本批修改檔案未新增 `@SuppressWarnings`。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (20.260s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code 回報第四批

使用者從 VS Code Problems 面板回報 5 個檔案的 deprecated、unused field、unused import 與 null type safety warning。本批處理原則：

- 不使用 `@SuppressWarnings`。
- `PlayerLoginEvent` 與 `ChatColor` deprecated warning 暫緩，不在 Step 20 單點改動。
- 僅處理 dead field、duplicate import 與等價 collection copy。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `game/state/share/login/checker/LoginChecker.java` | `PlayerLoginEvent` deprecated | 暫緩。登入事件替換會改登入時機、kick/disallow API 與 `UHCLoginEvent` wrapper contract，需另做專門相容設計。 |
| `game/state/share/login/LoginListener.java` | field `event` 未使用 | 刪除未使用欄位。 |
| `game/state/share/login/LoginListener.java` | Guava varargs copy 造成 null type safety warning | 改用 `new ArrayList<>(Arrays.asList(checkers))`，保留可變 list 行為。 |
| `game/state/share/login/LoginListener.java` | `PlayerLoginEvent` deprecated | 暫緩，理由同登入流程相容設計。 |
| `game/state/share/login/UHCLoginEvent.java` | `PlayerLoginEvent` deprecated | 暫緩，理由同登入流程相容設計。 |
| `game/timer/Timers.java` | 重複 `countdown.*` import 未使用 | 刪除重複 import。 |
| `game/timer/Timers.java` | Guava `Sets.newHashSet` 造成 null type safety warning | 改用 JDK `new HashSet<>(...)`。 |
| `game/UHCTeam.java` | Guava `Sets.newHashSet` 造成 null type safety warning | 改用 JDK `new HashSet<>(...)`。 |
| `game/UHCTeam.java` | `ChatColor` deprecated | 暫緩。Team color model 同時被 API、menu、scoreboard prefix 使用，若要改需整體評估。 |

驗證：

- `git diff --check` 通過。
- 本批修改檔案未新增 `@SuppressWarnings`。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (21.073s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code dump 第一刀

來源：`temp/step20-vscode-warnings.json`。本輪先處理可機械確認的低風險項目，並記錄明確不適合 Step 20 單點修改的項目。

處理原則：

- 不使用 `@SuppressWarnings`。
- 僅處理 duplicate import、明確未使用欄位 / private helper、泛型型別參數遮蔽。
- 可能影響 scenario、Discord、menu reflection、config reflection、Foundation / DatouNMS 或 deprecated API 的項目只記錄暫緩。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `listener/GameSettingsScenarioListener.java` | field `plugin` 未使用 | 刪除未使用欄位；建構子參數仍用來取得 `ScenarioManager`。 |
| `scoreboard/SidebarTheme.java` | 重複 `scoreboard.line.*` import 未使用 | 刪除重複 import。 |
| `tools/Hotbars.java` | 重複 `tools.spectator.*` import 未使用 | 刪除重複 import。 |
| `model/deathmsg/DeathMessageHandler.java` | field `formater` 未使用 | 刪除未使用欄位；未刪除空的 formatter 類別以避免混入 API / 歷史相容判斷。 |
| `practice/SimplePractice.java` | private method `unBreakItem(...)` 未使用 | 刪除未使用 private helper 與對應 import。 |
| `util/UniqueQueue.java` | method type parameter `T` hides class type parameter `T` | 將 `toArray(T[] arg0)` 的 method type parameter 改名為 `E`，符合 `Queue` contract，行為不變。 |

本輪暫緩：

| 檔案 / 類型 | 原因 |
| --- | --- |
| `scenario/impl/special/ScenarioArmorVsHealth.java` 的 `Map<UUID, Double>.remove(UHCPlayer)` | 看起來像真 bug，但會改 Armor Vs Health scenario 行為；等低風險項目處理完後再討論。 |
| `menu/**` 的 button field unused warning | 多數由 Foundation `ConfigMenu` reflection 掃描欄位註冊按鈕，屬 VS Code false positive，不刪。 |
| `settings/** init()` warning | Foundation / settings reflection lifecycle 可能使用，不在 Step 20 刪除。 |
| `DiscordVoiceHook.java` null safety warning | 會影響 Discord voice integration 行為，先記錄暫緩。 |
| `LegacyFoundationAdapter.java` / `LegacyDatouNmsAdapter.java` warning | Foundation / DatouNMS 相容邊界，非 Step 20 單點 cleanup。 |
| `ScenarioVanillaPlus.java` / `ScenarioCutClean.java` enum switch exhaustive warning | 牽涉 scenario 對 Bukkit enum 新常數的處理策略，不在 Step 20 批次中補 case。 |
| deprecated API warning | 沿用 Step 19 分類，不在 Step 20 單點修改。 |

驗證：

- `git diff --check` 通過。
- 本批修改檔案未新增 `@SuppressWarnings`。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (29.092s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code dump 第二刀

來源：`temp/step20-vscode-warnings.json`。本輪處理等價 collection copy；不處理 config / reflection / Discord formatting contract。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `model/InvinciblePlayer.java` | Guava `Maps.newHashMap` 造成 null type safety warning | 改用 JDK `new HashMap<>(...)`，保留 iterate snapshot 行為。 |
| `scenario/impl/death/ScenarioTimeBomb.java` | Guava `Sets.newHashSet` 造成 null type safety warning | 改用 JDK `new HashSet<>(...)`，保留 time-bomb chest snapshot 行為。 |
| `model/deathmsg/DeathMessageLoader.java` | Guava `Lists.newArrayList` / `Sets.newHashSet` 造成 null type safety warning | 改用 JDK `new ArrayList<>(...)` / `new HashSet<>()`。 |

本輪暫緩：

| 檔案 / 類型 | 原因 |
| --- | --- |
| `model/deathmsg/DeathMessageLoader.java` 的 YAML value unchecked cast | 牽涉 death-message YAML 資料形狀與反序列化容錯，不在 collection-copy 批次中改。 |
| `model/Registry.java` raw / unchecked warning | 後續確認 source、resource 與正式文件都沒有引用，已作為 dead code 單獨刪除；不為未使用的 reflection registry 泛型 contract 做 cleanup。 |
| `util/YamlConfigLoader.java` raw type warning | 設定載入反射流程，屬高風險 config lifecycle。 |
| `model/broadcast/impl/DiscordBroadcastSender.java` null type safety warning | `String.join` 與 Discord message formatting 的 null-handling contract 不在本批改。 |

驗證：

- `git diff --check` 通過。
- 本批修改檔案未新增 `@SuppressWarnings`。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (29.396s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code dump 第三刀

來源：`temp/step20-vscode-warnings.json`。本輪只處理非 reflection button 的明確 dead field。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `menu/impl/host/SidebarThemeSettingsMenu.java` | field `player` 未使用 | 刪除未使用欄位；建構子參數仍用於取得 `UHCPlayer`。 |

本輪暫緩：

| 檔案 / 類型 | 原因 |
| --- | --- |
| `menu/impl/InventoryViewer.java` 的 `ConfigDummyButton` field warning | `ConfigMenu` 會透過 reflection 註冊 config button 欄位，屬 VS Code false positive。 |
| `model/tutorial/model/Tutorial.java` 的 `MetadataValue` deprecated | metadata bridge 應集中於 `LegacyFoundationAdapter` 後續處理。 |
| `storage/WorldLoadingCacheStore.java` 的 `clearLoadedSections()` deprecated | Foundation `YamlConfig` cache lifecycle，需後續相容層判斷。 |

驗證：

- `git diff --check` 通過。
- 本批修改檔案未新增 `@SuppressWarnings`。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (31.111s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、DatouNMS 不支援新版提示、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## 2026-05-15 VS Code dump 第四刀

來源：`temp/step20-vscode-warnings.json`。本輪只處理 stale dump 中仍出現的 wildcard-import 類診斷，改為 explicit imports 以避免 VS Code 舊診斷混淆；不碰 menu button reflection、settings lifecycle 或 scenario 行為。

處理結果：

| 檔案 | VS Code warning | 處理 |
| --- | --- | --- |
| `scoreboard/SidebarTheme.java` | `scoreboard.line.*` 顯示 unused import | 改為實際使用的 scoreboard line 類別 explicit imports。 |
| `tools/Hotbars.java` | `tools.spectator.*` 顯示 unused import | 改為實際使用的 spectator item 類別 explicit imports。 |

本輪暫緩：

| 檔案 / 類型 | 原因 |
| --- | --- |
| `menu/**` 的 button field warning | `ConfigMenu` / menu button 欄位可能由 Foundation reflection 註冊，不能當一般 dead field 刪除。 |
| `settings/**` 的 `init()` method warning | `YamlConfig` / settings lifecycle 會反射呼叫初始化方法，不在 Step 20 小刀刪除。 |
| `hook/voice/DiscordVoiceHook.java` 與 `model/broadcast/impl/DiscordBroadcastSender.java` 的 null-safety warning | 涉及 DiscordSRV voice state 與訊息格式 null-handling contract，需單獨討論。 |
| `scenario/impl/special/ScenarioArmorVsHealth.java` 的 `Map<UUID, Double>.remove(UHCPlayer)` 與 `Double::sum` null-safety warning | 看起來可能是實際邏輯問題，但會改 Armor Vs Health scenario 狀態流，先記錄不動。 |
| `scenario/impl/block/ScenarioVanillaPlus.java`、`scenario/impl/rush/ScenarioCutClean.java`、`menu/impl/host/SavedSettingsMenu.java` 的 enum switch exhaustive warning | 增加 default 或補齊 enum case 會影響 scenario/menu 對未知 enum 的處理方式，先暫緩。 |
| `legacy/**`、`storage/WorldLoadingCacheStore.java`、`model/tutorial/model/Tutorial.java` 的 deprecated / raw / metadata warning | 屬 Foundation / DatouNMS / metadata 相容層範圍，後續步驟集中處理。 |

驗證：

- `git diff --check` 通過。
- 本批修改檔案未新增 `@SuppressWarnings`。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (26.426s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示、DiscordSRV console channel 設定提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## Command Line Warning 分類

| 類型 | 數量 | 代表檔案 | Step 20 判斷 |
| --- | ---: | --- | --- |
| `this-escape` | 52 | command、menu、Foundation `YamlConfig` / `SimpleCommand` / button 子類 | 不列為第一輪清理。多數是建構子內呼叫父類或可覆寫方法，若要消除通常會改 lifecycle 或新增 suppress，風險超過小型 warning cleanup。 |
| `rawtypes` | 14 | `util/YamlConfigLoader.java` | `model/Registry.java` 已確認無引用後刪除；剩餘 raw type 牽涉 config loading reflection，延後處理。 |
| `unchecked` | 5 | `util/YamlConfigLoader.java`、`game/settings/UHCGameSettings.java`、`model/deathmsg/DeathMessageLoader.java`、`legacy/LegacyFoundationAdapter.java` | `model/Registry.java` 已確認無引用後刪除；`LegacyFoundationAdapter` 排除到 Step 21。其餘需確認不改資料模型、YAML 結構或 public API 後再處理。 |
| `static` | 0 | 原為 `LobbyCountdown.java`、`StartCountdown.java`、`PortalListener.java`、`DefaultJoinMessage.java`、`WhitelistChecker.java`、`TimerBorder.java` | 第一刀已完成。 |
| `cast` | 1 | `util/GenerateUtil.java` | `CompMaterial#getData()` 的 `(byte)` cast 被 javac 判定 redundant，但該行直接呼叫 `LegacyDatouNmsAdapter#setBlockFast`，先記錄，不在第一輪混入 DatouNMS 路徑。 |

## Unused Import 候選

文字掃描後，再人工排除明顯 false positive，本輪找到 3 個可機械確認的候選：

| 檔案 | 行 | import | 判斷 |
| --- | ---: | --- | --- |
| `src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/AddCommand.java` | 3 | `org.mcwonderland.uhc.UHCPermission` | 第一刀已清。 |
| `src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/PlayingDeathListener.java` | 12 | `org.mcwonderland.uhc.model.InventoryContent` | 第一刀已清。 |
| `src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java` | 3 | `com.google.common.collect.Sets` | 第一刀已清。 |

`src/main/java/org/mineacademy/fo/model/SimpleReplacer.java` 在初步掃描中出現 false positive，實際上 `List`、`Objects`、`StringJoiner` 都仍有使用，不應清理。

## 明確排除到後續步驟

以下項目不列入 Step 20 實作：

1. deprecated API：沿用 Step 19 分類，剩餘項目交給 Step 21 / Step 22。
2. `Foundation` / `DatouNMS` 實質行為與相容層：交給 Step 21。
3. `ChatColor / team color model`、`PlayerLoginEvent`、metadata state：沿用 Step 19 延後決策。
4. `this-escape` 大量 constructor warning：若要真正消除，通常需要重排 command/menu/config lifecycle，不屬於低風險機械清理。
5. Bukkit listener、command、menu、config、serialization、public API 可能透過框架或 reflection 使用的 private member：除非 VS Code / javac / 人工檢查都能證明安全，否則不刪。

## VS Code Dump 收尾盤點

目前 `temp/step20-vscode-warnings.json` 是靜態 dump，仍包含已在前面小刀修掉的舊診斷。收尾判斷如下：

| 類型 | 檔案 / 例子 | 判斷 |
| --- | --- | --- |
| 已處理但 raw dump 仍保留 | `GameSettingsScenarioListener.plugin`、`SidebarThemeSettingsMenu.player`、`DeathMessageHandler.formater`、`SimplePractice.unBreakItem`、`UniqueQueue` generic shadowing、collection copy 類 warning | 目前 source 已無對應 dead member 或已改成低風險寫法，不再重複處理。 |
| menu / config reflection false positive | `menu/**` button 欄位、`settings/** init()` | 需保留給 Foundation / `ConfigMenu` / `YamlConfig` lifecycle 使用，不刪、不加 suppress。 |
| deprecated API / compatibility boundary | `PlayerLoginEvent`、`MetadataValue`、`ChatColor`、`clearLoadedSections()`、`LegacyFoundationAdapter`、`LegacyDatouNmsAdapter` | 屬 Step 21 / Step 22 或 compatibility layer 決策，不在 Step 20 改。 |
| raw / unchecked config / serialization | `YamlConfigLoader.java`、`UHCGameSettings.java`、`DeathMessageLoader.java` | 牽涉 reflection、YAML 資料形狀或 saved settings generic contract，先暫緩討論。`Registry.java` 已確認無引用後刪除。 |
| Discord null-safety | `DiscordVoiceHook.java`、`DiscordBroadcastSender.java` | `DiscordVoiceHook#getVoiceState()` 已確認 JDA 可回傳 null 並單獨修正；Discord 設定 ID 與 broadcast formatting 的 annotation noise 仍暫緩。 |
| scenario gameplay warning | `ScenarioArmorVsHealth.java` | `Map<UUID, Double>.remove(UHCPlayer)` 看起來可能是真 bug，但會改 scenario 狀態流，需討論後再做。 |
| enum switch exhaustive warning | `ScenarioVanillaPlus.java`、`ScenarioCutClean.java`、`SavedSettingsMenu.java` | 已確認三處都是刻意 partial handling；Step 20 cleanup 只加 no-op `default: break`，不補齊 enum case。 |
| constructor lifecycle warning | `this-escape` 大量 command/menu/config 類別 | 若要消除通常會重排 lifecycle 或新增 suppress；本步驟不做過度抽象化。 |

結論：本輪低風險可機械處理項目已清完；剩餘項目都需要功能邊界或後續步驟決策。後續如果 VS Code 產生新的 Problems 清單，仍可追加到 `temp/step20-vscode-warnings.json` 或另建新 dump，再依同一規則分批處理。

## Step 20 Review 後續討論項目

2026-05-15 針對近期 Step 20 commits 做只讀 review。結論是目前沒有看到過度抽象化；沒有新增 wrapper、service、adapter，也沒有為了 warning 重排架構。以下項目不需要立即 revert，但後續若要繼續整理，應單獨討論，不要混入 Armor Vs Health 修正同一刀。

| 項目 | 目前狀態 | 後續要討論的決策 |
| --- | --- | --- |
| `scoreboard/SidebarTheme.java`、`tools/Hotbars.java` explicit imports | 只是把仍使用的 wildcard import 展開；無 runtime 行為差異，必要性偏低但不屬過度抽象。 | Step 20 cleanup 決策完成：保留 explicit imports，不回復 wildcard import，也不列入 Step 21。 |
| `game/state/playing/listener/InteractListener.java` block action null guard | 對 `RIGHT_CLICK_BLOCK` / `LEFT_CLICK_BLOCK` 加 `clickedBlock != null` 後，review 判定為過度防禦。 | Step 20 cleanup 已回復；依 Bukkit block action contract 保留原本直接使用 `clickedBlock` 的流程，VS Code null warning 視為 false positive。 |
| `api/WonderlandUHCApi.java` | 刪除 unused private fields 後只剩空 constructor；已確認目前沒有已知外部插件使用，且全專案沒有程式碼、resource、plugin metadata 或文件宣告引用此 API 入口。 | Step 20 cleanup 已刪除；`api/**` 其餘 event / enum / settings interface 仍被插件內部使用，不一起清理。 |
| `model/deathmsg/DeathMessageFormatter.java` | `DeathMessageHandler` unused field 已移除；全專案沒有程式碼、config 或 reflection 引用這個空類別。 | Step 20 cleanup 已刪除；不混入 `DeathMessageLoader` YAML unchecked cast 或其他 death-message 資料模型調整。 |
| `scenario/impl/special/ScenarioArmorVsHealth.java` | `Map<UUID, Double>.remove(UHCPlayer)` 位於死亡重生後延遲套用 Armor Vs Health 的分支。正常 UHC 流程不會讓死亡玩家重生回遊戲繼續觸發此狀態。 | 不列入目前修正範圍；若未來要支援特殊復活流程，再單獨定義 gameplay 預期後處理。 |
| `model/Registry.java` | 原本是未被引用的 reflection registry 基底類別，只有自己和 cleanup 文件提到。 | Step 20 cleanup 已刪除；不為 dead code 做泛型化或新增 suppress。 |
| `hook/voice/DiscordVoiceHook.java` 的 `Member#getVoiceState()` | JDA source 標示 `getVoiceState()` 可能回傳 null，特別是 voice state cache disabled 時；目前直接呼叫 `.inVoiceChannel()` 可能 NPE。 | Step 20 cleanup 只補本地 null 檢查，將 null voice state 視為「不在語音內」。不重排 Discord voice lifecycle，也不處理設定 ID / broadcast formatter annotation noise。 |
| `ScenarioVanillaPlus.java` / `ScenarioCutClean.java` / `SavedSettingsMenu.java` enum switch | 三處 switch 都只定義少數有意義的 enum 值：`GRAVEL`、可熟化肉類生物、存檔選單左/中/右鍵。未列出的 enum 原本就是 no-op。 | Step 20 cleanup 只加入 no-op `default: break`，明確保留原本不處理其他 enum 的行為；不補齊 `Material` / `EntityType` / `ClickType` 全部 case。 |

## Step 20 Cleanup：WonderlandUHCApi

`api/WonderlandUHCApi.java` 在前面小刀移除 unused fields 後只剩空 constructor。重新盤點 `api/**` 後，判斷 `api/event/**`、`api/enums/**`、`api/game/**`、`api/Scenario` 與 `api/StaffOptions` 仍被插件內部使用，不屬於本 cleanup 範圍；只有 `WonderlandUHCApi` 是孤立空入口。全專案沒有 `new WonderlandUHCApi(...)`、resource、`plugin.yml` metadata 或文件宣告此 API 入口，且目前沒有已知外部插件使用，因此本 cleanup 單獨刪除該檔案。

驗證：

- `rg -n "WonderlandUHCApi|new\s+WonderlandUHCApi|org\.mcwonderland\.uhc\.api\.WonderlandUHCApi" . --glob '!build/**' --glob '!.gradle*/**' --glob '!target/**'` 只剩本文件紀錄，沒有程式碼或 resource 引用。
- `git diff --check` 通過。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (26.194s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## Step 20 Cleanup：InteractListener null guard

`game/state/playing/listener/InteractListener.java` 的 `RIGHT_CLICK_BLOCK` / `LEFT_CLICK_BLOCK` 分支曾為了 VS Code null analysis 加上 `clickedBlock != null`。Review 後判定這是過度防禦：Bukkit block action 本身代表 block interaction，若 action 是 block click 卻沒有 clicked block，應視為 API contract 異常，不應為了 warning 靜默跳過原本流程。因此本 cleanup 回復原本分支條件，將此 IDE warning 視為 false positive。

驗證：

- `git diff --check` 通過。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (27.762s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## Step 20 Cleanup：DeathMessageFormatter

`model/deathmsg/DeathMessageFormatter.java` 是空類別；唯一歷史引用是 `DeathMessageHandler` 的 unused field，已在前面 Step 20 小刀移除。重新掃描後沒有程式碼、resource、config 字串或 reflection 使用此類別，因此本 cleanup 直接刪除該檔案。

驗證：

- `rg -n "DeathMessageFormatter" src/main/java src/main/resources docs` 只剩本文件紀錄，沒有程式碼或 resource 引用。
- `git diff --check` 通過。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (25.636s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 仍無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F` 終止測試服並確認 `25567` 已釋放。

## Step 20 Cleanup：Registry

`model/Registry.java` 帶有 raw type / unchecked warning，但全專案 source、resource 與正式文件都沒有正式引用它。這種情況下不應為未使用的 reflection registry 重寫 generic contract，也不應用 `@SuppressWarnings` 隱藏 warning；本 cleanup 直接刪除 dead code。`YamlConfigLoader.java`、`UHCGameSettings.java` 與 `DeathMessageLoader.java` 仍屬 config / YAML / saved settings lifecycle，不混入本刀。

驗證：

- `rg -n "org\.mcwonderland\.uhc\.model\.Registry|extends\s+Registry|new\s+Registry|class\s+Registry" src/main/java src/main/resources docs` 未找到引用。
- `git diff --check` 通過。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (43.131s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示、Paper outdated 提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F /PID 23028` 終止測試服並確認 `25567` 已釋放。

## Step 20 Cleanup：DiscordVoiceHook voice state

`hook/voice/DiscordVoiceHook.java` 的 `Member#getVoiceState()` 在 JDA API 中標示為 nullable；若 Discord voice state cache 不可用，原本直接呼叫 `.inVoiceChannel()` 可能造成 NPE。本 cleanup 僅在 `moveVoiceChannel` 內加入本地 null 檢查，並沿用既有 `NOT_IN_VOICE` 使用者訊息。這不是重構，也沒有新增 wrapper / service / adapter。

本輪刻意不處理：

- `Settings.DiscordVoice.GUILD_ID`、`VOICE_CATEGORY`、`LOBBY_VOICE` 傳入 JDA `@Nonnull String` 的 annotation warning：若要補完整設定驗證，會變成 Discord voice setup policy，不在 Step 20 小刀處理。
- `DiscordBroadcastSender.java` 的 `sendMessage(msg)` annotation warning：公告模板目前由 `broadcasts.yml` 的 `Formatting` 產生，`String.join` 與 DiscordSRV `convertMentionsFromNames` 都回傳 `String`，先視為 annotation noise。

驗證：

- `git diff --check` 通過。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (38.215s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示、Paper outdated 提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F /PID 20888` 終止測試服並確認 `25567` 已釋放。

## Step 20 Cleanup：enum switch no-op defaults

`ScenarioVanillaPlus.java`、`ScenarioCutClean.java` 與 `SavedSettingsMenu.java` 的 enum switch 都是刻意 partial handling：Vanilla Plus 只處理 `GRAVEL` 的 flint 機率、Cut Clean 只處理幾種可熟化肉類生物、Saved Settings 選單只處理左鍵讀取 / 中鍵覆蓋 / 右鍵刪除。未列出的 enum 在原本流程就是 no-op。

本 cleanup 只加入 `default: break` 讓 no-op 意圖明確化，避免 JDT 要求列出所有 `Material`、`EntityType` 或 `ClickType`；不新增 gameplay 行為，也不補齊所有 enum case。

驗證：

- `git diff --check` 通過。
- `bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap` 通過。
- `plugins/WonderlandUHC.jar` 已更新為本輪封裝出的 `WonderlandUHC-1.0.0-alpha-2.jar`。
- 已部署到 Paper `1.21.11` 測試服並用 `start.bat` 啟動。
- Log 顯示 `[WonderlandUHC] Enabling WonderlandUHC v1.0.0-alpha-2`。
- Log 顯示 `Done (35.123s)! For help, type "help"`。
- 未出現新的 Java exception 或 stacktrace。
- 仍有既有環境警告：Paper fork 偵測訊息、Nashorn placeholder library 缺少提示、Paper outdated 提示；本輪未處理這些 Step 20 範圍外項目。
- 非 TTY 啟動時 stdin 無法送出 console `stop`；本次在 smoke test 完成後用 `taskkill /F /PID 29488` 終止測試服並確認 `25567` 已釋放。
