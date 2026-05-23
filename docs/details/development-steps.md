# WonderlandUHC 1.21.11 升級開發歷程細節說明

整理日期：2026-05-21

這份文件補充 `docs/development-step-summary.md`，用更完整但仍可閱讀的方式說明 Step 0 到 Step 27 各自做了什麼、當時的設計概念，以及每一步留下的結果。

本文件不是逐 commit changelog，也不重新列出舊過程文件路徑。若需要追溯實際修改，仍應以 commit history、目前程式碼、測試紀錄與正式摘要文件交叉判斷。

## 整體路線

這次升級不是單純把 `build.gradle` 改到新版本，而是分階段完成三件事：

1. 先建立能回歸的基線與測試入口。
2. 再把 Paper / Bukkit、Foundation、DatouNMS、外部插件與訊息格式等高耦合點逐步拆開。
3. 最後用實機驗收與文件整理，確認新版可以被發布、維護與交接。

後續維護時，若某個問題能對應到下列 step，應優先回到該 step 的設計概念，而不是重新展開整個升級工程。

## Step 0：建立基線與重構保護網

代表 commit：`f60599c`

### 目的

在真正升級與重構前，先固定「原本能做到什麼」與「之後怎麼證明沒有壞掉」。這一步的重點不是改功能，而是建立可重現的工作方式。

### 實作重點

- 補上基線與開發說明，記錄原本插件、測試服與主要依賴狀態。
- 建立可重現的驗證基準，避免後續修改只靠印象判斷。
- 將 1.16.5 舊版與 1.21.11 新版測試環境的差異先記錄下來。
- 用最小測試取代原本沒有意義的空測試，讓專案至少有基本自動化保護。
- 明確標出高風險依賴，例如 Foundation、DatouNMS、Packet、WorldBorder、custom ore generator、DiscordSRV。

### 概念與結果

Step 0 建立的是後續所有 step 的驗證基準。沒有這一步，後面每次修正都很容易變成「看起來能跑」，但無法判斷是真的修好、還是只是在某個本機狀態下剛好沒出錯。

## Step 1：拆出啟動組裝層

代表 commit：`62b0ceb`、`694f795`、`c7cc54a`

### 目的

把主 plugin class 從「什麼都做」改成「負責啟動與組裝」。原本啟動流程混合設定載入、依賴檢查、listener、command、scenario、scoreboard、practice、Discord hook 等責任，後續很難逐步替換。

### 實作重點

- 建立 `PluginBootstrap`，承接設定載入、resource 建立、dependency report、stats storage、延遲啟動任務等啟動責任。
- 建立 `FeatureRegistry`，集中註冊 listener、command、scenario、scoreboard、practice 與 Discord voice hook。
- 建立 dependency report，讓 console 可以看出外部插件是 available、disabled 或 unavailable。
- 讓 `WonderlandUHC` 主類變薄，只負責啟動 reloadable runtime 與 plugin runtime。
- 修補啟動時 `SimpleReplacer` 等相容問題，避免拆出啟動層後造成舊流程壞掉。

### 概念與結果

這一步沒有立即移除所有硬依賴，而是先讓依賴狀態可觀察。後續 Step 7、Step 21、Step 22 能夠逐步移除舊依賴，前提就是 Step 1 先把啟動責任拆清楚。

## Step 2：定義核心 UHC 模型

代表 commit：`365dbc8`、`0f90576`、`96f55b3`

### 目的

先定義不依賴 Bukkit / Paper 的比賽核心模型，讓 UHC 的狀態、設定與 transition 可以被單元測試理解，而不是完全綁在舊 `Game` singleton 和 listener 內。

### 實作重點

- 建立 `core.match` 相關模型，例如 match state、match settings、active match repository。
- 建立 `MatchSettingsMapper`，把既有 `UHCGameSettings` 轉成核心設定模型。
- 讓舊 `Game` facade 能同步到新的 active match，但不一次替換整個遊戲流程。
- 補上 match state、settings、transition 相關測試。
- 明確限制 Step 2 範圍：只做核心模型與橋接，不重寫死亡、勝負、傳送、scoreboard 或 GUI。

### 概念與結果

Step 2 的重點是「先建立可測的核心語言」。舊 `Game` 仍然是實際運行流程的主要入口，但後續 Step 10 可以開始把 match lifecycle 的關鍵動作收斂成 use case。

