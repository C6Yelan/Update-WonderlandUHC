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
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/port/ScoreboardPort.java`：Step 18 先驗收 scoreboard presentation 與 Paper `1.21.11` 相容性；只有實測需要時才建立最小 scoreboard helper / adapter，完整 port 化留到 Step 21 legacy 移除時判斷，Step 26 再做最終驗收。

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

- 歷史紀錄：`Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/impl/PlayersMenu.java` 當時仍透過 Foundation `ItemCreator.of(CompMaterial)` 建立 player head；Step 21 已完成 legacy menu / item 依賴移除。
- 歷史紀錄：`Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/menu/ButtonLocalization.java` 當時被 Foundation `ButtonReturnBack.setMaterial(CompMaterial)` 簽名綁住；Step 21 已移除 Foundation menu 全域設定路徑。
- 歷史紀錄：`Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java` 當時同時被 Foundation `YamlConfig#getMaterial` 與 `ItemCreator.of(CompMaterial, ...)` 綁住；Step 21 已完成 Foundation config / item 依賴移除。

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
- `Update-WonderlandUHC/src/main/resources/cache.db`（0 bytes 空白預設檔已移除；runtime `plugins/WonderlandUHC/cache.db` 仍由 `WorldLoadingCacheStore` 保存 / 載入，不能刪除現場資料）
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`（仍是主要 runtime static settings 設定檔，不能刪除；2026-05-21 已移除未讀取的 `Serialization`、`Version`、`Locale`、`Team.Allow_Character_Color`、`Prefix`）
- `Update-WonderlandUHC/src/main/resources/stats.yml`（0 bytes 空白預設檔已移除；runtime `plugins/WonderlandUHC/stats.yml` 仍由 `StatsStorageYaml` 保存 / 載入，不能刪除現場資料）
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
- `src/main/resources/savedgames.db` 的 0 bytes 空白預設檔已移除；runtime `plugins/WonderlandUHC/savedgames.db` 仍保留 UUID -> settings list 形狀，第一次保存時由 `SavedGameSettingsStore` 建立，不當成 legacy 檔刪除。
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
- Slice 7 收尾清理已完成：舊版 CenterCleaner pass/fail 失敗訊息 Java 欄位與預設 key 已移除；舊演算法設定 Java 欄位已移除，但預設 `settings.yml` 欄位保留為 legacy 相容欄位；`biomes.yml` 已於後續 resource migration 確認無 runtime 引用後移除。
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
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`（仍是 runtime sidebar theme 設定檔，不能刪除；2026-05-21 僅修正預設檔內 placeholder 註解，不改實際顯示行）

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
- Foundation menu / scoreboard 依賴已完成分類；實際壞掉的項目已修正，未阻塞升級且可運作的項目能被 Step 21 legacy 移除明確追蹤，並在 Step 26 最終驗收。

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
10. `PlayerLoginEvent` 已完成 Paper `1.21.11` 替代流程研究，但不在 Step 19 直接重寫；它牽涉登入 gate、權限 bypass、白名單與 `UHCPlayer` 建立時機，需列入 Step 22 Login Gate Migration 專門處理，並在 Step 26 最終對照驗收。

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
5. deprecated API、Foundation / DatouNMS、`ChatColor / team color model`、metadata state 不在本步驟處理，維持 Step 19 的分類結果並交給 Step 21；`PlayerLoginEvent` 交給 Step 22 Login Gate Migration。
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

補充進度（2026-05-17）：手動測試 `/staff` 時發現 `items.yml` 的 `Spectator.Random_Teleport.Type: ENDER_PORTAL_FRAME` 未被本地 `PluginItems` alias 承接，導致 staff / spectator hotbar 初始化失敗；當時先補 `ENDER_PORTAL_FRAME -> END_PORTAL_FRAME`，不改 hotbar 工具架構。後續 resource cleanup 已改用 `END_PORTAL_FRAME` 並移除 `PluginItems` material alias map。

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

補充進度（2026-05-17）：21.6 Foundation `ChatUtil` enum display cleanup 已完成。`ScenarioName#capitalize()` 與 `BorderType#fancyName()` 不再使用 Foundation `ChatUtil`，改用既有本地 `PluginText.bountifyCapitalized(...)`；`ScenarioName#capitalize()` 會保留底線，避免改動 scenario YAML section key，`BorderType#fancyName()` 仍只輸出顯示名稱。這刀不處理 `PlayerLoginEvent`、`MetadataValue`、`UHCTeam` team color model 或 `WonderlandUHC extends SimplePlugin` lifecycle；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 Foundation `PlayerCollection` whitelist cleanup 已完成。`Game` 與 `WhitelistCommand` 不再 import Foundation `PlayerCollection`，改用本地薄 `platform.player.PlayerCollection` 保留白名單玩家名稱 / UUID add、remove、contains 語意；`WhitelistChecker` 與 `RespawnCommand` 繼續透過 `Game#getWhiteList()` 使用同一份資料。這刀不改 `/whitelist` 子指令語意、不重做白名單 service，也不處理 `PlayerLoginEvent` login gate、`MetadataValue` 或 team color model；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 login event wrapper shrink 已完成。`UHCLoginEvent` 移除未使用的 hostname / address / setResult / setKickMessage / allow pass-through，`LoginChecker` 不再 import `PlayerLoginEvent`；目前仍保留 Bukkit `PlayerLoginEvent` 的登入判斷時機、`UHCPlayer` 建立時機與既有 kick 語意，不把 login gate 改成 pre-login / join 後分段模型。這刀不改白名單、滿員、等待 host 或遊戲中加入規則，也不處理 `MetadataValue` 或 team color model；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 Foundation `CompColor` team random color cleanup 已完成。`UHCTeam#getRandomColor()` 不再使用 Foundation `CompColor.getChatColors()`，改用本地固定 16 色 Bukkit `ChatColor` 清單保留原本循序分配語意。這刀不改 `UHCTeam` public API、不改 `ColorPickerMenu`，也不把 team color model 遷移到 Adventure 或 DyeColor；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 deprecated `MetadataValue` vanished cleanup 已完成。`PluginPlayers#getByName(..., true)` 保留 vanished 過濾語意，但 `PluginPlayers#isVanished(...)` 不再讀取 `MetadataValue` 清單，改用 `Player#hasMetadata("vanished")` 判斷外部 vanish metadata 是否存在。這刀不移除 vanished gate、不新增 vanish service，也不改玩家查找的 exact / prefix matching；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 Foundation `StrictMap` scenario limitations cleanup 已完成。`ScenarioLimitations` 不再使用 Foundation `StrictMap`，改用標準 `Map` / `HashMap` 保存玩家已挖礦物數與礦物限制；`getOrPut` 改成 `computeIfAbsent`，`override` 改成 `put`。這刀不改 Limitations 規則、限制數值、canonical ore 判斷或錯誤保護流程；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 Foundation `CompAttribute` final heal cleanup 已完成。`FinalHealCountdown` 不再使用 Foundation `CompAttribute.GENERIC_MAX_HEALTH`，改用 Bukkit `Attribute.MAX_HEALTH` 取得最大血量，null 時維持 20.0 fallback。這刀不改 final heal 觸發、廣播、音效或玩家篩選規則，也不處理 `Extra` 中仍較廣的 `CompAttribute` / `CompProperty` helper；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 `Extra` attribute / entity property cleanup 已完成。`Extra` 不再使用 Foundation `CompAttribute` / `CompProperty`，最大血量讀寫改用 Bukkit `Attribute.MAX_HEALTH`，CombatRelog / SitFreeze 的 no-AI 與 silent 行為改用 `LivingEntity#setAI(false)` / `Entity#setSilent(true)`。這刀保留 `Extra.setMaxHealth(...)` 會同步設定目前血量的既有語意，不新增 attribute service 或 entity property wrapper；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 Foundation `CompMaterial` utility cleanup 已完成。`SimplePractice` 與 `BorderUtil` 不再使用 Foundation `CompMaterial`，固定方塊改用 Bukkit `Material`，log / leaves / air / long grass 判斷改走既有 `PluginMaterials`；無 production / test 呼叫點的舊礦脈工具 `GenerateUtil` 已刪除。這刀不重寫 practice 世界清理、timer border 基岩牆或 border cache 流程，也不新增 material wrapper；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.6 Foundation `SimpleReplacer` placeholder cleanup 已完成。`GamePlaceholderReplacer` 不再繼承 Foundation `SimpleReplacer`，改在本檔內保留原本 `List<String>` join、`String#replace`、split 回 list 的薄替換語意；`/config`、Discord broadcast formatting 與 saved settings lore 預覽仍使用同一個 replacer。這刀不改 placeholder 名稱、不改 boolean 顯示文字、不改 `PluginText` 全域替換規則，也不新增 placeholder framework；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-17）：21.7 最終 Foundation dependency 拔除已完成。`WonderlandUHC` 不再繼承 Foundation `SimplePlugin`，改由 Bukkit `JavaPlugin` 明確執行 load / enable / reload / disable lifecycle；`reload()` 保留原本重新載入設定、scenario、scoreboard 與 runtime task 的語意，並在 reload / disable 取消本插件 scheduler task。`build.gradle` 已移除 Foundation dependency、`org.mineacademy` relocate 與 JitPack Foundation repo；`settings.gradle` 不再 include `../lib-foundation`；`scripts/package-plugin.sh`、部署與清理腳本不再建置或引用 `lib-foundation` / `--skip-foundation` / compatibility alias；本地 `org.mineacademy.fo.model.SimpleReplacer` shim 與 `scripts/bootstrap-foundation-deps.sh` 已刪除。`rg` gate 確認 `src/main/java` / `build.gradle` 無 `org.mineacademy.fo`、`LegacyFoundationAdapter`、`lib-foundation`、DatouNMS / NMS / CraftBukkit 命中；`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現 `ERROR` / `Exception`。

補充進度（2026-05-17）：Step 21 final gate 盤點已回歸 IDE deprecated cleanup。Foundation / DatouNMS 搜尋 gate 仍為 0；目前 VS Code 可見 deprecated/legacy 風險主要剩 `PlayerLoginEvent` login gate、`org.bukkit.ChatColor` team/color model 與多處 native command `getCommand(...)` 提示，其中 `getCommand(...)` 先依委託人先前決策暫緩到後續 command 註冊策略。低風險 cleanup 第一刀先處理工具耐久與未使用舊 effect helper：`PlayerUtils#costPlayerToolDurability(...)` 不再依賴 deprecated `Material#getMaxDurability()`，改用 Paper item data component `MAX_DAMAGE` 判斷破裂；`Extra#playBlockBreakEffect(...)` 未有 production 使用點且仍使用舊 `playEffect(..., Effect.STEP_SOUND, ...)`，已刪除。這刀不改登入模型、不改 team color public API，也不新增 wrapper 或 suppress warning。封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。

補充進度（2026-05-18）：Step 21 `ChatColor / legacy color model` migration 已完成。新增本地薄 `PluginColor` enum 承接目前唯一需要的 16 色資料模型，提供 legacy 色碼輸出、`NamedTextColor`、羊毛材質與舊設定解析；`UHCTeam`、scoreboard heart color、`ColorPickerMenu`、team settings 與 scoreboard settings 不再 import `org.bukkit.ChatColor`。本刀保留 `red`、`&c`、`§c` 等既有 `Scoreboard_Settings.Heart_Color` 讀取相容，保留 team prefix / placeholder 的 legacy 色碼輸出，不處理 `LegacyComponentSerializer` 或訊息格式 migration；後者已移到 Step 23。`rg "org\\.bukkit\\.ChatColor|\\bChatColor\\b" src/main/java src/test/java` 為 0；封裝、Paper `1.21.11` startup、`uhc reload` 與 log gate 已通過。至此 Step 21 除明確轉交 Step 22 / Step 23 的項目外，Foundation / DatouNMS / NMS / `ChatColor` gate 均已清空。

Step 21 必須完成已知 Foundation / NMS legacy 移除、必要替代，或在本步驟內取得明確接受的行為差異。`PlayerLoginEvent` 登入 gate 已確認不是單純 legacy 拔除，而是涉及未來 Paper login API 與權限查詢策略的架構議題，正式移到 Step 22 專門處理。舊色碼 / message format boundary 交給 Step 23；若要完全移除 legacy config format，交給 Step 24。剩餘 runtime legacy facade / bridge 交給 Step 25；Step 26 只做最終對照檢查，除非檢查出 regression 才回到對應實作層修正。

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
6. 準備 README / DEVELOPMENT / 部署文件需要的最終依賴狀態，供 Step 27 發布文件使用。
7. 將 `PlayerLoginEvent` 明確轉交 Step 22 Login Gate Migration；Step 21 不得用 wrapper、`@SuppressWarnings` 或 join 後硬踢來掩蓋 deprecated warning。
8. 處理 Step 19 延後的 `ChatColor / legacy color model`、`MetadataValue / legacy state` 與 Foundation config API；不能新增短期 wrapper 或 suppress 當作完成。`LegacyComponentSerializer` 與舊訊息格式收斂不混入本步驟，交給 Step 23。

完成條件：

- `rg -n "org\\.mineacademy\\.fo|LegacyFoundationAdapter|lib-foundation" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle` 為 0。
- `rg -n "DaTouNMS|datounms|NewerSpigotAPI|net\\.minecraft|org\\.bukkit\\.craftbukkit|NMS|Nms|nms|breakBlockNms|newBlockPosition|getHandleEntity|ObjectCreator|playerInteractManager" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle` 為 0，除非是 Paper API 文件明確允許且封裝在 `platform/paper/` 的正式 adapter。
- `Update-WonderlandUHC` 可不依賴 `lib-foundation` repo 完成 clean build、shadow jar 與 Paper `1.21.11` server smoke test。
- Step 19 延後的 deprecated 項目已被移除、替換，或改成非 legacy 的正式 platform/service 實作；`PlayerLoginEvent` 例外轉交 Step 22 以正式 login gate migration 處理，舊色碼 / message format boundary 轉交 Step 23，完整 legacy config format 移除轉交 Step 24，剩餘 runtime legacy facade / bridge 轉交 Step 25。其他行為差異都必須在 Step 21 內完成決策與記錄，Step 26 只做最終對照驗證。
- A15 搜尋檢查與 A16 最終驗收清單都已更新為「Foundation / DatouNMS 完全移除」狀態。

