# Paper 26 開發環境試推紀錄

## 隔離方式

- 分支：`experiment/update-to-26`
- 資料夾：`/home/ayaya/projects/Minecraft-Update-Plugin/Update-WonderlandUHC-26`
- 來源基線：`main` 的 `v1.21.11-0.1.3` 狀態
- 原本 `Update-WonderlandUHC` 的 Paper `1.21.11` 開發環境未被覆蓋或替換。

## 版本基線

依 PaperMC 目前文件與 26.1 公告，26 線不再使用舊的 `-R0.1-SNAPSHOT` API 版本格式，改用 `26.1.2.build.+` 這類 build 版本；Paper 26.1.2 的 Gradle 範例也改用 Java toolchain `25`。

本分支先只調整建置基線：

- `build.gradle`
  - 專案版本：`26.1.2-0.1.0-dev`
  - Java toolchain：`25`
  - `options.release`：`25`
  - Paper API：`io.papermc.paper:paper-api:26.1.2.build.+`
  - Shadow plugin：`9.4.2`
- `gradle/wrapper/gradle-wrapper.properties`
  - Gradle wrapper：`9.5.1`
- `plugin.yml`
  - `api-version: '26.1.2'`

## 初次驗證紀錄

### 2026-05-29 第一次封裝

指令使用既有腳本，並將 `PLUGIN_DIR` 指到新 worktree：

```bash
PLUGIN_DIR=/home/ayaya/projects/Minecraft-Update-Plugin/Update-WonderlandUHC-26 \
LOCAL_GRADLE_HOME=/home/ayaya/projects/Minecraft-Update-Plugin/Update-WonderlandUHC-26/.gradle-java25-local \
LOCAL_M2_REPO=/home/ayaya/projects/Minecraft-Update-Plugin/Update-WonderlandUHC-26/.m2-java25-local \
GRADLE_EXTRA_ARGS=-Dorg.gradle.java.installations.paths=/home/ayaya/.jdks/corretto-25.0.2,/home/ayaya/.jdks/corretto-21 \
bash scripts/Update-WonderlandUHC/package-plugin.sh
```

結果：失敗於 `shadowJar`。

實際錯誤：

```text
Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 69
```

判斷：

- Java 25 編譯已通過，阻塞點不是插件原始碼。
- 舊組合 `Gradle 8.10.2` + `com.gradleup.shadow 8.3.6` 在產生 shadow jar 時無法處理 Java 25 bytecode。
- Paper 26 線要繼續封裝，需要同步升級 Gradle wrapper 與 Shadow plugin。

### 2026-05-29 第二次封裝

調整：

- Gradle wrapper：`8.10.2` -> `9.5.1`
- Shadow plugin：`8.3.6` -> `9.4.2`

結果：成功。

產物：

```text
build/libs/WonderlandUHC-26.1.2-0.1.0-dev.jar
```

Jar 內 `plugin.yml` 已確認：

```yaml
version: 26.1.2-0.1.0-dev
api-version: '26.1.2'
```

保留警訊：

- Lombok 在 Java 25 編譯時觸發 `sun.misc.Unsafe::objectFieldOffset` 終端棄用警告。
- Gradle 9.5.1 回報目前 build 仍使用 deprecated Gradle features，未來升 Gradle 10 前需要再拆開看 `--warning-mode all`。

### 2026-05-29 第一次 Paper 26.1.2 啟動

測試服：

```text
/mnt/c/Users/a0919/OneDrive/桌面/Minecraft local server/check/paper-26.1.2
```

此資料夾是新建的 Paper 26.1.2 測試服，未覆蓋既有 `paper-1.21.11` 或 `paper-1.21.11-clean-no-optional`。

內容：

- `server.jar`：Paper `26.1.2` build `66`
- `plugins/WonderlandUHC.jar`：本分支封裝產物
- `plugins/LuckPerms-Bukkit-5.5.0.jar`
- `start.bat`：使用 Windows Java `25.0.2` 啟動

結果：Paper 啟動到 `Done`，但 WonderlandUHC 自行停用。

關鍵 log：

```text
[WonderlandUHC] Enabling WonderlandUHC v26.1.2-0.1.0-dev
[WonderlandUHC] - Chunky: 缺少 (必要插件未啟用，WonderlandUHC將停止啟用。)
[WonderlandUHC] Disabling WonderlandUHC v26.1.2-0.1.0-dev
Done (31.454s)! For help, type "help"
```

判斷：這是測試服依賴不完整，不是 Paper 26 API 編譯問題。

### 2026-05-29 第二次 Paper 26.1.2 啟動

補上：

```text
plugins/Chunky-Bukkit-1.4.40.jar
```

結果：成功啟動，WonderlandUHC 維持啟用，並由 `start.bat` 啟動後透過 stdin `stop` 正常關閉。

關鍵 log：

```text
[PluginInitializerManager] Bukkit plugins (3):
 - Chunky (1.4.40), LuckPerms (5.5.0), WonderlandUHC (26.1.2-0.1.0-dev)
[WonderlandUHC] Enabling WonderlandUHC v26.1.2-0.1.0-dev
Version: 26.1.2-0.1.0-dev
Done (12.863s)! For help, type "help"
Stopping the server
[WonderlandUHC] Disabling WonderlandUHC v26.1.2-0.1.0-dev
```

啟動後確認 `25571` port 已釋放。

## 目前遇到的問題

1. 只改 Paper API 與 Java toolchain 不夠，舊 `Shadow 8.3.6` 會在 `shadowJar` 讀 Java 25 bytecode 時失敗。
2. Paper 26 測試服必須補齊必要依賴 `Chunky`，否則 WonderlandUHC 會按既有保護邏輯停用。
3. WonderlandUHC 啟動時列出的依賴下載連結仍指向 `g=1.21.11`，在 Paper 26 分支需要改成 26 線資訊或改成不綁版本的連結。
4. 經 WSL 啟動 Windows `start.bat` 時，console 對彩色中文輸出會有亂碼；`logs/latest.log` 內容正常。
5. Paper 26.1.2 新世界資料已使用 `world/dimensions/minecraft/...` 結構，`uhc_practice` 也出現在該結構下；後續涉及世界路徑的功能要用這個測試服驗證。

## 待觀察風險

- `Gradle 9.5.1` 能封裝目前專案，但 Gradle 10 前仍有 deprecated feature 警告要清。
- Paper 26.1 的世界儲存結構有變更，後續伺服器實測時需要特別觀察插件涉及世界資料夾、地圖挑選、`uhc_practice` 與 `uhc_world` 類流程的行為。
- Paper 26.1 文件提到世界名稱相關 API 正在轉向 key-based API，後續若編譯或 runtime log 出現相關警告，應優先從世界建立、載入與邊界邏輯追蹤。
- 外部插件是否正式標示支援 Paper 26.1.2 仍需要另外確認，尤其是必要依賴 `LuckPerms`、`Chunky`，以及可選的 `DiscordSRV`。
