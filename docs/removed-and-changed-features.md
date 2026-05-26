# WonderlandUHC 1.21.11 移除與替換功能說明

整理日期：2026-05-23

這份文件從 commit history 追溯 `1.16.5` 舊版升級到 Paper `1.21.11` 過程中，被移除、替換或不再正式支援的功能面。它不是完整 changelog，也不取代 `docs/development-step-summary.md`；主要用途是讓委託人、服主與後續維護者快速理解新版和舊版的功能差異。

判讀原則：

- 「移除」代表原本的指令、設定、GUI、外部插件整合或 runtime 行為不再存在。
- 「替換」代表玩家需求仍保留，但實作來源改成新版相容方案。
- 純開發框架、legacy adapter、dead code 清理不應直接描述成玩家功能被刪除。

## 主升級線的功能變動

以下項目發生在 `1.16.5` 到 `1.21.11` 的主升級線，也就是 `main` / `update-to-1.21` 目前已包含的正式升級內容。

| 類型 | 舊版功能或依賴 | 1.21.11 狀態 | 主要 commit | 說明 |
| --- | --- | --- | --- | --- |
| 外部插件 | `PacketListenerAPI` | 移除依賴與啟動 gate | `f33e96c` | 不再要求 PacketListenerAPI 才能啟動。舊 packet 層 `SoundController` 也移除。 |
| 聲音控制 | packet 層 lobby / spectator 音效抑制 | 部分替換，部分需確認 | `f33e96c` | spectator 攻擊聲相關行為改由傷害事件取消處理；舊 lobby 走路/揮擊聲 packet 抑制不再是原本實作方式。 |
| 外部插件 | `custom-ore-generator` | 移除整合 | `c07114a` | 移除自訂礦物生成 hook、`GeneratorSettingsMenu`、`populators.yml` 與 populator runtime。 |
| 世界生成 | 自訂礦物生成設定 | 移除 | `c07114a` | 不再提供舊版「普通世界 / 豐盛世界 / 自訂礦物」設定面。 |
| 外部插件 | `WorldBorder` | 替換 | `487db02`, `5be9f2a` | 邊界改用 Paper 原生 world border；預生成改以 Chunky 為正式路線。WorldBorder 不再作為正式 fallback。 |
| 跑圖 | WorldBorder `wb fill` 跑圖流程 | 替換 | `487db02`, `5be9f2a` | 1.21.11 正式流程改為 Chunky square pregeneration。 |
| 資料儲存 | MySQL stats storage | 移除 | `9822a44` | 移除 `StatsStorageSql` 與 `Mysql` 設定區塊；目前保留 YAML stats 儲存路線。 |
| 指令 | `/mlg` | 移除 | `b6b758d` | 移除 MLG 傳送挑戰指令、權限與音效設定。 |
| 指令 | `/leave` | 移除 | `b6b758d` | 移除返回 Bungee lobby 的指令。 |
| 大廳物品 | lobby leave item | 移除 | `b6b758d` | 移除連到 `/leave` 的大廳離開物品。 |
| 伺服器網路 | `Bungee_Lobby` | 移除 | `b6b758d` | 不再提供透過設定指定 fallback server 的舊路線。 |
| Practice | `/practice <玩家>` | 移除 | `b6b758d` | 舊版 host 可替其他玩家切換 practice；新版只保留玩家自己 `/practice` 切換。 |
| 附魔 | `OldEnchant` 舊版附魔系統 | 移除 | `2a714be`, `b84ebbf` | 移除自動青金石、隱藏附魔、隨機附魔、舊附魔花費等 DatouNMS 依賴功能。 |
| 內建更新 | updater / old menu migration | 移除 | `b84ebbf` | 移除舊自動更新與 `menus.yml` 遷移器，以及對應 messages。 |
| 觀戰聊天 | `wonderland.uhc.spectator.chat.global` | 移除 | `a7a9daa` | 觀戰者聊天不再可用此權限直接廣播給全服。 |
| 設定資源 | `biomes.yml` / BiomeChanger | 移除 | `6d1501d` | 移除舊生態域覆蓋設定檔；新版中心搜尋不再依賴舊 biome 覆蓋。 |
| 設定資源 | `permissions.txt` | 移除預設 resource | `6d1501d` | 權限以 `plugin.yml` 與文件說明為主，不再產生舊 permissions text resource。 |
| 設定資源 | 預設 `cache.db`、`savedgames.db`、`stats.yml` | 移除 jar 預設 resource | `6d1501d` | 這些屬於運行資料，不應作為預設 resource 隨 jar 產生。 |
| 舊設定 | `Serialization`、`Version`、`Locale`、`Prefix` | 移除 | `6d1501d` | 清掉舊版授權、版本、語系或 prefix 殘留欄位。 |
| 隊伍設定 | `Allow_Character_Color` | 移除 | `6d1501d` | 隊伍徽章/顏色權限與 GUI 流程已改寫，不再使用此舊設定。 |