## 22. Login Gate Migration：移除 `PlayerLoginEvent` 登入 gate 依賴

Step 21 已確認 `PlayerLoginEvent` 不是可用 wrapper 或 suppress 安全清掉的 deprecated warning。Paper `1.21.11` 建議的 `PlayerConnectionValidateLoginEvent` 發生在更早的連線驗證階段，只提供 connection / profile，不提供完整 Bukkit `Player`；而 WonderlandUHC 現有登入 gate 需要 `Player#hasPermission`、白名單名稱 / UUID 判斷、滿員 bypass、遊戲中重連與 `UHCPlayer` 角色狀態。為了讓專案完成後仍具備可維護性與後續版本更新能力，本步驟專門重新設計登入 gate，而不是把警告藏起來。

詳細盤點、LuckPerms 權限策略、子切片順序與驗收 gate 記錄於 `docs/step-22-login-gate-plan.md`。Step 22 預設採用 LuckPerms 作為 pre-login bypass 權限查詢來源，讓 login gate 不再依賴 Bukkit `Player`；若後續不接受 LuckPerms 作為正式部署依賴，必須先回到該文件調整無 LuckPerms 時的拒絕 / 降級行為，再開始實作。

目前進度（2026-05-18）：已確認採用 LuckPerms hard dependency、fail closed 權限查詢策略與獨立 `BYPASS_JOIN_FULL` 滿員 bypass。實作已導入 LuckPerms API、`plugin.yml` hard dependency、`LoginSubject` / `LoginPermissionService`、Paper `PlayerConnectionValidateLoginEvent` 入口、UUID / name based 白名單查詢，以及不建立新 `UHCPlayer` 的遊戲中登入判斷；`src/main/java` / `src/test/java` 已無 `PlayerLoginEvent` 命中。白名單、等待 host、滿員、遊戲中加入、既有參賽者重連與 LuckPerms bypass / explicit deny 真實玩家情境矩陣已通過；封裝、Paper `1.21.11` startup、`uhc reload` 與最終 log gate 已通過。

本步驟不得做的事：

1. 不用 `@SuppressWarnings("deprecation")` 當作完成。
2. 不建立只包住 `PlayerLoginEvent` 的 wrapper 來隱藏 deprecated API。
3. 不把原本 login gate 改成玩家 join 後才 kick，除非該差異被明確驗收並接受；預設這不是等價行為。
4. 不在 Step 22 混入 `ChatColor` / team color model migration。

優先修改或新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/LoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/UHCLoginEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/checker/LoginChecker.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/checker/WhitelistChecker.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/preparing/PreparingLoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/starting/StartingLoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/playing/listener/PlayingLoginListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/player/PlayerCollection.java`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/application/login/`
- 建議新增 `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/integration/luckperms/`（Step 22 預設採用 LuckPerms）
- `Update-WonderlandUHC/build.gradle`
- `Update-WonderlandUHC/src/main/resources/plugin.yml`

要做的事：

1. 採用 LuckPerms 作為 pre-login bypass 權限查詢來源；第一個程式碼切片前需確認 LuckPerms 是 hard dependency 或 soft dependency，但預設以 hard dependency 讓缺少 LuckPerms 時明確失敗，避免默默放行或降級。
2. 建立不依賴 Bukkit `Player` 的 login subject，例如 UUID、玩家名稱、profile、連線來源、目前 game state、是否為既有 UHCPlayer / rejoin candidate。
3. 將白名單判斷改成 UUID / name based，不再依賴 `PlayerCollection.contains(Player)`。
4. 將 bypass 權限判斷從 `Player#hasPermission(...)` 改成可在 pre-login 階段使用的 permission service；若採 LuckPerms，必須處理 user load、快取、失敗 fallback 與伺服器未安裝時的啟動策略。
5. 將等待 host、生成中、滿員、遊戲中加入、遊戲中重連等檢查改成 login subject / game state 判斷；不能在檢查時建立新的 `UHCPlayer` 或依賴未完成初始化的 Bukkit `Player`。
6. 滿員邏輯需評估使用 Paper `PlayerServerFullCheckEvent` 或併入新的 login gate；bypass 行為必須有測試覆蓋。
7. 將事件入口改為 Paper `PlayerConnectionValidateLoginEvent`；若有 join 後才能做的非拒絕行為，拆到 `PlayerJoinEvent`，但不把拒絕流程延後成 join 後 kick。
8. 保留既有 kick 訊息內容與顏色格式，轉成 Adventure `Component` 時不得改變玩家可見文字。
9. 更新文件與測試清單，明確列出有 / 無 LuckPerms、白名單、等待 host、滿員、遊戲中加入、bypass 權限與重連情境的預期行為。

完成條件：

- `src/main/java` 不再 import 或監聽 `org.bukkit.event.player.PlayerLoginEvent`。
- `PlayerLoginEvent` warning 不靠 suppress / wrapper 消失，而是由正式 login gate migration 移除。
- 白名單、等待 host、滿員、遊戲中加入與 bypass 權限行為都有明確測試或手動驗收紀錄。
- 若引入 LuckPerms，`plugin.yml`、`build.gradle`、README / DEVELOPMENT 的依賴策略與 runtime 行為一致。
- 程式碼變更後依專案規則完成封裝、Paper `1.21.11` startup、`/uhc reload` 與 login gate 情境測試。

## 23. Message Format Migration：收斂舊色碼與訊息格式相容層

Step 21 只處理 `ChatColor` 資料模型與 IDE warning，不把 `LegacyComponentSerializer` 當作 deprecated API 一次清掉。`LegacyComponentSerializer` 是 Adventure 官方提供的舊色碼相容工具，適合暫時留在 config/message 邊界，但不應長期散落在 command、menu、scoreboard、event 與 game logic 中。本步驟專門把訊息格式收斂成可維護的邊界，並決定是否導入 MiniMessage 或只保留受控的 legacy 讀取相容。

補充進度（2026-05-18）：Step 23 初步盤點已完成，詳見 `docs/step-23-message-format-inventory.md`。目前 `LegacyComponentSerializer` 有 49 個命中，分布在 22 個 Java 檔案；`PluginText.colorize(...)` 有 12 個直接呼叫。resources 仍大量使用 legacy `&` 色碼與 `{placeholder}`，因此第一階段建議維持 legacy-compatible config，先把 Adventure `Component` 轉換集中到 `PluginText` / text boundary，不導入 MiniMessage-only，也不重寫 command、menu、scoreboard 或 login gate 流程。

補充進度（2026-05-18）：Step 23 第一個程式碼切片已完成。`PluginText` 新增基礎 Adventure `Component` / legacy string 轉換方法；`Chat` 與 `PluginPlayers` 不再各自持有 `LegacyComponentSerializer` 或重複 `&` 轉色邏輯，改委派 `PluginText`。本刀保留 `Chat#sendConversing(...)` raw legacy string、`PluginPlayers#kick(...)` 延遲排程、多行 kick 與 action bar 行為，不處理 menu / item / join / quit / login / MOTD、clickable command、golden head 或 scoreboard。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。

補充進度（2026-05-18）：Step 23 第二個程式碼切片已完成。`PluginMenu`、`UHCLoginEvent`、`LoginListener`、`UHCJoinEvent`、`MotdListener`、`LobbyQuitListener` 與 `RolePlayerEvents` 的 presentation message 轉換已改走 `PluginText`，不新增 formatter / registry，也不改 login、join、quit、menu 或 combat relog 流程。`UHCJoinEvent` 保留 null join message 行為；`QuitListener` 的 serialize -> colorize -> deserialize 後處理語意、item / golden head、clickable command 與 scoreboard 明確留到後續切片。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 命中降為 33 個。

補充進度（2026-05-18）：Step 23 第三個程式碼切片已完成。`PluginItems` 的 item display name / lore component 轉換與 `Extra#createHead()` 的 golden head recipe display name 已改走 `PluginText`。本刀保留 item name 預設 `&r&f`、lore 預設 `&7`、lore 內嵌換行拆行、material alias、`Settings.Misc.GOLDEN_HEAD_NAME` 來源與 golden head 功能判斷；`GoldenHeadListener`、`MainSettingsMenu` inventory editor、scenario fancy name、broadcast item list、clickable command 與 scoreboard 留到後續切片。後續 resource cleanup 已將 `gui.yml` / `items.yml` 改成 Bukkit 1.21 material 名稱並移除 `PluginItems` material alias map。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 命中降為 29 個。

補充進度（2026-05-18）：Step 23 第四個程式碼切片已完成。`AbstractScenario#getName()`、`GamePlaceholderReplacer` 與 `GoldenHeadListener` 的 `Component` -> legacy string 轉換已改走 `PluginText.toLegacyString(...)`，集中 scenario fancy name、broadcast custom item display name 與 golden head item display name 的 serializer 邊界。本刀保留 golden head 的 `name.equalsIgnoreCase(Settings.Misc.GOLDEN_HEAD_NAME)` 判斷、settings 格式與 recipe display name 來源，不改功能語意，也不處理 `MainSettingsMenu`、clickable command、`QuitListener` 或 scoreboard。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 23 行，剩餘 10 個 Java 檔案。

補充進度（2026-05-18）：Step 23 第五個程式碼切片已完成。`/team invite`、`/uhc regen` 與 `MainSettingsMenu` inventory editor 的 clickable command component 已改用 `PluginText.toComponent(...)` 建立，移除這三處本地 `legacyAmpersand()` serializer；run command、hover text、seed confirm / skip、`/tohead` / `/finish` input session 與背包保存流程都維持原樣。本刀沒有新增 clickable helper 或 command framework，也不處理 `MainSettingsMenu` golden head display name、`OreAlert` legacy string 輸出、`QuitListener` 或 scoreboard。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 18 行，剩餘 8 個 Java 檔案。

補充進度（2026-05-18）：Step 23 第六個程式碼切片已完成。`MainSettingsMenu` inventory editor 的 golden head display name 已改用 `PluginText.toComponent(Settings.Misc.GOLDEN_HEAD_NAME)`，並移除本檔剩餘的 `LegacyComponentSerializer` import / `LEGACY_SECTION` 欄位。本刀保留 `/tohead` input、`Material.GOLDEN_APPLE` 檢查、`Settings.Misc.GOLDEN_HEAD_NAME` 來源、背包保存與 game mode 還原流程，不新增 helper，也不處理 `OreAlert`、`CombatRelog`、`UHCTeam`、`QuitListener` 或 scoreboard。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 16 行，剩餘 7 個 Java 檔案。

補充進度（2026-05-18）：Step 23 第七個程式碼切片已完成。`CombatRelog` villager custom name 與 `UHCTeam` backpack inventory title 已改用 `PluginText.toComponent(...)` 建立，移除這兩檔本地 `LegacyComponentSerializer` import。本刀保留 combat relog entity 產生、no-AI / silent、裝備複製、背包大小、team name 來源與 team runtime 流程，不新增 helper，也不處理 `OreAlert`、`QuitListener` 或 scoreboard。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 12 行，剩餘 5 個 Java 檔案。

