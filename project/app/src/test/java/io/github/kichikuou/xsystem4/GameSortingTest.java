package io.github.kichikuou.xsystem4;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

/** Headless Android tests, not emulator/device visual validation. Java matches existing test conventions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameSortingTest {
    private Activity activity;
    @Before public void setup() {
        org.robolectric.shadows.ShadowEnvironment.setExternalStorageState(android.os.Environment.MEDIA_MOUNTED);
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.getSharedPreferences("alicehub_settings", 0).edit().clear().commit();
    }
    private GameSelectEntry entry(String id, String title, boolean installed) {
        GameDescriptor d = GameCatalog.INSTANCE.descriptorFor(activity, id, null);
        GameDescriptor descriptor = new GameDescriptor(d.getId(), title, d.getDefaultDirName(), d.getExpectedAinName(),
            d.getIconRes(), d.getAccentColor(), d.getAccentBorderColor(), d.getIconFrameColor(),
            d.getSupportsOnlineBaseInstall(), d.getOnlineBaseCdnUrl(), d.getSupportsOnlineMod(), d.getSupportsLocalMod(),
            d.isRance10(), d.isCnPatch(), d.getOverlayCdnBaseUrl(), d.getOverlayCatalogRelPath());
        Item item = installed ? new Item(title, new File(activity.getFilesDir(), id), activity.getFilesDir(), null, null, null) : null;
        return new GameSelectEntry(descriptor, item == null ? Collections.emptyList() : Collections.singletonList(item), item);
    }
    private GameSelectEntry entry(String id) { return entry(id, id, false); }
    private List<String> ids(List<GameSelectEntry> entries) {
        return entries.stream().map(e -> e.getDescriptor().getId()).collect(Collectors.toList());
    }
    private List<GameSelectEntry> sort(List<GameSelectEntry> entries, GameSortMode mode) {
        return GameSorting.INSTANCE.sorted(entries, mode, Locale.ENGLISH);
    }
    @Test public void defaultAndUnknownPreferencePreserveCatalogMode() {
        assertEquals(GameSortMode.CATALOG, AppSettings.INSTANCE.getGameSortMode(activity));
        android.content.SharedPreferences prefs = activity.getSharedPreferences("alicehub_settings", 0);
        prefs.edit().putString("game_sort", "future_mode").commit();
        assertEquals(GameSortMode.CATALOG, AppSettings.INSTANCE.getGameSortMode(activity));
        prefs.edit().putInt("game_sort", 123).commit();
        assertEquals(GameSortMode.CATALOG, AppSettings.INSTANCE.getGameSortMode(activity));
    }
    @Test public void everySelectionPersistsUsingStableKeysAcrossContexts() {
        for (GameSortMode mode : GameSortMode.values()) {
            AppSettings.INSTANCE.setGameSortMode(activity, mode);
            Context fresh = activity.createConfigurationContext(new Configuration(activity.getResources().getConfiguration()));
            assertEquals(mode.getKey(), fresh.getSharedPreferences("alicehub_settings", 0).getString("game_sort", null));
            assertEquals(mode, AppSettings.INSTANCE.getGameSortMode(fresh));
            assertEquals(mode, GameSortMode.Companion.fromKey(mode.getKey()));
        }
    }
    @Test public void installedAlwaysPrecedesUninstalledInEveryModeEvenWithoutMetadata() {
        List<GameSelectEntry> entries = Arrays.asList(entry(GameCatalog.ID_SENGOKU_RANCE, "A", false),
            entry("custom:unknown", "Z", true), entry(GameCatalog.ID_DOHNA_DOHNA, "B", false), entry(GameCatalog.ID_ESCALAYER, "Y", true));
        for (GameSortMode mode : GameSortMode.values()) {
            List<GameSelectEntry> sorted = sort(entries, mode);
            assertTrue(mode.toString(), sorted.subList(0, 2).stream().allMatch(e -> e.getPrimaryItem() != null));
            assertTrue(mode.toString(), sorted.subList(2, 4).stream().allMatch(e -> e.getPrimaryItem() == null));
        }
        assertEquals(Arrays.asList("custom:unknown", GameCatalog.ID_ESCALAYER, GameCatalog.ID_SENGOKU_RANCE, GameCatalog.ID_DOHNA_DOHNA),
            ids(sort(entries, GameSortMode.CATALOG)));
    }
    @Test public void nameUsesLocaleAscendingAndKeepsEqualTitlesStable() {
        List<GameSelectEntry> entries = Arrays.asList(entry("custom:b", "beta", false), entry("custom:a1", "Alpha", false), entry("custom:a2", "alpha", false));
        assertEquals(Arrays.asList("custom:a1", "custom:a2", "custom:b"), ids(sort(entries, GameSortMode.NAME)));
        List<GameSelectEntry> localized = Arrays.asList(entry("custom:z", "z", false), entry("custom:a", "ä", false));
        assertEquals("custom:a", GameSorting.INSTANCE.sorted(localized, GameSortMode.NAME, Locale.GERMAN).get(0).getDescriptor().getId());
        assertEquals("custom:z", GameSorting.INSTANCE.sorted(localized, GameSortMode.NAME, new Locale("sv")).get(0).getDescriptor().getId());
    }
    private Map<String, GameSortInfo> scoreFixture() {
        // Synthetic test-only rating scores. These are never written to the production snapshot.
        Map<String, GameSortInfo> fixture = new HashMap<>();
        fixture.put(GameCatalog.ID_DOHNA_DOHNA, new GameSortInfo("2020-11-27", 70.03, null, "test-only"));
        fixture.put(GameCatalog.ID_SENGOKU_RANCE, new GameSortInfo("2006-12-15", 70.02, null, "test-only"));
        fixture.put(GameCatalog.ID_EVENICLE, new GameSortInfo("2015-04-24", 70.04, null, "test-only"));
        fixture.put(GameCatalog.ID_ESCALAYER, new GameSortInfo("2002-08-02", 85.0, null, "test-only"));
        fixture.put(GameCatalog.ID_ESCALAYER_REBOOT, new GameSortInfo("2014-07-25", 85.0, null, "test-only"));
        return fixture;
    }
    @Test public void ratingIsDescendingRatingNotVotesReleaseDateOrName() {
        List<GameSelectEntry> entries = Arrays.asList(entry(GameCatalog.ID_DOHNA_DOHNA, "A", false), entry(GameCatalog.ID_SENGOKU_RANCE, "Z", false), entry(GameCatalog.ID_EVENICLE, "B", false));
        Map<String, GameSortInfo> fixture = scoreFixture();
        // Distinct fractional scores must not be rounded to an integer before sorting.
        // This also differs from the previous VNDB vote-count order and from release/name order.
        assertEquals(Arrays.asList(GameCatalog.ID_EVENICLE, GameCatalog.ID_DOHNA_DOHNA, GameCatalog.ID_SENGOKU_RANCE),
            ids(GameSorting.INSTANCE.sorted(entries, GameSortMode.RATING, Locale.ENGLISH, fixture::get)));
    }
    @Test public void legacyPopularityPreferenceMigratesToRatingWithoutResettingOtherModes() {
        android.content.SharedPreferences prefs = activity.getSharedPreferences("alicehub_settings", 0);
        prefs.edit().putString("game_sort", "popularity").commit();
        assertEquals(GameSortMode.RATING, GameSortMode.Companion.fromKey("popularity"));
        assertEquals(GameSortMode.RATING, AppSettings.INSTANCE.getGameSortMode(activity));
        assertEquals("rating", prefs.getString("game_sort", null));
        assertEquals(GameSortMode.RATING, AppSettings.INSTANCE.getGameSortMode(activity));
        prefs.edit().putString("game_sort", "release_date").commit();
        assertEquals(GameSortMode.RELEASE_DATE, AppSettings.INSTANCE.getGameSortMode(activity));
    }
    @Test public void releaseNewestFirstUsesListedEditionNotOriginalOrPort() {
        List<GameSelectEntry> entries = Arrays.asList(entry(GameCatalog.ID_ESCALAYER), entry(GameCatalog.ID_RANCE08), entry(GameCatalog.ID_ESCALAYER_REBOOT), entry(GameCatalog.ID_DOHNA_DOHNA));
        assertEquals(Arrays.asList(GameCatalog.ID_DOHNA_DOHNA, GameCatalog.ID_ESCALAYER_REBOOT, GameCatalog.ID_RANCE08, GameCatalog.ID_ESCALAYER), ids(sort(entries, GameSortMode.RELEASE_DATE)));
        assertEquals("2012-02-24", GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_RANCE08).getReleaseDate());
        assertEquals("2009-12-18", GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_RANCE02).getReleaseDate());
        assertEquals("2014-07-25", GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_ESCALAYER_REBOOT).getReleaseDate());
    }
    @Test public void equalAndMissingRatingsRemainStableAndDoNotMutateInput() {
        List<GameSelectEntry> entries = Arrays.asList(entry("custom:first"), entry(GameCatalog.ID_ESCALAYER_REBOOT), entry(GameCatalog.ID_RANCE02), entry(GameCatalog.ID_ESCALAYER), entry("custom:last"));
        List<GameSelectEntry> before = new ArrayList<>(entries);
        assertEquals(Arrays.asList(GameCatalog.ID_ESCALAYER_REBOOT, GameCatalog.ID_ESCALAYER, "custom:first", GameCatalog.ID_RANCE02, "custom:last"), ids(GameSorting.INSTANCE.sorted(entries, GameSortMode.RATING, Locale.ENGLISH, scoreFixture()::get)));
        assertEquals(before, entries);
        assertNull(GameSortMetadata.INSTANCE.forGame("custom:first"));
    }
    @Test public void equalAndMissingKeysCompareSafelyIncludingZeroVotes() throws Exception {
        // Internal generic comparator is also exercised directly for keys absent from the fixed snapshot.
        java.lang.reflect.Method comparator = Arrays.stream(GameSorting.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("descendingKnownFirst")).findFirst().get();
        comparator.setAccessible(true);
        assertEquals(0, comparator.invoke(GameSorting.INSTANCE, "2012-02-24", "2012-02-24"));
        assertEquals(0, comparator.invoke(GameSorting.INSTANCE, null, null));
        assertTrue((Integer) comparator.invoke(GameSorting.INSTANCE, 0, null) < 0);
        assertTrue((Integer) comparator.invoke(GameSorting.INSTANCE, null, 0) > 0);
        assertTrue((Integer) comparator.invoke(GameSorting.INSTANCE, Integer.MAX_VALUE, 0) < 0);
        List<GameSelectEntry> missing = Arrays.asList(entry("custom:two"), entry(GameCatalog.ID_EVENICLE), entry("custom:one"));
        assertEquals(Arrays.asList(GameCatalog.ID_EVENICLE, "custom:two", "custom:one"), ids(sort(missing, GameSortMode.RELEASE_DATE)));
    }
    @Test public void installedGroupsApplyEachSelectedSortIndependently() {
        for (GameSortMode mode : Arrays.asList(GameSortMode.NAME, GameSortMode.RATING, GameSortMode.RELEASE_DATE)) {
            List<GameSelectEntry> uninstalled = Arrays.asList(entry(GameCatalog.ID_DOHNA_DOHNA, "Z", false), entry(GameCatalog.ID_SENGOKU_RANCE, "A", false), entry("custom:u", "M", false));
            List<GameSelectEntry> installed = Arrays.asList(entry(GameCatalog.ID_ESCALAYER, "Z", true), entry(GameCatalog.ID_EVENICLE, "A", true), entry("custom:i", "M", true));
            List<String> expected = ids(sort(installed, mode)); expected.addAll(ids(sort(uninstalled, mode)));
            List<GameSelectEntry> both = new ArrayList<>(uninstalled); both.addAll(installed);
            assertEquals(expected, ids(sort(both, mode)));
        }
    }
    @Test public void metadataCoversOnlineBuiltInsWithRealSourcesAndValidDates() {
        for (String id : GameCatalog.INSTANCE.builtInSelectOrder().subList(0, 20)) {
            GameSortInfo data = GameSortMetadata.INSTANCE.forGame(id);
            assertNotNull(data); LocalDate.parse(data.getReleaseDate());
            assertTrue(data.getReleaseSourceUrl().startsWith("https://"));
            if (data.getRating() != null) {
                assertTrue(data.getRating() >= 0 && data.getRating() <= 100);
                assertTrue(data.getScoreSourceUrl().startsWith("https://vndb.org/v"));
            }
        }
    }
    @Test public void ratingsMatchPreservedPublicApiSnapshotNotVoteCounts() throws Exception {
        File file = new File("../docs/game-sort-vndb-snapshot.json");
        if (!file.isFile()) file = new File("../../docs/game-sort-vndb-snapshot.json");
        org.json.JSONObject snapshot = new org.json.JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        assertTrue(snapshot.getString("metric").startsWith("rating (Bayesian rating"));
        org.json.JSONArray results = snapshot.getJSONObject("response").getJSONArray("results");
        Map<String, Double> ratings = new HashMap<>();
        for (int i = 0; i < results.length(); i++) {
            org.json.JSONObject row = results.getJSONObject(i);
            ratings.put("https://vndb.org/" + row.getString("id"), row.isNull("rating") ? null : row.getDouble("rating"));
        }
        for (String id : GameCatalog.INSTANCE.builtInSelectOrder().subList(0, 20)) {
            GameSortInfo data = GameSortMetadata.INSTANCE.forGame(id);
            if (data.getRating() != null) assertEquals(ratings.get(data.getScoreSourceUrl()), data.getRating());
        }
        assertNull(GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_RANCE02).getRating());
        assertNull(GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_RANCE02).getScoreSourceUrl());
        assertEquals("https://vndb.org/v311", GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_ESCALAYER_REBOOT).getScoreSourceUrl());
        List<GameSelectEntry> entries = Arrays.asList(entry(GameCatalog.ID_SENGOKU_RANCE), entry(GameCatalog.ID_RANCE10), entry("custom:unknown"));
        // Actual X rating exceeds Sengoku, although Sengoku has far more votes.
        assertEquals(Arrays.asList(GameCatalog.ID_RANCE10, GameCatalog.ID_SENGOKU_RANCE, "custom:unknown"), ids(sort(entries, GameSortMode.RATING)));
        List<GameSelectEntry> missing = Arrays.asList(entry("custom:b"), entry(GameCatalog.ID_RANCE02), entry("custom:a"));
        assertEquals(ids(missing), ids(sort(missing, GameSortMode.RATING)));
    }
    @Test public void settingsDialogSelectsPersistsAndAnnouncesCurrentMode() {
        org.robolectric.android.controller.ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class).setup();
        SettingsActivity settings = controller.get();
        try {
            View row = settings.findViewById(R.id.row_settings_game_sort);
            row.performClick();
            android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(dialog.isShowing());
            assertEquals(GameSortMode.values().length, dialog.getListView().getCount());
            int[] fullLabels = {R.string.game_sort_catalog, R.string.game_sort_name, R.string.game_sort_rating, R.string.game_sort_release_date};
            for (int i = 0; i < fullLabels.length; i++) {
                assertEquals(settings.getString(fullLabels[i]), dialog.getListView().getAdapter().getItem(i));
            }
            assertNotEquals(View.VISIBLE, dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).getVisibility());
            int rating = GameSortMode.RATING.ordinal();
            dialog.getListView().performItemClick(dialog.getListView().getChildAt(rating), rating, rating);
            assertEquals(GameSortMode.RATING, AppSettings.INSTANCE.getGameSortMode(settings));
            assertFalse(dialog.isShowing());
            String label = settings.getString(R.string.game_sort_rating_short);
            assertEquals(label, ((TextView) settings.findViewById(R.id.value_settings_game_sort)).getText().toString());
            assertEquals(settings.getString(R.string.settings_game_sort) + ", " + label, row.getContentDescription().toString());
        } finally { controller.pause().stop().destroy(); }
    }
    private BaseAdapter adapter(GameList list) throws Exception {
        java.lang.reflect.Constructor<?> ctor = Class.forName("io.github.kichikuou.xsystem4.GameSelectAdapter").getDeclaredConstructor(Activity.class, GameList.class);
        ctor.setAccessible(true); return (BaseAdapter) ctor.newInstance(activity, list);
    }
    private void call(Object target, String name, Object... args) throws Exception {
        Class<?>[] types = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true); method.invoke(target, args);
    }
    private List<GameSelectEntry> entries(BaseAdapter adapter) {
        List<GameSelectEntry> entries = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) entries.add((GameSelectEntry) adapter.getItem(i));
        return entries;
    }
    @Test public void scannerAndAdapterReloadReflectInstallUninstallAndKeepSearch() throws Exception {
        GameList list = new GameList(activity);
        // Use the scanner-selected root, including its internal-storage fallback in headless Android.
        java.lang.reflect.Field roots = GameList.class.getDeclaredField("storageDirs");
        roots.setAccessible(true);
        File dir = new File((File) ((List<?>) roots.get(list)).get(0), "Rance10");
        assertNull(GameCatalog.INSTANCE.buildSelectEntries(activity, list).stream().filter(e -> e.getDescriptor().getId().equals(GameCatalog.ID_RANCE10)).findFirst().get().getPrimaryItem());
        list.close(); dir.mkdirs();
        Files.write(new File(dir, "AliceStart.ini").toPath(), "GameName = \"Rance10\"\n".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, "Rance10.ain").toPath(), new byte[]{0});
        try {
            list = new GameList(activity);
            assertTrue("Scanner must see the fixture installation", list.getSize() > 0);
            AppSettings.INSTANCE.setGameSortMode(activity, GameSortMode.RATING);
            BaseAdapter adapter = adapter(list);
            assertEquals(GameCatalog.ID_RANCE10, ((GameSelectEntry) adapter.getItem(0)).getDescriptor().getId());
            call(adapter, "setFilter", "rance");
            Set<String> first = new HashSet<>(ids(entries(adapter)));
            AppSettings.INSTANCE.setGameSortMode(activity, GameSortMode.RELEASE_DATE);
            call(adapter, "reload");
            List<GameSelectEntry> filtered = entries(adapter);
            assertEquals(first, new HashSet<>(ids(filtered)));
            assertEquals(GameCatalog.ID_RANCE10, filtered.get(0).getDescriptor().getId());
            assertFalse(ids(filtered).contains(GameCatalog.ID_EVENICLE));
            list.close(); new File(dir, "AliceStart.ini").delete(); new File(dir, "Rance10.ain").delete(); dir.delete();
            list = new GameList(activity);
            List<GameSelectEntry> after = GameCatalog.INSTANCE.buildSelectEntries(activity, list);
            assertNull(after.stream().filter(e -> e.getDescriptor().getId().equals(GameCatalog.ID_RANCE10)).findFirst().get().getPrimaryItem());
            assertEquals(GameCatalog.ID_DOHNA_DOHNA, after.get(0).getDescriptor().getId());
        } finally {
            list.close(); new File(dir, "AliceStart.ini").delete(); new File(dir, "Rance10.ain").delete(); dir.delete();
        }
    }
    @Test public void settingsRowIsInlineAccessibleAndEllipsizesShortValuesInAllLanguages() {
        int[] shortLabels = {R.string.game_sort_catalog_short, R.string.game_sort_name_short, R.string.game_sort_rating_short, R.string.game_sort_release_date_short};
        for (String language : Arrays.asList("en", "zh", "ja")) {
            for (float fontScale : new float[]{1f, 2f}) {
                Configuration config = new Configuration(activity.getResources().getConfiguration());
                config.setLocale(new Locale(language)); config.fontScale = fontScale;
                Context ctx = new ContextThemeWrapper(activity.createConfigurationContext(config), R.style.Theme_Launcher);
                View root = LayoutInflater.from(ctx).inflate(R.layout.activity_settings, null);
                android.widget.LinearLayout row = root.findViewById(R.id.row_settings_game_sort);
                assertTrue(row.isFocusable()); assertTrue(row.isClickable());
                assertEquals(android.widget.LinearLayout.HORIZONTAL, row.getOrientation());
                TextView title = (TextView) row.getChildAt(1);
                TextView value = root.findViewById(R.id.value_settings_game_sort);
                assertSame(row, value.getParent());
                assertEquals(android.text.TextUtils.TruncateAt.END, value.getEllipsize());
                assertEquals(1, value.getMaxLines());
                assertEquals(android.view.Gravity.END, value.getGravity() & android.view.Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK);
                for (int width : new int[]{240, 320}) for (int labelId : shortLabels) {
                    String text = ctx.getString(labelId);
                    assertFalse(text.matches(".*[()（）].*"));
                    value.setText(text);
                    root.measure(View.MeasureSpec.makeMeasureSpec((int) (width * ctx.getResources().getDisplayMetrics().density), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                    assertTrue(value.getWidth() > 0);
                    assertTrue(title.getWidth() > 0);
                    assertTrue(value.getLeft() >= title.getRight());
                    assertEquals(1, value.getLineCount());
                    assertTrue(value.getRight() <= row.getWidth() - row.getPaddingRight());
                }
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, root.findViewById(R.id.icon_settings_game_sort).getImportantForAccessibility());
            }
        }
    }
}
