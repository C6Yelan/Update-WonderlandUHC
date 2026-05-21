# WonderlandUHC

WonderlandUHC 是一款 Minecraft UHC 插件。本 repository 是基於原作者 WonderlandUHC 的 Paper `1.21.11` 更新維護版本，保留原專案脈絡，但不是原作者本人直接發布的新版本。

原專案前身為 **魯大頭UHC**，於 2019 年以付費形式發布，後續由原作者開源，讓玩家能自行主持 UHC 與朋友遊玩。本 fork 的主要目標是讓原插件能在新版 Paper / Java 環境中繼續運作，並整理後續維護所需的文件與流程。

![Marketing Seminar Facebook Cover](https://user-images.githubusercontent.com/41278925/184468277-25183ff3-07bd-4d61-9792-0045d36a0fcd.png)

## 目前支援環境

| 項目 | 版本 / 狀態 |
| --- | --- |
| Minecraft server | Paper `1.21.11` |
| Java | Java `21` |
| 插件版本 | `1.21.11-0.1.0` |
| 文字格式 | Adventure MiniMessage |
| 舊色碼格式 | 不再解析舊 Bukkit 色碼 |

版本號採用 fork 維護版格式：`<目標 Paper 版本>-<fork release 版本>`。例如 `1.21.11-0.1.0` 代表這是針對 Paper `1.21.11` 的 fork 第一次 `0.1.0` release；後續修正可遞增為 `1.21.11-0.1.1`，較大功能或設定變更可遞增為 `1.21.11-0.2.0`。

## 外部插件需求

| 插件 | 類型 | 用途 |
| --- | --- | --- |
| LuckPerms | 必要 | 登入階段權限查詢與 bypass 判斷。 |
| Chunky | 跑圖必要 | UHC 世界 chunk 預生成；若要使用新地圖流程必須安裝。 |
| DiscordSRV | 可選 | Discord 公告與語音頻道整合。 |

LuckPerms 會在 `plugin.yml` 中以 `depend` 宣告。Chunky 與 DiscordSRV 是 `softdepend`，缺少時不會阻止伺服器啟動，但缺少 Chunky 會導致正式預生成流程無法完成。

## 安裝方式

1. 準備 Paper `1.21.11` 伺服器，並使用 Java `21` 啟動。
2. 從本 repository 的 [Releases](https://github.com/C6Yelan/Update-WonderlandUHC/releases) 下載 WonderlandUHC jar。
3. 將 WonderlandUHC jar 放入伺服器 `plugins/`。
4. 安裝 LuckPerms。
5. 若要使用 UHC 新地圖產生與預生成流程，安裝 Chunky。
6. 若要使用 Discord 公告或語音整合，安裝 DiscordSRV。
7. 啟動伺服器，讓插件產生預設設定檔。
8. 依需求修改 `plugins/WonderlandUHC/` 內的設定檔，再重啟伺服器或依情況使用 `/uhc reload`。

正式開局前，請確認 console 中 WonderlandUHC 的 dependency status。LuckPerms 應為可用；若要跑新地圖，Chunky 也必須可用。

## 基本主持流程

常見流程如下：

```text
/uhc regen -> 預覽地圖 -> /uhc choose -> 等待 Chunky 預生成完成 -> /uhc start
```

流程重點：

1. `/uhc regen` 會建立預覽世界，可選擇是否啟用 CenterCleaner 搜尋較適合 UHC 的中心。
2. 主持人確認預覽世界後，使用 `/uhc choose` 進入正式跑圖流程。
3. `/uhc choose` 不是直接開局，而是保存目前地圖、重啟伺服器並交給 Chunky 預生成。
4. 預生成完成後，插件會將地圖狀態標記為可開局。
5. `/uhc start` 會開始大廳倒數，接著分批傳送玩家並進入正式遊戲。

更完整的流程摘要見 [docs/game-flow.md](docs/game-flow.md)。

## 訊息格式

新版預設訊息設定使用 Adventure MiniMessage，例如：

```text
<red>錯誤</red>
<gold><bold>標題</bold></gold>
<gray>玩家: </gray><green>{player}</green>
```

修改 `commands.yml`、`messages.yml`、`gui.yml`、`items.yml`、`scoreboards.yml`、`scenarios.yml`、`settings.yml` 等文字時，請使用 MiniMessage，並保留既有 `{placeholder}` 名稱。

舊 Bukkit 色碼不再作為正式格式解析，也不提供舊設定檔自動轉換工具。既有伺服器資料夾若包含舊格式訊息，需重置設定檔或人工改成 MiniMessage。

詳細摘要見 [docs/text-format.md](docs/text-format.md)。

## 維護文件

`docs/` 根目錄保留給委託人、原作者與後續維護者閱讀的摘要文件：

- [維護脈絡摘要](docs/maintenance-context.md)
- [開發歷程摘要](docs/development-step-summary.md)
- [專案檔案架構摘要](docs/project-structure.md)
- [外部整合插件摘要](docs/plugin-dependencies.md)
- [遊戲流程摘要](docs/game-flow.md)
- [選圖與中心搜尋機制摘要](docs/map-selection.md)
- [文字格式摘要](docs/text-format.md)

## 回報問題

若使用本 fork 的 release，請優先至本 repository 的 [Issues](https://github.com/C6Yelan/Update-WonderlandUHC/issues) 回報問題。回報時請盡量提供：

- Paper 與 Java 版本。
- WonderlandUHC 版本。
- 已安裝的 LuckPerms、Chunky、DiscordSRV 版本。
- console 錯誤訊息或 `logs/latest.log` 相關片段。
- 問題發生前執行的指令或操作步驟。

## 原作者與原專案背景

原作者：`LU__LU` / 魯魯

原專案：<https://github.com/MCWonderland/WonderlandUHC>

原專案 Discord 社群：<https://discord.gg/vTnEaX37Nr>

本 fork 保留原專案作者、開源脈絡與 GPL-3.0 授權。若需要理解本 fork 的維護背景與協作方式，請參考 [docs/maintenance-context.md](docs/maintenance-context.md)。
