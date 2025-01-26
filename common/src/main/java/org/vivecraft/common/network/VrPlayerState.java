package org.vivecraft.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.vivecraft.api.data.FBTMode;
import org.vivecraft.api.data.VRBodyPartData;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.VRPlayer;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.common.api_impl.data.VRPoseImpl;
import org.vivecraft.common.utils.MathUtils;

import javax.annotation.Nullable;

/**
 * holds all data from a player
 *
 * @param seated              if the player is in seated mode
 * @param hmd                 device Pose of the headset
 * @param leftHanded          if true, {@code mainHand} is the left hand, else {@code offHand} is
 * @param mainHand            device Pose of the main hand
 * @param reverseHands1legacy same as {@code leftHanded}, just here for legacy compatibility
 * @param offHand             device Pose of the offhand
 * @param fbtMode             determines what additional trackers are in the player state
 * @param waist               waist tracker pos, can be {@code null}
 * @param rightFoot           right foot tracker pos, can be {@code null}
 * @param leftFoot            left foot tracker pos, can be {@code null}
 * @param rightKnee           right knee tracker pos, can be {@code null}
 * @param leftKnee            left knee tracker pos, can be {@code null}
 * @param rightElbow          right elbow tracker pos, can be {@code null}
 * @param leftElbow           left elbow tracker pos, can be {@code null}
 */
