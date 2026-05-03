# WonderlandUHC 基線

這份文件記錄 `steps.md` 第 0 步的可回歸基線與高風險依賴。升級到 Java 21 / Paper 1.21.11 前，先用這裡的命令確認舊基線仍可重現。

## 2026-05-03 基線

- 主插件 repo：`Update-WonderlandUHC`，分支 `update-to-1.21`
- Foundation repo：預設相鄰目錄 `../lib-foundation`，分支 `work`；腳本可用 `FOUNDATION_DIR` 覆蓋。
- 舊版基線使用的 Java：Corretto 8，`1.8.0_482`
- 舊版 Minecraft/Paper 基線：Paper `1.16.5`
- 輸出 jar：`build/libs/WonderlandUHC-1.0.0-alpha-2.jar`

目前等價回歸命令：

```bash
bash scripts/package-plugin.sh --skip-foundation --no-clean
```

結果：

- `lib-foundation` Maven 打包：通過
- WonderlandUHC Gradle `test`：通過
- WonderlandUHC Gradle `shadowJar`：通過
- Windows Paper `1.16.5` server 可用 `127.0.0.1:25566` 連線。
- Windows Paper `1.16.5` server 可載入 `WonderlandUHC`，且伺服器到達 `Done`。

沙盒注意事項：

- Gradle 6.9 會透過本機通訊端啟動背景程序。如果在受限沙盒中執行這個命令，可能會在編譯前因 `java.net.SocketException: Operation not permitted` 失敗。這是環境限制，不是專案基線失敗。

## 目前測試拓樸

目前人工進服測試已改成 Windows 開服：

```text
C:\Users\a0919\OneDrive\桌面\Minecraft local server\paper-1.16.5
C:\Users\a0919\OneDrive\桌面\Minecraft local server\paper-1.21.11
```

連線位址：

```text
127.0.0.1:25566  # Paper 1.16.5 + WonderlandUHC
127.0.0.1:25567  # Paper 1.21.11 pure server
```

這個方向取代原本以 WSL 測試服作為人工進服環境的做法，避免 WSL2 IP 變動與 Windows portproxy 問題。WSL 仍負責封裝、單元測試與可選的非互動 smoke test。

2026-05-03 已確認：

- WSL `package-plugin.sh --skip-foundation --no-clean` 通過，`compileJava`、`compileTestJava`、`test`、`shadowJar` 都成功。
- Windows `127.0.0.1:25566` 可連到 Paper 1.16.5 server。
- Windows `127.0.0.1:25567` 可連到 Paper 1.21.11 server。
- WSL `.local-test-server` 與 `.local-test-server-1.21.11` 已刪除，避免和 Windows 測試服混淆。

## 目前回歸命令

封裝並部署到 Windows 1.16.5 測試服：

```bash
bash scripts/deploy-to-windows-server.sh
```

只封裝、不部署：

```bash
bash scripts/package-plugin.sh --skip-foundation --no-clean
```

目前的 1.21.11 Windows server 檢查結果：

- Paper `1.21.11-130` 下載成功，sha256 為 `25eb85bd8415195ce4bc188e1939e0c7cef77fb51d26d4e766407ee922561097`。
- Paper `1.21.11` 使用 Windows Java `25.0.2` 在連接埠 `25567` 啟動。
- `not updated in a while` 警告已消失；若 Paper 顯示 1.21.11 已是該 Minecraft 版本最新 build、但落後更新的 stable release，代表要改升級目標版本，而不是更換同版本 jar。
- WonderlandUHC 目前不部署到 1.21.11 server，因為目前插件仍是 1.16 時期的 jar，已知會失敗：
  - Foundation `Remain` 嘗試使用舊 NMS 類別，例如 `net.minecraft.server.Entity`。
  - Foundation 設定與新版內建 SnakeYAML API 不相容而失敗：`SafeConstructor: method 'void <init>()' not found`。

這確認了 1.21.11 server 可以作為新版 Paper 人工進服環境，但插件相容性工作仍未完成。請把它視為收斂目標，而不是已完成的 1.21.11 基線。

Paper server jar 更新時請以 Fill v3 API 為準，例如 `https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds`；舊的 v2 API 可能回傳過時 build 清單。

## 自動化邊界

目前保留腳本已自動化：

- WSL build / test / shadowJar。
- 將 jar 複製到 Windows 1.16.5 server 的 `plugins/WonderlandUHC.jar`。
- 部署前檢查 Windows `25566` 是否仍在監聽，避免覆蓋正在執行的 server jar。

可再自動化但目前刻意不做：

- 停止與啟動 Windows Java server。
- 檢查 `127.0.0.1:25566` / `25567` port。
- 檢查 `logs/latest.log` 是否出現 `Done (` 與插件啟用訊息。

目前不應把「玩家實際登入、操作 UI、走完整 UHC 流程」宣稱為已自動化。除非後續導入 Minecraft protocol bot 或專門 E2E 測試，這部分仍是人工驗收。

## 高風險依賴

| 依賴 | 目前建置 / 執行期角色 | 第 0 步風險標記 |
| --- | --- | --- |
| Foundation | 主插件將 Foundation `6.0.8` shade 進輸出；本機開發可從 `lib-foundation` 使用 `-PuseLocalFoundation`。 | 仍是範圍很廣的相容層。新程式碼不應加深對 Foundation 的直接耦合。 |
| DatouNms | `implementation 'com.github.lulu2002:DatouNms:1.2.2'`；`WonderlandUHC.setupNms()` 會在不支援的版本拋出錯誤。 | 在封裝或移除前，這是 1.21.11 的硬性執行期阻礙。 |
| PacketListenerAPI | `compileOnly` jar，且執行期會透過 `Dependency.PACKET_LISTENER_API.check()` 做硬性檢查。 | 宣告為 `softdepend`，但行為像必要依賴。目標冒煙測試要能在沒有外部插件時通過，它必須先變成可選。 |
| WorldBorder | `compileOnly` JitPack 依賴，且執行期會透過 `Dependency.WORLD_BORDER.check()` 加上版本檢查做硬性檢查。 | 宣告為 `softdepend`，但行為像必要依賴。核心邊界行為必須移到內部服務。 |
| custom-ore-generator | `compileOnly` jar；只有掛接時才會檢查，但世界初始化與生成器 UI 仍匯入它的 API。 | 可選整合必須留在核心啟動與世界生命週期之外。 |
| DiscordSRV | `compileOnly`；只有存在時才會掛接。 | 保持可選；語音整合不應阻止 UHC 啟動。 |

## 第 0 步護欄

- 重構時至少保持 `bash scripts/package-plugin.sh --skip-foundation --no-clean` 通過。
- 需要人工進服測試時，執行 `bash scripts/deploy-to-windows-server.sh`，再手動重啟 Windows 1.16.5 server。
- 不同機器可用 `FOUNDATION_DIR`、`WINDOWS_SERVER_DIR`、`LOCAL_M2_REPO`、`LOCAL_GRADLE_HOME` 等環境變數覆蓋本機路徑。
- 優先採用可編譯的小切片，再移動到下一個架構步驟。
- 如果切換 Java 21 / Paper 1.21.11 依賴時產生預期中的編譯錯誤，請把它們記錄為收斂清單，不要與無關的行為變更混在一起。
- 除非明確提升為儲存庫設定，否則將僅限本機使用的 `gradle.properties` 排除在共享提交之外。
