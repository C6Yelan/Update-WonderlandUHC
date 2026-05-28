# WonderlandUHC 維護驗證細節說明

整理日期：2026-05-23

這份文件說明維護者修改 WonderlandUHC 後，應如何判斷需要跑哪些驗證。它不是一般開服教學，而是用來避免「編譯過了但插件無法啟動」或「啟動過了但跑圖 / 開局流程壞掉」。

## 驗證分級

| 修改類型 | 最低驗證 | 建議補充 |
| --- | --- | --- |
| 純文件 | `git diff --check` | 人工讀過相關段落。 |
| README / docs 連結 | `git diff --check` | 確認相對路徑存在。 |
| Java 程式碼 | 封裝 + 伺服器啟動 | 依功能跑對應流程。 |
| `build.gradle` / `plugin.yml` | 封裝 + 伺服器啟動 | 檢查 jar metadata 與依賴插件狀態。 |
| `src/main/resources/*.yml` | 封裝 + 伺服器啟動 | 若是文字格式，檢查 MiniMessage 與 placeholder。 |
| 選圖 / Chunky / cache | 封裝 + 伺服器啟動 | 實際跑 `/uhc regen`、`/uhc choose`、重啟接續。 |
| 開局 / timer / player state | 封裝 + 伺服器啟動 | 實際跑 `/uhc start` 到 `PLAYING`。 |
| DiscordSRV / voice | 封裝 + 伺服器啟動 | 需在 DiscordSRV ready 後驗證對應功能。 |

只改文件時不需要跑伺服器；只要改到程式碼、resource 預設設定或建置設定，就應該至少跑封裝與啟動測試。

## 基本檢查

```bash
git diff --check
```

用途：

- 檢查 trailing whitespace。
- 檢查 patch 格式問題。
- 在 docs-only 修改時作為最低驗證。

也建議看：

```bash
git status --short
git diff --stat
```

用來確認沒有混入不相關檔案。

## 封裝

維護者應確認目前環境使用 JDK 21，並以 Gradle wrapper 建置：

```bash
./gradlew clean test shadowJar --no-daemon -Dorg.gradle.native=false
```

若目前 shell 的預設 Java 不是 21，先指定 `JAVA_HOME`：

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean test shadowJar --no-daemon -Dorg.gradle.native=false
```

這個流程會：

1. 清理舊 build output。
2. 編譯 main / test。
3. 執行 Gradle test。
4. 產生 shadow jar。

若只想檢查 Java 21 環境：

```bash
java -version
javac -version
./gradlew --version
```

## 封裝結果

封裝成功後，應確認：

1. `build/libs/WonderlandUHC-*.jar` 存在。
2. jar 檔名版本符合 `build.gradle` 的 `version`。
3. `plugin.yml` 內版本已被 `processResources` 展開。

版本或 manifest 變動時可檢查：

```bash
unzip -p build/libs/WonderlandUHC-1.21.11-0.1.3.jar plugin.yml
```

重點看：

- `version:`。
- `softdepend:`。
- `api-version:`。

## 部署到測試服

部署到測試服時，請用可檢查的流程完成下列步驟：

1. 先停止測試伺服器。
2. 確認要部署的是最新 `build/libs/WonderlandUHC-*.jar`。
3. 將 jar 複製到測試服 `plugins/`。
4. 確認測試服沒有載入舊版本 jar。
5. 再使用測試伺服器資料夾內的 `start.bat` 啟動。

若測試服仍在執行，不要直接覆蓋 jar；這種部署狀態不能視為有效驗證。

## 啟動測試

伺服器啟動應使用測試伺服器資料夾內的 `start.bat`。

啟動後檢查 `logs/latest.log`。

最低通過條件：

1. Paper 版本符合目標線，例如 `1.21.11`。
2. Java 版本是 21。
3. LuckPerms 已啟用。
4. Chunky 已啟用。
5. WonderlandUHC 已啟用。
6. console 有 WonderlandUHC 依賴插件狀態。
7. `LuckPerms` 與 `Chunky` 顯示 `可用`。
8. 缺少 optional plugin 時只顯示 `未啟用`，不應造成 WonderlandUHC enable 失敗。
9. log 不應出現 `ERROR`、`SEVERE`、`NoClassDefFoundError`、`NoSuchMethodError`、`ClassCastException` 或未處理 exception。

如果修改牽涉 Chunky，還需要進一步跑正式預生成流程。

## Dependency Status 判讀

可接受的啟動狀態例子：

```text
依賴插件狀態:
- LuckPerms: 可用
- Chunky: 可用
- DiscordSRV: 未啟用 (未偵測到可用插件，相關功能會停用。)
```

這代表必要依賴正常，DiscordSRV 缺少但不阻止啟動。

不正常例子：

```text
- LuckPerms: 缺少 (必要插件未啟用，WonderlandUHC將停止啟用。)
- Chunky: 缺少 (必要插件未啟用，WonderlandUHC將停止啟用。)
```

這代表部署狀態不正確，不能視為通過啟動測試。

## 功能流程驗證

### 選圖與跑圖

若修改以下區域，應實際跑選圖與跑圖：

- `application.world`
- `CenterCleaner`
- `MatchCenter`
- `WorldLoadingCacheState`
- `ChunkPregenerationService`
- `ChunkPregenerationAdapter`
- `settings.yml` 的 CenterCleaner。

建議流程：

1. 啟動伺服器。
2. 使用 `/uhc regen`。
3. 選擇 CenterCleaner confirm 或 skip。
4. 等預覽世界完成，確認主持人被傳送到預覽中心。
5. 使用 `/uhc choose`。
6. 確認伺服器重啟並進入 `GENERATING`。
7. 等 Chunky 預生成完成。
8. 確認狀態保存為 `DONE` 並再次重啟。
9. 重啟後確認可進入等待開局階段。

若 Chunky 缺少，不要把這套流程視為通過。

### 開局流程

若修改以下區域，應實際跑開局流程：

- `Game`
- `StateName`
- `GameTimerRunnable`
- `game.state`
- `game.timer`
- `ScatterHandler`
- team split / teleport。

建議流程：

1. 在 `LoadingStatus.DONE` 後進入大廳。
2. 使用 `/uhc start`。
3. 確認大廳倒數開始。
4. 確認進入 `TELEPORTING` 並分批傳送。
5. 確認進入 `PRE_START` 並凍結玩家。
6. 倒數後確認進入 `PLAYING`。
7. 檢查傷害、PvP、final heal、border timer 是否依設定推進。

### 文字與設定

若修改 `messages.yml`、`commands.yml`、`gui.yml`、`items.yml`、`scoreboards.yml`、`scenarios.yml`、`broadcasts.yml` 或 `settings.yml`：

1. 封裝確認 resource 可讀。
2. 啟動確認靜態設定沒有 missing configuration error。
3. 檢查修改過的畫面、訊息或 item 實際顯示。
4. 若新增 placeholder，確認程式真的有替換。
5. 若新增 MiniMessage tag，確認顯示端支援。

文字格式細節見 `docs/details/text-format.md`，設定載入面見 `docs/details/config-surface.md`。

## 測試結果紀錄

完成較大修改時，建議在 PR、commit message 或 release note 中留下：

1. 封裝指令。
2. 是否跑 server startup。
3. 測試伺服器版本。
4. 依賴狀態。
5. 是否跑選圖 / 跑圖 / 開局。
6. 已知未驗證項目。

維護結論應明確區分「編譯通過」、「啟動通過」、「主流程通過」與「特定功能通過」。
