package org.vivecraft.common.api_impl.data;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.vivecraft.api.data.VRBodyPartData;

public record VRBodyPartDataImpl(Vec3 pos, Vec3 rot, Quaternionfc quaternion) implements VRBodyPartData {

    @Override
    public Vec3 getPos() {
        return this.pos;
    }

    @Override
    public Vec3 getRot() {
        return this.rot;
    }

    @Override
    public double getPitch() {
        return Math.asin(this.rot.y / this.rot.length());
    }

    @Override
    public double getYaw() {
        return Math.atan2(-this.rot.x, this.rot.z);
    }

    @Override
    public double getRoll() {
        return -Math.atan2(2.0F * (quaternion.x() * quaternion.y() + quaternion.w() * quaternion.z()),
            quaternion.w() * quaternion.w() - quaternion.x() * quaternion.x() + quaternion.y() * quaternion.y() - quaternion.z() * quaternion.z());
    }

    @Override
    public Quaternionfc getQuaternion() {
        return this.quaternion;
    }

    @Override
    public String toString() {
        return "Position: " + getPos() + ", Rotation: " + getRot();
    }
}
