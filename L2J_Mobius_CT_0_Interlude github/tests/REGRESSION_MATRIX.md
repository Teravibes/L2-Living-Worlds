# Living World Regression Matrix

This matrix is the initial test inventory for the stabilization phase. It should evolve with the implementation. A checked result must refer to a reproducible automated test or a recorded manual run; feature notes alone are not test evidence.

## Result values

- `PASS` — expected behavior observed with evidence.
- `FAIL` — behavior differs from expectation; link an issue or record diagnostics.
- `BLOCKED` — environment or prerequisite unavailable.
- `NOT RUN` — no current result.

## Running the suite

Run everything (both Java harnesses + the Python brain tests) with one combined pass/fail summary:

```
./tests/run_all.sh
```

Exit code is 0 only if every suite passes. Individual suites:

## Automated test sources

- `[J]` — Java: standalone JDK harnesses under `tests/java/` — `FakePlayerChatParsingTest` (60), `FakePlayerStorePricingTest` (42), `FakePlayerNameFactoryTest` (seeded-name determinism/shape sweep). Easiest to run via `./tests/run_all.sh`; the raw command is in that script.
- `[P]` — Python: `tests/test_fpc_brain.py` (`unittest`, 21 tests). Run: `PROVIDER=ollama python -m unittest discover -s tests -p "test_*.py"`.
- Both run in CI (`.github/workflows/java-parsing-tests.yml`, `.github/workflows/python-brain-tests.yml`).

## P0 — release-blocking integrity

| ID | Area | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|---|
| P0-BOOT-01 | Boot | Start with all Living World systems enabled | Server reaches ready state without phantom/chat/party initialization exceptions | Integration/manual | NOT RUN |
| P0-BOOT-02 | Reload | Reload Living World configuration repeatedly | No duplicate actors, managers, listeners, or scheduled tasks | Integration/manual | NOT RUN |
| P0-DB-01 | Cleanup | Boot after an unclean stop with orphaned `account_name='phantom'` rows | Orphaned phantom rows are removed through the supported cleanup path | Integration | NOT RUN |
| P0-DB-02 | Persistence | Restart with stable regular identities | Stable identity and character mapping remain consistent; transient actors do not duplicate | Integration/manual | NOT RUN |
| P0-ECON-01 | Trade safety | Complete buy and sell transactions | Correct item/adena direction and quantities; no duplication or negative stock | Integration/manual | NOT RUN (price band + quantity clamp are unit-covered — see P1-TRADE-04/10; item/adena direction still needs integration) |
| P0-LIFE-01 | Teardown | Despawn, reload, dismiss party, and end meetup | Owned tasks and transient state are cancelled or removed | Integration | NOT RUN |
| P0-FAIL-01 | Sidecar failure | LLM HTTP request times out or returns invalid output | Game server remains responsive; no raw tags or exceptions reach the player | Automated/integration | NOT RUN |

## P1 — core product behavior

### Population and identity

| ID | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|
| P1-POP-01 | Spawn configured town population | Population respects configured limits and valid spawn positions | Integration/manual | NOT RUN |
| P1-POP-02 | Zone becomes empty | Population despawns according to timeout and policy | Integration/manual | NOT RUN |
| P1-POP-03 | Reload an active population | No duplicate characters or stale population records | Integration | NOT RUN |
| P1-ID-01 | Generate automatic regular slots twice | Same population and slot seed produce the same identity | Unit | PASS (unit: seeded name is deterministic - same seed -> same name) [J]; full slot/class/appearance identity still needs integration |
| P1-ID-02 | Two regulars are selected concurrently | The same regular is not live twice | Unit/integration | NOT RUN |
| P1-ID-03 | Befriend a random phantom | Phantom is promoted to a persistent regular without losing intended identity | Integration/manual | NOT RUN |

### Chat, tags, memory, and rate limits

