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

public class TsumashiboriCatalogTest {
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
    public void classifiesTsumashiboriFilesMojibakeAndDisambiguatesFromOthers() throws Exception {
        File cnDir = tmp.newFolder("TsumashiboriCN");
        byte[] gbkIni = ("; System4.0\n"
                + "GameName = \"妻しぼり\"\n"
                + "CodeName = \"妻しぼり.ain\"\n"
                + "SaveFolder = \"SaveData\"\n"
                + "MsgSkipName = \"已读文本跳过\"\n").getBytes(GBK);
        writeFile(new File(cnDir, "System40.ini"), gbkIni);
        writeFile(new File(cnDir, "妻しぼり.ain"), new byte[]{0});

        File mojibakeDir = tmp.newFolder("MojibakeRoot");
        byte[] sjisIni = ("GameName = \"妻しぼり\"\n"
                + "CodeName = \"妻しぼり.ain\"\n"
                + "SaveFolder = \"SaveData\"\n").getBytes(SHIFT_JIS);
        writeFile(new File(mojibakeDir, "System40.ini"), sjisIni);
        writeFile(new File(mojibakeDir, "嵢偟傏傝.ain"), new byte[]{0});

        File romajiDir = tmp.newFolder("CustomDir");
        writeFile(new File(romajiDir, "System40.ini"), "GameName = \"Tsumashibori\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(romajiDir, "Tsumashibori.ain"), new byte[]{0});

        File aldOnlyDir = tmp.newFolder("AldOnlyDir");
        writeFile(new File(aldOnlyDir, "System40.ini"), "GameName = \"妻しぼり\"\n".getBytes(GBK));
        writeFile(new File(aldOnlyDir, "妻しぼりGA.ald"), new byte[]{0});
        writeFile(new File(aldOnlyDir, "妻しぼりGZ.ald"), new byte[]{0});

        System40Ini parsedCn = System40Ini.Companion.parse(gbkIni);
        assertEquals("妻しぼり", parsedCn.getGameName());
        assertEquals("SaveData", parsedCn.getSaveFolder());

        System40Ini parsedSjis = System40Ini.Companion.parse(sjisIni);
        assertEquals("妻しぼり", parsedSjis.getGameName());
        assertEquals("SaveData", parsedSjis.getSaveFolder());

        File otherGame = tmp.newFolder("OtherGame");
        writeFile(new File(otherGame, "System40.ini"), "GameName = \"Other\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(otherGame, "OtherGame.ain"), new byte[]{0});

        File sys3Dir = tmp.newFolder("Sys3Game");
        writeFile(new File(sys3Dir, "DOKI_SA.ALD"), new byte[]{0});
        writeFile(new File(sys3Dir, "System39.ini"), "GameName = \"超昂天使エスカレイヤー\"\n".getBytes(StandardCharsets.UTF_8));

        assertFalse(GameCatalog.INSTANCE.isSystem3xGameDir(cnDir));
        assertTrue(GameCatalog.INSTANCE.isTsumashiboriGameDir(cnDir));
        assertTrue(GameCatalog.INSTANCE.isTsumashiboriGameDir(mojibakeDir));
        assertTrue(GameCatalog.INSTANCE.isTsumashiboriGameDir(romajiDir));
        assertTrue(GameCatalog.INSTANCE.isTsumashiboriGameDir(aldOnlyDir));
        assertFalse(GameCatalog.INSTANCE.isTsumashiboriGameDir(otherGame));
        assertFalse(GameCatalog.INSTANCE.isTsumashiboriGameDir(sys3Dir));
        assertFalse(GameCatalog.INSTANCE.isTsumashiboriGameFiles(Arrays.asList("System40.ini", "System39.ain")));
        assertFalse(GameCatalog.INSTANCE.isTsumashiboriGameFiles(Collections.singletonList("System40.ini")));
        assertTrue(GameCatalog.INSTANCE.isTsumashiboriGameFiles(Arrays.asList("System40.ini", "嵢偟傏傝GA.ald")));

        assertEquals(
                GameCatalog.ID_TSUMASHIBORI,
                GameCatalog.INSTANCE.classifyDirectory(cnDir, "UnknownFolder", null, null)
        );
        assertEquals(
                GameCatalog.ID_TSUMASHIBORI,
                GameCatalog.INSTANCE.classifyDirectory(mojibakeDir, "UnknownFolder", null, null)
        );
        assertEquals(
                GameCatalog.ID_TSUMASHIBORI,
                GameCatalog.INSTANCE.classifyDirectory(romajiDir, romajiDir.getName(), null, null)
        );
        assertEquals(
                GameCatalog.PREFIX_CUSTOM + "OtherGame",
                GameCatalog.INSTANCE.classifyDirectory(otherGame, otherGame.getName(), null, null)
        );

        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(cnDir, null, GameCatalog.ID_TSUMASHIBORI));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(cnDir, "妻しぼり.ain", GameCatalog.ID_TSUMASHIBORI));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(mojibakeDir, null, GameCatalog.ID_TSUMASHIBORI));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(mojibakeDir, "妻しぼり.ain", GameCatalog.ID_TSUMASHIBORI));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(cnDir, null, GameCatalog.ID_GALZOO));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(cnDir, null, GameCatalog.ID_IXSEAL));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(otherGame, null, GameCatalog.ID_TSUMASHIBORI));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(sys3Dir, null, GameCatalog.ID_TSUMASHIBORI));
    }

    @Test
    public void classifiesAliasesSaveDirsDescriptorCatalogOrderAndVndbMetadata() throws Exception {
        String[] aliases = {
                "Tsumashibori", "Tsumashibori-1", "Tsuma Shibori", "tsuma_shibori",
                "tsumasibori", "tumashibori", "tumasibori",
                "妻しぼり", "ツマシボリ", "つましぼり",
                "嵢偟傏傝", "嵢偟傏傝 汉化版",
                "妻管严", "妻管嚴", "妻榨", "妻搾",
                "\u69a8\u7cbe\u59bb", "\u643e\u7cbe\u59bb"
        };
        for (String alias : aliases) {
            assertTrue(alias, GameCatalog.INSTANCE.isTsumashiboriAlias(alias));
            assertEquals(alias, GameCatalog.ID_TSUMASHIBORI,
                    GameCatalog.INSTANCE.classifyDirectory(null, alias, null, null));
            assertEquals(alias, GameCatalog.ID_TSUMASHIBORI,
                    GameCatalog.INSTANCE.classifyDirectory(null, "custom", null, alias));
        }

        String[] nonAliases = {
                "Tsumamigui", "妻みぐい", "GALZOO", "Ixseal", "Rance06", "Escalayer", "SengokuRance", ""
        };
        for (String other : nonAliases) {
            assertFalse(other, GameCatalog.INSTANCE.isTsumashiboriAlias(other));
            String classified = GameCatalog.INSTANCE.classifyDirectory(null, other, null, null);
            assertFalse(other, GameCatalog.ID_TSUMASHIBORI.equals(classified));
        }

        File sysSaveDir = tmp.newFolder("TsumaSysSave");
        writeFile(new File(sysSaveDir, "妻しぼりSYS.asd"), new byte[]{1});
        writeFile(new File(sysSaveDir, "MsgSkip.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_TSUMASHIBORI,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", sysSaveDir, null)
        );

        File slotSaveDir = tmp.newFolder("TsumaSlotSave");
        writeFile(new File(slotSaveDir, "妻しぼり1.asd"), new byte[]{1});
        writeFile(new File(slotSaveDir, "妻しぼり99.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_TSUMASHIBORI,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", slotSaveDir, null)
        );

        File mojibakeSaveDir = tmp.newFolder("TsumaMojibakeSave");
        writeFile(new File(mojibakeSaveDir, "嵢偟傏傝SYS.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_TSUMASHIBORI,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", mojibakeSaveDir, null)
        );

        // Generic MsgSkip.asd alone must NOT falsely classify as Tsumashibori.
        File genericMsgSkipDir = tmp.newFolder("GenericMsgSkipSave");
        writeFile(new File(genericMsgSkipDir, "MsgSkip.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.PREFIX_CUSTOM + "UnknownDir",
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", genericMsgSkipDir, null)
        );

        GameDescriptor desc = GameCatalog.INSTANCE.descriptorForWithResolver(
                GameCatalog.ID_TSUMASHIBORI,
                null,
                resId -> "resolved:" + resId
        );
        assertEquals(GameCatalog.ID_TSUMASHIBORI, desc.getId());
        assertEquals("resolved:" + R.string.game_tsumashibori_title, desc.getTitle());
        assertEquals("Tsumashibori", desc.getDefaultDirName());
        assertNull(desc.getExpectedAinName());
        assertEquals(R.drawable.ic_game_tsumashibori, desc.getIconRes());
        assertTrue(desc.getSupportsOnlineBaseInstall());
        assertEquals(OnlineInstaller.TSUMASHIBORI_CDN_URL, desc.getOnlineBaseCdnUrl());
        assertFalse(desc.getSupportsOnlineMod());
        assertFalse(desc.getSupportsLocalMod());
        assertFalse(desc.isRance10());

        List<String> order = GameCatalog.builtInSelectOrder();
        assertEquals(20 + LocalGamePresets.INSTANCE.getAll().size(), order.size());
        int ixIdx = order.indexOf(GameCatalog.ID_IXSEAL);
        int tsIdx = order.indexOf(GameCatalog.ID_TSUMASHIBORI);
        assertTrue(ixIdx >= 0);
        assertTrue(tsIdx > ixIdx);
        for (String id : order.subList(0, 20)) {
            assertNotNull("Missing GameSortMetadata for " + id, GameSortMetadata.INSTANCE.forGame(id));
        }

        GameSortInfo info = GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_TSUMASHIBORI);
        assertNotNull(info);
        assertEquals("2006-08-04", info.getReleaseDate());
        assertEquals(79.0, info.getRating(), 0.001);
        assertEquals("https://vndb.org/v887", info.getScoreSourceUrl());
        assertEquals("https://www.alicesoft.com/tsumasibori/", info.getReleaseSourceUrl());

        // Verify parity with offline VNDB snapshot JSON without Android org.json stubs.
        File snapshotFile = new File("../docs/game-sort-vndb-snapshot.json");
        if (!snapshotFile.isFile()) {
            snapshotFile = new File("../../docs/game-sort-vndb-snapshot.json");
        }
        assertTrue("Snapshot JSON should exist", snapshotFile.isFile());
        String snapshotText = new String(Files.readAllBytes(snapshotFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("v887 missing from request filters", snapshotText.contains("\"v887\""));
        Matcher v887Matcher = Pattern.compile("\\{[^{}]*\"id\"\\s*:\\s*\"v887\"[^{}]*\\}", Pattern.DOTALL)
                .matcher(snapshotText);
        assertTrue("v887 object missing from game-sort-vndb-snapshot.json", v887Matcher.find());
        String v887Block = v887Matcher.group();
        assertTrue("v887 rating mismatch", Pattern.compile("\"rating\"\\s*:\\s*79(?:\\.0)?\\b").matcher(v887Block).find());
        assertTrue("v887 released mismatch", v887Block.contains("\"released\": \"2006-08-04\""));
        assertTrue("v887 alttitle mismatch", v887Block.contains("\"alttitle\": \"妻しぼり\""));

        GameSelectEntry galzooEntry = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_GALZOO, "GALZOO Island", "GALZOO",
                        null, 0, 0, 0, 0, true, OnlineInstaller.GALZOO_CDN_URL,
                        false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(),
                null
        );
        GameSelectEntry tsumaEntry = new GameSelectEntry(
                desc,
                Collections.emptyList(),
                null
        );
        List<GameSelectEntry> byDate = GameSorting.INSTANCE.sorted(
                Arrays.asList(galzooEntry, tsumaEntry),
                GameSortMode.RELEASE_DATE,
                Locale.ENGLISH
        );
        assertEquals(GameCatalog.ID_TSUMASHIBORI, byDate.get(0).getDescriptor().getId());
        List<GameSelectEntry> byRating = GameSorting.INSTANCE.sorted(
                Arrays.asList(galzooEntry, tsumaEntry),
                GameSortMode.RATING,
                Locale.ENGLISH
        );
        assertEquals(GameCatalog.ID_TSUMASHIBORI, byRating.get(0).getDescriptor().getId());
    }

    @Test
    public void modifierBridgeDisambiguatesTsumashiboriFromAllOtherGames() throws Exception {
        File gameDir = tmp.newFolder("TsumashiboriDir");
        writeFile(new File(gameDir, "System40.ini"), "GameName = \"妻しぼり\"\n".getBytes(GBK));
        writeFile(new File(gameDir, "妻しぼり.ain"), new byte[]{0});

        File saveDir = tmp.newFolder("Tsumashibori");
        writeFile(new File(saveDir, "妻しぼりSYS.asd"), new byte[]{1});
        writeFile(new File(saveDir, "MsgSkip.asd"), new byte[]{1});

        assertTrue(GameModifierBridge.INSTANCE.isTsumashiboriGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance10GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isDohnaDohnaGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicleGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicle2GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isSengokuRanceGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance01GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance02GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance03GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance06GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance08GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance09GameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerRebootGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isGalzooGameDir(gameDir, null, saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isIxsealGameDir(gameDir, null, saveDir));

        assertTrue(GameModifierBridge.INSTANCE.isTsumashiboriGameDir(null, "妻しぼり", saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isSengokuRanceGameDir(null, "妻しぼり", saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance08GameDir(null, "妻しぼり", saveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicleGameDir(null, "妻しぼり", saveDir));
    }

    @Test
    public void prunesTsumashiboriBloatDeprioritizesBackupAndPreservesAll22RuntimeFiles() throws Exception {
        String[] bloatPaths = {
                "System40.exe",
                "ijl15.dll",
                "卸载汉化.bat",
                "请读我.txt",
                "妻しぼり.ico",
                "嵢偟傏傝.ico",
                "DLL/SACT2.dll",
                "DLL/Sys42VM.dll",
                "backup/System40.exe",
                "backup/System40.ini",
                "backup/妻しぼり.ain",
                "backup/Data/Album.acx",
                "backup/DLL/Sys42VM.dll",
                "汉化补丁/全CG.rar",
                "汉化补丁/妻しぼり汉化补丁.rar",
                "汉化补丁/汉化修图补丁.rar",
                "解答集/00P表紙.jpg",
                "解答集/01P 葵 咲良.jpg",
                "SaveData/妻しぼりSYS.asd",
                "System40.ini.bak"
        };
        for (String rel : bloatPaths) {
            assertTrue("Expected bloat: " + rel, GameListKt.isTsumashiboriBloatRelPath(rel));
        }

        // Exact 22 runtime files confirmed by engine & R2 manifest.
        String[] runtimePaths = {
                "System40.ini",
                "妻しぼり.ain",
                "妻しぼりBA.ald",
                "妻しぼりBA.bgi",
                "妻しぼりGA.ald",
                "妻しぼりGB.ald",
                "妻しぼりGC.ald",
                "妻しぼりGM.ald",
                "妻しぼりGZ.ald",
                "妻しぼりWA.ald",
                "妻しぼりWA.wai",
                "妻しぼりWB.ald",
                "妻しぼりＯＰ.mpg",
                "AliceLogo2.alk",
                "DrawPluginDLL.inc",
                "SACT_DLL.inc",
                "Data/Album.acx",
                "Data/BackLogName.acx",
                "Data/Item.acx",
                "Data/Root.acx",
                "Data/Visual.acx",
                "Data/Weather.acx"
        };
        assertEquals(22, runtimePaths.length);
        for (String rel : runtimePaths) {
            assertFalse("Runtime file must not be treated as bloat: " + rel, GameListKt.isTsumashiboriBloatRelPath(rel));
            assertFalse("Runtime file must not be skipped by NativeGameImport: " + rel, NativeGameImport.INSTANCE.skip(rel));
        }

        // Verify findGameRootDirInternal prefers main root over backup/ even when both have System40.ini + 妻しぼり.ain.
        File stagingRoot = tmp.newFolder("staging");
        File mainRoot = new File(stagingRoot, "妻しぼり");
        for (String rel : runtimePaths) {
            writeFile(new File(mainRoot, rel), new byte[]{1, 2});
        }
        for (String rel : bloatPaths) {
            writeFile(new File(mainRoot, rel), new byte[]{3, 4});
        }
        // Also verify backup/ at a shallower path length than a nested main root is STILL deprioritized!
        File altStage = tmp.newFolder("alt_staging");
        File shallowBackup = new File(altStage, "backup");
        writeFile(new File(shallowBackup, "System40.ini"), "GameName = \"妻しぼり\"\n".getBytes(SHIFT_JIS));
        writeFile(new File(shallowBackup, "妻しぼり.ain"), new byte[]{9});
        File deeperMain = new File(altStage, "VeryLongWrapperFolderName/妻しぼり");
        writeFile(new File(deeperMain, "System40.ini"), "GameName = \"妻しぼり\"\n".getBytes(GBK));
        writeFile(new File(deeperMain, "妻しぼり.ain"), new byte[]{1});

        assertEquals(mainRoot, GameListKt.findGameRootDirInternal(stagingRoot, null, GameCatalog.ID_TSUMASHIBORI));
        assertEquals(deeperMain, GameListKt.findGameRootDirInternal(altStage, null, GameCatalog.ID_TSUMASHIBORI));

        GameListKt.pruneRedundantBaseBloat(mainRoot);
        for (String rel : runtimePaths) {
            assertTrue("Expected runtime file kept: " + rel, new File(mainRoot, rel).isFile());
        }
        for (String rel : bloatPaths) {
            assertFalse("Expected bloat deleted: " + rel, new File(mainRoot, rel).exists());
        }
        assertFalse(new File(mainRoot, "backup").exists());
        assertFalse(new File(mainRoot, "汉化补丁").exists());
        assertFalse(new File(mainRoot, "解答集").exists());
        assertFalse(new File(mainRoot, "DLL").exists());
        assertFalse(new File(mainRoot, "SaveData").exists());
    }

    @Test
    public void resolvesImportBaseNameAndDiscoversImportSourcesIgnoringBackupSubdirs() {
        assertEquals(
                "Tsumashibori",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "妻しぼり.ain"),
                        false
                )
        );
        assertEquals(
                "Tsumashibori",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "嵢偟傏傝.ain"),
                        false
                )
        );
        assertEquals(
                "Tsumashibori",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "Tsumashibori.ain"),
                        false
                )
        );

        // Container directory holding 妻しぼり/ + sibling backup/ + 汉化补丁/ + 解答集/
        Map<String, List<NativeGameImport.TreeEntry<String, String>>> tree = new HashMap<>();
        tree.put("container", Arrays.asList(
                new NativeGameImport.TreeEntry<>("container/backup", "backup", true, "container/backup"),
                new NativeGameImport.TreeEntry<>("container/patch", "汉化补丁", true, "container/patch"),
                new NativeGameImport.TreeEntry<>("container/guide", "解答集", true, "container/guide"),
                new NativeGameImport.TreeEntry<>("container/game", "妻しぼり", true, "container/game")
        ));
        tree.put("container/backup", Arrays.asList(
                new NativeGameImport.TreeEntry<>("container/backup/ini", "System40.ini", false, "container/backup/ini"),
                new NativeGameImport.TreeEntry<>("container/backup/ain", "妻しぼり.ain", false, "container/backup/ain")
        ));
        tree.put("container/patch", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("container/patch/rar", "妻しぼり汉化补丁.rar", false, "container/patch/rar")
        ));
        tree.put("container/guide", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("container/guide/jpg", "00P表紙.jpg", false, "container/guide/jpg")
        ));
        tree.put("container/game", Arrays.asList(
                new NativeGameImport.TreeEntry<>("container/game/ini", "System40.ini", false, "container/game/ini"),
                new NativeGameImport.TreeEntry<>("container/game/ain", "妻しぼり.ain", false, "container/game/ain"),
                new NativeGameImport.TreeEntry<>("container/game/ga", "妻しぼりGA.ald", false, "container/game/ga"),
                new NativeGameImport.TreeEntry<>("container/game/backup", "backup", true, "container/game/backup")
        ));

        NativeGameImport.DiscoveredImportSources<String, String> discovered = NativeGameImport.INSTANCE.discoverImportSources(
                "container",
                tree.get("container"),
                id -> tree.getOrDefault(id, Collections.emptyList())
        );
        assertNotNull(discovered.getPrimaryEntries());
        assertEquals(1, discovered.getSourceTrees().size());
        assertEquals("container/game", discovered.getSourceTrees().get(0).getFirst());
        assertEquals("", discovered.getSourceTrees().get(0).getThird());
        assertTrue(discovered.getIxsealPatchTrees().isEmpty());
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

    private static File buildTsumashiboriIso(File isoFile, String ainName) throws Exception {
        final int sectorSize = 2048;
        final int rootLba = 18;
        final int gameLba = 19;
        final int backupLba = 20;
        final int dataLba = 21;
        final int guideLba = 22;
        final int fileDataLba = 23;
        final int totalSectors = 24;
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
        writeDirRecord(rootOut, "妻しぼり", true, gameLba, sectorSize);
        System.arraycopy(rootOut.toByteArray(), 0, img, rootLba * sectorSize, rootOut.size());

        ByteArrayOutputStream gameOut = new ByteArrayOutputStream();
        writeDirRecord(gameOut, "System40.ini;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, ainName + ";1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "妻しぼりGZ.ald;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "System40.exe;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "请读我.txt;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "backup", true, backupLba, sectorSize);
        writeDirRecord(gameOut, "Data", true, dataLba, sectorSize);
        writeDirRecord(gameOut, "解答集", true, guideLba, sectorSize);
        System.arraycopy(gameOut.toByteArray(), 0, img, gameLba * sectorSize, gameOut.size());

        ByteArrayOutputStream backupOut = new ByteArrayOutputStream();
        writeDirRecord(backupOut, "System40.ini;1", false, fileDataLba, payload.length);
        writeDirRecord(backupOut, ainName + ";1", false, fileDataLba, payload.length);
        System.arraycopy(backupOut.toByteArray(), 0, img, backupLba * sectorSize, backupOut.size());

        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        writeDirRecord(dataOut, "Album.acx;1", false, fileDataLba, payload.length);
        System.arraycopy(dataOut.toByteArray(), 0, img, dataLba * sectorSize, dataOut.size());

        ByteArrayOutputStream guideOut = new ByteArrayOutputStream();
        writeDirRecord(guideOut, "00P表紙.jpg;1", false, fileDataLba, payload.length);
        System.arraycopy(guideOut.toByteArray(), 0, img, guideLba * sectorSize, guideOut.size());

        writeFile(isoFile, img);
        return isoFile;
    }

    @Test
    public void detectsAndExtractsTsumashiboriIsoIncludingMojibakeWithBloatPruning() throws Exception {
        File jpIso = buildTsumashiboriIso(tmp.newFile("tsuma_jp.iso"), "妻しぼり.ain");
        File mojibakeIso = buildTsumashiboriIso(tmp.newFile("tsuma_mojibake.iso"), "嵢偟傏傝.ain");

        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(jpIso, null, GameCatalog.ID_TSUMASHIBORI));
        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(jpIso, "妻しぼり.ain", GameCatalog.ID_TSUMASHIBORI));
        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(mojibakeIso, "妻しぼり.ain", GameCatalog.ID_TSUMASHIBORI));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(jpIso, null, GameCatalog.ID_IXSEAL));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(jpIso, null, GameCatalog.ID_GALZOO));

        File extractedDir = tmp.newFolder("extracted_iso");
        Function1<String, Boolean> skip = rel ->
                NativeGameImport.INSTANCE.skip(rel) || GameListKt.isTsumashiboriBloatRelPath(rel);
        int count = Iso9660Extractor.INSTANCE.extractFileToDirectory(
                jpIso,
                extractedDir,
                skip,
                false,
                null,
                null,
                null
        );
        assertEquals(4, count);

        File gameRoot = GameListKt.findGameRootDirInternal(extractedDir, null, GameCatalog.ID_TSUMASHIBORI);
        assertNotNull(gameRoot);
        assertEquals(new File(extractedDir, "妻しぼり"), gameRoot);
        assertTrue(new File(gameRoot, "System40.ini").isFile());
        assertTrue(new File(gameRoot, "妻しぼり.ain").isFile());
        assertTrue(new File(gameRoot, "妻しぼりGZ.ald").isFile());
        assertTrue(new File(gameRoot, "Data/Album.acx").isFile());
        assertFalse(new File(gameRoot, "System40.exe").exists());
        assertFalse(new File(gameRoot, "请读我.txt").exists());
        assertFalse(new File(gameRoot, "backup").exists());
        assertFalse(new File(gameRoot, "解答集").exists());
    }

    @Test
    public void exposesExpectedOnlineCdnUrlBaseSizeAndStagingEviction() throws Exception {
        assertEquals("https://alicehub.000.moe/tsumashibori", OnlineInstaller.TSUMASHIBORI_CDN_URL);
        assertEquals(
                2850964030L,
                AppSettings.DEFAULT_TSUMASHIBORI_BYTES
        );
        assertEquals(
                2850964030L,
                AppSettings.defaultBaseGameSizeForUrl(OnlineInstaller.TSUMASHIBORI_CDN_URL)
        );
        assertEquals(
                2850964030L,
                AppSettings.defaultBaseGameSizeForUrl(OnlineInstaller.TSUMASHIBORI_CDN_URL + "/")
        );

        File storage = tmp.newFolder("storage");
        File tsumaStaging = new File(storage, ".online_base_staging_Tsumashibori");
        File tsumaJpStaging = new File(storage, ".online_base_staging_妻しぼり");
        File ixsealStaging = new File(storage, ".online_base_staging_Ixseal");
        File galzooStaging = new File(storage, ".online_base_staging_GALZOO");
        assertTrue(tsumaStaging.mkdirs());
        assertTrue(tsumaJpStaging.mkdirs());
        assertTrue(ixsealStaging.mkdirs());
        assertTrue(galzooStaging.mkdirs());

        List<File> evicted = OnlineInstaller.INSTANCE.evictBaseStagingToTrash(storage, GameCatalog.ID_TSUMASHIBORI, null);
        assertEquals(2, evicted.size());
        assertFalse(tsumaStaging.exists());
        assertFalse(tsumaJpStaging.exists());
        assertTrue(ixsealStaging.exists());
        assertTrue(galzooStaging.exists());
    }
}
