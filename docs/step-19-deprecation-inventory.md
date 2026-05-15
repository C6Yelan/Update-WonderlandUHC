# Step 19 Deprecated API 盤點

建立日期：2026-05-15

本文件承接 `docs/steps.md` Step 19，用來整理目前 Paper `1.21.11` / Java 21 編譯時仍出現的 deprecated API。目的不是把所有警告一次修完，而是依風險與類型分類，避免把不相關的舊 API、Foundation 清理與行為重寫混在同一刀。

## 盤點方式

本輪使用 `-Xlint:deprecation` 重新封裝插件，僅針對 `Update-WonderlandUHC` 本體檢查 deprecated API。

```bash
GRADLE_EXTRA_ARGS='-I /tmp/gradle-xlint-deprecation.gradle' bash scripts/package-plugin-1.21.sh --skip-foundation --skip-bootstrap
```

結果：

- 封裝成功。
- 原始 deprecated warning 數量為 `148`。
- `148` 是 javac 原始警告數，包含 Lombok 針對 deprecated 型別或欄位產生的重複警告，不代表有 148 個獨立問題。
- 先前直接使用 `org.bukkit.conversations` 的遊戲開始公告輸入流程已移除，因此本輪警告中不再出現該直接 Conversation 路徑。

## 2026-05-15 最新收斂結果

最新一次 `-Xlint:deprecation` 封裝已通過，完整 log 暫存於 `/tmp/uhc-deprecation-current-step19.log`。

目前 raw deprecated warning 數量為 `75`，已從 Step 19 盤點基準的 `148` 降低。已完成的類型包含：

| 類型 | 狀態 | 備註 |
| --- | --- | --- |
| 事件訊息 / Component API | 已完成 | `quitMessage(Component)`、`deathMessage(Component)`、`motd(Component)` 已替換。 |
| 小型 Bukkit API cleanup | 已完成 | `hidePlayer(plugin, player)`、`Attribute.MAX_HEALTH`、`addPassenger`、plugin meta 等已替換。 |
| Scoreboard Component API | 已完成 | objective display name、team prefix / suffix / color 已改新版 API；`SimpleSidebar` 的 direct `ChatColor` 使用也已移除。 |
| ItemMeta 與 Inventory title | 已完成 | item display name / lore 與 inventory title 已改 component API，仍以 `LegacyComponentSerializer` 保留舊設定色碼相容。 |
| Runtime event / item API | 已完成 | `PlayerPickupItemEvent`、`Entity#setCustomName(String)`、`ItemStack#setType(Material)` 已替換。 |
| 低風險 ChatColor cleanup | 已完成 | `Chat`、`OreAlert`、`SimpleSidebar` 已移除直接 `org.bukkit.ChatColor` 使用；仍保留舊 `&` / `§` 色碼相容。 |

目前剩餘 warning 已集中在四類：

| 分類 | Raw warnings | 主要檔案 | Step 19 判斷 |
| --- | ---: | --- | --- |
| Login event | 32 | `game/state/share/login/UHCLoginEvent.java`、`game/state/share/login/LoginListener.java`、`game/state/share/login/checker/LoginChecker.java` | 已完成 Paper `1.21.11` 替代流程研究；不列為 Step 19 實作項，延後到 Step 21 legacy 移除處理，並在 Step 22 最終對照驗收。 |
| ChatColor / legacy color model | 35 | `game/UHCTeam.java`、`api/object/UHCTeam.java`、team / scoreboard menu | 已決定不列為 Step 19 實作項；team color model、public API 與 Foundation `ColorMenu` 回傳型別整包延後到 Step 21。 |
| MetadataValue / legacy state | 7 | `legacy/LegacyFoundationAdapter.java`、`model/tutorial/model/Tutorial.java`、`game/CombatRelog.java` | 先延後到 Step 21 legacy state / Foundation 移除相關工作。 |
| Foundation config API | 1 | `storage/WorldLoadingCacheStore.java` | 先延後到 Step 21 Foundation 設定層解除依賴。 |

