# Step 16 死亡與死亡掉落狀況盤點

建立日期：2026-05-12

本文件用來先討論死亡、死亡掉落、`/respawn`、combat relog 與 death/drop 類 scenario 的互動範圍。這不是最終修正方案，也不是測試通過紀錄；後續每個組合仍需依文件順序逐項確認。

舊版對照來源為同 workspace 的 `WonderlandUHC` repo。舊版行為只代表原作者當時的實作方式，不一定代表最終應保留；若委託人已有新規格，仍以新規格為準。

## 委託人決策

| 議題 | 決策 | 後續處理 |
| --- | --- | --- |
| `/respawn` 是否需要避免已掉落、已進 TimeBomb 箱或已被 SwapInventory 交換的物品被還原 | 維持舊版狀況，不修防 dupe。若 Host 使用 `/respawn` 導致物品複製，視為 Host 操作責任，不列為開發問題。 | Step 16 後續不得為了 `/respawn` dupe 額外改死亡 drops 或 `DeathPlayer` 邏輯；只需確認 `/respawn` 基本功能可用。 |
| Combat relog timeout 是否等同正式死亡 | 要，timeout 死亡應套用正式死亡流程與死亡 scenario。 | relog timeout 需納入 death scenario 組合測試，包含 TimeBomb、SwapInventory、BackPack 等。 |
| Combat relog 替身實體 | 舊版與新版程式碼都使用 `EntityType.VILLAGER` 村民替身；若其他文件描述為殭屍，屬文件錯誤，不是升級 bug。 | 不需改程式碼；後續以村民替身作為正確行為。 |

## 目前基準

| 類型 | 目前行為 | 主要風險 |
| --- | --- | --- |
| 一般玩家死亡 | Bukkit `EntityDeathEvent` 轉成 `UHCGamingDeathEvent`，scenario 共用同一份 `getDrops()`。 | 多個 scenario 同時修改 drops 時，listener 順序會決定最後地上掉落與箱內物品。 |
| 無 scenario 死亡 | 目前應保留 Bukkit 原生 drops。 | 這是先前確認的升級 bug 修正點；需獨立 commit，避免混入 scenario 組合修正。 |
| `DeathPlayer` 記錄 | 死亡時保存玩家 inventory、exp、level、死亡位置，供 `/respawn` 使用。 | 若物品已掉落、已交換或已進 TimeBomb 箱，`/respawn` 可能還原同一批物品造成 dupe。 |
| Custom Drops | 核心死亡流程會額外加入設定檔 Custom Drops。 | 會被 TimeBomb 收進箱，或被其他 drops scenario 一起處理。 |
| 死後 spectator | 玩家死亡後標記 spectator、強制 respawn，再套用 spectator 狀態與傳送中心。 | 不直接改 drops，但影響 `/respawn` 可操作時機。 |

## 新版目前死亡 drops ownership 機制

本節記錄 Step 16 期間實測與修正後的目前機制。這不是全新架構，而是把既有 listener 順序與已修正的分支規則明確化，方便後續維護時判斷「這批物品目前由誰負責」。

### 核心原則

1. `UHCGamingDeathEvent#getDrops()` 是死亡流程中共用的 mutable list。`SwapInventory`、`BackPack`、`TimeBomb`、Custom Drops 都可能讀寫同一份清單。
2. 同一批物品只能由一個消費者負責：要嘛留在 `e.getDrops()` 給後續 scenario，要嘛手動掉落並清空 / 不再加入事件 drops，要嘛放進玩家物品欄或死亡箱。
3. 只要某個 scenario 已手動釋放 drops，就必須避免該批物品仍留在事件 drops 造成複製。
4. `TimeBomb` 是死亡箱流程的最終消費者之一；只要需要進死亡箱，就必須把物品留在 `e.getDrops()` 等 `TimeBomb` 處理。
5. `/respawn` 仍維持舊版行為，直接讀 `DeathPlayer` 還原死亡當下保存的 inventory；它不會知道物品是否已掉地上、進死亡箱或被交換。這個 dupe 風險已由委託人決定視為 Host 操作責任，不再用程式修補。

### 目前事件分工