## Step 3：建立 Port / Adapter 邊界

代表 commit：`305a31e`、`b080927`

### 目的

把 Paper / Bukkit API 依賴往外推，避免核心流程直接綁死平台細節。這是為了讓版本升級、測試與外部能力替換有明確位置。

### 實作重點

- 建立初期 platform port，例如 scheduler、world、plugin asset、event publisher。
- 建立 Paper adapter，讓 bootstrap 或流程可以透過介面呼叫平台能力。
- 把啟動資源、世界建立、延遲任務等低風險呼叫先接到 adapter。
- 不在這一步強行重寫所有 Bukkit 呼叫，避免把邊界建設擴大成整包重構。

### 概念與結果

Step 3 是鋪路，不是改玩法。它讓後續 Step 7 的世界 / 預生成替換、Step 10 的 use case、Step 21 的 Foundation 移除有地方接線。

## Step 4：升級建置平台到 Java 21 / Paper 1.21.11

代表 commit：`a782ce2`、`369cbcf`、`8f523e4`

### 目的

讓專案真正站到 Java 21 與 Paper `1.21.11` 的建置基線上。升級前如果還停在舊 Java / Spigot API，後續修正都無法驗證是否真的相容新版伺服器。

### 實作重點

- 升級 Gradle wrapper、Shadow plugin、Lombok、Paper API 與 Java toolchain。
- 調整 `plugin.yml` metadata 與 Paper API version。
- 確認專案能在 Java 21 建置線上完成 compile、test 與 shadow jar。
- 讓 CI / 本機封裝路線符合 Java 21。
- 確認 compile 與 shadow jar 能在新版依賴下跑通。

### 概念與結果

Step 4 把「準備升級」變成「已經在新版建置線上收斂」。從這一步開始，後續錯誤不再是舊建置平台造成的假象，而是需要逐一處理的相容問題。

## Step 5：把 Foundation 降級成相容層

代表 commit：`f64fc2e`、`399b6b4`、`75c8585`、`d97d296`、`8faf4ca`

### 目的

Foundation 當時仍深度支撐 command、menu、設定、文字、material 等流程，但不能讓新程式碼繼續擴散對它的依賴。Step 5 的策略是先把 Foundation 降級成可控的過渡邊界。

### 實作重點

- 建立 Foundation 相關 adapter 邊界。
- 將部分 runtime helper、presentation replacer、material helper 透過 adapter 包住。
- 將可直接改用 Bukkit material 的遊戲流程先轉掉。
- 記錄剩餘 Foundation 邊界，避免後續誤以為已完全移除。
- 不一次刪除 Foundation，因為當時仍有大量穩定流程依賴它。

### 概念與結果

Step 5 是「止血」而不是「切除」。它先阻止新功能繼續往 Foundation 靠，並把真正的大型移除留到 Step 21。

## Step 6：隔離 DatouNMS

代表 commit：`e719749`、`2287fd3`、`0307bd2`

### 目的

DatouNMS 是舊版本中最容易在新版 Paper 啟動時造成 class loading 或 NMS API 問題的依賴。Step 6 的目標是先隔離，不讓它散落在主流程。

### 實作重點

- 盤點 DatouNMS 使用範圍。
- 建立 legacy adapter，將 DatouNMS 呼叫集中。
- 將 build 依賴標示成 legacy-only。
- 避免 core、application、listener、menu 等區域直接依賴 NMS。

### 概念與結果

這一步讓 Paper `1.21.11` 相容問題可以被集中處理。DatouNMS 尚未完全移除，但已不再被視為新架構的一部分。

## Step 7：外部整合重評估

代表 commit：`6754529`、`f33e96c`、`487db02`、`c07114a`、`e4c9ec6`

### 目的

重新判斷哪些外部插件是核心流程必須保留，哪些只是舊版本的實作方式。這一步處理的是 Packet、WorldBorder、custom ore generator、DiscordSRV 等外部整合。

### 實作重點

- 移除舊 Packet startup hard gate，避免舊 packet API 阻止插件啟動。
- 將舊 WorldBorder 指令式流程改往新版邊界與預生成架構。
- 移除舊 custom ore generator 整合，因為舊世界高度與插件相容性不適合直接延用到 1.21.11。
- 恢復 DiscordSRV 整合，但改成可選且有明確降級行為。
- 將世界邊界與預生成能力收斂到更明確的 service / adapter。

### 概念與結果

