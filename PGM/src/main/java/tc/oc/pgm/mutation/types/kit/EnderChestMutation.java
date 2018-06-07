package tc.oc.pgm.mutation.types.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.PGMTranslations;
import tc.oc.pgm.kits.FreeItemKit;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.Party;
import tc.oc.pgm.mutation.types.KitMutation;
import tc.oc.pgm.wool.WoolMatchModule;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

public class EnderChestMutation extends KitMutation {
    // TODO: Mutation name and lore

    // TODO: Prettify the given item with name and lore
    final static ItemKit CHEST_KIT = new FreeItemKit(item(Material.ENDER_CHEST));
    // TODO: Translations
    final static String CHEST_TITLE_KEY = "uhhh";
    final static int CHEST_SIZE = 27;

    final static Map<Party, Inventory> teamChests = new WeakHashMap<>();

    final boolean noWoolsAllowed;

    // T: Test me
    // @Inject public EnderChestMutation(Match match, Optional<WoolMatchModule> wmm) {
    public EnderChestMutation(Match match) {
        super(match, false, CHEST_KIT);
        // T: Is this injectable?
        this.noWoolsAllowed = match.getMatchModule(WoolMatchModule.class) == null;
        // this.noWoolsAllowed = wmm.isPresent();
    }

    // T: Don't know if to use ignoreCancelled
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestUse(PlayerInteractEvent event) {
        // Players right click to open inventories, might as well keep it the same here.
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getItem().getType() != Material.ENDER_CHEST) return;
        event.setCancelled(true);

        // T: Could this be null?
        Player bukkitPlayer = event.getPlayer();
        MatchPlayer player = match().getPlayer(bukkitPlayer);
        Party team = player.getParty();
        // T: I don't know if this check is necessary or not.
        if (team.isObserving()) return;

        @Nullable Inventory teamInventory = teamChests.get(team);

        // T: Should the inventories be initialized at the beginning of the match?
        if (teamInventory == null) {
            teamInventory = match().getServer().createInventory(null,
                    CHEST_SIZE,
                    PGMTranslations.t(CHEST_TITLE_KEY, player));
            teamChests.put(team, teamInventory);
        }

        bukkitPlayer.openInventory(teamInventory);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // T: I fear this first check may be a bit too resource intensive. I don't know if it is
        // T: Should I maintain a HashSet of the inventories too just for this check?
        if (teamChests.values().contains(event.getInventory()) &&
                isItemEvil(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isItemEvil(ItemStack item) {
        if(item.getType() == Material.ENDER_CHEST) return true;
        if(noWoolsAllowed && item.getType() == Material.WOOL) return true;

        return false;
    }
}
