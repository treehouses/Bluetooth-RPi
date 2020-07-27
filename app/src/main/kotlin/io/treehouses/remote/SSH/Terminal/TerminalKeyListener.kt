/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2010 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.treehouses.remote.SSH.Terminal

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import android.text.ClipboardManager
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import io.treehouses.remote.Views.terminal.VDUBuffer
import io.treehouses.remote.Views.terminal.vt320
import io.treehouses.remote.PreferenceConstants
import io.treehouses.remote.SSH.beans.SelectionArea
import java.io.IOException

/**
 * @author kenny
 */
// for ClipboardManager
class TerminalKeyListener(private val manager: TerminalManager?,
                          private val bridge: TerminalBridge,
                          private val buffer: VDUBuffer,
                          private var encoding: String?) : View.OnKeyListener, OnSharedPreferenceChangeListener {
    private var keymode: String? = null
    private val deviceHasHardKeyboard: Boolean
    private var shiftedNumbersAreFKeysOnHardKeyboard = false
    private var controlNumbersAreFKeysOnSoftKeyboard = false
    private var rightModifiersAreSlashAndTab = false
    private var leftModifiersAreSlashAndTab = false
    private var derivedMetaState = 0
    private var volumeKeysChangeFontSize = false
    private var interpretAsHardKeyboard = false
    private var stickyMetas = 0
    private var uchar = 0
    var metaState = 0
        private set
    var deadKey = 0
        private set

    // TODO add support for the new API.
    private var clipboard: ClipboardManager? = null
    private var selectingForCopy = false
    private val selectionArea: SelectionArea = SelectionArea()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(manager)
    private fun specialKeys(keyCode: Int, left: Boolean, right: Boolean) : Boolean {
        // Ignore all key-up events except for the special keys

        val altRight = keyCode == KeyEvent.KEYCODE_ALT_RIGHT
        val altLeft = keyCode == KeyEvent.KEYCODE_ALT_LEFT
        val shiftRight = keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
        val shiftLeft = keyCode == KeyEvent.KEYCODE_SHIFT_LEFT

        return if((right && altRight || left && altLeft) && metaState and OUR_SLASH != 0) {
                metaState = metaState and OUR_TRANSIENT.inv()
                bridge.transport!!.write('/'.toInt())
                true
            }else if((right && shiftRight || left && shiftLeft) && metaState and OUR_TAB != 0){
                metaState = metaState and OUR_TRANSIENT.inv()
                bridge.transport!!.write(0x09)
                true
            } else false
    }

    private fun handleModifierKeyCode(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ALT_RIGHT -> metaState = metaState or OUR_SLASH
            KeyEvent.KEYCODE_SHIFT_RIGHT -> metaState = metaState or OUR_TAB
            KeyEvent.KEYCODE_SHIFT_LEFT -> metaPress(OUR_SHIFT_ON)
            KeyEvent.KEYCODE_ALT_LEFT -> metaPress(OUR_ALT_ON)
            else -> return false
        }
        return true
    }

    private fun handleModifierKeys(event: KeyEvent, keyCode: Int, flag: Boolean): Boolean {
        var newFlag = flag
        /// Handle alt and shift keys if they aren't repeating
        if (event.repeatCount == 0) {
            when {
                rightModifiersAreSlashAndTab -> newFlag = handleModifierKeyCode(keyCode)
                leftModifiersAreSlashAndTab -> newFlag = handleModifierKeyCode(keyCode)
                else -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> metaPress(OUR_ALT_ON)
                        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> metaPress(OUR_SHIFT_ON)
                        else -> newFlag = false
                    }
                }
            }
            if (!newFlag && (keyCode == KEYCODE_CTRL_LEFT || keyCode == KEYCODE_CTRL_RIGHT)) metaPress(OUR_CTRL_ON)
            else newFlag = false
        }
        return newFlag
    }

    private fun handleDpadCenter(keyCode: Int, flag: Boolean): Boolean {
        var newFlag = flag
        if (!newFlag && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (selectingForCopy) handleSelection()
            else {
                if (metaState and OUR_CTRL_ON != 0) {
                    sendEscape()
                    metaState = metaState and OUR_CTRL_ON.inv()
                } else metaPress(OUR_CTRL_ON, true)
            }
            bridge.redraw()
            newFlag = true
        }
        return newFlag
    }

    private fun handleSelection() {
        if (selectionArea.isSelectingOrigin) selectionArea.finishSelectingOrigin() else {
            if (clipboard != null) {
                // copy selected area to clipboard
                val copiedText = selectionArea.copyFrom(buffer)
                clipboard!!.text = copiedText
                // XXX STOPSHIP
//							manager.notifyUser(manager.getString(
//									R.string.console_copy_done,
//									copiedText.length()));
                selectingForCopy = false
                selectionArea.reset()
            }
        }
    }

    private fun setDerivedMetaState(event: KeyEvent) {
        derivedMetaState = event.metaState
        if (metaState and OUR_SHIFT_MASK != 0) derivedMetaState = derivedMetaState or KeyEvent.META_SHIFT_ON
        if (metaState and OUR_ALT_MASK != 0) derivedMetaState = derivedMetaState or KeyEvent.META_ALT_ON
        if (metaState and OUR_CTRL_MASK != 0) derivedMetaState = derivedMetaState or HC_META_CTRL_ON
        if (metaState and OUR_TRANSIENT != 0) {
            metaState = metaState and OUR_TRANSIENT.inv()
            bridge.redraw()
        }
    }

    private fun needReturn(interpretAsHardKeyboard: Boolean, keyCode: Int, event: KeyEvent): Boolean {
        setDerivedMetaState(event)
        var shouldReturn = false
        val shiftedNumbersAreFKeys = shiftedNumbersAreFKeysOnHardKeyboard && interpretAsHardKeyboard
        val controlNumbersAreFKeys = controlNumbersAreFKeysOnSoftKeyboard && !interpretAsHardKeyboard
        // Test for modified numbers becoming function keys
        if (shiftedNumbersAreFKeys && derivedMetaState and KeyEvent.META_SHIFT_ON != 0 && sendFunctionKey(keyCode)) shouldReturn = true
        else if (controlNumbersAreFKeys && derivedMetaState and HC_META_CTRL_ON != 0 && !shouldReturn && sendFunctionKey(keyCode)) shouldReturn = true
        else if (derivedMetaState and HC_META_CTRL_ON != 0 && derivedMetaState and KeyEvent.META_SHIFT_ON != 0) {
            if (keyCode == KeyEvent.KEYCODE_C) {
                bridge.copyCurrentSelection()
                shouldReturn = true
                // CTRL-SHIFT-V to paste.
            } else if (keyCode == KeyEvent.KEYCODE_V && clipboard!!.hasText()) {
                bridge.injectString(clipboard!!.text.toString())
                shouldReturn = true
            } else if (keyCode == KeyEvent.KEYCODE_EQUALS || keyCode == KeyEvent.KEYCODE_PLUS) {
                bridge.increaseFontSize()
                shouldReturn = true
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_MINUS && derivedMetaState and HC_META_CTRL_ON != 0) {
            bridge.decreaseFontSize()
            shouldReturn = true
        }
        return shouldReturn
    }

    private fun getUnicode(event: KeyEvent) {
        // Ask the system to use the keymap to give us the unicode character for this key,
        // with our derived modifier state applied.
        uchar = event.getUnicodeChar(derivedMetaState and HC_META_CTRL_MASK.inv())
        val ucharWithoutAlt = event.getUnicodeChar(
                derivedMetaState and (HC_META_ALT_MASK or HC_META_CTRL_MASK).inv())
        if (uchar == 0) {
            // Keymap doesn't know the key with alt on it, so just go with the unmodified version
            uchar = ucharWithoutAlt
        } else if (uchar != ucharWithoutAlt) {
            // The alt key was used to modify the character returned; therefore, drop the alt
            // modifier from the state so we don't end up sending alt+key.
            derivedMetaState = derivedMetaState and HC_META_ALT_MASK.inv()
        }
    }

    private fun hasNonCtrlChar(): Boolean {
        // If we have a defined non-control character
        if (uchar >= 0x20) {
            if (derivedMetaState and HC_META_CTRL_ON != 0) uchar = keyAsControl(uchar)
            if (derivedMetaState and KeyEvent.META_ALT_ON != 0) sendEscape()
            if (uchar < 0x80) bridge.transport!!.write(uchar) else  // TODO write encoding routine that doesn't allocate each time
                bridge.transport!!.write(String(Character.toChars(uchar))
                        .toByteArray(charset(encoding ?: "UTF-8")))
            return true
        }
        return false
    }

    private fun hasShift(keyCode: Int): Boolean {
        // Remove shift from the modifier state as it has already been used by getUnicodeChar.
        derivedMetaState = derivedMetaState and KeyEvent.META_SHIFT_ON.inv()
        if (uchar and KeyCharacterMap.COMBINING_ACCENT != 0) {
            deadKey = uchar and KeyCharacterMap.COMBINING_ACCENT_MASK
            return true
        }
        if (deadKey != 0) {
            uchar = KeyCharacterMap.getDeadChar(deadKey, keyCode)
            deadKey = 0
        }
        return false
    }

    private fun hasMultiCharInput(event: KeyEvent, keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN && event.action == KeyEvent.ACTION_MULTIPLE) {
            bridge.transport!!.write(event.characters.toByteArray(charset(encoding ?: "UTF-8")))
            return true
        }
        return false
    }

    private fun checkOptions() {
        interpretAsHardKeyboard = deviceHasHardKeyboard && !manager!!.hardKeyboardHidden
        rightModifiersAreSlashAndTab = interpretAsHardKeyboard && PreferenceConstants.KEYMODE_RIGHT == keymode
        leftModifiersAreSlashAndTab = interpretAsHardKeyboard && PreferenceConstants.KEYMODE_LEFT == keymode
    }

    /**
     * Handle onKey() events coming down from a [TerminalView] above us.
     * Modify the keys to make more sense to a host then pass it to the transport.
     */
    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        try {
            // skip keys if we aren't connected yet or have been disconnected
            if (bridge.isDisconnected || bridge.transport == null) return false
            checkOptions()
            if (event.action == KeyEvent.ACTION_UP) return specialKeys(keyCode, leftModifiersAreSlashAndTab, rightModifiersAreSlashAndTab)

            //Log.i("CBKeyDebug", KeyEventUtil.describeKeyEvent(keyCode, event));
            bridge.resetScrollPosition()

            // Handle potentially multi-character IME input.
            if (hasMultiCharInput(event, keyCode) || flagRaised(event, keyCode) || needReturn(interpretAsHardKeyboard, keyCode, event)) return true

            getUnicode(event)

            if (hasShift(keyCode) || hasNonCtrlChar() || hasKeyCode(keyCode)) return true
        } catch (e: IOException) {
            handleProblem(e, "Problem while trying to handle an onKey() event")
        } catch (npe: NullPointerException) {
            Log.d(TAG, "Input before connection established ignored.")
            return true
        }
        return false
    }

    private fun flagRaised(event: KeyEvent, keyCode: Int): Boolean {
        var flag = true
        flag = handleModifierKeys(event, keyCode, flag)
        flag = handleDpadCenter(keyCode, flag)
        return flag
    }

    private fun handleCamera() {
        // check to see which shortcut the camera button triggers
        when (manager!!.prefs!!.getString(PreferenceConstants.CAMERA, PreferenceConstants.CAMERA_CTRLA_SPACE)) {
            PreferenceConstants.CAMERA_CTRLA_SPACE -> {
                bridge.transport!!.write(0x01)
                bridge.transport!!.write(' '.toInt())
            }
            PreferenceConstants.CAMERA_CTRLA -> bridge.transport!!.write(0x01)
            PreferenceConstants.CAMERA_ESC -> (buffer as vt320).keyTyped(vt320.KEY_ESCAPE, ' ', 0)
            PreferenceConstants.CAMERA_ESC_A -> {
                sendEscape()
                bridge.transport!!.write('a'.toInt())
            }
        }
    }

    private fun handleDpad(direction: String, key: Int) {
        if (selectingForCopy) {
            when (direction) {
                "up" -> selectionArea.decrementRow()
                "down" -> selectionArea.incrementRow()
                "left" -> selectionArea.decrementColumn()
                "right" -> selectionArea.incrementColumn()
            }
            bridge.redraw()
        } else {
            sendPressedKey(key)
            bridge.tryKeyVibrate()
        }
    }

    private fun hasKeyCode(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> sendEscape()
            KeyEvent.KEYCODE_TAB -> bridge.transport!!.write(0x09)
            KeyEvent.KEYCODE_CAMERA -> handleCamera()
            KeyEvent.KEYCODE_DEL -> sendPressedKey(vt320.KEY_BACK_SPACE)
            KeyEvent.KEYCODE_ENTER -> (buffer as vt320).keyTyped(vt320.KEY_ENTER, ' ', 0)
            KeyEvent.KEYCODE_DPAD_LEFT -> handleDpad("left", vt320.KEY_LEFT)
            KeyEvent.KEYCODE_DPAD_UP -> handleDpad("up", vt320.KEY_UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> handleDpad("down", vt320.KEY_DOWN)
            KeyEvent.KEYCODE_DPAD_RIGHT -> handleDpad("right", vt320.KEY_RIGHT)
            KEYCODE_INSERT -> sendPressedKey(vt320.KEY_INSERT)
            KEYCODE_FORWARD_DEL -> sendPressedKey(vt320.KEY_DELETE)
            KEYCODE_MOVE_HOME -> sendPressedKey(vt320.KEY_HOME)
            KEYCODE_MOVE_END -> sendPressedKey(vt320.KEY_END)
            KEYCODE_PAGE_UP -> sendPressedKey(vt320.KEY_PAGE_UP)
            KEYCODE_PAGE_DOWN -> sendPressedKey(vt320.KEY_DOWN)
            else -> return false
        }
        return true
    }

    private fun keyAsControl(keyIn: Int): Int {
        // Support CTRL-a through CTRL-z
        var key = keyIn
        if (key in 0x61..0x7A) key -= 0x60 else if (key in 0x40..0x5F) key -= 0x40 else if (key == 0x20) key = 0x00 else if (key == 0x3F) key = 0x7F
        return key
    }

    fun sendEscape() {
        (buffer as vt320).keyTyped(vt320.KEY_ESCAPE, ' ', 0)
    }

    fun sendTab() {
        try {
            bridge.transport!!.write(0x09)
        } catch (e: IOException) {
            handleProblem(e, "Problem while trying to send TAB press.")
        }
    }

    fun handleProblem(e: IOException, message: String) {
        Log.e(TAG, message, e)
        try {
            bridge.transport!!.flush()
        } catch (ioe: IOException) {
            Log.d(TAG, "Our transport was closed, dispatching disconnect event")
            bridge.dispatchDisconnect(false)
        }
    }

    fun sendPressedKey(key: Int, modifier: Int = stateForBuffer) {
        (buffer as vt320).keyPressed(key, ' ', modifier)
    }

    /**
     * @param keyCode
     * @return successful
     */
    private fun sendFunctionKey(keyCode: Int): Boolean {
        var handled = true
        when (keyCode) {
            KeyEvent.KEYCODE_1 -> sendPressedKey(vt320.KEY_F1, 0)
            KeyEvent.KEYCODE_2 -> sendPressedKey(vt320.KEY_F2, 0)
            KeyEvent.KEYCODE_3 -> sendPressedKey(vt320.KEY_F3, 0)
            KeyEvent.KEYCODE_4 -> sendPressedKey(vt320.KEY_F4, 0)
            KeyEvent.KEYCODE_5 -> sendPressedKey(vt320.KEY_F5, 0)
            KeyEvent.KEYCODE_6 -> sendPressedKey(vt320.KEY_F6, 0)
            KeyEvent.KEYCODE_7 -> sendPressedKey(vt320.KEY_F7, 0)
            KeyEvent.KEYCODE_8 -> sendPressedKey(vt320.KEY_F8, 0)
            KeyEvent.KEYCODE_9 -> sendPressedKey(vt320.KEY_F9, 0)
            KeyEvent.KEYCODE_0 -> sendPressedKey(vt320.KEY_F10, 0)
            //Add F11 and F12??
            else -> handled = false
        }
        return handled
    }

    /**
     * Handle meta key presses where the key can be locked on.
     *
     *
     * 1st press: next key to have meta state<br></br>
     * 2nd press: meta state is locked on<br></br>
     * 3rd press: disable meta state
     *
     * @param code
     */
    @JvmOverloads
    fun metaPress(code: Int, forceSticky: Boolean = false) {
        if (metaState and (code shl 1) != 0) {
            metaState = metaState and (code shl 1).inv()
        } else if (metaState and code != 0) {
            metaState = metaState and code.inv()
            metaState = metaState or (code shl 1)
        } else if (forceSticky || stickyMetas and code != 0) {
            metaState = metaState or code
        } else {
            // skip redraw
            return
        }
        bridge.redraw()
    }

    private val stateForBuffer: Int
        get() {
            var bufferState = 0
            if (metaState and OUR_CTRL_MASK != 0) bufferState = bufferState or vt320.KEY_CONTROL
            if (metaState and OUR_SHIFT_MASK != 0) bufferState = bufferState or vt320.KEY_SHIFT
            if (metaState and OUR_ALT_MASK != 0) bufferState = bufferState or vt320.KEY_ALT
            return bufferState
        }

    fun setClipboardManager(clipboard: ClipboardManager?) {
        this.clipboard = clipboard
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences,
                                           key: String) {
        if (PreferenceConstants.KEYMODE == key || PreferenceConstants.SHIFT_FKEYS == key || PreferenceConstants.CTRL_FKEYS == key || PreferenceConstants.VOLUME_FONT == key || PreferenceConstants.STICKY_MODIFIERS == key) {
            updatePrefs()
        }
    }

    private fun updatePrefs() {
        keymode = prefs.getString(PreferenceConstants.KEYMODE, PreferenceConstants.KEYMODE_NONE)
        shiftedNumbersAreFKeysOnHardKeyboard = prefs.getBoolean(PreferenceConstants.SHIFT_FKEYS, false)
        controlNumbersAreFKeysOnSoftKeyboard = prefs.getBoolean(PreferenceConstants.CTRL_FKEYS, false)
        volumeKeysChangeFontSize = prefs.getBoolean(PreferenceConstants.VOLUME_FONT, true)
        val stickyModifiers = prefs.getString(PreferenceConstants.STICKY_MODIFIERS,
                PreferenceConstants.NO)
        stickyMetas = when (stickyModifiers) {
            PreferenceConstants.ALT -> OUR_ALT_ON
            PreferenceConstants.YES -> OUR_SHIFT_ON or OUR_CTRL_ON or OUR_ALT_ON
            else -> 0
        }
    }

    fun setCharset(encoding: String) {
        this.encoding = encoding
    }

    companion object {
        private const val TAG = "CB.OnKeyListener"

        // Constants for our private tracking of modifier state
        const val OUR_CTRL_ON = 0x01
        const val OUR_CTRL_LOCK = 0x02
        const val OUR_ALT_ON = 0x04
        const val OUR_ALT_LOCK = 0x08
        const val OUR_SHIFT_ON = 0x10
        const val OUR_SHIFT_LOCK = 0x20
        private const val OUR_SLASH = 0x40
        private const val OUR_TAB = 0x80

        // All the transient key codes
        private const val OUR_TRANSIENT = (OUR_CTRL_ON or OUR_ALT_ON
                or OUR_SHIFT_ON or OUR_SLASH or OUR_TAB)

        // The bit mask of momentary and lock states for each
        private const val OUR_CTRL_MASK = OUR_CTRL_ON or OUR_CTRL_LOCK
        private const val OUR_ALT_MASK = OUR_ALT_ON or OUR_ALT_LOCK
        private const val OUR_SHIFT_MASK = OUR_SHIFT_ON or OUR_SHIFT_LOCK

        private const val KEYCODE_CTRL_LEFT = 113
        private const val KEYCODE_CTRL_RIGHT = 114
        private const val KEYCODE_INSERT = 124
        private const val KEYCODE_FORWARD_DEL = 112
        private const val KEYCODE_MOVE_HOME = 122
        private const val KEYCODE_MOVE_END = 123
        private const val KEYCODE_PAGE_DOWN = 93
        private const val KEYCODE_PAGE_UP = 92
        private const val HC_META_CTRL_ON = 0x1000
        private const val HC_META_CTRL_LEFT_ON = 0x2000
        private const val HC_META_CTRL_RIGHT_ON = 0x4000
        private const val HC_META_CTRL_MASK = (HC_META_CTRL_ON or HC_META_CTRL_RIGHT_ON
                or HC_META_CTRL_LEFT_ON)
        private const val HC_META_ALT_MASK = (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                or KeyEvent.META_ALT_RIGHT_ON)
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
        deviceHasHardKeyboard = (manager!!.res!!.configuration.keyboard
                == Configuration.KEYBOARD_QWERTY)
        updatePrefs()
    }
}