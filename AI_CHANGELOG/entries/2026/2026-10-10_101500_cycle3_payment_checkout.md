# AI Changelog Entry: CL-AND-20261010-101500-005

- **Entry ID**: CL-AND-20261010-101500-005
- **Timestamp**: 2026-10-10 10:15:00 IST (UTC+05:30)
- **Status**: COMPLETED (pending CI verification)
- **Risk Class**: R2
- **Author**: Subagent (parent orchestrator, audit loop Cycle 3 of 10 — Android PAYMENT)

## 1. Problem & Context
Cycle 3: implement the payment flow EXACTLY as the Flutter app has it now (`omastro-flutter-app` @ 8628ab8), via REST API, no redesign. Spec: `/home/hatch/workspace/flutter-payment-spec.md`.

## 2. Findings (server contract VERIFIED LIVE 2026-10-10 via curl, no auth needed for quotes)
- `POST /checkout/validate-coupon {coupon, plan?, product_id?, coins?}` -> `{success, data:{item_title, subtotal(+_display), discount_amount(+_display), tax_amount(+_display), tax_label, total(+_display), total_paise, coupon_code, coupon_error, coupon_message, message}}`. Invalid coupon -> `{success:false, code:"coupon_invalid", message, calculation:{...same shape...}}`. Numbers arrive as JSON numbers.
- `POST /checkout/create-order` -> 401 without Bearer token (auth required, as expected).
- Product mapping (server-authoritative, differs from Flutter UI defaults in places):
  - 208 = pro_monthly "Kundali Pro — Monthly": subtotal 499, GST 89.82, total 588.82
  - 209 = pro_yearly "Kundali Pro — Annual (Save 20%)": subtotal 4999 (NOT Flutter's 3999 default), total 5898.82
  - 210 = max_monthly "Kundali Max — Monthly": subtotal 999, total 1178.82
  - 211 = max_yearly "Kundali Max — Annual (Save 20%)": subtotal 9999 (NOT Flutter's 7999 default), total 11798.82
  - 704 + coins = **WALLET TOP-UP** (server titles it "OmAstro Wallet Top-up — ₹N Credit"): coins = pay-rupees x 100. Verified: coins=2500 -> pay 25 (+4.50 GST) -> ₹35 credit; coins=5000 -> pay 50 (+9.00) -> ₹80; coins=10000 -> pay 100 (+18.00) -> ₹200; coins=20000 -> pay 200 (+36.00) -> ₹400; coins=4000 -> pay 40 (+7.20) -> ₹40. This exactly matches the Android wallet slab structure (25→35, 50→80, 100→200, 200→400) — the Flutter ₹19/500-coins figure was a pre-quote UI estimate, the server quote is authoritative.
  - GST (18%) is ADDED ON TOP in the checkout flow (was "included" in the old wallet flow strings — note updated).
- Existing Android wallet flow used `/wallet/topup-order` + `/wallet/topup-verify` with a direct Razorpay launch (no quote sheet, no promo, theme #7B0D1E, retry max_count 4, no send_sms_hash). Replaced by the Flutter-exact flow below. Old repo methods (`WalletRepository.createOrder/verifyPayment`, `RechargeUseCase`) kept as truthful API bindings but no longer used by any screen.

## 3. Changes
New files:
- `data/model/CheckoutModels.kt` — `CheckoutItem` (planKey/productId/customCoins + `walletPack()`/`coinRecharge()`/`membershipPlan()` factories), `WALLET_TOPUP_PRODUCT_ID=704`, `RAZORPAY_FALLBACK_KEY_ID` (Flutter's `rzp_live_T8gzsClejXQi48`), `CHECKOUT_AMOUNT_FALLBACK_PAISE=49900`, `MEMBERSHIP_PLANS` catalog (plan keys + product ids + display prices), `PriceBreakdown`, `CheckoutOrder`.
- `data/repository/CheckoutRepository.kt` — `getQuote()` (validate-coupon), `createOrder()` (Flutter-exact body: 704+coins+qty / product_id / plan; coupon uppercased), `verifyPayment()` (order_id int + 3 razorpay fields). Tolerant number/string parsing, HTML stripped from server messages.
- `ui/screens/checkout/CheckoutViewModel.kt` — sheet state machine: openSheet (quote with empty coupon), applyCoupon/removeCoupon, `proceedToPay()` with double-tap guard (`_isPaymentInProgress`, Flutter's `_isPaymentInProgress`), `onRazorpaySuccess` (verify -> success message + `onPaymentFinished` refresh; verify-fail -> "Payment verification failed." + refresh anyway, Flutter parity), `onRazorpayCancelled` ("Payment was cancelled." / "Payment: <msg>"), `onExternalWallet` (info only, NO verify).
- `ui/screens/checkout/CheckoutSheet.kt` — `CheckoutSheetHost` (ModalBottomSheet + RazorpayPaymentActivity launcher + event forwarding) and `CheckoutSheetContent`: Flutter-exact layout — "Order Summary / ऑर्डर विवरण" header, item card (coin/crown icon, title, Base line), promo card (input + Apply / applied state + remove), breakdown (Subtotal / Promo Discount / `{tax_label}` / Total Payable), "Pay ₹X via Razorpay" button, "100% Secure 256-Bit SSL Razorpay Gateway" note.

Modified (in place):
- `data/remote/OmAstroApiService.kt` — `validateCoupon`, `createCheckoutOrder`, `verifyCheckout` (Map bodies; server mixes numbers and display strings).
- `data/remote/ApiClient.kt` — new interceptor: on 404 for `/wp-json/omastro/v1/*`, retry once as `?rest_route=/omastro/v1/*` (spec fallback; encoded form verified 200 live).
- `di/AppContainer.kt` — `checkoutRepository`.
- `ui/screens/wallet/RazorpayPaymentActivity.kt` — Flutter-exact options: theme #FF6B00, retry max_count 1, `send_sms_hash: true`, order_id only when non-empty, prefill email/name only when non-empty, `ExternalWalletListener` -> `RESULT_EXTERNAL_WALLET` (caller shows info, no verify).
- `ui/screens/wallet/WalletScreen.kt` — pack tap opens the checkout sheet (`CheckoutItem.walletPack(pay)`); NEW custom-coins card (input, min 100, Flutter coin-recharge entry); `CheckoutSheetHost` with `onPaymentFinished` = refreshBalance + refreshTransactions (Flutter's home-reload equivalent); old direct create-order/verify flow removed from the screen. Wallet-spend logic untouched.
- `ui/screens/wallet/WalletViewModel.kt` — old order/verify/pending-order code removed; added `postMessage()` for checkout events.
- `ui/screens/dashboard/DashboardScreen.kt` — Plan tab gains a Membership Plans section (4 cards: Pro Monthly ₹499, Pro Annual ₹4999, Max Monthly ₹999, Max Annual ₹9999; Buy -> checkout sheet with plan key); snackbar overlay for checkout events; payment finish -> `loadDashboard()` (plan status refresh).
- `ui/i18n/FeatureStrings.kt` — 40 new HI + EN keys (checkout sheet, plan cards, custom coins). `money_gst_note` corrected: GST is now ADDED on top (was "included") — matches the server quote.

## 4. Verification evidence
- Server contract probed live (curl, 2026-10-10): validate-coupon quotes for 704+coins (100/500/800/4000/5000/10000/20000), plans pro_monthly/pro_yearly/max_monthly/max_yearly, product ids 208-211, invalid-coupon shape; create-order 401 without auth (expected); `?rest_route=` fallback 200 incl. percent-encoded form.
- Static: no remaining references to removed WalletViewModel order functions; no emojis added; all new UI strings via W.t()/W.tf().
- Build: no local JDK at cycle start (apt-get install openjdk-17 in progress during this cycle); CI (`assembleDebug` + `assembleRelease` on push to main) is the compile gate — must be green after push.

## 5. Rejected approaches
- Routing wallet packs through invented product ids for /checkout: REJECTED — server probing showed product 704 + coins IS the wallet top-up; no invention needed.
- Keeping the old direct-to-Razorpay wallet flow: REJECTED — user directive is the Flutter-exact flow (quote sheet + promo + GST breakdown).
- Per-screen language toggles in the sheet: REJECTED — global Settings language drives everything (W.t reads LangPrefs).

## 6. NOT verifiable from here (needs device)
- End-to-end Razorpay payment (create-order needs a logged-in Bearer token; the Razorpay sheet needs a real device).
- Promo-code application with a REAL coupon code (only an invalid code was probed).
- `POST /checkout/verify` success path (needs a real captured payment).
