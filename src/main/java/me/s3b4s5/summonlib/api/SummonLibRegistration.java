package me.s3b4s5.summonlib.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import me.s3b4s5.summonlib.assets.config.SummonConfig;
import me.s3b4s5.summonlib.assets.config.model.follow.FollowConfig;
import me.s3b4s5.summonlib.assets.config.npc.motion.NpcMotionControllerConfig;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Stable public registration surface for side mods that extend SummonLib codecs.
 *
 * <p>Call these methods during plugin setup before related assets are loaded.
 * The methods here only register subtype codecs and interaction codecs; they
 * do not register asset stores or ECS systems.
 */
public final class SummonLibRegistration {

    private final JavaPlugin plugin;

    SummonLibRegistration(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Nonnull
    public JavaPlugin plugin() {
        return plugin;
    }

    /**
     * Registers a new follow asset subtype for {@code FollowConfig.CODEC}.
     */
    public <T extends FollowConfig> void registerFollowConfigType(
            @Nonnull String typeId,
            @Nonnull Class<T> configClass,
            @Nonnull BuilderCodec<T> codec
    ) {
        FollowConfig.CODEC.register(
                Objects.requireNonNull(typeId, "typeId"),
                Objects.requireNonNull(configClass, "configClass"),
                Objects.requireNonNull(codec, "codec")
        );
    }

    /**
     * Registers a new NPC motion controller asset subtype for
     * {@code NpcMotionControllerConfig.CODEC}.
     */
    public <T extends NpcMotionControllerConfig> void registerNpcMotionControllerType(
            @Nonnull String typeId,
            @Nonnull Class<T> configClass,
            @Nonnull BuilderCodec<T> codec
    ) {
        NpcMotionControllerConfig.CODEC.register(
                Objects.requireNonNull(typeId, "typeId"),
                Objects.requireNonNull(configClass, "configClass"),
                Objects.requireNonNull(codec, "codec")
        );
    }

    /**
     * Registers a new summon asset subtype for {@code SummonConfig.CODEC}.
     */
    public <T extends SummonConfig> void registerSummonConfigType(
            @Nonnull String typeId,
            @Nonnull Class<T> configClass,
            @Nonnull BuilderCodec<T> codec
    ) {
        SummonConfig.CODEC.register(
                Objects.requireNonNull(typeId, "typeId"),
                Objects.requireNonNull(configClass, "configClass"),
                Objects.requireNonNull(codec, "codec")
        );
    }

    /**
     * Registers a summon-related interaction codec on the plugin interaction
     * registry.
     */
    public <T extends Interaction> void registerInteraction(
            @Nonnull String typeId,
            @Nonnull Class<T> interactionClass,
            @Nonnull BuilderCodec<T> codec
    ) {
        plugin.getCodecRegistry(Interaction.CODEC).register(
                Objects.requireNonNull(typeId, "typeId"),
                Objects.requireNonNull(interactionClass, "interactionClass"),
                Objects.requireNonNull(codec, "codec")
        );
    }
}


