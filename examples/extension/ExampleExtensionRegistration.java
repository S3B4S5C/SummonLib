package me.s3b4s5.summonlib.examples.extension;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.api.SummonLibApi;
import me.s3b4s5.summonlib.api.SummonLibRegistration;

/**
 * Documentation-only example showing how a side-mod can register custom
 * SummonLib extension types during plugin setup.
 */
public final class ExampleExtensionRegistration extends JavaPlugin {

    @Override
    protected void setup() {
        SummonLibRegistration reg = SummonLibApi.registration(this);

        reg.registerFollowConfigType(
                SpiralFollowConfig.ASSET_TYPE_ID,
                SpiralFollowConfig.class,
                SpiralFollowConfig.ABSTRACT_CODEC
        );

        reg.registerNpcMotionControllerType(
                GlideNpcMotionControllerConfig.ASSET_TYPE_ID,
                GlideNpcMotionControllerConfig.class,
                GlideNpcMotionControllerConfig.ABSTRACT_CODEC
        );

        reg.registerSummonConfigType(
                TotemSummonConfig.ASSET_TYPE_ID,
                TotemSummonConfig.class,
                TotemSummonConfig.ABSTRACT_CODEC
        );
    }

    private static final class SpiralFollowConfig {
        static final String ASSET_TYPE_ID = "Spiral";
        static final Object ABSTRACT_CODEC = null;
    }

    private static final class GlideNpcMotionControllerConfig {
        static final String ASSET_TYPE_ID = "Glide";
        static final Object ABSTRACT_CODEC = null;
    }

    private static final class TotemSummonConfig {
        static final String ASSET_TYPE_ID = "Totem";
        static final Object ABSTRACT_CODEC = null;
    }
}
