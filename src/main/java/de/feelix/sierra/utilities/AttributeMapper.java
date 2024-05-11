package de.feelix.sierra.utilities;

import lombok.Getter;

/**
 * A utility class that maps attribute keys to their corresponding attribute values.
 * <p>
 * The AttributeMapper class is used to handle violations based on attribute modifiers in an ItemStack.
 */
@Getter
public enum AttributeMapper {

    Armor(new String[]{"generic.armor"}, 30, 0),

    ArmorTough(new String[]{"generic.armorToughness", "generic.armor_toughness"}, 20, 0),

    AttackDamage(new String[]{"generic.attackDamage", "generic.attack_damage"}, 2.048, 0),

    AttackKnockback(new String[]{"generic.attack_knockback", "generic.attack_knockback"}, 5, 0),

    AttackSpeed(new String[]{"generic.attackSpeed", "generic.attack_speed"}, 1.024, 0),

    FlyingSpeed(new String[]{"generic.flyingSpeed", "generic.flying_speed"}, 1.024, 0),

    HoseJumpStrength(new String[]{"horse.jumpStrength", "horse.jump_strength"}, 2, 0),

    JumpStrength(new String[]{"generic.jump_strength", "generic.jumpStrength"}, 32, 0),

    PlayerBlockInteraction(new String[]{"player.block_interaction_range", "player.blockInteractionRange"}, 64, 0),

    PlayerEntityRange(new String[]{"player.entity_interaction_range", "player.entityInteractionRange"}, 64, 0),

    PlayerBlockBreak(new String[]{"player.block_break_speed", "player.blockBreakSpeed"}, 1024, 0),

    KnockbackResistance(new String[]{"generic.knockbackResistance", "generic.knockback_resistance"}, 1, 0),

    Luck(new String[]{"generic.luck"}, 1.024, -1.024),

    Gravity(new String[]{"generic.gravity"}, 1, -1),

    FallDistance(new String[]{"generic.safe_fall_distance", "generic.safeFallDistance"}, 1.024, -1.024),

    FallDamageMultiplier(new String[]{"generic.fall_damage_multiplier", "generic.fallDamageMultiplier"}, 100, 0),

    MaxHealth(new String[]{"generic.maxHealth", "generic.max_health"}, 1, -1),

    MaxAbsorption(new String[]{"generic.max_absorption", "generic.maxAbsorption"}, 2.048, 0),

    Scale(new String[]{"generic.scale"}, 16, 0.0625),

    StepHeight(new String[]{"generic.step_height", "generic.stepHeight"}, 10, 0),

    FollowRange(new String[]{"generic.followRange", "generic.follow_range"}, 2.048, 0),

    MovementSpeed(new String[]{"generic.movementSpeed", "generic.movement_speed"}, 1.024, 0),

    SpawnReinforcements(new String[]{"zombie.spawnReinforcements", "zombie.spawn_reinforcements"}, 1, 0);

    /**
     * Represents a string array of keys.
     */
    private final String[] keys;

    /**
     * Represents the maximum value for an attribute.
     */
    private final double max;

    /**
     * Represents the minimum value for an attribute.
     */
    private final double min;

    /**
     * A utility class that maps attribute keys to their corresponding attribute values.
     */
    AttributeMapper(String[] keys, double max, double min) {
        this.keys = keys;
        this.max = max;
        this.min = min;
    }

    /**
     * Retrieves the AttributeMapper corresponding to the given key. The AttributeMapper is used to
     * handle violations based on attribute modifiers in an ItemStack.
     *
     * @param key The key to search for in the AttributeMapper enum.
     * @return The AttributeMapper corresponding to the given key, or null if no matching AttributeMapper is found.
     */
    public static AttributeMapper getAttributeMapper(String key) {
        key = key.toLowerCase().replace("minecraft:", "");
        for (AttributeMapper value : values()) {
            for (String s : value.keys) {
                if (s.equalsIgnoreCase(key)) {
                    return value;
                }
            }
        }
        return null;
    }
}
