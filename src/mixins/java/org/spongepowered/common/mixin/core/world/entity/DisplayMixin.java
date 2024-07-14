package org.spongepowered.common.mixin.core.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.DisplayBridge;

@Mixin(Display.class)
public abstract class DisplayMixin extends EntityMixin implements DisplayBridge  {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Vector3f> DATA_SCALE_ID;
    // @formatter:on

    @Override
    public void bridge$setScale(org.spongepowered.math.vector.Vector3f scale) {
        entityData.set(DATA_SCALE_ID, new Vector3f(scale.x(), scale.y(), scale.z()));
    }

    @Override
    public org.spongepowered.math.vector.Vector3f bridge$getScale() {
        Vector3f v = entityData.get(DATA_SCALE_ID);
        return new org.spongepowered.math.vector.Vector3f(v.x, v.y, v.z);
    }
}