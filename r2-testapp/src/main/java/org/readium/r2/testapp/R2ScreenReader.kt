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

    private var utterances = mutableListOf<String>()
    private var utterancesProgression: Int = 0
    private var resourceLength: Int = -1
    private var progression: Double = 0.0

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
                    utterancesProgression += (utterances.first()).length

                    //When the current utterance is done spoken, we removed it from the current text
                    utterances.removeAt(0)

                    progression = (utterancesProgression.toDouble() / resourceLength.toDouble())

                    if (utterances.isEmpty()) {
                        stopReading()

                        //TODO
                        //Go to next resource


                        utterancesProgression = 0
                    }
                }

                override fun onStart(p0: String?) {
                    //nothing to do
                }

                override fun onError(p0: String?) {
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
        if (utterances.isEmpty() && !paused) {
            utterances = text as MutableList<String>
        }

        for (bitsOfText in utterances) {
            textToSpeech.speak(bitsOfText, TextToSpeech.QUEUE_ADD, null, "")
        }
    }

    fun pauseReading() {
        paused = true
        textToSpeech.stop()
    }

    fun resumeReading() {
        startReading(utterances)
    }

    fun stopReading() {
        paused = false
        textToSpeech.stop()
        utterances.clear()
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

        resourceLength = plainTextFromHTML.length

        return plainTextFromHTML.split(".")
    }



    fun isTTSSpeaking(): Boolean {
        return textToSpeech.isSpeaking
    }

    fun isNotPaused(): Boolean {
        return paused
    }

}