package net.aufdemrand.denizen.utilities.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R1.CraftServer;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.projectiles.ProjectileSource;

public class CraftItemProjectile extends CraftItem implements DenizenCustomEntity, ItemProjectile {

    private boolean doesBounce;

    public CraftItemProjectile(CraftServer server, EntityItemProjectile entity) {
        super(server, entity);
    }

    @CreateEntity
    public static ItemProjectile createItemProjectile(Location location) {
        CraftWorld world = (CraftWorld) location.getWorld();
        EntityItemProjectile entity = new EntityItemProjectile(world, location);
        return (ItemProjectile) entity.getBukkitEntity();
    }

    @Override
    public EntityItemProjectile getHandle() {
        return (EntityItemProjectile) super.getHandle();
    }

    @Override
    public String getEntityTypeName() {
        return "ITEM_PROJECTILE";
    }

    @Override
    public ProjectileSource getShooter() {
        return getHandle().projectileSource;
    }

    @Override
    public void setShooter(ProjectileSource shooter) {
        if (shooter instanceof CraftLivingEntity) {
            getHandle().shooter = ((CraftLivingEntity) shooter).getHandle();
            if (shooter instanceof CraftHumanEntity) {
                getHandle().shooterName = ((CraftHumanEntity) shooter).getName();
            }
        } else {
            getHandle().shooter = null;
            getHandle().shooterName = null;
        }
        getHandle().projectileSource = shooter;
    }

    @Override
    public boolean doesBounce() {
        return doesBounce;
    }

    @Override
    public void setBounce(boolean doesBounce) {
        this.doesBounce = doesBounce;
    }


    @Override
    public void _INVALID_setShooter(LivingEntity livingEntity) {
        // Do nothing
    }

    @Override
    public LivingEntity _INVALID_getShooter() {
        return null;
    }
}
