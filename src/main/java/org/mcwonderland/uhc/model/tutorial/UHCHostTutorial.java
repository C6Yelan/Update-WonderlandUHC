package org.mcwonderland.uhc.model.tutorial;

import org.mcwonderland.uhc.model.tutorial.model.Tutorial;
import org.mcwonderland.uhc.model.tutorial.model.TutorialSection;
import org.bukkit.entity.Player;

public class UHCHostTutorial extends Tutorial {

    public UHCHostTutorial(Player player) {
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
                    "<gold><bold>歡迎來到WonderlandUHC教學 - 主持部分</bold></gold>",
                    " ",
                    "以下將會教你如何完整主持一場UHC",
                    "在進行過程中，你隨時可以輸入 <aqua><bold>exit</bold></aqua> <white>來停止教學</white>",
                    " ",
                    "<yellow>如果你已經準備好了，請輸入任意字元前往下個教學…</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new CheckSettingsSection();
        }
    }

    private class CheckSettingsSection extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "首先最重要的，請先<aqua><bold>確認你的遊戲設定無誤</bold></aqua>",
                    "如果有任何問題，趁白名單還沒關閉之前趕快做調整！",
                    " ",
                    "提醒: 大部分設定現在調整都還來得及，但請<dark_red><bold>千萬不要把邊界大小條大！</bold></dark_red>",
                    "否則玩家若傳送到未載入過的地方，會造成<red><bold>嚴重的伺服器卡頓。</bold></red>",
                    " ",
                    "<yellow>確認設定無誤後，請輸入任一字元前往下個教學。</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new StaffOptionsTutorial();
        }
    }

    private class StaffOptionsTutorial extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<aqua>要擔任主持人，當然要有合適的管理工具。</aqua>",
                    " ",
                    "只需輸入 <yellow>/staff</yellow> <white>指令，即可開啟管理員模式</white><gray>(再輸入一次關閉)</gray>",
                    " ",
                    "<aqua>此模式共有以下特點：</aqua>",
                    "  1. 隱形，玩家與觀戰者不會看到你",
                    "  2. 玩家挖掘礦物通知(每五個通知一次)",
                    "  3. 更方便的管理工具，能切換移動速度…等",
                    "  4. 醒目的聊天文字",
                    " ",
                    "<gold><bold>不妨現在就試試看吧！</bold></gold>",
                    " ",
                    "<yellow>了解如何使用管理員模式後，請輸入任一字元前往下個教學。</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new WhitelistTutorial();
        }
    }

    private class WhitelistTutorial extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<green>現在你可以將白名單關閉，開放玩家入場了。</green>",
                    " ",
                    "備註: 你也可以手動使用 <yellow>/whitelist</yellow> <white>指令來控制誰能加入遊戲。</white>",
                    " ",
                    "<yellow>白單關閉後，請輸入任一字元前往下個教學。</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new WaitingPlayer();
        }
    }

    private class WaitingPlayer extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<green>接著，只需等待玩家到齊，即可開始遊戲。</green>",
                    " ",
                    "等待的過程中，你也可以<dark_green>再次確認本場遊戲的設定是否符合預期</dark_green><white>。</white>",
                    " ",
                    "<yellow>玩家到齊後，請輸入任一字元前往下個教學。</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new TeamTutorialSection();
        }
    }

    private class TeamTutorialSection extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "在遊戲開始之前，還想告訴你這個插件的<dark_purple>分隊功能</dark_purple><white>：</white>",
                    " ",
                    "  /uhc switchteam <A玩家> <B玩家> <yellow>- 強制把A玩家加入B玩家的隊伍</yellow>",
                    "  /uhc resetteam <yellow>- 強制解散所有人的隊伍</yellow>",
                    "  /uhc splitteam <yellow>- 對</yellow><gold>沒有隊伍</gold><yellow>的玩家進行分隊</yellow>",
                    "  /uhc splitteam true <yellow>- 重新洗牌(先解散所有隊伍，再重新分配一次)</yellow>",
                    " ",
                    "以上指令<dark_aqua>並非一定要使用</dark_aqua><white>，因為遊戲在開始的瞬間</white>",
                    "系統也會自動使用 /uhc splitteam 指令來分隊。",
                    " ",
                    "<yellow>學會如何手動分配隊伍後，請輸入任一字元前往下個教學。</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new PrepareForStart();
        }
    }

    private class PrepareForStart extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<yellow><bold>很好，現在可以準備開始遊戲了！</bold></yellow>",
                    " ",
                    "請開啟設定介面<gray>(指令為 /uhc edit)</gray><white>，並</white><aqua>點擊右下角的物品來開始傳送倒數。</aqua>",
                    "以下大致為遊戲開始前的流程:",
                    " ",
                    "  <dark_aqua>傳送倒數 -> 傳送玩家並使其無法移動 -> 傳送完畢 -> </dark_aqua>",
                    "  <dark_aqua>開始遊戲倒數 -> 倒數結束 -> 解除移動限制並開始遊戲。</dark_aqua>",
                    " ",
                    "<yellow>遊戲開始後，請輸入任一字元前往下個教學。</yellow>"
            };
        }

        @Override
        protected TutorialSection nextSection() {
            return new UsefulCommands();
        }
    }

    private class UsefulCommands extends TutorialSection {
        @Override
        protected String[] getMessages() {
            return new String[]{
                    "<gold><bold>恭喜你已完成此教學，你已大致學會如何主持一場UHC！</bold></gold>",
                    " ",
                    "<aqua>最後想再告訴你一些實用的指令，希望對你有幫助：</aqua>",
                    "  /border <yellow>- 強制於 10 秒後收縮邊界</yellow>",
                    "  /respawn <yellow>- 復活玩家</yellow>",
                    "  /giveall <yellow>- 給予所有玩家(不包含觀戰者與管理員)指令數量的物品</yellow>",
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
