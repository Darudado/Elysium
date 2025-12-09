package elysium;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import elysium.faction.ELYS_Gen;
import exerelin.campaign.SectorManager;

import java.util.Map;

public class ElysiumModPlugin extends BaseModPlugin {

    public void onNewGame() {
        Map<String, Object> data = Global.getSector().getPersistentData();
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new ELYS_Gen().generate(Global.getSector());
            data.put("ElysiumGenerated", "Version 0.1.0");
        }
        addMusicListener();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        addMusicListener();
    }

    private void addMusicListener() {
        Global.getSector().addTransientListener(new ElysiumMusicListener());
        }
    }

    class ElysiumMusicListener extends BaseCampaignEventListener {

        public ElysiumMusicListener() {
            super(false); // false = only listen to important events
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            // Called after player battles end
            Global.getLogger(ElysiumModPlugin.class).info("Player engagement ended");

            if (Util.isElysiumMusicPlaying) {
                try {
                    Global.getSoundPlayer().playCustomMusic(1, 1, null, false);
                    Util.isElysiumMusicPlaying = false;
                    Global.getLogger(ElysiumModPlugin.class).info("Music stopped");
                } catch (Exception e) {
                    Global.getLogger(ElysiumModPlugin.class).warn("Failed to restore music", e);
                }
            }
        }

    }