| 階段 / Scenario | 目前責任 | drops ownership |
| --- | --- | --- |
| Bukkit / Paper 原生死亡 | 產生初始 `EntityDeathEvent#getDrops()`。 | 預設由 Bukkit 負責最後掉落，但後續 scenario 可能接手。 |
| `PlayingDeathListener` / relog | relog 替身死亡時，改用 relog 保存 inventory 作為 drops 來源。一般玩家死亡則保留 Bukkit 原生 drops。 | relog drops 仍放在同一份事件 drops 清單中。 |
| Custom Drops | 把設定檔 Custom Drops 加進事件 drops。 | 交給後續 scenario、Bukkit 或手動釋放分支處理。 |
| `SwapInventory` | 交換 victim / killer 物品欄，並把死亡 drops 改成 killer 原本物品欄，加上不屬於 victim 物品欄的既有 drops。 | 若 `TimeBomb` 開啟，交換後 drops 留在事件清單給死亡箱；若 `TimeBomb` 未開，`SwapInventory` 手動在死亡位置釋放並清空事件 drops。 |
| `BackPack` | 隊伍淘汰時處理被淘汰隊伍的 team backpack。 | 若 `SwapInventory` 有 killer，背包物品塞進 killer 物品欄，overflow 掉在 killer 位置；若 `TimeBomb` 開啟且未被 `SwapInventory` 接手，背包物品加入事件 drops 給死亡箱；若兩者都不接手，`BackPack` 手動在死亡位置釋放。 |
| `TimeBomb` | 在 HIGHEST priority 建立死亡箱，把事件 drops 盡量放入箱內。 | 死亡箱容量內物品進箱；overflow 手動掉在死亡位置；完成後清空事件 drops，避免重複掉落。 |
| relog timeout 手動死亡 | timeout 不是 Bukkit 原生死亡，因此死亡事件跑完後由 relog timeout 邏輯手動釋放剩餘 drops。 | 若 `TimeBomb` 已清空 drops，timeout 不再額外掉落同批物品；若沒有其他消費者，剩餘 drops 會被手動掉出。 |

### `SwapInventory` 改寫後規則

`SwapInventory` 的核心輸出不是「victim 物品掉地上」，而是：

1. killer 取得 victim 目前物品欄；若死亡實體是 relog 替身，使用 relog 保存 inventory。
2. victim 的物品欄變成 killer 原本物品欄。
3. 死亡 drops 變成 killer 原本物品欄，加上事件 drops 中不屬於 victim / relog inventory 的物品。
4. 未開 `TimeBomb` 時，交換後 drops 由 `SwapInventory` 立即手動掉在死亡位置，並清空事件 drops。
5. 開啟 `TimeBomb` 時，交換後 drops 留在事件 drops，讓 `TimeBomb` 收進死亡箱；此時 `SwapInventory` 不手動掉落，避免同批物品一邊進箱、一邊掉地上。

這個規則用來解決未開 `TimeBomb` 時 killer 原物品消失的問題，同時保留 `TimeBomb + SwapInventory` 已驗證的「死亡箱放 killer 原物品」行為。

### `BackPack` 改寫後規則

`BackPack` 只在隊伍淘汰時處理該隊伍的 team backpack。處理後會清空被淘汰隊伍背包，避免同一批背包物品保留在原背包中。

1. 若 `SwapInventory` 開啟且本次死亡有 killer，team backpack 物品視為 killer 戰利品，直接塞進 killer 物品欄；塞不下的 overflow 掉在 killer 位置。
2. 若沒有 `SwapInventory` 接手，但 `TimeBomb` 開啟，team backpack 物品加入事件 drops，交給 `TimeBomb` 放進死亡箱或 overflow 掉落。
3. 若 `SwapInventory` 與 `TimeBomb` 都沒有接手，`BackPack` 自己把 team backpack 物品掉在死亡位置。
4. 空背包格、`null` 與 `AIR` 不會加入 drops，避免 `TimeBomb` 建箱時遇到無效物品。

這個規則用來補齊 `BackPack` 單開時「隊伍背包被清空但沒有掉落」的缺口，同時不改變 `BackPack + TimeBomb + SwapInventory` 的既有驗收結果。

### `TimeBomb` 改寫後規則

`TimeBomb` 只處理事件 drops 當下看到的內容，因此前面 scenario 改 drops 的結果會直接決定死亡箱內容。

1. 建立雙箱後，依死亡箱容量建立 storable drops snapshot。
2. snapshot 內的物品放進死亡箱。
3. 超過死亡箱容量的剩餘 drops 由 `TimeBomb` 手動掉在死亡位置。
4. 入箱與 overflow 都處理完後，清空事件 drops，避免 Bukkit 或 timeout 後續再次掉落同批物品。
5. 若建箱失敗，`TimeBomb` 會停用本場 scenario，並保留 regular death drops 繼續走後續流程。

這個規則保留原作者「TimeBomb 接管死亡 drops」的設計，但修正舊版會直接清空 overflow、導致超出死亡箱容量物品消失的問題。

