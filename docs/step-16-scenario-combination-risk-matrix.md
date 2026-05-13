# Step 16 Scenario 組合風險盤點

本文件承接 `docs/steps.md` Step 16，作為多個 scenario 同時啟用時的驗收清單。文件會同時記錄靜態風險、委託人優先級、實機驗收結果，以及可由更高階組合覆蓋的重複測試。

## 範圍原則

- 盤點基準是 `ScenarioManager#registerDefaults()` 目前註冊的 38 個預設 scenario。
- Step 15 已完成單一 scenario 驗收；本文件只處理 scenario 彼此組合後可能互相覆蓋、放大或改變的行為。
- 若組合風險來自舊版原作者設計，後續實測時要標為舊版特性或接受差異，不直接視為 regression。
- 若無法找到舊版行為，後續實測結果需標為「程式碼推論」或「待確認」。
- 本文件不列滿全部 703 個二元組合；低風險且沒有共用事件、共用狀態或共用輸出的組合只做抽樣確認。完整排列組合的判斷基準是「同事件、同 drops、同 inventory、同 max health、同 projectile、同 consume/craft/task 狀態」。

## 驗收標記原則

- 「直接通過」代表該組合本身已在實機測過。
- 「覆蓋通過」代表更高階組合已測到相同程式分支與相同行為輸出，後續不重複測同一條路徑。
- 「仍需單測」代表雖然與已測組合相似，但少開或多開某 scenario 會改走不同分支，不能用高階組合直接代替。
- `/respawn` 造成的 dupe 維持委託人決議：屬 Host 操作責任，不作為 Step 16 開發修復項目。

## 風險等級

| 等級 | 意義 | 處理方式 |
| --- | --- | --- |
| P0 | 可能直接改變死亡、掉落、背包、最大血量、連鎖挖礦或礦物收益 | Step 16 優先實測 |
| P1 | 可能因事件順序、取消狀態或共享玩家狀態產生可接受差異或舊版特性 | P0 後實測 |
| P2 | 表面互動較弱，但仍需抽樣避免單項驗收被組合破壞 | 視時間抽樣 |

## 全部 Scenario 互動面

| Scenario | 主要互動面 | 組合注意點 |
| --- | --- | --- |
| `AbsorptionLess` | 食用後移除吸收效果 | 會影響 `ShiftKill` 對吸收血量的懲罰計算時機。 |
| `BackPack` | 隊伍背包、死亡 drops | 與 `TimeBomb`、`SwapInventory`、團隊淘汰時的死亡 drops 高度重疊。 |
| `BenchBlitz` | 合成工作台、craft prepare | 與 `HastyBoys` 同用 crafting 事件，但目標物品不同，低風險抽樣即可。 |
| `BloodDiamonds` | 鑽石礦扣血、`VeinMiner` 連鎖數量 | 與 `VeinMiners`、`NoClean`、`ArmorVsHealth`、`Limitations` 有血量與礦物互動。 |
| `BowLess` | 取消射弓 | 會壓過 `TripleArrow`、`LessBowDamage`、`Switcheroo` 的弓箭路線。 |
| `CutClean` | 礦物 / 動物 drops 替換、exp | 與多數 block/drop scenario 共用 `UHCBlockBreakEvent#getDrops()`。 |
| `DamageDogers` | 前 N 位受傷玩家直接死亡 | 會觸發完整死亡流程，需與 death/inventory 類一起測。 |
| `DiamondLess` | 移除鑽石掉落 | 與 `BloodDiamonds`、`CutClean`、`TripleOres`、`DoubleOrNothing` 共用鑽石礦 drops。 |
| `DoubleOrNothing` | 指定方塊 drops 隨機雙倍或清空 | 與 `TripleOres`、`CutClean`、`Limitations`、`VeinMiners` 有乘算 / 清空順序風險。 |
| `FastObsidian` | 挖黑曜石給 haste | 與 `HastyBoys` 可能堆疊挖掘速度。 |
| `FastSmelting` | 熔爐加速 task | 與 `CutClean` 的自動熟化路線互斥程度需確認，但不直接共用事件。 |
| `FragileRods` | 魚竿發射扣耐久 | 與 `RodLess` 同用 projectile launch，可能出現被禁止仍扣耐久。 |
| `FireLess` | 取消火焰 / 岩漿傷害 | 應避免被 `DamageDogers` 或 `IronMan` 當作有效受傷。 |
| `FoodNeophobia` | 第一種食物鎖定 | 與 `Soup`、金蘋果、藥水類 consume 行為需抽樣。 |
| `GoldLess` | 移除黃金掉落 | 與 `CutClean` 的 gold ingot 替換有順序風險。 |
| `HastyBoys` | 工具合成附效率 | 與 `NoEnchant`、`FastObsidian` 屬規則語意互動。 |
| `HorseLess` | 禁止騎馬 | 暫無高風險 scenario 組合，抽樣即可。 |
| `IronMan` | final heal 前未受傷加最大血量 | 與 `ArmorVsHealth` 同改 max health，與 damage 類同判斷受傷。 |
| `LessBowDamage` | 弓箭傷害縮放 | 與 `TripleArrow`、`Switcheroo`、`BowLess` 同弓箭路線。 |
| `Limitations` | 礦物上限、清空 drops/exp | 與 `CutClean`、`VeinMiners`、`DoubleOrNothing`、`TripleOres` 是最高風險 block 組。 |
| `LuckyLeaves` | 樹葉掉金蘋果 | 與 `Timber`、一般 apple rate 只需確認沒有重複或漏掉。 |
| `NoClean` | 擊殺後無敵 | 與 `ShiftKill`、`BloodDiamonds`、`TimeBomb` 爆炸與一般傷害取消互動。 |
| `NoEnchant` | 禁止開附魔台 | 與 `HastyBoys` 自動附效率屬規則語意互動。 |
| `NoFall` | 取消摔落傷害 | 應避免被 `DamageDogers` 或 `IronMan` 當作有效受傷。 |
| `PotionLess` | 禁止藥水飲用 / splash / brewing / effect | 與 `AbsorptionLess`、`FoodNeophobia` 的 consume 分界需抽樣。 |
| `RodLess` | 禁止釣竿 projectile | 與 `FragileRods` 可能產生取消但仍扣耐久。 |
| `ShiftKill` | 非蹲下擊殺扣殺手一半血量含吸收 | 與 `NoClean`、`ArmorVsHealth`、`IronMan`、death/inventory 類高風險。 |
| `SilkWeb` | 蜘蛛網 drops 替換 | 與 block/drop 大組互動低，抽樣確認即可。 |
| `Soup` | 蘑菇湯右鍵 / 左鍵回血 | 與 `ArmorVsHealth` 的 max health cap、`FoodNeophobia` 的 consume 分界需確認。 |
| `SwapInventory` | 死亡時交換死亡者與殺手 inventory，並替換 drops | 與 `TimeBomb`、`BackPack`、`ShiftKill` 是最高風險 death 組。 |
| `Switcheroo` | 弓箭命中交換位置 | 與 `TripleArrow` 可能多箭多次交換，與 `BowLess` 互斥。 |
| `Timber` | 連鎖砍樹 | 與 `LuckyLeaves`、耐久處理需抽樣。 |
| `TimeBomb` | 死亡箱、倒數爆炸、移除已入箱 drops | 與所有 death drops 修改 scenario 高風險。 |
| `TripleArrow` | 射箭額外生成兩支箭 | 與 `BowLess`、`LessBowDamage`、`Switcheroo`、Infinity 行為互動。 |
| `TripleOres` | 礦物 drops 三倍，Silk Touch 不套用 | 與 `DoubleOrNothing`、`CutClean`、`Limitations`、`DiamondLess`、`GoldLess` 高風險。 |
| `VanillaPlus` | gravel/flint 掉落調整 | 2026-05-13 委託人決議後，`CutClean` 不再介入 gravel/flint drops，避免削弱 `VanillaPlus` 效益。 |
| `VeinMiners` | 蹲下連鎖挖礦 | 與 `Limitations`、`BloodDiamonds`、`CutClean`、`DoubleOrNothing` 是最高風險 block 組。 |
| `ArmorVsHealth` | 穿戴護甲扣最大血量，停用 / 重生還原 | 與 `IronMan`、`ShiftKill`、`Soup`、死亡 / 重生流程高風險。 |

