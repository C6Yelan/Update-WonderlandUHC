# Step 24 Message Format Modernization 盤點

盤點日期：2026-05-18

工作分支：`step-24-message-format-modernization`

本文件承接 `docs/steps.md` Step 24，用來先固定訊息格式現代化前的現況、風險、策略選項與切片順序。此文件本身不代表已開始 MiniMessage migration，也不代表 legacy `&` / `§` 格式已移除。

## Step 24 後續清理：runtime legacy fallback removal

盤點日期：2026-05-19

工作分支：`step-24-runtime-legacy-removal`

Step 24 已完成 repository 預設 YAML 與伺服器 data folder 的 MiniMessage 遷移；此後續清理只處理「正式執行期是否仍接受 legacy `&` / `§` 訊息格式」。

目標：

- `PluginText.toComponent(...)` 不再把 `&` / `§` 視為可解析的正式訊息格式；含 legacy code 的字串若仍進入正式顯示路徑，應被視為尚未遷移的缺口，而不是被 fallback 修好。
- 不保留 legacy 設定檔轉換工具；既有 data folder 若仍有舊格式，處理方式是重置 resource 或人工改成 MiniMessage。
- `PluginText` 不保留 legacy serializer bridge；scoreboard、console、conversation 與 Golden Head 判斷都必須走 Component / MiniMessage / plain text 流程。
- 剩餘 Java inline legacy 字串需逐步改成 MiniMessage 或明確的 plain text；不建立大型 message registry、不重寫 command / menu / tutorial 流程。

目前盤點：

| 類型 | 狀態 | 本後續清理的處理方式 |
| --- | --- | --- |
| repository YAML | 已完成 MiniMessage 遷移，`src/main/resources` 無 legacy color code 命中。 | 保持不動，只用 `rg` gate 驗證。 |
| server data folder YAML | 已 reset 後驗證，正式 YAML 無 legacy color code 命中。 | 保持不動；不再提供 migration script。 |
| `PluginText.toComponent(...)` | 不再接受 legacy format。 | 只保留 MiniMessage tag parsing 與 unknown angle literal 保護。 |
| `MessageFormatMigration` / CLI / script | 屬於 legacy 相容工具。 | 移除。 |
| Java inline legacy 字串 | command help、tutorial、console、center cleaner debug、menu status、fallback item text 等仍有 `&` / `§`。 | 小刀轉成 MiniMessage，不引入新框架。 |
| placeholder value legacy conversion | `PluginText.replaceToString(...)` 仍會把 legacy replacement 值轉成 MiniMessage snippet。 | 移除 legacy conversion；必要的狀態 / 顏色值改由呼叫端提供 MiniMessage snippet 或 plain text。 |
| scoreboard / console bridge | 曾短期使用 legacy section string。 | 改成 Component / MiniMessage-only，不保留 legacy bridge。 |

完成條件：

1. `rg -n "LegacyComponentSerializer|PluginText\\.colorize|toLegacyString|toLegacyAmpersandString|MessageFormatMigration|legacyAmpersand|legacySection" src/main/java src/test/java build.gradle scripts` 不得有命中。
2. `rg -n "&[0-9A-Fa-fK-Ok-oRr]|§[0-9A-Fa-fK-Ok-oRr]" src/main/resources src/main/java/org/mcwonderland/uhc` 不得再出現正式顯示路徑需要的 legacy 訊息字串。
3. `PluginTextTest` 明確覆蓋：legacy input 不再被 `toComponent(...)` 當成顏色、MiniMessage 仍可解析、unknown angle text 仍保持 literal。
4. 完成封裝、部署、Paper `1.21.11` startup、`/uhc reload` 與 log gate。

## Step 24 邊界

Step 24 的目標是把訊息格式從目前的 legacy-compatible 狀態，推進到可長期維護的新格式。預設候選格式是 Adventure MiniMessage，但第一刀不能直接批次替換 YAML，也不能只把 `LegacyComponentSerializer` 名稱刪掉。

本步驟只處理文字格式、設定格式、遷移策略與對應 parser。以下不屬於 Step 24：

- 重寫 command、menu、scoreboard、login gate、team 或 scenario 業務流程。
- 借格式 migration 順手修改玩家可見文字內容。
- 用全域 message registry 或大型 message key framework 取代現有設定載入流程。
- 沒有備份、失敗處理與人工修復說明就改寫使用者既有 data folder 設定。

## 搜尋基準

已執行：

```bash
rg -n "LegacyComponentSerializer" src/main/java src/test/java
rg -n "PluginText\.colorize|PluginText\.toLegacyString|PluginText\.toLegacyAmpersandString|toComponent\(" src/main/java/org/mcwonderland/uhc
rg -n "&[0-9A-Fa-fK-Ok-oRrXx]|§[0-9A-Fa-fK-Ok-oRrXx]" src/main/resources src/main/java
rg -n "\{[A-Za-z0-9_+\-]+\}" src/main/resources/{broadcasts,commands,gui,items,messages,scenarios,scoreboards,settings}.yml
rg -n "Click|Hover|click|hover|\{click-|runCommand|showText|ClickEvent|HoverEvent|hoverEvent|clickEvent" src/main/java src/main/resources docs/step-23-message-format-inventory.md
```

## 現況摘要

- Step 23 已把 `LegacyComponentSerializer` 集中到 `src/main/java/org/mcwonderland/uhc/platform/text/PluginText.java`。目前 production / test source 只有此檔案還 import `LegacyComponentSerializer`。
- `PluginText` 目前提供 legacy string -> `Component`、nullable legacy string -> nullable `Component`、`Component` -> legacy section string、`Component` -> legacy ampersand string、`&` -> `§` colorize、placeholder replacement 與時間格式 helper。
- repo 預設 YAML 仍大量使用 legacy `&` 色碼，不能直接切成 MiniMessage-only parser。
- Java source 仍有不少 inline legacy literal，主要透過 `Chat.send(...)`、`PluginConsole.log(...)`、`Chat.sendConversing(...)` 或 `PluginText.colorize(...)` 輸出。
- `PluginBootstrap#loadFiles()` 目前只 `saveResourceIfMissing`，不會更新既有 data folder 檔案，也沒有 migration / backup 流程。

### 2026-05-18 第一刀：MiniMessage parser proof

已完成：

- `PluginText` 新增 `toMiniMessageComponent(...)`，只作 MiniMessage parser proof，尚未接到現有 production 輸出流程。
- `PluginText` 新增 `toMiniMessageString(...)`，讓 migration proof 可用既有 legacy parser 轉成 `Component` 後，再交給 Adventure MiniMessage serializer 輸出。
- 新增 `MessageFormatMigration#legacyToMiniMessage(...)`，目前只委派 `PluginText` 的 legacy parse / MiniMessage serialize 邊界，不手刻 legacy 色碼表。
- `PluginTextTest` 覆蓋 MiniMessage 顏色 / 樣式、legacy -> MiniMessage 顏色 / 樣式、legacy color code reset decoration、placeholder 保留與 literal `<`。

未處理：

- 尚未轉換任何 repo 預設 YAML。
- 尚未接入 data folder migration、backup、dry-run 或啟動流程。
- 尚未移除 runtime legacy parser；`LegacyComponentSerializer` 仍集中保留在 `PluginText`。

驗證：

- `./gradlew test --tests org.mcwonderland.uhc.platform.text.PluginTextTest --no-daemon -Dorg.gradle.native=false` 通過。

### 2026-05-18 第二刀：單檔 migration safety proof

已完成：

- `MessageFormatMigration` 新增單一檔案 `dryRun(...)` / `apply(...)`，只針對傳入檔案處理，不掃 data folder、不接 `PluginBootstrap`。
- dry-run 只回報是否會變更、備份路徑與轉換行數，不寫回原檔，也不建立備份。
- apply 僅在偵測到 legacy `&` / `§` 格式碼的行上轉換；沒有 legacy 格式碼的檔案不建立備份、不寫回。
- apply 會在原檔旁建立 `.legacy-format.bak`；既有備份檔不覆蓋，若備份路徑不是一般檔案，會在寫回前失敗。
- 寫回先產生同目錄 temp file，再替換原檔，避免轉換內容還沒寫完就破壞原檔。
- `MessageFormatMigrationTest` 覆蓋 dry-run 不寫檔、apply 建備份、既有備份不覆蓋、已遷移檔案不重寫、備份路徑異常不改原檔，以及只轉 legacy 行。

未處理：

- 尚未把 migration 接到啟動流程、command 或真實 data folder。
- 尚未轉換 repo 預設 YAML。
- 尚未做 YAML 結構感知轉換；本刀只證明檔案層安全行為。

### 2026-05-18 第三刀：MiniMessage YAML 寫法與低風險 command 試點

已完成：

- `PluginText.toComponent(...)` 改成短期 dual-read 過渡：遇到 legacy `&` / `§` 格式碼仍走 legacy parser；沒有 legacy 格式碼且含有已接受的 MiniMessage 顏色 / 樣式 tag 時，才走 MiniMessage parser。
- 未知角括號文字仍當純文字處理，例如 `/uhc regen <seed>`，避免把舊 help text 誤判成 MiniMessage tag。
- `commands.yml` 最前方低風險單行訊息已先轉成 MiniMessage，包括 command framework 的錯誤、冷卻、reload 與 usage label 類訊息。
- `PluginTextTest` 覆蓋 MiniMessage production parser、legacy fallback、placeholder replacement 與未知角括號文字。

未處理：

- 尚未轉換 `/team invite` 的 `{click-join}`、click / hover text、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。
- 尚未移除 legacy parser；目前仍是 Step 24 分支內的過渡期 dual-read。

### 2026-05-18 第四刀：placeholder value 邊界與 Whitelist 試點

已完成：

- `PluginText` 補強 MiniMessage template 的 placeholder value：若 template 已是 MiniMessage，插入的 legacy `&` / `§` 色碼值會先轉成會關閉 tag 的 MiniMessage 片段，避免顏色外溢到 placeholder 後方文字。
- MiniMessage template 內插入的 literal `<green>` 類文字會被當成 placeholder 文字，不會被解析成 tag。
- `commands.yml` 的 `Whitelist` 6 條單行訊息已轉成 MiniMessage；`Whitelist.List` 多行分隔線仍保留 legacy，留待多行訊息驗收時處理。
- `PluginTextTest` 覆蓋 MiniMessage template + legacy placeholder value，以及 placeholder value 內 literal MiniMessage tag escaping。

未處理：

- 尚未轉換 `GiveAll`、`Border`、`Respawn`、`Team` 等後續 command 區塊。
- 尚未處理多行 command list、click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第五刀：GiveAll / Border 單行 command 試點

已完成：

- `commands.yml` 的 `GiveAll` 3 條單行訊息已轉成 MiniMessage，保留紅色錯誤訊息、綠色廣播文字與白色 `{amount}` / `{material}` placeholder 語意。
- `commands.yml` 的 `Border` 2 條單行訊息已轉成 MiniMessage，保留紅色錯誤文字、白色範圍 placeholder 與白色粗體 `瞬縮系統`。

未處理：

- 尚未轉換 `Respawn`、`SetSpawn`、`Heal`、`SendCoords`、`Team`、`Info`、`TopKills`、`TeamList` 等後續 command 區塊。
- 尚未處理多行 command list、click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第六刀：SetSpawn / Heal / SendCoords 單行 command 試點

已完成：

- `commands.yml` 的 `SetSpawn.Spawn_Saved` 已轉成 MiniMessage，保留全綠色成功訊息。
- `commands.yml` 的 `Heal.Format` 已轉成 MiniMessage，保留綠色玩家、白色提示文字與紅色血量 / heart。
- `commands.yml` 的 `SendCoords.Format` 已轉成 MiniMessage，保留綠色框架、aqua 標題 / 玩家名、粗體箭頭與白色座標值。

未處理：

- 尚未轉換 `Respawn` 多行訊息或 `Team` / `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第七刀：Uhc SetHost / SpecToggle 單行 command 試點

已完成：

- `commands.yml` 的 `Uhc.SetHost.Host_Changed` 已轉成 MiniMessage，保留灰色主持人設定提示。
- `commands.yml` 的 `Uhc.SpecToggle` 2 條單行訊息已轉成 MiniMessage，保留灰色 / 深青 `觀戰系統` prefix、紅色錯誤訊息與黃色 `/{cmd}` placeholder。

未處理：

- 尚未轉換 `Uhc.Choose.Kick_Init_Msg` 或 `Uhc.Regen.Creating_World` 多行訊息。
- 尚未轉換 `Respawn` 多行訊息、`Team` / `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 click / hover、scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第八刀：Team 基礎檢查單行 command 試點

已完成：

- `commands.yml` 的 `Team.Already_Has_One`、`Player_Not_In_Team`、`Not_Owner`、`Player_Has_No_Team`、`You_Dont_Have_Team` 已轉成 MiniMessage，保留全紅色錯誤訊息。

未處理：

