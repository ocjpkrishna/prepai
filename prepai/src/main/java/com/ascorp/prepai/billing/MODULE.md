# billing

**Purpose:** sells and manages paid plans (Pro, Pro+) through Razorpay, and keeps each student's plan up to date.
**Built by:** Agent 4 | **Spec:** 1.4, 4.4, 5.1 (`subscriptions`), 9.1.1, 9.2 (Agent 4)

## Context
- Plans and their limits are defined by the `Plan` enum in `common/model/enums`. This module sells them; `quota` enforces them.
- The Razorpay webhook is the source of truth for a payment: it creates or updates the subscription and then asks `account.UserService` to change the user's plan.
- The payment flow still needs a pre-launch review (spec 15, TODO-3).

## Packages
```
billing/
├── MODULE.md
└── subscription/
    ├── controller/   SubscriptionController (plans, checkout, me), RazorpayWebhookController
    ├── service/      SubscriptionService, PlanCatalogService, RazorpayGateway (wraps razorpay-java)
    ├── repository/   SubscriptionRepository
    ├── model/entity/ Subscription
    ├── model/dto/    PlanDto, CheckoutRequest, CheckoutResponse, SubscriptionDto
    └── mapper/       SubscriptionMapper
```

## Uses these modules
`account` (`UserService`: change a user's plan), `common` (errors, `Plan`).

## Data
- Table: `subscriptions` (V4).
- Configuration: `prepai.razorpay.*`. Environment: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`.

## Rules and gotchas
- Verify the webhook signature and handle webhooks idempotently.
- Razorpay sits behind `RazorpayGateway` so tests never call the real service.
- `GET /subscriptions/plans` is public; the other endpoints need a logged-in user.
- Follow spec 9.1.2. `./gradlew check` must be green.

## Definition of done
Razorpay checkout creates a subscription and the webhook updates the student's plan. Plans are listed publicly.

## Status
- [ ] plans, checkout, subscription status
- [ ] Razorpay webhook with signature check and idempotency
- [ ] Flyway migration V4
- [ ] Tests mirrored under `src/test/java/.../billing/`
