# WonderlandUHC 1.21.11 升級與解耦重構路線圖

本文件的目標不只是把插件升級到 Minecraft / Paper `1.21.11`，而是把 WonderlandUHC 從目前高度耦合、功能混雜、依賴舊框架與 NMS 的狀態，逐步整理成可長期維護的單一本質插件。

這裡的「單一本質」定義為：

> WonderlandUHC 的核心職責是建立、管理並結束一場 UHC 比賽。

因此主插件應專注於：比賽狀態、玩家/隊伍、世界與邊界、傳送、勝負、可配置規則、必要主持工具。Discord 語音、封包音效控制、自訂礦物插件、舊版附魔模擬、舊 WorldBorder 預生成、複雜 GUI、統計展示、練習模式等，都不應再和核心生命週期綁死。

## 目前問題摘要

- `WonderlandUHC.java` 同時負責載入設定、NMS、listener、command、scenario、world、依賴檢查、stats、scoreboard、practice、Discord hook。
- 業務邏輯直接依賴 Bukkit/Paper、Foundation、DatouNms、PacketListenerAPI、custom-ore-generator、WorldBorder 指令。
- `Game.getGame()`、大量 static util、全域設定與事件 listener 讓流程難測、難替換、難分階段升級。
- `lib-foundation` 被 shading 進插件，但指令、設定、Menu、Remain/NMS、Scoreboard 都深度依賴它。
- 目前建置仍以 Java 8、Spigot API `1.16.5` 為主；目標 Paper `1.21.11` 應以 Java 21 建置與測試。

## 完成定義

完成升級與重構後，不是只要「能編譯」或「能開服」，而是至少要達成下列狀態：

1. 主插件可用 Java 21 / Paper `1.21.11` 完成 `compileJava`、`compileTestJava`、shadow jar 封裝與空服啟動。
2. `bash scripts/package-plugin.sh` 與 `bash scripts/deploy-to-windows-server.sh` 可作為穩定封裝 / 部署入口；腳本與文件說明一致。
3. 沒安裝 `PacketListenerAPI`、PacketEvents、外部 WorldBorder 插件、custom-ore-generator、DiscordSRV 時，核心 UHC 仍可啟動並跑完最小流程。
4. 核心 UHC 流程可運作並可驗證：建立/載入世界、設定 host、玩家加入、隊伍、倒數、傳送、開始、死亡/觀戰、勝負判斷、結束。
5. 核心邊界能力由內建 `BorderService` + `WorldBorderPort` 提供；Paper/Bukkit `WorldBorder` API 只出現在 `platform/paper` adapter，外部 `wb` 指令不可在核心流程中使用。
6. Packet、Discord、自訂礦物、外部 WorldBorder 預生成都只是 optional integration；不可成為 `depend`、`checkDepends()` 或 class loading 的硬條件。
7. `DaTouNMS`、Foundation NMS、`PacketListenerAPI`、`custom-ore-generator` API、`com.wimbli.WorldBorder` API 不得散落在 core/application/listener/menu/scenario；若短期保留，只能在 `legacy/` 或 `integration/` adapter。
8. Foundation 只留在相容層或逐步淘汰區，不再擴散到新業務邏輯；新功能只能依賴明確的 port/service。
9. 設定與資料有 migration/備份策略；舊 material、sound、biome、ore/populator 設定能被明確轉換或停用。
10. A15 搜尋檢查與 A16 最終驗收清單全部通過後，才算升級完成。

## 文件完整性與使用方式

這份文件的定位是「升級加解耦的施工清單」，不是單純的版本升級筆記。依照本文件做完後，目標狀態應該是：

- WonderlandUHC 可在 Java 21 / Paper `1.21.11` 編譯、打包、啟動。
- 核心 UHC 比賽流程可以在沒有 DatouNMS、PacketListenerAPI/PacketEvents、custom-ore-generator、外部 WorldBorder 插件、DiscordSRV 的情況下運作。
- 舊功能若仍保留，會被放在 `legacy/` 或 `integration/`，不再和核心狀態機綁死。
- 新功能的修改位置有明確邊界，不需要在 command、menu、listener、scenario 裡到處補版本判斷。

但要注意：靜態閱讀無法保證列出 100% 的最後編譯錯誤。真正最後一批檔案，必須在 Java 21 / Paper `1.21.11` 依賴切換後，由 `compileJava`、`compileTestJava`、伺服器 smoke test 與人工流程測試收斂。因此本文件採用兩層清單：

1. **已知必改清單**：目前已從專案結構與文字搜尋中確認會被碰到的檔案，附錄 A 已列出。
2. **編譯收斂清單**：升級依賴後由錯誤訊息產生，處理方式是補進 `platform/`、`legacy/` 或對應 application service，不應直接散修在核心流程。

若照本文件完成所有章節，並且最後的編譯、開服、最小遊戲流程測試都通過，就可以視為升級完成。若只完成前半段，例如只完成 build.gradle 與部分 API 修正，最多只能算「可開始收斂 1.21.11 編譯」，還不能算升級完成。

## 舊必要依賴整合判斷

這段是針對原專案實際檔案掃描後的結論。朋友用 vibe coding 得到的方向有參考價值，但不能直接當成目前專案要採用的路線：升級目標不是把舊外掛整包塞進 WonderlandUHC，而是把 UHC 核心必要能力內建成小而清楚的 service，外部插件只作為可選 adapter。

| 依賴 | 目前實際用途 | 是否建議整合進主插件 | 升級處理 |
| --- | --- | --- | --- |
| `PacketListenerAPI` | `WonderlandUHC.checkDepends()` 強制檢查；`PacketRegister` 註冊 `SoundController`；`SoundController` 攔音效與攻擊封包；`ScenarioArmorVsHealth` 攔 `PacketPlayOutUpdateAttributes` 更新血量。 | 不整合舊 `PacketListenerAPI`。`PacketEvents` 可作為新版本封包 adapter，但不可成為核心啟動條件。若要 shade 或內嵌 PacketEvents，需先確認 GPL-3.0 授權與散布方式；保守做法是 `compileOnly`/外部插件/optional adapter。 | 先移除 `Dependency.PACKET_LISTENER_API.check()` 的硬阻塞。音效控制預設停用或重寫成可選功能；`ArmorVsHealth` 優先用 Bukkit Attribute/事件改寫，真的需要封包時才接 `PacketEventsPacketPort`。 |
| 外部 `WorldBorder` 插件 | `WonderlandUHC.checkDepends()` 強制檢查；`checkWorldBorderVer()` 綁外部版本；`BorderUtil` 直接 dispatch `wb clear/set/wshape`；`ChunkFiller` 直接跑 `wb fill`；`WorldFillListener` 依賴 `WorldBorderFillFinishedEvent`。 | 不建議整合整個 WorldBorder 插件。核心邊界應使用 Paper/Bukkit `WorldBorder` API；真正缺的是 chunk pregeneration/fill 與 fill finished event，這兩者應重寫為 WonderlandUHC 自己的 `ChunkPregenerator` 或外部 bridge。 | 拆成 `BorderService`、`ChunkPregenerationService`、`BedrockBorderService`。比賽邊界設定與收縮內建；預生成若一開始風險太高，可以先做成停用時不阻塞開局的 optional workflow。 |
| `custom-ore-generator` | `WorldInitListener` 在 UHC 世界初始化時套用；`CustomOreGeneratorHook` 從 Bukkit services 取 API provider；`Populator`/`OreGen` 直接使用 COG API；`GeneratorSettingsMenu` 顯示 populator 設定。 | 不建議把整個 COG 當核心依賴。COG 是 MIT，理論上可 fork 或抽小部分概念，但 1.18+ 世界高度、deepslate/raw ore 與 1.21.11 支援都要重新驗證；直接整合整個外掛會把另一套世界生成系統的維護成本帶進主插件。 | 建立 `OreGenerationPort`。短期可保留 `CustomOreGeneratorAdapter` 作選配；中期若 UHC 確實需要自訂礦物，實作 WonderlandUHC 專用的小型 ore rule service，並重寫 `populators.yml` 資料模型。 |

