# Step 9 Center Cleaner 獨立實作規劃

本文件只規劃 Step 9 中的 CenterCleaner / biome 篩選與中心選址工作，不合併進 `docs/steps.md`，避免主步驟文件繼續膨脹。

依據來源：

- 工作區根目錄 `center-cleaner-design.md`
- `docs/steps.md` 的 Step 9 世界、邊界、傳送與地形服務段落
- 目前實作中的 `CenterCleaner`、`CenterCleanerMenu`、`BorderService`、`ChunkPregenerationService`、`BorderUtil`、`UHCWorldUtils`

## 1. 委託需求整理

委託人的核心要求不是重做一個舊版 biome 白名單，也不是清除或覆蓋地形，而是在固定成本內從同一張世界中選出較適合 UHC 的 `MatchCenter(x,z)`。

必須保留的原則：

- 使用單一世界與單一 seed。
- 不自動換 seed。
- 不自動刪世界重來。
- 不修改地形。
- 不覆蓋 biome。
- 不把 `0,0` 當成唯一中心。
- 搜尋必須有候選數、sample 數與時間上限。
- 找不到高分中心時，仍要回傳目前最佳結果與不推薦原因。
- biome 只作為分類、扣分、排除或權重訊號，不作為舊式 pass/fail 白名單。
- 結果必須能交給 WorldBorder 與後續預生成流程使用。

明確不做：

- 不移除外部 WorldBorder / Chunky 預生成流程。
- 不重做完整 chunk pregeneration。
- 不做地形修復、森林清除、填海、削山或 biome overwrite。
- 不分析村莊、結構、礦物分布或玩家出生點最佳化。
- 不做複雜 GUI。

## 2. 原始實作差距

Step 9 開始前，`CenterCleaner` 仍是舊流程：

- 建立世界前會刪除既有 UHC 世界。
- 若主持人啟用 CenterCleaner，只掃 `0,0` 周圍固定 `Range`。
- biome 判斷依賴舊 enum 名稱與 `Biome.valueOf(...)`。
- 河流、高度與 bad biome 仍是直接 pass/fail。
- 成功後主持人仍被傳送到 `UHCWorldUtils.getZeroZero()`。
- 找到不合格條件時只廣播訊息，沒有候選分數、原因集合或可接受的 fallback 結果。

Step 9 開始前，世界與邊界相關現況：

- `BorderService#setExactFixedBorder` 與 `BorderService#shrinkBorder` 都硬設 WorldBorder center 為 `0,0`。
- `UHCWorldUtils#getZeroZero()` 是多處傳送中心的共同入口，但只回傳 `(0, 100, 0)`。
- `ChunkPregenerationService` 目前依照既有邊界大小跑預生成，尚未接收非 `0,0` 的中心資料。
- `BorderUtil#isInBorder(Location, int)` 仍以原點半徑判斷。
- `biomes.yml` 是 biome 覆蓋設定資源，但目前不應被升格為新流程核心。

## 3. 新流程目標

第一版要完成的是：

```text
低成本地圖評估器 + 同世界中心選址器
```

流程輸入：

- UHC world。
- 主持人設定的 initial border size。
- 可選 seed。
- 固定搜尋預算與時間上限。

流程輸出：

- `MatchCenter(x,z)`。
- result status。
- total score。
- 分項比例與分數。
- 主要扣分原因。
- 是否建議跑圖。
- 是否因 soft/hard time limit 停止。

結果狀態先維持文件中定義的等級：

- `RECOMMENDED`
- `ACCEPTABLE`
- `POOR`
- `REJECTED`
- `TIME_LIMITED`
- `CANCELLED`

## 4. 建議資料模型

先建立小而明確的 model，不做通用地圖分析框架。

建議放在：

- `src/main/java/org/mcwonderland/uhc/application/world/MatchCenter.java`
- `src/main/java/org/mcwonderland/uhc/application/world/CenterSearchResult.java`
- `src/main/java/org/mcwonderland/uhc/application/world/CenterSearchStatus.java`
- `src/main/java/org/mcwonderland/uhc/application/world/CenterCandidateScore.java`
- `src/main/java/org/mcwonderland/uhc/application/world/CenterScoreReason.java`

第一版欄位只放實際需要的資料：

- center x/z。
- border size。
- total score。
- status。
- ocean ratio。
- water ratio。
- forest ratio。
- dense forest ratio。
- highland ratio。
- extreme highland ratio。
- standable ratio。
- cliff ratio。
- center height spread。
- low section count。
- reasons。
- elapsed time。

避免項目：

