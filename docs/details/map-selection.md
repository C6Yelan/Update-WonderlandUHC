# WonderlandUHC 選圖與中心搜尋細節說明

整理日期：2026-05-21

這份文件補充 `docs/map-selection.md` 的細節，說明新版選圖如何產生候選中心、如何抽樣地形、如何計算分數、哪些條件會造成拒絕，以及 debug output 應該怎麼判讀。

## 適用範圍

本文件說明的是目前 Paper `1.21.11` 更新線中的選圖與中心搜尋流程，主要對應：

- `/uhc regen`
- CenterCleaner 中心搜尋
- `MatchCenter`
- `CenterCandidateScore`
- Chunky 正式預生成前的地圖確認流程

本文件不是 Chunky 使用教學，也不是開服安裝教學。外部插件需求見 `docs/plugin-dependencies.md`，完整遊戲流程見 `docs/game-flow.md`。

## 主流程位置

實際主持流程大致是：

```text
/uhc regen -> 建立預覽世界 -> 中心搜尋 -> 主持人預覽 -> /uhc choose -> Chunky 預生成
```

重點：

1. `/uhc regen` 只產生與預覽地圖，不會直接開始正式跑圖。
2. `/uhc choose` 才會確認目前預覽地圖，保存設定與 `MatchCenter`，然後重啟並進入 Chunky 預生成。
3. 中心搜尋的目的不是阻止主持人選圖，而是提供更好的預覽中心與品質提示。
4. 即使搜尋結果不是 `RECOMMENDED`，系統仍會保留目前最佳中心供主持人人工判斷。

## MatchCenter

`MatchCenter` 是本場比賽實際使用的中心資料。

它包含：

| 欄位 | 用途 |
| --- | --- |
| `x` | Overworld 比賽中心 X。 |
| `z` | Overworld 比賽中心 Z。 |
| `borderSize` | 初始邊界大小。 |

後續流程會使用同一個 `MatchCenter`：

- Overworld world border 中心。
- 傳送與隨機散落位置的基準。
- Nether 對應中心換算。
- Chunky 預生成中心。
- staff / spectator 前往中心的定位。

因此新版的「中心點」不是世界原點 `0,0`，而是本場比賽採用的 `MatchCenter`。

## 候選中心產生方式

目前實機 `/uhc regen` 啟用 CenterCleaner 時，候選中心由 `CenterCandidateGenerator.expandingLandSearch(...)` 產生。

流程：

1. 以新世界的 world spawn 作為搜尋基準。
2. 將基準座標對齊到 chunk 邊界。
3. 第一個候選中心就是基準中心。
4. 之後每隔 `256` 格向外擴展一圈。
5. 每一圈依序加入東、西、南、北與四個對角方向。
6. 直到達到 `Search_Candidate_Count`。

預設設定：

```yaml
CenterCleaner:
  Preview_During_Search: false
  Debug_Search_Output: false
  Search_Candidate_Count: 15
```

若 `Search_Candidate_Count` 設定小於等於 `0`，程式會 fallback 成 `25`。

注意：程式中仍保留 `CenterValidationService` 的 25 候選 / 時間限制 API，主要用於非 runtime service 與測試；目前 `/uhc regen` 的實機搜尋走 `CenterCleaner` runtime task，不是該 service 的完整同步搜尋流程。

## 抽樣階段

實機搜尋會逐一評估候選中心。每個候選中心主要分三個階段：

| 階段 | runtime 抽樣 | 用途 |
| --- | --- | --- |
| `CENTER_SCAN` | `5 x 5` | 先檢查中心附近是否可站立、水域是否過高、高度差與斷崖風險。 |
| `COARSE_SCAN` | `5 x 5` | 快速掃描整個初始邊界範圍。 |
| `DETAILED_SCAN` | `7 x 7` | 對整個初始邊界範圍做較細評估，供最終分數使用。 |

實機流程會先做 `CENTER_SCAN`。如果候選中心正中心是 ocean biome，該候選會快速略過外圍詳掃，避免浪費時間。

中心掃描半徑由初始邊界大小決定：

```text
centerRadius = initialBorderSize * 0.06
```

但會限制在：

```text
最小 96
最大 192
```