補充發現：

- `plugin.yml` 目前把 `WorldBorder`、`PacketListenerApi`、`custom-ore-generator` 都列為 `softdepend`，但 `WonderlandUHC.checkDepends()` 實際會對 `WorldBorder` 與 `PacketListenerAPI` 強制 throw。升級第一步必須先讓宣告與行為一致。
- `SoundController.formatSoundStringFromPacket()` 目前直接回傳空字串，因此 lobby hit/step sound 的封包取消邏輯很可能本來就沒有完整運作。這不是值得優先移植到 PacketEvents 的核心功能。
- 目前外部 WorldBorder 的不可替代點不是「邊界」本身，而是 `wb fill`、`WorldBorderFillFinishedEvent` 與舊式預生成流程。Paper/Bukkit 已有 `WorldBorder` API，可支撐核心比賽邊界。
- `OreGen` 目前只設定 `STONE` replace material，且 `populators.yml` 使用舊版 0-64 高度模型。1.18+ 世界最低高度、deepslate、raw ore 與新版礦物分布都需要重設，不能只更新 COG jar。
- `WorldUtils.getBlockEXP()`、礦物 drop/exp、生成設定與 populator 需要一起檢查，否則即使 COG 能載入，UHC 規則也不一定符合 1.21.11。

### 全文件統一規則

以下規則套用到後續所有步驟與附錄，不只套用在本段依賴分析：

1. 先解除 hard dependency，再重接 optional adapter；不要一開始就替換 jar 或把外掛源碼搬進主插件。
2. 核心 UHC 能力要由內建 service 提供，外部插件只能增強體驗，不能決定插件是否能啟動。
3. 可選 integration 的 API import 只能出現在 `integration/` 或暫時的 `legacy/` adapter，不得散落在 core、application、listener、scenario、menu。
4. 舊 jar 不應是核心 build/runtime 必需項；若短期保留，必須是 `compileOnly`、optional runtime 或明確 legacy-only。
5. 每個階段都要先驗證「沒有外部插件」的狀態，再驗證有安裝 adapter 的狀態。
6. 不要先追求保留所有舊功能；先確保 Paper `1.21.11`、Java 21、無外部插件時可啟動並跑完最小 UHC 流程。

## 架構目標

建議把專案整理成下列邊界。初期可以先新增 package 與 adapter，不需要一次搬完所有舊程式。

```text
org.mcwonderland.uhc
  bootstrap/        插件啟動、依賴組裝、feature gate
  core/             純 UHC 核心規則，盡量不依賴 Bukkit/Foundation
  core/match/       比賽狀態、流程、時間軸
  core/player/      UHC player、role、team domain model
  core/rule/        scenario/rule 的純規則描述
  application/      use cases：start game、teleport、death、end game
  port/             WorldPort、PlayerPort、SchedulerPort、StoragePort 等介面
  platform/paper/   Paper/Bukkit API 實作
  integration/      DiscordSRV、PacketEvents adapter、ore generator 等選配整合
  presentation/     command、menu、tools、scoreboard
  storage/          config、cache、stats、migration
  legacy/           暫時包住 Foundation、DatouNMS、舊 util 的過渡層
```

依賴方向必須固定：

- `core` 不依賴 Bukkit、Paper、Foundation、NMS、外部插件、檔案系統。
- `application` 只依賴 `core` 與 `port`。
- `platform` 實作 `port`，可以碰 Paper API。
- `integration` 是選配模組，不可被 core 直接依賴。
- `presentation` 只負責把 command/menu/event 轉成 application use case。
- `legacy` 只能被 adapter 呼叫，不能被新 core 呼叫。

## 功能取捨原則

升級時先將功能分成三類，避免所有歷史功能繼續互相綁定。

核心必留：

- 建立/刪除/載入 UHC 世界與 Nether
- 玩家加入、離開、重連、死亡、觀戰
- 隊伍、主持人、staff
- 比賽狀態機與計時器
- 邊界設定與收縮
- 基本 scenario/rule 系統
- 基本指令與最小主持操作

可選模組：

- DiscordSRV / voice
- 自訂礦物生成
- Packet/Protocol 類功能
- 統計排行榜
- 複雜 GUI
- Practice mode
- 舊版附魔模擬
- 方塊假邊界視覺效果

建議移除或改成外部插件責任：

- 直接依賴舊 WorldBorder `wb` 指令的 chunk fill
- 只為舊版本相容存在的 NMS hack
- 舊版 Minecraft 專用 workaround
- 和 UHC 核心無關的測試/開發指令

取捨規則：

- 核心必留功能若目前依賴外部插件，先建立內建 service 或 `Noop` service，再決定是否提供 optional adapter。
- 可選模組只能透過 port/service 被呼叫；缺少依賴時只能停用該模組，不可阻塞主插件啟動。
- 舊功能若無法穩定升級到 1.21.11，先降級或停用，不要拖住核心 UHC 流程。
- 不要把舊 `PacketListenerAPI`、外部 WorldBorder 插件、Custom Ore Generator 整包搬進 WonderlandUHC。

## 0. 建立基線與重構保護網

先建立可回歸的基準，再開始拆耦合。

優先閱讀或修改：

- `docs/DEVELOPMENT.md`
- `docs/BASELINE.md`
- `build.gradle`
- `src/test/java/org/mcwonderland/uhc/TestingTest.java`
- `scripts/deploy-to-windows-server.sh`
- `scripts/package-plugin.sh`
- `scripts/bootstrap-foundation-deps.sh`
- `scripts/clean-workspace.sh`

要做的事：

1. 記錄目前 1.16.5 可建置與可啟動基線。
2. 新增最小 smoke test，而不是只保留 `Assert.assertEquals(1, 1)`。
3. 建立 1.21.11 Windows 測試服與文件化 smoke 驗證流程，但先不要求完整遊戲可玩；目前不自動啟動伺服器，也不自動登入遊戲。
4. 在文件中標記所有現有高風險依賴：Foundation、DatouNms、PacketListenerAPI、custom-ore-generator、WorldBorder、DiscordSRV。
5. 每個重構切片都要盡量保持可編譯；若 Java 21 / Paper 1.21.11 版本切換造成預期編譯錯誤，必須集中記錄成收斂清單，不要混入無關架構或功能行為變更。

完成條件：

- 能重現舊基線。
- 有最小自動測試或伺服器 smoke test 能擋住啟動級回歸。

## 1. 拆出啟動組裝層