| ID | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|
| P1-CHAT-01 | Send say, shout, trade, whisper, and friend messages | Correct route, audience, and brain mode are used | Automated/integration | NOT RUN |
| P1-CHAT-02 | Brain returns valid control tags | Commands are acted upon and stripped from visible text | Unit/integration | PASS (unit: MEET/SHOP tag patterns) [J] |
| P1-CHAT-03 | Brain returns malformed MEET closings | Supported malformed forms are stripped safely and interpreted only when valid | Unit | PASS (unit) [J] |
| P1-CHAT-04 | Two bots reply to one line | Reply cap, deduplication, and stagger rules are respected | Unit/integration | NOT RUN |
| P1-CHAT-05 | Public chatter reaches its rate limit | Ambient chatter is limited without blocking priority trade offers | Unit/integration | NOT RUN |
| P1-MEM-01 | Deterministic memory extraction receives known phrases | Only intended durable facts are persisted; no malformed file output | Python unit | NOT RUN |
| P1-MEM-02 | Memory file is absent or malformed | Brain starts or degrades safely without losing unrelated functionality | Python unit | NOT RUN |

### Trade and meetup lifecycle

| ID | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|
| P1-TRADE-01 | Parse `+WTB ssd 5k` | Side is player-buy/bot-sell; quantity is 5000 | Unit | PASS (unit: quantity=5000, cap enforced) [J]; side routing not yet unit-tested |
| P1-TRADE-02 | Parse `+WTS ssd 5k` | Side is player-sell/bot-buy; quantity is 5000 | Unit | PASS (unit: ad detected + quantity) [J]/[P]; side routing not yet unit-tested |
| P1-TRADE-03 | Omit quantity | Supported fallback quantity policy is used within safe limits | Unit | PASS (unit: parser returns 0 when absent) [J] |
| P1-TRADE-04 | Negotiate a new unit price | Item, side, count, unit price, and total remain consistent | Unit/integration | PASS (unit: clampDealPrice pins the negotiated price into a sane sell/buy band) [J]; item/side/total still need integration |
| P1-TRADE-10 | Negotiate/inject an absurd quantity | Deal count is clamped to [1, 2,000,000]; non-stackables stay a single piece | Unit | PASS (unit: normalizedDealCount) [J] |
| P1-TRADE-05 | Meet at gatekeeper/warehouse/shop | Destination is valid and offset from the NPC model | Integration/manual | NOT RUN |
| P1-TRADE-06 | Complete a deal | Store and active deal context close cleanly | Integration/manual | NOT RUN |
| P1-TRADE-07 | Cancel before meetup | Pending deal context clears even when no meetup was started | Integration | NOT RUN |
| P1-TRADE-08 | Timeout or abandon meetup | Actor leaves according to policy and all deal/meet state clears | Integration/manual | NOT RUN |
| P1-TRADE-09 | Advertise while ambient chat cap is exhausted | Eligible trade responder can still send the priority offer | Integration | NOT RUN |

### Party, support, and normal combat

| ID | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|
| P1-PARTY-00 | Parse an LFP shout into recruit counts (e.g. "2 dd healer", "lfm buffer lvl 57") | Counts (1-6) attach to the right role word, level numbers are not counted, plurals resolve, party-wide cap holds | Unit | PASS (unit: countBefore + parseRoleRequests) [J]; class-name phase-1 not yet unit-tested |
| P1-PARTY-01 | Recruit a level-matched party | Valid role composition is selected and members join once | Integration/manual | NOT RUN |
| P1-PARTY-02 | Member pathing fails temporarily | Recruitment does not duplicate or permanently wedge the member | Integration/manual | NOT RUN |
| P1-PARTY-03 | Dismiss or disband | Members leave and all ownership/scheduled state clears | Integration | NOT RUN |
| P1-BUFF-01 | Apply pre-buffs to melee and caster roles | Required slot-safe buffs land; conflicting buffs do not flap | Unit/integration | NOT RUN |
| P1-BUFF-02 | Request greater or prophecy buff explicitly | On-request buff is handled without entering auto-maintenance loops | Integration/manual | NOT RUN |
| P1-HEAL-01 | Multiple members take damage | Healer prioritizes emergencies and avoids wasteful overheal | Simulation/manual | NOT RUN |
| P1-HEAL-02 | Member dies and resurrection is available | One healer claims resurrection; recovery gating prevents immediate failure | Simulation/manual | NOT RUN |
| P1-DPS-01 | DPS reaches zero MP | Auto-attacks continue when valid and useful | Simulation/manual | NOT RUN |
| P1-FOLLOW-01 | Leader moves between combat and idle states | Party maintains useful spacing and does not oscillate indefinitely | Simulation/manual | NOT RUN |