- 尚未轉換 `Team.Create` / `Team.Invite` / `Team.Join` / `Team.Public` / `Team.Kick` / `Team.Chat`。
- 尚未處理 `Team.Invite.Invitation_Messages` 的 `{click-join}` click / hover 行為。
- 尚未轉換 `Uhc.Choose` / `Uhc.Regen` / `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第九刀：Team Invite / Join / Public 單行 command 試點

已完成：

- `commands.yml` 的 `Team.Invite.Already_In_Your_Team` 與 `Team.Join.No_Invitation` 已轉成 MiniMessage，保留全紅色錯誤訊息。
- `commands.yml` 的 `Team.Public.Opened` / `Closed` 已轉成 MiniMessage，保留灰色 / 綠色 `隊伍` prefix，以及公開時綠色、關閉時紅色的狀態訊息。

未處理：

- 尚未轉換 `Team.Create`、`Team.Invite.Invited` / `Invitation_Messages` / `Click_Here`、`Team.Kick` 或 `Team.Chat`。
- 尚未處理 `Team.Invite.Invitation_Messages` 的 `{click-join}` click / hover 行為。
- 尚未轉換 `Uhc.Choose` / `Uhc.Regen` / `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十刀：Team Chat 單行 command 試點

已完成：

- `commands.yml` 的 `Team.Chat.Cant_Use` / `Joined` / `Quitted` 已轉成 MiniMessage，保留灰色 / 綠色粗體 `隊伍聊天` prefix、不可用時紅色錯誤、加入時綠色狀態 / `/{cmd}`、離開時紅色狀態 / `/{cmd}`。

未處理：

- 尚未轉換 `Team.Create`、`Team.Invite.Invited` / `Invitation_Messages` / `Click_Here` 或 `Team.Kick`。
- 尚未處理 `Team.Invite.Invitation_Messages` 的 `{click-join}` click / hover 行為。
- 尚未轉換 `Uhc.Choose` / `Uhc.Regen` / `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十一刀：Team Create / Kick 多行 command 試點

已完成：

- `commands.yml` 的 `Team.Create.Created` 已轉成 MiniMessage，保留灰色刪除線分隔線、綠色成功訊息、白色說明文字與綠色 `/{cmd}` placeholder。
- `commands.yml` 的 `Team.Kick.Kicked` 已轉成 MiniMessage，保留灰色刪除線分隔線、紅色 `{player}` 與白色踢出訊息。

未處理：

- 尚未轉換 `Team.Invite.Invited` / `Invitation_Messages` / `Click_Here`。
- 尚未處理 `Team.Invite.Invitation_Messages` 的 `{click-join}` click / hover 行為。
- 尚未轉換 `Uhc.Choose` / `Uhc.Regen` / `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十二刀：Team Invite 發送通知多行 command 試點

已完成：

- `commands.yml` 的 `Team.Invite.Invited` 已轉成 MiniMessage，保留灰色刪除線分隔線、綠色 `{player}` / `{target}` placeholder 與白色邀請狀態文字。

未處理：

- 尚未轉換 `Team.Invite.Invitation_Messages` / `Click_Here`。
- 尚未處理 `Team.Invite.Invitation_Messages` 的 `{click-join}` click / hover 行為。
- 尚未轉換 `Uhc.Choose` / `Uhc.Regen` / `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十三刀：Team Invite click / hover 訊息試點

已完成：

- `commands.yml` 的 `Team.Invite.Invitation_Messages` 與 `Click_Here` 已轉成 MiniMessage，保留灰色刪除線分隔線、綠色 `{player}`、白色邀請文字，以及金色粗體 `[點擊這裡來接受]`。
- 保留既有 `InviteCommand` 行為：YAML 不新增 click / hover DSL，仍由程式碼對包含 `{click-join}` 的整行掛上 `/team join <player>` run command 與 hover text。
- `PluginTextTest.toComponentKeepsClickJoinMiniMessageReplacementClickable` 覆蓋 `{click-join}` raw replace 後仍可解析 MiniMessage，並可掛上 click / hover event。

未處理：

- 尚未轉換 `Uhc.Choose` / `Uhc.Regen` / `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十四刀：Uhc Choose / Regen 多行 command 試點

已完成：

- `commands.yml` 的 `Uhc.Choose.Kick_Init_Msg` 已轉成 MiniMessage，保留空白行、灰色 `[WonderlandUHC]` 框架、綠色 `Wonderland`、白色 `UHC` 與灰色提示文字。
- `commands.yml` 的 `Uhc.Regen.Creating_World` 已轉成 MiniMessage，保留空白行、綠色預覽世界提示、灰色說明文字、綠色 `/uhc choose` 與金色 `/uhc regen`。

未處理：

- 尚未轉換 `Respawn` 多行訊息或 `Whitelist.List`、`Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十五刀：Whitelist List 多行 command 試點

已完成：

- `commands.yml` 的 `Whitelist.List` 已轉成 MiniMessage，保留灰色刪除線分隔線、aqua 粗體 `白名單列表` 標題、灰色項目前綴與 aqua `{players}` placeholder。

未處理：

- 尚未轉換 `Respawn` 多行訊息或 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十六刀：Respawn command 訊息試點

已完成：

- `commands.yml` 的 `Respawn.Is_Playing` / `Broadcast` / `Respawned` 已轉成 MiniMessage，保留紅色錯誤、白色 `{mod}` / `{player}`、綠色復活廣播文字、白色刪除線分隔線、綠色粗體標題、空白行與 aqua 說明文字。
- 本刀只轉訊息格式，未更動 `RespawnCommand` 的復活流程、傳送、無敵、事件或 scenario 互動。

未處理：

- 尚未轉換 `Info` / `TopKills` / `TeamList` 等後續 command 區塊。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十七刀：GamePlaceholderReplacer placeholder 相容性

已完成：

- `GamePlaceholderReplacer` 改用既有 `PluginText.replaceToString(...)` 套用 placeholder，避免 MiniMessage template 被 raw legacy replacement 值污染後整行退回 legacy parser。
- 本刀不新增全域 message registry 或 YAML DSL，只復用 Step 24 已建立的 placeholder 邊界處理。

未處理：

- 尚未轉換 `commands.yml` 的 `Config.Messages`、`TopKills`、`TeamList`。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十八刀：Config Messages 多行 command 試點

已完成：

- `commands.yml` 的 `Config.Messages` 已轉成 MiniMessage，保留灰色刪除線分隔線、白色主持人 / 模式標籤、綠色計時 /規則標籤、黃色物資 / 世界設定標籤、灰色 placeholder 值與綠色 `/scenarios` / `/disableitems`。
- 本刀依賴第十七刀的 `GamePlaceholderReplacer` placeholder 邊界處理，避免 `{friendly-fire}` / `{nether-on}` 等 legacy boolean replacement 污染 MiniMessage template。

未處理：

- 尚未轉換 `commands.yml` 的 `TopKills`、`TeamList`。
- 尚未處理 scoreboard、GUI、item lore、scenario 或 `Golden_Head_Name`。

### 2026-05-19 第十九刀：TopKills / TeamList command 收尾

已完成：

- `commands.yml` 的 `TopKills.Messages` 已轉成 MiniMessage，保留灰色刪除線分隔線、深紅粗體標題，以及第 1 到第 5 名原本的紅 / 金 / 黃 / 白階層配色。
- `commands.yml` 的 `TeamList.Messages` 已轉成 MiniMessage，保留灰色刪除線分隔線、綠色粗體標籤、隊伍 `{color}` 對 `[character] teamname` 的動態顏色、存活玩家的綠色名稱 / 紅色血量 / 深紅粗體 heart，以及死亡玩家的紅色名稱。
- `ListCommand` 改用 `PluginText.replaceToString(...)` 處理一般 placeholder，並只對 `{color}` 做局部 MiniMessage color tag 轉換，以保留舊版隊伍顏色控制行為；沒有新增全域 registry 或 YAML DSL。
- `commands.yml` 目前已無 legacy `&` / `§` 色碼命中。

未處理：

- 尚未處理 scoreboard、GUI、item lore、scenario、`messages.yml`、`broadcasts.yml`、`items.yml`、`gui.yml`、`scoreboards.yml`、`scenarios.yml` 或 `Golden_Head_Name`。

### 2026-05-19 第二十刀：broadcasts.yml Invalid_Channel 試點

已完成：

- `broadcasts.yml` 的 `Discord.Invalid_Channel` 已轉成 MiniMessage，保留紅色玩家端錯誤訊息。
- Discord 公告 `Formatting` 內容不含 legacy 色碼，本刀未改動純文字公告模板。

未處理：

- 尚未處理 scoreboard、GUI、item lore、scenario、`messages.yml`、`items.yml`、`gui.yml`、`scoreboards.yml`、`scenarios.yml` 或 `Golden_Head_Name`。

### 2026-05-19 第二十一刀：items.yml hotbar / tool item 試點

已完成：

- `items.yml` 的 lobby、spectator、staff addon hotbar item `Name` / `Lore` 已轉成 MiniMessage，保留原本黃色 / 紅色 / 金色 / 綠色 / aqua 名稱與灰色 lore。
- `PluginItems` 僅在 item Name / Lore 已含 MiniMessage tag 時跳過舊版 `&r&f` / `&7` 預設前綴，避免 MiniMessage 被 legacy prefix 污染；legacy 與純文字 item 仍保留原本預設名稱 / lore 顏色行為。
- 本刀不處理 `Golden_Head_Name`，也不處理 GUI menu resource。

未處理：

- 尚未處理 scoreboard、GUI、scenario、`messages.yml`、`gui.yml`、`scoreboards.yml`、`scenarios.yml` 或 `Golden_Head_Name`。

### 2026-05-19 第二十二刀：settings.yml Golden_Head_Name 試點

已完成：

- `settings.yml` 的 `Misc.Golden_Head_Name` 已轉成 MiniMessage，保留原本金色粗體 `金頭顱` 語意。
- 目前 `Extra#createHead()` 與 `MainSettingsMenu` 已透過 `PluginText.toComponent(Settings.Misc.GOLDEN_HEAD_NAME)` 顯示 golden head 名稱，`GoldenHeadListener` 仍使用同一份 `Settings.Misc.GOLDEN_HEAD_NAME` 字串判斷 golden head，不新增另一套 registry 或判斷 DSL。
- 本刀只處理 repo 預設 resource，不處理既有 data folder 的自動遷移。

未處理：

- 尚未處理 scoreboard、GUI、scenario、`messages.yml`、`gui.yml`、`scoreboards.yml`、`scenarios.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十三刀：scenarios.yml 前段 scenario 名稱試點

已完成：

- `scenarios.yml` 的 `Armor_Vs_Health`、`Absorption_Less`、`Backpack`、`Bench_Blitz`、`Blood_Diamonds` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留原本 scenario 主色與灰色英文代號。
- 本刀只處理 scenario icon display name。`ScenarioConfig#getFancyName()` 仍把設定值交給 `PluginItems.create(...)`，沿用第二十一刀的 item name MiniMessage 相容處理。
- 本刀不處理 `Armor_Vs_Health.Warn_Msg`、runtime broadcast、`{fancy-time}` 或其他 scenario 訊息。

未處理：

- `scenarios.yml` 尚未轉換其他 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十四刀：scenarios.yml 第二組 scenario 名稱

已完成：

- `scenarios.yml` 的 `Bow_Less`、`Cut_Clean`、`Damage_Dogers`、`Diamond_Less`、`Double_Or_Nothing` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留紅 / 黃 / 深紅主色與灰色英文代號。
- 本刀延續第二十三刀的 scenario icon display name 路徑，只處理 `Name`，不處理同區塊的 runtime 訊息。

未處理：

- `Damage_Dogers.Death_Cause_This` 仍保留 legacy，需另切 runtime broadcast 驗證 placeholder、粗體與玩家名顏色。
- `scenarios.yml` 尚未轉換其他 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十五刀：scenarios.yml 第三組 scenario 名稱

已完成：

- `scenarios.yml` 的 `Fast_Obsidian`、`Fast_Smelting`、`Fire_Less`、`Food_Neophobia`、`Gold_Less` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留黃 / 金 / 深綠 / 紅主色與灰色英文代號。
- 本刀仍只處理 scenario icon display name，不改 scenario runtime 訊息。

未處理：

- `Food_Neophobia.First_Eat` / `Just_Can_Eat` 仍保留 legacy，需另切 runtime 訊息驗證 placeholder、粗體與食物名稱顏色。
- `scenarios.yml` 尚未轉換其他 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十六刀：scenarios.yml 第四組 scenario 名稱

已完成：

- `scenarios.yml` 的 `Hasty_Boys`、`Horse_Less`、`Iron_Man`、`Less_Bow_Damage`、`Limitations` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留黃 / 紅 / aqua / 深綠主色與灰色英文代號。
- 本刀仍只處理 scenario icon display name，不改 scenario runtime 訊息。

未處理：

- `Iron_Man.Damage_Before_Final_Heal`、`Limitations.Reached_Limit`、`Limitations.Cant_Mine_More` 仍保留 legacy，需另切 runtime 訊息驗證 placeholder、粗體與礦物名稱顏色。
- `scenarios.yml` 尚未轉換其他 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十七刀：scenarios.yml 第五組 scenario 名稱

