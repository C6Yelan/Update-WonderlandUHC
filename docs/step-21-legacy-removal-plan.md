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
- 既有 `Extra.sound(...)` 在本切片先保持為全專案播放入口；當時仍沿用 Foundation `SimpleSound#play(...)`，後續 21.4 sound config / playback 切片已改為本地 `PluginSound`。工具耐久破裂音改在 `PlayerUtils` 直接使用 Bukkit `Player#playSound(...)`。
- `rg -n "LegacyFoundationAdapter\.(playSound|playGlobalSound|playItemBreakSound)" src/main/java src/test/java` 無命中。
- 後續完成狀態：本切片當時只移除 adapter sound wrapper，尚未移除 Foundation `SimpleSound` dependency；該 dependency 已於 21.4 sound config / playback 切片移除，只剩 21.5 menu click sound 簽名使用點。
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
- `Dependency` 不再提供 Foundation 風格 `check()` / `checkSoft()`；`/reconnect` 改在 command 內檢查 DiscordSRV 是否 hooked，並用本地 `Chat.send(...)` 回覆缺 dependency 訊息。
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
- `WonderlandUHC#onPluginStart()` 的 `setTellPrefix("")` 已隨 21.3 command framework 收尾移除；command 訊息改由各 native command 直接使用既有 `Chat` / `Messages` / `CommandSettings` 輸出。

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

#### 21.3 只讀盤點：command framework 使用點

更新日期：2026-05-16

盤點分支：`step-21-legacy-removal`

本段最初只整理文件，未修改程式碼；後續已逐批完成 production single command 遷移。最新掃描指令：

```bash
rg -n "SimpleCommand|SimpleSubCommand|SimpleCommandGroup|registerCommand|setTellPrefix|configureMenuClickSound" src/main/java/org/mcwonderland/uhc docs/step-21-legacy-removal-plan.md docs/steps.md
rg -n "getPlayer\\(|getSender\\(|returnTell\\(|tell\\(|findPlayer\\(|checkBoolean\\(|checkConsole\\(|joinArgs\\(|rangeArgs\\(|tabComplete\\(|setUsage\\(|setDescription\\(|setPermission\\(|getLabel\\(" src/main/java/org/mcwonderland/uhc/command
```

目前 production single command 與 group command framework 都已離開 Foundation dynamic registration；`/uhc` 是最後一組已完成遷移的 group command。本段後續只保留 command slice 驗收與下一步切換，不再把 `/uhc` 列為待處理。`WonderlandUHC extends SimplePlugin` 與 `onPluginStart()` / `onReloadablesStart()` lifecycle 仍存在，需接續 settings / reload lifecycle 盤點後再處理，不能把本刀視為整個 21.3 lifecycle 完成。

| 類型 | 目前數量 / 使用點 | 風險 |
| --- | ---: | --- |
| `SimpleCommand` import | 0 個檔案 | production single command 與舊測試 / 空指令已清出。 |
| `SimpleSubCommand` import | 0 個檔案 | `/uhc`、`/team`、`/whitelist|/wl` 都已改走本地 native dispatch。 |
| `SimpleCommandGroup` import | 0 個檔案 | Foundation group command registration path 已清空。 |
| Foundation dynamic registration | 0 個入口 | `FeatureRegistry#registerCommands(...)` 與 `FeatureRegistry#registerCommandGroups(...)` 已移除。 |
| `plugin.yml` static `commands:` | 主要正式指令都以 Bukkit/Paper native command entry 註冊 | command framework 不再需要 Foundation dynamic command registration；後續只剩 menu / settings / sound 等非 command 依賴。 |

目前註冊入口：

- `FeatureRegistry#registerCommands(...)` 已移除；production single command 與舊 `TEST_MODE` 測試指令都不再走 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前註冊 Bukkit native `/backpack|/bp`、`/border`、`/config|/cfg`、`/disableitems`、`/finish`、`/giveall`、`/practice`、`/reconnect`、`/respawn`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/spectoggle`、`/staff`、`/stats`、`/team`、`/tohead`、`/topkills|/killtop|/kt`、`/uhc`、`/viewheal|/h` 與 `/whitelist|/wl`。
- `FeatureRegistry#registerCommandGroups(...)` 已移除，不再註冊 Foundation group commands。
- `WonderlandUHC#onPluginStart()` 只註冊 native commands；不再透過 `LegacyFoundationAdapter.commandGroupRegistrar(...)` 註冊 group commands。
- `WonderlandUHC#onPluginReload()` 不再重新註冊 command groups，避免 reload 時重複註冊 Bukkit command executor。

目前 `LegacyFoundationAdapter` 中和 command / lifecycle 相關的殘留：

| 方法 | 目前作用 | 本切片判斷 |
| --- | --- | --- |
| `setTellPrefix("")` | 已移除。 | command helper 已離開 Foundation，不再需要全域 Foundation tell prefix。 |
| `commandGroupRegistrar(...)` | 已移除。 | group command 已改走 Bukkit/Paper native registration。 |
| `configureMenuClickSound()` | 設定 Foundation `Menu` click sound。 | 屬於 21.5 menu framework，不應混進 command 切片。 |

command helper 收斂方式：

| helper / lifecycle | 收斂方式 | 注意 |
| --- | --- | --- |
| sender helper | 各 native command 直接使用 Bukkit `CommandSender` / `Player`；`/uhc` 與 `/team` 只保留 package-local 薄 helper。 | 不抽成全插件共用 command framework。 |
| args helper | 缺參數、boolean、player name completion 在各 command 或 package-local helper 內處理。 | 不重寫 world / timer / menu 業務邏輯。 |
| validation / message | 直接使用 `Chat`、`Messages`、`CommandSettings` 與既有文字。 | 只替換 Foundation helper，不順手改訊息模板。 |
| metadata | 由 `plugin.yml` command entry、native executor 與本地 help 輸出承接。 | 指令 label / alias 需由實測與 Step 22 對照確認。 |
| tab completion | 由 Bukkit `TabCompleter` 或 command 自身 `tabComplete(...)` 承接。 | 不追求一次補完所有 UX 差異。 |
| group dispatch | `/uhc` 使用 `UHCCommand` + `UHCSubCommand`，`/team` 使用 `TeamCommand` + `TeamSubCommand`。 | 這些 helper 只服務各自 package，不升級成新框架。 |

下一刀建議：

1. 21.3 command framework 已完成最小替換，`SimpleCommand` / `SimpleSubCommand` / `SimpleCommandGroup` 與 dynamic registration path 已清空。
2. 下一步不要再擴大 command 重構；優先只讀盤點 21.4 settings / YAML config lifecycle，因為 `WonderlandUHC extends SimplePlugin`、`onReloadablesStart()`、reload 與 Foundation settings lifecycle 仍綁在一起。
3. `configureMenuClickSound()` 明確留到 21.5 menu framework，不在 command framework 收尾時順手處理。
4. Step 22 再做完整 command UX 對照；若發現 regression，回到對應 command 檔案修正，不在本刀預先過度防禦。

