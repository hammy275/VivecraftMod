package org.vivecraft.client_vr.gameplay.trackers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.vivecraft.common.utils.MathUtils;
import org.vivecraft.api.client.Tracker;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.common.utils.MathUtils;
import org.vivecraft.data.ItemTags;

public class TelescopeTracker implements Tracker {
    public static final ModelResourceLocation SCOPE_MODEL = new ModelResourceLocation("vivecraft", "spyglass_in_hand",
        "inventory");
    private static final float LENS_DIST_MAX = 0.05F;
    private static final float LENS_DIST_MIN = 0.185F;
    private static final float LENS_DOT_MAX = 0.9F;
    private static final float LENS_DOT_MIN = 0.75F;

    private final boolean[] viewing = new boolean[2];

    protected Minecraft mc;
    protected ClientDataHolderVR dh;

    public TelescopeTracker(Minecraft mc, ClientDataHolderVR dh) {
        this.mc = mc;
        this.dh = dh;
    }

    @Override
    public boolean isActive(LocalPlayer player) {
        return player != null &&
            !this.dh.bowTracker.isActive(player) &&
            (isTelescope(player.getMainHandItem()) || isTelescope(player.getOffhandItem()));
    }

    @Override
    public boolean itemInUse(LocalPlayer player) {
        return this.viewing[0] || this.viewing[1];
    }

    @Override
    public void reset(LocalPlayer player) {
        this.viewing[0] = false;
        this.viewing[1] = false;
    }

    @Override
    public void doProcess(LocalPlayer player) {
        for (int c = 0; c < 2; c++) {
            if (isTelescope(player.getItemInHand(InteractionHand.values()[c]))) {
                if (isViewing(c)) {
                    if (!this.viewing[c]) {
                        this.mc.gameMode.useItem(player, InteractionHand.values()[c]);
                    }
                    this.viewing[c] = true;
                } else {
                    this.viewing[c] = false;
                }
            } else {
                this.viewing[c] = false;
            }
        }
    }

    @Override
    public TrackerTickType tickType() {
        return TrackerTickType.PER_TICK;
    }

    /**
     * @param itemStack ItemStack to check
     * @return if the given {@code itemStack} is a telescope
     */
    public static boolean isTelescope(ItemStack itemStack) {
        return itemStack != null &&
            (itemStack.is(Items.SPYGLASS) || isLegacyTelescope(itemStack) || itemStack.is(ItemTags.VIVECRAFT_TELESCOPE)
            );
    }

    // TODO: old eye of the farseer, remove this eventually
    public static boolean isLegacyTelescope(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        } else if (!itemStack.hasCustomHoverName()) {
            return false;
        } else if (itemStack.getItem() != Items.ENDER_EYE) {
            return false;
        } else if (!itemStack.hasTag() || !itemStack.getTag().getBoolean("Unbreakable")) {
            return false;
        } else {
            return itemStack.getHoverName().getString().equals("Eye of the Farseer") ||
                (itemStack.getHoverName().getContents() instanceof TranslatableContents translatableContents &&
                    translatableContents.getKey().equals("vivecraft.item.telescope")
                );
        }
    }

    private static Vector3f getLensOrigin(int controller) {
        VRData.VRDevicePose con = ClientDataHolderVR.getInstance().vrPlayer.vrdata_room_pre.getController(controller);
        return con.getPositionF().add(getViewVector(controller).mul(-0.2F).add(con.getDirection().mul(0.05F)));
    }

    private static Vector3f getViewVector(int controller) {
        return ClientDataHolderVR.getInstance().vrPlayer.vrdata_room_pre.getController(controller).getCustomVector(
            MathUtils.DOWN);
    }

    public static boolean isViewing(int controller) {
        return viewPercent(controller) > 0.0F;
    }

    public static float viewPercent(int controller) {
        // seated doesn't have a fadeout
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && ClientDataHolderVR.getInstance().vrSettings.seated) {
            if (isTelescope(player.getUseItem())) {
                return 1;
            } else {
                return 0;
            }
        }

        float out = 0.0F;

        out = Math.max(out, viewPercent(controller, RenderPass.LEFT));
        out = Math.max(out, viewPercent(controller, RenderPass.RIGHT));

        return out;
    }

    private static float viewPercent(int controller, RenderPass renderPass) {
        if (ClientDataHolderVR.getInstance().vrPlayer == null) {
            return 0.0F;
        } else {
            VRData.VRDevicePose eyeRoom = ClientDataHolderVR.getInstance().vrPlayer.vrdata_room_pre.getEye(renderPass);
            float dist = eyeRoom.getPositionF().sub(getLensOrigin(controller)).length();
            Vector3f look = eyeRoom.getDirection();
            float dot = Math.abs(look.dot(getViewVector(controller)));

            float dfact = 0.0F;
            float distfact = 0.0F;

            if (dot > LENS_DOT_MIN) {
                if (dot > LENS_DOT_MAX) {
                    dfact = 1.0F;
                } else {
                    dfact = (dot - LENS_DOT_MIN) / (LENS_DOT_MAX - LENS_DOT_MIN);
                }
            }

            if (dist < LENS_DIST_MIN) {
                if (dist < LENS_DIST_MAX) {
                    distfact = 1.0F;
                } else {
                    distfact = 1.0F - (dist - LENS_DIST_MAX) / (LENS_DIST_MIN - LENS_DIST_MAX);
                }
            }

            return Math.min(dfact, distfact);
        }
    }
}
