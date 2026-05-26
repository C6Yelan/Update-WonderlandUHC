# WonderlandUHC 1.21.11 升級開發歷程摘要

整理日期：2026-05-21

這份文件用較短的篇幅說明本輪升級與重構的開發歷程，主要給委託人、原作者與後續接手維護者快速理解「每一步為什麼做、實際做了什麼、最後留下什麼狀態」。

本摘要只保留開發脈絡與代表 commit，不列出後續可能整併或刪除的舊文件路徑。

## 開發主軸

整體可以分成六個階段：

1. Step 0-4：建立可回歸基線，拆出啟動責任，讓專案能用 Java 21 / Paper `1.21.11` 編譯與封裝。
2. Step 5-9：把 Foundation、DatouNMS、外部整合、設定持久化與世界中心選擇從舊耦合中拆開。
3. Step 10-13：補上比賽生命週期、主持設定、玩家工具與 Discord 公告等主要使用流程。
4. Step 14-20：逐步驗收 scenario、presentation、scoreboard、practice、Bukkit/Paper API 與 IDE warning，避免一次大改造成不可驗證。
5. Step 21-25：完成 Foundation / NMS / legacy 訊息格式 / runtime legacy facade 的正式清理。
6. Step 26-27：用實機 checklist 做最終驗收，修正 regression，最後整理發布與維護文件。

## Step 摘要表

