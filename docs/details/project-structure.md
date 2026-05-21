# WonderlandUHC 專案架構細節說明

整理日期：2026-05-21

這份文件補充 `docs/project-structure.md`，用維護者視角說明目前資料夾與 Java package 的邊界。重點不是要求後續維護者一次把舊結構搬乾淨，而是協助判斷新增功能、修 bug 或調整相容層時應該先看哪裡。

## 整體分層

| 層級 | 主要 package | 定位 |
| --- | --- | --- |
| 規則 / 模型層 | `core` | 不依賴 Bukkit / Paper 的 UHC 狀態、規則與轉換判斷。 |
| 流程層 | `application` | 比賽、世界、登入、border 等可被 command / menu / listener 呼叫的 use case。 |
| 平台邊界層 | `port`、`platform`、`integration` | Paper / Bukkit、外部插件與可替換能力的邊界。 |
| 既有功能層 | `game`、`command`、`menu`、`scenario`、`scoreboard`、`settings` 等 | 原插件主要功能與 Paper `1.21.11` 相容整理後保留的實際運行流程。 |

目前的新架構是漸進式整理，不代表所有功能都已完全移入 `core` / `application`。維護時應看實際風險與收益，不以清空舊 package 為目標。

## 根目錄

| 路徑 | 維護定位 |
| --- | --- |
| `src/main/java/` | 插件 Java 實作。 |
| `src/main/resources/` | jar 內預設 resource 與 `plugin.yml`。伺服器已有同名設定檔時不會自動覆蓋。 |
| `src/test/java/` | 不需啟動 Paper 的單元測試與行為測試。 |
| `build.gradle`、`gradlew`、`gradle/` | Gradle 建置設定與 wrapper。 |
| `libs/` | 本地 compileOnly jar，目前用於 Chunky API。 |
| `docs/` | 維護摘要。根目錄保留短文件。 |
| `docs/details/` | 細節文件。修改對應系統前再讀。 |

## 新架構 Package

### `core`

`core` 放純規則與狀態模型，不應依賴 Bukkit / Paper。

適合放：

- 比賽狀態轉換。
- 純資料模型。
- 可用一般 unit test 驗證的規則。

不適合放：

- `Player`、`World`、`JavaPlugin`。
- 傳送、發訊息、GUI、事件註冊。

### `application`

`application` 放跨入口流程。

目前代表區域：

- `application.match`
- `application.world`
- `application.border`
- `application.login`

適合放：

- command / menu / listener 都可能共用的流程。
- 世界選圖、預生成、比賽開始、停止、登入權限等 use case。
- 可透過 port 隔離平台細節的流程。

如果邏輯只有單一使用點且很薄，不必為了形式新增 service。

### `port`

`port` 定義平台能力介面，例如 scheduler、world、asset、event publisher、Chunky 預生成。

新增 port 前應確認至少符合一項：

1. 需要在測試中替換 Paper 行為。
2. 同一流程可能有不同運行實作。
3. 外部 API 會污染純流程。

只有單一使用點時，不要為了抽象而新增 port。

### `platform`

`platform` 放 Paper / Bukkit / Adventure adapter 與平台 helper。

代表區域：

- `platform.paper`
- `platform.scheduler`
- `platform.text`
- `platform.item`
- `platform.menu`
- `platform.console`
- `platform.player`
- `platform.sound`

Paper 版本升級、Adventure 文字格式、item / menu 顯示問題，多半先看這裡。

### `integration`

`integration` 放外部插件 API 邊界。

目前包含：

- `integration.luckperms`
- `integration.chunky`
- `integration.ChunkPregenerationAdapters`

維護時應把外部 API 呼叫集中在這裡或既有 hook 邊界，不要讓 LuckPerms / Chunky / DiscordSRV 呼叫散落到主要遊戲流程。

## 既有功能 Package

### `game`

`game` 仍是實際遊戲流程核心。

包含：

- `Game` 與 state queue。
- `StateName`。
- state listener。
- timer。
- team、player、border、teleport。
- `CenterCleaner` 運行流程。
- `WorldLoadingCacheState`。