已完成：

- `scenarios.yml` 的 `Lucky_Leaves`、`No_Clean`、`No_Enchant`、`No_Fall`、`Potion_Less` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留黃 / aqua / 紅主色與灰色英文代號。
- 本刀仍只處理 scenario icon display name，不改 scenario description 或 runtime 訊息。

未處理：

- `No_Clean.Description` 的 `{fancy-time}` 仍只保留 placeholder，不改語法；此刀未處理 scenario lore 顯示語意之外的 runtime 行為。
- `scenarios.yml` 尚未轉換其他 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十八刀：scenarios.yml 第六組 scenario 名稱

已完成：

- `scenarios.yml` 的 `Rod_Less`、`Shift_Kill`、`Silk_Web`、`Soup`、`Swap_Inventory` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留紅 / aqua / 黃主色與灰色英文代號。
- 本刀仍只處理 scenario icon display name，不改 scenario description 或 runtime 訊息。

未處理：

- `scenarios.yml` 尚未轉換最後一批 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第二十九刀：scenarios.yml 第七組 scenario 名稱

已完成：

- `scenarios.yml` 的 `Switcheroo`、`Timber`、`Time_Bomb`、`Triple_Arrow`、`Triple_Ores` 這 5 個 scenario `Name` 已轉成 MiniMessage，保留 aqua / 黃主色與灰色英文代號。
- 本刀仍只處理 scenario icon display name，不改 scenario description 或 runtime 訊息。

未處理：

- `Time_Bomb.Description` 的 `{fancy-time}` 與 `Time_Bomb.Exploded` 仍保留原格式，需另切 lore/runtime 訊息驗證。
- `scenarios.yml` 尚未轉換最後 3 個 scenario `Name`、runtime 訊息、`Warn_Msg` 與 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十刀：scenarios.yml scenario 名稱收尾

已完成：

- `scenarios.yml` 的 `Vanilla_Plus`、`Vein_Miners`、`Fragile_Rods` 這 3 個 scenario `Name` 已轉成 MiniMessage，保留黃 / 紅主色與灰色英文代號。
- `scenarios.yml` 的 scenario icon display name 已全部轉成 MiniMessage；目前 `rg -n "^  Name: \"&" src/main/resources/scenarios.yml` 應無命中。

未處理：

- `scenarios.yml` 尚未轉換 runtime 訊息、`Warn_Msg`、scenario lore 中需要進一步驗收的 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十一刀：scenarios.yml 無 placeholder runtime 訊息試點

已完成：

- `Backpack.Cant_Use_Msg` 已轉成 MiniMessage，保留紅色錯誤訊息語意；目前此欄位只由 scenario config 載入，`BackPackCommand` 不會實際送出它，本刀不改 command 行為。
- `Bench_Blitz.Workbench_Created` 已轉成 MiniMessage，保留灰色括號、深綠 `匠魂之心` 與紅色限制提示；既有 `ScenarioBenchBlitz` 仍透過 `Chat.send(...)` 顯示。
- 本刀只處理不含 placeholder 的 scenario runtime 訊息，不處理需要 replacement escaping 的訊息。

未處理：

- `scenarios.yml` 尚未轉換帶 placeholder 的 runtime 訊息、`Warn_Msg`、scenario lore 中需要進一步驗收的 `{fancy-time}` 相關文字。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十二刀：Armor_Vs_Health Warn_Msg

已完成：

- `Armor_Vs_Health.Warn_Msg` 已轉成 MiniMessage，保留深紅刪除線分隔線、空白行、紅色提示文字與 `{fancy-time}` placeholder。
- `ScenarioArmorVsHealth` 仍沿用 `PluginText.replaceTimeToArray(Warn_Msg, Apply_Within_Seconds)` 後交給 `Chat.send(...)`，不新增專用 message registry 或 YAML DSL。
- `PluginTextTest.replaceTimeToArrayKeepsMiniMessageMultilineTemplate` 覆蓋 MiniMessage 多行 template 經 `{fancy-time}` 替換後仍可解析。

未處理：

- `scenarios.yml` 尚未轉換其他帶 placeholder 的 runtime 訊息。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十三刀：Damage_Dogers Death_Cause_This

已完成：

- `Damage_Dogers.Death_Cause_This` 已轉成 MiniMessage，保留灰色括號、深紅 scenario 名稱、深紅粗體 `{player}`、紅色粗體提示、深紅粗體 `{amount}` 與空格排版。
- `ScenarioDamageDogers` 仍沿用既有 `.replace("{player}", entity.getName()).replace("{amount}", remaining + "")` 後交給 `Chat.broadcast(...)`；玩家名與剩餘數量不是任意格式輸入，本刀不新增 helper。

未處理：

- `scenarios.yml` 尚未轉換其他帶 placeholder 的 runtime 訊息。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十四刀：Food_Neophobia runtime 訊息

已完成：

- `Food_Neophobia.First_Eat` 已轉成 MiniMessage，保留灰色括號、深綠 scenario 名稱、紅色粗體提示、金色粗體 `{foodtype}` 與紅色粗體句點。
- `Food_Neophobia.Just_Can_Eat` 已轉成 MiniMessage，保留灰色括號、深綠 scenario 名稱、紅色提示與金色 `{foodtype}`。
- `ScenarioFoodNeophobia` 仍沿用既有 `.replace("{foodtype}", material.name())` 後交給 `Chat.send(...)`；`{foodtype}` 來源是 Bukkit `Material.name()`，本刀不新增 helper。

未處理：

- `scenarios.yml` 尚未轉換 `Iron_Man`、`Limitations`、`Time_Bomb` 的 runtime 訊息。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十五刀：Iron_Man / Time_Bomb runtime 訊息

已完成：

- `Iron_Man.Damage_Before_Final_Heal` 已轉成 MiniMessage，保留灰色括號、aqua scenario 名稱與紅色粗體提示。
- `Time_Bomb.Exploded` 已轉成 MiniMessage，保留深灰括號、紅色 scenario 名稱、灰色 `{player}` 與爆炸訊息。
- `ScenarioIronMan` 仍透過 `Chat.send(...)` 顯示；`ScenarioTimeBomb` 仍沿用既有 `.replace("{player}", owner.getName())` 後 `Chat.broadcast(...)`，不新增 helper。

未處理：

- `scenarios.yml` 尚未轉換 `Limitations` 的兩條 runtime 訊息。
- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

### 2026-05-19 第三十六刀：Limitations runtime 訊息與 scenarios.yml 收尾

已完成：

- `Limitations.Reached_Limit` 已轉成 MiniMessage，保留灰色括號、深綠 scenario 名稱與紅色限制提示。
- `Limitations.Cant_Mine_More` 已轉成 MiniMessage，保留灰色括號、深綠 scenario 名稱、紅色粗體提示、金色粗體 `{block}` 與 `{amount}` 文字。
- `ScenarioLimitations` 仍沿用既有 `.replace("{amount}", ...).replace("{block}", blockType.name())` 後交給 `Chat.send(...)`；replacement 來源是整數與 Bukkit `Material.name()`，本刀不新增 helper。
- `scenarios.yml` 目前已無 legacy `&` / `§` 色碼命中。

未處理：

- 尚未處理 scoreboard、GUI、`messages.yml`、`gui.yml`、`scoreboards.yml` 或既有 data folder 遷移。

## MiniMessage YAML 寫法規範

新的 resource 預設使用 MiniMessage tag，而不是 legacy `&` 色碼。撰寫時以「可讀的區段」為主，不追求機械地把每個 `&` 直接換成 tag。

建議：

```yml
No_Console: "<red>不好意思，只有玩家才能執行此指令。</red>"
Reload_Success: "<green>{plugin_name} {plugin_version}</green> <white>已重新載入。</white>"
Team_Chat: "<gray>[<green><bold>隊伍聊天</bold></green>]</gray> <green>{player}: {msg}</green>"
Divider: "<gray><strikethrough>--------------------</strikethrough></gray>"
```

規則：

- 顏色與樣式要包住明確文字區段，例如 `<red>...</red>`、`<bold>...</bold>`。
- placeholder 保留現有 `{player}`、`{time}`、`{fancy-time}` 語法，不改成 MiniMessage tag。
- click / hover 行為仍由程式碼建立，不在 YAML 新增 DSL。
- 舊 data folder 與尚未轉換的 repo resource 可暫時繼續使用 `&` / `§`，但完成 Step 24 前必須移除或明確記錄保留條件。

## Resource 格式盤點

| 檔案 | legacy 色碼命中 | placeholder 命中 | Step 24 風險 |
| --- | ---: | ---: | --- |
| `src/main/resources/broadcasts.yml` | 1 | 19 | Discord 公告 placeholder 與玩家可見排版需保留。 |
| `src/main/resources/commands.yml` | 101 | 47 | command help、多行訊息、`{click-join}` 邀請訊息與使用方式格式。 |
| `src/main/resources/gui.yml` | 257 | 66 | menu title、button name/lore、`&r` reset、隊伍顏色方塊、背包標題。 |
| `src/main/resources/items.yml` | 20 | 0 | hotbar / tool display name 與 lore。 |
| `src/main/resources/messages.yml` | 220 | 108 | join/quit/login kick、倒數、死亡、NoClean、Discord voice、updater、editor prompt。 |
| `src/main/resources/scenarios.yml` | 51 | 18 | scenario fancy name/lore、死亡/限制類提示、`{fancy-time}`。 |
| `src/main/resources/scoreboards.yml` | 160 | 130 | scoreboard line、title、placeholder、strikethrough divider、長度切割。 |
| `src/main/resources/settings.yml` | 1 | 1 | `Golden_Head_Name` 同時是顯示與 golden head 判斷來源。 |

目前未在 `biomes.yml`、`sounds.yml`、`spawns.yml`、`stats.yml` 找到 legacy 色碼命中。

## 必須保留的格式需求

### 1. Legacy 色碼與樣式語意

目前使用的格式不只包含顏色，也包含 `&l` bold、`&m` strikethrough、`&r` reset 等樣式。常見使用點：

- scoreboard 分隔線：`&7&m*----------------------*`、`&f&m---------------------`。
- menu / command 強調文字：`&a&l`、`&c&l`、`&6&l`。
- 插入玩家自訂值前的 reset：例如 `&r{name}`、`&r{title}`、`&r{color}`。

MiniMessage migration 必須能保留這些樣式語意，不能只處理顏色。

### 2. Placeholder 語意

現有 placeholder 主要是 `{name}` 格式，`PluginText` 也支援帶空白控制的 `{key+}`、`{+key}`、`{+key+}`。Step 24 不能把 placeholder 語法改成 MiniMessage tag，除非同時修改所有呼叫點並完成驗收。

建議 Step 24 保留現有 `{placeholder}` 語法，由 `PluginText` 先做 placeholder replacement，再交給正式 parser。若 placeholder value 本身可能含有格式碼，需要在測試中明確覆蓋。

### 3. Click / Hover text

目前 clickable component 仍由程式碼建立事件，文字來源則來自 legacy 設定或 inline 字串：

- `/team invite` 使用 `commands.yml` 的 `{click-join}` 與 `Click_Here`，程式碼建立 `/team join <player>` run command 與 hover text。
- `/uhc regen` confirm / skip prompt 在程式碼內建立 clickable component 與 hover text。
- inventory editor 使用 `messages.yml` 的 `Click_To_Head` / `Click_Finish`，程式碼建立 `/tohead` / `/finish` run command。

Step 24 不應把 click / hover 行為改成 YAML DSL；第一階段只處理文字格式轉換。

### 4. Scoreboard 特殊限制

`SimpleSidebar` 仍保留 entry code、15 行 entry、prefix / suffix 切割、hex color carry 與 legacy color carry 邏輯。Step 24 若直接輸出 MiniMessage component，仍需保留這些 scoreboard 限制：

- title 長度與 line prefix / suffix 切割不可改壞。
- placeholder 產生的 team color / player name 仍需接上後續顏色。
- 分隔線與空行不能造成 team entry 衝突。

Scoreboard 必須獨立作為高風險驗收項，不和一般 chat message 混在同一刀。

### 5. Item / Menu / Golden Head

`PluginItems` 目前對 item name 預設補 `&r&f`，lore 每行預設補 `&7`。`settings.yml` 的 `Golden_Head_Name` 目前透過 `PluginText.colorize(...)` 存入 `Settings.Misc.GOLDEN_HEAD_NAME`，並被 golden head display name 與判斷流程共用。

Step 24 不能只把 `Golden_Head_Name` 轉成 MiniMessage 顯示格式，而不處理 `GoldenHeadListener` 的判斷語意。

### 6. Raw / Console 輸出

`Chat#sendConversing(...)` 使用 `sendRawMessage(PluginText.colorize(...))`，console log 也大量沿用 legacy color string。若 Step 24 要完全移除 runtime legacy parser，需要先決定 console/raw 輸出是否也轉成 `Component` 或另有 console 專用格式。

