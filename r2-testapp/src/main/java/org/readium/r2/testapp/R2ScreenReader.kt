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
import android.speech.tts.UtteranceProgressListener
import android.text.Html
import android.widget.Toast
import org.readium.r2.shared.Publication
import java.net.URI
import java.net.URL
import java.util.*


/**
 * R2ScreenReader
 *
 * A basic screen reader based on Android's TextToSpeech
 *
 *
 */


class R2ScreenReader(private val context: Context, private val publication: Publication) {

    private var initialized = false
    private var paused = false

    private var fullText = mutableListOf<String>()

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

            //checking progression
            textToSpeech.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                override fun onDone(p0: String?) {
                    //When the current utterance is done spoken, we removed it from the current text
                    fullText.removeAt(0)

                    if (fullText.isEmpty()) {
                        stopReading()

                        //TODO
                        //Go to next resource
                    }
                }

                override fun onStart(p0: String?) {
                    //nothing to do
                }

                override fun onError(p0: String?) {
                    //TODO
                    //Even though it's deprecated, still needed to instantiate UtteranceProgressListener object
                }
            })

        } else {
            Toast.makeText(context.applicationContext, "There was an error with the TTS initialization", Toast.LENGTH_LONG).show()
        }
    }

    fun shutdown() {
        initialized = false

        stopReading()

        textToSpeech.shutdown()
    }


    fun startReading(text: List<String>) {
        if (fullText.isEmpty() && !paused) {
            fullText = text as MutableList<String>
        }

        for (bitsOfText in fullText) {
            textToSpeech.speak(bitsOfText, TextToSpeech.QUEUE_ADD, null, "")
        }
    }

    fun pauseReading() {
        paused = true
        textToSpeech.stop()
    }

    fun resumeReading() {
        startReading(fullText)
    }

    fun stopReading() {
        textToSpeech.stop()
        paused = false
        fullText.clear()
    }


    fun getUtterances(baseURL: String, epubName: String, resourceHref: String): List<String> {
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

        return plainTextFromHTML.split(".")
    }



    fun isTTSSpeaking(): Boolean {
        return textToSpeech.isSpeaking
    }

    fun isNotPaused(): Boolean {
        return paused
    }

}