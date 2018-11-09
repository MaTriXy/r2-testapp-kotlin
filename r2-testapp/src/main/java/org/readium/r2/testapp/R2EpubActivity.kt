/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.widget.TextView
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.readium.r2.navigator.BASE_URL
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Locations
import org.readium.r2.shared.LocatorText
import org.readium.r2.shared.drm.DRMModel
import java.net.URI

/**
 * R2EpubActivity : Extension of the R2EpubActivity() from navigator
 *
 * That Activity manage everything related to the menu
 *      ( Table of content, User Settings, Drm, Bookmarks )
 *
 */
class R2EpubActivity : R2EpubActivity() {

    // Provide access to the Bookmarks & Positions Databases
    private lateinit var bookmarksDB: BookmarksDatabase
    private lateinit var positionsDB: PositionsDatabase

    private lateinit var screenReader: R2ScreenReader
    private var ttsOn = false

    protected var drmModel: DRMModel? = null
    protected var menuDrm: MenuItem? = null
    protected var menuToc: MenuItem? = null
    protected var menuBmk: MenuItem? = null

    protected var menuAccessibility: SubMenu? = null
    protected var menuScreenReader: MenuItem? = null

    private var bookId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarksDB = BookmarksDatabase(this)
        positionsDB = PositionsDatabase(this)

        Handler().postDelayed({
            bookId = intent.getLongExtra("bookId", -1)
            if (intent.getSerializableExtra("drmModel") != null) {
                drmModel = intent.getSerializableExtra("drmModel") as DRMModel
                drmModel?.let {
                    runOnUiThread {
                        menuDrm?.isVisible = true
                    }
                } ?: run {
                    runOnUiThread {
                        menuDrm?.isVisible = false
                    }
                }
            }
        }, 100)

        val appearancePref = preferences.getInt("appearance", 0)
        val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
        val textColors = mutableListOf("#000000", "#000000", "#ffffff")
        resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        (resourcePager.focusedChild?.findViewById(org.readium.r2.navigator.R.id.book_title) as? TextView)?.setTextColor(Color.parseColor(textColors[appearancePref]))
        toggleActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(org.readium.r2.testapp.R.menu.menu_epub, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)

        menuAccessibility = menu?.findItem(R.id.accessibility)?.subMenu
        menuScreenReader = menuAccessibility?.findItem(R.id.screen_reader)

        menuDrm?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.toc), 0, 0, Gravity.END)
                return true
            }
            R.id.screen_reader -> {
                val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString()).toInt()
                val resourceHref = publication.spine[resourcePager.currentItem].href!!

                if (!ttsOn) {
                    ttsOn = true

                    screenReader.configureTTS()

                    if (URI(resourceHref).isAbsolute) {
                        screenReader.read(screenReader.getText("", "", resourceHref))
                    } else {
                        val text = screenReader.getText("$BASE_URL:$port/", epubName, resourceHref)
                        println("Texte : $text")
                        screenReader.read(text)
                    }
                    item.title = resources.getString(R.string.epubactivity_accessibility_screen_reader_stop)
                } else {
                    ttsOn = false
                    screenReader.stop()
                    item.title = resources.getString(R.string.epubactivity_accessibility_screen_reader_start)
                }

                return true
            }
            R.id.drm -> {
                startActivityForResult(intentFor<DRMManagementActivity>("drmModel" to drmModel), 1)
                return true
            }
            R.id.bookmark -> {
                val resourceIndex = resourcePager.currentItem.toLong()
                val resourceHref = publication.spine[resourcePager.currentItem].href!!
                val resourceTitle = publication.spine[resourcePager.currentItem].title?: ""
                val locations = Locations.fromJSON(JSONObject(preferences.getString("${publicationIdentifier}-documentLocations", "{}")))
                val currentPage = positionsDB.positions.getCurrentPage(bookId, resourceHref, locations.progression!!)

                val bookmark = Bookmark(
                        bookId,
                        publicationIdentifier,
                        resourceIndex,
                        resourceHref,
                        resourceTitle,
                        Locations(progression = locations.progression, position = currentPage),
                        LocatorText()
                )
                
                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    runOnUiThread {
                        toast("Bookmark added at page $currentPage")
                    }
                } ?:run {
                    runOnUiThread {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }

            else -> return false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getBooleanExtra("returned", false)) {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onPause() {
        screenReader.shutdown()

        super.onPause()
    }

    override fun onDestroy() {
        screenReader.shutdown()

        super.onDestroy()
    }

    override fun onResume() {
        screenReader = R2ScreenReader(this, publication)

        super.onResume()
    }

}