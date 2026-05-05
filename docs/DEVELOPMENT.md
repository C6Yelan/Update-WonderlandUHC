# WonderlandUHC 開發操作手冊

這份文件只保留目前仍有效的本機開發、封裝與測試流程。升級與重構施工計畫請看 `steps.md`。
目前第 0 步基線與高風險依賴紀錄請看 `BASELINE.md`。

## 專案結構

- `./`：WonderlandUHC 主插件，Gradle 專案。
- `../lib-foundation/`：舊 Foundation 相容層，Maven 專案，路徑可用 `FOUNDATION_DIR` 覆蓋。
- `scripts/`：本機封裝、測試服、smoke test 與清理腳本。

目前正式升級前的可回歸基線仍是 Java 8 / Paper 1.16.5；`steps.md` 的目標狀態是 Java 21 / Paper 1.21.11。

## 目前開發流程

目前採用「WSL 開發與封裝、Windows 開服與人工測試」：

1. 在 WSL 修改程式與執行 Gradle/Maven。
2. 在 WSL 產出 WonderlandUHC shadow jar。
3. 將 jar 複製到 Windows 測試服的 `plugins/`。
4. 從 Windows 啟動 Paper server，Minecraft client 直接連 `127.0.0.1`。

這個流程避免 WSL2 IP 變動、Windows portproxy 與 localhost forwarding 問題。

日常封裝並部署到 Windows 1.16.5 測試服：

```bash
bash scripts/deploy-to-windows-server.sh
```

這個腳本會執行快速封裝，然後將最新 jar 複製到：

```text
C:\Users\a0919\OneDrive\桌面\Minecraft local server\paper-1.16.5\plugins\WonderlandUHC.jar
```

它不會自動啟動、停止或重啟伺服器。部署前請先停止 Windows 的 `paper-1.16.5` server，部署後再手動開啟 `start.bat`。
若 `25566` 仍在監聽，腳本會拒絕覆蓋 jar，避免 Windows 鎖檔或部署後未生效。

不同機器或不同測試服位置可用參數或環境變數覆蓋：

```bash
WINDOWS_SERVER_DIR="/mnt/c/path/to/paper-1.16.5" bash scripts/deploy-to-windows-server.sh
bash scripts/deploy-to-windows-server.sh --server-dir "/mnt/c/path/to/paper-1.16.5"
```

完整重新封裝並部署：

```bash
bash scripts/deploy-to-windows-server.sh --full-build
```

產物會在：

```text
build/libs/WonderlandUHC-1.0.0-alpha-2.jar
```

只封裝、不部署：

```bash
bash scripts/package-plugin.sh --skip-foundation --no-clean
```

Java 21 / Paper 1.21.11 升級線的過渡封裝入口：

```bash
bash scripts/package-plugin-1.21.sh --check-env
bash scripts/package-plugin-1.21.sh --skip-foundation --no-clean
```

這個腳本固定使用 WSL JDK 21，預設路徑為 `/home/ayaya/.jdks/corretto-21`，並使用 Gradle `8.10.2`、Shadow `8.3.6`、Lombok `1.18.44`、Paper API `1.21.11-R0.1-SNAPSHOT`、`.gradle-java21-local/` 與 `.m2-java21-local/`，避免污染 Java 8 / 1.16.5 基線快取。`--check-env` 只驗證 Java 21 與本機快取路徑，不會封裝。MockBukkit v1.16 不支援 Paper API `1.21.11`，升級線測試先保留純單元測試，Bukkit runtime 由真 Paper smoke test 驗證。

## 腳本職責

- [常用] `scripts/deploy-to-windows-server.sh`：封裝 WonderlandUHC 並複製 jar 到 Windows 1.16.5 測試服；不控制伺服器啟停。
- [常用] `scripts/package-plugin.sh`：刷新 Foundation 相容依賴、建置 `lib-foundation`、執行 Gradle test、產生 shadow jar。
- [常用] `scripts/package-plugin-1.21.sh`：使用 WSL JDK 21 與獨立本機快取執行升級線封裝；目前屬 Step 4 過渡入口。
- [維護] `scripts/bootstrap-foundation-deps.sh`：準備 `lib-foundation` 需要的本機 Maven 相容依賴。
- [維護] `scripts/clean-workspace.sh`：清理 WSL 建置產物與本機快取；不清 Windows 測試服。

常用可覆蓋環境變數：

