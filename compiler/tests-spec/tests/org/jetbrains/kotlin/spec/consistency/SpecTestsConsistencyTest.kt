/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.consistency

import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.SpecTestLinkedType
import org.jetbrains.kotlin.spec.utils.TestArea
import org.jetbrains.kotlin.spec.utils.spec.HtmlSpecLoader
import java.io.File
import kotlin.io.walkTopDown

enum class SectionTag(val level: Int) { h1(1), h2(2), h3(3), h4(4), h5(5) }

class SpecTestsConsistencyTest {
    fun test() {
        val spec = HtmlSpecLoader.loadSpec("0.1-85")

        TestArea.values().forEach {
            File("${GeneralConfiguration.TESTDATA_PATH}/${it.testDataPath}/${SpecTestLinkedType.LINKED.testDataPath}").walkTopDown().forEach {

            }
        }
    }
}
