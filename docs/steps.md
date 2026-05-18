# WonderlandUHC 1.21.11 升級與解耦重構路線圖

本文件的目標不只是把插件升級到 Minecraft / Paper `1.21.11`，而是把 WonderlandUHC 從目前高度耦合、功能混雜、依賴舊框架與 NMS 的狀態，逐步整理成可長期維護的單一本質插件。

這裡的「單一本質」定義為：

> WonderlandUHC 的核心職責是建立、管理並結束一場 UHC 比賽。

因此主插件應專注於：比賽狀態、玩家/隊伍、世界建立與預生成、邊界、傳送、勝負、可配置規則、必要主持工具。DiscordSRV、封包音效控制、自訂礦物插件、舊版附魔模擬、舊 WorldBorder 插件實作、GUI、統計展示、練習模式等，不能再以舊式 hard dependency 和核心生命週期綁死；但舊版已提供且被確認仍要保留的功能，必須透過原生 API、選配 integration 或後續專用實作完整保留。已被明確接受移除的功能，例如 MySQL / 長期資料庫 stats 路線與 1.7 舊附魔模擬，不再列為新版修復目標。

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
3. 沒安裝 `PacketListenerAPI`、PacketEvents、舊外部 WorldBorder 插件、DiscordSRV 時，核心 UHC 仍可啟動並跑完最小流程；邊界與預生成不得因此缺席，若改採新外部替代插件，該插件必須列入升級後部署條件並通過驗證。DiscordSRV 的語意沿用舊版：有安裝且設定啟用時要完整提供功能，未安裝時只停用對應整合。舊 `custom-ore-generator` 已實測不支援 Paper `1.21.11`，自訂礦物功能在本輪升級暫停，升級完成後重新設計。
4. 核心 UHC 流程可運作並可驗證：建立/載入世界、預生成、設定 host、玩家加入、隊伍、倒數、傳送、開始、死亡/觀戰、勝負判斷、結束。
5. 邊界能力由明確的 `BorderService` + `WorldBorderPort` 協調；預生成由明確的 `ChunkPregenerationService` 協調；實作來源可以是 Paper/Bukkit 原生 API、已驗證的新版本外部插件 adapter，或最後才考慮的專用內部實作。
6. Packet、自訂礦物、DiscordSRV、舊 WorldBorder bridge 都不能沿用舊 hard dependency；若某個新版本替代插件被決定為保留舊功能的必要實作，必須明確寫成升級後部署依賴，而不是默默在 class loading 或 `checkDepends()` 中失敗。DiscordSRV 公告與隊伍語音已被確認要完整保留，但只能在 DiscordSRV 可用時啟用。
7. `DaTouNMS`、Foundation NMS、`PacketListenerAPI`、`custom-ore-generator` API、`com.wimbli.WorldBorder` API 不得散落在 core/application/listener/menu/scenario；若短期保留，只能在 `legacy/` 或 `integration/` adapter。
8. Foundation 只留在相容層或逐步淘汰區，不再擴散到新業務邏輯；新功能只能依賴明確的 port/service。
9. 設定與資料有 migration/備份策略；舊 material、sound、biome、ore/populator 設定能被明確轉換或停用。
10. A15 搜尋檢查、1.16.5 原版功能對照檢核與 A16 最終驗收清單全部通過後，才算升級完成。

## 文件完整性與使用方式

這份文件的定位是「升級加解耦的施工清單」，不是單純的版本升級筆記。依照本文件做完後，目標狀態應該是：

- WonderlandUHC 可在 Java 21 / Paper `1.21.11` 編譯、打包、啟動。
- 核心 UHC 比賽流程可以在沒有 DatouNMS、PacketListenerAPI/PacketEvents、舊外部 WorldBorder 插件、DiscordSRV 的情況下運作；邊界與預生成能力必須由原生 API、已驗證替代插件或專用實作完整保留。DiscordSRV 功能採舊版選配模式：未安裝不阻塞核心，有安裝就必須完整接上。舊 `custom-ore-generator` 不再列為可選 integration；自訂礦物功能暫停並列為升級後重設計項目。
- 舊功能若仍保留，會被放在 `legacy/` 或 `integration/`，不再和核心狀態機綁死。
- 新功能的修改位置有明確邊界，不需要在 command、menu、listener、scenario 裡到處補版本判斷。

但要注意：靜態閱讀無法保證列出 100% 的最後編譯錯誤。真正最後一批檔案，必須在 Java 21 / Paper `1.21.11` 依賴切換後，由 `compileJava`、`compileTestJava`、伺服器 smoke test 與人工流程測試收斂。因此本文件採用兩層清單：

1. **已知必改清單**：目前已從專案結構與文字搜尋中確認會被碰到的檔案，附錄 A 已列出。
2. **編譯收斂清單**：升級依賴後由錯誤訊息產生，處理方式是補進 `platform/`、`legacy/` 或對應 application service，不應直接散修在核心流程。

若照本文件完成所有章節，並且最後的編譯、開服、最小遊戲流程測試都通過，就可以視為升級完成。若只完成前半段，例如只完成 build.gradle 與部分 API 修正，最多只能算「可開始收斂 1.21.11 編譯」，還不能算升級完成。

## 舊必要依賴整合判斷

這段是針對原專案實際檔案掃描後的結論。朋友用 vibe coding 得到的方向有參考價值，但不能直接當成目前專案要採用的路線：升級目標不是把舊外掛整包塞進 WonderlandUHC，也不是因為難做就移除舊功能，而是先找能保留同等功能的新版本實作，並用清楚的 service/adapter 邊界接入。

| 依賴 | 目前實際用途 | 是否建議整合進主插件 | 升級處理 |
| --- | --- | --- | --- |
| `PacketListenerAPI` | `WonderlandUHC.checkDepends()` 強制檢查；`PacketRegister` 註冊 `SoundController`；`SoundController` 攔音效與攻擊封包；`ScenarioArmorVsHealth` 攔 `PacketPlayOutUpdateAttributes` 更新血量。 | 不整合舊 `PacketListenerAPI`，也不預設導入 PacketEvents。現有 packet 用途不是 UHC 開局、隊伍、死亡、勝負的核心條件；能用 Paper/Bukkit 原生事件與 Attribute API 取代的先取代，不能穩定取代的功能先從主線停用並記為待補，不視為永久移除。 | 先移除 `Dependency.PACKET_LISTENER_API.check()` 的硬阻塞。`ArmorVsHealth` 優先用 Paper/Bukkit equipment/attribute 事件改寫；死亡/旁觀互動限制優先用 Bukkit event gate。`SoundController` 不直接移植舊實作；大廳音效抑制後續仍要補回，若原生 API 無法等價處理，才評估 optional PacketEvents adapter。 |
| 外部 `WorldBorder` 插件 | `WonderlandUHC.checkDepends()` 強制檢查；`checkWorldBorderVer()` 綁外部版本；`BorderUtil` 直接 dispatch `wb clear/set/wshape`；`ChunkFiller` 直接跑 `wb fill`；`WorldFillListener` 依賴 `WorldBorderFillFinishedEvent`。`Bedrock_Border_Height` 舊設定預設為 `0`，只有大於 `0` 時才會在 fill 完成後額外生成基岩牆。 | 不整合整個舊 WorldBorder 插件到本體。邊界與預生成是必留能力，但實作來源不預設為本體手刻；應先驗證 `WorldBorder [Renew] 1.18.X - 1.21.X`、`Chunky` 等新版本替代插件能否完整承接 border / fill / progress / completion workflow。舊版 WonderlandUHC 沒有使用 trim，trim 不列為升級必需能力。基岩牆不是原生邊界 API 的一部分，但設定啟用時是必須保留的舊版功能。 | 拆成 `BorderService`、`ChunkPregenerationService` 作為主要接線邊界。Paper/Bukkit 原生 `WorldBorder` 可承接邊界設定與收縮；fill/progress/完成事件優先找新版本外部插件 adapter。`BedrockBorderService` 在 `Bedrock_Border_Height > 0` 時必須提供等價行為；若 DatouNMS fast block 不可用，需補 Paper/Bukkit fallback，不能把啟用後的基岩牆降級成 unavailable。完整手刻只作最後手段。若邊界、預生成或舊設定啟用時的必要行為未完整保留，本步驟不能標記完成。 |
| `custom-ore-generator` | `WorldInitListener` 在 UHC 世界初始化時套用；`CustomOreGeneratorHook` 從 Bukkit services 取 API provider；`Populator`/`OreGen` 直接使用 COG API；`GeneratorSettingsMenu` 顯示 populator 設定。正式版 COG `2025.08.12` 已實測在 Paper `1.21.11` 拒絕載入，並明確標示只支援到 `1.21.8`。Jenkins dev build `dev-110-SNAPSHOT` 可在 Paper `1.21.11` enable，但只驗證啟動，尚未驗證 ore config、world config、舊 API 等價性或實際新 chunk 生成。 | 不整合進主插件核心，也不再建立舊 COG adapter。舊 COG API 與舊 `populators.yml` 的 0-64 / `STONE` 礦物模型不適合延用到 1.21.11；本輪升級先移除舊整合與 UI 入口，讓核心 UHC 不依賴此功能。Jenkins dev build 只作升級完成後重新設計時的候選來源，不列為本輪部署依賴。 | 移除舊 jar、`plugin.yml` softdepend、`de.derfrzocker.*` import、世界初始化套用流程與 generator 選單。自訂礦物功能暫停，不列入 Step 7 完成條件；升級完成後若仍需要，重新依 1.21.11 世界高度、deepslate/raw ore、每世界設定與預生成時機設計新版實作。 |

補充發現：

- Step 7 開始前，`plugin.yml` 把 `WorldBorder`、`PacketListenerApi`、`custom-ore-generator` 都列為 `softdepend`，但舊啟動檢查實際會對 `WorldBorder` 與 `PacketListenerAPI` 強制 throw。升級第一步必須先讓宣告與行為一致；移除舊 Packet 主線後，`PacketListenerApi` 不應再留在 `softdepend`；移除舊 COG 主線後，`custom-ore-generator` 也不應再留在 `softdepend`。
- `SoundController.formatSoundStringFromPacket()` 目前直接回傳空字串，因此 lobby hit/step sound 的封包取消邏輯很可能本來就沒有完整運作。升級時不直接移植這段舊實作，但「大廳階段抑制攻擊/腳步聲」仍是後續要補回的功能需求，應先記為 optional packet feature backlog。
- 目前舊外部 WorldBorder 的不可替代點不是「邊界」本身，而是 `wb fill`、`WorldBorderFillFinishedEvent` 與舊式預生成流程；預生成已被確認為必留要求，所以要先尋找並驗證新版本替代插件，不能因為原生 API 不完整就移除或降級。
- `OreGen` 目前只設定 `STONE` replace material，且 `populators.yml` 使用舊版 0-64 高度模型。1.18+ 世界最低高度、deepslate、raw ore 與新版礦物分布都需要重設，不能只更新 COG jar 或改用未驗證 dev build。
- `WorldUtils.getBlockEXP()`、礦物 drop/exp、生成設定與 populator 需要一起檢查，否則即使 COG 能載入，UHC 規則也不一定符合 1.21.11。

### 全文件統一規則

以下規則套用到後續所有步驟與附錄，不只套用在本段依賴分析：

1. 先解除 hard dependency，再重接 optional adapter；不要一開始就替換 jar 或把外掛源碼搬進主插件。
2. 核心 UHC 能力要由明確 service/adapter 協調，實作來源可以是原生 API、已驗證外部插件或必要時的專用實作；不能因為舊依賴不可用就讓邊界、預生成等既有功能缺席。
3. 可選 integration 的 API import 只能出現在 `integration/` 或暫時的 `legacy/` adapter，不得散落在 core、application、listener、scenario、menu。
4. 舊 jar 不應是核心 build/runtime 必需項；若短期保留，必須是 `compileOnly`、optional runtime 或明確 legacy-only。
5. 每個階段都要先驗證「沒有外部插件」的狀態，再驗證有安裝 adapter 的狀態。
6. 不能因升級困難而移除舊版本已有功能；若某項功能無法立刻穩定落地，必須標成未完成項並繼續尋找替代或實作路線，不能把功能刪除當作完成。

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
- 主持工具與開賽操作

選配但功能需保留：

- DiscordSRV 公告與隊伍語音；未安裝時停用，有安裝時完整保留
- 自訂礦物生成
- Packet/Protocol 類功能
- Practice mode
- 基岩牆 / 方塊假邊界視覺效果；預設 `Bedrock_Border_Height: 0` 時不生成，但設定啟用時必須保留舊版行為

可降級或取消的功能：

- MySQL / 長期資料庫 stats 儲存；委託人已表示不想保留資料庫路線
- 多局累積、長期營運取向的資料庫排行榜流程；但當局 / 本機 YAML 類 stats 紀錄、`/stats`、`/topkills` 與賽中/賽後顯示仍要保留
- 1.7 舊附魔模擬；委託人已正式決定在新版移除此功能，不嘗試修復舊 `OldEnchant` / packet / NMS 實作

建議移除或改成外部插件責任：

- 直接依賴舊 WorldBorder `wb` 指令的 chunk fill
- 只為舊版本相容存在的 NMS hack
- 舊版 Minecraft 專用 workaround
- 和 UHC 核心無關的測試/開發指令

取捨規則：

- 核心必留功能若目前依賴外部插件，先建立明確 service/adapter 邊界，再驗證原生 API、替代插件或專用實作哪一種能完整保留功能。
- 選配整合只能透過 port/service 被呼叫；缺少依賴時只能停用該整合，不可阻塞主插件啟動。但若依賴存在且設定啟用，功能必須與舊版等價。
- 舊版本已有功能若暫時無法穩定升級到 1.21.11，必須列為未完成項並持續尋找替代或實作路線；除非使用者明確接受，不能以降級、停用或移除作為完成狀態。
- 目前已明確接受的例外：MySQL / 長期資料庫 stats 路線、1.7 舊附魔模擬可正式移除；後續步驟不得再把它們列回必修修復項。
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
3. 外部依賴檢查先集中成 `DependencyReport`，回傳 available/disabled/reason，並輸出啟動時的依賴狀態。
4. listener、command、scenario、scoreboard、practice、Discord hook 都透過 feature registry 註冊。
5. `TEST_MODE`、`Common.runLater`、`CacheSaver` 等全域行為不要直接散在主類。
6. `WorldBorder`、`PacketListenerAPI` 等目前仍作為啟動硬依賴的解除，不在本步驟處理；完整 optional integration 策略留到 Step 7。

完成條件：

- 主類能在 150 行左右表達啟動流程。
- 已有 `DependencyReport` 可觀察依賴狀態。
- 缺少已確認為選配的依賴時，只停用對應 feature。
- `WorldBorder`、`PacketListenerAPI` 的 hard dependency 解除與 packet/worldborder gate 不屬於 Step 1，必須在 Step 7 處理。

## 2. 定義核心 UHC 模型

