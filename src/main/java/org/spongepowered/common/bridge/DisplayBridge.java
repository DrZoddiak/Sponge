package org.spongepowered.common.bridge;

import org.spongepowered.math.vector.Vector3f;

public interface DisplayBridge {

    void bridge$setScale(Vector3f scale);

    Vector3f bridge$getScale();

}