### Raid behavior

| ID | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|
| P1-RAID-01 | Raid has live minions | Released/held adds are focused in intended order without premature pulls | Simulation/manual | NOT RUN |
| P1-RAID-02 | Raid casts damaging AoE | Eligible backline members step out and resume their role | Simulation/manual | NOT RUN |
| P1-RAID-03 | Raid casts non-damaging root/hold | Members do not abandon useful actions for pointless dodging | Simulation/manual | NOT RUN |
| P1-RAID-04 | Tank dies and is resurrected | Tank can re-establish attacks and threat without taunt spam lockup | Simulation/manual | NOT RUN |
| P1-RAID-05 | Archer is assigned backline positioning | Hold distance remains within actual weapon range | Unit/simulation | NOT RUN |
| P1-RAID-06 | Support MP declines | Recharge, healing, buffs, and auto-attacks follow the intended priority | Simulation/manual | NOT RUN |

## P2 — polish and experiential quality

| ID | Area | Scenario | Expected result | Best test level | Status |
|---|---|---|---|---|---|
| P2-SOC-01 | Typing latency | Delay scales plausibly with message length and remains bounded | Unit/manual | NOT RUN |
| P2-SOC-02 | Recurring identity | A returning regular sounds and appears recognizably consistent | Manual | NOT RUN |
| P2-SOC-03 | Memory continuity | Relevant memory improves continuity without appearing in every opening | Manual | NOT RUN |
| P2-MOVE-01 | Landmark meetup | Character stands near, not inside, the landmark NPC | Manual | NOT RUN |
| P2-VIS-01 | Gear/appearance combinations | Supported races/classes render without obviously missing body pieces | Manual | NOT RUN |
| P2-WORLD-01 | Thirty-minute town observation | Population feels active but not spammy or synchronized | Manual | NOT RUN |

## Initial automation candidates

Automate these first because they are deterministic, high-value, and comparatively isolated:

1. Trade quantity and side parsing.
2. Control-tag recognition and stripping, including malformed closings.
3. Deal-context creation and cleanup state transitions.
4. Stable regular identity generation from population/slot seeds.
5. Role classification for known classes.
6. Buff-kit slot uniqueness and request aliases.
7. Rate-limit separation between ambient messages and trade offers.
8. Python memory load/save and deterministic extraction.
9. Typing-delay minimum, maximum, and length scaling.
10. Raid range calculations and target-release predicates where dependencies can be isolated.

## Evidence requirements

For each manual or integration run, record:

- build/commit identifier;
- configuration and test character details;
- start and end time;
- scenario IDs exercised;
- expected and observed results;
- relevant server/brain log excerpts;
- screenshots when visual placement or behavior matters;
- database counts before and after lifecycle tests;
- final `PASS`, `FAIL`, or `BLOCKED` result.

javac -d build/test-classes \
  "java/org/l2jmobius/gameserver/managers/FakePlayerChatParsing.java" \
  "tests/java/FakePlayerChatParsingTest.java"
java -cp build/test-classes FakePlayerChatParsingTest

PROVIDER=ollama python -m unittest discover -s tests -p "test_*.py" -v
