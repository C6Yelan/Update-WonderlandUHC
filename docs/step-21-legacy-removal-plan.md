# Step 21 Legacy Foundation / NMS 移除規劃

建立日期：2026-05-16

本文件承接 `docs/steps.md` Step 21，用來記錄 legacy Foundation / DatouNMS 移除前的現況盤點、切片順序與驗收 gate。Step 21 不再新增頂層 step；它本身是最終移除 gate，但實作必須拆成多個可驗證子切片。

## 判斷結論

Step 21 不適合一刀直接完成。

原因是目前 legacy 殘留不是單一 dependency declaration，而是同時分布在 plugin lifecycle、command framework、menu framework、YAML settings lifecycle、scenario runtime、scoreboard、登入 gate、team color model、metadata state 與 NMS fallback。若一次移除，會把多個高風險行為混在同一個 diff，難以判斷 regression 來源，也容易變成為了移除依賴而重寫已可運作的功能。

本步驟採用「一個 Step 21、多個子切片」：

- 不新增 Step 21 之外的頂層 step。
- 不把 Step 22 的最終功能對照驗收提前混入 Step 21；Step 22 只做檢查，不承接已知應在 Step 21 完成的程式碼修改。
- 不把 Step 23 的 README / 使用者文件完整改寫提前混入 Step 21。
- 每個程式碼切片都必須保持可封裝、可啟動，並留下清楚的 done / not-done gate。

## 目前現況盤點

以下數字來自 2026-05-16 在 `Update-WonderlandUHC` 目前 `update-to-1.21` 分支上的文字掃描。它們是規劃用的規模指標，不代表獨立 bug 數。

| 掃描項目 | 目前命中文件數 | 代表意義 |
| --- | ---: | --- |
| `org.mineacademy.fo` / `LegacyFoundationAdapter` / `lib-foundation` | 208 | Foundation 仍是大範圍 runtime / framework dependency。 |
| direct `org.mineacademy.fo` import | 114 | command、menu、settings、config、utility 仍直接依賴 Foundation API。 |
| `LegacyFoundationAdapter` | 124 | Foundation 已部分退到 adapter，但 adapter isolation 不等於依賴已可拔除。 |
| DatouNMS / NMS / `LegacyDatouNmsAdapter` / `net.minecraft` | 18 | NMS 功能已集中很多，但仍有多個 runtime call site 與 `RuntimeUtil` reflection。 |
| `PlayerLoginEvent` / `ChatColor` / metadata / Foundation config / Foundation menu color | 43 | Step 19 / Step 20 明確延後到 Step 21 的相容性與資料模型項目。 |

依主要 package 粗分，Foundation / `LegacyFoundationAdapter` 命中集中在：

| package | 命中文件數 | 主要風險 |
| --- | ---: | --- |
| `command/**` | 45 | `SimpleCommand` / `SimpleSubCommand` / `SimpleCommandGroup` lifecycle、permission、tab completion、sender helper。 |
| `menu/**` | 25 | `ConfigMenu` / `ConfigMenuPagged` / button / item drawer / color picker / inventory editor。 |
| `settings/**` | 6 | `SimpleSettings`、`YamlStaticConfig`、`YamlConfigLoader` 與靜態設定初始化。 |
| `scenario/**` | 27 | material、sound、scheduler、error handling、NMS fallback 與 gameplay path。 |
| `game/**` | 43 | player state、login gate、death flow、timer、join/quit、combat relog metadata。 |
| `util` / `tools` / `storage` / `scoreboard` | 23 | material helper、file/config store、metadata、scoreboard、runtime metric。 |

`build.gradle` 目前仍有：

- `foundationVersion`
- `useLocalFoundation`
- `localFoundationJar`
- Foundation implementation dependency
- Shadow relocate `org.mineacademy`
- DatouNMS implementation dependency

`WonderlandUHC` 目前仍 `extends SimplePlugin`，啟動與 reload lifecycle 仍透過 Foundation `SimplePlugin` 提供的 hook 與 command registration。

## 從前面步驟延後到 Step 21 的項目

Step 19 / Step 20 已明確把下列項目排除到 Step 21，不能在本步驟用短期 wrapper、`@SuppressWarnings` 或只改 import 當作完成：

| 項目 | 目前判斷 |
| --- | --- |
| `PlayerLoginEvent` | 不是一對一 API 替換；會影響登入 gate、白名單、等待 host、滿員、遊戲中加入、bypass 權限與 `UHCPlayer` 建立時機。 |
| `ChatColor / legacy color model` | 牽涉 team public API、Foundation `ColorMenu`、team menu、scoreboard team color 與舊色碼相容。 |
| `MetadataValue / legacy state` | 牽涉 `UHCPlayer`、combat relog、tutorial 與 Foundation metadata helper。 |
| Foundation config API | 例如 `YamlConfig#clearLoadedSections()`、`YamlConfig` 繼承與 settings reload lifecycle。 |
| `this-escape` constructor warnings | 多數來自 Foundation command/menu/config lifecycle；若要真正消除，應在移除 Foundation lifecycle 時自然消失，不應單獨加 suppress。 |

## 子切片順序

### 21.0 規劃與 inventory 固定

目標：

- 固定本文件作為 Step 21 的執行地圖。
- 確認 Step 21 不新增頂層 step，只拆子切片。
- 在每個子切片開始前重新跑對應 `rg`，避免使用過期數字。

完成條件：

- 本文件存在並被 `docs/steps.md` Step 21 指向。
- 目前 legacy 殘留被分類到後續子切片。
- 尚未修改程式碼。

### 21.1 DatouNMS / NMS 移除

目標：

- 移除 `com.github.lulu2002:DatouNms` dependency。
- 刪除或改名 `LegacyDatouNmsAdapter`，不再用 legacy NMS 名義保留未驗證依賴。
- 將仍需要的低階行為改成 Paper/Bukkit API 或明確接受的 fallback。

優先處理行為：

| 行為 | 預期方向 |
| --- | --- |
| absorption hearts | 優先使用 Bukkit/Paper player absorption API；保留 potion-effect fallback 只在需要時做本地 helper。 |
| armor points | 使用 Bukkit attribute / material fallback，避免依賴 DatouNMS `ArmorInfo`。 |
| exp pickup control | 若 Paper/Bukkit 無等價控制，需在 Step 21 內決定正式 no-op / 移除呼叫 / 其他替代，並記錄為 Step 22 只需驗證的已知差異。 |
| death animation | 若無正式 API，不重新導入 NMS；需在 Step 21 內明確接受移除或改成正式 no-op，Step 22 只驗證結果。 |
| custom exp orb | 使用 Bukkit `ExperienceOrb` fallback。 |
| old enchant simulation | 依既有升級決策標記為正式移除或 no-op，不建立新 NMS。 |
| fast block set | 使用 Bukkit/Paper block set API；高風險大量方塊操作用實測驗證效能與正確性。 |
| large chest merge | 使用 Bukkit `BlockData` chest type fallback。 |
| TPS reflection | 移除 `net.minecraft.server` reflection；只保留 Paper `getTPS()` / 已接受 fallback。 |