### `DamageDogers` 與 killer 保留規則

`DamageDogers` 觸發時不能直接 `setHealth(0)`，因為直接把血量設為 0 可能讓 Paper/Bukkit 後續死亡流程遺失原始 attacker / killer，導致 `SwapInventory`、`TimeBomb`、`BackPack` 判斷不到 killer。

目前改成在原本的 `UHCPlayerDamageEvent` 上把傷害改成致死值，讓死亡仍由原 damage event 推進。這樣可以保留 Bukkit 原本的 killer 歸屬，並讓 death / inventory 類 scenario 走同一條正式死亡流程。

### `ShiftKill` 與 `NoClean` 規則

`ShiftKill` 懲罰屬於 scenario 規則，不應被 `NoClean` 擊殺後給予的短暫無敵取消。

目前 `ShiftKill` 對 killer 造成懲罰傷害時，只繞過插件內部 `InvinciblePlayer` 的取消邏輯，不改變 Bukkit 原本的傷害 / 死亡事件流程。外部傷害仍維持 `NoClean` 保護。

## 舊版核心死亡流程

| 類型 | 舊版原作者處理 | 對目前判斷的意義 |
| --- | --- | --- |
| 一般玩家死亡 drops | 舊版在 `PlayingDeathListener#setDropAsInventoryItem()` 會先從 `InventoryContent.contentsOf(player)` 重建玩家物品，然後 `e.getDrops().clear()` 再塞回 `UHCGamingDeathEvent#getDrops()`。 | 「核心死亡流程接管 drops」是原作者設計，不是新版才出現；但新版無 scenario 不掉落是升級後此設計失效，需要修。 |
| combat relog drops | 舊版同一方法若找到 `CombatRelog`，會改用 `relog.getInventoryContent().getAllItems()` 作為 drops 來源。 | relog 替身死亡時，drops 來源應是離線時保存的 inventory。 |
| `DeathPlayer` 保存 | 舊版死亡時同樣在 LOWEST priority 建立 `DeathPlayer`，保存死亡當下 inventory、位置、exp、level。 | `/respawn` 會還原死亡當下 inventory 是舊版設計；它沒有知道物品後續是否已被 TimeBomb 或 SwapInventory 接管。 |
| Custom Drops | 舊版 NORMAL priority 直接把設定檔 Custom Drops 加進 `e.getDrops()`。 | Custom Drops 本來就會跟 TimeBomb、SwapInventory、BackPack 共用同一份 drops。 |
| 死後 spectator | 舊版死亡時直接 `changeSpectatorRole()`，新版因 1.21 respawn 流程有調整，改成標記後延遲 respawn / 套用 spectator。 | 這是升級相容調整，不應影響 drops 歸屬判斷。 |

## 指令與非 scenario 功能

| 指令 / 功能 | 目前行為 | 需要討論 |
| --- | --- | --- |
| `/respawn <玩家>` | 從 `DeathPlayer` 還原背包、等級、經驗、死亡位置，並給重生無敵。 | 最大風險是跟 TimeBomb、SwapInventory、一般地上掉落產生 dupe。 |
| `/staff` | 遊戲中活人切 staff 會建立一份 `DeathPlayer`，但不觸發死亡掉落。 | 這是保存狀態，不是死亡流程；需決定是否排除 Step 16 手動測試。 |
| `/giveall` | 會給在線玩家物品；若玩家在 combat relog 中，會加到 relog 保存 inventory。 | 影響 relog 替身死亡時的掉落內容。 |
| `/practice` | 練習模式死亡會清空 drops、補滿死者與 killer 血量、延遲補 kit。 | 這不是正式 UHC 死亡，但會造成「死亡沒掉落」的合法情境。 |
| `/backpack` | 指令本身只存取隊伍背包；真正死亡 drops 由 BackPack scenario 處理。 | 需要跟 TimeBomb、SwapInventory 一起討論物品歸屬。 |

## 舊版指令與非 scenario 功能

