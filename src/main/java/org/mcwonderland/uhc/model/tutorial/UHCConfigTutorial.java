package org.mcwonderland.uhc.model.tutorial;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.References;
import org.mcwonderland.uhc.model.tutorial.model.Tutorial;
import org.mcwonderland.uhc.model.tutorial.model.TutorialSection;

public class UHCConfigTutorial extends Tutorial {

    public UHCConfigTutorial(Player player) {
        super(player);
    }

    @Override
    protected TutorialSection getFirstSection() {
        return new Intro();
    }


    private class Intro extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<gold><bold>歡迎來到WonderlandUHC教學 - 設定部分</bold></gold>",
                    " ",
                    "以下將會教你如何從頭開始，完整設定出屬於自己的UHC。",
                    "在進行過程中，你隨時可以輸入 <aqua><bold>exit</bold></aqua> <white>來停止教學</white>",
                    " ",
                    "<yellow>如果你已經準備好了，請輸入任意字元前往下個教學…</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new EditorMenuTutorialSection();
        }
    }

    private class EditorMenuTutorialSection extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "首先，請透過輸入 <yellow>/uhc edit</yellow> <white>來開啟設定介面。</white>",
                    " ",
                    "你會發現，設定介面有許多功能",
                    "請你花個時間，<gold>由左上往右下一一點擊每個物品</gold><white>，並查看其功能使用方式。</white>",
                    " ",
                    "了解插件的使用方式<gold><bold>相當重要</bold></gold><white>，在嘗試的過程來了解如何使用插件，</white><aqua><bold>日後遊戲主持起來方能得心應手</bold></aqua>",
                    "還請你在此階段要<light_purple><bold>保持耐心</bold></light_purple><white>，仔細的閱讀每個物品的說明文字並嘗試調整看看。</white>",
                    " ",
                    "<yellow>了解每個設定物品的用途後，請輸入任意字元前往下個教學...</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new AnyQuestions();
        }
    }

    private class AnyQuestions extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<blue>到這裡有任何問題嗎?</blue>",
                    " ",
                    "如果不是很了解某些功能如何使用，可先至<click:open_url:'" + References.DOWNLOAD_LINK + "'><aqua>插件下載頁面</aqua></click><white>查看專案資訊。</white>",
                    "連結: <click:open_url:'" + References.DOWNLOAD_LINK + "'><dark_aqua>" + References.DOWNLOAD_LINK + "</dark_aqua></click>",
                    " ",
                    "<yellow>查看後，請輸入任意字元前往下個教學...</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new TemplateTutorialSection();
        }
    }

    private class TemplateTutorialSection extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "接著，這邊想跟你隆重介紹設定頁面裡的<yellow><bold>模板功能</bold></yellow>",
                    " ",
                    "如同該設定物品的說明文字所述，此功能可以<green>儲存你設定好的模板</green><white>，並於日後</white><green>一鍵載入現在的設定</green><white>。</white>",
                    "如果你還未嘗試過，不仿現在使用看看吧！",
                    " ",
                    "先使用 <yellow>/uhc edit</yellow> <white>，再</white><yellow>點擊模板物品</yellow><white>來進入頁面</white>",
                    "最後點擊介面左下角的<dark_green>另存新檔</dark_green><white>物品來儲存模板。</white>",
                    " ",
                    "<yellow>嘗試過模板功能後，請輸入任意字元前往下個教學...</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new CreatingWorldTutorialSection();
        }
    }

    private class CreatingWorldTutorialSection extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<yellow>不錯！到目前為止你已經設定完你的第一場遊戲了</yellow>",
                    " ",
                    "然而你現在所在的地方是<red>大廳世界</red><white>，並不能拿來玩UHC。</white>",
                    "所以請你<aqua>點擊設定頁面右下角的物品</aqua><white>，或是使用 </white><yellow>/uhc regen</yellow> <white>來創建遊戲世界。</white>",
                    " ",
                    "<yellow>世界創建完畢後，請輸入任意字元前往下個教學...</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new FilterWorldTutorialSection();
        }
    }

    private class FilterWorldTutorialSection extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<gold>你滿意現在的世界嗎?</gold>",
                    " ",
                    "如果覺得中心點<red>不夠平整</red><white>，可以依照上面的文字所述，</white><gold><bold>輸入指令重新生成世界</bold></gold><white>，直到滿意為止。</white>",
                    " ",
                    "<dark_red><bold>注意！</bold></dark_red><red><bold>找到合適的世界後，別急著輸入指令載入地圖，這邊有些事情還沒交代給你，麻煩繼續把教學看完。</bold></red>",
                    " ",
                    "<yellow>若已找到合適的世界，請輸入任意字元前往下個教學...</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new PrepareForNextOne();
        }
    }

    private class PrepareForNextOne extends TutorialSection {

        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<gold><bold>恭喜你設定完成！現在可輸入指令開始生成地圖了。</bold></gold>",
                    " ",
                    "屆時地圖載入完畢後，別忘了輸入 <aqua><bold>/uhc toturial host</bold></aqua> <white>繼續下階段的教學！</white>",
                    " ",
                    "<yellow><bold>本教學已告一段落。</bold></yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return TutorialSection.END_TUTORIAL;
        }
    }
}