切片限制：

- 不同時重寫 command/menu/settings。
- 不為一兩個 NMS call site 建立大型 platform framework。
- 若功能已在 `LegacyDatouNmsAdapter` 內有 Bukkit fallback，優先把 fallback 變成正式實作。

驗收：

- `rg -n "DaTouNMS|datounms|NewerSpigotAPI|LegacyDatouNmsAdapter|net\\.minecraft|org\\.bukkit\\.craftbukkit" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle`
- `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean`
- Paper `1.21.11` server startup smoke test。

#### 21.1 現況盤點：DatouNMS / NMS 使用點

盤點日期：2026-05-16

盤點分支：`step-21-legacy-removal`

此段為 Step 21 開始前的初始只讀盤點。後續實作進度記錄於本節下方，避免把已完成項目誤判為仍待處理。初始掃描指令：

```bash
rg -n "LegacyDatouNmsAdapter|DaTouNMS|NewerSpigotAPI|net\\.minecraft|org\\.bukkit\\.craftbukkit" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle
rg -n "LegacyDatouNmsAdapter\\.current\\(\\)\\." Update-WonderlandUHC/src/main/java
```

目前 DatouNMS / NMS 相關殘留分成三層：

1. `build.gradle` 仍宣告 `implementation 'com.github.lulu2002:DatouNms:1.2.2'`。
2. `legacy/LegacyDatouNmsAdapter.java` 仍直接 import `DaTouNMS`、`ArmorInfo`、`NewerSpigotAPI`，並透過 `PlatformCapabilities` 暫時集中能力旗標。
3. 主插件多處仍呼叫 `LegacyDatouNmsAdapter.current()`；另有 `RuntimeUtil` 直接反射 `net.minecraft.server.<version>.MinecraftServer` 讀 TPS。

使用點分類：

| 類型 | 使用點 | 目前行為 | Step 21 判斷 |
| --- | --- | --- | --- |
| bootstrap setup | `bootstrap/PluginBootstrap.java#setupNms()` | 啟動時呼叫 `LegacyDatouNmsAdapter.initialize(plugin)`；DatouNMS 不支援時降級成 unavailable。 | 第一刀後應移除 setup；若仍需 capability 狀態，改成非 DatouNMS 的正式 platform capability 或直接刪除。 |
| dependency declaration | `build.gradle` | `implementation 'com.github.lulu2002:DatouNms:1.2.2'`。 | 不能第一步先拔；需等所有 call site 轉完後在 21.7 最終拔除。 |
| absorption hearts | `scoreboard/SimpleScores.java`、`util/PlayerUtils.java`、`scenario/impl/death/ScenarioShiftKill.java` | 用 DatouNMS 讀 absorption；adapter 內已有 `Player#getAbsorptionAmount()` / potion effect fallback。 | 適合作為第一批正式化：把 fallback 移到主插件 helper，使用點改走 helper，不保留 DatouNMS 路徑。 |
| armor points | `util/PlayerUtils.java` | 用 DatouNMS armor point；adapter 內已有 Bukkit attribute 與 vanilla material fallback。 | 適合作為第一批正式化：直接把現有 fallback 變正式實作。 |
| exp pickup control | `RolePlayerApplier`、`RoleSpectatorApplier`、`RoleStaffApplier`、`RespawnCommand` | DatouNMS 可用時控制玩家是否撿經驗；不可用時 no-op。 | Paper/Bukkit 若無穩定等價 API，需在 Step 21 內改成明確 no-op helper、移除呼叫或完成其他替代；Step 22 只驗證這個已知差異。不得重新導入 NMS。 |
| death animation | `PlayingDeathListener`、`TestCommand` | DatouNMS 可用時播放死亡動畫；不可用時 no-op。 | 無穩定 API 時不修復 NMS；`TestCommand` 測試分支可移除或改成 no-op，正式死亡流程需在 Step 21 內決定是否接受移除。 |
| custom exp orb | `util/WorldUtils.java#spawnOrb` | DatouNMS 可生成指定 amount/value；fallback 逐顆 spawn `ExperienceOrb` 並設 experience。 | 適合作為第一批正式化：保留 Bukkit fallback，移除 DatouNMS branch。 |
| fast block set | `util/GenerateUtil.java`、`util/BorderUtil.java` | DatouNMS 可 super fast set block；fallback 使用 Bukkit `setType(..., applyPhysics)`。 | 可轉為正式 Bukkit/Paper block set helper，但需獨立測試 bedrock border / ore clump。不要和 absorption/armor 混同一刀。 |
| large chest merge | `scenario/impl/death/ScenarioTimeBomb.java` | DatouNMS `NewerSpigotAPI.mergeChest`；fallback 已用 Bukkit chest `BlockData` 設定左右大箱。 | 適合作為第一批或第二批正式化；需測 Time Bomb 死亡箱仍能合併與存物品。 |
| old enchant simulation | `listener/OldEnchantListener.java` | random seed、old costs、hide enchants 依 DatouNMS；其餘 lapis / exp level 邏輯仍在 listener。 | 已完成：1.7 舊附魔模擬正式移除，解除 `OldEnchantListener` 註冊並刪除檔案；`settings.yml` / `Settings.OldEnchant` 暫留作舊設定相容欄位，不再有 runtime 行為。 |
| TPS NMS reflection | `util/RuntimeUtil.java` | 先走 Paper `Bukkit#getTPS()`，再走 reflected Paper `getTPS()`，最後仍嘗試 `net.minecraft.server.<version>.MinecraftServer#recentTps`。 | 應在 21.1 移除最後的 NMS class reflection，只保留 Paper API / accepted fallback。這可和 DatouNMS 第一批一起做，因為同屬 NMS 搜尋 gate。 |

目前方法呼叫粗略數量：

| 方法 | 呼叫數 | 建議處理批次 |
| --- | ---: | --- |
| `getAbsorptionHearts` | 3 | 已完成：改走 `PlayerUtils#getAbsorptionHearts`，使用 Paper/Bukkit API 與 potion fallback。 |
| `getArmorPoints` | 1 | 已完成：改走 Bukkit attribute / item meta attribute，不再維護材質名稱表。 |
| `spawnExpOrb` | 1 | 已完成：改走 Bukkit `ExperienceOrb`。 |
| `mergeLargeChest` | 1 | 已完成：`ScenarioTimeBomb` 改用 Bukkit chest `BlockData` 設定左右大箱。 |
| `setCanPickupExp` | 4 | 本輪完成：移除 role / respawn 的 DatouNMS 呼叫，改用 Paper `Player#setExpCooldown(...)` 保留舊版 cooldown 語意，並用 `EntityTargetLivingEntityEvent` / `PlayerPickupExperienceEvent` 補足 XP orb target / pickup 邊界。 |
| `playDeathAnimation` | 2 | 已完成：正式死亡流程與 `TestCommand` debug 分支不再呼叫 DatouNMS；`Death_Animation` 設定在 1.21 線成為已接受 no-op。 |
| `setBlockFast` | 3 | 已完成：`GenerateUtil` / `BorderUtil` 改走 Bukkit `Block#setType(..., false)`。 |
| old enchant methods | 3 | 已完成：解除 `OldEnchantListener` 註冊並刪除檔案；`OldEnchant` 設定暫留為相容欄位。 |

