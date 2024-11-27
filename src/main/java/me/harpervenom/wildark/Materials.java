package me.harpervenom.wildark;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static me.harpervenom.wildark.WILDARK.getPlugin;

public class Materials {

    private static final List<Material> tools;
    private static final List<Material> exceptions;
    private static final List<Material> woodBlocks;

    private static final List<Material> lockBlocks;

    static {
        tools = Arrays.stream(Material.values())
                .filter(material ->
                        (material.name().startsWith("DIAMOND") || material.name().startsWith("NETHERITE"))
                                && (material.name().endsWith("PICKAXE") || material.name().endsWith("AXE") || material.name().endsWith("SHOVEL")))
                .collect(Collectors.toList());

        woodBlocks = Arrays.stream(Material.values())
                .filter(material ->
                        material.name().contains("OAK") || material.name().contains("SPRUCE") || material.name().contains("BIRCH")
                                || material.name().contains("ACACIA") || material.name().contains("JUNGLE") || material.name().contains("BAMBOO")
                                || material.name().contains("CHERRY") || material.name().contains("MANGROVE") || material.name().contains("CRIMSON")
                                || material.name().contains("WARPED")
                )
                .collect(Collectors.toList());

        lockBlocks = Arrays.stream(Material.values())
                .filter(material ->
                        (material.name().contains("DOOR") || material.name().contains("CHEST") || material == Material.BARREL))
                .collect(Collectors.toList());

        exceptions = Arrays.stream(Material.values())
                .filter(material ->
                        material == Material.SCAFFOLDING || (
                                material.name().contains("CARPET") || material.name().contains("SHULKER")
                                        || material.name().contains("CANDLE") || material.name().contains("VEIN") || material.name().contains("WEB")
                                        || material.name().contains("LICHEN") || material.name().contains("LEAF")

                        )
                )
                .collect(Collectors.toList());

//        exceptions.addAll(lockBlocks);
    }

    public static List<Material> getTools() {
        return tools;
    }

    public static List<Material> getLockBlocks() {
        return lockBlocks;
    }

    public static boolean isTrueBlock(Player p, Block b) {
        return !(Double.isInfinite(b.getBreakSpeed(p)) || exceptions.contains(b.getType()));
    }

    public static int getMaxBlockHealth(Block b) {
        Material type = b.getType();

        if (type == Material.NETHERITE_BLOCK) return 16;
        if (type == Material.DIAMOND_BLOCK) return 8;
        if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN
                || type == Material.IRON_BLOCK || type == Material.IRON_DOOR) {
            return 6;
        }

        return 3;
    }

    public static int getToolDamage(Block b, ItemStack tool) {
        if (tool.getType().name().contains("SHOVEL") && woodBlocks.contains(b.getType())) {
            return 1152;
        }
        return 384;
    }

    public static int getToolAttackDamage(ItemStack tool) {
        Material type = tool.getType();
        if (type.name().contains("WOODEN")) return getPlugin().getConfig().getInt("wooden_attack_damage");
        if (type.name().contains("STONE")) return getPlugin().getConfig().getInt("stone_attack_damage");
        if (type.name().contains("GOLDEN")) return getPlugin().getConfig().getInt("golden_attack_damage");
        if (type.name().contains("IRON")) return getPlugin().getConfig().getInt("iron_attack_damage");
        if (type.name().contains("DIAMOND")) return getPlugin().getConfig().getInt("diamond_attack_damage");
        if (type.name().contains("NETHERITE")) return getPlugin().getConfig().getInt("netherite_attack_damage");
        return 0;
    }
}
