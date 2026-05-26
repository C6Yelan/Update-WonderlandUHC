# WonderlandUHC 遊戲流程細節說明

整理日期：2026-05-23

這份文件補充 `docs/game-flow.md` 的細節，說明 WonderlandUHC 從啟動、選圖、跑圖、開局到結束時，各狀態、cache、重啟與玩家登入限制如何互相配合。摘要文件只保留主流程；需要排查流程卡住、修改開局流程或確認重啟接續行為時，再閱讀本文件。

## 適用範圍

本文件說明的是目前 Paper `1.21.11` 更新線中的主流程，主要對應：

- 插件啟動後的世界載入恢復。
- `/uhc regen` 預覽世界產生。
- `/uhc choose` 地圖確認與 Chunky 預生成。
- `/uhc start` 大廳倒數、分批傳送與正式開始。
- `/uhc stop` 與遊戲結束後的 cache 清理。

本文件不是安裝教學，也不是權限表。外部插件需求見 `docs/plugin-dependencies.md`，選圖規則見 `docs/map-selection.md`。

## 兩條狀態線

目前流程同時有兩條狀態線。

| 狀態線 | 程式名稱 | 主要用途 |
| --- | --- | --- |
| 世界載入狀態 | `LoadingStatus` | 判斷地圖是否還在設定、是否已預覽、是否正在跑圖、是否已可開局。 |
| 比賽狀態 | `StateName` | 判斷正式比賽是否在等待、傳送、開局倒數或遊戲中。 |

這兩條線不要混在一起理解。

`LoadingStatus` 主要回答：

```text
這張 UHC 世界現在能不能拿來開局？
```

`StateName` 主要回答：

```text
這一場比賽現在進行到哪個階段？
```

因此世界尚未 `DONE` 時，問題通常在選圖或跑圖流程；比賽尚未 `PLAYING` 時，問題才是開局倒數、傳送或正式遊戲流程。

## 世界載入狀態

`LoadingStatus` 有四種值。

| 狀態 | 是否保留 UHC 世界 | 是否重啟後接續跑圖 | 典型情境 |
| --- | --- | --- | --- |
| `CONFIGURING` | 否 | 否 | 尚未產生可用預覽世界，或 cache 不存在。 |
| `WORLD_READY` | 是 | 否 | `/uhc regen` 已完成，主持人正在預覽地圖。 |
| `GENERATING` | 是 | 是 | `/uhc choose` 後，伺服器重啟並等待 Chunky 預生成。 |
| `DONE` | 是 | 否 | Chunky 預生成與邊界處理完成，可開局。 |

啟動時若狀態不是 `CONFIGURING`，插件會保留 UHC 世界，套用 cache 中保存的設定，並恢復 `MatchCenter`。

若狀態是 `CONFIGURING`，插件會移除既有 UHC 世界與 Nether 世界，避免沿用上一輪未確認或不完整的地圖。

## Cache 內容

世界載入流程會寫入插件資料夾內的 `cache.db`。

目前保存內容包含：

| 欄位 | 用途 |
| --- | --- |
| `Loading_Status` | 目前世界載入狀態。 |
| `Host` | 本場主持人名稱。 |
| `Settings` | 本場 UHC 設定快照。 |
| `Match_Center.X` | 本場中心 X。 |
| `Match_Center.Z` | 本場中心 Z。 |
| `Match_Center.Border_Size` | 保存中心時的初始邊界大小。 |

`cache.db` 的定位是「本場世界載入進度與設定快照」，不是一般開服設定檔。

實務上不要手動編輯它，除非是在排查壞檔或刻意清掉卡住的流程。正常清除方式是使用 `/uhc stop`，或在確認不需要保留本場資料時刪除該檔後重新啟動。

## 啟動恢復流程

插件啟動後會延遲一 tick 執行主要恢復流程。

流程摘要：

1. 註冊 scenario。
2. 讀取 `cache.db` 中的世界載入狀態。
3. 將 cache 中的 Host 設回 `Game`。
4. 若狀態是 `CONFIGURING`，刪除 UHC 世界與 Nether 世界。
5. 若狀態不是 `CONFIGURING`，套用 cache 中的本場設定。
6. 恢復 `MatchCenter`。
7. 確保 UHC 世界與 Nether 世界存在。
8. 設定初始邊界。
9. 若狀態是 `GENERATING`，呼叫 Chunky 接續預生成。
10. 重新讀取保存的設定組。
11. 輸出插件啟動訊息。

這也是為什麼 `/uhc choose` 後會先踢人並重啟：正式預生成不是在同一個 runtime 內直接完成，而是透過 cache 記住狀態，再於重啟後接續跑圖。

## `/uhc regen`

`/uhc regen` 只負責產生預覽世界。

主持人執行時會先看到 CenterCleaner 選擇提示：