#### 21.1 進度更新

更新日期：2026-05-16

已完成項目：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 已推送：`1a07614` | simple DatouNMS reads | `SimpleScores`、`ScenarioShiftKill`、`PlayerUtils`、`WorldUtils`、`RuntimeUtil` 不再透過 DatouNMS 讀 absorption、armor points、custom exp orb 或 TPS NMS reflection。 | `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；Paper `1.21.11` server 以 `start.bat` 啟動到 `Done`。 |
| 已推送：`0c53444` | block placement | `GenerateUtil`、`BorderUtil` 的 `setBlockFast` 呼叫改成直接 Bukkit `Block#setType(..., false)`；沒有新增 helper 或抽象層。 | `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；Paper `1.21.11` server 以 `start.bat` 啟動到 `Done`。 |
| 已推送：`70c3d6c` | large chest merge | `ScenarioTimeBomb` 不再呼叫 `LegacyDatouNmsAdapter#mergeLargeChest`，改用 Bukkit chest `BlockData` 設定左右箱與 facing；沒有新增 helper 或抽象層。 | `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；Paper `1.21.11` server 以 `start.bat` 啟動到 `Done`。 |
| 已推送：`e1d3394` | death animation | `PlayingDeathListener` 不再呼叫 DatouNMS 播放假玩家死亡動畫；`TestCommand` 移除 `ani` debug 分支。Paper/Bukkit 無穩定等價 API，本切片不導入 packet/NMS 替代。 | `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；Paper `1.21.11` server 以 `start.bat` 啟動到 `Done`。 |
| 已推送：`4be50ea` | old enchant removal | `FeatureRegistry` 不再註冊 `OldEnchantListener`，並刪除 listener 檔案。1.7 舊附魔模擬正式移除；`OldEnchant` 設定暫留相容。 | `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；Paper `1.21.11` server 以 `start.bat` 啟動到 `Done`。 |
| 本輪完成 | exp pickup control | `RolePlayerApplier`、`RoleSpectatorApplier`、`RoleStaffApplier`、`RespawnCommand` 不再呼叫 `setCanPickupExp`；role / respawn 改用 Paper `Player#setExpCooldown(...)`，staff/spectator 設為 `Integer.MAX_VALUE`，player/respawn 設回 `0`。另新增 `ExperiencePickupListener`，在 XP orb target 選到 `RoleName.SPECTATOR` / `RoleName.STAFF` 時改指向最近的 `RoleName.PLAYER`，沒有參賽玩家則清成 `null`；pickup 事件仍取消作保險。這不建立大型狀態系統，也不手寫 orb 物理。 | `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`。已接受 staff/spectator client 視角可能仍有 XP 吸附假象，因其不影響公平性且活著玩家視角正常。 |
| 已推送：`e8c0bb9` | dependency / adapter cleanup | 移除 `build.gradle` 的 DatouNMS dependency、`WonderlandUHC#onPluginStart()` 的 `bootstrap.setupNms()` 呼叫、`PluginBootstrap#setupNms()`、`LegacyDatouNmsAdapter.java`、`PlatformCapabilities.java` 與只測 legacy adapter 的 `LegacyDatouNmsAdapterTest`。本刀不處理 Foundation，也不新增 DatouNMS replacement service。 | `rg` gate 已確認 `src/main/java` / `build.gradle` 無 DatouNMS、NMS、CraftBukkit 與 legacy DatouNMS 命中；`scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，且 DaTouNMS unsupported 訊息已不再出現。 |

目前 `LegacyDatouNmsAdapter.current()` 主流程剩餘呼叫：

| 類型 | 使用點 | 下一步判斷 |
| --- | --- | --- |
| 無 | 無 | 主流程 call site 已清空；`PluginBootstrap#setupNms()`、`LegacyDatouNmsAdapter` 本體、`PlatformCapabilities` 與 `build.gradle` DatouNMS dependency 已在本輪移除，且封裝與 Paper startup 已通過。 |

#### 21.1 DatouNMS dependency / adapter cleanup 盤點

更新日期：2026-05-16

本輪只讀盤點指令：

```bash
rg -n "DaTouNMS|datounms|NewerSpigotAPI|LegacyDatouNmsAdapter|PlatformCapabilities|setupNms|net\\.minecraft|org\\.bukkit\\.craftbukkit|ArmorInfo" src/main/java build.gradle
rg -n "LegacyDatouNmsAdapter\\.current\\(\\)|capabilities\\(|is[A-Za-z]+Available\\(" src/main/java
```

盤點結果：

| 類型 | 目前殘留 | 判斷 |
| --- | --- | --- |
| dependency | 已移除 `build.gradle` 的 `implementation 'com.github.lulu2002:DatouNms:1.2.2'`。 | DatouNMS 不再是主插件 build/runtime dependency。 |
| bootstrap setup | 已移除 `WonderlandUHC#onPluginStart()` 的 `bootstrap.setupNms()` 呼叫與 `PluginBootstrap#setupNms()` 方法。 | 不需要替代初始化流程。 |
| adapter 本體 | 已刪除 `legacy/LegacyDatouNmsAdapter.java`。 | 未把舊 fallback 抽成新 helper；正式使用點已各自改成 Bukkit/Paper 實作。 |
| adapter test | 已刪除 `src/test/java/org/mcwonderland/uhc/legacy/LegacyDatouNmsAdapterTest.java`。 | 測試目標已被正式移除；不為測試重新建立 legacy adapter 或替代 helper。 |
| capability wrapper | 已刪除 `platform/PlatformCapabilities.java`。 | 該類只服務 DatouNMS adapter，無需保留。 |
| NMS / CraftBukkit reflection | `src/main/java` / `build.gradle` 搜尋未發現 DatouNMS、NMS、CraftBukkit 或 `ArmorInfo` 使用；後續補強 gate 也移除 `PlayerUtils#breakBlockNms`、`LegacyFoundationAdapter#getHandleEntity`、`newBlockPosition` 與 `ObjectCreator.NMS_BLOCKPOSITION`。 | 搜尋 gate、封裝與 server startup 均已通過。 |

本輪修改範圍：

- `build.gradle`
- `src/main/java/org/mcwonderland/uhc/WonderlandUHC.java`
- `src/main/java/org/mcwonderland/uhc/bootstrap/PluginBootstrap.java`
- `src/main/java/org/mcwonderland/uhc/legacy/LegacyDatouNmsAdapter.java`
- `src/main/java/org/mcwonderland/uhc/platform/PlatformCapabilities.java`
- `src/test/java/org/mcwonderland/uhc/legacy/LegacyDatouNmsAdapterTest.java`
- `docs/step-21-legacy-removal-plan.md`
- `docs/steps.md`