Step 7 的重點不是「少裝幾個插件」，而是讓外部整合不再以舊式 hard dependency 綁住核心 UHC。真正需要保留的能力要保留，但實作來源可以改成 Paper API、Chunky 或明確 adapter。

## Step 8：重整設定與持久化邊界

代表 commit：`5916df1`、`9822a44`、`67a613f`

### 目的

將 cache、saved games、settings 等資料讀寫從舊框架和散落流程中收斂出來，降低升級時資料格式與設定檔載入的風險。

### 實作重點

- 建立或整理世界載入 cache store。
- 整理 saved game settings 的儲存邏輯。
- 將 `UHCGameSettings` 的 map / section 轉換整理成明確流程。
- 避免設定儲存邏輯散在 GUI、command 或舊 utility 中。
- 明確區分預設 resource、運行資料與本場設定快照。

### 概念與結果

Step 8 讓後續 `/uhc choose`、重啟接續、host setting 持久化有可靠資料邊界。這也是 Step 9、Step 10、Step 11 能修世界與主持設定流程的前置。

## Step 9：重做世界、邊界、傳送與中心選擇

代表 commit：`eeadee9`、`12fd0e4`、`e238afa`

### 目的

讓新版不再假設 `0,0` 一定是 UHC 最佳中心，並把世界中心、邊界與預生成流程整理成可理解的 `MatchCenter` 概念。

### 實作重點

- 建立中心候選與評分模型。
- 建立水域、地形、分區、中心品質、森林等維度的候選中心評估。
- 將 `MatchCenter` 接入實際世界流程，讓邊界、傳送、預生成使用同一中心。
- 支援主持人在預覽世界檢查候選中心後，再用 `/uhc choose` 進入正式跑圖。
- 保留人工判斷，不讓評分完全取代主持選圖。

### 概念與結果

Step 9 改變的是地圖中心觀念：中心是本場比賽選出的 `MatchCenter`，不是固定世界原點。選圖細節見 `docs/details/map-selection.md`。

## Step 10：重整狀態機與 Application Use Cases

代表 commit：`704cc34`、`23b22ac`

### 目的

把比賽生命週期的關鍵動作，例如開始比賽、timer tick、死亡判定、結束比賽，逐步收斂成 application use case。

### 實作重點

- 建立 match lifecycle use cases。
- 讓 timer tick、match transition、death handling、end match 等流程有更清楚的應用層入口。
- 修正世界載入已完成時的狀態保留，避免重啟後把 ready world 清掉。
- 保留舊 `Game` / state 流程作為實際運行入口，不一次全面替換。

### 概念與結果

Step 10 是從「只有 core model」往「實際流程可用 use case 表達」前進。它沒有完全重寫遊戲狀態機，但開始把關鍵動作從舊流程中抽出可測邏輯。

## Step 11：核心主持設定入口修復

代表 commit：`df6c0fc`、`fdd4824`

### 目的

在前面完成世界與狀態邊界後，回頭修復主持人實際操作會遇到的設定入口問題，尤其是 GUI 設定與 inventory editor。

### 實作重點

- 修正 host settings menu 的設定保存。
- 確保修改邊界、時間、Nether、scoreboard 等設定時，能正確影響本場設定。
- 修復 inventory editor 的開始物品、練習物品、死亡掉落物、禁止物品等操作。
- 確保需要寫入 cache 的設定有適時保存。

### 概念與結果

這一步承認「架構可編譯」不等於「主持人能用」。Step 11 把主持設定入口恢復成可實際操作的狀態。

## Step 12：Hotbar / Spectator 工具與 Step 9 回測

代表 commit：`1a41d22`

### 目的

驗收 hotbar、spectator、staff 工具，以及 Step 9 改動後的中心 / Nether 相關操作，確認玩家工具沒有因世界中心變更而失效。

### 實作重點

- 驗證 spectator / staff hotbar。
- 驗證傳送中心、隨機傳送、Nether player list 等工具。
- 檢查 Nether portal 與 spectator 流程的相容邊界。
- 將接受的差異與仍需後續處理的項目分開。

### 概念與結果

Step 12 主要是驗收與分類，不是重寫工具系統。它確保前面改世界中心後，觀戰與管理工具仍能在新版流程中成立。

## Step 13：Discord 公告 GUI / Conversation 正式化

代表 commit：`f950f59`

### 目的

