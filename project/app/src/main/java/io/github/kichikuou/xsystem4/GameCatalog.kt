package io.github.kichikuou.xsystem4

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import java.io.File

data class GameDescriptor(
    val id: String,
    val title: String,
    val defaultDirName: String,
    val expectedAinName: String?,
    val iconRes: Int,
    val accentColor: Int,
    val accentBorderColor: Int,
    val iconFrameColor: Int,
    val supportsOnlineBaseInstall: Boolean,
    val onlineBaseCdnUrl: String?,
    val supportsOnlineMod: Boolean,
    val supportsLocalMod: Boolean,
    val isRance10: Boolean,
    val isCnPatch: Boolean = false,
    val overlayCdnBaseUrl: String? = null,
    val overlayCatalogRelPath: String = "files/mods/index.json"
) {
    val effectiveOverlayCdnUrl: String?
        get() = overlayCdnBaseUrl ?: onlineBaseCdnUrl

    val overlayKind: String
        get() = if (isCnPatch) InstalledModInfo.KIND_CN_PATCH else InstalledModInfo.KIND_MOD
}

data class GameSelectEntry(
    val descriptor: GameDescriptor,
    val items: List<Item>,
    val primaryItem: Item?
)

object GameCatalog {
    const val ID_RANCE10 = "rance10"
    const val ID_DOHNA_DOHNA = "dohnadohna"
    const val ID_EVENICLE = "evenicle"
    const val ID_EVENICLE2 = "evenicle2"
    const val ID_SENGOKU_RANCE = "sengokurance"
    const val ID_RANCE01 = "rance01"
    const val ID_RANCE02 = "rance02"
    const val ID_RANCE03 = "rance03"
    const val ID_RANCE06 = "rance06"
    const val ID_RANCE08 = "rance08"
    const val ID_RANCE09 = "rance09"
    const val ID_KICHIKUOU = "kichikuou"
    const val ID_DAIAKUJI = "daiakuji"
    const val ID_ESCALAYER = "escalayer"
    const val ID_ESCALAYER_REBOOT = "escalayer_reboot"
    const val ID_GALZOO = "galzoo"
    const val ID_IXSEAL = "ixseal"
    const val ID_HARUKA = "haruka"
    const val ID_TSUMASHIBORI = "tsumashibori"
    const val ID_TSUMAMIGUI3 = "tsumamigui3"
    const val PREFIX_CUSTOM = "custom:"
    const val GITHUB_URL = "https://github.com/TomorrowX6/AliceHub"

    fun isSystem3xGameFiles(fileNames: Collection<String>): Boolean {
        val hasSys4Ain = fileNames.any { it.endsWith(".ain", true) && !it.equals("System39.ain", true) }
        val hasSys4Ini = fileNames.any { it.equals("System40.ini", true) || it.equals("AliceStart.ini", true) }
        if (hasSys4Ain || hasSys4Ini) return false
        return fileNames.any {
            it.endsWith("sa.ald", true) ||
                it.equals("adisk.ald", true) ||
                it.equals("System39.ini", true) ||
                it.equals("System39.ain", true)
        }
    }