## 判讀原則

1. 不是所有剩餘 deprecated API 都和 Foundation 有關；多數仍是插件本體直接使用 Bukkit / Paper 舊 API。
2. Foundation 內部與高度綁定 Foundation 的清理不列為本輪優先目標，因為後續會整體解除依賴。
3. 能保持原行為並替換成 Paper / Adventure 新 API 的項目，可在 Step 19 內繼續分批處理。
4. 牽涉隊伍顏色模型、物品資料保留或 public API 的項目，需先研究替代 API 與行為差異，不應直接批量替換；team color / `ColorMenu` / public API 已決定延後到 Step 21。
5. 登入流程已確認不是單純 deprecated method 替換；`PlayerLoginEvent` 的移除會改動登入 gate、權限 bypass、白名單與 `UHCPlayer` 建立時機，因此延後到 Step 21 處理，Step 22 驗收。
6. 一般 IDE warning，例如 unused、null-safety 或 unchecked noise，不列入本 Step 19 deprecated API 清理；低風險雜訊整理已移到 Step 20。
7. 每一刀應以「API 類型」為單位，不再只為一兩個檔案單獨 commit；但也不能把不同風險的項目混成過大變更。

## `LegacyComponentSerializer` 過渡註記

事件訊息、MOTD、scoreboard 或 GUI 文字改成 Paper / Adventure `Component` API 時，短期內可以使用 `LegacyComponentSerializer.legacySection()` 承接舊設定與舊程式中的 `&` / `§` 色碼字串。

這不是最終文字系統，也不代表 legacy text 已完成清理。它的用途只是在替換 deprecated Bukkit string method 時保留既有設定行為，避免顏色碼直接顯示成一般文字。

Step 21 後續處理 `ChatColor / legacy color model` 時，必須回頭檢查這些 `LegacyComponentSerializer` 使用點，決定是否：

1. 集中成明確的 config text migration / compatibility layer。
2. 改成 MiniMessage 或其他新版設定文字格式。
3. 保留舊 `&` 色碼相容，但限制在少數輸入邊界，不再散落在 listener、scoreboard、menu 與 domain model。

## 已完成的 Step 19 類型

| 分類 | 代表 deprecated API | 主要檔案 | 建議處理 |
| --- | --- | --- | --- |
| 事件訊息 / Component API | `PlayerQuitEvent#setQuitMessage(String)`、`PlayerDeathEvent#setDeathMessage(String)`、`ServerListPingEvent#setMotd(String)` | `game/player/role/player/RolePlayerEvents.java`、`game/player/role/spectator/RoleSpectatorEvents.java`、`game/player/role/models/EmptyEventHandler.java`、`game/state/share/QuitListener.java`、`game/state/share/LobbyQuitListener.java`、`game/state/playing/listener/death/PlayingDeathListener.java`、`game/state/share/MotdListener.java` | 已完成。 |
| Scoreboard Component API | `Objective#setDisplayName(String)`、`Team#setPrefix(String)`、`Team#setSuffix(String)`、`Team#setColor(ChatColor)` | `scoreboard/SimpleScores.java`、`scoreboard/SimpleSidebar.java` | 已完成 component API；`SimpleSidebar` 已移除 direct `ChatColor`。 |
| ItemMeta 與 GUI 文字 | `ItemMeta#getDisplayName()`、`ItemMeta#setDisplayName(String)`、`ItemMeta#getLore()` | `model/GamePlaceholderReplacer.java`、`scenario/impl/AbstractScenario.java`、`menu/model/InventoryEditButton.java`、`game/state/playing/listener/GoldenHeadListener.java`、`menu/ButtonLocalization.java`、`util/Extra.java` | 已完成。 |
| Inventory title | `Bukkit#createInventory(..., String)` | `game/UHCTeam.java`、`game/state/playing/listener/InteractListener.java` | 已完成。 |
| 小型 Bukkit API cleanup | `Player#hidePlayer(Player)`、`Player#showPlayer(Player)`、`Damageable#getMaxHealth()`、`LivingEntity#addPotionEffect(PotionEffect, boolean)`、`Entity#setPassenger(Entity)`、`Plugin#getDescription()` | `util/PlayerHider.java`、`practice/CommonPracticeListener.java`、`model/freeze/SitFreeze.java`、`util/Extra.java`、`bootstrap/PluginBootstrap.java`、`Dependency.java` | 已完成。 |
| Runtime event / item API | `PlayerPickupItemEvent`、`Entity#setCustomName(String)`、`ItemStack#setType(Material)` | `game/state/playing/listener/ItemListener.java`、`game/CombatRelog.java`、`scenario/impl/rush/ScenarioCutClean.java`、`scenario/impl/consume/ScenarioSoup.java`、`util/WorldUtils.java` | 已完成。 |
| 低風險 ChatColor cleanup | `ChatColor.translateAlternateColorCodes()`、`ChatColor#getLastColors()`、小型顯示 enum 顏色 | `util/Chat.java`、`scoreboard/SimpleSidebar.java`、`game/player/staff/OreAlert.java` | 已完成；保留舊 `&` / `§` 色碼輸入相容。 |