本輪 fallback leave 移除結果：

- 刪除 `command/impl/LeaveCommand.java`，不再提供 `/leave` 指令。
- 移除 `FeatureRegistry#registerCommands(...)` 中的 `/leave` 註冊，且不保留 `registerNativeCommands()` 過渡方法。
- 刪除 lobby / spectator hotbar 的 `LeaveItem`，避免道具點擊後觸發不存在的指令。
- `MatchStopService` 結束比賽時只刪 cache 並關閉伺服器，不再把玩家送往 fallback server。
- 移除 `Extra#sendToFallbackServer(...)`、`Settings.BUNGEE_LOBBY`、`settings.yml` 的 `Bungee_Lobby`、`plugin.yml` / `permissions.txt` 的 `wonderland.uhc.command.leave`。
- 移除只服務 BungeeCord outgoing channel 的 `PluginMessagingPort` / `PaperPluginMessagingPort` 與 `PluginBootstrap#registerPluginChannels()`。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、menu、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/setspawn` 第一個 native command 實作結果：

- `command/impl/host/SetSpawnCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/setspawn` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 新增直接註冊 `/setspawn` 的 native command path，沒有建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/setspawn` command entry，讓 Bukkit/Paper 提供 native command registration。
- console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家權限仍走 `UHCPermission.COMMAND_SET_SPAWN`；成功訊息改用 `Chat.send(player, CommandSettings.SetSpawn.SPAWN_SAVED)`，音效仍使用 `Sounds.Commands.SET_SPAWN`。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、menu、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/config|/cfg` native command 實作結果：

- `command/impl/info/ConfigCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/config|/cfg` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 加入 `/config|/cfg` native command registration，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/config` command entry 與 `cfg` alias。
- 保留 console 可執行的舊語意；權限檢查改用 `CommandSender#hasPermission(...)`，拒絕訊息沿用 `Messages.NO_PERMISSION`，輸出改用 `Chat.send(sender, GamePlaceholderReplacer.replace(...))`。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、menu、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/disableitems` native command 實作結果：

- `command/impl/info/DisableItemsCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/disableitems` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems` 與 `/setspawn`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/disableitems` command entry。
- 保留 player-only 舊語意；console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家權限仍走 `UHCPermission.COMMAND_DISABLEITEMS`；成功時仍直接開啟 `DisableItemListMenu`。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、其他 menu command、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/scenarios` native command 實作結果：

- `command/impl/info/ScenariosCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/scenarios` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/scenarios` 與 `/setspawn`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/scenarios` command entry。
- 保留 player-only 舊語意；console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家權限仍走 `UHCPermission.COMMAND_SCENARIOS`；成功時仍直接開啟 `EnabledScenariosMenu`。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、其他 menu command、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/finish` / `/tohead` native command 實作結果：

- `command/impl/host/InventoryEditorInputCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/finish`、`/tohead` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/scenarios`、`/setspawn` 與 `/tohead`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/finish` 與 `/tohead` command entry。
- 保留 player-only 舊語意；console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家不在 conversation 時仍顯示 `&c目前沒有正在等待的設定輸入。`；成功時仍送入固定 input `finish` 或 `tohead`，其中 `tohead` 會由 `InventoryEditButton` 將玩家主手普通金蘋果轉換為金頭顱。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、conversation 流程本身、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/staff` native command 實作結果：

- `command/impl/host/StaffCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/staff` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/scenarios`、`/setspawn`、`/staff` 與 `/tohead`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/staff` command entry。
- 保留 player-only 舊語意；console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家權限仍走 `UHCPermission.COMMAND_STAFF`；角色切換邏輯維持原本 STAFF / waiting player / non-waiting spectator 流程。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、staff role implementation、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/reconnect` native command 實作結果：

- `command/impl/game/ReconnectCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/reconnect` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/reconnect`、`/scenarios`、`/setspawn`、`/staff` 與 `/tohead`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/reconnect` command entry。
- 保留 player-only 舊語意；console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家權限仍走 `UHCPermission.COMMAND_RECONNECT`；DiscordSRV 未啟用與語音功能關閉分支由明確 early return 承接原本 `returnTell(...)` / `checkBoolean(...)` 行為。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、Discord voice hook implementation、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/topkills|/killtop|/kt` native command 實作結果：

- `command/impl/info/TopKillsCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/topkills|/killtop|/kt` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/reconnect`、`/scenarios`、`/setspawn`、`/staff`、`/tohead` 與 `/topkills|/killtop|/kt`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/topkills` command entry 與 `killtop`、`kt` alias。
- 保留 player-only 舊語意；console 執行使用 `commands.yml` 的 `No_Console` 訊息；玩家權限仍走 `UHCPermission.COMMAND_TOPKILLS`；遊戲未開始分支改用本地 `GameUtils.isGameStarted()` + `Messages.NOT_YET_STARTED`，排行榜排序、訊息模板與音效維持原本流程。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、stats model、scoreboard 或 Foundation `setTellPrefix("")`。

本輪 `/viewheal|/h` native command 實作結果：

- `command/impl/info/ViewHealCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/viewheal|/h` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/reconnect`、`/scenarios`、`/setspawn`、`/staff`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/viewheal` command entry、`h` alias 與基本 usage。
- 保留 console 可查指定玩家血量的舊語意；權限改用 `CommandSender#hasPermission(...)`；玩家查找改用既有 `PluginPlayers.getByName(..., true)` 承接線上玩家與 vanished 過濾；找不到玩家訊息保留 Foundation 原本預設文字，未新增共用 command helper。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、stats model、health format 或 Foundation `setTellPrefix("")`。

本輪 `/mlg` 移除結果：

- 確認 `/mlg` 只是主持人把指定玩家傳送到自己位置並播放 MLG 音效的舊活動 / 測試指令，現行比賽流程不需要保留。
- 刪除 `command/impl/host/MLGCommand.java`，不再提供 `/mlg` 指令。
- `FeatureRegistry#registerCommands(...)` 與 `FeatureRegistry#registerNativeCommands()` 都不再註冊 `/mlg`。
- 移除 `plugin.yml` 的 `/mlg` command entry、`wonderland.uhc.command.mlg` host child permission、`permissions.txt` 權限說明、`UHCPermission.COMMAND_MLG`、`Sounds.Commands.MLG` 與 `sounds.yml` 的 `Commands.Mlg` 設定。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、其他 host command 或 Foundation `setTellPrefix("")`。

本輪 `/sendcoords|/scs` native command 實作結果：