補充進度（2026-05-18）：Step 23 第八個程式碼切片已完成。`PluginText` 新增薄的 `toLegacyAmpersandString(Component)`，`OreAlert#colorizedName()` 改委派此方法，保留 staff ore alert 需要的 `&` 色碼 legacy string 輸出形狀，並移除 `OreAlert` 本地 `LegacyComponentSerializer` / `LEGACY_AMPERSAND`。本刀不改 ore alert toggle、alert 對象、ore material matching 或訊息格式，也不處理 `QuitListener` 或 scoreboard。`PluginTextTest` 已覆蓋 ampersand serialize；`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 11 行，剩餘 4 個 Java 檔案。

補充進度（2026-05-18）：Step 23 第九個程式碼切片已完成。`QuitListener` 的 quit message 後處理保留原本 serialize -> colorize -> deserialize 語意，但改委派 `PluginText.toLegacyString(...)` 與 `PluginText.toComponent(...)`，並移除本檔本地 `LegacyComponentSerializer` import。本刀保留 `onPlayerQuit(e)` 呼叫順序、null quit message 行為與 quit event flow，不處理 scoreboard。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 8 行，剩餘 3 個 Java 檔案。

補充進度（2026-05-18）：Step 23 第十個程式碼切片已完成。`SimpleSidebar` 與 `SimpleScores` 的 scoreboard component 轉換已改委派 `PluginText.toComponent(...)`，移除兩檔本地 `LegacyComponentSerializer` import；`SimpleSidebar` 保留 `§` entry code、15 行 entry、16 / 32 字元切割、hex color carry 與 legacy color carry 邏輯，`SimpleScores` 保留 team color fail-safe、team prefix truncate、below-name heart 與 tab health objective flow。本刀不重寫 scoreboard 行為。`bash scripts/package-plugin-1.21.sh` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，console 執行 `uhc reload` 成功，latest.log 未出現本刀相關 `ERROR` / `Exception`。`LegacyComponentSerializer` 的 `rg -n` 命中降為 3 行，剩餘 1 個 Java 檔案：`PluginText`。

本步驟不得做的事：

1. 不因為名稱含有 `Legacy` 就全域刪除 `LegacyComponentSerializer`，導致 `&c` / `§c` 舊設定與舊訊息失效。
2. 不在沒有 migration 與文件前，直接把 `messages.yml`、`items.yml` 或其他 config 改成 MiniMessage-only。
3. 不改變玩家可見文字內容、換行、placeholder 或既有顏色，除非有明確驗收紀錄。
4. 不重新設計 command、menu、scoreboard 或 login gate 的業務流程；本步驟只處理訊息格式邊界。

優先修改或新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/text/PluginText.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/util/Chat.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/menu/PluginMenu.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/item/PluginItems.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleSidebar.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/scoreboard/SimpleScores.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/login/UHCLoginEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/join/UHCJoinEvent.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/MotdListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/LobbyQuitListener.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/state/share/QuitListener.java`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/items.yml`
- `Update-WonderlandUHC/README.md`
- `DEVELOPMENT.md`

要做的事：

1. 盤點所有 `LegacyComponentSerializer` 使用點，分類為 config/message 邊界、component 輸出、legacy string serialize、placeholder 相容或可直接改成 `Component` API。
2. 建立單一訊息格式入口，例如 `PluginText` 或專用 message formatter，讓 `&` / `§` 讀取、Adventure `Component` 輸出與 placeholder replacement 集中處理。
3. 將 command、menu、scoreboard、join/quit/login kick、item display name 等散落的 serializer 呼叫逐步改走同一入口。
4. 保留舊設定相容：既有 `&c`、`§c`、舊換行與 placeholder 必須能被讀取；若導入 MiniMessage，需明確定義 legacy 與 MiniMessage 的共存規則。
5. 記錄目前對外訊息格式策略：Step 23 完成後維持 legacy-compatible config，`LegacyComponentSerializer` 只允許集中在 `PluginText`；完整 MiniMessage / legacy config migration 交給 Step 24。
6. 對玩家可見訊息建立抽樣驗收清單，至少包含登入拒絕、join/quit、team invite、scoreboard title/line、menu title、item display name、golden head、combat relog name。
7. 更新 Step 24 / Step 27 需要的文件待辦，說明舊設定是否仍可使用、推薦的新格式，以及未來移除 legacy 相容時的 migration 路徑。

完成條件：

- `LegacyComponentSerializer` 不再散落於業務邏輯；若仍存在，必須集中在明確的 text/config/message 邊界。
- 既有 `&` / `§` 舊色碼設定仍可讀取，玩家可見文字與顏色未出現未接受差異。
- 完整 MiniMessage / legacy config format migration 已被明確轉交 Step 24，不混入本步驟。
- README / DEVELOPMENT 的最終對外說明留到 Step 27，但本步驟已記錄訊息格式策略與維護邊界。
- 程式碼變更後依專案規則完成封裝、Paper `1.21.11` startup、`/uhc reload` 與訊息顯示抽樣測試。

## 24. Message Format Modernization：MiniMessage / legacy config migration

Step 23 已把 `LegacyComponentSerializer` 集中到 `PluginText`，讓業務邏輯不再直接依賴 legacy serializer；但 repository 預設 YAML 與既有伺服器設定仍大量使用 `&` / `§` 舊色碼。若目標是讓專案未來更容易持續更新，不能只把 `LegacyComponentSerializer` 名稱硬刪掉，而要把設定格式正式遷移到可維護的新格式，並提供既有設定的遷移策略。

本步驟的目標是定義並實作下一代訊息格式。預設方向是 MiniMessage，但第一刀前仍需確認是否採用 MiniMessage-only、MiniMessage + 一次性 legacy migrator，或短期 dual-read 過渡。只要 runtime 還要直接讀 `&` / `§`，就不算完全移除 legacy format。

補充進度（2026-05-18）：Step 24 已建立只讀盤點文件 `docs/message-format-migration.md`，先固定現有 legacy 色碼 / placeholder / click-hover / scoreboard / item-menu / data folder migration 需求與停損線。後續程式碼切片必須先解決 parser、備份、dry-run、失敗不覆蓋與人工修復路徑，不能直接批次轉 YAML 或直接切成 MiniMessage-only。

補充進度（2026-05-18）：Step 24 第一個程式碼切片已完成 MiniMessage parser proof。`PluginText` 新增 `toMiniMessageComponent(...)` 與 `toMiniMessageString(...)`，`MessageFormatMigration` 目前只委派既有 legacy parse -> `Component` -> MiniMessage serialize，不手刻 legacy 色碼表，並補測試覆蓋 MiniMessage parse、legacy -> MiniMessage、legacy color code reset decoration、placeholder 保留與 literal `<` escaping。本刀尚未切換 production parser、未轉 YAML、未接 data folder migration；`LegacyComponentSerializer` 仍集中保留在 `PluginText`。

補充進度（2026-05-18）：Step 24 第二個程式碼切片已完成單檔 migration safety proof。`MessageFormatMigration` 新增 `dryRun(...)` / `apply(...)`，先證明備份命名、dry-run 不寫檔、既有備份不覆蓋、備份路徑異常時不改原檔、temp file 寫回與只轉 legacy 行的行為。本刀仍未接 `PluginBootstrap`、未掃 data folder、未轉 repo 預設 YAML，也尚未宣稱 runtime legacy parser 可移除。

補充進度（2026-05-18）：Step 24 第三個程式碼切片已開始實際 resource 試點。`PluginText.toComponent(...)` 先採短期 dual-read：legacy `&` / `§` 格式碼仍走 legacy parser，含已接受 MiniMessage 顏色 / 樣式 tag 的訊息走 MiniMessage parser，未知角括號文字仍作為純文字避免誤判；`commands.yml` 最前方低風險單行訊息已轉成可讀的 MiniMessage 區段寫法。本刀不處理 click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`，也不代表 legacy parser 已可移除。

補充進度（2026-05-18）：Step 24 第四個程式碼切片已補強 MiniMessage template 的 placeholder value 邊界。若 MiniMessage template 插入 legacy `&` / `§` 色碼值，`PluginText` 會把該值轉成會關閉 tag 的 MiniMessage 片段，避免顏色外溢或讓整段訊息退回 legacy parser；literal `<green>` 類 placeholder 文字也會被 escape。`commands.yml` 的 `Whitelist` 6 條單行訊息已轉成 MiniMessage，`Whitelist.List` 多行分隔線與其他 command 區塊仍留待後續小刀。

補充進度（2026-05-19）：Step 24 第五個程式碼切片已轉換 `commands.yml` 的 `GiveAll` 與 `Border` 單行訊息。這刀只處理 5 條低風險 command 文字，保留錯誤紅色、成功綠色、白色 placeholder 與 `瞬縮系統` 白色粗體語意；不處理多行 command list、click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第六個程式碼切片已轉換 `commands.yml` 的 `SetSpawn`、`Heal` 與 `SendCoords` 單行訊息。這刀只處理 3 條低風險 command 文字，保留全綠成功訊息、heal 顯示的綠色玩家 / 白色文字 / 紅色血量，以及座標訊息的綠色框架、aqua 標題 / 玩家名、粗體箭頭與白色座標值；不處理 `Respawn` 多行、`Team` / `Info` / `TopKills` / `TeamList`、click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第七個程式碼切片已轉換 `commands.yml` 的 `Uhc.SetHost.Host_Changed` 與 `Uhc.SpecToggle` 2 條單行訊息。這刀保留灰色主持人設定提示、灰色 / 深青 `觀戰系統` prefix、紅色錯誤訊息與黃色 `/{cmd}` placeholder；不處理 `Uhc.Choose` / `Uhc.Regen` 多行、`Respawn` 多行、`Team` / `Info` / `TopKills` / `TeamList`、click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第八個程式碼切片已轉換 `commands.yml` 的 `Team.Already_Has_One`、`Player_Not_In_Team`、`Not_Owner`、`Player_Has_No_Team`、`You_Dont_Have_Team` 5 條基礎檢查單行訊息。這刀只保留原本全紅色錯誤訊息語意；不處理 `Team.Create` / `Team.Invite` / `Team.Join` / `Team.Public` / `Team.Kick` / `Team.Chat`、`Team.Invite.Invitation_Messages` 的 `{click-join}` click / hover、多行 command、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第九個程式碼切片已轉換 `commands.yml` 的 `Team.Invite.Already_In_Your_Team`、`Team.Join.No_Invitation`、`Team.Public.Opened`、`Team.Public.Closed` 4 條單行訊息。這刀保留紅色 invite / join 錯誤、灰色 / 綠色 `隊伍` prefix，以及公開狀態的綠色 / 紅色語意；不處理 `Team.Create`、`Team.Invite.Invited` / `Invitation_Messages` / `Click_Here`、`Team.Kick`、`Team.Chat`、`{click-join}` click / hover、多行 command、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十個程式碼切片已轉換 `commands.yml` 的 `Team.Chat.Cant_Use`、`Joined`、`Quitted` 3 條單行訊息。這刀保留灰色 / 綠色粗體 `隊伍聊天` prefix、不可用時紅色錯誤、加入時綠色狀態 / `/{cmd}`、離開時紅色狀態 / `/{cmd}`；不處理 `Team.Create`、`Team.Invite.Invited` / `Invitation_Messages` / `Click_Here`、`Team.Kick`、`{click-join}` click / hover、多行 command、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十一個程式碼切片已轉換 `commands.yml` 的 `Team.Create.Created` 與 `Team.Kick.Kicked` 多行訊息。這刀保留灰色刪除線分隔線、創建成功的綠色訊息、白色說明文字與綠色 `/{cmd}` placeholder，以及踢出訊息的紅色 `{player}` / 白色結果文字；不處理 `Team.Invite.Invited` / `Invitation_Messages` / `Click_Here`、`{click-join}` click / hover、其他多行 command、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十二個程式碼切片已轉換 `commands.yml` 的 `Team.Invite.Invited` 多行訊息。這刀保留灰色刪除線分隔線、綠色 `{player}` / `{target}` placeholder 與白色邀請狀態文字；不處理 `Team.Invite.Invitation_Messages` / `Click_Here`、`{click-join}` click / hover、其他多行 command、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十三個程式碼切片已轉換 `commands.yml` 的 `Team.Invite.Invitation_Messages` 與 `Click_Here`。這刀保留灰色刪除線分隔線、綠色 `{player}`、白色邀請文字、金色粗體 `[點擊這裡來接受]`，且不新增 YAML click / hover DSL；既有 `InviteCommand` 仍對包含 `{click-join}` 的整行掛上 `/team join <player>` run command 與 hover text，並以 `PluginTextTest.toComponentKeepsClickJoinMiniMessageReplacementClickable` 覆蓋 raw replace 後可解析與掛事件。仍未處理其他多行 command、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十四個程式碼切片已轉換 `commands.yml` 的 `Uhc.Choose.Kick_Init_Msg` 與 `Uhc.Regen.Creating_World` 多行訊息。這刀保留 choose kick message 的空白行、灰色 `[WonderlandUHC]` 框架、綠色 `Wonderland`、白色 `UHC`、灰色提示文字，以及 regen 訊息的空白行、綠色預覽提示、灰色說明文字、綠色 `/uhc choose` 與金色 `/uhc regen`；不處理 `Respawn`、`Whitelist.List`、`Info` / `TopKills` / `TeamList`、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十五個程式碼切片已轉換 `commands.yml` 的 `Whitelist.List` 多行訊息。這刀保留灰色刪除線分隔線、aqua 粗體 `白名單列表` 標題、灰色項目前綴與 aqua `{players}` placeholder；不處理 `Respawn`、`Info` / `TopKills` / `TeamList`、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十六個程式碼切片已轉換 `commands.yml` 的 `Respawn.Is_Playing`、`Broadcast`、`Respawned`。這刀保留紅色錯誤、白色 `{mod}` / `{player}`、綠色復活廣播文字、白色刪除線分隔線、綠色粗體標題、空白行與 aqua 說明文字；只改訊息格式，未更動 `RespawnCommand` 的復活流程、傳送、無敵、事件或 scenario 互動，且不處理 `Info` / `TopKills` / `TeamList`、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第十七個程式碼切片已修正 `GamePlaceholderReplacer` 的 placeholder 套用方式，改用既有 `PluginText.replaceToString(...)`，避免 `Config.Messages` / broadcast / GUI 類 MiniMessage template 被 raw legacy replacement 值污染後整行退回 legacy parser。本刀不新增全域 message registry 或 YAML DSL，也尚未轉換 `Config.Messages`、`TopKills`、`TeamList`。

補充進度（2026-05-19）：Step 24 第十八個程式碼切片已轉換 `commands.yml` 的 `Config.Messages`。這刀保留灰色刪除線分隔線、白色主持人 / 模式標籤、綠色計時 / 規則標籤、黃色物資 / 世界設定標籤、灰色 placeholder 值與綠色 `/scenarios` / `/disableitems`；此區塊依賴第十七刀的 `GamePlaceholderReplacer` placeholder 邊界處理，避免 `{friendly-fire}` / `{nether-on}` 等 legacy boolean replacement 污染 MiniMessage template。仍未轉換 `TopKills`、`TeamList`。

補充進度（2026-05-19）：Step 24 第十九個程式碼切片已轉換 `commands.yml` 的 `TopKills.Messages` 與 `TeamList.Messages`，並讓 `ListCommand` 以 `PluginText.replaceToString(...)` 處理一般 placeholder，只對 `{color}` 做局部 MiniMessage color tag 轉換以保留舊版隊伍顏色控制行為。本刀保留 TopKills 的紅 / 金 / 黃 / 白名次配色、TeamList 的隊伍動態顏色、存活 / 死亡玩家顏色與 heart 樣式；`commands.yml` 目前已無 legacy `&` / `§` 色碼命中。仍未處理 scoreboard、GUI、item lore、scenario、`messages.yml`、`broadcasts.yml`、`items.yml`、`gui.yml`、`scoreboards.yml`、`scenarios.yml` 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第二十個程式碼切片已轉換 `broadcasts.yml` 的 `Discord.Invalid_Channel`，保留紅色玩家端錯誤訊息；Discord 公告 `Formatting` 純文字模板本身不含 legacy 色碼，未改動。仍未處理 scoreboard、GUI、item lore、scenario、`messages.yml`、`items.yml`、`gui.yml`、`scoreboards.yml`、`scenarios.yml` 或 `Golden_Head_Name`。