把 Discord 公告功能從臨時或不完整入口，回到正式主持 GUI 流程中，並讓 DiscordSRV 缺少或未 ready 時有可理解的錯誤。

### 實作重點

- 修復 `/uhc edit` 中的 Discord 公告入口。
- 整理輸入 IP、入場時間、開場時間的 conversation 流程。
- 將 Discord message formatting 與 delivery failure 處理得更明確。
- 移除臨時入口，避免維護時存在兩套公告流程。

### 概念與結果

Step 13 讓 Discord 公告成為正式 host workflow 的一部分，但仍維持 DiscordSRV optional integration 的定位。

## Step 14：Scenario 盤點與高風險相容層隔離

代表 commit：`233135f`、`7535684`、`399d699`、`b547655`

### 目的

Scenario 數量多、涉及事件多，若一次重寫很容易造成不可驗證。Step 14 先盤點並隔離高風險載入與運行問題，避免單一 scenario 壞掉拖垮整個插件。

### 實作重點

- 隔離 scenario loading，讓載入失敗更可控。
- 強化 scenario runtime 行為。
- 修復 scenario menu 與死亡流程中的阻塞問題。
- 記錄 scenario 驗證結果，區分已修、可接受差異與後續項目。

### 概念與結果

Step 14 的重點是恢復可玩性與可驗證性，而不是建立新的 rule engine。它先讓 scenario 系統在 Paper `1.21.11` 下不會成為整體啟動與遊戲流程的 blocker。

## Step 15：Scenario 規則逐項行為補齊

代表 commit：`78c25bb`、`069d8db`、`82dd504`、`ffc683f`、`55b52e6`

### 目的

在 Step 14 穩住 scenario 系統後，逐項修正單一 scenario 的具體行為差異。

### 實作重點

- 修正 block scenario 掉落行為。
- 穩定 scenario state。
- 現代化 item durability 相關處理。
- 修正或正規化 scenario sound config。
- 記錄單一 scenario 驗收結果。

### 概念與結果

Step 15 用小切片修單一 scenario，不把所有規則混成一次大改。這降低了驗收成本，也讓問題能對應到具體 scenario。

## Step 16：Scenario 混合互動驗收

代表 commit：`2c038cc`、`b56f0ca`、`4788d82`

### 目的

單一 scenario 能用不代表多個 scenario 疊加後仍正確。Step 16 驗證死亡、背包、掉落、block drop 等高風險組合互動。

### 實作重點

- 建立 scenario 組合驗收分類。
- 修正 death inventory 組合問題。
- 修正 block drop 組合差異。
- 確認多個 scenario 同時啟用時不互相覆蓋核心行為。

### 概念與結果

Step 16 把驗證從「功能單點」提升到「玩法組合」。這對 UHC 很重要，因為實際場次常會同時開多個 scenario。

## Step 17：核心 Presentation 入口瘦身

代表 commit：`2e0cbd8`、`760622c`、`2f6c68d`

### 目的

讓指令與 GUI 層變薄，把可共用的主持流程往共用 flow 收斂，但不把本來已經很薄的 command 硬抽成過度抽象。

### 實作重點

- 修正 time placeholder 正規化。
- 將核心 host control flow 做適度共享。
- 記錄 presentation cleanup 完成狀態。
- 明確避免將 thin command / menu 為了架構漂亮而重寫。

### 概念與結果

Step 17 的設計重點是「有共用收益才抽」。這一步也建立了後續面對 overthinking 的判準：可讀、可驗證、降低重複才是理由。

## Step 18：Scoreboard / Practice / 非核心 Presentation 收斂

代表 commit：`0f2b52b`、`457643f`、`9f740f0`、`05f8c24`

### 目的

補齊玩家看得到的顯示與操作問題。底層能跑不代表實際使用體驗完整，scoreboard、practice、玩家選單、item/death mechanics 都需要回測。

### 實作重點

- 建立 validation checklist。
- 修復 staff scoreboard 與 ore alerts。
- 穩定 practice 與玩家選單。
- 修復 item 與 death mechanics。
- 將非核心 presentation 的實際失效點逐項收斂。

### 概念與結果

Step 18 避免專案只完成底層升級，卻忽略玩家在場內看到與操作的部分。這一步補齊的是「實際使用感」。

## Step 19：Bukkit / Paper API 與剩餘相容 Adapter 清理

代表 commit：`6d3478b`、`d554cee`、`d8ddc19`