先把「UHC 是什麼」從 Bukkit listener 與 static util 裡抽出來。
本步驟只負責建立可測試的核心模型與最小 legacy 橋接，不負責把既有 runtime 權威來源整批切換到新模型。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/Game.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/StateName.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/*.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/`

要做的事：

1. 新增 `core/match/Match` 或 `UhcMatch`，代表一場比賽，不直接碰 Bukkit。
2. 新增 `MatchState`，替代直接散用 `StateName` 與狀態 class。
3. 新增純資料型 `UhcTeam`、`UhcParticipant`、`MatchSettings`，逐步降低對 Bukkit `Player` 的直接依賴。
4. 新增 legacy mapper，把舊 `StateName`、`UHCGameSettings` 的必要資料轉成 core `MatchState`、`MatchSettings`。
5. 新增 `MatchRepository` 或等價入口，保存目前 active `UhcMatch`。
6. 先保留舊 `Game` 作 facade，讓舊程式能跑；本步驟只讓 `Game` 最小橋接 active match、同步 state/settings。
7. 不在本步驟大量替換 command、menu、listener、scoreboard 裡的 `Game.getGame()`、`Game.getSettings()`、`UHCTeam`、`UHCPlayer` 使用。

本步驟不要做：

- 不把 `GameManager.getWinner()` 改成以 `UhcMatch` 作為 runtime 權威來源。
- 不把舊 `UHCTeam`、`UHCPlayer` 的資料來源切到 core model。
- 不重寫狀態機、倒數、傳送、死亡、勝負、結束流程。
- 不重整 command、menu、listener、scoreboard 對 `Game` 的直接依賴。
- 上述 runtime 流程遷移留到 Step 10；核心 presentation 入口瘦身留到 Step 17，scoreboard / practice / 非核心 presentation 收斂留到 Step 18。

完成條件：

- 核心比賽狀態可以不啟動 Bukkit server 也能做單元測試。
- `UhcMatch` 能保存 state、settings、participants、teams，並能判斷 alive teams / winner candidate。
- legacy `StateName` 與 `UHCGameSettings` 能被轉成 core model 的資料。
- 舊 `Game` 仍能橋接到新 model，至少能建立 active `UhcMatch` 並同步 state/settings，避免一次性大改。
- 既有 runtime 仍由舊 `Game` / `GameState` / `UHCTeam` / `UHCPlayer` 控制；Step 2 不要求新 model 成為 runtime 權威來源。

## 3. 建立 Port/Adapter 隔離 Bukkit 與 Paper

這一步是升級與維護性的共同基礎。所有跨版本 API 都應集中在 adapter。
本步驟的定位是「鋪設邊界」，不是「改變 runtime 行為」。除非是低風險呼叫點，否則只先新增 port、adapter 骨架與必要的 legacy wrapper，不提前解除 hard dependency，也不提前重寫世界、邊界、設定、scenario、scoreboard 或 presentation 流程。

本步驟建議優先新增並可少量接線：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldBorderPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/PlayerPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/SchedulerPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/EventPublisher.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/paper/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/*.java`

本步驟可以先定義但暫不接入 runtime，實際行為改造留給後續步驟：

- Packet integration 介面不在本步驟預設新增；Step 7 先移除舊 `PacketListenerAPI`，並優先用 Paper/Bukkit 原生 API 收斂 `SoundController`、`ScenarioArmorVsHealth`。
- 舊 COG / `populators.yml` 不在本步驟建立 port；Step 7 已決定移除舊整合，升級完成後若需要自訂礦物再另開新版設計。
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ChunkPregenerationPort.java`：Step 7 才處理核心預生成服務、`ChunkFiller`、`WorldFillListener` 與 Paper pregenerator；預生成不是 optional integration。
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/StoragePort.java`：Step 8 只先處理 `cache.db`、`savedgames.db`、MySQL stats 移除與 scenario material 設定；完整 `StoragePort`、config migration 與 static config 重整留到後續設定 migration 步驟。
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ScoreboardPort.java`：Step 18 先驗收 scoreboard presentation 與 Paper `1.21.11` 相容性；只有實測需要時才建立最小 scoreboard helper / adapter，完整 port 化留到 Step 21 legacy 移除時判斷，Step 22 再做最終驗收。

優先被 adapter 包住的舊檔案：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/PlayerUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Extra.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/UHCWorldUtils.java`

要做的事：

1. 建立 `port/`、`platform/paper/`、`legacy/` 的最小結構，先讓新邊界可被後續 application service 使用。
2. 把 scheduler 包起來，不讓新的 core/application 直接呼叫 `Common.runLater` 或 Bukkit scheduler；既有大量 listener/scenario 呼叫不在本步驟一次替換。
3. 把 `WorldUtils.spawnOrb()`、`GenerateUtil.setBlockSuperFast()`、`PlayerUtils.breakBlockNms()` 這類跨版本或 NMS 呼叫先包到 port/adapter 或 legacy wrapper；底層可以暫時仍呼叫舊實作，不在本步驟移除 DatouNMS。
4. `WorldBorderPort` 只先定義核心需要的邊界操作語意，並可包住 Paper/Bukkit `WorldBorder` API；`BorderUtil` 的外部 `wb` 指令與 hard dependency 行為不得在本步驟移除。
5. 訊息與音效輸出若有新 application service 需要，先定義輸出邊界；不要在本步驟大規模改寫 Foundation `Common`、`SimpleSound`、menu 或 command。
6. 每次新增跨版本 API，都必須先進 port 或 platform adapter，不得讓新的 application service 直接 import Bukkit、Foundation、NMS 或外部插件 API。

本步驟不要做：

- 不移除 `Dependency.WORLD_BORDER.check()`、`Dependency.PACKET_LISTENER_API.check()` 或 `checkWorldBorderVer()`；這是 Step 7。
- 不讓缺少 WorldBorder、PacketListenerAPI 時的啟動行為改變；optional integration 的行為收斂是 Step 7。舊 COG 移除語意也在 Step 7 決定。
- 不建立完整 `BorderService`、`WorldLifecycleService`、`TeleportService`、`CenterValidationService`；這是 Step 9。
- 不重整 static `Settings.*`、完整 config migration 或泛用 storage port；Step 8 只做最小持久化邊界。舊 `populators.yml` 不再是 Step 8 migration 來源。
- 不重整 command、menu、tools、scoreboard 或 practice 的責任邊界；核心入口是 Step 17，scoreboard / practice / 非核心 presentation 是 Step 18。
- 不把 runtime 權威來源切到新的 application use case；這是 Step 10。

完成條件：

- 新增的 port/adapter 骨架可編譯，且已示範至少一個低風險呼叫點如何透過 port/adapter 使用。
- 新的 application service 不需要 import `org.bukkit.*`、`org.mineacademy.fo.*`、DatouNMS 或外部插件 API。
- 既有 runtime 行為沒有因本步驟改變；缺少外部插件時是否能啟動，不作為 Step 3 驗收項。

## 4. 升級建置平台到 Java 21 / Paper 1.21.11

架構入口與 adapter 邊界建立後，再升級建置平台，編譯錯誤會更集中。
本步驟只處理 build tool、Java toolchain、Paper API baseline、測試入口與部署腳本，不處理 Foundation legacy 化、DatouNMS 隔離、WorldBorder/Packet/custom-ore-generator 選配化，也不以修完所有 1.21.11 API 編譯錯誤作為本步驟目標。

優先修改：

- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/settings.gradle`
- `Update-WonderlandUHC/gradle.properties`
- `Update-WonderlandUHC/gradle/wrapper/gradle-wrapper.properties`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `scripts/deploy-to-windows-server.sh`
- `scripts/package-plugin.sh`
- `scripts/package-plugin-1.21.sh`
- `scripts/clean-workspace.sh`
- `Update-WonderlandUHC/.github/workflows/build-and-publish-release.yml`

要做的事：

1. Gradle Wrapper 升級到支援 Java 21，例如 Gradle `8.10.2`。
2. Shadow plugin 升級到相容新版 Gradle 的版本，例如 `com.gradleup.shadow` `8.3.6`。
3. Java target 改成 Java 21 toolchain 或 `release = 21`。
4. `spigot-api:1.16.5` 改成 Paper `1.21.11` API；若後續真的需要 NMS 開發，再評估 paperweight-userdev，不在本步驟提前導入。
5. Lombok 升級到支援 Java 21 的版本，例如 `1.18.44`。
6. 測試依賴升級；MockBukkit 若不支援 1.21.11，拆成純單元測試與真 Paper smoke test。
7. 腳本與 CI 改成 Java 21 / Paper `1.21.11` 的建置與部署入口；過渡期間可用 `scripts/package-plugin-1.21.sh` 固定 Java 21，完成後再收斂回 `scripts/package-plugin.sh`，部署仍必須透過 `scripts/deploy-to-windows-server.sh`。
8. 第一輪 `compileJava` / `compileTestJava` 只用來分流錯誤：build/toolchain 問題在本步驟修正；Foundation、DatouNMS、WorldBorder、Packet、自訂礦物與 runtime 行為錯誤記錄到後續步驟，不在本步驟擴張處理。
9. `plugin.yml` 的 `api-version` 更新到 `1.21`；但 runtime hard dependency 與 optional feature gate 不在本步驟解除，留到 Step 7。

本步驟不要做：

- 不修改 `lib-foundation` source，也不要求 `lib-foundation` 在本步驟完成 Java 21 / Paper `1.21.11` 全量相容；Foundation 編譯與 legacy 收斂留到 Step 5。
- 不移除或改寫 `DaTouNMS` 呼叫；隔離與降級策略留到 Step 6。
- 不解除 `WorldBorder`、`PacketListenerAPI`、`custom-ore-generator` 的 hard dependency，也不新增 optional adapter；外部整合選配化留到 Step 7。
- 不把編譯錯誤散修在 command、menu、listener、scenario 或 util 裡；本步驟只能修 build 設定、依賴座標、測試入口與腳本入口。

完成條件：

- 依賴解析與 toolchain 正常。
- Gradle Wrapper、Shadow plugin、Java 21、Paper `1.21.11` API、Lombok 與測試入口的版本組合不再互相阻塞。
- 編譯錯誤已集中並分類成 API 相容或耦合點，不再是 build tool 問題。
- 若 `compileJava` / `compileTestJava` 仍因 Foundation、DatouNMS、WorldBorder、Packet 或 custom-ore-generator 耦合失敗，必須在本步驟輸出收斂清單，不能提前把後續步驟做掉。

Step 4 收斂清單：

- 主插件可用 Java 21、Gradle `8.10.2`、Shadow `8.3.6`、Lombok `1.18.44` 與 Paper API `1.21.11-R0.1-SNAPSHOT` 完成封裝。
- MockBukkit v1.16 不支援 Paper API `1.21.11`，升級線測試拆成純單元測試與真 Paper smoke test。
- 真 Paper `1.21.11` smoke test 可讓 server 進入 `Done` 並載入 jar；目前插件仍會在 Foundation / SnakeYAML / DatouNMS 舊類別處停用，分流到 Step 5 與 Step 6。
- `WorldBorder`、`PacketListenerAPI`、`custom-ore-generator` 的啟動選配化不在 Step 4 處理，分流到 Step 7。

## 5. 把 Foundation 降級成 Legacy 相容層

不要再讓 Foundation 成為新架構的中心。短期可以保留它，長期應逐步退出核心。
Step 4 只會把主插件建置平台切到 Java 21 / Paper `1.21.11`；若切換後 Foundation 本身無法在新 JDK 或新 API baseline 下編譯，從本步驟開始處理。

優先修改：

- `lib-foundation/pom.xml`
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

1. `lib-foundation/pom.xml` 同步到 Java 21 / Paper `1.21.11` 可用的編譯基準；只修建置與必要 provided dependency，不把 Foundation 重新升格成新架構中心。
2. `MinecraftVersion` 加上 1.21 或改成可處理未知未來版本的比較。
3. `Remain`、NBT、Reflection 只修到足以支撐現有功能啟動，不要再新增新業務到 Foundation。
4. 建立 `LegacyFoundationAdapter`，集中使用 `Common`、`YamlConfig`、`SimpleCommand`、`Menu`。
5. 新設定、新 command、新 menu 不再直接繼承 Foundation 類別；先走自己的 service/adapter。
6. `CompMaterial`、`CompSound` 可暫時保留為 legacy alias parser，但資料應逐步轉成 Bukkit/Paper 原生 key。

本步驟不要為了消除 import 而硬改仍被 Foundation API 簽名綁住的檔案：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/PlayersMenu.java` 目前仍透過 Foundation `ItemCreator.of(CompMaterial)` 建立 player head；Step 18 先實測 spectator player menu，只有 player head / skull owner 實際失效時才局部修正，否則留 Step 21 legacy 移除追蹤。
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/ButtonLocalization.java` 目前被 Foundation `ButtonReturnBack.setMaterial(CompMaterial)` 簽名綁住；Step 18 只驗收 back / page button 行為，若可運作就不提前替換 Foundation menu API。
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java` 目前同時被 Foundation `YamlConfig#getMaterial` 與 `ItemCreator.of(CompMaterial, ...)` 綁住；Step 8 設定/material migration 與 Step 14/15 scenario 收斂已處理主要行為，Step 18 只驗收 scenario menu 顯示，非阻塞相依留 Step 21 legacy 移除。

完成條件：

- `lib-foundation` 至少能在 Step 4 建立的 Java 21 / Paper `1.21.11` build baseline 下 compile。
- Foundation 問題不會阻止 core/application 單元測試。
- 新程式碼不再擴散 Foundation import。

## 6. 移除或隔離 DatouNms

目前 `PluginBootstrap.setupNms()` 已經 catch `UnSupportedNmsException`，不再因為 DaTouNMS 不支援目前 Minecraft 版本而直接讓插件啟動失敗；它只會記錄「legacy NMS-backed features will be unavailable」。這代表 Step 6 不需要再從「避免 setup 直接 throw」開始，而是要處理真正殘留的風險：多個功能仍直接呼叫 `DaTouNMS` / `NewerSpigotAPI`，一旦底層 NMS 不可用，仍可能在功能執行時失敗。

本步驟目標是把 DatouNMS 從一般業務程式碼中移出，集中成可查詢、可降級的 legacy adapter。不要為了清掉 import 一次重寫整個玩法、GUI、WorldBorder 或 scenario 系統；沒有穩定 Paper/Bukkit API 的功能先明確標成 unavailable，讓核心 UHC 流程繼續啟動。

優先修改：

- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/PluginBootstrap.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/LegacyDatouNmsAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/PlatformCapabilities.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/OldEnchantListener.java`（1.7 舊附魔功能已接受移除；本檔只作清理/解除註冊候選，不作修復）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/WorldUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/PlayerUtils.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/PlayingDeathListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioShiftKill.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/ScenarioTimeBomb.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/RespawnCommand.java`

要做的事：

1. 新增 `PlatformCapabilities`，明確列出 absorption、fast block set、custom exp orb、death animation、pickup exp control、large chest merge 是否可用；1.7 舊附魔功能已接受移除，不列為新版需修復 capability。
2. 新增 `legacy/LegacyDatouNmsAdapter`，由它負責 `DaTouNMS.setup(plugin)`、捕捉 unsupported 狀態、暴露 capability，並集中包住仍需要 DatouNMS 的舊功能。
3. `PluginBootstrap.setupNms()` 改成建立或初始化 `LegacyDatouNmsAdapter`，不要再讓 bootstrap、listener、scenario、util、scoreboard、role 直接 import `me.lulu.datounms.*`。
4. 1.21.11 主線優先用 Paper/Bukkit API 實作：例如 absorption 優先走 Bukkit attribute / potion effect 可用資訊，普通方塊放置優先走 Bukkit/Paper 安全 API。
5. 無穩定 API 的功能不可直接刪除；例如 death animation、pickup exp control、自訂 exp orb、fast block set，必須先判斷是否屬於舊版對外可見且需完整保留的行為。若暫時做不到，只能標成未完成或需使用者確認的降級項，不能把停用視為完成。1.7 舊附魔模擬是本規則的已確認例外：新版不修復，後續只需要移除相關註冊與殘留檔案。
6. `ScenarioTimeBomb` 目前透過 `NewerSpigotAPI.mergeChest()` 使用 DatouNMS model helper；本步驟要一併收斂到 adapter 或改用 Paper/Bukkit 可維護做法。
7. `TestCommand` 已於 Step 21 移除；不要為測試指令保留核心 runtime 依賴。
8. 從 `build.gradle` 移除 DatouNMS `implementation`，或改成明確 legacy-only / compileOnly 過渡依賴；不可讓 DatouNMS 成為新主線功能的必要 runtime。

本步驟不要做：

- 不處理 `WorldBorder`、`PacketListenerAPI`、`custom-ore-generator`、DiscordSRV 的 optional integration；這些是 Step 7。
- 不重寫外部 `wb` 指令、`WorldBorderFillFinishedEvent` 或 chunk pregeneration 流程；若 DatouNMS 只是在該流程中快速放置方塊，本步驟只先隔離或降級那個 NMS 寫方塊能力。
- 不把 Foundation menu、command、settings 大規模替換掉；Foundation legacy 收斂屬於 Step 5 與後續 menu/command 步驟。
- 不為了消除 import 而擴大重構 scenario 架構；scenario feature gate 與規則引擎整理留到 Step 14。

完成條件：

- `rg "DaTouNMS|datounms" Update-WonderlandUHC/src/main/java` 只剩 legacy adapter 或完全為 0。
- `rg "NewerSpigotAPI" Update-WonderlandUHC/src/main/java` 只剩 legacy adapter 或完全為 0。
- DatouNMS 不可用時，插件仍可啟動，核心 UHC 最小流程不因 death animation、pickup exp control、fast block set 或 custom exp orb 缺席而中止；1.7 舊附魔模擬缺席是已接受的正式移除，不算未完成修復項。
- 若 DatouNMS 仍作為過渡依賴存在，`build.gradle` 必須清楚標示它只服務 legacy adapter，不能被新程式碼直接引用。

## 7. 外部整合重評估，完整保留既有功能

外部插件不是 WonderlandUHC 的本質，但舊版本已經具備的功能不能因升級而消失。Step 7 的目標是移除舊硬綁定，重新確認每個功能在 1.21.11 的實作來源：原生 API 可完整承接的用原生 API；原生 API 不完整的先找新版本替代插件；完整手刻只作最後手段。

優先修改：

- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/hook/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/ScenarioArmorVsHealth.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldInitListener.java`（舊 COG world-init hook，Step 7 移除）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/populator/*.java`（舊 COG populator，Step 7 移除）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/ChunkPregenerationService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/worldborder/LegacyWorldBorderFillListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/worldborder/LegacyWorldBorderPregenerationAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ChunkPregenerationPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/ChunkFiller.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/libs/PacketListenerAPI_v3.7.9-SNAPSHOT.jar`
- `Update-WonderlandUHC/libs/custom-ore-generator-2022.06.11.jar`

施工順序：

1. **修正 hard dependency**
   - `plugin.yml` 不可繼續宣告舊版硬綁定；`PacketListenerAPI`、舊外部 `WorldBorder`、`custom-ore-generator` 不可沿用舊的啟動阻塞。
   - `Dependency.java` 與 `WonderlandUHC.java` 要移除 `Dependency.WORLD_BORDER.check()`、`Dependency.PACKET_LISTENER_API.check()` 與 `checkWorldBorderVer()` 的啟動阻塞。
   - 若選定新版本替代插件作為必要實作，必須明確寫入部署需求、驗證流程與缺失時的錯誤訊息，而不是沿用舊 `checkDepends()` 行為。

2. **確認 WorldBorder 與預生成替代路線**
   - Paper/Bukkit `WorldBorder` API 可承接中心、大小、警告、收縮與 inside 判斷；這部分可透過 `BorderService` / `WorldBorderPort` 包住。
   - `wb fill`、progress、完成事件不是原生 `WorldBorder` API 的完整能力；先把這些行為隔離成 `ChunkPregenerationService`，不得在一般 border port 內混用外部 `wb` 指令。
   - 預生成替代方案優先驗證 `Chunky`：它已有 Paper/Bukkit 1.21.11 相容版本，並提供 square selection、worldborder sync、progress、continue/cancel 與完成 callback；採用前仍要設計完成偵測與重啟銜接。
   - `WorldBorder [Renew] 1.18.X - 1.21.X` 可作為候選方案，因為它保留 fill 方向並標示支援 1.21；但其討論串已有 2026 年負面回報，不能在未實測前直接列為首選部署依賴。
   - `BorderUtil.setWBBorder()`、`removeWBBorder()`、`ChunkFiller`、`WorldFillListener` 不能在未驗證替代插件前被簡單刪除；要先決定是接新插件 adapter、原生 API，或最後才寫專用實作。

3. **移除舊 Packet 依賴，優先改用原生 API**
   - `PacketRegister` 與 `SoundController` 不得無條件註冊舊 `PacketListenerAPI` handler。
   - `ScenarioArmorVsHealth` 優先改成 Paper/Bukkit equipment/attribute 事件流程；死亡/旁觀互動限制優先用 Bukkit event gate。
   - `SoundController` 不直接移植舊實作；目前音效判斷本來就不完整，但大廳音效抑制後續仍要補回。若 Paper/Bukkit 原生 API 無法等價處理，才評估 optional PacketEvents adapter。
   - Step 7 不預設建立 `PacketPort` 或導入 PacketEvents；只有在出現原生 API 做不到且必須保留的明確需求時，才補最小 optional adapter。PacketEvents 不得在授權與散布策略未定前 shade。

4. **移除舊 COG 自訂礦物整合**
   - 正式版 Custom Ore Generator 已實測不支援 Paper `1.21.11`；Jenkins dev build `dev-110-SNAPSHOT` 可在 Paper `1.21.11` enable，但功能等價性未驗證。加上舊 `populators.yml` 使用 0-64 高度與只替換 `STONE` 的舊世界模型，本輪升級不維護舊 COG 相容層。
   - 移除 `CustomOreGeneratorHook`、`WorldInitListener`、`OreGen`、`Populator` 的 `de.derfrzocker.*` import 與呼叫點，不建立 `OreGenerationPort` / `NoopOreGenerationPort` / `CustomOreGeneratorAdapter`。
   - 移除 generator 選單與世界建立時的 populator 套用流程；自訂礦物功能暫停，不阻塞核心 UHC。
   - `populators.yml` 與舊 COG jar 不再隨插件散布。升級完成後若仍需要自訂礦物，另開新版設計，依 1.21.11 高度、deepslate/raw ore、replace materials、每世界啟用與預生成時機重做。

5. **清掉舊 jar 與驗證**
   - `PacketListenerAPI_v3.7.9-SNAPSHOT.jar` 不應再是 build/runtime 必需項。
   - `custom-ore-generator-2022.06.11.jar` 不應再是核心編譯條件，也不應保留在 `libs/`。
   - 最終用 A15 的 `rg` 指令確認舊 API 只剩在 `integration/` 或完全消失。

6. **DiscordSRV 完整保留但解除硬綁定**
   - DiscordSRV 公告與隊伍語音都是舊版插件功能的一部分；有安裝且設定啟用時必須完整保留。
   - DiscordSRV integration 至少要覆蓋：賽事公告、頻道設定、提及轉換、隊伍語音頻道建立、入隊/退隊搬移、解散隊伍回 lobby、死亡狀態下 `/reconnect` 回 lobby。
   - DiscordSRV 未安裝時只停用 Discord 功能，不影響核心開局、死亡、勝負或結束流程；有安裝但設定錯誤時要明確回報，不可靜默失敗。
   - `DiscordSRV-Build-1.30.5.jar` 已在 Paper `1.21.11` 測試服通過初步啟動相容檢查：插件可被 Paper remap、載入並進入 enable 流程，伺服器可達 `Done`。未提供 `BotToken` 時，DiscordSRV 會回報 `No bot token has been set in the config` 後停用，這只能證明 jar 可載入，不能判定功能合格。
   - 已在測試服本機填入真實 `BotToken` 後重啟驗證，DiscordSRV 可完成 `[JDA] Login Successful!`、`Connected to WebSocket`、`Finished Loading!`，且伺服器可達 `Done`。這代表 DiscordSRV `1.30.5` 在 Paper `1.21.11` 的登入與 WebSocket 啟動路徑可用。
   - 曾在玩家完成 `/discord link` 後，執行 `/team create` 可建立隊伍語音頻道但無法移動玩家；log 顯示 `IncompatibleClassChangeError: Found interface github.scarsz.discordsrv.objects.managers.AccountLinkManager, but class was expected`。原因是主插件仍以 DiscordSRV `1.25.1` API 編譯，而 DiscordSRV `1.30.5` 已將 `AccountLinkManager` 暴露為 interface。此問題已改用 DiscordSRV `1.30.5` API 編譯並通過重新封裝與測試服啟動。
   - 隊伍語音已完成本機實測：玩家完成 `/discord link` 並在 Discord 語音內時，`/team create` 可建立隊伍語音頻道、移動玩家並顯示成功訊息；正式 `/reconnect` 指令可被客戶端辨識、可執行語音重連，且不再透過 `PlayerCommandPreprocessEvent` 攔截 raw command。曾出現的「成功移動但同時顯示錯誤」來自 `TeamJoinedEvent` 早於隊伍語音頻道建立，已改成頻道尚未建立時略過該次入隊搬移，交由 `TeamCreatedEvent` 建立頻道後統一移動；重測只剩正常訊息。後續實測 `/team leave` 可將玩家移回 lobby，`/team disband` 可將玩家移回 lobby 並刪除隊伍語音頻道，且遊戲內沒有額外錯誤訊息。
   - 若為了驗證 Discord 公告路徑而暫時加入 `/uhc announce` 這類直接指令，必須標記為 Step 7 測試入口，不可視為正式功能；舊版主要公告入口是 GUI / Conversation 流程，GUI 修復後應移除此臨時指令，避免新版多出舊版沒有的主持指令。
   - 臨時 `/uhc announce` 已驗證 Discord 公告底層 sender 可送出，`Discord.Channel_Ids`、提及轉換與 `@everyone` active mention 可用。這代表 Step 7 的 DiscordSRV integration 能力已驗收；正式 GUI / Conversation 公告入口屬於 Step 13 的公告正式化範圍，不能反向卡住 Step 7。
   - 進階功能測試需在測試服本機設定真實 Discord bot，不得把 token 或 Discord server 私密設定提交到 repo。需要填寫 `plugins/DiscordSRV/config.yml` 的 `BotToken`，`plugins/WonderlandUHC/broadcasts.yml` 的 `Discord.Channel_Ids`，以及 `plugins/WonderlandUHC/settings.yml` 的 `DiscordVoice.Use`、`Guild_Id`、`Voice_Category`、`Lobby_Voice`。
   - Step 13 接續事項：修復並驗證 `/uhc edit` 的 Discord 公告 GUI / Conversation 入口，確認它能使用同一套公告 sender、錯誤回報與 mention 行為；GUI 修復後移除臨時 `/uhc announce` 測試入口。

完成條件：

- 空 Paper 1.21.11 server 可啟動核心 WonderlandUHC。
- 舊 WorldBorder 插件被移除後，邊界、fill、預生成進度與完成後續流程仍需由原生 API 或已驗證替代插件完整保留；不能把「跳過預生成」視為完成。舊版 WonderlandUHC 沒有使用 trim，所以 trim 不列為 Step 7 必做能力。
- 自訂礦物 / COG 是本輪升級明確接受的暫停項；舊 COG 程式碼、jar、softdepend 與 generator 選單需移除，核心流程不得再依賴它。
- DiscordSRV 沒安裝時只停用整合；安裝並啟用時，公告底層 sender、提及轉換、頻道設定與隊伍語音流程完整可用。GUI / Conversation 公告入口的修復與臨時 `/uhc announce` 移除屬於 Step 13，不作為 Step 7 阻塞項。
- 每個 integration 都可獨立啟用、停用、測試。
- `rg "Dependency\\.(WORLD_BORDER|PACKET_LISTENER_API)\\.check" Update-WonderlandUHC/src/main/java` 為 0。
- `rg "dispatchCommand\\(.*wb|WorldBorderFillFinishedEvent" Update-WonderlandUHC/src/main/java` 只剩 optional bridge 或為 0。
- `rg "org\\.inventivetalent\\.packetlistener|de\\.derfrzocker" Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario` 為 0，或只剩明確標記的 legacy 過渡檔。

## 8. 重整設定與持久化邊界

設定目前散在 Foundation static config、db 資源、cache saver 與 game settings。Step 8 的合理邊界不是一次建立完整 config framework，而是先把會影響啟動、重啟接續、主持 preset、stats 路線與 scenario material 讀取的風險收斂到明確位置。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/bootstrap/PluginBootstrap.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/UHCGameSettingsSaver.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/settings/Settings.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/settings/StatsStorageYaml.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/settings/StatsStorageSql.java`（移除）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/update/CacheSaver.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/cache/WorldLoadingCache.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/cache/WorldLoadingCacheStore.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/settings/SavedGameSettingsStore.java`
- `Update-WonderlandUHC/src/main/resources/cache.db`
- `Update-WonderlandUHC/src/main/resources/savedgames.db`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/stats.yml`
- `Update-WonderlandUHC/src/main/resources/gamecache.db`（已確認舊版與新版皆未被程式引用，視為舊殘留資源，應移除而非遷移）

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/cache/`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/storage/settings/`

要做的事：

1. `cache.db` 只保留會影響切階段 / 重啟接續的資料，例如 `Loading_Status`、host、已選 settings；先以 `WorldLoadingCacheStore` 包住讀寫，保留 `CacheSaver` 作 legacy facade，避免一次改爆舊 call sites。
2. `savedgames.db` 保留舊版主持設定 preset 或等價功能；先以 `SavedGameSettingsStore` 包住讀寫，保留 `UHCGameSettingsSaver` 作 legacy facade。
3. 生成狀態、host、已選設定與世界載入狀態必須可持久化；生成完成後要像舊版一樣保存 `DONE` 或等價狀態，重啟後不能回到未生成或設定遺失。
4. `gamecache.db` 已確認舊版與新版皆未被程式引用，視為舊殘留資源，從打包資源移除；不得為它補 migration 或 runtime facade。
5. `StatsStorageSql` / MySQL 長期資料庫路線不保留；移除 MySQL 設定與 SQL storage，stats 維持 YAML 或等價本機儲存，並保留 `/stats`、`/topkills`、`StatsMenu` 等既有查詢 / 顯示入口。
6. `ScenarioConfig` 的 `Type` 讀取與 scenario icon material 從 Foundation `CompMaterial` 過渡到 Bukkit/Paper 原生 `Material`；`scenarios.yml` 內已知舊 alias 要改成 1.21 可讀名稱。

本步驟不做：

- 不把所有 static `Settings.*` 一次改成 immutable settings object；這會碰到大量 command/menu/use case 呼叫，留到後續設定 service 重構。
- 不建立通用 config version / migration framework；完整備份、版本化 migration 與跨資源名稱掃描留到 A12 / 最終 migration 步驟。
- 不處理 `gui.yml`、`items.yml`、`sounds.yml`、`biomes.yml` 的全量 alias 寫回；阻擋核心主持入口的 material 先在 Step 11 修，核心 menu resource 收斂在 Step 17，非核心 presentation resource 收斂留到 Step 18。
- 不新增抽象的 `StoragePort` 來包所有檔案；目前只針對 `cache.db` 與 `savedgames.db` 建立具名 store，避免過度抽象。

完成條件：

- `cache.db` 讀寫已由 `WorldLoadingCacheStore` 集中，舊 `CacheSaver` 只保留相容 facade。
- `savedgames.db` 讀寫已由 `SavedGameSettingsStore` 集中，舊 `UHCGameSettingsSaver` 只保留相容 facade。
- 生成完成狀態、host 與 UHC 設定在重啟後仍能恢復，不因重啟回到未生成或設定遺失。
- 未被程式引用的舊殘留資料檔不得被列為必做 migration；`gamecache.db` 已從打包資源移除。
- MySQL stats 設定與 `StatsStorageSql` 已移除；YAML / 本機 stats 路線仍可被啟動流程載入。
- `ScenarioConfig` 與 `scenarios.yml` 不再依賴舊 material alias 才能讀取 scenario icon。

## 9. 重做世界、邊界、傳送與地形服務

世界相關是 UHC 核心，但不應散落在 `Extra`、`WorldUtils`、`BorderUtil`、listener、command 內。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CenterCleaner.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/border/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/teleport/*.java`
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
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/MatchCenter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/CenterCandidateScore.java`

要做的事：

1. 世界建立/刪除/載入集中到 `WorldLifecycleService`。
2. 邊界設定/收縮集中到 `BorderService`。
3. 隨機傳送、Nether 對應座標集中到 `TeleportService`。
4. `CenterCleaner` 改成 `CenterValidationService`，不再只驗證 `0,0` 或用世界重生作為主要篩選方式。
5. 建立 `MatchCenter` / candidate center 概念：在同一張世界內做有限數量的候選中心點搜尋，最後選出一個適合 UHC PvP 的中心再交給邊界與預生成流程。
6. 中心點評分必須包含 PvP 地圖品質，不只是 biome 相容：基本平坦度、地形破碎度、可視距離、可移動空間、水域/懸崖/密林比例、中心附近是否能安全集結。
7. biome 使用 1.21.11 namespaced key，不再硬寫舊 enum；biome 只能作為評分、排除或權重條件，不得用覆蓋 biome 的方式把地圖改成想要的樣子。
8. 中心點不要求一定是 plains，但必須避免明顯不適合 PvP 的區域，例如大面積海洋、過密森林、極端山崖、破碎峽谷或可活動面積太少的地形。
9. 高度掃描全部使用 `world.getMinHeight()` / `world.getMaxHeight()`，評分時要考慮 1.18+ deep world 的新版高度與洞穴地形。
10. 礦物與地形規則要包含 1.18+ deep world。

完成條件：

- 世界生命週期可以獨立測試與人工驗證。
- command、menu、game state 不再各自直接操作世界細節。
- `CenterValidationService` 能輸出候選中心點的評分原因，人工驗證時可看出為什麼選中或排除某個中心。
- 最終中心點必須是適合 PvP 的區域；不能只因為舊 biome 名稱通過或 `0,0` 可用就接受。
- 地圖品質篩選不得使用 biome overwrite，也不得靠無限制重生世界來碰運氣。

目前進度註記：

- Step 9 的 CenterCleaner 分支已完成 Slice 5 暫時可用版：`CenterCleaner.createWorld(...)` 已接回同世界候選中心搜尋，主持人可取得搜尋進度與結果；舊版固定 `0,0` pass/fail 掃描已不再是 1.21 中心搜尋主流程。
- 候選中心目前以世界重生點為基準擴散，候選數預設為 `15`；水域/海洋/中心區/地形/森林等評分先停在可用版，不再於本輪升級前繼續細調演算法。
- 測試用逐候選傳送預覽與完整 debug 計算輸出已關閉，正常流程只讓主持人看到中心搜尋進度與結果，不對一般玩家廣播。
- Slice 6 已完成並人工驗收：`MatchCenter` 會寫入 `cache.db`，重啟後仍能讓 WorldBorder、pregeneration、`BorderUtil#isInBorder(...)` 與基岩邊界圍繞同一個中心；目前實測中心沒有掉回 `0,0`。
- Slice 7 第一刀已完成啟動驗證，目標是把「回中心」與散佈流程改讀 `MatchCenter`：`/uhc tp`、spectator center tool、spectator join、staff start teleport、縮圈拉回、UHC 主世界隨機散佈與 Nether 對應座標都已接到目前比賽中心。
- Slice 7 的人工驗收目前有阻礙：旁觀者工具 / 部分 GUI 無法操作，且 Nether 開關目前無法透過介面啟動，因此 spectator tool 與 Nether 對應座標先標為待 GUI 修復或提供替代測試入口後再驗；目前可先用 `/uhc tp`、`/uhc start`、`/border <size>` 驗證中心傳送、散佈與縮圈拉回。
- Slice 7 已由遊戲內確認 `/uhc start` 與 `/border <size>` 基本可正常使用；新發現 `邊界將於 {fancy-time} 後收縮...` 的倒數訊息 placeholder 未替換，這屬於訊息 / countdown 顯示問題，先另列後續處理，不阻塞 MatchCenter 串接驗收。
- Slice 7 追加修正：Nether center 應由主世界 `MatchCenter / 8` 推導，Nether WorldBorder、Nether 邊界判斷、Nether 隨機傳送與 portal 對應座標都應使用這個維度中心；不同 world / dimension 的邊界中心與邊界大小不必相同。
- CenterCleaner 停用或搜尋無結果時，UHC match center 應 fallback 到 world spawn location，而不是固定 `0,0`；practice mode 可維持 origin center，但只能透過明確的 origin/practice-only API 使用。
- Slice 7 收尾清理已完成：舊版 CenterCleaner pass/fail 失敗訊息 Java 欄位與預設 key 已移除；舊演算法設定 Java 欄位已移除，但預設 `settings.yml` 欄位保留為 legacy 相容欄位；`biomes.yml` 目前只剩資源抽出，保留到後續 resource migration 再處理。
- 細部地形/水域門檻調整延後到整體升級完成後再回來處理；不要在 Slice 7 混入評分演算法變更。
- Step 9 本輪升級可視為暫時完成：中心搜尋、MatchCenter 保存、WorldBorder / pregeneration / border 判斷、回中心與散佈流程已接到目前中心；Nether GUI 開關需等 Step 11 核心主持設定入口修復後回測，spectator tool 與 Nether portal 實跑需等 Step 12 hotbar / spectator 工具修復後回測。

## 10. 重整狀態機與 Application Use Cases

目前狀態切換與 listener 行為分散，應該變成明確的 use case。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/GameTimerRunnable.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/CombatRelog.java`

建議新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/StartMatchUseCase.java`（已新增，保留在 core transition 邊界；不直接接到 `/uhc start`，避免 core match 先進 TELEPORTING 但 legacy state 仍停在 WAITING 倒數。）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/HandleDeathUseCase.java`（已新增）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/EndMatchUseCase.java`（已新增）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/MatchTimerTickUseCase.java`（已新增）
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/CombatRelogTickUseCase.java`（已新增）

不新增：

- `CreateMatchUseCase`：目前 match 建立只包 `UhcMatch.create(...)` / `MatchRepository`，新增 use case 只會形成空殼抽象；後續若有多資料來源或建立流程前置條件再補。
- `TeleportTeamsUseCase`：目前散佈流程同時含 Bukkit 傳送、freeze、玩家可見性、訊息與 legacy timer 副作用，強行抽出會比現況更抽象且風險較高；先保留在 `ScatterHandler`，等 platform teleport/freeze port 明確後再拆。

要做的事：

1. 把 `WAITING -> TELEPORTING -> PRE_START -> PLAYING -> ENDING` 轉成明確 transition。
   - 目前第一刀已把已接線的 legacy 狀態切換透過 `MatchTransitionUseCase` 執行；人工驗證已確認 `WAITING -> TELEPORTING -> PRE_START -> PLAYING -> ENDING` 可跑通，臨時 transition log 已移除。
   - `PLAYING -> ENDING` 目前透過勝利判定接入，並委派給 `EndMatchUseCase`；勝利訊息、`GameEndEvent` 與正式 cache 清理責任仍留在 legacy `GameManager.checkWin()`。
2. 每個 transition 宣告前置條件、動作、失敗原因。
   - `MatchTransitionResult` 已提供 `MatchTransitionStatus`，目前涵蓋 `SUCCESS`、`MISSING_MATCH`、`INVALID_SOURCE_STATE`、`INVALID_TARGET_STATE`，讓 legacy 呼叫端可保留文字錯誤，同時讓測試能直接驗證失敗類型。
3. Listener 只把 Bukkit event 轉成 use case 呼叫。
   - `HandleDeathUseCase` 第一刀已承接「只有一隊存活時才有 winner」的判斷；legacy `GameManager.checkWin()` 仍負責勝利訊息、`GameEndEvent`、正式 cache 清理位置與呼叫 `EndMatchUseCase`，避免一次搬動 death listener 與 presentation 行為。
   - 勝利結束後已恢復 `CacheSaver.deleteCache()`，讓下一次啟動回到正式 `CONFIGURING` 流程；測試期保留世界的需求已改由 `WORLD_READY` handoff 解決。
4. Game timer 只負責時間 tick，不直接到處修改世界/玩家/設定。
   - `MatchTimerTickUseCase` 第一刀已承接 tick / second 推進規則，`GameTimerRunnable` 保留排程與執行目前 state timers 的 legacy 責任，避免一次搬動 countdown 的世界/玩家副作用。
5. Combat relog 改成 match participant 狀態的一部分，實體替身是 platform implementation。
   - `CombatRelogTickUseCase` 第一刀已承接 relog 倒數、過期與移動邊界外傷害判定；legacy `RelogExpireChecker` 仍負責 broadcast、entity damage、死亡事件與替身移除，避免一次搬動 Bukkit entity 行為。
   - Relog timeout 透過手動 `EntityDeathEvent` 取得最終 drops 後，必須明確把未被場景清空的 drops 丟在替身死亡位置，避免與在線死亡 / 替身被殺的噴裝行為不一致。
   - Relog timeout 已恢復為讀取 `Relog_In_Minutes * 60`，`STEP10_TEMP_RELOG_TIMEOUT_BYPASS` 測試值已移除。
6. 世界生成 / 預生成完成後的狀態交接必須透過 storage/service 保存；即使需要重啟伺服器，也不能讓比賽設定或 loading status 被重置。
   - `LoadingStatus.WORLD_READY` 用於保存「預覽世界與 `MatchCenter` 已建立、尚未 `/uhc choose` 預生成」的狀態，重啟後不得把已建立的 `uhc_world` 當成 `CONFIGURING` 清掉；`GENERATING` 仍負責重啟後接續預生成，`DONE` 則負責可開局狀態。
7. Step 11 修復核心 GUI 後必須確認 Nether 開關可操作；Step 12 修復 spectator hotbar 後，必須回測 Step 9 Slice 7 目前 blocked 的項目：spectator center tool 是否傳送到 `MatchCenter`、Nether 進出座標是否以主世界 `MatchCenter / 8` 作為 Nether center。
8. `STEP10_TEMP_GUI_TEST_BYPASS` 已移除：`Damage_Time` 預設值恢復為 `60 * 5`，`PlayingState` 不再於進入 PLAYING 時直接啟用傷害。

完成條件：

- 沒有 GUI、scenario、Discord、stats 時，核心比賽流程仍可跑通。
- 狀態轉換可以寫單元測試。
- 生成完成後重啟或重新載入，仍可從已保存狀態繼續後續流程。
- Step 11 修復核心 GUI / Nether 開關後，Step 12 已回頭驗收 Step 9 Slice 7 的 spectator tool 與 Nether 對應座標，不得只做啟動測試。
- `rg -n "STEP10_TEMP_GUI_TEST_BYPASS" src/main/java src/main/resources/settings.yml` 不得再找到殘留。（已完成）
- `rg -n "STEP10_TEMP_WORLD_REUSE_BYPASS"` 不得再找到殘留。（已完成）
- `rg -n "STEP10_TEMP_RELOG_TIMEOUT_BYPASS"` 不得再找到殘留。（已完成）

## 11. 核心主持設定入口修復

GUI 原本被放在 presentation cleanup，但目前它已經阻擋 Step 9 與 Step 10 的核心驗收：主持人無法可靠透過設定 GUI 修改 timer / Nether。本步驟只修會阻擋開局與核心設定驗收的主持入口，不處理 hotbar、spectator tool、Discord 公告或完整 GUI 架構重寫。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/uhc/EditCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/MainGui.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/MainSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/TimeSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/BorderSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/model/InventoryEditButton.java`
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/items.yml`

要做的事：

1. 盤點 `/uhc edit`、SettingsBook、host settings menu 在 Paper `1.21.11` 上的實際失效點。
2. 修復會阻擋核心主持流程的 menu 入口：settings book 能開、主設定 GUI 能操作、核心按鈕不因 material alias 或 Foundation menu 差異失效。
3. `TimeSettingsMenu` 必須能寫入並保存 `Damage_Time`、PvP、border shrink、disable nether 等核心 timer，避免測試只能靠手動改 config。
4. `MainSettingsMenu` 的 Nether toggle 必須能正確寫入 `UHCGameSettings`，讓 Step 12 可以回測 Nether portal / border center。
5. `MainSettingsMenu` 的開始物品 `Custom_Inventory` editor 必須能完成保存；`finish` / `tohead` 這類 inventory editor conversation 輸入不得被 Paper `1.21.11` 當成未知指令而中斷。此處只驗收開始物品與開局需要的金頭顱轉換，practice inventory、禁用物品清單、死亡掉落物等完整非核心 editor 驗收留到 Step 18。
6. 只修會阻擋上述入口的 Bukkit/Paper API、Foundation menu 行為或 `gui.yml` / `items.yml` material alias；完整 API 清理仍留到 Step 19，presentation 架構整理分別留到 Step 17 / Step 18。

不做的事：

- 不全面重寫 GUI 架構。
- 不修 spectator / staff hotbar；這是 Step 12。
- 不修 Discord 公告 GUI / Conversation 入口；這是 Step 13。
- 不全面瘦身 command / menu / tools / scoreboard；核心入口是 Step 17，scoreboard 與非核心 presentation 是 Step 18。
- 不整理 practice mode；這是 Step 18。
- 不完整驗收 `Practice_Inventory`、`Disable_Items`、`Custom_Drops` 等非開局必要 inventory editor；若共用修正順手恢復功能，只能視為附帶效果，正式驗收仍留到 Step 18。
- 不全面重做 scenario GUI；scenario 盤點 / rule 化是 Step 14 / Step 15，scenario menu presentation 收斂是 Step 18。
- 不一次清完所有 Bukkit/Paper API；非核心入口 API 清理由 Step 19 處理。

完成條件：

- 不手動改 config，也能透過主持入口設定核心 timer、Nether 開關並開始比賽。
- 不手動改 config，也能透過主持入口設定開始物品，並且 inventory editor 的完成與金頭顱轉換入口不再落到未知指令。
- Step 9 / Step 10 不再因核心主持設定 GUI 無法操作而卡住。
- 修復只限核心主持設定入口與必要 Paper API；hotbar、Discord 與未觸及的 presentation cleanup 明確留在 Step 12、Step 13、Step 17 / Step 18。

## 12. Hotbar / Spectator 工具與 Step 9 回測

Step 11 修好核心主持設定後，再處理 spectator / staff hotbar 與 Step 9 留下的實跑驗收。這一步的重點是工具是否可給予、可觸發，並且所有回中心與 Nether 對應座標都以目前 `MatchCenter` 為準。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/tools/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/staff/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/MainSettingsMenu.java`
- `Update-WonderlandUHC/src/main/resources/items.yml`

要做的事：

1. 盤點 spectator / staff hotbar 在 Paper `1.21.11` 上的實際失效點。
2. spectator / staff hotbar 必須能給予並觸發。
3. spectator center tool 必須傳送到目前 `MatchCenter`，不是固定 `0,0`。
4. 回測 Step 9 Slice 7 blocked 項目：spectator center tool、Nether 開關、Nether portal 進出座標是否以主世界 `MatchCenter / 8` 作為 Nether center。
5. 若 Nether portal 或 spectator tool 因非本步驟問題仍不可測，必須留下具體不可測原因與下一步歸屬。

不做的事：

- 不修 Discord 公告 GUI / Conversation 入口；這是 Step 13。
- 不全面整理 tools 架構；這是 Step 17。
- 不處理 scoreboard、practice 或非核心 presentation；這是 Step 18。
- `/staff` 開啟後再關閉時 staff hotbar 物品不會自動清除，已確認是舊版既有行為，不視為本輪升級造成的 regression；若之後要調整，歸到 Step 17 的 staff/tools lifecycle 收斂。
- `邊界將於 {fancy-time} 後收縮...` 這類 border countdown placeholder 未替換問題屬於核心流程訊息輸出，歸到 Step 17 處理；不阻塞 Step 12 hotbar / spectator 驗收。

完成條件：

- spectator / staff hotbar 核心工具可用，且 center tool 以 `MatchCenter` 為準。
- Step 9 Slice 7 被 GUI / hotbar 阻擋的 spectator center tool、Nether toggle、Nether portal `MatchCenter / 8` 驗收完成或留下具體不可測原因。
- 修復只限 hotbar / spectator / Step 9 回測需要的必要範圍；未觸及的 presentation cleanup 明確留在 Step 17 / Step 18。
- Paper `1.21.11` 實測除舊版 `/staff` 關閉不清物品外，其餘 spectator / staff hotbar、center tool、Nether toggle 與 Nether portal `MatchCenter / 8` 行為皆正常時，Step 12 可視為完成；上述 staff 物品殘留與 border countdown `{fancy-time}` 不列為 Step 12 blocker。

## 13. Discord 公告 GUI / Conversation 正式化

Step 7 已驗證 Discord 公告底層 sender、頻道設定、提及轉換與 `@everyone` active mention 可用；剩下的是舊版正式入口仍應透過 GUI / Conversation 使用，而不是保留臨時測試指令。本步驟只處理公告正式入口與臨時入口移除。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/uhc/EditCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/BroadcastSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/model/broadcast/**/*.java`
- `Update-WonderlandUHC/src/main/resources/gui.yml`

要做的事：

1. 修復 Step 7 留下的正式 Discord 公告 GUI / Conversation 入口。
2. 確認 `/uhc edit` 的公告流程能使用既有 Discord sender、錯誤回報與 mention 行為。
3. GUI 修復後移除臨時 `/uhc announce` 測試入口；不得把臨時指令視為新版正式功能。

目前狀態：

- `/uhc edit` 的 Discord 公告 GUI / Conversation 入口已可送出公告，並已驗證錯誤頻道 ID 會回報玩家可讀錯誤。
- DiscordSRV 未安裝時，公告 GUI 會回報玩家可讀錯誤，不會建立 Discord sender，也不會因預期的缺依賴狀態寫入新的 exception。
- 臨時 `/uhc announce`、`broadcastdiscord` 與 `discordbroadcast` 測試入口已移除，不列為新版正式功能。

不做的事：

- 不重做 DiscordSRV optional integration；Step 7 已處理整合可用性與降級行為。
- 不把公告流程擴成新的主持指令功能。
- 不整理整體 command / menu 架構；這是 Step 17。

完成條件：

- Step 7 的正式公告 GUI / Conversation 入口可用，且臨時測試入口不再作為正式流程的一部分。
- DiscordSRV 沒安裝時仍只停用整合；安裝並啟用時，GUI / Conversation 公告入口能走已驗證的 sender 行為。
- 文件與程式碼都清楚標記臨時測試入口已移除或不存在。

## 14. Scenario 盤點與高風險相容層隔離

Scenario 是 UHC 的重要玩法，舊版既有 scenario 都要保留功能；但目前 scenario 同時混有規則、Bukkit event、NMS/packet、設定讀取與 GUI 顯示。這一步只做盤點、分類、feature gate 與高風險相容層隔離，避免一口氣重寫所有 scenario。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioManager.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioName.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/AbstractScenario.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ConfigBasedScenario.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/**/*.java`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`

要做的事：

1. 建立逐項 scenario 盤點表：舊版行為、目前 1.21.11 狀態、使用的 Foundation / DatouNMS / packet / Bukkit 舊 API、是否影響核心流程、測試方式。
2. 先把 scenario 分成純 Bukkit event、block/drop/exp、death/inventory、packet/NMS、高風險 GUI/config 類。
3. NMS/packet 型 scenario 先獨立 feature gate，例如 `ScenarioArmorVsHealth`、`ScenarioTimeBomb`、`ScenarioShiftKill`；不可用時不得讓核心流程或其他 scenario 壞掉。
4. `ScenarioConfig` 與 `scenarios.yml` 的 material / sound 讀取先保證不會因單一舊 alias 讓整體 scenario 載入失敗。
5. 不在本步驟完整導入 `RuleEngine` 或重寫所有 scenario；只先建立可驗收清單與風險隔離。

目前狀態：

- Scenario 盤點表已放在 `docs/step-14-scenario-inventory.md`，避免把完整分類細節塞進本總表。
- `ScenarioManager` 已改成逐項隔離 scenario 建構、reload、enable / disable 失敗；單一 scenario 不可用時會記錄 unavailable 原因，不會中斷其他 scenario 註冊、重載或遊戲開始時的 listener 重新掛載。
- `ScenarioConfig` 已針對 scenario icon、`Material` 欄位、`List<Material>` 欄位與 `SimpleSound` 欄位加入本地 alias / namespace 解析與具體錯誤訊息，並由 `ScenarioManager` 在建構 / reload 失敗時隔離單一 scenario，避免舊 alias 解析失敗時拖垮整體 scenario 載入。
- `ScenarioTimeBomb` 已補上死亡箱 runtime 降級：建立死亡箱失敗時會停用單一 scenario 並保留原死亡掉落；箱子容量不足時只移除已放入箱子的掉落，其餘掉落維持 Bukkit 原流程；單一死亡箱 ticker / explosion 失敗不會中斷其他死亡箱。
- `ScenarioDamageDogers` 已補上 damage event runtime 隔離；盾牌判斷、即死處理、廣播或音效任一步失敗時，只停用 `Damage_Dogers` 並讓傷害流程繼續。
- `ScenarioShiftKill` 已補上死亡事件 runtime 隔離；absorption 讀取會在 DatouNMS 不可用或 linkage 失敗時走 Bukkit `getAbsorptionAmount()` fallback，失敗時停用單一 scenario 並讓死亡流程繼續。
- `ScenarioArmorVsHealth` 已補上 inventory / interact / item break / respawn 與 delayed health update 的 runtime 隔離；armor point linkage 失敗時會優先讀 Bukkit armor attribute，讀不到時退回原版護甲值規則（包含銅裝），max health 更新保留最低值，停用時歸還本 scenario 實際扣掉的 max health，失敗時停用單一 scenario 並讓遊戲流程繼續。
- `ScenarioIronMan` 已補上 damage / final heal / respawn 的 runtime 隔離；final heal 成功套用額外血量後才記入 iron man 名單，停用還原採逐人 best-effort，失敗時停用單一 scenario 並讓遊戲流程繼續。
- `ScenarioLimitations` 已補上 block break runtime 隔離；每位玩家的礦物計數、掉落替換、上限訊息或音效任一步失敗時，只停用 `Limitations` 並讓破壞流程繼續。
- `ScenarioSwapInventory` 已補上死亡事件 runtime 隔離與交換 rollback；先用快照計算 drops，inventory / combat relog inventory 任一步交換失敗時會嘗試回復原 inventory 與 drops，失敗時停用單一 scenario 並讓死亡流程繼續。
- `ScenarioFastSmelting` 已補上 furnace boost task 的 runtime 隔離與停用清理；boost key 以 world + block 座標區分，單一 furnace tick 失敗時會停掉該 boost 並停用單一 scenario，burn / cook time 更新保留安全邊界。
- `ScenarioTimber` 已補上 block break runtime 隔離；共用 `VeinMiner` 現在會在連鎖破壞結束後清除玩家 mining 狀態，單一方塊 NMS break 失敗時優先退回 Bukkit/Paper `Player#breakBlock(Block)`，再以 `Block#breakNaturally(ItemStack)` 保底，避免一個方塊失敗拖垮整批連鎖破壞。
- `ScenarioVeinMiners` 已補上 block break runtime 隔離；共用 `VeinMiner` 的 mining cleanup 與 Bukkit/Paper player break fallback 會保護連鎖挖礦流程，單次 handler 失敗時只停用 `Vein_Miners`。
- Step 14 實跑收尾結果已記錄在 `docs/step-14-scenario-inventory.md`。`ScenarioSettingsMenu` 已改為 6 rows 單頁顯示並補既有 `gui.yml` migration；`Armor_Vs_Health` 已補 armor attribute / 銅裝 fallback、同 tick 重複扣血與停用還原且已回測通過；`VeinMiner` 已改用玩家原生破壞流程作為新版 fallback，經驗、耐久與 fortune 已回測通過；核心死亡流程已改成先標記 spectator、等新版 `spigot().respawn()` 後再套用 spectator 狀態並傳送到中心點且已回測通過；`Time_Bomb` 已補 death drops null / AIR 過濾與 chest item 快照，死亡箱、倒數、爆炸與廣播已回測通過。deep ore、silk touch 掉落、FastSmelting 第二輪加速等行為補齊留到 Step 15。

