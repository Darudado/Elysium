package elysium.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import elysium.Util;

import java.awt.*;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicRender;

public class EnergyBlasterOnHit implements OnHitEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

	if (target instanceof CombatEntityAPI) {
	    // Determine size/power based on projectile ID
	    String projectileId = projectile.getProjectileSpecId();
	    float explosionRadius = 100f;
	    float baseDamage = 600f;

	    if (projectileId.contains("medium")) {
		explosionRadius = 200f;
		baseDamage = 1500;
	    } else if (projectileId.contains("large")) {
		explosionRadius = 300f;
		baseDamage = 3000f;
	    }

	    // Energy impact visual effect (primary glow)
	    MagicRender.battlespace(
		    Global.getSettings().getSprite("graphics/fx/particlealpha_textured.png"),
		    point,
		    new Vector2f(),
		    new Vector2f(explosionRadius * 0.5f, explosionRadius * 0.5f),
		    new Vector2f(explosionRadius * 1.5f, explosionRadius * 1.5f),
		    projectile.getFacing() - 90f,
		    0f,
		    new Color(0, 255, 255, 255),
		    true,
		    0.12f,
		    0f,
		    0.24f
	    );

	    // Create explosion with proper damage spec
	    if (!shieldHit) {
		// Create visual and damaging explosion effect
		DamagingExplosionSpec explosion = new DamagingExplosionSpec(
			0.1f, // duration
			explosionRadius, // radius
			explosionRadius * 0.5f, // core radius
			baseDamage, // max damage
			baseDamage * 0.5f, // min damage (half of max)
			CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
			CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
			5f, // particle size min
			3f, // particle size range
			1f, // particle duration
			explosionRadius > 75f ? 30 : 20, // particle count
			Util.ELYSIUM_SECONDARY, // particle color - blue
			Util.ELYSIUM_PRIMARY  // explosion color - cyan
		);

		explosion.setDamageType(DamageType.HIGH_EXPLOSIVE); // High explosive damage as requested
		// More impressive visual for medium and large weapons
		explosion.setShowGraphic(true);
		explosion.setUseDetailedExplosion(true);
		explosion.setDetailedExplosionFlashRadius(explosionRadius * 2f);
		explosion.setDetailedExplosionFlashColorCore(Util.ELYSIUM_PRIMARY);
		explosion.setDetailedExplosionFlashColorFringe(Util.ELYSIUM_SECONDARY);


		// Apply the explosion at the hit location
		engine.spawnDamagingExplosion(explosion, projectile.getSource(), point);

		// Play explosion sound with size-based volume
		float volume = 0.6f + (explosionRadius / 300f);
		Global.getSoundPlayer().playSound("explosion_from_damage", volume, 0.6f, point, new Vector2f());
	    }

	    // Additional ring visual effect
	    MagicRender.battlespace(
		    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
		    point,
		    new Vector2f(),
		    new Vector2f(explosionRadius * 0.3f, explosionRadius * 0.3f),
		    new Vector2f(explosionRadius * 2f, explosionRadius * 2f),
		    MathUtils.getRandomNumberInRange(0, 360),
		    0f,
		    new Color(0, 255, 255, 230),
		    true,
		    0.15f,
		    0f,
		    0.3f
	    );
	}
    }
}