    fun isSystem3xGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isSystem3xGameFiles(files.map { it.name })
    }

    @JvmOverloads
    fun isEscalayerGameFiles(
        fileNames: Collection<String>,
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (!isSystem3xGameFiles(fileNames)) return false
        if (fileNames.any { it.equals("DOKI_SA.ALD", true) }) return true
        val iniName = fileNames.firstOrNull { it.equals("System39.ini", true) }
        if (iniName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(iniName)
                if (raw != null) {
                    val title = System40Ini.parse(raw).gameName?.lowercase() ?: ""
                    if (title.contains("エスカレイヤー") || title.contains("escalayer") || title.contains("超昂天使")) {
                        return true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun isEscalayerGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isEscalayerGameFiles(files.map { it.name }) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) f.readBytes() else null
        }
    }

    @JvmOverloads
    fun isKichikuouGameFiles(
        fileNames: Collection<String>,
        bgmFileNames: Collection<String> = emptyList(),
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (!isSystem3xGameFiles(fileNames)) return false
        if (fileNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("KichikuRanceSA.ALD", true) ||
                    it.equals("KichikuRanceGA.ald", true) ||
                    repaired.equals("鬼畜王SA.ALD", true) ||
                    it.equals("鬼畜王SA.ALD", true) ||
                    repaired.equals("鬼畜王GA.ALD", true) ||
                    it.equals("鬼畜王GA.ALD", true)
            }
        ) {
            return true
        }
        val playlistName = fileNames.firstOrNull { it.equals("playlist.txt", true) }
        if (playlistName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(playlistName)
                if (raw != null && String(raw, Charsets.ISO_8859_1).lowercase().contains("kichiku_")) {
                    return true
                }
            } catch (_: Throwable) {
            }
        }
        if (bgmFileNames.any { it.lowercase().startsWith("kichiku_") }) {
            return true
        }
        val iniName = fileNames.firstOrNull { it.equals("System39.ini", true) }
        if (iniName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(iniName)
                if (raw != null) {
                    val title = System40Ini.parse(raw).gameName?.lowercase() ?: ""
                    if (title.contains("鬼畜王") || title.contains("kichiku")) {
                        return true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun isKichikuouGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        val fileNames = files.map { it.name }
        if (!isSystem3xGameFiles(fileNames)) return false
        val bgmDir = dir.listFiles()?.firstOrNull { it.isDirectory && it.name.equals("bgm", true) }
        val bgmFileNames = bgmDir?.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()
        return isKichikuouGameFiles(fileNames, bgmFileNames) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) f.readBytes() else null
        }
    }

    private val DAIAKUJI_ALIAS_TOKENS = listOf(
        "daiakuji",
        "daiakuzi",
        "大悪司",
        "大恶司",
        "戭埆巌",
        "戝埆巌"
    )

    private val DAIAKUJI_ALD_NAMES = setOf(
        "大悪司sa.ald",
        "大悪司ga.ald",
        "大悪司wa.ald",
        "大悪司da.ald",
        "大恶司sa.ald",
        "大恶司ga.ald",
        "大恶司wa.ald",
        "大恶司da.ald",
        "daiakujisa.ald",
        "daiakujiga.ald",
        "daiakujiwa.ald",
        "daiakujida.ald",
        "daiakuji_sa.ald",
        "daiakuji_ga.ald",
        "daiakuzisa.ald",
        "daiakuziga.ald",
        "戭埆巌sa.ald",
        "戭埆巌ga.ald",
        "戭埆巌wa.ald",
        "戭埆巌da.ald",
        "戝埆巌sa.ald",
        "戝埆巌ga.ald",
        "戝埆巌wa.ald",
        "戝埆巌da.ald"
    )

    fun isDaiakujiAlias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        val rawLower = raw.lowercase()
        return DAIAKUJI_ALIAS_TOKENS.any { token ->
            repaired.contains(token) || rawLower.contains(token)
        }
    }

    @JvmOverloads
    fun isDaiakujiGameFiles(
        fileNames: Collection<String>,
        bgmFileNames: Collection<String> = emptyList(),
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (!isSystem3xGameFiles(fileNames)) return false
        if (fileNames.any {
                val lower = it.lowercase()
                val repairedLower = ArchiveExtractor.repairSjisAsGbkMojibake(it).lowercase()
                lower in DAIAKUJI_ALD_NAMES || repairedLower in DAIAKUJI_ALD_NAMES
            }
        ) {
            return true
        }
        if (bgmFileNames.any {
                val lower = it.lowercase()
                lower.startsWith("daiakuji_") || lower.startsWith("daiakuzi_")
            }
        ) {
            return true
        }
        if (readSmallFile != null) {
            for (listName in listOf("_inmm.ini", "playlist.txt")) {
                val matchName = fileNames.firstOrNull { it.equals(listName, true) } ?: continue
                try {
                    val raw = readSmallFile(matchName) ?: continue
                    val latin = String(raw, Charsets.ISO_8859_1).lowercase()
                    if (latin.contains("daiakuji_") || latin.contains("daiakuzi_")) {
                        return true
                    }
                    val decoded = ArchiveExtractor.decodeBestCjkText(raw)
                    if (isDaiakujiAlias(decoded)) {
                        return true
                    }
                } catch (_: Throwable) {
                }
            }
            val iniName = fileNames.firstOrNull { it.equals("System39.ini", true) }
            if (iniName != null) {
                try {
                    val raw = readSmallFile(iniName)
                    if (raw != null) {
                        val title = System40Ini.parse(raw).gameName ?: ""
                        if (isDaiakujiAlias(title)) {
                            return true
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
        return false
    }

    fun isDaiakujiGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        val fileNames = files.map { it.name }
        if (!isSystem3xGameFiles(fileNames)) return false
        val bgmDir = dir.listFiles()?.firstOrNull { it.isDirectory && it.name.equals("bgm", true) }
        val bgmFileNames = bgmDir?.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()
        return isDaiakujiGameFiles(fileNames, bgmFileNames) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) f.readBytes() else null
        }
    }

    /**
     * Classifies a game root strictly from its files (never directory/display names), returning
     * a built-in game ID if recognized or null for custom/unrecognized roots.
     */
    @JvmOverloads
    fun classifyGameRootFiles(
        fileNames: Collection<String>,
        bgmFileNames: Collection<String> = emptyList(),
        readSmallFile: ((String) -> ByteArray?)? = null
    ): String? {
        val ainNames = fileNames.filter { it.endsWith(".ain", ignoreCase = true) }
        if (ainNames.any { it.equals("Rance10.ain", ignoreCase = true) }) {
            return ID_RANCE10
        }
        if (ainNames.any { it.equals("dohnadohna.ain", ignoreCase = true) }) {
            return ID_DOHNA_DOHNA
        }
        if (ainNames.any { it.equals("Evenicle2.ain", ignoreCase = true) }) {
            return ID_EVENICLE2
        }
        if (ainNames.any { it.equals("Evenicle.ain", ignoreCase = true) }) {
            return ID_EVENICLE
        }
        if (ainNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("EscalayerReboot.ain", ignoreCase = true) ||
                    repaired.equals("EscalayerReboot.ain", ignoreCase = true) ||
                    it.equals("エスカレイヤー・リブート.ain", ignoreCase = true) ||
                    it.equals("エスカレイヤーリブート.ain", ignoreCase = true) ||
                    repaired.equals("エスカレイヤー・リブート.ain", ignoreCase = true) ||
                    repaired.equals("エスカレイヤーリブート.ain", ignoreCase = true)
            }
        ) {
            return ID_ESCALAYER_REBOOT
        }
        if (ainNames.any { it.equals("Rance01.ain", ignoreCase = true) } ||
            fileNames.any { it.startsWith("Rance01", ignoreCase = true) && it.endsWith(".ald", ignoreCase = true) }
        ) {
            return ID_RANCE01
        }
        if (ainNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("Rance02.ain", ignoreCase = true) ||
                    it.equals("ランス０２.ain", ignoreCase = true) ||
                    it.equals("ランス02.ain", ignoreCase = true) ||
                    repaired.equals("ランス０２.ain", ignoreCase = true) ||
                    repaired.equals("ランス02.ain", ignoreCase = true)
            } || fileNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.endsWith(".ald", ignoreCase = true) &&
                    (it.startsWith("ランス０２") || it.startsWith("ランス02") ||
                        repaired.startsWith("ランス０２") || repaired.startsWith("ランス02"))
            }
        ) {
            return ID_RANCE02
        }
        if (ainNames.any { it.equals("Rance03.ain", ignoreCase = true) }) {
            return ID_RANCE03
        }
        if (isRance06GameFiles(fileNames)) {
            return ID_RANCE06
        }
        if (isGalzooGameFiles(fileNames)) {
            return ID_GALZOO
        }
        if (isHarukaGameFiles(fileNames, readSmallFile)) {
            return ID_HARUKA
        }
        if (ainNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("Rance09.ain", ignoreCase = true) ||
                    it.equals("Rance9.ain", ignoreCase = true) ||
                    repaired.equals("Rance9.ain", ignoreCase = true)
            }
        ) {
            return ID_RANCE09
        }
        if (ainNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("Rance08.ain", ignoreCase = true) ||
                    it.equals("Rance8.ain", ignoreCase = true) ||
                    it.equals("RanceQuest.ain", ignoreCase = true) ||
                    it.contains("ランス・クエスト") ||
                    it.contains("儔儞僗丒僋僄僗僩") ||
                    repaired.contains("ランス・クエスト")
            }
        ) {
            return ID_RANCE08
        }
        if (ainNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("SengokuRance.ain", ignoreCase = true) ||
                    it.equals("AliceCN.ain", ignoreCase = true) ||
                    it.contains("愴崙儔儞僗") ||
                    it.contains("戦国ランス") ||
                    repaired.contains("戦国ランス")
            } || fileNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.endsWith("GA.ald", ignoreCase = true) &&
                    (it.contains("愴崙") || it.contains("戦国") || repaired.contains("戦国"))
            }
        ) {
            return ID_SENGOKU_RANCE
        }
        if (isIxsealGameFiles(fileNames, readSmallFile)) {
            return ID_IXSEAL
        }
        if (isTsumamigui3GameFiles(fileNames, readSmallFile)) {
            return ID_TSUMAMIGUI3
        }
        if (isTsumashiboriGameFiles(fileNames, readSmallFile)) {
            return ID_TSUMASHIBORI
        }
        if (isEscalayerGameFiles(fileNames, readSmallFile)) {
            return ID_ESCALAYER
        }
        if (isKichikuouGameFiles(fileNames, bgmFileNames, readSmallFile)) {
            return ID_KICHIKUOU
        }
        if (isDaiakujiGameFiles(fileNames, bgmFileNames, readSmallFile)) {
            return ID_DAIAKUJI
        }
        return LocalGamePresets.recognize(fileNames, readSmallFile)
    }

    fun classifyGameRootDir(gameDir: File?): String? {
        if (gameDir == null || !gameDir.isDirectory) return null
        val children = gameDir.listFiles() ?: return null
        val files = children.filter { it.isFile }
        val fileNames = files.map { it.name }
        val bgmFileNames = if (isSystem3xGameFiles(fileNames)) {
            children.firstOrNull { it.isDirectory && it.name.equals("bgm", true) }
                ?.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                .orEmpty()
        } else {
            emptyList()
        }
        return classifyGameRootFiles(fileNames, bgmFileNames) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) {
                try { f.readBytes() } catch (_: Throwable) { null }
            } else {
                null
            }
        }
    }

    @JvmOverloads
    fun matchesTargetGameFiles(
        targetGameId: String?,
        fileNames: Collection<String>,
        bgmFileNames: Collection<String> = emptyList(),
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (targetGameId.isNullOrBlank() || targetGameId.startsWith(PREFIX_CUSTOM)) {
            return true
        }
        return classifyGameRootFiles(fileNames, bgmFileNames, readSmallFile) == targetGameId
    }

    fun matchesTargetGame(targetGameId: String?, gameDir: File?): Boolean {
        if (targetGameId.isNullOrBlank() || targetGameId.startsWith(PREFIX_CUSTOM)) {
            return true
        }
        if (gameDir == null || !gameDir.isDirectory) return false
        return classifyGameRootDir(gameDir) == targetGameId
    }

    fun validateImportedGameDir(
        gameDir: File?,
        expectedAinName: String?,
        targetGameId: String?
    ): Boolean {
        if (gameDir == null || !gameDir.isDirectory) return false
        if (!expectedAinName.isNullOrBlank()) {
            val hasExpected = gameDir.listFiles()?.any {
                it.isFile && (
                    it.name.equals(expectedAinName, ignoreCase = true) ||
                        ArchiveExtractor.repairSjisAsGbkMojibake(it.name).equals(expectedAinName, ignoreCase = true)
                    )
            } == true
            if (!hasExpected) return false
        }
        return matchesTargetGame(targetGameId, gameDir)
    }

    private const val RUNNING_GAME_FILE = ".running_game_root"
    private val RANCE01_CN_ALIAS_REGEX = Regex("(?<![0-9０-９])兰斯0?1(?![0-9０-９])")
    private val RANCE02_CN_ALIAS_REGEX = Regex("(?<![0-9０-９])兰斯0?2(?![0-9０-９])")
    private val RANCE03_CN_ALIAS_REGEX = Regex("(?<![0-9０-９])兰斯0?3(?![0-9０-９])")
    private val RANCE06_CN_ALIAS_REGEX = Regex("(?<![0-9０-９])兰斯[0０]?[6６](?![0-9０-９])")
    private val RANCE06_JP_ALIAS_REGEX = Regex("(?<![0-9０-９])ランス[0０]?[6６](?![0-9０-９])")
    private val RANCE06_EN_ALIAS_REGEX = Regex("(?<![a-z])rance\\s*(?:0?6(?![0-9])|vi(?![a-z]))", RegexOption.IGNORE_CASE)
    private val RANCE06_ALD_REGEX = Regex("^(?:rance0?6|ランス[0０]?[6６])(?:|[a-z_].*)\\.ald$", RegexOption.IGNORE_CASE)
    private val RANCE06_SAVE_SLOT_REGEX = Regex("^Save[A-La-l]\\.asd$")
    private val RANCE08_CN_ALIAS_REGEX = Regex("(?<![0-9０-９])兰斯0?8(?![0-9０-９])")
    private val RANCE09_CN_ALIAS_REGEX = Regex("(?<![0-9０-９])兰斯0?9(?![0-9０-９])")
    private val ESCALAYER_R_JA_ALIAS_REGEX = Regex("エスカレイヤー[\\s・_-]*r(?![a-z0-9])")
    private val ESCALAYER_R_CN_ALIAS_REGEX = Regex("艾斯卡蕾雅[\\s・_-]*r(?![a-z0-9])")
    private val ESCALAYER_R_EN_ALIAS_REGEX = Regex("(?<![a-z0-9])escalayer[\\s._-]*r(?![a-z0-9])")
    private val GALZOO_EN_ALIAS_REGEX =
        Regex("(?<![a-z0-9])galzoo(?:[\\s._-]*(?:island|cn))?(?![a-z0-9])", RegexOption.IGNORE_CASE)
    private val GALZOO_ALD_REGEX =
        Regex("^(?:galzoo(?:island)?|galzooアイランド|ギャルズーアイランド)(?:|[a-z_].*)\\.ald$", RegexOption.IGNORE_CASE)
    private val GALZOO_SAVE_SLOT_REGEX =
        Regex("^(?:galzoo|ギャルズー).*\\.(?:asd|sav|rsm)$", RegexOption.IGNORE_CASE)
    private val HARUKA_EN_ALIAS_REGEX =
        Regex("(?<![a-z0-9])(?:beat\\s+blades\\s+haruka|choukou\\s+sennin\\s+haruka|haruka)(?![a-z0-9])", RegexOption.IGNORE_CASE)
    private val HARUKA_ALD_REGEX =
        Regex("^(?:超昂閃忍ハルカ|超昂闪忍遥|超昂閃忍遥|haruka)(?:|[a-z_].*)\\.ald$", RegexOption.IGNORE_CASE)
    private val HARUKA_SYSTEM_SAVE_REGEX =
        Regex("^(?:超昂閃忍ハルカ|超昂闪忍遥|超昂閃忍遥|beat\\s*blades\\s*haruka|choukou\\s*sennin\\s*haruka|haruka)(?:sf|sys|fn)\\.asd$", RegexOption.IGNORE_CASE)

    fun isHarukaSystemSaveFileName(fileName: String): Boolean {
        val raw = fileName.trim()
        if (raw.isEmpty()) return false
        val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(raw)
        return raw.equals("システム共通データ.asd", ignoreCase = true) ||
            repaired.equals("システム共通データ.asd", ignoreCase = true) ||
            HARUKA_SYSTEM_SAVE_REGEX.matches(raw) ||
            HARUKA_SYSTEM_SAVE_REGEX.matches(repaired)
    }

    private val TSUMASHIBORI_EN_ALIAS_REGEX =
        Regex("(?<![a-z0-9])ts?uma[\\s._-]*sh?ibori(?![a-z0-9])", RegexOption.IGNORE_CASE)
    private val TSUMASHIBORI_ALD_REGEX =
        Regex("^(?:妻しぼり|嵢偟傏傝|ts?umash?ibori)[a-z0-9_]*\\.ald$", RegexOption.IGNORE_CASE)
    private val TSUMAMIGUI3_EN_ALIAS_REGEX =
        Regex("(?<![a-z0-9])ts?uma[\\s._-]*migui[\\s._-]*(?:3|iii)(?![a-z0-9])", RegexOption.IGNORE_CASE)
    private val TSUMAMIGUI3_CJK_ALIAS_REGEX =
        Regex("(?:妻みぐい|ツマミグイ|つまみぐい|嵢傒偖偄|妻中蜜)[\\s_-]*[3３Ⅲ]")
    private val TSUMAMIGUI3_ASSET_REGEX =
        Regex("^ts?umamigui3(?:cg|flat|pact|sound|voice)\\.afa$", RegexOption.IGNORE_CASE)
    private val TSUMASHIBORI_SAVE_SLOT_REGEX =
        Regex("^(?:妻しぼり|嵢偟傏傝|ts?umash?ibori)(?:sys|\\d+)\\.asd$", RegexOption.IGNORE_CASE)

    fun isEscalayerRebootAlias(lowerText: String): Boolean {
        return lowerText.contains("escalayerreboot") ||
            lowerText.contains("escalayer reboot") ||
            lowerText.contains("escalayer_reboot") ||
            lowerText.contains("escalayer-reboot") ||
            lowerText.contains("escalayer・reboot") ||
            lowerText.contains("escalayerr") ||
            lowerText.contains("エスカレイヤー・リブート") ||
            lowerText.contains("エスカレイヤーリブート") ||
            lowerText.contains("エスカレイヤー リブート") ||
            lowerText.contains("リブート") ||
            ((lowerText.contains("超昂天使") || lowerText.contains("艾斯卡蕾雅") || lowerText.contains("escalayer")) &&
                (lowerText.contains("reboot") || lowerText.contains("重制") || lowerText.contains("重启"))) ||
            ESCALAYER_R_JA_ALIAS_REGEX.containsMatchIn(lowerText) ||
            ESCALAYER_R_CN_ALIAS_REGEX.containsMatchIn(lowerText) ||
            ESCALAYER_R_EN_ALIAS_REGEX.containsMatchIn(lowerText)
    }

    fun isIxsealAlias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val lower = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        if (lower.contains("escalayer") || lower.contains("超昂天使") || lower.contains("エスカレイヤー")) {
            return false
        }
        return lower.contains("ixseal") ||
            lower.contains("エクシール") ||
            lower.contains("超昂神騎") ||
            lower.contains("超昂神骑") ||
            lower.contains("beat valkyrie")
    }

    @JvmOverloads
    fun isIxsealGameFiles(
        fileNames: Collection<String>,
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (isSystem3xGameFiles(fileNames)) return false
        val sys4AinNames = fileNames.filter {
            it.endsWith(".ain", ignoreCase = true) && !it.equals("System39.ain", ignoreCase = true)
        }
        if (sys4AinNames.isEmpty()) return false
        if (sys4AinNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("Ixseal.ain", ignoreCase = true) ||
                    repaired.equals("Ixseal.ain", ignoreCase = true) ||
                    it.equals("エクシール.ain", ignoreCase = true) ||
                    repaired.equals("エクシール.ain", ignoreCase = true) ||
                    it.equals("超昂神騎エクシール.ain", ignoreCase = true) ||
                    repaired.equals("超昂神騎エクシール.ain", ignoreCase = true) ||
                    it.equals("超昂神骑.ain", ignoreCase = true) ||
                    repaired.equals("超昂神骑.ain", ignoreCase = true) ||
                    it.equals("超昂神騎.ain", ignoreCase = true) ||
                    repaired.equals("超昂神騎.ain", ignoreCase = true)
            }
        ) {
            return true
        }
        if (fileNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("IxsealCG.afa", ignoreCase = true) ||
                    repaired.equals("IxsealCG.afa", ignoreCase = true) ||
                    it.equals("IxsealFont.fnl", ignoreCase = true) ||
                    it.equals("IxsealVoice.afa", ignoreCase = true) ||
                    it.equals("IxsealFlat.afa", ignoreCase = true)
            }
        ) {
            return true
        }
        val iniName = fileNames.firstOrNull { it.equals("AliceStart.ini", ignoreCase = true) }
        if (iniName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(iniName)
                if (raw != null) {
                    val parsed = System40Ini.parse(raw)
                    val title = parsed.gameName?.trim().orEmpty()
                    if (title.isNotEmpty() && isIxsealAlias(title)) {
                        return true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun isIxsealGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isIxsealGameFiles(files.map { it.name }) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) {
                try { f.readBytes() } catch (_: Throwable) { null }
            } else {
                null
            }
        }
    }

    fun isHarukaAlias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val lower = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        val rawLower = raw.lowercase()
        if (lower.contains("escalayer") || lower.contains("超昂天使") || lower.contains("エスカレイヤー") ||
            lower.contains("ixseal") || lower.contains("超昂神騎") || lower.contains("超昂神骑") ||
            lower.contains("エクシール") || lower.contains("beat angel") || lower.contains("beat valkyrie")
        ) {
            return false
        }
        return HARUKA_EN_ALIAS_REGEX.containsMatchIn(lower) ||
            lower.contains("beatbladesharuka") ||
            lower.contains("choukousennin") ||
            lower.contains("超昂閃忍") ||
            lower.contains("超昂闪忍") ||
            lower.contains("閃忍ハルカ") ||
            lower.contains("闪忍遥") ||
            lower.contains("ハルカ") ||
            rawLower.contains("挻峍慚擡") ||
            rawLower.contains("僴儖僇")
    }

    @JvmOverloads
    fun isHarukaGameFiles(
        fileNames: Collection<String>,
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (isSystem3xGameFiles(fileNames)) return false
        val sys4AinNames = fileNames.filter {
            it.endsWith(".ain", ignoreCase = true) && !it.equals("System39.ain", ignoreCase = true)
        }
        if (sys4AinNames.isEmpty()) return false
        if (sys4AinNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("超昂閃忍ハルカ.ain", ignoreCase = true) ||
                    repaired.equals("超昂閃忍ハルカ.ain", ignoreCase = true) ||
                    it.equals("超昂闪忍遥.ain", ignoreCase = true) ||
                    repaired.equals("超昂闪忍遥.ain", ignoreCase = true) ||
                    it.equals("超昂閃忍遥.ain", ignoreCase = true) ||
                    repaired.equals("超昂閃忍遥.ain", ignoreCase = true) ||
                    it.equals("ハルカ.ain", ignoreCase = true) ||
                    repaired.equals("ハルカ.ain", ignoreCase = true) ||
                    it.equals("Haruka.ain", ignoreCase = true) ||
                    repaired.equals("Haruka.ain", ignoreCase = true) ||
                    it.equals("BeatBladesHaruka.ain", ignoreCase = true) ||
                    it.equals("ChoukouSenninHaruka.ain", ignoreCase = true)
            }
        ) {
            return true
        }
        if (fileNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("HarukaDll.inc", ignoreCase = true) ||
                    repaired.equals("HarukaDll.inc", ignoreCase = true) ||
                    HARUKA_ALD_REGEX.matches(it) ||
                    HARUKA_ALD_REGEX.matches(repaired)
            }
        ) {
            return true
        }
        val iniName = fileNames.firstOrNull {
            it.equals("System40.ini", ignoreCase = true) || it.equals("AliceStart.ini", ignoreCase = true)
        }
        if (iniName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(iniName)
                if (raw != null) {
                    val title = System40Ini.parse(raw).gameName?.trim().orEmpty()
                    if (title.isNotEmpty() && isHarukaAlias(title)) {
                        return true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun isHarukaGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isHarukaGameFiles(files.map { it.name }) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) {
                try { f.readBytes() } catch (_: Throwable) { null }
            } else {
                null
            }
        }
    }

    fun isEvenicle2Alias(lowerText: String): Boolean {
        return lowerText.contains("evenicle2") ||
            lowerText.contains("evenicle 2") ||
            lowerText.contains("evenicle_2") ||
            lowerText.contains("evenicle ii") ||
            lowerText.contains("evenicleii") ||
            lowerText.contains("evenicle ⅱ") ||
            lowerText.contains("evenicleⅱ") ||
            lowerText.contains("イブニクル2") ||
            lowerText.contains("イブニクル２") ||
            lowerText.contains("イブニクル 2") ||
            lowerText.contains("イブニクル ２") ||
            lowerText.contains("イブニクルⅱ") ||
            lowerText.contains("夏娃年代记2") ||
            lowerText.contains("夏娃年代记 2") ||
            lowerText.contains("夏娃年代记ii") ||
            lowerText.contains("夏娃年代记ⅱ") ||
            lowerText.contains("夏娃年代記2") ||
            lowerText.contains("夏娃2")
    }

    fun isRance06GameFiles(fileNames: Collection<String>): Boolean {
        if (isSystem3xGameFiles(fileNames)) return false
        val ainNames = fileNames.filter { it.endsWith(".ain", ignoreCase = true) }
        if (ainNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                it.equals("Rance6.ain", ignoreCase = true) ||
                    it.equals("Rance06.ain", ignoreCase = true) ||
                    it.equals("RanceVI.ain", ignoreCase = true) ||
                    it.equals("ランス6.ain", ignoreCase = true) ||
                    it.equals("ランス６.ain", ignoreCase = true) ||
                    it.equals("ランス06.ain", ignoreCase = true) ||
                    it.equals("ランス０６.ain", ignoreCase = true) ||
                    repaired.equals("Rance6.ain", ignoreCase = true) ||
                    repaired.equals("Rance06.ain", ignoreCase = true) ||
                    repaired.equals("ランス6.ain", ignoreCase = true) ||
                    repaired.equals("ランス６.ain", ignoreCase = true) ||
                    repaired.equals("ランス06.ain", ignoreCase = true) ||
                    repaired.equals("ランス０６.ain", ignoreCase = true) ||
                    it.contains("ゼス崩壊") ||
                    repaired.contains("ゼス崩壊") ||
                    it.contains("赛斯崩坏") ||
                    repaired.contains("赛斯崩坏")
            }
        ) {
            return true
        }
        return fileNames.any {
            val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
            it.equals("Rance6DLL.inc", ignoreCase = true) ||
                RANCE06_ALD_REGEX.matches(it) ||
                RANCE06_ALD_REGEX.matches(repaired)
        }
    }

    fun isRance06Alias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val lower = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        return RANCE06_EN_ALIAS_REGEX.containsMatchIn(lower) ||
            RANCE06_CN_ALIAS_REGEX.containsMatchIn(lower) ||
            RANCE06_JP_ALIAS_REGEX.containsMatchIn(lower) ||
            lower.contains("ゼス崩壊") ||
            lower.contains("赛斯崩坏") ||
            lower.contains("賽斯崩壞") ||
            lower.contains("collapse of zeth")
    }

    fun isGalzooGameFiles(fileNames: Collection<String>): Boolean {
        if (isSystem3xGameFiles(fileNames)) return false
        val ainNames = fileNames.filter { it.endsWith(".ain", ignoreCase = true) }
        val isGalzooAin = { rawAin: String ->
            val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(rawAin)
            rawAin.equals("GALZOO.ain", ignoreCase = true) ||
                repaired.equals("GALZOO.ain", ignoreCase = true) ||
                rawAin.equals("GALZOOIsland.ain", ignoreCase = true) ||
                repaired.equals("GALZOOIsland.ain", ignoreCase = true) ||
                rawAin.equals("GALZOO Island.ain", ignoreCase = true) ||
                rawAin.equals("GALZOO_Island.ain", ignoreCase = true) ||
                rawAin.equals("GALZOOアイランド.ain", ignoreCase = true) ||
                repaired.equals("GALZOOアイランド.ain", ignoreCase = true) ||
                rawAin.equals("ギャルズーアイランド.ain", ignoreCase = true) ||
                repaired.equals("ギャルズーアイランド.ain", ignoreCase = true)
        }
        if (ainNames.any(isGalzooAin)) {
            return true
        }
        val nonSpecialAins = ainNames.filter {
            !it.equals("System39.ain", ignoreCase = true) && !it.equals("AliceCN.ain", ignoreCase = true)
        }
        if (nonSpecialAins.isNotEmpty()) {
            return false
        }
        return fileNames.any {
            val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
            it.equals("GALZOODLL.inc", ignoreCase = true) ||
                GALZOO_ALD_REGEX.matches(it) ||
                GALZOO_ALD_REGEX.matches(repaired)
        }
    }

    fun isGalzooGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isGalzooGameFiles(files.map { it.name })
    }

    fun isGalzooAlias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val lower = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        return GALZOO_EN_ALIAS_REGEX.containsMatchIn(lower) ||
            lower.contains("galzooisland") ||
            lower.contains("galzooアイランド") ||
            lower.contains("galzoo アイランド") ||
            lower.contains("ギャルズー") ||
            lower.contains("galzoo岛") ||
            lower.contains("galzoo 岛") ||
            lower.contains("galzoo艾兰德") ||
            lower.contains("galzoo 艾兰德")
    }

    fun isTsumashiboriAlias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val rawLower = raw.lowercase()
        val repairedLower = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        val check: (String) -> Boolean = { s ->
            TSUMASHIBORI_EN_ALIAS_REGEX.containsMatchIn(s) ||
                s.contains("妻しぼり") ||
                s.contains("ツマシボリ") ||
                s.contains("つましぼり") ||
                s.contains("嵢偟傏傝") ||
                s.contains("妻管严") ||
                s.contains("妻管嚴") ||
                s.contains("妻榨") ||
                s.contains("妻搾") ||
                s.contains("\u69a8\u7cbe\u59bb") ||
                s.contains("\u643e\u7cbe\u59bb")
        }
        return check(repairedLower) || check(rawLower)
    }

    @JvmOverloads
    fun isTsumashiboriGameFiles(
        fileNames: Collection<String>,
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (isSystem3xGameFiles(fileNames)) return false
        val ainNames = fileNames.filter { it.endsWith(".ain", ignoreCase = true) }
        val isTsumaAin = { rawAin: String ->
            val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(rawAin)
            rawAin.equals("妻しぼり.ain", ignoreCase = true) ||
                repaired.equals("妻しぼり.ain", ignoreCase = true) ||
                rawAin.equals("嵢偟傏傝.ain", ignoreCase = true) ||
                rawAin.equals("Tsumashibori.ain", ignoreCase = true) ||
                repaired.equals("Tsumashibori.ain", ignoreCase = true) ||
                rawAin.equals("Tsumasibori.ain", ignoreCase = true) ||
                rawAin.equals("Tumashibori.ain", ignoreCase = true) ||
                rawAin.equals("Tumasibori.ain", ignoreCase = true) ||
                rawAin.equals("ツマシボリ.ain", ignoreCase = true) ||
                repaired.equals("ツマシボリ.ain", ignoreCase = true) ||
                rawAin.equals("妻管严.ain", ignoreCase = true) ||
                repaired.equals("妻管严.ain", ignoreCase = true)
        }
        if (ainNames.any(isTsumaAin)) {
            return true
        }
        if (fileNames.any {
                val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it)
                TSUMASHIBORI_ALD_REGEX.matches(it) ||
                    TSUMASHIBORI_ALD_REGEX.matches(repaired) ||
                    it.equals("妻しぼりＯＰ.mpg", ignoreCase = true) ||
                    repaired.equals("妻しぼりＯＰ.mpg", ignoreCase = true) ||
                    it.equals("嵢偟傏傝俷俹.mpg", ignoreCase = true)
            }
        ) {
            val nonSpecialAins = ainNames.filter {
                !it.equals("System39.ain", ignoreCase = true) && !it.equals("AliceCN.ain", ignoreCase = true)
            }
            if (nonSpecialAins.isEmpty()) {
                return true
            }
        }
        val sys4Ains = ainNames.filter { !it.equals("System39.ain", ignoreCase = true) }
        val iniName = fileNames.firstOrNull {
            it.equals("System40.ini", ignoreCase = true) || it.equals("AliceStart.ini", ignoreCase = true)
        }
        if (sys4Ains.isNotEmpty() && iniName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(iniName)
                if (raw != null) {
                    val title = System40Ini.parse(raw).gameName?.trim().orEmpty()
                    if (title.isNotEmpty() && isTsumashiboriAlias(title)) {
                        return true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun isTsumashiboriGameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isTsumashiboriGameFiles(files.map { it.name }) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) {
                try { f.readBytes() } catch (_: Throwable) { null }
            } else {
                null
            }
        }
    }

    fun isTsumamigui3Alias(raw: String): Boolean {
        if (raw.isBlank()) return false
        val rawLower = raw.lowercase()
        val repairedLower = ArchiveExtractor.repairSjisAsGbkMojibake(raw).lowercase()
        val check: (String) -> Boolean = { s ->
            TSUMAMIGUI3_EN_ALIAS_REGEX.containsMatchIn(s) || TSUMAMIGUI3_CJK_ALIAS_REGEX.containsMatchIn(s)
        }
        return check(repairedLower) || check(rawLower)
    }

    @JvmOverloads
    fun isTsumamigui3GameFiles(
        fileNames: Collection<String>,
        readSmallFile: ((String) -> ByteArray?)? = null
    ): Boolean {
        if (isSystem3xGameFiles(fileNames)) return false
        val ainNames = fileNames.filter { it.endsWith(".ain", ignoreCase = true) }
        if (ainNames.any { it.equals("Tsumamigui3.ain", ignoreCase = true) || it.equals("Tumamigui3.ain", ignoreCase = true) }) {
            return true
        }
        if (fileNames.any { TSUMAMIGUI3_ASSET_REGEX.matches(it) }) {
            val otherAins = ainNames.filter { !it.equals("System39.ain", ignoreCase = true) }
            if (otherAins.isEmpty()) return true
        }
        val iniName = fileNames.firstOrNull {
            it.equals("System40.ini", ignoreCase = true) || it.equals("AliceStart.ini", ignoreCase = true)
        }
        if (ainNames.isNotEmpty() && iniName != null && readSmallFile != null) {
            try {
                val raw = readSmallFile(iniName)
                if (raw != null) {
                    val title = System40Ini.parse(raw).gameName?.trim().orEmpty()
                    if (title.isNotEmpty() && isTsumamigui3Alias(title)) {
                        return true
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return false
    }

    fun isTsumamigui3GameDir(dir: File?): Boolean {
        if (dir == null || !dir.isDirectory) return false
        val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
        return isTsumamigui3GameFiles(files.map { it.name }) { name ->
            val f = files.firstOrNull { it.name.equals(name, true) }
            if (f != null && f.length() <= 256L * 1024L) {
                try { f.readBytes() } catch (_: Throwable) { null }
            } else {
                null
            }
        }
    }

    /** Strips any trailing overlay badge tag ([MOD ...] / [汉化 ...]) from a game's display name. */
    fun cleanItemTitle(rawName: String): String {
        val cut = listOf(" [MOD", " [汉化")
            .map { rawName.indexOf(it) }
            .filter { it > 0 }
            .minOrNull()
        return if (cut != null) rawName.substring(0, cut) else rawName
    }

    fun classifyItem(item: Item): String {
        return classifyDirectory(
            gameDir = item.path,
            dirName = item.path.name,
            saveDir = item.savedir,
            displayName = item.name
        )
    }

    fun classifyDirectory(
        gameDir: File?,
        dirName: String,
        saveDir: File? = null,
        displayName: String? = null
    ): String {
        if (gameDir != null && gameDir.isDirectory) {
            val files = gameDir.listFiles()
            if (files != null) {
                classifyGameRootDir(gameDir)?.let { return it }
                val ainFiles = files.filter { it.isFile && it.name.endsWith(".ain", ignoreCase = true) }
                if (ainFiles.any { !it.name.equals("System39.ain", ignoreCase = true) }) {
                    return "$PREFIX_CUSTOM$dirName"
                }
                if (isSystem3xGameDir(gameDir)) {
                    return "$PREFIX_CUSTOM$dirName"
                }
            }
        }

        if (saveDir != null && saveDir.isDirectory) {
            if (saveDir.listFiles()?.any {
                    it.isFile && (
                        it.name.equals("Esca.sav", ignoreCase = true) ||
                            it.name.lowercase().startsWith("escalayerreboot_")
                        )
                } == true &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile
            ) {
                return ID_ESCALAYER_REBOOT
            }
            if (saveDir.listFiles()?.any {
                    it.isFile && (
                        it.name.equals("Ixseal_Extra.sav", ignoreCase = true) ||
                            it.name.lowercase().startsWith("ixseal_")
                        )
                } == true &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile
            ) {
                return ID_IXSEAL
            }
            if (saveDir.listFiles()?.any {
                    it.isFile && isHarukaSystemSaveFileName(it.name)
                } == true &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "CommonSystemData.sav").isFile &&
                !File(saveDir, "AFInfo.asd").isFile &&
                !File(saveDir, "AFConfig.asd").isFile &&
                !File(saveDir, "AFCommon.asd").isFile &&
                !File(saveDir, "3DSetting.asd").isFile &&
                !File(saveDir, "音楽モード.asd").isFile &&
                !File(saveDir, "壒妝儌乕僪.asd").isFile &&
                !File(saveDir, "顔ＣＧ").isDirectory &&
                !File(saveDir, "婄俠俧").isDirectory
            ) {
                return ID_HARUKA
            }
            if (saveDir.listFiles()?.any {
                    if (!it.isFile) return@any false
                    val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it.name)
                    TSUMASHIBORI_SAVE_SLOT_REGEX.matches(it.name) ||
                        TSUMASHIBORI_SAVE_SLOT_REGEX.matches(repaired)
                } == true &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile
            ) {
                return ID_TSUMASHIBORI
            }
            val saveFiles = saveDir.listFiles()?.filter { it.isFile }.orEmpty()
            val hasGlobalSave = saveFiles.any {
                it.name.equals("GlobalSave.asd", ignoreCase = true) ||
                    it.name.equals("GlobalSaveData.asd", ignoreCase = true)
            }
            val hasPrefixedE2Af = saveFiles.any { f ->
                val lowerRepaired = ArchiveExtractor.repairSjisAsGbkMojibake(f.name).lowercase()
                isEvenicle2Alias(lowerRepaired) && (
                    lowerRepaired.endsWith("afinfo.asd") ||
                        lowerRepaired.endsWith("afcgmode.asd") ||
                        lowerRepaired.endsWith("afbgmmode.asd") ||
                        lowerRepaired.endsWith("afmemmode.asd") ||
                        lowerRepaired.endsWith("afcommon.asd")
                    )
            }
            val hasE2AfMarker = saveFiles.any { f ->
                val lower = f.name.lowercase()
                lower.endsWith("afcommon.asd") ||
                    lower.endsWith("afconfig.asd") ||
                    lower.endsWith("afinfo.asd") ||
                    lower.endsWith("afcgmode.asd") ||
                    lower.endsWith("afbgmmode.asd") ||
                    lower.endsWith("afmemmode.asd")
            }
            val hasCommonOther = saveFiles.any { it.name.equals("CommonOtherData.sav", ignoreCase = true) }
            val has3DConfig = saveFiles.any { it.name.equals("3DConfig.vsf", ignoreCase = true) }
            if (!hasGlobalSave && (hasPrefixedE2Af || ((hasCommonOther || has3DConfig) && hasE2AfMarker))) {
                return ID_EVENICLE2
            }
            if (File(saveDir, "AFConfig.asd").isFile &&
                (File(saveDir, "AFCGMode.asd").isFile ||
                    File(saveDir, "Thumb0000.qnt").isFile ||
                    saveDir.listFiles()?.any { it.isFile && it.name.endsWith(".vsf", ignoreCase = true) } == true) &&
                !File(saveDir, "AFInfo.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "GlobalSave.asd").isFile
            ) {
                return ID_RANCE09
            }
            if ((File(saveDir, "ＣＧモード.asd").isFile ||
                    File(saveDir, "回想モード.asd").isFile ||
                    File(saveDir, "QC.asd").isFile) &&
                !File(saveDir, "共有.asd").isFile &&
                !File(saveDir, "嫟桳.asd").isFile &&
                !File(saveDir, "音楽モード.asd").isFile &&
                !File(saveDir, "壒妝儌乕僪.asd").isFile &&
                !File(saveDir, "3DSetting.asd").isFile &&
                !File(saveDir, "顔ＣＧ").isDirectory &&
                !File(saveDir, "婄俠俧").isDirectory &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile
            ) {
                return ID_RANCE02
            }
            if ((File(saveDir, "共有.asd").isFile ||
                    File(saveDir, "嫟桳.asd").isFile ||
                    File(saveDir, "ＣＧモード.asd").isFile ||
                    File(saveDir, "俠俧儌乕僪.asd").isFile ||
                    File(saveDir, "音楽モード.asd").isFile ||
                    File(saveDir, "壒妝儌乕僪.asd").isFile ||
                    File(saveDir, "3DSetting.asd").isFile ||
                    File(saveDir, "顔ＣＧ").isDirectory ||
                    File(saveDir, "婄俠俧").isDirectory) &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile
            ) {
                return ID_RANCE08
            }
            if (File(saveDir, "CommonSystemData.sav").isFile &&
                (File(saveDir, "AFCommon.asd").isFile ||
                    File(saveDir, "AFCGMode.asd").isFile ||
                    File(saveDir, "AFBGMMode.asd").isFile) &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "AFInfo.asd").isFile &&
                !File(saveDir, "AFConfig.asd").isFile
            ) {
                return ID_RANCE01
            }
            if ((File(saveDir, "AFBGMMode.asd").isFile ||
                    File(saveDir, "AFCommon.asd").isFile ||
                    saveDir.listFiles()?.any {
                        it.isFile && it.name.startsWith("AFSaveData", ignoreCase = true) && it.name.endsWith(".asd", ignoreCase = true)
                    } == true) &&
                !File(saveDir, "CommonSystemData.sav").isFile &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "AFInfo.asd").isFile &&
                !File(saveDir, "AFConfig.asd").isFile
            ) {
                return ID_RANCE03
            }
            if ((File(saveDir, "Progress.asd").isFile ||
                    (File(saveDir, "Config.asd").isFile &&
                        (File(saveDir, "Joypad.asd").isFile ||
                            File(saveDir, "已读文本跳过.asd").isFile ||
                            File(saveDir, "既読スキップ.asd").isFile)) ||
                    saveDir.listFiles()?.any {
                        it.isFile && RANCE06_SAVE_SLOT_REGEX.matches(it.name)
                    } == true) &&
                !File(saveDir, "CommonSystemData.sav").isFile &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "AFInfo.asd").isFile &&
                !File(saveDir, "AFConfig.asd").isFile &&
                !File(saveDir, "AFCommon.asd").isFile
            ) {
                return ID_RANCE06
            }
            if ((File(saveDir, "keyfile.dat").isFile ||
                    File(saveDir, "MsgSkipFlag.sav").isFile ||
                    saveDir.listFiles()?.any {
                        if (!it.isFile) return@any false
                        val repaired = ArchiveExtractor.repairSjisAsGbkMojibake(it.name)
                        GALZOO_SAVE_SLOT_REGEX.matches(it.name) ||
                            GALZOO_SAVE_SLOT_REGEX.matches(repaired)
                    } == true) &&
                !File(saveDir, "Progress.asd").isFile &&
                !File(saveDir, "Config.asd").isFile &&
                !File(saveDir, "CommonSystemData.sav").isFile &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "AFInfo.asd").isFile &&
                !File(saveDir, "AFConfig.asd").isFile &&
                !File(saveDir, "AFCommon.asd").isFile
            ) {
                return ID_GALZOO
            }
            if ((File(saveDir, "key0104.dat").isFile ||
                    saveDir.listFiles()?.any {
                        it.isFile && it.name.endsWith("_Sys.ASD", ignoreCase = true)
                    } == true) &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile
            ) {
                return ID_SENGOKU_RANCE
            }
            if (File(saveDir, "CommonSystemData.sav").isFile &&
                !File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                !File(saveDir, "3DSetting.asd").isFile &&
                !File(saveDir, "共有.asd").isFile &&
                !File(saveDir, "嫟桳.asd").isFile &&
                !File(saveDir, "AFCommon.asd").isFile &&
                !File(saveDir, "AFCGMode.asd").isFile &&
                !File(saveDir, "AFBGMMode.asd").isFile &&
                !File(saveDir, "Esca.sav").isFile &&
                saveDir.listFiles()?.any { it.isFile && it.name.lowercase().startsWith("escalayerreboot_") } != true
            ) {
                return ID_EVENICLE
            }
            if ((File(saveDir, "GlobalSaveData.asd").isFile ||
                    File(saveDir, "AFInfo.asd").isFile ||
                    File(saveDir, "SaveData5000.asd").isFile) &&
                !File(saveDir, "GlobalSave.asd").isFile
            ) {
                return ID_DOHNA_DOHNA
            }
            if (File(saveDir, "GlobalSave.asd").isFile) {
                return ID_RANCE10
            }
            if (saveDir.listFiles()?.any {
                    it.isFile && it.name.lowercase().startsWith("doki_s") && it.name.lowercase().endsWith(".asd")
                } == true
            ) {
                return ID_ESCALAYER
            }
            if (!File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                saveDir.listFiles()?.any {
                    if (!it.isFile) return@any false
                    val lower = ArchiveExtractor.repairSjisAsGbkMojibake(it.name).lowercase()
                    (lower.startsWith("kichikurance") && (lower.endsWith(".asd") || lower.endsWith(".msgskip"))) ||
                        ((lower.startsWith("鬼畜王") || lower.startsWith("婼抺墹")) && lower.endsWith(".asd"))
                } == true
            ) {
                return ID_KICHIKUOU
            }
            if (!File(saveDir, "GlobalSave.asd").isFile &&
                !File(saveDir, "GlobalSaveData.asd").isFile &&
                saveDir.listFiles()?.any {
                    if (!it.isFile) return@any false
                    val lower = it.name.lowercase()
                    (lower.endsWith(".asd") || lower.endsWith(".msgskip")) && isDaiakujiAlias(it.name)
                } == true
            ) {
                return ID_DAIAKUJI
            }
        }

        val lowerDir = ArchiveExtractor.repairSjisAsGbkMojibake(dirName).lowercase()
        if (lowerDir == "rance10" || lowerDir.startsWith("rance10-") ||
            lowerDir.contains("rance 10") ||
            lowerDir.contains("兰斯10") || lowerDir.contains("兰斯１０") ||
            lowerDir.contains("ランス10") || lowerDir.contains("ランス１０")
        ) {
            return ID_RANCE10
        }
        if (lowerDir == "rance09" || lowerDir.startsWith("rance09-") ||
            lowerDir == "rance9" || lowerDir.startsWith("rance9-") ||
            lowerDir.contains("rance 09") || lowerDir.contains("rance 9") || lowerDir.contains("rance ix") ||
            RANCE09_CN_ALIAS_REGEX.containsMatchIn(lowerDir) || lowerDir.contains("ランス09") ||
            lowerDir.contains("ランス9") || lowerDir.contains("ヘルマン") || lowerDir.contains("赫尔曼")
        ) {
            return ID_RANCE09
        }
        if (lowerDir == "rance08" || lowerDir.startsWith("rance08-") ||
            lowerDir == "rance8" || lowerDir.startsWith("rance8-") ||
            lowerDir.contains("rance 08") || lowerDir.contains("rancequest") || lowerDir.contains("rance quest") ||
            RANCE08_CN_ALIAS_REGEX.containsMatchIn(lowerDir) || lowerDir.contains("ランス08") ||
            lowerDir.contains("ランス8") || lowerDir.contains("ランス・クエスト")
        ) {
            return ID_RANCE08
        }
        if (lowerDir == "rance01" || lowerDir.startsWith("rance01-") || lowerDir.contains("rance 01") ||
            RANCE01_CN_ALIAS_REGEX.containsMatchIn(lowerDir) || lowerDir.contains("ランス01") ||
            lowerDir.contains("ランス０１")) {
            return ID_RANCE01
        }
        if (lowerDir == "rance02" || lowerDir.startsWith("rance02-") || lowerDir.contains("rance 02") ||
            RANCE02_CN_ALIAS_REGEX.containsMatchIn(lowerDir) || lowerDir.contains("ランス02") ||
            lowerDir.contains("ランス０２")) {
            return ID_RANCE02
        }
        if (lowerDir == "rance03" || lowerDir.startsWith("rance03-") || lowerDir.contains("rance 03") ||
            RANCE03_CN_ALIAS_REGEX.containsMatchIn(lowerDir) || lowerDir.contains("ランス03")
        ) {
            return ID_RANCE03
        }
        if (isRance06Alias(lowerDir)) {
            return ID_RANCE06
        }
        if (isGalzooAlias(lowerDir)) {
            return ID_GALZOO
        }
        if (lowerDir == "sengokurance" || lowerDir.startsWith("sengokurance-") || lowerDir.contains("sengoku") ||
            lowerDir.contains("战国兰斯") || lowerDir.contains("戦国ランス") || lowerDir.contains("愴崙儔儞僗")
        ) {
            return ID_SENGOKU_RANCE
        }
        if (isEvenicle2Alias(lowerDir)) {
            return ID_EVENICLE2
        }
        if (lowerDir == "evenicle" || lowerDir.startsWith("evenicle-") || lowerDir.contains("イブニクル") || lowerDir.contains("夏娃")) {
            return ID_EVENICLE
        }
        if (lowerDir == "dohnadohna" || lowerDir.startsWith("dohnadohna-") || lowerDir.contains("多娜多娜")) {
            return ID_DOHNA_DOHNA
        }
        if (isEscalayerRebootAlias(lowerDir)) {
            return ID_ESCALAYER_REBOOT
        }
        if (lowerDir == "escalayer" || lowerDir.startsWith("escalayer-") || lowerDir.contains("escalayer") ||
            lowerDir.contains("超昂天使") || lowerDir.contains("エスカレイヤー")
        ) {
            return ID_ESCALAYER
        }
        if (isIxsealAlias(lowerDir)) {
            return ID_IXSEAL
        }
        if (isHarukaAlias(dirName)) {
            return ID_HARUKA
        }
        if (isTsumamigui3Alias(dirName)) {
            return ID_TSUMAMIGUI3
        }
        if (isTsumashiboriAlias(dirName)) {
            return ID_TSUMASHIBORI
        }
        if (lowerDir == "kichikurance" || lowerDir.startsWith("kichikurance-") ||
            lowerDir == "kichikuou" || lowerDir.startsWith("kichikuou-") ||
            lowerDir.contains("kichikuou") || lowerDir.contains("kichiku") ||
            lowerDir.contains("鬼畜王")
        ) {
            return ID_KICHIKUOU
        }
        if (isDaiakujiAlias(dirName)) {
            return ID_DAIAKUJI
        }

        val lowerDisp = displayName?.let { ArchiveExtractor.repairSjisAsGbkMojibake(it) }?.lowercase() ?: ""
        if (isEscalayerRebootAlias(lowerDisp)) {
            return ID_ESCALAYER_REBOOT
        }
        if (lowerDisp.contains("escalayer") || lowerDisp.contains("超昂天使") ||
            lowerDisp.contains("エスカレイヤー") || lowerDisp.contains("艾斯卡蕾雅")
        ) {
            return ID_ESCALAYER
        }
        if (isIxsealAlias(lowerDisp)) {
            return ID_IXSEAL
        }
        if (!displayName.isNullOrBlank() && isHarukaAlias(displayName)) {
            return ID_HARUKA
        }
        if (!displayName.isNullOrBlank() && isTsumamigui3Alias(displayName)) {
            return ID_TSUMAMIGUI3
        }
        if (!displayName.isNullOrBlank() && isTsumashiboriAlias(displayName)) {
            return ID_TSUMASHIBORI
        }
        if (lowerDisp.contains("kichikuou") || lowerDisp.contains("kichikurance") || lowerDisp.contains("鬼畜王")) {
            return ID_KICHIKUOU
        }
        if (!displayName.isNullOrBlank() && isDaiakujiAlias(displayName)) {
            return ID_DAIAKUJI
        }
        if (lowerDisp.contains("兰斯10") || lowerDisp.contains("兰斯１０") || lowerDisp.contains("ランス10") ||
            lowerDisp.contains("ランス１０") || lowerDisp.contains("rance10") || lowerDisp.contains("rance 10")
        ) {
            return ID_RANCE10
        }
        if (lowerDisp.contains("rance09") || lowerDisp.contains("rance 09") ||
            lowerDisp.contains("rance9") || lowerDisp.contains("rance 9") || lowerDisp.contains("rance ix") ||
            RANCE09_CN_ALIAS_REGEX.containsMatchIn(lowerDisp) || lowerDisp.contains("ランス09") ||
            lowerDisp.contains("ランス9") || lowerDisp.contains("ヘルマン") || lowerDisp.contains("赫尔曼")
        ) {
            return ID_RANCE09
        }
        if (lowerDisp.contains("rance08") || lowerDisp.contains("rance 08") ||
            lowerDisp.contains("rancequest") || lowerDisp.contains("rance quest") ||
            RANCE08_CN_ALIAS_REGEX.containsMatchIn(lowerDisp) || lowerDisp.contains("ランス08") ||
            lowerDisp.contains("ランス8") || lowerDisp.contains("ランス・クエスト") ||
            lowerDisp.contains("ランスクエスト") || lowerDisp.contains("マグナム") || lowerDisp.contains("magnum")
        ) {
            return ID_RANCE08
        }
        if (lowerDisp.contains("rance01") || lowerDisp.contains("rance 01") ||
            RANCE01_CN_ALIAS_REGEX.containsMatchIn(lowerDisp) || lowerDisp.contains("ランス01") ||
            lowerDisp.contains("ランス０１") ||
            lowerDisp.contains("光をもとめて") || lowerDisp.contains("寻找小光")) {
            return ID_RANCE01
        }
        if (lowerDisp.contains("rance02") || lowerDisp.contains("rance 02") ||
            RANCE02_CN_ALIAS_REGEX.containsMatchIn(lowerDisp) || lowerDisp.contains("ランス02") ||
            lowerDisp.contains("ランス０２") || lowerDisp.contains("反叛的少女") || lowerDisp.contains("反逆の少女")) {
            return ID_RANCE02
        }
        if (lowerDisp.contains("rance03") || lowerDisp.contains("rance 03") ||
            RANCE03_CN_ALIAS_REGEX.containsMatchIn(lowerDisp) || lowerDisp.contains("ランス03") ||
            lowerDisp.contains("リーザス") || lowerDisp.contains("利萨斯")
        ) {
            return ID_RANCE03
        }
        if (isRance06Alias(lowerDisp)) {
            return ID_RANCE06
        }
        if (isGalzooAlias(lowerDisp)) {
            return ID_GALZOO
        }
        if (lowerDisp.contains("sengoku") || lowerDisp.contains("战国兰斯") || lowerDisp.contains("戰國蘭斯") ||
            lowerDisp.contains("戦国ランス")
        ) {
            return ID_SENGOKU_RANCE
        }
        if (isEvenicle2Alias(lowerDisp)) {
            return ID_EVENICLE2
        }
        if (lowerDisp.contains("イブニクル") || lowerDisp.contains("evenicle") || lowerDisp.contains("夏娃")) {
            return ID_EVENICLE
        }
        if (lowerDisp.contains("多娜多娜") || lowerDisp.contains("ドーナドーナ") || lowerDisp.contains("dohna")) {
            return ID_DOHNA_DOHNA
        }

        return "$PREFIX_CUSTOM$dirName"
    }

    fun matchingItems(gameList: GameList, gameId: String): List<Item> {
        val result = ArrayList<Item>()
        for (i in 0 until gameList.size) {
            val item = gameList[i]
            if (classifyItem(item) == gameId) {
                result.add(item)
            }
        }
        result.sortBy { it.path.name }
        return result
    }

    fun descriptorFor(context: Context, gameId: String, gameList: GameList? = null): GameDescriptor =
        descriptorForWithResolver(gameId, gameList) { resId -> context.getString(resId) }

    @JvmOverloads
    fun descriptorForWithResolver(
        gameId: String,
        gameList: GameList? = null,
        stringResolver: (Int) -> String
    ): GameDescriptor {
        LocalGamePresets.byId[gameId]?.let { preset ->
            return GameDescriptor(
                id = preset.id,
                title = stringResolver(preset.titleRes),
                defaultDirName = preset.defaultDirName,
                expectedAinName = null,
                iconRes = R.drawable.ic_launcher,
                accentColor = 0xFF3F51B5.toInt(),
                accentBorderColor = 0x665C6BC0,
                iconFrameColor = 0xFF232733.toInt(),
                supportsOnlineBaseInstall = false,
                onlineBaseCdnUrl = null,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
        }
        return when (gameId) {
            ID_RANCE10 -> GameDescriptor(
                id = ID_RANCE10,
                title = stringResolver(R.string.game_rance10_title),
                defaultDirName = "Rance10",
                expectedAinName = "Rance10.ain",
                iconRes = R.drawable.ic_game_rance10,
                accentColor = 0xFFD84315.toInt(),
                accentBorderColor = 0x66FF7043,
                iconFrameColor = 0xFF221A18.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE10_CDN_URL,
                supportsOnlineMod = true,
                supportsLocalMod = true,
                isRance10 = true
            )
            ID_DOHNA_DOHNA -> GameDescriptor(
                id = ID_DOHNA_DOHNA,
                title = stringResolver(R.string.game_dohnadohna_title),
                defaultDirName = "dohnadohna",
                expectedAinName = "dohnadohna.ain",
                iconRes = R.drawable.ic_game_dohnadohna,
                accentColor = 0xFFE91E63.toInt(),
                accentBorderColor = 0x66FF4081,
                iconFrameColor = 0x00000000,
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.DOHNA_DOHNA_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_EVENICLE -> GameDescriptor(
                id = ID_EVENICLE,
                title = stringResolver(R.string.game_evenicle_title),
                defaultDirName = "Evenicle",
                expectedAinName = "Evenicle.ain",
                iconRes = R.drawable.ic_game_evenicle,
                accentColor = 0xFF1E88E5.toInt(),
                accentBorderColor = 0x6642A5F5,
                iconFrameColor = 0xFF151E2E.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.EVENICLE_CDN_URL,
                supportsOnlineMod = true,
                supportsLocalMod = true,
                isRance10 = false,
                isCnPatch = true,
                overlayCdnBaseUrl = OnlineInstaller.EVENICLE_CDN_URL,
                overlayCatalogRelPath = "files/cn-patch/index.json"
            )
            ID_EVENICLE2 -> GameDescriptor(
                id = ID_EVENICLE2,
                title = stringResolver(R.string.game_evenicle2_title),
                defaultDirName = "Evenicle2",
                expectedAinName = "Evenicle2.ain",
                iconRes = R.drawable.ic_game_evenicle2,
                accentColor = 0xFF00838F.toInt(),
                accentBorderColor = 0x6626C6DA,
                iconFrameColor = 0xFF0F2026.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.EVENICLE2_CDN_URL,
                supportsOnlineMod = true,
                supportsLocalMod = true,
                isRance10 = false,
                isCnPatch = true,
                overlayCdnBaseUrl = OnlineInstaller.EVENICLE2_CDN_URL,
                overlayCatalogRelPath = "files/cn-patch/index.json"
            )
            ID_SENGOKU_RANCE -> GameDescriptor(
                id = ID_SENGOKU_RANCE,
                title = stringResolver(R.string.game_sengokurance_title),
                defaultDirName = "SengokuRance",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_sengokurance,
                accentColor = 0xFFD84315.toInt(),
                accentBorderColor = 0x66FF7043,
                iconFrameColor = 0xFF1F1410.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.SENGOKU_RANCE_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_RANCE01 -> GameDescriptor(
                id = ID_RANCE01,
                title = stringResolver(R.string.game_rance01_title),
                defaultDirName = "Rance01",
                expectedAinName = "Rance01.ain",
                iconRes = R.drawable.ic_game_rance01,
                accentColor = 0xFFD99B26.toInt(),
                accentBorderColor = 0x66F5C451,
                iconFrameColor = 0xFF211A0E.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE01_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_RANCE02 -> GameDescriptor(
                id = ID_RANCE02,
                title = stringResolver(R.string.game_rance02_title),
                defaultDirName = "Rance02",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_rance02,
                accentColor = 0xFF3FA67A.toInt(),
                accentBorderColor = 0x666BD4A6,
                iconFrameColor = 0xFF102018.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE02_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_RANCE03 -> GameDescriptor(
                id = ID_RANCE03,
                title = stringResolver(R.string.game_rance03_title),
                defaultDirName = "Rance03",
                expectedAinName = "Rance03.ain",
                iconRes = R.drawable.ic_game_rance03,
                accentColor = 0xFFC62828.toInt(),
                accentBorderColor = 0x66EF5350,
                iconFrameColor = 0xFF1D1012.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE03_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_RANCE06 -> GameDescriptor(
                id = ID_RANCE06,
                title = stringResolver(R.string.game_rance06_title),
                defaultDirName = "Rance06",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_rance06,
                accentColor = 0xFF00796B.toInt(),
                accentBorderColor = 0x664DB6AC,
                iconFrameColor = 0xFF0E1F1D.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE06_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_RANCE08 -> GameDescriptor(
                id = ID_RANCE08,
                title = stringResolver(R.string.game_rance08_title),
                defaultDirName = "Rance08",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_rance08,
                accentColor = 0xFF2E7D32.toInt(),
                accentBorderColor = 0x6666BB6A,
                iconFrameColor = 0xFF102014.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE08_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_RANCE09 -> GameDescriptor(
                id = ID_RANCE09,
                title = stringResolver(R.string.game_rance09_title),
                defaultDirName = "Rance09",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_rance09,
                accentColor = 0xFF1565C0.toInt(),
                accentBorderColor = 0x6642A5F5,
                iconFrameColor = 0xFF101928.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.RANCE09_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_ESCALAYER -> GameDescriptor(
                id = ID_ESCALAYER,
                title = stringResolver(R.string.game_escalayer_title),
                defaultDirName = "Escalayer",
                expectedAinName = "DOKI_SA.ALD",
                iconRes = R.drawable.ic_game_escalayer,
                accentColor = 0xFFC2185B.toInt(),
                accentBorderColor = 0x66F06292,
                iconFrameColor = 0xFF281224.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.ESCALAYER_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_ESCALAYER_REBOOT -> GameDescriptor(
                id = ID_ESCALAYER_REBOOT,
                title = stringResolver(R.string.game_escalayer_reboot_title),
                defaultDirName = "EscalayerReboot",
                expectedAinName = "EscalayerReboot.ain",
                iconRes = R.drawable.ic_game_escalayer_reboot,
                accentColor = 0xFF8E24AA.toInt(),
                accentBorderColor = 0x66BA68C8,
                iconFrameColor = 0xFF24122B.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.ESCALAYER_REBOOT_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_GALZOO -> GameDescriptor(
                id = ID_GALZOO,
                title = stringResolver(R.string.game_galzoo_title),
                defaultDirName = "GALZOO",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_galzoo,
                accentColor = 0xFF0288D1.toInt(),
                accentBorderColor = 0x664FC3F7,
                iconFrameColor = 0xFF0D2136.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.GALZOO_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_IXSEAL -> GameDescriptor(
                id = ID_IXSEAL,
                title = stringResolver(R.string.game_ixseal_title),
                defaultDirName = "Ixseal",
                expectedAinName = "Ixseal.ain",
                iconRes = R.drawable.ic_game_ixseal,
                accentColor = 0xFF0277BD.toInt(),
                accentBorderColor = 0x664FC3F7,
                iconFrameColor = 0xFF0E1B28.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.IXSEAL_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_HARUKA -> GameDescriptor(
                id = ID_HARUKA,
                title = stringResolver(R.string.game_haruka_title),
                defaultDirName = "Haruka",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_haruka,
                accentColor = 0xFFE08E00.toInt(),
                accentBorderColor = 0x66FFB74D,
                iconFrameColor = 0xFF211814.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.HARUKA_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_TSUMASHIBORI -> GameDescriptor(
                id = ID_TSUMASHIBORI,
                title = stringResolver(R.string.game_tsumashibori_title),
                defaultDirName = "Tsumashibori",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_tsumashibori,
                accentColor = 0xFFD81B60.toInt(),
                accentBorderColor = 0x66F06292,
                iconFrameColor = 0xFF28121B.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.TSUMASHIBORI_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_TSUMAMIGUI3 -> GameDescriptor(
                id = ID_TSUMAMIGUI3,
                title = stringResolver(R.string.game_tsumamigui3_title),
                defaultDirName = "Tsumamigui3",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_tsumamigui3,
                accentColor = 0xFFE65100.toInt(),
                accentBorderColor = 0x66FFB74D,
                iconFrameColor = 0xFF2A1A10.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.TSUMAMIGUI3_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_KICHIKUOU -> GameDescriptor(
                id = ID_KICHIKUOU,
                title = stringResolver(R.string.game_kichikuou_title),
                defaultDirName = "KichikuRance",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_kichikuou,
                accentColor = 0xFFB71C1C.toInt(),
                accentBorderColor = 0x66EF5350,
                iconFrameColor = 0xFF1E1111.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.KICHIKUOU_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            ID_DAIAKUJI -> GameDescriptor(
                id = ID_DAIAKUJI,
                title = stringResolver(R.string.game_daiakuji_title),
                defaultDirName = "Daiakuji",
                expectedAinName = null,
                iconRes = R.drawable.ic_game_daiakuji,
                accentColor = 0xFFC62828.toInt(),
                accentBorderColor = 0x66EF5350,
                iconFrameColor = 0xFF1E1010.toInt(),
                supportsOnlineBaseInstall = true,
                onlineBaseCdnUrl = OnlineInstaller.DAIAKUJI_CDN_URL,
                supportsOnlineMod = false,
                supportsLocalMod = false,
                isRance10 = false
            )
            else -> {
                val customDir = gameId.removePrefix(PREFIX_CUSTOM).ifBlank { "Game" }
                val matched = gameList?.let { list ->
                    (0 until list.size).map { list[it] }.firstOrNull { it.path.name == customDir }
                }
                val title = matched?.let { cleanItemTitle(it.name) } ?: customDir
                GameDescriptor(
                    id = "$PREFIX_CUSTOM$customDir",
                    title = title,
                    defaultDirName = customDir,
                    expectedAinName = null,
                    iconRes = R.drawable.ic_launcher,
                    accentColor = 0xFF3F51B5.toInt(),
                    accentBorderColor = 0x665C6BC0,
                    iconFrameColor = 0xFF232733.toInt(),
                    supportsOnlineBaseInstall = false,
                    onlineBaseCdnUrl = null,
                    supportsOnlineMod = false,
                    supportsLocalMod = false,
                    isRance10 = false
                )
            }
        }
    }

    @JvmStatic
    fun builtInSelectOrder(): List<String> = listOf(
        ID_RANCE10,
        ID_DOHNA_DOHNA,
        ID_EVENICLE,
        ID_EVENICLE2,
        ID_RANCE01,
        ID_RANCE02,
        ID_RANCE03,
        ID_RANCE06,
        ID_SENGOKU_RANCE,
        ID_RANCE08,
        ID_RANCE09,
        ID_KICHIKUOU,
        ID_DAIAKUJI,
        ID_ESCALAYER,
        ID_ESCALAYER_REBOOT,
        ID_GALZOO,
        ID_IXSEAL,
        ID_HARUKA,
        ID_TSUMASHIBORI,
        ID_TSUMAMIGUI3
    ) + LocalGamePresets.all.map { it.id }

    fun buildSelectEntries(context: Context, gameList: GameList): List<GameSelectEntry> {
        val entries = ArrayList<GameSelectEntry>()
        val builtInIds = builtInSelectOrder()
        val builtInSet = builtInIds.toHashSet()

        for (gid in builtInIds) {
            val desc = descriptorFor(context, gid, gameList)
            val matched = matchingItems(gameList, gid)
            entries.add(GameSelectEntry(desc, matched, matched.firstOrNull()))
        }

        for (i in 0 until gameList.size) {
            val item = gameList[i]
            val cid = classifyItem(item)
            if (cid !in builtInSet) {
                val desc = descriptorFor(context, cid, gameList)
                entries.add(GameSelectEntry(desc, listOf(item), item))
            }
        }
        return GameSorting.sorted(entries, AppSettings.getGameSortMode(context), AppSettings.resolveLocale(context))
    }

    fun writeRunningGameRoot(context: Context, gameRoot: String) {
        try {
            File(context.filesDir, RUNNING_GAME_FILE).writeText(gameRoot, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
    }

    fun clearRunningGameRoot(context: Context) {
        try {
            File(context.filesDir, RUNNING_GAME_FILE).delete()
        } catch (_: Throwable) {
        }
    }

    fun isGameProcessRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val targetProcName = "${context.packageName}:game"
        return try {
            am.runningAppProcesses?.any { it.processName == targetProcName } == true
        } catch (_: Throwable) {
            false
        }
    }

    fun showGameStopFailedToast(context: Context) {
        val msg = GamepadBridge.tr(
            context,
            "无法终止正在运行的游戏进程，请稍后重试",
            "実行中のゲームを終了できませんでした。しばらくしてから再試行してください",
            "Failed to stop running game process. Please try again."
        )
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun waitForGameProcessExit(
        activity: Activity,
        maxRetries: Int = 30,
        intervalMs: Long = 50L,
        onExited: () -> Unit,
        onTimeout: () -> Unit = { showGameStopFailedToast(activity) }
    ) {
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0
        val checkRunnable = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed) return
                if (!isGameProcessRunning(activity)) {
                    clearRunningGameRoot(activity)
                    onExited()
                    return
                }
                attempts++
                if (attempts >= maxRetries) {
                    onTimeout()
                } else {
                    handler.postDelayed(this, intervalMs)
                }
            }
        }
        if (!isGameProcessRunning(activity)) {
            clearRunningGameRoot(activity)
            onExited()
        } else {
            handler.postDelayed(checkRunnable, intervalMs)
        }
    }

    fun getRunningGameRoot(context: Context): String? {
        if (!isGameProcessRunning(context)) {
            clearRunningGameRoot(context)
            return null
        }
        return try {
            val f = File(context.filesDir, RUNNING_GAME_FILE)
            if (f.isFile) f.readText(Charsets.UTF_8).trim().takeIf { it.isNotEmpty() } else null
        } catch (_: Throwable) {
            null
        }
    }

    fun launchGameItem(activity: Activity, item: Item) {
        if (item.error != null) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.error)
                .setMessage(item.error)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }
        val saveDir = item.savedir ?: return
        val targetRoot = item.path.path
        val running = isGameProcessRunning(activity)
        val runningRoot = if (running) getRunningGameRoot(activity) else null

        if (running && runningRoot != null && runningRoot != targetRoot) {
            val cleanName = cleanItemTitle(item.name)
            AlertDialog.Builder(activity)
                .setTitle(R.string.switch_running_game_title)
                .setMessage(activity.getString(R.string.switch_running_game_msg, cleanName))
                .setPositiveButton(R.string.switch_running_game_confirm) { _, _ ->
                    killRunningGameProcess(activity)
                    waitForGameProcessExit(
                        activity = activity,
                        onExited = {
                            startXSystem4Activity(activity, targetRoot, saveDir.path)
                        }
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        if (running && runningRoot == null) {
            // Previous :game process is in final teardown; wait until it fully exits before launching.
            waitForGameProcessExit(
                activity = activity,
                onExited = {
                    startXSystem4Activity(activity, targetRoot, saveDir.path)
                }
            )
            return
        }

        startXSystem4Activity(activity, targetRoot, saveDir.path)
    }

    fun killRunningGameProcess(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val targetProcName = "${context.packageName}:game"
        try {
            am?.runningAppProcesses?.firstOrNull { it.processName == targetProcName }?.let { proc ->
                Process.killProcess(proc.pid)
            }
        } catch (_: Throwable) {
        }
        clearRunningGameRoot(context)
    }

    /** Shows the standard local-game import chooser for an archive/image or a directory. */
    fun showLocalGameImportDialog(
        activity: Activity,
        titleRes: Int,
        archiveReqCode: Int,
        dirReqCode: Int
    ) {
        val options = arrayOf(
            activity.getString(R.string.game_opt_zip),
            activity.getString(R.string.game_opt_dir)
        )
        AlertDialog.Builder(activity)
            .setTitle(titleRes)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        activity.startActivityForResult(
                            Intent.createChooser(i, activity.getString(R.string.choose_a_file)),
                            archiveReqCode
                        )
                    }
                    1 -> {
                        activity.startActivityForResult(
                            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            dirReqCode
                        )
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startXSystem4Activity(context: Context, gameRoot: String, saveDir: String) {
        val i = Intent().apply {
            setClass(context.applicationContext, XSystem4Activity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(XSystem4Activity.EXTRA_GAME_ROOT, gameRoot)
            putExtra(XSystem4Activity.EXTRA_SAVE_DIR, saveDir)
        }
        context.startActivity(i)
    }
}