## 策略選項

### A. MiniMessage-only + 一次性 legacy migrator

說明：

- repo 預設 YAML 全部轉成 MiniMessage。
- runtime parser 不再讀 legacy `&` / `§` 格式。
- 既有伺服器 data folder 透過 migration 工具備份後轉檔。

優點：

- 最符合 Step 24 完成條件。
- 完成後可移除 `LegacyComponentSerializer` import。

風險：

- migration 必須完整覆蓋所有 YAML 與 inline / raw 邊界。
- 轉檔失敗會直接影響既有伺服器設定。
- scoreboard、golden head、placeholder value escaping 風險最高。

### B. MiniMessage repo defaults + 有期限 dual-read 過渡

說明：

- repo 預設 YAML 轉成 MiniMessage。
- runtime 暫時允許舊 data folder 繼續讀 legacy 格式，或只在 migration 時讀 legacy。
- 文件明確記錄 dual-read 的期限與移除條件。

優點：

- 對既有伺服器比較安全。
- 可分批驗證 repo defaults 與實際 data folder。

風險：

- 只要 runtime 還直接讀 legacy，就不能宣稱 legacy format 完全移除。
- parser 判斷規則若不清楚，可能讓 `<red>` 這類文字與 legacy fallback 互相干擾。

### C. 保留 legacy-compatible config，只補文件

說明：

- 不做 MiniMessage migration。
- 繼續把 `LegacyComponentSerializer` 留在 `PluginText`。

優點：

- 風險最低。

風險：

- 不能完成 Step 24。
- 只能作為暫緩決策，不是本步驟的實作終點。

## 建議方向

建議採用「MiniMessage repo defaults + 一次性 legacy migration」作為最終目標，但允許在 Step 24 分支內短期保留 dual-read / legacy converter 來降低切換風險。完成 Step 24 前，必須移除或明確保留並記錄任何 runtime legacy parser；若仍保留，就不能宣稱 legacy 完全消失。

第一個程式碼切片不應直接轉 YAML。建議先補 parser / escaping / migrator 的小型測試，確認 MiniMessage dependency、placeholder replacement 與 literal `<` / `>` 行為後，再開始轉資源檔。

## 建議切片順序

1. 文件盤點：建立本文件，固定格式需求、風險與停損線。
2. Parser proof：確認 MiniMessage dependency，新增 `PluginText` 的 MiniMessage parsing / serialization 測試，不切換 production 行為。
3. Migration 設計：新增 migration 設計文件或小型 migration 類別，先處理備份命名、可重跑性、失敗不覆蓋與 dry-run 報告。
4. Resource 第一批：轉 `commands.yml` / `messages.yml` 的低風險訊息，保留玩家可見文字與 placeholder。
5. Resource 第二批：轉 `items.yml` / `gui.yml`，獨立驗收 menu title、button name/lore、hotbar item、golden head。
6. Resource 第三批：轉 `scoreboards.yml`，獨立驗收 title、line、team prefix、below-name heart、tab health。
7. Resource 第四批：轉 `scenarios.yml` / `broadcasts.yml` / `settings.yml`，特別驗收 scenario fancy name、Discord broadcast、`Golden_Head_Name`。
8. Data folder migration：用測試服既有 data folder 驗證 backup、dry-run、轉檔與失敗回復。
9. Legacy boundary gate：確認 `LegacyComponentSerializer` 只集中在 `PluginText`，repo resources 不再用 legacy 色碼作為格式控制；若 runtime 短期保留 dual-read，必須記錄原因與移除條件。
10. 文件收尾：更新 README / DEVELOPMENT 的格式規則與後續新增訊息規範。

## 驗收清單

每個程式碼切片後依專案規則執行：

```bash
bash scripts/package-plugin-1.21.sh
bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/paper-1.21.11" --port 25567 --skip-build
```

伺服器仍使用測試服資料夾內的 `start.bat` 啟動。至少抽樣：

- `/uhc reload`。
- login gate kick message：白名單、等待 host、滿員、遊戲中加入。
- join / quit / combat relog disconnect message。
- `/team invite` clickable message、run command 與 hover。
- `/uhc regen` confirm / skip clickable prompt。
- host menu title、button name/lore、inventory editor `/tohead` / `/finish`。
- item display name / lore：hotbar tools、scenario icon、golden head。
- scoreboard title、line、team prefix、below-name heart、tab health。
- MOTD。
- Discord broadcast 與 active mention placeholder。
- legacy data folder migration：備份存在、原檔不在失敗時被破壞、轉檔後可重啟。

## Overthinking 停損線

遇到以下情況應先停止並討論：

- 想新增全域 message registry、message key framework 或 YAML click/hover DSL。
- 想一次批次轉完所有 YAML，且沒有逐批驗收。
- 想把 placeholder 語法從 `{key}` 改成 MiniMessage tag。
- 想為了消除 `LegacyComponentSerializer` 而改變玩家可見文字、顏色、換行或 scoreboard 行為。
- migration 需要改寫既有 data folder，但尚未設計備份、dry-run、失敗處理與人工修復說明。

### 2026-05-19 第三十七刀：scoreboard MiniMessage bridge 與 Default.Lobby 試點

`scoreboards.yml` 不能直接整檔轉成 MiniMessage，因為現有 `SimpleSidebar` 會先用 legacy 字串做 16 / 32 字元切割並延續顏色。若讓 `<green>` 這類 tag 直接進入切割流程，會切到 markup 本身，顯示結果反而不可靠。

本刀只在 `SimpleSidebar` 進入既有切割邏輯前，把 title / line 先經由 `PluginText.toComponent(...)` 與 `PluginText.toLegacyString(...)` 正規化成 legacy section string。切割、slot、team prefix / suffix 與顏色延續邏輯維持原樣，避免重寫 scoreboard renderer。

Resource 只轉換 `Default.Lobby`：

- 灰色刪除線分隔線。
- 白色 label。
- 綠色 `{online_players}` / `{max_players}` / `{team_size}` / `{teleport_in}` placeholder。
- 原本空白行維持不變。

本刀不轉換其他 scoreboard 區塊、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第三十八刀：scoreboards.yml Default.Starting

本刀延續第三十七刀的 `SimpleSidebar` bridge，不再調整 renderer，只轉換 `scoreboards.yml` 的 `Default.Starting` active lines。

保留內容：

- 灰色刪除線分隔線。
- 白色 `遊戲倒數` / `已傳送` / `未傳送` label。
- 綠色 `{start_in}` / `{teleported}` / `{teleporting}` placeholder。
- 原本 `&f ` 空白行改為 `<white> </white>`，保留白色控制與單一空白字元。

本刀不轉換 `Default` 其他狀態、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第三十九刀：scoreboards.yml Default.Spectator_Solo

本刀只轉換 `scoreboards.yml` 的 `Default.Spectator_Solo` active lines，沿用既有 spectator solo placeholder 與行數。

保留內容：

- 灰色刪除線分隔線。
- 白色 `時間` / `玩家` / `邊界大小` / `收縮倒數` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder。
- 原本純空白行維持 `" "` 不變。

本刀不轉換 `Spectator_Teams`、staff / player scoreboard、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十刀：scoreboards.yml Default.Spectator_Teams

本刀只轉換 `scoreboards.yml` 的 `Default.Spectator_Teams` active lines，內容與 `Default.Spectator_Solo` 同型，僅保留既有 teams spectator section 邊界。

保留內容：

- 灰色刪除線分隔線。
- 白色 `時間` / `玩家` / `邊界大小` / `收縮倒數` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder。
- 原本純空白行維持 `" "` 不變。

本刀不轉換 staff / player scoreboard、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十一刀：scoreboards.yml Default.Staff_Solo

本刀只轉換 `scoreboards.yml` 的 `Default.Staff_Solo` active lines，沿用第三十七刀的 `SimpleSidebar` bridge，不調整 renderer。

保留內容：

- 灰色刪除線分隔線。
- 白色 `順暢度` / `記憶體` label，灰色 `(TPS)` / `(RAM)` 標籤，綠色 `{tps}` / `{free_ram}` placeholder。
- 白色 `時間` / `玩家` / `邊界大小` / `收縮倒數` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder。
- 原本純空白行維持 `" "` 不變。

本刀不轉換 `Staff_Teams`、player scoreboard、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十二刀：scoreboards.yml Default.Staff_Teams

本刀只轉換 `scoreboards.yml` 的 `Default.Staff_Teams` active lines，內容與 `Default.Staff_Solo` 同型，僅保留既有 teams staff section 邊界。

保留內容：

- 灰色刪除線分隔線。
- 白色 `順暢度` / `記憶體` label，灰色 `(TPS)` / `(RAM)` 標籤，綠色 `{tps}` / `{free_ram}` placeholder。
- 白色 `時間` / `玩家` / `邊界大小` / `收縮倒數` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` / `{shrink_in}` placeholder。
- 原本純空白行維持 `" "` 不變。

本刀不轉換 player scoreboard、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十三刀：scoreboards.yml Default.Player_Solo

本刀只轉換 `scoreboards.yml` 的 `Default.Player_Solo` active lines，沿用第三十七刀的 `SimpleSidebar` bridge，不調整 renderer。

保留內容：

- 灰色刪除線分隔線。
- 白色 `遊戲時間` / `玩家數量` / `擊殺數` / `邊界大小` / `收縮倒數` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{kills}` / `{border_size}` / `{shrink_in}` placeholder。
- 原本雙空白行 `"  "` 與單空白行 `" "` 均維持不變。

本刀不轉換 `Player_Teams`、`Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十四刀：scoreboards.yml Default.Player_Teams

本刀只轉換 `scoreboards.yml` 的 `Default.Player_Teams` active lines，完成 `Default` active theme 的 scoreboard legacy 色碼轉換。

保留內容：

- 灰色刪除線分隔線。
- 白色 `遊戲時間` / `玩家數量` / `隊伍數量` / `擊殺數` / `隊伍擊殺數` / `邊界大小` / `收縮倒數` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{alive_teams}` / `{all_teams}` / `{kills}` / `{team_kills}` / `{border_size}` / `{shrink_in}` placeholder。
- 原本兩條雙空白行 `"  "` 均維持不變。

本刀不轉換 `Ultra` theme、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十五刀：scoreboards.yml Ultra.Lobby

本刀開始轉換 `Ultra` active theme，但仍只處理 `scoreboards.yml` 的 `Ultra.Lobby` active lines。

保留內容：

- 白色刪除線分隔線。
- 白色 `玩家數量` / `主持人` / `遊戲模式` / `將於` label。
- 綠色 `{online_players}` / `{host}` / `{team_size}` / `{teleport_in}` placeholder。
- 綠色 `YourIP.net`。
- 原本兩條純空白行 `" "` 均維持不變。

本刀不轉換 `Ultra` 其他狀態、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十六刀：scoreboards.yml Ultra.Starting

本刀只轉換 `scoreboards.yml` 的 `Ultra.Starting` active lines。

保留內容：

- 白色刪除線分隔線。
- 白色 `傳送玩家中....` 與 `將於` / `後開始` 文字。
- 綠色 `{start_in}` placeholder。
- 綠色 `YourIP.net`。
- 原本純空白行 `" "` 維持不變。

本刀不轉換 `Ultra` 其他狀態、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十七刀：scoreboards.yml Ultra.Spectator_Solo

本刀只轉換 `scoreboards.yml` 的 `Ultra.Spectator_Solo` active lines。

保留內容：

- 白色刪除線分隔線。
- 白色 `遊戲時間` / `存活玩家` / `邊界` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder。
- 綠色 `YourIP.net`。

本刀不轉換 `Ultra.Spectator_Teams`、staff / player scoreboard、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十八刀：scoreboards.yml Ultra.Spectator_Teams

本刀只轉換 `scoreboards.yml` 的 `Ultra.Spectator_Teams` active lines，內容與 `Ultra.Spectator_Solo` 同型，僅保留 teams spectator section 邊界。

保留內容：

- 白色刪除線分隔線。
- 白色 `遊戲時間` / `存活玩家` / `邊界` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder。
- 綠色 `YourIP.net`。

本刀不轉換 staff / player scoreboard、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第四十九刀：scoreboards.yml Ultra.Staff_Solo

本刀只轉換 `scoreboards.yml` 的 `Ultra.Staff_Solo` active lines。

保留內容：

- 灰色刪除線分隔線。
- 白色 `順暢度` / `記憶體` label，灰色 `(TPS)` / `(RAM)` 標籤，綠色 `{tps}` / `{free_ram}` placeholder。
- 白色 `遊戲時間` / `存活玩家` / `邊界` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder。

本刀不轉換 `Ultra.Staff_Teams`、player scoreboard、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第五十刀：scoreboards.yml Ultra.Staff_Teams

本刀只轉換 `scoreboards.yml` 的 `Ultra.Staff_Teams` active lines，內容與 `Ultra.Staff_Solo` 同型，僅保留 teams staff section 邊界。

保留內容：

