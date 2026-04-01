package me.s3b4s5.summonlib.api;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Public entry point for side-mod bootstrap and registration.
 *
 * <p>Typical usage during another plugin's setup:</p>
 *
 * <pre>{@code
 * SummonLibRegistration reg = SummonLibApi.registration(this);
 * reg.registerFollowConfigType(...);
 * }</pre>
 *
 * <p>SummonLib itself also uses this surface for its built-in codec
 * registration so extension code follows the same path as the library.</p>
 */
public final class SummonLibApi {

    private SummonLibApi() {
    }

    @Nonnull
    public static SummonLibRegistration registration(@Nonnull JavaPlugin plugin) {
        return new SummonLibRegistration(Objects.requireNonNull(plugin, "plugin"));
    }

    /**
     * Registers SummonLib's built-in summon, follow, and NPC motion asset types.
     */
    public static void registerBuiltinTypes(@Nonnull JavaPlugin plugin) {
        SummonLibRegistrars.registerBuiltinTypes(registration(plugin));
    }

    /**
     * Registers SummonLib's built-in summon-related interactions.
     */
    public static void registerBuiltinInteractions(@Nonnull JavaPlugin plugin) {
        SummonLibRegistrars.registerBuiltinInteractions(registration(plugin));
    }
}


