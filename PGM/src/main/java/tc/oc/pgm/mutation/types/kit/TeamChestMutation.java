package tc.oc.pgm.mutation.types.kit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.commons.bukkit.inventory.Slot;
import tc.oc.commons.bukkit.item.ItemBuilder;
import tc.oc.commons.core.collection.WeakHashSet;
import tc.oc.pgm.PGMTranslations;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.SlotItemKit;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.Party;
import tc.oc.pgm.mutation.types.KitMutation;
import tc.oc.pgm.wool.WoolMatchModule;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import static org.bukkit.Bukkit.getLogger;

public class TeamChestMutation extends KitMutation {
    final static int SLOT_ID = 17; // Top right
    final static Material TOOL_TYPE = Material.ENDER_CHEST;
    final static String ITEM_NAME_KEY = "mutation.type.teamchest.item_name";
    final static String ITEM_LORE_KEY = "mutation.type.teamchest.item_lore";
    final static int CHEST_SIZE = 27;

    final Map<Party, Inventory> teamChests = new WeakHashMap<>();
    final Set<Inventory> inventorySet;

    final boolean woolsAllowed;

    // TODO: Test me
    // @Inject public TeamChestMutation(Match match, Optional<WoolMatchModule> wmm) {
    public TeamChestMutation(Match match) {
        super(match, false);
        // T: Is this injectable?
        this.woolsAllowed = match.getMatchModule(WoolMatchModule.class) == null;
        // this.woolsAllowed = !wmm.isPresent();
        for (Party party : match().getParties()) {
            if (party.isParticipatingType()) {
                // Could the chest title be localized properly?
                teamChests.put(party, match().getServer().createInventory(null, CHEST_SIZE));
            }
        }
        // Cached for performance
        inventorySet = new WeakHashSet<>(teamChests.values());
    }

    @Override
    public void kits(MatchPlayer player, List<Kit> kits) {
        super.kits(player, kits);
        kits.add(getKitForPlayer(player));
    }
    // Open shared inventory instead of placing the chest
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestUse(PlayerInteractEvent event) {
        // Players right click to open inventories, might as well keep it the same here.
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getItem().getType() != TOOL_TYPE) return;

        // T: Can this be null?
        Player bukkitPlayer = event.getPlayer();
        Optional<Inventory> oTeamInventory = getTeamsInventory(bukkitPlayer);

        oTeamInventory.ifPresent(new Consumer<Inventory>() {
            @Override
            public void accept(Inventory teamInventory) {
                event.setCancelled(true);
                bukkitPlayer.openInventory(teamInventory);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // No putting evil items (ender chest, possibly wool) into the chest
        // BUG: Sometimes this check does not work.
        if ((inventorySet.contains(event.getView().getTopInventory()) ||
                inventorySet.contains(event.getView().getBottomInventory())) &&
                isItemEvil(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }

        // If normal right click, in their inventory, on the chest, then open shared inventory.
        getTeamsInventory(event.getActor()).ifPresent(new Consumer<Inventory>() {
            @Override
            public void accept(Inventory teamInventory) {
                if (event.getInventory().getType() == InventoryType.CRAFTING &&
                        event.getCurrentItem().getType() == TOOL_TYPE &&
                        event.getAction() == InventoryAction.PICKUP_HALF) {
                    event.setCancelled(true);
                    // This resets their mouse position annoyingly, but without it items can get stuck in places.
                    event.getActor().closeInventory();
                    event.getActor().openInventory(teamInventory);
                }
            }
        });
    }

    private boolean isItemEvil(ItemStack item) {
        if(item.getType() == TOOL_TYPE) return true;
        if(!woolsAllowed && item.getType() == Material.WOOL) return true;

        return false;
    }

    private Optional<Inventory> getTeamsInventory(Player bukkitPlayer) {
        MatchPlayer player = match().getPlayer(bukkitPlayer);
        Party team = player.getParty();

        if (!team.isParticipating()) return Optional.empty();

        Inventory teamInventory = teamChests.get(team);
        if (teamInventory == null) {
            throw new IllegalStateException("Team Chest Inventory does not exist for participating team");
        }

        return Optional.of(teamInventory);
    }

    private Kit getKitForPlayer(MatchPlayer player) {
        ItemStack stack = new ItemBuilder(item(TOOL_TYPE))
                .name(ChatColor.DARK_PURPLE + PGMTranslations.t(ITEM_NAME_KEY, player))
                .lore(ChatColor.DARK_AQUA + PGMTranslations.t(ITEM_LORE_KEY, player))
                .get();

        ItemKit kit = new SlotItemKit(stack, Slot.Player.forIndex(SLOT_ID));
        return kit;
    }
}
