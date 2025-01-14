# Voiceless Survival Addon

---

## 🛠️ **Dependency**

[Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)

---

### 🌟 **Description**

**Voiceless Survival Addon** introduces an immersive challenge to Minecraft! Hostile mobs can now **hear your voice** through your microphone (via [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)) and react to the sounds you make.

- **Speak cautiously:** Noise attracts mobs.
- **Stay silent:** Avoid detection and survive longer.

---

### 🔑 **Key Features**

- 🎙️ **Voice-based sound detection:** Mobs react to sounds from your microphone.
- ⚙️ **Customizable settings:** Adjust mob behavior in the configuration file.
- 🐾 **Supports all mobs:** Compatible with both vanilla and modded mobs.

---

### 📋 **Planned Features**

- 🔇 **Whispering mode:** Mobs detect lower sounds at reduced ranges when you speak softly.
- 🏃‍♂️ **Flee Feature:** Mobs will flee from players or other mobs based on configurable parameters.
- 🔊 **Sound Effects Impact:** Configurable effects to sounds around the world, allowing mobs to react or flee based on sound intensity.
- 🕵️‍♂️ **Stealth System:** Players can avoid mob detection by sneaking silently and staying out of sight.

---

### 🎮 **Gameplay Example**

> *What happens when you talk near a zombie?*

![Zombie Example](https://i.imgur.com/o1bWTs8.gif)

---

### 🛠️ **Configuration Guide**

1. **Locate the Config File**
    - The file is located in the `config/` directory of your Minecraft instance.
    - File name: `ezvcsurvival-common.toml`

2. **Add Custom Mob Settings**
    - Use the following format to define mob behavior:

      ```toml
      mob_configs = [
          "minecraft:zombie=speed=1.5,range=20,threshold=-40.0",
          "minecraft:skeleton=speed=1.2,range=15,threshold=-35.0",
          "moddedmob:example=speed=0.8,range=10,threshold=-50.0"
      ]
      ```

3. **Save and Restart**
    - Save the file and restart your game or server to apply changes.

---

### 🏆 **Credits**

- **[Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)** by Max Henkel – Essential for microphone integration.
- **Voiceless Survival Addon** adds gameplay mechanics without redistributing Simple Voice Chat.