因此即使初始邊界很小或很大，中心精掃範圍也不會過度縮小或擴大。

## 抽樣資料

每個抽樣點會讀取：

| 資料 | 用途 |
| --- | --- |
| biome key | 判斷 ocean、river、forest、mountain 等類型。 |
| surface material | 判斷最高方塊是否是水面、kelp、seagrass 等。 |
| surface Y | 判斷高度、中心高度差與高地風險。 |
| sea level | 作為高地與極端高地基準。 |
| standable | 判斷最高方塊是否可站立，且上方是否為空氣。 |
| neighbor height diff | 比較東西南北 `16` 格外高度差，判斷斷崖風險。 |

高度判斷：

| 項目 | 條件 |
| --- | --- |
| highland | `surfaceY >= seaLevel + 55` |
| extreme highland | `surfaceY >= seaLevel + 85` |
| cliff | 鄰近高度差 `>= 24` |

## Biome 分類

目前 biome 分類是固定清單。

Ocean 類：

- `ocean`
- `deep_ocean`
- `warm_ocean`
- `lukewarm_ocean`
- `cold_ocean`
- `frozen_ocean`
- `deep_warm_ocean`
- `deep_lukewarm_ocean`
- `deep_cold_ocean`
- `deep_frozen_ocean`

Water-like 類：

- `river`
- `frozen_river`
- `swamp`
- `mangrove_swamp`

Forest 類：

- `forest`
- `birch_forest`
- `flower_forest`
- `taiga`
- `cherry_grove`

Dense forest 類：

- `dark_forest`
- `jungle`
- `bamboo_jungle`
- `old_growth_birch_forest`
- `old_growth_pine_taiga`
- `old_growth_spruce_taiga`

Mountain hint 類：

- `windswept_hills`
- `windswept_forest`
- `windswept_gravelly_hills`
- `stony_peaks`
- `jagged_peaks`
- `frozen_peaks`
- `snowy_slopes`

Biome key 會忽略 namespace，例如 `minecraft:ocean` 會當作 `ocean`。

## 3 x 3 區域分布

除了整體比例外，系統也會把初始邊界內的抽樣點切成 `3 x 3` 區域。

用途是避免這種情況：

- 總水域比例看起來還能接受。
- 但某一側或相鄰兩區幾乎都是大片水域。
- 導致一部分玩家出生區品質明顯很差。

區域評估會計算：

| 指標 | 意義 |
| --- | --- |
| `maxSectionWaterRatio` | 單一區域最高總水域比例。 |
| `maxAdjacentSectionWaterRatio` | 相鄰兩區最高總水域比例。 |
| `maxSectionLargeWaterRatio` | 單一區域最高大片水域風險。 |
| `maxAdjacentSectionLargeWaterRatio` | 相鄰兩區最高大片水域風險。 |
| `lowSectionCount` | 大片水域風險超過 `45%` 的區域數量。 |

大片水域風險不是單純水方塊比例，而是加權計算：

| 類型 | 風險值 |
| --- | --- |
| ocean biome | `1.0` |
| water surface | `0.45` |
| river / swamp 等 water-like biome | `0.25` |

這樣做是為了讓海洋比小河流更嚴重，但不把所有水域都視為同等問題。

## 分數組成

每個候選中心會得到五個分數，再加權成總分。

| 分數 | 權重 | 來源 |
| --- | --- | --- |
| water | `22%` | ocean、非 ocean 水域、river / swamp 等比例。 |
| terrain | `24%` | highland、extreme highland、cliff 比例。 |
| section balance | `18%` | 3 x 3 區域水域分布與 low section 數量。 |
| center | `22%` | 中心附近大片水域、可站立比例、高度差、斷崖比例。 |
| forest | `14%` | forest 是否適量、dense forest 是否過多。 |

總分公式：

```text
total =
  waterScore * 0.22
  + terrainScore * 0.24
  + sectionBalanceScore * 0.18
  + centerScore * 0.22
  + forestScore * 0.14
```

各子分數都是 `0` 到 `100`，最後總分也是 `0` 到 `100`。

## 子分數門檻

### Water score

`waterScore` 由三個部分組成：