本刀不處理：

- `LegacyFoundationAdapter` 或 Foundation dependency。
- `org.mineacademy.fo.*` command/menu/settings 繼承。
- `PlayerLoginEvent`、ChatColor、metadata state 等 Step 21 後續項目。
- 重新抽一個 DatouNMS replacement service。

本輪驗收：

```bash
rg -n "DaTouNMS|datounms|NewerSpigotAPI|LegacyDatouNmsAdapter|PlatformCapabilities|setupNms|net\\.minecraft|org\\.bukkit\\.craftbukkit|ArmorInfo" src/main/java build.gradle
rg -n "NMS|Nms|nms|breakBlockNms|newBlockPosition|getHandleEntity|ObjectCreator|playerInteractManager" src/main/java build.gradle
scripts/package-plugin-1.21.sh --skip-foundation --no-clean
```

`rg` gate、封裝、部署與 Paper `1.21.11` `start.bat` startup smoke test 均已通過。

後續建議順序：

1. commit 並 push dependency / adapter cleanup。
2. 進入 21.2 Foundation utility adapter 拆除盤點；不得和 21.1 DatouNMS 收尾混在同一刀。

後續切片仍不建議包含：

- 重寫 role / spectator / respawn 流程。
- 把 dependency cleanup 與 Foundation utility adapter 拆除混在同一刀處理。

### 21.2 Foundation utility adapter 拆除

目標：

- 逐步移除非 inheritance 型的 `LegacyFoundationAdapter` 呼叫。
- 把已廣泛共用且確實需要統一語意的功能改成主插件自己的小型 helper。
- 對單一使用點或 thin behavior 直接改成 Bukkit/Paper API，不抽 service。

建議依功能族群處理：

| 功能族群 | 代表方法 / 類型 | 預期方向 |
| --- | --- | --- |
| logging / error | `log`、`logNoPrefix`、`error` | 小型 plugin logger/helper；保留舊色碼輸出語意。 |
| scheduler / events | `runLater`、`runTimer`、`callEvent` | 使用 Bukkit scheduler / plugin manager；已有 port 時優先沿用既有 port。 |
| placeholder / text | `replaceToArray`、`replaceTimeToString`、`colorize` | 收斂到既有 `GamePlaceholderReplacer` / `TimePlaceholderFormatter` / text helper，不引入新文字系統。 |
| material / item / sound | `materialOf`、`itemOf`、`playSound`、`CompMaterial`、`CompSound` | 優先使用 Bukkit `Material` / `Sound` 與既有 parser；舊 alias 相容限縮在 config/input 邊界。 |
| metadata / entity state | `setTempMetadata`、`getTempMetadata` | 轉成 Bukkit `PersistentDataContainer`、本地 map，或明確 owner-managed state；不可只包一層 suppress。 |
| file/config utility | `getFile`、`getOrMakeFile`、`extractFile` | 使用 `JavaPlugin#getDataFolder()` 與既有 asset port。 |
| command component | `sendRunCommandComponent` | 使用 Adventure component API 或既有 text helper。 |

#### 21.2 只讀盤點：`LegacyFoundationAdapter` 使用點

更新日期：2026-05-16。

本輪只做盤點與文件更新，未修改程式碼。掃描指令：

```bash
rg -n "LegacyFoundationAdapter\.([A-Za-z0-9_]+)" src/main/java src/test/java
rg -o "LegacyFoundationAdapter\.[A-Za-z0-9_]+" src/main/java src/test/java | sed 's/.*LegacyFoundationAdapter\.//' | sort | uniq -c | sort -nr
rg -n "org\.mineacademy\.fo|LegacyFoundationAdapter|lib-foundation" src/main/java build.gradle | wc -l
```

盤點結果：

- `org.mineacademy.fo`、`LegacyFoundationAdapter` 與 `lib-foundation` 相關命中合計仍有 `648` 行；移除 `LegacyFoundationAdapter` 只是 21.2 utility adapter 拆除的一部分，不能視為已移除 Foundation。
- `LegacyFoundationAdapter` 仍包住 logging、scheduler、event、placeholder/text、material/item/sound、metadata/reflection、player/command、file/config、random/math 與 version/platform 等多種 Foundation helper。
- 高頻方法包含：`materialOf` `24`、`error` `24`、`callEvent` `19`、`runLater` `18`、`log` `12`、`getOnlinePlayers` `12`、`colorize` `9`、`replaceToList` `8`、`isAir` `7`、`logNoPrefix` `7`。
- 中低頻但風險高的方法包含：`setTempMetadata` / `getTempMetadata` / `hasTempMetadata`、`setChunkForceLoaded`、`getHandleEntity`、`newBlockPosition`、`invoke`、`commandGroupRegistrar`、`sendRunCommandComponent`。

依實際使用點重新排序的 21.2 建議：

| 優先級 | 功能族群 | 盤點判斷 |
| --- | --- | --- |
| 先做 | logging / console | `log`、`logNoPrefix`、`logFramed`、`consoleLineSmooth` 屬於低 gameplay 風險，且多處共享輸出語意；第一刀可建立極小 logger/helper 或直接替換明確使用點。 |
| 暫緩 | `error` | 次數高且多在 scenario / listener catch block；可作為 logging 第二刀，先確認舊 `Common.error` 的輸出與 stack trace 語意。 |
| 暫緩 | scheduler / events | `runLater`、`runTimer`、`callEvent` 牽涉 tick 時序與 async/sync 邊界；可做，但不應作為 21.2 第一刀。 |
| 暫緩 | material / item / sound | `materialOf` 次數最高，且與 config alias、scenario、menu/settings 顯示混在一起；需要更細切，不可一次大改。 |
| 暫緩 | metadata / reflection / chunk | 牽涉 runtime state 與 Paper 版本差異；不得只包一層新的 suppress helper。 |
| 不屬於 21.2 第一刀 | command / menu / settings framework | `commandGroupRegistrar`、Foundation command base、Foundation menu/config direct import 應留給 21.3、21.4、21.5 的對應切片。 |

下一個實作切片建議先處理 logging / console utility，範圍限縮為：

- `LegacyFoundationAdapter.log(...)`
- `LegacyFoundationAdapter.logNoPrefix(...)`
- `LegacyFoundationAdapter.logFramed(...)`
- `LegacyFoundationAdapter.consoleLineSmooth(...)`
- 如有實際呼叫再一併處理 `logReplacing(...)`

此切片不先處理 `error(...)`，也不碰 command、menu、settings、scheduler、material parser 或 metadata/reflection。這樣可以先拔掉一組 Foundation utility dependency，同時避免把行為風險擴散到 gameplay。

本切片實作結果：