完成條件：

- 所有舊版預設 scenario 都有盤點結果與保留策略。
- 任一高風險 scenario 不可用時，不影響其他 scenario 與核心比賽流程。
- scenario 相關 Foundation / DatouNMS / packet 依賴已分類，後續能分批替換。

## 15. Scenario 規則模組化與逐項行為補齊

本步驟接續 Step 14 的盤點結果，分批把 scenario 從舊式 listener / global state 寫法收斂成可測、可替換、可降級的規則模組。這一步才處理行為補齊與 rule engine，不把全部工作壓在盤點步驟。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/core/rule/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/rule/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/ScenarioManager.java`
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
2. block/drop 類統一透過 `BlockDropPort` 或等價 service，不要各自手動 drop exp/item。
3. `ScenarioCutClean`、`ScenarioLimitations` 要支援 deep ores/raw ores。
4. `ScenarioSilkWeb`、`ScenarioTripleArrow` 等舊 API 使用點改到 platform helper。
5. NMS/packet 型 scenario 優先用 Paper/Bukkit 原生 API 或穩定替代實作補回同等功能；不能因舊 packet/NMS 不可用就刪除 scenario。
6. Scenario 啟用/停用不得直接註冊不可控 listener；由 `RuleEngine` 或明確 lifecycle 管理。

完成條件：

- 可以只載入核心 rule set 跑遊戲。
- 舊版預設 scenario 的單獨啟用行為都有驗收結果；除非使用者明確接受，不能移除或永久降級。
- scenario 單獨啟用時的行為差異已分類為一致、可接受差異、版本限制或 regression。

目前狀態：

- 單一 scenario 的實測與修正紀錄已整理在 `docs/step-15-scenario-single-validation.md`。
- 截至 2026-05-11，38 個預設 scenario 的單開行為已完成驗收；已知單項 blocker 已修正或被分類為舊版特性 / 委託人接受差異。
- `SilkWeb`、`TripleArrow`、`BowLess`、`Soup`、`PotionLess`、`Switcheroo`、`TimeBomb` 箱上可視倒數等差異已分類，不阻塞 Step 15 收尾。
- 多 scenario 同時啟用的互動驗收不屬於 Step 15，已拆到 Step 16。

## 16. Scenario 混合互動驗收

本步驟只處理多個 scenario 同時啟用時的互動行為。Step 15 先把單一 scenario 行為補齊並分類；不要在 Step 15 期間把多 scenario 組合問題混進單項驗收，避免原作者特性、可接受差異與升級 regression 被混在一起判斷。

優先修改：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/death/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/block/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/rush/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/special/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/events/UHCGamingDeathEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/events/UHCBlockBreakEvent.java`