| 選項 | 實際指令 | 行為 |
| --- | --- | --- |
| 啟用 CenterCleaner | `/uhc regen confirm` | 建立預覽世界後搜尋較適合 UHC 的中心。 |
| 不啟用 CenterCleaner | `/uhc regen skip` | 直接建立預覽世界，沿用較接近原版的流程。 |

若需要指定 seed，可以使用：

```text
/uhc regen <seed>
/uhc regen confirm <seed>
/uhc regen skip <seed>
```

其中 `/uhc regen <seed>` 會帶著 seed 顯示 CenterCleaner 選擇提示；`confirm` 或 `skip` 才會實際建立預覽世界。

預覽世界完成後，流程會：

1. 將世界載入狀態設為 `WORLD_READY`。
2. 保存目前設定與 `MatchCenter` 到 `cache.db`。
3. 將主持人傳送到預覽中心。
4. 讓主持人用 creative 模式人工檢查地形。

`/uhc regen` 在 `LoadingStatus.DONE` 後會被拒絕，避免已跑完圖的正式世界被誤覆蓋。

## `/uhc choose`

`/uhc choose` 是「確認目前預覽世界並開始正式跑圖」。

執行後會：

1. 若目前狀態已經是 `DONE`，直接返回，不重複跑圖。
2. 將世界載入狀態改為 `GENERATING`。
3. 若執行者是玩家，保存該玩家為 Host。
4. 保存目前設定與 `MatchCenter` 到 `cache.db`。
5. 用 `commands.yml` 的 `Uhc.Choose.Kick_Init_Msg` 踢出線上玩家。
6. 下一 tick 執行伺服器重啟。

因此 `/uhc choose` 不是 `/uhc start`，也不是正式開局指令。它只是把預覽世界轉入正式預生成流程。

## Chunky 預生成

重啟後若 `LoadingStatus` 是 `GENERATING`，插件會呼叫 Chunky 對 UHC 世界預生成。

預生成中心使用目前 `MatchCenter`，半徑依初始邊界大小計算：

```text
pregeneration radius = initial border radius + 1
```

Chunky task 目前使用 square shape、region pattern、`MatchCenter` 換算後的中心，以及依本場初始邊界計算出的半徑。舊版 `settings.yml` 的 `ChunkLoading` 區塊已移除，目前不再提供 `Frequency`、`Padding` 或強制載入 Nether chunk 的舊設定。

Overworld 完成後，插件會先建立邊界，再判斷 Nether：

1. 如果本場設定啟用 Nether，繼續預生成 Nether。
2. 如果本場沒有啟用 Nether，將狀態改為 `DONE` 並重啟。

Nether 完成後也會將狀態改為 `DONE`、保存 cache，然後重啟。

## `DONE` 後的等待階段

當 `LoadingStatus` 是 `DONE` 時，世界已可開局，但比賽狀態仍是 `WAITING`。

此時開服者會看到幾個差異：

| 項目 | 行為 |
| --- | --- |
| MOTD | 顯示開放入場與目前人數。 |
| 玩家登入 | 依插件白名單、人數上限與 bypass 權限判斷。 |
| 大廳訊息 | 顯示本場主持人、scenario 與 config 提示。 |
| 熱鍵 / 選單 | 可出現隊伍相關道具與開局按鈕。 |
| 主選單 | 原本的產生地圖按鈕改為開始遊戲按鈕。 |

因此 `DONE` 不是遊戲已開始，而是「地圖已準備好，可以正常進入等待開局」。

## `/uhc start`

`/uhc start` 只會在比賽狀態為 `WAITING` 時有效。

執行後會：

1. 將 `GameTimerRunnable.RUN` 設為 `true`。
2. 播放大廳倒數開始音效。
3. 由 `LobbyCountdown` 依 `Game.Pre_Start_Time` 推進。

若目前狀態不是 `WAITING`，會顯示遊戲已經開始倒數，不會重新啟動一場新流程。

## 大廳倒數到傳送

`LobbyCountdown` 完成時會把狀態從 `WAITING` 推進到 `TELEPORTING`。

完成瞬間會：

1. 設定初始邊界。
2. 記錄目前初始邊界大小。
3. 關閉 UHC 世界的 locator bar。
4. 依隊伍設定分隊。
5. 關閉線上玩家正在開啟的選單。
6. staff 傳送到本場中心。
7. 非 staff 玩家清空狀態。
8. 啟動 `ScatterHandler`。

這個階段仍不是正式開打；它只是準備把隊伍分批傳送到 UHC 世界。

## `TELEPORTING`

`TELEPORTING` 階段由 `ScatterHandler` 控制。

流程：

1. 依 `Game.Teleport_Players_Delay` 的 tick 間隔處理隊伍。
2. 每次取一個隊伍。
3. 依目前邊界與中心，將隊伍隨機傳送到 UHC 世界。
4. 顯示該隊已傳送進度。
5. 凍結被傳送的玩家。
6. 全部隊伍處理完成後，廣播傳送完成並進入 `PRE_START`。

`Game.Teleport_Players_Delay` 在預設設定中是 `15` tick。設定越小傳送越快，但瞬間負載越高。