- 灰色刪除線分隔線。
- 白色 `順暢度` / `記憶體` label，灰色 `(TPS)` / `(RAM)` 標籤，綠色 `{tps}` / `{free_ram}` placeholder。
- 白色 `遊戲時間` / `存活玩家` / `邊界` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{border_size}` placeholder。

本刀不轉換 player scoreboard、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第五十一刀：scoreboards.yml Ultra.Player_Solo

本刀只轉換 `scoreboards.yml` 的 `Ultra.Player_Solo` active lines。

保留內容：

- 白色刪除線分隔線。
- 白色 `遊戲時間` / `存活玩家` / `擊殺數` / `邊界` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{kills}` / `{border_size}` placeholder。
- 綠色 `YourIP.net`。

本刀不轉換 `Ultra.Player_Teams`、註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第五十二刀：scoreboards.yml Ultra.Player_Teams

本刀只轉換 `scoreboards.yml` 的 `Ultra.Player_Teams` active lines，完成 `Ultra` active theme 的 scoreboard legacy 色碼轉換。

保留內容：

- 白色刪除線分隔線。
- 白色 `遊戲時間` / `存活玩家` / `擊殺數` / `隊伍擊殺數` / `邊界` label。
- 綠色 `{game_time}` / `{remaining}` / `{all}` / `{kills}` / `{team_kills}` / `{border_size}` placeholder。
- 綠色 `YourIP.net`。

本刀不轉換註解中的 Badlion 範例、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第五十三刀：scoreboards.yml Badlion 註解範例

本刀只轉換 `scoreboards.yml` 最下方註解中的 `Badlion` 自訂範例，保留所有 `#` 註解符號與原本範例結構，不讓範例變成 active theme。

保留內容：

- 綠色主要 label / `YourIP.net`。
- 白色一般 placeholder、紅色 `{host}`、黃色 `{start_in}`。
- 灰色 `(TPS)` / `(RAM)` 標籤與白色 TPS / RAM placeholder。

本刀不更動 active scoreboard theme、team prefix、below-name heart、tab health，也不處理既有 data folder migration。

### 2026-05-19 第五十四刀：messages.yml 共用頂層訊息

本刀開始轉換 `messages.yml`，只處理檔案最前方 8 條共用頂層訊息。

保留內容：

- `No_Permission` / `Not_Yet_Started` / `Use_Only_While_Waiting` / `Not_Gaming_Player` / `Only_For_Spectator` 的紅色錯誤文字。
- `Error` 的深灰粗體括號、深紅粗體驚嘆號與紅色錯誤文字。
- `Enabled` / `Disabled` 的綠色 / 紅色文字，以及左右 `--` 刪除線語意。

本刀不轉換 `Host`、`Editor`、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater、click / hover 訊息，也不處理既有 data folder migration。

### 2026-05-19 第五十五刀：messages.yml Host 區塊

本刀只轉換 `messages.yml` 的 `Host` 區塊。

保留內容：

- 灰色括號 prefix，白名單 aqua、特殊模式 light purple、地獄 red、隊伍 aqua、邊界 dark green、遊戲世界 green。
- 綠色 `{player}` / `{scenario}` / `{type}` placeholder。
- 開啟 / 成功狀態的綠色、關閉 / 刪除 / 錯誤狀態的紅色。
- `World_Doesnt_Exist` 中 `/uhc regen` 維持白色。

本刀不轉換 `Editor`、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater、click / hover 訊息，也不處理既有 data folder migration。

### 2026-05-19 第五十六刀：messages.yml inventory editor click labels

本刀只轉換 `messages.yml` 的 `Editor.Click_Finish`、`Editor.Inventory.Click_To_Head` 與 `Editor.Inventory.To_Head_Failed`。

保留內容：

- `Click_Finish` 的綠色粗體按鈕文字。
- `Click_To_Head` 的金色粗體按鈕文字。
- `To_Head_Failed` 的紅色錯誤文字。
- click 行為仍由 `MainSettingsMenu#runCommandComponent(...)` 在程式碼掛上 `finish` / `tohead` run command，不新增 YAML click / hover DSL。

本刀新增 `PluginTextTest.toComponentKeepsInventoryEditorMiniMessageRunCommandClickable`，覆蓋 MiniMessage label 外掛 run command click event 後仍保留點擊事件。本刀不轉換其他 `Editor` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater，也不處理既有 data folder migration。

### 2026-05-19 第五十七刀：messages.yml Editor.Number

本刀只轉換 `messages.yml` 的 `Editor.Number` 區塊。

保留內容：

- `Invalid_Number` 的紅色錯誤文字。
- 各數字輸入 prompt 的黃色文字。
- 各 saved 訊息的灰色文字與 `{number}` placeholder。

本刀的 replacement 來源為解析後的數字，不涉及玩家任意格式輸入；因此不改 `startIntegerInput(...)` / `startDoubleInput(...)` 流程。本刀不轉換 `Editor.Text`、`Editor.Time`、`Editor.Inventory` 的 prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater，也不處理既有 data folder migration。

### 2026-05-19 第五十八刀：Editor.Text placeholder 邊界

本刀只補 `Editor.Text` 轉換前需要的 placeholder 邊界，不轉換 resource。

調整內容：

- `MainSettingsMenu` 的 title saved 訊息改用 `PluginText.replaceToString(...)` 套用 `{title}` / `{player}`。
- `TeamSettingsMenu` 的 team name、team character、already-used 訊息改用 `PluginText.replaceToString(...)` 套用 `{player}` / `{name}` / `{character}` / `{symbol}`。
- `TeamCharacter.Message` 的 `{length}` 也改用同一個既有 replacement 入口。

本刀避免之後 `Editor.Text` 改成 MiniMessage template 時，被玩家輸入的 literal `<tag>` 或 legacy 色碼污染整段訊息；不新增新的 message registry、YAML DSL 或通用抽象。本刀新增 `PluginTextTest.replaceToStringKeepsEditorTextInputLiteralInsideMiniMessageTemplate` 覆蓋玩家輸入 literal tag 時會被當作文字保留。

### 2026-05-19 第五十九刀：messages.yml Editor.Text

本刀只轉換 `messages.yml` 的 `Editor.Text` 區塊。

保留內容：

- Title / TeamName / TeamCharacter prompt 的黃色文字。
- Saved 訊息的灰色主文、綠色 `{player}`，以及 `{title}` / `{name}` / `{character}` 的 reset 語意。
- `Already_Used` 的深灰 `{symbol}` 與紅色錯誤文字。

本刀依賴第五十八刀的 `PluginText.replaceToString(...)` placeholder 邊界處理，避免玩家輸入污染 MiniMessage template。本刀不轉換 `Editor.Time`、`Editor.Inventory` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater，也不處理既有 data folder migration。

### 2026-05-19 第六十刀：messages.yml Editor.Broadcast

本刀只轉換 `messages.yml` 的 `Editor.Broadcast` 三個輸入 prompt。

保留內容：

- `Ip` / `Join_Time` / `Start_Time` 的黃色提示文字。
- 不改 `GameStartTimeInputSession` 的輸入流程、取消文字、Discord 公告內容或完成後 delivery 行為。

本刀不涉及 placeholder、click/hover、換行或玩家輸入回填，因此只做 resource 格式轉換；不轉換 `Editor.Time`、`Editor.Inventory` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater，也不處理既有 data folder migration。

### 2026-05-19 第六十一刀：messages.yml Editor.Time 一般時間訊息

本刀只轉換 `messages.yml` 的 `Editor.Time.Invalid_Time` 與五組一般時間設定：

- `Damage`
- `FinalHeal`
- `Pvp`
- `BorderShrink`
- `DisableNether`

保留內容：

- invalid time 的紅色錯誤文字與金色 `小時:分鐘:秒` 格式提示。
- 各 prompt 的黃色主文、灰色 `(小時:分鐘:秒)` 與黃色冒號。
- 各 saved 訊息的灰色主文與綠色 `{time}`。

本刀確認一般時間 saved 訊息已透過既有 `PluginText.replaceTimeToString(...)` 套用 `{time}`；不改 `TimeSettingsMenu` 輸入流程。本刀不轉換 `ShrinkCalculator`，因其由 `BorderSettingsMenu` 另一條流程處理；也不轉換 `Editor.Inventory` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十二刀：messages.yml Editor.Time ShrinkCalculator

本刀只轉換 `messages.yml` 的 `Editor.Time.ShrinkCalculator` 兩行。

保留內容：

- prompt 的黃色主文、金色 `{init}x{init}`、水藍色 `{final}x{final}`、灰色 `(小時:分鐘:秒)` 與黃色冒號。
- saved 訊息的灰色主文與綠色 `一秒 {speed} 格`。