- `command/impl/game/SendCoordsCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/sendcoords|/scs` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/reconnect`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/staff`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/sendcoords` command entry 與 `scs` alias。
- 保留 player-only、public permission、遊戲開始檢查、遊戲玩家檢查、隊伍座標訊息與隊伍音效流程；`CommandHelper` exception flow 改為本地 `GameUtils` + `Messages` early return，未新增共用 command helper。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、team model、座標格式或 Foundation `setTellPrefix("")`。

本輪 `/spectoggle` native command 實作結果：

- `command/impl/game/SpecToggleCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/spectoggle` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/config|/cfg`、`/disableitems`、`/finish`、`/reconnect`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/spectoggle`、`/staff`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/spectoggle` command entry。
- 保留 player-only、public permission、遊戲開始檢查、spectator-only 檢查、`SPECTATE_MODE == DEFAULT` 限制、creative/spectator gamemode toggle、訊息與音效流程；`getLabel()` 改用 Bukkit 傳入的 `label`，未新增共用 command helper。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、spectator mode implementation、hotbar item 或 Foundation `setTellPrefix("")`。

本輪 `/border` native command 實作結果：

- `command/impl/host/BorderCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/border` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/border`、`/config|/cfg`、`/disableitems`、`/finish`、`/reconnect`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/spectoggle`、`/staff`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/border` command entry 與基本 usage。
- 保留 player-only、host permission、遊戲開始檢查、`1..initialBorder` 數字範圍檢查與 `BorderShrinkRequestService#requestShrink(size)` 流程；Foundation `findNumber(...)` 改為本指令內的最小整數 parse，未新增共用 parser。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、border service、border mode implementation 或 Foundation `setTellPrefix("")`。

本輪 `/giveall` native command 實作結果：

- `command/impl/host/GiveAllCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor` + `TabCompleter`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/giveall` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/border`、`/config|/cfg`、`/disableitems`、`/finish`、`/giveall`、`/reconnect`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/spectoggle`、`/staff`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/giveall` command entry 與 usage。
- 保留 console 可執行、moderator permission、遊戲開始檢查、物品名稱與數量驗證、發給所有 `RoleName.PLAYER`，以及離線 combat relog 玩家背包補發流程；Foundation `findMaterial(...)` / `findNumber(...)` / `completeLastWord(...)` 改為本指令內的 Bukkit `Material` parse 與最小 tab completion，未新增共用 parser。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、其他 tab completion、CombatRelog inventory 流程或 Foundation `setTellPrefix("")`。

本輪 `/stats` native command 實作結果：

- `command/impl/info/StatsCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/stats` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/border`、`/config|/cfg`、`/disableitems`、`/finish`、`/giveall`、`/reconnect`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/spectoggle`、`/staff`、`/stats`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/stats` command entry 與 usage。
- 保留 player-only、public permission、`/stats` 查自己、`/stats <玩家>` 查線上非 vanished 玩家，以及直接開啟 `StatsMenu` 的流程；Foundation `findPlayer(...)` 改用既有 `PluginPlayers.getByName(..., true)`，找不到玩家訊息沿用 Foundation 預設文字，未新增共用 command helper。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、tab completion、StatsMenu、stats storage、GUI 設定或 Foundation `setTellPrefix("")`。

本輪 `/practice` native command 實作結果：

- `command/impl/game/PracticeCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/practice` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 目前直接註冊 `/border`、`/config|/cfg`、`/disableitems`、`/finish`、`/giveall`、`/practice`、`/reconnect`、`/scenarios`、`/sendcoords|/scs`、`/setspawn`、`/spectoggle`、`/staff`、`/stats`、`/tohead`、`/topkills|/killtop|/kt` 與 `/viewheal|/h`，仍未建立完整 command base 或 subcommand framework。
- `plugin.yml` 補上 `/practice` command entry 與 usage。
- 依委託人決策，舊版原本存在的 `/practice <玩家>` 不保留；新版只保留玩家執行 `/practice` 切換自己加入 / 退出練習模式。
- 移除未使用的 `wonderland.uhc.host.practiceother` host child permission；保留 public `wonderland.uhc.command.practice` 權限。
- 本刀未處理 `/uhc`、`/team`、`/whitelist`、practice world、practice inventory、死亡補裝、破壞方塊取消、SimplePractice 內的 `CompMaterial` 或 Foundation `setTellPrefix("")`。

本輪 `/backpack|/bp` native command 實作結果：

- `command/impl/game/BackPackCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/backpack|/bp` 交給 Foundation dynamic command registration。
- `FeatureRegistry#registerNativeCommands()` 直接註冊 `/backpack|/bp`；`plugin.yml` 補上 `/backpack` command entry 與 `bp` alias。
- 保留舊行為：主指令不額外檢查 `wonderland.uhc.command.backpack`；只有 `/backpack <玩家>` 檢查 `wonderland.uhc.host.seebackpack`；BackPack scenario 未啟用時仍 silent return；超過一個參數時仍走開自己背包的舊分支。
- 本刀未處理 `ScenarioBackPack`、`UHCTeam` 背包資料、死亡掉落、隊伍資料結構、permission 文件整理或 Foundation `setTellPrefix("")`。

本輪 `/respawn` native command 實作結果：

- `command/impl/host/RespawnCommand.java` 不再 extends Foundation `SimpleCommand`，改成 Bukkit `CommandExecutor` + `TabCompleter`。
- `FeatureRegistry#registerCommands(...)` 不再把 `/respawn` 交給 Foundation dynamic command registration；目前 production single command 已不再依賴這個 registration path。
- `FeatureRegistry#registerNativeCommands()` 直接註冊 `/respawn`；`plugin.yml` 補上 `/respawn` command entry 與 usage。
- 保留 host permission、player-only、遊戲開始檢查、目標不可為現役玩家、玩家名稱補全、死亡資料還原、傳送、無敵時間、音效、廣播與 `UHCPlayerRespawnedEvent` 流程；只把 Foundation sender/helper 替換為 Bukkit executor 的明確 sender。
- 本刀未處理 `DeathPlayer`、`RespawnHandler` 抽離、role flow、scenario respawn event 行為、重生傳送規則或 Foundation `setTellPrefix("")`。

本輪測試 / 空指令清理結果：

- 刪除 `command/TestCommand.java`，不再保留舊 `TEST_MODE` debug 指令。
- 刪除 `command/impl/EmptyCommand.java`，移除未發現正式註冊點的舊 Foundation placeholder。
- 移除 `FeatureRegistry#registerCommands(...)`；`WonderlandUHC#onPluginStart()` 不再呼叫 `featureRegistry.registerCommands(this::registerCommand)`。
- 目前 single command dynamic registration path 已收掉；正式 single commands 全部走 `plugin.yml` + Bukkit native command executor。
- 本刀未移除 `WonderlandUHC.TEST_MODE` 本身，因為它仍控制 `PluginBootstrap#applyTestModeSettings()` 的時間設定；是否移除 TEST_MODE 屬於後續 bootstrap / settings 清理，不混入 command framework 收尾。

