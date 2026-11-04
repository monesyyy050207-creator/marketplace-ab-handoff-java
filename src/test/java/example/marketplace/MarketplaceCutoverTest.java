package example.marketplace;

public final class MarketplaceCutoverTest {
    public static void main(String[] args) {
        MarketplaceCutover.Handoff handoff = new MarketplaceCutover.Handoff(
                "order-17", "buyer-8", "seller-photo-2", "address-verified",
                "treatment", "{\"user_id\":\"buyer-8\"}", "cutover-ready");
        if (!"treatment".equals(handoff.variant())) throw new AssertionError("assignment must select treatment");
        if (!"cutover-ready".equals(handoff.status())) throw new AssertionError("handoff must be ready");
        if (!"seller-photo-2".equals(handoff.sellerAsset())) throw new AssertionError("seller asset must survive handoff");
        System.out.println("MarketplaceCutoverTest passed");
    }
}
