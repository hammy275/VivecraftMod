package org.vivecraft.mod_compat_vr.optifine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;
import org.vivecraft.client_vr.settings.VRSettings;
import org.vivecraft.mod_compat_vr.shaders.ShadersHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class OptifineHelper {

    private static boolean CHECKED_FOR_OPTIFINE = false;
    private static boolean OPTIFINE_LOADED = false;

    private static final Map<String, Pair<ShadersHelper.UniformType, Object>> SHADER_UNIFORMS = new HashMap<>();
    private static final Map<String, Object> SHADER_UNIFORMS_DATA = new HashMap<>();

    private static Class<?> Config;
    private static Method Config_IsShaders;
    private static Method Config_IsRenderRegions;
    private static Method Config_IsSkyEnabled;
    private static Method Config_IsSunMoonEnabled;
    private static Method Config_IsStarsEnabled;
    private static Method Config_IsCustomColors;
    private static Method Config_IsAntialiasing;
    private static Method Config_IsAntialiasingConfigured;

    private static Class<?> SmartAnimations;
    private static Method SmartAnimations_SpriteRendered;

    private static Class<?> CustomColors;
    private static Method CustomColors_GetSkyColor;
    private static Method CustomColors_GetSkyColoEnd;
    private static Method CustomColors_GetUnderwaterColor;
    private static Method CustomColors_GetUnderlavaColor;
    private static Method CustomColors_GetFogColor;
    private static Method CustomColors_GetFogColorEnd;
    private static Method CustomColors_GetFogColorNether;

    private static Class<?> ShadersRender;
    private static Method ShadersRender_BeginOutline;
    private static Method ShadersRender_EndOutline;

    private static Class<?> Shaders;
    private static Method Shaders_BeginEntities;
    private static Method Shaders_EndEntities;
    private static Method Shaders_SetCameraShadow;
    private static Field Shaders_DFB;
    private static Field Shaders_isShadowPass;
    private static Field Shaders_shaderUniforms;

    private static Method ShadersFramebuffer_BindFramebuffer;

    private static Method ShaderUniforms_make1i;
    private static Method ShaderUniforms_make3f;
    private static Method ShaderUniforms_makeM4;

    private static Method ShaderUniform1i_setValue;
    private static Method ShaderUniform3f_setValue;
    private static Method ShaderUniformM4_setValue;

    private static Field Options_ofRenderRegions;
    private static Field Options_ofCloudHeight;
    private static Field Options_ofAoLevel;
    private static Field Vertex_renderPositions;
    private static Method VertexRecord_renderPositions;

    private static Method Mutable_get;
    private static Method Mutable_set;

    /**
     * @return if Optifine is present
     */
    public static boolean isOptifineLoaded() {
        if (!CHECKED_FOR_OPTIFINE) {
            CHECKED_FOR_OPTIFINE = true;
            // check for optifine with a class search
            try {
                Class.forName("net.optifine.Config");
                VRSettings.LOGGER.info("Vivecraft: Optifine detected");
                OPTIFINE_LOADED = true;
            } catch (ClassNotFoundException ignore) {
                VRSettings.LOGGER.info("Vivecraft: Optifine not detected");
                OPTIFINE_LOADED = false;
            }
            if (OPTIFINE_LOADED) {
                init();
            }
        }
        return OPTIFINE_LOADED;
    }

    /**
     * @return if a shaderpack is in use
     */
    public static boolean isShaderActive() {
        try {
            return (boolean) Config_IsShaders.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "IsShaders");
            return false;
        }
    }

    public static boolean isRenderingShadows() {
        try {
            return (boolean) Shaders_isShadowPass.get(Shaders);
        } catch (IllegalAccessException e) {
            logError(e, "isShadowPass");
            return false;
        }
    }

    /**
     * binds the Shaders_ framebuffer
     *
     * @return if the shader framebuffer got bound
     */
    public static boolean bindShaderFramebuffer() {
        try {
            Object dfb = Shaders_DFB.get(Shaders);
            if (dfb != null) {
                ShadersFramebuffer_BindFramebuffer.invoke(dfb);
                return true;
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "dfb.BindFramebuffer");
        }
        return false;
    }

    /**
     * starts using the outline shader
     */
    public static void beginOutlineShader() {
        try {
            ShadersRender_BeginOutline.invoke(ShadersRender);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "beginOutline");
        }
    }

    /**
     * stops using the outline shader
     */
    public static void endOutlineShader() {
        try {
            ShadersRender_EndOutline.invoke(ShadersRender);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "endOutline");
        }
    }

    /**
     * starts using the entity shader
     */
    public static void beginEntities() {
        try {
            Shaders_BeginEntities.invoke(Shaders);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "beginEntities");
        }
    }

    /**
     * stops using the entity shader
     */
    public static void endEntities() {
        try {
            Shaders_EndEntities.invoke(Shaders);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "endEntities");
        }
    }

    /**
     * sets the position of the shadow Camera
     *
     * @param poseStack   PoseStack for orienting
     * @param camera      camera get the position from
     * @param partialTick current partial tick
     */
    public static void setCameraShadow(PoseStack poseStack, Camera camera, float partialTick) {
        try {
            Shaders_SetCameraShadow.invoke(Shaders, poseStack, camera, partialTick);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "setCameraShadow");
        }
    }

    /**
     * @return if the sun/moon is enabled
     */
    public static boolean isSunMoonEnabled() {
        try {
            return (boolean) Config_IsSunMoonEnabled.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "isSunMoonEnabled");
            return true;
        }
    }

    /**
     * @return if the sky is enabled
     */
    public static boolean isSkyEnabled() {
        try {
            return (boolean) Config_IsSkyEnabled.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "isSkyEnabled");
            return true;
        }
    }

    /**
     * @return if the stars are enabled
     */
    public static boolean isStarsEnabled() {
        try {
            return (boolean) Config_IsStarsEnabled.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "isStarsEnabled");
            return true;
        }
    }

    /**
     * @return if custom colors are enabled
     */
    public static boolean isCustomColors() {
        try {
            return (boolean) Config_IsCustomColors.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "isCustomColors");
            return false;
        }
    }

    /**
     * @return if antialiasing is enabled
     */
    public static boolean isAntialiasing() {
        try {
            return (boolean) Config_IsAntialiasing.invoke(Config) ||
                (boolean) Config_IsAntialiasingConfigured.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "isAntialiasing");
            return false;
        }
    }

    /**
     * @return if render regions is enabled
     */
    public static boolean isRenderRegions() {
        try {
            return (boolean) Config_IsRenderRegions.invoke(Config);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "isRenderRegions");
            return false;
        }
    }

    /**
     * enables/disables render regions
     *
     * @param active new state
     */
    public static void setRenderRegions(boolean active) {
        try {
            Options_ofRenderRegions.set(Minecraft.getInstance().options, active);
        } catch (IllegalAccessException e) {
            VRSettings.LOGGER.error("Vivecraft: error setting Optifine render regions:", e);
        }
    }

    /**
     * applies the RPs custom sky color
     *
     * @param skyColor    original sky color
     * @param blockAccess level this is checked for
     * @param x           player position x
     * @param y           player position y
     * @param z           player position z
     * @return altered skyColor
     */
    public static Vec3 getCustomSkyColor(Vec3 skyColor, BlockAndTintGetter blockAccess, double x, double y, double z) {
        try {
            return (Vec3) CustomColors_GetSkyColor.invoke(CustomColors, skyColor, blockAccess, x, y, z);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getSkyColor");
            return skyColor;
        }
    }

    /**
     * applies the RPs custom End sky color
     *
     * @param skyColor original sky color
     * @return altered skyColor
     */
    public static Vec3 getCustomSkyColorEnd(Vec3 skyColor) {
        try {
            return (Vec3) CustomColors_GetSkyColoEnd.invoke(CustomColors, skyColor);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getSkyColorEnd");
            return skyColor;
        }
    }

    /**
     * gets the custom underwater fog color
     *
     * @param blockAccess level this is checked for
     * @param x           player position x
     * @param y           player position y
     * @param z           player position z
     * @return underwater fog color, or {@code null} on an error
     */
    public static Vec3 getCustomUnderwaterColor(BlockAndTintGetter blockAccess, double x, double y, double z) {
        try {
            return (Vec3) CustomColors_GetUnderwaterColor.invoke(CustomColors, blockAccess, x, y, z);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getUnderwaterColor");
            return null;
        }
    }

    /**
     * gets the custom underlava fog color
     *
     * @param blockAccess level this is checked for
     * @param x           player position x
     * @param y           player position y
     * @param z           player position z
     * @return underlava fog color, or {@code null} on an error
     */
    public static Vec3 getCustomUnderlavaColor(BlockAndTintGetter blockAccess, double x, double y, double z) {
        try {
            return (Vec3) CustomColors_GetUnderlavaColor.invoke(CustomColors, blockAccess, x, y, z);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getUnderlavaColor");
            return null;
        }
    }

    /**
     * applies the RPs custom fog color
     *
     * @param fogColor    original fog color
     * @param blockAccess level this is checked for
     * @param x           player position x
     * @param y           player position y
     * @param z           player position z
     * @return altered fogColor
     */
    public static Vec3 getCustomFogColor(Vec3 fogColor, BlockAndTintGetter blockAccess, double x, double y, double z) {
        try {
            return (Vec3) CustomColors_GetFogColor.invoke(CustomColors, fogColor, blockAccess, x, y, z);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getFogColor");
            return fogColor;
        }
    }

    /**
     * applies the RPs custom End fog color
     *
     * @param fogColor original fog color
     * @return altered fogColor
     */
    public static Vec3 getCustomFogColorEnd(Vec3 fogColor) {
        try {
            return (Vec3) CustomColors_GetFogColorEnd.invoke(CustomColors, fogColor);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getFogColorEnd");
            return fogColor;
        }
    }

    /**
     * applies the RPs custom Nether fog color
     *
     * @param fogColor original fog color
     * @return altered fogColor
     */
    public static Vec3 getCustomFogColorNether(Vec3 fogColor) {
        try {
            return (Vec3) CustomColors_GetFogColorNether.invoke(CustomColors, fogColor);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logError(e, "getFogColorNether");
            return fogColor;
        }
    }

    /**
     * @return Optifines cloud height offset 0-1, needs to be multiplied by 128
     */
    public static double getCloudHeight() {
        try {
            return (double) Options_ofCloudHeight.get(Minecraft.getInstance().options);
        } catch (IllegalAccessException e) {
            logError(e, "getCloudHeight");
            return 0;
        }
    }

    /**
     * @return Optifines AO setting
     */
    public static double getAoLevel() {
        try {
            return (double) Options_ofAoLevel.get(Minecraft.getInstance().options);
        } catch (IllegalAccessException e) {
            logError(e, "getAoLevel");
            return 1.0;
        }
    }

    /**
     * marks the given Sprite to be animated in this frame
     *
     * @param sprite sprite to mark
     */
    public static void markTextureAsActive(TextureAtlasSprite sprite) {
        try {
            SmartAnimations_SpriteRendered.invoke(SmartAnimations, sprite);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logError(e, "spriteRendered");
        }
    }

    /**
     * copies the cached position from one vertex to another one
     *
     * @param source vertex to copy from
     * @param dest   vertex to copy to
     */
    public static void copyRenderPositions(ModelPart.Vertex source, ModelPart.Vertex dest) {
        try {
            if (Vertex_renderPositions != null) {
                Vertex_renderPositions.set(dest, Vertex_renderPositions.get(source));
            } else if (VertexRecord_renderPositions != null) {
                Mutable_set.invoke(VertexRecord_renderPositions.invoke(dest),
                    Mutable_get.invoke(VertexRecord_renderPositions.invoke(source)));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            VRSettings.LOGGER.error("Vivecraft: error copying Optifine vertex data:", e);
        }
    }

    /**
     * creates and updates Optifines shader uniforms added by vivecraft
     */
    public static void updateUniforms() {
        if (!isOptifineLoaded()) return;
        try {
            for (Triple<String, ShadersHelper.UniformType, Supplier<?>> uniform : ShadersHelper.getUniforms()) {
                String name = uniform.getLeft();
                // create the uniform and data holder, if they aren't created yet
                if (!SHADER_UNIFORMS.containsKey(name)) {
                    Method m = switch (uniform.getMiddle()) {
                        case MATRIX4F -> ShaderUniforms_makeM4;
                        case VECTOR3F -> ShaderUniforms_make3f;
                        case BOOLEAN, INTEGER -> ShaderUniforms_make1i;
                    };
                    SHADER_UNIFORMS.put(uniform.getLeft(),
                        Pair.of(uniform.getMiddle(), m.invoke(Shaders_shaderUniforms.get(null), name)));
                    SHADER_UNIFORMS_DATA.put(name, switch (uniform.getMiddle()) {
                        case MATRIX4F -> MemoryUtil.memAllocFloat(16);
                        case VECTOR3F -> new Vector3f();
                        case BOOLEAN, INTEGER -> 0;
                    });
                }

                // update the uniform with the given supplier
                switch (uniform.getMiddle()) {
                    case MATRIX4F -> {
                        FloatBuffer floatBuffer = (FloatBuffer) SHADER_UNIFORMS_DATA.get(name);
                        floatBuffer.clear();
                        ((Matrix4fc) uniform.getRight().get()).get(floatBuffer);
                    }
                    case VECTOR3F ->
                        ((Vector3f) SHADER_UNIFORMS_DATA.get(name)).set((Vector3fc) uniform.getRight().get());
                    case INTEGER -> SHADER_UNIFORMS_DATA.put(name, uniform.getRight().get());
                    case BOOLEAN -> SHADER_UNIFORMS_DATA.put(name, ((boolean) uniform.getRight().get()) ? 1 : 0);
                    default -> throw new IllegalStateException("Unexpected uniform type: " + uniform.getMiddle());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            VRSettings.LOGGER.error("Vivecraft: error updating shader uniform data:", e);
        }
    }

    /**
     * sets Optifines shader uniforms added by vivecraft
     */
    public static void setUniforms() {
        if (!isOptifineLoaded()) return;
        try {
            for (Map.Entry<String, Pair<ShadersHelper.UniformType, Object>> entry : SHADER_UNIFORMS.entrySet()) {
                String name = entry.getKey();
                Object data = SHADER_UNIFORMS_DATA.get(name);
                Object uniform = entry.getValue().getRight();

                switch (entry.getValue().getLeft()) {
                    case MATRIX4F -> ShaderUniformM4_setValue.invoke(uniform, false, data);
                    case VECTOR3F -> {
                        Vector3f vec = (Vector3f) data;
                        ShaderUniform3f_setValue.invoke(uniform, vec.x, vec.y, vec.z);
                    }
                    case BOOLEAN, INTEGER -> ShaderUniform1i_setValue.invoke(uniform, data);
                    default ->
                        throw new IllegalStateException("Unexpected uniform type: " + entry.getValue().getLeft());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            VRSettings.LOGGER.error("Vivecraft: error updating shader uniform data:", e);
        }
    }

    /**
     * initializes all Reflections
     */
    private static void init() {
        try {
            Config = Class.forName("net.optifine.Config");
            Config_IsShaders = Config.getMethod("isShaders");
            Config_IsRenderRegions = Config.getMethod("isRenderRegions");
            Config_IsSkyEnabled = Config.getMethod("isSkyEnabled");
            Config_IsSunMoonEnabled = Config.getMethod("isSunMoonEnabled");
            Config_IsStarsEnabled = Config.getMethod("isStarsEnabled");
            Config_IsCustomColors = Config.getMethod("isCustomColors");
            Config_IsAntialiasing = Config.getMethod("isAntialiasing");
            Config_IsAntialiasingConfigured = Config.getMethod("isAntialiasingConfigured");

            SmartAnimations = Class.forName("net.optifine.SmartAnimations");
            SmartAnimations_SpriteRendered = SmartAnimations.getMethod("spriteRendered", TextureAtlasSprite.class);

            Options_ofRenderRegions = Options.class.getField("ofRenderRegions");
            Options_ofCloudHeight = Options.class.getField("ofCloudsHeight");
            Options_ofAoLevel = Options.class.getField("ofAoLevel");

            CustomColors = Class.forName("net.optifine.CustomColors");
            CustomColors_GetSkyColor = CustomColors.getMethod("getSkyColor", Vec3.class, BlockAndTintGetter.class,
                double.class, double.class, double.class);

            CustomColors_GetUnderwaterColor = CustomColors.getMethod("getUnderwaterColor", BlockAndTintGetter.class,
                double.class, double.class, double.class);
            CustomColors_GetUnderlavaColor = CustomColors.getMethod("getUnderlavaColor", BlockAndTintGetter.class,
                double.class, double.class, double.class);

            ShadersRender = Class.forName("net.optifine.shaders.ShadersRender");
            ShadersRender_BeginOutline = ShadersRender.getMethod("beginOutline");
            ShadersRender_EndOutline = ShadersRender.getMethod("endOutline");

            Shaders = Class.forName("net.optifine.shaders.Shaders");
            Shaders_BeginEntities = Shaders.getMethod("beginEntities");
            Shaders_EndEntities = Shaders.getMethod("endEntities");
            Shaders_SetCameraShadow = Shaders.getMethod("setCameraShadow", PoseStack.class, Camera.class, float.class);
            Shaders_isShadowPass = Shaders.getField("isShadowPass");

            Class<?> ShaderUniforms = Class.forName("net.optifine.shaders.uniform.ShaderUniforms");
            ShaderUniforms_make1i = ShaderUniforms.getMethod("make1i", String.class);
            ShaderUniforms_make3f = ShaderUniforms.getMethod("make3f", String.class);
            ShaderUniforms_makeM4 = ShaderUniforms.getMethod("makeM4", String.class);

            ShaderUniform1i_setValue = Class.forName("net.optifine.shaders.uniform.ShaderUniform1i")
                .getMethod("setValue", int.class);
            ShaderUniform3f_setValue = Class.forName("net.optifine.shaders.uniform.ShaderUniform3f")
                .getMethod("setValue", float.class, float.class, float.class);
            ShaderUniformM4_setValue = Class.forName("net.optifine.shaders.uniform.ShaderUniformM4")
                .getMethod("setValue", boolean.class, FloatBuffer.class);

            Class<?> ShadersFramebuffer = Class.forName("net.optifine.shaders.ShadersFramebuffer");
            ShadersFramebuffer_BindFramebuffer = ShadersFramebuffer.getMethod("bindFramebuffer");

            // private methods
            CustomColors_GetSkyColoEnd = CustomColors.getDeclaredMethod("getSkyColorEnd", Vec3.class);
            CustomColors_GetSkyColoEnd.setAccessible(true);
            CustomColors_GetFogColor = CustomColors.getDeclaredMethod("getFogColor", Vec3.class,
                BlockAndTintGetter.class, double.class, double.class, double.class);
            CustomColors_GetFogColor.setAccessible(true);
            CustomColors_GetFogColorEnd = CustomColors.getDeclaredMethod("getFogColorEnd", Vec3.class);
            CustomColors_GetFogColorEnd.setAccessible(true);
            CustomColors_GetFogColorNether = CustomColors.getDeclaredMethod("getFogColorNether", Vec3.class);
            CustomColors_GetFogColorNether.setAccessible(true);

            // private Fields
            Shaders_DFB = Shaders.getDeclaredField("dfb");
            Shaders_DFB.setAccessible(true);
            Shaders_shaderUniforms = Shaders.getDeclaredField("shaderUniforms");
            Shaders_shaderUniforms.setAccessible(true);

            try {
                Vertex_renderPositions = ModelPart.Vertex.class.getField("renderPositions");
            } catch (NoSuchFieldException e) {
                // this version doesn't have the entity render improvements
                Vertex_renderPositions = null;
                // check if record type
                if (ModelPart.Vertex.class.isRecord()) {
                    // check for mutable renderPositions
                    for (RecordComponent comp : ModelPart.Vertex.class.getRecordComponents()) {
                        if (comp.getName().equals("renderPositions")) {
                            VertexRecord_renderPositions = comp.getAccessor();
                        }
                    }
                }
                try {
                    Class<?> Mutable = Class.forName("net.optifine.util.Mutable");
                    Mutable_get = Mutable.getMethod("get");
                    Mutable_set = Mutable.getMethod("set", Object.class);
                } catch (ClassNotFoundException | NoSuchMethodException eRecord) {
                    VertexRecord_renderPositions = null;
                    Mutable_get = null;
                    Mutable_set = null;
                }
            }
        } catch (ClassNotFoundException e) {
            VRSettings.LOGGER.error("Vivecraft: Optifine detected, but couldn't load class:", e);
            OPTIFINE_LOADED = false;
        } catch (NoSuchMethodException e) {
            VRSettings.LOGGER.error("Vivecraft: Optifine detected, but couldn't load Method:", e);
            OPTIFINE_LOADED = false;
        } catch (NoSuchFieldException e) {
            VRSettings.LOGGER.error("Vivecraft: Optifine detected, but couldn't load Field:", e);
        }
    }

    private static void logError(Exception e, String call) {
        VRSettings.LOGGER.error("Vivecraft: error calling Optifine '{}':", call, e);
    }
}
