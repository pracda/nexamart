# NexaMart — Demo & Screenshot Playbook

For Prasiddha, ahead of the Aug 29, 10:00 AM Central presentation. Run this once, start to
finish, a day or two before presenting, and again the morning of. Everything here runs on
**your own machine** — I can't reach your running app to capture these myself, so follow the
exact steps below and save each image where noted.

Estimated time: 20–25 minutes for a full pass (servers + 8 screenshots + demo rehearsal).

## 0. Before you start

- Open a terminal (PowerShell or Command Prompt) in `...\SWE\NexaMart`.
- Close PowerPoint if `NexaMart_Presentation.pptx` is open — Windows currently has a lock file
  on it (`~$NexaMart_Presentation.pptx`), which will block me from writing updates to that exact
  filename later. The final deck comes to you as `NexaMart_Presentation_Final.pptx` (a new file)
  specifically so it doesn't collide with whatever's open right now.
- Have your `OPENAI_API_KEY` ready (from wherever you stored it — `.env` file or a password
  manager). Every AI screenshot below needs it set.
- Decide your screenshot tool: **Windows Snipping Tool** — press `Win + Shift + S`, drag a box
  around the app window, then `Ctrl + V` into Paint (or straight into a folder if your Windows
  version supports "Save as" from the snip toolbar). Save everything into
  `NexaMart\docs\screenshots\` using the filenames below — that's exactly what the README and
  the SRS/diagrams docs link to.

## 1. Capture Screenshot 8 first — automated tests passing

This one doesn't need the servers running, so get it out of the way first.

```powershell
cd nexamart-backend
mvn test
```

Wait for it to finish — you're looking for a summary block near the end that says something like:

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Screenshot the terminal** showing that summary and `BUILD SUCCESS`. Save as:
`docs/screenshots/08-mvn-test-success.png`

If anything fails here, stop and fix it before continuing — don't present with a red build.

## 2. Start the backend

Still in `nexamart-backend`:

```powershell
$env:OPENAI_API_KEY="sk-...your key..."
mvn spring-boot:run
```

Wait until the log shows Spring Boot's startup banner and `Started ... in ... seconds`. Leave
this terminal running — don't close it.

**Sanity check before moving on:** open a browser to `http://localhost:8081` — you should get
some response (even a 404/whitelabel page is fine, it means the server is up).

## 3. Start the frontend

Open a **second** terminal in `NexaMart\nexamart-frontend`:

```powershell
npm install
npm run dev
```

Open `http://localhost:5173` in your browser once it says `ready`. You should see the NexaMart
storefront.

> **Restart gotcha:** every time you restart the backend, log out and back in on every browser
> tab you have open — old JWTs 403 after a restart. If you restart the backend partway through
> this playbook, redo the login step below before continuing.

## 4. Buyer walkthrough — Screenshots 1, 2, 3

1. Log in as `buyer@nexamart.dev` / `password123`.
2. Click the chat bubble (bottom-right) and type: `find me headphones under $50`
3. Wait for the AI reply. **Screenshot the chat panel** showing your message and the real reply.
   Save as `docs/screenshots/01-buyer-product-finder-chat.png`
4. Add the returned product to your cart, go to **Cart**, click **Checkout**. **Screenshot the
   order confirmation** (note the order number — you'll need it in the next step). Save as
   `docs/screenshots/02-buyer-checkout.png`
5. Back in the chat widget, type: `where is order #<the number from step 4>?`
6. **Screenshot the reply** showing real status/items. Save as
   `docs/screenshots/03-buyer-order-tracking.png`

## 5. Seller walkthrough — Screenshots 4, 5

1. Log out, log back in as `seller@nexamart.dev` / `password123` (or use a second browser /
   incognito window so you can keep the buyer tab logged in too).
2. Go to **Seller Dashboard → AI Listing Assistant**.
3. Type a product name you haven't used before — e.g. `ceramic coffee mug`, notes:
   `microwave safe, 12oz`. Click **Generate with AI**.
4. **Screenshot the generated draft** (title, description, bullets, SEO tags, category) before
   you publish it. Save as `docs/screenshots/04-seller-listing-assistant.png`
5. Fill in price/stock and click **Publish listing** (optional for the screenshot, but do it —
   you'll want this listing live for the actual demo too).
6. In the seller's chat widget, ask: `which of my products are running low?` then
   `what are my top selling products and total revenue?`
7. **Screenshot one of those replies** (whichever reads better). Save as
   `docs/screenshots/05-seller-sales-chat.png`

## 6. Admin walkthrough — Screenshots 6, 7

1. Log out, log back in as `admin@nexamart.dev` / `password123`.
2. Go to **Admin Dashboard**, open the seed dispute (`Dispute #1 — Order #1 — Bailey Buyer`).
3. Click **Summarize with AI**. **Screenshot the thread + AI summary.** Save as
   `docs/screenshots/06-admin-dispute-summary.png`
4. Scroll to **AI Fraud Detector**, click **Run fraud scan**.
5. **Screenshot the flagged results** (the USB-C Charging Cable priced under category average
   should appear, with an AI-written explanation). Save as
   `docs/screenshots/07-admin-fraud-detector.png`

## 7. Double-check your 8 files

You should now have, in `NexaMart\docs\screenshots\`:

```
01-buyer-product-finder-chat.png
02-buyer-checkout.png
03-buyer-order-tracking.png
04-seller-listing-assistant.png
05-seller-sales-chat.png
06-admin-dispute-summary.png
07-admin-fraud-detector.png
08-mvn-test-success.png
```

Open each one once just to confirm it's readable (not cut off, text legible) before moving on.

## 8. Commit everything

```powershell
cd ..
git add docs/ README.md
git status
```

Confirm the staged list looks right (the new `docs/VISION.md`, `docs/SRS.md`,
`docs/diagrams.md`, `docs/screenshots/*.png`, `docs/screenshots/README.md`, and the modified
`README.md`), then:

```powershell
git commit -m "docs: add SRS, vision, UML diagrams, screenshots, and test evidence for final submission"
git push
```

Verify on `github.com/pracda/nexamart` that `docs/` shows up with all four `.md` files rendering
(the diagrams should show as actual boxes-and-arrows pictures, not raw text — GitHub renders
Mermaid automatically).

## 9. Rehearse the live demo once, for real

Now that you know every screen works, run the same 4 beats one more time as a dry run —
timed, without stopping for screenshots — using `NexaMart_Presentation_Script_Final.docx`'s
"Live Demo Walkthrough" section:

1. Buyer: chat search → add to cart → checkout (~40s)
2. Buyer: chat "where is order #___?" (~20s)
3. Seller: AI Listing Assistant → generate → publish a **new, different** product (don't reuse
   the coffee mug from step 5 above — pick something fresh so it's obviously live, not memorized)
   (~60s)
4. Seller: chat inventory + sales question (~30s)
5. Stretch, only if ahead of schedule: Admin dispute summary + fraud scan (~40s)

Target: under 3 minutes for beats 1–4. Do this the morning of, right before you present, so you
know the API key still has quota and nothing regressed since your last restart.

## If something breaks on presentation day

- **OpenAI slow or down:** narrate the architecture (Slide 6, the 4-step flow) while it loads,
  or fall back to the screenshots you just captured — "here's the same exchange captured
  earlier" is a completely normal thing to say.
- **Backend/frontend crashed:** have a 60–90 second screen recording of a clean run saved
  locally as a last resort (record one the same day you take these screenshots, while everything
  is known-working).
- **JWT 403 errors mid-demo:** you restarted the backend and didn't re-log-in on that tab — log
  out/in again.
