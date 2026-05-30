# WonderlandUHC 1.21.11-0.1.4

## 支援環境

- Minecraft Paper `1.21.11`
- Java `21`

## 主要更新

- 修正邊界收縮速度未依臨時設定加速或減速的問題，收縮時間與速度計算已對齊 `1.16` 原作者實作。
- 修正移動邊界期間記分板顯示與實際世界邊界差距過大的問題，記分板會讀取目前實際 worldborder 大小。
- 修正新版 Paper `WorldBorder#changeSize` 使用 ticks 的差異，避免收縮時間被錯誤縮短。
- 調整 UHC 世界初始邊界設定，固定邊界會維持與 `1.16` 一致的設定值 `+2` 行為。
- 修正世界邊界警示不可見的問題，保留玩家接近邊界時可見的原版警示效果。
- 修正 NoClean 倒數與記分板顯示可能不同步的問題。
- 修正開始遊戲後主世界、地獄與終界時間未歸零，以及幻翼生成規則未關閉的問題。
- 修正玩家加入訊息中的線上人數顯示，使其改為目前人數與伺服器上限。
- 修正金頭判定、重生/死亡位置與 Discord 語音等待流程的相容性問題。
- 修正設定選單儲存後快取未同步，造成切換選單或重新開啟時顯示舊值的問題。

## 移除或調整的舊功能

- 移除先前會重設獨立大廳世界 worldborder 的流程，避免 UHC 邊界設定影響大廳世界。
- 邊界操作現在只會套用到 `uhc_world` 與 `uhc_world_nether`。

## 注意事項

- LuckPerms 是 runtime 必要依賴，缺少時 WonderlandUHC 會停止啟用。
- Chunky 是 runtime 必要依賴，缺少時 WonderlandUHC 會停止啟用。
- DiscordSRV 是可選依賴；未安裝時 Discord 公告與語音相關功能會停用。
- 伺服器既有的 `plugins/WonderlandUHC/` 設定檔不會被插件自動覆蓋。如果更新後仍看到舊 GUI、舊指令說明或舊教學文字，請先關閉伺服器並備份，再視需求刪除 `gui.yml`、`commands.yml`、`messages.yml`、`settings.yml` 等文字設定檔，重新啟動後讓插件產生新版文本。

## 已知問題

- 開始遊戲後，臨時調整邊界無法作用，請使用原版指令。

## 檔案資訊

- jar 檔案：`WonderlandUHC-1.21.11-0.1.4.jar`
- jar SHA-256：`6a3439ac139e0b686575b364196cf63d4048a039cfade24ea71cc67286b71f1a`
- 對應 commit：`v1.21.11-0.1.4` tag 指向的 commit