補充進度（2026-05-19）：Step 24 第二十一個程式碼切片已轉換 `items.yml` 的 lobby、spectator、staff addon hotbar item `Name` / `Lore`，保留原本黃色 / 紅色 / 金色 / 綠色 / aqua 名稱與灰色 lore。`PluginItems` 僅在 item Name / Lore 已含 MiniMessage tag 時跳過舊版 `&r&f` / `&7` 預設前綴，避免 MiniMessage 被 legacy prefix 污染；legacy 與純文字 item 仍保留原本預設名稱 / lore 顏色行為。本刀不處理 `Golden_Head_Name` 或 GUI menu resource。後續 resource cleanup 已移除不再被讀取的 `Lobby.Leave`，並將 `gui.yml` / `items.yml` 舊材質名改成 Bukkit 1.21 material 名稱，移除 `PluginItems` material alias map。

補充進度（2026-05-19）：Step 24 第二十二個程式碼切片已轉換 `settings.yml` 的 `Misc.Golden_Head_Name`，保留原本金色粗體 `金頭顱` 語意。`Extra#createHead()` 與 `MainSettingsMenu` 已透過 `PluginText.toComponent(Settings.Misc.GOLDEN_HEAD_NAME)` 顯示，`GoldenHeadListener` 仍沿用同一份 `Settings.Misc.GOLDEN_HEAD_NAME` 判斷 golden head；本刀只處理 repo 預設 resource，不處理既有 data folder 自動遷移，也不新增 registry 或 YAML DSL。

補充進度（2026-05-19）：Step 24 第二十三個程式碼切片已轉換 `scenarios.yml` 前段 5 個 scenario `Name`：`Armor_Vs_Health`、`Absorption_Less`、`Backpack`、`Bench_Blitz`、`Blood_Diamonds`。這刀只處理 scenario icon display name，保留原本主色與灰色英文代號，沿用 `ScenarioConfig#getFancyName()` -> `PluginItems.create(...)` 的既有路徑；不處理 `Armor_Vs_Health.Warn_Msg`、runtime broadcast、`{fancy-time}` 或其他 scenario 訊息。

補充進度（2026-05-19）：Step 24 第二十四個程式碼切片已轉換 `scenarios.yml` 第二組 5 個 scenario `Name`：`Bow_Less`、`Cut_Clean`、`Damage_Dogers`、`Diamond_Less`、`Double_Or_Nothing`。這刀只處理 scenario icon display name，保留紅 / 黃 / 深紅主色與灰色英文代號；`Damage_Dogers.Death_Cause_This` 仍保留 legacy，留到 runtime broadcast 切片再處理 placeholder、粗體與玩家名顏色。

補充進度（2026-05-19）：Step 24 第二十五個程式碼切片已轉換 `scenarios.yml` 第三組 5 個 scenario `Name`：`Fast_Obsidian`、`Fast_Smelting`、`Fire_Less`、`Food_Neophobia`、`Gold_Less`。這刀只處理 scenario icon display name，保留黃 / 金 / 深綠 / 紅主色與灰色英文代號；`Food_Neophobia.First_Eat` / `Just_Can_Eat` 仍保留 legacy，留到 runtime 訊息切片再處理 placeholder、粗體與食物名稱顏色。

補充進度（2026-05-19）：Step 24 第二十六個程式碼切片已轉換 `scenarios.yml` 第四組 5 個 scenario `Name`：`Hasty_Boys`、`Horse_Less`、`Iron_Man`、`Less_Bow_Damage`、`Limitations`。這刀只處理 scenario icon display name，保留黃 / 紅 / aqua / 深綠主色與灰色英文代號；`Iron_Man.Damage_Before_Final_Heal`、`Limitations.Reached_Limit`、`Limitations.Cant_Mine_More` 仍保留 legacy，留到 runtime 訊息切片再處理 placeholder、粗體與礦物名稱顏色。

補充進度（2026-05-19）：Step 24 第二十七個程式碼切片已轉換 `scenarios.yml` 第五組 5 個 scenario `Name`：`Lucky_Leaves`、`No_Clean`、`No_Enchant`、`No_Fall`、`Potion_Less`。這刀只處理 scenario icon display name，保留黃 / aqua / 紅主色與灰色英文代號；`No_Clean.Description` 的 `{fancy-time}` placeholder 保留原語法，未處理其他 scenario lore 或 runtime 行為。

補充進度（2026-05-19）：Step 24 第二十八個程式碼切片已轉換 `scenarios.yml` 第六組 5 個 scenario `Name`：`Rod_Less`、`Shift_Kill`、`Silk_Web`、`Soup`、`Swap_Inventory`。這刀只處理 scenario icon display name，保留紅 / aqua / 黃主色與灰色英文代號；未處理 scenario description、runtime 訊息、`Warn_Msg` 或 `{fancy-time}` 行為。

補充進度（2026-05-19）：Step 24 第二十九個程式碼切片已轉換 `scenarios.yml` 第七組 5 個 scenario `Name`：`Switcheroo`、`Timber`、`Time_Bomb`、`Triple_Arrow`、`Triple_Ores`。這刀只處理 scenario icon display name，保留 aqua / 黃主色與灰色英文代號；`Time_Bomb.Description` 的 `{fancy-time}` 與 `Time_Bomb.Exploded` 仍保留原格式，留到 lore/runtime 訊息切片再驗證。

補充進度（2026-05-19）：Step 24 第三十個程式碼切片已完成 `scenarios.yml` scenario icon display name 收尾，轉換 `Vanilla_Plus`、`Vein_Miners`、`Fragile_Rods` 這 3 個 `Name`，保留黃 / 紅主色與灰色英文代號。`scenarios.yml` 目前已無 `Name: "&..."` 命中；剩餘 legacy 色碼集中在 runtime 訊息、`Warn_Msg` 與需要另行驗收的 `{fancy-time}` 相關文字。

補充進度（2026-05-19）：Step 24 第三十一個程式碼切片已轉換 `scenarios.yml` 的無 placeholder runtime 訊息 `Backpack.Cant_Use_Msg` 與 `Bench_Blitz.Workbench_Created`。`Cant_Use_Msg` 目前只由 scenario config 載入，`BackPackCommand` 不會實際送出它，本刀不改 command 行為；`Workbench_Created` 保留灰色括號、深綠 `匠魂之心` 與紅色限制提示，仍透過 `ScenarioBenchBlitz` 的 `Chat.send(...)` 顯示。帶 placeholder 的 runtime 訊息與 `Warn_Msg` 留到後續切片。

補充進度（2026-05-19）：Step 24 第三十二個程式碼切片已轉換 `Armor_Vs_Health.Warn_Msg`，保留深紅刪除線分隔線、空白行、紅色提示文字與 `{fancy-time}` placeholder。`ScenarioArmorVsHealth` 仍沿用 `PluginText.replaceTimeToArray(Warn_Msg, Apply_Within_Seconds)` 後交給 `Chat.send(...)`，不新增專用 message registry 或 YAML DSL；新增 `PluginTextTest.replaceTimeToArrayKeepsMiniMessageMultilineTemplate` 覆蓋 MiniMessage 多行 template 經時間 placeholder 替換後仍可解析。

補充進度（2026-05-19）：Step 24 第三十三個程式碼切片已轉換 `Damage_Dogers.Death_Cause_This`，保留灰色括號、深紅 scenario 名稱、深紅粗體 `{player}`、紅色粗體提示、深紅粗體 `{amount}` 與空格排版。`ScenarioDamageDogers` 仍沿用既有 `.replace("{player}", entity.getName()).replace("{amount}", remaining + "")` 後交給 `Chat.broadcast(...)`；玩家名與剩餘數量不是任意格式輸入，本刀不新增 helper。

補充進度（2026-05-19）：Step 24 第三十四個程式碼切片已轉換 `Food_Neophobia.First_Eat` 與 `Just_Can_Eat`，保留灰色括號、深綠 scenario 名稱、紅色提示、第一口訊息的粗體語意，以及金色 `{foodtype}`。`ScenarioFoodNeophobia` 仍沿用既有 `.replace("{foodtype}", material.name())` 後交給 `Chat.send(...)`；`{foodtype}` 來源是 Bukkit `Material.name()`，本刀不新增 helper。

補充進度（2026-05-19）：Step 24 第三十五個程式碼切片已轉換 `Iron_Man.Damage_Before_Final_Heal` 與 `Time_Bomb.Exploded`。`Iron_Man` 保留灰色括號、aqua scenario 名稱與紅色粗體提示；`Time_Bomb` 保留深灰括號、紅色 scenario 名稱、灰色 `{player}` 與爆炸訊息。`ScenarioIronMan` 仍透過 `Chat.send(...)` 顯示；`ScenarioTimeBomb` 仍沿用既有 `.replace("{player}", owner.getName())` 後 `Chat.broadcast(...)`，本刀不新增 helper。

補充進度（2026-05-19）：Step 24 第三十六個程式碼切片已轉換 `Limitations.Reached_Limit` 與 `Cant_Mine_More`，保留灰色括號、深綠 scenario 名稱、紅色提示、粗體語意、金色粗體 `{block}` 與 `{amount}` 文字。`ScenarioLimitations` 仍沿用既有 `.replace("{amount}", ...).replace("{block}", blockType.name())` 後交給 `Chat.send(...)`；replacement 來源是整數與 Bukkit `Material.name()`，本刀不新增 helper。`scenarios.yml` 目前已無 legacy `&` / `§` 色碼命中。

補充進度（2026-05-19）：Step 24 第三十七個程式碼切片已補上 scoreboard MiniMessage bridge，`SimpleSidebar` 在既有 title / line 切割與顏色延續前，先把 MiniMessage 或 legacy 輸入統一轉成 legacy section string；切割、slot 與 team prefix / suffix 流程維持原樣。Resource 只試點轉換 `scoreboards.yml` 的 `Default.Lobby`，保留灰色刪除線、白色 label、綠色 placeholder 與空白行；其他 scoreboard 區塊、`Ultra` theme、註解範例、team prefix、below-name heart、tab health 與既有 data folder migration 仍未處理。

補充進度（2026-05-19）：Step 24 第三十八個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Starting` active lines，保留灰色刪除線、白色 `遊戲倒數` / `已傳送` / `未傳送` label、綠色 `{start_in}` / `{teleported}` / `{teleporting}` placeholder，並將原本 `&f ` 空白行轉為 `<white> </white>`。本刀沿用第三十七刀的 `SimpleSidebar` bridge，不再調整 renderer，也不處理其他 scoreboard 區塊、`Ultra` theme、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第三十九個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Spectator_Solo` active lines，保留灰色刪除線、白色 `時間` / `玩家` / `邊界大小` / `收縮倒數` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder，原本純空白行維持 `" "` 不變。本刀不處理 `Spectator_Teams`、staff / player scoreboard、`Ultra` theme、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Spectator_Teams` active lines，保留與 `Default.Spectator_Solo` 同型的灰色刪除線、白色 `時間` / `玩家` / `邊界大小` / `收縮倒數` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder，原本純空白行維持 `" "` 不變。本刀不處理 staff / player scoreboard、`Ultra` theme、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十一個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Staff_Solo` active lines，保留灰色刪除線、白色 `順暢度` / `記憶體` label、灰色 `(TPS)` / `(RAM)` 標籤、綠色 `{tps}` / `{free_ram}` placeholder，以及白色時間 / 玩家 / 邊界 label 與綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder；原本純空白行維持 `" "` 不變。本刀不處理 `Staff_Teams`、player scoreboard、`Ultra` theme、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十二個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Staff_Teams` active lines，保留與 `Default.Staff_Solo` 同型的灰色刪除線、白色 `順暢度` / `記憶體` label、灰色 `(TPS)` / `(RAM)` 標籤、綠色 `{tps}` / `{free_ram}` placeholder，以及白色時間 / 玩家 / 邊界 label 與綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder；原本純空白行維持 `" "` 不變。本刀不處理 player scoreboard、`Ultra` theme、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十三個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Player_Solo` active lines，保留灰色刪除線、白色 `遊戲時間` / `玩家數量` / `擊殺數` / `邊界大小` / `收縮倒數` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{kills}` / `{border_size}` / `{shrink_in}` placeholder，原本雙空白行 `"  "` 與單空白行 `" "` 均維持不變。本刀不處理 `Player_Teams`、`Ultra` theme、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十四個程式碼切片已轉換 `scoreboards.yml` 的 `Default.Player_Teams` active lines，保留灰色刪除線、白色 `遊戲時間` / `玩家數量` / `隊伍數量` / `擊殺數` / `隊伍擊殺數` / `邊界大小` / `收縮倒數` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{alive_teams}` / `{all_teams}` / `{kills}` / `{team_kills}` / `{border_size}` / `{shrink_in}` placeholder，原本兩條雙空白行 `"  "` 均維持不變。至此 `Default` active theme 已無 legacy `&` / `§` 色碼；`Ultra` theme、註解範例與既有 data folder migration 仍未處理。