- 新增 `PluginConsole` 承接 `log`、`logNoPrefix`、`logFramed` 與 `consoleLineSmooth` 的 console 輸出語意。
- `LegacyFoundationAdapter.log(...)`、`logNoPrefix(...)`、`logFramed(...)`、`consoleLineSmooth()`、`logReplacing(...)` 已從 adapter 移除。
- 呼叫點已改為直接使用 `PluginConsole`；`rg -n "LegacyFoundationAdapter\.(log|logNoPrefix|logFramed|consoleLineSmooth|logReplacing)" src/main/java src/test/java` 無命中。
- 本切片刻意不處理 scheduler、material parser、metadata/reflection、command、menu、settings。

後續 `error(...)` 小切片實作結果：

- `PluginConsole.error(Throwable, String...)` 承接錯誤輸出、stack trace、`%error%` placeholder 替換與 framed console message。
- `LegacyFoundationAdapter.error(...)` 已從 adapter 移除。
- 24 個 scenario / listener / model runtime failure handler 已改為 `PluginConsole.error(...)`；不改 catch 行為、不改 scenario 邏輯。
- `rg -n "LegacyFoundationAdapter\.error\(" src/main/java src/test/java` 無命中。
- 本切片不移植 Foundation `Debugger` / `errors.log` subsystem，避免把 Foundation debug layer 搬成新的大型相容層。

後續 scheduler / events 小切片實作結果：

- 新增 `PluginScheduler` 承接 `runLater`、`runAsync`、`runLaterAsync`、`runTimer` 與 `runTimerAsync`，底層直接使用 Bukkit scheduler 與 `WonderlandUHC` plugin instance。
- 新增 `PluginEvents` 承接 `callEvent` 與 `registerEvents`，底層直接使用 Bukkit plugin manager。
- `LegacyFoundationAdapter` 的 scheduler / event wrapper 已移除，呼叫點改為 `PluginScheduler` / `PluginEvents`；`rg -n "LegacyFoundationAdapter\.(runLater|runAsync|runLaterAsync|runTimer|runTimerAsync|callEvent|registerEvents)|LegacyFoundationAdapter::registerEvents" src/main/java src/test/java` 無命中。
- 本切片只替換 Foundation `Common` scheduler/event helper；不改 tick 時序、不重寫 listener/scenario 邏輯，也不建立大型 task framework。
- `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 已通過。

後續 player / action bar 小切片實作結果：

- 新增 `PluginPlayers` 承接 `onlinePlayers`、`getByUniqueId`、`getByName`、`playerNames`、`kick` 與 `sendActionBar`，底層使用 Bukkit online player API、metadata vanished flag、Adventure component 與 `PluginScheduler`。
- `LegacyFoundationAdapter` 的 `getOnlinePlayers`、`getPlayerByUUID`、`getPlayerByNick`、`getPlayerNames`、`kickPlayer`、`sendActionBar` wrapper 已移除，呼叫點改為 `PluginPlayers`。
- `rg -n "LegacyFoundationAdapter\.(getOnlinePlayers|getPlayerByUUID|getPlayerByNick|getPlayerNames|kickPlayer|sendActionBar)|LegacyFoundationAdapter::getPlayerByUUID" src/main/java src/test/java` 無命中。
- 本切片不重寫 player state、permission、team、menu 或 nickname integration；`getByName` 保留 online player exact / prefix lookup 與 vanished metadata gate，避免把 Foundation hook layer 搬回主插件。
- `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 已通過。

後續 text / placeholder / time 小切片實作結果：

- 新增 `PluginText` 承接 legacy color、strip color、placeholder pair replacement、time placeholder、clock time、five-digit number formatting 與 enum display name formatting。
- `LegacyFoundationAdapter` 的 `colorize`、`stripColors`、`replaceToArray`、`replaceToString`、`replaceToList`、`replaceJoinedToList`、`replaceTimeToArray`、`replaceTimeToString`、`replaceTimeToList`、`replaceTimePlaceholders`、`formatTime`、`formatFiveDigits`、`bountifyCapitalized` wrapper 已移除，呼叫點改為 `PluginText`。
- `rg -n "LegacyFoundationAdapter\.(colorize|stripColors|replaceToArray|replaceToString|replaceToList|replaceJoinedToList|replaceTimeToArray|replaceTimeToString|replaceTimeToList|replaceTimePlaceholders|formatTime|formatFiveDigits|bountifyCapitalized)" src/main/java src/test/java` 無命中。
- 本切片不導入 MiniMessage 或新訊息框架，也不重寫 command/menu/settings lifecycle；只承接原本散在 Foundation 的純字串 helper。
- `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 已通過。

後續 material / item / block classification 小切片實作結果：

- 新增 `PluginMaterials` 承接 vanilla `Material` lookup、`ItemStack` 建立、air / leaves / log / grass / double plant 判斷與 player inventory first item lookup。
- `LegacyFoundationAdapter` 的 `materialOf`、`itemOf`、`isAir`、`isLeaves`、`isLog`、`isLongGrass`、`isDoublePlant`、`getFirstItem` wrapper 已移除，呼叫點改為 `PluginMaterials`。
- `rg -n "LegacyFoundationAdapter\.(materialOf|itemOf|isAir|isLeaves|isLog|isLongGrass|isDoublePlant|getFirstItem)" src/main/java src/test/java` 無命中。
- 本切片只處理明確 vanilla material 與 block classification；不處理 `ScenarioConfig` / Foundation `YamlConfig#getMaterial`、Foundation `ItemCreator`、menu material parser 或舊 alias config migration。
- `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 已通過。

後續 random / math 小切片實作結果：

- 新增 `PluginRandom` 承接 `nextItem`、`chance`、`nextBoolean`，底層使用 Java `ThreadLocalRandom`；不建立大型 random service 或可注入狀態系統。
- `LegacyFoundationAdapter` 的 `nextItem`、`chance`、`nextBoolean`、`range`、`ceiling`、`isSimilar`、`getMaxHealth` wrapper 已移除；隨機呼叫點改為 `PluginRandom`，數值 clamp / radius / item similarity 改用 Java / Bukkit 原生 API，player max health 改由 `PlayerUtils#getMaxHealth` 使用 Bukkit `Attribute.MAX_HEALTH`。
- `rg -n "LegacyFoundationAdapter\.(nextItem|chance|nextBoolean|range|ceiling|isSimilar|getMaxHealth)" src/main/java src/test/java` 無命中。
- 本切片不處理 command、menu、settings、metadata/reflection、sound 或 file/config helper。

後續 sound playback 小切片實作結果：

