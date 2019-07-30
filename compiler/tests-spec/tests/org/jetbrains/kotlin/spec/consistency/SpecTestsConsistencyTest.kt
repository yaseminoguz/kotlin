/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.consistency

import junit.framework.TestCase
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.SpecTestLinkedType
import org.jetbrains.kotlin.spec.utils.TestArea
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.parseLinkedSpecTest
import org.jetbrains.kotlin.spec.utils.spec.HtmlSpecLoader
import org.jetbrains.kotlin.spec.utils.spec.HtmlSpecSentencesMapBuilder
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.io.walkTopDown

@RunWith(JUnit3RunnerWithInners::class)
class SpecTestsConsistencyTest : TestCase() {
    fun testRun() {
        val spec = HtmlSpecLoader.loadSpec("0.1-85") ?: return
        val sentences = HtmlSpecSentencesMapBuilder.build(spec)

        TestArea.values().forEach {
            File("${GeneralConfiguration.TESTDATA_PATH}/${it.testDataPath}/${SpecTestLinkedType.LINKED.testDataPath}").walkTopDown()
                .forEach { file ->
                    if (file.isFile && file.extension == "kt") {
                        val test = parseLinkedSpecTest(file.canonicalPath, mapOf("main" to file.readText()))
                        val sectionsPath = setOf(*test.place.sections.toTypedArray(), test.place.paragraphNumber)
                        val sentenceNumber = test.place.sentenceNumber
                        val paragraphSentences = sentences[sectionsPath]
                        if (paragraphSentences != null && paragraphSentences.size >= sentenceNumber) {
                            println("Current")
                            println("In the latest version: ${paragraphSentences[sentenceNumber]}")
                        }
                    }
            }
        }
    }
}
