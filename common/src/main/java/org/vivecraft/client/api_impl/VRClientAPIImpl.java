package org.vivecraft.client.api_impl;

import org.jetbrains.annotations.Nullable;
import org.vivecraft.api.client.Tracker;
import org.vivecraft.api.client.VRClientAPI;
import org.vivecraft.api.client.data.VRPoseHistory;
import org.vivecraft.api.data.FBTMode;
import org.vivecraft.api.data.VRPose;
import org.vivecraft.client.api_impl.data.VRPoseHistoryImpl;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;
import org.vivecraft.client_vr.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.client_vr.provider.ControllerType;

public final class VRClientAPIImpl implements VRClientAPI {

    public static final VRClientAPIImpl INSTANCE = new VRClientAPIImpl();
    // If updated, should also update Javadocs in VRClientAPI
    private static final int MAX_CONFIGURABLE_HISTORY_TICKS = 200;

    private final VRPoseHistoryImpl poseHistory = new VRPoseHistoryImpl();
    private int maxPoseHistorySize = 0;

    private VRClientAPIImpl() {
    }

    public void clearPoseHistory() {
        this.poseHistory.clear();
    }

    public void addPoseToHistory(VRPose pose) {
        this.poseHistory.addPose(pose);
    }

    public int maxPoseHistorySize() {
        return this.maxPoseHistorySize;
    }

    @Nullable
    @Override
    public VRPose getLatestRoomPose() {
        if (!isVRActive()) {
            return null;
        }
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_room_pre.asVRPose();
    }

    @Nullable
    @Override
    public VRPose getPostTickRoomPose() {
        if (!isVRActive()) {
            return null;
        }
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_room_post.asVRPose();
    }

    @Nullable
    @Override
    public VRPose getPreTickWorldPose() {
        if (!isVRActive()) {
            return null;
        }
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_pre.asVRPose();
    }

    @Nullable
    @Override
    public VRPose getPostTickWorldPose() {
        if (!isVRActive()) {
            return null;
        }
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_post.asVRPose();
    }

    @Nullable
    @Override
    public VRPose getWorldRenderPose() {
        if (!isVRActive()) {
            return null;
        }
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_render.asVRPose();
    }

    @Override
    public void triggerHapticPulse(int controllerNum, float duration, float frequency, float amplitude, float delay) {
        if (controllerNum != 0 && controllerNum != 1) {
            throw new IllegalArgumentException("Can only trigger a haptic pulse for controllers 0 and 1.");
        }
        if (amplitude < 0F || amplitude > 1F) {
            throw new IllegalArgumentException("The amplitude of a haptic pulse must be between 0 and 1.");
        }
        if (isVRActive() && !isSeated()) {
            ClientDataHolderVR.getInstance().vr.triggerHapticPulse(
                ControllerType.values()[controllerNum],
                duration,
                frequency,
                amplitude,
                delay
            );
        }
    }

    @Override
    public boolean isSeated() {
        return ClientDataHolderVR.getInstance().vrSettings.seated;
    }

    @Override
    public boolean isLeftHanded() {
        return ClientDataHolderVR.getInstance().vrSettings.reverseHands;
    }

    @Override
    public FBTMode getFBTMode() {
        // Need to check if VR is running, not just initialized, since the VR player is set after initialization
        if (!isVRActive()) {
            return FBTMode.ARMS_ONLY;
        }
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_pre.fbtMode;
    }

    @Override
    public boolean isVRInitialized() {
        return VRState.VR_INITIALIZED;
    }

    @Override
    public boolean isVRActive() {
        return VRState.VR_RUNNING;
    }

    @Override
    public float getWorldScale() {
        if (isVRActive()) {
            return ClientDataHolderVR.getInstance().vrPlayer.worldScale;
        } else {
            return 1f;
        }
    }

    @Override
    public void requestTicksOfHistory(int maxTicksBack) throws IllegalArgumentException {
        if (maxTicksBack <= 0) {
            throw new IllegalArgumentException("Must call requestTicksOfHistory() with a positive number.");
        }
        this.maxPoseHistorySize = Math.max(this.maxPoseHistorySize, Math.min(maxTicksBack, MAX_CONFIGURABLE_HISTORY_TICKS));
    }

    @Override
    @Nullable
    public VRPoseHistory getHistoricalVRPoses() {
        if (!isVRActive() || this.maxPoseHistorySize <= 0) {
            return null;
        }
        return this.poseHistory;
    }

    @Override
    public void registerTracker(Tracker tracker) {
        ClientDataHolderVR.getInstance().registerTracker(tracker);
    }

    @Override
    public boolean setKeyboardState(boolean isNowOpen) {
        if (isVRActive()) {
            return KeyboardHandler.setOverlayShowing(isNowOpen);
        }
        return false;
    }
}
