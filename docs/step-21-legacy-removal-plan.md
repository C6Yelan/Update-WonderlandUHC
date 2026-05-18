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

本輪只做只讀盤點與文件紀錄，尚未修改程式碼。掃描指令：

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
| old enchant simulation | `listener/OldEnchantListener.java` | random seed、old costs、hide enchants 依 DatouNMS；其餘 lapis / exp level 邏輯仍在 listener。 | `docs/steps.md` 已明確接受 1.7 舊附魔模擬正式移除；Step 21 不修復 NMS，只清理 NMS 呼叫與註冊殘留，避免把它列成 regression。 |
| TPS NMS reflection | `util/RuntimeUtil.java` | 先走 Paper `Bukkit#getTPS()`，再走 reflected Paper `getTPS()`，最後仍嘗試 `net.minecraft.server.<version>.MinecraftServer#recentTps`。 | 應在 21.1 移除最後的 NMS class reflection，只保留 Paper API / accepted fallback。這可和 DatouNMS 第一批一起做，因為同屬 NMS 搜尋 gate。 |

目前方法呼叫粗略數量：

| 方法 | 呼叫數 | 建議處理批次 |
| --- | ---: | --- |
| `getAbsorptionHearts` | 3 | 第一批：正式化 Bukkit absorption helper。 |
| `getArmorPoints` | 1 | 第一批：正式化 Bukkit / vanilla armor helper。 |
| `spawnExpOrb` | 1 | 第一批：正式化 Bukkit exp orb fallback。 |
| `mergeLargeChest` | 1 | 第一批或第二批：正式化 Bukkit chest merge fallback。 |
| `setCanPickupExp` | 4 | 第二批：在 Step 21 內改成正式 no-op / 移除呼叫 / 其他替代，並記錄 Step 22 檢查點。 |
| `playDeathAnimation` | 2 | 第二批：在 Step 21 內改成正式 no-op 或接受移除；`TestCommand` 測試入口可清理。 |
| `setBlockFast` | 3 | 第二批：單獨測 border / ore generation。 |
| old enchant methods | 3 | 第三批：正式移除 1.7 舊附魔 NMS path，並檢查 listener 註冊。 |

建議第一刀：

1. 新增或重用小型 runtime helper，承接 absorption hearts、armor points、custom exp orb、large chest merge 與 TPS Paper fallback。
2. 只替換上述已有 Bukkit fallback 的使用點。
3. 不處理 command/menu/settings，也不移除 `build.gradle` 的 DatouNMS dependency。
4. 跑 DatouNMS 搜尋 gate，預期仍會剩下 `setCanPickupExp`、`playDeathAnimation`、`setBlockFast`、old enchant 與 `LegacyDatouNmsAdapter` 本體；這些要留給下一批。
5. 封裝並用 Paper `1.21.11` server startup smoke test 驗證。

第一刀不建議包含：

- 直接刪 `LegacyDatouNmsAdapter`。
- 直接刪 DatouNMS dependency。
- 重寫 role / spectator / respawn 流程。
- 重寫 `OldEnchantListener` 的整個設定與 GUI 行為。
- 把 block placement、border、ore generation 和 scoreboard/health 同一刀處理。

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
