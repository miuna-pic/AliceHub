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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GalzooCatalogTest {
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
            int size,
            boolean joliet
    ) throws Exception {
        byte[] idBytes = joliet
                ? name.getBytes(StandardCharsets.UTF_16BE)
                : name.getBytes(StandardCharsets.US_ASCII);
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

    private static File buildTestIso(
            File isoFile,
            boolean joliet,
            List<String> gameDirFiles,
            String subDirPath
    ) throws Exception {
        final int sectorSize = 2048;
        final int rootLba = 18;
        final int midLba = 19;
        final int leafLba = 20;
        final int dataLba = 21;
        final int totalSectors = 23;
        byte[] img = new byte[totalSectors * sectorSize];

        int vdOff = 16 * sectorSize;
        img[vdOff] = (byte) (joliet ? 2 : 1);
        img[vdOff + 1] = 'C';
        img[vdOff + 2] = 'D';
        img[vdOff + 3] = '0';
        img[vdOff + 4] = '0';
        img[vdOff + 5] = '1';
        if (joliet) {
            img[vdOff + 88] = 0x25;
            img[vdOff + 89] = 0x2F;
            img[vdOff + 90] = 0x45;
        }
        putLe32(img, vdOff + 158, rootLba);
        putLe32(img, vdOff + 166, sectorSize);

        int termOff = 17 * sectorSize;
        img[termOff] = (byte) 255;
        img[termOff + 1] = 'C';
        img[termOff + 2] = 'D';
        img[termOff + 3] = '0';
        img[termOff + 4] = '0';
        img[termOff + 5] = '1';

        int targetDirLba = rootLba;
        if (subDirPath != null && !subDirPath.isEmpty()) {
            String[] segs = subDirPath.split("/");
            if (segs.length == 1) {
                ByteArrayOutputStream rootOut = new ByteArrayOutputStream();
                writeDirRecord(rootOut, segs[0], true, midLba, sectorSize, joliet);
                byte[] rootBytes = rootOut.toByteArray();
                System.arraycopy(rootBytes, 0, img, rootLba * sectorSize, rootBytes.length);
                targetDirLba = midLba;
            } else if (segs.length == 2) {
                ByteArrayOutputStream rootOut = new ByteArrayOutputStream();
                writeDirRecord(rootOut, segs[0], true, midLba, sectorSize, joliet);
                byte[] rootBytes = rootOut.toByteArray();
                System.arraycopy(rootBytes, 0, img, rootLba * sectorSize, rootBytes.length);

                ByteArrayOutputStream midOut = new ByteArrayOutputStream();
                writeDirRecord(midOut, segs[1], true, leafLba, sectorSize, joliet);
                byte[] midBytes = midOut.toByteArray();
                System.arraycopy(midBytes, 0, img, midLba * sectorSize, midBytes.length);
                targetDirLba = leafLba;
            }
        }

        byte[] payload = "SYS4".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(payload, 0, img, dataLba * sectorSize, payload.length);

        ByteArrayOutputStream gameOut = new ByteArrayOutputStream();
        for (String name : gameDirFiles) {
            writeDirRecord(gameOut, name + ";1", false, dataLba, payload.length, joliet);
        }
        byte[] gameBytes = gameOut.toByteArray();
        System.arraycopy(gameBytes, 0, img, targetDirLba * sectorSize, gameBytes.length);

        writeFile(isoFile, img);
        return isoFile;
    }

    @Test
    public void registersGalzooInBuiltInOrderAndSortMetadata() {
        assertEquals("galzoo", GameCatalog.ID_GALZOO);
        List<String> order = GameCatalog.builtInSelectOrder();
        int idxReboot = order.indexOf(GameCatalog.ID_ESCALAYER_REBOOT);
        int idxGalzoo = order.indexOf(GameCatalog.ID_GALZOO);
        assertTrue(idxReboot >= 0);
        assertEquals(idxReboot + 1, idxGalzoo);

        GameSortInfo info = GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_GALZOO);
        assertNotNull(info);
        assertEquals("2005-12-09", info.getReleaseDate());
        assertEquals(Double.valueOf(76.8), info.getRating());
        assertEquals("https://vndb.org/v334", info.getScoreSourceUrl());
        assertEquals("https://www.alicesoft.com/galzoo/", info.getReleaseSourceUrl());
    }

    @Test
    public void classifiesChineseAndJapaneseGalzooGameRoots() throws Exception {
        File cnDir = tmp.newFolder("GALZOO");
        String cnIni = "GameName\t= \"GALZOO汉化版V1.00（由 哈尼喵汉化组 组织汉化）\"\r\n" +
                "CodeName\t= \"GALZOO.ain\"\r\n" +
                "SaveFolder\t= \"SaveData\"\r\n" +
                "MsgSkipName = \"已读文本跳过\"\r\n";
        writeFile(new File(cnDir, "System40.ini"), cnIni.getBytes(Charset.forName("GBK")));
        writeFile(new File(cnDir, "GALZOO.ain"), new byte[]{1, 2, 3, 4});
        writeFile(new File(cnDir, "GALZOOGA.ald"), new byte[]{0});
        writeFile(new File(cnDir, "GALZOODLL.inc"), new byte[]{0});
        writeFile(new File(cnDir, "Data/PolyObj.lin"), new byte[]{0});
        writeFile(new File(cnDir, "Data/Event.alk"), new byte[]{0});

        System40Ini parsed = System40Ini.Companion.parse(new File(cnDir, "System40.ini"));
        assertEquals("GALZOO汉化版V1.00（由 哈尼喵汉化组 组织汉化）", parsed.getGameName());
        assertEquals("SaveData", parsed.getSaveFolder());
        assertFalse(GameCatalog.INSTANCE.isSystem3xGameDir(cnDir));
        assertTrue(GameCatalog.INSTANCE.isGalzooGameDir(cnDir));
        assertEquals(GameCatalog.ID_GALZOO, GameCatalog.INSTANCE.classifyGameRootDir(cnDir));
        assertEquals(GameCatalog.ID_GALZOO, GameCatalog.INSTANCE.classifyDirectory(cnDir, cnDir.getName(), null, null));

        File jpDir = tmp.newFolder("JP_Galzoo");
        String jpIni = "GameName = \"GALZOOアイランド\"\r\nCodeName = \"GALZOO.ain\"\r\nSaveFolder = \"SaveData\"\r\n";
        writeFile(new File(jpDir, "AliceStart.ini"), jpIni.getBytes(Charset.forName("Shift_JIS")));
        writeFile(new File(jpDir, "GALZOOアイランド.ain"), new byte[]{1, 2, 3, 4});
        assertTrue(GameCatalog.INSTANCE.isGalzooGameDir(jpDir));
        assertEquals(GameCatalog.ID_GALZOO, GameCatalog.INSTANCE.classifyDirectory(jpDir, jpDir.getName(), null, null));

        File altDir = tmp.newFolder("AltGalzoo");
        writeFile(new File(altDir, "System40.ini"), "GameName = \"GALZOO Island\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(altDir, "GALZOOIsland.ain"), new byte[]{1});
        assertEquals(GameCatalog.ID_GALZOO, GameCatalog.INSTANCE.classifyDirectory(altDir, altDir.getName(), null, null));

        // Even if a CN repack renames GALZOO.ain to AliceCN.ain, GALZOO*.ald / GALZOODLL.inc classify as GALZOO (never Sengoku Rance).
        File aliceCnDir = tmp.newFolder("RepackGalzoo");
        writeFile(new File(aliceCnDir, "System40.ini"), cnIni.getBytes(Charset.forName("GBK")));
        writeFile(new File(aliceCnDir, "AliceCN.ain"), new byte[]{1});
        writeFile(new File(aliceCnDir, "GALZOOGA.ald"), new byte[]{0});
        writeFile(new File(aliceCnDir, "GALZOODLL.inc"), new byte[]{0});
        assertEquals(GameCatalog.ID_GALZOO, GameCatalog.INSTANCE.classifyGameRootDir(aliceCnDir));

        File extractedCacheRoot = new File("/root/.cache/alicehub-galzoo-game");
        File extractedGameDir = new File("/root/.cache/alicehub-galzoo-game/GALZOO");
        if (extractedGameDir.isDirectory()) {
            assertEquals(GameCatalog.ID_GALZOO, GameCatalog.INSTANCE.classifyGameRootDir(extractedGameDir));
            File discovered = GameListKt.findGameRootDirInternal(extractedCacheRoot, null, GameCatalog.ID_GALZOO);
            assertNotNull(discovered);
            assertEquals(extractedGameDir.getCanonicalPath(), discovered.getCanonicalPath());
        }
    }

    @Test
    public void classifiesGalzooBySaveDirAndAliasesWithoutCollisions() throws Exception {
        File saveFull = tmp.newFolder("GalzooSaveFull");
        writeFile(new File(saveFull, "keyfile.dat"), new byte[]{1});
        writeFile(new File(saveFull, "MsgSkip.asd"), new byte[]{2});
        writeFile(new File(saveFull, "MsgSkipFlag.sav"), new byte[]{3});
        writeFile(new File(saveFull, "GALZOO汉化版V1.00（由 哈尼喵汉化组 组织汉化）01.ASD"), new byte[]{4});
        assertEquals(
                GameCatalog.ID_GALZOO,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownSaveDir", saveFull, null)
        );

        File saveKeyOnly = tmp.newFolder("GalzooSaveKey");
        writeFile(new File(saveKeyOnly, "keyfile.dat"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_GALZOO,
                GameCatalog.INSTANCE.classifyDirectory(null, "CustomSave", saveKeyOnly, null)
        );

        File saveFlagOnly = tmp.newFolder("GalzooSaveFlag");
        writeFile(new File(saveFlagOnly, "MsgSkipFlag.sav"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_GALZOO,
                GameCatalog.INSTANCE.classifyDirectory(null, "CustomSave", saveFlagOnly, null)
        );

        File saveSlotOnly = tmp.newFolder("GalzooSaveSlot");
        writeFile(new File(saveSlotOnly, "GALZOOアイランド01.ASD"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_GALZOO,
                GameCatalog.INSTANCE.classifyDirectory(null, "CustomSave", saveSlotOnly, null)
        );

        assertTrue(GameModifierBridge.INSTANCE.isGalzooGameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance10GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isDohnaDohnaGameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicleGameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicle2GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance01GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance02GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance03GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance06GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isSengokuRanceGameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance08GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isRance09GameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(null, "GALZOO", saveFull));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerRebootGameDir(null, "GALZOO", saveFull));

        String[] positiveAliases = new String[]{
                "GALZOO",
                "GALZOO-1",
                "GALZOO Island",
                "GALZOOIsland",
                "GALZOOアイランド",
                "GALZOO アイランド",
                "ギャルズーアイランド",
                "GALZOO岛",
                "GALZOO艾兰德",
                "GALZOO汉化版V1.00（由 哈尼喵汉化组 组织汉化）",
                "[ALICESOFT] アイランド GALZOO 汉化硬盘版"
        };
        for (String alias : positiveAliases) {
            assertTrue("Expected positive alias: " + alias, GameCatalog.INSTANCE.isGalzooAlias(alias));
            assertEquals(
                    "Expected ID_GALZOO for dirName: " + alias,
                    GameCatalog.ID_GALZOO,
                    GameCatalog.INSTANCE.classifyDirectory(null, alias, null, null)
            );
            assertEquals(
                    "Expected ID_GALZOO for displayName: " + alias,
                    GameCatalog.ID_GALZOO,
                    GameCatalog.INSTANCE.classifyDirectory(null, "OtherFolder", null, alias)
            );
        }

        String[] negativeAliases = new String[]{
                "GALZOO2",
                "MyGalzoo",
                "Rance10",
                "Rance06",
                "SengokuRance",
                "Evenicle",
                "Evenicle2",
                "Escalayer",
                "EscalayerReboot",
                "DohnaDohna",
                "KichikuRance"
        };
        for (String nonGalzoo : negativeAliases) {
            assertFalse("Expected negative alias: " + nonGalzoo, GameCatalog.INSTANCE.isGalzooAlias(nonGalzoo));
            assertNotEquals(
                    "Must not classify as ID_GALZOO: " + nonGalzoo,
                    GameCatalog.ID_GALZOO,
                    GameCatalog.INSTANCE.classifyDirectory(null, nonGalzoo, null, nonGalzoo)
            );
        }
    }

    @Test
    public void validatesTargetGameFilesDirsAndIsoImagesWhenExpectedAinIsNull() throws Exception {
        File galzooDir = tmp.newFolder("ImportedGalzoo");
        writeFile(new File(galzooDir, "System40.ini"), "GameName = \"GALZOO\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(galzooDir, "GALZOO.ain"), new byte[]{0});

        File galzooJp = tmp.newFolder("ImportedGalzooJp");
        writeFile(new File(galzooJp, "AliceStart.ini"), "GameName = \"GALZOOアイランド\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(galzooJp, "GALZOOアイランド.ain"), new byte[]{0});

        // Unrelated game inside a folder deceptively named "GALZOO" must still be rejected!
        File fakeGalzoo = tmp.newFolder("GALZOO");
        writeFile(new File(fakeGalzoo, "AliceStart.ini"), "GameName = \"Other\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(fakeGalzoo, "OtherGame.ain"), new byte[]{0});

        File rance06 = tmp.newFolder("Rance06Dir");
        writeFile(new File(rance06, "System40.ini"), "GameName = \"Rance6\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(rance06, "Rance6.ain"), new byte[]{0});

        File sengoku = tmp.newFolder("SengokuDir");
        writeFile(new File(sengoku, "AliceStart.ini"), "GameName = \"Sengoku\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(sengoku, "SengokuRance.ain"), new byte[]{0});

        File rance10 = tmp.newFolder("Rance10Dir");
        writeFile(new File(rance10, "AliceStart.ini"), "GameName = \"Rance10\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(rance10, "Rance10.ain"), new byte[]{0});

        File kichiku = tmp.newFolder("KichikuDir");
        writeFile(new File(kichiku, "KichikuRanceSA.ALD"), new byte[]{0});

        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(galzooDir, null, GameCatalog.ID_GALZOO));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(galzooJp, null, GameCatalog.ID_GALZOO));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(fakeGalzoo, null, GameCatalog.ID_GALZOO));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(rance06, null, GameCatalog.ID_GALZOO));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(sengoku, null, GameCatalog.ID_GALZOO));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(rance10, null, GameCatalog.ID_GALZOO));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(kichiku, null, GameCatalog.ID_GALZOO));

        assertTrue(GameCatalog.INSTANCE.matchesTargetGameFiles(
                GameCatalog.ID_GALZOO,
                Arrays.asList("System40.ini", "GALZOO.ain", "GALZOOGA.ald")
        ));
        assertTrue(GameCatalog.INSTANCE.matchesTargetGameFiles(
                GameCatalog.ID_GALZOO,
                Arrays.asList("AliceStart.ini", "GALZOOアイランド.ain")
        ));
        assertFalse(GameCatalog.INSTANCE.matchesTargetGameFiles(
                GameCatalog.ID_GALZOO,
                Arrays.asList("System40.ini", "Rance6.ain", "Rance6GA.ald")
        ));
        assertFalse(GameCatalog.INSTANCE.matchesTargetGameFiles(
                GameCatalog.ID_GALZOO,
                Collections.singletonList("DOKI_SA.ALD")
        ));

        File galzooIso = buildTestIso(
                tmp.newFile("galzoo.iso"),
                false,
                Arrays.asList("System40.ini", "GALZOO.ain", "GALZOOGA.ald"),
                "GALZOO"
        );
        File galzooJolietIso = buildTestIso(
                tmp.newFile("galzoo_joliet.mdf"),
                true,
                Arrays.asList("AliceStart.ini", "GALZOOIsland.ain"),
                "InstallSet/InstallData"
        );
        File r06Iso = buildTestIso(
                tmp.newFile("rance6.iso"),
                false,
                Arrays.asList("System40.ini", "Rance6.ain"),
                "GameData"
        );

        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(galzooIso, null, GameCatalog.ID_GALZOO));
        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(galzooJolietIso, null, GameCatalog.ID_GALZOO));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(r06Iso, null, GameCatalog.ID_GALZOO));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(galzooIso, null, GameCatalog.ID_RANCE06));

        File isoExtractOut = tmp.newFolder("iso_out");
        int extractedCount = Iso9660Extractor.INSTANCE.extractFileToDirectory(
                galzooJolietIso,
                isoExtractOut,
                rel -> NativeGameImport.INSTANCE.skip(rel),
                false,
                null,
                null,
                null
        );
        assertEquals(2, extractedCount);
        File foundFromIso = GameListKt.findGameRootDirInternal(isoExtractOut, null, GameCatalog.ID_GALZOO);
        assertNotNull(foundFromIso);
        assertEquals("InstallData", foundFromIso.getName());
    }

    @Test
    public void mergesCompanionDataPrunesBloatAndResolvesDefaultDirName() throws Exception {
        assertEquals(
                "GALZOO",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "GALZOO.ain", "GALZOOGA.ald"),
                        false
                )
        );
        assertEquals(
                "GALZOO",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("AliceStart.ini", "GALZOOアイランド.ain"),
                        false
                )
        );
        assertEquals(
                "GALZOO",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("System40.ini", "AliceCN.ain", "GALZOOGA.ald", "GALZOODLL.inc"),
                        false
                )
        );

        assertTrue(GameListKt.isGalzooBloatRelPath("System40.exe"));
        assertTrue(GameListKt.isGalzooBloatRelPath("GALZOO/System40.exe"));
        assertTrue(GameListKt.isGalzooBloatRelPath("GALZOO/ijl15.dll"));
        assertTrue(GameListKt.isGalzooBloatRelPath("GALZOO/GALZOO.ico"));
        assertTrue(GameListKt.isGalzooBloatRelPath("三合一/DLL/DrawDungeon14.dll"));
        assertTrue(GameListKt.isGalzooBloatRelPath("三合一/SaveData/keyfile.dat"));
        assertTrue(GameListKt.isGalzooBloatRelPath("三合一/Data/map01.mrk"));
        assertTrue(GameListKt.isGalzooBloatRelPath("Data/map08.mrk"));
        assertTrue(GameListKt.isGalzooBloatRelPath("desktop.ini"));
        assertTrue(GameListKt.isGalzooBloatRelPath("autorun.inf"));

        String[] rootRuntimePaths = new String[]{
                "System40.ini",
                "GALZOO.ain",
                "GALZOOBA.ald",
                "GALZOOBA.bgi",
                "GALZOOGA.ald",
                "GALZOOGB.ald",
                "GALZOOGC.ald",
                "GALZOOWA.ald",
                "GALZOOWA.wai",
                "GALZOOWB.ald",
                "AliceLogo2.alk",
                "DrawPluginDLL.inc",
                "GALZOODLL.inc",
                "SACT_DLL.inc"
        };
        String[] dataRuntimePaths = new String[]{
                "Data/Event.alk",
                "Data/PolyObj.lin",
                "Data/map01.dgn", "Data/map01.dtx", "Data/map01.tes",
                "Data/map02.dgn", "Data/map02.dtx", "Data/map02.tes",
                "Data/map03.dgn", "Data/map03.dtx", "Data/map03.tes",
                "Data/map04.dgn", "Data/map04.dtx", "Data/map04.tes",
                "Data/map05.dgn", "Data/map05.dtx", "Data/map05.tes",
                "Data/map06.dgn", "Data/map06.dtx", "Data/map06.tes",
                "Data/map07.dgn", "Data/map07.dtx", "Data/map07.tes",
                "Data/map08.dgn", "Data/map08.dtx", "Data/map08.tes",
                "Data/map09.dgn", "Data/map09.dtx", "Data/map09.tes",
                "Data/map10.dgn", "Data/map10.dtx", "Data/map10.tes"
        };
        assertEquals(14, rootRuntimePaths.length);
        assertEquals(32, dataRuntimePaths.length);

        File stageRoot = tmp.newFolder("stage0");
        File gameDir = new File(stageRoot, "GALZOO");
        File companionDataDir = new File(stageRoot, "三合一/Data");
        for (String rel : rootRuntimePaths) {
            assertFalse("Runtime file must be kept: " + rel, GameListKt.isGalzooBloatRelPath(rel));
            assertFalse("Runtime file must not be skipped by NativeGameImport: " + rel, NativeGameImport.INSTANCE.skip(rel));
            writeFile(new File(gameDir, rel), new byte[]{1, 2});
        }
        for (String rel : dataRuntimePaths) {
            assertFalse("Data runtime file must be kept: " + rel, GameListKt.isGalzooBloatRelPath(rel));
            assertFalse("Data runtime file must not be skipped by NativeGameImport: " + rel, NativeGameImport.INSTANCE.skip(rel));
            writeFile(new File(companionDataDir, rel.substring("Data/".length())), new byte[]{3, 4});
        }
        // Populate bloat inside GALZOO and 三合一
        writeFile(new File(gameDir, "System40.exe"), new byte[]{9});
        writeFile(new File(gameDir, "ijl15.dll"), new byte[]{9});
        writeFile(new File(gameDir, "GALZOO.ico"), new byte[]{9});
        writeFile(new File(gameDir, "BackData/GALZOO.ain"), new byte[]{9});
        writeFile(new File(gameDir, "BackData/System40.ini"), new byte[]{9});
        writeFile(new File(gameDir, "desktop.ini"), new byte[]{9});
        writeFile(new File(companionDataDir, "map01.mrk"), new byte[0]);
        writeFile(new File(stageRoot, "三合一/DLL/DrawDungeon14.dll"), new byte[]{9});
        writeFile(new File(stageRoot, "三合一/SaveData/keyfile.dat"), new byte[]{9});

        File chosenRoot = GameListKt.findGameRootDirInternal(stageRoot, null, GameCatalog.ID_GALZOO);
        assertNotNull(chosenRoot);
        assertEquals(gameDir.getCanonicalPath(), chosenRoot.getCanonicalPath());

        GameListKt.mergeGalzooCompanionData(Arrays.asList(stageRoot, chosenRoot), chosenRoot);
        GameListKt.pruneRedundantBaseBloat(chosenRoot);

        for (String rel : rootRuntimePaths) {
            assertTrue("Root runtime file must survive: " + rel, new File(chosenRoot, rel).isFile());
        }
        for (String rel : dataRuntimePaths) {
            assertTrue("Merged Data file must survive: " + rel, new File(chosenRoot, rel).isFile());
        }
        assertFalse(new File(chosenRoot, "System40.exe").exists());
        assertFalse(new File(chosenRoot, "ijl15.dll").exists());
        assertFalse(new File(chosenRoot, "GALZOO.ico").exists());
        assertFalse(new File(chosenRoot, "BackData").exists());
        assertFalse(new File(chosenRoot, "desktop.ini").exists());
        assertFalse(new File(chosenRoot, "Data/map01.mrk").exists());
    }

    @Test
    public void verifiesOnlineInstallerCdnUrlExpectedBytesAndSnapshotParity() throws Exception {
        assertEquals("https://alicehub.000.moe/galzoo", OnlineInstaller.GALZOO_CDN_URL);
        assertEquals(4027583522L, AppSettings.defaultBaseGameSizeForUrl(OnlineInstaller.GALZOO_CDN_URL));
        assertEquals(4027583522L, AppSettings.defaultBaseGameSizeForUrl("https://alicehub.000.moe/galzoo/"));

        File storage = tmp.newFolder("storage");
        File galzooStaging = new File(storage, ".online_base_staging_GALZOO");
        File r06Staging = new File(storage, ".online_base_staging_Rance06");
        File r10Staging = new File(storage, ".online_base_staging_Rance10");
        assertTrue(galzooStaging.mkdirs());
        assertTrue(r06Staging.mkdirs());
        assertTrue(r10Staging.mkdirs());

        List<File> evicted = OnlineInstaller.INSTANCE.evictBaseStagingToTrash(storage, GameCatalog.ID_GALZOO, null);
        assertEquals(1, evicted.size());
        assertFalse(galzooStaging.exists());
        assertTrue(r06Staging.exists());
        assertTrue(r10Staging.exists());

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
        assertEquals(Double.valueOf(76.8), vndbRatings.get("https://vndb.org/v334"));

        for (String id : GameCatalog.builtInSelectOrder().subList(0, 20)) {
            GameSortInfo meta = GameSortMetadata.INSTANCE.forGame(id);
            assertNotNull("Missing GameSortMetadata for built-in: " + id, meta);
            LocalDate.parse(meta.getReleaseDate());
            assertTrue(meta.getReleaseSourceUrl().startsWith("https://"));
            if (meta.getRating() != null) {
                assertTrue(meta.getScoreSourceUrl().startsWith("https://vndb.org/v"));
                assertEquals(vndbRatings.get(meta.getScoreSourceUrl()), meta.getRating());
            }
        }

        // Verify GameSorting places GALZOO Island cleanly by VNDB rating & release date without Robolectric.
        GameSelectEntry rance06 = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_RANCE06, "Rance 06", "Rance06", null, 0, 0, 0, 0, true, OnlineInstaller.RANCE06_CDN_URL, false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(), null);
        GameSelectEntry galzoo = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_GALZOO, "GALZOO Island", "GALZOO", null, 0, 0, 0, 0, true, OnlineInstaller.GALZOO_CDN_URL, false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(), null);
        GameSelectEntry evenicle2 = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_EVENICLE2, "Evenicle 2", "Evenicle2", "Evenicle2.ain", 0, 0, 0, 0, true, OnlineInstaller.EVENICLE2_CDN_URL, false, false, false, false, null, "files/mods/index.json"),
                Collections.emptyList(), null);

        List<GameSelectEntry> byRating = GameSorting.INSTANCE.sorted(
                Arrays.asList(evenicle2, galzoo, rance06), GameSortMode.RATING, Locale.ENGLISH);
        assertEquals(GameCatalog.ID_RANCE06, byRating.get(0).getDescriptor().getId());
        assertEquals(GameCatalog.ID_GALZOO, byRating.get(1).getDescriptor().getId());
        assertEquals(GameCatalog.ID_EVENICLE2, byRating.get(2).getDescriptor().getId());

        List<GameSelectEntry> byRelease = GameSorting.INSTANCE.sorted(
                Arrays.asList(rance06, galzoo, evenicle2), GameSortMode.RELEASE_DATE, Locale.ENGLISH);
        assertEquals(GameCatalog.ID_EVENICLE2, byRelease.get(0).getDescriptor().getId());
        assertEquals(GameCatalog.ID_GALZOO, byRelease.get(1).getDescriptor().getId());
        assertEquals(GameCatalog.ID_RANCE06, byRelease.get(2).getDescriptor().getId());
    }
}