本輪 `/whitelist|/wl` native command 實作結果：

- 刪除 `command/impl/host/whitelist/*CommandGroup` / `*SubCommand` 與 `add`、`remove`、`list`、`clear` 四個 Foundation subcommand 檔案，改由 `WhitelistCommand` 以 Bukkit `CommandExecutor` + `TabCompleter` 處理。
- `FeatureRegistry#registerCommandGroups(...)` 不再註冊 `WhitelistCommandGroup("whitelist|wl")`；`FeatureRegistry#registerNativeCommands()` 直接註冊 `/whitelist|/wl`。
- `plugin.yml` 補上 `/whitelist` command entry 與 `wl` alias。
- 保留共用權限 `wonderland.uhc.command.whitelist`、`Game.getGame().getWhiteList()` / `PlayerCollection` 底層資料、`add/remove/list/clear` 語意、`CommandSettings.Whitelist.*` 訊息與 `Chat.broadcastWithPerm(...)` 廣播方式。
- `/whitelist clear` 改用 `sender.getName()` 取代舊 `getPlayer().getName()`，避免 console 執行時取不到玩家；其餘流程不順手改白名單資料結構或登入檢查。
- 本刀未處理 `/uhc`、`/team`、`PlayerCollection` 的 Foundation collection 依賴、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/team` native command 實作結果：

- 刪除 `TeamCommandGroup` 對 Foundation `SimpleCommandGroup` 的繼承，改由 `TeamCommand` 以 Bukkit `CommandExecutor` + `TabCompleter` 註冊 `/team`。
- 保留原本 11 個子指令檔案：`chat`、`create`、`disband`、`invite`、`join`、`leave`、`kick`、`list`、`promote`、`public`、`settings`，沒有把隊伍業務邏輯合併成大型 switch。
- `TeamSubCommand` / `TeamOwnerCommand` 改成只服務 `/team` package 的本地薄 helper，承接 player-only、permission、usage、`tell(...)`、`returnTell(...)`、`checkBoolean(...)`、玩家查找、隊伍檢查與 owner 檢查；不抽成全插件共用 command framework。
- `FeatureRegistry#registerCommandGroups(...)` 不再註冊 `TeamCommandGroup("team")`；`FeatureRegistry#registerNativeCommands()` 直接註冊 `/team`。
- `plugin.yml` 補上 `/team` command entry；tab completion 先補第一層子指令與玩家目標子指令的第二層玩家名稱。
- 原本 `/team promote` 舊檔案讀取 `args[0]` 但未宣告最小參數；本刀只補上 `<玩家>` usage 與最小參數檢查，避免缺參數時噴錯，不額外改 promote 邏輯。
- 已用 `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 封裝通過，並部署到 Paper `1.21.11` 測試服啟動；console smoke test `/team`、`/team ?` 與 `/team create` player-only 回覆正常，latest.log 無啟動 / 指令錯誤命中。
- 本刀未處理 `/uhc` group、`CommandHelper` 在 `/uhc` 的 Foundation exception 依賴、menu framework、settings lifecycle 或 Foundation `setTellPrefix("")`。

本輪 `/uhc` native command 實作結果：

- 刪除 `UHCMainCommandGroup`、`UHCCommandGroup` 與 `CommandHelper`，不再保留 Foundation group command bridge。
- 新增 `command/uhc/UHCCommand.java` 與 `command/uhc/UHCSubCommand.java`，只服務 `/uhc` package 的本地 dispatch/helper；不抽成全插件共用 command framework。
- 保留 `reload|rl`、`choose`、`edit`、`regen`、`resetteam`、`sethost`、`splitteam`、`switchteam`、`stop`、`tp`、`start`、`tutorial` 子指令。
- 保留內部指令 label：SettingsBook 仍執行 `uhc edit`，`RegenWorldCommand` 仍輸出 `/uhc regen confirm` 與 `/uhc regen skip`。
- `FeatureRegistry#registerCommandGroups(...)`、`LegacyFoundationAdapter.commandGroupRegistrar(...)` 與 `LegacyFoundationAdapter.setTellPrefix(...)` 已移除；`WonderlandUHC#onPluginStart()` 不再註冊 Foundation command groups。
- `plugin.yml` 補上 `/uhc` command entry；`FeatureRegistry#registerNativeCommands()` 直接註冊 `/uhc`。
- 本刀只替換 command framework，不修改 world selection、regeneration、start/stop、team split、host menu 或 timer 業務流程。
- 已用 `scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 封裝通過，並部署到 Paper `1.21.11` 測試服啟動；console smoke test `/uhc`、`/uhc ?` 與 `/uhc edit` player-only 回覆正常，latest.log 無啟動 / 指令錯誤命中。

建議拆法：

1. production single command、舊測試 / 空指令、`/whitelist|/wl`、`/team` 與 `/uhc` 已完成 native 化或移除。
2. command framework 切片到此收尾；下一步應先只讀盤點 21.4 settings / YAML config lifecycle 與 `SimplePlugin` lifecycle 解除順序。
3. 21.5 menu framework 仍留到後面；不要在 command 收尾時順手處理 `configureMenuClickSound()` 或 menu click 行為。
4. 後續不要為了「順手整理」重寫 world regeneration、start/stop、host menu、team split 或 timer lifecycle。
5. 目前先接受各 command 的小量註冊樣板重複；player-only、permission、args、tab completion、訊息輸出不抽成新的全域 command framework。

高風險點：

- `checkConsole()`、`getPlayer()`、`args`、`tell()`、`returnTell()` 這些 Foundation command helper 需要保留語意。
- tab completion 與 permission denial message 需對照舊行為。
- reload 不再負責 command group re-registration；若後續修改 reload flow，需避免重複註冊 native executor。

驗收：

- 主要 command 能在 Paper `1.21.11` 啟動後註冊。
- 至少 smoke test `/uhc`、host command、team command、info command、permission denied / console 路徑。
- 不把完整 command UX regression 驗收放進本切片；Step 21 仍要完成已知 command framework 替換，Step 22 只做最終對照檢查。

### 21.4 settings / YAML config lifecycle 移除

目標：

- 移除 `SimpleSettings`、`YamlStaticConfig`、`YamlConfig`、Foundation `YamlConfigLoader` 依賴。
- 保留現有 YAML 檔案結構與設定 key，不做資料格式重設計。
- reload 與 config cache 行為可被明確追蹤。

#### 21.4 只讀盤點：settings / YAML / lifecycle 使用點

更新日期：2026-05-17

盤點分支：`step-21-legacy-removal`

最新掃描指令：

```bash
rg -n "SimplePlugin|SimpleSettings|YamlStaticConfig|YamlConfig|YamlConfigLoader|SimpleLocalization|SimpleSound|ConfigSerializable|SerializedMap" src/main/java/org/mcwonderland/uhc
rg -n "class .* extends (AutoLoadStaticConfig|SimpleSettings|YamlStaticConfig|YamlConfig)" src/main/java/org/mcwonderland/uhc
rg -n "implements ConfigSerializable|SerializedMap" src/main/java/org/mcwonderland/uhc/game src/main/java/org/mcwonderland/uhc/model src/main/java/org/mcwonderland/uhc/stats src/main/java/org/mcwonderland/uhc/util
```

21.4 收尾後狀態：

| 類型 | 目前使用點 | 判斷 |
| --- | --- | --- |
| Foundation source import | `rg -n "org\\.mineacademy\\.fo" src/main/java/org/mcwonderland/uhc` 仍有 `129` 行 | command、settings、非 menu YAML readers、sound config 與 serialization model 已清空；剩餘集中在 `SimplePlugin` lifecycle、21.5 menu framework / tool / `ItemCreator` 類使用點，尚不可移除 dependency。 |
| plugin lifecycle | `WonderlandUHC extends SimplePlugin`，`getInstance()` 仍讀 `SimplePlugin.getInstance()` | 不再視為 21.4 settings / YAML 未完成項；需等 21.5 menu framework 解除後再處理正式 Bukkit/Paper lifecycle，避免先拔 lifecycle 造成 menu 行為回歸。 |
| static config lifecycle | `Settings`、`Messages`、`CommandSettings`、`Sounds` 已改用本地 `PluginStaticConfig` 明確載入。 | 21.4 已完成；保留 public static 欄位語意、現有 YAML key 與既有後處理。 |
| reflection loader | 舊 `util/YamlConfigLoader` 已移除。 | 21.4 已完成；未新增 schema registry、annotation framework 或通用 ORM。 |
| command localization bridge | `CommandSettings` 已不再寫入 Foundation `SimpleLocalization.Commands.*`。 | command 已 native 化，主插件 command 訊息直接使用 `CommandSettings`；Foundation menu / conversation localization 不在本橋接範圍。 |
| YAML object stores | `DeathMessageLoader`、`ScenarioConfig`、`StatsStorageYaml`、`WorldLoadingCacheStore`、`SavedGameSettingsStore`、`UHCSpawn`、`SidebarTheme`、`AbstractBroadcastSender` 已移除 Foundation `YamlConfig`。 | 21.4 非 menu YAML readers 已基本完成；`ButtonLocalization` / `gui.yml` 仍屬 21.5 menu framework，不混入本段。 |
| serialized model | `UHCStats`、`UHCGameSettings`、5 個 sub-settings、`InventoryContent` 已改成本地資料讀寫；未使用的 `SimpleLocation` 已刪除。 | 主插件 source 已無 Foundation `SerializedMap` / `ConfigSerializable` 命中。 |
| sound config | `Sounds`、`SoundConfigParser`、`Extra.sound(...)` 與 scenario `@FilePath` sound 欄位已改用本地 `PluginSound`。 | 21.4 已完成；剩餘 Foundation `SimpleSound` 只在 `StatsMenu#getClickSound()` 與 `LegacyFoundationAdapter#configureMenuClickSound()`，屬 21.5 menu click sound。 |
| menu config | `ButtonLocalization` extends `YamlConfig`，`UHCMenuSection` 仍用 Foundation `MenuSection` / `SimplePlugin.getInstance()` 讀 `gui.yml` | 明確延後到 21.5 menu framework；21.4 不應順手拆 `gui.yml` menu 行為。 |

