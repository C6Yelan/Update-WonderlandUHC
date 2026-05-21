# WonderlandUHC 文字格式細節說明

整理日期：2026-05-21

這份文件補充 `docs/text-format.md` 的細節，說明 WonderlandUHC 在設定檔中如何處理 MiniMessage、placeholder、YAML 寫法與 Discord 公告格式。摘要文件只保留核心觀念；若需要修改大量文字、排查顯示問題或新增可見訊息，才需要閱讀本文件。

## 適用範圍

本文件主要適用於以下預設設定檔：

| 檔案 | 主要文字 |
| --- | --- |
| `messages.yml` | 玩家訊息、遊戲流程訊息、死亡訊息、登入 / 踢出訊息。 |
| `commands.yml` | 指令回覆、錯誤訊息、指令流程提示。 |
| `gui.yml` | GUI title、按鈕名稱、lore。 |
| `items.yml` | 大廳、觀戰、staff 工具物品名稱與 lore。 |
| `scenarios.yml` | scenario 名稱、說明、警告訊息與 scenario-specific 訊息。 |
| `scoreboards.yml` | scoreboard theme 與每一行顯示文字。 |
| `settings.yml` | 少量玩家可見名稱，例如隊伍預設名稱、金頭顱名稱。 |
| `broadcasts.yml` | Discord 功能錯誤訊息與 Discord 公告模板。 |

`sounds.yml` 是音效格式，`spawns.yml` 是座標資料，`plugin.yml` 是 Bukkit / Paper manifest；這些不屬於主要 MiniMessage 設定檔。

## 解析規則

WonderlandUHC 的文字處理集中在 `PluginText`。

主要行為：

1. 文字中包含正式支援的 MiniMessage 顏色、樣式或 hex tag 時，會用 MiniMessage 解析。
2. 沒有正式支援 tag 的文字會照普通文字顯示。
3. 舊 Bukkit 色碼，例如 `&a` 或 `§a`，會照原字顯示。
4. `{player}` 這類 placeholder 會先由 WonderlandUHC 替換，再交給顯示層處理。
5. 一般 placeholder 值會被 escape，避免玩家輸入的 `<red>` 被當成 MiniMessage tag。
6. 少數由程式內部明確標記為 formatted 的值，才會作為 MiniMessage 片段插入。

這代表設定檔可以安全使用：

```text
<gray>玩家: </gray><green>{player}</green>
```

但玩家名稱若剛好是 `<red>Alex`，正常情況下不會被解析成紅色。

## 正式支援的 tag

目前設定檔正式支援的格式以顏色、樣式與 hex 色碼為主。

### 顏色

| Tag | 顏色 |
| --- | --- |
| `<black>` | 黑色 |
| `<dark_blue>` | 深藍 |
| `<dark_green>` | 深綠 |
| `<dark_aqua>` | 深青 |
| `<dark_red>` | 深紅 |
| `<dark_purple>` | 深紫 |
| `<gold>` | 金色 |
| `<gray>` | 灰色 |
| `<dark_gray>` | 深灰 |
| `<blue>` | 藍色 |
| `<green>` | 綠色 |
| `<aqua>` | 青色 |
| `<red>` | 紅色 |
| `<light_purple>` | 淺紫 |
| `<yellow>` | 黃色 |
| `<white>` | 白色 |

### 樣式

| Tag | 效果 |
| --- | --- |
| `<bold>` | 粗體 |
| `<italic>` | 斜體 |
| `<underlined>` | 底線 |
| `<strikethrough>` | 刪除線 |
| `<obfuscated>` | Minecraft 亂碼閃爍文字 |
| `<reset>` | 重設前面套用的顏色與樣式 |

### Hex 色碼

可以使用六位十六進位色碼：

```text
<#ffcc00>金黃色文字</#ffcc00>
```

建議 hex 字母使用小寫，與程式目前的偵測規則保持一致。

## Tag 寫法建議

建議 tag 成對關閉：

```text
<green>成功</green>
<gray>玩家: </gray><green>{player}</green>
```

不建議依賴未關閉 tag 的延續效果：

```text
<green>成功
```

雖然部分情況仍可能被解析，但後續修改時容易讓整段文字被意外套用同一個顏色或樣式。

巢狀 tag 可以使用，但要保持關閉順序清楚：

```text
<green><bold>遊戲開始</bold></green>
```

如果只想讓一小段文字套用樣式，請只包住該段：

```text
<gray>主持人: </gray><gold>{host}</gold>
```

## 不建議使用的格式

不要在新設定中使用舊 Bukkit 色碼：

```text
&a成功
§c錯誤
```

這些內容不會被轉成顏色，而會顯示成普通文字。

也不要把 WonderlandUHC placeholder 改成 MiniMessage tag：

```text
<player>
<time>
```

正確寫法是：

```text
{player}
{time}
```

`<seed>` 這類不是正式支援格式的角括號文字，通常會被視為普通文字；但為了避免混淆，設定檔中若要表示參數，仍建議使用 `{seed}` 或直接寫成文字說明。

## Placeholder 規則

WonderlandUHC 使用 `{name}` 形式的 placeholder。

常見例子：

```text
<green>{player}</green><gray> 開啟了 </gray><green>{scenario}</green><gray>。</gray>
```

維護原則：

1. 不確定用途時，不要刪除既有 placeholder。
2. 可以移動 placeholder 的位置，但名稱必須保持一致。
3. placeholder 名稱大小寫與底線、連字號都要保留。
4. placeholder 不是 MiniMessage tag，不要改成 `<player>`。

### 空白控制變體

`PluginText` 支援幾種 placeholder 空白控制寫法：