## P0 Death / Inventory 組合

| 組合 | 風險 | 初步檢查點 | 委託人優先級 |
| --- | --- | --- | --- |
| `NoClean + ShiftKill` | `NoClean` 與 `ShiftKill` 都在 `UHCGamingDeathEvent` 預設 priority；實測曾發現殺手先取得無敵後，`ShiftKill` 的 `killer.damage()` 會被無敵取消。已修正為 `ShiftKill` 自傷懲罰會暫時避開插件內部 NoClean 無敵，外部傷害仍維持 NoClean 保護；懲罰改以殺手最大生命值與吸收血量計算，符合委託人指定「ShiftKill 統一會致死」。 | 2026-05-13 實測通過：滿血非蹲下擊殺會扣最大血量一半並取得無敵；低血非蹲下擊殺會致死且不被 NoClean 擋掉；蹲下擊殺不扣血但仍取得 NoClean 無敵。 | ShiftKill最高優先級 |
| `ShiftKill + SwapInventory` | `ShiftKill` 會在非蹲下擊殺時懲罰殺手，`SwapInventory` 會在同一死亡事件交換死亡者與殺手 inventory 並替換 drops。2026-05-13 實測發現交換成功，但未開 `TimeBomb` 時擊殺者原物品沒有穩定掉出；已修正為 `SwapInventory` 在未開 `TimeBomb` 時手動釋放交換後 drops。`TimeBomb` 開啟時不走此手動釋放分支，避免影響已通過的 TimeBomb 收箱組合。 | 2026-05-13 修正後直接通過：滿血非蹲擊殺會扣殺手最大血量一半、交換成功，地上掉落擊殺者原物品；低血非蹲擊殺會讓殺手懲罰致死，雙方死亡且雙方物品都掉落；蹲下擊殺不扣血、交換成功，地上掉落擊殺者原物品。 | ShiftKill最高優先級 |
| `ShiftKill + TimeBomb` | `ShiftKill` 會在非蹲下擊殺時懲罰殺手，`TimeBomb` 會在之後把死亡 drops 放入死亡箱；若懲罰造成殺手死亡，可能巢狀建立殺手自己的死亡箱。 | 2026-05-13 直接通過：滿血非蹲擊殺會扣半血並只產生受害者死亡箱；低血非蹲擊殺會讓殺手懲罰致死，並產生雙方各自死亡箱；蹲下擊殺不扣血且只產生受害者死亡箱。overflow 已由 `TimeBomb + BackPack` 覆蓋通過。 | ShiftKill最高優先級 |
| `TimeBomb + SwapInventory` | `SwapInventory` 會替換 `UHCGamingDeathEvent#getDrops()`，`TimeBomb` 之後把當下 drops 放入死亡箱。 | 2026-05-13 直接 / 覆蓋通過：單開兩者時已確認死亡箱放入擊殺者原本身上物品；`BackPack + TimeBomb + SwapInventory` 又覆蓋相同的 `SwapInventory` drops 進 `TimeBomb` 路徑。後續除非發現無 BackPack 時結果不同，否則不重複測。 | SwapInventory最高優先級 |
| `TimeBomb + BackPack` | `BackPack` 會把隊伍背包內容加入 drops；背包內容可能含空格，且死亡箱容量有限。已修正 `TimeBomb` 建箱後不再依賴事件剩餘 drops 自動掉落，而是由插件手動在死亡位置釋放 overflow，避免超出箱子容量的 backpack 物品消失。 | 2026-05-13 直接通過：victim 身上物品與被淘汰隊伍 backpack 物品會進入 `TimeBomb` 箱；被淘汰隊伍 backpack 會清空；超過死亡箱容量的物品會維持一般掉落，不擴大死亡箱。 | overflow維持一般掉落，不擴大死亡箱 |
| `SwapInventory + BackPack` | 交換 inventory 後，團隊淘汰背包 drops 再加入；同一事件內玩家 inventory 與隊伍 backpack 來源不同。 | 2026-05-13 覆蓋通過：`BackPack + TimeBomb + SwapInventory` 已測到 `SwapInventory` 開啟時 BackPack 物品塞進擊殺者背包、overflow 從擊殺者身上噴出、被淘汰隊伍 backpack 清空；`TimeBomb` 不影響這條 BackPack 分支。 | backpack物品視為塞進擊殺者背包，若背包已滿則從擊殺者身上噴出 |
| `NoClean + TimeBomb` | 殺手取得無敵後，後續死亡箱爆炸可能傷到殺手或旁人。 | 2026-05-13 直接通過：killer 擊殺後會取得 NoClean 無敵提示；killer 站在死亡箱爆炸範圍內不受爆炸傷害；未取得 NoClean 的旁人會受到爆炸傷害；死亡箱會正常爆炸 / 消失，console 無錯誤。 | NoClean最高優先級 |
| `BackPack + TimeBomb + SwapInventory` | 三者同時改死亡 drops，是最高風險完整 death-drop 組。已修正 `BackPack` 不再把空背包格 `null` 放進 drops，避免 `TimeBomb` 建箱時 NPE；隊伍淘汰後會清空被淘汰隊伍 backpack，避免同一批 backpack 物品保留在原背包。 | 2026-05-13 實測通過：只有隊伍淘汰時才會處理被淘汰隊伍 backpack；`TimeBomb` 箱內只包含擊殺者身上物品，不包含擊殺者隊伍 backpack；擊殺者隊伍 backpack 仍保留；被淘汰隊伍 backpack 物品會塞進擊殺者背包，若 overflow 會從擊殺者身上噴出。 | SwapInventory最高優先級；若觸發擊殺事件，將擊殺者身上物品放進timebomb，隨後敵方含backpak的所有物品塞進擊殺者背包，若overflow則從擊殺者身上噴出 |
| `NoClean + ShiftKill + TimeBomb + SwapInventory + BackPack` | 擊殺獎勵、殺手懲罰、inventory 交換、死亡箱與隊伍 backpack 同時發生，事件順序任何假設都可能被放大。 | 2026-05-13 直接通過：滿血非蹲擊殺時，killer 取得 NoClean、被 ShiftKill 扣半血，victim 死亡箱放 killer 原物品，victim 物品與 victim 隊伍 backpack 進 killer 身上；低血非蹲擊殺時，killer 也會被 ShiftKill 懲罰致死，雙方死亡箱與物品歸屬符合預期。若測試為一人一隊，killer 死亡時 killer 隊伍也淘汰，因此 killer 隊伍 backpack 也會被處理並掉出 / 進箱，這是正確結果；蹲下擊殺時不扣血、不產生 killer 死亡箱，其餘交換、收箱與 backpack 清空符合預期。 | ShiftKill最高優先級，不可因為觸發noclean而躲掉懲罰，若同時死亡則交換死亡箱物品；overflow維持一般掉落，不擴大死亡箱 |
| `DamageDogers + 任一 death/inventory 組合` | `DamageDogers` 原本會在受傷時直接 `setHealth(0)`，導致後續死亡流程可能遺失原攻擊者；已修正為改寫原傷害事件成致死傷害，保留 Bukkit 原本的 killer 歸屬。 | 2026-05-13 直接通過：`DamageDogers + TimeBomb + BackPack + SwapInventory` 中，C6Yelan 攻擊 _SnYe 觸發 DamageDogers 致死後，killer 仍保留為 C6Yelan；`SwapInventory`、`TimeBomb`、`BackPack` 照常執行，_SnYe 死亡箱為 C6Yelan 原物品，C6Yelan 取得 _SnYe 物品與 _SnYe 隊伍 backpack，_SnYe 隊伍 backpack 清空，console 無錯誤。 | 無論如何，DamageDogers最高優先級 |