目前啟動 / reload 責任分布：

| 入口 | 目前作用 | 21.4 注意 |
| --- | --- | --- |
| `WonderlandUHC#onPluginStart()` | 建立 `PluginBootstrap` / `FeatureRegistry`、保存 resources、註冊 listeners / commands、setup practice / Discord voice、排程 delayed startup | 如果改成 `JavaPlugin#onEnable()`，要保留這些順序與 delayed startup 行為。 |
| `WonderlandUHC#onReloadablesStart()` | 目前依賴 Foundation reloadables lifecycle；先保存 resources 並載入 `Settings` / `Messages` / `CommandSettings` / `Sounds`，再檢查 dependency、`configureFoundationLibrary()`、載入 scoreboard themes / spawns / stats、啟動 runtime task、套用 test mode | settings / YAML 顯式載入已完成；正式移除 `SimplePlugin` 前，仍需在 21.5 menu framework 解除後保留這些 startup phase 順序。 |
| `WonderlandUHC#onPluginReload()` | `scenarioManager.reloadAll()` 與 `UHCGameSettingsSaver.reloadFromFile()`；Foundation reloadables lifecycle 仍會先跑 `onReloadablesStart()` 重新載入 static config / spawns / themes / stats。 | 21.4 已完成 settings / messages / commands / sounds / spawns / savedgames / sidebar / scenario config 的 reload 邊界；後續 lifecycle 移除時需保留同等順序。 |
| `PluginBootstrap#configureFoundationLibrary()` | `ButtonLocalization.load()` 與 `LegacyFoundationAdapter.configureMenuClickSound()` | 這屬 21.5 menu framework；21.4 只記錄，不混改。 |
| `PluginBootstrap#scheduleDelayedStartupTasks()` | delayed register scenarios、restore world loading cache、reload saved settings、log enabled banner | `CacheSaver` static 初始化目前會讀 `cache.db`；替換 store 時要避免 class-load 時序變成隱性錯誤。 |

優先處理：

| 檔案 / 類型 | 預期方向 |
| --- | --- |
| `Settings` / `Messages` / `CommandSettings` | 第一刀建立明確 static config load / reload 流程，保留 public static 欄位、現有 YAML key、缺值行為與 `PluginText.colorize(...)` 等既有後處理。 |
| `Sounds` / `SoundConfigParser` / `Extra.sound(...)` | 單獨一刀把 `SimpleSound` 換成本地 sound value 或直接 Bukkit sound playback；保留 `sounds.yml` 格式，不順手改倒數或 scenario 音效邏輯。 |
| `UHCSpawn` / `Spawns` | 已移除 Foundation `YamlConfig` 讀寫；保留 `spawns.yml` 路徑、`Lobby` key、單行 `world x y z yaw pitch` 格式、未設定時回 world spawn 的行為與 reload 行為。 |
| `WorldLoadingCacheStore` / `CacheSaver` | 已移除 `YamlConfig.clearLoadedSections()` 與 Foundation file access；保留 `cache.db` 中 `Host`、`Loading_Status`、`Settings`、`Match_Center.*` 形狀。 |
| `SavedGameSettingsStore` / `UHCGameSettingsSaver` | 已移除 Foundation `YamlConfig` 讀寫；保留 `savedgames.db` 以 player UUID 儲存多組 `UHCGameSettings` 的形狀。 |
| `StatsStorageYaml` / `UHCStats` | 保留 `stats.yml` 每 UUID 下的 `Game_Played`、`Kills`、`Wins` 形狀；不改成資料庫、不擴充統計模型。 |
| `ScenarioConfig` / scenario `@FilePath` 欄位 | 已移除 Foundation `YamlConfig` 讀取；保留 `scenarios.yml` 既有 scenario key、`Type`、`Name`、`Description`、`@FilePath` 欄位、舊 material alias 與 sound alias。 |
| `SidebarTheme` / `scoreboards.yml` | 已移除 Foundation `YamlConfig` 讀取；保留頂層 theme key、8 組 scoreboard line list 與 `Default` fallback 語意，不改 scoreboard line model；host menu 中的 theme selector UI 留 21.5 驗證。 |
| `AbstractBroadcastSender` / `DiscordBroadcastSender` | 已移除 Foundation `YamlConfig` 讀取；保留 `broadcasts.yml` 的 `Discord.Formatting`、`Discord.Invalid_Channel`、`Discord.Channel_Ids` 與 DiscordSRV 發送流程。 |
| `DeathMessageLoader` | 已移除 Foundation `YamlConfig` 讀取；保留 `messages.yml` death message 結構，不改死亡訊息行為。 |
| `ButtonLocalization` / `UHCMenuSection` / `gui.yml` | 延後到 21.5 menu framework，不在 21.4 順手拆。 |

