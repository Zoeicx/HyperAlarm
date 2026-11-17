plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Keep Kotlin 2.4.10 on the built-in Kotlin classpath for EzHookTool 1.3.0.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.lsparanoid) apply false
}

/**
 * Guards UI text that never reaches a translation file.
 *
 * Android lint only checks hardcoded text in `res/layout` (`HardcodedText`) and only reports
 * missing translations for the module that declares a string, so three kinds of mistake kept
 * slipping through this repository: preference titles/summaries written as literals in `res/xml`,
 * Chinese string literals in Java, and fork-base strings that exist in `values/` but not in every
 * locale.
 *
 * Nothing is grandfathered: one gap fails the build. There used to be a baseline file that froze the
 * inherited backlog, but the backlog is gone, and an always-empty ledger only hides what the task
 * actually enforces.
 *
 *   ./gradlew checkI18n
 */
tasks.register("checkI18n") {
    group = "verification"
    description = "Fails when visible UI text is not a translated string resource"

    val projectDir = rootProject.projectDir

    // Modules whose resources and sources ship in the APK.
    val modules = listOf(
        "app/src/main",
        "library/core/src/main",
        "library/common/src/main",
        "library/provision/src/main",
        "library/libhook/src/main",
    )
    // The clock process cannot load this module's resources, so its messages stay Chinese.
    val hookSide = "library/libhook/src/main/java/"
    // ROM detection compares against the labels Xiaomi puts into Build.VERSION fields.
    val javaAllowed = listOf("江南", "月色")

    doLast {
        val problems = sortedSetOf<String>()
        val cjk = Regex("[\\u3400-\\u9fff\\u3040-\\u30ff\\uac00-\\ud7af]")
        val xmlComment = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL))
        val textAttr = Regex(
            "(?<!tools:)android:(title|summary|text|hint|label|contentDescription|entries|entryValues)=\"([^\"]*)\""
        )
        val stringDef = Regex("""<string\b([^>]*)>(.*?)</string>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val javaLiteral = Regex("""\"((?:[^\"\\]|\\.)*)\"""")
        val localeTag = Regex("""<locale android:name="([^"]+)"""")

        fun rel(file: File) = file.relativeTo(projectDir).invariantSeparatorsPath
        fun files(root: File, vararg extensions: String): List<File> =
            root.walkTopDown().filter { it.isFile && it.extension in extensions }.toList()

        // 1) Visible text attributes that are literals instead of @string.
        for (module in modules) {
            val res = File(projectDir, "$module/res")
            if (!res.isDirectory) continue
            for (file in files(res, "xml")) {
                if (file.parentFile.name.startsWith("values")) continue
                val text = xmlComment.replace(file.readText(), "")
                for (match in textAttr.findAll(text)) {
                    val value = match.groupValues[2]
                    if (value.isEmpty() || value.startsWith("@") || value.startsWith("?") || value.contains("%s")) continue
                    if (!cjk.containsMatchIn(value)) continue
                    problems += "literal\t${rel(file)}\t${match.groupValues[1]}=$value"
                }
            }
        }
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml")
        if (manifest.isFile) {
            val text = xmlComment.replace(manifest.readText(), "")
            for (match in Regex("android:label=\"([^\"]*)\"").findAll(text)) {
                val value = match.groupValues[1]
                if (value.startsWith("@") || !cjk.containsMatchIn(value)) continue
                problems += "literal\tapp/src/main/AndroidManifest.xml\tlabel=$value"
            }
        }

        // 2) Chinese literals in Java/Kotlin that should have been resources.
        for (module in modules) {
            val src = File(projectDir, "$module/java")
            if (!src.isDirectory) continue
            for (file in files(src, "java", "kt")) {
                val path = rel(file)
                if (path.startsWith(hookSide)) continue
                file.readLines().forEachIndexed { index, rawLine ->
                    val line = rawLine.trim()
                    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) return@forEachIndexed
                    val code = rawLine.substringBefore("//")
                    for (match in javaLiteral.findAll(code)) {
                        val literal = match.groupValues[1]
                        if (!cjk.containsMatchIn(literal)) continue
                        if (javaAllowed.any { it in literal }) continue
                        problems += "java\t$path:${index + 1}\t$literal"
                    }
                }
            }
        }

        // 3) Strings this fork declares must exist in every locale from locales_config.xml.
        val localeTags = localeTag.findAll(
            File(projectDir, "app/src/main/res/xml/locales_config.xml").readText()
        ).map { it.groupValues[1] }.toList()
        val legacyLanguage = mapOf("id" to "in", "he" to "iw", "yi" to "ji")
        fun localeDir(tag: String): String {
            if (tag == "en") return "values"
            val language = tag.substringBefore('-')
            val region = tag.substringAfter('-', "")
            val dir = legacyLanguage[language] ?: language
            return if (region.isEmpty()) "values-$dir" else "values-$dir-r$region"
        }

        val base = mutableMapOf<String, String>()          // string name -> module that declares it
        val translated = mutableMapOf<String, MutableSet<String>>()
        for (module in modules) {
            val res = File(projectDir, "$module/res")
            if (!res.isDirectory) continue
            res.listFiles()?.sortedBy { it.name }?.forEach { entry ->
                if (!entry.isDirectory || !entry.name.startsWith("values")) return@forEach
                entry.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
                    for (match in stringDef.findAll(file.readText())) {
                        val attributes = match.groupValues[1]
                        if (attributes.contains("translatable=\"false\"")) continue
                        val name = Regex("name=\"([^\"]+)\"").find(attributes)?.groupValues?.get(1) ?: continue
                        if (entry.name == "values") base.putIfAbsent(name, module)
                        else translated.getOrPut(entry.name) { mutableSetOf() } += name
                    }
                }
            }
        }
        for (tag in localeTags) {
            val dir = localeDir(tag)
            if (dir == "values") continue
            val have = translated[dir] ?: emptySet()
            for (name in base.keys.sorted()) {
                // Every module, not just app: the fork base shipped 997 untranslated library strings
                // and this loop used to wave them through, which is why they were only noticed much
                // later. The backlog is empty now, so nothing here is grandfathered in.
                if (name !in have) problems += "translation\t$dir\t$name"
            }
        }

        // 4) A translation has to keep the default string's format arguments. A missing %1$s does not
        //    fail the build on its own - it shows up as StringFormatInvalid when the string is used, or
        //    as a silently wrong message (a version prompt without the version, two links pointing at
        //    the same URL).
        val defaultBodies = mutableMapOf<String, String>()      // name -> body from values/
        val ph = Regex("""%(?:[0-9]+\$)?[sd]""")
        val defaultFiles = mutableMapOf<String, MutableSet<String>>()
        for (module in modules) {
            val res = File(projectDir, "$module/res/values")
            if (!res.isDirectory) continue
            res.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
                for (match in stringDef.findAll(file.readText())) {
                    val attributes = match.groupValues[1]
                    if (attributes.contains("translatable=\"false\"")) continue
                    val name = Regex("name=\"([^\"]+)\"").find(attributes)?.groupValues?.get(1) ?: continue
                    defaultBodies.putIfAbsent(name, match.groupValues[2])
                }
            }
        }
        for (module in modules) {
            val res = File(projectDir, "$module/res")
            if (!res.isDirectory) continue
            res.listFiles()?.filter { it.isDirectory && it.name.startsWith("values-") }?.forEach { entry ->
                entry.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
                    for (match in stringDef.findAll(file.readText())) {
                        val attributes = match.groupValues[1]
                        if (attributes.contains("translatable=\"false\"")) continue
                        val name = Regex("name=\"([^\"]+)\"").find(attributes)?.groupValues?.get(1) ?: continue
                        val expected = defaultBodies[name] ?: continue
                        val want = ph.findAll(expected).map { it.value }.sorted().toList()
                        val have = ph.findAll(match.groupValues[2]).map { it.value }.sorted().toList()
                        if (want != have) {
                            problems += "placeholder	${rel(file)}	$name	want=$want have=$have"
                        }
                    }
                }
            }
        }

        // 5) 梗体中文（values-zh-rME）必须覆盖本 fork 声明的每一条字符串。它不是"翻译质量"
        //    问题：缺条目时界面会掉回英文默认值，梗体语言就变成了大杂烩。
        val memeHave = translated["values-zh-rME"] ?: emptySet()
        for (module in modules) {
            for (name in base.keys.sorted()) {
                if (base[name] != module) continue
                if (name !in memeHave) problems += "meme	$module	$name"
            }
        }

        problems.forEach { println("PROBLEM $it") }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "${problems.size} i18n problem(s): visible text needs @string, Java must not carry " +
                    "Chinese literals, and every string this fork declares needs all " +
                    "${localeTags.size} locales"
            )
        }
        println("i18n check clean")
    }
}
