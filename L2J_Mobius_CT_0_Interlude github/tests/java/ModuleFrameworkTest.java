/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.modules.Json;
import org.l2jmobius.gameserver.modules.ModuleDatabaseSpec;
import org.l2jmobius.gameserver.modules.ModuleException;
import org.l2jmobius.gameserver.modules.ModuleIdRange;
import org.l2jmobius.gameserver.modules.ModuleManifest;
import org.l2jmobius.gameserver.modules.ModuleManifestReader;
import org.l2jmobius.gameserver.modules.ModuleResourceType;
import org.l2jmobius.gameserver.modules.ModuleValidator;
import org.l2jmobius.gameserver.modules.ModuleValidator.Candidate;

/**
 * Standalone (no JUnit, no game server) regression harness for the module framework's pure pieces: the dependency-free
 * {@link Json} reader and the {@link ModuleManifestReader} validation rules. These are the parts a malformed or hostile
 * manifest hits first, so they must accept a good manifest and refuse a bad one with a clear failure rather than
 * enabling a half-defined module.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/gameserver/modules/Json.java" \
 *         "java/org/l2jmobius/gameserver/modules/ModuleException.java" \
 *         "java/org/l2jmobius/gameserver/modules/ModuleManifest.java" \
 *         "java/org/l2jmobius/gameserver/modules/ModuleManifestReader.java" \
 *         "tests/java/ModuleFrameworkTest.java"
 *   java -cp build/test-classes ModuleFrameworkTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class ModuleFrameworkTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args) throws Exception
	{
		testJsonParsesTypes();
		testJsonRejectsMalformed();
		testManifestAcceptsValid();
		testManifestRejectsBadInput();
		testManifestParsesResources();
		testManifestRejectsBadResources();
		testManifestParsesGovernance();
		testManifestRejectsBadGovernance();
		testIdRange();
		testValidatorReservationOverlap();
		testValidatorConflicts();
		testValidatorDependencies();
		testValidatorCycles();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
	}

	private static void testJsonParsesTypes() throws Exception
	{
		final Object parsed = Json.parse("{ \"s\": \"a\\nb\", \"i\": 100, \"d\": 1.5, \"b\": true, \"n\": null, \"a\": [1, 2, 3] }");
		check("object is a Map", parsed instanceof Map);

		@SuppressWarnings("unchecked")
		final Map<String, Object> root = (Map<String, Object>) parsed;
		check("string with escape", "a\nb".equals(root.get("s")));
		check("integral number is Long", Long.valueOf(100L).equals(root.get("i")));
		check("fractional number is Double", Double.valueOf(1.5).equals(root.get("d")));
		check("boolean", Boolean.TRUE.equals(root.get("b")));
		check("null value", root.get("n") == null);
		check("array is a List", root.get("a") instanceof List);
		check("array length", ((List<?>) root.get("a")).size() == 3);
	}

	private static void testJsonRejectsMalformed()
	{
		check("trailing comma rejected", jsonFails("{ \"a\": 1, }"));
		check("missing colon rejected", jsonFails("{ \"a\" 1 }"));
		check("unterminated string rejected", jsonFails("{ \"a\": \"x }"));
		check("bare word rejected", jsonFails("nope"));
		check("trailing content rejected", jsonFails("{} extra"));
	}

	private static void testManifestAcceptsValid() throws Exception
	{
		final Path file = writeManifest("{ \"id\": \"hello-world\", \"name\": \"Hello World\", \"version\": \"1.0.0\", \"apiVersion\": \"1\", \"entrypoint\": \"modules.helloworld.HelloWorldModule\", \"priority\": 50 }");
		final ModuleManifest manifest = new ModuleManifestReader().read(file);
		check("id parsed", "hello-world".equals(manifest.getId()));
		check("entrypoint parsed", "modules.helloworld.HelloWorldModule".equals(manifest.getEntrypoint()));
		check("priority parsed", manifest.getPriority() == 50);

		final Path noPriority = writeManifest("{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"modules.a.A\" }");
		check("priority defaults to 100", new ModuleManifestReader().read(noPriority).getPriority() == 100);
	}

	private static void testManifestRejectsBadInput() throws Exception
	{
		check("missing id rejected", manifestFails("{ \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\" }"));
		check("uppercase id rejected", manifestFails("{ \"id\": \"Hello\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\" }"));
		check("spaced id rejected", manifestFails("{ \"id\": \"hello world\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\" }"));
		check("unsupported apiVersion rejected", manifestFails("{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"2\", \"entrypoint\": \"x\" }"));
		check("missing entrypoint rejected", manifestFails("{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\" }"));
		check("array root rejected", manifestFails("[ 1, 2, 3 ]"));
	}

	private static void testManifestParsesResources() throws Exception
	{
		final String base = "{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\"";

		final ModuleManifest withItems = new ModuleManifestReader().read(writeManifest(base + ", \"resources\": { \"items\": [\"data/items\", \"data/more\"] } }"));
		check("items resource paths parsed", withItems.getResourcePaths(ModuleResourceType.ITEMS).equals(List.of("data/items", "data/more")));
		check("undeclared type is empty", withItems.getResourcePaths(ModuleResourceType.SKILLS).isEmpty());

		final ModuleManifest none = new ModuleManifestReader().read(writeManifest(base + " }"));
		check("absent resources block is empty", none.getResourcePaths(ModuleResourceType.ITEMS).isEmpty());

		// An unknown resource type is warned about and skipped, not a hard failure.
		final ModuleManifest unknownType = new ModuleManifestReader().read(writeManifest(base + ", \"resources\": { \"widgets\": [\"data/widgets\"], \"items\": [\"data/items\"] } }"));
		check("unknown resource type skipped, known kept", unknownType.getResourcePaths(ModuleResourceType.ITEMS).equals(List.of("data/items")));

		final ModuleManifest spawns = new ModuleManifestReader().read(writeManifest(base + ", \"resources\": { \"spawns\": [\"data/spawns\"] } }"));
		check("spawns resource paths parsed", spawns.getResourcePaths(ModuleResourceType.SPAWNS).equals(List.of("data/spawns")));
		check("spawns is a known resource type", ModuleResourceType.fromKey("spawns") != null);
	}

	private static void testManifestRejectsBadResources() throws Exception
	{
		final String base = "{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\"";
		check("absolute resource path rejected", manifestFails(base + ", \"resources\": { \"items\": [\"/etc/passwd\"] } }"));
		check("windows drive resource path rejected", manifestFails(base + ", \"resources\": { \"items\": [\"C:/data\"] } }"));
		check("parent-traversal resource path rejected", manifestFails(base + ", \"resources\": { \"items\": [\"../../secret\"] } }"));
		check("non-array resource value rejected", manifestFails(base + ", \"resources\": { \"items\": \"data/items\" } }"));
		check("non-string resource element rejected", manifestFails(base + ", \"resources\": { \"items\": [123] } }"));
		check("non-object resources block rejected", manifestFails(base + ", \"resources\": [\"data/items\"] }"));
	}

	private static void testManifestParsesGovernance() throws Exception
	{
		final String base = "{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\"";
		final ModuleManifest m = new ModuleManifestReader().read(writeManifest(base + ", \"reserves\": { \"items\": [[60000, 60009]], \"skills\": [[100, 100]] }, \"dependencies\": [\"core-lib\"], \"conflicts\": [\"old-mod\"] }"));

		check("reserve items range parsed", m.getReserves(ModuleResourceType.ITEMS).equals(List.of(new ModuleIdRange(60000, 60009))));
		check("reserve single-id range parsed", m.getReserves(ModuleResourceType.SKILLS).get(0).contains(100));
		check("reserve npcs empty when absent", m.getReserves(ModuleResourceType.NPCS).isEmpty());
		check("dependencies parsed", m.getDependencies().equals(List.of("core-lib")));
		check("conflicts parsed", m.getConflicts().equals(List.of("old-mod")));

		final ModuleManifest none = new ModuleManifestReader().read(writeManifest(base + " }"));
		check("governance defaults empty", none.getReserves(ModuleResourceType.ITEMS).isEmpty() && none.getDependencies().isEmpty() && none.getConflicts().isEmpty());

		final ModuleManifest db = new ModuleManifestReader().read(writeManifest(base + ", \"database\": { \"install\": \"sql/install.sql\", \"remove\": \"sql/remove.sql\", \"tables\": [\"tamed_pet\"] } }"));
		final ModuleDatabaseSpec spec = db.getDatabase();
		check("database install parsed", spec.install().equals("sql/install.sql") && spec.hasInstall());
		check("database remove parsed", spec.remove().equals("sql/remove.sql") && spec.hasRemove());
		check("database tables parsed", spec.tables().equals(List.of("tamed_pet")));
		check("absent database is empty", !none.getDatabase().hasInstall() && !none.getDatabase().hasRemove());
	}

	private static void testManifestRejectsBadGovernance() throws Exception
	{
		final String base = "{ \"id\": \"a\", \"name\": \"A\", \"version\": \"1\", \"apiVersion\": \"1\", \"entrypoint\": \"x\"";
		check("reserve on html rejected (no id space)", manifestFails(base + ", \"reserves\": { \"html\": [[1, 2]] } }"));
		check("reserve on spawns rejected (no id space)", manifestFails(base + ", \"reserves\": { \"spawns\": [[1, 2]] } }"));
		check("reserve high below low rejected", manifestFails(base + ", \"reserves\": { \"items\": [[10, 5]] } }"));
		check("reserve non-pair rejected", manifestFails(base + ", \"reserves\": { \"items\": [[10]] } }"));
		check("reserve negative id rejected", manifestFails(base + ", \"reserves\": { \"items\": [[-1, 5]] } }"));
		check("reserve non-integer id rejected", manifestFails(base + ", \"reserves\": { \"items\": [[1.5, 5]] } }"));
		check("reserve non-array value rejected", manifestFails(base + ", \"reserves\": { \"items\": 60000 } }"));
		check("dependencies non-array rejected", manifestFails(base + ", \"dependencies\": \"core-lib\" }"));
		check("dependency invalid id rejected", manifestFails(base + ", \"dependencies\": [\"Bad Id\"] }"));
		check("self dependency rejected", manifestFails(base + ", \"dependencies\": [\"a\"] }"));
		check("self conflict rejected", manifestFails(base + ", \"conflicts\": [\"a\"] }"));
		check("database non-object rejected", manifestFails(base + ", \"database\": \"sql/install.sql\" }"));
		check("database tables non-array rejected", manifestFails(base + ", \"database\": { \"tables\": \"tamed_pet\" } }"));
		check("database unsafe install path rejected", manifestFails(base + ", \"database\": { \"install\": \"../../evil.sql\" } }"));
	}

	private static void testIdRange()
	{
		final ModuleIdRange r = new ModuleIdRange(60000, 60009);
		check("range contains its low end", r.contains(60000));
		check("range contains its high end", r.contains(60009));
		check("range excludes just below", !r.contains(59999));
		check("range excludes just above", !r.contains(60010));
		check("overlapping ranges detected", r.overlaps(new ModuleIdRange(60009, 60020)));
		check("adjacent ranges do not overlap", !r.overlaps(new ModuleIdRange(60010, 60020)));
		check("single-id range prints without dash", new ModuleIdRange(5, 5).toString().equals("5"));
	}

	private static Candidate cand(String id, boolean enabled, List<String> deps, List<String> conflicts, Map<ModuleResourceType, List<ModuleIdRange>> reserves)
	{
		return new Candidate(id, enabled, deps, conflicts, reserves);
	}

	private static void testValidatorReservationOverlap()
	{
		final Candidate a = cand("a", true, List.of(), List.of(), Map.of(ModuleResourceType.ITEMS, List.of(new ModuleIdRange(100, 110))));
		final Candidate b = cand("b", true, List.of(), List.of(), Map.of(ModuleResourceType.ITEMS, List.of(new ModuleIdRange(105, 115))));
		final Candidate c = cand("c", true, List.of(), List.of(), Map.of(ModuleResourceType.ITEMS, List.of(new ModuleIdRange(200, 210))));
		final Map<String, String> refused = ModuleValidator.validate(List.of(a, b, c));
		check("overlapping reserves refuse both", refused.containsKey("a") && refused.containsKey("b"));
		check("non-overlapping reserve kept", !refused.containsKey("c"));

		final Candidate d = cand("d", true, List.of(), List.of(), Map.of(ModuleResourceType.ITEMS, List.of(new ModuleIdRange(100, 110))));
		final Candidate e = cand("e", false, List.of(), List.of(), Map.of(ModuleResourceType.ITEMS, List.of(new ModuleIdRange(100, 110))));
		check("disabled module never collides", ModuleValidator.validate(List.of(d, e)).isEmpty());

		final Candidate f = cand("f", true, List.of(), List.of(), Map.of(ModuleResourceType.ITEMS, List.of(new ModuleIdRange(100, 110))));
		final Candidate g = cand("g", true, List.of(), List.of(), Map.of(ModuleResourceType.SKILLS, List.of(new ModuleIdRange(100, 110))));
		check("same range different type does not collide", ModuleValidator.validate(List.of(f, g)).isEmpty());
	}

	private static void testValidatorConflicts()
	{
		final Candidate a = cand("a", true, List.of(), List.of("b"), Map.of());
		final Candidate b = cand("b", true, List.of(), List.of(), Map.of());
		final Map<String, String> refused = ModuleValidator.validate(List.of(a, b));
		check("declaring module refused on conflict", refused.containsKey("a"));
		check("conflicted-with module kept", !refused.containsKey("b"));

		final Candidate c = cand("c", true, List.of(), List.of("d"), Map.of());
		final Candidate d = cand("d", false, List.of(), List.of(), Map.of());
		check("conflict with disabled module is harmless", ModuleValidator.validate(List.of(c, d)).isEmpty());
	}

	private static void testValidatorDependencies()
	{
		final Candidate a = cand("a", true, List.of("missing"), List.of(), Map.of());
		check("missing dependency refused", ModuleValidator.validate(List.of(a)).containsKey("a"));

		final Candidate b = cand("b", true, List.of("c"), List.of(), Map.of());
		final Candidate cDisabled = cand("c", false, List.of(), List.of(), Map.of());
		check("disabled dependency refused", ModuleValidator.validate(List.of(b, cDisabled)).containsKey("b"));

		final Candidate d = cand("d", true, List.of("e"), List.of(), Map.of());
		final Candidate e = cand("e", true, List.of(), List.of(), Map.of());
		check("satisfied dependency kept", ModuleValidator.validate(List.of(d, e)).isEmpty());

		// f -> g -> missing, so refusing g must cascade to f.
		final Candidate f = cand("f", true, List.of("g"), List.of(), Map.of());
		final Candidate g = cand("g", true, List.of("missing"), List.of(), Map.of());
		final Map<String, String> refused = ModuleValidator.validate(List.of(f, g));
		check("dependency failure cascades", refused.containsKey("f") && refused.containsKey("g"));
	}

	private static void testValidatorCycles()
	{
		final Candidate a = cand("a", true, List.of("b"), List.of(), Map.of());
		final Candidate b = cand("b", true, List.of("a"), List.of(), Map.of());
		final Map<String, String> refused = ModuleValidator.validate(List.of(a, b));
		check("dependency cycle refuses both", refused.containsKey("a") && refused.containsKey("b"));

		final Candidate x = cand("x", true, List.of("y"), List.of(), Map.of());
		final Candidate y = cand("y", true, List.of(), List.of(), Map.of());
		check("acyclic chain kept", ModuleValidator.validate(List.of(x, y)).isEmpty());
	}

	private static boolean jsonFails(String text)
	{
		try
		{
			Json.parse(text);
			return false;
		}
		catch (Json.JsonException e)
		{
			return true;
		}
	}

	private static boolean manifestFails(String text) throws Exception
	{
		final Path file = writeManifest(text);
		try
		{
			new ModuleManifestReader().read(file);
			return false;
		}
		catch (ModuleException e)
		{
			return true;
		}
	}

	private static Path writeManifest(String text) throws Exception
	{
		final Path file = Files.createTempFile("module", ".json");
		file.toFile().deleteOnExit();
		Files.write(file, text.getBytes(StandardCharsets.UTF_8));
		return file;
	}

	private static void check(String label, boolean ok)
	{
		checks++;
		if (!ok)
		{
			failures++;
			System.out.println("  FAIL: " + label);
		}
		else
		{
			System.out.println("  ok:   " + label);
		}
	}
}