## P0 Block / Drop 組合

| 組合 | 風險 | 初步檢查點 | 委託人優先級 |
| --- | --- | --- | --- |
| `VeinMiners + Limitations` | 連鎖挖礦會重入 block break；上限計數、超上限清空 drops 與 exp 需要確認是否以每顆礦計算。 | 2026-05-13 直接通過：只開 `VeinMiners + Limitations` 時，C6Yelan 蹲下連鎖挖 17 顆相連鑽石礦，最多只取得 16 顆鑽石相關掉落，第 17 顆超上限礦物不掉落、不給經驗，並有上限提示；達上限後再單挖鑽石礦同樣不掉落、不給經驗且有提示，console 無錯誤。 | Limitations最高優先級、礦物完全獨立計算計算 |
| `VeinMiners + BloodDiamonds` | `BloodDiamonds` 會在初始事件估算連鎖鑽石數，重入 mining 時跳過；若估算與實際破壞不同會扣血不準。實測曾發現 `BloodDiamonds` 可能晚於 `VeinMiners` 計算，導致連鎖 3 顆只扣 1 點血；已修正為 `BloodDiamonds` 於 LOW priority 先估算礦脈數量，再讓 `VeinMiners` 破壞相連礦物。 | 2026-05-13 修正後直接通過：只開 `VeinMiners + BloodDiamonds` 時，C6Yelan 滿血蹲下連鎖挖 3 顆相連鑽石礦，3 顆都被挖掉，血量由 20 變成 17，掉落維持正常鑽石礦掉落；不蹲下單挖 1 顆鑽石礦則血量由 20 變成 19，console 無錯誤。 | 扣血事件完全獨立計算計算，若功能完全正常則維持原樣 |
| `VeinMiners + CutClean` | 連鎖破壞每顆礦都可能走自訂 drops；`CutClean` 也會在 drops modified 後重設 exp。 | 2026-05-13 直接通過：只開 `VeinMiners + CutClean` 時，C6Yelan 蹲下連鎖挖 3 顆相連鐵礦會掉 3 個鐵錠，蹲下連鎖挖 3 顆相連金礦會掉 3 個金錠；皆不是 raw ore 或礦石本身，有正常經驗掉落，工具耐久正常消耗，console 無錯誤。 | 礦物exp、耐久完全獨立計算計算，若功能完全正常則維持原樣 |
| `VeinMiners + DoubleOrNothing` | 每顆連鎖礦可能各自隨機雙倍或清空；也可能只預期整條礦脈一次判定。 | 2026-05-13 直接通過：只開 `VeinMiners + DoubleOrNothing` 時，C6Yelan 蹲下連鎖挖 8 顆相連鑽石礦，8 顆都被挖掉，最終掉落 8 顆鑽石，符合每顆礦物獨立 0x / 2x 判定後可能出現的偶數總量，console 無錯誤。 | DoubleOrNothing最高優先級，每顆礦物獨立計算 |
| `VeinMiners + TripleOres` | 連鎖每顆礦都三倍；Silk Touch 例外需確認在所有連鎖事件都生效。 | 2026-05-13 直接通過：只開 `VeinMiners + TripleOres` 時，C6Yelan 使用普通未附魔鑽石鎬蹲下連鎖挖 3 顆相連鑽石礦，掉落總數為 9 顆鑽石；使用 Silk Touch 鑽石鎬連鎖挖 3 顆相連鑽石礦，掉落總數為 3 顆鑽石礦，`TripleOres` 未對 Silk Touch 生效，console 無錯誤。 | 同上(每顆礦物所有屬性獨立計算)，若功能完全正常則維持原樣 |
| `Limitations + CutClean` | `Limitations` 超上限會清 drops 並設 exp 0；`CutClean` HIGHEST 之後可能因 drops modified 再設定 block exp。實測曾發現超上限時不掉物品但仍掉經驗；已修正為 `CutClean` 不覆蓋前面 scenario 已明確設定過的 exp。 | 2026-05-13 修正後直接通過：只開 `Limitations + CutClean` 時，C6Yelan 鐵礦達 64 上限後再挖第 65 顆鐵礦，第 65 顆不掉鐵錠 / raw iron / 鐵礦、不掉經驗，有超上限提示，console 無錯誤。 | Limitations最高優先級，超出上限不可獲得經驗和drop |
| `Limitations + DoubleOrNothing` | 兩者同改 drops；若 listener 順序因 reload / saved settings 改變，可能先清空或先雙倍。 | 2026-05-13 直接通過：只開 `Limitations + DoubleOrNothing` 時，_SnYe 未達上限挖鑽石礦只會掉 0 或 2 顆鑽石，不會掉 1 顆；無論 `DoubleOrNothing` 判定為 0x 或 2x 都列入 `Limitations` 方塊計數，第 16 顆後出現鑽石上限提示；第 17 顆超上限鑽石礦不掉落、不掉經驗，有超上限提示，console 無錯誤。 | Limitations最高優先級，超上限時應永遠無drops，DoubleOrNothing無論結果如何都列入礦物計數 |
| `Limitations + TripleOres` | 上限規則與收益倍增同時存在，超上限應避免被後續倍增復活 drops。 | 2026-05-13 直接通過：只開 `Limitations + TripleOres` 時，未達上限挖 1 顆鑽石礦會掉 3 顆鑽石，但 `Limitations` 只以 1 顆礦物方塊計數；累計第 16 顆後出現鑽石上限提示；第 17 顆超上限鑽石礦不掉落、不掉經驗，有超上限提示，console 無錯誤。 | Limitations最高優先級，礦物以「挖掘方塊數」計數 |
| `BloodDiamonds + DiamondLess` | 挖鑽石扣血但 `DiamondLess` 移除鑽石收益，可能是舊版懲罰組合。 | 2026-05-13 直接通過：只開 `BloodDiamonds + DiamondLess` 時，C6Yelan 滿血挖 1 顆鑽石礦，血量由 20 變成 19，方塊正常破壞，不掉鑽石、不掉鑽石礦，console 無錯誤。此組合接受「扣血但無鑽石」，不修改判定規則。 | 若功能完全正常則維持原樣，不修改判定規則(接受「扣血但無鑽石」) |
| `GoldLess + CutClean` | `GoldLess` 移除 raw gold；`CutClean` 會把 raw gold / gold ore 轉 gold ingot。 | 2026-05-13 直接通過：只開 `GoldLess + CutClean` 時，C6Yelan 挖 1 顆金礦，方塊正常破壞，不掉金錠、不掉 raw gold、不掉金礦，`CutClean` 沒有轉出黃金收益，console 無錯誤。 | GoldLess最高優先級 |
| `DiamondLess + CutClean + TripleOres` | 同一鑽石礦 drops 會經過移除、型別替換、倍增。 | 2026-05-13 直接通過：只開 `DiamondLess + CutClean + TripleOres` 時，C6Yelan 挖 1 顆鑽石礦，方塊正常破壞，不掉鑽石、不掉鑽石礦，`TripleOres` 未復活 3 顆鑽石，console 無錯誤；仍有經驗掉落，接受為不需額外修正的行為，避免為此增加過度複雜度。 | DiamondLess最高優先級 |
| `DoubleOrNothing + TripleOres + CutClean` | drops 數量可能變成 0、2x、3x 或 6x，且型別可能先後改變。 | 2026-05-13 直接通過：只開 `DoubleOrNothing + TripleOres + CutClean` 時，C6Yelan 挖金礦，方塊正常破壞，最終掉落只會是 0 個金錠或 6 個金錠，不掉 raw gold，也沒有 2 個或 3 個金錠等異常數量，console 無錯誤。 | 同時觸發，簡單來說就是此事件會使特定礦物變成0x或6x |
| `VanillaPlus + CutClean` | 舊版 `CutClean` 會把 gravel 轉 flint；2026-05-13 委託人決議改為 `CutClean` 不介入 gravel/flint，讓 `VanillaPlus` 保留自身機率規則。 | 2026-05-13 修正後直接通過：只開 `VanillaPlus + CutClean` 時，C6Yelan 連續挖 gravel，不會被 `CutClean` 強制全轉 flint，結果維持 `VanillaPlus` 自身機率規則；同時抽樣挖鐵礦仍會掉鐵錠，`CutClean` 礦物熟化未受影響，console 無錯誤。 | 需求調整：`VanillaPlus` 負責 gravel/flint；`CutClean` 只負責礦物熟化與動物熟肉 |
| `Timber + LuckyLeaves` | `Timber` 只連鎖 log，但樹葉 decay / 手挖可能接著觸發 apple 與 golden apple 邏輯。 | 2026-05-13 直接通過：只開 `Timber + LuckyLeaves` 時，C6Yelan 砍樹幹最下方一格，`Timber` 只連鎖處理木頭，未直接破壞樹葉；樹葉自然 decay 與手動挖樹葉皆正常，不會噴異常大量掉落物，console 無 `Scenario 'Timber' failed` 或其他錯誤。 | LuckyLeaves最高優先級，取代apple掉落 |

