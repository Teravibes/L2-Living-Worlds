import org.l2jmobius.gameserver.managers.FakePlayerStoreEligibility;

/** Standalone regression harness for the curated FakePlayer market allow-list. */
public class FakePlayerStoreEligibilityTest
{
	private static int checks;
	private static int failures;

	public static void main(String[] args)
	{
		eq(2440, FakePlayerStoreEligibility.size(), "curated pool size");

		allowed(126, "Artisan's Sword (D-grade base weapon, added by request)");
		allowed(1463, "D-grade Soulshot");
		allowed(3948, "D-grade Blessed Spiritshot");
		allowed(5575, "Ancient Adena");
		allowed(6569, "Blessed Enchant Weapon D");
		allowed(6578, "Blessed Enchant Armor S");

		blocked(1, "ordinary NPC retail weapon");
		blocked(57, "base Adena");
		blocked(1835, "NPC retail No-grade Soulshot");
		blocked(3947, "NPC retail No-grade Blessed Spiritshot");
		blocked(6645, "specialist Beast Soulshot");

		if (failures > 0)
		{
			throw new AssertionError(failures + " of " + checks + " eligibility checks failed");
		}
		System.out.println("PASS: " + checks + " FakePlayer store eligibility checks");
	}

	private static void allowed(int itemId, String what)
	{
		eq(true, FakePlayerStoreEligibility.isAllowed(itemId), what + " is included");
	}

	private static void blocked(int itemId, String what)
	{
		eq(false, FakePlayerStoreEligibility.isAllowed(itemId), what + " is excluded");
	}

	private static void eq(Object expected, Object actual, String what)
	{
		checks++;
		if (!expected.equals(actual))
		{
			failures++;
			System.out.println("FAIL: " + what + " - expected [" + expected + "] but got [" + actual + "]");
		}
	}
}
