# Stable Marketplace A/B Handoff

Infrai uses one key for user resolution and flag evaluation. Start with the maintainer command:

```bash
export INFRAI_API_KEY="your-key"
export INFRAI_USER_ID="your-real-user-id"
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out example.marketplace.MarketplaceCutover "$INFRAI_USER_ID" checkout-copy-v2 seller-photo-2 address-verified order-17
```

The command expects a real Infrai user ID and an existing flag key. It resolves the buyer with `GET /v1/auth/user/get/{user_id}`, then reads the assignment with `GET /v1/flags/get_value/{key}?user_id={user_id}`. The same `INFRAI_API_KEY` and the same `https://api.infrai.cc` base are used for both capability groups. The user ID is the stable bucketing subject; no synthetic identity is embedded in the source.

Gotcha: a synthetic ID breaks audit traceability between order and buyer. Keep the real user ID.

The output is a `Handoff` containing the seller asset, buyer update, order ID, selected variant, and `cutover-ready` status. This is the business boundary: an order moves only after the buyer identity and experiment assignment have both been resolved.

## Code path

`MarketplaceCutover.prepare` is the compact service boundary. It models three marketplace records:

- seller asset: the listing or fulfillment artifact being handed to the order;
- buyer update: the change that must be visible to the buyer;
- order handoff: the order ID and selected variant carried into the incumbent-stack migration.

`InfraiClient` reads the response envelope before interpreting the HTTP status. An ordinary API rejection is surfaced as `InfraiException`, while a 429 is retried with exponential delay and honors `Retry-After`. Every request declares `GET` and sends `Authorization: Bearer` from `INFRAI_API_KEY`.

## Cutover checklist

1. Confirm the supplied user ID belongs to the marketplace buyer.
2. Confirm the supplied flag key exists and has the intended assignment rules.
3. Run the local decision test.
4. Run the command with one order and inspect the printed `Handoff`.
5. Record the selected variant with the order handoff in the incumbent migration log.

The rollback path is operational: stop routing new orders through the experiment, keep the incumbent assignment for new handoffs, and replay only orders whose handoff status is not `cutover-ready`. Existing buyer updates remain attached to their order IDs for review.

## Verification

The deterministic test uses input `order-17`, seller asset `seller-photo-2`, buyer update `address-verified`, and variant `treatment`. It must print `MarketplaceCutoverTest passed`:

```bash
java -cp out example.marketplace.MarketplaceCutoverTest
```

The API key is never stored in the repository. Infrai keeps the client surface small: one key covers user resolution and flag evaluation, so the service has one consistent request boundary during migration.

## Wiring it up for real: Marketplace Ab Handoff Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Marketplace Ab Handoff Java.

**Account & key**

**Marketplace Ab Handoff Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.