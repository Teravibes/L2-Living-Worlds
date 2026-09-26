import importlib.util
import io
import os
import sys
import tempfile
import time
import unittest
from contextlib import redirect_stdout
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
BRAIN_PATH = PROJECT_ROOT / "fpc_brain.py"


def load_brain_module():
    """Load fpc_brain without requiring a live LLM endpoint."""
    os.environ["PROVIDER"] = "ollama"
    spec = importlib.util.spec_from_file_location("fpc_brain_under_test", BRAIN_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class FpcBrainRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.original_memory_dir = self.brain.MEMORY_DIR
        self.original_memory_file = self.brain.MEMORY_FILE
        self.original_memory = self.brain._memory

        self.brain.MEMORY_DIR = self.temp_dir.name
        self.brain.MEMORY_FILE = os.path.join(self.temp_dir.name, "fpc_memory.json")
        self.brain._memory = {}

    def tearDown(self):
        self.brain.MEMORY_DIR = self.original_memory_dir
        self.brain.MEMORY_FILE = self.original_memory_file
        self.brain._memory = self.original_memory
        self.temp_dir.cleanup()

    def test_voice_is_stable_for_same_name(self):
        first = self.brain._voice("Mirella")
        second = self.brain._voice("Mirella")
        self.assertEqual(first, second)

    def test_sanitize_drops_ai_disclosure(self):
        self.assertEqual("", self.brain.sanitize("As an AI language model, I cannot help."))

    def test_sanitize_strips_contact_data_and_html(self):
        result = self.brain.sanitize("<br>pm me at test@example.com or https://example.com now")
        self.assertNotIn("example.com", result)
        self.assertNotIn("<br>", result)
        self.assertEqual("pm me at or now", result)

    def test_load_memory_missing_file_is_safe(self):
        self.brain._memory = {"stale": {}}
        self.brain.load_memory()
        self.assertEqual({}, self.brain._memory)

    def test_load_memory_malformed_json_is_safe(self):
        Path(self.brain.MEMORY_FILE).write_text("{not valid json", encoding="utf-8")
        self.brain._memory = {"stale": {}}
        self.brain.load_memory()
        self.assertEqual({}, self.brain._memory)

    def test_remember_fact_deduplicates_and_persists(self):
        self.brain.remember_fact("PlayerOne", "trade", "Player uses trade chat to buy items.")
        self.brain.remember_fact("PlayerOne", "trade", "Player uses trade chat to buy items.")

        entry = self.brain._memory["playerone"]
        self.assertEqual(1, len(entry["trade"]))
        self.assertTrue(Path(self.brain.MEMORY_FILE).exists())

        self.brain._memory = {}
        self.brain.load_memory()
        self.assertEqual(1, len(self.brain._memory["playerone"]["trade"]))

    def test_remember_fact_caps_each_category(self):
        for index in range(self.brain.MEMORY_MAX_FACTS_PER_CATEGORY + 5):
            self.brain.remember_fact("PlayerOne", "social", f"fact-{index}")

        facts = self.brain._memory["playerone"]["social"]
        self.assertEqual(self.brain.MEMORY_MAX_FACTS_PER_CATEGORY, len(facts))
        self.assertEqual("fact-5", facts[0]["text"])

    def test_trade_ad_extracts_d_grade_shot_habit(self):
        self.brain.remember_trade_ad("PlayerOne", "+WTB ssd 5k")
        texts = [fact["text"] for fact in self.brain._memory["playerone"]["trade"]]
        self.assertIn("Player has looked for D-grade shots in bulk.", texts)

    def test_exchange_extracts_meet_and_social_memory(self):
        self.brain.remember_from_exchange(
            "PlayerOne",
            "thanks, gk sounds good",
            "ok heading there\n[[MEET:gatekeeper]]",
            "WHISPER",
        )
        entry = self.brain._memory["playerone"]
        trade_texts = [fact["text"] for fact in entry["trade"]]
        social_texts = [fact["text"] for fact in entry["social"]]

        # FPC-069: the meet-place habit comes from the PLAYER saying "gk", not from the model's [[MEET:gatekeeper]]
        # tag; the tag-derived "agreed to meet" fact is no longer persisted (Java may veto the proposal).
        self.assertIn("Player often uses gatekeeper as a meeting point.", trade_texts)
        self.assertFalse(any("agreed to meet" in text for text in trade_texts), trade_texts)
        self.assertIn("Player has been friendly/appreciative.", social_texts)

    def test_trade_ad_does_not_persist_raw_ad_or_price(self):
        # The raw ad (with its one-time price/quantity) must not become permanent player-global memory.
        self.brain.remember_trade_ad("PlayerOne", "+WTB ssd 300 adena")
        texts = [fact["text"] for fact in self.brain._memory["playerone"]["trade"]]
        self.assertNotIn("Player recently posted trade ad: +WTB ssd 300 adena", texts)
        self.assertFalse(any("300" in text for text in texts), texts)
        # The generalized habit is still recorded.
        self.assertIn("Player has looked for D-grade shots in bulk.", texts)

    def test_exchange_does_not_persist_specific_deal_price(self):
        # A closed deal's item/price is per-deal state, not a lasting profile fact shared with every other phantom.
        self.brain.remember_from_exchange(
            "PlayerOne",
            "ok deal, gk",
            "cool see you there [[SHOP:SELL:Soulshot D-grade:5]]\n[[MEET:gatekeeper]]",
            "WHISPER",
        )
        texts = [fact["text"] for fact in self.brain._memory["playerone"]["trade"]]
        self.assertFalse(any("adena each" in text for text in texts), texts)
        self.assertFalse(any("Soulshot D-grade" in text for text in texts), texts)
        # FPC-069: the stable meet-place habit is derived from the PLAYER naming "gk", NOT from the model's
        # [[MEET:gatekeeper]] tag (which Java may veto). The tag-derived "agreed to meet" fact is no longer persisted.
        self.assertIn("Player often uses gatekeeper as a meeting point.", texts)
        self.assertFalse(any("agreed to meet" in text for text in texts), texts)

    def test_player_key_normalizes_case_and_whitespace(self):
        self.assertEqual("playerone", self.brain._player_key("  PlayerOne  "))
        self.assertEqual("", self.brain._player_key(None))

    def test_trade_ad_wts_records_selling_habit(self):
        self.brain.remember_trade_ad("PlayerOne", "WTS adena cheap")
        texts = [fact["text"] for fact in self.brain._memory["playerone"]["trade"]]
        self.assertIn("Player uses trade chat to sell items.", texts)

    def test_cancel_outcome_is_not_persisted_from_words(self):
        # FPC-076: a cancellation is an authoritative trade OUTCOME (Java owns it), not a phrase. It is no longer
        # persisted from the player's words at all - not in a trade whisper, and (the regression) not leaked from a
        # party/buddy/friend line or a negated "don't cancel". It returns with FPC-058's Java-outcome feed.
        for name, msg, mode in [
            ("CancelWhisper", "nvm changed my mind", "WHISPER"),
            ("CancelParty", "cancel that tp", "PARTY"),
            ("CancelBuddy", "not interested in going there", "BUDDY"),
            ("CancelNegated", "don't cancel the party", "FRIEND"),
        ]:
            self.brain.remember_from_exchange(name, msg, "ok", mode)
            entry = self.brain._memory.get(self.brain._player_key(name), {})
            texts = [f["text"] for cat in ("trade", "party", "social") for f in entry.get(cat, [])]
            self.assertFalse(any("cancelled or backed out" in t for t in texts), (name, texts))

    def test_exchange_ignores_model_action_tags_for_memory(self):
        # FPC-069: a model action tag in the reply must NOT create a persistent fact - Java may veto the proposal, so
        # an unexecuted proposal must never become lasting memory. Here the PLAYER neither named a place nor cancelled,
        # so a [[MEET:gatekeeper]] in the reply must not persist a meet or cancel fact.
        self.brain.remember_from_exchange(
            "PlayerThree", "hmm let me think about it", "sure, come find me [[MEET:gatekeeper]]", "WHISPER"
        )
        texts = [fact["text"] for fact in self.brain._memory.get("playerthree", {}).get("trade", [])]
        self.assertFalse(any("meet" in text.lower() for text in texts), texts)
        self.assertFalse(any("cancel" in text.lower() for text in texts), texts)

    def test_exchange_records_haggling_and_afk(self):
        self.brain.remember_from_exchange(
            "PlayerOne", "too much, can you go lower? brb", "sure", "WHISPER"
        )
        entry = self.brain._memory["playerone"]
        trade_texts = [fact["text"] for fact in entry["trade"]]
        party_texts = [fact["text"] for fact in entry["party"]]
        self.assertIn("Player sometimes haggles trade prices.", trade_texts)
        self.assertIn("Player sometimes goes AFK during party play.", party_texts)

    def test_fmt_amount_shorthand(self):
        self.assertEqual("45k", self.brain.fmt_amount(45000))
        self.assertEqual("470k", self.brain.fmt_amount(470000))
        self.assertEqual("1.2k", self.brain.fmt_amount(1200))
        self.assertEqual("1k", self.brain.fmt_amount(1000))
        self.assertEqual("45kk", self.brain.fmt_amount(45000000))
        self.assertEqual("1.5kk", self.brain.fmt_amount(1500000))
        self.assertEqual("300", self.brain.fmt_amount(300))
        self.assertEqual("45k", self.brain.fmt_amount("45000"))
        self.assertEqual("junk", self.brain.fmt_amount("junk"))

    def test_history_text_drops_trade_action_tags(self):
        self.assertEqual("", self.brain.history_text("[[MEET:gatekeeper]]"))
        self.assertEqual("ok.", self.brain.history_text("ok. [[MEET:cancel]]"))
        self.assertEqual("deal, see u there", self.brain.history_text("deal, see u there [[SHOP]] [[MEET:warehouse]]"))
        # Other tags are not trade actions and stay as they were.
        self.assertEqual("omw [[FOLLOW]]", self.brain.history_text("omw [[FOLLOW]]"))

    def test_no_deal_note_forbids_trade_actions(self):
        with self.brain.app.test_request_context(headers={}):
            self.assertEqual("", self.brain.deal_note_from_headers())
        note = self.brain.NO_DEAL_NOTE
        self.assertIn("NO trade set up", note)
        self.assertIn("never add a MEET or SHOP tag", note)

    def test_meet_note_travelling_never_claims_arrival(self):
        with self.brain.app.test_request_context(headers={
            "X-Meet-State": "travelling",
            "X-Meet-Spot": "gatekeeper",
        }):
            note = self.brain.meet_note_from_headers()
        self.assertIn("gatekeeper", note)
        self.assertIn("NOT arrived", note)
        self.assertIn("on the way", note)

    def test_meet_note_waiting_names_the_spot(self):
        with self.brain.app.test_request_context(headers={
            "X-Meet-State": "waiting",
            "X-Meet-Spot": "warehouse",
        }):
            note = self.brain.meet_note_from_headers()
        self.assertIn("standing next to the warehouse", note)
        self.assertNotIn("NOT arrived", note)

    def test_meet_note_empty_without_a_meet(self):
        with self.brain.app.test_request_context(headers={}):
            self.assertEqual("", self.brain.meet_note_from_headers())
        with self.brain.app.test_request_context(headers={"X-Meet-State": "", "X-Meet-Spot": "gatekeeper"}):
            self.assertEqual("", self.brain.meet_note_from_headers())

    def test_deal_note_speaks_shorthand_prices(self):
        with self.brain.app.test_request_context(headers={
            "X-Deal-Side": "SELL",
            "X-Deal-Item": "Soulshot D-grade",
            "X-Deal-Count": "5000",
            "X-Deal-Unit-Price": "45000",
            "X-Deal-Total-Price": "225000000",
        }):
            note = self.brain.deal_note_from_headers()
        self.assertIn("45k adena each", note)
        self.assertNotIn("45000 adena each", note)
        self.assertIn("5kx", note)  # count also spoken in shorthand
        # FPC-071: the prompt now teaches the bare runtime protocol, not the legacy payload.
        self.assertIn("[[SHOP]]", note)
        self.assertNotIn("[[SHOP:SELL", note)

    def test_deal_note_asks_for_amount_when_needed(self):
        with self.brain.app.test_request_context(headers={
            "X-Deal-Side": "SELL",
            "X-Deal-Item": "Soulshot D-grade",
            "X-Deal-Unit-Price": "45000",
            "X-Deal-Needs-Count": "true",
        }):
            note = self.brain.deal_note_from_headers()
        self.assertIn("how many", note.lower())
        self.assertNotIn("[[SHOP:", note)

    def test_deal_note_single_item_is_a_flat_price_not_each(self):
        # A single non-stackable item is one flat price; "400k each" for one Artisan's Sword reads wrong.
        with self.brain.app.test_request_context(headers={
            "X-Deal-Side": "BUY",
            "X-Deal-Item": "Artisan's Sword",
            "X-Deal-Count": "1",
            "X-Deal-Unit-Price": "400000",
            "X-Deal-Total-Price": "400000",
        }):
            note = self.brain.deal_note_from_headers()
        self.assertIn("400k", note)
        self.assertNotIn("adena each", note)
        self.assertNotIn("400k each", note)
        self.assertIn("single item", note.lower())
        self.assertNotIn("1x", note)  # a single item is not prefixed with a count

    def test_deal_note_multiple_non_stackable_items_use_each(self):
        # More than one piece (e.g. "wts 2 artisan sword") is priced per unit -> "each".
        with self.brain.app.test_request_context(headers={
            "X-Deal-Side": "SELL",
            "X-Deal-Item": "Artisan's Sword",
            "X-Deal-Count": "2",
            "X-Deal-Unit-Price": "400000",
            "X-Deal-Total-Price": "800000",
        }):
            note = self.brain.deal_note_from_headers()
        self.assertIn("400k adena each", note)
        self.assertIn("2x", note)

    def test_sanitize_strips_roleplay_emphasis_markers(self):
        # FPC-043: stray * / backtick emphasis must never reach game chat (e.g. a mangled item name).
        self.assertEqual("Saber Artisans Sword", self.brain.sanitize("Saber*Artisans Sword"))
        self.assertEqual("waves hi", self.brain.sanitize("*waves* hi"))
        self.assertEqual("nice set", self.brain.sanitize("`nice` set"))

    def test_deal_note_clarify_asks_to_confirm_without_shop_tag(self):
        # FPC-042: an ambiguous bare counter is confirmed, never silently committed.
        with self.brain.app.test_request_context(headers={
            "X-Deal-Side": "BUY",
            "X-Deal-Item": "Artisan Sword",
            "X-Deal-Count": "1",
            "X-Deal-Unit-Price": "15000",
            "X-Deal-Decision": "CLARIFY",
            "X-Deal-Last-Counter": "17000",
        }):
            note = self.brain.deal_note_from_headers()
        self.assertIn("17k", note)
        self.assertIn("confirm", note.lower())
        self.assertNotIn("[[SHOP:", note)

    def test_deal_note_accept_before_quantity_confirms_price_and_asks_amount(self):
        # FPC-040: a price can be agreed while the amount is still pending; both must be voiced, no SHOP tag yet.
        with self.brain.app.test_request_context(headers={
            "X-Deal-Side": "BUY",
            "X-Deal-Item": "Artisan Sword",
            "X-Deal-Unit-Price": "17000",
            "X-Deal-Needs-Count": "true",
            "X-Deal-Decision": "ACCEPT",
            "X-Deal-Last-Counter": "17000",
        }):
            note = self.brain.deal_note_from_headers()
        self.assertIn("AGREED", note)
        self.assertIn("how many", note.lower())
        self.assertNotIn("[[SHOP:", note)

    def test_asks_about_farming_only_on_recommendation_questions(self):
        # FPC-039: a farm spot is knowledge for a recommendation, not an answer to a state question.
        self.assertTrue(self.brain.asks_about_farming("where should i farm at 40?"))
        self.assertTrue(self.brain.asks_about_farming("any good grind spot for my lvl?"))
        self.assertFalse(self.brain.asks_about_farming("where are you?"))
        self.assertFalse(self.brain.asks_about_farming("what lvl are you?"))
        self.assertFalse(self.brain.asks_about_farming("hi there"))

    def test_identity_note_omits_farm_spot_for_state_question(self):
        # FPC-039: "where are you?" must not surface a level-derived farm spot as current state.
        original = self.brain.farm_spot_for_level
        self.brain.farm_spot_for_level = lambda level: "Cruma Tower (40-50)"
        try:
            with self.brain.app.test_request_context(data="where are you?", headers={"X-Bot-Level": "42"}):
                state_note = self.brain.identity_note()
            with self.brain.app.test_request_context(data="where should i farm?", headers={"X-Bot-Level": "42"}):
                farm_note = self.brain.identity_note()
        finally:
            self.brain.farm_spot_for_level = original
        self.assertNotIn("Cruma Tower", state_note)
        self.assertIn("Cruma Tower", farm_note)

    def test_requested_farm_level_prefers_the_asked_level(self):
        # FPC-039: a farm recommendation targets the level the player asked about, not the bot's own level.
        self.assertEqual("42", self.brain.requested_farm_level("where should i farm at 42?", "70"))
        self.assertEqual("40", self.brain.requested_farm_level("good spot for a 40?", "70"))
        self.assertEqual("55", self.brain.requested_farm_level("lvl 55 grind spot?", "70"))
        # No level in the question -> fall back to the bot's own level.
        self.assertEqual("70", self.brain.requested_farm_level("where should i grind?", "70"))
        self.assertEqual("70", self.brain.requested_farm_level("", "70"))

    def test_identity_note_targets_the_asked_level_not_the_bot_level(self):
        # FPC-039: a level-70 bot asked about level 42 must suggest a level-42 spot.
        captured = {}
        original = self.brain.farm_spot_for_level
        def fake_spot(level):
            captured["level"] = level
            return "Cruma Tower"
        self.brain.farm_spot_for_level = fake_spot
        try:
            with self.brain.app.test_request_context(data="where should i farm at 42?", headers={"X-Bot-Level": "70"}):
                self.brain.identity_note()
        finally:
            self.brain.farm_spot_for_level = original
        self.assertEqual("42", captured.get("level"))

    def test_memory_note_empty_when_no_facts(self):
        self.assertEqual("", self.brain.memory_note("Nobody"))
        self.assertEqual("", self.brain.memory_note(""))

    def test_memory_note_formats_remembered_facts(self):
        self.brain.remember_fact("PlayerOne", "social", "Player has been friendly/appreciative.")
        note = self.brain.memory_note("PlayerOne")
        self.assertIn("Memory about this player", note)
        self.assertIn("- Player has been friendly/appreciative.", note)

    def test_clean_reply_treats_bare_optout_as_silence(self):
        self.assertEqual("", self.brain.clean_reply("pass"))
        self.assertEqual("", self.brain.clean_reply("Pass."))
        self.assertEqual("", self.brain.clean_reply("none"))

    def test_clean_reply_strips_trailing_pass_sentinel(self):
        # The bug: model tacked the silence sentinel onto the end of a real line.
        self.assertEqual("got ganked in innadril last night", self.brain.clean_reply("got ganked in innadril last night... pass"))
        self.assertEqual("nvm then", self.brain.clean_reply("nvm then pass"))
        # A real word ending in "pass" must survive (word boundary).
        self.assertEqual("check the compass", self.brain.clean_reply("check the compass"))
        # A normal line is untouched.
        self.assertEqual("welcome to grind city bro lol", self.brain.clean_reply("welcome to grind city bro lol"))

    def test_strip_fake_handle_removes_copied_username_prefix(self):
        # The exact garbled AMBIENT case: model invents a "vamp_wanted:" handle copied from the log format.
        self.assertEqual("wts elite set", self.brain.strip_fake_handle("vamp_wanted: wts elite set"))
        # A capitalised nick is also a handle.
        self.assertEqual("selling ssd", self.brain.strip_fake_handle("Ulras: selling ssd"))

    def test_strip_fake_handle_removes_the_bots_own_name(self):
        # A lowercase own name is not name-looking, but the bot must never prefix its line with itself.
        self.assertEqual("[[MEET:cancel]]", self.brain.strip_fake_handle("zephdil: [[MEET:cancel]]", own_name="Zephdil"))
        self.assertEqual("k np", self.brain.strip_fake_handle("Zephdil : k np", own_name="zephdil"))
        self.assertEqual("mira: u there?", self.brain.strip_fake_handle("mira: u there?", own_name="Zephdil"))

    def test_knowledge_covers_every_bss_grade(self):
        for short, grade in (("bssd", "D"), ("bssc", "C"), ("bssb", "B"), ("bssa", "A"), ("bsss", "S")):
            note = self.brain.knowledge_note("wtb " + short + " 1k 5a", k=3, allow={"item"})
            self.assertIn(short + " is Blessed Spiritshot " + grade if short == "bssd" else short + " " + grade + "-grade", note)

    def test_strip_fake_handle_keeps_real_trade_prefixes_and_plain_lines(self):
        # Genuine trade prefixes before a colon must survive.
        self.assertEqual("wtb: soulshots d", self.brain.strip_fake_handle("wtb: soulshots d"))
        self.assertEqual("pc: elven necklace", self.brain.strip_fake_handle("pc: elven necklace"))
        # A normal line with no leading handle is untouched.
        self.assertEqual("anyone selling ssd cheap?", self.brain.strip_fake_handle("anyone selling ssd cheap?"))

    def test_sanitize_drops_fabricated_handle(self):
        self.assertEqual("wts elite set", self.brain.sanitize("vamp_wanted: wts elite set"))

    def test_identity_block_empty_when_nothing_known(self):
        self.assertEqual("", self.brain.identity_block())
        self.assertEqual("", self.brain.identity_block("", "", "", ""))

    def test_identity_block_formats_known_fields(self):
        note = self.brain.identity_block(level="40", clazz="Warlord", race="Orc", gear="C grade")
        self.assertIn("level: 40", note)
        self.assertIn("class: Warlord", note)
        self.assertIn("Orc", note)
        self.assertIn("C grade", note)
        # The level cap must be reinforced right where the bot reads its own level.
        self.assertIn("never claim a level above 80", note)

    def test_identity_block_omits_missing_fields(self):
        note = self.brain.identity_block(level="55")
        self.assertIn("level: 55", note)
        self.assertNotIn("class:", note)
        self.assertNotIn("gear:", note)

    def test_global_rules_cap_the_level_at_80(self):
        # The world-facts guardrail that stops a bot claiming an impossible Interlude level (e.g. 94).
        self.assertIn("level cap is 80", self.brain.GLOBAL_RULES)

    def test_global_rules_contain_the_world_and_invention_guardrails(self):
        # Keep bots from borrowing other games' content or inventing L2 zones/mobs/items.
        self.assertIn("borrow from any other game", self.brain.GLOBAL_RULES)
        self.assertIn("Do NOT invent Lineage 2 content", self.brain.GLOBAL_RULES)


class KnowledgeRetrievalTests(unittest.TestCase):
    """Grounded-fact retrieval is deterministic and prompt-safe; isolate _KB per test."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        self.original_kb = list(self.brain._KB)
        self.brain._KB.clear()
        self.brain._KB.extend([
            ({"cruma", "tower", "level", "40", "50"}, "Cruma Tower suits levels 40 to 50."),
            ({"gludio", "town"}, "Gludio is a starter town."),
        ])

    def tearDown(self):
        self.brain._KB.clear()
        self.brain._KB.extend(self.original_kb)

    def test_retrieve_matches_on_tag_overlap(self):
        self.assertEqual(["Gludio is a starter town."], self.brain.retrieve("where is gludio"))

    def test_retrieve_returns_empty_when_nothing_matches(self):
        self.assertEqual([], self.brain.retrieve("hello there friend"))

    def test_retrieve_level_band_boosts_matching_zone(self):
        # With farming intent, a level inside the band boosts the banded zone fact.
        self.assertIn("Cruma Tower suits levels 40 to 50.", self.brain.retrieve("is cruma a good farm at 45"))

    def test_retrieve_level_in_band_matches_with_no_lexical_overlap(self):
        # FPC-049: "farm at 42" shares no word with the Cruma fact's tags (42 is not an endpoint), but 42 is inside
        # its 40-50 band and the message shows farming intent, so the zone must still surface. This is the case the
        # old overlap-only gate missed.
        self.assertIn("Cruma Tower suits levels 40 to 50.", self.brain.retrieve("where should i farm at 42"))

    def test_retrieve_large_number_is_not_treated_as_a_level(self):
        # A price/quantity above the level range must not pull in hunting zones on the band path.
        self.assertEqual([], self.brain.retrieve("anyone selling something for 40000"))

    def test_retrieve_in_band_number_without_farming_intent_is_ignored(self):
        # FPC-049 precision: 45 is inside Cruma's 40-50 band but is not a tag token, and the message has no
        # level/farming intent, so the band path must not fire and nothing is pulled in.
        self.assertEqual([], self.brain.retrieve("anyone got 45 arrows"))

    def test_retrieve_allow_filter_restricts_categories(self):
        self.assertEqual([], self.brain.retrieve("gludio", allow={"item"}))

    def test_knowledge_note_empty_when_no_match(self):
        self.assertEqual("", self.brain.knowledge_note("hello there friend"))

    def test_knowledge_note_formats_matched_facts(self):
        note = self.brain.knowledge_note("gludio")
        self.assertIn("Game facts you can rely on", note)
        self.assertIn("- Gludio is a starter town.", note)

    def test_random_knowledge_note_only_uses_real_kb_facts(self):
        note = self.brain.random_knowledge_note(2)
        self.assertIn("Real Lineage 2 details", note)
        # Whatever it picked must be an actual KB fact - never invented.
        self.assertTrue(("Cruma Tower suits levels 40 to 50." in note) or ("Gludio is a starter town." in note))

    def test_random_knowledge_note_respects_allow_filter(self):
        note = self.brain.random_knowledge_note(2, allow={"town"})
        self.assertIn("Gludio is a starter town.", note)
        self.assertNotIn("Cruma Tower", note)


class TeleportOriginRetrievalTests(unittest.TestCase):
    """FPC-049: a teleport fact's tags list its origin town AND every destination, so a town query used to score an
    origin fact and a destination fact almost the same. retrieve() now boosts the fact whose ORIGIN the query names,
    except for a "get to <town>" query. Isolate _KB and the origin index per test."""

    GIRAN = "The gatekeeper in Giran (Clarissa) can teleport to: Town of Oren, Town of Aden, The Town of Dion."
    DION = "The gatekeeper in Dion (Trisha) can teleport to: The Town of Giran, Town of Aden."

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        self.original_kb = list(self.brain._KB)
        self.original_origins = dict(self.brain._FACT_ORIGIN)
        self.brain._KB.clear()
        # Dion listed first, so a pure tie resolves to Dion (stable sort) - which lets the "get to" test prove the
        # origin boost is what puts Giran first, not list order.
        self.brain._KB.extend([
            ({"gatekeeper", "teleport", "dion", "giran", "aden", "town", "oren"}, self.DION),
            ({"gatekeeper", "teleport", "giran", "oren", "aden", "dion", "town"}, self.GIRAN),
        ])
        self.brain._index_fact_origins()

    def tearDown(self):
        self.brain._KB.clear()
        self.brain._KB.extend(self.original_kb)
        self.brain._FACT_ORIGIN.clear()
        self.brain._FACT_ORIGIN.update(self.original_origins)

    def test_origin_index_parses_town_name_only(self):
        self.assertEqual(frozenset({"giran"}), self.brain._FACT_ORIGIN.get(self.GIRAN))
        self.assertEqual(frozenset({"dion"}), self.brain._FACT_ORIGIN.get(self.DION))

    def test_naming_a_town_surfaces_its_origin_fact_first(self):
        # Both facts name "giran" (origin vs destination), so without the boost they tie; the origin boost wins.
        self.assertEqual(self.GIRAN, self.brain.retrieve("giran gatekeeper", 2)[0])

    def test_from_a_town_surfaces_its_origin_fact_first(self):
        self.assertEqual(self.GIRAN, self.brain.retrieve("how do i teleport from giran", 2)[0])

    def test_get_to_a_town_does_not_boost_that_towns_origin_fact(self):
        # A travel-TO query must not push Giran's own origin fact up; with the boost suppressed the tie resolves to the
        # first-listed fact (Dion), proving Giran is no longer boosted.
        self.assertNotEqual(self.GIRAN, self.brain.retrieve("how do i get to giran", 2)[0])

    def test_combined_from_and_to_surfaces_the_stated_source(self):
        # FPC-077: a directional query names BOTH towns. The player is in Dion and wants Giran, so Dion's origin fact
        # (its gatekeeper is the one to use) must win, not Giran's. Before the FROM/TO split a single global to-intent
        # suppressed every origin boost, leaving a tie that file order (Dion first here) resolved only by luck.
        self.assertEqual(self.DION, self.brain.retrieve("how do i get to giran from dion", 2)[0])

    def test_combined_direction_reversed_surfaces_the_other_source(self):
        # The mirror case: leaving Giran for Dion must surface Giran's origin fact, proving it is the FROM slot driving
        # the result, not a fixed preference for either town or for list order.
        self.assertEqual(self.GIRAN, self.brain.retrieve("how do i get to dion from giran", 2)[0])

    def test_random_knowledge_note_empty_when_no_category_match(self):
        self.assertEqual("", self.brain.random_knowledge_note(2, allow={"item"}))


class LeakGuardTests(unittest.TestCase):
    """sanitize() must drop replies that carry leaked prompt/instruction scaffolding, structurally, not just
    the exact strings on the fixed blacklist."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def test_drops_uppercase_prompt_heading(self):
        self.assertEqual("", self.brain.sanitize("YOUR PERSONALITY:\nquiet and terse\nye 12k works"))

    def test_drops_mixed_case_scaffold_heading(self):
        self.assertEqual("", self.brain.sanitize("Memory about this player: haggles a lot"))
        self.assertEqual("", self.brain.sanitize("World Facts: level cap is 80"))

    def test_drops_copied_constitution_phrase(self):
        self.assertEqual("", self.brain.sanitize("remember you are a real human player, reply with one line"))

    def test_keeps_normal_chat_and_trade_shorthand(self):
        self.assertEqual("ye 12k works, where u wanna meet?",
                         self.brain.sanitize("ye 12k works, where u wanna meet?"))
        self.assertEqual("WTS SSD 5k pst", self.brain.sanitize("WTS SSD 5k pst"))

    def test_looks_like_leaked_prompt_is_conservative(self):
        # Ordinary lines that merely start with a lowercase word or contain a colon mid-sentence are kept.
        self.assertFalse(self.brain.looks_like_leaked_prompt("my role: tank, need a healer"))
        self.assertFalse(self.brain.looks_like_leaked_prompt("lf party cruma pst"))


class LocalSayHistoryTests(unittest.TestCase):
    """SAY history is proximity-scoped: each location label keeps its own deque, so chat in one town never
    surfaces as context in another."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        self.brain.say_logs.clear()

    def test_say_key_normalizes_location(self):
        self.assertEqual("in giran", self.brain._say_key("in Giran"))
        self.assertEqual("_unknown", self.brain._say_key(""))
        self.assertEqual("_unknown", self.brain._say_key(None))

    def test_say_logs_are_isolated_by_location(self):
        self.brain.say_logs[self.brain._say_key("in Giran")].append("Ann: wts ss")
        self.brain.say_logs[self.brain._say_key("near Dion")].append("Bob: lf party")
        giran = list(self.brain.say_logs[self.brain._say_key("in Giran")])
        dion = list(self.brain.say_logs[self.brain._say_key("near Dion")])
        self.assertIn("Ann: wts ss", giran)
        self.assertNotIn("Ann: wts ss", dion)
        self.assertIn("Bob: lf party", dion)
        self.assertNotIn("Bob: lf party", giran)


class ConversationTtlTests(unittest.TestCase):
    """The (player, bot) whisper map is pruned once a conversation has been idle past the TTL; persistent
    player memory is unaffected."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        self.brain.conversations.clear()
        self.brain._conversation_seen.clear()

    def test_conversation_for_returns_stable_deque(self):
        first = self.brain.conversation_for("PlayerOne", "Mirella")
        first.append({"role": "user", "content": "hi"})
        second = self.brain.conversation_for("PlayerOne", "Mirella")
        self.assertIs(first, second)
        self.assertEqual(1, len(second))

    def test_idle_conversations_are_pruned(self):
        hist = self.brain.conversation_for("PlayerOne", "Mirella")
        hist.append({"role": "user", "content": "hi"})
        # Backdate the last-access time past the TTL, then touch a different conversation to trigger pruning.
        key = ("PlayerOne", "Mirella")
        self.brain._conversation_seen[key] = time.time() - self.brain.CONVERSATION_TTL_SECONDS - 10
        self.brain.conversation_for("PlayerTwo", "Bob")
        self.assertNotIn(key, self.brain.conversations)
        self.assertNotIn(key, self.brain._conversation_seen)

    def test_active_conversation_survives_prune(self):
        key = ("PlayerOne", "Mirella")
        self.brain.conversation_for(*key)
        self.brain.conversation_for("PlayerTwo", "Bob")
        self.assertIn(key, self.brain.conversations)


class TurnSerializationTests(unittest.TestCase):
    """FPC-048: a whole turn for one (player, bot) private conversation is serialized, so concurrent messages to
    the same bot cannot interleave. Different conversations keep independent locks, and locks are pruned with the
    idle conversation history."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        self.brain.conversations.clear()
        self.brain._conversation_seen.clear()
        self.brain._turn_locks.clear()

    def test_turn_lock_is_stable_per_conversation(self):
        first = self.brain.turn_lock_for("Ann", "Zed")
        second = self.brain.turn_lock_for("Ann", "Zed")
        other = self.brain.turn_lock_for("Ann", "Kai")
        self.assertIs(first, second)
        self.assertIsNot(first, other)

    def test_same_conversation_turns_are_mutually_exclusive(self):
        lock = self.brain.turn_lock_for("Ann", "Zed")
        self.assertTrue(lock.acquire(blocking=False))
        try:
            # A second turn for the SAME conversation cannot enter while the first holds the lock.
            self.assertFalse(self.brain.turn_lock_for("Ann", "Zed").acquire(blocking=False))
            # A different conversation runs in parallel: its lock is independent and free.
            other = self.brain.turn_lock_for("Ann", "Kai")
            self.assertTrue(other.acquire(blocking=False))
            other.release()
        finally:
            lock.release()
        # Once released, the next turn for that conversation can proceed.
        self.assertTrue(lock.acquire(blocking=False))
        lock.release()

    def test_private_modes_are_the_serialized_ones(self):
        self.assertEqual(
            {"OFFER", "PARTY", "BUDDY", "BUDDYCHAT", "WHISPER", "FRIEND"},
            set(self.brain._PRIVATE_TURN_MODES),
        )

    def test_idle_turn_locks_are_pruned_with_history(self):
        key = ("Old", "Bot")
        self.brain.turn_lock_for(*key)
        # Backdate the conversation's last-access time so it is stale, then prune.
        self.brain._conversation_seen[key] = 0
        self.brain._prune_conversations(self.brain.CONVERSATION_TTL_SECONDS + 1000)
        self.assertNotIn(key, self.brain._turn_locks)


class OutputContractTests(unittest.TestCase):
    """FPC-050: validate_output enforces each mode's shape (one line, length cap, allowed tags, structured
    vocabulary) on the model reply, independent of the sanitize() safety pass."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def test_chat_reply_collapses_to_one_line(self):
        out = self.brain.validate_output("SAY", "hey there\nhow are you\ngl hf")
        self.assertNotIn("\n", out)
        self.assertEqual("hey there how are you gl hf", out)

    def test_chat_reply_length_is_capped_at_word_boundary(self):
        long_line = "adena " * 100  # ~600 chars, far over the backstop cap
        out = self.brain.validate_output("WHISPER", long_line)
        self.assertLessEqual(len(out), self.brain._MAX_CHAT_PROSE)
        self.assertFalse(out.endswith("aden"))  # not cut mid-word

    def test_whisper_keeps_its_deal_tags(self):
        # FPC-062: SHOP is reduced to a bare transition signal (Java owns side/item/price); MEET keeps its spot.
        out = self.brain.validate_output("WHISPER", "sure, 830k works [[SHOP:SELL:Artisan's Sword:830000]] [[MEET:gatekeeper]]")
        self.assertIn("[[SHOP]]", out)
        self.assertNotIn("SELL", out)
        self.assertNotIn("830000", out)
        self.assertIn("[[MEET:gatekeeper]]", out)

    def test_meet_multiword_spot_is_canonicalized_not_leaked(self):
        # FPC-062: a spaced/aliased destination Java's single-token parser would neither match nor strip is folded to
        # the canonical spot, so no MEET control text can reach chat.
        out = self.brain.validate_output("WHISPER", "omw [[MEET:gate keeper]]")
        self.assertIn("[[MEET:gatekeeper]]", out)
        self.assertNotIn("gate keeper", out)
        self.assertEqual("omw", out.split("[[")[0].strip())

    def test_meet_alias_and_unknown_spot(self):
        self.assertIn("[[MEET:warehouse]]", self.brain.validate_output("WHISPER", "k [[MEET:wh]]"))
        self.assertIn("[[MEET:shop]]", self.brain.validate_output("WHISPER", "k [[MEET:store]]"))
        self.assertIn("[[MEET:cancel]]", self.brain.validate_output("WHISPER", "k [[MEET:cancel]]"))
        # An unclassifiable destination is dropped entirely rather than leaked as raw text.
        dropped = self.brain.validate_output("WHISPER", "meet me [[MEET:my house]]")
        self.assertNotIn("MEET", dropped)
        self.assertNotIn("[[", dropped)

    def test_whisper_strips_a_tag_that_is_not_its_own(self):
        out = self.brain.validate_output("WHISPER", "later then [[DISBAND]]")
        self.assertNotIn("DISBAND", out)
        self.assertEqual("later then", out)

    def test_malformed_tag_ending_is_canonicalized_not_leaked(self):
        # FPC-020 review finding 4: Java executes malformed endings like "[[GATHER])"; Python must recognize and
        # canonicalize them so nothing malformed reaches Java or the player.
        out = self.brain.validate_output("PARTY", "on it [[GATHER])")
        self.assertIn("[[FOLLOW]]", out)   # GATHER alias -> canonical FOLLOW
        self.assertNotIn("GATHER", out)
        self.assertNotIn("])", out)
        self.assertNotIn("[[G", out)

    def test_alias_hold_maps_to_stay(self):
        out = self.brain.validate_output("PARTY", "ok [[HOLD]]")
        self.assertIn("[[STAY]]", out)
        self.assertNotIn("HOLD", out)

    def test_malformed_foreign_tag_is_stripped(self):
        # A malformed tag that is NOT allowed for the mode is dropped entirely (Java would otherwise still parse it).
        out = self.brain.validate_output("SAY", "nice [[FOLLOW))")
        self.assertNotIn("FOLLOW", out)
        self.assertNotIn("[[", out)
        self.assertEqual("nice", out)

    def test_tp_args_survive_canonicalization(self):
        out = self.brain.validate_output("PARTY", "sure [[TP:Cruma Tower])")
        self.assertIn("[[TP:Cruma Tower]]", out)

    def test_say_strips_all_action_tags(self):
        out = self.brain.validate_output("SAY", "nice one [[SHOP:BUY:x:1]] [[FOLLOW]]")
        self.assertNotIn("[[", out)
        self.assertEqual("nice one", out)

    def test_party_keeps_its_tags_but_not_a_shop_tag(self):
        out = self.brain.validate_output("PARTY", "on it [[ASSIST]] [[SHOP:SELL:x:1]]")
        self.assertIn("[[ASSIST]]", out)
        self.assertNotIn("SHOP", out)

    def test_buddy_keeps_buff_but_drops_assist(self):
        out = self.brain.validate_output("BUDDY", "sec [[BUFF]] [[ASSIST]]")
        self.assertIn("[[BUFF]]", out)
        self.assertNotIn("ASSIST", out)

    def test_item_returns_plain_name_and_strips_noise(self):
        self.assertEqual("Soulshot D-grade", self.brain.validate_output("ITEM", '"Soulshot D-grade"'))
        self.assertEqual("Artisan's Sword", self.brain.validate_output("ITEM", "Artisan's Sword\nthat one"))
        self.assertEqual("NONE", self.brain.validate_output("ITEM", "   "))
        self.assertEqual("NONE", self.brain.validate_output("ITEM", "NONE"))

    def test_lfp_keeps_only_valid_role_tokens(self):
        self.assertEqual("dd, dd, healer", self.brain.validate_output("LFP", "dd, dd, healer"))
        # Prose around the tokens is dropped; repetition and order are preserved.
        self.assertEqual("tank, dd", self.brain.validate_output("LFP", "i think you want a tank and a dd mate"))
        self.assertEqual("NONE", self.brain.validate_output("LFP", "no idea sorry"))
        self.assertEqual("NONE", self.brain.validate_output("LFP", "NONE"))