- `LegacyFoundationAdapter` 的 `playSound`、`playGlobalSound`、`playItemBreakSound` wrapper 已移除。
- 既有 `Extra.sound(...)` 保持為全專案播放入口，直接呼叫目前設定仍使用的 Foundation `SimpleSound#play(...)`；工具耐久破裂音改在 `PlayerUtils` 直接使用 Bukkit `Player#playSound(...)`。
- `rg -n "LegacyFoundationAdapter\.(playSound|playGlobalSound|playItemBreakSound)" src/main/java src/test/java` 無命中。
- 重要未完成項：本切片只移除 adapter sound wrapper，尚未移除 Foundation `SimpleSound` dependency。`Sounds.java`、`SoundConfigParser`、`YamlConfigLoader`、scenario sound 欄位與 `sounds.yml` migration 仍需後續獨立盤點與實作，不能在 Step 21 收尾時誤判為已完成。
- 本切片不新增 `PluginSounds` 或其他播放 service，不處理 `sounds.yml`、`SimpleSound` 設定型別、`SoundConfigParser` 或 Foundation config parser。

後續 version / old-server compatibility 小切片實作結果：

- 固定 Paper `1.21.11` 目標後，`Dependency`、`WorldUtils`、`CenterCleaner`、`ScenarioPotionLess`、`PlayingState` 不再透過 Foundation `MinecraftVersion` / `Remain.isPaper()` 判斷舊版分支。
- `PortalListener` 移除只服務舊版 travel agent 的 reflection fallback，不再呼叫 Foundation `getMethod` / `invoke`。
- `LegacyFoundationAdapter` 的 `isAtLeastMinecraft1_13`、`isAtLeastMinecraft1_14`、`isAtLeastMinecraft1_9`、`isAtLeastMinecraft1_11`、`isOlderThanMinecraft1_9`、`isOlderThanMinecraft1_14`、`getServerVersion`、`isPaperServer`、`getMethod`、`invoke` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\.(isAtLeastMinecraft1_13|isAtLeastMinecraft1_14|isAtLeastMinecraft1_9|isAtLeastMinecraft1_11|isOlderThanMinecraft1_9|isOlderThanMinecraft1_14|getServerVersion|isPaperServer|getMethod|invoke)" src/main/java src/test/java` 無命中。
- 本切片不新增 `PluginVersion` 或 platform detection wrapper；只刪除固定 1.21.11 目標下不再需要的舊版相容分支。

後續 legacy WorldBorder fallback 小切片實作結果：

- 舊外部 WorldBorder plugin 不再作為預生成 fallback；預生成正式部署依賴為 `Chunky`，缺少 Chunky 時會明確報錯，不再建議暫時啟用 legacy WorldBorder。
- 刪除 `LegacyWorldBorderPregenerationAdapter`、`LegacyWorldBorderFillListener` 與 `FeatureRegistry` 的 legacy listener 註冊；`ChunkPregenerationAdapters` 只選擇 `ChunkyPregenerationAdapter` 或 `MissingChunkPregenerationAdapter`。
- `Dependency.WORLD_BORDER`、`plugin.yml` 的 `WorldBorder` softdepend、`build.gradle` 的 WorldBorder `compileOnly` 與舊 WorldBorder 訊息 key 已移除。
- `rg -n "dispatchCommand\\(.*wb|WorldBorderFillFinishedEvent|com\\.wimbli\\.WorldBorder" src/main/java src/main/resources` 無命中。
- 本切片不改 Paper/Bukkit `WorldBorderPort`、`PaperWorldBorderAdapter` 或既有 Chunky 預生成主路徑。

後續 metadata / chunk force-loaded 小切片實作結果：

- `CombatRelog`、`CombatRelogListener`、`UHCPlayer`、`InvinciblePlayer`、`Tutorial` 與 `ScenarioTripleArrow` 不再透過 `LegacyFoundationAdapter` 存取 Foundation `CompMetadata` / `ChunkKeeper`。
- `UHCPlayer`、combat relog、tutorial 與 no-clean bypass 這類 runtime object state 改成 owner-managed map / set；`ScenarioTripleArrow` 的 projectile marker 改用 `PersistentDataContainer`；combat relog chunk keep alive 改用 Bukkit `Chunk#setForceLoaded(...)`。
- `LegacyFoundationAdapter` 的 `getTempMetadata`、`hasTempMetadata`、`setTempMetadata`、`removeTempMetadata` 與 `setChunkForceLoaded` wrapper 已移除。
- `rg -n "getTempMetadata|hasTempMetadata|setTempMetadata|removeTempMetadata|setChunkForceLoaded|CompMetadata|ChunkKeeper" src/main/java src/test/java` 無命中。
- 本切片不重寫 `UHCPlayer` lifecycle、combat relog model、tutorial 流程或 scenario 行為，也不新增新的 metadata service；沒有改用 Bukkit deprecated metadata API 當替代。

後續 permission / validation 小切片實作結果：

- `UHCPermission` 不再透過 Foundation `PlayerUtil.hasPerm` / `Valid.checkPermission`；權限判斷改用 Bukkit `Player#hasPermission(...)`，缺權限訊息改讀本地 `Messages.NO_PERMISSION`。
- `Dependency` 不再提供 Foundation 風格 `check()` / `checkSoft()`；目前唯一使用點 `/reconnect` 改在 command 內檢查 DiscordSRV 是否 hooked，並用既有 command `returnTell(...)` 回覆缺 dependency 訊息。
- `LegacyFoundationAdapter` 的 `hasPermission`、`checkPermission` 與 `checkBoolean` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.(hasPermission|checkPermission|checkBoolean)|org\\.mineacademy\\.fo\\.PlayerUtil|org\\.mineacademy\\.fo\\.Valid|Dependency\\.[A-Z_]+\\.check" src/main/java src/test/java` 無命中。
- 本切片不處理 `SimpleCommand` / command `returnTell(...)` 最終移除；這仍屬 command framework 切片。

後續 file extraction / data-file access 小切片實作結果：

- `PluginBootstrap#loadFiles()` 不再透過 Foundation `FileUtil.extract(...)`；改用 Bukkit `JavaPlugin#saveResource(path, false)` 複製內建設定檔。
- `OldMenusCheck` 與 `WorldLoadingCacheStore#delete()` 不再透過 Foundation `FileUtil#getFile` / `getOrMakeFile`；改用 `WonderlandUHC#getDataFolder()` 下的明確 `File` 路徑。
- `LegacyFoundationAdapter` 的 `extractFile`、`getFile` 與 `getOrMakeFile` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.(extractFile|getFile|getOrMakeFile)|org\\.mineacademy\\.fo\\.FileUtil" src/main/java src/test/java` 無命中。
- 本切片不建立通用 file service，也不處理 Foundation `YamlConfig` / settings lifecycle。

後續 broadcast failure 小切片實作結果：

- `DiscordBroadcastSender` 不再透過 Foundation `FoException` 表示可回覆給玩家的 Discord 發送失敗；改用主插件本地 `BroadcastDeliveryException`。
- `BroadcastSettingsMenu` 只捕捉 `BroadcastDeliveryException` 並把訊息回覆給玩家；其他 runtime exception 仍維持拋出，避免吞掉真正錯誤。
- `LegacyFoundationAdapter` 的 `failure` 與 `isFailure` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.(failure|isFailure)" src/main/java src/test/java` 無命中。
- 本切片不重寫 broadcast framework、DiscordSRV 整合或 menu lifecycle。