| 指令 / 功能 | 舊版原作者處理 | 對目前判斷的意義 |
| --- | --- | --- |
| `/respawn <玩家>` | 舊版直接讀 `DeathPlayer`，清空玩家、切回 SURVIVAL、還原保存的 inventory / exp / level，並傳回死亡位置或邊界內隨機點。 | 舊版沒有針對「物品已掉地上、已進 TimeBomb 箱、已被 SwapInventory 交換」做防 dupe。這比較像原設計缺口，不是 1.21 專屬升級 bug。 |
| `/staff` | 舊版活人切 staff 也會建立 `DeathPlayer`，但不觸發死亡事件與 drops。 | 這條路應視為主持工具保存狀態，不應直接跟死亡掉落混在一起判斷。 |
| `/giveall` | 舊版也會給在線玩家物品；離線 relog 玩家則加到 `CombatRelog` 保存 inventory。 | relog 替身死亡時包含 `/giveall` 後新增物品，是舊版設計。 |
| `/practice` | 舊版 practice death 同樣 `e.getDrops().clear()`，補滿死者與 killer 血量，延遲補 practice kit。 | practice 中死亡不掉落是原作者特性，不應當成 UHC 正式死亡 bug。 |
| `/backpack` | 舊版 `/backpack` 只是存取隊伍背包；死亡加入 drops 由 BackPack scenario listener 負責。 | 背包物品歸屬應回到 BackPack scenario 與 death drops 順序討論。 |

## Combat Relog

| 狀況 | 目前行為 | 主要風險 |
| --- | --- | --- |
| 玩家離線，relog 關閉 | 直接 `setHealth(0)`，走正常玩家死亡流程。 | 應等同一般玩家死亡掉落。 |
| 玩家離線，relog 開啟 | 生成 villager 村民替身，保存 inventory、血量、exp、藥水。 | 保存 inventory 是後續替身死亡的掉落來源。 |
| 玩家回來 | 移除替身，把替身狀態與保存 inventory 還原給玩家。 | 若替身已死亡但 cache 沒清乾淨，可能造成狀態或物品錯亂。 |
| 替身被殺 | 替身也會觸發 `UHCGamingDeathEvent`，drops 來源是 relog 保存 inventory。 | SwapInventory、TimeBomb、BackPack 都可能套用。 |
| relog timeout | Timer 手動建立 `EntityDeathEvent`，跑死亡流程後把剩餘 drops 手動丟地上。 | 這條路不是 Bukkit 原生死亡，需要單獨測剩餘 drops、TimeBomb、SwapInventory。 |

## 舊版 Combat Relog

| 狀況 | 舊版原作者處理 | 對目前判斷的意義 |
| --- | --- | --- |
| 玩家離線，relog 關閉 | 舊版同樣在 quit 時 `player.setHealth(0)`。 | 這條路應走一般玩家死亡規則。 |
| 玩家離線，relog 開啟 | 舊版同樣建立 `EntityType.VILLAGER` 村民替身，保存 inventory、血量、exp、藥水。 | 替身使用村民是原作者設計；不是從殭屍變村民的升級 bug。 |
| 玩家回來 | 舊版同樣把替身血量、inventory、exp、藥水還原給玩家，延遲移除替身並傳送到替身位置。 | 若替身死亡後仍可回復，才是 cache 清理問題。 |
| 替身被殺 | 舊版替身透過 metadata 取得 `UHCPlayer`，進入同一套 `UHCGamingDeathEvent`。 | 替身被殺應納入 death scenario 組合測試。 |
| relog timeout | 舊版 timer 只手動 `Common.callEvent(new EntityDeathEvent(relog.getEntity(), Lists.newArrayList()))`，接著移除替身與 relog cache，沒有額外把 event drops 丟到世界。 | 舊版 timeout 是否真的一般噴裝需要實測或接受為舊版缺口；新版目前有手動丟出 event 剩餘 drops 的相容修正。 |

## Combat Relog 實測紀錄

測試日期：2026-05-13

| 項目 | 結果 | 判斷 |
| --- | --- | --- |
| relog timeout，無 scenario | 超時死亡有掉出離線玩家物品。 | 符合委託人「timeout 等同正式死亡」要求。 |
| relog timeout + `TimeBomb` | 符合，死亡箱流程正常。 | 通過。 |
| relog timeout + `BackPack` | 單獨搭配符合；與 `TimeBomb` 搭配也符合。 | 通過。 |
| relog 替身被玩家殺 + `SwapInventory` | 符合。 | 通過。 |
| relog 替身被玩家殺 + `SwapInventory` + `TimeBomb` | 符合；搭配 `BackPack` 時 overflow 維持一般掉落。 | 通過。 |

## Death / Drop Scenario

