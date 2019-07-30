/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.spec

import org.jsoup.nodes.Element
import java.util.*

typealias Location = Set<String>
typealias SentencesByLocation = MutableMap<Location, MutableList<String>>

object HtmlSpecSentencesMapBuilder {
    enum class SectionTag(val level: Int) { h1(1), h2(2), h3(3), h4(4), h5(5) }

    private const val PARAGRAPH_SELECTORS = ".paragraph, dl, ul, ol"
    private const val SECTION_SELECTORS = "h1, h2, h3, h4, h5"

    fun build(spec: Element): SentencesByLocation {
        var paragraphCounter = 0
        val currentSectionsPath = Stack<Pair<SectionTag, String>>()
        val sentencesByLocation: SentencesByLocation = mutableMapOf()

        spec.select("$SECTION_SELECTORS, $PARAGRAPH_SELECTORS, .sentence").forEach { element ->
            when {
                element.`is`(SECTION_SELECTORS) -> {
                    val sectionTag = SectionTag.valueOf(element.tagName().toLowerCase())
                    while (!currentSectionsPath.empty() && currentSectionsPath.peek().first.level >= sectionTag.level) {
                        currentSectionsPath.pop()
                    }

                    currentSectionsPath.push(Pair(sectionTag, element.attr("id")))
                    paragraphCounter = 0
                }
                element.`is`(PARAGRAPH_SELECTORS) -> paragraphCounter++
                else -> {
                    val sentenceLocation = currentSectionsPath.map { it.second }.toMutableSet().apply { add(paragraphCounter.toString()) }
                    sentencesByLocation.putIfAbsent(sentenceLocation, mutableListOf())
                    sentencesByLocation[sentenceLocation]!!.add(element.text())
                }
            }
        }

        return sentencesByLocation
    }
}