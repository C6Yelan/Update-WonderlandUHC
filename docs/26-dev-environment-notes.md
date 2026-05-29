# Paper 26 開發環境試推結果

## 隔離方式

- 分支：`experiment/update-to-26`
- 資料夾：`/home/ayaya/projects/Minecraft-Update-Plugin/Update-WonderlandUHC-26`
- 來源基線：`main` 的 `v1.21.11-0.1.3` 狀態
- 原本 `Update-WonderlandUHC` 的 Paper `1.21.11` 開發環境未被覆蓋或替換。

## 最終建置基線

- Minecraft / Paper：Paper `26.1.2`
- Java toolchain：Java `25`
- Paper API：`io.papermc.paper:paper-api:26.1.2.build.+`
- Gradle wrapper：`9.5.1`
- Shadow plugin：`9.4.2`
- 插件測試版本：`26.1.2-0.1.0-dev`
- `plugin.yml`：`api-version: '26.1.2'`
- `build.gradle` 已改為 Gradle 10 相容寫法，`--warning-mode all` 未再列出 Gradle deprecation warnings。

## 最終驗證結果

- 封裝成功。
- Jar 內 `plugin.yml` 已確認展開為：

```yaml
version: 26.1.2-0.1.0-dev
api-version: '26.1.2'
```

- 新建 Paper `26.1.2` 測試服資料夾：

```text
/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/check/paper-26.1.2
```

- 測試服使用 Windows Java `25.0.2`，並透過該資料夾內的 `start.bat` 啟動。
- 測試服依賴已更新至目前 26.1.2 相容最新版：`LuckPerms 5.5.53`、`Chunky 1.5.3`、`DiscordSRV 1.30.5`。
- `LuckPerms 5.5.53`、`Chunky 1.5.3` 與 `DiscordSRV 1.30.5` 安裝後，WonderlandUHC 可正常啟用。
- Paper 啟動到 `Done`，WonderlandUHC 顯示版本 `26.1.2-0.1.0-dev`。
- 伺服器可透過 `stop` 正常關閉，測試後 `25571` port 已釋放。
- `latest.log` 未看到 WonderlandUHC 導致的 `ERROR`、`Exception`、`SEVERE`、`Unsupported` 或 `Could not`。

## 發現事項

- Java 25 會顯示 `sun.misc.Unsafe` 相關警告；目前看到的 server runtime 警告來自 Paper 依賴的 `org.joml`，不是 WonderlandUHC 程式碼造成。
- Java 25 編譯時仍會顯示 Lombok 觸發的 `sun.misc.Unsafe` 警告；目前 Lombok `1.18.44` 已是官方列出的最新版本且支援 JDK 25，暫不為此切到非正式版本。
- DiscordSRV `1.30.5` 若未設定 bot token，會自行輸出 `No bot token has been set in the config` 並停用；這是 DiscordSRV 未配置造成，不是 WonderlandUHC 啟動失敗。
- WonderlandUHC 啟動時列出的依賴下載連結仍指向 `g=1.21.11`，正式 26 線 release 前應改成 26 線資訊或不綁版本的連結。
- Paper 26.1.2 新世界資料使用 `world/dimensions/minecraft/...` 結構，後續功能測試需特別覆蓋世界建立、載入、選圖、`uhc_practice` 與 `uhc_world` 相關流程。

## 通過條件

目前升級到 Paper `26.1.2` 沒有看到啟動層面的核心阻塞。若後續完整功能測試通過，正式 release 前至少需要完成：

- 將測試版本號 `26.1.2-0.1.0-dev` 調整為正式 release 版本。
- 確認 release jar 內 `plugin.yml` 的 `version` 與 `api-version` 正確。
- 更新 README / 文件中與 Paper、Java、依賴下載版本相關的內容。
- 使用 Paper `26.1.2` 測試服再次完成封裝與啟動驗證。
