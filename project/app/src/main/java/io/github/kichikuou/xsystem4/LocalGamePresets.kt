package io.github.kichikuou.xsystem4

import java.text.Normalizer

enum class PresetEngine { SYSTEM3, SYSTEM4 }

data class LocalGamePreset(
    val id: String,
    val titleRes: Int,
    val defaultDirName: String,
    val engine: PresetEngine,
    val japaneseTitle: String
)

object LocalGamePresets {
    val all: List<LocalGamePreset> = listOf(
        LocalGamePreset("rance04", R.string.game_local_rance04_title, "rance04", PresetEngine.SYSTEM3, "RanceIV －教団の遺産－"),
        LocalGamePreset("rance41", R.string.game_local_rance41_title, "rance41", PresetEngine.SYSTEM3, "ランス4.1"),
        LocalGamePreset("rance42", R.string.game_local_rance42_title, "rance42", PresetEngine.SYSTEM3, "ランス4.2"),
        LocalGamePreset("katsumi", R.string.game_local_katsumi_title, "katsumi", PresetEngine.SYSTEM3, "いけないかつみ先生"),
        LocalGamePreset("toushin2", R.string.game_local_toushin2_title, "toushin2", PresetEngine.SYSTEM3, "闘神都市II"),
        LocalGamePreset("kaerunyo", R.string.game_local_kaerunyo_title, "kaerunyo", PresetEngine.SYSTEM3, "かえるにょ・ぱにょ～ん"),
        LocalGamePreset("ikusa_miko", R.string.game_local_ikusa_miko_title, "ikusa_miko", PresetEngine.SYSTEM3, "戦巫女"),
        LocalGamePreset("alice456", R.string.game_local_alice456_title, "alice456", PresetEngine.SYSTEM3, "ALICEの館４・５・６"),
        LocalGamePreset("zero", R.string.game_local_zero_title, "zero", PresetEngine.SYSTEM3, "零式"),
        LocalGamePreset("atlach_nacha", R.string.game_local_atlach_nacha_title, "atlach_nacha", PresetEngine.SYSTEM3, "アトラク＝ナクア"),
        LocalGamePreset("koi_no_owari", R.string.game_local_koi_no_owari_title, "koi_no_owari", PresetEngine.SYSTEM3, "恋のおわり"),
        LocalGamePreset("oudou_yuusha", R.string.game_local_oudou_yuusha_title, "oudou_yuusha", PresetEngine.SYSTEM3, "王道勇者"),
        LocalGamePreset("ambivalenz", R.string.game_local_ambivalenz_title, "ambivalenz", PresetEngine.SYSTEM3, "AmbivalenZ"),
        LocalGamePreset("diabolique", R.string.game_local_diabolique_title, "diabolique", PresetEngine.SYSTEM3, "DiaboLiQuE"),
        LocalGamePreset("mugen_houyou", R.string.game_local_mugen_houyou_title, "mugen_houyou", PresetEngine.SYSTEM3, "夢幻泡影"),
        LocalGamePreset("pastel_chime", R.string.game_local_pastel_chime_title, "pastel_chime", PresetEngine.SYSTEM3, "ぱすてるチャイム"),
        LocalGamePreset("prostudent_good", R.string.game_local_prostudent_good_title, "prostudent_good", PresetEngine.SYSTEM3, "ぷろすちゅーでんとGood"),
        LocalGamePreset("mamorigami", R.string.game_local_mamorigami_title, "mamorigami", PresetEngine.SYSTEM3, "守り神様"),
        LocalGamePreset("mamatoto", R.string.game_local_mamatoto_title, "mamatoto", PresetEngine.SYSTEM3, "ママトト"),
        LocalGamePreset("hushaby_baby", R.string.game_local_hushaby_baby_title, "hushaby_baby", PresetEngine.SYSTEM3, "HUSHABY BABY"),
        LocalGamePreset("darcrows", R.string.game_local_darcrows_title, "darcrows", PresetEngine.SYSTEM3, "DARCROWS"),
        LocalGamePreset("kakure_zuki", R.string.game_local_kakure_zuki_title, "kakure_zuki", PresetEngine.SYSTEM3, "隠れ月"),
        LocalGamePreset("persiom", R.string.game_local_persiom_title, "persiom", PresetEngine.SYSTEM3, "PERSIOM"),
        LocalGamePreset("seein_ao", R.string.game_local_seein_ao_title, "seein_ao", PresetEngine.SYSTEM3, "SeeIn青"),
        LocalGamePreset("alice20", R.string.game_local_alice20_title, "alice20", PresetEngine.SYSTEM3, "20世紀アリス"),
        LocalGamePreset("only_you_recurss", R.string.game_local_only_you_recurss_title, "only_you_recurss", PresetEngine.SYSTEM3, "Only you ～リ・クルス～"),
        LocalGamePreset("oujisama1", R.string.game_local_oujisama1_title, "oujisama1", PresetEngine.SYSTEM3, "王子さまＬｖ１"),
        LocalGamePreset("oujisama15", R.string.game_local_oujisama15_title, "oujisama15", PresetEngine.SYSTEM3, "王子さまLV1.5"),
        LocalGamePreset("tsumamigui", R.string.game_local_tsumamigui_title, "tsumamigui", PresetEngine.SYSTEM3, "妻みぐい"),
        LocalGamePreset("rance5d", R.string.game_local_rance5d_title, "rance5d", PresetEngine.SYSTEM3, "ランス５Ｄ"),
        LocalGamePreset("ore_no_shita", R.string.game_local_ore_no_shita_title, "ore_no_shita", PresetEngine.SYSTEM3, "俺の下であがけ"),
        LocalGamePreset("rakuenkou", R.string.game_local_rakuenkou_title, "rakuenkou", PresetEngine.SYSTEM3, "楽園行"),
        LocalGamePreset("tsumamigui2", R.string.game_local_tsumamigui2_title, "tsumamigui2", PresetEngine.SYSTEM3, "妻みぐい２"),
        LocalGamePreset("shell_creil", R.string.game_local_shell_creil_title, "shell_creil", PresetEngine.SYSTEM3, "シェル・クレイル"),
        LocalGamePreset("night_demon", R.string.game_local_night_demon_title, "night_demon", PresetEngine.SYSTEM3, "ナイトデーモン"),
        LocalGamePreset("alice_cd", R.string.game_local_alice_cd_title, "alice_cd", PresetEngine.SYSTEM3, "アリスCD"),
        LocalGamePreset("dalk_gaiden", R.string.game_local_dalk_gaiden_title, "dalk_gaiden", PresetEngine.SYSTEM4, "DALK外伝"),
        LocalGamePreset("mamanyonyo", R.string.game_local_mamanyonyo_title, "mamanyonyo", PresetEngine.SYSTEM4, "ままにょにょ"),
        LocalGamePreset("daibanchou", R.string.game_local_daibanchou_title, "daibanchou", PresetEngine.SYSTEM4, "大番長 -Big Bang Age-"),
        LocalGamePreset("majo_no_shokuzai", R.string.game_local_majo_no_shokuzai_title, "majo_no_shokuzai", PresetEngine.SYSTEM4, "魔女の贖罪"),
        LocalGamePreset("manana", R.string.game_local_manana_title, "manana", PresetEngine.SYSTEM4, "ウルトラ魔法少女まなな"),
        LocalGamePreset("alice7", R.string.game_local_alice7_title, "alice7", PresetEngine.SYSTEM4, "ALICEの館7"),
        LocalGamePreset("pastel_continue", R.string.game_local_pastel_continue_title, "pastel_continue", PresetEngine.SYSTEM4, "ぱすてるチャイムContinue"),
        LocalGamePreset("pastel_cpp", R.string.game_local_pastel_cpp_title, "pastel_cpp", PresetEngine.SYSTEM4, "ぱすちゃC＋＋"),
        LocalGamePreset("yokubari_saboten", R.string.game_local_yokubari_saboten_title, "yokubari_saboten", PresetEngine.SYSTEM4, "よくばりサボテン"),
        LocalGamePreset("double_sensei", R.string.game_local_double_sensei_title, "double_sensei", PresetEngine.SYSTEM4, "だぶる先生らいふっ"),
        LocalGamePreset("ojousama", R.string.game_local_ojousama_title, "ojousama", PresetEngine.SYSTEM4, "お嬢様をいいなりにするゲーム"),
        LocalGamePreset("sengoku_three", R.string.game_local_sengoku_three_title, "sengoku_three", PresetEngine.SYSTEM4, "戦国ランス～三匹が斬ったり突いたり燃やしたり"),
        LocalGamePreset("alivez", R.string.game_local_alivez_title, "alivez", PresetEngine.SYSTEM4, "AliveZ"),
        LocalGamePreset("edoranze", R.string.game_local_edoranze_title, "edoranze", PresetEngine.SYSTEM4, "エドランゼ ～Come on!水戸姫様～"),
        LocalGamePreset("toushin3", R.string.game_local_toushin3_title, "toushin3", PresetEngine.SYSTEM4, "闘神都市III"),
        LocalGamePreset("vanish", R.string.game_local_vanish_title, "vanish", PresetEngine.SYSTEM4, "ばにしゅ！～おっぱいの消えた王国～"),
        LocalGamePreset("boku_hokenshitsu", R.string.game_local_boku_hokenshitsu_title, "boku_hokenshitsu", PresetEngine.SYSTEM4, "僕だけの保健室"),
        LocalGamePreset("momoiro_guardian", R.string.game_local_momoiro_guardian_title, "momoiro_guardian", PresetEngine.SYSTEM4, "ももいろガーディアン"),
        LocalGamePreset("alice2010", R.string.game_local_alice2010_title, "alice2010", PresetEngine.SYSTEM4, "Alice 2010"),
        LocalGamePreset("shamans_sanctuary", R.string.game_local_shamans_sanctuary_title, "shamans_sanctuary", PresetEngine.SYSTEM4, "しゃーまんず・さんくちゅあり－巫女の聖域－"),
        LocalGamePreset("daiteikoku", R.string.game_local_daiteikoku_title, "daiteikoku", PresetEngine.SYSTEM4, "大帝国"),
        LocalGamePreset("oyako_rankan", R.string.game_local_oyako_rankan_title, "oyako_rankan", PresetEngine.SYSTEM4, "母娘乱館"),
        LocalGamePreset("pastel3", R.string.game_local_pastel3_title, "pastel3", PresetEngine.SYSTEM4, "パステルチャイム3 バインドシーカー")
    )
    val byId: Map<String, LocalGamePreset> = all.associateBy { it.id }

    private fun normalizedTitle(title: String): String =
        Normalizer.normalize(title, Normalizer.Form.NFKC).trim().lowercase(java.util.Locale.ROOT)

    fun recognize(fileNames: Collection<String>, readSmallFile: ((String) -> ByteArray?)?): String? {
        if (readSmallFile == null) return null
        val engine = if (GameCatalog.isSystem3xGameFiles(fileNames)) PresetEngine.SYSTEM3 else PresetEngine.SYSTEM4
        val iniNames = if (engine == PresetEngine.SYSTEM3) listOf("System39.ini") else listOf("System40.ini", "AliceStart.ini")
        val matches = iniNames.mapNotNull { ini ->
            val name = fileNames.firstOrNull { it.equals(ini, true) } ?: return@mapNotNull null
            val raw = try { readSmallFile(name) } catch (_: Exception) { null } ?: return@mapNotNull null
            if (raw.size > 256 * 1024) return@mapNotNull null
            val title = System40Ini.parse(raw).gameName ?: return@mapNotNull null
            all.singleOrNull { it.engine == engine && normalizedTitle(it.japaneseTitle) == normalizedTitle(title) }?.id
        }.distinct()
        return matches.singleOrNull()
    }
}