### 目的

整理 Paper `1.21.11` 下不再合適的 API 用法，替換可以安全替換的舊 API，並把高風險項目分流到後續專門步驟。

### 實作重點

- 替換部分舊運行 API。
- 現代化 text 與 item API。
- 盤點 deprecated API。
- 將登入 gate、team color、metadata、Foundation config 等高風險項目延後到更合適的 step。

### 概念與結果

Step 19 不追求 warning 歸零。它只處理能保持行為一致的 API 替換，避免把 deprecated cleanup 擴大成登入架構或資料模型重寫。

## Step 20：IDE 小型警告整理

代表 commit：`daf9447`、`1b7f644`、`6677dd3`

### 目的

清理低風險 warning、死碼與明確無用殘留，但避免把 Foundation、DatouNMS、登入 gate、team color、deprecated API 等高風險項目混進同一刀。

### 實作重點

- 移除低風險 unused code。
- 清掉 dead cleanup leftovers。
- 對 enum switch 等低風險 warning 做明確 no-op 處理。
- 記錄哪些警告不應在 Step 20 處理。
- 對被 review 判定過度防禦的修改回復或避免擴大。

### 概念與結果

Step 20 是「小型整理」，不是架構重構。它的價值在於降低噪音，讓後續真正高風險清理更容易看清楚。

## Step 21：最終移除 Foundation / NMS 相容層

代表 commit：`28ae679`、`2a714be`、`5be9f2a`、`b6b758d`、`26c2e24`、`b7ef54d`、`d1dcc01`、`b7cad53`、`070ce6c`、`e1caddb`

### 目的

正式拆除 Foundation / DatouNMS 作為主要支柱的狀態。這是整個升級線最大型的依賴清理。

### 實作重點

- 移除 DatouNMS 使用。
- 替換 Foundation utility wrappers。
- 將 command 從 Foundation command framework 遷出。
- 遷移 config 與 settings loaders。
- 遷移 menu framework。
- 從 build 中移除 Foundation dependency。
- 替換 `ChatColor` 舊色彩模型。
- 處理安全的 IDE warning 與後續狀態文件。

### 概念與結果

Step 21 是從「相容層控管」走到「正式移除」。完成後，WonderlandUHC 不再依靠 Foundation / DatouNMS 來支撐主要運行流程。

## Step 22：Login Gate Migration

代表 commit：`c31b80c`、`16d5267`

### 目的

移除對舊 `PlayerLoginEvent` 登入 gate 的依賴，改用 Paper `1.21.11` 更合適的連線驗證流程，並以 LuckPerms 查詢登入前權限。

### 實作重點

- 規劃登入 gate migration，固定 LuckPerms 作為正式部署依賴。
- 建立 login subject / permission service 邊界。
- 用 LuckPerms 查詢 bypass permission。
- 將白名單、設定中、滿員、遊戲中加入等登入拒絕邏輯接到新流程。
- 保留玩家可見訊息語意。

### 概念與結果

Step 22 解決的是登入模型差異，不只是替換 deprecated event。玩家完整加入伺服器前無法依賴 Bukkit `Player` 狀態，因此需要 LuckPerms 這類登入前可查詢的權限來源。

## Step 23：Message Format Boundary

代表 commit：`5f733ac`、`17432a8`、`c92b63c`

### 目的

先收斂文字格式邊界，讓業務邏輯不再到處直接碰 Adventure serializer、legacy serializer 或字串轉換細節。

### 實作重點

- 集中 text conversion API。
- 將訊息格式化路徑導向 `PluginText`。
- 保留當時必要的格式相容邊界。
- 文件化 message format boundary 與後續 migration 計畫。

### 概念與結果

Step 23 沒有一次把所有設定檔改成新格式，而是先把「格式轉換責任」集中。這讓 Step 24 可以更乾淨地移除舊格式支援。

## Step 24：MiniMessage 現代化

代表 commit：`8db78ee`、`35b1d6d`

### 目的

將預設訊息資源正式改成 Adventure MiniMessage，並移除舊 `&` / `§` 色碼作為運行時正式格式的支援。

### 實作重點

- 將 messages、commands、GUI、items、scenario、scoreboard 等預設 resource 逐步轉成 MiniMessage。
- 移除 legacy parser fallback。
- 移除不再保留的 message migration 工具與相容 bridge。
- 將 Java inline 訊息改成 MiniMessage、Component 或 plain text。
- 記錄 MiniMessage-only migration 結果。