要做的事：

1. 先建立 scenario 組合驗收清單，從高風險組合開始：`ShiftKill`、`NoClean`、`TimeBomb`、`SwapInventory`、`BackPack`、`VeinMiners`、`Limitations`、`BloodDiamonds`、`CutClean`。
2. 每個組合先確認舊版原作者行為；若無法實測舊版，必須標成程式碼推論或待確認，不得直接視為 regression。
3. Death/inventory 類先確認事件順序、掉落清單、復活資料、死亡箱、背包交換與無敵時間是否互相覆蓋。
4. Block/drop 類先確認連鎖挖礦、礦物上限、經驗、fortune/silk touch、自動熔煉與扣血規則同時啟用時是否符合預期。
5. 對每個組合分類為一致、可接受差異、版本限制、升級 regression、或原版既有特性。
6. 只修升級造成的 regression；原作者特性或可接受差異只記錄，不在本步驟改成新規則。

完成條件：

- 高風險 scenario 組合有明確驗收結果與分類。
- 單一 scenario 已通過的行為不因組合修正而 regression。
- 混合互動問題有自己的測試紀錄，不再回塞 Step 15 的單項行為驗收。

目前狀態（2026-05-13）：

- 高風險 death/inventory、block/drop、health/projectile 組合已在 `docs/step-16-scenario-combination-risk-matrix.md` 完成驗收、修正或分類。
- 剩餘 P1 consume/craft/task、P2 低風險抽樣與跨組合風險已完成程式碼檢視；未發現阻塞 1.21.11 升級的高影響問題，暫不逐項實測。
- `Limitations`、`BenchBlitz`、`IronMan` 等舊版長生命週期狀態保留為作者架構限制；目前 `/uhc stop` 會關閉伺服器，不列為 Step 16 升級修復項。
- Step 16 可告一段落；後續針對低風險玩法的調整應另列改善，不阻塞進入 Step 17。

## 17. 核心 Presentation 入口瘦身

Step 11 到 Step 13 只修「能不能用」與解除核心驗收阻塞；本步驟開始整理核心 command / menu / tools 的責任邊界。範圍限於主持與遊戲主流程入口，不含 scoreboard、practice 與非核心展示。

本步驟不是要把每個指令都抽成 service。若指令本身已經只是薄薄的參數檢查與一兩個明確呼叫，維持原狀即可；過度抽象化的候選要從本步驟移除。

已完成的收斂：

- `/uhc regen` 與 CenterCleaner GUI 共用預覽世界建立入口。
- `/uhc choose` 將 cache、host、kick 與 restart 流程收斂到單一入口。
- `/uhc start` 與 GUI Start 共用開始倒數入口。
- `/uhc stop` 將 cache 清除、fallback 與 shutdown 流程收斂到單一入口。
- `/border` 將縮邊界與倒數同步流程收斂到單一入口。
- `{fancy-time}` / time placeholder 已修正 1.21.11 實測遇到的格式化問題。

本步驟不再處理：

