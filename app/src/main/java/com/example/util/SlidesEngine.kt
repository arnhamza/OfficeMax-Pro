package com.example.util

import org.json.JSONArray
import org.json.JSONObject

enum class SlideLayout(val label: String) {
    TITLE_SLIDE("Title Slide"),
    BULLET_LIST("Bullet Points"),
    STAT_METRIC("Big Stat & KPI"),
    QUOTE("Quote / Focus"),
    TWO_COLUMN("Comparison / Split")
}

data class OfficeSlide(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String = "",
    val bulletPoints: List<String> = emptyList(),
    val layout: SlideLayout = SlideLayout.BULLET_LIST,
    val speakerNotes: String = "",
    val accentHex: String = "#D83B01", // PowerPoint Orange
    val statNumber: String = "",
    val statLabel: String = ""
)

object SlidesEngine {

    fun serializeSlides(slides: List<OfficeSlide>): String {
        val array = JSONArray()
        slides.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("subtitle", s.subtitle)
            obj.put("bullets", JSONArray(s.bulletPoints))
            obj.put("layout", s.layout.name)
            obj.put("speakerNotes", s.speakerNotes)
            obj.put("accentHex", s.accentHex)
            obj.put("statNumber", s.statNumber)
            obj.put("statLabel", s.statLabel)
            array.put(obj)
        }
        return array.toString()
    }

    fun parseSlides(jsonStr: String): List<OfficeSlide> {
        if (jsonStr.isBlank()) return defaultPitchDeck()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<OfficeSlide>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val bullets = mutableListOf<String>()
                val bArr = obj.optJSONArray("bullets")
                if (bArr != null) {
                    for (b in 0 until bArr.length()) {
                        bullets.add(bArr.getString(b))
                    }
                }
                list.add(
                    OfficeSlide(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "Untitled Slide"),
                        subtitle = obj.optString("subtitle", ""),
                        bulletPoints = bullets,
                        layout = try {
                            SlideLayout.valueOf(obj.optString("layout", SlideLayout.BULLET_LIST.name))
                        } catch (_: Exception) {
                            SlideLayout.BULLET_LIST
                        },
                        speakerNotes = obj.optString("speakerNotes", ""),
                        accentHex = obj.optString("accentHex", "#D83B01"),
                        statNumber = obj.optString("statNumber", ""),
                        statLabel = obj.optString("statLabel", "")
                    )
                )
            }
            if (list.isEmpty()) defaultPitchDeck() else list
        } catch (_: Exception) {
            defaultPitchDeck()
        }
    }

    fun defaultPitchDeck(): List<OfficeSlide> {
        return listOf(
            OfficeSlide(
                title = "OfficePro Suite 2026",
                subtitle = "Unified Mobile Productivity & Intelligence",
                layout = SlideLayout.TITLE_SLIDE,
                accentHex = "#185ABD",
                speakerNotes = "Welcome stakeholders and introduce our next-gen mobile office ecosystem."
            ),
            OfficeSlide(
                title = "Executive Summary",
                subtitle = "Core Value Proposition",
                bulletPoints = listOf(
                    "All-in-one suite: Word Docs, Excel Sheets, PowerPoint Slides, and PDF Studio",
                    "Integrated on-device OCR Scanner for instant document digitization",
                    "Direct access file manager with native device storage excess & fast export",
                    "Built-in office task manager linking documents to project deliverables"
                ),
                layout = SlideLayout.BULLET_LIST,
                accentHex = "#107C41",
                speakerNotes = "Highlight how uniting all 4 office pillars on Android saves user switching time."
            ),
            OfficeSlide(
                title = "Performance Metrics",
                subtitle = "Year-over-Year Mobile Document Processing",
                statNumber = "+240%",
                statLabel = "Increase in on-device document workflows and conversions",
                layout = SlideLayout.STAT_METRIC,
                accentHex = "#D83B01",
                speakerNotes = "Reference internal benchmark data comparing standalone single-purpose apps."
            ),
            OfficeSlide(
                title = "Vision Statement",
                subtitle = "Empowering productive work anywhere",
                bulletPoints = listOf(
                    "\"Productivity isn't about working longer; it's about seamless access to tools that work for you.\""
                ),
                layout = SlideLayout.QUOTE,
                accentHex = "#744DA9",
                speakerNotes = "Wrap up with visionary quote before Q&A."
            )
        )
    }
}