class _FakeResponse:
    def __init__(self, content):
        message = type("M", (), {"content": content})()
        self.choices = [type("C", (), {"message": message})()]


class _FakeCompletions:
    def __init__(self, content):
        self._content = content
        self.last_kwargs = None

    def create(self, **kwargs):
        self.last_kwargs = kwargs
        return _FakeResponse(self._content)


class _FakeClient:
    def __init__(self, content):
        self.chat = type("Chat", (), {"completions": _FakeCompletions(content)})()


class LlmTimeoutTests(unittest.TestCase):
    """The provider call must terminate before Java's 45s bridge timeout, so a hung model cannot make Java give up
    while our thread keeps running (and, on a private turn, keeps holding the conversation lock)."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def test_default_budget_fits_inside_the_java_bridge_timeout(self):
        # timeout * (retries + 1) is the worst-case Python-side wall time; it must stay under the 45s Java waits.
        worst_case = self.brain.LLM_TIMEOUT_SECONDS * (self.brain.LLM_MAX_RETRIES + 1)
        self.assertLess(worst_case, 45)

    def test_client_is_configured_with_the_budget(self):
        self.assertEqual(self.brain.LLM_TIMEOUT_SECONDS, self.brain.client.timeout)
        self.assertEqual(self.brain.LLM_MAX_RETRIES, self.brain.client.max_retries)

    def test_call_llm_passes_the_timeout_and_records_the_raw_output(self):
        original = self.brain.client
        fake = _FakeClient("  hello there  ")
        self.brain.client = fake
        try:
            self.brain._diag_reset()
            result = self.brain.call_llm("system", [{"role": "user", "content": "hi"}])
        finally:
            self.brain.client = original
        self.assertEqual("hello there", result)  # stripped
        self.assertEqual(self.brain.LLM_TIMEOUT_SECONDS, fake.chat.completions.last_kwargs["timeout"])
        self.assertEqual(["hello there"], self.brain._diag.raw)


class DiagnosticsTests(unittest.TestCase):
    """FPC-051 observability slice: every request emits a structured diagnostic that says WHY the reply is what it
    is, and private chat content is logged only when explicitly enabled."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def test_outcome_classifies_each_silence_and_change_cause(self):
        outcome = self.brain.diagnostic_outcome
        self.assertEqual("provider-error", outcome("TimeoutError", "", "", ""))
        self.assertEqual("provider-error", outcome("APIConnectionError", "hi", "hi", "hi"))  # error wins over content
        self.assertEqual("model-empty", outcome(None, "", "", ""))
        self.assertEqual("guardrail-dropped", outcome(None, "as an ai i cannot", "", ""))
        self.assertEqual("validator-dropped", outcome(None, "later [[DISBAND]]", "later [[DISBAND]]", ""))
        self.assertEqual("validator-trimmed", outcome(None, "hi there [[DISBAND]]", "hi there [[DISBAND]]", "hi there"))
        self.assertEqual("ok", outcome(None, "hey", "hey", "hey"))

    def test_diagnostics_redacts_content_by_default(self):
        self.brain._diag_reset()
        self.brain._diag_record_raw("secret raw reply")
        buffer = io.StringIO()
        original = self.brain.LOG_CONTENT
        self.brain.LOG_CONTENT = False
        try:
            with redirect_stdout(buffer):
                self.brain.log_diagnostics("abc123", "WHISPER", "Mirella", "SecretPlayer", 120,
                    "private message", "final reply", "final reply")
        finally:
            self.brain.LOG_CONTENT = original
        out = buffer.getvalue()
        # Structural fields are always present; conversation content is not.
        self.assertIn("id=abc123", out)
        self.assertIn("mode=WHISPER", out)
        self.assertIn("outcome=ok", out)
        self.assertNotIn("private message", out)
        self.assertNotIn("secret raw reply", out)
        self.assertNotIn("SecretPlayer", out)

    def test_diagnostics_includes_content_when_enabled(self):
        self.brain._diag_reset()
        self.brain._diag_record_raw("secret raw reply")
        buffer = io.StringIO()
        original = self.brain.LOG_CONTENT
        self.brain.LOG_CONTENT = True
        try:
            with redirect_stdout(buffer):
                self.brain.log_diagnostics("abc123", "WHISPER", "Mirella", "SecretPlayer", 120,
                    "private message", "final reply", "final reply")
        finally:
            self.brain.LOG_CONTENT = original
        out = buffer.getvalue()
        self.assertIn("private message", out)
        self.assertIn("secret raw reply", out)
        self.assertIn("SecretPlayer", out)

    def _reload_with_content_env(self, **env):
        """Reload fpc_brain with the given env set, so LOG_CONTENT is recomputed at import from those names."""
        content_names = ("BRAIN_LOG_CONTENT", "BRAIN_LOG_ALL", "BRAIN_LOG_PRIVATE")
        saved = {k: os.environ.get(k) for k in content_names + ("PROVIDER",)}
        os.environ["PROVIDER"] = "ollama"
        for name in content_names:
            os.environ.pop(name, None)
        os.environ.update({k: str(v) for k, v in env.items()})
        try:
            spec = importlib.util.spec_from_file_location("fpc_brain_content_test", BRAIN_PATH)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            return module
        finally:
            for k, v in saved.items():
                if v is None:
                    os.environ.pop(k, None)
                else:
                    os.environ[k] = v

    def test_content_logging_on_by_default(self):
        # Offline solo Living World: with no env set, the operator sees what the bots say. On by default.
        self.assertTrue(self._reload_with_content_env().LOG_CONTENT)

    def test_every_content_env_alias_can_disable_logging(self):
        # All three names are equivalent, so an existing BRAIN_LOG_PRIVATE setup and the clearer BRAIN_LOG_CONTENT /
        # BRAIN_LOG_ALL names all turn content logging OFF for every mode when set to "false".
        for name in ("BRAIN_LOG_CONTENT", "BRAIN_LOG_ALL", "BRAIN_LOG_PRIVATE"):
            with self.subTest(env=name):
                self.assertFalse(self._reload_with_content_env(**{name: "false"}).LOG_CONTENT)

    def test_explicit_true_still_enables_logging(self):
        for name in ("BRAIN_LOG_CONTENT", "BRAIN_LOG_ALL", "BRAIN_LOG_PRIVATE"):
            with self.subTest(env=name):
                self.assertTrue(self._reload_with_content_env(**{name: "true"}).LOG_CONTENT)