## 需要先研究再動

| 分類 | 代表 deprecated API | 主要檔案 | 風險 |
| --- | --- | --- | --- |
| ChatColor / legacy color model | `org.bukkit.ChatColor`、Foundation `ColorMenu` 回傳型別 | `game/UHCTeam.java`、`api/object/UHCTeam.java`、`menu/impl/host/ScoreboardSettingsMenu.java`、`menu/impl/game/TeamSettingsMenu.java` | 剩餘部分牽涉 team color、menu 色彩選擇與 public API。已決定整包延後到 Step 21，不在 Step 19 用 wrapper 或 suppress 消除警告。 |

## `PlayerLoginEvent` 延後決策

Paper `1.21.11` 對 `PlayerLoginEvent` 的建議替代不是一對一改型別：

- `PlayerConnectionValidateLoginEvent` 是 pre-login 驗證主替代，但只提供 connection / profile，不提供完整 `Player`。
- `PlayerServerFullCheckEvent` 只處理伺服器最大人數判斷，不等於目前 UHC 自己的 `Game.getSettings().getMaxPlayers()` gate。
- `AsyncPlayerPreLoginEvent` 是 async，不能直接搬目前會碰 Bukkit player permission、`Game` 狀態與 `UHCPlayer` 的 checker。
- `ProfileWhitelistVerifyEvent` 偏向原生 server whitelist，不等於目前 UHC 自己的 whitelist / bypass 行為。

目前登入流程會在 `UHCLoginEvent` 建構時使用 `PlayerLoginEvent#getPlayer()` 建立 `UHCPlayer`，並在 checker 中使用 `Player` 做白名單、權限 bypass、遊戲已開始與滿員判斷。若在 Step 19 直接替換，會改變登入前後的玩家物件建立時機與拒絕語意，風險超過一般 deprecated cleanup。

因此本項在 Step 19 只記錄為刻意保留：

1. Step 21 重新處理登入 gate，決定是否以 Paper login connection event 重寫，或將部分需要完整 `Player` 的檢查明確延後到 join 後處理。
2. Step 22 將白名單、等待 host、滿員、遊戲中加入、bypass 權限列入 1.16 / 1.21 最終對照 checklist。
3. 在 Step 21 之前不得只為消除 warning 新增一層抽象包住 `PlayerLoginEvent`，那會掩蓋 deprecated API，卻不解決登入模型差異。

## 可延後到 Foundation / Legacy 清理

