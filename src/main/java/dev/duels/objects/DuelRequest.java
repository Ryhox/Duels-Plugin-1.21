    package dev.duels.objects;

    import java.util.UUID;

    public class DuelRequest {

        private final UUID sender;
        private final UUID target;
        private final String kitName;
        private final String arenaName;
        private final int bestOf;
        private final long timestamp;

        public DuelRequest(UUID sender, UUID target, String kitName, String arenaName, int bestOf) {
            this.sender = sender;
            this.target = target;
            this.kitName = kitName;
            this.arenaName = arenaName;
            this.bestOf = bestOf;
            this.timestamp = System.currentTimeMillis();
        }




        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public String getKitName() { return kitName; }
        public int getBestOf() { return bestOf; }
        public long getTimestamp() { return timestamp; }
        public String getArenaName() {return arenaName; }

        public boolean isExpired(int timeoutSeconds) {
            return System.currentTimeMillis() - timestamp > (timeoutSeconds * 1000L);
        }
    }