本刀確認 `BorderSettingsMenu` 只以數字替換 `{init}`、`{final}`、`{speed}`，不涉及玩家任意格式輸入回填，因此不新增 placeholder 抽象或改動輸入流程。本刀不轉換 `Editor.Inventory` prompt / saved 訊息、`Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十三刀：messages.yml Editor.Inventory prompt / saved

本刀只轉換 `messages.yml` 的 `Editor.Inventory` 四組 inventory editor prompt / saved 訊息：

- `CustomInventory`
- `PracticeInventory`
- `CustomDrops`
- `DisableItems`

保留內容：

- prompt 的灰色主文與綠色「想設定的...放到背包」重點文字。
- saved 訊息的綠色完成文字。

本刀不碰已是 MiniMessage 的 `To_Head_Failed`、`Click_To_Head`、`Editor.Click_Finish`，也不改 `InventoryEditSession` 的 click command、背包保存、Golden Head item meta 或玩家 inventory backup 流程。本刀不轉換 `Lobby`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十四刀：messages.yml Lobby 單行訊息

本刀只轉換 `messages.yml` 的 `Lobby` 單行訊息：

- `Non_Spawn_Set`
- `Player_Join_Msg`
- `Player_Leave_Msg`
- `Automatic_Game_Canceled`

保留內容：

- `Non_Spawn_Set` 的紅色粗體主文與白色粗體 `/{cmd}`。
- join / leave 的綠色 `{player}` 與灰色人數文字。
- auto-cancel 的紅色主文與黃色 `{amount}`。

本刀確認替換來源只包含固定 command、玩家名稱或數字，不涉及多行 welcome banner；因此不改 `DefaultJoinMessage`、`LobbyQuitListener` 或 `UHCWorldUtils` 的流程。本刀不轉換 `Welcome_Msg_Configuring` / `Welcome_Msg`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十五刀：messages.yml Lobby Welcome_Msg_Configuring

本刀只轉換 `messages.yml` 的 `Lobby.Welcome_Msg_Configuring` 多行訊息。

保留內容：

- 上下分隔線的灰色刪除線。
- 未設定提示的金色主文與黃色 `/uhc edit`。
- 教學提示的深水藍主文與白色 `/{cmd}`。
- 空白行與原本換行順序。

本刀確認 `DefaultJoinMessage` 對此訊息仍使用既有 `PluginText.replaceToString(List, ...)` 套用 `{cmd}` 並保留換行；不改 join 流程。本刀不轉換正式 `Welcome_Msg`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十六刀：messages.yml Lobby Welcome_Msg

本刀只轉換 `messages.yml` 的 `Lobby.Welcome_Msg` 多行訊息。

保留內容：

- 上下分隔線的灰色刪除線。
- 歡迎、主持人、指令提示的綠色主文。
- 白色 `{player}`、`{host}`、`/scenarios`、`/config`。
- 空白行與原本換行順序。

本刀確認 `DefaultJoinMessage` 對此訊息仍使用既有 `PluginText.replaceToArray(List, ...)` 套用 placeholder 並逐行送出；不改 join 流程。本刀不轉換死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十七刀：messages.yml Kick

本刀只轉換 `messages.yml` 的 `Kick` 六個單行訊息。

保留內容：

- `Waiting_Host`、`Generating`、`Whitelisted`、`Full`、`Game_Started` 的紅色文字。
- `Thanks_For_Playing` 的灰色主文與白色 `{player}`。

本刀確認 login disallow path 最終仍由既有 `PluginText.toComponent(...)` 解析訊息；`LoginChecker` 既有 `PluginText.colorize(...)` 不會修改 MiniMessage tag，因此不改 login checker 或登入流程。本刀不轉換 `CenterCleaner`、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十八刀：messages.yml CenterCleaner 主要搜尋訊息

本刀只轉換 `messages.yml` 的 `CenterCleaner` 主要搜尋訊息，不含 `Search_Regen_Hint`：

- `Search_Started`
- `Search_Progress`
- `Search_Preview`
- `Search_Result`
- `Search_Recommended`
- `Search_Not_Recommended`
- `Search_Time_Limited`
- `Search_Cancelled`

保留內容：

- `[中心搜尋]` 前綴的灰色括號與綠色標籤。
- 各搜尋狀態的白色、灰色、綠色、黃色、紅色語意。
- `{current}`、`{total}`、`{stage}`、`{status}`、`{score}`、`{x}`、`{z}`、`{reasons}` placeholder。

本刀確認 placeholder 來源只包含 enum、數字、固定中文階段文字或原因清單文字，因此不新增 replacement 抽象或改動 `CenterCleaner` 搜尋流程。`Search_Regen_Hint` 因包含 literal `<seed>`，保留到後續小刀；本刀不轉換隊伍、Staff、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第六十九刀：messages.yml CenterCleaner Search_Regen_Hint

本刀只轉換 `messages.yml` 的 `CenterCleaner.Search_Regen_Hint`。

保留內容：

- 灰色主文。
- 金色 `/uhc regen` 與 `/uhc regen <seed>`。
- literal `<seed>` 文字。

本刀新增 `PluginTextTest.toComponentKeepsUnknownAngleTextLiteralInsideMiniMessage`，確認 MiniMessage template 中的 unknown angle text 會以 literal 文字保留；因此不需要為此訊息新增特殊 escape 或更動 `CenterCleaner` 流程。本刀不轉換隊伍、Staff、死亡訊息、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十刀：messages.yml Team

本刀只轉換 `messages.yml` 的 `Team` 五個單行訊息。

保留內容：

- `Full_Msg`、`Only_In_Chosen_Mode` 的紅色文字。
- join / leave / promote 的灰色 `[隊伍]` 括號與綠色標籤。
- join / leave 的深綠 `{player}`、綠色加入文字、紅色離開文字與灰色人數。
- promote 的白色 `{player}` 與金色新隊長文字。

本刀確認 placeholder 來源只包含玩家名稱與數字，不涉及玩家自訂隊伍名稱或徽章；因此不改 `UHCTeam`、team command 或 team menu 流程。本刀不轉換 Staff、Spectator、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十一刀：messages.yml Staff enabled / disabled

本刀只轉換 `messages.yml` 的 `Staff.Enabled` 與 `Staff.Disabled`。

保留內容：

- 灰色括號與金色 `管理系統` 前綴。
- enabled 的綠色文字。
- disabled 的紅色文字。

本刀不轉換 `Staff.Mined_Alert`，因為 `{block}` 目前由 `OreAlert.colorizedName()` 提供 legacy `&` 色碼；直接轉 MiniMessage 會形成 mixed MiniMessage / legacy template。`Mined_Alert` 留到下一刀先補 replacement 邊界再轉換。本刀不轉換 Spectator、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十二刀：messages.yml Staff Mined_Alert

本刀轉換 `messages.yml` 的 `Staff.Mined_Alert`，並只在 `StatsListener` 補必要 replacement 邊界。

保留內容：

- 灰色括號與金色 `管理系統` 前綴。
- 灰色 `{player}` / 主文 / `個` / 句點。
- 金色 `{amount}`。
- `{block}` 保留原本由 `OreAlert.colorizedName()` 提供的礦物顏色。

`StatsListener` 原本以 raw `.replace(...)` 套用 `{block}`；因 `{block}` 可能帶 legacy `&` 色碼，MiniMessage template 會變成 mixed format。本刀改用既有 `PluginText.replaceToString(...)`，沿用現有 placeholder 入口，未新增新抽象或更動 ore alert 流程。本刀不轉換 Spectator、倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十三刀：messages.yml Spectator

本刀只轉換 `messages.yml` 的 `Spectator` 三個訊息，並在死亡踢出提示接回既有時間 placeholder helper。

保留內容：

- `No_Perm_To_Spec` 的黃色粗體主文與白色粗體 `{fancy-time}`。
- `Death_Kick_Message` 的灰色括號、綠色 `Wonderland`、白色 `UHC` 與灰色說明文字。
- `Teleported_To_Player` 的灰色括號、深青色 `觀戰系統` 前綴與綠色 `{player}`。

`No_Perm_To_Spec` resource 原本使用 `{fancy-time}`，但呼叫點只 raw replace `{time}`；本刀改用既有 `PluginText.replaceTimeToString(...)`，維持同一組時間 placeholder 行為，未新增新抽象。`Teleported_To_Player` 的 `{player}` 來源是 Minecraft player name，因此不改 `GameUtils` 傳送流程。本刀不轉換倒數、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十四刀：messages.yml CountDown main single-line messages

本刀只轉換 `messages.yml` 的 `CountDown` 主層單行訊息，範圍從 `Scatter_Announce` 到 `Starting_Announce`；不轉換 `Game_Started` 多行 banner，也不轉換 `CountDown.Border` 子區塊。

保留內容：

- 灰色括號與金色粗體 `遊戲提醒` 前綴。
- scatter started / finished 的黃色粗體文字。
- `Scatting_Players` 的灰色傳送進度與 `{count}` / `{total}`。
- damage / PvP / final heal / nether / start announce 的原色時間 emphasis：黃色、紅色、light purple、dark red、dark green 粗體 `{fancy-time}`。
- enabled / disabled 類訊息的紅色文字。

一般倒數 announce 已由 `Countdown` 透過既有 `PluginText.replaceTimeToString(...)` 套用時間 placeholder；`Scatting_Players` 的 replacement 來源只包含數字，因此不改 `ScatterHandler`。本刀不新增新抽象，不轉換 `Game_Started`、`CountDown.Border`、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十五刀：messages.yml CountDown Game_Started

本刀只轉換 `messages.yml` 的 `CountDown.Game_Started` 多行開局 banner。

保留內容：

- 首尾灰色刪除線分隔線。
- 綠色粗體 `遊戲開始了!`。
- 兩個純空白行。
- 四行白色說明文字。
- 綠色主持人文字、白色 `{host}` 與綠色祝福文字。

`StartCountdown` 已使用既有 `PluginText.replaceToArray(List, ...)` 套用 `{host}` 並保留多行輸出；本刀不改倒數狀態機、開局流程或 placeholder API，也不轉換 `CountDown.Border`、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十六刀：messages.yml CountDown Border

本刀只轉換 `messages.yml` 的 `CountDown.Border` 四個邊界倒數訊息。

保留內容：

- 灰色括號與金色粗體 `遊戲提醒` 前綴。
- Timer announce 的灰色主文、dark red 粗體 `{fancy-time}` 與 dark red 粗體 `{size}x{size}`。
- Timer reduced 的灰色收縮完成文字與 `{size}x{size}`。
- Shrink announce 的 gray 主文與 dark red 粗體 `{fancy-time}`。
- Shrink reduced 的灰色開始收縮文字與 `{size}x{size}`。

`TimerBorder` / `MovingBorder` 只替換數字 `{size}`，倒數時間仍由 `Countdown` 的既有 `PluginText.replaceTimeToString(...)` 套用；本刀不改 border mode、縮圈流程或 countdown flow，也不轉換 `Game`、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十七刀：messages.yml Game simple single-line messages

本刀只轉換 `messages.yml` 的 `Game` 簡單單行訊息：

- `Cant_Join_Before_Pvp_Enabled`
- `No_Nether`
- `World_Border_Reached`
- `Item_Disabled`
- `Relog_Death`
- `Ipvp_Lava`
- `Ipvp_Fire`
- `Arrow_Health_Message`
- `Closing_Server_Msg`

保留內容：

- 地獄限制訊息的灰色括號、紅色 `地獄` 前綴與紅色主文。
- 世界邊界與 iPvP fire 的紅色粗體。
- item disabled、iPvP lava 的紅色文字。
- combat relog death 的白色 `{player}`、深灰擊殺數括號、紅色 `{playerKills}` 與紅色主文。
- arrow health 的紅色 `{player}` / `{heal}❤` 與灰色主文。
- closing server 的紅色粗體主文與白色粗體 `{seconds}`。

本刀刻意不轉換 `Player_Disconnect`、`Player_Reconnect`、`You_Have_Been_Killed`、`Team_Eliminated` 與 `Victory_Broadcast`，因為那些訊息涉及 team chat format、自訂隊伍名稱或多行勝利名單，需各自檢查 replacement 邊界。本刀不改 portal、iPvP、combat relog、arrow health 或 shutdown 流程。

### 2026-05-19 第七十八刀：messages.yml Game reconnect / disconnect

本刀只轉換 `messages.yml` 的 `Game.Player_Disconnect` 與 `Game.Player_Reconnect`，並補兩個 `{player}` replacement 邊界。

保留內容：

- disconnect 的白色 `{player}` 與灰色 `離線了。`。
- reconnect 的白色 `{player}` 與綠色 `重新登入了遊戲。`。

這兩個 `{player}` 來源會串接 `team.getChatFormat()`，可能帶 legacy team color；直接 raw replace 到 MiniMessage template 會形成 mixed format。本刀改用既有 `PluginText.replaceToString(...)`：`PlayingJoinListener` 廣播 reconnect 訊息、`RolePlayerEvents` 產生 quit message component。未新增新抽象，也不改 combat relog、join state 或 team color 流程。本刀不轉換 `You_Have_Been_Killed`、`Team_Eliminated`、`Victory_Broadcast`、NoClean、PlayerDeath、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第七十九刀：messages.yml Game killed / eliminated messages

本刀只轉換 `messages.yml` 的 `Game.You_Have_Been_Killed` 與 `Game.Team_Eliminated`，並在 `UHCDeathDataHandler` 補對應 replacement 邊界。

保留內容：

- killed message 的灰色主文、紅色 `{team}` / `{character}` / `{killer}`、紅色 `{heal}❤`。
- team eliminated 的黃色 `{team}` 與紅色 `全隊滅絕。`。

`{killer}` 會串接 `killerUTeam.getChatFormat()`，`{team}` / `{character}` 可能來自玩家自訂隊伍名稱與符號；直接 raw replace 到 MiniMessage template 會形成 mixed format 或讓 placeholder 文字被當作 tag。`UHCDeathDataHandler` 改用既有 `PluginText.replaceToString(...)`，不新增新抽象，也不更動擊殺、隊伍淘汰或死亡流程。本刀不轉換 `Victory_Broadcast`、NoClean、PlayerDeath、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第八十刀：messages.yml Game NoClean

本刀只轉換 `messages.yml` 的 `Game.NoClean` 四個無敵提示。

保留內容：

- `Obtained` 的灰色括號、黃色粗體 `無敵` 前綴、白色粗體主文與 gold 粗體 `{fancy-time}`。
- `End` 的同樣前綴與紅色粗體結束文字。
- `Action_Bar` 的黃色粗體倒數文字與 `{fancy-time}`。
- `Action_Bar_End` 的紅色粗體結束文字。

`Obtained` 與 `Action_Bar` 已由 `InvinciblePlayer` 透過既有 `PluginText.replaceTimePlaceholders(...)` 套用時間 placeholder；固定訊息直接由既有 component 入口解析。本刀不改 invincible / no-clean 流程，也不轉換 `Victory_Broadcast`、PlayerDeath、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第八十一刀：messages.yml Game Victory_Broadcast

本刀只轉換 `messages.yml` 的 `Game.Victory_Broadcast` 多行勝利廣播。

保留內容：

- 首尾白色刪除線分隔線。
- gold 粗體勝利標題與 `{winner}`。
- 兩個純空白行。
- gold `隊伍總擊殺數:` 與白色 `{kills}`。
- gold `隊伍玩家:`。
- 兩個 leading spaces 與黃色 `{players}`。
- gray 粗體 `WonderlandUHC感謝你的遊玩。`。

`GameManager` 已先用既有 `PluginText.replaceToList(...)` 處理 `{winner}` / `{kills}`，再把 `{players}` 展開為玩家名稱；玩家名稱本身不帶 legacy formatting，因此本刀不改勝利廣播流程，也不新增 abstraction。本刀不轉換 PlayerDeath、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第八十二刀：messages.yml Game PlayerDeath environmental

本刀只轉換 `messages.yml` 的 `Game.PlayerDeath` 環境死亡訊息：

- `Lava`
- `Fire,Fire_Tick`
- `Suffocation`
- `Contact`
- `Fall`
- `Drowning`

保留內容：

- 白色 `{player}`。
- dark gray 擊殺數括號。
- 紅色 `{playerKills}`。
- 紅色死亡文案。

`DeathMessageHandler` 已使用既有 `PluginText.replaceToString(...)` 處理 `{player}` 與 `{playerKills}`，可保留可能來自 team chat format 的 legacy color。本刀不改 death message selection，不轉換 `Entity_*`、`Logout`、`Other`、`Player_Killed`、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第八十三刀：messages.yml Game PlayerDeath entity / logout / other

本刀只轉換 `messages.yml` 的 `Game.PlayerDeath` 剩餘非玩家擊殺訊息：

- `Entity_Attack`
- `Entity_Explosion`
- `Block_Explosion`
- `Logout`
- `Other`

保留內容：

- 白色 `{player}`。
- dark gray 擊殺數括號。
- 紅色 `{playerKills}`。
- 紅色死亡文案。
- `Entity_*` 的 `{entity}` 與 `Logout` 的 `{minute}`。

這些訊息同樣透過 `DeathMessageHandler` 的既有 `PluginText.replaceToString(...)` 套用 placeholder；本刀不改 death message selection，不轉換 `Player_Killed`、聊天格式、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第八十四刀：messages.yml Game PlayerDeath Player_Killed

本刀只轉換 `messages.yml` 的 `Game.PlayerDeath.Player_Killed` 四條玩家擊殺死亡訊息。

保留內容：

- 紅色 `{player}` 與 `{killer}`。
- dark gray 雙方擊殺數括號。
- 紅色 `{playerKills}` 與 `{killerKills}`。
- gray `被` 與四種擊殺文案。

`DeathMessageHandler` 已使用既有 `PluginText.replaceToString(...)` 處理 `{player}` / `{killer}`，可保留雙方 team chat format 的 legacy color。本刀不改 death message selection 或 kill attribution flow。至此 `messages.yml` 的 `Game.PlayerDeath` default resource 已無 legacy `&` / `§` 色碼；聊天格式、Discord voice、updater 與既有 data folder migration 仍未處理。

### 2026-05-19 第八十五刀：messages.yml ChatFormat

本刀只轉換 `messages.yml` 的 `ChatFormat` 四個聊天格式，並補既有聊天 replacement 邊界。

保留內容：

- team chat 的灰色括號、綠色粗體 `隊伍聊天` 前綴與綠色 `{player}: {msg}`。
- player chat 的灰色括號、aqua `玩家` 前綴、綠色 `{player}` 與白色 `: {msg}`。
- spectator chat 的灰色整行。
- staff chat 的紅色粗體 `[管理員]` 與黃色粗體 `{player}: {msg}`。

`{msg}` 是玩家輸入，team chat command 也會保留 legacy `&` 色碼；直接 raw replace 到 MiniMessage template 會形成 mixed format，或讓玩家輸入的 angle text 被當成 tag。`RoleChat#replace(...)` 與 `ChatCommand#sendTeamChat(...)` 改用既有 `PluginText.replaceToString(...)`，沿用既有 escaping / legacy value conversion，不新增聊天格式 registry 或 DSL。本刀不轉換 Console、Discord voice、updater 或既有 data folder migration。

