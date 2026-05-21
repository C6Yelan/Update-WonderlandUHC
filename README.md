# WonderlandUHC
WonderlandUHC是一款擁有高度自訂性、高效能、極具趣味性的MinecraftUHC插件，內含多達38項特殊模式，以及各式各樣有趣的UHC相關設定，歡迎前來下載體驗！

<img width="851" height="315" alt="UHC Group photo (自訂)" src="https://github.com/user-attachments/assets/f504568a-ee37-42e2-ba21-23f928a2e016" />



## 安裝方式
### 插件教學資源
如果您不清楚Minecraft伺服器的安裝方式、看不懂底下的敘述，可以通過下列方法學習
- 觀看包含安裝伺服器的完整教學影片：[前往插件教學影片清單](https://www.youtube.com/watch?v=muiqzpNwOAk&list=PL86KqzDo7_n0Vkthb9I85hfS390i-4NPb)
- 若安裝插件後，不熟悉遊戲內操作，可在遊戲裡輸入`/uhc tutorial`，獲取遊戲內UHC主持教學

### 文字安裝步驟
1. 準備 Paper `1.21.11` 伺服器[(也可以點我下載)](https://fill-ui.papermc.io/projects/paper/version/1.21.11)，並使用 Java `21` 啟動
2. 從[Releases](https://github.com/C6Yelan/Update-WonderlandUHC/releases)下載 WonderlandUHC.jar
3. 將插件放入伺服器的插件資料夾 `plugins/`
4. 安裝[LuckPerms](https://modrinth.com/plugin/luckperms/changelog?g=1.21.11&l=paper)和[Chunky](https://modrinth.com/plugin/chunky/changelog?c=release&g=1.21.11&l=paperr)兩款依賴插件，放入伺服器的插件資料夾 `plugins/`
(若未安裝就開啟伺服器，可能導致插件無法使用！)
5. 若需使用Discord群組公告功能及語音系統，請安裝[DiscordSRV](https://modrinth.com/plugin/discordsrv/versions?g=1.21.11&l=paper)
6. 啟動伺服器，讓插件產生預設設定檔
7. 依需求修改 `plugins/WonderlandUHC/` 內的設定檔，再重啟伺服器或依情況使用 `/uhc reload`
8. 享受你的UHC比賽！

#### ※若您還是對安裝插件或主持UHC有疑問，下圖為Fork開發者之一，請在Discord私訊我，我很樂意協助你！

<img width="392" height="279" alt="image" src="https://github.com/user-attachments/assets/6205e399-aa5e-4070-b596-823340c5fadd" />

## 目前支援狀況

| 項目 | 版本 / 狀態 |
| --- | --- |
| Minecraft | Paper `1.21.11` |
| Java | Java `21` |
| 插件版本 | `1.21.11-0.1.0` |

此插件未來將視情況跟進新版本，或考慮支援舊版本，目前會以1.21.11為基礎開發

## 外部插件介紹

| 插件 | 類型 | 用途 |
| --- | --- | --- |
| LuckPerms | 必要 |  檢查與驗證玩家權限 |
| Chunky | 必要 |進行UHC世界跑圖，提高遊玩效能|
| DiscordSRV | 可選 |啟用Discord群組公告功能及語音系統 |

注意事項：
**未安裝Chunky不會阻止伺服器啟動和插件啟用，但無法開始UHC世界跑圖，這會導致插件無法進入準備開始的狀態，因此使用插件時，請務必檢查是否已安裝Chunky！**
(此狀況可能於未來修正)

## 插件運作流程

常見流程如下：

```text
管理員進入伺服器調整設定->預覽UHC地圖->等待Chunky預生成完成 ->邀請玩家開始UHC比賽
```

更完整的流程摘要見 [docs/game-flow.md](docs/game-flow.md)

## 訊息格式

新版新版預設訊息設定使用 Adventure MiniMessage，例如：

```text
<red>錯誤</red>
<gold><bold>標題</bold></gold>
<gray>玩家: </gray><green>{player}</green>
```

修改 `commands.yml`、`messages.yml`、`gui.yml`、`items.yml`、`scoreboards.yml`、`scenarios.yml`、`settings.yml` 等文件時，請使用MiniMessage格式，並保留 `{placeholder}` 名稱

詳細摘要見 [docs/text-format.md](docs/text-format.md)

## 開發文件

如果您對此插件的Fork流程有興趣，可前往`docs/`瀏覽相關文件：

- [維護脈絡摘要](docs/maintenance-context.md)
- [開發歷程摘要](docs/development-step-summary.md)
- [專案檔案架構摘要](docs/project-structure.md)
- [外部整合插件摘要](docs/plugin-dependencies.md)
- [遊戲流程摘要](docs/game-flow.md)
- [選圖與中心搜尋機制摘要](docs/map-selection.md)
- [文字格式摘要](docs/text-format.md)

## 回報問題

若在遊玩中遇到bug，請優先至[Issues](https://github.com/C6Yelan/Update-WonderlandUHC/issues)回報問題，並附上盡可能詳細的敘述與影像資源！

**※原作者已不再更新此插件，請不要將問題回報給原作者，以免給原作者造成困擾！**

## Fork作者、原作者與原專案背景介紹
### Fork開發者
- [C6Yelan](https://github.com/C6Yelan/)：負責主要開發、研究程式碼、升級插件
- [SnYe 玥雪](https://github.com/SnYe-SnowYue)：設計新版中心點篩選系統，設計宣傳網頁，接手後續插件更新
### 原作者
#### LU__LU / 魯魯
- Github個人檔案：[點此前往](https://github.com/lulu2002)
- 原專案連結：<https://github.com/MCWonderland/WonderlandUHC>
### 原專案背景介紹
前身為魯大頭UHC，於2019年以付費形式發布，本次Fork插件的主要目標是讓原插件能在新版Paper環境中繼續運作，並提供新版本支援

本fork保留原專案作者、開源脈絡與 GPL-3.0 授權。若需要理解本 fork 的維護背景與協作方式，請參考 [docs/maintenance-context.md](docs/maintenance-context.md)