補充進度（2026-05-19）：Step 24 第四十五個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Lobby` active lines，保留白色刪除線分隔線、白色 `玩家數量` / `主持人` / `遊戲模式` / `將於` label、綠色 `{online_players}` / `{host}` / `{team_size}` / `{teleport_in}` placeholder、綠色 `YourIP.net`，原本兩條純空白行 `" "` 均維持不變。本刀不處理 `Ultra` 其他狀態、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十六個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Starting` active lines，保留白色刪除線分隔線、白色 `傳送玩家中....` 與 `將於` / `後開始` 文字、綠色 `{start_in}` placeholder、綠色 `YourIP.net`，原本純空白行 `" "` 維持不變。本刀不處理 `Ultra` 其他狀態、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十七個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Spectator_Solo` active lines，保留白色刪除線分隔線、白色 `遊戲時間` / `存活玩家` / `邊界` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder、綠色 `YourIP.net`。本刀不處理 `Ultra.Spectator_Teams`、staff / player scoreboard、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十八個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Spectator_Teams` active lines，保留與 `Ultra.Spectator_Solo` 同型的白色刪除線分隔線、白色 `遊戲時間` / `存活玩家` / `邊界` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder、綠色 `YourIP.net`。本刀不處理 staff / player scoreboard、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第四十九個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Staff_Solo` active lines，保留灰色刪除線分隔線、白色 `順暢度` / `記憶體` label、灰色 `(TPS)` / `(RAM)` 標籤、綠色 `{tps}` / `{free_ram}` placeholder，以及白色 `遊戲時間` / `存活玩家` / `邊界` label 與綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder。本刀不處理 `Ultra.Staff_Teams`、player scoreboard、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Staff_Teams` active lines，保留與 `Ultra.Staff_Solo` 同型的灰色刪除線分隔線、白色 `順暢度` / `記憶體` label、灰色 `(TPS)` / `(RAM)` 標籤、綠色 `{tps}` / `{free_ram}` placeholder，以及白色 `遊戲時間` / `存活玩家` / `邊界` label 與綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder。本刀不處理 player scoreboard、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十一個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Player_Solo` active lines，保留白色刪除線分隔線、白色 `遊戲時間` / `存活玩家` / `擊殺數` / `邊界` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{kills}` / `{border_size}` placeholder、綠色 `YourIP.net`。本刀不處理 `Ultra.Player_Teams`、註解範例或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十二個程式碼切片已轉換 `scoreboards.yml` 的 `Ultra.Player_Teams` active lines，保留白色刪除線分隔線、白色 `遊戲時間` / `存活玩家` / `擊殺數` / `隊伍擊殺數` / `邊界` label、綠色 `{game_time}` / `{remaining}` / `{all}` / `{kills}` / `{team_kills}` / `{border_size}` placeholder、綠色 `YourIP.net`。至此 `Ultra` active theme 已無 legacy `&` / `§` 色碼；註解範例與既有 data folder migration 仍未處理。

補充進度（2026-05-19）：Step 24 第五十三個程式碼切片已轉換 `scoreboards.yml` 註解中的 `Badlion` 自訂範例，保留 `#` 註解狀態與原本範例結構，只把範例內的 legacy 色碼改成 MiniMessage。至此 `scoreboards.yml` 已無 legacy `&` / `§` 色碼命中；team prefix、below-name heart、tab health 與既有 data folder migration 仍未處理。

補充進度（2026-05-19）：Step 24 第五十四個程式碼切片已開始轉換 `messages.yml`，只處理檔案最前方 8 條共用頂層訊息，保留紅色錯誤文字、`Error` 的深灰粗體括號 / 深紅粗體驚嘆號，以及 `Enabled` / `Disabled` 左右 `--` 的刪除線語意。本刀不處理 `Host`、`Editor`、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater、click / hover 訊息或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十五個程式碼切片已轉換 `messages.yml` 的 `Host` 區塊，保留灰色括號 prefix、白名單 aqua、特殊模式 light purple、地獄 red、隊伍 aqua、邊界 dark green、遊戲世界 green、綠色 placeholder，以及開啟 / 成功為綠色、關閉 / 錯誤為紅色的語意。本刀不處理 `Editor`、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater、click / hover 訊息或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十六個程式碼切片已轉換 `messages.yml` 的 inventory editor click labels：`Editor.Click_Finish`、`Editor.Inventory.Click_To_Head` 與 `Editor.Inventory.To_Head_Failed`。click 行為仍由 `MainSettingsMenu#runCommandComponent(...)` 在程式碼掛上 `finish` / `tohead` run command，不新增 YAML click / hover DSL；並新增 `PluginTextTest.toComponentKeepsInventoryEditorMiniMessageRunCommandClickable` 覆蓋 MiniMessage label 外掛 run command 仍保留 click event。本刀不處理其他 `Editor` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十七個程式碼切片已轉換 `messages.yml` 的 `Editor.Number` 區塊，保留紅色 `Invalid_Number`、黃色數字輸入 prompt、灰色 saved 訊息與 `{number}` placeholder。這些 replacement 來源為解析後的數字，不涉及玩家任意格式輸入，因此不改 `startIntegerInput(...)` / `startDoubleInput(...)` 流程。本刀不處理 `Editor.Text`、`Editor.Time`、`Editor.Inventory` 的 prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第五十八個程式碼切片先補 `Editor.Text` placeholder 邊界，不轉換 resource。`MainSettingsMenu` title saved 訊息，以及 `TeamSettingsMenu` team name / character / already-used 訊息改用既有 `PluginText.replaceToString(...)` 套用 placeholder，避免後續 MiniMessage template 被玩家輸入的 literal `<tag>` 或 legacy 色碼污染；新增 `PluginTextTest.replaceToStringKeepsEditorTextInputLiteralInsideMiniMessageTemplate` 覆蓋此邊界。本刀不新增 message registry、YAML DSL 或通用抽象。

補充進度（2026-05-19）：Step 24 第五十九個程式碼切片已轉換 `messages.yml` 的 `Editor.Text` 區塊，保留黃色 prompt、灰色 saved 主文、綠色 `{player}`、`{title}` / `{name}` / `{character}` 的 reset 語意，以及 `Already_Used` 的深灰 `{symbol}` / 紅色錯誤文字。本刀依賴第五十八刀的 placeholder 邊界處理，不處理 `Editor.Time`、`Editor.Inventory` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第六十個程式碼切片已轉換 `messages.yml` 的 `Editor.Broadcast` 三個輸入 prompt，只保留原本黃色提示文字，不改 `GameStartTimeInputSession` 的輸入流程、取消文字、Discord 公告內容或完成後 delivery 行為。本刀不涉及 placeholder、click/hover、換行或玩家輸入回填，也不處理 `Editor.Time`、`Editor.Inventory` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

補充進度（2026-05-19）：Step 24 第六十一個程式碼切片已轉換 `messages.yml` 的 `Editor.Time.Invalid_Time` 與 `Damage`、`FinalHeal`、`Pvp`、`BorderShrink`、`DisableNether` 五組一般時間設定，保留 invalid time 紅色錯誤文字 / 金色格式提示、prompt 的黃色主文 / 灰色 `(小時:分鐘:秒)` / 黃色冒號，以及 saved 訊息的灰色主文 / 綠色 `{time}`。本刀確認一般時間 saved 已走既有 `PluginText.replaceTimeToString(...)`，不改 `TimeSettingsMenu` 輸入流程；`ShrinkCalculator` 因走 `BorderSettingsMenu` 另一條流程，保留到後續小刀。

補充進度（2026-05-19）：Step 24 第六十二個程式碼切片已轉換 `messages.yml` 的 `Editor.Time.ShrinkCalculator` 兩行，保留 prompt 的黃色主文、金色 `{init}x{init}`、水藍色 `{final}x{final}`、灰色 `(小時:分鐘:秒)` / 黃色冒號，以及 saved 訊息的灰色主文 / 綠色 `一秒 {speed} 格`。本刀確認 `BorderSettingsMenu` 只以數字替換 `{init}`、`{final}`、`{speed}`，不新增 placeholder 抽象或改動輸入流程。

補充進度（2026-05-19）：Step 24 第六十三個程式碼切片已轉換 `messages.yml` 的 `Editor.Inventory` 四組 inventory editor prompt / saved 訊息，保留 prompt 的灰色主文與綠色重點文字，以及 saved 訊息的綠色完成文字。本刀不碰已是 MiniMessage 的 `To_Head_Failed`、`Click_To_Head`、`Editor.Click_Finish`，也不改 `InventoryEditSession` 的 click command、背包保存、Golden Head item meta 或玩家 inventory backup 流程。

補充進度（2026-05-19）：Step 24 第六十四個程式碼切片已轉換 `messages.yml` 的 `Lobby.Non_Spawn_Set`、`Player_Join_Msg`、`Player_Leave_Msg`、`Automatic_Game_Canceled` 四個單行訊息，保留 `Non_Spawn_Set` 的紅色粗體主文 / 白色粗體 `/{cmd}`、join / leave 的綠色 `{player}` / 灰色人數文字，以及 auto-cancel 的紅色主文 / 黃色 `{amount}`。本刀確認替換來源只包含固定 command、玩家名稱或數字，不改 `DefaultJoinMessage`、`LobbyQuitListener` 或 `UHCWorldUtils` 的流程；多行 `Welcome_Msg_Configuring` / `Welcome_Msg` 留到後續小刀。

補充進度（2026-05-19）：Step 24 第六十五個程式碼切片已轉換 `messages.yml` 的 `Lobby.Welcome_Msg_Configuring` 多行訊息，保留上下灰色刪除線分隔線、金色未設定提示 / 黃色 `/uhc edit`、深水藍教學提示 / 白色 `/{cmd}`，以及空白行與換行順序。本刀確認 `DefaultJoinMessage` 仍使用既有 `PluginText.replaceToString(List, ...)` 套用 `{cmd}` 並保留換行，不改 join 流程；正式 `Welcome_Msg` 留到後續小刀。

補充進度（2026-05-19）：Step 24 第六十六個程式碼切片已轉換 `messages.yml` 的 `Lobby.Welcome_Msg` 多行訊息，保留上下灰色刪除線分隔線、歡迎 / 主持人 / 指令提示的綠色主文、白色 `{player}` / `{host}` / `/scenarios` / `/config`，以及空白行與換行順序。本刀確認 `DefaultJoinMessage` 仍使用既有 `PluginText.replaceToArray(List, ...)` 套用 placeholder 並逐行送出，不改 join 流程。

補充進度（2026-05-19）：Step 24 第六十七個程式碼切片已轉換 `messages.yml` 的 `Kick` 六個單行訊息，保留五個拒絕登入訊息的紅色文字，以及 `Thanks_For_Playing` 的灰色主文 / 白色 `{player}`。本刀確認 login disallow path 最終仍由既有 `PluginText.toComponent(...)` 解析訊息，`LoginChecker` 既有 `PluginText.colorize(...)` 不會修改 MiniMessage tag，因此不改 login checker 或登入流程。

補充進度（2026-05-19）：Step 24 第六十八個程式碼切片已轉換 `messages.yml` 的 `CenterCleaner` 主要搜尋訊息，但不含 `Search_Regen_Hint`。本刀保留 `[中心搜尋]` 前綴的灰色括號 / 綠色標籤、各搜尋狀態的白色 / 灰色 / 綠色 / 黃色 / 紅色語意，以及 `{current}`、`{total}`、`{stage}`、`{status}`、`{score}`、`{x}`、`{z}`、`{reasons}` placeholder。替換來源只包含 enum、數字、固定中文階段文字或原因清單文字，因此不新增 replacement 抽象或改動 `CenterCleaner` 搜尋流程；含 literal `<seed>` 的 `Search_Regen_Hint` 留到後續小刀。

補充進度（2026-05-19）：Step 24 第六十九個程式碼切片已轉換 `messages.yml` 的 `CenterCleaner.Search_Regen_Hint`，保留灰色主文、金色 `/uhc regen` / `/uhc regen <seed>` 與 literal `<seed>` 文字。本刀新增 `PluginTextTest.toComponentKeepsUnknownAngleTextLiteralInsideMiniMessage`，確認 MiniMessage template 中的 unknown angle text 會以 literal 文字保留，因此不新增特殊 escape 或更動 `CenterCleaner` 流程。

補充進度（2026-05-19）：Step 24 第七十個程式碼切片已轉換 `messages.yml` 的 `Team` 五個單行訊息，保留 `Full_Msg` / `Only_In_Chosen_Mode` 的紅色文字、join / leave / promote 的灰色 `[隊伍]` 括號與綠色標籤、join / leave 的深綠 `{player}` / 綠色加入文字 / 紅色離開文字 / 灰色人數，以及 promote 的白色 `{player}` / 金色新隊長文字。placeholder 來源只包含玩家名稱與數字，不涉及玩家自訂隊伍名稱或徽章；因此不改 `UHCTeam`、team command 或 team menu 流程。

補充進度（2026-05-19）：Step 24 第七十一個程式碼切片已轉換 `messages.yml` 的 `Staff.Enabled` 與 `Staff.Disabled`，保留灰色括號 / 金色 `管理系統` 前綴、enabled 綠色文字與 disabled 紅色文字。本刀不轉換 `Staff.Mined_Alert`，因為 `{block}` 目前由 `OreAlert.colorizedName()` 提供 legacy `&` 色碼；直接轉 MiniMessage 會形成 mixed MiniMessage / legacy template。`Mined_Alert` 留到下一刀先補 replacement 邊界再轉換。

補充進度（2026-05-19）：Step 24 第七十二個程式碼切片已轉換 `messages.yml` 的 `Staff.Mined_Alert`，保留灰色括號 / 金色 `管理系統` 前綴、灰色 `{player}` / 主文 / `個` / 句點、金色 `{amount}`，以及 `{block}` 由 `OreAlert.colorizedName()` 提供的礦物顏色。`StatsListener` 的 raw `.replace(...)` 改用既有 `PluginText.replaceToString(...)`，避免 `{block}` legacy `&` 色碼與 MiniMessage template 混用；不新增新抽象或更動 ore alert 流程。