建議拆法：

1. 先只新增或改出一個很薄的 YAML 讀寫入口，負責「從 plugin data folder 載入 / 保存 `YamlConfiguration`」即可；不要建立 schema registry、annotation framework 或通用 ORM。
2. 第一個程式碼切片只處理 `Settings`、`Messages`、`CommandSettings` 與 reload 呼叫順序，先讓 static config 不再靠 `SimpleSettings` / `YamlStaticConfig`。已完成。
3. 第二刀處理 `Sounds` 與 `SimpleSound`，因為 sound 型別被 static config、scenario config、menu click sound 共同使用；但 menu click sound 設定本身仍留到 21.5。21.4 sound config 已完成，menu click sound 仍留 21.5。
4. 第三刀處理 `SerializedMap` / `ConfigSerializable` 模型替換，再處理 `cache.db`、`savedgames.db`、`stats.yml` 這些需要 serialize / deserialize 的 store。已完成，未使用的 `SimpleLocation` 也已刪除。
5. 第四刀處理較單純的 YAML readers：`UHCSpawn`、`SidebarTheme`、death message、broadcast sender、`ScenarioConfig`。每個 store 保留原檔案與 key，不做資料格式 migration。已完成。
6. `WonderlandUHC extends SimplePlugin` 不在 21.4 硬拔；需等 21.5 menu framework 解除後，再把 Bukkit/Paper plugin lifecycle 正式化。

切片限制：

- 不把 YAML 轉資料庫。
- 不改 config key 命名與檔案格式，除非 Step 21 移除 dependency 必要。
- 不為了 generic warning 改動保存資料模型。
- 不新增大型 config abstraction；只有多處共用且語意相同的檔案載入 / 保存可抽薄 helper。
- 不在 21.4 處理 `gui.yml` menu layout、button click sound、inventory editor GUI 或 menu pagination；這些留 21.5。

驗收：

- 既有 config 檔案可讀。
- `/uhc reload` 不因移除 Foundation settings lifecycle 而 exception，且 reload 後 settings / messages / command strings / saved game cache 行為仍可用。
- host settings、scenario settings、saved game、stats、spawn、death message、broadcast formatting、scoreboard theme 至少完成啟動與基本讀取 smoke。
- `cache.db` 刪除 / 保存、`savedgames.db` 載入 / 保存、`stats.yml` 讀寫不改 YAML 形狀。
- 每個程式碼切片完成後跑 `rg -n "SimpleSettings|YamlStaticConfig|YamlConfig|YamlConfigLoader|SimpleLocalization|SerializedMap|ConfigSerializable|SimpleSound" src/main/java/org/mcwonderland/uhc` 對照減量。

#### 21.4 進度更新

更新日期：2026-05-17

第一個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | static config lifecycle 第一刀 | `Settings`、`Messages`、`CommandSettings` 不再繼承 `SimpleSettings` / `AutoLoadStaticConfig` / `YamlStaticConfig`。新增本地 `PluginStaticConfig`，只承接現有 public static 欄位、現有 YAML key 命名、`init()` 後處理與目前需要的型別讀取；`PluginBootstrap#loadStaticConfiguration()` 在 `onReloadablesStart()` 明確載入 resources 後呼叫。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪設定載入 exception。 |

本刀刻意未處理：

- `Sounds` 仍繼承 `AutoLoadStaticConfig`，並仍使用 Foundation `SimpleSound`；下一刀再獨立處理 sound value / playback。
- `AutoLoadStaticConfig` 與 `util/YamlConfigLoader` 仍暫留給 `Sounds` 使用，不在本刀硬刪。
- `CommandSettings` 已不再寫入 `SimpleLocalization.Commands.*`；native command 訊息直接使用 `CommandSettings`。
- `WonderlandUHC` 仍 `extends SimplePlugin`；必須等 settings / YAML stores / sounds 都脫離 Foundation 後再改 plugin lifecycle。
- `cache.db`、`savedgames.db`、`stats.yml`、`spawns.yml`、`scoreboards.yml`、death message、broadcast sender、`ScenarioConfig` 與 `gui.yml` 尚未在本刀處理。

本輪搜尋結果：

```bash
rg -n "SimpleSettings|YamlStaticConfig|YamlConfigLoader|SimpleLocalization|SimpleSound|ConfigSerializable|SerializedMap" src/main/java/org/mcwonderland/uhc
rg -n "class .* extends (AutoLoadStaticConfig|SimpleSettings|YamlStaticConfig|YamlConfig)" src/main/java/org/mcwonderland/uhc
```

`Settings` / `Messages` / `CommandSettings` 已不再命中 Foundation static config inheritance；`CommandSettings` 的 `SimpleLocalization` bridge 也已移除。剩餘命中集中在 `SimpleSound` menu click path 與 21.5 menu framework，符合後續拆分邊界。

更新日期：2026-05-17

第二個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | sound config / playback 第一刀 | 新增本地 `PluginSound` value，保留 `sounds.yml` / `scenarios.yml` 原有 `SOUND volume pitch` 與 `none` 格式；`SoundConfigParser` 改回傳 `PluginSound`，`Sounds` 改走 `PluginStaticConfig` 明確載入，`Extra.sound(...)` 與 scenario 的 `@FilePath` sound 欄位改用 `PluginSound`。`AutoLoadStaticConfig` 與舊 `YamlConfigLoader` 已移除。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 sound config / reload exception。 |

本刀刻意未處理：