## 後續 QoL/Bugfix 分支的額外變動

以下項目發生在 `qol-bugfix-20260523` 分支，屬於升級完成後依新委託進行的 QoL 與 bugfix 切片。若這些內容最後會一起 release，建議也放進 release note。

| 類型 | 舊功能或設定 | 新狀態 | commit | 說明 |
| --- | --- | --- | --- | --- |
| 指令 | `/h` / `/viewheal` | 移除 | `3789e0e` | 移除血量查看指令與權限；血量資訊改依現有 scoreboard、tab/below-name 或箭矢命中提示等方式呈現。 |
| Scoreboard | scoreboard theme selector | 移除 | `3789e0e` | 移除多風格選擇 GUI 與 `SidebarThemeSettingsMenu`，只保留 default scoreboard 文本與更新/血量顏色設定。 |
| 世界規則 | `Always_Day` | 移除 | `1f5903f` | UHC 世界不再由此舊設定強制永晝。 |
| 世界規則 | `No_Fire_Tick` | 移除 | `1f5903f` | UHC 世界火焰蔓延回到新版正規世界規則處理。 |
| 世界規則 | `Anti_Rain` | 移除 | `1f5903f` | 不再由舊 listener 阻止下雨。 |
| 跑圖設定 | `ChunkLoading` | 移除 | `1f5903f` | 舊 `Frequency`、`Padding`、`Force_Loading_Nether_Chunk` 不再保留，避免與 Chunky 預生成流程重複。 |
| 地獄限制 | `Nether_Before_Pvp` | 移除 | `1f5903f` | 玩家能否進入地獄改為只看本場是否啟用地獄，不再用 PvP 開啟前限制進入地獄。 |

## 不應描述成刪功能的項目

這些變更容易在 commit history 中看到大量刪除，但本質上是升級替換或內部重構，不應直接對外寫成「功能被刪」。

| 項目 | 判斷 |
| --- | --- |
| Foundation 移除 | 屬於內部 framework 替換。command、menu、settings 等多數使用者功能仍保留，只是改成 repo 自己的實作。 |
| DatouNMS 移除 | 大多是內部相容層移除；真正跟玩家功能直接相關的是 `OldEnchant` 等 NMS 依賴行為。 |
| command framework 改寫 | `/team`、`/uhc` 等多數指令是換實作，不是刪除。 |
| menu framework 改寫 | 大多是 GUI 底層從 Foundation menu 換成本 repo menu，不是 GUI 功能整包消失。 |
| DiscordSRV | Discord 公告與語音整合仍保留；只是依賴狀態與缺失時的降級行為重新整理。 |
| 中心點與邊界流程 | 舊 `0,0` 假設被替換為 match center / center search，不是移除選圖功能。 |
| MiniMessage 遷移 | 舊 `&` / `§` 預設格式不再作為正式 runtime 支援目標，但玩家訊息功能仍存在。 |
