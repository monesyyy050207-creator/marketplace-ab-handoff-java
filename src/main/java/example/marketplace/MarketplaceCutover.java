package example.marketplace;

import example.infrai.InfraiClient;

/** Executable cutover path: resolve the buyer, then read the stable experiment bucket. */
public final class MarketplaceCutover {
    private final InfraiClient infrai;

    public MarketplaceCutover(InfraiClient infrai) { this.infrai = infrai; }

    public Handoff prepare(String userId, String flagKey, String sellerAsset,
                           String buyerUpdate, String orderId)
            throws Exception {
        String buyer = infrai.user(userId);
        String assignment = infrai.bucket(flagKey, userId);
        String variant = assignment.contains("control") ? "control" : "treatment";
        return new Handoff(orderId, userId, sellerAsset, buyerUpdate, variant,
                buyer, "cutover-ready");
    }

    public record Handoff(String orderId, String buyerId, String sellerAsset,
                          String buyerUpdate, String variant, String buyer,
                          String status) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("usage: java ...MarketplaceCutover <user-id> <flag-key> <seller-asset> <buyer-update> <order-id>");
            System.exit(2);
        }
        String apiKey = System.getenv("INFRAI_API_KEY");
        Handoff handoff = new MarketplaceCutover(new InfraiClient(apiKey))
                .prepare(args[0], args[1], args[2], args[3], args[4]);
        System.out.println(handoff);
    }
}