- 不建立抽象 `MapQualityPlugin`、`BiomeRuleEngine`、`TerrainAnalyzer` 這類過大名稱。
- 不把每個評分維度做成可插拔 strategy，除非後續真的有多套規則。
- 不設計長期資料庫儲存；第一版只要能在本次生成流程中傳遞結果。

## 5. 實作切片

舊程式碼處理原則：

- 新流程尚未接上前，舊入口與舊行為先保留，避免尚未完成的新流程破壞世界建立。
- 每個 slice 只刪除已被該 slice 明確取代、且沒有其他呼叫點的舊程式碼。
- 若舊類別仍被 menu、command、bootstrap 或 runtime flow 使用，先改成 facade 或相容 wrapper，不直接刪除。
- `biomes.yml`、舊 CenterCleaner config 與舊訊息文字不在本文件第一輪直接刪除；等新流程穩定後再放進 config/resource migration 或 Step 12 類工作。
- 每次刪舊碼前都要用 `rg` 確認引用點，刪除後仍要封裝與開服測試。

### Slice 1：只建立模型與純計算規則

目標：

- 建立 `MatchCenter` 與搜尋結果資料結構。
- 建立 biome 分類工具，使用 1.21.11 namespaced key 或 Bukkit key，不再依賴舊 enum 名稱。
- 建立 score/status/reason 的純計算規則。

舊碼處理時機：

- 不刪任何舊 `CenterCleaner` 程式碼。
- 不刪 `Settings.CenterCleaner`、`messages.yml` 或 `biomes.yml`。
- 不修改 `CenterCleanerMenu`、`RegenWorldCommand` 或世界建立流程。

驗收：

- 可用單元測試驗證比例門檻與 status 分級。
- 不碰世界建立、刪除、預生成與 GUI。

### Slice 2：建立 sample 與候選產生

目標：

- `0,0` 只作為第一候選。
- 產生第一層 9 個候選中心。
- 必要時產生第二層最多 16 個候選。
- offset 使用 initial border 推導，並 round 到 chunk 邊界。
- 建立固定 sample budget，不隨邊界面積平方成長。

舊碼處理時機：

- 不刪舊 biome pass/fail method，因為尚未接上 runtime。
- 可新增新候選/sample 類別，但不把舊 `Range` 掃描改成新候選流程。
- 若發現舊 helper 完全沒有引用，只先記錄，不在這一刀清理。

驗收：

- 候選數不超過 25。
- sample 座標可預測、可測試。
- 不載入或修改地形。

### Slice 3：建立 World sample reader

目標：

- 從 Bukkit `World` 讀取 biome、surface y、sea level、可站立性與鄰近高度差。
- 使用 `world.getMinHeight()` / `world.getMaxHeight()` 支援 1.18+ 高度。
- 建立同一世界座標的 sample cache。

舊碼處理時機：

- 不刪 `CenterCleaner#isValidBiome...`，因為舊 runtime 仍可能呼叫。
- 不修改 `CenterCleaner#createWorld(...)` 的世界刪除與建立行為。
- 若新增 sample reader 已覆蓋高度讀取需求，也先不刪舊高度檢查，等 Slice 5 接線時一起處理。

驗收：

- 能在已載入世界中對固定座標取樣。
- 不逐格掃完整邊界。
- 每 tick 處理量可控。

### Slice 4：建立 CenterValidationService

目標：

- 將候選、粗掃、詳掃、中心精掃串成有限任務。
- 支援 soft time limit、hard time limit、candidate limit、取消與進度回報。
- 輸出目前最佳結果，即使沒有達到推薦分數。

舊碼處理時機：

- 新 `CenterValidationService` 完成後，仍先不替換 `CenterCleaner` runtime。
- 可在測試中證明新服務不需要舊白名單，但舊 private method 等到 Slice 5 接線後再刪。
- 若新增訊息 key，舊 `Messages.CenterCleaner.*` 先保留，避免舊入口缺字串。

驗收：

- 180 秒內必定完成或強制停止。
- 結果包含 score、status、主要扣分原因與是否建議跑圖。
- 不自動刪世界重來，不自動換 seed。

### Slice 5：接回建立世界與主持人流程

目標：

- `CenterCleaner.createWorld(...)` 不再負責完整評分細節，只負責建立世界與啟動中心搜尋。
- `CenterCleanerMenu` 保留主持人入口，但回饋改成搜尋進度與結果。
- 主持人能接受結果、預覽、再搜尋或手動換 seed。

舊碼處理時機：