| 寫法 | 含意 |
| --- | --- |
| `{name}` | 直接替換。 |
| `{name+}` | 若值不為空，替換後方加一個空白。 |
| `{+name}` | 若值不為空，替換前方加一個空白。 |
| `{+name+}` | 若值不為空，前後各加一個空白。 |

用途是避免可選文字為空時留下多餘空白。

範例：

```text
<gray>隊伍:</gray>{+team}
```

若 `{team}` 是空值，整段不會留下多餘空白；若 `{team}` 有值，顯示時會在隊伍名稱前補一個空白。

通常只需要保留既有寫法，不建議隨意改成空白控制變體。

## YAML 寫法

設定檔文字建議使用雙引號包起來：

```yaml
Name: "<green>遊戲設定</green>"
Lore:
  - "<gray>點擊開啟設定選單。</gray>"
```

原因：

1. MiniMessage 會使用 `<`、`>`、`#` 等符號。
2. YAML 中的 `:`、`#`、空白與特殊符號可能影響解析。
3. 引號能降低設定檔被誤判成其他 YAML 語法的風險。

### 多行文字

多行訊息通常使用 YAML list：

```yaml
Respawned:
  - "<green><bold>生命延續</bold></green>"
  - " "
  - "<aqua>你獲得了繼續遊玩的機會。</aqua>"
```

空白行可以保留成：

```yaml
- " "
```

不要使用 tab 縮排，請使用空白縮排。

### 常見 YAML 錯誤

不建議：

```yaml
Message: <green>成功</green>
```

建議：

```yaml
Message: "<green>成功</green>"
```

不建議：

```yaml
Lore:
- "<gray>沒有縮排</gray>"
```

建議：

```yaml
Lore:
  - "<gray>有縮排</gray>"
```

不建議使用智慧引號：

```yaml
Name: “<green>遊戲設定</green>”
```

請使用一般英文雙引號：

```yaml
Name: "<green>遊戲設定</green>"
```

## 不同設定檔的注意事項

| 檔案 | 注意事項 |
| --- | --- |
| `messages.yml` | 多數玩家訊息會套用 MiniMessage；死亡訊息、登入提示、流程廣播都在這裡。 |
| `commands.yml` | 指令錯誤與提示文字常有 `{label}`、`{cmd}`、`{player}`，改文字時要保留 placeholder。 |
| `gui.yml` | `Title`、`Name`、`Lore` 會顯示在 inventory GUI；物品名稱和 lore 預設會關閉 Minecraft 物品斜體。 |
| `items.yml` | 工具物品名稱與 lore 使用 MiniMessage；`Type`、`Slot` 不是文字格式。 |
| `scenarios.yml` | `Name`、`Description`、警告訊息可使用 MiniMessage；`*_Sound` 欄位是音效格式，不是文字。 |
| `scoreboards.yml` | 每一行都可使用 MiniMessage 與 scoreboard placeholder；行數與長度仍需考慮 Minecraft 顯示限制。 |
| `settings.yml` | 大多是數值或開關，只有少量可見名稱文字需要 MiniMessage。 |
| `broadcasts.yml` | 遊戲內錯誤訊息可用 MiniMessage；Discord 公告模板需參考下方 Discord 規則。 |

## Discord 公告格式

`broadcasts.yml` 中的 Discord 區塊需要分成兩類：

```yaml
Discord:
  Invalid_Channel: "<red>抱歉，此Discord頻道並不存在。</red>"
  Formatting:
    - "```"
    - "主持人: {host}"
    - "伺服器IP: {ip}"
    - "```@everyone"
```

`Invalid_Channel` 這類錯誤訊息會回到遊戲內，因此可以使用 MiniMessage。

`Discord.Formatting` 會送到 Discord。送出前，WonderlandUHC 會把 Minecraft 顏色與樣式轉成純文字，因此：

1. 不要依賴 MiniMessage 顏色顯示在 Discord。
2. 可以使用 Discord markdown，例如 code block、清單與 `@everyone`。
3. 可以使用 WonderlandUHC placeholder，例如 `{host}`、`{join_time}`、`{scenarios}`。
4. 若 placeholder 內容本身帶有 Minecraft 顏色，送出前也會被轉成純文字。

## 程式內部 formatted 值

部分狀態文字由程式內部提供 MiniMessage 片段，例如：

```text
<gray>狀態: </gray>{status}
```

如果 `{status}` 由程式標記為 formatted，可能會被替換成：

```text
<green>On</green>
```

這是為了讓像啟用 / 停用、隊伍顏色、內部狀態這類受控值可以保留顏色。一般玩家輸入或一般 placeholder 值不會這樣處理，會被 escape 成普通文字。

## Click / Hover 互動文字

目前設定檔正式說明範圍以顏色、樣式與 placeholder 為主。

點擊指令、hover 提示這類互動效果，多數由 Java 程式碼建立 Adventure `ClickEvent` / `HoverEvent`，不建議直接在 YAML 裡自行加入 MiniMessage click / hover tag。

若未來要把互動文字開放給設定檔，應先新增明確的設定格式、測試與文件，不要只在現有 YAML 中嘗試加入新 tag。

## 修改檢查清單

修改文字設定後，建議檢查：

1. YAML 是否仍能正常載入。
2. MiniMessage tag 是否成對關閉。
3. placeholder 名稱是否仍與原本一致。
4. 是否誤用了舊 Bukkit 色碼。
5. `sounds.yml`、`*_Sound`、`Type`、`Slot` 這類非文字欄位是否沒有被誤改。
6. 多行 list 是否維持正確縮排。
7. Discord 公告是否沒有依賴 Minecraft 顏色。

若修改大量預設設定檔，建議重新封裝並用測試伺服器啟動一次，確認 `uhc reload` 與主要 GUI / 指令訊息沒有載入錯誤。