- `/uhc tp`：目前邏輯很薄，保留在 command 內即可，不抽 application service；先前抽出的單用途 service 已還原。
- `/uhc edit`、`/uhc sethost`、`/setspawn`、白名單、reload/tutorial 與一般 host setting menu：目前沒有足夠複雜度支撐新的抽象。
- `/giveall`：功能正常，Step 17 不為了 presentation 瘦身抽出新 service；後續 Step 21 只處理 command framework 脫離 Foundation。
- `/respawn`：功能正常但流程風險高；除非日後出現明確 bug，否則不為了瘦身改動。
- spectator / staff hotbar 與 scoreboard / practice / 非核心展示：維持 Step 18 範圍。
- `/staff` 關閉後不清 hotbar 物品是已確認的舊版行為；除非明確決定改變舊行為，否則不放進 Step 17。
- `gui.yml`、`items.yml` 只有在核心入口實測出現 material alias 或 button 設定問題時才修改，不做預防性掃改。
- Foundation command/menu base 類可以暫時保留；完整移除 Foundation 留到 Step 21。

完成條件：

- 已完成的 `/uhc regen`、CenterCleaner GUI、`/uhc choose`、`/uhc start`、GUI Start、`/uhc stop` 與 `/border` 維持既有人工驗收結果。
- `/uhc tp` 的過度抽象化已還原，且仍保留以目前 `MatchCenter` 傳送的行為。
- 新增或修改的核心入口不再擴大 Foundation 耦合，且沒有為單一薄指令新增不必要 service。

## 18. Scoreboard / Practice / 非核心 Presentation 收斂

本步驟處理 Step 17 未納入的展示與非核心入口，避免 command/menu/tools/scoreboard/practice 全部塞在同一步。做法沿用 Step 17 的保守原則：先驗收、再判斷是否需要小修；已能運作且只是架構不夠乾淨的 thin command / menu，不為了抽象化而改。

驗收與檢查範圍：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/info/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/team/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/game/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/practice/*.java`
- `Update-WonderlandUHC/src/main/resources/commands.yml`
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`

要做的事：

1. 建立並執行 scoreboard、practice、team/info menu 與非核心 inventory editor 驗收。
2. Scoreboard 先檢查 Paper `1.21.11` 啟動、sidebar 更新、placeholder 與 staff metric；只有發現 async scoreboard、placeholder 或 TPS/RAM 顯示問題時，才做最小修正。不要預防性導入大型 projection/view model。
3. `RuntimeUtil.getTPS()` 的 Paper `getTPS()` / fallback stopgap 只在 staff scoreboard 實測仍有錯誤、log spam 或難以追蹤時，收斂成小型 scoreboard metric helper；不做通用 runtime metric 架構。
4. Practice mode 保留 `/practice` 舊功能；驗收指令、練習世界、邊界、物品、隨機傳送、死亡補裝與破壞方塊取消。除非實測壞掉，不重做 service/use case。
5. Team / info / stats 類 command 與 menu 先驗收；若目前只是權限、參數檢查與呼叫既有 team/menu 方法，維持原狀，不硬抽 application service。
6. `PlayersMenu`、`ButtonLocalization`、player head / skull / page button 等 Foundation menu 高風險點先實測或列入 Step 21 追蹤；只有 Paper `1.21.11` 實際失效時才在本步驟局部修正。
7. 回測 Step 11 共用 `InventoryEditButton` 修正對非核心 editor 的附帶影響：`Practice_Inventory`、`Disable_Items`、`Custom_Drops` 必須各自能完成保存，若仍有差異則在本步驟處理。

不做的事：

- 不把所有 menu 改成 view model。
- 不把所有 menu action 改成 use case。
- 不把所有 team/info command 抽成 application service。
- 不全面替換 Foundation `ConfigMenu` / `ConfigMenuPagged`；Foundation 最終移除留到 Step 21。
- 不做全量 resource alias migration；只處理 Step 18 驗收發現的非核心 presentation 問題。

驗收清單：

1. Paper `1.21.11` 測試服啟動後，plugin enable 成功，log 沒有 scoreboard / TPS / Foundation menu 相關 exception。
2. Lobby scoreboard 能正常顯示 title、玩家數、主持人、傳送倒數等 placeholder，不外露 `{...}`。
3. Playing / spectator / staff scoreboard 能更新遊戲時間、邊界、存活數、擊殺數；staff scoreboard 的 `{tps}`、`{free_ram}` 有合理數值且不產生 NMS lookup spam。
4. Staff options menu 的挖掘黃金 / 鑽石提示可切換；遊戲中玩家挖掘一般與 deepslate 黃金 / 鑽石礦時，staff 每五個礦物提示一次。
5. `/practice` 在等待階段可加入與退出；玩家會被傳送到練習世界、取得 practice inventory、退出後回 lobby 並清理狀態。
6. Practice 中死亡會清掉掉落物、恢復血量並重新補裝；practice 中破壞方塊會被取消。
7. `/disableitems`、`/scenarios`、`/stats [玩家]` 可開啟對應 menu，item / scenario icon / stats placeholder 正常顯示。
8. `/team create`、invite / join、leave、public、settings 基本流程可用；team selector menu 可顯示 open team、建立隊伍與加入隊伍。
9. Spectator player menu 的 overworld / nether player list 可開啟，player head 顯示不造成 exception，點擊可傳送。
10. 非核心 inventory editor `Practice_Inventory`、`Disable_Items`、`Custom_Drops` 各自可進入、`finish` 保存、`tohead` 不被 Paper 當成未知指令；保存結果能被對應功能使用或至少不造成 exception。
11. 若上述項目已可運作但仍依賴 Foundation menu，先記錄為 Step 21 legacy 移除追蹤，不在 Step 18 為抽象化而重寫。

完成條件：

- Scoreboard、practice、team/info menu 有驗收清單。
- 非核心 presentation 壞掉不會阻塞核心比賽流程。
- Foundation menu / scoreboard 依賴已完成分類；實際壞掉的項目已修正，未阻塞升級且可運作的項目能被 Step 21 legacy 移除明確追蹤，並在 Step 22 最終驗收。

## 19. Bukkit/Paper API 與剩餘 Legacy Adapter 清理

當 Step 11 到 Step 18 已把核心入口、scenario 與 presentation 主要路徑拆開後，再集中清理剩餘 Bukkit/Paper API 與 legacy adapter。這一步是發布前移除相容層的前置整理，不是最後才第一次拆 Foundation / DatouNMS。

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
9. 對 `LegacyFoundationAdapter` / `LegacyDatouNmsAdapter` 剩餘呼叫分類：可立即替換、等 Step 21 拔除、或需明確保留為正式 platform service。
10. `PlayerLoginEvent` 已完成 Paper `1.21.11` 替代流程研究，但不在 Step 19 直接重寫；它牽涉登入 gate、權限 bypass、白名單與 `UHCPlayer` 建立時機，需列入 Step 21 legacy 移除處理，並在 Step 22 最終對照驗收。

完成條件：

- 1.21.11 compile 通過。
- API 相容碼集中，不散落在 core/application。
- Step 21 移除相容層前的剩餘清單具體且可驗證。

## 20. IDE 小型警告整理

本步驟只處理 IDE / 靜態分析中低風險、低行為影響的雜訊警告，讓後續 deprecated、Foundation、NMS 與行為 regression warning 更容易被看見。這不是全量 lint cleanup，也不追求把所有 IDE warning 歸零。

優先修改或盤點：

- `Update-WonderlandUHC/src/main/java/**/*.java`
- `Update-WonderlandUHC/src/test/java/**/*.java`
- `Update-WonderlandUHC/docs/ide-warning-cleanup-inventory.md`

要做的事：

1. 先盤點 IDE warning 類型，至少分成 unused import、明確 unused private member、local variable / redundant cast、null-safety / unchecked noise、deprecated / legacy API、Foundation / DatouNMS 相關 warning。
2. 第一輪只清可機械判斷且不改行為的警告：unused import、明確 unused private method / private field / local variable、明顯 redundant cast 或 redundant type argument。
3. 若 IDE 標示 unused 但可能被 Bukkit、Foundation、command、listener、menu、config、serialization、reflection 或 public API 使用，先記錄不刪。
4. null-safety、unchecked conversion、raw type 只在不改資料模型、不改 public API、不新增抽象層時處理；需要模型或 API 調整的項目延後到對應功能步驟。
5. deprecated API、Foundation / DatouNMS、`ChatColor / team color model`、`PlayerLoginEvent`、metadata state 不在本步驟處理，維持 Step 19 的分類結果並交給 Step 21。
6. 不用大量 `@SuppressWarnings` 或 wrapper 隱藏 warning；只有第三方 API annotation 誤判且有文件說明時才可局部使用。

完成條件：

- 已建立 IDE warning 分類紀錄，並清掉一批低風險雜訊警告。
- 沒有刪除 public API、plugin.yml 註冊入口、config / reflection 可能使用的成員。
- 沒有修改 Foundation / DatouNMS / deprecated API 的實質行為。
- 程式碼變更後依專案規則完成封裝與 Paper `1.21.11` 啟動 smoke test。

## 21. 最終移除 Legacy Foundation / NMS 相容層

Step 5 與 Step 6 只負責讓 Foundation、DatouNMS 從主線退到 legacy 層；Step 11 到 Step 19 逐步替換核心入口、scenario、presentation 與 API 使用點。本步驟是最終驗收前的 legacy 拔除 gate，只做最後拔線、刪除依賴與必要替代，不應承擔第一次大規模拆除。

詳細盤點、子切片順序與每刀驗收 gate 記錄於 `docs/step-21-legacy-removal-plan.md`；Step 21 不新增頂層 step，但實作必須拆成多個可驗證子切片。

目前進度（2026-05-16）：Step 21 已建立 `step-21-legacy-removal` 分支並完成 21.1 DatouNMS / NMS 移除整理，`update-to-1.21` 已包含 `d3da1e9` 與 `e8c0bb9` 兩個整理後 commit。主流程 `LegacyDatouNmsAdapter.current()` call site 已清空；`setupNms()`、`LegacyDatouNmsAdapter`、`PlatformCapabilities` 與 DatouNMS dependency 已移除；後續補強也移除 `PlayerUtils#breakBlockNms` 與 `LegacyFoundationAdapter#getHandleEntity` / `newBlockPosition` 的間接 NMS reflection。`src/main/java` / `build.gradle` 搜尋 gate 已無 DatouNMS / NMS 命中，封裝與 Paper startup 已通過。21.2 Foundation utility adapter 已完成只讀盤點：Foundation 相關命中仍有 `648` 行；logging / console utility 第一刀已改用 `PluginConsole`，`LegacyFoundationAdapter.log*` / `consoleLineSmooth` 使用點已清空；`error(...)` 小切片已改由 `PluginConsole.error(...)` 承接，`LegacyFoundationAdapter.error(...)` 使用點已清空；scheduler / event 小切片已改由 `PluginScheduler` / `PluginEvents` 承接，`LegacyFoundationAdapter` 的 `runLater` / `runTimer` / `callEvent` / `registerEvents` 使用點已清空；player / action bar 小切片已改由 `PluginPlayers` 承接，`getOnlinePlayers` / `getPlayerByUUID` / `getPlayerByNick` / `getPlayerNames` / `kickPlayer` / `sendActionBar` 使用點已清空；text / placeholder / time 小切片已改由 `PluginText` 承接，`colorize` / `replace*` / `formatTime` / `formatFiveDigits` / `bountifyCapitalized` 使用點已清空；material / item / block classification 小切片已改由 `PluginMaterials` 承接，`materialOf` / `itemOf` / `isAir` / `isLeaves` / `isLog` / `isLongGrass` / `isDoublePlant` / `getFirstItem` 使用點已清空；random / math 小切片已改由 `PluginRandom` 與 Java/Bukkit 原生 API 承接，`nextItem` / `chance` / `nextBoolean` / `range` / `ceiling` / `isSimilar` / `getMaxHealth` 使用點已清空；sound playback 小切片已讓 `Extra.sound(...)` 直接播放既有 `SimpleSound`，工具破裂音改走 Bukkit API，`playSound` / `playGlobalSound` / `playItemBreakSound` 使用點已清空，但 Foundation `SimpleSound`、`Sounds.java`、`SoundConfigParser`、`YamlConfigLoader`、scenario sound 欄位與 `sounds.yml` migration 尚未移除，需後續獨立處理；version / old-server compatibility 小切片已固定 Paper `1.21.11` 目標，移除 `isAtLeastMinecraft*` / `isOlderThanMinecraft*` / `isPaperServer` / `getServerVersion` 與舊版 portal reflection fallback 使用點；legacy WorldBorder fallback 已移除，預生成正式依賴 Chunky，`Dependency.WORLD_BORDER`、WorldBorder softdepend、WorldBorder compileOnly、`wb fill` fallback 與 `WorldBorderFillFinishedEvent` listener 已清空；metadata / chunk force-loaded 小切片已改用 owner-managed map / set、projectile PDC marker 與 `Chunk#setForceLoaded(...)`，`LegacyFoundationAdapter` 的 temp metadata 與 `setChunkForceLoaded` wrapper 已清空；permission / validation 小切片已改用 Bukkit permission 與本地 `Messages.NO_PERMISSION`，移除 `LegacyFoundationAdapter` 的 `hasPermission` / `checkPermission` / `checkBoolean` wrapper，並移除 `Dependency.check()` / `checkSoft()` 的 Foundation exception 依賴；file extraction / data-file access 小切片已改用 Bukkit `saveResource(..., false)` 與 plugin data folder 明確路徑，`extractFile` / `getFile` / `getOrMakeFile` wrapper 已清空；broadcast failure 小切片已改用本地 `BroadcastDeliveryException`，`LegacyFoundationAdapter.failure` / `isFailure` wrapper 已清空；reflection / location fallback 小切片已移除 `getLocationOrDefault` / `getFieldContent` / `getStaticFieldContent` wrapper，`TestCommand files` 反射 debug 分支已移除；command dispatch 小切片已讓 team chat 轉發改用 `PluginScheduler` + Bukkit `Player#performCommand(...)`，`dispatchCommand` / `dispatchCommandAsPlayer` wrapper 已清空；tutorial boxed message 小切片已在 `TutorialSection` 本地輸出分隔線與訊息，`tellBoxed` / `broadcastBoxed` wrapper 已清空；clickable run-command component 小切片已改用 Adventure component，`sendRunCommandComponent` wrapper 已清空，既有 `&` 色碼仍暫以 Adventure legacy ampersand serializer 承接；time symbols 小切片已改由 `TimePlaceholderFormatter` 直接設定，`configureTimeSymbols` wrapper 已清空。21.3 command framework 已完成只讀盤點：command 目錄約 55 個檔案，`SimpleCommand` 4 個、`SimpleSubCommand` 14 個、`SimpleCommandGroup` 6 個；確認新版不會使用 fallback server 後，已正式移除 `/leave`、lobby/spectator leave hotbar item、`Bungee_Lobby`、BungeeCord outgoing channel 與 `sendToFallbackServer`；`/setspawn`、`/config|/cfg`、`/disableitems`、`/scenarios`、`/finish`、`/tohead`、`/staff`、`/reconnect`、`/topkills|/killtop|/kt`、`/viewheal|/h`、`/sendcoords|/scs`、`/spectoggle`、`/border`、`/giveall`、`/stats` 與 `/practice` 已改成 Bukkit `CommandExecutor` + `plugin.yml` native command entry，並從 Foundation dynamic command registration 移除；`/mlg` 舊活動/測試指令已移除，包含 command class、註冊、權限與專用音效設定。`/practice <玩家>` 舊參數已依委託人決策不保留，只保留 `/practice` 切換自己；後續補充進度已開始處理 group command，以後續 `/whitelist`、`/team` 與 `/uhc` 狀態紀錄為準。

補充進度（2026-05-16）：`/backpack|/bp`、`/respawn` 已改成 Bukkit native command；舊 `TestCommand` / `EmptyCommand` 與 `FeatureRegistry#registerCommands(...)` 已移除，single command dynamic registration path 已收掉。進入 group command 前已依序盤點並處理 `/whitelist`、`/team` 與 `/uhc`，沒有一次重寫三組 group。

補充進度（2026-05-16）：`/whitelist|/wl` 已改成 Bukkit native command，保留 `add/remove/list/clear` 與既有白名單資料、權限、訊息語意；`/team` 也已改成 Bukkit native command，保留 `chat/create/disband/invite/join/leave/kick/list/promote/public/settings` 子指令與 team package 內的薄 dispatch/helper。`/uhc` 已改成 Bukkit native command，保留 `reload|rl/choose/edit/regen/resetteam/sethost/splitteam/switchteam/stop/tp/start/tutorial` 子指令與 `uhc edit`、`/uhc regen confirm/skip` 內部 label。`FeatureRegistry#registerCommandGroups(...)`、`LegacyFoundationAdapter.commandGroupRegistrar(...)`、`LegacyFoundationAdapter.setTellPrefix(...)`、`CommandHelper`、`UHCCommandGroup` 與 `UHCMainCommandGroup` 已移除；command package 目前不再有 `SimpleCommand` / `SimpleSubCommand` / `SimpleCommandGroup` source 使用。下一步不要再擴大 command 重構，應先只讀盤點 21.4 settings / YAML config lifecycle 與 `WonderlandUHC extends SimplePlugin` / reload lifecycle 的解除順序；21.5 menu framework 後續再處理。

補充進度（2026-05-17）：21.4 settings / YAML config lifecycle 已完成只讀盤點並更新 `docs/step-21-legacy-removal-plan.md`。目前 Foundation source import 仍有 `176` 行；command framework 已清空，但 `WonderlandUHC extends SimplePlugin`、`Settings extends SimpleSettings`、`AutoLoadStaticConfig extends YamlStaticConfig`、多個 `YamlConfig` store、`SerializedMap` / `ConfigSerializable` model 與 `SimpleSound` sound config 仍綁住 Foundation。21.4 第一刀不應直接拔 `SimplePlugin`，應先把 `Settings` / `Messages` / `CommandSettings` 的 static config load / reload 顯式化；`Sounds`、`cache.db` / `savedgames.db` / `stats.yml` store、scenario / scoreboard / death message / broadcast YAML reader 再分批處理。`ButtonLocalization`、`UHCMenuSection` 與 `gui.yml` menu 行為留到 21.5，不混入 21.4。

補充進度（2026-05-17）：21.4 第一個程式碼切片已完成。`Settings`、`Messages`、`CommandSettings` 已改用本地 `PluginStaticConfig` 明確從 plugin data folder 載入 YAML，不再繼承 `SimpleSettings` / `AutoLoadStaticConfig` / `YamlStaticConfig`；`PluginBootstrap#loadStaticConfiguration()` 於 `onReloadablesStart()` 保存 resources 後明確載入三份 static config。封裝通過，Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功。該切片當時尚未處理 `Sounds`、`AutoLoadStaticConfig`、`YamlConfigLoader`、`SimpleLocalization` bridge、`SimplePlugin` lifecycle 與其他 `YamlConfig` store；後續 21.4 補充進度已記錄完成項與剩餘 21.5 邊界。

補充進度（2026-05-17）：21.4 sound config / playback 第一刀已完成。`Sounds` 已改用本地 `PluginStaticConfig` 明確載入，`SoundConfigParser`、`Extra.sound(...)` 與 scenario `@FilePath` sound 欄位已改走本地 `PluginSound`，並保留既有 `sounds.yml` / `scenarios.yml` 音效字串格式；`AutoLoadStaticConfig` 與舊 `YamlConfigLoader` 已移除。`SimpleSound` 目前只剩 `StatsMenu#getClickSound()` 與 `LegacyFoundationAdapter#configureMenuClickSound()`，這兩個仍被 Foundation menu API 簽名綁住，留到 21.5 menu framework，不混入 21.4。

