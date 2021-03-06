package tc.oc.pgm.tracker.trackers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Listener;
import tc.oc.commons.core.logging.Loggers;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.ParticipantState;
import tc.oc.pgm.tracker.EntityResolver;
import tc.oc.pgm.tracker.damage.EntityInfo;
import tc.oc.pgm.tracker.damage.FallingBlockInfo;
import tc.oc.pgm.tracker.damage.MobInfo;
import tc.oc.pgm.tracker.damage.OwnerInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.damage.PlayerInfo;
import tc.oc.pgm.tracker.damage.ThrownPotionInfo;
import tc.oc.pgm.tracker.damage.TrackerInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tracks the ownership of {@link Entity}s and resolves damage caused by them
 */
public class EntityTracker implements EntityResolver, Listener {

    private final Logger logger;
    private final Match match;
    private final Map<Entity, TrackerInfo> entities = new HashMap<>();

    @Inject EntityTracker(Loggers loggers, Match match) {
        this.logger = loggers.get(getClass());
        this.match = match;
    }

    public PhysicalInfo createEntity(Entity entity, @Nullable ParticipantState owner) {
        if(entity instanceof ThrownPotion) {
            return new ThrownPotionInfo((ThrownPotion) entity, owner);
        } else if(entity instanceof FallingBlock) {
            return new FallingBlockInfo((FallingBlock) entity, owner);
        } else if(entity instanceof LivingEntity) {
            return new MobInfo((LivingEntity) entity, owner);
        } else {
            return new EntityInfo(entity, owner);
        }
    }

    @Override
    public PhysicalInfo resolveEntity(Entity entity) {
        MatchPlayer player = match.getParticipant(entity);
        if(player != null) {
            return new PlayerInfo(player);
        }

        TrackerInfo info = entities.get(entity);
        if(info instanceof PhysicalInfo) return (PhysicalInfo) info;

        ParticipantState owner = info instanceof OwnerInfo ? ((OwnerInfo) info).getOwner()
                                                           : null;
        return createEntity(entity, owner);
    }

    @Override
    public @Nullable TrackerInfo resolveInfo(Entity entity) {
        return entities.get(checkNotNull(entity));
    }

    @Override
    public @Nullable ParticipantState getOwner(Entity entity) {
        if(entity instanceof Player) {
            return match.getParticipantState(entity); // Players own themselves
        } else {
            OwnerInfo info = resolveInfo(entity, OwnerInfo.class);
            return info == null ? null : info.getOwner();
        }
    }

    public void trackEntity(Entity entity, @Nullable TrackerInfo info) {
        checkNotNull(entity);
        if(info == null) {
            entities.remove(entity);
            logger.fine("Clear entity=" + entity);
        } else {
            entities.put(entity, info);
            logger.fine("Track entity=" + entity + " info=" + info);
        }
    }
}