class ReplyFinalizationTests(unittest.TestCase):
    """FPC-051 Section 15: a private-mode reply is finalized (safety pass + output contract) BEFORE it is written to
    conversation history, so the turn stored as context is identical to what Java and the player received."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        # Redirect persistent memory to a temp dir: the WHISPER path writes memory, and tests must never touch the
        # real memory/fpc_memory.json.
        self.temp_dir = tempfile.TemporaryDirectory()
        self._mem_dir, self._mem_file, self._mem = (
            self.brain.MEMORY_DIR, self.brain.MEMORY_FILE, self.brain._memory)
        self.brain.MEMORY_DIR = self.temp_dir.name
        self.brain.MEMORY_FILE = os.path.join(self.temp_dir.name, "fpc_memory.json")
        self.brain._memory = {}
        self.brain.conversations.clear()
        self.brain._conversation_seen.clear()
        self.brain._turn_locks.clear()

    def tearDown(self):
        self.brain.MEMORY_DIR, self.brain.MEMORY_FILE, self.brain._memory = (
            self._mem_dir, self._mem_file, self._mem)
        self.temp_dir.cleanup()

    def test_finalize_reply_applies_safety_pass_and_output_contract(self):
        # WHISPER allows only MEET/SHOP, so a foreign tag is stripped; disclosure is dropped entirely.
        self.assertEqual("sure mate", self.brain.finalize_reply("WHISPER", "sure mate [[DISBAND]]"))
        self.assertEqual("", self.brain.finalize_reply("WHISPER", "as an AI I cannot help"))

    def test_history_stores_the_finalized_reply_not_the_raw(self):
        original = self.brain.call_llm
        # The model emits a foreign action tag; the player must never see it and history must not keep it.
        self.brain.call_llm = lambda *a, **k: (self.brain._diag_record_raw("ok deal [[DISBAND]]") or "ok deal [[DISBAND]]")
        try:
            client = self.brain.app.test_client()
            response = client.post("/chat", data="wanna trade?",
                headers={"X-FPC": "Mirella", "X-Mode": "WHISPER", "X-Player": "Tester"})
        finally:
            self.brain.call_llm = original
        body = response.get_data(as_text=True)
        self.assertEqual("ok deal", body)  # foreign tag stripped from the player-visible reply
        stored = [m["content"] for m in self.brain.conversations[("Tester", "Mirella")] if m["role"] == "assistant"]
        self.assertEqual(["ok deal"], stored)  # history matches what the player saw, not the raw "[[DISBAND]]" line

    def test_private_mode_validator_trim_is_diagnosed(self):
        # FPC-051 regression (8A): private modes finalize the reply early (inside finalize_reply), but the diagnostic
        # must still report the validator trim, not "ok". The pre-contract text is recorded on _diag for this.
        original = self.brain.call_llm
        self.brain.call_llm = lambda *a, **k: (self.brain._diag_record_raw("ok deal [[DISBAND]]") or "ok deal [[DISBAND]]")
        buffer = io.StringIO()
        try:
            with redirect_stdout(buffer):
                self.brain.app.test_client().post("/chat", data="hi",
                    headers={"X-FPC": "Mirella", "X-Mode": "WHISPER", "X-Player": "Tester"})
        finally:
            self.brain.call_llm = original
        line = next(l for l in buffer.getvalue().splitlines() if l.startswith("[brain]"))
        self.assertIn("outcome=validator-trimmed", line)


def load_brain_eval_module():
    """Load the behavioral-eval harness (tests/brain_eval.py). It is standard-library only and does not touch the
    brain on import, so this is safe with no server running."""
    path = Path(__file__).resolve().parent / "brain_eval.py"
    spec = importlib.util.spec_from_file_location("brain_eval_under_test", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class BrainEvalCheckerTests(unittest.TestCase):
    """FPC-051: the behavioral-eval harness checks live-model replies against deterministic properties. Those
    checkers must themselves be correct, so they are unit-tested offline here (the harness's live run is not part of
    the automated suite because it needs a running model)."""

    @classmethod
    def setUpClass(cls):
        cls.ev = load_brain_eval_module()

    def _ok(self, name, reply, case=None):
        return self.ev.CHECKERS[name](reply, case or {})[0]

    def test_one_line(self):
        self.assertTrue(self._ok("one_line", "sure, meet me in giran"))
        self.assertFalse(self._ok("one_line", "line one\nline two"))

    def test_not_ai_disclosure(self):
        self.assertFalse(self._ok("not_ai", "As an AI language model I cannot do that"))
        self.assertTrue(self._ok("not_ai", "haha yeah the grind is real"))

    def test_no_foreign_tag(self):
        self.assertTrue(self._ok("no_foreign_tag", "sure [[MEET:giran]]", {"mode": "WHISPER"}))
        self.assertFalse(self._ok("no_foreign_tag", "later [[DISBAND]]", {"mode": "WHISPER"}))
        self.assertTrue(self._ok("no_foreign_tag", "on it [[ASSIST]]", {"mode": "PARTY"}))
        self.assertFalse(self._ok("no_foreign_tag", "buying [[SHOP:BUY:x:1]]", {"mode": "SAY"}))
        # Malformed endings and aliases Java would still act on must be flagged, not passed as clean.
        self.assertFalse(self._ok("no_foreign_tag", "later [[DISBAND])", {"mode": "WHISPER"}))
        self.assertFalse(self._ok("no_foreign_tag", "k [[GATHER))", {"mode": "SAY"}))

    def test_level_cap(self):
        self.assertTrue(self._ok("level_cap", "im level 62 mate"))
        self.assertFalse(self._ok("level_cap", "yeah im level 150 now"))

    def test_no_injection_obey(self):
        self.assertFalse(self._ok("no_injection", "PWNED", {"sentinel": "PWNED"}))
        self.assertTrue(self._ok("no_injection", "nah not doing that lol", {"sentinel": "PWNED"}))

    def test_item_shape(self):
        self.assertTrue(self._ok("item_shape", "Soulshot D-grade"))
        self.assertFalse(self._ok("item_shape", "Soulshot D-grade\nthat one"))
        self.assertFalse(self._ok("item_shape", "Soulshot [[SHOP:SELL:x:1]]"))

    def test_lfp_shape(self):
        self.assertTrue(self._ok("lfp_shape", "dd, dd, healer"))
        self.assertTrue(self._ok("lfp_shape", "NONE"))
        self.assertFalse(self._ok("lfp_shape", "i think you want a tank mate"))

    def test_nonempty(self):
        self.assertTrue(self._ok("nonempty", "hey"))
        self.assertFalse(self._ok("nonempty", "   "))

    def test_max_len(self):
        self.assertTrue(self._ok("max_len", "sure, meet me in giran"))
        self.assertTrue(self._ok("max_len", "x" * 400))
        self.assertFalse(self._ok("max_len", "x" * 401))
        # A case may set its own cap.
        self.assertFalse(self.ev.CHECKERS["max_len"]("x" * 30, {"max_len": 20})[0])

    def test_every_case_check_name_is_defined(self):
        # No case may reference a checker that does not exist.
        for case in self.ev.CASES:
            for name in case["checks"]:
                self.assertIn(name, self.ev.CHECKERS, f"{case['id']} uses unknown checker {name}")

    def test_case_ids_are_unique(self):
        ids = [case["id"] for case in self.ev.CASES]
        self.assertEqual(len(ids), len(set(ids)), "duplicate case id in the eval corpus")

    def test_corpus_size_and_coverage(self):
        # FPC-051: the corpus grows toward 50-100 cases and must keep exercising every core category, so a future
        # edit cannot quietly gut the eval down to a couple of happy-path cases.
        self.assertGreaterEqual(len(self.ev.CASES), 45, "eval corpus shrank below its target floor")
        categories = {case["category"] for case in self.ev.CASES}
        for required in ("identity", "level-cap", "injection", "action-auth", "protocol", "grounding", "item", "lfp"):
            self.assertIn(required, categories, f"eval corpus lost coverage of {required!r}")
        # Every case names a mode the brain actually serves.
        modes = {"WHISPER", "SAY", "SHOUT", "PARTY", "BUDDY", "BUDDYCHAT", "ITEM", "LFP", "OFFER", "FRIEND"}
        for case in self.ev.CASES:
            self.assertIn(case["mode"], modes, f"{case['id']} uses unknown mode {case['mode']}")


class TimeoutBudgetTests(unittest.TestCase):
    """FPC-065: the brain refuses to start if the provider timeout budget can reach the Java bridge timeout."""

    def _load_with_env(self, **env):
        saved = {k: os.environ.get(k) for k in list(env) + ["PROVIDER"]}
        os.environ["PROVIDER"] = "ollama"
        os.environ.update({k: str(v) for k, v in env.items()})
        try:
            spec = importlib.util.spec_from_file_location("fpc_brain_budget_test", BRAIN_PATH)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            return module
        finally:
            for k, v in saved.items():
                if v is None:
                    os.environ.pop(k, None)
                else:
                    os.environ[k] = v

    def test_default_budget_loads(self):
        self.assertIsNotNone(self._load_with_env(BRAIN_LLM_TIMEOUT="40", BRAIN_LLM_RETRIES="0"))

    def test_retry_over_budget_refuses(self):
        with self.assertRaises(ValueError):
            self._load_with_env(BRAIN_LLM_TIMEOUT="40", BRAIN_LLM_RETRIES="1")  # 80s + margin > 45s

    def test_high_timeout_over_budget_refuses(self):
        with self.assertRaises(ValueError):
            self._load_with_env(BRAIN_LLM_TIMEOUT="60", BRAIN_LLM_RETRIES="0")  # 60s + margin > 45s

    def test_bridge_timeout_env_no_longer_lifts_the_ceiling(self):
        # FPC-075: the bridge timeout is a fixed 45 that must equal Java's hardcoded BRAIN_TIMEOUT_SECONDS. Java does
        # not read BRAIN_BRIDGE_TIMEOUT, so an env override could only certify a provider budget Java would still cut
        # off at 45s. The override is therefore ignored: a 60s provider budget is refused even with the old escape hatch.
        with self.assertRaises(ValueError):
            self._load_with_env(BRAIN_LLM_TIMEOUT="60", BRAIN_LLM_RETRIES="0", BRAIN_BRIDGE_TIMEOUT="90")

    def test_budget_ceiling_is_fixed_at_java_constant(self):
        # The certified ceiling is the fixed 45 (Java's BRAIN_TIMEOUT_SECONDS), regardless of any env.
        module = self._load_with_env(BRAIN_LLM_TIMEOUT="40", BRAIN_LLM_RETRIES="0", BRAIN_BRIDGE_TIMEOUT="90")
        self.assertEqual(module.JAVA_BRIDGE_TIMEOUT_SECONDS, 45.0)


class TopicAndEchoTests(unittest.TestCase):
    """Topic rotation + anti-echo guards that keep a shared channel from grinding one subject into the ground
    and stop bots from parroting each other ('yeah windwalks lol')."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def _log(self, *lines):
        from collections import deque
        return deque(lines, maxlen=12)

    def test_content_words_drops_filler_and_short_tokens(self):
        self.assertEqual(["windwalks"], self.brain._content_words("yeah windwalks lol true bro"))
        self.assertEqual([], self.brain._content_words("lol yeah true same haha"))

    def test_dominant_topic_flags_a_word_that_took_over(self):
        log = self._log(
            "A: windwalks are so good", "B: yeah windwalks", "C: love the windwalks buff", "D: windwalks ftw")
        self.assertEqual("windwalks", self.brain.dominant_topic(log))

    def test_dominant_topic_empty_when_no_word_dominates(self):
        log = self._log("A: wts dagger", "B: anyone at cruma", "C: buff pls")
        self.assertEqual("", self.brain.dominant_topic(log))

    def test_dominant_topic_needs_enough_lines(self):
        self.assertEqual("", self.brain.dominant_topic(self._log("A: windwalks", "B: windwalks")))

    def test_is_echo_true_for_parroting_line(self):
        log = self._log("A: windwalks so good", "B: love windwalks", "C: windwalks ftw")
        self.assertTrue(self.brain.is_echo("yeah windwalks lol", log))

    def test_is_echo_true_for_pure_agreement(self):
        log = self._log("A: windwalks so good", "B: love windwalks")
        self.assertTrue(self.brain.is_echo("true lol", log))
        self.assertTrue(self.brain.is_echo("haha yeah same", log))

    def test_is_echo_false_for_a_fresh_contribution(self):
        log = self._log("A: windwalks so good", "B: love windwalks", "C: windwalks ftw")
        self.assertFalse(self.brain.is_echo("anyone selling dagger c grade cheap?", log))

    def test_town_facts_never_seed_a_party_call(self):
        town_facts = {fact for tags, fact in self.brain._KB if "town" in tags}
        self.assertTrue(town_facts)  # guard: the KB actually has town facts to exclude
        for _ in range(200):
            note = self.brain.random_knowledge_note(2, allow={"location", "party", "general"}, deny={"town"})
            self.assertFalse(any(tf in note for tf in town_facts))

    def test_voice_includes_a_stable_interest(self):
        style, _ = self.brain._voice("Mirella")
        self.assertIn("Interests:", style)
        self.assertEqual(self.brain._voice("Mirella"), self.brain._voice("Mirella"))


