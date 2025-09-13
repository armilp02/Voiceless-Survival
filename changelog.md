## Version 2.0.0

- 🔄️ **Changes:**
  - Goal injection is now handled through Mixins.
  - General sounds are now separated from gun sounds.
  - Mobs no longer react to sounds when targeting the player.
  - Rework PointBlank GunFire detection.
  - Enhance handling of sound events.
  - The configuration system has been reorganized for better clarity and usability.
  - Update audio level calculation.
  - Disabled Sound Physics Remastered mod to refactor it in a future update.


- ✅ **Added:**
    - Add SuperBWarfare Mod support.
    - Mobs now get a speed multiplier when targeting a player who is speaking. 
    - **New JSON Config Files:**
      - `entity_voices.json`: Defines how entities react to your voice, applicable to both vanilla and modded mobs.
      - `generalsounds.json`: Registers all mobs and sound events listed in SOUND_EVENT for easier management.
      - `gunfire.json`: Configures mob reactions to gunfire and prioritizes sound events related to shooting.
    - These configs are fully editable in-game via `/ezvcconfig`, allowing you to tweak every parameter.
  