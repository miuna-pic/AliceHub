package io.github.kichikuou.xsystem4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

import static org.junit.Assert.*;

public class LocalGamePresetsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void presetsAreUniqueLocalOnlyAndFollowExistingOrder() {
        List<String> original = Arrays.asList(
                "rance10", "dohnadohna", "evenicle", "evenicle2", "rance01", "rance02",
                "rance03", "rance06", "sengokurance", "rance08", "rance09", "kichikuou",
                "daiakuji", "escalayer", "escalayer_reboot", "galzoo", "ixseal", "haruka",
                "tsumashibori", "tsumamigui3");
        List<String> order = GameCatalog.builtInSelectOrder();
        assertEquals(original, order.subList(0, 20));
        assertEquals(59, LocalGamePresets.INSTANCE.getAll().size());
        assertEquals(79, order.size());
        assertEquals(order.size(), new HashSet<>(order).size());
        int index = 20;
        for (LocalGamePreset preset : LocalGamePresets.INSTANCE.getAll()) {
            assertEquals(preset.getId(), order.get(index++));
            assertFalse(original.contains(preset.getId()));
            GameDescriptor descriptor = GameCatalog.INSTANCE.descriptorForWithResolver(
                    preset.getId(), null, res -> "resolved:" + res);
            assertEquals("resolved:" + preset.getTitleRes(), descriptor.getTitle());
            assertEquals(preset.getId(), descriptor.getId());
            assertEquals(preset.getDefaultDirName(), descriptor.getDefaultDirName());
            assertEquals(R.drawable.ic_launcher, descriptor.getIconRes());
            assertNull(descriptor.getExpectedAinName());
            assertFalse(descriptor.getSupportsOnlineBaseInstall());
            assertNull(descriptor.getOnlineBaseCdnUrl());
            assertNull(descriptor.getEffectiveOverlayCdnUrl());
            assertFalse(descriptor.getSupportsOnlineMod());
            assertFalse(descriptor.getSupportsLocalMod());
            assertNull(GameSortMetadata.INSTANCE.forGame(preset.getId()));
        }
    }

    @Test public void everyPresetHasEnglishJapaneseAndSimplifiedChineseResources() throws Exception {
        for (String locale : Arrays.asList("values", "values-ja", "values-zh")) {
            File file = new File("src/main/res/" + locale + "/strings.xml");
            if (!file.isFile()) file = new File("app/src/main/res/" + locale + "/strings.xml");
            NodeList strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(file).getElementsByTagName("string");
            for (LocalGamePreset preset : LocalGamePresets.INSTANCE.getAll()) {
                String name = "game_local_" + preset.getId() + "_title";
                String value = null;
                for (int i = 0; i < strings.getLength(); i++) {
                    if (strings.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                        value = strings.item(i).getTextContent();
                    }
                }
                assertNotNull(locale + ":" + name, value);
                assertFalse(value.trim().isEmpty());
                if (locale.equals("values-ja")) assertEquals(preset.getJapaneseTitle(), value);
            }
        }
    }

    private File fixture(String ini, String title, Charset charset) throws Exception {
        File dir = temporary.newFolder();
        Files.write(new File(dir, ini).toPath(), ("GameName = \"" + title + "\"\r\n").getBytes(charset));
        return dir;
    }

    @Test public void exactIniTitlesRecognizeEveryPresetAndValidateImports() throws Exception {
        for (LocalGamePreset preset : LocalGamePresets.INSTANCE.getAll()) {
            List<String> inis = preset.getEngine() == PresetEngine.SYSTEM3
                    ? Arrays.asList("System39.ini") : Arrays.asList("System40.ini", "AliceStart.ini");
            for (String ini : inis) {
                File dir = fixture(ini, preset.getJapaneseTitle(), StandardCharsets.UTF_8);
                assertEquals(preset.getId(), GameCatalog.INSTANCE.classifyDirectory(dir, "unrelated", null, null));
                assertTrue(GameCatalog.INSTANCE.validateImportedGameDir(dir, null, preset.getId()));
            }
        }
    }

    @Test public void acceptsShiftJisAndWidthNormalization() throws Exception {
        File dir = fixture("sYsTeM39.InI", "闘神都市II", Charset.forName("Shift_JIS"));
        assertEquals("toushin2", GameCatalog.INSTANCE.classifyDirectory(dir, "unknown", null, null));
        dir = fixture("System40.ini", Normalizer.normalize("ぱすちゃC＋＋", Normalizer.Form.NFKC), StandardCharsets.UTF_8);
        assertEquals("pastel_cpp", GameCatalog.INSTANCE.classifyDirectory(dir, "unknown", null, null));
    }

    @Test public void emptyUnknownPartialWrongEngineAndConflictingTitlesStayCustom() throws Exception {
        assertEquals("custom:unknown", GameCatalog.INSTANCE.classifyDirectory(temporary.newFolder(), "unknown", null, null));
        for (String title : Arrays.asList("", "Unknown", "ままにょにょ sequel", "ままにょ", "闘神都市II")) {
            File dir = fixture("System40.ini", title, StandardCharsets.UTF_8);
            assertEquals(title, "custom:unknown", GameCatalog.INSTANCE.classifyDirectory(dir, "unknown", null, null));
        }
        File conflict = fixture("System40.ini", "ままにょにょ", StandardCharsets.UTF_8);
        Files.write(new File(conflict, "AliceStart.ini").toPath(), "GameName = \"DALK外伝\"".getBytes(StandardCharsets.UTF_8));
        assertEquals("custom:unknown", GameCatalog.INSTANCE.classifyDirectory(conflict, "unknown", null, null));
        assertNull(LocalGamePresets.INSTANCE.recognize(Arrays.asList("System40.ini"), name -> null));
        assertNull(LocalGamePresets.INSTANCE.recognize(Arrays.asList("System40.ini"), name -> { throw new IllegalStateException("unreadable"); }));
        assertNull(LocalGamePresets.INSTANCE.recognize(Arrays.asList("System40.ini"), name -> new byte[256 * 1024 + 1]));
        assertNull(LocalGamePresets.INSTANCE.recognize(Arrays.asList("System40.ini"), null));
    }

    @Test public void existingAssetRecognitionStillTakesPriority() throws Exception {
        File dir = fixture("System40.ini", "ままにょにょ", StandardCharsets.UTF_8);
        Files.write(new File(dir, "Rance10.ain").toPath(), new byte[0]);
        assertEquals(GameCatalog.ID_RANCE10, GameCatalog.INSTANCE.classifyDirectory(dir, "unknown", null, null));
    }
}
