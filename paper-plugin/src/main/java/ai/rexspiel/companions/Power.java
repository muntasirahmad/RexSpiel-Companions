package ai.rexspiel.companions;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public record Power(String name, ChatColor color, Material icon, PotionEffectType passive, int amplifier, Particle particle) {
    private static final List<String> AI_NAMES = List.of(
        "ChatGPT", "Meta AI", "Grok", "Claude", "Gemini", "DeepSeek", "Qwen", "Mistral",
        "OpenAI", "Copilot", "Perplexity", "Llama", "Command R", "Cohere", "Sora",
        "Nova", "Kimi", "Yi", "ERNIE", "Poe", "Le Chat", "Suno AI", "Pi AI",
        "Character AI", "Replika", "Cursor AI", "Phind", "Mistral Large", "Gemma", "Qwen Max",
        "DeepSeek R1", "Grok Vision", "Claude Sonnet", "Gemini Flash", "GPT-o", "Meta Llama",
        "OpenRouter AI", "HuggingChat", "Cerebras AI", "Fireworks AI", "Together AI",
        "Sambanova AI", "Cohere Aya", "Amazon Nova", "Microsoft Phi", "xAI", "Z.ai",
        "GLM", "Ollama AI", "RexSpiel AI"
    );
    public static final List<Power> ALL = List.of(
        p("Flame Guardian", ChatColor.RED, Material.BLAZE_POWDER, PotionEffectType.FIRE_RESISTANCE, 0, Particle.FLAME),
        p("Storm Runner", ChatColor.AQUA, Material.LIGHTNING_ROD, PotionEffectType.SPEED, 1, Particle.ELECTRIC_SPARK),
        p("Iron Skin", ChatColor.GRAY, Material.IRON_INGOT, PotionEffectType.RESISTANCE, 1, Particle.CRIT),
        p("Regenerator", ChatColor.GREEN, Material.GHAST_TEAR, PotionEffectType.REGENERATION, 0, Particle.HEART),
        p("Night Hunter", ChatColor.DARK_PURPLE, Material.ENDER_EYE, PotionEffectType.NIGHT_VISION, 0, Particle.PORTAL),
        p("Jump Master", ChatColor.YELLOW, Material.RABBIT_FOOT, PotionEffectType.JUMP_BOOST, 1, Particle.CLOUD),
        p("Aqua Soul", ChatColor.BLUE, Material.HEART_OF_THE_SEA, PotionEffectType.WATER_BREATHING, 0, Particle.FALLING_WATER),
        p("Haste Core", ChatColor.GOLD, Material.GOLDEN_PICKAXE, PotionEffectType.HASTE, 1, Particle.WAX_ON),
        p("Shadow Step", ChatColor.DARK_GRAY, Material.BLACK_DYE, PotionEffectType.SPEED, 0, Particle.SMOKE),
        p("Frost Walker", ChatColor.WHITE, Material.POWDER_SNOW_BUCKET, PotionEffectType.RESISTANCE, 0, Particle.CLOUD),
        p("Beacon Heart", ChatColor.LIGHT_PURPLE, Material.BEACON, PotionEffectType.ABSORPTION, 1, Particle.END_ROD),
        p("Wolf Spirit", ChatColor.RED, Material.BONE, PotionEffectType.STRENGTH, 0, Particle.SOUL),
        p("Vampire Fang", ChatColor.DARK_RED, Material.FERMENTED_SPIDER_EYE, PotionEffectType.SATURATION, 0, Particle.DAMAGE_INDICATOR),
        p("Wind Dancer", ChatColor.WHITE, Material.FEATHER, PotionEffectType.SLOW_FALLING, 0, Particle.CLOUD),
        p("Miner's Luck", ChatColor.GOLD, Material.EMERALD, PotionEffectType.LUCK, 1, Particle.HAPPY_VILLAGER),
        p("Guardian Shell", ChatColor.BLUE, Material.SHIELD, PotionEffectType.RESISTANCE, 0, Particle.TOTEM_OF_UNDYING),
        p("Battle Focus", ChatColor.RED, Material.IRON_SWORD, PotionEffectType.STRENGTH, 0, Particle.CRIT),
        p("Swift Blade", ChatColor.AQUA, Material.DIAMOND_SWORD, PotionEffectType.SPEED, 1, Particle.SWEEP_ATTACK),
        p("Emerald Mind", ChatColor.GREEN, Material.EMERALD_BLOCK, PotionEffectType.HERO_OF_THE_VILLAGE, 0, Particle.HAPPY_VILLAGER),
        p("Soul Fire", ChatColor.BLUE, Material.SOUL_LANTERN, PotionEffectType.FIRE_RESISTANCE, 0, Particle.SOUL_FIRE_FLAME),
        p("Web Breaker", ChatColor.WHITE, Material.STRING, PotionEffectType.SPEED, 0, Particle.BLOCK),
        p("Poison Ward", ChatColor.DARK_GREEN, Material.MILK_BUCKET, PotionEffectType.RESISTANCE, 0, Particle.CLOUD),
        p("Dragon Blood", ChatColor.LIGHT_PURPLE, Material.DRAGON_BREATH, PotionEffectType.FIRE_RESISTANCE, 0, Particle.DRAGON_BREATH),
        p("Wither Guard", ChatColor.DARK_GRAY, Material.WITHER_SKELETON_SKULL, PotionEffectType.RESISTANCE, 1, Particle.ASH),
        p("End Walker", ChatColor.DARK_PURPLE, Material.CHORUS_FRUIT, PotionEffectType.SLOW_FALLING, 0, Particle.REVERSE_PORTAL),
        p("Ocean King", ChatColor.BLUE, Material.TRIDENT, PotionEffectType.DOLPHINS_GRACE, 0, Particle.BUBBLE),
        p("Desert Fox", ChatColor.GOLD, Material.SAND, PotionEffectType.SPEED, 0, Particle.FALLING_DUST),
        p("Jungle Heart", ChatColor.GREEN, Material.VINE, PotionEffectType.REGENERATION, 0, Particle.COMPOSTER),
        p("Cave Echo", ChatColor.DARK_GRAY, Material.SCULK, PotionEffectType.NIGHT_VISION, 0, Particle.SCULK_SOUL),
        p("Thunder Fist", ChatColor.YELLOW, Material.COPPER_BLOCK, PotionEffectType.STRENGTH, 1, Particle.ELECTRIC_SPARK),
        p("Crystal Ward", ChatColor.LIGHT_PURPLE, Material.AMETHYST_SHARD, PotionEffectType.ABSORPTION, 0, Particle.END_ROD),
        p("Golden Soul", ChatColor.YELLOW, Material.GOLDEN_APPLE, PotionEffectType.REGENERATION, 0, Particle.WAX_ON),
        p("Ruby Rage", ChatColor.RED, Material.REDSTONE, PotionEffectType.STRENGTH, 1, Particle.DAMAGE_INDICATOR),
        p("Sapphire Shield", ChatColor.BLUE, Material.LAPIS_LAZULI, PotionEffectType.RESISTANCE, 1, Particle.BUBBLE_POP),
        p("Verdant Touch", ChatColor.GREEN, Material.MOSS_BLOCK, PotionEffectType.REGENERATION, 0, Particle.COMPOSTER),
        p("Void Guard", ChatColor.DARK_PURPLE, Material.OBSIDIAN, PotionEffectType.RESISTANCE, 0, Particle.REVERSE_PORTAL),
        p("Star Caller", ChatColor.WHITE, Material.NETHER_STAR, PotionEffectType.LUCK, 1, Particle.FIREWORK),
        p("Hunter's Eye", ChatColor.DARK_GREEN, Material.SPYGLASS, PotionEffectType.NIGHT_VISION, 0, Particle.GLOW),
        p("Berserker", ChatColor.DARK_RED, Material.NETHERITE_AXE, PotionEffectType.STRENGTH, 1, Particle.CRIT),
        p("Medic", ChatColor.GREEN, Material.GOLDEN_CARROT, PotionEffectType.REGENERATION, 0, Particle.HEART),
        p("Anchor", ChatColor.GRAY, Material.RESPAWN_ANCHOR, PotionEffectType.RESISTANCE, 1, Particle.BLOCK),
        p("Comet", ChatColor.AQUA, Material.ICE, PotionEffectType.SPEED, 1, Particle.CLOUD),
        p("Magma Core", ChatColor.RED, Material.MAGMA_CREAM, PotionEffectType.FIRE_RESISTANCE, 0, Particle.LAVA),
        p("Spectral", ChatColor.WHITE, Material.SPECTRAL_ARROW, PotionEffectType.INVISIBILITY, 0, Particle.FIREWORK),
        p("Sun Blessing", ChatColor.YELLOW, Material.SUNFLOWER, PotionEffectType.REGENERATION, 0, Particle.END_ROD),
        p("Moon Blessing", ChatColor.BLUE, Material.BLUE_ORCHID, PotionEffectType.NIGHT_VISION, 0, Particle.ENCHANT),
        p("Titan", ChatColor.GRAY, Material.IRON_BLOCK, PotionEffectType.RESISTANCE, 1, Particle.BLOCK),
        p("Phoenix", ChatColor.RED, Material.PHANTOM_MEMBRANE, PotionEffectType.FIRE_RESISTANCE, 0, Particle.FLAME),
        p("Ranger", ChatColor.GREEN, Material.BOW, PotionEffectType.SPEED, 0, Particle.CRIT),
        p("Dread Knight", ChatColor.DARK_GRAY, Material.NETHERITE_SWORD, PotionEffectType.RESISTANCE, 0, Particle.SOUL),
        p("Lucky Star", ChatColor.YELLOW, Material.NETHER_STAR, PotionEffectType.LUCK, 2, Particle.TOTEM_OF_UNDYING)
    );

    public static Power strongest() {
        return ALL.get(22); // Dragon Blood: fixed strongest special companion.
    }

    public String aiName() {
        return AI_NAMES.get(ALL.indexOf(this) % AI_NAMES.size());
    }

    public static Power randomLesser() {
        Power selected;
        do {
            selected = ALL.get((int) (Math.random() * ALL.size()));
        } while (selected.name().equals(strongest().name()));
        return selected;
    }

    private static Power p(String n, ChatColor c, Material m, PotionEffectType e, int a, Particle fx) {
        return new Power(n, c, m, e, a, fx);
    }
}
