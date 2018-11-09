/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.text.Html
import android.widget.Toast
import org.readium.r2.shared.Publication
import java.net.URI
import java.net.URL
import java.util.*


class R2ScreenReader(private val context: Context, private val publication: Publication) {

    private var initialized = false

    private val textToSpeech = TextToSpeech(context,
            TextToSpeech.OnInitListener { status ->
                initialized = (status != TextToSpeech.ERROR)
            })



    private fun isInitialized(): Boolean {
        return initialized
    }


    fun configureTTS() {
        if (isInitialized()) {
            val language = textToSpeech.setLanguage(Locale(publication.metadata.languages.firstOrNull()))

            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context.applicationContext, "There was an error with the TTS language, switching to EN-US", Toast.LENGTH_LONG).show()
                textToSpeech.language = Locale.US
            }
        } else {
            Toast.makeText(context.applicationContext, "There was an error with the TTS initialization", Toast.LENGTH_LONG).show()
        }
    }

    fun shutdown() {
        initialized = false
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    fun stop() {
        textToSpeech.stop()
    }


    fun read(text: String) {
        val textSplitted = text.split(".")

        for (bitsOfText in textSplitted) {
            textToSpeech.speak(bitsOfText, TextToSpeech.QUEUE_ADD, null, "")
        }
    }


    fun getText(baseURL: String, epubName: String, resourceHref: String): String {
        var plainTextFromHTML = ""

        val thread = Thread(Runnable {
            val resourceURL: URL
            val text: String?

            if (URI(resourceHref).isAbsolute) {
                resourceURL = URL(resourceHref)
            } else {
                resourceURL = URL(baseURL + epubName + resourceHref)
            }

            text = resourceURL.readText()
            plainTextFromHTML = Html.fromHtml(text).toString().replace("\n".toRegex(), "").trim { it <= ' ' }
            plainTextFromHTML = plainTextFromHTML.substring(plainTextFromHTML.indexOf('}') + 1)
        })

        thread.start()
        thread.join()

        return plainTextFromHTML
    }

}