package io.github.kichikuou.xsystem4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HarukaCatalogTest {
    private static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");
    private static final Charset GBK = Charset.forName("GBK");

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private static void writeFile(File file, byte[] bytes) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs());
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }

    @Test
    public void classifiesHarukaFilesAndDisambiguatesFromEscalayerRebootIxsealAndOthers() throws Exception {
        File jpDir = tmp.newFolder("Haruka");
        byte[] sjisIni = ("IniFileVersion\t= 1\r\n"
                + "ViewWidth\t= 800\r\n"
                + "ViewHeight\t= 600\r\n"
                + "GameName\t= \"超昂閃忍ハルカ\"\r\n"
                + "CodeName\t= \"超昂閃忍ハルカ.ain\"\r\n"
                + "SaveFolder\t= \"SaveData\"\r\n"
                + "MsgSkipName = \"既読スキップ\"\r\n").getBytes(SHIFT_JIS);
        writeFile(new File(jpDir, "System40.ini"), sjisIni);
        writeFile(new File(jpDir, "超昂閃忍ハルカ.ain"), new byte[]{0});
        writeFile(new File(jpDir, "HarukaDll.inc"), new byte[]{0});
        writeFile(new File(jpDir, "超昂閃忍ハルカGA.ald"), new byte[]{0});

        System40Ini parsedJp = System40Ini.Companion.parse(sjisIni);
        assertEquals("超昂閃忍ハルカ", parsedJp.getGameName());
        assertEquals("SaveData", parsedJp.getSaveFolder());

        File mojibakeDir = tmp.newFolder("MojibakeHaruka");
        writeFile(new File(mojibakeDir, "System40.ini"), sjisIni);
        writeFile(new File(mojibakeDir, "挻峍慚擡僴儖僇.ain"), new byte[]{0});

        File cnRepackDir = tmp.newFolder("CnHaruka");
        byte[] gbkIni = ("GameName = \"超昂闪忍遥\"\r\n"
                + "CodeName = \"AliceCN.ain\"\r\n"
                + "SaveFolder = \"SaveData\"\r\n").getBytes(GBK);
        writeFile(new File(cnRepackDir, "System40.ini"), gbkIni);
        writeFile(new File(cnRepackDir, "AliceCN.ain"), new byte[]{0});
        writeFile(new File(cnRepackDir, "HarukaDll.inc"), new byte[]{0});

        File enDir = tmp.newFolder("BeatBladesHaruka");
        writeFile(new File(enDir, "System40.ini"), "GameName = \"Beat Blades Haruka\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(enDir, "Haruka.ain"), new byte[]{0});

        File sys3Escalayer = tmp.newFolder("Escalayer");
        writeFile(new File(sys3Escalayer, "DOKI_SA.ALD"), new byte[]{0});
        writeFile(new File(sys3Escalayer, "System39.ini"), "GameName = \"超昂天使エスカレイヤー\"\n".getBytes(StandardCharsets.UTF_8));

        File rebootDir = tmp.newFolder("EscalayerReboot");
        writeFile(new File(rebootDir, "AliceStart.ini"), "GameName = \"超昂天使Escalayer，by axela\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(rebootDir, "EscalayerReboot.ain"), new byte[]{0});

        File ixsealDir = tmp.newFolder("Ixseal");
        writeFile(new File(ixsealDir, "AliceStart.ini"), "GameName = \"超昂神騎エクシール\"\n".getBytes(SHIFT_JIS));
        writeFile(new File(ixsealDir, "Ixseal.ain"), new byte[]{0});

        File sengokuDir = tmp.newFolder("SengokuRance");
        writeFile(new File(sengokuDir, "System40.ini"), "GameName = \"戦国ランス\"\n".getBytes(SHIFT_JIS));
        writeFile(new File(sengokuDir, "AliceCN.ain"), new byte[]{0});
        writeFile(new File(sengokuDir, "戦国ランスGA.ald"), new byte[]{0});

        File fakeHaruka = tmp.newFolder("HarukaFake");
        writeFile(new File(fakeHaruka, "System40.ini"), "GameName = \"Other\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(fakeHaruka, "OtherGame.ain"), new byte[]{0});

        assertFalse(GameCatalog.INSTANCE.isSystem3xGameDir(jpDir));
        assertTrue(GameCatalog.INSTANCE.isHarukaGameDir(jpDir));
        assertTrue(GameCatalog.INSTANCE.isHarukaGameDir(mojibakeDir));
        assertTrue(GameCatalog.INSTANCE.isHarukaGameDir(cnRepackDir));
        assertTrue(GameCatalog.INSTANCE.isHarukaGameDir(enDir));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameDir(sys3Escalayer));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameDir(rebootDir));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameDir(ixsealDir));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameDir(sengokuDir));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameDir(fakeHaruka));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameFiles(Arrays.asList("System40.ini", "System39.ain")));
        assertFalse(GameCatalog.INSTANCE.isHarukaGameFiles(Collections.singletonList("System40.ini")));

        assertEquals(GameCatalog.ID_HARUKA, GameCatalog.INSTANCE.classifyDirectory(jpDir, jpDir.getName(), null, null));
        assertEquals(GameCatalog.ID_HARUKA, GameCatalog.INSTANCE.classifyDirectory(mojibakeDir, mojibakeDir.getName(), null, null));
        assertEquals(GameCatalog.ID_HARUKA, GameCatalog.INSTANCE.classifyDirectory(cnRepackDir, cnRepackDir.getName(), null, null));
        assertEquals(GameCatalog.ID_HARUKA, GameCatalog.INSTANCE.classifyDirectory(enDir, enDir.getName(), null, null));
        assertEquals(GameCatalog.ID_ESCALAYER, GameCatalog.INSTANCE.classifyDirectory(sys3Escalayer, sys3Escalayer.getName(), null, null));
        assertEquals(GameCatalog.ID_ESCALAYER_REBOOT, GameCatalog.INSTANCE.classifyDirectory(rebootDir, rebootDir.getName(), null, null));
        assertEquals(GameCatalog.ID_IXSEAL, GameCatalog.INSTANCE.classifyDirectory(ixsealDir, ixsealDir.getName(), null, null));
        assertEquals(GameCatalog.ID_SENGOKU_RANCE, GameCatalog.INSTANCE.classifyDirectory(sengokuDir, sengokuDir.getName(), null, null));
        assertEquals(GameCatalog.PREFIX_CUSTOM + "HarukaFake", GameCatalog.INSTANCE.classifyDirectory(fakeHaruka, fakeHaruka.getName(), null, null));

        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, null, GameCatalog.ID_HARUKA));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, "超昂閃忍ハルカ.ain", GameCatalog.ID_HARUKA));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(mojibakeDir, null, GameCatalog.ID_HARUKA));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(cnRepackDir, null, GameCatalog.ID_HARUKA));

        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, null, GameCatalog.ID_ESCALAYER));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, null, GameCatalog.ID_ESCALAYER_REBOOT));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, null, GameCatalog.ID_IXSEAL));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(sys3Escalayer, null, GameCatalog.ID_HARUKA));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(rebootDir, null, GameCatalog.ID_HARUKA));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(ixsealDir, null, GameCatalog.ID_HARUKA));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(fakeHaruka, null, GameCatalog.ID_HARUKA));

        GameDescriptor desc = GameCatalog.INSTANCE.descriptorForWithResolver(
                GameCatalog.ID_HARUKA,
                null,
                resId -> resId == R.string.game_haruka_title ? "Beat Blades Haruka" : "other"
        );
        assertEquals(GameCatalog.ID_HARUKA, desc.getId());
        assertEquals("Beat Blades Haruka", desc.getTitle());
        assertEquals("Haruka", desc.getDefaultDirName());
        assertNull(desc.getExpectedAinName());
        assertEquals(R.drawable.ic_game_haruka, desc.getIconRes());
        assertTrue(desc.getSupportsOnlineBaseInstall());
        assertEquals(OnlineInstaller.HARUKA_CDN_URL, desc.getOnlineBaseCdnUrl());
        assertFalse(desc.getSupportsOnlineMod());
        assertFalse(desc.getSupportsLocalMod());
        assertFalse(desc.isRance10());
    }

    @Test
    public void classifiesAliasesSaveDirsCatalogOrderAndSortingMetadata() throws Exception {
        String[] harukaAliases = {
                "Haruka", "Haruka-1", "Beat Blades Haruka", "Choukou Sennin Haruka",
                "超昂閃忍ハルカ", "超昂闪忍遥", "閃忍ハルカ", "闪忍遥", "ハルカ", "挻峍慚擡僴儖僇"
        };
        for (String alias : harukaAliases) {
            assertTrue(alias, GameCatalog.INSTANCE.isHarukaAlias(alias));
            assertFalse(alias, GameCatalog.INSTANCE.isIxsealAlias(alias));
            assertFalse(alias, GameCatalog.INSTANCE.isEscalayerRebootAlias(alias.toLowerCase(Locale.ROOT)));
            assertEquals(alias, GameCatalog.ID_HARUKA,
                    GameCatalog.INSTANCE.classifyDirectory(null, alias, null, null));
            assertEquals(alias, GameCatalog.ID_HARUKA,
                    GameCatalog.INSTANCE.classifyDirectory(null, "custom", null, alias));
        }

        String[] nonHarukaAliases = {
                "Escalayer", "Escalayer-1", "Beat Angel Escalayer",
                "EscalayerReboot", "Beat Angel Escalayer Reboot",
                "超昂天使エスカレイヤー", "超昂天使エスカレイヤー・リブート",
                "Ixseal", "Beat Valkyrie Ixseal", "超昂神騎エクシール", "超昂神骑"
        };
        for (String alias : nonHarukaAliases) {
            assertFalse(alias, GameCatalog.INSTANCE.isHarukaAlias(alias));
            String classified = GameCatalog.INSTANCE.classifyDirectory(null, alias, null, null);
            assertFalse(alias, GameCatalog.ID_HARUKA.equals(classified));
        }

        File harukaSaveDir = tmp.newFolder("HarukaSaves");
        writeFile(new File(harukaSaveDir, "システム共通データ.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "共有.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "ＣＧモード.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "回想モード.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "QC.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "MsgSkip.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "01.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaSaveDir, null)
        );

        // Live boot + prologue save dir without システム共通データ.asd yet: must classify as ID_HARUKA (never ID_RANCE02).
        File harukaBootSaveDir = tmp.newFolder("HarukaBootSaves");
        writeFile(new File(harukaBootSaveDir, "超昂閃忍ハルカSF.asd"), new byte[]{1});
        writeFile(new File(harukaBootSaveDir, "ＣＧモード.asd"), new byte[]{1});
        writeFile(new File(harukaBootSaveDir, "MsgSkip.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaBootSaveDir, null)
        );
        assertTrue(GameModifierBridge.INSTANCE.isHarukaGameDir(null, "UnknownDir", harukaBootSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance02GameDir(null, "UnknownDir", harukaBootSaveDir));

        File harukaSysSaveDir = tmp.newFolder("HarukaSysSaves");
        writeFile(new File(harukaSysSaveDir, "超昂閃忍ハルカSYS.asd"), new byte[]{1});
        writeFile(new File(harukaSysSaveDir, "ＣＧモード.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaSysSaveDir, null)
        );

        File harukaFnSaveDir = tmp.newFolder("HarukaFnSaves");
        writeFile(new File(harukaFnSaveDir, "超昂閃忍ハルカFN.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaFnSaveDir, null)
        );

        File harukaMojibakeBootSaveDir = tmp.newFolder("HarukaMojibakeBootSaves");
        writeFile(new File(harukaMojibakeBootSaveDir, "挻峍慚擡僴儖僇SF.asd"), new byte[]{1});
        writeFile(new File(harukaMojibakeBootSaveDir, "俠俧儌乕僪.asd"), new byte[]{1});
        writeFile(new File(harukaMojibakeBootSaveDir, "MsgSkip.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaMojibakeBootSaveDir, null)
        );

        File harukaCnSaveDir = tmp.newFolder("HarukaCnSaves");
        writeFile(new File(harukaCnSaveDir, "超昂闪忍遥SF.asd"), new byte[]{1});
        writeFile(new File(harukaCnSaveDir, "ＣＧモード.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaCnSaveDir, null)
        );

        File harukaEnSaveDir = tmp.newFolder("HarukaEnSaves");
        writeFile(new File(harukaEnSaveDir, "HarukaSYS.asd"), new byte[]{1});
        writeFile(new File(harukaEnSaveDir, "ＣＧモード.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", harukaEnSaveDir, null)
        );

        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("超昂閃忍ハルカSF.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("超昂閃忍ハルカSYS.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("超昂閃忍ハルカFN.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("挻峍慚擡僴儖僇SF.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("超昂闪忍遥SF.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("HarukaSYS.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("Beat Blades HarukaSF.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("システム共通データ.asd"));
        assertTrue(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("僔僗僥儉嫟捠僨乕僞.asd"));
        assertFalse(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("01.asd"));
        assertFalse(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("QC.asd"));
        assertFalse(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("ＣＧモード.asd"));
        assertFalse(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("妻しぼりSYS.asd"));
        assertFalse(GameCatalog.INSTANCE.isHarukaSystemSaveFileName("Ixseal_Extra.sav"));

        File mojibakeHarukaSaveDir = tmp.newFolder("HarukaMojibakeSaves");
        writeFile(new File(mojibakeHarukaSaveDir, "僔僗僥儉嫟捠僨乕僞.asd"), new byte[]{1});
        writeFile(new File(mojibakeHarukaSaveDir, "嫟桳.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_HARUKA,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", mojibakeHarukaSaveDir, null)
        );

        File rance08SaveDir = tmp.newFolder("Rance08Saves");
        writeFile(new File(rance08SaveDir, "共有.asd"), new byte[]{1});
        writeFile(new File(rance08SaveDir, "ＣＧモード.asd"), new byte[]{1});
        writeFile(new File(rance08SaveDir, "3DSetting.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_RANCE08,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", rance08SaveDir, null)
        );

        File rance02SaveDir = tmp.newFolder("Rance02Saves");
        writeFile(new File(rance02SaveDir, "ＣＧモード.asd"), new byte[]{1});
        writeFile(new File(rance02SaveDir, "回想モード.asd"), new byte[]{1});
        writeFile(new File(rance02SaveDir, "QC.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_RANCE02,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", rance02SaveDir, null)
        );

        List<String> order = GameCatalog.builtInSelectOrder();
        assertEquals(20 + LocalGamePresets.INSTANCE.getAll().size(), order.size());
        int ixIdx = order.indexOf(GameCatalog.ID_IXSEAL);
        int harukaIdx = order.indexOf(GameCatalog.ID_HARUKA);
        assertTrue(ixIdx >= 0);
        assertTrue(harukaIdx > ixIdx);
        for (String id : order.subList(0, 20)) {
            assertNotNull("Missing GameSortMetadata for " + id, GameSortMetadata.INSTANCE.forGame(id));
        }

        GameSortInfo info = GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_HARUKA);
        assertNotNull(info);
        assertEquals("2008-02-29", info.getReleaseDate());
        assertEquals(67.9, info.getRating(), 0.001);
        assertEquals("https://vndb.org/v284", info.getScoreSourceUrl());
        assertEquals("https://www.alicesoft.com/haruka/", info.getReleaseSourceUrl());

        File snapFile = new File("../docs/game-sort-vndb-snapshot.json");
        if (!snapFile.isFile()) {
            snapFile = new File("../../docs/game-sort-vndb-snapshot.json");
        }
        assertTrue("Expected docs/game-sort-vndb-snapshot.json", snapFile.isFile());
        String snapJson = new String(Files.readAllBytes(snapFile.toPath()), StandardCharsets.UTF_8);
        int resultsStart = snapJson.indexOf("\"results\"");
        int resultsEnd = snapJson.indexOf("\"edition_mapping_releases\"");
        assertTrue(resultsStart > 0 && resultsEnd > resultsStart);
        String resultsSection = snapJson.substring(resultsStart, resultsEnd);

        Pattern idPat = Pattern.compile("\"id\"\\s*:\\s*\"(v\\d+)\"");
        Pattern ratingPat = Pattern.compile("\"rating\"\\s*:\\s*(null|[0-9]+(?:\\.[0-9]+)?)");
        Map<String, Double> vndbRatings = new HashMap<>();
        for (String block : resultsSection.split("\\{")) {
            Matcher idM = idPat.matcher(block);
            Matcher ratM = ratingPat.matcher(block);
            if (idM.find() && ratM.find()) {
                String rawVal = ratM.group(1);
                vndbRatings.put("https://vndb.org/" + idM.group(1), "null".equals(rawVal) ? null : Double.valueOf(rawVal));
            }
        }
        assertEquals(Double.valueOf(67.9), vndbRatings.get("https://vndb.org/v284"));
        for (String id : order.subList(0, 20)) {
            GameSortInfo meta = GameSortMetadata.INSTANCE.forGame(id);
            assertNotNull(meta);
            LocalDate.parse(meta.getReleaseDate());
            assertTrue(meta.getReleaseSourceUrl().startsWith("https://"));
            if (meta.getRating() != null) {
                assertTrue(meta.getScoreSourceUrl().startsWith("https://vndb.org/v"));
                assertEquals(vndbRatings.get(meta.getScoreSourceUrl()), meta.getRating());
            }
        }

        GameSelectEntry escalayerEntry = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_ESCALAYER, "Beat Angel Escalayer", "Escalayer",
                        "DOKI_SA.ALD", 0, 0, 0, 0, true, OnlineInstaller.ESCALAYER_CDN_URL,
                        false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(),
                null
        );
        GameSelectEntry harukaEntry = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_HARUKA, "Beat Blades Haruka", "Haruka",
                        null, 0, 0, 0, 0, true, OnlineInstaller.HARUKA_CDN_URL,
                        false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(),
                null
        );
        GameSelectEntry ixsealEntry = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_IXSEAL, "Beat Valkyrie Ixseal", "Ixseal",
                        "Ixseal.ain", 0, 0, 0, 0, true, OnlineInstaller.IXSEAL_CDN_URL,
                        false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(),
                null
        );
        List<GameSelectEntry> byDate = GameSorting.INSTANCE.sorted(
                Arrays.asList(escalayerEntry, harukaEntry, ixsealEntry),
                GameSortMode.RELEASE_DATE,
                Locale.ENGLISH
        );
        assertEquals(GameCatalog.ID_IXSEAL, byDate.get(0).getDescriptor().getId());
        assertEquals(GameCatalog.ID_HARUKA, byDate.get(1).getDescriptor().getId());
        assertEquals(GameCatalog.ID_ESCALAYER, byDate.get(2).getDescriptor().getId());

        List<GameSelectEntry> byRating = GameSorting.INSTANCE.sorted(
                Arrays.asList(harukaEntry, escalayerEntry, ixsealEntry),
                GameSortMode.RATING,
                Locale.ENGLISH
        );
        assertEquals(GameCatalog.ID_IXSEAL, byRating.get(0).getDescriptor().getId());
        assertEquals(GameCatalog.ID_ESCALAYER, byRating.get(1).getDescriptor().getId());
        assertEquals(GameCatalog.ID_HARUKA, byRating.get(2).getDescriptor().getId());
    }

    @Test
    public void modifierBridgeDisambiguatesHarukaFromEscalayerRebootIxsealRance08AndOthers() throws Exception {
        File harukaDir = tmp.newFolder("HarukaDir");
        writeFile(new File(harukaDir, "System40.ini"), "GameName = \"超昂閃忍ハルカ\"\n".getBytes(SHIFT_JIS));
        writeFile(new File(harukaDir, "超昂閃忍ハルカ.ain"), new byte[]{0});

        File harukaSaveDir = tmp.newFolder("Haruka");
        writeFile(new File(harukaSaveDir, "システム共通データ.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "共有.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "ＣＧモード.asd"), new byte[]{1});
        writeFile(new File(harukaSaveDir, "回想モード.asd"), new byte[]{1});

        assertTrue(GameModifierBridge.INSTANCE.isHarukaGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerRebootGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isIxsealGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isGalzooGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicleGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicle2GameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance10GameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isDohnaDohnaGameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance03GameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance08GameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance09GameDir(harukaDir, null, harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isSengokuRanceGameDir(harukaDir, null, harukaSaveDir));

        assertTrue(GameModifierBridge.INSTANCE.isHarukaGameDir(null, "Haruka", harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance08GameDir(null, "Haruka", harukaSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(null, "超昂閃忍ハルカ", null));
        assertFalse(GameModifierBridge.INSTANCE.isIxsealGameDir(null, "超昂閃忍ハルカ", null));
    }

    @Test
    public void prunesHarukaBloatAndPreservesAll21RuntimeFiles() throws Exception {
        String[] bloatPaths = {
                "als_inst.exe",
                "Eraser.exe",
                "Sys40Reg.exe",
                "Setup.bmp",
                "広告/大番長.BMP",
                "広告/戦国ランス.bmp",
                "峀崘/大番長.BMP",
                "超昂閃忍ハルカDM.dat",
                "超昂閃忍ハルカDM.DAT",
                "挻峍慚擡僴儖僇DM.dat",
                "System40.exe",
                "ijl15.dll",
                "超昂閃忍ハルカ.ico",
                "DLL/SACTDX.dll",
                "DLL/Confirm3.dll",
                "SaveData/01.asd"
        };
        for (String rel : bloatPaths) {
            assertTrue(rel, GameListKt.isHarukaBloatRelPath(rel));
        }

        String[] runtimePaths = {
                "System40.ini",
                "超昂閃忍ハルカ.ain",
                "超昂閃忍ハルカBA.ald",
                "超昂閃忍ハルカBA.bgi",
                "超昂閃忍ハルカGA.ald",
                "超昂閃忍ハルカGB.ald",
                "超昂閃忍ハルカWA.ald",
                "超昂閃忍ハルカWA.wai",
                "AliceLogo3.alk",
                "AliceLogoWave.alk",
                "HarukaDll.inc",
                "Version.txt",
                "Data/CGMode.acx",
                "Data/Recollection.acx",
                "movie/Opening.alm",
                "movie/StaffRoll.alm",
                "movie/Haruka01.alm",
                "movie/Haruka02.alm",
                "movie/Narika01.alm",
                "movie/Narika02.alm",
                "movie/Subaru01.alm"
        };
        assertEquals(21, runtimePaths.length);
        for (String rel : runtimePaths) {
            assertFalse(rel, GameListKt.isHarukaBloatRelPath(rel));
            assertFalse(rel, NativeGameImport.INSTANCE.skip(rel));
        }

        File stagingRoot = tmp.newFolder("staging");
        File containerDir = new File(stagingRoot, "超昂閃忍ハルカ");
        File gameDir = new File(containerDir, "gamedata");
        writeFile(new File(containerDir, "als_inst.exe"), new byte[]{1});
        writeFile(new File(containerDir, "Eraser.exe"), new byte[]{1});
        writeFile(new File(containerDir, "Sys40Reg.exe"), new byte[]{1});
        writeFile(new File(containerDir, "Setup.bmp"), new byte[]{1});
        writeFile(new File(containerDir, "広告/大番長.BMP"), new byte[]{1});
        writeFile(new File(containerDir, "超昂閃忍ハルカDM.dat"), new byte[]{1});

        for (String rel : runtimePaths) {
            writeFile(new File(gameDir, rel), new byte[]{1, 2});
        }
        for (String rel : bloatPaths) {
            writeFile(new File(gameDir, rel), new byte[]{3, 4});
        }

        File foundRoot = GameListKt.findGameRootDirInternal(stagingRoot, null, GameCatalog.ID_HARUKA);
        assertEquals(gameDir, foundRoot);

        GameListKt.pruneRedundantBaseBloat(foundRoot);
        for (String rel : runtimePaths) {
            assertTrue("Expected runtime file kept: " + rel, new File(foundRoot, rel).isFile());
        }
        for (String rel : bloatPaths) {
            assertFalse("Expected bloat deleted: " + rel, new File(foundRoot, rel).exists());
        }
        assertFalse(new File(foundRoot, "DLL").exists());
        assertFalse(new File(foundRoot, "SaveData").exists());
        assertFalse(new File(foundRoot, "広告").exists());
    }

    @Test
    public void resolvesImportBaseNameAndDiscoversContainerGamedataImport() {
        assertEquals(
                "Haruka",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "超昂閃忍ハルカ.ain"),
                        false
                )
        );
        assertEquals(
                "Haruka",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "挻峍慚擡僴儖僇.ain"),
                        false
                )
        );
        assertEquals(
                "Haruka",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "Haruka.ain"),
                        false
                )
        );
        assertEquals(
                "Haruka",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "AliceCN.ain", "HarukaDll.inc"),
                        false
                )
        );

        Map<String, List<NativeGameImport.TreeEntry<String, String>>> containerTree = new HashMap<>();
        containerTree.put("root", Arrays.asList(
                new NativeGameImport.TreeEntry<>("root/als", "als_inst.exe", false, "root/als"),
                new NativeGameImport.TreeEntry<>("root/setup", "Setup.bmp", false, "root/setup"),
                new NativeGameImport.TreeEntry<>("root/dm", "超昂閃忍ハルカDM.dat", false, "root/dm"),
                new NativeGameImport.TreeEntry<>("root/ads", "広告", true, "root/ads"),
                new NativeGameImport.TreeEntry<>("root/gamedata", "gamedata", true, "root/gamedata")
        ));
        containerTree.put("root/gamedata", Arrays.asList(
                new NativeGameImport.TreeEntry<>("root/gamedata/ini", "System40.ini", false, "root/gamedata/ini"),
                new NativeGameImport.TreeEntry<>("root/gamedata/ain", "超昂閃忍ハルカ.ain", false, "root/gamedata/ain"),
                new NativeGameImport.TreeEntry<>("root/gamedata/inc", "HarukaDll.inc", false, "root/gamedata/inc"),
                new NativeGameImport.TreeEntry<>("root/gamedata/data", "Data", true, "root/gamedata/data"),
                new NativeGameImport.TreeEntry<>("root/gamedata/movie", "movie", true, "root/gamedata/movie")
        ));
        final boolean[] visitedAds = {false};
        NativeGameImport.DiscoveredImportSources<String, String> discovered =
                NativeGameImport.INSTANCE.discoverImportSources(
                        "root",
                        containerTree.get("root"),
                        id -> {
                            if ("root/ads".equals(id)) visitedAds[0] = true;
                            return containerTree.getOrDefault(id, Collections.emptyList());
                        }
                );
        assertFalse("Should not traverse 広告/ bloat folder", visitedAds[0]);
        assertNotNull(discovered.getPrimaryEntries());
        assertEquals(1, discovered.getSourceTrees().size());
        assertEquals("root/gamedata", discovered.getSourceTrees().get(0).getFirst());

        Map<String, List<NativeGameImport.TreeEntry<String, String>>> wrappedTree = new HashMap<>();
        wrappedTree.put("wrap", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("wrap/haruka", "超昂閃忍ハルカ", true, "wrap/haruka")
        ));
        wrappedTree.put("wrap/haruka", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("wrap/haruka/gamedata", "gamedata", true, "wrap/haruka/gamedata")
        ));
        wrappedTree.put("wrap/haruka/gamedata", Arrays.asList(
                new NativeGameImport.TreeEntry<>("wrap/haruka/gamedata/ini", "System40.ini", false, "wrap/haruka/gamedata/ini"),
                new NativeGameImport.TreeEntry<>("wrap/haruka/gamedata/ain", "超昂閃忍ハルカ.ain", false, "wrap/haruka/gamedata/ain")
        ));
        NativeGameImport.DiscoveredImportSources<String, String> wrapped =
                NativeGameImport.INSTANCE.discoverImportSources(
                        "wrap",
                        wrappedTree.get("wrap"),
                        id -> wrappedTree.getOrDefault(id, Collections.emptyList())
                );
        assertNotNull(wrapped.getPrimaryEntries());
        assertEquals(1, wrapped.getSourceTrees().size());
        assertEquals("wrap/haruka/gamedata", wrapped.getSourceTrees().get(0).getFirst());
    }

    private static void putLe32(byte[] buf, int off, int val) {
        buf[off] = (byte) (val & 0xFF);
        buf[off + 1] = (byte) ((val >>> 8) & 0xFF);
        buf[off + 2] = (byte) ((val >>> 16) & 0xFF);
        buf[off + 3] = (byte) ((val >>> 24) & 0xFF);
    }

    private static void writeDirRecord(
            ByteArrayOutputStream out,
            String name,
            boolean isDir,
            int lba,
            int size
    ) throws Exception {
        byte[] idBytes = name.getBytes(StandardCharsets.UTF_16BE);
        int recLen = 33 + idBytes.length;
        if ((recLen & 1) != 0) recLen++;
        byte[] rec = new byte[recLen];
        rec[0] = (byte) recLen;
        putLe32(rec, 2, lba);
        putLe32(rec, 10, size);
        rec[25] = (byte) (isDir ? 0x02 : 0x00);
        rec[32] = (byte) idBytes.length;
        System.arraycopy(idBytes, 0, rec, 33, idBytes.length);
        out.write(rec);
    }

    private static File buildHarukaIso(File isoFile) throws Exception {
        final int sectorSize = 2048;
        final int rootLba = 18;
        final int containerLba = 19;
        final int adsLba = 20;
        final int gamedataLba = 21;
        final int dllLba = 22;
        final int dataLba = 23;
        final int movieLba = 24;
        final int fileDataLba = 25;
        final int totalSectors = 26;
        byte[] img = new byte[totalSectors * sectorSize];

        byte[] payload = "test".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(payload, 0, img, fileDataLba * sectorSize, payload.length);

        int svdOff = 16 * sectorSize;
        img[svdOff] = 2;
        img[svdOff + 1] = 'C';
        img[svdOff + 2] = 'D';
        img[svdOff + 3] = '0';
        img[svdOff + 4] = '0';
        img[svdOff + 5] = '1';
        img[svdOff + 88] = 0x25;
        img[svdOff + 89] = 0x2F;
        img[svdOff + 90] = 0x45;
        putLe32(img, svdOff + 158, rootLba);
        putLe32(img, svdOff + 166, sectorSize);

        int termOff = 17 * sectorSize;
        img[termOff] = (byte) 255;
        img[termOff + 1] = 'C';
        img[termOff + 2] = 'D';
        img[termOff + 3] = '0';
        img[termOff + 4] = '0';
        img[termOff + 5] = '1';

        ByteArrayOutputStream rootOut = new ByteArrayOutputStream();
        writeDirRecord(rootOut, "超昂閃忍ハルカ", true, containerLba, sectorSize);
        System.arraycopy(rootOut.toByteArray(), 0, img, rootLba * sectorSize, rootOut.size());

        ByteArrayOutputStream containerOut = new ByteArrayOutputStream();
        writeDirRecord(containerOut, "als_inst.exe;1", false, fileDataLba, payload.length);
        writeDirRecord(containerOut, "Setup.bmp;1", false, fileDataLba, payload.length);
        writeDirRecord(containerOut, "超昂閃忍ハルカDM.dat;1", false, fileDataLba, payload.length);
        writeDirRecord(containerOut, "広告", true, adsLba, sectorSize);
        writeDirRecord(containerOut, "gamedata", true, gamedataLba, sectorSize);
        System.arraycopy(containerOut.toByteArray(), 0, img, containerLba * sectorSize, containerOut.size());

        ByteArrayOutputStream adsOut = new ByteArrayOutputStream();
        writeDirRecord(adsOut, "大番長.BMP;1", false, fileDataLba, payload.length);
        System.arraycopy(adsOut.toByteArray(), 0, img, adsLba * sectorSize, adsOut.size());

        ByteArrayOutputStream gamedataOut = new ByteArrayOutputStream();
        writeDirRecord(gamedataOut, "System40.ini;1", false, fileDataLba, payload.length);
        writeDirRecord(gamedataOut, "超昂閃忍ハルカ.ain;1", false, fileDataLba, payload.length);
        writeDirRecord(gamedataOut, "HarukaDll.inc;1", false, fileDataLba, payload.length);
        writeDirRecord(gamedataOut, "System40.exe;1", false, fileDataLba, payload.length);
        writeDirRecord(gamedataOut, "ijl15.dll;1", false, fileDataLba, payload.length);
        writeDirRecord(gamedataOut, "超昂閃忍ハルカ.ico;1", false, fileDataLba, payload.length);
        writeDirRecord(gamedataOut, "DLL", true, dllLba, sectorSize);
        writeDirRecord(gamedataOut, "Data", true, dataLba, sectorSize);
        writeDirRecord(gamedataOut, "movie", true, movieLba, sectorSize);
        System.arraycopy(gamedataOut.toByteArray(), 0, img, gamedataLba * sectorSize, gamedataOut.size());

        ByteArrayOutputStream dllOut = new ByteArrayOutputStream();
        writeDirRecord(dllOut, "SACTDX.dll;1", false, fileDataLba, payload.length);
        System.arraycopy(dllOut.toByteArray(), 0, img, dllLba * sectorSize, dllOut.size());

        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        writeDirRecord(dataOut, "CGMode.acx;1", false, fileDataLba, payload.length);
        System.arraycopy(dataOut.toByteArray(), 0, img, dataLba * sectorSize, dataOut.size());

        ByteArrayOutputStream movieOut = new ByteArrayOutputStream();
        writeDirRecord(movieOut, "Opening.alm;1", false, fileDataLba, payload.length);
        System.arraycopy(movieOut.toByteArray(), 0, img, movieLba * sectorSize, movieOut.size());

        writeFile(isoFile, img);
        return isoFile;
    }

    @Test
    public void detectsAndExtractsHarukaIsoWithGamedataAndBloatPruning() throws Exception {
        File iso = buildHarukaIso(tmp.newFile("haruka.iso"));

        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, GameCatalog.ID_HARUKA));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, "DOKI_SA.ALD", GameCatalog.ID_ESCALAYER));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, "EscalayerReboot.ain", GameCatalog.ID_ESCALAYER_REBOOT));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, "Ixseal.ain", GameCatalog.ID_IXSEAL));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, GameCatalog.ID_ESCALAYER));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, GameCatalog.ID_ESCALAYER_REBOOT));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, GameCatalog.ID_IXSEAL));
        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, null));

        File extractedDir = tmp.newFolder("extracted_iso");
        Function1<String, Boolean> skip = rel ->
                NativeGameImport.INSTANCE.skip(rel) || GameListKt.isHarukaBloatRelPath(rel);
        int count = Iso9660Extractor.INSTANCE.extractFileToDirectory(
                iso,
                extractedDir,
                skip,
                false,
                null,
                null,
                null
        );
        assertEquals(5, count);

        File gameRoot = GameListKt.findGameRootDirInternal(
                extractedDir,
                null,
                GameCatalog.ID_HARUKA
        );
        assertNotNull(gameRoot);
        assertEquals(new File(extractedDir, "超昂閃忍ハルカ/gamedata"), gameRoot);
        assertTrue(new File(gameRoot, "System40.ini").isFile());
        assertTrue(new File(gameRoot, "超昂閃忍ハルカ.ain").isFile());
        assertTrue(new File(gameRoot, "HarukaDll.inc").isFile());
        assertTrue(new File(gameRoot, "Data/CGMode.acx").isFile());
        assertTrue(new File(gameRoot, "movie/Opening.alm").isFile());

        assertFalse(new File(extractedDir, "超昂閃忍ハルカ/als_inst.exe").exists());
        assertFalse(new File(extractedDir, "超昂閃忍ハルカ/Setup.bmp").exists());
        assertFalse(new File(extractedDir, "超昂閃忍ハルカ/超昂閃忍ハルカDM.dat").exists());
        assertFalse(new File(extractedDir, "超昂閃忍ハルカ/広告").exists());
        assertFalse(new File(gameRoot, "System40.exe").exists());
        assertFalse(new File(gameRoot, "ijl15.dll").exists());
        assertFalse(new File(gameRoot, "超昂閃忍ハルカ.ico").exists());
        assertFalse(new File(gameRoot, "DLL").exists());
    }

    @Test
    public void exposesExpectedOnlineCdnUrlBaseSizeAndStagingEviction() throws Exception {
        assertEquals("https://alicehub.000.moe/haruka", OnlineInstaller.HARUKA_CDN_URL);
        assertEquals(
                3797821541L,
                AppSettings.INSTANCE.defaultBaseGameSizeForUrl(OnlineInstaller.HARUKA_CDN_URL)
        );
        assertEquals(
                4729863037L,
                AppSettings.INSTANCE.defaultBaseGameSizeForUrl(OnlineInstaller.IXSEAL_CDN_URL)
        );

        File storage = tmp.newFolder("storage");
        File escStaging = new File(storage, ".online_base_staging_Escalayer");
        File rebootStaging = new File(storage, ".online_base_staging_EscalayerReboot");
        File ixsealStaging = new File(storage, ".online_base_staging_Ixseal");
        File harukaStaging = new File(storage, ".online_base_staging_Haruka");
        assertTrue(escStaging.mkdirs());
        assertTrue(rebootStaging.mkdirs());
        assertTrue(ixsealStaging.mkdirs());
        assertTrue(harukaStaging.mkdirs());

        List<File> evicted = OnlineInstaller.INSTANCE.evictBaseStagingToTrash(storage, GameCatalog.ID_HARUKA, null);
        assertEquals(1, evicted.size());
        assertFalse(harukaStaging.exists());
        assertTrue(ixsealStaging.exists());
        assertTrue(rebootStaging.exists());
        assertTrue(escStaging.exists());
    }

    @Test
    public void resolvesOnlineInstallDirBaseNameForHarukaCnAndJpLayouts() throws Exception {
        File cnStaging = tmp.newFolder("online_haruka_cn_staging");
        byte[] gbkIni = ("GameName = \"超昂闪忍遥\"\r\n"
                + "CodeName = \"AliceCN.ain\"\r\n"
                + "SaveFolder = \"SaveData\"\r\n").getBytes(GBK);
        writeFile(new File(cnStaging, "System40.ini"), gbkIni);
        writeFile(new File(cnStaging, "AliceCN.ain"), new byte[]{0});
        writeFile(new File(cnStaging, "HarukaDll.inc"), new byte[]{0});

        assertEquals(
                "Haruka",
                OnlineInstaller.INSTANCE.resolveInstallDirBaseName(cnStaging, "Haruka")
        );
        assertEquals(
                "Haruka",
                OnlineInstaller.INSTANCE.resolveInstallDirBaseName(cnStaging, "")
        );

        // Even with a minimal ASCII System40.ini (classification solely via AliceCN.ain + HarukaDll.inc),
        // OnlineInstaller must preserve the manifest/default "Haruka" directory instead of deriving "AliceCN".
        File cnMinimalIniStaging = tmp.newFolder("online_haruka_cn_minimal_staging");
        writeFile(new File(cnMinimalIniStaging, "System40.ini"), "CodeName = \"AliceCN.ain\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(cnMinimalIniStaging, "AliceCN.ain"), new byte[]{0});
        writeFile(new File(cnMinimalIniStaging, "HarukaDll.inc"), new byte[]{0});

        assertEquals(
                "Haruka",
                OnlineInstaller.INSTANCE.resolveInstallDirBaseName(cnMinimalIniStaging, "Haruka")
        );
        assertEquals(
                "Haruka",
                OnlineInstaller.INSTANCE.resolveInstallDirBaseName(cnMinimalIniStaging, "")
        );

        File jpStaging = tmp.newFolder("online_haruka_jp_staging");
        writeFile(new File(jpStaging, "System40.ini"), "GameName = \"超昂閃忍ハルカ\"\n".getBytes(SHIFT_JIS));
        writeFile(new File(jpStaging, "超昂閃忍ハルカ.ain"), new byte[]{0});
        writeFile(new File(jpStaging, "HarukaDll.inc"), new byte[]{0});

        assertEquals(
                "Haruka",
                OnlineInstaller.INSTANCE.resolveInstallDirBaseName(jpStaging, "Haruka")
        );
        assertEquals(
                "Haruka",
                OnlineInstaller.INSTANCE.resolveInstallDirBaseName(jpStaging, "")
        );
    }
}