補充進度（2026-05-17）：21.4 `stats.yml` 第一刀已完成。`StatsStorageYaml` 已改用 Bukkit `YamlConfiguration` 直接讀寫 `stats.yml`，`UHCStats` 不再實作 Foundation `ConfigSerializable`，也不再使用 `SerializedMap`；既有資料 key 保留為 `Game_Played`、`Kills`、`Wins`，本場 `kills` 與 `oreMined` 仍不寫入累積統計檔。封裝通過，Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功。`cache.db` / `savedgames.db`、`UHCGameSettings` nested serialize / deserialize、其他 `YamlConfig` readers 與 21.5 menu framework 尚未處理。

補充進度（2026-05-17）：21.4 `cache.db` / `savedgames.db` settings model 第一刀已完成。`UHCGameSettings`、5 個 sub-settings 與 `InventoryContent` 已改成本地 map / section 讀寫，不再使用 Foundation `ConfigSerializable` / `SerializedMap`；`WorldLoadingCacheStore` 與 `SavedGameSettingsStore` 也已改用 Bukkit `YamlConfiguration`，保留既有 `cache.db` 的 `Host`、`Loading_Status`、`Settings`、`Match_Center.*` 形狀，以及 `savedgames.db` 的 UUID -> settings list 形狀。封裝通過，Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done` 並可讀回既有 `cache.db`，console 執行 `uhc reload` 成功；尚未做主持選單人工保存測試，後續需確認 GUI 改動 timer / border / inventory preset 後寫回形狀仍一致。`SimpleLocation` 後續已確認無 production / test 使用點並刪除。

補充進度（2026-05-17）：21.4 `spawns.yml` reader 第一刀已完成。`UHCSpawn` 已移除 Foundation `YamlConfig` 繼承，改用 Bukkit `YamlConfiguration` 直接讀寫 `spawns.yml`；保留既有 `Lobby` key 與單行 `world x y z yaw pitch` 格式，讀取缺值或解析失敗時仍 fallback 到主世界 spawn 並讓 `isSet()` 維持 false，`/setspawn` 寫回仍使用 block 座標與整數 yaw / pitch。封裝通過，Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功；`settings/spawn` package 已無 Foundation `YamlConfig` import。其他 `YamlConfig` readers，例如 `ScenarioConfig`、death message，仍留在後續 YAML reader 切片。

補充進度（2026-05-17）：21.4 `scoreboards.yml` theme reader 第一刀已完成。`SidebarTheme` 與內層 `ThemeLoader` 已移除 Foundation `YamlConfig` 繼承，改由 `SidebarTheme.loadThemes()` 使用 Bukkit `YamlConfiguration` 直接讀取 `scoreboards.yml`；保留頂層 theme key 順序、`Default` fallback 語意、8 組 scoreboard line list 與 host theme selector 使用的 `getAllThemes()` 行為。封裝通過，Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功；`SidebarTheme.java` 已無 Foundation `YamlConfig` import / inheritance。`SidebarThemeSettingsMenu` 仍屬 21.5 menu framework，不混入本刀。

補充進度（2026-05-17）：21.4 `broadcasts.yml` sender reader 第一刀已完成。`AbstractBroadcastSender` 已移除 Foundation `YamlConfig` 繼承，改用 Bukkit `YamlConfiguration` 直接讀取 `broadcasts.yml`；保留 `Discord` section、`Formatting`、`Invalid_Channel`、`Channel_Ids` 的相對讀取語意，`DiscordBroadcastSender` 的 DiscordSRV ready 檢查、mention 轉換、allowed mentions 與錯誤回報流程不改。封裝通過，Paper `1.21.11` 測試服以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功；`model/broadcast` package 已無 Foundation `YamlConfig` import / inheritance。`BroadcastSettingsMenu` 仍屬 21.5 menu framework，不混入本刀。

補充進度（2026-05-17）：21.4 `messages.yml` death message reader 與 `scenarios.yml` scenario config reader 已完成。`DeathMessageLoader` 與 `ScenarioConfig` 都已改用 Bukkit `YamlConfiguration`，保留既有 death message 結構、DamageCause key、scenario key、`Type` / `Name` / `Description`、`@FilePath` 反射填值、舊 material alias 與 sound alias；封裝與 Paper `1.21.11` startup / `uhc reload` 均已通過。`CommandSettings` 的 Foundation `SimpleLocalization.Commands.*` bridge 已移除，native command 訊息直接使用 `commands.yml` 載入後的 `CommandSettings` 欄位。

補充進度（2026-05-17）：21.4 settings / YAML config lifecycle 程式碼範圍已收尾。`SimpleSettings`、`YamlStaticConfig`、`AutoLoadStaticConfig`、舊 `YamlConfigLoader`、Foundation `SerializedMap` / `ConfigSerializable` 與主插件 command localization bridge 在 source 內已無命中；非 menu YAML readers 已離開 Foundation `YamlConfig`。剩餘 `ButtonLocalization extends YamlConfig`、`UHCMenuSection` / `MenuSection`、Foundation menu classes、`StatsMenu#getClickSound()` / `LegacyFoundationAdapter#configureMenuClickSound()` 的 `SimpleSound`、以及 `WonderlandUHC extends SimplePlugin` 都歸到 21.5 menu framework / 後續 lifecycle 移除，不視為 21.4 未完成項。

補充進度（2026-05-17）：進入 21.5 前，21.4 已整理成 4 個 logical commits 並 fast-forward merge 回 `update-to-1.21`；`step-21-legacy-removal` 與 `update-to-1.21` 都已完成封裝、Paper `1.21.11` startup 與 `uhc reload` 驗證，備份分支 `step-21-before-menu-framework-20260517` 已刪除。21.5 menu framework 已完成只讀盤點並記錄於 `docs/step-21-legacy-removal-plan.md`：目前 menu package 26 個檔案、`ConfigMenu` 類型約 18 個、`ConfigMenuPagged` 7 個，另有 `ButtonLocalization` / `UHCMenuSection` / `LegacyFoundationAdapter#configureMenuClickSound`、Foundation `Tool` / `ConfigItem` hotbar 工具、`ItemCreator`、conversation button、inventory editor 與 color picker 等風險點。第一刀建議只做本地 `gui.yml` / `items.yml` item reader 與低風險 icon / localization 讀取，不直接重寫 host settings 或 inventory editor。

補充進度（2026-05-17）：21.5 第一個程式碼切片已完成。新增本地 `PluginItems` 承接薄的 YAML item 建立與 Bukkit item meta；`ButtonLocalization` 不再繼承 Foundation `YamlConfig` / `ItemCreator` 讀取 `gui.yml` 的 `Leave` item，`ConfigBasedScenario` 不再使用 Foundation `ItemCreator` 建立 scenario icon。這刀不碰 Foundation menu base、pagination、host settings、inventory editor、conversation 或 tools registry；封裝、Paper `1.21.11` startup 與 `uhc reload` 已通過。

補充進度（2026-05-17）：21.5 tools registry 第一刀已完成。`UHCTool` 不再繼承 Foundation `Tool` 或使用 `ConfigItem.fromItemsFile(...)`，改由本地薄 registry 讀 `items.yml` 的 item / slot；`ToolListener` 接手 hotbar tool 的右鍵 dispatch、背包點擊保護與丟棄保護，`DisableItemListener` 改用本地 `UHCTool` 判斷，`WonderlandUHC#areToolsEnabled()` 關閉 Foundation 自動 `ToolsListener`。這刀不碰 menu item render 的 `ConfigItem`、host settings、pagination 或 inventory editor；封裝、Paper `1.21.11` startup 與 `uhc reload` 已通過。

補充進度（2026-05-17）：手動測試 `/staff` 時發現 `items.yml` 的 `Spectator.Random_Teleport.Type: ENDER_PORTAL_FRAME` 未被本地 `PluginItems` alias 承接，導致 staff / spectator hotbar 初始化失敗；已補 `ENDER_PORTAL_FRAME -> END_PORTAL_FRAME`，不改 hotbar 工具架構。

補充進度（2026-05-17）：21.5 local fixed menu base / `StatsMenu` 第一刀已完成。新增本地薄 `PluginMenu` / `PluginMenuListener` / `PluginMenuSection`，只承接固定 slot menu 的開啟、渲染、點擊取消與 click dispatch；`StatsMenu` 不再繼承 Foundation `ConfigMenu` 或使用 `ConfigClickableButton` / `ConfigMenuButton` / `SimpleSound`，改讀 `gui.yml` 的 `Stats` section 與 4 個固定統計 item。這刀不碰 parent/back、pagination、host settings、conversation 或 inventory editor；封裝、Paper `1.21.11` startup 與 `uhc reload` 已通過，玩家端 `/stats` 點擊不可拿走仍需手動確認。

補充進度（2026-05-17）：21.5 local read-only pagination 第一刀已完成。新增本地薄 `PluginPagedMenu`，只承接 item snapshot、page size clamp、title page suffix、上一頁 / 下一頁按鈕與 click 後重開；`DisableItemListMenu` 與 `EnabledScenariosMenu` 不再繼承 Foundation `ConfigMenuPagged`，改用本地 `PluginMenuSection` / `PluginPagedMenu` 顯示既有 disabled item 與 enabled scenario icon。這刀不碰 parent/back、host settings、player head、team selector、scenario settings、saved settings 或 sidebar theme selector；封裝、Paper `1.21.11` startup 與 `uhc reload` 已通過，玩家端 `/disableitems`、`/scenarios` 與多頁翻頁仍需手動確認。

補充進度（2026-05-17）：21.5 `InventoryViewer` 第一刀已完成。`InventoryViewer` 不再繼承 Foundation `ConfigMenu`，也不再使用 `ConfigDummyButton` / `InventoryDrawer` / `SimpleReplacer`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `See_Inventory`，保留 staff 右鍵玩家入口、壓縮顯示目標玩家 storage / armor / extra contents，以及 slot `51` / `52` / `53` 的血量、飽食度、等級資訊。這刀不新增 drawer framework，也不碰 `PlayersMenu`、host settings、conversation 或 inventory editor；封裝、Paper `1.21.11` startup 與 `uhc reload` 已通過，玩家端 staff 右鍵開啟與點擊不可拿走仍需手動確認。

補充進度（2026-05-17）：21.5 `PlayersMenu` 第一刀已完成。`PlayersMenu` 不再繼承 Foundation `ConfigMenuPagged`，也不再使用 Foundation `ItemCreator` / `CompMaterial`；改用本地 `PluginPagedMenu` / `PluginMenuSection` 顯示主世界或地獄的存活參賽玩家列表，保留名稱排序、`PLAYER_HEAD`、online mode head owner，以及點擊後 `GameUtils.spectateTeleport(...)`。`OverworldPlayersItem` / `NetherPlayersItem` 改用本地 `PluginMenuSection`。這刀不新增 skull helper，也不碰 `TeamSelectorMenu` 或 host settings；封裝、Paper `1.21.11` startup 與 `uhc reload` 已通過，spectator 玩家端 hotbar 開啟與點頭傳送仍需手動確認。

補充進度（2026-05-17）：21.5 dead `EmptyMenu` removal 已完成。`EmptyMenu` 沒有任何建立或引用點，且仍繼承 Foundation `ConfigMenu` 並使用空白 `UHCMenuSection.of("")`；本刀直接刪除該死碼，不新增替代 menu abstraction。`InventoryListener` 仍在 `PlayingState` 提供 spectator inventory 防護，並且目前仍需相容尚未遷移的 Foundation menu，因此不在本刀改動；後續等更多 menu 遷移完成後，再一起評估本地 menu open-state 與 bottom-inventory policy。封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `TeamSelectorMenu` 第一刀已完成。`TeamSelectorMenu` 不再繼承 Foundation `ConfigMenuPagged`，也不再使用 Foundation `Menu.getMenu(...)` / `Button` / `InventoryDrawer` / `ItemCreator` / `ConfigItem`；改用本地 `PluginPagedMenu` / `PluginMenuSection` 顯示 `Team_Selector`。保留 open-join team 列表、滿隊/可加入 item、點擊加入隊伍、底部 `Create_Your_Own` 按鈕執行 `team create`，以及 team join / leave / disband 時刷新已開啟隊伍選單。`PluginPagedMenu` 只放寬 `getItemAt(...)` / `onClick(...)` 讓子類可處理固定 slot，不新增完整 footer framework。本刀不碰 `TeamSettingsMenu`、`ColorPickerMenu`、conversation input、`/team` command 或 `UHCTeam` 核心行為；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `SavedSettingsMenu` 第一刀已完成。`SavedSettingsMenu` 不再繼承 Foundation `ConfigMenuPagged`，也不再使用 Foundation `Menu` parent / `Button` / `ItemCreator` / `ConfigItem`；改用本地 `PluginPagedMenu` / `PluginMenuSection` 顯示 `Saves`。保留 saved settings 列表、左鍵載入後回 `MainSettingsMenu`、中鍵覆蓋記憶體中的既有設定、右鍵刪除並保存、底部 `Save_As` 另存並保存；返回主設定頁只在本 menu 內使用 `Leave` item，不新增通用 parent/back framework。`PluginMenuSection` 只新增按鈕 name / lore 讀取 accessor，讓 saved settings 預覽繼續走 `GamePlaceholderReplacer`。本刀不改 `savedgames.db` 格式、不修正中鍵覆蓋未立即保存的既有行為、不遷移其他 host settings menu；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `SidebarThemeSettingsMenu` 第一刀已完成。`SidebarThemeSettingsMenu` 不再繼承 Foundation `ConfigMenuPagged`，也不再使用 Foundation `Menu` parent / `ItemCreator` / `ConfigItem` / `UHCMenuSection`；改用本地 `PluginPagedMenu` / `PluginMenuSection` 顯示 `Sidebar_Theme_Selector`。保留 theme 列表、`{theme_name}`、`{theme_preview}`、lobby line preview 前的 `updateGlobalVariables()`，以及點擊 theme 後寫入 `Game.getSettings().getScoreboardSettings().setSidebarTheme(...)` 並回到 `ScoreboardSettingsMenu`。本刀不遷移 `ScoreboardSettingsMenu` 本體、heart color selector、update tick button、`SidebarTheme` / `ScoreLines` 或 scoreboard 更新邏輯；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `ScenarioSettingsMenu` 第一刀已完成。`ScenarioSettingsMenu` 不再繼承 Foundation `ConfigMenuPagged`，也不再使用 Foundation `Menu` parent / `Button` / `ItemCreator` / `UHCMenuSection`；改用本地 `PluginPagedMenu` / `PluginMenuSection` 顯示 `Scenarios`。保留 scenario icon、啟用/停用狀態 lore、enabled glow、點擊 toggle、成功後廣播與播放音效、底部 `Clear_Scenarios` 清除全部並刷新 menu。因本地 `PluginPagedMenu` 不會自動重畫 clicked item，本刀在 toggle / clear 後只重開本 menu 以更新狀態；不改 `ScenarioManager` / `Scenario` / `ConfigBasedScenario` / scenario YAML，也不新增通用 item builder 或 parent/back framework；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `ScoreboardSettingsMenu` / `ColorPickerMenu` 第一刀已完成。`ScoreboardSettingsMenu` 不再繼承 Foundation `ConfigMenu`，改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Scoreboard`，保留 `Themes` 開啟 sidebar theme selector、`Update_Ticks` 左鍵減 1 / 右鍵加 1 且最低 1、`Heart_Color` 開啟 color picker 並寫回 scoreboard heart color。`ColorPickerMenu` 不再繼承 Foundation `ColorMenu`，改用本地 `PluginPagedMenu<ChatColor>` 顯示 16 種 Bukkit `ChatColor` 對應羊毛，選色後回呼原本 menu；`TeamSettingsMenu` 只調整 color picker 返回方式，不遷移 team settings 本體。這刀不改 `UHCScoreboardSettings` 的 `CompChatColor` model / 儲存格式，也不新增通用 parent/back framework；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `TeamSettingsMenu` 第一刀已完成。`TeamSettingsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `ConfigSaveInputButton` / `ConfigBooleanButton` / `ConfigClickableButton` / `SimpleReplacer` / `UHCMenuSection`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Team_Settings`。保留隊伍名稱輸入、隊伍徽章輸入、徽章重複檢查、隊伍顏色選擇、`team public` 自由加入切換與 `team ?` 說明入口。文字輸入只在 `TeamSettingsMenu` 內用短暫 input session 實作，並由既有 `ChatListener` 攔截；不新增通用 conversation framework，也不使用已 deprecated 的 Bukkit `ConversationFactory`。這刀不改 `UHCTeam` 資料模型、team public 指令、team color model、team persistence 或 inventory editor / number / time input button；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `StaffOptionsMenu` 第一刀已完成。刪除只包 `UHCPlayer` / `StaffOptions` 的薄 Foundation base `StaffMenu`；`StaffOptionsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `ConfigBooleanButton` / `ConfigChangeValueButton` / `ConfigMenuButton` / `ItemPath` / `UHCMenuSection`。改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Staff_Options`，保留黃金 / 鑽石挖礦提示 toggle、顯示 spectator / staff toggle 後 `uhcPlayer.checkHide()`、移動 / 飛行速度左鍵減 1 / 右鍵加 1 且限制 1 到 5。狀態文字保留 Foundation 舊語意 `&aOn` / `&cOff`；不改 `StaffOptions` model、staff hotbar item、role hide 邏輯、ore alert listener 或速度公式；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `BroadcastSettingsMenu` 第一刀已完成。`BroadcastSettingsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `ConfigClickableButton` / `ItemPath` / `UHCMenuSection`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Broadcast`。保留 `Discord` 按鈕、DiscordSRV soft dependency 檢查、缺少 DiscordSRV 時的提示、`GameStartTimeInputSession` 輸入流程、`DiscordBroadcastSender` 發送流程，以及只捕捉 `BroadcastDeliveryException` 回覆玩家的舊錯誤邊界。這刀不改 `GameStartTimeInputSession` / `ChatListener` / `DiscordBroadcastSender` / `AbstractBroadcastSender`、不改 `broadcasts.yml` 或 DiscordSRV 發送細節，也不新增 broadcast / menu helper 抽象；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `TimeSettingsMenu` 第一刀已完成。`TimeSettingsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `ConfigTimeEditButton` / `ConfigMenuButton` / `UHCMenuSection`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Times`。保留 `Damage`、`Final_Heal`、`Pvp`、`Border_Shrink`、`Disable_Nether` 五個時間按鈕，保留點擊後輸入秒 / 分:秒 / 時:分:秒、無效時間回 `Messages.Editor.Time.INVALID_TIME`、有效輸入後寫入 `UHCTimerSettings` 並 `CacheSaver.saveCache()`；`ChatListener` 只新增必要 input session 轉接。這刀不改 `BorderSettingsMenu`、timer countdown 行為、時間設定資料模型或 YAML 格式，也不新增全域 conversation framework；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `BorderSettingsMenu` 第一刀已完成。`BorderSettingsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `UHCNumberEditButton` / `ConfigNumberEditButton` / `ConfigTimeEditButton` / `ConfigLeftOrRightButton` / `ConfigMenuButton` / `ItemPath` / `UHCMenuSection` / `SimpleReplacer`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Border`。保留邊界大小、地獄邊界、最終邊界大小、邊界收縮速度、收縮耗時計算器與左右鍵切換邊界模式，所有有效輸入仍寫入 `UHCBorderSettings` 並 `CacheSaver.saveCache()`，邊界模式切換仍廣播。這刀不新增正數、上限或 NaN 防禦，不抽全域 conversation framework，也不改 `BorderUtil` / `UHCBorderSettings` / border runtime / YAML 格式；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `TeamModeSettingsMenu` 第一刀已完成。`TeamModeSettingsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `ConfigChangeValueButton` / `ConfigBooleanButton` / `ConfigLeftOrRightButton` / `ConfigClickableButton` / `UHCMenuSection` / `SimpleReplacer`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Teams`。保留隊伍大小左鍵減 1 / 右鍵加 1 且最低 1、同隊傷害點擊切換並廣播、分隊模式左鍵 `CHOSEN` / 右鍵 `RANDOM`，lore 仍顯示 enum `name()`；這刀不新增 `CacheSaver.saveCache()`、不改 `TeamSplitMode` 顯示文字、不改 `UHCTeamSettings` / team runtime / YAML 格式；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `MainSettingsMenu` 第一刀已完成。`MainSettingsMenu` 不再繼承 Foundation `ConfigMenu`，也不再使用 Foundation `ConfigClickableButton` / `ConfigBooleanButton` / `ConfigChangeValueButton` / `ConfigEditorButton` / `ConfigInventoryEditorButton` / `UHCNumberEditButton` / `InventoryEditButton` / `UHCMenuSection` / `SimpleReplacer`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Main`。保留 host settings 主入口、whitelist / Nether / ender pearl damage toggle、players / title 輸入、apple rate / initial experience 左右鍵調整、start / generate map 判斷，以及 custom inventory / practice inventory / custom drops / disable items editor 的 `/finish` / `/tohead` 流程。這刀不遷移 `CenterCleanerMenu`，`Generate_Map` 仍暫時接到既有 confirm menu；也不新增全域 conversation / inventory editor framework。封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `CenterCleanerMenu` 第一刀已完成。`CenterCleanerMenu` 不再繼承 Foundation `ConfigConfirmMenu`，也不再使用 Foundation `Menu` / `UHCMenuSection`；改用本地 `PluginMenu` / `PluginMenuSection` 顯示 `Center_Cleaner`。保留 `Agree` / `Disagree` 兩個按鈕分別呼叫 `PreviewWorldGenerationService.create(player, true, seed)` / `PreviewWorldGenerationService.create(player, false, seed)`，`MainSettingsMenu` 的 `Generate_Map` 與 `MainGui#abrirCenterCleaner(...)` 也改開本地 menu。這刀不改世界生成服務、`CenterCleaner` 或 `/uhc regen`，也不新增通用 confirm menu base；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 dead Foundation menu wrapper removal 已完成。`UHCNumberEditButton`、`InventoryEditButton`、`UHCMenuSection` 已確認在 production / test source 無使用點並刪除；number input、inventory editor 與 gui section 讀取已由前面 host menu / 本地 menu 切片承接。這刀不新增替代 wrapper，不改 `MainSettingsMenu` 的本地 input / inventory editor 行為，也不處理 `ButtonLocalization`、`LegacyFoundationAdapter.configureMenuClickSound()` 或 `InventoryListener` 的 Foundation menu 判斷；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 Foundation return button / menu click sound cleanup 已完成。`ButtonLocalization` 與 `LegacyFoundationAdapter` 已刪除，`WonderlandUHC#onReloadablesStart()` 不再呼叫 `PluginBootstrap#configureFoundationLibrary()`；`ButtonLocalization` 只設定 Foundation `ButtonReturnBack`，`LegacyFoundationAdapter.configureMenuClickSound()` 只設定 Foundation `Menu.setSound(...)`，而目前 menu 已改用本地 `PluginMenu` / `PluginPagedMenu`，不再讀這兩個 Foundation 全域設定。這刀不清理 `gui.yml` 根部 back / page button 資源殘留、不處理 `InventoryListener` spectator 防護判斷，也不處理 `WonderlandUHC extends SimplePlugin` lifecycle；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 `InventoryListener` spectator menu 判斷已完成。比賽中 spectator inventory click policy 不變，但 `InventoryListener` 不再使用 Foundation `Menu.getMenu(player)` 判斷是否正在看 menu，改用 top inventory holder 是否為本地 `PluginMenu`；minecart inventory open 防護不變。這刀不改 `PluginMenuListener`、不放寬 spectator 點擊規則，也不處理 `WonderlandUHC extends SimplePlugin` lifecycle 或 `CompChatColor` 類型殘留；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.5 scoreboard heart color `CompChatColor` 移除已完成。`UHCScoreboardSettings` 的 `heartColor` 不再使用 Foundation `CompChatColor`，改用既有 color picker 已採用的 Bukkit `ChatColor`；`ScoreboardSettingsMenu` 選色後直接寫入 `ChatColor`，並恢復讀取 `Scoreboard_Settings.Heart_Color`，相容 `red`、`&c`、`§c` 這類既有保存值，無效值維持預設紅色。這刀不遷移 `ColorPickerMenu` / `UHCTeam` / team public API 的 `ChatColor` model，也不改 scoreboard objective 或 sidebar 更新流程；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