後續 reflection / location fallback 小切片實作結果：

- `UHCSpawn` 不再透過 `LegacyFoundationAdapter#getLocationOrDefault(...)`；設定讀取失敗時直接在使用點保留既有 fallback，回到主世界 spawn。
- `TestCommand` 的 `files` debug 分支不再反射 Foundation `YamlConfig.loadedFiles`，該反射入口已移除。
- `LegacyFoundationAdapter` 的 `getLocationOrDefault`、`getFieldContent` 與 `getStaticFieldContent` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.(getLocationOrDefault|getFieldContent|getStaticFieldContent)|org\\.mineacademy\\.fo\\.ReflectionUtil" src/main/java src/test/java` 無命中。
- 本切片不重寫 `YamlConfig` / settings lifecycle，也不新增 reflection helper。

後續 command dispatch 小切片實作結果：

- `PlayerChat` 的 team chat 轉發不再透過 Foundation `Common.dispatchCommandAsPlayer(...)`；改用既有 `PluginScheduler.runLater(0, ...)` 回主執行緒後呼叫 Bukkit `Player#performCommand(...)`。
- 保留舊 wrapper 的 `{player}` 替換語意；chat event 仍不在 async thread 直接執行 command。
- `LegacyFoundationAdapter` 的 `dispatchCommand` 與 `dispatchCommandAsPlayer` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.(dispatchCommand|dispatchCommandAsPlayer)" src/main/java src/test/java` 無命中。
- 本切片不新增 command helper，也不重寫 team command / command framework。

後續 tutorial boxed message 小切片實作結果：

- `TutorialSection` 不再透過 Foundation `BoxedMessage.tell(...)`；改在 tutorial section 本地輸出上下分隔線與訊息內容。
- `LegacyFoundationAdapter` 的 `tellBoxed` 與 `broadcastBoxed` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.(tellBoxed|broadcastBoxed)|org\\.mineacademy\\.fo\\.model\\.BoxedMessage" src/main/java src/test/java` 無命中。
- 本切片不新增 boxed message service，也不重寫 tutorial 流程。

後續 clickable run-command component 小切片實作結果：

- `InviteCommand`、`RegenWorldCommand` 與 `InventoryEditButton` 不再透過 Foundation `SimpleComponent` 送 clickable run-command 訊息；改用 Adventure `Component`、`ClickEvent.runCommand(...)` 與必要的 `HoverEvent.showText(...)`。
- `LegacyFoundationAdapter` 的 `sendRunCommandComponent` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.sendRunCommandComponent|org\\.mineacademy\\.fo\\.model\\.SimpleComponent" src/main/java src/test/java` 無命中。
- 本切片不新增共用 component helper，也不重寫 command / menu framework。
- 這些使用點仍透過 Adventure `LegacyComponentSerializer.legacyAmpersand()` 承接既有 `&` 色碼設定；這不是 Foundation 回流。未來若要改成 MiniMessage 或其他正式文字格式，需放到獨立文字格式 / 設定訊息切片，不在本小切片處理。

後續 time symbols 小切片實作結果：

- `Messages.Symbol#init()` 不再透過 `LegacyFoundationAdapter.configureTimeSymbols(...)` 寫 Foundation `TimeUtil` 全域欄位；改直接設定本地 `TimePlaceholderFormatter`。
- `LegacyFoundationAdapter` 的 `configureTimeSymbols` wrapper 已移除。
- `rg -n "LegacyFoundationAdapter\\.configureTimeSymbols|org\\.mineacademy\\.fo\\.TimeUtil|TimeUtil\\." src/main/java src/test/java` 無命中。
- `WonderlandUHC#onPluginStart()` 的 `setTellPrefix("")` 暫時保留，因為目前 command framework 仍使用 Foundation `SimpleCommand#tell(...)`；此項應隨 21.3 command framework 移除時處理，不在本切片硬刪。

切片限制：

- 不同功能族群不要混在同一刀。
- 不新增「萬用 FoundationReplacementService」。
- 若 helper 只包一行 Bukkit API，除非有多處共享語意，否則保持在使用點。

驗收：

- 每個功能族群結束後跑該族群的 `rg`。
- 每一批程式碼變更後封裝與 server startup smoke test。

### 21.3 plugin lifecycle / command framework 移除

目標：

- `WonderlandUHC` 不再 `extends SimplePlugin`，改成標準 Bukkit/Paper plugin lifecycle。
- 移除 `SimpleCommand`、`SimpleSubCommand`、`SimpleCommandGroup` 依賴。
- 保留既有 command label、permission、sender type、tab completion 與錯誤訊息語意。

建議拆法：

1. 先建立最小 command registration path，承接現有 `plugin.yml` command。
2. 先處理 command group 與 registration lifecycle，再分批遷移 command 實作。
3. 以命令族群切片，例如 public/game commands、host commands、`/uhc` subcommands、team/whitelist subcommands。
4. 每批命令只替換 framework，不順手改業務邏輯。

高風險點：

- `checkConsole()`、`getPlayer()`、`args`、`tell()`、`returnTell()` 這些 Foundation command helper 需要保留語意。
- tab completion 與 permission denial message 需對照舊行為。
- reload registration 不得重複註冊或漏註冊。

驗收：

- 主要 command 能在 Paper `1.21.11` 啟動後註冊。
- 至少 smoke test `/uhc`、host command、team command、info command、permission denied / console 路徑。
- 不把完整 command UX regression 驗收放進本切片；Step 21 仍要完成已知 command framework 替換，Step 22 只做最終對照檢查。

### 21.4 settings / YAML config lifecycle 移除

目標：

- 移除 `SimpleSettings`、`YamlStaticConfig`、`YamlConfig`、Foundation `YamlConfigLoader` 依賴。
- 保留現有 YAML 檔案結構與設定 key，不做資料格式重設計。
- reload 與 config cache 行為可被明確追蹤。

優先處理：

| 檔案 / 類型 | 預期方向 |
| --- | --- |
| `Settings` / `CommandSettings` / `Messages` / `Sounds` | 使用 Bukkit `YamlConfiguration` 或既有本地 loader 讀取，先維持 public static 欄位語意。 |
| `ButtonLocalization` | 若 menu 尚未移除 Foundation，先避免單獨拆；應與 menu 切片協調。 |
| `UHCSpawn` / `Spawns` | 保留 spawn YAML 路徑與 reload 行為。 |
| `WorldLoadingCacheStore` / `SavedGameSettingsStore` | 移除 `clearLoadedSections()` 與 Foundation file access。 |
| `ScenarioConfig` / `StatsStorageYaml` / death message loader | 保留現有資料形狀，不藉此改 stats 或 scenario model。 |

切片限制：

- 不把 YAML 轉資料庫。
- 不改 config key 命名與檔案格式，除非 Step 21 移除 dependency 必要。
- 不為了 generic warning 改動保存資料模型。

驗收：

