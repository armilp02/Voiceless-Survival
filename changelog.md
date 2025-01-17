### Changelog

#### Version 0.1.0

- Updated to version 0.1.0 to reflect significant new features and improvements.
- Added dynamic adjustments to detection range and mob speed based on player whispering:
  - Whispering reduces detection range and speed using configurable `whisper_range_multiplier` and `whisper_speed_multiplier`.
  - Sneaking further reduces detection range when whispering or speaking normally. *(This will be improved with the stealth system update.)*
- Ported the mod to Minecraft 1.21.1

#### Version 0.0.5

- Fixed a bug where mobs would stay idle at the sound source when the player stopped talking.
- Improved `player interaction`: mobs now reset targets if out of range and use an attack cooldown to prevent constant attacks.

#### Version 0.0.4

- Added a system that iterates through configured mob IDs to check if the audio level exceeds their individual `threshold`.
- If the audio surpasses the threshold, the player's position is registered and remains active for 5 seconds.
