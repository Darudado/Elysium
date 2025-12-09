package elysium;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class CombatMusicPlugin implements EveryFrameCombatPlugin {

    private boolean played = false;

    @Override
    public void init(CombatEngineAPI engine) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
	Global.getLogger(this.getClass()).info("CombatMusicPlugin init() called!");
	CombatEngineAPI engine = Global.getCombatEngine();
	if (engine == null ) return;
	if (engine.isPaused() || played) return;

	if (shouldPlayMusic(engine)) {
	    if (engine.getTotalElapsedTime(false) > 1f) {
		try {
		    Global.getSoundPlayer().playCustomMusic(1, 1, "ELYS_battlemusic", true);
		    played = true;
		} catch (Exception e) {
		    Global.getLogger(this.getClass()).warn("Failed to play music set ELYS_battlemusic", e);
		}
	    }
	}
    }

    private boolean shouldPlayMusic(CombatEngineAPI engine) {
	if (engine.getContext() == null) return false ;
	if (engine.getContext().getOtherFleet() == null) return false ;
	FactionAPI enemyFaction = engine.getContext().getOtherFleet().getFaction();
	return enemyFaction.getId().equals("elysium") | enemyFaction.getId().equals("elysium_void");
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
}