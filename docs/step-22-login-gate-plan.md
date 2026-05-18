# Step 22 Login Gate Migration 規劃

建立日期：2026-05-18

本文件承接 `docs/steps.md` Step 22，用來固定 `PlayerLoginEvent` 移除前的設計方向、切片順序與驗收 gate。Step 22 的目標不是把 deprecated warning 藏起來，而是把登入拒絕流程移到 Paper `1.21.11` 建議的連線驗證階段，同時保留舊版玩家可見行為。

## 判斷結論

Step 22 預設採用 LuckPerms 讓登入 gate 轉型成不依賴 Bukkit `Player` 的架構。

原因是目前 WonderlandUHC 的登入 gate 在 `PlayerLoginEvent` 階段直接使用 `Player#hasPermission(...)`、白名單 `PlayerCollection.contains(Player)`、遊戲中 `UHCPlayer` 角色狀態與滿員 bypass。Paper `PlayerConnectionValidateLoginEvent` 發生在更早的 connection/profile 階段，沒有可安全使用的完整 Bukkit `Player`。若仍想保留 bypass 權限語意，最務實的方向是讓 pre-login 權限查詢改由 LuckPerms user data 承接。

本步驟採用下列原則：

- 預設把 LuckPerms 視為 Step 22 的正式部署依賴，而不是臨時相容層。
- 不建立只包住 `PlayerLoginEvent` 的 wrapper，也不使用 `@SuppressWarnings("deprecation")`。
- 不把拒絕流程延後成 `PlayerJoinEvent` 後 kick；除非後續人工驗收明確接受這個行為差異。
- 不重寫 command、menu、team、scoreboard 或 message format；Step 22 只處理 login gate。
- 不新增大型 permission framework；只建立登入 gate 需要的薄 permission service / LuckPerms integration。

## 目前風險

| 項目 | 現況 | Step 22 風險 |
| --- | --- | --- |
| `PlayerLoginEvent` | `LoginListener` 直接監聽並建立 `UHCLoginEvent`。 | deprecated API 需移除；直接替換 event 會失去 `Player`。 |
| `UHCPlayer` 建立時機 | `UHCLoginEvent` constructor 會呼叫 `UHCPlayer.getUHCPlayer(source.getPlayer())`。 | pre-login 階段不應建立或更新 `UHCPlayer`。 |
| 白名單 | `WhitelistChecker` 以 `Player` 查 name / UUID。 | 需改成 profile UUID / name based。 |
| bypass 權限 | `UHCPermission.hasPerm(Player)` 依賴 Bukkit `Player#hasPermission(...)`。 | 需改由 LuckPerms 在 profile / UUID 階段查詢。 |
| 滿員 | 目前 `PreparingLoginListener.FullChecker` 用線上玩家數與 configuring bypass 判斷。 | 需評估 Paper `PlayerServerFullCheckEvent`，並修正權限語意。 |
| 遊戲中加入 / 重連 | `PlayingLoginListener` 用 `GameUtils.isGamingPlayer(Player)` 判斷角色。 | 需改成不建立 `UHCPlayer` 的 existing participant / rejoin candidate 判斷。 |
| 訊息格式 | kick message 目前仍以 legacy section color 轉 Adventure component。 | Step 22 只保留既有顯示；完整 message format 收斂留給 Step 23。 |

## LuckPerms 策略

預設策略：

1. 在 `build.gradle` 新增 LuckPerms API `compileOnly` dependency。
2. 在 `plugin.yml` 宣告 LuckPerms 依賴策略。建議先以 hard dependency 實作，讓缺少 LuckPerms 時明確停止啟動；若後續部署上不能接受 hard dependency，再改成 soft dependency 並明確定義無 LuckPerms 時 bypass 權限不可查詢的拒絕 / 降級行為。
3. 新增 `integration/luckperms/` 的薄 adapter，只負責用 UUID / name 載入 user data 並查詢 `UHCPermission` 字串。
4. 新增登入專用 permission service，例如 `LoginPermissionService`，讓 login gate 不直接散落 LuckPerms API。
5. 若 LuckPerms user load 失敗，預設 fail closed：不能因權限查詢失敗而放行本應被 whitelist、滿員或遊戲中 gate 擋下的玩家。

需要在第一個程式碼切片前確認的決策：

- LuckPerms 是否成為正式 hard dependency。
- 測試服是否已安裝 LuckPerms，或需要先補測試服部署。
- 現有 `plugin.yml` 的 `wonderland.uhc.bypass.join.full` 與程式內 `BYPASS_JOIN_CONFIGURING` 滿員 bypass 不一致，應修正成獨立 `BYPASS_JOIN_FULL`，還是保留舊行為。

## 子切片順序

### 22.0 文件與現況固定

目標：

- 固定本文件作為 Step 22 的執行地圖。
- 在 `docs/steps.md` 明確指向本文件。
- 確認 Step 22 預設採用 LuckPerms，不再把權限策略留到實作途中才決定。

完成條件：

- 本文件存在並被 `docs/steps.md` Step 22 指向。
- 尚未修改程式碼。
- `git diff --check` 通過。

### 22.1 Login subject / decision model

目標：

- 建立不依賴 Bukkit `Player` 的登入資料模型。
- 將登入檢查輸出變成明確 allow / deny decision。