| Scenario | 單獨行為 | 主要組合風險 |
| --- | --- | --- |
| `NoClean` | killer 擊殺後獲得無敵。 | 會取消 `ShiftKill` 的自傷；委託人已指定 ShiftKill 應優先。 |
| `ShiftKill` | 非蹲下擊殺時 killer 扣目前血量與吸收血量的一半。 | 委託人已指定要統一致死規則，會影響低血 killer 雙死。 |
| `SwapInventory` | 死亡者與 killer 交換 inventory，並把死亡 drops 改成 killer 原物品。 | 跟 TimeBomb、BackPack、`/respawn` 最危險。 |
| `TimeBomb` | 把死亡 drops 放進雙箱，箱內物品倒數後爆炸，overflow 留一般掉落。 | 跟 `/respawn` 容易 dupe；跟 SwapInventory 決定箱內是哪方物品。 |
| `BackPack` | 隊伍淘汰時，把隊伍 backpack 內容加進 death drops。 | 跟 TimeBomb 容量、overflow、SwapInventory 交換後歸屬要確認。 |
| `ArmorVsHealth` | 裝甲會降低最大血量，重生後也會重新處理。 | 會影響 ShiftKill 致死機率與 `/respawn` 後血量。 |
| `IronMan` | 傷害與 respawn 相關狀態。 | 跟死亡重生、ArmorVsHealth 有狀態還原風險。 |
| `AbsorptionLess` | 移除吸收效果。 | 會影響 ShiftKill 計算吸收血量。 |
| `CutClean` | 動物死亡 drops 轉熟食。 | 不是玩家死亡，但也改 `EntityDeathEvent#getDrops()`，可列為非玩家 death drop。 |

## 舊版 Death / Drop Scenario

| Scenario | 舊版原作者處理 | 對目前判斷的意義 |
| --- | --- | --- |
| `NoClean` | 舊版在 `UHCGamingDeathEvent` 預設 priority 給 killer 無敵。 | 與 `ShiftKill` 同 priority，實際順序可能讓無敵先套用並取消自傷；委託人已指定新規格為 ShiftKill 優先。 |
| `ShiftKill` | 舊版在 killer 非蹲下時執行 `killer.damage((killer.getHealth() + absorption) / 2)`。 | 低血不一定致死是舊版行為；委託人已指定「shiftkill 統一會致死」則屬新規格變更。 |
| `SwapInventory` | 舊版 LOW priority：先從 drops 移除死亡者 inventory 物品，再加入 killer inventory 物品，最後交換雙方 inventory；若死亡實體是 relog 替身，先把 relog inventory 改成 killer inventory。 | 「死亡 drops 變成 killer 原物品，死亡者物品到 killer 身上」是舊版設計；跟 TimeBomb 同開時，TimeBomb 會吃到 SwapInventory 後的 drops。 |
| `TimeBomb` | 舊版 HIGHEST priority：建立雙箱，最多把前 54 個 drops 放進箱子，最後直接 `e.getDrops().clear()`。 | 舊版沒有 overflow 保留；新版目前保留 overflow 是相容改善。TimeBomb 會接管當下 drops，因此跟 SwapInventory 順序非常關鍵。 |
| `BackPack` | 舊版在隊伍淘汰時直接把 team backpack contents 加進 `e.getDrops()`。 | 背包內容本來就會被後續 TimeBomb 收進箱，或跟 SwapInventory 後的 drops 混在一起。 |
| `ArmorVsHealth` | 舊版會在 respawn event 後延遲重新計算裝甲造成的最大血量扣減。 | `/respawn` 後血量變化是原作者設計的一部分，測 ShiftKill 時要注意最大血量不是固定 20。 |
| `IronMan` | 舊版也會處理 damage / respawn 狀態。 | 屬於死亡重生後狀態類風險，不直接決定 drops。 |
| `AbsorptionLess` | 舊版食用後移除吸收效果。 | 舊版 ShiftKill 使用當下 absorption 計算；是否保留時機差異需依測試結果決定。 |
| `CutClean` | 舊版在一般 `EntityDeathEvent` 上把動物肉類 drops 換成熟食。 | 這是非玩家死亡 drops 修改，不應混入玩家死亡核心修正。 |

## 初步處理順序建議

1. 「無 scenario 正常掉落」已先單獨修正；這是升級 bug，不是舊版預期。
2. 再決定本文件哪些項目屬於 Step 16 必測，哪些只記錄不測。
3. `/respawn` dupe 已決定維持舊版狀況，不作為 Step 16 修正目標。
4. 第一個高風險組合建議從 combat relog timeout / 替身被殺搭配 TimeBomb、SwapInventory、BackPack 開始，因為委託人已指定 timeout 應等同正式死亡。
5. Combat relog 的替身被殺與 timeout 需獨立列測，不能用一般玩家死亡結果直接代表。
6. `/practice`、`/staff` 屬於特殊功能路徑，先確認是否納入 Step 16；若納入，應與正式 UHC 死亡分開記錄。