先把 `WonderlandUHC.java` 從「什麼都做」改成「只負責啟動與組裝」。這一步比升 API 更重要，因為後續所有解耦都需要清楚入口。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/PluginBootstrap.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/FeatureRegistry.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/DependencyReport.java`

要做的事：

1. 將 `loadFiles()`、`setupNms()`、`registerListeners()`、`registerCommands()`、`loadStatsStorage()`、`checkDepends()` 拆到 bootstrap/application services。
2. `WonderlandUHC.onPluginStart()` 只保留：建立 bootstrap、啟動核心、註冊 presentation、輸出啟動結果。
3. 外部依賴檢查改成 `DependencyReport`，回傳 available/disabled/reason，不直接 throw 讓插件死亡。
4. listener、command、scenario、scoreboard、practice、Discord hook 都透過 feature registry 註冊。
5. `TEST_MODE`、`Common.runLater`、`CacheSaver` 等全域行為不要直接散在主類。

完成條件：

- 主類能在 150 行左右表達啟動流程。
- 缺少選配依賴時只停用對應 feature。

## 2. 定義核心 UHC 模型

先把「UHC 是什麼」從 Bukkit listener 與 static util 裡抽出來。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/Game.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/GameManager.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/StateName.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/UHCTeam.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/UHCPlayer.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/*.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/`

要做的事：

1. 新增 `core/match/Match` 或 `UhcMatch`，代表一場比賽，不直接碰 Bukkit。
2. 新增 `MatchState`，替代直接散用 `StateName` 與狀態 class。
3. 新增純資料型 `UhcTeam`、`UhcParticipant`、`MatchSettings`，逐步降低對 Bukkit `Player` 的直接依賴。
4. 將 `Game.getGame()` 的使用逐步改成注入 `MatchService` 或 `MatchRepository`。
5. 先保留舊 `Game` 作 facade，讓舊程式能跑；新程式不再直接呼叫它。

完成條件：

- 核心比賽狀態可以不啟動 Bukkit server 也能做單元測試。
- 舊 `Game` 仍能橋接到新 model，避免一次性大改。

## 3. 建立 Port/Adapter 隔離 Bukkit 與 Paper

這一步是升級與維護性的共同基礎。所有跨版本 API 都應集中在 adapter。

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldBorderPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/PacketPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/OreGenerationPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ChunkPregenerationPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/PlayerPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/SchedulerPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ScoreboardPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/StoragePort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/EventPublisher.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/*.java`

優先被 adapter 包住的舊檔案：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/PlayerUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Extra.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/UHCWorldUtils.java`

要做的事：

1. 把 `WorldUtils.spawnOrb()`、`GenerateUtil.setBlockSuperFast()`、`BorderUtil`、`PlayerUtils.breakBlockNms()` 包到 port。
2. 把 scheduler 包起來，不讓 core/application 直接呼叫 `Common.runLater` 或 Bukkit scheduler。
3. 把訊息與音效輸出包起來，不讓 core 直接碰 Foundation `Common`、`SimpleSound`。
4. 把 storage/config 存取包起來，降低 static config 對核心流程的耦合。
5. 每次新增跨版本 API，都必須先進 port 或 platform adapter。

完成條件：

- 新的 application service 不需要 import `org.bukkit.*`、`org.mineacademy.fo.*` 或外部插件 API。

## 4. 升級建置平台到 Java 21 / Paper 1.21.11

架構入口與 adapter 邊界建立後，再升級建置平台，編譯錯誤會更集中。

優先修改：

- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/settings.gradle`
- `Update-WonderlandUHC/gradle.properties`
- `Update-WonderlandUHC/gradle/wrapper/gradle-wrapper.properties`
- `lib-foundation/pom.xml`
- `scripts/deploy-to-windows-server.sh`
- `scripts/package-plugin.sh`
- `scripts/bootstrap-foundation-deps.sh`
- `scripts/clean-workspace.sh`
- `Update-WonderlandUHC/.github/workflows/build-and-publish-release.yml`

要做的事：

1. Gradle Wrapper 升級到支援 Java 21。
2. Shadow plugin 升級到相容新版 Gradle 的版本。
3. Java target 改成 Java 21 toolchain 或 `release = 21`。
4. `spigot-api:1.16.5` 改成 Paper `1.21.11` API 或 Paper userdev。
5. Lombok 升級到支援 Java 21 的版本。
6. 測試依賴升級；MockBukkit 若不支援 1.21.11，拆成純單元測試與真 Paper smoke test。
7. `lib-foundation/pom.xml` 同步改 Java/API 基準，至少先能 compile。

完成條件：

- 依賴解析與 toolchain 正常。
- 編譯錯誤集中在 API 相容或耦合點，不是 build tool 問題。

## 5. 把 Foundation 降級成 Legacy 相容層

不要再讓 Foundation 成為新架構的中心。短期可以保留它，長期應逐步退出核心。

優先修改：

- `lib-foundation/src/main/java/org/mineacademy/fo/MinecraftVersion.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/SimplePlugin.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/ReflectionUtil.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/Remain.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompMaterial.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompSound.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/nbt/*.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/**/*.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/**/*.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/`

要做的事：

1. `MinecraftVersion` 加上 1.21 或改成可處理未知未來版本的比較。
2. `Remain`、NBT、Reflection 只修到足以支撐現有功能啟動，不要再新增新業務到 Foundation。
3. 建立 `LegacyFoundationAdapter`，集中使用 `Common`、`YamlConfig`、`SimpleCommand`、`Menu`。
4. 新設定、新 command、新 menu 不再直接繼承 Foundation 類別；先走自己的 service/adapter。
5. `CompMaterial`、`CompSound` 可暫時保留為 legacy alias parser，但資料應逐步轉成 Bukkit/Paper 原生 key。

完成條件：

- Foundation 問題不會阻止 core/application 單元測試。
- 新程式碼不再擴散 Foundation import。

## 6. 移除或隔離 DatouNms

目前 `WonderlandUHC.setupNms()` 直接讓不支援版本啟動失敗。這必須變成可替換 adapter。

優先修改：

- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/OldEnchantListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/PlayerUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Extra.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/PlayingDeathListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioShiftKill.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/RespawnCommand.java`

要做的事：

1. 新增 `PlatformCapabilities`，明確列出 absorption、fast block set、custom exp orb、old enchant、death animation、pickup exp control 是否可用。
2. `DaTouNMS` 呼叫全部移到 `legacy/DatouNmsAdapter`。
3. 1.21.11 主線優先用 Paper/Bukkit API 實作。
4. 無穩定 API 的功能先降級或停用，不可阻塞核心比賽。
5. 從 `build.gradle` 移除或改成 legacy-only 依賴。

完成條件：

- `rg "DaTouNMS|datounms" Update-WonderlandUHC/src/main/java` 只剩 legacy adapter 或完全為 0。

## 7. 外部整合全部改成選配模組

外部插件不是 WonderlandUHC 的本質。它們只能增強體驗，不能決定核心是否能啟動。

優先修改：

- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/hook/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioArmorVsHealth.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldInitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/populator/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldFillListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/ChunkFiller.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/libs/PacketListenerAPI_v3.7.9-SNAPSHOT.jar`
- `Update-WonderlandUHC/libs/custom-ore-generator-2022.06.11.jar`

施工順序：

1. **修正 hard dependency**
   - `plugin.yml` 只能宣告 optional integration；`PacketListenerAPI`、外部 `WorldBorder`、`custom-ore-generator` 不可升格為 `depend`。
   - `Dependency.java` 與 `WonderlandUHC.java` 要移除 `Dependency.WORLD_BORDER.check()`、`Dependency.PACKET_LISTENER_API.check()` 與 `checkWorldBorderVer()` 的啟動阻塞。
   - 這一步完成後，即使還沒有新功能，空 Paper server 也不應因缺少這三個外掛而停服。

2. **先內建 UHC 核心邊界**
   - 建立 `BorderService` 作為 application service，透過 `WorldBorderPort` 設定中心、大小、警告、收縮與清除；Paper/Bukkit `WorldBorder` API 只能出現在 `platform/paper` adapter。
   - `BorderUtil.setWBBorder()`、`removeWBBorder()` 的 `wb` 指令要移除或移到 `integration/worldborder/` bridge。
   - `ChunkFiller` 與 `WorldFillListener` 先接 `NoopChunkPregenerationService`，讓預生成缺席時不阻塞開局，再決定是否實作 Paper chunk pregenerator 或外部 bridge。

3. **把 Packet 功能改成可選**
   - 建立 `PacketPort` 與 `NoopPacketPort`，讓 packet 功能缺席時回傳 unavailable，而不是讓插件啟動失敗。
   - `PacketRegister` 與 `SoundController` 不得無條件註冊舊 `PacketListenerAPI` handler。
   - `ScenarioArmorVsHealth` 優先改成 Bukkit Attribute/事件流程；若仍需要封包，再透過 PacketEvents adapter。
   - PacketEvents 只作 optional adapter；不要在授權與散布策略未定前直接 shade。

4. **把自訂礦物改成可選或內建小服務**
   - 建立 `OreGenerationPort` 與 `NoopOreGenerationPort`，讓世界建立流程不直接依賴 COG API。
   - `CustomOreGeneratorHook`、`WorldInitListener`、`OreGen`、`Populator` 的 `de.derfrzocker.*` import 要限縮到 optional adapter。
   - `populators.yml` 要重寫為 1.21.11 資料模型，不沿用舊版 0-64 高度與只替換 `STONE` 的假設。

5. **清掉舊 jar 與驗證**
   - `PacketListenerAPI_v3.7.9-SNAPSHOT.jar` 不應再是 build/runtime 必需項。
   - `custom-ore-generator-2022.06.11.jar` 不應再是核心編譯條件；若保留 COG adapter，依賴要改成 optional/compileOnly 並有缺席測試。
   - 最終用 A15 的 `rg` 指令確認舊 API 只剩在 `integration/` 或完全消失。

6. **DiscordSRV 維持選配**
   - DiscordSRV 只保留 optional voice integration，失敗時只停語音。
   - DiscordSRV 的錯誤不可影響核心開局、死亡、勝負或結束流程。

完成條件：

- 空 Paper 1.21.11 server 可啟動核心 WonderlandUHC。
- 每個 integration 都可獨立啟用、停用、測試。
- `rg "Dependency\\.(WORLD_BORDER|PACKET_LISTENER_API)\\.check" Update-WonderlandUHC/src/main/java` 為 0。
- `rg "dispatchCommand\\(.*wb|WorldBorderFillFinishedEvent" Update-WonderlandUHC/src/main/java` 只剩 optional bridge 或為 0。
- `rg "org\\.inventivetalent\\.packetlistener|de\\.derfrzocker" Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario` 為 0，或只剩明確標記的 legacy 過渡檔。

## 8. 重整設定與持久化邊界

設定目前散在 Foundation static config、db 資源、cache saver 與 game settings。要把「讀寫設定」和「使用設定」分開。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/settings/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/sub/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/update/*.java`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/biomes.yml`
- `Update-WonderlandUHC/src/main/resources/populators.yml`
- `Update-WonderlandUHC/src/main/resources/sounds.yml`
- `Update-WonderlandUHC/src/main/resources/cache.db`
- `Update-WonderlandUHC/src/main/resources/gamecache.db`
- `Update-WonderlandUHC/src/main/resources/savedgames.db`

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/config/`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/migration/`

要做的事：

1. 將 static `Settings.*` 轉成啟動時載入的 immutable settings object，再注入 service。
2. 建立 config version 與 migration，處理舊 material、sound、biome、world generator 設定。
3. 資源檔 alias 逐步寫回新名稱，不長期依賴 `CompMaterial`/`CompSound` 舊 alias。
4. `cache.db`、`gamecache.db`、`savedgames.db` 讀寫要從 core 分離，透過 `StoragePort`。
5. 設定讀取錯誤應明確回報，不應在任意 class static init 時爆炸。

完成條件：

- 核心 use case 可以透過 settings object 測試。
- 舊設定升級前會備份，升級後可讀。

## 9. 重做世界、邊界、傳送與地形服務

世界相關是 UHC 核心，但不應散落在 `Extra`、`WorldUtils`、`BorderUtil`、listener、command 內。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CenterCleaner.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/border/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/teleport/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldInitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/ChunkFiller.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/UHCWorldUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Extra.java`

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/WorldLifecycleService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/BorderService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/TeleportService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/CenterValidationService.java`

要做的事：

1. 世界建立/刪除/載入集中到 `WorldLifecycleService`。
2. 邊界設定/收縮集中到 `BorderService`。
3. 隨機傳送、Nether 對應座標集中到 `TeleportService`。
4. CenterCleaner 改成 `CenterValidationService`，使用 1.21.11 biome key，不再硬寫舊 enum。
5. 高度掃描全部使用 `world.getMinHeight()` / `world.getMaxHeight()`。
6. 礦物與地形規則要包含 1.18+ deep world。
7. Chunk pregeneration 是可選 integration，不是核心世界服務的必要依賴。

完成條件：

- 世界生命週期可以獨立測試與人工驗證。
- command、menu、game state 不再各自直接操作世界細節。

## 10. 重整狀態機與 Application Use Cases

目前狀態切換與 listener 行為分散，應該變成明確的 use case。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/GameTimerRunnable.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CombatRelog.java`

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/CreateMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/StartMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/TeleportTeamsUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/HandleDeathUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/EndMatchUseCase.java`

要做的事：

1. 把 `WAITING -> TELEPORTING -> PRE_START -> PLAYING -> ENDING` 轉成明確 transition。
2. 每個 transition 宣告前置條件、動作、失敗原因。
3. Listener 只把 Bukkit event 轉成 use case 呼叫。
4. Game timer 只負責時間 tick，不直接到處修改世界/玩家/設定。
5. Combat relog 改成 match participant 狀態的一部分，實體替身是 platform implementation。

完成條件：

- 沒有 GUI、scenario、Discord、stats 時，核心比賽流程仍可跑通。
- 狀態轉換可以寫單元測試。

## 11. Scenario 改成規則模組

Scenario 是 UHC 的重要玩法，但不應讓每個 scenario 任意操作全域狀態、NMS、外部插件。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioManager.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioName.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/consume/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/damage/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/disable/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/rush/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/events/UHCBlockBreakEvent.java`

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/rule/Rule.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/rule/RuleSet.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/rule/RuleEngine.java`

要做的事：

1. Scenario 分成純規則、Bukkit event adapter、presentation config 三層。
2. block/drop 類先統一透過 `BlockDropPort`，不要各自手動 drop exp/item。
3. NMS/packet 型 scenario 先獨立 feature gate，例如 `ScenarioArmorVsHealth`。
4. `ScenarioCutClean`、`ScenarioLimitations` 要支援 deep ores/raw ores。
5. `ScenarioSilkWeb`、`ScenarioTripleArrow` 等舊 API 使用點先改到 platform helper。
6. Scenario 啟用/停用不得直接註冊不可控 listener；由 `RuleEngine` 管理。

完成條件：

- 可以只載入核心 rule set 跑遊戲。
- 任一 scenario 壞掉時，不影響其他 scenario 與核心流程。

## 12. Presentation 層瘦身：Command / Menu / Tools / Scoreboard

GUI 與指令是操作入口，不應含核心邏輯。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/tools/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/practice/*.java`
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/commands.yml`
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`