### 2026-05-19 第八十六刀：messages.yml Console

本刀只轉換 `messages.yml` 的 `Console` 七個預設訊息，並補 console output 讀取 MiniMessage 的最小支援。

保留內容：

- `生態域轉換系統` 前綴：灰色括號、yellow 系統名稱。
- biome 成功訊息：gray 文字、green `{biome}` / `{changed}`。
- biome 失敗訊息：red 文字、dark red `{biome}` / `{changed}`。
- `遊戲世界` 前綴：灰色括號、green 系統名稱。
- chunk 載入開始 / 完成：green 文字、white `{world}` / `{number}`。
- nether chunk 偵測：yellow bold。
- force nether chunk：gold bold。

`PluginConsole` 原本只把 legacy `&` 轉成 `§`，若只改 resource 會在 console 印出 `<gray>` 等 raw tag。本刀讓 `PluginConsole` 透過既有 `PluginText.toComponent(...)` / `toLegacyString(...)` 輸出 legacy console string，保留既有 `log(...)` / `logNoPrefix(...)` 呼叫介面，不新增 console message registry。`ChunkPregenerationService` 的 `{world}` / `{number}` replacement 改用既有 `PluginText.replaceToString(...)`，避免 MiniMessage template 與 raw replace 混用。本刀不處理 `Dependency`、`Motd`、`DiscordVoice`、`Updater` 或 data folder migration。

### 2026-05-19 第八十七刀：messages.yml Dependency

本刀只轉換 `messages.yml` 的 `Dependency.Require_Dependency` 與 `Dependency.Require_Soft_Dependency`。

保留內容：

- red 主文。
- white `{plugin}`。
- aqua `{url}`。
- 兩條訊息原本的停用 / 功能不可用語意。

目前 `Require_Dependency` default resource 沒有直接使用點；`Require_Soft_Dependency` 由 `/reconnect` 與 Discord 公告設定使用。這兩個使用點改用既有 `PluginText.replaceToString(...)` 處理 `{plugin}` / `{url}`，不新增 dependency message helper，也不改 DiscordSRV hook 判斷流程。本刀不處理 `Motd`、`DiscordVoice`、`Updater` 或 data folder migration。

### 2026-05-19 第八十八刀：messages.yml Motd

本刀只轉換 `messages.yml` 的 `Motd` 五個 server list MOTD 預設訊息。

保留內容：

- configuring / generating：dark aqua。
- waiting：green `開放入場`、gray 括號與 slash、white `{online}`、gray `{max}`。
- starting：dark green。
- playing：red 主文、white `{remaining}`。

`MotdListener` 已使用既有 `PluginText.toComponent(...)` 設定 Paper MOTD，因此不需要新增 MOTD adapter。`PreparingMotdListener` 與 `PlayingMotdListener` 的 placeholder replacement 改用既有 `PluginText.replaceToString(...)`，不改 state selection、online/max/remaining 計算或 server list ping 事件流程。本刀不處理 `DiscordVoice`、`Updater` 或 data folder migration。

### 2026-05-19 第八十九刀：messages.yml DiscordVoice

本刀只轉換 `messages.yml` 的 `DiscordVoice` 五個預設訊息。

保留內容：

- 前綴：gray 括號、aqua bold `Discord語音`。
- 連線失敗 / 不在語音 / 移動失敗：red 主文。
- moved：yellow 主文與 `{channel}`。
- reconnecting：gray 主文。

`Chat.send(...)` 已使用既有 `PluginText.toComponent(...)`，因此一般 Discord voice 訊息不需要新增 adapter。`Moved` 的 `{channel}` 來源是 Discord voice channel name，改用既有 `PluginText.replaceToString(...)`，避免 channel name 的 angle text 被當成 MiniMessage tag。本刀不改 DiscordSRV hook、reconnect/move 流程或 console diagnostic 訊息，也不處理 `Updater` 或 data folder migration。

### 2026-05-19 第九十刀：messages.yml Updater status / success

本刀只轉換 `messages.yml` 的 `Updater.Checking_Updates`、`Updater.Up_To_Date` 與 `Updater.Success`。

保留內容：

- checking updates：gray。
- up to date：green。
- success 框線：dark gray strikethrough。
- success 主文：gold bold。
- `{fancy-time}` placeholder 原樣保留。

目前 `Messages.Updater` default resource 沒有實際使用點；本刀只做預設 resource 格式遷移，不新增 updater presenter，也不補不存在的更新流程。本刀不處理 `Updater.Failed` 或 data folder migration。

### 2026-05-19 第九十一刀：messages.yml Updater failed

本刀只轉換 `messages.yml` 的 `Updater.Failed` 三組預設訊息。

保留內容：

- failed 框線：dark red strikethrough。
- Internet 主文：red bold。
- Internet 檢查項目：保留兩個前導空白與 gold 編號。
- IO exception 主文：red bold。
- IO exception 回報文字：gold，`{link}` 為 aqua，錯誤訊息 label 為 red，`{exception}` 為 gray。
- File not found 主文：red bold，`{link}` 為 aqua，錯誤訊息 label 為 red，`{exception}` 為 gray。

目前 `Messages.Updater.Failed` default resource 沒有實際使用點；本刀只完成預設 resource 格式遷移，不新增 updater presenter，也不補不存在的更新流程。至此 `messages.yml` default resource 已無 legacy `&` / `§` 色碼；剩餘 legacy 命中集中在 `gui.yml`。

### 2026-05-19 第九十二刀：gui.yml pagination controls

本刀只轉換 `gui.yml` 檔案最上方的通用 `Leave`、`Next_Page`、`First_Page`、`Previous_Page`、`Last_Page`。

保留內容：

- leave：red 名稱、gray lore。
- next page：white `第 {page} 頁`、dark gray `>>`、gray lore。
- first / last page：gray 名稱與 gray lore。
- previous page：dark gray `<<`、white `第 {page} 頁`、gray lore。

`PluginItems` 已使用既有 `PluginText.toComponent(...)` 與 `PluginText.replaceToString(...)` 建立 item name/lore，因此本刀不新增 GUI message registry 或 YAML DSL。`Previous_Page` 的 literal `<<` 由 `PluginTextTest#toComponentKeepsPaginationArrowsLiteralInsideMiniMessage` 覆蓋，避免 MiniMessage tag parsing 邊界退化。本刀不處理 `Main` 或其他 GUI section。

### 2026-05-19 第九十三刀：gui.yml Main primary actions

本刀只轉換 `gui.yml` 的 `Main.Title` 與 `Main.Buttons` 前段 `Players`、`Whitelist`、`Generate_Map`、`Start`。

保留內容：

- title：dark green bold。
- players：green 名稱、gray 目前數量 label、green `{number}`、yellow 點擊提示。
- whitelist：aqua 名稱、gray 說明與狀態 label，`{status}` 原 placeholder。
- generate map：gold bold 名稱、gray lore。
- start：green bold 名稱、gray lore。

`{status}` 的值仍由 `MainSettingsMenu` 既有 `&aOn` / `&cOff` 提供；`PluginText.replaceToString(...)` 會在 MiniMessage template 中保留 legacy value 的顏色語意，因此本刀不改狀態常數或 menu flow。本刀不處理 `Main` 其他按鈕。

### 2026-05-19 第九十四刀：gui.yml Main navigation actions

本刀只轉換 `gui.yml` 的 `Main.Buttons` 導覽按鈕 `Scenarios`、`Team`、`Border`、`Time`。

保留內容：

- 四個按鈕名稱皆為 gold。
- 四個 lore 皆為 gray。

這些按鈕沒有 placeholder，也沒有特殊 click/hover 行為；本刀只做 resource 格式遷移，不改 `MainSettingsMenu` 的 slot dispatch 或開啟子選單流程。

### 2026-05-19 第九十五刀：gui.yml Main inventory editor actions

本刀只轉換 `gui.yml` 的 `Main.Buttons` 背包 / 掉落 / 禁用物品編輯入口：`Custom_Inventory`、`Custom_Drops`、`Disable_Items`、`Practice_Inventory`。

保留內容：

- 四個按鈕名稱皆為 yellow。
- 說明 lore 皆為 gray。
- `點擊來設定` lore 皆為 yellow。

這些按鈕只開啟既有 inventory editor / item-list flow；本刀不改 `InventorySaver`、`InventoryEditSession` 或任何物品序列化邏輯。

### 2026-05-19 第九十六刀：gui.yml Main numeric and toggle actions

本刀只轉換 `gui.yml` 的 `Main.Buttons` 數值 / 開關按鈕：`Apple_Rate`、`Experience`、`Nether`、`Ender_Pearl_Damage`。

保留內容：

- Apple / Experience 名稱：green。
- 當前數值 label：gray，`{count}` 與單位：green。
- 左鍵 / 右鍵操作提示：yellow。
- Nether / Ender pearl damage 名稱：green。
- toggle 說明與 `狀態:` label：gray，`{status}` 原 placeholder。

`{status}` 仍由既有 legacy `&aOn` / `&cOff` 提供並由 `PluginText.replaceToString(...)` 轉入 MiniMessage template；本刀不改加減數值、toggle 或設定儲存流程。

### 2026-05-19 第九十七刀：gui.yml Main remaining utility actions

本刀只轉換 `gui.yml` 的 `Main.Buttons` 剩餘工具入口：`Saves`、`Scoreboard`、`Title`、`Broadcast`。

保留內容：

- Saves 名稱：gold；說明主文 gray，`另存新檔` aqua，`匯入設定檔` yellow。
- Scoreboard / Title / Broadcast 名稱：light purple。
- Title 目前標題 label：gray，`{title}` 預設為 green。
- Title / Broadcast 點擊提示：yellow。
- 其他說明 lore：gray。

`{title}` 來源可能帶 legacy 顏色，仍由既有 `PluginText.replaceToString(...)` 轉入 MiniMessage template；本刀不改標題輸入、公告流程、計分板或模板載入流程。

### 2026-05-19 第九十八刀：gui.yml Teams menu

本刀只轉換 `gui.yml` 的 `Teams` 區段：標題、隊伍大小、同隊傷害、分隊模式。

保留內容：

- 標題：dark green + bold。
- Size 名稱：green；目前大小 label gray，`{count}` green；左右鍵提示 yellow。
- Team_Fire 名稱：red；說明與 `狀態:` label gray，`{status}` 原 placeholder。
- Team_Split_Mode 名稱：yellow；目前模式 label gray，`{type}` green。
- 分隊模式左鍵提示：yellow + white `>` + green `自選隊伍`。
- 分隊模式右鍵提示：aqua + white `>` + aqua `隨機分隊`。

`{status}` 仍由既有 legacy `&aOn` / `&cOff` 提供並由 `PluginText.replaceToString(...)` 轉入 MiniMessage template；本刀不改隊伍大小、同隊傷害 toggle 或分隊模式切換流程。

### 2026-05-19 第九十九刀：gui.yml Border basic settings

本刀只轉換 `gui.yml` 的 `Border` 基礎設定：標題、`Size`、`Nether_Size`、`Border_Type`。

保留內容：