- `FOUNDATION_DIR`：指定 `lib-foundation` repo 位置，預設 `../lib-foundation`。
- `LOCAL_M2_REPO`：指定本機 Maven alias repository，預設 `.m2-local`。
- `LOCAL_GRADLE_HOME`：指定本機 Gradle user home，預設 `.gradle-local`。
- `JAVA21_HOME`：指定 WSL JDK 21 位置，`scripts/package-plugin-1.21.sh` 預設 `/home/ayaya/.jdks/corretto-21`。
- `WINDOWS_SERVER_DIR`：指定 Windows 測試服目錄，預設目前本機 1.16.5 測試服路徑。
- `WINDOWS_SERVER_PORT`：指定部署前 port 檢查，預設 `25566`。

## Windows 測試服

Windows 測試服位置：

```text
C:\Users\a0919\OneDrive\桌面\Minecraft local server\paper-1.16.5
C:\Users\a0919\OneDrive\桌面\Minecraft local server\paper-1.21.11
```

連線位址：

```text
127.0.0.1:25566  # Paper 1.16.5 + WonderlandUHC
127.0.0.1:25567  # Paper 1.21.11 pure server
```

Java 注意事項：

- `paper-1.16.5/start.bat` 固定使用 Windows Java 8：`C:\Program Files (x86)\Java\jre1.8.0_491\bin\java.exe`
- `paper-1.21.11/start.bat` 固定使用 Windows Java 21：`C:\Program Files\Java\jdk-21.0.11\bin\java.exe`
- WSL 的 Java 21 封裝入口使用 `/home/ayaya/.jdks/corretto-21`；Windows Java 21 只負責開啟 Paper server，不負責 WSL 封裝。
- `paper-1.21.11/server.jar` 使用 Paper `1.21.11-130` stable build；更新時以 Fill v3 API 為準：`https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds`。
- 如果 Paper 提示 1.21.11 已是該 Minecraft 版本最新 build、但落後更新的 stable release，這不是 jar 過舊，而是升級目標版本本身需要重新決定。
- `C6Yelan` 已寫入兩套 server 的 `ops.json`。

目前 1.21.11 server 可放入升級線 jar 做 smoke test；預期結果是 Paper server 進入 `Done` 並嘗試載入 WonderlandUHC，但插件仍會在 Foundation/NMS/SnakeYAML 相容性處停用。升級收斂完成前，這個 smoke test 用來確認 build/platform baseline，不能視為完整遊戲流程驗收。

## 自動化可行性

目前保留的腳本已自動化到「封裝、部署 jar」：

- WSL 執行 `package-plugin.sh`。
- WSL 將 jar 複製到 `/mnt/c/.../paper-1.16.5/plugins/WonderlandUHC.jar`。
- 升級線可用 `WINDOWS_SERVER_DIR` 與 `WINDOWS_SERVER_PORT` 指向 `paper-1.21.11` 後部署 jar。
- 部署前檢查 Windows `25566` 是否仍有 server 在監聽；有的話會要求先手動關服。

可以再自動化但目前刻意不做：

- 停止 / 啟動 Windows Java server。
- 用 PowerShell `Test-NetConnection 127.0.0.1 -Port 25566` 檢查連線。
- 讀取 Windows server `logs/latest.log`，確認出現 `Done (` 與 WonderlandUHC 啟用訊息。

「玩家真的能完整進入遊戲並操作」仍屬人工驗收，除非後續引入 Minecraft protocol bot 或專門的端到端測試工具。

## 1.21.11 收斂提醒

目前 `paper-1.21.11` Windows server 可用來放入升級線 jar 做 smoke test，但 WonderlandUHC 仍會在 Foundation/NMS/SnakeYAML 相容性處失敗。等後續升級相容性完成後，再把部署腳本預設目標收斂到新版 server。

## 依賴原則

- `WorldBorder`、`PacketListenerAPI`、`custom-ore-generator`、`DiscordSRV` 都應視為選配 integration。
- 沒有選配依賴時，插件至少要能啟動核心 UHC 流程。
- 任何新跨版本 API 都應放進 `port/`、`platform/`、`integration/` 或 `legacy/`，不要散進 core/application。
- `gradle.properties` 若用於本機 `useLocalFoundation=true`，保持本機狀態；它已被 `.gitignore` 排除，不要當成必須提交的共享設定。

## 清理

```bash
bash scripts/clean-workspace.sh
```

深度清理：

```bash
bash scripts/clean-workspace.sh --deep
```

深度清理會移除更多本機快取與測試服資料，使用前確認不需要保留本機測試世界。