要做的事：

1. Command 只做參數解析、權限檢查、呼叫 use case、輸出結果。
2. Menu 只讀 view model，不直接改 `Game`、世界、scenario。
3. Tools 只觸發 use case，不直接操作玩家狀態。
4. Scoreboard 改成讀取 projection/view model，不直接查全域 mutable state。
5. Practice mode 若保留，應成為 optional feature，不是核心啟動必要條件。
6. Foundation command/menu 先用 adapter 包住，之後逐步替換。

完成條件：

- 任何核心流程都能不用 GUI 完成。
- GUI 壞掉不會阻塞主持人用指令開局。

## 13. Bukkit/Paper 1.21.11 API 修正

當邊界清楚後，再集中修 API。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/*.java`
- 尚未搬移的舊檔案：`util/*.java`、`events/*.java`、`listener/**/*.java`、`menu/**/*.java`、`scenario/**/*.java`

要做的事：

1. `getItemInHand()` 改 main hand。
2. `sendBlockChange(Location, Material, byte)` 改 `BlockData`。
3. `ItemStack#getDurability()` / `setDurability()` 改 `Damageable` ItemMeta 或 DataComponent。
4. `EntityDamageEvent.DamageModifier` 替換成穩定判斷。
5. `ShapedRecipe` 改 `NamespacedKey`。
6. Scoreboard objective criteria 改新版 API。
7. Chat/event API 評估 Paper Adventure。
8. ItemMeta、SkullMeta、Profile、custom model data、component text 集中在 platform adapter。

完成條件：

- 1.21.11 compile 通過。
- API 相容碼集中，不散落在 core/application。

## 14. 測試策略

重構後必須能測核心，而不只靠人工開服。

優先修改或新增：

- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/TestingTest.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/core/**/*.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/application/**/*.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/storage/**/*.java`
- `scripts/package-plugin.sh`
- `scripts/deploy-to-windows-server.sh`

測試分層：

1. Core unit tests：狀態轉換、隊伍、勝負、計時、rule。
2. Application tests：start match、teleport、death、end match，使用 fake ports。
3. Storage tests：config migration、舊資料讀取。
4. Platform smoke tests：真 Paper 1.21.11 開服。
5. Manual checklist：GUI、scenario、Discord、optional integrations。

完成條件：

- core/application 不需要啟動 Bukkit 就能測。
- Paper smoke test 可驗證插件啟動與最小遊戲流程。

## 15. 發布與文件

最後更新使用者文件與開發文件。

優先修改：

- `Update-WonderlandUHC/README.md`
- `DEVELOPMENT.md`
- `Update-WonderlandUHC/.github/workflows/build-and-publish-release.yml`
- `scripts/*.sh`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`

要做的事：

1. README 改成目前支援 1.21.11，並清楚標記 legacy 支援策略。
2. 文件說明核心功能與 optional feature。
3. 列出移除、降級或選配化的舊功能。
4. 開發文件加入架構邊界規則：core 不可 import Bukkit/Foundation/NMS。
5. CI 改 Java 21 與新版 Gradle。

完成條件：

- 新開發者知道該改哪一層。
- 使用者知道升級前需要備份與哪些功能可能變成選配。

## 建議實作順序總表

1. 建立測試與 1.16.5 基線。
2. 拆 `WonderlandUHC.java`，建立 bootstrap 與 feature registry。
3. 定義 core/application/port/platform/legacy package 邊界。
4. 把世界、玩家、scheduler、scoreboard、storage、event 發布包成 port。
5. 升級 Java 21、Gradle、Paper 1.21.11 API，先取得可收斂的編譯錯誤清單。
6. 將 Foundation 降級為 legacy adapter，不再讓新程式碼直接依賴。
7. 將 DatouNMS 移到 legacy adapter 或移除。
8. 將 WorldBorder、PacketListenerAPI、custom-ore-generator、DiscordSRV 改成 optional integration。
9. 修 1.21.11 API 編譯錯誤，集中在 platform/legacy 層。
10. 重做世界/邊界/傳送服務，Paper API 實作放在 platform adapter。
11. 重整狀態機與 use cases。
12. Scenario 改成規則模組，分批升級。
13. 瘦身 command/menu/tools/scoreboard。
14. 做 config migration 與資料備份。
15. 補測試、CI、文件、發布流程。

## 不建議做的事

- 不要只把 API 版本改到 1.21.11，然後繼續維持同樣耦合。
- 不要在 scenario、command、menu 中直接新增 Paper/NMS 判斷。
- 不要讓 optional integration 的失敗造成主插件無法啟動。
- 不要在 core 裡讀 YAML、呼叫 Bukkit scheduler、dispatch command 或操作 inventory。
- 不要一次重寫所有系統；用 adapter 包住舊系統，再逐步替換。

## 附錄 A：目前已知需要修改或檢查的檔案清單

本附錄列出目前靜態掃描到的升級範圍。這些檔案不是全部都要大改；有些只需要確認、有些要移到 adapter、有些要刪除依賴、有些要等編譯錯誤出現後再處理。執行時請用「先分類、再切片、每片保持可編譯」的方式處理。

### A1. 建置、腳本、metadata

這批檔案決定是否能切到 Java 21 / Paper `1.21.11`。

- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/settings.gradle`
- `Update-WonderlandUHC/gradle.properties`
- `Update-WonderlandUHC/gradle/wrapper/gradle-wrapper.properties`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `Update-WonderlandUHC/.github/workflows/build-and-publish-release.yml`
- `lib-foundation/pom.xml`
- `scripts/deploy-to-windows-server.sh`
- `scripts/package-plugin.sh`
- `scripts/bootstrap-foundation-deps.sh`
- `scripts/clean-workspace.sh`

必做事項：

1. Java toolchain 改為 Java 21。
2. Gradle wrapper 與 Shadow plugin 升到支援 Java 21 的版本。
3. Paper API 或 paperweight-userdev 改為 `1.21.11`。
4. `plugin.yml` 的 `api-version` 更新到 1.21 系列，依賴改成 soft/optional feature gate。
5. CI 與本機腳本都使用同一套 Java 21 與輸出 jar。

### A2. 啟動入口與依賴組裝

這批檔案是解耦第一刀，先把插件啟動從巨型主類拆開。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/PluginBootstrap.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/FeatureRegistry.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/DependencyReport.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/PluginServices.java`

必做事項：

1. `WonderlandUHC` 只保留生命週期與 bootstrap 呼叫。
2. 外部依賴檢查不可直接讓主插件死亡，除非是核心必需條件。
3. `setupNms()` 改成能力偵測與 adapter 註冊。
4. listener、command、scenario、scoreboard、practice、integration 的註冊都移到 feature registry。

### A3. DatouNMS 強耦合檔案

這批檔案目前直接或間接使用 DatouNMS，升到 `1.21.11` 時風險最高。目標是讓搜尋結果只剩 `legacy/DatouNmsAdapter`，或完全移除。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/TestCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/RespawnCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/player/RolePlayerApplier.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/spectator/RoleSpectatorApplier.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/staff/RoleStaffApplier.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/PlayingDeathListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/OldEnchantListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioShiftKill.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioTimeBomb.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Extra.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/PlayerUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/build.gradle`

處理方向：

1. 能用 Paper/Bukkit 原生 API 替代的先替代。
2. 無穩定 API 的功能先降級為 optional capability。
3. 只要和核心開局、死亡、觀戰、傳送無關，就不要因為 NMS 不可用而阻塞插件。

### A4. Packet / Protocol 類整合

目前專案使用 `PacketListenerAPI_v3.7.9-SNAPSHOT.jar`，這不是 1.21.11 可靠基礎。PacketEvents 已有 1.21.x/1.21.11 支援路線，但它應該是 optional adapter，不應變成核心啟動條件。掃描後確認目前 packet 用途集中在音效控制與 `ArmorVsHealth` scenario，不是 UHC 開局、隊伍、死亡、勝負的核心必需能力。

- `Update-WonderlandUHC/libs/PacketListenerAPI_v3.7.9-SNAPSHOT.jar`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/hook/packet/PacketRegister.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/hook/packet/SoundController.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioArmorVsHealth.java`
- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`

處理方向：

1. 移除 `Dependency.PACKET_LISTENER_API.check()` 與 `PacketRegister.registerPacketListeners()` 的無條件啟動路徑。
2. 建立 `PacketPort` 與 `NoopPacketPort`；PacketEvents 實作放在 `integration/packet/packetevents/`，並由 feature gate 決定是否註冊。
3. `SoundController` 先不要直接移植。`formatSoundStringFromPacket()` 目前回傳空字串，表示 hit/step sound 判斷很可能不完整；升級時應先把這個功能標成 optional，等有明確需求再用 PacketEvents 重寫。
4. `ScenarioArmorVsHealth` 優先改成 Bukkit Attribute/事件驅動；若仍需攔截 attribute packet，必須透過 `PacketPort`，且沒有 adapter 時 scenario 自動停用並給主持人提示。
5. 若採用 PacketEvents，預設用 `compileOnly` 或外部插件方式；不要在未確認 GPL-3.0 散布影響前直接 shade 進 WonderlandUHC。
6. 最終 `rg "org\\.inventivetalent\\.packetlistener|PacketPlayOut|PacketPlayIn" Update-WonderlandUHC/src/main/java` 只能出現在 optional packet adapter，或完全為 0。

### A5. 自訂礦物與 Populator

自訂礦物應是選配，不應綁住核心世界建立。Custom Ore Generator 有更新到 1.21.x 的紀錄，但目前公開資訊停在 1.21.8 支援，不能假設可直接承擔 1.21.11 核心能力。它的授權是 MIT，代表未來可以 fork 或抽取概念，但不代表應該把完整外掛整合進主插件。

- `Update-WonderlandUHC/libs/custom-ore-generator-2022.06.11.jar`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/hook/CustomOreGeneratorHook.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldInitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/populator/OreGen.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/populator/Populator.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/config/GeneratorSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/config/MainSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/resources/populators.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`

處理方向：

1. 建立 `OreGenerationPort`、`NoopOreGenerationPort`、`CustomOreGeneratorAdapter`。世界建立流程只呼叫 port，不直接 import `de.derfrzocker.*`。
2. `CustomOreGeneratorHook.getOreService()` 要改成 null-safe；service provider 不存在、版本不符、設定讀取失敗時，只停用自訂生成。
3. `WorldInitListener` 不得在 UHC 世界初始化時直接操作 COG API；它應呼叫 application service，再由 adapter 決定是否套用。
4. `populators.yml` 要做 1.18+ 資料模型 migration：支援 `min_y=-64`、`max_y=320`、deepslate/raw ore、replace materials 不只 `STONE`，並明確分 overworld/nether/end。
5. `GeneratorSettingsMenu` 與 `MainSettingsMenu` 在 ore service 不可用時顯示停用狀態，不能 throw 或打開空資料。
6. `WorldUtils.getBlockEXP()`、礦物掉落、exp 與 scenario 若有礦物假設，要一起調整到 1.21.11 材料名稱。
7. 若中期要內建 UHC 專用 ore generator，不要整合完整 COG；只實作本插件需要的礦脈規則、設定讀寫與測試。

### A6. WorldBorder 與 Chunk Fill

核心邊界應改用 Bukkit/Paper `WorldBorder`。外部 WorldBorder 插件的主要價值在舊式 fill/trim 工作流，不是比賽邊界本身。Paper 1.21.11 的 `WorldBorder` API 已提供 center、size、warning、damage 與 `changeSize`，其中舊的 `setSize(double, long)` 路線已被標記 deprecated，升級時要避免把新程式寫在即將移除的 API 上。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldFillListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/practice/SimplePractice.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/ChunkFiller.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `scripts/deploy-to-windows-server.sh`

處理方向：

1. 建立 `BorderService` 負責核心邊界設定、清除、收縮與查詢；它只依賴 `WorldBorderPort`，不直接 import Bukkit/Paper。
2. 建立 `WorldBorderPort` 與 `PaperWorldBorderAdapter`；`World#getWorldBorder()`、`setCenter`、`setSize`、`changeSize` 只能放在 `platform/paper` 實作。
3. `BorderUtil.setWBBorder()`、`removeWBBorder()` 不得再直接 dispatch `wb` 指令；若保留外部 WorldBorder bridge，指令呼叫必須集中在 `integration/worldborder/` 並可整體停用。
4. `ChunkFiller.fill()` 與 `WorldFillListener` 要被 `ChunkPregenerationService` 取代。短期可先提供 `NoopChunkPregenerationService`，讓未預生成時也能開局；中期再實作 Paper chunk loading 或外部插件 bridge。
5. `WorldBorderFillFinishedEvent` 不能留在核心 listener，否則沒裝外部 WorldBorder 時 class loading 會阻塞。
6. bedrock border 生成要從 `DaTouNMS.getWorldNMS().setBlockSuperFast` 轉成 Paper/Bukkit 安全寫方塊流程，或成為 optional visual feature。
7. `checkWorldBorderVer()` 與 `Dependency.WORLD_BORDER.check()` 刪除或移到 optional bridge，不可阻止主插件啟動。
8. smoke test 必須覆蓋沒有外部 WorldBorder 插件時：插件啟動、設定邊界、開始收縮、停止遊戲都可運作。

### A7. Bukkit/Paper API 風險檔案

這批檔案包含舊 API、deprecated API 或跨版本風險。升級依賴後要優先編譯並集中修到 `platform/paper/` 或 `legacy/`。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/TestCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/events/UHCBlockBreakEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CombatRelog.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/UHCTeam.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/border/blockborder/BlockBorder.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/ChatListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/model/InventoryEditButton.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/model/tutorial/model/TutorialListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioSilkWeb.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioTripleArrow.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleSidebar.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Extra.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/PlayerUtils.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/model/ItemCreator.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/model/SkullCreator.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/tool/Tool.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/tool/ToolsListener.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/model/FoundationEnchantmentListener.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/model/SimpleScoreboard.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompEquipmentSlot.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompMaterial.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompMonsterEgg.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/JsonItemStack.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/Remain.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/visual/BlockVisualizer.java`

常見修正項目：

1. `getItemInHand()` 改成 main hand API。
2. `sendBlockChange(Location, Material, byte)` 改成 `BlockData`。
3. durability 改成 `Damageable` ItemMeta 或新版資料 API。
4. 舊 damage modifier 改成新版傷害計算方式。
5. `ShapedRecipe` 改 `NamespacedKey`。
6. skull/profile、scoreboard、chat component、custom model data 集中封裝。

### A8. Core game 與狀態機檔案

這批檔案是插件本質。升級時不能只讓它編譯，要順便把生命週期整理成可測 use case。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/Game.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/GameManager.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/GameTimerRunnable.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/StateName.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/UHCTeam.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CombatRelog.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CenterCleaner.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/UHCPlayer.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/sub/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/teleport/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/border/**/*.java`

狀態機檔案：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/GameState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/SimpleGameState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/PlayingState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/BlockListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/CombatRelogListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/DamageListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/DisableItemListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/GoldenHeadListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/IPvPListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/InteractListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/InventoryListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/ItemListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PaperListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PearlDamageListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PlayerStateListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PlayingJoinListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PlayingLoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PlayingMotdListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PlayingQuitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PortalListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/StatsListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/PlayingDeathListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/UHCDeathDataHandler.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingCommonListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingJoinListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingLoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingMotdListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingQuitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/CommonListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/LobbyQuitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/MotdListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/QuitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/join/ClearBehavior.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/join/DefaultJoinMessage.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/join/JoinBehavior.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/join/JoinListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/join/UHCJoinEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/LoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/UHCLoginEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/checker/LoginChecker.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/checker/WhitelistChecker.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/PreStartState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/StartingJoinListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/StartingLoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/StartingMotdListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/StartingQuitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/StartingState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/TeleportingState.java`