- 既有 config 檔案可讀。
- host settings、scenario settings、saved game、stats、spawn、death message 至少完成啟動與基本讀取 smoke。
- reload path 不因移除 Foundation 而 exception。

### 21.5 menu framework 移除

目標：

- 移除 Foundation `Menu`、`ConfigMenu`、`ConfigMenuPagged`、button、inventory drawer、`ItemCreator`、`ColorMenu` 依賴。
- 保留 Step 11 / Step 12 / Step 18 已實測可用的 GUI 行為。
- 不為了架構美觀重寫已可運作的 thin menu；只替換 Foundation framework 必要行為。

建議分批：

1. 建立最小本地 menu base，僅承接目前需要的 open、click、back、pagination、item render、sound。
2. 先處理 leaf / simple menu，再處理 host settings 主流程。
3. 單獨處理 inventory editor 與 `finish` / `tohead` 輸入，避免和一般 menu click 混在一起。
4. 最後處理 color picker 與 team color model。

高風險點：

- player head / skull owner。
- back button / page button。
- host settings persistence。
- inventory editor command input。
- `ButtonLocalization` / `gui.yml` material alias。
- `TeamSettingsMenu` 與 `ScoreboardSettingsMenu` 的 color handling。

驗收：

- Paper `1.21.11` 啟動後無 menu exception。
- `/uhc edit`、main host settings、scenario menu、team menu、player menu、stats menu、staff menu、inventory editor 各自至少進入一次。
- Step 21 仍要完成已知 menu framework 替換；完整 GUI 舊新版對照留 Step 22 檢查。

### 21.6 Step 19 延後項：login gate、team color、metadata state

目標：

- 移除 `PlayerLoginEvent` deprecated path 或明確改成正式分段登入模型。
- 移除 `org.bukkit.ChatColor` 作為 team domain / public API 的主要模型。
- 移除 Foundation metadata helper 與 `MetadataValue` 相依。

登入 gate 預期決策：

- 不新增 wrapper 只隱藏 `PlayerLoginEvent`。
- 需明確決定哪些檢查在 pre-login 可做，哪些需要完整 `Player` 後移到 join 後。
- 白名單、等待 host、滿員、遊戲中加入、bypass 權限、`UHCPlayer` 建立時機都要在 Step 21 完成登入模型決策與修改，並列入 Step 22 對照 checklist 做檢查。

team color 預期決策：

- 選定正式 team color model，例如 Adventure `NamedTextColor`、Bukkit `DyeColor` / `Team.Option` 可用模型，或本地 enum。
- public API 若仍保留相容 method，必須標明 deprecated/bridge 語意，不讓 Foundation `ColorMenu` 牽住主模型。
- scoreboard team color、menu color picker、team display name 要一起驗證。

metadata state 預期決策：

- `UHCPlayer` 與 combat relog 這類 runtime state 優先改成 owner-managed map / object lifecycle。
- tutorial 或 entity metadata 若需要跨 tick / entity store，優先使用 Bukkit `PersistentDataContainer` 或明確清理的本地狀態。
- 不為了消 warning 加 suppress。

驗收：

- `rg -n "PlayerLoginEvent|org\\.bukkit\\.ChatColor|MetadataValue|clearLoadedSections" Update-WonderlandUHC/src/main/java`
- 登入 gate 基本 smoke test。
- team create/edit/display 基本 smoke test。
- combat relog / death path 不因 metadata 移除直接 exception。

### 21.7 最終 dependency 拔除與搜尋 gate

目標：

- 從 `build.gradle` 移除 Foundation / DatouNMS dependency 與 Foundation relocate。
- 刪除 `legacy/LegacyFoundationAdapter.java`、`legacy/LegacyDatouNmsAdapter.java` 與只為它們存在的 wrapper。
- 主插件可不依賴 `lib-foundation` repo 完成 clean build、shadow jar 與 Paper `1.21.11` startup。
- 更新 A15 / A16 對 Foundation / DatouNMS 的最終狀態描述。

最終搜尋 gate：

```bash
rg -n "org\\.mineacademy\\.fo|LegacyFoundationAdapter|lib-foundation" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle
rg -n "DaTouNMS|datounms|NewerSpigotAPI|LegacyDatouNmsAdapter|net\\.minecraft|org\\.bukkit\\.craftbukkit" Update-WonderlandUHC/src/main/java Update-WonderlandUHC/build.gradle
rg -n "PlayerLoginEvent|org\\.bukkit\\.ChatColor|MetadataValue|clearLoadedSections" Update-WonderlandUHC/src/main/java
```

完成條件：

- 上述搜尋 gate 為 0，除非 `docs/steps.md` 明確允許的 Paper API adapter 例外已記錄。
- `build.gradle` 不再宣告 Foundation / DatouNMS。
- Shadow jar 不再 relocate `org.mineacademy`。
- 封裝通過。
- Paper `1.21.11` server startup smoke test 通過。
- Step 22 最終對照 checklist 已補上 Step 21 完成或明確接受的所有行為差異；不能把已知待修改項留到 Step 22 才處理。

## 每個程式碼切片的固定驗證

每個 Step 21 程式碼切片完成後都必須做：

1. 使用 `scripts/` 內腳本封裝，不使用裸 Gradle 指令。
2. 啟動伺服器時使用伺服器資料夾內的 `start.bat`。
3. 檢查 latest log，至少確認 WonderlandUHC enable path 沒有因本切片新增 exception。
4. 回報本切片 done / not-done，不把下一個切片的尚未完成項目包裝成已完成。

目前升級線封裝入口：

```bash
bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean
```

若切片已移除 Foundation dependency，`--skip-foundation` 只代表不嘗試建置本機 `lib-foundation`，不應再成為 runtime 依賴條件。

## 過度抽象化停止條件

遇到以下情況要停止實作並先討論：

- 為了替換一個 Foundation helper，開始建立通用大型 service，但目前只有一兩個使用點。
- 已可運作的 thin command / menu 被重寫成多層架構，而目的只是「看起來更乾淨」。
- 為了消除 IDE warning 或 deprecated warning，改動資料模型、YAML 格式、public API 或玩家可見行為。
- 同一刀同時碰 command、menu、settings、login gate 與 scenario gameplay。
- 拔 dependency 前無法說清楚該切片在 Step 21 已完成或接受的 runtime 行為差異，以及 Step 22 只需檢查的項目。

## 建議 commit 分組

Step 21 完成後若需要整理 history，建議壓成少數邏輯 commit，而不是每個小修一個永久 commit：

1. `docs(step-21): plan legacy removal slices`
2. `refactor(step-21): remove legacy NMS dependency`
3. `refactor(step-21): replace Foundation utility usage`
4. `refactor(step-21): migrate commands and plugin lifecycle`
5. `refactor(step-21): migrate settings and menus`
6. `refactor(step-21): resolve login color and metadata legacy state`
7. `build(step-21): remove Foundation and DatouNMS dependencies`

實際 commit 數量可依最後 diff 調整，但每個 commit 都要能說明行為風險與驗證結果。