public record VrPlayerState(boolean seated, Pose hmd, boolean leftHanded, Pose mainHand,
                            boolean reverseHands1legacy, Pose offHand,
                            FBTMode fbtMode, @Nullable Pose waist,
                            @Nullable Pose rightFoot, @Nullable Pose leftFoot,
                            @Nullable Pose rightKnee, @Nullable Pose leftKnee,
                            @Nullable Pose rightElbow, @Nullable Pose leftElbow)
{

    /**
     * strips the VrPlayerState down to only contain legacy data
     *
     * @param other   VrPlayerState to strip down
     * @param version version to strip the packet down to
     */
    public VrPlayerState(VrPlayerState other, int version) {
        this(
            other.seated,
            other.hmd,
            other.leftHanded,
            other.mainHand,
            other.reverseHands1legacy,
            other.offHand,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? FBTMode.ARMS_ONLY : other.fbtMode,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.waist,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.rightFoot,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.leftFoot,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.rightKnee,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.leftKnee,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.rightElbow,
            version < CommonNetworkHelper.NETWORK_VERSION_FBT ? null : other.leftElbow
        );
    }

    public static VrPlayerState create(VRPlayer vrPlayer) {
        FBTMode fbtMode = vrPlayer.vrdata_world_post.fbtMode;
        if (ClientNetworking.USED_NETWORK_VERSION < CommonNetworkHelper.NETWORK_VERSION_FBT) {
            // don't send fbt data to legacy servers
            fbtMode = FBTMode.ARMS_ONLY;
        }

        boolean hasFbt = fbtMode != FBTMode.ARMS_ONLY;
        boolean hasExtendedFbt = fbtMode == FBTMode.WITH_JOINTS;

        return new VrPlayerState(
            ClientDataHolderVR.getInstance().vrSettings.seated,
            hmdPose(vrPlayer),
            ClientDataHolderVR.getInstance().vrSettings.reverseHands,
            devicePose(vrPlayer, MCVR.MAIN_CONTROLLER),
            ClientDataHolderVR.getInstance().vrSettings.reverseHands,
            devicePose(vrPlayer, MCVR.OFFHAND_CONTROLLER),
            fbtMode,
            hasFbt ? devicePose(vrPlayer, MCVR.WAIST_TRACKER) : null,
            hasFbt ? devicePose(vrPlayer, MCVR.RIGHT_FOOT_TRACKER) : null,
            hasFbt ? devicePose(vrPlayer, MCVR.LEFT_FOOT_TRACKER) : null,
            hasExtendedFbt ? devicePose(vrPlayer, MCVR.RIGHT_KNEE_TRACKER) : null,
            hasExtendedFbt ? devicePose(vrPlayer, MCVR.LEFT_KNEE_TRACKER) : null,
            hasExtendedFbt ? devicePose(vrPlayer, MCVR.RIGHT_ELBOW_TRACKER) : null,
            hasExtendedFbt ? devicePose(vrPlayer, MCVR.LEFT_ELBOW_TRACKER) : null
        );
    }

    /**
     * creates the headset Pose object from the client vr data
     *
     * @param vrPlayer object holding the client data
     * @return Pose object of the current headset state
     */
    private static Pose hmdPose(VRPlayer vrPlayer) {
        Vector3f position = MathUtils.subtractToVector3f(
            vrPlayer.vrdata_world_post.getEye(RenderPass.CENTER).getPosition(),
            Minecraft.getInstance().player.position());

        Quaternionf orientation = vrPlayer.vrdata_world_post.hmd.getMatrix().getNormalizedRotation(new Quaternionf());
        return new Pose(position, orientation);
    }

    /**
     * creates the device Pose object for the specified device, from the client vr data
     *
     * @param vrPlayer object holding the client data
     * @param device   index of the device to get the Pose for
     * @return Pose object of the current device state
     */
    private static Pose devicePose(VRPlayer vrPlayer, int device) {
        Vector3f position = MathUtils.subtractToVector3f(
            vrPlayer.vrdata_world_post.getDevice(device).getPosition(),
            Minecraft.getInstance().player.position());

        Quaternionf orientation = vrPlayer.vrdata_world_post.getDevice(device)
            .getMatrix().getNormalizedRotation(new Quaternionf());
        return new Pose(position, orientation);
    }

    /**
     * creates the controller Pose object for the specified controller, from the client vr data
     *
     * @param vrPlayer object holding the client data
     * @param tracker  index of the tracker to get the Pose for
     * @return TrackerPose object of the current tracker state
     */
    private static TrackerPose trackerPose(VRPlayer vrPlayer, int tracker) {
        Vector3f position = MathUtils.subtractToVector3f(
            vrPlayer.vrdata_world_post.getDevice(tracker).getPosition(),
            Minecraft.getInstance().player.position());
        return new TrackerPose(position);
    }

    /**
     * @param buffer     buffer to read from
     * @param bytesAfter specifies how many bytes in the buffer are meant to be left unread
     * @return a VrPlayerState read from the given {@code buffer}
     */
    public static VrPlayerState deserialize(FriendlyByteBuf buffer, int bytesAfter) {
        boolean seated = buffer.readBoolean();
        Pose hmd = Pose.deserialize(buffer);
        boolean reverseHands = buffer.readBoolean();
        Pose mainController = Pose.deserialize(buffer);
        boolean reverseHandsLegacy = buffer.readBoolean();
        Pose offController = Pose.deserialize(buffer);

        // the rest here is only sent when the client has any fbt trackers
        FBTMode fbtMode = FBTMode.ARMS_ONLY;
        Pose waist = null;
        Pose rightFoot = null;
        Pose leftFoot = null;
        Pose rightKnee = null;
        Pose leftKnee = null;
        Pose rightElbow = null;
        Pose leftElbow = null;
        if (buffer.readableBytes() > bytesAfter) {
            fbtMode = FBTMode.values()[buffer.readByte()];
        }
        if (fbtMode != FBTMode.ARMS_ONLY) {
            waist = Pose.deserialize(buffer);
            rightFoot = Pose.deserialize(buffer);
            leftFoot = Pose.deserialize(buffer);
        }
        if (fbtMode == FBTMode.WITH_JOINTS) {
            rightKnee = Pose.deserialize(buffer);
            leftKnee = Pose.deserialize(buffer);
            rightElbow = Pose.deserialize(buffer);
            leftElbow = Pose.deserialize(buffer);
        }
        return new VrPlayerState(seated,
            hmd,
            reverseHands,
            mainController,
            reverseHandsLegacy,
            offController,
            fbtMode, waist,
            rightFoot, leftFoot,
            rightKnee, leftKnee,
            rightElbow, leftElbow);
    }

    /**
     * gets the Pose for the given body part
     * @param bodyPart BodyPart to get the pose for
     * @return Pose of the {@code bodyPart}, or {@code null} if the body part is not valid for the current FBT mode
     */
    @Nullable
    public Pose getBodyPartPose(BodyPart bodyPart) {
        return switch(bodyPart) {
            case MAIN_HAND -> this.mainHand;
            case OFF_HAND -> this.offHand;
            case LEFT_FOOT -> this.leftFoot;
            case RIGHT_FOOT -> this.rightFoot;
            case LEFT_ELBOW -> this.leftElbow;
            case RIGHT_ELBOW -> this.rightElbow;
            case LEFT_KNEE -> this.leftKnee;
            case RIGHT_KNEE -> this.rightKnee;
            case WAIST -> this.waist;
        };
    }

    /**
     * writes this VrPlayerState to the given {@code buffer}
     *
     * @param buffer buffer to write to
     */
    public void serialize(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.seated);
        this.hmd.serialize(buffer);
        buffer.writeBoolean(this.leftHanded);
        this.mainHand.serialize(buffer);
        buffer.writeBoolean(this.leftHanded);
        this.offHand.serialize(buffer);
        // only send those, if it is there and the server supports it
        if (this.fbtMode != FBTMode.ARMS_ONLY) {
            buffer.writeByte(this.fbtMode.ordinal());
            this.waist.serialize(buffer);
            this.rightFoot.serialize(buffer);
            this.leftFoot.serialize(buffer);
            if (this.fbtMode == FBTMode.WITH_JOINTS) {
                this.rightKnee.serialize(buffer);
                this.leftKnee.serialize(buffer);
                this.rightElbow.serialize(buffer);
                this.leftElbow.serialize(buffer);
            }
        }
    }

    /**
     * @param playerPos The current position of the player.
     *
     * @return This object as a pose for use with the API.
     */
    public VRPoseImpl asPose(Vec3 playerPos) {
        return new VRPoseImpl(
            this.hmd.asBodyPartData(playerPos),
            this.mainHand.asBodyPartData(playerPos),
            this.offHand.asBodyPartData(playerPos),
            getDataOrNull(this.rightFoot, playerPos),
            getDataOrNull(this.leftFoot, playerPos),
            getDataOrNull(this.waist, playerPos),
            getDataOrNull(this.rightKnee, playerPos),
            getDataOrNull(this.leftKnee, playerPos),
            getDataOrNull(this.rightElbow, playerPos),
            getDataOrNull(this.leftElbow, playerPos),
            this.seated,
            this.leftHanded,
            this.fbtMode
        );
    }

    @Nullable
    private static VRBodyPartData getDataOrNull(Pose pose, Vec3 playerPos) {
        return pose == null ? null : pose.asBodyPartData(playerPos);
    }

}