| 分類 | 代表 deprecated API | 主要檔案 | 延後原因 |
| --- | --- | --- | --- |
| Foundation config API | `YamlConfig#clearLoadedSections()` | `storage/WorldLoadingCacheStore.java` | 屬 Foundation 設定層相容問題，後續解除 Foundation 依賴時再一起處理較合理。 |
| MetadataValue / legacy state | `MetadataValue` | `legacy/LegacyFoundationAdapter.java`、`model/tutorial/model/Tutorial.java`、`game/CombatRelog.java` | 一部分是 legacy adapter，一部分是舊狀態記錄方式。若要移除，應和對應功能狀態管理一起重寫，不適合只為消 warning 改。 |

## 目前警告熱點

以下是 `-Xlint:deprecation` 依檔案統計的主要熱點。數量是 raw warning 數，會包含 Lombok 產生的重複警告。

| Raw warnings | 檔案 |
| ---: | --- |
| 25 | `game/state/share/login/UHCLoginEvent.java` |
| 23 | `game/UHCTeam.java` |
| 10 | `api/object/UHCTeam.java` |
| 6 | `game/state/share/login/LoginListener.java` |
| 5 | `legacy/LegacyFoundationAdapter.java` |
| 1 | `storage/WorldLoadingCacheStore.java` |
| 1 | `model/tutorial/model/Tutorial.java` |
| 1 | `menu/impl/host/ScoreboardSettingsMenu.java` |
| 1 | `menu/impl/game/TeamSettingsMenu.java` |
| 1 | `game/state/share/login/checker/LoginChecker.java` |
| 1 | `game/CombatRelog.java` |

## 建議剩餘處理順序

1. `ChatColor / team color model` 已完成盤點並延後到 Step 21；原因是它同時牽涉 team public API、Foundation `ColorMenu`、menu color picker 與 scoreboard team color，不適合在 Step 19 做短期 adapter。
2. `PlayerLoginEvent` 已完成研究並延後，不再作為 Step 19 下一刀。
3. `MetadataValue` 先延後到 Step 21 legacy state / metadata store 清理；它和 `LegacyFoundationAdapter`、tutorial、combat relog 狀態綁定，不適合只為消 warning 改。
4. `WorldLoadingCacheStore#clearLoadedSections()` 先延後到 Step 21 Foundation config 解除依賴。
5. Step 19 可視為「低風險可替換 API 已完成」，剩餘項目轉入後續專門步驟。

## 不建議的處理方式

- 不建議為了消除所有 warning 一次全面改成 Adventure component，因為 `ChatColor`、ItemMeta、scoreboard、team API 的行為邊界不同。
- 不建議現在修改 `lib-foundation` 或 Foundation 內部 API，除非它直接阻塞插件本體啟動或封裝。
- 不建議只新增一個巨大 `LegacyTextAdapter` 後到處包，這會把舊文字模型藏起來，但不會真正降低後續解除 Foundation / Bukkit legacy API 的成本。
- 不建議先動 `PlayerLoginEvent`；Paper 替代流程已確認會牽涉登入 gate 重設，應延後到 Step 21 處理，Step 22 驗收。
- 不建議在 Step 19 為 team color 新增暫時 wrapper 或 suppress；這會隱藏 `ChatColor` 警告，但不會解決 Foundation `ColorMenu` 與 public API 的實際依賴。

## Step 19 後續完成判斷

Step 19 不需要追求 raw warning 歸零才算有進展。比較合理的完成標準是：

1. 插件本體中可直接替換且低風險的 deprecated Paper / Bukkit API 已清掉。
2. 高風險項目已分類並寫明延後理由，不再混在一般 cleanup 中。
3. 每一刀改完都能完成封裝與 Paper `1.21.11` 啟動 smoke test。
4. 若最後仍保留 deprecated API，必須能說明它是 Foundation 解除依賴、Login 流程研究、ChatColor 模型切換或行為風險造成的刻意保留；目前 team color 已明確歸入 Step 21。