## `PRE_START`

`PRE_START` 是所有隊伍都已傳送後的正式開局倒數。

倒數時間由 `Game.Time_To_Start_After_Teleport` 控制。

倒數結束時會：

1. 記錄本場總玩家數。
2. 將比賽狀態推進到 `PLAYING`。
3. 設定 UHC 世界規則。
4. 解除線上玩家凍結。
5. 參賽玩家切換為 survival。
6. 清空玩家狀態並發放初始背包。
7. 套用初始經驗等級。
8. 離線參賽玩家改為觀戰身份。
9. 發出 `UHCStartedEvent`。

世界規則包含：

- 關閉 locator bar。
- 開啟時間與天氣推進。
- 將火焰蔓延半徑設為 `128`。
- 開啟生物與怪物生成。
- 設定世界出生點。
- 將 UHC 世界難度設為 hard。

## `PLAYING`

`PLAYING` 是正式遊戲階段。

主要 timer 包含：

| Timer | 用途 |
| --- | --- |
| `DAMAGE` | 開啟傷害。 |
| `FINAL_HEAL` | 執行最終回血。 |
| `PVP` | 開啟 PvP。 |
| `BORDER` | 執行邊界收縮。 |
| `NETHER_CLOSE` | 關閉 Nether 並傳回玩家。 |
| `RELOG_CHECKER` | 處理 combat relog 到期與邊界外傷害。 |
| `BORDER_SIZE_UPDATER` | 移動邊界模式下同步目前邊界大小。 |

這些 timer 會依 `settings.yml` 與本場 `UHCGameSettings` 的時間設定運作，並在設定的公告秒數發出倒數訊息。

## 遊戲結束

遊戲中死亡、隊伍淘汰與勝負判定會持續檢查最後勝利隊伍。

當出現勝利隊伍時會：

1. 將 active match 推進到 ending。
2. 廣播勝利訊息。
3. 播放勝利音效。
4. 發出 `GameEndEvent`。
5. 刪除 `cache.db`。

刪除 cache 的效果是：下一次伺服器啟動時不會把上一場當成可接續流程。

## `/uhc stop`

`/uhc stop` 的行為很直接：

1. 刪除 `cache.db`。
2. 關閉伺服器。

它適合用在：

- 主持人要中止目前場次。
- 跑圖流程卡住，確認不需要保留本場地圖後要重新開始。
- 測試環境需要清掉世界載入狀態。

它不是暫停比賽功能。使用後下一次啟動會依沒有 cache 的狀態重新進入 `CONFIGURING`。

## 登入與 MOTD 判斷

等待階段的登入判斷依世界載入狀態而不同。

| 狀態 | 一般玩家登入 | MOTD |
| --- | --- | --- |
| `CONFIGURING` | 通常被拒絕，除非有設定階段 bypass。 | 遊戲設定中。 |
| `WORLD_READY` | 通常被拒絕，除非有設定階段 bypass。 | 遊戲設定中。 |
| `GENERATING` | 被拒絕，顯示世界正在載入。 | 正在載入地圖。 |
| `DONE` | 可依插件白名單、人數上限進入。 | 開放入場與人數。 |

這也是為什麼開服者不應在 `WORLD_READY` 或 `GENERATING` 階段開放玩家正式入場。

## 常見卡住點

| 現象 | 優先檢查 |
| --- | --- |
| 玩家說進不來，顯示遊戲設定中 | `Loading_Status` 是否還是 `CONFIGURING` 或 `WORLD_READY`。 |
| 玩家說世界正在載入中 | 是否正在 `GENERATING`，以及 Chunky 是否安裝並正常啟用。 |
| `/uhc regen` 不能用 | 是否已經 `DONE`。已完成跑圖後不允許重新產生預覽世界。 |
| `/uhc choose` 後重啟但沒有完成 | 檢查 console dependency、Chunky hook、Chunky 任務與 `cache.db` 狀態。 |
| `/uhc start` 看似沒反應 | 檢查目前比賽狀態是否仍是 `WAITING`，以及是否已經啟動倒數。 |
| 開局後地圖中心不在 `0,0` | 這是新版正常行為，實際中心以 `MatchCenter` 為準。 |

## 維護原則

1. 不要把 `LoadingStatus` 當成比賽狀態，也不要把 `StateName` 當成跑圖狀態。
2. 修改 `/uhc choose`、Chunky、重啟或 cache 行為時，要同時檢查重啟後是否能恢復。
3. 修改選圖或邊界中心時，要確認 `MatchCenter` 仍會保存並被預生成、邊界與傳送流程共用。
4. 修改開局倒數時，要分清楚 `WAITING -> TELEPORTING` 與 `PRE_START -> PLAYING` 兩段。
5. 修改登入限制時，要確認 `CONFIGURING`、`WORLD_READY`、`GENERATING`、`DONE` 四種狀態的玩家體驗。
6. `cache.db` 是流程資料，不應被當成長期設定來源。