```text
oceanScore * 0.75
+ nonOceanWaterScore * 0.20
+ riverScore * 0.05
```

| 指標 | 100 分範圍 | 約 75 分門檻 | 明顯低分 / 0 分門檻 |
| --- | --- | --- | --- |
| ocean ratio | `<= 8%` | `20%` | `> 35%` |
| non-ocean water ratio | `<= 25%` | `45%` | `> 65%` |
| river ratio | `<= 15%` | `30%` | `> 45%` |

### Terrain score

取以下三者的最低分：

| 指標 | 100 分範圍 | 約 75 分門檻 | 明顯低分 / 0 分門檻 |
| --- | --- | --- | --- |
| highland ratio | `<= 18%` | `30%` | `> 42%` |
| extreme highland ratio | `<= 4%` | `8%` | `> 15%` |
| cliff ratio | `<= 12%` | `35%` | `> 60%` |

### Section balance score

取以下三者的最低分：

| 指標 | 100 分範圍 | 約 75 分門檻 | 明顯低分 / 0 分門檻 |
| --- | --- | --- | --- |
| low section count | `<= 2` | `5` | `>= 8` |
| max section large water ratio | `<= 35%` | `55%` | `> 75%` |
| max adjacent section large water ratio | `<= 35%` | `52%` | `> 70%` |

### Center score

取以下四者的最低分：

| 指標 | 理想範圍 | 風險門檻 |
| --- | --- | --- |
| center large water ratio | `<= 8%` 最佳，`20%` 開始扣更多分，`> 35%` 為高風險。 |
| standable ratio | `>= 75%` 最佳，`< 55%` 為高風險。 |
| center height spread | `<= 40` 最佳，`70` 開始扣更多分，`> 110` 為高風險。 |
| cliff ratio | `<= 12%` 最佳，`35%` 開始扣更多分，`> 60%` 為高風險。 |

### Forest score

取以下兩者的最低分：

| 指標 | 判斷 |
| --- | --- |
| forest ratio | `10%` 到 `35%` 最佳；太少會扣分，超過 `45%` 明顯扣分，超過 `55%` 高風險。 |
| dense forest ratio | `<= 10%` 最佳，`18%` 開始扣更多分，`> 28%` 高風險。 |

森林不是越少越好。適量森林可接受，主要避免整張圖太多密林或高遮蔽區域。

## 硬性拒絕與軟性阻礙

硬性拒絕原因會讓候選中心直接變成 `REJECTED`，即使總分看起來不低。

| 原因 | 條件 |
| --- | --- |
| `OCEAN_RATIO_TOO_HIGH` | ocean ratio `> 35%` |
| `WATER_RATIO_TOO_HIGH` | total water ratio `> 75%` |
| `SECTION_WATER_TOO_HIGH` | max section large water ratio `> 75%` |
| `ADJACENT_SECTION_WATER_TOO_HIGH` | max adjacent section large water ratio `> 70%` |
| `CENTER_WATER_TOO_HIGH` | center large water ratio `> 35%` |
| `CENTER_STANDABLE_RATIO_TOO_LOW` | standable ratio `< 55%` |
| `CENTER_HEIGHT_SPREAD_TOO_HIGH` | center height spread `> 110` |
| `CENTER_CLIFF_RATIO_TOO_HIGH` | cliff ratio `> 60%` |
| `TOO_MANY_LOW_SECTIONS` | low section count `>= 8` |

軟性阻礙不一定會讓候選中心變成 `REJECTED`，但會阻止它成為 `RECOMMENDED`。

| 原因 | 條件 |
| --- | --- |
| `FOREST_RATIO_TOO_HIGH` | forest ratio `> 55%` |
| `DENSE_FOREST_RATIO_TOO_HIGH` | dense forest ratio `> 28%` |
| `HIGHLAND_RATIO_TOO_HIGH` | highland ratio `> 42%` |
| `EXTREME_HIGHLAND_RATIO_TOO_HIGH` | extreme highland ratio `> 15%` |

例如一張圖分數很高，但 dense forest ratio 太高，狀態可能會是 `ACCEPTABLE`，而不是 `RECOMMENDED`。

## 狀態判斷

候選中心狀態判斷順序：