補充進度（2026-05-19）：Step 24 第七十三個程式碼切片已轉換 `messages.yml` 的 `Spectator` 三個訊息，保留 `No_Perm_To_Spec` 的黃色粗體主文 / 白色粗體 `{fancy-time}`、`Death_Kick_Message` 的灰色括號 / 綠色 `Wonderland` / 白色 `UHC` / 灰色說明文字，以及 `Teleported_To_Player` 的灰色括號 / 深青色 `觀戰系統` / 綠色 `{player}`。`No_Perm_To_Spec` 原本使用 `{fancy-time}`，呼叫點改用既有 `PluginText.replaceTimeToString(...)` 以接回同一組時間 placeholder；不新增新抽象或更動觀戰傳送流程。

補充進度（2026-05-19）：Step 24 第七十四個程式碼切片已轉換 `messages.yml` 的 `CountDown` 主層單行訊息，範圍從 `Scatter_Announce` 到 `Starting_Announce`；保留灰色括號 / 金色粗體 `遊戲提醒` 前綴、scatter started / finished 的黃色粗體、`Scatting_Players` 的灰色進度與 `{count}` / `{total}`，以及 damage / PvP / final heal / nether / start announce 的原色粗體 `{fancy-time}`。一般倒數 announce 沿用既有 `PluginText.replaceTimeToString(...)`，`Scatting_Players` 只替換數字，因此不改 `Countdown` 或 `ScatterHandler`。

補充進度（2026-05-19）：Step 24 第七十五個程式碼切片已轉換 `messages.yml` 的 `CountDown.Game_Started` 多行開局 banner，保留首尾灰色刪除線、綠色粗體 `遊戲開始了!`、兩個純空白行、四行白色說明文字，以及綠色主持人文字 / 白色 `{host}` / 綠色祝福文字。`StartCountdown` 既有 `PluginText.replaceToArray(List, ...)` 已負責 `{host}` 與多行輸出，因此不改倒數狀態機或開局流程。

補充進度（2026-05-19）：Step 24 第七十六個程式碼切片已轉換 `messages.yml` 的 `CountDown.Border` 四個邊界倒數訊息，保留灰色括號 / 金色粗體 `遊戲提醒` 前綴、Timer announce 的 dark red 粗體 `{fancy-time}` 與 `{size}x{size}`、Timer reduced 的灰色完成文字、Shrink announce 的 dark red 粗體 `{fancy-time}`，以及 Shrink reduced 的灰色開始收縮文字。`TimerBorder` / `MovingBorder` 只替換數字 `{size}`，倒數時間仍由既有 `PluginText.replaceTimeToString(...)` 套用，因此不改 border mode 或 countdown flow。

補充進度（2026-05-19）：Step 24 第七十七個程式碼切片已轉換 `messages.yml` 的 `Game` 簡單單行訊息：地獄限制、世界邊界、item disabled、combat relog death、iPvP、arrow health 與 closing server。保留地獄限制的灰色括號 / 紅色前綴與紅色主文、世界邊界與 iPvP fire 的紅色粗體、combat relog death 的白色 `{player}` / 深灰擊殺數括號 / 紅色 `{playerKills}` / 紅色主文、arrow health 的紅色 `{player}` / `{heal}❤` 與 gray 主文，以及 closing server 的紅色粗體主文 / 白色粗體 `{seconds}`。本刀刻意不轉換涉及 team chat format、自訂隊伍名稱或多行勝利名單的訊息。

補充進度（2026-05-19）：Step 24 第七十八個程式碼切片已轉換 `messages.yml` 的 `Game.Player_Disconnect` 與 `Game.Player_Reconnect`，保留 disconnect 的白色 `{player}` / 灰色 `離線了。`，以及 reconnect 的白色 `{player}` / 綠色 `重新登入了遊戲。`。兩者的 `{player}` 來源會串接 `team.getChatFormat()`，可能帶 legacy team color；`PlayingJoinListener` 與 `RolePlayerEvents` 已改用既有 `PluginText.replaceToString(...)` 避免 mixed MiniMessage / legacy template，不新增新抽象或改動 join / combat relog 流程。

補充進度（2026-05-19）：Step 24 第七十九個程式碼切片已轉換 `messages.yml` 的 `Game.You_Have_Been_Killed` 與 `Game.Team_Eliminated`，保留 killed message 的 gray 主文、red `{team}` / `{character}` / `{killer}`、red `{heal}❤`，以及 team eliminated 的 yellow `{team}` / red `全隊滅絕。`。`{killer}` 會串接 `killerUTeam.getChatFormat()`，`{team}` / `{character}` 可能來自玩家自訂隊伍名稱與符號；`UHCDeathDataHandler` 已改用既有 `PluginText.replaceToString(...)` 避免 mixed MiniMessage / legacy template，不新增新抽象或更動死亡流程。

補充進度（2026-05-19）：Step 24 第八十個程式碼切片已轉換 `messages.yml` 的 `Game.NoClean` 四個無敵提示，保留 `Obtained` 的灰色括號 / 黃色粗體 `無敵` 前綴 / 白色粗體主文 / gold 粗體 `{fancy-time}`、`End` 的紅色粗體、`Action_Bar` 的黃色粗體倒數文字與 `{fancy-time}`，以及 `Action_Bar_End` 的紅色粗體。`InvinciblePlayer` 已透過既有 `PluginText.replaceTimePlaceholders(...)` 處理時間 placeholder，因此不改 invincible / no-clean 流程。

補充進度（2026-05-19）：Step 24 第八十一個程式碼切片已轉換 `messages.yml` 的 `Game.Victory_Broadcast` 多行勝利廣播，保留首尾白色刪除線、gold 粗體勝利標題與 `{winner}`、兩個純空白行、gold `隊伍總擊殺數:` / 白色 `{kills}`、gold `隊伍玩家:`、兩個 leading spaces / yellow `{players}`，以及 gray 粗體 `WonderlandUHC感謝你的遊玩。`。`GameManager` 已用既有 `PluginText.replaceToList(...)` 處理 `{winner}` / `{kills}`，再展開純玩家名稱 `{players}`，因此不改勝利廣播流程。

補充進度（2026-05-19）：Step 24 第八十二個程式碼切片已轉換 `messages.yml` 的 `Game.PlayerDeath` 環境死亡訊息，範圍為 `Lava`、`Fire,Fire_Tick`、`Suffocation`、`Contact`、`Fall`、`Drowning`；保留白色 `{player}`、dark gray 擊殺數括號、紅色 `{playerKills}` 與紅色死亡文案。`DeathMessageHandler` 已使用既有 `PluginText.replaceToString(...)` 處理 placeholder，因此不改 death message selection，也不轉換 `Entity_*`、`Logout`、`Other`、`Player_Killed`。

補充進度（2026-05-19）：Step 24 第八十三個程式碼切片已轉換 `messages.yml` 的 `Game.PlayerDeath` 剩餘非玩家擊殺訊息，範圍為 `Entity_Attack`、`Entity_Explosion`、`Block_Explosion`、`Logout`、`Other`；保留白色 `{player}`、dark gray 擊殺數括號、紅色 `{playerKills}`、紅色死亡文案，以及 `{entity}` / `{minute}` placeholder。這些訊息同樣透過既有 `DeathMessageHandler` 與 `PluginText.replaceToString(...)` 處理，不改 death message selection。

補充進度（2026-05-19）：Step 24 第八十四個程式碼切片已轉換 `messages.yml` 的 `Game.PlayerDeath.Player_Killed` 四條玩家擊殺死亡訊息，保留紅色 `{player}` / `{killer}`、dark gray 雙方擊殺數括號、紅色 `{playerKills}` / `{killerKills}`，以及 gray `被` 與四種擊殺文案。`DeathMessageHandler` 已使用既有 `PluginText.replaceToString(...)` 處理 placeholder，可保留雙方 team chat format 的 legacy color；至此 `Game.PlayerDeath` default resource 已無 legacy 色碼。

補充進度（2026-05-19）：Step 24 第八十五個程式碼切片已轉換 `messages.yml` 的 `ChatFormat` 四個聊天格式，保留 team chat 的灰色括號 / 綠色粗體 `隊伍聊天` / 綠色 `{player}: {msg}`、player chat 的灰色括號 / aqua `玩家` / 綠色 `{player}` / 白色 `: {msg}`、spectator chat 的 gray 整行，以及 staff chat 的 red bold `[管理員]` / yellow bold `{player}: {msg}`。`{msg}` 是玩家輸入且 team chat command 會保留 legacy `&` 色碼；`RoleChat#replace(...)` 與 `ChatCommand#sendTeamChat(...)` 已改用既有 `PluginText.replaceToString(...)`，避免 mixed MiniMessage / legacy template，不新增聊天格式 registry 或 DSL。

補充進度（2026-05-19）：Step 24 第八十六個程式碼切片已轉換 `messages.yml` 的 `Console` 七個預設訊息，保留 `生態域轉換系統` / `遊戲世界` 前綴、biome 成功 green placeholder、biome 失敗 red / dark red、chunk 載入 green / white、nether 偵測 yellow bold 與 force nether gold bold。`PluginConsole` 已改用既有 `PluginText.toComponent(...)` / `toLegacyString(...)` 輸出 legacy console string，讓 console 可接受 MiniMessage resource；`ChunkPregenerationService` 的 `{world}` / `{number}` replacement 已改用既有 `PluginText.replaceToString(...)`。本刀不新增 console message registry 或 YAML DSL。

補充進度（2026-05-19）：Step 24 第八十七個程式碼切片已轉換 `messages.yml` 的 `Dependency.Require_Dependency` 與 `Dependency.Require_Soft_Dependency`，保留 red 主文、white `{plugin}`、aqua `{url}` 以及原本的硬依賴停用 / 軟依賴功能不可用語意。`Require_Soft_Dependency` 的兩個使用點（`/reconnect`、Discord 公告設定）已改用既有 `PluginText.replaceToString(...)` 處理 placeholder；不新增 dependency message helper，也不改 DiscordSRV hook 判斷流程。

補充進度（2026-05-19）：Step 24 第八十八個程式碼切片已轉換 `messages.yml` 的 `Motd` 五個 server list MOTD 訊息，保留 configuring / generating 的 dark aqua、waiting 的 green `開放入場` / gray 括號與 slash / white `{online}` / gray `{max}`、starting 的 dark green，以及 playing 的 red 主文 / white `{remaining}`。`MotdListener` 既有 `PluginText.toComponent(...)` 已能處理 MiniMessage；`PreparingMotdListener` 與 `PlayingMotdListener` 已改用既有 `PluginText.replaceToString(...)` 處理 placeholder，不新增 MOTD adapter。

補充進度（2026-05-19）：Step 24 第八十九個程式碼切片已轉換 `messages.yml` 的 `DiscordVoice` 五個預設訊息，保留 gray 括號 / aqua bold `Discord語音` 前綴、red 失敗訊息、yellow moved 訊息與 gray reconnecting 訊息。`Moved` 的 `{channel}` 來源是 Discord voice channel name，`DiscordVoiceHook` 已改用既有 `PluginText.replaceToString(...)` 處理 placeholder，避免 channel name 的 angle text 被解析成 MiniMessage tag；不新增 Discord voice message helper。

補充進度（2026-05-19）：Step 24 第九十個程式碼切片已轉換 `messages.yml` 的 `Updater.Checking_Updates`、`Updater.Up_To_Date` 與 `Updater.Success`，保留 checking gray、up-to-date green、success dark gray strikethrough 框線、gold bold 主文與 `{fancy-time}` placeholder。`Messages.Updater` 目前沒有實際使用點；本刀只做預設 resource 格式遷移，不新增 updater presenter 或補不存在的更新流程。

補充進度（2026-05-19）：Step 24 第九十一個程式碼切片已轉換 `messages.yml` 的 `Updater.Failed` 三組預設訊息，保留 dark red strikethrough 框線、Internet red bold 主文與 gold 編號檢查項、IO / File not found red bold 主文、gold 回報文字、aqua `{link}`、red 錯誤訊息 label 與 gray `{exception}`。`Messages.Updater.Failed` 目前沒有實際使用點；本刀只做預設 resource 格式遷移。至此 `messages.yml` default resource 已無 legacy `&` / `§` 色碼，剩餘 legacy 命中集中在 `gui.yml`。

補充進度（2026-05-21）：resource cleanup 已移除 `messages.yml` 中不再被讀取的 `Error`、`Kick.Thanks_For_Playing`、`Game.World_Border_Reached`、`Game.Closing_Server_Msg`、`Console.Biome_Replaced`、`Console.Biome_Not_Exist`、`Console.Can_Not_Replace_Biome`、`Dependency.Require_Dependency`。其中需要載入欄位的項目已同步移除 `Messages.java` public static 欄位，避免缺 key 造成啟動失敗；`Game.PlayerDeath` 動態死亡訊息、MOTD、DiscordVoice、chunk loading console 訊息與 soft dependency 訊息保留。

補充進度（2026-05-19）：Step 24 第九十二個程式碼切片已轉換 `gui.yml` 最上方的通用 `Leave`、`Next_Page`、`First_Page`、`Previous_Page`、`Last_Page`，保留 leave red / gray、next white `{page}` 與 dark gray `>>`、first/last gray、previous dark gray `<<` 與 white `{page}`。`PluginItems` 既有 name/lore 建構路徑已能讀 MiniMessage；另補 `PluginTextTest#toComponentKeepsPaginationArrowsLiteralInsideMiniMessage` 覆蓋 literal `<<` 邊界，不新增 GUI registry 或 YAML DSL。後續 resource cleanup 已確認 `Next_Page` / `First_Page` / `Previous_Page` / `Last_Page` 不再被 `PluginPagedMenu` 讀取並移除，`Leave` 保留給子設定頁返回按鈕；分頁 / 動態模板按鈕中不再被讀取的 `Slot: -1` placeholder 也已移除。

