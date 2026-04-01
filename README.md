# SummonLib

SummonLib is an asset-driven minion/summon library for Hytale server mods.  
It lets you define summons in JSON assets and then invoke or remove them through interactions and items without hardcoding per-summon logic.

It also exposes an API for code-driven customization, so mods can modify or extend summon behavior through Java when assets alone are not enough.

## What it does

- **Asset-driven summons**: define summon behavior and parameters via `SummonConfig` assets.
- **Code-driven extension API**: supports modifying and extending summon-related behavior through code.
- **Summon types** (current):
    - **MODEL**: a non-NPC model summon that follows the player using formation logic (no real navigation; ignores blocks). *(stable)*
    - **NPC_ROLE**: an NPC-based summon that moves with actual navigation and collides with blocks. *(stable)*
    - **WORM**: a new summon type currently in **WIP**. *(not stable yet)*
- **Simple casting/removal**: wire summon actions through Interactions and RootInteractions for items to use.

## Links

- [CurseForge](https://www.curseforge.com/hytale/mods/summonlib).
- [Wiki / Documentation](https://github.com/S3B4S5C/SummonLib-Docs/wiki).
- Issues / Bug Reports / Suggestions: join the lib's [Discord Server](https://discord.gg/3PZP8SbYRm).

## Status

This project is actively developed and may change as new features (formations, additional summon behaviors, expanded tuning, etc.) are added.

## License

See `LICENSE`.