# WonderlandUHC 外部整合插件摘要

整理日期：2026-05-23

這份文件說明新版本 WonderlandUHC 會整合哪些外部插件、各自用途，以及缺少時對伺服器啟動與功能的影響。

## 整合插件總覽

| 插件 | 類型 | 用途 | 缺少時行為 |
| --- | --- | --- | --- |
| LuckPerms | runtime 必要依賴 | 登入階段權限查詢，判斷玩家是否能 bypass 滿人、白名單、遊戲已開始或設定中狀態。 | `plugin.yml` 以 `softdepend` 控制載入順序；啟動檢查會視為必要依賴，缺少或未啟用時 WonderlandUHC 會停止啟用。 |
| Chunky | runtime 必要依賴 | 世界預生成，承接 UHC 世界產生後的 chunk pregeneration 流程。 | `plugin.yml` 以 `softdepend` 控制載入順序；啟動檢查會視為必要依賴，缺少或未啟用時 WonderlandUHC 會停止啟用。 |
| DiscordSRV | 可選依賴 | Discord 公告與 Discord 語音頻道整合，例如公告發送、隊伍語音、`/reconnect`。 | 伺服器仍可啟動；Discord 相關功能會停用或回傳未安裝 / 不可用提示。 |

## plugin.yml 宣告

目前 `plugin.yml` 的依賴宣告為：

```yaml
softdepend: [ LuckPerms, Chunky, DiscordSRV ]
```

這代表三個外部插件都由 Paper 先作為 soft dependency 處理載入順序。WonderlandUHC 自己的啟動檢查會再把 LuckPerms 與 Chunky 視為必要依賴；任一插件不可用時，WonderlandUHC 會停止啟用。DiscordSRV 不阻止啟動，相關功能會在缺少時停用。

## 各插件用途

### LuckPerms

LuckPerms 是新版登入 gate 的權限來源。

主要用於：

- `wonderland.uhc.bypass.join.configuring`
- `wonderland.uhc.bypass.join.full`
- `wonderland.uhc.bypass.join.started`
- `wonderland.uhc.bypass.join.whitelist`

WonderlandUHC 會在登入流程中透過 LuckPerms 查詢玩家權限，而不是依賴已完整加入伺服器後的 Bukkit `Player` 狀態。

### Chunky

Chunky 是新版世界預生成流程的必要外部插件。它在 `plugin.yml` 中是 `softdepend`，用於控制 Paper 載入順序；WonderlandUHC 啟動檢查會把它視為必要依賴。

主要用於：

- 對 UHC 世界執行 square / radius 預生成。
- 透過 Chunky API 接收預生成完成事件。
- 完成後接回 WonderlandUHC 的世界載入狀態、cache save 與後續流程。

注意：`libs/Chunky-Bukkit-1.5.3.jar` 只是本專案的本地編譯依賴，讓 Gradle 封裝時找得到 Chunky API。實際開服若要啟用 Chunky 整合，仍需在伺服器 `plugins/` 放入 Chunky 插件。

### DiscordSRV

DiscordSRV 是 Discord 文字公告與語音整合的來源。

主要用於：

- 主持選單中的 Discord 公告發送。
- Discord 語音頻道設定。
- 隊伍語音頻道建立、移動與清理。
- 玩家使用 `/reconnect` 重新連回隊伍或大廳語音。

若 DiscordSRV 未安裝、未啟用或尚未 ready，相關功能應回覆明確訊息，不影響 WonderlandUHC 主流程啟動。

## 啟動時依賴狀態

WonderlandUHC 啟動時會在 console 輸出依賴插件狀態，方便確認外部整合狀態。

狀態意義：

| Console 顯示 | 內部狀態 | 意義 |
| --- | --- | --- |
| `可用` | `Available` | 插件存在且已啟用。 |
| `未啟用` | `Disabled` | 可選插件不存在或未 hooked，對應功能停用。 |
| `缺少` | `Unavailable` | 必要插件不存在或未 hooked，屬於需要修正的部署問題。 |

更完整的 `plugin.yml` / `build.gradle` 宣告、runtime adapter、缺少依賴時的行為與驗證重點，見 `docs/details/plugin-dependencies.md`。
