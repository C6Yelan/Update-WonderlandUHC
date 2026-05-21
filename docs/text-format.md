# WonderlandUHC 文字格式摘要

整理日期：2026-05-21

這份文件摘要說明新版本 WonderlandUHC 的文字格式規則。重點是讓委託人、原作者與後續維護者理解：新版正式文字格式以 Adventure MiniMessage 為準，舊版 `&a` / `§a` 色碼不再作為正式支援格式。

## 核心觀念

1. 預設設定檔中的玩家可見文字使用 MiniMessage。
2. 沒有顏色或樣式需求時，可以保留純文字。
3. `{player}`、`{time}`、`{scenario}` 這類大括號內容是 WonderlandUHC placeholder，不是 MiniMessage tag。
4. 舊版 `&a` / `§a` 色碼會照原字顯示，不會被當作顏色解析。
5. 設定檔中的文字建議使用雙引號包起來，降低 YAML 特殊字元造成格式錯誤的機率。
6. MiniMessage tag 建議成對關閉，例如 `<green>文字</green>`，避免後續文字被意外套用同一個樣式。

## MiniMessage 用法

常見格式：

```text
<red>紅色文字</red>
<green><bold>綠色粗體</bold></green>
<gray>玩家: </gray><green>{player}</green>
<#ffcc00>自訂色碼文字</#ffcc00>
```

在 YAML 設定檔中建議寫成：

```yaml
Name: "<green>遊戲設定</green>"
Lore:
  - "<gray>點擊開啟設定選單。</gray>"
```

常見色彩：

- `<black>`、`<dark_blue>`、`<dark_green>`、`<dark_aqua>`
- `<dark_red>`、`<dark_purple>`、`<gold>`、`<gray>`
- `<dark_gray>`、`<blue>`、`<green>`、`<aqua>`
- `<red>`、`<light_purple>`、`<yellow>`、`<white>`
- `<#rrggbb>` 十六進位色碼

常見樣式：

| 樣式 | 含意 |
| --- | --- |
| `<bold>` | 粗體。 |
| `<italic>` | 斜體。 |
| `<underlined>` | 底線。 |
| `<strikethrough>` | 刪除線。 |
| `<obfuscated>` | Minecraft 亂碼閃爍文字效果。 |
| `<reset>` | 重設前面套用的顏色與樣式。 |

## Placeholder 規則

WonderlandUHC 會在程式中替換 `{name}` 形式的 placeholder。

常見例子：

```text
<green>{player}</green><gray> 開啟了 </gray><green>{scenario}</green><gray>。</gray>
```

維護原則：

1. 不確定用途時，不要刪除既有 placeholder。
2. 可以移動 placeholder 的位置，但名稱必須保持一致。
3. placeholder 不是 MiniMessage tag，不要改成 `<player>`。
4. `{+name}`、`{name+}`、`{+name+}` 是保留空白用的 placeholder 變體，通常只在既有文字中維持即可。

## 設定檔範圍

主要會使用 MiniMessage 的預設設定檔：

| 檔案 | 文字範圍 |
| --- | --- |
| `messages.yml` | 一般玩家訊息、遊戲流程訊息、死亡訊息、登入 / 踢出訊息。 |
| `commands.yml` | 指令回覆、指令流程提示、指令列表。 |
| `gui.yml` | GUI title、button name、button lore。 |
| `items.yml` | 大廳、觀戰、staff 等工具物品名稱與 lore。 |
| `scenarios.yml` | scenario 顯示名稱、說明、警告與 scenario-specific 訊息。 |
| `scoreboards.yml` | scoreboard theme 與每一行顯示文字。 |
| `settings.yml` | 少量可見名稱文字，例如隊伍預設名稱、金頭顱名稱。 |
| `broadcasts.yml` | Discord 功能錯誤訊息與 Discord 公告格式。 |

`sounds.yml`、`spawns.yml`、`plugin.yml` 不是主要文字格式設定檔，不需要套用同一組 MiniMessage 說明。

## Discord 公告

`broadcasts.yml` 需要分開看：

- `Invalid_Channel` 這類遊戲內錯誤訊息使用 MiniMessage。
- `Discord.Formatting` 會送到 Discord，發送前會移除 Minecraft 文字顏色；這部分主要使用 Discord markdown、換行與 placeholder。

因此 `Discord.Formatting` 內可以保留 ` ``` `、`@everyone`、清單、分隔線等 Discord 文字格式，但不應依賴 MiniMessage 顏色顯示在 Discord。

## 維護原則

1. 新增玩家可見文字時，優先使用 MiniMessage。
2. 不新增 legacy `&` / `§` 色碼範例。
3. 修改文字時，同時確認 placeholder 是否仍與程式碼替換名稱一致。
4. 預設設定檔頂部的格式註解是給開服者看的，應保持簡短可讀。
5. 這份文件只作為維護摘要；release 使用者不應被要求先閱讀 `docs/` 才知道基本格式。

更完整的 MiniMessage、placeholder、YAML 與 Discord 格式細節，見 `docs/details/text-format.md`。