處理方向：

1. listener 不直接做核心決策，只轉成 use case input。
2. 狀態轉換集中管理，不在 listener/menu/command 中任意改 state。
3. `Game.getGame()` 先用 facade 過渡，最後由 application service 取代。

### A9. Scenario 完整檢查清單

Scenario 全部都要檢查，但不代表全部都要同一批改。建議先分成純規則、Paper adapter、optional integration 三種。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioManager.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioName.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/annotation/FilePath.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/AbstractScenario.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ConfigBasedScenario.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioBloodDiamonds.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioDiamondLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioDoubleOrNothing.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioFastObsidian.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioGoldLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioLuckyLeaves.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioSilkWeb.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioTimber.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioTripleOres.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioVanillaPlus.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/ScenarioVeinMiners.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/consume/ScenarioAbsorptionLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/consume/ScenarioFoodNeophobia.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/consume/ScenarioPotionLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/consume/ScenarioSoup.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/damage/ScenarioDamageDogers.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/damage/ScenarioFireLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/damage/ScenarioLessBowDamage.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/damage/ScenarioNoFall.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/damage/ScenarioSwitcheroo.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioNoClean.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioShiftKill.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioSwapInventory.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioTimeBomb.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/disable/ScenarioBowLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/disable/ScenarioHorseLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/disable/ScenarioNoEnchant.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/disable/ScenarioRodLess.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/rush/ScenarioCutClean.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/rush/ScenarioFastSmelting.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/rush/ScenarioHastyBoys.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioArmorVsHealth.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioBackPack.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioBenchBlitz.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioFragileRods.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioIronMan.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioLimitations.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioTripleArrow.java`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`

優先處理順序：

1. 純 Bukkit event 類 scenario。
2. block/drop/exp 類 scenario。
3. death/inventory 類 scenario。
4. packet/NMS 類 scenario。
5. 顯示與 GUI 設定類 scenario。

### A10. Command 完整檢查清單

Command 應逐步改成只負責輸入輸出，不放核心邏輯。每個 command 都要確認它是否直接碰 `Game`、world、scenario、Foundation menu 或外部插件。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/CommandHelper.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/TestCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/UHCCommandGroup.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/EmptyCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/LeaveCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/BackPackCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/PracticeCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/SendCoordsCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/SpecToggleCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/BorderCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/GiveAllCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/MLGCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/RespawnCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/SetSpawnCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/StaffCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/AddCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/ClearCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/ListCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/RemoveCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/WhitelistCommandGroup.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/whitelist/WhitelistSubCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/ConfigCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/DisableItemsCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/ScenariosCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/StatsCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/TopKillsCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/ViewHealCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/ChatCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/CreateCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/DisbandCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/InviteCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/JoinCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/KickCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/LeaveCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/ListCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/PromoteCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/PublicCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/SettingsCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/TeamCommandGroup.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/TeamOwnerCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/team/TeamSubCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/ChooseWorldCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/EditCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/RegenWorldCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/ReloadCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/ResetTeamCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/SetHostCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/SplitTeamCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/StartCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/StopCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/SwitchTeamCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/TpUHCWorldCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/TutorialCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/uhc/UHCMainCommandGroup.java`
- `Update-WonderlandUHC/src/main/resources/commands.yml`

### A11. Menu / GUI / Scoreboard / Tools 檔案

Presentation 層要變薄。GUI 與 scoreboard 不能繼續直接修改核心狀態。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/ButtonLocalization.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/EmptyMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/MainGui.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/UHCMenuSection.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/InventoryViewer.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/PlayersMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/DisableItemListMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/EnabledScenariosMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/StatsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/TeamSelectorMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/TeamSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/staff/StaffMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/staff/StaffOptionsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/BorderSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/BroadcastSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/CenterCleanerMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/GeneratorSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/MainSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/SavedSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/ScenarioSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/ScoreboardSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/SidebarThemeSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/TeamModeSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/TimeSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/model/ColorPickerMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/model/InventoryEditButton.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/model/UHCNumberEditButton.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleSidebar.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/tools/**/*.java`
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`

處理方向：

1. Menu 改讀 view model。
2. Menu action 改呼叫 use case。
3. Scoreboard 改讀 projection，不直接查全域 mutable state。
4. Foundation menu 先由 legacy adapter 包住，之後逐步替換。

### A12. 設定、語言、資料與資源檔

這批檔案需要做 config migration 與 1.21.11 名稱更新。

- `Update-WonderlandUHC/src/main/resources/biomes.yml`
- `Update-WonderlandUHC/src/main/resources/broadcasts.yml`
- `Update-WonderlandUHC/src/main/resources/cache.db`
- `Update-WonderlandUHC/src/main/resources/commands.yml`
- `Update-WonderlandUHC/src/main/resources/gamecache.db`
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/items.yml`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/permissions.txt`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `Update-WonderlandUHC/src/main/resources/populators.yml`
- `Update-WonderlandUHC/src/main/resources/savedgames.db`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/sounds.yml`
- `Update-WonderlandUHC/src/main/resources/spawns.yml`
- `Update-WonderlandUHC/src/main/resources/stats.yml`

處理方向：

1. 先備份舊設定，再跑 migration。
2. material、sound、biome、entity、potion、enchantment 名稱全部檢查。
3. 舊 DB 形態若只是預設資源，確認是否仍應打包進 jar。
4. `plugin.yml` 的 softdepend 與 commands 要和實際 feature registry 對齊。

### A13. Foundation 高風險檔案

Foundation 目前仍是主要基礎，但升級後應變成 legacy layer。以下檔案要分三類：必修才能啟動、可由主插件 adapter 替代、可淘汰。