補充進度（2026-05-19）：Step 24 第九十三個程式碼切片已轉換 `gui.yml` 的 `Main.Title` 與前段 `Players`、`Whitelist`、`Generate_Map`、`Start`，保留 title dark green bold、players green / gray / yellow、whitelist aqua / gray 與 `{status}`、generate map gold bold / gray、start green bold / gray。`{status}` 值仍由 `MainSettingsMenu` 既有 `&aOn` / `&cOff` 提供，透過既有 `PluginText.replaceToString(...)` 轉入 MiniMessage template；不改狀態常數或 menu flow。

補充進度（2026-05-19）：Step 24 第九十四個程式碼切片已轉換 `gui.yml` 的 `Main.Buttons` 導覽按鈕 `Scenarios`、`Team`、`Border`、`Time`，保留四個按鈕名稱 gold、lore gray。這些按鈕沒有 placeholder 或特殊互動文字；本刀只做 resource 格式遷移，不改 `MainSettingsMenu` slot dispatch 或開啟子選單流程。

補充進度（2026-05-19）：Step 24 第九十五個程式碼切片已轉換 `gui.yml` 的 `Main.Buttons` 背包 / 掉落 / 禁用物品編輯入口 `Custom_Inventory`、`Custom_Drops`、`Disable_Items`、`Practice_Inventory`，保留四個按鈕名稱 yellow、說明 lore gray、`點擊來設定` lore yellow。本刀不改 `InventorySaver`、`InventoryEditSession` 或物品序列化邏輯。

補充進度（2026-05-19）：Step 24 第九十六個程式碼切片已轉換 `gui.yml` 的 `Main.Buttons` 數值 / 開關按鈕 `Apple_Rate`、`Experience`、`Nether`、`Ender_Pearl_Damage`，保留 green 名稱、gray label、green `{count}`、yellow 左鍵 / 右鍵操作提示，以及 toggle 的 gray 說明與 `{status}` placeholder。`{status}` 仍由既有 `&aOn` / `&cOff` 提供並由 `PluginText.replaceToString(...)` 轉入 MiniMessage template；不改加減數值、toggle 或設定儲存流程。

補充進度（2026-05-19）：Step 24 第九十七個程式碼切片已轉換 `gui.yml` 的 `Main.Buttons` 剩餘工具入口 `Saves`、`Scoreboard`、`Title`、`Broadcast`，保留 Saves gold / gray / aqua / yellow、Scoreboard / Title / Broadcast light purple、Title gray label / green `{title}`、點擊提示 yellow。`{title}` 來源可能帶 legacy 顏色，仍由既有 `PluginText.replaceToString(...)` 轉入 MiniMessage template；不改標題輸入、公告、計分板或模板流程。

補充進度（2026-05-19）：Step 24 第九十八個程式碼切片已轉換 `gui.yml` 的 `Teams` 區段，保留標題 dark green + bold、Size green / gray / yellow、Team_Fire red / gray / `{status}`、Team_Split_Mode yellow / gray / green / aqua / white `>`。`{status}` 仍由既有 `&aOn` / `&cOff` 提供並由 `PluginText.replaceToString(...)` 轉入 MiniMessage template；不改隊伍大小、同隊傷害 toggle 或分隊模式切換流程。

補充進度（2026-05-19）：Step 24 第九十九個程式碼切片已轉換 `gui.yml` 的 `Border` 基礎設定 `Title`、`Size`、`Nether_Size`、`Border_Type`，保留標題 dark green + bold、Size / Nether_Size green / gray / yellow、Border_Type yellow / gray / green / aqua / white `>`。`{type}` 仍由 `BorderType.fancyName()` 提供純文字並套用 green；不改邊界大小輸入、地獄邊界大小輸入或邊界模式切換流程。

補充進度（2026-05-19）：Step 24 第一百個程式碼切片已轉換 `gui.yml` 的 `Border` 剩餘收縮設定 `Final_Size_Of_Shrink_Mode_Border`、`Border_Shrink_Speed`、`Shrink_Calculator`，保留 yellow / gray / green / red / gold 的原語意，以及 `{number}`、`{fancy-time}` placeholders。`{fancy-time}` 仍由既有 `PluginText.replaceTimeToString(...)` 提供純文字並套用 green；不改最終邊界、收縮速度或計算器輸入流程。

補充進度（2026-05-19）：Step 24 第一百零一個程式碼切片已轉換 `gui.yml` 的 `Times` 區段，保留標題 dark green + bold、Damage / Border_Shrink / Disable_Nether red、Final_Heal light purple、Pvp gold、時間 label gray、`{time}` green 與點擊提示 yellow。`{time}` 仍由既有 `PluginText.formatTime(...)` 提供純文字並套用 green；不改時間輸入、儲存或聊天提示流程。

補充進度（2026-05-19）：Step 24 第一百零二個程式碼切片已修正 `PluginPagedMenu#getTitle()` 的多頁頁碼 suffix，避免分頁 GUI 標題轉成 MiniMessage 後與舊 `&8` 頁碼混用而被 legacy parser 優先處理。做法只在多頁時將原標題轉成 `Component`、append dark gray 頁碼，再序列化回 MiniMessage string；不改分頁邏輯、頁碼位置、上一頁 / 下一頁 item，也不新增 message registry。

補充進度（2026-05-19）：Step 24 第一百零三個程式碼切片已轉換 `gui.yml` 的 `Scenarios` 與 `Enabled_Scenarios`，保留標題 dark green + bold、清除模式 red、清除說明 gray。Scenario 動態 item 仍由既有 scenario icon / fancy name / enabled 狀態流程產生；不改 scenario toggle、清除模式、分頁邏輯或 scenario resource。

補充進度（2026-05-19）：Step 24 第一百零四個程式碼切片已轉換 `gui.yml` 的 `Saves` 區段，保留標題 dark green + bold、Save_As green / gray / yellow、Saved.Name reset + white、preview 分隔線 white + strikethrough、主設定 green / aqua / gray、次設定 yellow / gray、左右鍵 green / red + gray `>`。同刀只把 `SavedSettingsMenu#convertToItemStack(...)` 的 saved item name 由直接 `.replace(...)` 改為 `PluginText.replaceToString(...)`，讓 `{saved_game_title}` 在 MiniMessage template 中仍可安全吃 legacy title；不改模板儲存、載入、覆蓋、刪除或分頁流程。

補充進度（2026-05-19）：Step 24 第一百零五個程式碼切片已轉換 `gui.yml` 的 `Scoreboard` 區段，保留標題 dark green + bold、Themes yellow / gray / green、Update_Ticks light purple / gray / green / yellow、Heart_Color green / gray / 依 `{color}` 套用愛心顏色。`ScoreboardSettingsMenu` 對 Heart_Color 的 `{color}` 改傳 MiniMessage 色名，只供 GUI template 建立 `<{color}>❤</{color}>` 使用；實際 scoreboard heart color 儲存與 `PluginColor#toString()` 的 legacy 輸出不變。不改 scoreboard 更新頻率、風格選擇、ColorPicker 或玩家頭頂血量邏輯。

補充進度（2026-05-19）：Step 24 第一百零六個程式碼切片已轉換 `gui.yml` 的 `Sidebar_Theme_Selector` 固定文字，保留標題 dark green + bold、theme item yellow、`{theme_name}`、preview label yellow、`{theme_preview}` placeholder 與點擊提示 yellow；不改 `SidebarThemeSettingsMenu`、theme preview 來源、分頁邏輯或 scoreboard theme 設定流程。

補充進度（2026-05-19）：Step 24 第一百零七個程式碼切片已轉換 `gui.yml` 的 `Broadcast` 區段固定文字，保留標題 dark green + bold、Discord 按鈕 aqua、說明 gray；不改公告發送流程、DiscordSRV 整合或公告內容格式。

補充進度（2026-05-19）：Step 24 第一百零八個程式碼切片已轉換 `gui.yml` 的 `Team_Selector` 區段，保留標題 dark green + bold、Available green / gray / yellow、Full dark red + bold、Create_Your_Own yellow + bold / gray、`{name}` / `{character}` reset + white、`{players}` placeholder。`TeamSelectorMenu` 對 `{color}` 改傳 MiniMessage 色名，只供 GUI template 建立 `<{color}>█</{color}>` 使用；不改隊伍列表來源、自由加入篩選、加入隊伍或建立隊伍流程。

補充進度（2026-05-19）：Step 24 第一百零九個程式碼切片已轉換 `gui.yml` 的 `Team_Settings` 區段，保留標題 dark green + bold、Name green / gray / yellow、Color aqua / gray / 動態顏色方塊、Character gold + gray / green / yellow、Open_Join gold / gray / dark purple / `{status}` / yellow、Help aqua / gray。`TeamSettingsMenu` 對 `{color}` 改傳 MiniMessage 色名，只供 GUI template 建立 `<{color}>█</{color}>` 使用；不改隊伍名稱輸入、徽章輸入、權限檢查、ColorPicker、`/team public` 或 help command 流程。

補充進度（2026-05-19）：Step 24 第一百一十個程式碼切片已轉換 `gui.yml` 三個單純清單標題 `Players_Overworld.Title`、`Players_Nether.Title`、`Disable_Item_List.Title`，皆保留 dark green + bold；不改玩家列表、地獄玩家列表、禁用物品列表的資料來源或分頁流程。

補充進度（2026-05-19）：Step 24 第一百一十一個程式碼切片已轉換 `gui.yml` 的 `Stats` 區段，保留標題 green + bold、Played / Wins / Kills / Kdr label gray、數值 placeholder yellow、所有 lore gray。`{played}`、`{wins}`、`{kills}`、`{kdr}` 仍由 `StatsMenu` 既有 `getButtonItem(...)` 流程填入；不改統計讀取或 KDR 計算。

補充進度（2026-05-19）：Step 24 第一百一十二個程式碼切片已轉換 `gui.yml` 的 `Staff_Options` 區段，保留標題 dark green + bold、四個 toggle 名稱 gray、狀態 label gray、`{status}` 動態顏色、點擊提示 yellow，以及 Moving_Speed 的 gray 說明、aqua `{count}`、紅 / 綠左右鍵提示。`{status}` 仍由 `StaffOptionsMenu` 既有 `&aOn` / `&cOff` 提供並由 `PluginText.replaceToString(...)` 轉入 MiniMessage template；不改 staff option toggle、挖礦提示、玩家顯示或速度調整流程。

補充進度（2026-05-19）：Step 24 第一百一十三個程式碼切片已轉換 `gui.yml` 的 `Center_Cleaner` 區段固定文字，保留標題 dark green + bold、Agree green / gray / red 說明、Disagree red / gray / red 說明；不改 `CenterCleanerMenu`、預覽世界產生、中心點清理選擇或傳送流程。

補充進度（2026-05-19）：Step 24 第一百一十四個程式碼切片已轉換 `gui.yml` 最後的 legacy GUI 色碼命中，包含 `See_Inventory` 的 title、Health / Hunger / Level 名稱與數值 lore，以及 `Color_Picker.Title`。`{player}` 仍由 `InventoryViewer#getTitle()` 透過 `PluginText.replaceToString(...)` 填入，`{health}`、`{hunger}`、`{level}` 仍由既有資訊 item 流程填入；不改背包內容複製、資訊 item slot 或顏色選擇流程。

補充進度（2026-05-19）：Step 24 第一百一十五個程式碼切片曾補上既有 data folder 的人工 migration 入口；後續因使用者明確要求不保留 legacy 相容層，此工具已在 runtime legacy removal 收尾中移除。Step 24 完成後不再提供 `scripts/migrate-message-format.sh`、Gradle `messageFormatMigration` task、`MessageFormatMigration` 或 `MessageFormatMigrationCli`。

補充進度（2026-05-19）：Step 24 後續清理已建立 `step-24-runtime-legacy-removal` 分支並補充 `docs/message-format-migration.md` 的 runtime legacy fallback removal 盤點。此後續清理的完成標準已依使用者要求調整為：`PluginText.toComponent(...)` 不再把 legacy `&` / `§` 當作正式訊息格式解析；不保留 `LegacyComponentSerializer`、`MessageFormatMigration`、legacy migration script 或 legacy string bridge；scoreboard / console / conversation / Golden Head 判斷均改走 Component、MiniMessage 或 plain text；剩餘 Java inline legacy 字串需小刀轉成 MiniMessage 或 plain text，不建立新的 message registry 或 command/menu framework。

補充進度（2026-05-19）：Step 24 後續清理已完成不保留相容層的 runtime legacy removal。`PluginText.toComponent(...)` 已移除 legacy `&` / `§` parser branch，含 legacy code 的字串會保持 literal；`PluginText.toLegacyString(...)` / `toLegacyAmpersandString(...)`、`LegacyComponentSerializer`、`MessageFormatMigration`、`MessageFormatMigrationCli`、`scripts/migrate-message-format.sh` 與 Gradle `messageFormatMigration` task 已移除；Java inline legacy 訊息已小刀轉成 MiniMessage 或明確 `Component` / plain text。完成驗證包含 `PluginTextTest` / `MatchSettingsTest`、完整 `./gradlew test`、legacy 搜尋 gate、`scripts/package-plugin-1.21.sh` 封裝、`scripts/deploy-to-windows-server.sh --skip-build` 部署，以及 Paper `1.21.11` `start.bat` 啟動、`uhc reload`、`stop`。

優先修改或新增：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/platform/text/PluginText.java`
- `Update-WonderlandUHC/src/test/java/org/mcwonderland/uhc/platform/text/PluginTextTest.java`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/items.yml`
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`
- `Update-WonderlandUHC/src/main/resources/commands.yml`
- `Update-WonderlandUHC/src/main/resources/broadcasts.yml`
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`
- `Update-WonderlandUHC/docs/message-format-migration.md`
- `Update-WonderlandUHC/README.md`
- `DEVELOPMENT.md`

要做的事：

