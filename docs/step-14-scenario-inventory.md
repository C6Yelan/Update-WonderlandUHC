# Step 14 Scenario 盤點與高風險相容層隔離

本文件承接 `docs/steps.md` Step 14。這一步只做 scenario 盤點、分類、feature gate 與高風險相容層隔離；不導入完整 `RuleEngine`，也不重寫 scenario GUI。

## 目前註冊方式

- 預設 scenario 由 `ScenarioManager#registerDefaults()` 建立，共 38 個。
- 大多數 scenario 繼承 `ConfigBasedScenario`，並在 `reload()` 時讀取 `scenarios.yml`。
- scenario 啟用後才註冊 listener；`ScenarioListener` 在遊戲開始時會重新 disable / enable 已啟用 scenario，讓 listener 重新掛上。
- `ScenarioConfig` 目前負責讀取 icon material、name、description、`@FilePath` 欄位與 `SimpleSound`。

## 盤點分類

| Scenario | 分類 | 主要依賴 / 風險 | 1.21.11 保留策略 | 建議測試方式 |
| --- | --- | --- | --- | --- |
| Absorption_Less | consume / Bukkit event | `PotionEffectType.ABSORPTION` | 保留，現有 Bukkit API 可用 | 食用金蘋果後確認吸收效果被移除 |
| Backpack | inventory / command | `UHCTeam` shared inventory、`/backpack` | 保留，屬於隊伍功能 | 隊伍成員開啟背包與非啟用時錯誤訊息 |
| Bench_Blitz | crafting / config sound | `PrepareItemCraftEvent`、`SimpleSound` | 保留；config / sound 讀取風險已由 `ScenarioConfig` 與 `ScenarioManager` 隔離 | 製作第一個與第二個 crafting table |
| Blood_Diamonds | block/drop | `UHCBlockBreakEvent`、legacy material adapter | 保留 | 挖鑽石礦扣血 |
| Bow_Less | disable / Bukkit event | `EntityShootBowEvent` | 保留 | 射弓被取消 |
| Cut_Clean | block/drop/entity drop | 多個 1.21 material 名稱 | 保留，後續 Step 15 再補完整掉落規則 | 挖礦與殺動物掉落熟食 / ingot |
| Damage_Dogers | damage/death | `SimpleSound`、傷害事件取消/死亡處理 | 保留，高行為風險；已補 damage event runtime 隔離，盾牌判斷、死亡處理、廣播或音效失敗時停用單一 scenario | 前 N 位受傷玩家死亡與訊息 |
| Diamond_Less | block/drop | `UHCBlockBreakEvent`、legacy material adapter | 保留 | 鑽石礦不掉鑽石 |
| Double_Or_Nothing | block/drop/config list | `List<Material>` config、`SimpleSound` | 保留；material list / sound 讀取風險已由 `ScenarioConfig` 與 `ScenarioManager` 隔離 | 指定方塊隨機雙倍或清空掉落 |
| Fast_Obsidian | block/effect | `PotionEffectType.HASTE`、block damage event | 保留 | 挖黑曜石時給 haste |
| Fast_Smelting | furnace/task | furnace inventory 與排程 | 保留，高行為風險；已補 furnace boost task runtime 隔離、停用清理、world-aware key 與 burn / cook time 邊界 | 熔爐燒製速度 |
| Fragile_Rods | item durability | `PlayerUtils.costPlayerToolDurability` | 保留，後續 Step 18 清 Paper item API | 魚竿甩出扣耐久 |
| Fire_Less | damage | fire/lava damage cause | 保留 | 火焰與岩漿傷害被取消 |
| Food_Neophobia | consume/config sound | material allowlist、player metadata | 保留 | 第一次食物鎖定與例外食物 |
| Gold_Less | block/drop | `UHCBlockBreakEvent`、legacy material adapter | 保留 | 金礦不掉黃金 |
| Hasty_Boys | crafting/enchant | item enchant 產生 | 保留 | 合成工具附加效率 III |
| Horse_Less | disable / Bukkit event | entity mount | 保留 | 騎馬被取消 |
| Iron_Man | health/global state | max health 修改、game phase | 保留，高行為風險；已補 damage / final heal / respawn runtime 隔離與停用時逐人還原保護 | 最終回血前未受傷加血，停用後還原 |
| Less_Bow_Damage | damage/config | projectile damage scaling | 保留 | 弓箭傷害降低百分比 |
| Limitations | block/drop/config map | ore material、per-player state、`SimpleSound` | 保留，高行為風險；已補 block break runtime 隔離，計數、掉落替換、訊息或音效失敗時停用單一 scenario | 達到挖礦上限後掉落清空 |
| Lucky_Leaves | block/drop/random | leaves decay/break 與掉落 | 保留 | 樹葉機率掉金蘋果 |
| No_Clean | death/invincible | `InvinciblePlayer` state | 保留 | 擊殺後給無敵時間 |
| No_Enchant | disable / Bukkit event | enchant event | 保留 | 附魔被取消 |
| No_Fall | damage | fall damage cause | 保留 | 摔落傷害被取消 |
| Potion_Less | consume/brewing | 1.9 lingering potion、1.14 effect event | 保留，version listener 已隔離在 `initListeners()` | 飲用、丟擲、滯留與釀造藥水被取消 |
| Rod_Less | disable / Bukkit event | fishing rod use | 保留 | 釣竿使用被取消 |
| Shift_Kill | death / legacy NMS fallback | `LegacyDatouNmsAdapter#getAbsorptionHearts` | 保留；已補 DatouNMS linkage 失敗時走 Bukkit `getAbsorptionAmount()` fallback，死亡事件失敗時停用單一 scenario | 非蹲下擊殺扣一半血含吸收血量 |
| Silk_Web | block/drop/legacy hand API | `getItemInHand()`、legacy material adapter | 保留，Step 18 再清手持 API | 剪刀挖蜘蛛網掉蜘蛛網，其他掉線 |
| Soup | consume/health | max health 讀取、material adapter | 保留 | 喝湯補血並變碗 |
| Swap_Inventory | death/inventory | 死亡時交換背包與裝備 | 保留，高行為風險；已補死亡事件 runtime 隔離、drops 快照計算與 inventory / combat relog rollback | 擊殺後交換 inventory |
| Switcheroo | damage/projectile/config sound | projectile hit teleport、`SimpleSound` | 保留 | 弓箭命中後交換位置 |
| Timber | block/tree | `VeinMiner` / block break fallback | 保留，高行為風險；已補 block break runtime 隔離、mining 狀態 cleanup 與單一方塊 break fallback | 挖一塊木頭破壞整棵樹 |
| Time_Bomb | death/inventory / legacy NMS fallback | `LegacyDatouNmsAdapter#mergeLargeChest`、ticker、chest inventory | 保留，高風險；已補死亡箱建立失敗時停用單一 scenario、容量不足時保留 overflow 掉落、ticker 單箱失敗隔離 | 死亡後生成大箱子、倒數、爆炸、已入箱掉落從死亡掉落移除 |
| Triple_Arrow | projectile / legacy hand API | `getItemInHand()`、metadata、箭矢扣除 | 保留，Step 18 再清手持 API | 射箭額外生成兩支箭，Infinity 不扣箭 |
| Triple_Ores | block/drop | `UHCBlockBreakEvent` | 保留 | 礦物掉落三倍 |
| Vanilla_Plus | block/drop/random | gravel/flint drop replacement | 保留 | gravel 掉 flint 機率提高 |
| Vein_Miners | block/recursive break | `VeinMiner` 連鎖破壞 | 保留，高行為風險；已補 block break runtime 隔離，共用 `VeinMiner` 已具備 mining cleanup 與單一方塊 break fallback | 挖礦連鎖破壞相鄰礦 |
| Armor_Vs_Health | health/global state | max health 修改、inventory events、respawn delay | 保留，高風險；已補 event / delayed task runtime 隔離、armor point linkage fallback 與 max health 下限保護 | 裝備值扣最大血量，重生後延遲套用 |