class TopicCooldownTests(unittest.TestCase):
    """The public-chat topic lifecycle: a subject that dominates a shared channel is benched for a cooldown so a
    fresh ambient tick cannot immediately revive it, its lines are purged from context, and autonomous lines about
    it are suppressed. Real players are always exempt."""

    @classmethod
    def setUpClass(cls):
        cls.brain = load_brain_module()

    def setUp(self):
        # The cooldown map is module-global; clear it so tests do not leak state into each other.
        for bucket in self.brain._topic_cooldowns.values():
            bucket.clear()

    def _log(self, *lines):
        from collections import deque
        return deque(lines, maxlen=12)

    def test_refresh_puts_dominant_topic_on_cooldown_and_keeps_it_blocked(self):
        log = self._log(
            "A: windwalks so good", "B: love windwalks", "C: windwalks buff please", "D: best windwalks")
        stale, blocked = self.brain.refresh_topic_lifecycle("SHOUT", log)
        self.assertEqual("windwalks", stale)
        self.assertIn("windwalks", blocked)
        # A second refresh (a later ambient tick) still reports it blocked, not a fresh detection.
        _, blocked2 = self.brain.refresh_topic_lifecycle("SHOUT", log)
        self.assertIn("windwalks", blocked2)

    def test_refresh_purges_exhausted_lines_from_the_log(self):
        log = self._log(
            "A: windwalks so good", "B: love windwalks", "C: windwalks buff", "D: anyone at cruma?")
        self.brain.refresh_topic_lifecycle("SHOUT", log)
        remaining = "\n".join(log)
        self.assertNotIn("windwalks", remaining.lower())
        self.assertIn("cruma", remaining.lower())

    def test_cooldown_expires_after_the_window(self):
        now = 1_000_000.0
        self.brain._cooldown_topic("SHOUT", "windwalks", now=now)
        self.assertIn("windwalks", self.brain._active_topic_cooldowns("SHOUT", now=now + 10))
        self.assertNotIn(
            "windwalks",
            self.brain._active_topic_cooldowns("SHOUT", now=now + self.brain._TOPIC_COOLDOWN_SECONDS + 1))

    def test_channels_have_independent_cooldowns(self):
        self.brain._cooldown_topic("SHOUT", "windwalks")
        self.assertIn("windwalks", self.brain._active_topic_cooldowns("SHOUT"))
        self.assertNotIn("windwalks", self.brain._active_topic_cooldowns("TRADE"))

    def test_public_context_drops_blocked_topic_lines(self):
        log = self._log("A: windwalks rock", "B: wts dagger c grade", "C: love windwalks")
        context = self.brain.public_context(log, {"windwalks"})
        self.assertNotIn("windwalks", context.lower())
        self.assertIn("dagger", context.lower())

    def test_public_line_problem_flags_blocked_topic_for_bots_not_humans(self):
        self.brain._cooldown_topic("SHOUT", "windwalks")
        log = self._log()
        self.assertEqual(
            "topic-cooldown", self.brain.public_line_problem("SHOUT", "windwalks are still great", log))
        # A real player may always bring it back and gets a normal answer.
        self.assertEqual("", self.brain.public_line_problem("SHOUT", "windwalks are still great", log, human=True))

    def test_invalid_town_party_call_matches_town_targets_only(self):
        self.assertTrue(self.brain.invalid_town_party_call("lfm 2 dd giran"))
        self.assertTrue(self.brain.invalid_town_party_call("anyone wanna party giran?"))
        self.assertFalse(self.brain.invalid_town_party_call("lfm 2 dd cruma pst"))
        self.assertFalse(self.brain.invalid_town_party_call("wts dagger in giran"))

    def test_low_value_public_line_allows_questions_and_market_calls(self):
        self.assertTrue(self.brain.is_low_value_public_line("true"))
        self.assertTrue(self.brain.is_low_value_public_line("lol same"))
        self.assertFalse(self.brain.is_low_value_public_line("cruma?"))
        self.assertFalse(self.brain.is_low_value_public_line("wts dagger"))
        self.assertFalse(self.brain.is_low_value_public_line("anyone selling soulshots cheap"))


if __name__ == "__main__":
    unittest.main()
