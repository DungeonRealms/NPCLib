/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package net.jitse.npclib.nms.holograms;

import com.comphenix.tinyprotocol.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author Jitse Boonstra
 */
public class Hologram {

    private final double delta = 0.3;

    private Set<UUID> shown = new HashSet<>();
    private List<Object> armorStands = new ArrayList<>();
    private List<Object> spawnPackets = new LinkedList<>();
    private List<Object> destroyPackets = new LinkedList<>();

    // Classes:
    private static final Class<?> CHAT_COMPONENT_TEXT_CLAZZ = Reflection.getMinecraftClass("ChatComponentText");
    private static final Class<?> CHAT_BASE_COMPONENT_CLAZZ = Reflection.getMinecraftClass("IChatBaseComponent");
    private static final Class<?> ENTITY_ARMOR_STAND_CLAZZ = Reflection.getMinecraftClass("EntityArmorStand");
    private static final Class<?> ENTITY_LIVING_CLAZZ = Reflection.getMinecraftClass("EntityLiving");
    private static final Class<?> ENTITY_CLAZZ = Reflection.getMinecraftClass("Entity");
    private static final Class<?> CRAFT_BUKKIT_CLASS = Reflection.getCraftBukkitClass("CraftWorld");
    private static final Class<?> CRAFT_PLAYER_CLAZZ = Reflection.getCraftBukkitClass("entity.CraftPlayer");
    private static final Class<?> PACKET_PLAY_OUT_SPAWN_ENTITY_LIVING_CLAZZ = Reflection.getMinecraftClass(
            "PacketPlayOutSpawnEntityLiving");
    private static final Class<?> PACKET_PLAY_OUT_ENTITY_DESTROY_CLAZZ = Reflection.getMinecraftClass(
            "PacketPlayOutEntityDestroy");
    private static final Class<?> ENTITY_PLAYER_CLAZZ = Reflection.getMinecraftClass("EntityPlayer");
    private static final Class<?> PLAYER_CONNECTION_CLAZZ = Reflection.getMinecraftClass("PlayerConnection");
    private static final Class<?> PACKET_CLAZZ = Reflection.getMinecraftClass("Packet");

    // Constructors:
    private static final Reflection.ConstructorInvoker CHAT_COMPONENT_TEXT_CONSTRUCTOR = Reflection
            .getConstructor(CHAT_COMPONENT_TEXT_CLAZZ, String.class);
    private static final Reflection.ConstructorInvoker PACKET_PLAY_OUT_SPAWN_ENTITY_LIVING_CONSTRUCTOR = Reflection
            .getConstructor(PACKET_PLAY_OUT_SPAWN_ENTITY_LIVING_CLAZZ, ENTITY_LIVING_CLAZZ);
    private static final Reflection.ConstructorInvoker PACKET_PLAY_OUT_ENTITY_DESTROY_CONSTRUCTOR = Reflection
            .getConstructor(PACKET_PLAY_OUT_ENTITY_DESTROY_CLAZZ, int[].class);

    // Fields:
    private static final Reflection.FieldAccessor playerConnectionField = Reflection.getField(ENTITY_PLAYER_CLAZZ,
            "playerConnection", PLAYER_CONNECTION_CLAZZ);