- `StatsMenu#getClickSound()` 與 `LegacyFoundationAdapter#configureMenuClickSound()` 仍使用 Foundation `SimpleSound`，因為它們被 Foundation menu API 簽名綁住，留到 21.5 menu framework 移除時一起處理。
- `ScenarioConfig` 仍繼承 Foundation `YamlConfig`；本刀只替換 scenario sound 欄位型別，scenario YAML reader 本體留在後續 YAML readers 切片。
- `sounds.yml` 不做 key migration 或全量 alias 寫回，只保留既有格式與既有 alias parser。

本輪搜尋結果：

```bash
rg -n "AutoLoadStaticConfig|YamlConfigLoader|org\\.mineacademy\\.fo\\.model\\.SimpleSound|SimpleSound" src/main/java/org/mcwonderland/uhc
```

`AutoLoadStaticConfig` / `YamlConfigLoader` 已無 source 殘留；`SimpleSound` 只剩 `StatsMenu` 與 `LegacyFoundationAdapter` 兩個 Foundation menu 簽名使用點，不屬於 21.4 sound config 切片。

更新日期：2026-05-17

第三個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | stats.yml 第一刀 | `StatsStorageYaml` 不再繼承 Foundation `YamlConfig`，改用 Bukkit `YamlConfiguration` 直接讀寫 `stats.yml`；`UHCStats` 移除 `ConfigSerializable` / `SerializedMap`，保留既有 `Game_Played`、`Kills`、`Wins` 三個累積統計 key，不保存本場 `kills` 或 `oreMined`。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 stats storage exception。 |

本刀刻意未處理：

- `cache.db` / `savedgames.db` 尚未處理，因為它們依賴完整 `UHCGameSettings` nested serialize / deserialize。
- `UHCGameSettings` 與其 sub-settings、`InventoryContent` 在 stats.yml 第一刀當時仍使用 Foundation `SerializedMap` / `ConfigSerializable`；已於下一個資料模型切片完成替換。
- 其他 `YamlConfig` readers，例如 `ScenarioConfig`、death message，仍留在後續 YAML reader 切片。
- `ButtonLocalization` / `gui.yml` 仍屬 21.5 menu framework，不混入 21.4。

本輪搜尋結果：

```bash
rg -n "SerializedMap|ConfigSerializable|YamlConfig" src/main/java/org/mcwonderland/uhc/stats
```

`stats` package 已無 Foundation `SerializedMap` / `ConfigSerializable` / `YamlConfig` 命中；剩餘資料模型命中集中在 `UHCGameSettings`、sub-settings、`InventoryContent` 與目前未使用的 `SimpleLocation`。

更新日期：2026-05-17