## P1 Damage / Projectile / Health 組合

| 組合 | 風險 | 初步檢查點 | 委託人優先級 |
| --- | --- | --- | --- |
| `ArmorVsHealth + IronMan` | 兩者都改 `max health`，停用 / 重生 / final heal 的還原順序可能互相覆蓋。 | 2026-05-13 直接通過：只開 `ArmorVsHealth + IronMan` 時，C6Yelan 穿 `iron_chestplate` 後最大血量由 20 降為 14，final heal 後因未受傷取得 IronMan 加成，最大血量與目前血量皆為 18；_SnYe 在 final heal 前受有效傷害後最大血量維持 20，未取得 IronMan 加成；C6Yelan 穿 `iron_chestplate` 後脫下，最大血量仍維持 14，不會因脫裝恢復，之後未受傷 final heal 仍正確變 18。 | IronMan最高優先級 |
| `ArmorVsHealth + ShiftKill` | `ShiftKill` 依最大血量與吸收血量扣一半；`ArmorVsHealth` 會降低最大血量，懲罰值也會跟著降低，但低血量時仍可致死。 | 2026-05-13 直接通過：只開 `ArmorVsHealth + ShiftKill` 時，C6Yelan 穿 `iron_chestplate` 後最大血量為 14，滿血非蹲下擊殺 _SnYe 後被扣 7 血並剩 7 血、不死亡；滿血蹲下擊殺 _SnYe 不被扣血，仍為 14；C6Yelan 先降到 7 血或以下再非蹲下擊殺 _SnYe，會因 `ShiftKill` 懲罰死亡。 | ShiftKill最高優先級，並且可致死 |
| `ArmorVsHealth + Soup` | `Soup` 用目前 max health 作為回血上限；`ArmorVsHealth` 扣掉的最大血量脫甲後不會恢復，Soup 不應繞過此上限。 | 2026-05-13 直接通過：只開 `ArmorVsHealth + Soup` 時，C6Yelan 穿 `iron_chestplate` 後最大血量為 14；血量 8 時喝 `mushroom_stew` 只回到 14，不會超過上限，蘑菇湯變成 `bowl`；血量 4 時喝湯回到 11，蘑菇湯變成 `bowl`；脫下 `iron_chestplate` 後最大血量仍為 14，血量 8 時喝湯最多仍只回到 14，不會回到 20。 | ArmorVsHealth最高優先級，穿甲扣 max health 後喝湯不應超過目前上限；脫甲後不可用 Soup 回到原本 20 |
| `IronMan + DamageDogers` | `DamageDogers` 直接殺死前 N 位受傷者；`IronMan` 也在 damage event 記錄是否受傷。 | 2026-05-13 直接通過：只開 `IronMan + DamageDogers` 時，C6Yelan 攻擊 _SnYe 一次會讓 _SnYe 成為 `DamageDogers` 犧牲者死亡；_SnYe 重生後觸發 final heal，最大血量維持 20，不會取得 IronMan 加成；同場未受有效傷害的 C6Yelan final heal 後最大血量與目前血量皆為 24；`DamageDogers` 前 3 位名額用完後，第 4 次普通受傷不再被直接殺死，但該玩家仍因受傷而不能取得 IronMan 加成。 | DamageDogers最高優先級，死亡不須觸發DamageDogers |
| `FireLess / NoFall + DamageDogers` | 火焰 / 摔落傷害應先取消，避免仍被 DamageDogers 判定死亡。 | 2026-05-13 直接通過：只開 `FireLess + DamageDogers` 時，C6Yelan 被火燒或接觸岩漿皆不扣血、不死亡、不出現 `DamageDogers` 犧牲者廣播；只開 `NoFall + DamageDogers` 時，C6Yelan 從會造成摔落傷害的高度落下，不扣血、不死亡、不出現犧牲者廣播；取消傷害後再讓 _SnYe 受到普通攻擊，_SnYe 仍會成為第一位 `DamageDogers` 犧牲者，確認火焰 / 岩漿 / 摔落未消耗名額。 | 火焰、岩漿、摔落都不應消耗 DamageDogers 名額。 |
| `FireLess / NoFall + IronMan` | 被取消的傷害不應讓玩家失去 IronMan 資格。 | 2026-05-13 直接通過：只開 `FireLess + IronMan` 時，C6Yelan 在 final heal 前被火燒或接觸岩漿皆不扣血，且 final heal 後最大血量變 24；只開 `NoFall + IronMan` 時，C6Yelan 在 final heal 前從會造成摔落傷害的高度落下不扣血，final heal 後最大血量變 24；console 無 `Scenario 'Iron_Man' failed`。 | 被取消的傷害不應讓玩家失去 IronMan 資格。 |
| `AbsorptionLess + ShiftKill` | `ShiftKill` 讀殺手當下吸收血量；`AbsorptionLess` 是食用後下一 tick 移除吸收效果。 | 2026-05-13 直接通過 / edge case 不處理：只開 `AbsorptionLess + ShiftKill` 時，C6Yelan 吃 `golden_apple` 並等待吸收黃心消失後，非蹲下擊殺 _SnYe 只扣 10 血，從 20 變 10，未把吸收血量算入懲罰；吃金蘋果後立刻同 tick 擊殺屬於過度 edge case，不處理；吸收消失後蹲下擊殺 _SnYe 不被 `ShiftKill` 扣血。 | 若功能完全正常則維持原樣；吃金蘋果後同 tick 立刻擊殺屬過度 edge case，不處理 |
| `BowLess + TripleArrow` | `BowLess` 取消射弓，`TripleArrow` 使用同一射弓事件；`TripleArrow` 使用 `ignoreCancelled = true`，因此射弓被取消後不應生成額外箭。 | 2026-05-13 直接通過 / 核心機制接受：只開 `BowLess + TripleArrow` 時，C6Yelan 嘗試射弓不會射出箭，`TripleArrow` 也不會生成額外箭；Paper 1.21 核心仍會消耗箭矢，決議遵守核心機制，不做補償或額外攔截，避免與核心行為對抗。 | BowLess最高優先級；禁止射弓時不生成額外箭，箭矢消耗遵守核心機制 |
| `BowLess + LessBowDamage / Switcheroo` | 弓箭被取消後，後續命中類 scenario 不應發生。 | 2026-05-13 直接通過：只開 `BowLess + LessBowDamage` 或 `BowLess + Switcheroo` 時，C6Yelan 嘗試射弓會被 `BowLess` 取消，沒有箭命中，因此不會觸發降傷，也不會觸發位置交換。 | BowLess最高優先級 |
| `TripleArrow + LessBowDamage` | 額外箭也會造成弓箭傷害事件。 | 2026-05-13 直接通過：只開 `TripleArrow + LessBowDamage` 時，C6Yelan 射弓會產生 3 支箭；命中 _SnYe 時，箭矢傷害皆會套用 `LessBowDamage` 的 50% 減傷，若多支箭同時命中則總傷害會疊加但每支箭仍是減傷後傷害；箭矢只消耗 1 支，這是 Step 15 已通過且決議不修改的舊版特性。 | 額外箭也會造成弓箭傷害事件；箭矢消耗維持 Step 15 舊版特性 |
| `TripleArrow + Switcheroo` | 額外箭命中可能造成多次位置交換。 | 2026-05-13 直接通過：只開 `TripleArrow + Switcheroo` 時，C6Yelan 射 _SnYe 且只有單支箭命中會交換位置一次；近距離讓多支箭命中時，每支命中的箭都可能觸發一次位置交換，偶數次可能回到原位、奇數次會互換，維持此組合特性；箭矢消耗維持 Step 15 舊版特性，射一次只消耗 1 支。 | 若功能完全正常則維持原樣；多箭命中可多次交換，箭矢消耗維持 Step 15 舊版特性 |
| `RodLess + FragileRods` | `FragileRods` 可能在 `RodLess` 取消前扣耐久，造成禁止使用仍扣耐久。 | 2026-05-13 直接通過 / 現況接受：只開 `RodLess + FragileRods` 時，C6Yelan 使用 `fishing_rod` 不會拋出魚鉤，`RodLess` 禁用效果正常；釣竿仍會扣耐久，決議接受既有事件順序 / 核心機制，不額外修正。 | RodLess最高優先級；魚鉤不得拋出，耐久消耗接受現況不修 |
| `Switcheroo + NoFall` | 交換位置可能導致落差傷害，`NoFall` 應取消後續摔落。 | 2026-05-13 直接通過：只開 `Switcheroo + NoFall` 時，C6Yelan 站高處射中低處 _SnYe，兩人正常交換位置，後續摔落不扣血、不死亡；C6Yelan 站低處射中高處 _SnYe，兩人正常交換位置，後續摔落同樣不扣血、不死亡；console 無 `Switcheroo` 或 `NoFall` 相關錯誤。 | `NoFall` 應取消後續摔落 |