- `lib-foundation/src/main/java/org/mineacademy/fo/BlockUtil.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/Common.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/MinecraftVersion.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/PlayerUtil.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/ReflectionUtil.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/SerializeUtil.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/ConversationCommand.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/DebugCommand.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/PermsCommand.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/ReloadCommand.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/SimpleCommand.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/SimpleCommandGroup.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/command/SimpleSubCommand.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/Menu.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/MenuContainer.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/MenuContainerChances.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/MenuListener.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/MenuPagged.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/MenuQuantitable.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/menu/MenuTools.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/AutoRegisterScanner.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/EnchantmentPacketListener.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/FoundationFilter.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/FoundationListener.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/Library.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/Reloadables.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/SimplePlugin.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompAttribute.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompBarColor.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompBarStyle.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompBiome.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompChatColor.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompColor.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompEquipmentSlot.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompItemFlag.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompMaterial.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompMetadata.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompMonsterEgg.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompParticle.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompProperty.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompSound.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/CompToastStyle.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/JsonItemStack.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/NmsEntity.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/remain/Remain.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/ConfigItems.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/ConfigSection.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/FileConfig.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/Lang.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/SimpleLocalization.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/SimpleSettings.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/YamlComments.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/YamlConfig.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/YamlConfigLoader.java`
- `lib-foundation/src/main/java/org/mineacademy/fo/settings/YamlStaticConfig.java`

處理方向：

1. `MinecraftVersion` 不能因為未知新版直接失敗。
2. `Remain` 與 NBT 能移除就移除，不能移除就封裝。
3. command/menu/settings 先保持可用，但新程式不得再直接依賴。
4. 若 Foundation 修正成本超過替代成本，優先在主插件建立自己的輕量 adapter。

### A14. 建議新增的核心邊界檔案

這些不是現有檔案，但建議在重構過程新增，用來讓後續修改有固定位置。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/match/Match.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/match/MatchState.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/player/UhcParticipant.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/player/UhcTeam.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/rule/Rule.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/rule/RuleSet.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/CreateMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/StartMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/TeleportTeamsUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/HandleDeathUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/EndMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/WorldLifecycleService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/BorderService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/TeleportService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/CenterValidationService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/rule/RuleEngine.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldBorderPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/PacketPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/OreGenerationPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ChunkPregenerationPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/PlayerPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/SchedulerPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ScoreboardPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/StoragePort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/EventPublisher.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/PaperWorldAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/PaperWorldBorderAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/PaperPlayerAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/PaperSchedulerAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/PaperScoreboardAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/LegacyFoundationAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/DatouNmsAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/discord/DiscordVoiceIntegration.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/ore/OreGeneratorIntegration.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/packet/PacketIntegration.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/worldborder/ExternalWorldBorderIntegration.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/config/ConfigRepository.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/migration/ConfigMigrationService.java`

### A15. 每一批修改後都要執行的搜尋檢查

完成每個階段後，用搜尋確認耦合是否真的下降。

```bash
rg -n "DaTouNMS|datounms" Update-WonderlandUHC/src/main/java
rg -n "Dependency\\.(WORLD_BORDER|PACKET_LISTENER_API)\\.check|checkWorldBorderVer" Update-WonderlandUHC/src/main/java
rg -n "org\\.inventivetalent\\.packetlistener|PacketPlayOut|PacketPlayIn" Update-WonderlandUHC/src/main/java
rg -n "de\\.derfrzocker|custom-ore|CustomOre|OreGenerator" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/src/main/resources
rg -n "dispatchCommand\\(.*wb|WorldBorderFillFinishedEvent|com\\.wimbli\\.WorldBorder" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/src/main/resources
rg -n "Game\\.getGame\\(|Common\\.run|Bukkit\\.getScheduler|YamlStaticConfig|SimpleCommand|Menu" Update-WonderlandUHC/src/main/java
rg -n "getItemInHand|getDurability|setDurability|DamageModifier|sendBlockChange|ShapedRecipe|SkullMeta|Objective" Update-WonderlandUHC/src/main/java lib-foundation/src/main/java
rg -n "org\\.bukkit|org\\.mineacademy|net\\.minecraft|nms" Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application
```

判斷標準：

- `core/` 不應出現 Bukkit、Foundation、NMS import。
- `application/` 可以依賴 `port/`，不應直接依賴 Paper 或 Foundation。
- `integration/` 可以依賴外部插件 API，但必須可停用。
- `legacy/` 可以包住舊 API，但不可被 core 直接呼叫。
- `WonderlandUHC.java` 不應再包含大量註冊與流程細節。
- `Dependency.WORLD_BORDER.check()`、`Dependency.PACKET_LISTENER_API.check()` 與 `checkWorldBorderVer()` 不應再存在於啟動路徑。
- `org.inventivetalent.*`、`de.derfrzocker.*`、`com.wimbli.WorldBorder.*` 只能出現在 `integration/` 或暫時 `legacy/`，不能出現在 core、application、listener、menu、scenario。
- Bukkit/Paper `WorldBorder` API 只能存在於 `platform/paper` 的 `WorldBorderPort` 實作，不應出現在 application `BorderService`，也不應再和外部 `wb` 指令混在同一個 util。

### A16. 最終驗收清單

完成所有修改後，要用這份清單判定是否真的升級完成。

- `Update-WonderlandUHC` 可用 Java 21 執行 `compileJava`。
- `Update-WonderlandUHC` 可用 Java 21 執行 `compileTestJava`。
- `Update-WonderlandUHC` 可產出 shadow jar。
- `lib-foundation` 若仍被使用，必須能在 Java 21 與 1.21.11 API baseline 下編譯。
- 空 Paper `1.21.11` server 放入 jar 後可啟動，不因選配依賴缺失而停服。
- `/uhc` 或主要 command 可註冊。
- 可建立或載入 UHC 世界。
- 可設定 host、隊伍、邊界、scenario。
- 可從 waiting 進入 teleporting、pre-start、playing。
- 玩家死亡後可進入正確 spectator/staff/player 狀態。
- 最小勝負判斷與結束流程可跑完。
- 缺少 DiscordSRV、custom-ore-generator、PacketListenerAPI/PacketEvents、外部 WorldBorder 插件時，只停用對應非核心功能；核心邊界、開局、死亡、勝負與結束流程仍由內建 service 運作。
- config migration 會備份舊檔並產生可讀的新設定。
- README、開發文件、啟動腳本與 CI 都更新為 1.21.11。

## 參考資料

- Paper 1.21.11 下載頁：https://papermc.io/downloads/paper/
- Paper Fill v3 builds API：https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds
- Paper 1.21.11 API 文件：https://jd.papermc.io/paper/1.21.11/
- Paper Java 需求：https://docs.papermc.io/paper/getting-started/
- Paper `plugin.yml` 文件：https://docs.papermc.io/paper/dev/plugin-yml/
- Paper paperweight-userdev 文件：https://docs.papermc.io/paper/dev/userdev/
- Paper Data Component API：https://docs.papermc.io/paper/dev/data-component-api/
- Paper 1.21.11 `WorldBorder` API：https://jd.papermc.io/paper/1.21.11/org/bukkit/WorldBorder.html
- PacketEvents Modrinth：https://modrinth.com/plugin/packetevents
- PacketEvents 1.21.11 支援更新：https://www.spigotmc.org/resources/packetevents-api.80279/update?update=620382
- PacketEvents GitHub 與授權：https://github.com/retrooper/packetevents
- Custom Ore Generator Spigot：https://www.spigotmc.org/resources/custom-ore-generator-%E3%80%8E1-8-1-21-8%E3%80%8F.64339/
- Custom Ore Generator GitHub 與授權：https://github.com/DerFrZocker/Custom-Ore-Generator
- WorldBorder Spigot：https://www.spigotmc.org/resources/worldborder.60905/
- WorldBorder Renew 1.18-1.21：https://www.spigotmc.org/resources/worldborder-renew-1-18-x-1-21-x.126588/
