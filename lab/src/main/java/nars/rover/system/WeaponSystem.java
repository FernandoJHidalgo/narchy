package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.obj.Physical;
import nars.rover.obj.Weapon;
import org.jbox2d.common.Vec2;

/**
 * @author Daan van Yperen
 */
//@Wire
public class WeaponSystem extends EntityProcessingSystem {

    private ComponentMapper<Weapon> wm;
    private ComponentMapper<Physical> pm;
    //private ComponentMapper<Bounds> om;
    //private ComponentMapper<Attached> atm;
    //private ComponentMapper<Anim> am;
    //private ComponentMapper<Physics> ym;
    //private ComponentMapper<Gravity> gm;
    //private ComponentMapper<Buildable> bm;
    private GroupManager groupManager;
    /*private PhysicsSystem physicsSystems;
    private AfterPhysicsSystem afterPhysicsSystem;
    private AttachmentSystem attachmentSystem;
    private ParticleSystem particleSystem;*/
    private TagManager tagManager;
    public Entity player;
    //private AssetSystem assetSystem;
    //private DialogRenderSystem dialogRenderSystem;

    public WeaponSystem() {
        super(Aspect.all(Weapon.class, Physical.class));
    }

    private Vec2 vTmp = new Vec2();

    @Override
    protected void begin() {
        player = tagManager.getEntity("player");
    }

    @Override
    protected void process(Entity gun) {

        final Weapon weapon = wm.get(gun);

        // avoid cooldown on unbuilt structures.
        //if ( bm.has(gun) && !bm.get(gun).built ) return;

        if (weapon.firing || !weapon.cooldownWhileNotFiring)
        {
            weapon.cooldown -= world.delta;
        }
        weapon.sfxCooldown -= world.delta;


        if (weapon.firing) {
            weapon.firing = false;

            if (weapon.cooldown <= 0) {

                weapon.cooldown = weapon.fireCooldown;


                final Physical pos = pm.get(gun);
                //final Bounds bounds = om.get(gun);
                //final Anim anim = am.get(gun);

//                float aimRotation = anim.rotation + weapon.aimRotation;
//                if ( weapon.muzzleFlare ) {
//                    // determine muzzle location.
//                    vTmp.set(28,0).rotate(aimRotation).add(pos.x-8, pos.y+2).add(bounds.cx(), bounds.cy());
//                    particleSystem.setRotation(aimRotation);
//                    particleSystem.spawnParticle((int)vTmp.x, (int)vTmp.y, "muzzle-flare");
//                    particleSystem.setRotation(0);
//                }

//                if ( weapon.shellParticle != null )
//                {
//                    vTmp.set(18,0).rotate(aimRotation).add(pos.x-8, pos.y+2).add(bounds.cx(), bounds.cy());
//                    particleSystem.spawnParticle((int)vTmp.x, (int)vTmp.y, weapon.shellParticle);
//                }

//                if ( weapon.fireSfxId != null )
//                {
//                    if ( weapon.sfxCooldown <= 0 )
//                    {
//                        weapon.sfxCooldown = 1/30f;
//                        assetSystem.playSfx(weapon.fireSfxId,gun);
//                    }
//                }

//                // repeated bullets.
//                for (int c = 0, s = MathUtils.random(weapon.minBullets, weapon.maxBullets); c < s; c++) {
//
//                    Entity bullet = EntityFactory.createBullet(world, pos.x + bounds.cx(), pos.y + bounds.cy());
//
//                    // rotate bullet to player rotation
//                    float rotation = aimRotation + MathUtils.random(-weapon.spread, weapon.spread);
//                    Anim bulletAnim = am.get(bullet);
//                    bulletAnim.rotation = rotation;
//                    bulletAnim.id = weapon.bulletAnimId;
//
//                    // push back the user.
//                    if (atm.has(gun)) {
//                        Attached attachedTo = atm.get(gun);
//                        if (attachedTo.parent != null && attachedTo.parent.isActive()) {
//                            physicsSystems.push(attachedTo.parent, rotation - 180, weapon.recoil);
//                        }
//                    }
//
//
//                    if ( weapon.bulletPayload.type == Payload.DamageType.RESOURCE )
//                    {
//                        Homing homing = new Homing(player);
//                        homing.maxDistance = 100;
//                        homing.maxVelocity = 500;
//                        homing.speedFactor = 2;
//                        bullet.edit().add(homing);
//                    }
//
//
//                    Physics physics = ym.get(bullet);
//                    physics.friction = weapon.bulletFriction;
//                    physics.bounce = weapon.bulletBounce;
//
//                    Payload payload = weapon.bulletPayload.clone();
//                    payload.triggerGroup = weapon.enemyGroup;
//                    bullet.edit().add(payload);
//
//                    Gravity gravity = gm.get(bullet);
//                    gravity.y *= weapon.bulletGravityFactor;
//                    gravity.x *= weapon.bulletGravityFactor;
//
//                    attachmentSystem.push(gun, rotation - 180, weapon.recoil / s);
//                    physicsSystems.push(bullet, rotation, weapon.bulletSpeed);
//                }


            }
        }

    }

//    private void cycleWeaponPickup(Weapon weapon) {
//        dialogRenderSystem.randomSay(DialogRenderSystem.WEAPON_READY_MESSAGES);
//        switch (weapon.bulletAnimId)
//        {
//            case "rifle": weapon.bulletAnimId="shotgun"; break;
//            case "shotgun": weapon.bulletAnimId="grenadelauncher"; break;
//            case "grenadelauncher": weapon.bulletAnimId="flamethrower"; break;
//            case "flamethrower": weapon.bulletAnimId="rifle"; break;
//        }
//    }
}
