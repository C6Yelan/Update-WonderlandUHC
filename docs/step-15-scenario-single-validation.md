# Step 15 Scenario 單項驗收紀錄

本文件承接 `docs/steps.md` Step 15，只記錄單一 scenario 啟用時的行為補齊與驗收結果。多個 scenario 同時啟用後的互動行為已拆到 Step 16，不在本文件內判定。

## 範圍原則

- 單一 scenario 若與舊版行為一致，列為通過。
- 若舊版本來就是可疑設計，但委託人未要求變更，列為舊版特性或接受差異。
- 若委託人明確要求改掉舊版特性，列為委託人指定變更。
- 混合 scenario 行為只記錄為 Step 16 待驗收，不回塞 Step 15。

## 驗收總結

- `ScenarioManager` 目前註冊 38 個預設 scenario。
- Step 15 單開 scenario 已完成逐項驗收；目前沒有已知單項 blocker。
- 已知剩餘清理多屬後續步驟：舊 API 集中清理在 Step 19，混合互動驗收在 Step 16。

## 已修正後通過

| Scenario | 結果 | 備註 |
| --- | --- | --- |
| `Limitations` | 通過 | 補 deep ore、silk touch 與連鎖挖礦上限處理。 |
| `DiamondLess` | 通過 | 補一般 / deep diamond ore 掉落處理。 |
| `GoldLess` | 通過 | 補一般 / deep gold ore 掉落處理。 |
| `BloodDiamonds` | 通過 | 補 deep ore 與連鎖挖礦扣血；無敵時間內不扣血為委託人要求。 |
| `DoubleOrNothing` | 通過 | 機率檢視正常，連鎖挖礦掉落重入問題已修正。 |
| `CutClean` | 通過 | 補 raw ore / deep ore 與動物熟肉掉落。 |
| `VeinMiners` | 通過 | 修正連鎖挖礦多掉一個與內部自訂掉落重入問題。 |
| `FastSmelting` | 通過 | 修正第二輪加速、多人多熔爐與重啟後加速狀態。 |
| `FoodNeophobia` | 通過 | 修正關閉重開後食物限制狀態殘留。 |
| `DamageDogers` | 通過 | 修正關閉重開後犧牲者計數殘留。 |
| `PotionLess` | 通過 | 改用 Paper 1.21 `BrewEvent#getResults()`，移除舊 NMS brew result 反射錯誤。 |
| `SwapInventory` | 通過 | 過濾死亡掉落中的 `null` / `AIR`，避免與 `TimeBomb` 組合時死亡箱建立 NPE。 |
| `TimeBomb` | 通過 | 死亡箱、倒數 tick、爆炸與廣播正常；箱上可視倒數來源不明且委託人不要求補回。 |
| `FragileRods` | 通過 | 改用 1.21 `Damageable` ItemMeta 扣耐久，物品破裂時播放原生破裂音。 |
| `TripleOres` | 通過 | 委託人指定修正：Silk Touch 掉落礦物方塊時不套用三倍，避免無限礦物。 |

## 單開測試通過

| Scenario | 結果 | 備註 |
| --- | --- | --- |
| `AbsorptionLess` | 通過 | 金蘋果吸收效果會被移除。 |
| `BackPack` | 通過 | 單開使用正常。 |
| `BenchBlitz` | 通過 | 第一個工作台後限制再次製作。 |
| `BowLess` | 通過 | 舊版特性：會消耗箭但不射出。 |
| `FastObsidian` | 通過 | 挖黑曜石加速正常，離開或停止挖不殘留效果。 |
| `FireLess` | 通過 | 火焰 / 岩漿傷害取消正常。 |
| `HastyBoys` | 通過 | 工具自動附效率，開關正常。 |
| `HorseLess` | 通過 | 騎乘限制正常。 |
| `IronMan` | 通過 | 單開行為正常。 |
| `LessBowDamage` | 通過 | 預設 50% 可將滿弓傷害由 5 顆心降為 2.5 顆心。 |
| `LuckyLeaves` | 通過 | 樹葉可掉落金蘋果。 |
| `NoClean` | 通過 | 擊殺後無敵時間正常。 |
| `NoEnchant` | 通過 | 附魔限制正常。 |
| `NoFall` | 通過 | 摔落傷害取消正常。 |
| `RodLess` | 通過 | 釣竿限制正常。 |
| `ShiftKill` | 通過 | 單開正常；與其他死亡 scenario 的互動留 Step 16。 |
| `Soup` | 通過 | 左鍵也能喝湯為舊版特性。 |
| `Switcheroo` | 通過 | 位置偏移為舊版直接交換 `Location` 的行為。 |
| `Timber` | 通過 | 單開砍樹正常。 |
| `VanillaPlus` | 通過 | Gravel 掉 flint 機率提高正常。 |
| `ArmorVsHealth` | 通過 | 單開穿脫裝備、死亡 / 重生 / 關閉還原正常。 |

## 接受差異與不處理項

| Scenario | 分類 | 結論 |
| --- | --- | --- |
| `SilkWeb` | 舊版 / 版本差異接受 | 1.21 原版剪刀挖蜘蛛網已掉蜘蛛網，scenario 效果不明顯；委託人未要求另補。 |
| `TripleArrow` | 舊版特性 | 三箭只回收一箭、Infinity 不耗箭，符合舊版設計。 |
| `BowLess` | 舊版特性 | 取消射擊但消耗箭，符合舊版設計。 |
| `Soup` | 舊版特性 | 左鍵也能喝湯，符合舊版設計。 |
| `PotionLess` | 舊版特性 | 藥水物品會消耗但沒有實際效果，符合舊版設計；釀造 NMS 錯誤已修。 |
| `Switcheroo` | 舊版特性 | 命中後位置交換有偏移，符合舊版直接交換 `Location` 的行為。 |
| `TimeBomb` | 不要求補回 | 舊版資料夾與舊 jar 未找到箱上可視倒數來源；委託人表示不用特別補。 |

## 後續歸屬

- `ShiftKill` 與其他死亡 scenario 同時啟用時的死亡行為互動，歸 Step 16。
- `TimeBomb + SwapInventory`、`NoClean + TimeBomb` 等死亡 / inventory 組合，歸 Step 16。
- `VeinMiners + Limitations / BloodDiamonds / CutClean` 等 block/drop 組合，歸 Step 16。
- `getItemInHand()`、`DamageModifier`、DatouNMS legacy adapter、TPS NMS fallback 等 API 清理，歸 Step 19。

## Step 15 收尾判定

截至 2026-05-11，Step 15 的單一 scenario 行為補齊可以視為完成。進入 Step 16 前，不需要再把混合 scenario 組合問題回塞 Step 15。