- 標題：dark green + bold。
- Size / Nether_Size 名稱：green；目前大小 label gray，`{number}` green；點擊提示 yellow。
- Border_Type 名稱：yellow；目前模式 label gray，`{type}` green。
- Border_Type 左鍵提示：yellow + white `>` + green `收縮模式`。
- Border_Type 右鍵提示：aqua + white `>` + aqua `TP模式`。
- Border_Type 說明文字：gray。

`{type}` 仍由 `BorderType.fancyName()` 提供純文字並套用 green；本刀不改邊界大小輸入、地獄邊界大小輸入或邊界模式切換流程。

### 2026-05-19 第一百刀：gui.yml Border shrink settings

本刀只轉換 `gui.yml` 的 `Border` 剩餘收縮設定：`Final_Size_Of_Shrink_Mode_Border`、`Border_Shrink_Speed`、`Shrink_Calculator`。

保留內容：

- Final_Size_Of_Shrink_Mode_Border 名稱與點擊提示：yellow；目前大小 label gray，`{number}` green；限制提示 red。
- Border_Shrink_Speed 名稱主文 yellow，括號說明 gray；目前速度 / 收縮耗時 label gray，`{number}` / `{fancy-time}` green。
- Shrink_Calculator 名稱：gold；說明主文 gray，重點輸入文字與自動計算結果 green；點擊提示 yellow。

`{fancy-time}` 仍由既有 `PluginText.replaceTimeToString(...)` 提供純文字並套用 green；本刀不改最終邊界、收縮速度或計算器輸入流程。

### 2026-05-19 第一百零一刀：gui.yml Times menu

本刀只轉換 `gui.yml` 的 `Times` 區段。

保留內容：

- 標題：dark green + bold。
- Damage / Border_Shrink / Disable_Nether 名稱：red。
- Final_Heal 名稱：light purple。
- Pvp 名稱：gold。
- 目前時間 label 與地獄關閉說明：gray；`{time}` green；點擊提示 yellow。

`{time}` 仍由既有 `PluginText.formatTime(...)` 提供純文字並套用 green；本刀不改時間輸入、儲存或聊天提示流程。

### 2026-05-19 第一百零二刀：PluginPagedMenu title suffix

本刀只修正分頁選單標題的頁碼 suffix。

原因：`PluginPagedMenu#getTitle()` 原本會把 `&8{page}/{total}` 直接串到標題後面。當 `Scenarios`、`Saves` 等分頁 GUI 標題改成 MiniMessage 後，多頁狀態會變成 MiniMessage + legacy 混用，`PluginText.toComponent(...)` 會優先以 legacy 處理，導致 MiniMessage tag 可能被當成一般文字。

做法：保留既有頁碼顯示語意 dark gray，只在多頁時先把原標題轉成 `Component`，再 append dark gray 頁碼，最後序列化回 MiniMessage string 給既有 `PluginMenu` 流程處理。

本刀不改分頁邏輯、頁碼位置、上一頁 / 下一頁 item，也不新增 message registry。

### 2026-05-19 第一百零三刀：gui.yml Scenarios menus

本刀只轉換 `gui.yml` 的 `Scenarios` 與 `Enabled_Scenarios`：

- `Scenarios.Title`：dark green + bold。
- `Scenarios.Buttons.Clear_Scenarios.Name`：red。
- `Scenarios.Buttons.Clear_Scenarios.Lore`：gray。
- `Enabled_Scenarios.Title`：dark green + bold。

Scenario 動態 item 仍由既有 scenario icon / fancy name / enabled 狀態流程產生；本刀不改 scenario toggle、清除模式、分頁邏輯或 scenario resource。

### 2026-05-19 第一百零四刀：gui.yml Saves menu

本刀轉換 `gui.yml` 的 `Saves` 區段，並只補一個必要入口：

- `SavedSettingsMenu#convertToItemStack(...)` 的 saved item name 原本最後用 `.replace("{saved_game_title}", ...)` 覆蓋一次；改為 `PluginText.replaceToString(...)`，讓 `{saved_game_title}` 在 MiniMessage template 中仍可安全吃 legacy title。
- `Saves.Title`：dark green + bold。
- `Save_As` 名稱 green、說明 gray、點擊提示 yellow。
- `Saved.Name` 保留 reset + white 的原語意，`{saved_game_title}` 仍可帶既有顏色。
- Saved preview 分隔線保留 white + strikethrough；主設定 label green、次設定 label yellow、值 gray；`{team-size}` aqua；左右鍵提示保留 green / red + gray `>`。

`GamePlaceholderReplacer` 已使用 `PluginText.replaceToString(...)` 處理 lore placeholders，本刀不改模板儲存、載入、覆蓋、刪除或分頁流程。

### 2026-05-19 第一百零五刀：gui.yml Scoreboard menu

本刀轉換 `gui.yml` 的 `Scoreboard` 區段，並只補血量顏色顯示所需的 placeholder 來源：

- `Scoreboard.Title`：dark green + bold。
- Themes 名稱 yellow；目前風格 label gray，`{theme}` green；點擊提示 yellow。
- Update_Ticks 名稱 light purple；目前延遲 label gray，`{count} Tick一次` green；說明 gray；左右鍵提示 yellow。
- Heart_Color 名稱 green；目前顏色 label gray；愛心以 `<{color}>❤</{color}>` 套用目前顏色；說明 gray；點擊提示 yellow。
- `ScoreboardSettingsMenu` 對 `Heart_Color` 的 `{color}` 改傳 MiniMessage 色名，例如 `red` / `dark_red`，只供 GUI template 建立 tag 使用。

實際 scoreboard heart color 儲存與 `PluginColor#toString()` 的 legacy 輸出不變；本刀不改 scoreboard 更新頻率、風格選擇、ColorPicker 或玩家頭頂血量邏輯。

### 2026-05-19 第一百零六刀：gui.yml Sidebar theme selector

本刀只轉換 `gui.yml` 的 `Sidebar_Theme_Selector` 固定文字：

- 標題：dark green + bold。
- Theme item 名稱：yellow，保留 `{theme_name}`。
- preview label 與點擊提示：yellow。
- `{theme_preview}` 保持原 placeholder。

本刀不改 `SidebarThemeSettingsMenu`、theme preview 來源、分頁邏輯或 scoreboard theme 設定流程。

### 2026-05-19 第一百零七刀：gui.yml Broadcast menu

本刀只轉換 `gui.yml` 的 `Broadcast` 區段固定文字：

- 標題：dark green + bold。
- Discord 按鈕名稱：aqua。
- Discord 按鈕說明：gray。

本刀不改公告發送流程、DiscordSRV 整合或公告內容格式。

### 2026-05-19 第一百零八刀：gui.yml Team selector

本刀轉換 `gui.yml` 的 `Team_Selector` 區段，並只補隊伍顏色方塊顯示所需的 placeholder 來源：

- 標題：dark green + bold。
- Available 名稱 green；目前人數 label gray，`{slots}/{max}` green；隊伍名稱 / 顏色 / 標誌 label gray；玩家列表 label gray；加入提示 yellow。
- Full 名稱 dark red + bold；內容同 Available，但不含加入提示。
- Create_Your_Own 名稱 yellow + bold；說明 gray。
- `TeamSelectorMenu` 對 `{color}` 改傳 MiniMessage 色名，讓 `<{color}>█</{color}>` 能保留原本由隊伍顏色染方塊的效果。

`{name}`、`{character}` 仍保留 reset + white 的原語意；`{players}` 仍由既有 `UHCPlayers.toNames(...)` 提供。本刀不改隊伍列表來源、自由加入篩選、加入隊伍或建立隊伍流程。

### 2026-05-19 第一百零九刀：gui.yml Team settings menu

本刀轉換 `gui.yml` 的 `Team_Settings` 區段，並只補隊伍顏色方塊顯示所需的 placeholder 來源：

- 標題：dark green + bold。
- Name 名稱 green；目前名字 label gray，`{name}` green；點擊提示 yellow。
- Color 名稱 aqua；目前顏色 label gray；方塊以 `<{color}>█</{color}>` 套用目前顏色；點擊提示 yellow。
- Character 名稱 gold + gray 括號說明；目前徽章 label gray，`{character}` green；點擊提示 yellow。
- Open_Join 名稱 gold；說明 gray，`自由選擇` dark purple；`{status}` 保留原 placeholder 顏色；點擊提示 yellow。
- Help 名稱 aqua；說明 gray。
- `TeamSettingsMenu` 對 `{color}` 改傳 MiniMessage 色名，讓 `<{color}>█</{color}>` 能保留原本由隊伍顏色染方塊的效果。

本刀不改隊伍名稱輸入、徽章輸入、權限檢查、ColorPicker、`/team public` 或 help command 流程。

### 2026-05-19 第一百一十刀：gui.yml simple list menu titles

本刀只轉換三個只有標題的清單選單：

- `Players_Overworld.Title`：dark green + bold。
- `Players_Nether.Title`：dark green + bold。
- `Disable_Item_List.Title`：dark green + bold。

本刀不改玩家列表、地獄玩家列表、禁用物品列表的資料來源或分頁流程。

### 2026-05-19 第一百一十一刀：gui.yml Stats menu

本刀只轉換 `gui.yml` 的 `Stats` 區段：

- 標題：green + bold。
- Played / Wins / Kills / Kdr 名稱 label：gray；數值 placeholder：yellow。
- 所有 lore：gray。

`{played}`、`{wins}`、`{kills}`、`{kdr}` 仍由 `StatsMenu` 既有 `getButtonItem(...)` 流程填入；本刀不改統計讀取或 KDR 計算。

### 2026-05-19 第一百一十二刀：gui.yml Staff_Options menu

本刀只轉換 `gui.yml` 的 `Staff_Options` 區段：

- 標題：dark green + bold。
- Gold / Diamond alert、顯示觀察者、顯示管理員名稱：gray。
- 狀態 label：gray；`{status}` 保留既有動態顏色。
- 點擊提示：yellow。
- Moving_Speed 名稱與說明：gray；`{count}` aqua；左鍵提示 red / white / yellow；右鍵提示 green / white / yellow。

`{status}` 仍由 `StaffOptionsMenu` 既有 `&aOn` / `&cOff` 值提供，透過 `PluginText.replaceToString(...)` 轉入 MiniMessage template；本刀不改 staff option toggle、挖礦提示、玩家顯示或速度調整流程。

### 2026-05-19 第一百一十三刀：gui.yml Center_Cleaner menu

本刀只轉換 `gui.yml` 的 `Center_Cleaner` 區段固定文字：

- 標題：dark green + bold。
- Agree 名稱：green；一般說明 gray；載入時間提示 red。
- Disagree 名稱：red；一般說明 gray；中心點平坦風險提示 red。

本刀不改 `CenterCleanerMenu`、預覽世界產生、中心點清理選擇或傳送流程。

### 2026-05-19 第一百一十四刀：gui.yml See_Inventory and Color_Picker titles

本刀轉換 `gui.yml` 最後的 legacy GUI 色碼命中：

- `See_Inventory.Title`：`{player}` dark green + bold，後段文字 yellow。
- Health / Hunger / Level 名稱：yellow。
- Health 數值：light purple + bold；Hunger 數值：gold + bold；Level 數值：green + bold；其餘 label gray。
- `Color_Picker.Title`：dark green + bold。

`{player}` 仍由 `InventoryViewer#getTitle()` 透過 `PluginText.replaceToString(...)` 填入；`{health}`、`{hunger}`、`{level}` 仍由既有 `setInfoItem(...)` 流程填入。本刀不改背包內容複製、資訊 item slot 或顏色選擇流程。

### 2026-05-19 第一百一十五刀：manual data folder migration tool（已移除）

此工具曾作為「保留 legacy 轉換但不進 production parser」的中間方案；使用者已明確要求不保留相容層，因此後續清理移除 `scripts/migrate-message-format.sh`、Gradle `messageFormatMigration` task、`MessageFormatMigration` 與 `MessageFormatMigrationCli`。Step 24 完成後不再提供 legacy data folder 轉換工具。

### Step 24 收斂狀態

目前策略已收斂為「repo 預設 resource 使用 MiniMessage + production parser 不接受 legacy `&` / `§` fallback + 不保留 legacy serializer bridge」。`PluginText.toComponent(...)` 只處理 MiniMessage format tag 或 plain text；scoreboard、console、conversation、ore alert 與 Golden Head 判斷都走 Component / MiniMessage / plain text 流程。

完成狀態：

- `src/main/resources` 預設 resource 已無 legacy `&` / `§` 色碼搜尋命中。
- `src/main/java/org/mcwonderland/uhc` 內正式顯示路徑已無 legacy `&` / `§` 色碼搜尋命中。
- `PluginTextTest` 覆蓋 legacy input 保持 literal、MiniMessage parser、placeholder escaping 與 internal formatted snippet。
- legacy data folder 轉換工具已移除；既有舊格式設定必須重置 resource 或人工改成 MiniMessage。
- 已完成 Java 21 測試、`scripts/package-plugin-1.21.sh` 封裝、`scripts/deploy-to-windows-server.sh --skip-build` 部署，以及 Paper `1.21.11` `start.bat` 啟動、`uhc reload`、`stop` 驗證。