| Step | 主題 | 做了什麼 | 概念 / 留下的結果 | 代表 commit |
| --- | --- | --- | --- | --- |
| 0 | 建立基線與保護網 | 補上 baseline 文件與可重現的 1.16.5 / 1.21.11 測試入口。 | 先定義「原本怎麼運作」與「升級後怎麼驗證」，避免後面只靠印象判斷。 | `f60599c` |
| 1 | 拆出啟動組裝層 | 從主 plugin class 拆出啟動組裝、功能註冊與 dependency report。 | 讓啟動流程可讀、可檢查，也讓後續移除硬依賴時有清楚入口。 | `62b0ceb`、`694f795`、`c7cc54a` |
| 2 | 定義核心 UHC 模型 | 建立核心 match model，並用最小 bridge 接回舊 `Game` runtime。 | 先建立未來可測的核心模型，但不急著全面取代舊 runtime。 | `365dbc8`、`0f90576`、`96f55b3` |
| 3 | Port / Adapter 邊界 | 建立 scheduler、world、plugin messaging、asset 等薄 port / adapter。 | 把 Bukkit / Paper 依賴往外推，讓核心與應用層不要直接綁平台 API。 | `305a31e`、`b080927` |
| 4 | Java 21 / Paper 1.21.11 建置平台 | 升級 Gradle、Shadow、Lombok、Paper API、CI 與 `plugin.yml` metadata。 | 先讓專案站上 Java 21 / Paper `1.21.11` 的建置基線。 | `a782ce2`、`369cbcf`、`8f523e4` |
| 5 | Foundation 降級為 legacy 相容層 | 把還不能一次移除的 Foundation 呼叫集中到 adapter 或明確保留點。 | 不再讓新程式碼直接擴散 Foundation 依賴，為後續真正移除鋪路。 | `f64fc2e` 到 `8faf4ca` |
| 6 | DatouNMS 隔離 | 將 DatouNMS 相關讀寫和版本判斷收斂到 legacy adapter，並標記為 legacy-only dependency。 | 避免 Paper `1.21.11` 啟動時被舊 NMS 寫法直接擋住。 | `e719749`、`2287fd3`、`0307bd2` |
| 7 | 外部整合重評估 | 移除舊 Packet / startup hard gate，改寫世界邊界與預生成，移除舊 custom ore generator，恢復 DiscordSRV 整合。 | 保留真正影響玩家流程的整合；不可靠或不相容的舊插件 API 轉為移除或替代。 | `6754529` 到 `e4c9ec6` |
| 8 | 設定與持久化邊界 | 隔離 cache、saved games、settings 等持久化 store，並處理設定儲存格式。 | 讓設定 / 資料檔讀寫不再散落於舊框架，降低升級資料格式風險。 | `5916df1`、`9822a44`、`67a613f` |
| 9 | 世界、邊界、傳送與中心選擇 | 設計並整合 match center validation / runtime，支援更合理的 UHC 中心點選擇。 | 不再把 `0,0` 當成唯一中心；以候選中心評分與實際世界狀態選點。 | `eeadee9`、`12fd0e4`、`e238afa` |
| 10 | 狀態機與 Application Use Cases | 新增 match lifecycle use cases，例如 start、timer tick、death、end match。 | 把比賽生命週期的關鍵動作收成可讀、可測的應用流程，但舊 facade 仍保留作為過渡。 | `704cc34`、`23b22ac` |
| 11 | 核心主持設定入口修復 | 修正 host settings menu 的持久化與 inventory editor 操作。 | 確保主持人設定遊戲、編輯背包、reload 後仍能保留資料。 | `df6c0fc`、`fdd4824` |
| 12 | Hotbar / Spectator 工具驗收 | 驗證 spectator / staff hotbar、中心傳送、Nether portal 等工具流程，並記錄接受邊界。 | 這一步主要是驗收與分類，不是重寫工具系統。 | `1a41d22` |
| 13 | Discord 公告 GUI 正式化 | 修復正式 `/uhc edit` 裡的 Discord 公告 GUI / Conversation 流程，移除臨時入口。 | 讓公告回到正式主持 GUI 中，並保留 DiscordSRV 缺失時的可理解降級訊息。 | `f950f59` |
| 14 | Scenario 盤點與高風險隔離 | 建立 scenario inventory，隔離 scenario 載入失敗，修復選單、死亡流程與高風險 runtime blocker。 | 不讓單一 scenario 壞掉拖垮整個插件；先恢復可玩性與可驗證性。 | `233135f`、`7535684`、`399d699`、`b547655` |
| 15 | 單一 Scenario 行為補齊 | 逐項修正 block drop、scenario state、item durability、sound config 等 scenario 行為。 | 將單一 scenario 在 Paper `1.21.11` 下的差異逐步補齊，而不是一次重寫 rule engine。 | `78c25bb` 到 `55b52e6` |
| 16 | Scenario 混合互動驗收 | 針對多個 scenario 同時啟用時的死亡 / inventory / block drop 組合做驗收與修正。 | 確認不同 scenario 疊加後不會互相破壞核心玩法。 | `2c038cc`、`b56f0ca`、`4788d82` |
| 17 | 核心 Presentation 入口瘦身 | 修正 time placeholder，整理 host control flow，記錄 presentation cleanup 完成狀態。 | 讓指令 / GUI 層變薄，將共用主持流程收斂，但不把已能運作的 thin command 過度抽象化。 | `2e0cbd8`、`760622c`、`2f6c68d` |
| 18 | Scoreboard / Practice / 非核心 Presentation | 建立 validation checklist，修復 staff scoreboard、ore alerts、practice、player menus、item / death mechanics。 | 補齊玩家看得到的顯示與操作問題，避免只重構底層卻忽略實際使用感。 | `0f2b52b`、`457643f`、`9f740f0`、`05f8c24` |
| 19 | Bukkit / Paper API 與剩餘 Legacy Adapter | 替換 legacy runtime API，現代化 text / item API，整理 deprecated 與後續計畫。 | 把 Paper `1.21.11` 下不再合適的 API 用法收斂，避免升級後靠 deprecated 行為撐住。 | `6d3478b`、`d554cee`、`d8ddc19` |
| 20 | IDE 小型警告整理 | 清除低風險 warning 與死碼殘留，並記錄哪些 warning 不應混進本步驟處理。 | 只處理低行為風險項目，不把 cleanup 擴大成架構重寫。 | `daf9447`、`1b7f644`、`6677dd3` |
| 21 | 最終移除 Foundation / NMS 相容層 | 拆除 DatouNMS、Foundation utility、command framework、config loader、menu framework、Foundation dependency 與舊色彩模型。 | 這是最大型的依賴清理：插件 runtime 不再依靠 Foundation / DatouNMS 當主要支柱。 | `28ae679` 到 `e1caddb` |
| 22 | Login Gate Migration | 以 LuckPerms 為權限查詢基礎，移除 `PlayerLoginEvent` 登入 gate 依賴，改用 Paper `1.21.11` 的登入驗證流程。 | 登入拒絕、白名單、滿員 bypass 等行為不再依賴 Bukkit `Player` 尚未建立時的舊流程。 | `c31b80c`、`16d5267` |
| 23 | Message Format Boundary | 先盤點 legacy serializer，將 text conversion 集中到 `PluginText`，讓業務邏輯不再到處直接使用 serializer。 | 先收斂訊息格式邊界，不急著一次把所有設定檔轉成新格式。 | `5f733ac`、`17432a8`、`c92b63c` |
| 24 | MiniMessage 現代化 | 將預設訊息資源改成 MiniMessage-only，移除 legacy `&` / `§` runtime parser fallback 與遷移工具。 | 正式訊息格式改用 Adventure MiniMessage；舊格式不再當成 runtime 支援目標。 | `8db78ee`、`35b1d6d` |
| 25 | Runtime Legacy Residue Cleanup | 清掉剩餘 legacy facade / bridge / mapper / dead updater artifacts，並把仍保留的 alias / section-code 行為重新命名為正式支援。 | 區分「歷史文件中的 legacy」和「runtime 仍拖住插件的 legacy」，只清後者。 | `327cde4`、`3016760`、`b84ebbf`、`d56d284` |
| 26 | 最終驗收與 regression 修正 | 使用 1.16.5 / 1.21.11 checklist 實測，整理 regression 修正總表，修正權限、scenario、聊天、隊伍、文字格式、GUI、音效、console、whitelist 與資源殘留。 | 這一步不是形式審查，而是把實際遊戲中暴露的差異分類、修正或明確接受。 | `a7a9daa`、`6d1501d` |
| 27 | 發布與文件 | 整理使用者文件、開發文件、發布說明與最終 dependency 狀態。 | 將前面 26 步的最終事實轉成給服主、委託人、原作者與新維護者能讀懂的文件。 | 本文件 |

## 最終狀態概覽

目前升級線的核心狀態如下：

- 建置目標已改為 Java 21 / Paper `1.21.11`。
- DatouNMS 與 Foundation 已不再作為主要 runtime 依賴。
- LuckPerms 成為登入 gate / 權限查詢策略的一部分。
- 訊息預設格式已改為 Adventure MiniMessage。
- Chunky 作為必要依賴處理；DiscordSRV 以可選依賴與明確降級行為處理。
- Scenario、GUI、scoreboard、practice、host settings、登入 gate 與主要遊戲流程已經過 Paper `1.21.11` 實機驗收與 regression 修正。
- Step 26 修正後回測已完成；若後續有新失敗，應回到小範圍修正切片，不應重新展開大範圍重構。

更完整的每個 step 目的、實作重點與留下的設計概念，見 `docs/details/development-steps.md`。