小修 bug 可直接在原位置修。若某段流程開始被多個入口共用，再考慮抽到 `application`。

### `command`

`command` 是指令入口，負責：

1. 解析參數。
2. 檢查 sender 與權限。
3. 呼叫流程。
4. 回覆訊息。

不建議把大型流程堆在新的 command class。若 GUI 或 listener 也會用到同一流程，應考慮抽出。

### `menu`

`menu` 是 inventory GUI。

目前仍有部分既有寫法會直接操作 `Game.getSettings()`、cache 或事件細節。這些不需要為了目錄漂亮一次拆掉；只有在減少重複、降低風險或修 bug 時才整理。

### `scenario`

`scenario` 放 UHC scenario 的註冊、設定、listener 與實作。

新增或修改 scenario 時，應同時看：

- Java scenario class。
- `scenarios.yml`。
- `@FilePath` 設定注入。
- scenario 之間的組合風險。

### `scoreboard`

`scoreboard` 管理 sidebar、tab、below-name health 與 theme。

`SidebarTheme.loadThemes()` 會從 `scoreboards.yml` 讀取所有 theme。修改 scoreboard 時，除了 YAML，也要確認 placeholder 與 line 類別是否一致。

### `settings`

`settings` 管理靜態設定載入：

- `Settings`
- `Messages`
- `CommandSettings`
- `Sounds`
- `UHCFiles`
- `PluginStaticConfig`

新增設定時通常要同步修改預設 YAML、Java 欄位、解析 / fallback 與啟動驗證。設定檔載入面見 `docs/details/config-surface.md`。

## 資料與儲存

| Package / 檔案 | 定位 |
| --- | --- |
| `storage` | `cache.db`、`savedgames.db` 等運行資料讀寫。 |
| `stats` | `stats.yml` 統計資料。 |
| `settings/spawn` | `spawns.yml` 座標資料。 |
| `game/settings/UHCGameSettings` | 本場遊戲設定快照。 |

`cache.db`、`savedgames.db`、`stats.yml` 是運行資料，不應作為 jar 預設 resource 維護。

## 新功能放置判斷

| 需求 | 優先位置 |
| --- | --- |
| 純規則、狀態轉換、可無 Paper 測試 | `core` |
| 跨 command / menu / listener 的流程 | `application` |
| Paper API adapter | `platform` |
| 外部插件 API adapter | `integration` 或既有 hook 邊界 |
| 新指令入口 | `command` |
| 新 GUI 畫面或按鈕 | `menu` 搭配 `gui.yml` |
| 新 scenario | `scenario` 搭配 `scenarios.yml` |
| 新文字 / 音效 / 預設設定 | `settings` 與對應 resource |

## 避免過度整理

通常不需要新增抽象的情況：

1. 只有單一使用點，且邏輯很薄。
2. 改動只是為了讓 package 名稱更整齊。
3. 抽出後沒有更容易測試，也沒有減少重複。
4. 大範圍搬移才能完成，但當前 bug 可用小修解決。

比較值得整理的情況：

1. 同一流程已在 command、menu、listener 重複出現。
2. Paper API 或外部插件 API 讓核心流程很難測。
3. 修改一個功能需要同時理解太多無關細節。
4. 版本升級時同類錯誤反覆出現。

## 細節文件入口

| 文件 | 何時閱讀 |
| --- | --- |
| `docs/details/plugin-dependencies.md` | 修改 LuckPerms、Chunky、DiscordSRV 或 dependency report 時。 |
| `docs/details/game-flow.md` | 修改選圖、跑圖、cache、開局、登入限制或 timer 時。 |
| `docs/details/map-selection.md` | 修改 CenterCleaner、候選中心或分數門檻時。 |
| `docs/details/text-format.md` | 修改文字格式、placeholder、MiniMessage 或 Discord 格式時。 |
| `docs/details/config-surface.md` | 新增或修改 YAML 設定面時。 |
| `docs/details/verification.md` | 判斷修改後需要跑哪些封裝與實機驗證時。 |