1. 若命中硬性拒絕，或總分低於 `50`，狀態為 `REJECTED`。
2. 若沒有任何阻礙原因，且總分 `>= 75`，狀態為 `RECOMMENDED`。
3. 若總分 `>= 60`，狀態為 `ACCEPTABLE`。
4. 其餘 `50` 到 `59` 分為 `POOR`。

`TIME_LIMITED` 與 `CANCELLED` 是搜尋結果層級的狀態，不是單一候選中心分數狀態。

目前實機 `CenterCleaner` runtime task 主要依候選數逐步完成搜尋；`TIME_LIMITED` 主要保留給 `CenterValidationService` 這類 service API 使用。

## Debug output 判讀

若在 `settings.yml` 開啟：

```yaml
CenterCleaner:
  Debug_Search_Output: true
```

每個候選中心會輸出較詳細的計算資訊。

常見欄位：

| 欄位 | 意義 |
| --- | --- |
| `候選 n/total` | 目前第幾個候選中心。 |
| `X / Z` | 候選中心座標。 |
| `狀態` | 此候選中心的分數狀態。 |
| `總分` | 加權後總分。 |
| `加權1` | water、terrain、section balance 的分數與權重。 |
| `加權2` | center、forest 的分數與權重。 |
| `水域` | ocean、river / swamp、總水域、中心水域、中心大片水域。 |
| `水域分布` | 3 x 3 區域與相鄰區域的最高水域風險。 |
| `地形` | forest、dense forest、highland、extreme highland 比例。 |
| `中心` | 可站立比例、斷崖比例、高度差、low section 數量。 |
| `扣分原因` | 對應 `CenterScoreReason` 的中文說明。 |

判讀建議：

1. 先看 `狀態` 與 `總分`。
2. 若狀態不是 `RECOMMENDED`，看 `扣分原因`。
3. 若主要問題是中心水域、高度差或斷崖，通常代表中心附近不適合開局。
4. 若主要問題是區域分布，可能是某側地圖有大片水域。
5. 若只有森林或高地軟性原因，可人工預覽判斷是否仍可接受。

## 設定項目

目前使用者可調的中心搜尋設定在 `settings.yml`：

| 設定 | 預設 | 用途 |
| --- | --- | --- |
| `Preview_During_Search` | `false` | 搜尋每個候選中心時，是否把主持人傳送到該候選中心。 |
| `Debug_Search_Output` | `false` | 是否輸出每個候選中心的詳細評分資訊。 |
| `Search_Candidate_Count` | `15` | 最多評估幾個候選中心。 |
| `Generator_Settings` | 一段 generator JSON | 目前主要用於不啟用 CenterCleaner 的直接建圖流程。 |

一般開服不建議長期開啟 `Debug_Search_Output`，否則 console 與聊天訊息會很吵。

`Preview_During_Search` 若開啟，主持人會被傳送到每個候選中心，適合人工觀察搜尋過程，但也會讓體感流程更頻繁跳轉。

## 實務建議

1. 預設先使用 `Search_Candidate_Count: 15`，不要一開始就大幅提高。
2. 如果常找不到可接受中心，再考慮提高候選數。
3. 如果世界本身 seed 很差，增加候選數不一定能解決，直接換 seed 可能更有效。
4. `RECOMMENDED` 可優先人工預覽。
5. `ACCEPTABLE` 不代表差，只是可能有軟性問題或分數未達推薦門檻。
6. `POOR` 與 `REJECTED` 不建議直接跑圖，除非主持人已人工確認地形仍符合需求。
7. CenterCleaner 是輔助工具，不取代主持人對實際地形與比賽需求的判斷。

## 修改規則時的注意事項

如果未來要調整分數或門檻，建議同時處理：

1. `CenterCandidateScore` 的權重、分數函式與狀態門檻。
2. `CenterCandidateScoreTest` 的對應測試。
3. `docs/map-selection.md` 的摘要。
4. 本文件的細節門檻。
5. 實機 `/uhc regen` 預覽與 Chunky 預生成 smoke test。

不要只改文件或只改常數。選圖規則會直接影響主持人對地圖品質的信任，因此分數、訊息與文件需要保持一致。