## P1 Consume / Craft / Task 組合

| 組合 | 風險 | 初步檢查點 | 委託人優先級 |
| --- | --- | --- | --- |
| `FoodNeophobia + Soup` | `Soup` 走 interact，不是一般 consume；可能繞過食物鎖定。 | 蘑菇湯是否應計入第一食物需確認舊版。 | 若原作者邏輯有漏洞，則改為Soup同金蘋果不應被鎖定 |
| `FoodNeophobia + AbsorptionLess` | 金蘋果被 `FoodNeophobia` 忽略，但 `AbsorptionLess` 會移除吸收效果。 | 金蘋果不應鎖食物種類，吸收效果應被移除。 | 若功能完全正常則維持原樣 |
| `FoodNeophobia + PotionLess` | `FoodNeophobia` 忽略 potion，`PotionLess` 取消飲用。 | potion 不應鎖食物種類，且不應產生藥水效果。 | potion 不應鎖食物種類，且不應產生藥水效果 |
| `PotionLess + AbsorptionLess` | 兩者都碰 consume/effect，但目標不同。 | 藥水被取消、金蘋果吸收被移除，互不影響。 | 藥水被取消、金蘋果吸收被移除，互不影響 |
| `BenchBlitz + HastyBoys` | 兩者同用 craft prepare，但一個處理工作台，一個處理工具。 | 已做過工作台後，工具合成效率附魔不應被清成 AIR。 | 若功能完全正常則維持原樣 |
| `HastyBoys + NoEnchant` | `NoEnchant` 禁附魔台，但 `HastyBoys` 仍給工具效率。 | 是否接受自動附效率不受 NoEnchant 限制。 | 自動附效率不受 NoEnchant 限制 |
| `HastyBoys + FastObsidian` | 工具效率與 haste 疊加，黑曜石速度可能遠快於單開。 | 只需確認 potion 不殘留，是否過快先記錄。 | 若功能完全正常則維持原樣 |
| `FastSmelting + CutClean` | `CutClean` 讓礦物 / 食物不必進熔爐，`FastSmelting` 仍可加速其他熔爐項目。 | 不是直接衝突，抽樣確認熔爐 task 沒有殘留即可。 | `FastSmelting`仍可加速其他熔爐項目 |