Step 21 必須完成已知 legacy 移除、必要替代，或在本步驟內取得明確接受的行為差異；不得把已知修改項推到 Step 22。Step 22 只做最終對照檢查，除非檢查出 regression 才回到對應實作層修正。

優先修改：

- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/settings/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/**/*.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/**/*.java`
- `Update-WonderlandUHC/src/main/resources/*.yml`

要做的事：

1. 移除 `lib-foundation` shade / dependency；主插件封裝不得再包含 Foundation。
2. 移除 DatouNMS dependency；主插件不得再依賴 `DaTouNMS`、`NewerSpigotAPI` 或直接 `net.minecraft.*` / obc/craftbukkit 反射。
3. 將 `LegacyFoundationAdapter`、`LegacyDatouNmsAdapter` 與只為它們存在的 wrapper 刪除，或改名為非 legacy 的正式 platform/service 實作。
4. command、menu、settings、scoreboard、message、sound、conversation、tools 全部改走主插件自己的 service / adapter 或 Paper/Bukkit API，不再繼承 Foundation 類別。
5. 若某項舊功能最後仍需要低階實作，必須明確落在 `platform/paper/` 或專用 integration，且具備可降級行為；不能用「legacy NMS」名義繼續保留未驗證依賴。
6. 準備 README / DEVELOPMENT / 部署文件需要的最終依賴狀態，供 Step 23 發布文件使用。
7. 重新處理 `PlayerLoginEvent`：不可只用 wrapper 隱藏 deprecated warning；需在本步驟決定以 Paper login connection event 重寫登入 gate，或將需要完整 `Player` 的檢查明確延後到 join 後處理，並在 Step 22 最終對照驗證。
8. 處理 Step 19 延後的 `ChatColor / legacy color model`、`MetadataValue / legacy state` 與 Foundation config API；不能新增短期 wrapper 或 suppress 當作完成。

完成條件：

- `rg -n "org\\.mineacademy\\.fo|LegacyFoundationAdapter|lib-foundation" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle` 為 0。
- `rg -n "DaTouNMS|datounms|NewerSpigotAPI|net\\.minecraft|org\\.bukkit\\.craftbukkit|NMS|Nms|nms|breakBlockNms|newBlockPosition|getHandleEntity|ObjectCreator|playerInteractManager" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle` 為 0，除非是 Paper API 文件明確允許且封裝在 `platform/paper/` 的正式 adapter。
- `Update-WonderlandUHC` 可不依賴 `lib-foundation` repo 完成 clean build、shadow jar 與 Paper `1.21.11` server smoke test。
- Step 19 延後的 deprecated 項目已被移除、替換，或改成非 legacy 的正式 platform/service 實作；任何行為差異都必須在 Step 21 內完成決策與記錄，Step 22 只做最終對照驗證。
- A15 搜尋檢查與 A16 最終驗收清單都已更新為「Foundation / DatouNMS 完全移除」狀態。

## 22. 最終驗收：測試策略與 1.16 / 1.21 功能對照

Step 21 移除 legacy 相容層後，必須用同一份 checklist 驗證升級後行為，而不是只靠人工印象或單次開服。這一步是發布前最終驗收，不再承擔新的 legacy 拔除工作；若驗收發現 regression，回到對應實作層修正後重新驗收。

優先修改或新增：

- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/TestingTest.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/core/**/*.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/application/**/*.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/storage/**/*.java`
- `Update-WonderlandUHC/docs/1.16-to-1.21-function-checklist.md`
- `scripts/package-plugin.sh`
- `scripts/deploy-to-windows-server.sh`

測試分層：

1. Core unit tests：狀態轉換、隊伍、勝負、計時、rule。
2. Application tests：start match、teleport、death、end match，使用 fake ports。
3. Storage tests：config migration、舊資料讀取。
4. Platform smoke tests：真 Paper 1.21.11 開服。
5. 1.16.5 baseline comparison：用同一份操作清單對照升級前後的實際功能行為。
6. Manual checklist：GUI、scenario、Discord、optional integrations。

1.16.5 原版功能對照檢核：

1. 以升級前 `main` / Paper `1.16.5` 可運作版本建立功能 baseline，不以記憶或程式碼推測代替實測紀錄。
2. 對同一份 checklist 分別在 Paper `1.16.5` 與 Step 21 完成後的 Paper `1.21.11` 執行，至少覆蓋：開服啟動、主要 command、host 設定、玩家加入、登入 gate（白名單、等待 host、滿員、遊戲中加入、bypass 權限）、隊伍、scenario、倒數、傳送、開局、死亡、觀戰、復活、掉落、經驗、邊界、世界生成、勝負判斷、結束流程、GUI、scoreboard、optional integrations。
3. 每個項目必須分類為「一致」、「可接受差異」、「版本限制造成的差異」、「升級造成 regression」、「原版既有問題」。
4. 所有「升級造成 regression」必須修正，或取得明確接受紀錄後才能進入發布步驟。
5. 對 Step 5 到 Step 21 曾改動或降級的 legacy/platform/integration 行為要特別標註，例如 absorption、armor points、pickup exp control、death animation、custom exp orb、fast block set、large chest merge、WorldBorder、Packet、custom-ore-generator、Foundation / DatouNMS 移除、登入 gate、team color。1.7 舊附魔模擬要標成「委託人已接受正式移除」，不要列為 regression。

完成條件：

- core/application 不需要啟動 Bukkit 就能測。
- Paper smoke test 可驗證插件啟動與最小遊戲流程。
- Step 21 移除 legacy 層後，1.16.5 / 1.21.11 功能對照檢核已完成，所有 regression 都已修正或被明確接受。
- A15 搜尋檢查、A16 最終驗收清單與發布前手動 checklist 都已通過。

## 23. 發布與文件

最後更新使用者文件與開發文件。此步驟必須在 Step 21 的最終依賴狀態與 Step 22 的最終驗收結果確定後才做，避免發布文件描述和實際 runtime dependency 不一致。

優先修改：

- `Update-WonderlandUHC/README.md`
- `DEVELOPMENT.md`
- `Update-WonderlandUHC/.github/workflows/build-and-publish-release.yml`
- `scripts/*.sh`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`

要做的事：

1. README 改成目前支援 1.21.11，並清楚標記最終 legacy 支援策略。
2. 文件說明核心功能與 optional feature。
3. 列出舊功能的保留方式、替代插件、未完成項與驗證結果；任何移除、降級或選配化都必須有明確接受理由。
4. 開發文件加入架構邊界規則：core 不可 import Bukkit/Foundation/NMS。
5. CI 改 Java 21 與新版 Gradle。
6. 發布前確認 Step 21 相容層移除與 Step 22 最終新舊版對照驗收都已通過。

完成條件：

- 新開發者知道該改哪一層。
- 使用者知道升級前需要備份與哪些功能可能變成選配。
- 發布文件與最終 runtime dependency 狀態一致。

## 建議實作順序總表

0. 建立測試與 1.16.5 基線。
1. 拆 `WonderlandUHC.java`，建立 bootstrap 與 feature registry。
2. 定義 core/application/port/platform/legacy package 邊界。
3. 把世界、玩家、scheduler、scoreboard、storage、event 發布包成 port。
4. 升級 Java 21、Gradle、Paper 1.21.11 API，先取得可收斂的編譯錯誤清單。
5. 將 Foundation 降級為 legacy adapter，不再讓新程式碼直接依賴。
6. 將 DatouNMS 移到 legacy adapter 或移除。
7. 將 WorldBorder、PacketListenerAPI、DiscordSRV 改成 optional integration；舊 custom-ore-generator 整合移除，功能暫停到升級完成後重新設計。
8. 重整設定與持久化邊界。
9. 重做世界/邊界/傳送服務，Paper API 實作放在 platform adapter。
10. 重整狀態機與 use cases。
11. 修復核心主持設定入口，解除 Step 9 / Step 10 的 GUI 設定阻塞。
12. 修復 hotbar / spectator 工具，回測 Step 9 的 center 與 Nether blocked 項目。
13. 修復 Discord 公告 GUI / Conversation 正式入口，移除臨時測試指令。
14. Scenario 盤點與高風險相容層隔離。
15. Scenario 規則模組化與逐項行為補齊。
16. Scenario 混合互動驗收。
17. 核心 presentation 入口瘦身。
18. Scoreboard / practice / 非核心 presentation 收斂。
19. Bukkit/Paper API 與剩餘 legacy adapter 清理。
20. IDE 小型警告整理。
21. 在最終驗收前移除 Legacy Foundation / NMS 相容層。
22. 補測試與 1.16.5 / 1.21.11 最終功能對照檢核。
23. 發布與文件更新。

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
- `scripts/package-plugin-1.21.sh`
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

這批檔案目前直接或間接使用 DatouNMS，升到 `1.21.11` 時風險最高。目標是讓搜尋結果只剩 `legacy/LegacyDatouNmsAdapter`，或完全移除。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/RespawnCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/player/RolePlayerApplier.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/spectator/RoleSpectatorApplier.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/player/role/staff/RoleStaffApplier.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/death/PlayingDeathListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/OldEnchantListener.java`（1.7 舊附魔功能已接受移除；此檔為清理/解除註冊候選，不修復）
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

Step 7 開始前，專案使用 `PacketListenerAPI_v3.7.9-SNAPSHOT.jar`；這不是 1.21.11 可靠基礎。掃描後確認 packet 用途集中在音效控制與 `ArmorVsHealth` scenario，不是 UHC 開局、隊伍、死亡、勝負的核心條件。Step 7 預設方向是移除舊 Packet 依賴並優先使用 Paper/Bukkit 原生 API；PacketEvents 只在後續確認有原生 API 做不到、且必須保留的需求時才作 optional adapter。

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
2. 不預設建立 `PacketPort`、`NoopPacketPort` 或 PacketEvents adapter；先用 Paper/Bukkit 原生事件與 Attribute API 收斂現有功能。
3. `SoundController` 先不要直接移植。`formatSoundStringFromPacket()` 目前回傳空字串，表示 hit/step sound 判斷很可能不完整；升級時應先把「大廳階段抑制攻擊/腳步聲」標成 optional packet feature backlog，後續若原生 API 無法等價處理，再用最小 PacketEvents adapter 重寫。
4. `ScenarioArmorVsHealth` 優先改成 Paper/Bukkit equipment/attribute 事件驅動；若真的仍需攔截 attribute packet，必須先重新確認該 scenario 是必留需求，再設計最小 optional adapter。
5. 若後續採用 PacketEvents，預設用 `compileOnly` 或外部插件方式；不要在未確認 GPL-3.0 散布影響前直接 shade 進 WonderlandUHC。
6. 最終 `rg "org\\.inventivetalent\\.packetlistener|PacketPlayOut|PacketPlayIn" Update-WonderlandUHC/src/main/java` 應為 0；若後續新增 optional packet adapter，只能出現在 `integration/packet/`。

第二片實作紀錄：

- 舊 `PacketRegister` / `SoundController` / `PacketListenerAPI_v3.7.9-SNAPSHOT.jar` 已從主線移除，`plugin.yml` 也不再宣告 `PacketListenerApi` softdepend。
- `ScenarioArmorVsHealth` 先改用 Bukkit inventory / interact / item break 事件觸發既有 health update，不再攔 `PacketPlayOutUpdateAttributes`。
- 死亡玩家 / 旁觀者攻擊限制先由 `DamageListener` 的 Bukkit damage gate 承接。
- 大廳階段抑制攻擊/腳步聲尚未等價補回，保留為 optional packet feature backlog；不得把這項視為永久刪除。

### A5. 自訂礦物與 Populator

自訂礦物在本輪 1.21.11 升級暫停。正式版 Custom Ore Generator 已實測在 Paper `1.21.11` 拒絕載入；Jenkins dev build `dev-110-SNAPSHOT` 可 enable，但尚未驗證 ore config、world config、舊 API 等價性或實際新 chunk 生成。舊 `populators.yml` 使用 0-64 高度與只替換 `STONE` 的舊世界模型，不適合直接遷移。Step 7 要移除舊 COG 程式碼、jar、softdepend、世界初始化套用流程與 generator 選單；升級完成後若仍需要自訂礦物，另開新版設計，不背舊 COG API。

- `Update-WonderlandUHC/libs/custom-ore-generator-2022.06.11.jar`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/hook/CustomOreGeneratorHook.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/listener/WorldInitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/populator/OreGen.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/populator/Populator.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/GeneratorSettingsMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/host/MainSettingsMenu.java`
- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/resources/populators.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`

處理方向：

1. 移除 `custom-ore-generator-2022.06.11.jar` 與 `build.gradle` 的舊 COG compileOnly。
2. `plugin.yml` 移除 `custom-ore-generator` softdepend，`Dependency.java` 與啟動檢查不再追蹤 COG 狀態。
3. 刪除 `CustomOreGeneratorHook`、`WorldInitListener`、`OreGen`、`Populator` 與 generator 選單入口。
4. 世界建立完成訊息不再顯示舊 generator / populator 名稱。
5. `populators.yml` 不再作為插件資源抽出；既有伺服器端舊檔可保留作歷史資料，但本輪升級不讀取、不套用。
6. `WorldUtils.getBlockEXP()`、礦物掉落、exp 與 scenario 若有礦物假設，仍要在後續 1.21.11 材料收斂時檢查；這不是舊 COG adapter。
9. 只有在已確認沒有可接受插件替代時，才討論內建 UHC 專用 ore rule service；這是最後手段，不是預設規劃。

### A6. WorldBorder 與 Chunk Fill

核心邊界可優先改用 Bukkit/Paper `WorldBorder`，但預生成 / fill / progress / 完成事件要先尋找新版本插件替代。外部 WorldBorder 插件的主要價值在舊式 fill 工作流，不是比賽邊界本身；這部分不能因原生 API 不完整就刪除。舊版 WonderlandUHC 沒有使用 trim，所以 trim 不列為 Step 7 核心替代需求。Paper 1.21.11 的 `WorldBorder` API 已提供 center、size、warning、damage、inside 判斷與 `changeSize`；舊的 `setSize(double, long)` 與 `setWarningTime(int)` 都已被標記 deprecated，升級時要避免把新程式寫在即將移除的 API 上。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/Dependency.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/ChunkPregenerationService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/worldborder/LegacyWorldBorderFillListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/worldborder/LegacyWorldBorderPregenerationAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ChunkPregenerationPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/practice/SimplePractice.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/BorderUtil.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/ChunkFiller.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/GenerateUtil.java`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `scripts/deploy-to-windows-server.sh`

處理方向：

1. 建立 `BorderService` 負責邊界設定、清除、收縮與查詢；它只依賴 `WorldBorderPort`，不直接 import Bukkit/Paper 或外部插件 API。
2. 建立 `WorldBorderPort` 與 `PaperWorldBorderAdapter`；`World#getWorldBorder()`、`setCenter`、`setSize`、`changeSize` 只能放在 `platform/paper` 實作。
3. 預生成先建立 `ChunkPregenerationPort` / `ChunkPregenerationService`，把 `wb fill` 啟動與 `WorldBorderFillFinishedEvent` 完成回呼集中到 `integration/worldborder` legacy adapter；行為先保持與舊版一致，不在同一切片測試或切換新替代插件。
4. 預生成替代方案已優先實測 `Chunky-Bukkit-1.4.40`，可作為升級替代候選。下一個程式切片應建立 `ChunkyPregenerationAdapter`，用 Chunky API 取代舊 `wb fill` 與 `WorldBorderFillFinishedEvent`，並把完成 callback 接回 `LoadingStatus.DONE`、cache save、restart handoff。Chunky 的 trim 能力只視為額外維護工具，不接入 WonderlandUHC 自動流程。
5. `WorldBorder [Renew] 1.18.X - 1.21.X` 保留為備援候選。因 Chunky 已通過 1.21.11 server-side qualification，除非 Chunky adapter 實作時發現無法覆蓋舊流程，否則不先升格 WorldBorder Renew 為部署依賴。
6. `BorderUtil.setWBBorder()`、`removeWBBorder()`、`ChunkFiller.fill()` 與 legacy WorldBorder listener 不得在替代方案確認前直接刪除；遷移後的行為必須覆蓋舊版既有功能。
7. `WorldBorderFillFinishedEvent` 不能留在核心 listener；若替代插件有事件或 API，放在 `integration/` adapter，若只有 command/progress，必須有明確輪詢或完成偵測策略。
8. 基岩牆不是原生邊界 API 的一部分，但 `Bedrock_Border_Height` 舊設定必須保留。預設 `0` 時不生成；若設定大於 `0`，必須在 fill 完成後產生等價基岩牆。DatouNMS fast block 不可用時要補 Paper/Bukkit fallback，不能把啟用後的舊行為標成 unsupported 或默默移除。
9. `checkWorldBorderVer()` 與 `Dependency.WORLD_BORDER.check()` 刪除或改成新依賴驗證，不可阻止主插件用錯誤訊息以外的方式硬崩。
10. smoke test 必須覆蓋選定替代方案：插件啟動、設定邊界、預生成、進度/完成偵測、開始收縮、停止遊戲都可運作。

第三片盤點決策紀錄：

- 現況耦合分為三類：`BorderUtil` 混合 Paper 原生 border API 與外部 `wb set/clear/wshape` 指令；`ChunkFiller` 只透過 `wb fill` 啟動預生成；`WorldFillListener` 依賴 `WorldBorderFillFinishedEvent` 來串接完成 log、基岩牆、Nether 預生成、`LoadingStatus.DONE`、cache save 與 server restart。
- Paper 原生 `WorldBorder` 只承接一般邊界能力，不承接預生成；下一片只抽 `BorderService` / `WorldBorderPort`，不得順手刪掉 fill listener 或把 `ChunkFiller` 改成 noop。
- 預生成首選候選為 `Chunky`，因為它有 1.21.11 相容版本、progress/continue/cancel/worldborder 指令與較明確的預生成定位；完成偵測要優先實測 API callback。
- `WorldBorder [Renew]` 候選價值是保留舊 `wb fill` 語意，但要先實測 Paper 1.21.11、完成事件/API 與資料安全；未通過前不應直接升格為必要部署依賴。

第四片實作紀錄：

- 已新增 `WorldBorderPort`、`PaperWorldBorderAdapter` 與薄 `BorderService`；`BorderUtil` 與 `SimplePractice` 不再直接呼叫 `World#getWorldBorder()`。
- 一般 Paper 原生邊界設定、警告時間、警告距離、收縮與尺寸查詢改走 port/service；新的收縮路線使用 `changeSize`，警告時間使用 `setWarningTimeTicks`。
- 外部 `wb set/wshape/clear` 指令暫時保留為 legacy WorldBorder plugin bridge，因為現有 `ChunkFiller.fill()` 仍透過 `wb fill` 預生成並依賴外掛邊界狀態。這些指令不得被視為新的一般邊界 API；後續 `ChunkPregenerationService`/adapter 確認後再移出或刪除。

第五片實作紀錄：

- 已新增 `ChunkPregenerationPort` 與 `ChunkPregenerationService`，把開始預生成、完成後 log、基岩牆、Nether 預生成、`LoadingStatus.DONE`、cache save 與 restart handoff 集中到預生成服務。
- 歷史紀錄：當時曾先把舊 WorldBorder plugin 的 `wb fill` 與 `WorldBorderFillFinishedEvent` 集中到 `integration/worldborder` legacy adapter，作為 Chunky 替代方案完成前的過渡。Step 21 已正式刪除這個 fallback。

第六片實測紀錄：

- 已在 Paper `1.21.11-130` 測試服安裝 `Chunky-Bukkit-1.4.40`，並暫時停用舊 `WorldBorder.jar` 為 `WorldBorder.jar.disabled`。啟動結果只載入 `Chunky` 與 `WonderlandUHC`，WonderlandUHC 正常 enable。Step 21 後 dependency report 不再追蹤舊 WorldBorder plugin。
- Chunky command flow 實測通過：`chunky world`、`chunky shape square`、`chunky center`、`chunky radius`、`chunky selection`、`chunky start`、`chunky progress`、`chunky pause`、`chunky continue`、`chunky cancel` + `chunky confirm` 都可由 console 操作。`progress` 會回報 processed chunks、百分比、ETA、rate 與 current coordinate。
- Paper native worldborder sync 實測通過：在 `uhc_practice` 執行 `worldborder center 0 0` 與 `worldborder set 128` 後，`chunky worldborder` 會把 Chunky selection 設為 center `0.5, 0.5`、radius `64`。
- `chunky trim` 實測可用，但舊版 WonderlandUHC 沒有使用 trim，所以不列為升級替代需求，也不接入自動預生成流程；最多保留為人工維護時可用的外部工具能力。
- 小範圍自然完成實測通過：`uhc_practice` radius `64` 預生成可完成，log 顯示 `Task finished ... Processed: 81 chunks (100.00%)`。此外 jar 內有 `org.popcraft.chunky.api.ChunkyAPI`，提供 `startTask`、`pauseTask`、`continueTask`、`cancelTask`、`isRunning`、`onGenerationProgress`、`onGenerationComplete`，並有 `GenerationProgressEvent` / `GenerationCompleteEvent`。下一片 adapter 應優先用 API callback，而不是只解析 console log。
- 合格判斷：Chunky 可作為 1.21.11 預生成 / progress / pause-continue-cancel 的升級替代候選。仍需在下一片程式實作中證明 overworld -> nether 順序、完成 callback 接 `LoadingStatus.DONE`、cache save、restart handoff，以及缺少 Chunky 時的明確錯誤訊息。

第七片實作紀錄：

- 已新增 `integration/chunky/ChunkyPregenerationAdapter`，透過 Chunky API `startTask` 啟動 square / region 預生成，並用 `GenerationCompleteEvent` 接回 `ChunkPregenerationService` 的既有完成流程。
- `ChunkFiller` 改由 `ChunkPregenerationAdapters` 選擇實作：優先使用 `Chunky`；若 `Chunky` 不存在，預生成會給出明確錯誤，不會默默跳過。Step 21 已移除舊 `WorldBorder` plugin fallback。
- `plugin.yml` 已把 `Chunky` 列為 `softdepend`，`Dependency` / 啟動 dependency report 也會顯示 `Chunky` 狀態。Chunky adapter 採 `compileOnly` 依賴，不把 Chunky class shade 進 WonderlandUHC jar。
- Paper `1.21.11-130`、port `25567` 實測：`Chunky-Bukkit-1.4.40` 啟用、舊 `WorldBorder.jar` 停用時，`uhc_world` 可完成小範圍預生成，完成後接回 `LoadingStatus.DONE`、cache save、border handoff 與 `restartServer()`。測試服因 Windows start.bat 沒有 `start.sh`，重啟 handoff 會以 `Startup script './start.sh' does not exist! Stopping server.` 收尾，這是測試服環境限制，不是插件啟動失敗。
- 實測發現 Chunky 的 `GenerationCompleteEvent` 可能早於最後一筆 `complete=true` progress 狀態被 adapter 觀測到，因此完成判斷以 complete event 為準；不能再要求最後一筆 progress 必須先標為 complete。
- 本片尚未處理 `Bedrock_Border_Height > 0` 的 Paper/Bukkit fallback；基岩牆仍是 Step 7 後續必補項，不能因 Chunky adapter 完成而視為整個 WorldBorder / 預生成遷移完成。

第八片實作紀錄：

- `BorderUtil.setBorders()` 與 `removeWBBorder()` 不再 dispatch `wb set`、`wb wshape` 或 `wb clear`；一般邊界設定與清除只走 Paper/Bukkit `WorldBorder` adapter。
- `ChunkPregenerationPort.startSquarePregeneration()` 明確接收預生成目標 radius，沿用舊 `getRadius(size) + 1` 外擴語意，讓 Chunky adapter 不需要依賴一般邊界流程裡的舊 `wb` 指令副作用。
- Step 21 已移除舊 WorldBorder plugin 的 `wb set/wshape/fill/fill confirm` fallback；`rg "dispatchCommand\\(.*wb"` 應為 0。
- Paper `1.21.11-130`、port `25567` 實測：在 `Loading_Status: GENERATING`、`Initial_Border: 128`、`Force_Loading_Nether_Chunk: false` 下，Chunky 仍可啟動並完成 `uhc_world` 預生成，log 顯示 `Task finished for uhc_world. Processed: 121 chunks (100.00%)`，後續進入 `Border Generated!`、`restartServer()`，`cache.db` 回到 `Loading_Status: DONE`。

第九片實作紀錄：

- `LegacyDatouNmsAdapter.setBlockFast(...)` 已提供 DatouNMS 不可用時的 Bukkit `Block#setType(...)` fallback；本片沒有新增第二套方塊放置抽象，而是修正 `BorderUtil.generateBorder()` 讓這個 fallback 能在基岩牆流程中穩定使用。
- `BorderUtil.generateBorder()` 不再每次呼叫都重設 `preBlocksPlaced` / `preBlocksNumber`，因此 `Bedrock_Border_Height > 1` 時，第一層完成後的後續呼叫會沿用既有座標向上加高，符合舊版基岩牆高度設定意圖。
- `preBorderBlocks` 改為依 world 保存第一層座標，避免 overworld / nether 都產生基岩牆時共用同一份座標清單。
- Paper `1.21.11-130`、port `25567` 實測：DatouNMS 不支援、`Bedrock_Border_Height: 2`、`Loading_Status: GENERATING`、`Initial_Border: 128`、`Force_Loading_Nether_Chunk: false` 下，Chunky 完成 `uhc_world` 預生成後進入 `Generating Border...` / `Border Generated!`，server 保存 `uhc_world` block chunks，`cache.db` 回到 `Loading_Status: DONE`。測試後已將測試服本地 `Bedrock_Border_Height` 還原為 `0`。

### A7. Bukkit/Paper API 風險檔案

這批檔案包含舊 API、deprecated API 或跨版本風險。升級依賴後要優先編譯並集中修到 `platform/paper/` 或 `legacy/`。

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
- `lib-foundation/src/main/java/org/mineacademy/fo/model/FoundationEnchantmentListener.java`（1.7 舊附魔功能已接受移除；只作清理候選，不作新版修復）
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
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/UHCCommandGroup.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/BackPackCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/PracticeCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/SendCoordsCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/game/SpecToggleCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/BorderCommand.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/command/impl/host/GiveAllCommand.java`
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

Presentation 層要變薄，但不能把已能運作的 thin menu / command 為了抽象化而重寫。Step 18 先用驗收清單找出 Paper `1.21.11` 實際失效點；Step 21 legacy 移除再集中處理仍可運作但尚未移除的 Foundation menu / scoreboard 依賴。

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/ButtonLocalization.java`
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

1. 先驗收 menu / scoreboard / tools 在 Paper `1.21.11` 的實際行為，只有壞掉或有明確升級風險時才局部修正。
2. 已能運作的 menu 不強制改讀 view model；已能運作的 menu action 不強制改呼叫 use case。
3. Scoreboard 只在 placeholder、更新排程或 staff metric 實測失效時建立最小 helper / adapter，不預防性重寫 projection。
4. Foundation menu 仍可暫時保留；Step 18 只修會阻塞非核心 presentation 驗收的相容問題，其餘列入 Step 21 legacy 移除。
5. `RuntimeUtil.getTPS()` 已先補 Paper `getTPS()` / fallback stopgap，避免 Paper `1.21.11` 因 TPS placeholder 持續噴 `ClassNotFoundException` / NPE；Step 18 只有在 staff scoreboard 驗收仍有異常時，才把使用點收成小型 scoreboard metric helper。

### A12. 設定、語言、資料與資源檔

這批檔案需要做完整 config migration、備份與 1.21.11 名稱更新。Step 8 只先處理最小持久化邊界與 `scenarios.yml` 的已知 material alias；本附錄保留未來全量資源 migration 的提醒。

- `Update-WonderlandUHC/src/main/resources/biomes.yml`
- `Update-WonderlandUHC/src/main/resources/broadcasts.yml`
- `Update-WonderlandUHC/src/main/resources/cache.db`
- `Update-WonderlandUHC/src/main/resources/commands.yml`
- `Update-WonderlandUHC/src/main/resources/gamecache.db`（已確認舊版與新版皆未被程式引用，已移除，未來不得補 migration）
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/items.yml`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/permissions.txt`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `Update-WonderlandUHC/src/main/resources/savedgames.db`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/sounds.yml`
- `Update-WonderlandUHC/src/main/resources/spawns.yml`
- `Update-WonderlandUHC/src/main/resources/stats.yml`

處理方向：

1. 先備份舊設定，再跑完整 migration；這是後續 migration 步驟，不是 Step 8 完成條件。
2. material、sound、biome、entity、potion、enchantment 名稱全部檢查；1.7 舊附魔模擬本身已接受移除，但設定中可能仍有一般 enchantment key，需要跟物品/GUI 設定一起檢查。
3. 舊 DB 形態若只是預設資源，確認是否仍應打包進 jar；只有會影響切階段、重啟接續、主持 preset 或當局 stats 的資料需要保留。
4. `gui.yml`、`items.yml`、`sounds.yml`、`biomes.yml` 的 alias 寫回與 menu button material 更新，阻擋核心主持入口的部分先在 Step 11 處理，核心 menu resource 收斂在 Step 17；Step 18 只處理驗收中實際造成非核心 presentation 失效的 resource 問題，其餘全量 migration 留後續步驟。
5. `plugin.yml` 的 softdepend 與 commands 要和實際 feature registry 對齊。

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
- `lib-foundation/src/main/java/org/mineacademy/fo/plugin/EnchantmentPacketListener.java`（1.7 舊附魔功能已接受移除；只作清理候選，不作新版修復）
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
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/StartMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/HandleDeathUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/match/EndMatchUseCase.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/WorldLifecycleService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/BorderService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/ChunkPregenerationService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/TeleportService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/world/CenterValidationService.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/rule/RuleEngine.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldPort.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/WorldBorderPort.java`
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
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/legacy/LegacyDatouNmsAdapter.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/discord/DiscordVoiceIntegration.java`
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
rg -n "de\\.derfrzocker|custom-ore|CustomOre|OreGenerator" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/src/main/resources Update-WonderlandUHC/build.gradle
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
- `org.inventivetalent.*`、`de.derfrzocker.*`、`com.wimbli.WorldBorder.*` 不能出現在 core、application、listener、menu、scenario；`de.derfrzocker.*` 在本輪 COG 移除後應完全消失。
- Bukkit/Paper `WorldBorder` API 只能存在於 `platform/paper` 的 `WorldBorderPort` 實作，不應出現在 application `BorderService`，也不應再和外部 `wb` 指令混在同一個 util。

### A16. 最終驗收清單

完成所有修改後，要用這份清單判定是否真的升級完成。

- `Update-WonderlandUHC` 可用 Java 21 執行 `compileJava`。
- `Update-WonderlandUHC` 可用 Java 21 執行 `compileTestJava`。
- `Update-WonderlandUHC` 可產出 shadow jar。
- `Update-WonderlandUHC` 不再依賴或 shade `lib-foundation`，也不再 import `org.mineacademy.fo.*`。
- `Update-WonderlandUHC` 不再依賴 DatouNMS、`NewerSpigotAPI` 或 legacy NMS wrapper；任何必要低階平台行為都已改為 Paper/Bukkit API 或正式 `platform/paper/` adapter。
- 空 Paper `1.21.11` server 放入 jar 後可啟動，不因選配依賴缺失而停服。
- `/uhc` 或主要 command 可註冊。
- 可建立或載入 UHC 世界，並完成與舊版等價的預生成流程。
- 可設定 host、隊伍、邊界、scenario。
- 主持工具指令可完成舊版等價操作，包含 choose/start/stop/regen/tp/edit、border、whitelist、staff、respawn、giveall。
- 可從 waiting 進入 teleporting、pre-start、playing。
- 玩家死亡後可進入正確 spectator/staff/player 狀態。
- 最小勝負判斷與結束流程可跑完。
- 生成完成狀態、host 與 UHC 設定在重啟後仍可恢復，不因重啟回到未生成狀態。
- Practice mode 的指令、世界、邊界、物品、隨機傳送與死亡補裝已保留或差異已被明確接受。
- 舊版預設 scenario 都有逐項驗收；可以用新實作方式，但功能不可缺失。
- Paper `1.16.5` 原版與 Paper `1.21.11` 升級版已用同一份功能 checklist 對照；所有差異都有分類，所有升級造成 regression 都已修正或被明確接受。
- 缺少 DiscordSRV、PacketListenerAPI/PacketEvents 時，只停用對應整合；若安裝 DiscordSRV 並啟用設定，舊版等價功能必須完整可用。自訂礦物 / COG 功能是本輪升級明確接受的暫停項，升級完成後另行重設計。
- WorldBorder / 預生成若改採新替代插件，缺少該插件時必須明確阻止相關流程並提示部署需求，不能默默降級或刪功能。
- MySQL / 長期資料庫 stats 路線已移除；YAML / 本機 stats 可正確紀錄當局資料，`/stats`、`/topkills` 與相關顯示入口可用。
- 完整 config migration 會備份舊檔並產生可讀的新設定；Step 8 的最小持久化邊界不取代這個最終驗收項。
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
- Ore Control Spigot：https://www.spigotmc.org/resources/ore-control-%E3%80%8E1-18-1-21-4%E3%80%8F.63621/
- DevWorldGen Modrinth：https://modrinth.com/plugin/devworldgen
- WorldBorder Spigot：https://www.spigotmc.org/resources/worldborder.60905/
- WorldBorder Renew 1.18-1.21：https://www.spigotmc.org/resources/worldborder-renew-1-18-x-1-21-x.126588/
- Chunky Bukkit：https://www.curseforge.com/minecraft/bukkit-plugins/chunky-pregenerator
