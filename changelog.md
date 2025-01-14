### Changelog

#### Version 0.0.5

- Fixed a bug where mobs would stay idle at the sound source when the player stopped talking.
- Improved `player interaction`: mobs now reset targets if out of range and use an attack cooldown to prevent constant attacks.

#### Version 0.0.4

- Added a system that iterates through configured mob IDs to check if the audio level exceeds their individual `threshold`.
- If the audio surpasses the threshold, the player's position is registered and remains active for 5 seconds.
