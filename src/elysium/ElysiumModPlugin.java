package elysium;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
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
    }

}