- 這一刀才是移除舊 `CenterCleaner` pass/fail 掃描的主要時機。
- `CenterCleaner` 類別本身先保留成 facade，因為 menu/command 入口仍依賴它。
- 若 `isValidBiome1_8`、`isValidBiome1_9`、`BIOME_THRESHOLD`、舊河流檢查、舊高度檢查已無引用，應在這一刀刪除。
- `Settings.CenterCleaner.Range`、`Check_River_In`、`Max_High`、`Bad_Biome_Limit` 不再被新流程使用時，先標記為 legacy/deprecated，不直接移除設定檔欄位。
- `Generator_Settings` 不再套到 1.21.11 新流程；若保留，只作 legacy 設定相容，不當成新地形修正手段。

驗收：

- 停用 CenterCleaner 時仍能照舊建立世界。
- 啟用 CenterCleaner 時不再只掃 `0,0`。
- 找到結果後主持人能看到推薦或不推薦原因。

### Slice 6：讓 MatchCenter 影響 WorldBorder 與後續流程

目標：

- `BorderService` 支援指定 `MatchCenter` 設定固定邊界與收縮邊界。
- `ChunkPregenerationService` / pregeneration adapter 使用目前 MatchCenter 的 border center。
- `BorderUtil#isInBorder(...)` 支援非 `0,0` 中心。
- `UHCWorldUtils` 提供目前比賽中心位置，不再只有 `getZeroZero()`。

舊碼處理時機：

- `BorderService` 原本硬設 `0,0` 的方法可改成呼叫帶 `MatchCenter` 的 overload；若外部仍需要舊簽名，保留舊方法並委派到目前中心。
- `BorderUtil#isInBorder(Location, int)` 若仍有呼叫點，先保留相容 overload，再新增以 `MatchCenter` 為基準的判斷。
- `UHCWorldUtils#getZeroZero()` 不在這一刀直接刪除；先新增「目前比賽中心」方法，讓後續 call site 逐步替換。
- 預生成流程確認使用 `MatchCenter` 後，才清掉只服務 `0,0` 的中間 helper。
- Slice 6 保留的舊簽名只允許作為過渡相容入口，不得視為永久 API；每個保留方法都要標明後續刪除條件。
- 當 Slice 6 驗收已證明 WorldBorder、pregeneration、`BorderUtil#isInBorder(...)` 都能透過目前 `MatchCenter` 運作，且 Slice 7 已替換掉對舊原點語意的 runtime 呼叫點後，應刪除舊的 `0,0` 委派方法或改成明確 legacy/test-only 名稱。

驗收：

- WorldBorder center 與選出的 `MatchCenter` 一致。
- 預生成範圍以同一個中心為基準。
- 邊界內判斷不再假設原點。

### Slice 7：逐步替換散落的 `0,0` 使用點

目標：

- 主持人/旁觀者傳送中心。
- 縮圈時邊界外玩家拉回。
- spectator tools。
- `TpUHCWorldCommand`。
- Nether 對應座標；Nether center 應明確由主世界 `MatchCenter / 8` 推導，不再隱性固定為 `0,0`。
- 停用 CenterCleaner 時，主世界中心使用 `World#getSpawnLocation()`，不得退回固定 `0,0`。

舊碼處理時機：

- 這一刀逐一替換所有「回中心」呼叫點，替換前後都要確認舊版行為仍存在。
- 當 `UHCWorldUtils#getZeroZero()` 沒有呼叫點，或只剩明確 legacy 測試用途，才刪除或改名為 legacy。
- `BorderUtil` 舊原點判斷沒有 runtime 呼叫點後才刪除。
- Slice 7 完成時要回查 Slice 6 為相容保留的舊方法；若已沒有正式流程呼叫，必須在同一刀或緊接的 cleanup commit 中刪除，避免新舊中心 API 長期並存造成誤用。
- 若 `Messages.CenterCleaner` 舊失敗訊息已不再被使用，可在這一刀或後續 resource cleanup 中移除；若還要支援舊 config 顯示，先保留。
- `biomes.yml` 是否移除不在這一刀決定，除非已確認打包資源、文件與使用者設定遷移都不再需要它。

驗收：

- 舊版語意保留：所有「回中心」行為仍存在，只是中心改讀 `MatchCenter`。
- 沒有把功能刪掉來避免處理非 `0,0`。
- 若 GUI 或 spectator hotbar 無法操作，先不要把 GUI 修復塞進這一刀；把 spectator tool 與 Nether 開關相關驗收標成 blocked，等介面修復或新增明確測試入口後再驗。
- 在 GUI 修復前，Slice 7 可先用 `/uhc tp`、`/uhc start`、`/border <size>` 驗證中心傳送、散佈與縮圈拉回是否圍繞目前 `MatchCenter`。
- 已知非阻塞問題：遊戲內驗收時發現 `邊界將於 {fancy-time} 後收縮...` 未替換，這是 countdown 訊息顯示問題，不屬於 `MatchCenter` 中心串接；後續處理倒數訊息時再修。
- 若 CenterCleaner 停用，應使用 UHC world spawn 作為 `MatchCenter`；若 CenterCleaner 搜尋沒有可用候選，也優先 fallback 到 world spawn。
- Practice mode 是獨立非 UHC match world，可保留 origin center，但 API 命名必須明確標成 origin/practice-only，避免 UHC runtime 誤用。