## P2 低風險抽樣組合

| 組合 | 風險 | 初步檢查點 | 委託人優先級 |
| --- | --- | --- | --- |
| `HorseLess + 任一 combat scenario` | `HorseLess` 只禁止騎馬，通常不改 combat / drops。 | 騎乘被取消後，不應影響玩家受傷、擊殺或死亡流程。 | 若功能完全正常則維持原樣 |
| `SilkWeb + block/drop 組` | `SilkWeb` 只處理 cobweb，與礦物 drops 不同目標。 | 剪刀與非剪刀挖 cobweb 的 drops 不受其他礦物規則影響。 | 若功能完全正常則維持原樣 |
| `LuckyLeaves + VanillaPlus` | 兩者都是隨機掉落，但目標方塊不同。 | 挖樹葉與 gravel 各自維持原本掉落規則。 | 若功能完全正常則維持原樣 |
| `FastSmelting + disable 類 scenario` | 熔爐 task 與 bow/rod/horse/enchant 禁用路線分離。 | 啟用 / 停用後熔爐 task 不殘留，disable 類仍正常取消。 | 若功能完全正常則維持原樣 |
| `BenchBlitz + consume 類 scenario` | craft 與 consume 分離。 | 合成工作台限制不應影響吃東西、喝湯或 potion 取消。 | 若功能完全正常則維持原樣 |

