### Changelog

#### Version 0.6.1

- Now you can configure `any sound`, whether from Minecraft or Mods, to affect mob behavior.
- `Hostile mobs` move toward the sound, while `passive mobs` flee.

#### Version 0.4.1

- Organized the project structure to improve maintainability.
- Added "Flee" mechanic, enhancing gameplay dynamics.

#### Version 0.2.1

- Now mobs ignore you in creative mode
- Improved Voicechat API Compatibility (2.5.0)

#### Version 0.2.0

- Sneaking reduces detection range (configurable via `sneaking_range_multiplier`).
- Rain and thunderstorms reduce detection range (configurable via `thunder_range_multiplier`).
- Sound intensity now accounts for distance using a logarithmic formula.

#### Version 0.1.0

- Updated to version 0.1.0 to reflect significant new features and improvements.
- Added dynamic adjustments to detection range and mob speed based on player whispering:
    - Whispering reduces detection range and speed using configurable `whisper_range_multiplier` and `whisper_speed_multiplier`.
    - Sneaking further reduces detection range when whispering or speaking normally. *(This will be improved with the stealth system update.)*

#### Version 0.0.5

- Fixed a bug where mobs would stay idle at the sound source when the player stopped talking.
- Improved `player interaction`: mobs now reset targets if out of range and use an attack cooldown to prevent constant attacks.

#### Version 0.0.4

- Added a system that iterates through configured mob IDs to check if the audio level exceeds their individual `threshold`.
- If the audio surpasses the threshold, the player's position is registered and remains active for 5 seconds.