## Step 14 第一階段實作邊界

1. `ScenarioManager` 註冊與重載必須逐項隔離。
2. 單一 scenario 建構、`reload()` 或 config 讀取失敗時，應記錄為 unavailable，不得中斷其他 scenario 註冊。
3. unavailable scenario 不應出現在一般 scenario 清單或啟用清單中。
4. 後續 Step 15 才逐項補完整行為與 rule module；Step 17 才處理 scenario menu presentation。

## 已知後續項目

- `ScenarioConfig` 已補上 scenario icon、`Material`、`List<Material>` 與 `SimpleSound` 的 alias / namespace 解析；後續若遇到更多舊 alias，應只擴充本地 alias 表，不把問題擴成 scenario 架構重寫。
- Step 14 程式隔離與本輪回測 blocker 已完成；`ScenarioSettingsMenu`、核心死亡流程、`Armor_Vs_Health`、`Vein_Miners` 與 `Time_Bomb` 已完成實機確認；`Timber` 走同一個 `VeinMiner` 玩家原生破壞 fallback，不再另列 Step 14 blocker。
- `Damage_Dogers`、`Shift_Kill`、`Iron_Man`、`Limitations`、`Swap_Inventory`、`Fast_Smelting` 目前已有 runtime 隔離；逐項完整玩法差異與 rule module 收斂留到 Step 15，不再列為 Step 14 blocker。
- `getItemInHand()` 等 Bukkit 舊 API 清理留到 Step 18，除非它直接阻擋 Step 14 的啟動安全。