    // Methods:
    private static final Reflection.MethodInvoker SET_LOCATION_METHOD = Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
            "setLocation", double.class, double.class, double.class, float.class, float.class);
    private static final Reflection.MethodInvoker SET_SMALL_METHOD = Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
            "setSmall", boolean.class);
    private static final Reflection.MethodInvoker SET_INVISIBLE_METHOD = Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
            "setInvisible", boolean.class);
    private static final Reflection.MethodInvoker SET_BASE_PLATE_METHOD = Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
            "setBasePlate", boolean.class);
    private static final Reflection.MethodInvoker SET_ARMS_METHOD = Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
            "setArms", boolean.class);
    private static final Reflection.MethodInvoker PLAYER_GET_HANDLE_METHOD = Reflection.getMethod(CRAFT_PLAYER_CLAZZ,
            "getHandle");
    private static final Reflection.MethodInvoker SEND_PACKET_METHOD = Reflection.getMethod(PLAYER_CONNECTION_CLAZZ,
            "sendPacket", PACKET_CLAZZ);
    private static final Reflection.MethodInvoker GET_ID_METHOD = Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
            "getId");

    private final Location start;
    private final Object worldServer;

    private boolean above_1_12_r1;
    private List<String> lines;

    public Hologram(Location location, List<String> lines) {
        this.start = location;
        this.lines = lines;

        this.worldServer = Reflection.getMethod(CRAFT_BUKKIT_CLASS, "getHandle")
                .invoke(CRAFT_BUKKIT_CLASS.cast(location.getWorld()));

    }

    public void generatePackets(boolean above1_9_r2, boolean above_1_12_r1) {
        this.above_1_12_r1 = above_1_12_r1;

        Reflection.MethodInvoker gravityMethod = (above1_9_r2 ? Reflection.getMethod(ENTITY_CLAZZ,
                "setNoGravity", boolean.class) : Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
                "setGravity", boolean.class));

        Reflection.MethodInvoker customNameMethod = (above_1_12_r1 ? Reflection.getMethod(ENTITY_CLAZZ,
                "setCustomName", CHAT_BASE_COMPONENT_CLAZZ) : Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
                "setCustomName", String.class));

        Reflection.MethodInvoker customNameVisibilityMethod = (above_1_12_r1 ? Reflection.getMethod(ENTITY_CLAZZ,
                "setCustomNameVisible", boolean.class) : Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
                "setCustomNameVisible", boolean.class));

        Location location = start.clone().add(0, delta * lines.size(), 0);
        Class<?> worldClass = worldServer.getClass().getSuperclass();

        if (start.getWorld().getEnvironment() != World.Environment.NORMAL) {
            worldClass = worldClass.getSuperclass();
        }

        Reflection.ConstructorInvoker entityArmorStandConstructor = Reflection
                .getConstructor(ENTITY_ARMOR_STAND_CLAZZ, worldClass);

        for (String line : lines) {
            Object entityArmorStand = entityArmorStandConstructor.invoke(worldServer);

            SET_LOCATION_METHOD.invoke(entityArmorStand, location.getX(), location.getY(), location.getZ(), 0, 0);
            customNameMethod.invoke(entityArmorStand, above_1_12_r1 ? CHAT_COMPONENT_TEXT_CONSTRUCTOR.invoke(line) : line);
            customNameVisibilityMethod.invoke(entityArmorStand, true);
            gravityMethod.invoke(entityArmorStand, above1_9_r2);
            SET_SMALL_METHOD.invoke(entityArmorStand, true);
            SET_INVISIBLE_METHOD.invoke(entityArmorStand, true);
            SET_BASE_PLATE_METHOD.invoke(entityArmorStand, false);
            SET_ARMS_METHOD.invoke(entityArmorStand, false);

            location.subtract(0, delta, 0);

            armorStands.add(entityArmorStand);

            Object spawnPacket = PACKET_PLAY_OUT_SPAWN_ENTITY_LIVING_CONSTRUCTOR.invoke(entityArmorStand);
            spawnPackets.add(spawnPacket);

            Object destroyPacket = PACKET_PLAY_OUT_ENTITY_DESTROY_CONSTRUCTOR
                    .invoke(new int[]{(int) GET_ID_METHOD.invoke(entityArmorStand)});
            destroyPackets.add(destroyPacket);
        }
    }

    public void updateText(List<String> newLines) {
        if (lines.size() != newLines.size()) {
            throw new IllegalArgumentException("New NPC text cannot differ in size from old text.");
        }

        Reflection.MethodInvoker customNameVisibilityMethod = (above_1_12_r1 ? Reflection.getMethod(ENTITY_CLAZZ,
                "setCustomNameVisible", boolean.class) : Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
                "setCustomNameVisible", boolean.class));

        Reflection.MethodInvoker customNameMethod = (above_1_12_r1 ? Reflection.getMethod(ENTITY_CLAZZ,
                "setCustomName", CHAT_BASE_COMPONENT_CLAZZ) : Reflection.getMethod(ENTITY_ARMOR_STAND_CLAZZ,
                "setCustomName", String.class));

        int i = 0;
        for (String oldLine : lines) {
            if (oldLine.isEmpty() && !newLines.get(i).isEmpty()) {
                // Need to spawn
                for (UUID uuid : shown) {
                    Player player = Bukkit.getPlayer(uuid);
                    Object playerConnection = playerConnectionField.get(PLAYER_GET_HANDLE_METHOD
                            .invoke(CRAFT_PLAYER_CLAZZ.cast(player)));
                    SEND_PACKET_METHOD.invoke(playerConnection, spawnPackets.get(i));
                }
            } else if (!oldLine.isEmpty() && newLines.get(i).isEmpty()) {
                // Need to destroy
                for (UUID uuid : shown) {
                    Player player = Bukkit.getPlayer(uuid);
                    Object playerConnection = playerConnectionField.get(PLAYER_GET_HANDLE_METHOD
                            .invoke(CRAFT_PLAYER_CLAZZ.cast(player)));
                    SEND_PACKET_METHOD.invoke(playerConnection, destroyPackets.get(i));
                }
            }

            // TODO: Send PacketPlayOutEntityMetadata to all players (that the hologram is visible to).
//            PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata();
//            Object armorstand = armorStands.get(i);
//            int entityId = (int) Reflection.getMethod(EntityArmorStand.class, "getId", int.class).invoke(armorstand);
//            Reflection.getField(PacketPlayOutEntityMetadata.class, "a", int.class).set(packet, entityId);
//            List list = Collections.singletonList(new WatchableObject(entityId, value));
//            Reflection.getField(EntityArmorStand.class, "b", List.class).set(packet, list);

            Object entityArmorStand = armorStands.get(i);
            String newLine = newLines.get(i);
            customNameMethod.invoke(entityArmorStand, above_1_12_r1 ? CHAT_COMPONENT_TEXT_CONSTRUCTOR.invoke(newLine) : newLine);

            i++;
        }

        this.lines = newLines;
    }

    public void spawn(Player player) {
        if (shown.contains(player.getUniqueId())) {
            throw new IllegalArgumentException("Hologram is already shown to player");
        }

        shown.add(player.getUniqueId());

        Object playerConnection = playerConnectionField.get(PLAYER_GET_HANDLE_METHOD
                .invoke(CRAFT_PLAYER_CLAZZ.cast(player)));

        int i = 0;
        for (String line : lines) {
            if (line.isEmpty()) {
                i++;
                continue;
            }

            SEND_PACKET_METHOD.invoke(playerConnection, spawnPackets.get(i));
            i++;
        }
    }

    public void destroy(Player player) {
        if (!shown.contains(player.getUniqueId())) {
            throw new IllegalArgumentException("Hologram is not shown to player");
        }

        shown.remove(player.getUniqueId());

        Object playerConnection = playerConnectionField.get(PLAYER_GET_HANDLE_METHOD
                .invoke(CRAFT_PLAYER_CLAZZ.cast(player)));

        int i = 0;
        for (String line : lines) {
            if (line.isEmpty()) {
                i++;
                continue;
            }

            SEND_PACKET_METHOD.invoke(playerConnection, destroyPackets.get(i));
            i++;
        }
    }
}