## 6. 第一版評分規則

第一版可先照委託文件保守落地，不額外發明複雜規則。

總分 100：

- `waterScore`：28%
- `terrainScore`：22%
- `sectionBalanceScore`：18%
- `centerScore`：18%
- `forestScore`：14%

硬性限制：

- ocean biome 比例過高不可推薦。
- 水面比例過高不可推薦。
- 任一分區水域過高不可推薦。
- 中心區水面過高不可推薦。
- 中心區可站立比例太低不可推薦。
- 中心高度差過大不可推薦。
- 中心斷崖比例過高不可推薦。
- 低分分區太多不可推薦。

這些限制只影響推薦等級，不代表整張圖一定不能回傳；系統仍要回傳目前最佳候選與原因。

## 7. 設定策略

第一版不要把所有數值都做成公開 config。建議先用常數集中在服務內，等人工測試穩定後再決定哪些需要暴露。

可以先固定：

- `targetScore = 78`
- `earlyStopScore = 85`
- `softTimeLimit = 90 秒`
- `hardTimeLimit = 180 秒`
- `candidateLimit = 25`
- `centerRefineLimit = 2`

既有 `Settings.CenterCleaner` 的處理：

- `Range`、`Check_River_In`、`Max_High`、`Bad_Biome_Limit` 是舊演算法設定，第一版不應直接沿用為新演算法核心。
- `Generator_Settings` 在 1.21.11 不應作為地形修正方案。
- `Allow_Bad_Biome` 可暫時只影響是否允許主持人接受非推薦結果，但不應恢復舊 pass/fail 白名單。
- Slice 7 收尾時已確認 `Range`、`Check_River_In`、`Max_High`、`Bad_Biome_Limit` 與 `Allow_Bad_Biome` 沒有 runtime 引用；Java static 欄位已移除，預設設定檔欄位先保留給舊設定相容與後續 resource migration。
- 舊版 pass/fail 失敗訊息已不再被 runtime 使用；Java 欄位與預設 `messages.yml` key 已移除。
- `biomes.yml` 後續已確認只剩預設資源抽出，沒有 runtime 引用；resource migration 已將其從 `UHCFiles` 與 `src/main/resources` 移除。

## 8. 驗證方式

每個程式碼 slice 完成後都需要依照 repo 規則驗證：

- 使用 `scripts/` 內既有腳本封裝插件。
- 使用伺服器資料夾內的 `start.bat` 啟動伺服器。
- 確認插件能啟動到 `Done`。

CenterCleaner 專屬人工驗證：

- 啟用 CenterCleaner 建立世界。
- 確認不會因不理想 biome 自動刪世界重來。
- 確認有限候選數內有結果。
- 確認結果包含分數、狀態、中心 X/Z 與原因。
- 接受結果後確認 WorldBorder center 與 MatchCenter 一致。
- 跑預生成後確認使用同一個中心。

## 9. overthinking 檢查

實作時遇到以下狀況要停止並重新討論：

- 想做完整地圖品質平台，而不是 CenterCleaner 的有限任務。
- 想把 biome 規則做成過度泛用 DSL。
- 想把所有評分權重全部 config 化。
- 想新增資料庫或長期統計。
- 想把 WorldBorder / pregeneration 一次全部重寫。
- 想把玩家出生點、結構、礦物或地形修復納入同一刀。
- 想透過刪除舊功能避開非 `0,0` 中心傳遞問題。

## 10. 建議第一刀

第一刀只做 `MatchCenter` 與純計算 model，不碰 Bukkit world、不碰 GUI、不碰世界建立流程。

原因：

- 可以先固定資料契約，避免後面服務與 UI 各自傳不同格式。
- 純計算測試成本低。
- 不會影響插件啟動。
- 可以先確認委託人的規則被正確理解，再接 Bukkit 實作。

第一刀完成條件：

- `MatchCenter`、`CenterSearchResult`、`CenterSearchStatus`、`CenterCandidateScore` 可編譯。
- 評分門檻有單元測試。
- 沒有改變現有 CenterCleaner 執行流程。