## 2026-05-10 實跑驗收紀錄

以下是 Step 14 收尾前的實測結果。分類只代表下一步處理順序，不代表功能可被移除。

### Step 14 收尾回測結果

1. `ScenarioSettingsMenu` 分頁 / 單頁顯示。
   - 現象：點 scenario menu 第二頁時 console 出現 `Could not find class: net.minecraft.network.chat.IChatBaseComponent$ChatSerializer`，來源是 Foundation `MenuPagged#updatePage()` 呼叫舊 NMS inventory title 更新。
   - 處理結果：`lib-foundation` 的 paged menu 換頁已改為重新開啟同一個 menu 狀態，讓 Bukkit 建立帶新標題的 inventory；scenario menu 本輪改為 `Rows: 6`，目前預設 scenario 可在單頁 45 格內容區顯示，既有 `gui.yml` 也會 migration 到 6 rows。
   - 回測結果：目前已是一頁顯示；若未來 scenario 數量超過單頁內容區，仍會回到 Foundation paged menu 流程。

2. 核心死亡流程修正。
   - 現象：死亡後無法按下重生按鈕；按 title screen 再 respawn 後會進入假死狀態，無法操作；`/respawn` 也無法復活該玩家；重進後恢復正常，且可成功復活。
   - 影響：會阻塞所有 death 類 scenario 實測，包括 `Time_Bomb`、`Damage_Dogers`、`Shift_Kill`、`Swap_Inventory`。
   - 處理結果：`PlayingDeathListener` 會先把 UHC 內部角色標成 spectator，但延後到新版可用的 `Player#spigot().respawn()` 後才套用 spectator 的清背包 / gamemode，避免在原版死亡流程尚未結束時改動玩家狀態；套用 spectator 後會傳送到賽事中心點；`/respawn` 指令也會先把仍在原版死亡畫面的玩家復活，再延後還原 inventory、位置與遊戲狀態。
   - 回測結果：死亡後不再卡 `You Died!`，玩家可正常進入 spectator 並位於中心點。

3. `Time_Bomb` 死亡箱、倒數與爆炸修正。
   - 現象：死亡時大箱子與物品存放正常；超過時間後沒有爆炸，也沒有明確倒數。
   - 錯誤：`NullPointerException` at `ScenarioTimeBomb$TimeBombRunner.putItemsIn(ScenarioTimeBomb.java:135)`，Paper `TransformingRandomAccessList#get` 讀到不相容狀態；目前 runtime guard 會停用 `Time_Bomb` 並保留一般死亡掉落。
   - 處理結果：死亡流程建立 drops 時會過濾 null / AIR，避免玩家 inventory 空格進入 death drops；`Time_Bomb` 放入箱子前會先建立可存物品快照，不再直接索引 Paper 的轉換包裝 list。
   - 回測結果：死亡箱可放入物品，倒數結束後會爆炸並廣播訊息；未再出現 `TransformingRandomAccessList#get` / `ScenarioTimeBomb` 錯誤。