### 概念與結果

Step 24 正式確立新版文字格式：MiniMessage 是正式格式，舊 Bukkit 色碼不再是運行時相容目標。文字格式細節見 `docs/details/text-format.md`。

## Step 25：Runtime Legacy Residue Cleanup

代表 commit：`327cde4`、`3016760`、`b84ebbf`、`d56d284`

### 目的

Step 21 與 Step 24 移除大量舊依賴後，仍可能留下舊 facade、bridge、mapper、dead updater 或只是過渡命名的殘留。Step 25 清的是這些仍會拖住未來維護的殘留。

### 實作重點

- 將舊 cache / saved settings facade 轉成正式命名與正式入口。
- 移除舊 match state / settings bridge。
- 將仍需保留的 material / sound alias、team prefix section-code 行為改成正式支援名稱。
- 移除 dead updater、舊資源、未讀取欄位與無用 package。
- 記錄清理結果與搜尋 gate。

### 概念與結果

Step 25 區分「歷史文件中的 legacy」與「程式運行仍依賴的舊殘留」。它不為了字樣歸零亂改功能，而是移除真正會影響未來維護的舊橋接。

## Step 26：最終驗收與 regression 修正

代表 commit：`a7a9daa`、`6d1501d`

### 目的

用同一份 checklist 對照舊版與新版行為，找出升級後的 regression，並在發布前修正或明確接受差異。

### 實作重點

- 執行 1.16.5 與 1.21.11 功能對照驗收。
- 修正 1.21 validation issues。
- 處理權限、scenario、聊天、隊伍、文字格式、GUI、音效、console、whitelist 等問題。
- 刪除或修剪不再需要的舊 1.21 config assets。
- 將不能或不該修的差異標成明確接受，而不是模糊跳過。

### 概念與結果

Step 26 是發布前真正驗收，不是形式檢查。它把前面所有架構、依賴、訊息格式與流程修改拉回實際遊戲行為驗證。

## Step 27：發布與文件整理

代表內容：目前 `README.md`、`docs/` 摘要與 `docs/details/` 細節文件整理。

### 目的

將前面 26 個 step 的最終事實整理成可交接、可維護、可發布的文件。這一步不是繼續改架構，而是把已完成的升級狀態說清楚。

### 實作重點

- 移除舊過程文件，讓 `docs/` 根目錄保持摘要導向。
- 新增開發歷程摘要。
- 新增專案架構、外部整合、遊戲流程、選圖、文字格式、維護脈絡等摘要文件。
- 新增細節文件，補充架構、依賴、驗證、設定載入、遊戲流程、選圖與文字格式。
- 重寫 README，讓 release 使用者先看到安裝、依賴、版本與基本主持流程。
- 調整版本命名為 fork 的 `1.21.11-0.1.x` 類型，而不是沿用原作者舊 alpha 名稱。
- 補上 LuckPerms 依賴狀態與 console dependency report 的一致性。

### 概念與結果

Step 27 的重點是「讓人知道目前專案真正長什麼樣」。摘要文件給委託人、原作者與維護者快速理解；細節文件則在真正要修改系統時提供上下文。過程用臨時文件已移除，文件若與程式碼不一致，應以目前程式碼與最新驗證結果為準，並回頭修文件。

## 後續維護判斷

若未來要繼續開發，建議依下列方式回到對應 step：

| 問題類型 | 優先回看 |
| --- | --- |
| 啟動、dependency report、feature registration | Step 1、Step 7、Step 27 |
| Paper / Java 建置或 plugin metadata | Step 4 |
| match state、timer、死亡、勝負 | Step 2、Step 10 |
| 世界、中心、Chunky、跑圖 | Step 7、Step 9、Step 26 |
| host GUI、settings cache、saved games | Step 8、Step 11 |
| spectator / staff / hotbar | Step 12、Step 18 |
| Discord 公告或語音 | Step 7、Step 13 |
| scenario 行為 | Step 14、Step 15、Step 16 |
| scoreboard、practice、玩家顯示 | Step 18 |
| deprecated API 或 platform API | Step 19、Step 21 |
| 文字格式、MiniMessage、placeholder | Step 23、Step 24 |
| 舊殘留、命名、dead code | Step 20、Step 25 |
| 發布前驗證 | Step 26、`docs/details/verification.md` |