建議新增：

- `src/main/java/org/mcwonderland/uhc/application/login/LoginSubject.java`
- `src/main/java/org/mcwonderland/uhc/application/login/LoginDecision.java`
- `src/main/java/org/mcwonderland/uhc/application/login/LoginGate.java`

切片限制：

- 不切換事件入口。
- 不導入 LuckPerms。
- 不改玩家可見 kick message。

完成條件：

- 現有 `LoginChecker` 可逐步改讀 `LoginSubject`。
- 尚未移除 `PlayerLoginEvent` 前，行為仍與原本一致。
- 封裝與 Paper `1.21.11` startup smoke test 通過。

### 22.2 白名單與 existing participant 判斷改成 UUID / name based

目標：

- `PlayerCollection` 提供明確的 UUID / name 查詢方法。
- 白名單檢查不再需要 `Player`。
- 遊戲中重連判斷不因 pre-login 階段缺少 `Player` 而建立新的 `UHCPlayer`。

切片限制：

- 不重寫 `UHCPlayer` 整體生命週期。
- 不改 `/whitelist` command 的玩家可見語意。
- 不改 team / role 行為。

完成條件：

- 白名單可用 UUID / name 判斷。
- 遊戲中 existing player / rejoin candidate 判斷有單元測試或可手動驗收紀錄。
- 封裝與 Paper `1.21.11` startup smoke test 通過。

### 22.3 LuckPerms permission service

目標：

- 導入 LuckPerms API 作為 pre-login bypass 權限來源。
- 將 `wonderland.uhc.bypass.join.whitelist`、`wonderland.uhc.bypass.join.configuring`、`wonderland.uhc.bypass.join.started` 與滿員 bypass 都轉成可由 UUID / name 查詢。

切片限制：

- 不為所有 command permission 改寫 `UHCPermission`。
- 不建立通用權限框架。
- 不把 LuckPerms API 直接散落到 login checker。

完成條件：

- LuckPerms installed 時，pre-login 權限查詢可用。
- LuckPerms missing / user load failure 的行為明確，且和 `plugin.yml` 依賴策略一致。
- 封裝與 Paper `1.21.11` startup smoke test 通過。

### 22.4 切換 Paper login event

目標：

- `LoginListener` 改監聽 Paper `PlayerConnectionValidateLoginEvent`。
- `UHCLoginEvent` 不再包 `PlayerLoginEvent`，改包 Step 22 的 login subject / decision。
- 滿員 bypass 若需要，接入 Paper `PlayerServerFullCheckEvent` 或在 login gate 中留下清楚邊界。

切片限制：

- 不把拒絕流程延後到 join 後 kick。
- 不在 login event 中建立 `UHCPlayer`。
- 不改 join 後才需要的 role apply / scoreboard / hotbar 初始化流程。

完成條件：

- `rg -n "org\\.bukkit\\.event\\.player\\.PlayerLoginEvent|PlayerLoginEvent" src/main/java src/test/java` 只允許文件或測試說明命中，production source 為 0。
- VS Code `PlayerLoginEvent` warning 消失，不靠 suppress / wrapper。
- 封裝、Paper `1.21.11` startup 與 `/uhc reload` 通過。

### 22.5 Login gate 情境驗收

最低驗收矩陣：

| 情境 | 預期 |
| --- | --- |
| LuckPerms 安裝且使用者無 bypass | 依白名單、等待 host、滿員、遊戲中 gate 正常拒絕。 |
| LuckPerms 安裝且使用者有 whitelist bypass | 白名單開啟時仍可進入。 |
| LuckPerms 安裝且使用者有 configuring bypass | 等待 host / configuring gate 可進入。 |
| LuckPerms 安裝且使用者有 started bypass | 遊戲中非參賽者可依舊版語意進入。 |
| 滿員且使用者無滿員 bypass | 被滿員 gate 擋下。 |
| 滿員且使用者有滿員 bypass | 可進入，且權限名稱與 `plugin.yml` 一致。 |
| 遊戲中既有參賽者重連 | 不被 game started gate 擋下，且 join 後角色 / relog 流程維持。 |
| 遊戲中陌生玩家加入 | 無 bypass 時被 `Messages.Kick.GAME_STARTED` 擋下。 |
| LuckPerms 缺失或 user load 失敗 | 行為與依賴策略一致；hard dependency 時應在啟動階段明確失敗。 |

完成條件：

- 以上矩陣有測試或手動驗收紀錄。
- `docs/ide-warning-current.json` 更新為 Step 22 已完成或只剩非 login-gate warning。
- `docs/steps.md` Step 22 進度更新。
- 程式碼變更後依專案規則完成封裝、Paper `1.21.11` startup、`/uhc reload` 與 login gate 情境測試。

## 不屬於 Step 22

- `LegacyComponentSerializer` / `&` / `§` 訊息格式收斂：交給 Step 23。
- README / DEVELOPMENT 的最終發布依賴描述：Step 25 發布文件再整理，但 Step 22 若導入 LuckPerms，需先留下 runtime dependency 記錄。
- command permission framework 全面重寫。
- `UHCPlayer` / role / team model 全面重構。
- login 後 scoreboard、hotbar、join message 的 presentation cleanup。