4. `Armor_Vs_Health` 裝備血量修正。
   - 現象：第二頁可測後，穿戴一般護甲可以扣最大血量，但銅裝沒有判定，且可能因同一 tick 多個裝備事件重複扣血。
   - 初判：1.21 下 DaTouNMS 不支援，原本 armor point fallback 回傳 0；第一輪修正後又發現延遲扣血會讓多個同 tick 事件讀到相同舊成本。
   - 處理結果：已在 `LegacyDatouNmsAdapter#getArmorPoints` 優先讀 Bukkit armor attribute，讀不到時用原版護甲值規則 fallback（包含銅裝）；`ScenarioArmorVsHealth` 改為立即記錄玩家 UUID 成本，避免同一批事件重複扣血，停用 scenario 時會補回本 scenario 實際扣掉的 max health。
   - 回測結果：一般裝備與銅裝會扣最大血量，重複點擊不會重複扣血，中途關閉會正常回復血量。

### Step 15 行為補齊候選

1. `Limitations` 深板岩礦物無法判定。
   - 判定：1.21 material / deep ore 規則補齊，屬 Step 15。

2. `Limitations` 使用 silk touch pickaxe 挖礦可以累積上限，但超過上限後仍會掉落。
   - 判定：掉落替換規則需重新檢查 `UHCBlockBreakEvent` drops 與 Bukkit silk touch drops，屬 Step 15 行為補齊。

3. `Fast_Smelting` 第一次燒礦 / 食物會加速；把物品全拿下再放回去後變回原版速度。
   - 判定：boost task 在無 smelting item 時停止後，下一輪 burn event / cook state 沒有重新建立加速；屬 Step 15 行為補齊。
   - 備註：目前程式只判斷 `Material.FURNACE`，依現有實作與舊版範圍先視為只支援一般熔爐；是否擴充到 `BLAST_FURNACE` / `SMOKER` 需另行確認。

4. `Double_Or_Nothing` 深板岩礦物無法判定。
   - 判定：1.21 material / deep ore 規則補齊，屬 Step 15。

### 2026-05-11 回測補充

- `ScenarioSettingsMenu` 曾因 Foundation paged title update 在 1.21 失效而無法穩定切頁；本輪最終改為 `Rows: 6` 單頁顯示，目前預設 scenario 數量可放進 45 格內容區，並補上既有 `gui.yml` migration，讓舊伺服器資料夾也會升到 6 rows。
- `Vein_Miners` 蹲下挖礦連鎖本身可運作；本輪已先把 fallback 改成玩家原生破壞流程，回測確認經驗、耐久與 fortune 可正常連鎖運作。
- `Armor_Vs_Health` 一般護甲可扣最大血量，但銅裝漏判定、可能重複扣血，且中途停用不會歸還血量；本輪改用 Bukkit armor attribute + 原版規則 fallback，並修正同 tick 重複扣血與停用還原，回測確認一般裝備與銅裝會扣最大血量、重複點擊不會重複扣血、中途關閉會正常回復血量。
- 核心死亡流程本輪改成延後 `spigot().respawn()` 後套用 spectator 狀態並傳送中心點，回測確認不再卡 `You Died!` 且 spectator 位置正確。
- `Time_Bomb` 本輪改為 death drops 過濾 null / AIR 並在放箱前建立可存物品快照，回測確認死亡箱、倒數、爆炸與廣播正常。

## Step 14 收尾結論

- Step 14 的盤點、分類、單一 scenario 隔離與高風險 runtime guard 已完成。
- 本輪發現的 Step 14 blocker 已完成實機回測：scenario menu、`Armor_Vs_Health`、`Vein_Miners`、核心死亡流程與 `Time_Bomb`；`Timber` 已套用同一個 `VeinMiner` 修正路徑。
- `Limitations` / `Double_Or_Nothing` 深板岩礦物、`Limitations` silk touch 超上限掉落、`Fast_Smelting` 第二輪加速等屬 Step 15 行為補齊，不阻塞 Step 14 收尾。