1. 先做只讀盤點：列出所有 `&` / `§` 色碼、placeholder、換行、click/hover text、scoreboard line 與 item lore 的格式需求；不要直接批次替換。
2. 決定新格式策略：本步驟採 MiniMessage-only，runtime parser 不再接受 legacy `&` / `§`。
3. 既有設定處理方式：不提供自動 migrator；實際伺服器 data folder 以重置 resource 或人工改成 MiniMessage 為準。
4. 將 repo 內預設 YAML 從 legacy 色碼轉成新格式，保留所有玩家可見文字、placeholder 名稱、換行與顏色語意。
5. 更新 `PluginText` 的解析入口，使用正式 parser（例如 Adventure MiniMessage），不得手寫一套不完整的 markup parser。
6. 補測試覆蓋 legacy input 保持 literal、MiniMessage parse、placeholder replacement、literal `<` / `>`、scoreboard line、item lore 與 click/hover text。
7. 針對既有伺服器資料夾測試正式 YAML：確認 root data folder 無 legacy `&` / `§` 色碼；停用或不用的舊 YAML 不留在正式 data folder。
8. 更新 README / DEVELOPMENT，明確說明推薦格式、舊格式是否仍可讀、如何遷移，以及後續新增訊息時應使用哪種格式。

不得做的事：

1. 不用單純 `.replace("&", ...)` 之類的字串替換假裝完成 migration。
2. 不在沒有備份與 fallback 說明的情況下改寫使用者既有設定檔。
3. 不把 command、menu、scoreboard、item 或 scenario 的業務流程混入本步驟。
4. 不為了「刪掉 Legacy 字樣」而讓既有顏色、placeholder、換行或 scoreboard 顯示發生未接受差異。

完成條件：

- `LegacyComponentSerializer`、`PluginText.toLegacyString(...)`、`PluginText.toLegacyAmpersandString(...)`、legacy migration tool、legacy parser / serializer bridge 均不存在於 production / test source 與建置腳本。
- repo 預設 resources 不再使用 legacy `&` / `§` 色碼作為訊息格式；若有 literal `&` / `§`，必須是文件化的文字內容而非格式控制。
- 伺服器正式 data folder root YAML 不再使用 legacy `&` / `§` 色碼；舊設定不保留相容讀取。
- README / DEVELOPMENT 已說明新訊息格式與未來新增訊息的維護規則，且不再指向 legacy migration script。
- 程式碼變更後依專案規則完成封裝、Paper `1.21.11` startup、`/uhc reload` 與訊息顯示抽樣測試。

## 25. Runtime Legacy Residue Cleanup：清除剩餘 legacy facade / bridge

Step 24 已清除訊息格式 legacy，但專案內仍可能存在非訊息格式的 legacy 殘留，例如舊 static facade、舊狀態轉換橋接、舊資料格式讀取相容、或只因過渡期命名而存在的 `Legacy*` 類別。本步驟只處理這些「升級主線還會繼續拖住未來更新」的殘留，不回頭修改 Step 24 的 MiniMessage 格式遷移，也不新增大型抽象。

優先盤點：

- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/LegacyGameStateTransitions.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/CacheSaver.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/game/settings/UHCGameSettingsSaver.java`
- `Update-WonderlandUHC/src/main/java/org/mcwonderland/uhc/**/*.java`
- `Update-WonderlandUHC/src/test/java/**/*.java`
- `Update-WonderlandUHC/docs/steps.md`

要做的事：

1. 先做只讀盤點：列出 production / test source 內所有 `legacy`、`compat`、`deprecated`、舊 facade、舊 enum / state mapper、舊資料格式 fallback 與只為過渡存在的 wrapper。
2. 對每個殘留分類為「可直接刪除」、「可改名為正式 service / adapter」、「仍被流程依賴，需先替換呼叫點」、「只是歷史文件文字」。
3. 先處理小型 facade：若只是為舊呼叫點保留的 static wrapper，改呼叫點直接使用正式 store / service，再刪 wrapper。
4. 再處理狀態橋接：若 `LegacyGameStateTransitions` 仍存在，先確認 `Game` 舊狀態與 Step 10 use case 的關係，再以正式 match transition flow 取代；不得只改名掩蓋舊依賴。
5. 檢查 config / color / material / state 讀取路徑是否仍接受舊格式；若 Step 24 / Step 21 已明確決定不保留相容，需移除讀取 fallback 或改成明確錯誤。
6. 更新文件，把「legacy 清理已完成」和「仍屬歷史紀錄」分開，避免最終驗收把歷史文字誤判成 runtime legacy。

不得做的事：

1. 不用搜尋到 `Legacy` 就直接刪檔；必須先確認呼叫點與行為。
2. 不把剩餘 facade 包成新的 facade；能直接用正式 service / store 就直接接。
3. 不在本步驟重寫核心比賽流程、設定系統或 scoreboard，只移除已明確可替換的過渡層。
4. 不為了「legacy 字樣歸零」改變玩家可見行為、資料保存語意或 Step 24 訊息格式結果。

完成條件：

- `rg -n "(?i)\\blegacy\\b|compat|deprecated" src/main/java src/test/java build.gradle scripts` 的 production / test 命中已清空，或只剩明確文件化且非 runtime 的例外。
- `CacheSaver`、`UHCGameSettingsSaver`、`LegacyGameStateTransitions` 已刪除、改名為正式實作，或有明確不可刪原因與後續阻塞項。
- Step 24 訊息格式 legacy removal gate 仍維持通過。
- 程式碼變更後依專案規則完成封裝、Paper `1.21.11` startup、`/uhc reload` 與 log gate。

補充進度（2026-05-19）：Step 25 runtime legacy residue cleanup 已完成，詳見 `docs/step-25-runtime-legacy-inventory.md`。`UHCGameSettingsSaver` 已改成正式 `SavedGameSettingsCache`，`CacheSaver` 已改成正式 `WorldLoadingCacheState`，`LegacyGameStateTransitions` 已由 `MatchTransition.fromSourceState(...)` 取代，`LegacyMatchSettingsMapper` 已改成 `MatchSettingsMapper`，unused `LegacyMatchStateMapper` 已刪除。material / sound alias 與 team prefix section-code 行為保留，但命名已轉成正式 alias / section-code support，不再視為 runtime legacy facade；unused `update` package、`Messages.Updater`、`Settings.OldEnchant`、預設 updater / OldEnchant resource 與 CenterCleaner 未讀取 pass/fail 欄位已移除。完成驗證包含 source/resource 搜尋 gate、`./gradlew test --no-daemon`、`scripts/package-plugin-1.21.sh`、`scripts/deploy-to-windows-server.sh --skip-build`、Paper `1.21.11` `start.bat` 啟動、`uhc reload`、`stop`、latest.log / plugin error.log / port gate。

## 26. 最終驗收：測試策略與 1.16 / 1.21 功能對照

Step 21 移除 Foundation / NMS 相容層、完成 Step 22 Login Gate Migration、在 Step 23 收斂舊色碼 / message format boundary、在 Step 24 完成下一代訊息格式 migration，並在 Step 25 清除剩餘 runtime legacy facade / bridge 後，必須用同一份 checklist 驗證升級後行為，而不是只靠人工印象或單次開服。這一步是發布前最終驗收，不再承擔新的 legacy 拔除、登入架構遷移或訊息格式 migration 工作；若驗收發現 regression，回到對應實作層修正後重新驗收。

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
2. 對同一份 checklist 分別在 Paper `1.16.5` 與 Step 25 完成後的 Paper `1.21.11` 執行，至少覆蓋：開服啟動、主要 command、host 設定、玩家加入、登入 gate（白名單、等待 host、滿員、遊戲中加入、bypass 權限）、隊伍、scenario、倒數、傳送、開局、死亡、觀戰、復活、掉落、經驗、邊界、世界生成、勝負判斷、結束流程、GUI、scoreboard、optional integrations。
3. 每個項目必須分類為「一致」、「可接受差異」、「版本限制造成的差異」、「升級造成 regression」、「原版既有問題」。
4. 所有「升級造成 regression」必須修正，或取得明確接受紀錄後才能進入發布步驟。
5. 對 Step 5 到 Step 25 曾改動或降級的 legacy/platform/integration 行為要特別標註，例如 absorption、armor points、pickup exp control、death animation、custom exp orb、fast block set、large chest merge、WorldBorder、Packet、custom-ore-generator、Foundation / DatouNMS 移除、登入 gate、team color、message format、runtime legacy facade。1.7 舊附魔模擬要標成「委託人已接受正式移除」，不要列為 regression。

完成條件：

- core/application 不需要啟動 Bukkit 就能測。
- Paper smoke test 可驗證插件啟動與最小遊戲流程。
- Step 21 legacy 移除、Step 22 login gate migration、Step 23 message format boundary 收斂、Step 24 message format modernization 與 Step 25 runtime legacy residue cleanup 後，1.16.5 / 1.21.11 功能對照檢核已完成，所有 regression 都已修正或被明確接受。
- A15 搜尋檢查、A16 最終驗收清單與發布前手動 checklist 都已通過。

## 27. 發布與文件

最後更新使用者文件與開發文件。此步驟必須在 Step 21 的最終依賴狀態、Step 22 的 login gate dependency / 行為策略、Step 23 的訊息格式邊界、Step 24 的訊息格式現代化結果、Step 25 的 runtime legacy residue cleanup 與 Step 26 的最終驗收結果確定後才做，避免發布文件描述和實際 runtime dependency 不一致。

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
6. 發布前確認 Step 21 相容層移除、Step 22 Login Gate Migration、Step 23 Message Format Boundary、Step 24 Message Format Modernization、Step 25 Runtime Legacy Residue Cleanup 與 Step 26 最終新舊版對照驗收都已通過。

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
22. 專門重做 login gate，移除 `PlayerLoginEvent` 依賴並決定 LuckPerms / permission 查詢策略。
23. 收斂舊色碼與訊息格式相容層，集中 `LegacyComponentSerializer` 使用邊界。
24. 將訊息格式現代化，若決定完全移除 legacy config format，需完成 MiniMessage / 新格式 migration。
25. 清除剩餘 runtime legacy facade / bridge。
26. 補測試與 1.16.5 / 1.21.11 最終功能對照檢核。
27. 發布與文件更新。

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

- `Update-WonderlandUHC/src/main/resources/biomes.yml`（已確認只剩 legacy resource 抽出，已移除，未來不得補 migration）
- `Update-WonderlandUHC/src/main/resources/broadcasts.yml`
- `Update-WonderlandUHC/src/main/resources/cache.db`（0 bytes 空白預設檔已移除；runtime `plugins/WonderlandUHC/cache.db` 仍由 `WorldLoadingCacheStore` 保存 / 載入，不能刪除現場資料）
- `Update-WonderlandUHC/src/main/resources/commands.yml`
- `Update-WonderlandUHC/src/main/resources/gamecache.db`（已確認舊版與新版皆未被程式引用，已移除，未來不得補 migration）
- `Update-WonderlandUHC/src/main/resources/gui.yml`
- `Update-WonderlandUHC/src/main/resources/items.yml`
- `Update-WonderlandUHC/src/main/resources/messages.yml`
- `Update-WonderlandUHC/src/main/resources/permissions.txt`（已確認只是重複權限說明文件；2026-05-21 已移除，權限宣告以 `plugin.yml` 為準）
- `Update-WonderlandUHC/src/main/resources/plugin.yml`
- `Update-WonderlandUHC/src/main/resources/savedgames.db`（0 bytes 空白預設檔已移除；runtime `plugins/WonderlandUHC/savedgames.db` 仍由 `SavedGameSettingsStore` 保存 / 載入，不能當 legacy 資料刪除）
- `Update-WonderlandUHC/src/main/resources/scenarios.yml`（仍是 runtime scenario 設定檔，不能刪除；2026-05-21 已將預設檔內 scenario 專用音效改為 1.21 原生 sound 名稱，並移除 scenario material 舊名 alias）
- `Update-WonderlandUHC/src/main/resources/scoreboards.yml`
- `Update-WonderlandUHC/src/main/resources/settings.yml`
- `Update-WonderlandUHC/src/main/resources/sounds.yml`（仍是 command / host / game / countdown / tutorial 音效設定檔，不能刪除；2026-05-21 已將預設檔改為 1.21 原生 Bukkit sound 名稱，並移除 `SoundConfigParser` 的舊 sound alias 轉換）
- `Update-WonderlandUHC/src/main/resources/spawns.yml`（仍是大廳重生點 runtime data 設定檔，由 `/setspawn` 寫回 `Lobby`，不能刪除；測試服現場值屬本機資料，不應用模板覆蓋）
- `Update-WonderlandUHC/src/main/resources/stats.yml`（0 bytes 空白預設檔已移除；runtime `plugins/WonderlandUHC/stats.yml` 仍由 `StatsStorageYaml` 保存 / 載入，不能刪除現場資料）

處理方向：

1. 先備份舊設定，再跑完整 migration；這是後續 migration 步驟，不是 Step 8 完成條件。
2. material、sound、entity、potion、enchantment 名稱全部檢查；1.7 舊附魔模擬本身已接受移除，但設定中可能仍有一般 enchantment key，需要跟物品/GUI 設定一起檢查。舊 `biomes.yml` 已移除，不再列入 migration。
3. 舊 DB 形態若只是預設資源，確認是否仍應打包進 jar；只有會影響切階段、重啟接續、主持 preset 或當局 stats 的資料需要保留。
4. `gui.yml`、`items.yml` 的 alias 寫回與 menu button material 更新，阻擋核心主持入口的部分先在 Step 11 處理，核心 menu resource 收斂在 Step 17；`sounds.yml` 已於 2026-05-21 改為 1.21 原生 Bukkit sound 名稱。Step 18 只處理驗收中實際造成非核心 presentation 失效的 resource 問題，其餘全量 migration 留後續步驟。
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