## 跨組合風險

| 風險 | 影響 | 後續實測方式 | 委託人優先級 |
| --- | --- | --- | --- |
| 同 priority listener 順序 | 多個 scenario 都是預設 priority，且啟用順序可能受 settings reload 影響。 | 同一組合至少測一次正常開局啟用，必要時再測 saved settings reload 後啟用。 | 維持原邏輯，不大幅修改；若codex說明有潛在風險則進行修改 |
| `UHCGamingDeathEvent#getDrops()` 是共享 mutable list | `SwapInventory`、`BackPack`、`TimeBomb`、核心 custom drops 都改同一份 list。 | Death 組合都要在事件結束後檢查地上掉落、箱內物品與玩家 inventory。 | 維持原邏輯，不大幅修改；若codex說明有潛在風險則進行修改 |
| `UHCBlockBreakEvent#getDrops()` 是共享 mutable list | block/drop scenario 會同時改數量、型別、清空與 exp。 | 每個 block 組合都要檢查 drops、exp、方塊是否消失、工具耐久。 | 維持原邏輯，不大幅修改；若codex說明有潛在風險則進行修改 |
| 直接 `Player#damage()` 可能被無敵或取消事件影響 | `ShiftKill`、`BloodDiamonds` 直接造成傷害。 | 搭配 `NoClean`、`FireLess`、`NoFall`、damage disabled 狀態確認。 | 維持原邏輯，不大幅修改；若codex說明有潛在風險則進行修改 |
| max health 多來源修改 | `ArmorVsHealth` 與 `IronMan` 都會變更最大血量並在停用時還原。 | 測開啟、關閉、死亡、重生、final heal 的所有順序。 | 維持原邏輯，不大幅修改；若codex說明有潛在風險則進行修改 |
| static state 清理 | `DamageDogers`、`FoodNeophobia`、`Limitations`、`BenchBlitz`、`TimeBomb`、`ArmorVsHealth`、`IronMan` 持有 static 或長生命週期狀態。 | 測 scenario 關閉重開、遊戲重開與玩家重連後狀態是否殘留。 | 維持原邏輯，不大幅修改；若codex說明有潛在風險則進行修改 |