第四個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | cache.db / savedgames.db settings model 第一刀 | `UHCGameSettings`、5 個 sub-settings 與 `InventoryContent` 改成本地 `toMap()` / `fromSection(...)` / `fromMap(...)`，移除 Foundation `ConfigSerializable` / `SerializedMap`；`WorldLoadingCacheStore` 與 `SavedGameSettingsStore` 改用 Bukkit `YamlConfiguration` 直接讀寫，保留 `cache.db` 的 `Host`、`Loading_Status`、`Settings`、`Match_Center.*` 形狀，以及 `savedgames.db` 的 UUID -> settings list 形狀。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`，既有 `cache.db` 可被讀回；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 settings model / cache store exception。 |

本刀刻意未處理：

- `CacheSaver` / `UHCGameSettingsSaver` 仍作為 legacy facade 留給現有 call site，不在本刀重排主流程。
- 尚未做主持選單人工保存測試；後續需用 GUI 改動 timer / border / inventory preset 後確認 `cache.db` / `savedgames.db` 寫回形狀仍一致。
- `SimpleLocation` 後續已確認沒有實際引用並刪除，未改寫成新的資料模型。
- 其他 `YamlConfig` readers，例如 `UHCSpawn`、`ScenarioConfig`、`SidebarTheme`、death message、broadcast sender，仍留在後續 YAML reader 切片。

本輪搜尋結果：

```bash
rg -n "YamlConfig|SerializedMap|ConfigSerializable" src/main/java/org/mcwonderland/uhc/storage src/main/java/org/mcwonderland/uhc/game/settings src/main/java/org/mcwonderland/uhc/model/InventoryContent.java
```

上述範圍已無 Foundation `YamlConfig` / `SerializedMap` / `ConfigSerializable` 命中；後續 dead code cleanup 也已刪除未使用的 `SimpleLocation`，主插件 source 已無 `SerializedMap` / `ConfigSerializable` 命中。

第五個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | spawns.yml reader 第一刀 | `UHCSpawn` 不再繼承 Foundation `YamlConfig`，改用 Bukkit `YamlConfiguration` 直接讀寫 `spawns.yml`；保留既有 `Lobby` key 與單行 `world x y z yaw pitch` 格式，讀取缺值或解析失敗時仍 fallback 到主世界 spawn 並讓 `isSet()` 維持 false；`/setspawn` 寫回仍使用 block 座標與整數 yaw / pitch。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 spawn reader exception。 |

本刀刻意未處理：

- `Spawns` 仍保留目前 static facade，因為 call site 很少，沒有必要為本刀抽新的 spawn service。
- `/setspawn` 指令流程、權限、訊息與音效不改，只替換底層檔案讀寫。
- 其他 `YamlConfig` readers，例如 `ScenarioConfig`、`SidebarTheme`、death message、broadcast sender，仍留在後續 YAML reader 切片。

本輪搜尋結果：

```bash
rg -n "org\\.mineacademy\\.fo\\.settings\\.YamlConfig" src/main/java/org/mcwonderland/uhc/settings/spawn
```

`settings/spawn` package 已無 Foundation `YamlConfig` import。

第六個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | scoreboards.yml theme reader 第一刀 | `SidebarTheme` 與內層 `ThemeLoader` 不再繼承 Foundation `YamlConfig`，改由 `SidebarTheme.loadThemes()` 使用 Bukkit `YamlConfiguration` 直接讀取 `scoreboards.yml`；保留頂層 theme key 順序、`Default` fallback 語意、8 組 scoreboard line list 與 host theme selector 使用的 `getAllThemes()` 行為。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 scoreboard theme reader exception。 |

本刀刻意未處理：

- `SidebarThemeSettingsMenu` 仍屬 21.5 menu framework，不在本刀改掉 Foundation menu / `ConfigMenuPagged`。
- Scoreboard line model、placeholder replacement、`ScoreBoardUpdater` 流程不改。
- 其他 `YamlConfig` readers，例如 `ScenarioConfig`、death message、broadcast sender，仍留在後續 YAML reader 切片。

本輪搜尋結果：

```bash
rg -n "org\\.mineacademy\\.fo\\.settings\\.YamlConfig|extends YamlConfig" src/main/java/org/mcwonderland/uhc/scoreboard/SidebarTheme.java
```

`SidebarTheme.java` 已無 Foundation `YamlConfig` import / inheritance。

第七個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | broadcasts.yml sender reader 第一刀 | `AbstractBroadcastSender` 不再繼承 Foundation `YamlConfig`，改用 Bukkit `YamlConfiguration` 直接讀取 `broadcasts.yml`；保留 `Discord` section、`Formatting`、`Invalid_Channel`、`Channel_Ids` 的相對讀取語意，`DiscordBroadcastSender` 的 DiscordSRV ready 檢查、mention 轉換、allowed mentions 與錯誤回報流程不改。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 broadcast sender reader exception。 |

本刀刻意未處理：

- `BroadcastSettingsMenu` 仍屬 21.5 menu framework，不在本刀改掉 Foundation menu / `ConfigMenu`。
- DiscordSRV 發送流程、Conversation 輸入、公告 placeholder 與 active mention 行為不改。
- 其他 `YamlConfig` readers，例如 `ScenarioConfig`、death message，仍留在後續 YAML reader 切片。

本輪搜尋結果：

```bash
rg -n "org\\.mineacademy\\.fo\\.settings\\.YamlConfig|extends YamlConfig" src/main/java/org/mcwonderland/uhc/model/broadcast src/main/java/org/mcwonderland/uhc/model/broadcast/impl
```

`model/broadcast` package 已無 Foundation `YamlConfig` import / inheritance。

第八個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | messages.yml death message reader 第一刀 | `DeathMessageLoader` 不再繼承 Foundation `YamlConfig`，改用 Bukkit `YamlConfiguration` 直接讀取 `messages.yml`；保留 `Game.PlayerDeath`、`Other`、`Player_Killed`、逗號分隔 DamageCause key 與無效 key 忽略語意。未使用的舊抽象類 `DeathMessages` 已刪除。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 death message / YAML reader exception。 |

本刀刻意未處理：

- `DeathMessageHandler` 的 placeholder、隨機挑選、killer / entity 判斷不改，避免把 reader 替換變成死亡訊息行為重寫。
- `messages.yml` 內容與 key 不做 migration。
- 實際死亡廣播人工測試留到 Step 22 最終對照；本刀只完成啟動、reload 與 reader 搜尋 gate。
- 其他 `YamlConfig` readers，例如 `ScenarioConfig`，仍留在後續 YAML reader 切片；`ButtonLocalization` / `gui.yml` 仍屬 21.5 menu framework。

本輪搜尋結果：

```bash
rg -n "org\\.mineacademy\\.fo\\.settings\\.YamlConfig|extends YamlConfig" src/main/java/org/mcwonderland/uhc/model/deathmsg
```

`model/deathmsg` package 已無 Foundation `YamlConfig` import / inheritance。

第九個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | scenarios.yml scenario config reader 第一刀 | `ScenarioConfig` 不再繼承 Foundation `YamlConfig`，改用 Bukkit `YamlConfiguration` 直接讀取 `scenarios.yml`；保留 scenario name prefix、`Type`、`Name`、`Description`、`@FilePath` 反射填值、`Integer` / `Boolean` / `String` / `List<String>` / `Material` / `List<Material>` / `PluginSound` 讀取、舊 material alias 與 sound alias 行為。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現 scenario isolation、scenario config、scenario material 或 scenario sound 相關 exception。 |

本刀刻意未處理：

- `ConfigBasedScenario` 的 `ItemCreator` icon 建立仍屬 21.5 menu / Foundation menu framework，不在本刀順手替換。
- scenario 註冊流程、scenario 啟用 / 停用流程、scenario gameplay 行為不改。
- `scenarios.yml` 內容與 key 不做 migration；舊 material / sound alias 仍保留以讀取既有設定。
- 不新增通用 config framework；本刀只在 `ScenarioConfig` 內保留現有 reader 需要的少量讀值方法。

本輪搜尋結果：

```bash
rg -n "org\\.mineacademy\\.fo\\.settings\\.YamlConfig|extends YamlConfig" src/main/java/org/mcwonderland/uhc/scenario/impl/ScenarioConfig.java src/main/java/org/mcwonderland/uhc/scenario
```

`scenario` package 已無 Foundation `YamlConfig` import / inheritance。

第十個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | CommandSettings SimpleLocalization bridge cleanup | `CommandSettings` 不再 import 或寫入 Foundation `SimpleLocalization.Commands.*`；保留所有 public static command message 欄位，仍由本地 `PluginStaticConfig` 從 `commands.yml` 載入。native command helper 繼續直接使用 `CommandSettings.NO_CONSOLE`、`LABEL_USAGE`、`RELOAD_SUCCESS` 等欄位。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；console 執行 `sendcoords` 仍回覆 `commands.yml` 的 `No_Console` 訊息。 |

本刀刻意未處理：

- Foundation menu / conversation 仍可使用 Foundation 自己的 `SimpleLocalization.Menu` / `SimpleLocalization.Conversation` 預設值；這些屬於 21.5 menu framework 或 lifecycle 移除時處理，不由 `CommandSettings` bridge 承接。
- 不改 `commands.yml` key、不改 command usage / tab completion、不改 native command helper。
- 不在本刀移除 `SimplePlugin` lifecycle。

本輪搜尋結果：

```bash
rg -n "SimpleLocalization|initSimpleLocalizationValues|COOLDOWN_WAIT" src/main/java/org/mcwonderland/uhc
```

主插件 source 已無 `SimpleLocalization` 命中。

第十一個程式碼切片已完成：

| 狀態 | 切片 | 實際處理 | 驗證 |
| --- | --- | --- | --- |
| 本輪完成 | SimpleLocation dead code cleanup | 刪除未使用的 `SimpleLocation`；不新增替代 map / section helper，因為 production / test source 都沒有使用點。主插件 source 已無 Foundation `SerializedMap` / `ConfigSerializable` 命中。 | `bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean` 通過；部署到 Paper `1.21.11` 測試服後以 `start.bat` 啟動到 `Done`；console 執行 `uhc reload` 成功輸出 `WonderlandUHC 1.0.0-alpha-2 已重新載入。`；`latest.log` 未出現本輪 cleanup 相關 exception。 |

本刀刻意未處理：

- 不新增 `Location` serializer helper，因為目前沒有使用者。
- `RespawnCommand`、`StartCountdown`、`LobbyCountdown` 的 `org.mcwonderland.uhc.util.*` wildcard import 不在本刀順手改成 explicit import；它們不構成 `SimpleLocation` 使用點。
- `SimpleSound` menu click path 與 21.5 menu framework 仍留到 21.5。

本輪搜尋結果：

```bash
rg -n "\bSimpleLocation\b|SerializedMap|ConfigSerializable" src/main/java src/test/java
```

主插件 production / test source 已無 `SimpleLocation`、Foundation `SerializedMap`、Foundation `ConfigSerializable` 命中。

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
