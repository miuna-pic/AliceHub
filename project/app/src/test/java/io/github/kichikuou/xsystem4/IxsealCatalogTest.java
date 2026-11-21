package io.github.kichikuou.xsystem4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IxsealCatalogTest {
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
    public void classifiesIxsealFilesAndDisambiguatesFromEscalayerAndOthers() throws Exception {
        File jpDir = tmp.newFolder("Ixseal");
        byte[] sjisIni = ("GameName = \"超昂神騎エクシール\"\n"
                + "BootName = \"Ixseal\"\n"
                + "CodeName = \"Ixseal.ain\"\n"
                + "SaveFolder = \"SaveData\"\n").getBytes(SHIFT_JIS);
        writeFile(new File(jpDir, "AliceStart.ini"), sjisIni);
        writeFile(new File(jpDir, "Ixseal.ain"), new byte[]{0});

        File cnDir = tmp.newFolder("CustomGameFolder");
        byte[] gbkIni = ("// 僔僗僥儉僙僢僩傾僢僾\n"
                + "GameName = \"超昂神骑 汉化版V0.99【哈尼喵汉化组】\"\n"
                + "BootName = \"Ixseal\"\n"
                + "CodeName = \"Ixseal.ain\"\n"
                + "SaveFolder = \"SaveData\"\n").getBytes(GBK);
        writeFile(new File(cnDir, "AliceStart.ini"), gbkIni);
        writeFile(new File(cnDir, "ixseal.ain"), new byte[]{0});

        System40Ini parsedJp = System40Ini.Companion.parse(sjisIni);
        assertEquals("超昂神騎エクシール", parsedJp.getGameName());
        assertEquals("SaveData", parsedJp.getSaveFolder());

        System40Ini parsedCn = System40Ini.Companion.parse(gbkIni);
        assertEquals("超昂神骑 汉化版V0.99【哈尼喵汉化组】", parsedCn.getGameName());
        assertEquals("SaveData", parsedCn.getSaveFolder());

        File sys3Escalayer = tmp.newFolder("Escalayer");
        writeFile(new File(sys3Escalayer, "DOKI_SA.ALD"), new byte[]{0});
        writeFile(new File(sys3Escalayer, "System39.ini"), "GameName = \"超昂天使エスカレイヤー\"\n".getBytes(StandardCharsets.UTF_8));

        File rebootDir = tmp.newFolder("EscalayerReboot");
        writeFile(new File(rebootDir, "AliceStart.ini"), "GameName = \"超昂天使Escalayer，by axela\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(rebootDir, "EscalayerReboot.ain"), new byte[]{0});

        File fakeIxseal = tmp.newFolder("IxsealFake");
        writeFile(new File(fakeIxseal, "AliceStart.ini"), "GameName = \"Other\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(fakeIxseal, "OtherGame.ain"), new byte[]{0});

        assertFalse(GameCatalog.INSTANCE.isSystem3xGameDir(jpDir));
        assertTrue(GameCatalog.INSTANCE.isIxsealGameDir(jpDir));
        assertTrue(GameCatalog.INSTANCE.isIxsealGameDir(cnDir));
        assertFalse(GameCatalog.INSTANCE.isIxsealGameDir(sys3Escalayer));
        assertFalse(GameCatalog.INSTANCE.isIxsealGameDir(rebootDir));
        assertFalse(GameCatalog.INSTANCE.isIxsealGameDir(fakeIxseal));
        assertFalse(GameCatalog.INSTANCE.isIxsealGameFiles(Arrays.asList("AliceStart.ini", "System39.ain")));
        assertFalse(GameCatalog.INSTANCE.isIxsealGameFiles(Collections.singletonList("AliceStart.ini")));

        assertEquals(
                GameCatalog.ID_IXSEAL,
                GameCatalog.INSTANCE.classifyDirectory(jpDir, jpDir.getName(), null, null)
        );
        assertEquals(
                GameCatalog.ID_IXSEAL,
                GameCatalog.INSTANCE.classifyDirectory(cnDir, cnDir.getName(), null, "超昂神骑 汉化版V0.99【哈尼喵汉化组】")
        );
        assertEquals(
                GameCatalog.ID_ESCALAYER,
                GameCatalog.INSTANCE.classifyDirectory(sys3Escalayer, sys3Escalayer.getName(), null, null)
        );
        assertEquals(
                GameCatalog.ID_ESCALAYER_REBOOT,
                GameCatalog.INSTANCE.classifyDirectory(rebootDir, rebootDir.getName(), null, null)
        );
        assertEquals(
                GameCatalog.PREFIX_CUSTOM + "IxsealFake",
                GameCatalog.INSTANCE.classifyDirectory(fakeIxseal, fakeIxseal.getName(), null, null)
        );

        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, "Ixseal.ain", GameCatalog.ID_IXSEAL));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, null, GameCatalog.ID_IXSEAL));
        assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(cnDir, "Ixseal.ain", GameCatalog.ID_IXSEAL));

        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, "EscalayerReboot.ain", GameCatalog.ID_ESCALAYER_REBOOT));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(jpDir, null, GameCatalog.ID_ESCALAYER));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(sys3Escalayer, "Ixseal.ain", GameCatalog.ID_IXSEAL));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(rebootDir, "Ixseal.ain", GameCatalog.ID_IXSEAL));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(fakeIxseal, "Ixseal.ain", GameCatalog.ID_IXSEAL));
        assertFalse(GameCatalog.INSTANCE.validateImportedGameDir(fakeIxseal, null, GameCatalog.ID_IXSEAL));
    }

    @Test
    public void classifiesAliasesSaveDirsCatalogOrderAndSortingMetadata() throws Exception {
        String[] ixsealAliases = {
                "Ixseal", "Ixseal-1", "Beat Valkyrie Ixseal",
                "超昂神騎エクシール", "超昂神騎", "超昂神骑",
                "エクシール", "超昂神骑 汉化版V0.99【哈尼喵汉化组】"
        };
        for (String alias : ixsealAliases) {
            assertEquals(alias, GameCatalog.ID_IXSEAL,
                    GameCatalog.INSTANCE.classifyDirectory(null, alias, null, null));
            assertEquals(alias, GameCatalog.ID_IXSEAL,
                    GameCatalog.INSTANCE.classifyDirectory(null, "custom", null, alias));
        }

        String[] escalayerAliases = {
                "Escalayer", "Escalayer-1", "Beat Angel Escalayer",
                "EscalayerReboot", "Beat Angel Escalayer Reboot",
                "超昂天使エスカレイヤー", "超昂天使エスカレイヤー・リブート"
        };
        for (String alias : escalayerAliases) {
            assertFalse(alias, GameCatalog.INSTANCE.isIxsealAlias(alias));
            String classified = GameCatalog.INSTANCE.classifyDirectory(null, alias, null, null);
            assertFalse(alias, GameCatalog.ID_IXSEAL.equals(classified));
        }

        File ixsealSaveDir = tmp.newFolder("IxsealSaves");
        writeFile(new File(ixsealSaveDir, "Ixseal_AFBGMMode.asd"), new byte[]{1});
        writeFile(new File(ixsealSaveDir, "Ixseal_Extra.sav"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_IXSEAL,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", ixsealSaveDir, null)
        );

        File evenicleSaveDir = tmp.newFolder("EvenicleSaves");
        writeFile(new File(evenicleSaveDir, "CommonSystemData.sav"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_EVENICLE,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", evenicleSaveDir, null)
        );

        File sys3EscSaveDir = tmp.newFolder("Sys3EscSaves");
        writeFile(new File(sys3EscSaveDir, "doki_sa.asd"), new byte[]{1});
        assertEquals(
                GameCatalog.ID_ESCALAYER,
                GameCatalog.INSTANCE.classifyDirectory(null, "UnknownDir", sys3EscSaveDir, null)
        );

        List<String> order = GameCatalog.builtInSelectOrder();
        assertEquals(20 + LocalGamePresets.INSTANCE.getAll().size(), order.size());
        int erIdx = order.indexOf(GameCatalog.ID_ESCALAYER_REBOOT);
        int galIdx = order.indexOf(GameCatalog.ID_GALZOO);
        int ixIdx = order.indexOf(GameCatalog.ID_IXSEAL);
        assertTrue(erIdx >= 0);
        assertTrue(galIdx > erIdx);
        assertTrue(ixIdx > galIdx);
        assertTrue(order.contains(GameCatalog.ID_DAIAKUJI));
        for (String id : order.subList(0, 20)) {
            assertNotNull("Missing GameSortMetadata for " + id, GameSortMetadata.INSTANCE.forGame(id));
        }

        GameSortInfo info = GameSortMetadata.INSTANCE.forGame(GameCatalog.ID_IXSEAL);
        assertNotNull(info);
        assertEquals("2017-07-28", info.getReleaseDate());
        assertEquals(72.0, info.getRating(), 0.001);
        assertEquals("https://vndb.org/v21142", info.getScoreSourceUrl());
        assertEquals("https://www.alicesoft.com/ixseal/", info.getReleaseSourceUrl());

        GameSelectEntry rebootEntry = new GameSelectEntry(
                new GameDescriptor(GameCatalog.ID_ESCALAYER_REBOOT, "Escalayer Reboot", "EscalayerReboot",
                        "EscalayerReboot.ain", 0, 0, 0, 0, true, OnlineInstaller.ESCALAYER_REBOOT_CDN_URL,
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
                Arrays.asList(rebootEntry, ixsealEntry),
                GameSortMode.RELEASE_DATE,
                java.util.Locale.ENGLISH
        );
        assertEquals(GameCatalog.ID_IXSEAL, byDate.get(0).getDescriptor().getId());
        List<GameSelectEntry> byRating = GameSorting.INSTANCE.sorted(
                Arrays.asList(rebootEntry, ixsealEntry),
                GameSortMode.RATING,
                java.util.Locale.ENGLISH
        );
        assertEquals(GameCatalog.ID_IXSEAL, byRating.get(0).getDescriptor().getId());
    }

    @Test
    public void modifierBridgeDisambiguatesIxsealFromEscalayerAndOthers() throws Exception {
        File ixsealDir = tmp.newFolder("IxsealDir");
        writeFile(new File(ixsealDir, "AliceStart.ini"), "GameName = \"超昂神騎エクシール\"\n".getBytes(StandardCharsets.UTF_8));
        writeFile(new File(ixsealDir, "Ixseal.ain"), new byte[]{0});

        File ixsealSaveDir = tmp.newFolder("Ixseal");
        writeFile(new File(ixsealSaveDir, "Ixseal_AFBGMMode.asd"), new byte[]{1});
        writeFile(new File(ixsealSaveDir, "Ixseal_Extra.sav"), new byte[]{1});

        assertTrue(GameModifierBridge.INSTANCE.isIxsealGameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerRebootGameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicleGameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicle2GameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance10GameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isDohnaDohnaGameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance03GameDir(ixsealDir, null, ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isRance09GameDir(ixsealDir, null, ixsealSaveDir));

        assertTrue(GameModifierBridge.INSTANCE.isIxsealGameDir(null, "Ixseal", ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(null, "Ixseal", ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEvenicleGameDir(null, "Ixseal", ixsealSaveDir));
        assertFalse(GameModifierBridge.INSTANCE.isEscalayerGameDir(null, "超昂神騎エクシール", null));

        File sys3Dir = tmp.newFolder("Sys3Escalayer");
        writeFile(new File(sys3Dir, "DOKI_SA.ALD"), new byte[]{0});
        assertTrue(GameModifierBridge.INSTANCE.isEscalayerGameDir(sys3Dir, null, null));
        assertFalse(GameModifierBridge.INSTANCE.isIxsealGameDir(sys3Dir, null, null));
    }

    @Test
    public void prunesIxsealBloatOverlaysPatchCnAndPreservesRuntimeFiles() throws Exception {
        String[] bloatPaths = {
                "Ixseal.exe",
                "OpenSaveFolder.exe",
                "ResetConfig.exe",
                "Uninstaller.exe",
                "DLL/DrawMovie.dll",
                "DirectX/dxsetup.exe",
                "@vieira.txt",
                "Game uploaded by the user vieira",
                "汉化补丁备份/AliceStart.ini",
                "汉化补丁备份/Ixseal.ain",
                "汉化补丁备份/IxsealCG.afa",
                "原版有码备份/IxsealCG.afa",
                "无码补丁备份/IxsealCG.afa",
                "patchWM/IxsealCG.afa",
                "特典相关/ALICE SOUND ALBUM VOL.30 IX SEAL & MAMAN/Scans/Cover.jpg",
                "全CG存档/Ixseal_AFBGMMode.asd",
                "全CG存档/Ixseal_Extra.sav",
                "SaveData/Ixseal_Extra.sav"
        };
        for (String rel : bloatPaths) {
            assertTrue(rel, GameListKt.isIxsealBloatRelPath(rel));
        }

        // Archive staging keeps patchCN/*.ain/*.afa/*.ini when skipPatchCnSubdir=false
        // while still stripping Windows executables inside patchCN.
        assertFalse(GameListKt.isIxsealBloatRelPath("patchCN/超昂神骑Release_CNPack_251225/Ixseal.ain", false));
        assertTrue(GameListKt.isIxsealBloatRelPath("patchCN/超昂神骑Release_CNPack_251225/Ixseal.exe", false));
        assertTrue(GameListKt.isIxsealBloatRelPath("patchCN/超昂神骑Release_CNPack_251225/Ixseal.ain", true));

        String[] runtimePaths = {
                "AliceStart.ini",
                "Ixseal.ain",
                "Ixseal.ex",
                "IxsealFont.fnl",
                "IxsealCG.afa",
                "IxsealFlat.afa",
                "IxsealPact.afa",
                "IxsealSound.afa",
                "IxsealVoice.afa",
                "Version.txt",
                "Data/Shader.slk",
                "Data/Movie/ix_op01.apeg",
                "Manual/index.html",
                "Manual/img/pm01.jpg",
                "License/libogg/COPYING"
        };
        for (String rel : runtimePaths) {
            assertFalse(rel, GameListKt.isIxsealBloatRelPath(rel));
            assertFalse(rel, NativeGameImport.INSTANCE.skip(rel));
        }

        File stagingRoot = tmp.newFolder("staging");
        File gameDir = new File(stagingRoot, "Wrapper/Ixseal");
        for (String rel : runtimePaths) {
            writeFile(new File(gameDir, rel), new byte[]{1, 2});
        }
        for (String rel : bloatPaths) {
            writeFile(new File(gameDir, rel), new byte[]{3, 4});
        }
        File patchCnDir = new File(stagingRoot, "Wrapper/patchCN/超昂神骑Release_CNPack_251225");
        writeFile(new File(patchCnDir, "AliceStart.ini"), "GameName = \"超昂神骑\"\n".getBytes(GBK));
        writeFile(new File(patchCnDir, "Ixseal.ain"), new byte[]{9, 9, 9});
        writeFile(new File(patchCnDir, "IxsealCG.afa"), new byte[]{8, 8, 8});
        writeFile(new File(patchCnDir, "Ixseal.exe"), new byte[]{7, 7});

        File foundRoot = GameListKt.findGameRootDirInternal(stagingRoot, "Ixseal.ain", GameCatalog.ID_IXSEAL);
        assertEquals(gameDir, foundRoot);

        Function0<Unit> noCancel = () -> Unit.INSTANCE;
        GameListKt.overlayIxsealCnPatchIfPresent(Collections.singletonList(stagingRoot), foundRoot, noCancel);
        GameListKt.pruneRedundantBaseBloat(foundRoot);

        assertEquals((byte) 9, java.nio.file.Files.readAllBytes(new File(foundRoot, "Ixseal.ain").toPath())[0]);
        assertEquals((byte) 8, java.nio.file.Files.readAllBytes(new File(foundRoot, "IxsealCG.afa").toPath())[0]);
        for (String rel : runtimePaths) {
            assertTrue("Expected runtime file kept: " + rel, new File(foundRoot, rel).isFile());
        }
        for (String rel : bloatPaths) {
            assertFalse("Expected bloat deleted: " + rel, new File(foundRoot, rel).exists());
        }
        assertFalse(new File(foundRoot, "汉化补丁备份").exists());
        assertFalse(new File(foundRoot, "原版有码备份").exists());
        assertFalse(new File(foundRoot, "无码补丁备份").exists());
        assertFalse(new File(foundRoot, "特典相关").exists());
        assertFalse(new File(foundRoot, "全CG存档").exists());
        assertFalse(new File(foundRoot, "DLL").exists());
        assertFalse(new File(foundRoot, "DirectX").exists());
        assertFalse(new File(foundRoot, "SaveData").exists());
    }

    @Test
    public void resolvesImportBaseNameForIxseal() {
        assertEquals(
                "Ixseal",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("AliceStart.ini", "Ixseal.ain"),
                        false
                )
        );
        assertEquals(
                "Ixseal",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("AliceStart.ini", "エクシール.ain"),
                        false
                )
        );
        assertEquals(
                "Ixseal",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("AliceStart.ini", "超昂神騎エクシール.ain"),
                        false
                )
        );
        assertEquals(
                "Ixseal",
                NativeGameImport.INSTANCE.resolveImportBaseNameFromFileNames(
                        Arrays.asList("AliceStart.ini", "超昂神骑.ain"),
                        false
                )
        );
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

    private static File buildIxsealIso(File isoFile) throws Exception {
        final int sectorSize = 2048;
        final int rootLba = 18;
        final int gameLba = 19;
        final int backupLba = 20;
        final int bonusLba = 21;
        final int dataLba = 22;
        final int movieLba = 23;
        final int manualLba = 24;
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
        writeDirRecord(rootOut, "Ixseal", true, gameLba, sectorSize);
        System.arraycopy(rootOut.toByteArray(), 0, img, rootLba * sectorSize, rootOut.size());

        ByteArrayOutputStream gameOut = new ByteArrayOutputStream();
        writeDirRecord(gameOut, "AliceStart.ini;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "Ixseal.ain;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "Ixseal.exe;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "OpenSaveFolder.exe;1", false, fileDataLba, payload.length);
        writeDirRecord(gameOut, "汉化补丁备份", true, backupLba, sectorSize);
        writeDirRecord(gameOut, "特典相关", true, bonusLba, sectorSize);
        writeDirRecord(gameOut, "Data", true, dataLba, sectorSize);
        writeDirRecord(gameOut, "Manual", true, manualLba, sectorSize);
        System.arraycopy(gameOut.toByteArray(), 0, img, gameLba * sectorSize, gameOut.size());

        ByteArrayOutputStream backupOut = new ByteArrayOutputStream();
        writeDirRecord(backupOut, "Ixseal.ain;1", false, fileDataLba, payload.length);
        System.arraycopy(backupOut.toByteArray(), 0, img, backupLba * sectorSize, backupOut.size());

        ByteArrayOutputStream bonusOut = new ByteArrayOutputStream();
        writeDirRecord(bonusOut, "Cover.jpg;1", false, fileDataLba, payload.length);
        System.arraycopy(bonusOut.toByteArray(), 0, img, bonusLba * sectorSize, bonusOut.size());

        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        writeDirRecord(dataOut, "Shader.slk;1", false, fileDataLba, payload.length);
        writeDirRecord(dataOut, "Movie", true, movieLba, sectorSize);
        System.arraycopy(dataOut.toByteArray(), 0, img, dataLba * sectorSize, dataOut.size());

        ByteArrayOutputStream movieOut = new ByteArrayOutputStream();
        writeDirRecord(movieOut, "ix_op01.apeg;1", false, fileDataLba, payload.length);
        System.arraycopy(movieOut.toByteArray(), 0, img, movieLba * sectorSize, movieOut.size());

        ByteArrayOutputStream manualOut = new ByteArrayOutputStream();
        writeDirRecord(manualOut, "index.html;1", false, fileDataLba, payload.length);
        System.arraycopy(manualOut.toByteArray(), 0, img, manualLba * sectorSize, manualOut.size());

        writeFile(isoFile, img);
        return isoFile;
    }

    @Test
    public void detectsAndExtractsIxsealIsoWithBloatPruning() throws Exception {
        File iso = buildIxsealIso(tmp.newFile("ixseal.iso"));

        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(iso, "Ixseal.ain", GameCatalog.ID_IXSEAL));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, "DOKI_SA.ALD", GameCatalog.ID_ESCALAYER));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, "EscalayerReboot.ain", GameCatalog.ID_ESCALAYER_REBOOT));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, GameCatalog.ID_ESCALAYER));
        assertFalse(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, GameCatalog.ID_ESCALAYER_REBOOT));
        assertTrue(Iso9660Extractor.INSTANCE.containsGameRoot(iso, null, null));

        File extractedDir = tmp.newFolder("extracted_iso");
        Function1<String, Boolean> skip = rel ->
                NativeGameImport.INSTANCE.skip(rel) || GameListKt.isIxsealBloatRelPath(rel, true);
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
                "Ixseal.ain",
                GameCatalog.ID_IXSEAL
        );
        assertNotNull(gameRoot);
        assertEquals(new File(extractedDir, "Ixseal"), gameRoot);
        assertTrue(new File(gameRoot, "AliceStart.ini").isFile());
        assertTrue(new File(gameRoot, "Ixseal.ain").isFile());
        assertTrue(new File(gameRoot, "Data/Shader.slk").isFile());
        assertTrue(new File(gameRoot, "Data/Movie/ix_op01.apeg").isFile());
        assertTrue(new File(gameRoot, "Manual/index.html").isFile());

        assertFalse(new File(gameRoot, "Ixseal.exe").exists());
        assertFalse(new File(gameRoot, "OpenSaveFolder.exe").exists());
        assertFalse(new File(gameRoot, "汉化补丁备份").exists());
        assertFalse(new File(gameRoot, "特典相关").exists());
    }

    @Test
    public void exposesExpectedOnlineCdnUrlBaseSizeAndStagingEviction() throws Exception {
        assertEquals("https://alicehub.000.moe/ixseal", OnlineInstaller.IXSEAL_CDN_URL);
        assertEquals(
                4729863037L,
                AppSettings.INSTANCE.defaultBaseGameSizeForUrl(OnlineInstaller.IXSEAL_CDN_URL)
        );
        assertEquals(
                2470174002L,
                AppSettings.INSTANCE.defaultBaseGameSizeForUrl(OnlineInstaller.ESCALAYER_REBOOT_CDN_URL)
        );

        File storage = tmp.newFolder("storage");
        File escStaging = new File(storage, ".online_base_staging_Escalayer");
        File rebootStaging = new File(storage, ".online_base_staging_EscalayerReboot");
        File ixsealStaging = new File(storage, ".online_base_staging_Ixseal");
        assertTrue(escStaging.mkdirs());
        assertTrue(rebootStaging.mkdirs());
        assertTrue(ixsealStaging.mkdirs());

        List<File> evictedIx = OnlineInstaller.INSTANCE.evictBaseStagingToTrash(storage, GameCatalog.ID_IXSEAL, null);
        assertEquals(1, evictedIx.size());
        assertFalse(ixsealStaging.exists());
        assertTrue(rebootStaging.exists());
        assertTrue(escStaging.exists());
    }

    @Test
    public void nativeGameImportDiscoversPatchCnInDirectAndContainerRoots() {
        java.util.Map<String, List<NativeGameImport.TreeEntry<String, String>>> directTree = new java.util.HashMap<>();
        directTree.put("root", Arrays.asList(
                new NativeGameImport.TreeEntry<>("root/ini", "AliceStart.ini", false, "root/ini"),
                new NativeGameImport.TreeEntry<>("root/ain", "Ixseal.ain", false, "root/ain"),
                new NativeGameImport.TreeEntry<>("root/patchCN", "patchCN", true, "root/patchCN")
        ));
        directTree.put("root/patchCN", Arrays.asList(
                new NativeGameImport.TreeEntry<>("root/patchCN/ain", "Ixseal.ain", false, "root/patchCN/ain"),
                new NativeGameImport.TreeEntry<>("root/patchCN/cg", "IxsealCG.afa", false, "root/patchCN/cg")
        ));
        NativeGameImport.DiscoveredImportSources<String, String> direct = NativeGameImport.INSTANCE.discoverImportSources(
                "root",
                directTree.get("root"),
                id -> directTree.getOrDefault(id, Collections.emptyList())
        );
        assertNotNull(direct.getPrimaryEntries());
        assertEquals(1, direct.getSourceTrees().size());
        assertEquals(1, direct.getIxsealPatchTrees().size());
        assertEquals("root/patchCN/ain", direct.getIxsealPatchTrees().get(0).get(0).getPayload());

        java.util.Map<String, List<NativeGameImport.TreeEntry<String, String>>> nestedPatchTree = new java.util.HashMap<>();
        nestedPatchTree.put("root", Arrays.asList(
                new NativeGameImport.TreeEntry<>("root/ini", "AliceStart.ini", false, "root/ini"),
                new NativeGameImport.TreeEntry<>("root/ain", "Ixseal.ain", false, "root/ain"),
                new NativeGameImport.TreeEntry<>("root/patchCN", "patchCN", true, "root/patchCN")
        ));
        nestedPatchTree.put("root/patchCN", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("root/patchCN/sub", "汉化补丁", true, "root/patchCN/sub")
        ));
        nestedPatchTree.put("root/patchCN/sub", Arrays.asList(
                new NativeGameImport.TreeEntry<>("root/patchCN/sub/ini", "AliceStart.ini", false, "root/patchCN/sub/ini"),
                new NativeGameImport.TreeEntry<>("root/patchCN/sub/ain", "Ixseal.ain", false, "root/patchCN/sub/ain")
        ));
        NativeGameImport.DiscoveredImportSources<String, String> nestedDirect = NativeGameImport.INSTANCE.discoverImportSources(
                "root",
                nestedPatchTree.get("root"),
                id -> nestedPatchTree.getOrDefault(id, Collections.emptyList())
        );
        assertEquals(1, nestedDirect.getIxsealPatchTrees().size());
        assertEquals("root/patchCN/sub/ini", nestedDirect.getIxsealPatchTrees().get(0).get(0).getPayload());

        java.util.Map<String, List<NativeGameImport.TreeEntry<String, String>>> containerTree = new java.util.HashMap<>();
        containerTree.put("container", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("container/Ixseal", "Ixseal", true, "container/Ixseal")
        ));
        containerTree.put("container/Ixseal", Arrays.asList(
                new NativeGameImport.TreeEntry<>("container/Ixseal/ini", "AliceStart.ini", false, "container/Ixseal/ini"),
                new NativeGameImport.TreeEntry<>("container/Ixseal/ain", "Ixseal.ain", false, "container/Ixseal/ain"),
                new NativeGameImport.TreeEntry<>("container/Ixseal/patchCN", "patchCN", true, "container/Ixseal/patchCN")
        ));
        containerTree.put("container/Ixseal/patchCN", Collections.singletonList(
                new NativeGameImport.TreeEntry<>("container/Ixseal/patchCN/ain", "Ixseal.ain", false, "container/Ixseal/patchCN/ain")
        ));
        NativeGameImport.DiscoveredImportSources<String, String> container = NativeGameImport.INSTANCE.discoverImportSources(
                "container",
                containerTree.get("container"),
                id -> containerTree.getOrDefault(id, Collections.emptyList())
        );
        assertNotNull(container.getPrimaryEntries());
        assertEquals(1, container.getSourceTrees().size());
        assertEquals(1, container.getIxsealPatchTrees().size());
        assertEquals("container/Ixseal/patchCN/ain", container.getIxsealPatchTrees().get(0).get(0).getPayload());
    }
}