## 收尾判定

截至 2026-05-13，Step 16 的高風險 scenario 組合已完成實機驗收、修正或分類：

1. Death / inventory P0 已直接或覆蓋通過：`NoClean + ShiftKill`、`NoClean + TimeBomb`、`ShiftKill + SwapInventory`、`ShiftKill + TimeBomb`、`TimeBomb + SwapInventory`、`TimeBomb + BackPack`、`SwapInventory + BackPack`、`BackPack + TimeBomb + SwapInventory`、`DamageDogers + TimeBomb + BackPack + SwapInventory`。
2. Block / drops P0 已通過：`VeinMiners + Limitations`、`VeinMiners + BloodDiamonds`、`VeinMiners + CutClean`、`VeinMiners + DoubleOrNothing`、`VeinMiners + TripleOres`、`Limitations + CutClean`、`Limitations + DoubleOrNothing`、`Limitations + TripleOres`、`BloodDiamonds + DiamondLess`、`GoldLess + CutClean`、`DiamondLess + CutClean + TripleOres`、`DoubleOrNothing + TripleOres + CutClean`、`VanillaPlus + CutClean`、`Timber + LuckyLeaves`。
3. Damage / projectile / health P1 已通過或明確接受現況：`ArmorVsHealth + IronMan`、`ArmorVsHealth + ShiftKill`、`ArmorVsHealth + Soup`、`IronMan + DamageDogers`、`FireLess / NoFall + DamageDogers`、`FireLess / NoFall + IronMan`、`AbsorptionLess + ShiftKill`、`BowLess + TripleArrow`、`BowLess + LessBowDamage / Switcheroo`、`TripleArrow + LessBowDamage`、`TripleArrow + Switcheroo`、`RodLess + FragileRods`、`Switcheroo + NoFall`。
4. 剩餘 P1 consume / craft / task、P2 低風險抽樣與跨組合風險，已按「是否阻塞 1.21.11 升級」重新檢視對應程式碼；目前未發現需要在 Step 16 追加修正的高影響問題。

剩餘項目不再逐項實測，理由如下：

- `FoodNeophobia`、`Soup`、`AbsorptionLess`、`PotionLess` 多屬玩家規則定義與事件入口差異，不是升級造成的明確 regression。
- `BenchBlitz`、`HastyBoys`、`NoEnchant` 的 craft / enchant 互動屬舊版玩法規則；目前沒有看到會阻塞 1.21.11 啟動或核心遊戲流程的高風險衝突。
- `FastSmelting` 已在 `onDisable()` 取消加速 task，未見 task 殘留的升級 blocker；`FastObsidian` 的 haste 疊加屬玩法平衡，不在本輪擴修。
- P2 的 `HorseLess`、`SilkWeb`、`LuckyLeaves + VanillaPlus`、`BenchBlitz + consume 類` 事件面分離，沒有共享 drops / inventory / max health / death state 的高風險耦合。
- static state 類風險已檢視：`DamageDogers`、`FoodNeophobia`、`FastSmelting`、`ArmorVsHealth` 有清理路徑；`Limitations`、`BenchBlitz`、`IronMan` 仍帶有舊版長生命週期狀態特性，但目前 `/uhc stop` 會關閉伺服器，這類同服多場殘留屬作者架構限制，不作為 Step 16 升級修復項。

Step 16 結論：

- 高風險 scenario 混合互動已完成驗收與分類。
- 已修正的項目均有對應實測；可接受差異、舊版特性、核心機制差異已記錄。
- 單一 scenario 已通過的行為未因組合修正出現需阻塞升級的 regression。
- 本輪 scenario 工作可在 Step 16 告一段落；後續若委託人針對剩餘低風險玩法提出具體需求，應另列後續改善，不再阻塞升級主線。
