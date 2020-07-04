package de.mud.terminal

import android.text.AndroidCharacter
import de.mud.terminal.Precomposer.precompose
import java.util.*

/*
 * This file is part of "JTA - Telnet/SSH for the JAVA(tm) platform".
 *
 * (c) Matthias L. Jugel, Marcus Meiner 1996-2005. All Rights Reserved.
 *
 * Please visit http://javatelnet.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */ /**
 * Implementation of a VT terminal emulation plus ANSI compatible.
 *
 *
 * <B>Maintainer:</B> Marcus Meißner
 *
 * @author Matthias L. Jugel, Marcus Meißner
 * @version $Id: de.mud.terminal.vt320.java 507 2005-10-25 10:14:52Z marcus $
 */
abstract class vt320 @JvmOverloads constructor(width: Int = 80, height: Int = 24) : VDUBuffer(width, height), VDUInput {
    private var debugStr: StringBuilder?
    abstract fun debug(notice: String?)

    /**
     * Write an answer back to the remote host. This is needed to be able to
     * send terminal answers requests like status and type information.
     *
     * @param b the array of bytes to be sent
     */
    abstract override fun write(b: ByteArray)

    /**
     * Write an answer back to the remote host. This is needed to be able to
     * send terminal answers requests like status and type information.
     *
     * @param b the array of bytes to be sent
     */
    abstract fun write(b: Int)

    /**
     * Play the beep sound ...
     */
    open fun beep() { /* do nothing by default */
    }

    /**
     * Convenience function for putString(char[], int, int)
     */
    fun putString(s: String?) {
        val len = s!!.length
        val tmp = CharArray(len)
        s.toCharArray(tmp, 0, 0, len)
        putString(tmp, null, 0, len)
    }

    /**
     * Put string at current cursor position. Moves cursor
     * according to the String. Does NOT wrap.
     *
     * @param s     character array
     * @param start place to start in array
     * @param len   number of characters to process
     */
    fun putString(s: CharArray, fullwidths: ByteArray?, start: Int, len: Int) {
        if (len > 0) {
            //markLine(R, 1);
            var lastChar = -1
            var c: Char
            var isWide = false
            for (i in 0 until len) {
                c = s[start + i]
                // Shortcut for my favorite ASCII
                if (c.toInt() <= 0x7F) {
                    if (lastChar != -1) putChar(lastChar.toChar(), isWide, false)
                    lastChar = c.toInt()
                    isWide = false
                } else if (!Character.isLowSurrogate(c) && !Character.isHighSurrogate(c)) {
                    if (Character.getType(c) == Character.NON_SPACING_MARK.toInt()) {
                        if (lastChar != -1) {
                            val nc = precompose(lastChar.toChar(), c)
                            putChar(nc, isWide, false)
                            lastChar = -1
                        }
                    } else {
                        if (lastChar != -1) putChar(lastChar.toChar(), isWide, false)
                        lastChar = c.toInt()
                        if (fullwidths != null) {
                            val width = fullwidths[i]
                            isWide = (width.toInt() == AndroidCharacter.EAST_ASIAN_WIDTH_WIDE
                                    || width.toInt() == AndroidCharacter.EAST_ASIAN_WIDTH_FULL_WIDTH)
                        }
                    }
                }
            }
            if (lastChar != -1) putChar(lastChar.toChar(), isWide, false)
            setCursorPosition(C, R)
            redraw()
        }
    }

    protected open fun sendTelnetCommand(cmd: Byte) {}

    /**
     * Sent the changed window size from the terminal to all listeners.
     */
    protected open fun setWindowSize(c: Int, r: Int) {
        /* To be overridden by Terminal.java */
    }

    override fun setScreenSize(c: Int, r: Int, broadcast: Boolean) {
        val oldrows = height
        if (debug > 2) {
            if (debugStr == null) debugStr = StringBuilder()
            debugStr!!.append("setscreensize (")
                    .append(c)
                    .append(',')
                    .append(r)
                    .append(',')
                    .append(broadcast)
                    .append(')')
            debug(debugStr.toString())
            debugStr!!.setLength(0)
        }
        super.setScreenSize(c, r, false)

        // Don't let the cursor go off the screen. Scroll down if needed.
        if (R >= r) {
            screenBase += R - (r - 1)
            setWindowBase(screenBase)
        }
        R = cursorRow
        C = cursorColumn
        if (broadcast) {
            setWindowSize(c, r) /* broadcast up */
        }
    }

    fun setBackspace(type: Int) {
        when (type) {
            DELETE_IS_DEL -> {
                BackSpace[0] = "\u007f"
                BackSpace[1] = "\b"
            }
            DELETE_IS_BACKSPACE -> {
                BackSpace[0] = "\b"
                BackSpace[1] = "\u007f"
            }
        }
    }

    val isMouseReportEnabled: Boolean
        get() = mouserpt != 0

    /**
     * Terminal is mouse-aware and requires (x,y) coordinates of
     * on the terminal (character coordinates) and the button clicked.
     *
     * @param x
     * @param y
     * @param modifiers
     */
    override fun mousePressed(x: Int, y: Int, modifiers: Int) {
        if (mouserpt == 0) return
        mousebut = 3
        if (modifiers and 16 == 16) mousebut = 0
        if (modifiers and 8 == 8) mousebut = 1
        if (modifiers and 4 == 4) mousebut = 2
        val mousecode: Int
        mousecode = if (mouserpt == 9) /* X10 Mouse */ 0x20 or mousebut.toInt() else  /* normal xterm mouse reporting */ mousebut or 0x20 or (modifiers and 7 shl 2)
        val b = ByteArray(6)
        b[0] = 27
        b[1] = '['.toByte()
        b[2] = 'M'.toByte()
        b[3] = mousecode.toByte()
        b[4] = (0x20 + x + 1).toByte()
        b[5] = (0x20 + y + 1).toByte()
        write(b) // FIXME: writeSpecial here
    }

    /**
     * Passes mouse wheel events to the terminal.
     *
     * @param down  True if scrolling down the page. False if scrolling up.
     * @param x
     * @param y
     * @param ctrl
     * @param shift
     * @param meta
     */
    override fun mouseWheel(down: Boolean, x: Int, y: Int, ctrl: Boolean, shift: Boolean, meta: Boolean) {
        if (mouserpt == 0 || mouserpt == 9) return
        var mods = 0
        if (ctrl) mods = mods or 16
        if (shift) mods = mods or 4
        if (meta) mods = mods or 8
        val mousecode = (if (down) 0 else 1) + 96 or 0x20 or mods
        val b = ByteArray(6)
        b[0] = 27
        b[1] = '['.toByte()
        b[2] = 'M'.toByte()
        b[3] = mousecode.toByte()
        b[4] = (0x20 + x + 1).toByte()
        b[5] = (0x20 + y + 1).toByte()
        write(b) // FIXME: writeSpecial here
    }

    /**
     * Passes mouse move events to the terminal.
     *
     * @param button The mouse button pressed. 3 indicates no button is pressed.
     * @param x
     * @param y
     * @param ctrl
     * @param shift
     * @param meta
     */
    fun mouseMoved(button: Int, x: Int, y: Int, ctrl: Boolean, shift: Boolean, meta: Boolean) {
        if (mouserpt != 1002 && mouserpt != 1003) return

        // 1002 only reports drags. 1003 reports any movement.
        if (mouserpt == 1002 && button == 3) return
        var mods = 0
        if (ctrl) mods = mods or 16
        if (shift) mods = mods or 4
        if (meta) mods = mods or 8

        // Normal mouse code plus additional 32 to indicate movement.
        val mousecode = button + 0x40 or mods
        val b = ByteArray(6)
        b[0] = 27
        b[1] = '['.toByte()
        b[2] = 'M'.toByte()
        b[3] = mousecode.toByte()
        b[4] = (0x20 + x + 1).toByte()
        b[5] = (0x20 + y + 1).toByte()
        write(b) // FIXME: writeSpecial here
    }

    /**
     * Terminal is mouse-aware and requires the coordinates and button
     * of the release.
     *
     * @param x
     * @param y
     */
    override fun mouseReleased(x: Int, y: Int) {
        if (mouserpt == 0) return

        /* problem is tht modifiers still have the released button set in them.
    int mods = modifiers;
    mousebut = 3;
    if ((mods & 16)==16) mousebut=0;
    if ((mods &  8)==8 ) mousebut=1;
    if ((mods &  4)==4 ) mousebut=2;
    */
        val mousecode: Int
        mousecode = if (mouserpt == 9) 0x20 + mousebut /* same as press? appears so. */ else '#'.toInt()
        val b = ByteArray(6)
        b[0] = 27
        b[1] = '['.toByte()
        b[2] = 'M'.toByte()
        b[3] = mousecode.toByte()
        b[4] = (0x20 + x + 1).toByte()
        b[5] = (0x20 + y + 1).toByte()
        write(b) // FIXME: writeSpecial here
        mousebut = 0
    }

    /**
     * we should do localecho (passed from other modules). false is default
     */
    private var localecho = false

    /**
     * Enable or disable the local echo property of the terminal.
     *
     * @param echo true if the terminal should echo locally
     */
    fun setLocalEcho(echo: Boolean) {
        localecho = echo
    }

    /**
     * Enable the VMS mode of the terminal to handle some things differently
     * for VMS hosts.
     *
     * @param vms true for vms mode, false for normal mode
     */
    fun setVMS(vms: Boolean) {
        this.vms = vms
    }

    /**
     * Enable the usage of the IBM character set used by some BBS's. Special
     * graphical character are available in this mode.
     *
     * @param ibm true to use the ibm character set
     */
    fun setIBMCharset(ibm: Boolean) {
        useibmcharset = ibm
    }

    /**
     * Override the standard key codes used by the terminal emulation.
     *
     * @param codes a properties object containing key code definitions
     */
    override fun setKeyCodes(codes: Properties) {
        var res: String?
        val prefixes = arrayOf("", "S", "C", "A")
        var i: Int
        i = 0
        while (i < 10) {
            res = codes.getProperty("NUMPAD$i")
            if (res != null) Numpad[i] = unEscape(res)
            i++
        }
        i = 1
        while (i < 20) {
            res = codes.getProperty("F$i")
            if (res != null) FunctionKey[i] = unEscape(res)
            res = codes.getProperty("SF$i")
            if (res != null) FunctionKeyShift[i] = unEscape(res)
            res = codes.getProperty("CF$i")
            if (res != null) FunctionKeyCtrl[i] = unEscape(res)
            res = codes.getProperty("AF$i")
            if (res != null) FunctionKeyAlt[i] = unEscape(res)
            i++
        }
        i = 0
        while (i < 4) {
            res = codes.getProperty(prefixes[i] + "PGUP")
            if (res != null) PrevScn[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "PGDOWN")
            if (res != null) NextScn[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "END")
            if (res != null) KeyEnd[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "HOME")
            if (res != null) KeyHome[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "INSERT")
            if (res != null) Insert[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "REMOVE")
            if (res != null) Remove[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "UP")
            if (res != null) KeyUp[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "DOWN")
            if (res != null) KeyDown[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "LEFT")
            if (res != null) KeyLeft[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "RIGHT")
            if (res != null) KeyRight[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "ESCAPE")
            if (res != null) Escape[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "BACKSPACE")
            if (res != null) BackSpace[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "TAB")
            if (res != null) TabKey[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "NUMPLUS")
            if (res != null) NUMPlus[i] = unEscape(res)
            res = codes.getProperty(prefixes[i] + "NUMDECIMAL")
            if (res != null) NUMDot[i] = unEscape(res)
            i++
        }
    }

    /**
     * Set the terminal id used to identify this terminal.
     *
     * @param terminalID the id string
     */
    fun setTerminalID(terminalID: String) {
        this.terminalID = terminalID
        if (terminalID == "scoansi") {
            FunctionKey[1] = "\u001b[M"
            FunctionKey[2] = "\u001b[N"
            FunctionKey[3] = "\u001b[O"
            FunctionKey[4] = "\u001b[P"
            FunctionKey[5] = "\u001b[Q"
            FunctionKey[6] = "\u001b[R"
            FunctionKey[7] = "\u001b[S"
            FunctionKey[8] = "\u001b[T"
            FunctionKey[9] = "\u001b[U"
            FunctionKey[10] = "\u001b[V"
            FunctionKey[11] = "\u001b[W"
            FunctionKey[12] = "\u001b[X"
            FunctionKey[13] = "\u001b[Y"
            FunctionKey[14] = "?"
            FunctionKey[15] = "\u001b[a"
            FunctionKey[16] = "\u001b[b"
            FunctionKey[17] = "\u001b[c"
            FunctionKey[18] = "\u001b[d"
            FunctionKey[19] = "\u001b[e"
            FunctionKey[20] = "\u001b[f"
            PrevScn[3] = "\u001b[I"
            PrevScn[2] = PrevScn[3]
            PrevScn[1] = PrevScn[2]
            PrevScn[0] = PrevScn[1]
            NextScn[3] = "\u001b[G"
            NextScn[2] = NextScn[3]
            NextScn[1] = NextScn[2]
            NextScn[0] = NextScn[1]
            // more theoretically.
        }
    }

    fun setAnswerBack(ab: String) {
        answerBack = unEscape(ab)
    }

    /**
     * Get the terminal id used to identify this terminal.
     */
    fun getTerminalID(): String {
        return terminalID
    }

    /**
     * A small conveniance method thar converts the string to a byte array
     * for sending.
     *
     * @param s the string to be sent
     */
    private fun write(s: String?, doecho: Boolean = localecho): Boolean {
        if (debug > 2) {
            debugStr!!.append("write(|")
                    .append(s)
                    .append("|,")
                    .append(doecho)
            debug(debugStr.toString())
            debugStr!!.setLength(0)
        }
        if (s == null) // aka the empty string.
            return true
        /* NOTE: getBytes() honours some locale, it *CONVERTS* the string.
         * However, we output only 7bit stuff towards the target, and *some*
         * 8 bit control codes. We must not mess up the latter, so we do hand
         * by hand copy.
         */
        val arr = ByteArray(s.length)
        for (i in 0 until s.length) {
            arr[i] = s[i].toByte()
        }
        write(arr)
        if (doecho) putString(s)
        return true
    }

    private fun write(s: Int, doecho: Boolean): Boolean {
        if (debug > 2) {
            debugStr!!.append("write(|")
                    .append(s)
                    .append("|,")
                    .append(doecho)
            debug(debugStr.toString())
            debugStr!!.setLength(0)
        }
        write(s)

        // TODO check if character is wide
        if (doecho) putChar(s.toChar(), false, false)
        return true
    }

    // ===================================================================
    // the actual terminal emulation code comes here:
    // ===================================================================
    private var terminalID = "de.mud.terminal.vt320"
    private var answerBack = "Use Terminal.answerback to set ...\n"

    // X - COLUMNS, Y - ROWS
    var R = 0
    var C = 0
    var attributes: Long = 0
    var Sc = 0
    var Sr = 0
    var Stm = 0
    var Sbm = 0
    var Sa: Long = 0
    var Sgr = 0.toChar()
    var Sgl = 0.toChar()
    var Sgx: CharArray?
    var insertmode = 0
    var statusmode = 0
    var vt52mode = false
    var keypadmode = false /* false - numeric, true - application */
    var output8bit = false
    var normalcursor = 0
    var moveoutsidemargins = true
    var wraparound = true
    var sendcrlf = true
    var capslock = false
    var numlock = false
    var mouserpt = 0
    var mouserptSaved = 0
    var mousebut: Byte = 0
    var useibmcharset = false
    var lastwaslf = 0
    var usedcharsets = false

    /* The graphics charsets
     * B - default ASCII
     * A - ISO Latin 1
     * 0 - DEC SPECIAL
     * < - User defined
     * ....
     */
    var gx: CharArray
    var gl // GL (left charset) = 0.toChar()
    var gr // GR (right charset) = 0.toChar()
    var onegl // single shift override for GL. = 0

    /**
     * Strings to send on function key pressing
     */
    private val Numpad: Array<String?>
    private val FunctionKey: Array<String?>
    private val FunctionKeyShift: Array<String?>
    private val FunctionKeyCtrl: Array<String?>
    private val FunctionKeyAlt: Array<String?>
    private val TabKey: Array<String?>
    private val KeyUp: Array<String?>
    private val KeyDown: Array<String?>
    private val KeyLeft: Array<String?>
    private val KeyRight: Array<String?>
    private val KPMinus: String
    private val KPComma: String
    private val KPPeriod: String
    private val KPEnter: String
    private val PF1: String
    private val PF2: String
    private val PF3: String
    private val PF4: String
    private val Help: String
    private val Do: String
    private val Find: String
    private val Select: String
    private val KeyHome: Array<String?>
    private val KeyEnd: Array<String?>
    private val Insert: Array<String?>
    private val Remove: Array<String?>
    private val PrevScn: Array<String?>
    private val NextScn: Array<String?>
    private val Escape: Array<String?>
    private val BackSpace: Array<String?>
    private val NUMDot: Array<String?>
    private val NUMPlus: Array<String?>
    private var osc: String? = null
    private var dcs /* to memorize OSC & DCS control sequence */: String? = null

    /**
     * de.mud.terminal.vt320 state variable (internal)
     */
    private var term_state = TSTATE_DATA

    /**
     * in vms mode, set by Terminal.VMS property
     */
    private var vms = false

    /**
     * Tabulators
     */
    private var Tabs: ByteArray

    /**
     * The list of integers as used by CSI
     */
    private val DCEvars = IntArray(30)
    private var DCEvar = 0

    /**
     * A small conveniance method thar converts a 7bit string to the 8bit
     * version depending on VT52/Output8Bit mode.
     *
     * @param s the string to be sent
     */
    private fun writeSpecial(s: String?): Boolean {
        var s = s ?: return true
        if (s.length >= 3 && s[0].toInt() == 27 && s[1] == 'O') {
            if (vt52mode) {
                s = if (s[2] >= 'P' && s[2] <= 'S') {
                    "\u001b" + s.substring(2) /* ESC x */
                } else {
                    "\u001b?" + s.substring(2) /* ESC ? x */
                }
            } else {
                if (output8bit) {
                    s = "\u008f" + s.substring(2) /* SS3 x */
                } /* else keep string as it is */
            }
        }
        if (s.length >= 3 && s[0].toInt() == 27 && s[1] == '[') {
            if (output8bit) {
                s = "\u009b" + s.substring(2) /* CSI ... */
            } /* else keep */
        }
        return write(s, false)
    }

    /**
     * main keytyping event handler...
     */
    override fun keyPressed(keyCode: Int, keyChar: Char, modifiers: Int) {
        val control = modifiers and VDUInput.KEY_CONTROL != 0
        val shift = modifiers and VDUInput.KEY_SHIFT != 0
        val alt = modifiers and VDUInput.KEY_ALT != 0
        if (debug > 1) {
            debugStr!!.append("keyPressed(")
                    .append(keyCode)
                    .append(", ")
                    .append(keyChar.toInt())
                    .append(", ")
                    .append(modifiers)
                    .append(')')
            debug(debugStr.toString())
            debugStr!!.setLength(0)
        }
        var xind: Int
        var fmap: Array<String?>
        xind = 0
        fmap = FunctionKey
        if (shift) {
            fmap = FunctionKeyShift
            xind = 1
        }
        if (control) {
            fmap = FunctionKeyCtrl
            xind = 2
        }
        if (alt) {
            fmap = FunctionKeyAlt
            xind = 3
        }
        when (keyCode) {
            KEY_PAUSE -> if (shift || control) sendTelnetCommand(243.toByte()) // BREAK
            KEY_F1 -> writeSpecial(fmap[1])
            KEY_F2 -> writeSpecial(fmap[2])
            KEY_F3 -> writeSpecial(fmap[3])
            KEY_F4 -> writeSpecial(fmap[4])
            KEY_F5 -> writeSpecial(fmap[5])
            KEY_F6 -> writeSpecial(fmap[6])
            KEY_F7 -> writeSpecial(fmap[7])
            KEY_F8 -> writeSpecial(fmap[8])
            KEY_F9 -> writeSpecial(fmap[9])
            KEY_F10 -> writeSpecial(fmap[10])
            KEY_F11 -> writeSpecial(fmap[11])
            KEY_F12 -> writeSpecial(fmap[12])
            KEY_UP -> writeSpecial(KeyUp[xind])
            KEY_DOWN -> writeSpecial(KeyDown[xind])
            KEY_LEFT -> writeSpecial(KeyLeft[xind])
            KEY_RIGHT -> writeSpecial(KeyRight[xind])
            KEY_PAGE_DOWN -> writeSpecial(NextScn[xind])
            KEY_PAGE_UP -> writeSpecial(PrevScn[xind])
            KEY_INSERT -> writeSpecial(Insert[xind])
            KEY_DELETE -> writeSpecial(Remove[xind])
            KEY_BACK_SPACE -> {
                writeSpecial(BackSpace[xind])
                if (localecho) {
                    if (BackSpace[xind] === "\b") {
                        putString("\b \b") // make the last char 'deleted'
                    } else {
                        putString(BackSpace[xind]) // echo it
                    }
                }
            }
            KEY_HOME -> writeSpecial(KeyHome[xind])
            KEY_END -> writeSpecial(KeyEnd[xind])
            KEY_NUM_LOCK -> {
                if (vms && control) {
                    writeSpecial(PF1)
                }
                if (!control) numlock = !numlock
            }
            KEY_CAPS_LOCK -> {
                capslock = !capslock
                return
            }
            KEY_SHIFT, KEY_CONTROL, KEY_ALT -> return
            else -> {
            }
        }
    }
    /*
  public void keyReleased(KeyEvent evt) {
    if (debug > 1) debug("keyReleased("+evt+")");
    // ignore
  }
*/
    /**
     * Handle key Typed events for the terminal, this will get
     * all normal key types, but no shift/alt/control/numlock.
     */
    override fun keyTyped(keyCode: Int, keyChar: Char, modifiers: Int) {
        val control = modifiers and VDUInput.KEY_CONTROL != 0
        val shift = modifiers and VDUInput.KEY_SHIFT != 0
        val alt = modifiers and VDUInput.KEY_ALT != 0
        if (debug > 1) debug("keyTyped(" + keyCode + ", " + keyChar.toInt() + ", " + modifiers + ")")
        if (keyChar == '\t') {
            if (shift) {
                write(TabKey[1], false)
            } else {
                if (control) {
                    write(TabKey[2], false)
                } else {
                    if (alt) {
                        write(TabKey[3], false)
                    } else {
                        write(TabKey[0], false)
                    }
                }
            }
            return
        }
        if (alt) {
            write((keyChar.toInt() or 0x80).toChar().toInt())
            return
        }
        if ((keyCode == KEY_ENTER || keyChar.toInt() == 10)
                && !control) {
            write('\r'.toInt())
            if (localecho) putString("\r\n") // bad hack
            return
        }
        if (keyCode == 10 && !control) {
            debug("Sending \\r")
            write('\r'.toInt())
            return
        }

        // FIXME: on german PC keyboards you have to use Alt-Ctrl-q to get an @,
        // so we can't just use it here... will probably break some other VMS
        // codes.  -Marcus
        // if(((!vms && keyChar == '2') || keyChar == '@' || keyChar == ' ')
        //    && control)
        if ((!vms && keyChar == '2' || keyChar == ' ') && control) write(0)
        if (vms) {
            if (keyChar.toInt() == 127 && !control) {
                if (shift) writeSpecial(Insert[0]) //  VMS shift delete = insert
                else writeSpecial(Remove[0]) //  VMS delete = remove
                return
            } else if (control) when (keyChar) {
                '0' -> {
                    writeSpecial(Numpad[0])
                    return
                }
                '1' -> {
                    writeSpecial(Numpad[1])
                    return
                }
                '2' -> {
                    writeSpecial(Numpad[2])
                    return
                }
                '3' -> {
                    writeSpecial(Numpad[3])
                    return
                }
                '4' -> {
                    writeSpecial(Numpad[4])
                    return
                }
                '5' -> {
                    writeSpecial(Numpad[5])
                    return
                }
                '6' -> {
                    writeSpecial(Numpad[6])
                    return
                }
                '7' -> {
                    writeSpecial(Numpad[7])
                    return
                }
                '8' -> {
                    writeSpecial(Numpad[8])
                    return
                }
                '9' -> {
                    writeSpecial(Numpad[9])
                    return
                }
                '.' -> {
                    writeSpecial(KPPeriod)
                    return
                }
                '-', 31 -> {
                    writeSpecial(KPMinus)
                    return
                }
                '+' -> {
                    writeSpecial(KPComma)
                    return
                }
                10 -> {
                    writeSpecial(KPEnter)
                    return
                }
                '/' -> {
                    writeSpecial(PF2)
                    return
                }
                '*' -> {
                    writeSpecial(PF3)
                    return
                }
                else -> {
                }
            }
            /* Now what does this do and how did it get here. -Marcus
      if (shift && keyChar < 32) {
        write(PF1+(char)(keyChar + 64));
        return;
      }
      */
        }

        // FIXME: not used?
        //String fmap[];
        var xind: Int
        xind = 0
        //fmap = FunctionKey;
        if (shift) {
            //fmap = FunctionKeyShift;
            xind = 1
        }
        if (control) {
            //fmap = FunctionKeyCtrl;
            xind = 2
        }
        if (alt) {
            //fmap = FunctionKeyAlt;
            xind = 3
        }
        if (keyCode == KEY_ESCAPE) {
            writeSpecial(Escape[xind])
            return
        }
        if (modifiers and VDUInput.KEY_ACTION != 0) when (keyCode) {
            KEY_NUMPAD0 -> {
                writeSpecial(Numpad[0])
                return
            }
            KEY_NUMPAD1 -> {
                writeSpecial(Numpad[1])
                return
            }
            KEY_NUMPAD2 -> {
                writeSpecial(Numpad[2])
                return
            }
            KEY_NUMPAD3 -> {
                writeSpecial(Numpad[3])
                return
            }
            KEY_NUMPAD4 -> {
                writeSpecial(Numpad[4])
                return
            }
            KEY_NUMPAD5 -> {
                writeSpecial(Numpad[5])
                return
            }
            KEY_NUMPAD6 -> {
                writeSpecial(Numpad[6])
                return
            }
            KEY_NUMPAD7 -> {
                writeSpecial(Numpad[7])
                return
            }
            KEY_NUMPAD8 -> {
                writeSpecial(Numpad[8])
                return
            }
            KEY_NUMPAD9 -> {
                writeSpecial(Numpad[9])
                return
            }
            KEY_DECIMAL -> {
                writeSpecial(NUMDot[xind])
                return
            }
            KEY_ADD -> {
                writeSpecial(NUMPlus[xind])
                return
            }
        }
        if (!(keyChar.toInt() == 8 || keyChar.toInt() == 127 || keyChar == '\r' || keyChar == '\n')) {
            write(keyChar.toInt())
            return
        }
    }

    private fun handle_dcs(dcs: String?) {
        debugStr!!.append("DCS: ")
                .append(dcs)
        debug(debugStr.toString())
        debugStr!!.setLength(0)
    }

    private fun handle_osc(osc: String?) {
        if (osc!!.length > 2 && osc.substring(0, 2) == "4;") {
            // Define color palette
            val colorData = osc.split(";".toRegex()).toTypedArray()
            try {
                val colorIndex = colorData[1].toInt()
                if ("rgb:" == colorData[2].substring(0, 4)) {
                    val rgb = colorData[2].substring(4).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val red = rgb[0].substring(0, 2).toInt(16) and 0xFF
                    val green = rgb[1].substring(0, 2).toInt(16) and 0xFF
                    val blue = rgb[2].substring(0, 2).toInt(16) and 0xFF
                    display.setColor(colorIndex, red, green, blue)
                }
            } catch (e: Exception) {
                debugStr!!.append("OSC: invalid color sequence encountered: ")
                        .append(osc)
                debug(debugStr.toString())
                debugStr!!.setLength(0)
            }
        } else debug("OSC: $osc")
    }

    fun map_cp850_unicode(x: Char): Char {
        return if (x.toInt() >= 0x100) x else unimap[x.toInt()]
    }

    private fun _SetCursor(row: Int, col: Int) {
        var maxr = height - 1
        val tm = topMargin
        R = if (row < 0) 0 else row
        C = if (col < 0) 0 else if (col >= width) width - 1 else col
        if (!moveoutsidemargins) {
            R += tm
            maxr = bottomMargin
        }
        if (R > maxr) R = maxr
    }

    private fun putChar(c: Char, isWide: Boolean, doshowcursor: Boolean) {
        var c = c
        val rows = height //statusline
        val columns = width
        when (term_state) {
            TSTATE_DATA -> {
                /* FIXME: we shouldn't use chars with bit 8 set if ibmcharset.
                 * probably... but some BBS do anyway...
                 */if (!useibmcharset) {
                    var doneflag = true
                    when (c) {
                        OSC -> {
                            osc = ""
                            term_state = TSTATE_OSC
                        }
                        RI -> {
                            if (R > topMargin) R-- else insertLine(R, 1, SCROLL_DOWN)
                            if (debug > 1) debug("RI")
                        }
                        IND -> {
                            if (debug > 2) {
                                debugStr!!.append("IND at ")
                                        .append(R)
                                        .append(", tm is ")
                                        .append(topMargin)
                                        .append(", bm is ")
                                        .append(bottomMargin)
                                debug(debugStr.toString())
                                debugStr!!.setLength(0)
                            }
                            if (R == bottomMargin || R == rows - 1) insertLine(R, 1, SCROLL_UP) else R++
                            if (debug > 1) debug("IND (at $R )")
                        }
                        NEL -> {
                            if (R == bottomMargin || R == rows - 1) insertLine(R, 1, SCROLL_UP) else R++
                            C = 0
                            if (debug > 1) debug("NEL (at $R )")
                        }
                        HTS -> {
                            Tabs[C] = 1
                            if (debug > 1) debug("HTS")
                        }
                        DCS -> {
                            dcs = ""
                            term_state = TSTATE_DCS
                        }
                        else -> doneflag = false
                    }
                    if (doneflag) break
                }
                when (c) {
                    SS3 -> onegl = 3
                    SS2 -> onegl = 2
                    CSI -> {
                        DCEvar = 0
                        DCEvars[0] = 0
                        DCEvars[1] = 0
                        DCEvars[2] = 0
                        DCEvars[3] = 0
                        term_state = TSTATE_CSI
                    }
                    ESC -> {
                        term_state = TSTATE_ESC
                        lastwaslf = 0
                    }
                    5 -> write(answerBack, false)
                    12 -> {
                        /* FormFeed, Home for the BBS world */deleteArea(0, 0, columns, rows, attributes)
                        run {
                            R = 0
                            C = R
                        }
                    }
                    '\b' -> {
                        C--
                        if (C < 0) C = 0
                        lastwaslf = 0
                    }
                    '\t' -> {
                        do {
                            // Don't overwrite or insert! TABS are not destructive, but movement!
                            C++
                        } while (C < columns && Tabs[C] == 0)
                        lastwaslf = 0
                    }
                    '\r' -> C = 0
                    '\n' -> {
                        if (debug > 3) debug("R= $R, bm $bottomMargin, tm=$topMargin, rows=$rows")
                        if (!vms) {
                            if (lastwaslf != 0 && lastwaslf != c.toInt()) //  Ray: I do not understand this logic.
                                break
                            lastwaslf = c.toInt()
                            /*C = 0;*/
                        }
                        if (R == bottomMargin || R >= rows - 1) insertLine(R, 1, SCROLL_UP) else R++
                    }
                    7 -> beep()
                    '\u000e' -> {
                        /* ^N, Shift out - Put G1 into GL */gl = 1.toChar()
                        usedcharsets = true
                    }
                    '\u000f' -> {
                        /* ^O, Shift in - Put G0 into GL */gl = 0.toChar()
                        usedcharsets = true
                    }
                    else -> {
                        var thisgl = gl.toInt()
                        if (onegl >= 0) {
                            thisgl = onegl
                            onegl = -1
                        }
                        lastwaslf = 0
                        if (c.toInt() < 32) {
                            if (c.toInt() != 0) if (debug > 0) debug("TSTATE_DATA char: " + c.toInt())
                            /*break; some BBS really want those characters, like hearst etc. */if (c.toInt() == 0) /* print 0 ... you bet */ break
                        }
                        if (C >= columns) {
                            if (wraparound) {
                                var bot = rows

                                // If we're in the scroll region, check against the bottom margin
                                if (R <= bottomMargin && R >= topMargin) bot = bottomMargin + 1
                                if (R < bot - 1) R++ else {
                                    if (debug > 3) debug("scrolling due to wrap at $R")
                                    insertLine(R, 1, SCROLL_UP)
                                }
                                C = 0
                            } else {
                                // cursor stays on last character.
                                C = columns - 1
                            }
                        }
                        var mapped = false

                        // Mapping if DEC Special is chosen charset
                        if (usedcharsets) {
                            if (c >= '\u0020' && c <= '\u007f') {
                                when (gx[thisgl]) {
                                    '0' -> {
                                        // Remap SCOANSI line drawing to VT100 line drawing chars
                                        // for our SCO using customers.
                                        if (terminalID == "scoansi" || terminalID == "ansi") {
                                            var i = 0
                                            while (i < scoansi_acs.length) {
                                                if (c == scoansi_acs[i]) {
                                                    c = scoansi_acs[i + 1]
                                                    break
                                                }
                                                i += 2
                                            }
                                        }
                                        if (c >= '\u005f' && c <= '\u007e') {
                                            c = DECSPECIAL[c.toShort() - 0x5f]
                                            mapped = true
                                        }
                                    }
                                    '<' -> {
                                        c = (c.toInt() and 0x7f or 0x80).toChar()
                                        mapped = true
                                    }
                                    'A', 'B' -> mapped = true
                                    else -> debug("Unsupported GL mapping: " + gx[thisgl])
                                }
                            }
                            if (!mapped && c >= '\u0080' && c <= '\u00ff') {
                                when (gx[gr.toInt()]) {
                                    '0' -> if (c >= '\u00df' && c <= '\u00fe') {
                                        c = DECSPECIAL[c - '\u00df']
                                        mapped = true
                                    }
                                    '<', 'A', 'B' -> mapped = true
                                    else -> debug("Unsupported GR mapping: " + gx[gr.toInt()])
                                }
                            }
                        }
                        if (!mapped && useibmcharset) c = map_cp850_unicode(c)

                        /*if(true || (statusmode == 0)) { */if (isWide) {
                            if (C >= columns - 1) {
                                if (wraparound) {
                                    var bot = rows

                                    // If we're in the scroll region, check against the bottom margin
                                    if (R <= bottomMargin && R >= topMargin) bot = bottomMargin + 1
                                    if (R < bot - 1) R++ else {
                                        if (debug > 3) debug("scrolling due to wrap at $R")
                                        insertLine(R, 1, SCROLL_UP)
                                    }
                                    C = 0
                                } else {
                                    // cursor stays on last wide character.
                                    C = columns - 2
                                }
                            }
                        }
                        if (insertmode == 1) {
                            if (isWide) {
                                insertChar(C++, R, c, attributes or FULLWIDTH)
                                insertChar(C, R, ' ', attributes or FULLWIDTH)
                            } else insertChar(C, R, c, attributes)
                        } else {
                            if (isWide) {
                                putChar(C++, R, c, attributes or FULLWIDTH)
                                putChar(C, R, ' ', attributes or FULLWIDTH)
                            } else putChar(C, R, c, attributes)
                        }

                        /*
                } else {
                if (insertmode==1) {
                insertChar(C, rows, c, attributes);
                } else {
                putChar(C, rows, c, attributes);
                }
                }
              */C++
                    }
                }
            }
            TSTATE_OSC -> {
                if (c.toInt() < 0x20 && c != ESC) { // NP - No printing character
                    handle_osc(osc)
                    term_state = TSTATE_DATA
                    break
                }
                //but check for vt102 ESC \
                if (c == '\\' && osc!![osc!!.length - 1] == ESC) {
                    handle_osc(osc)
                    term_state = TSTATE_DATA
                    break
                }
                osc = osc + c
            }
            TSTATE_ESCSPACE -> {
                term_state = TSTATE_DATA
                when (c) {
                    'F' -> output8bit = false
                    'G' -> output8bit = true
                    else -> debug("ESC <space> $c unhandled.")
                }
            }
            TSTATE_ESC -> {
                term_state = TSTATE_DATA
                when (c) {
                    ' ' -> term_state = TSTATE_ESCSPACE
                    '#' -> term_state = TSTATE_ESCSQUARE
                    'c' ->                         /* Hard terminal reset */reset()
                    '[' -> {
                        DCEvar = 0
                        DCEvars[0] = 0
                        DCEvars[1] = 0
                        DCEvars[2] = 0
                        DCEvars[3] = 0
                        term_state = TSTATE_CSI
                    }
                    ']' -> {
                        osc = ""
                        term_state = TSTATE_OSC
                    }
                    'P' -> {
                        dcs = ""
                        term_state = TSTATE_DCS
                    }
                    'A' -> {
                        R--
                        if (R < 0) R = 0
                    }
                    'B' -> {
                        R++
                        if (R >= rows) R = rows - 1
                    }
                    'C' -> {
                        C++
                        if (C >= columns) C = columns - 1
                    }
                    'I' -> insertLine(R, 1, SCROLL_DOWN)
                    'E' -> {
                        if (R == bottomMargin || R == rows - 1) insertLine(R, 1, SCROLL_UP) else R++
                        C = 0
                        if (debug > 1) debug("ESC E (at $R)")
                    }
                    'D' -> {
                        if (R == bottomMargin || R == rows - 1) insertLine(R, 1, SCROLL_UP) else R++
                        if (debug > 1) debug("ESC D (at $R )")
                    }
                    'J' -> {
                        if (R < rows - 1) deleteArea(0, R + 1, columns, rows - R - 1, attributes)
                        if (C < columns - 1) deleteArea(C, R, columns - C, 1, attributes)
                    }
                    'K' -> if (C < columns - 1) deleteArea(C, R, columns - C, 1, attributes)
                    'M' -> {
                        debug("ESC M : R is $R, tm is $topMargin, bm is $bottomMargin")
                        if (R > topMargin) { // just go up 1 line.
                            R--
                        } else { // scroll down
                            insertLine(R, 1, SCROLL_DOWN)
                        }
                        /* else do nothing ; */if (debug > 2) debug("ESC M ")
                    }
                    'H' -> {
                        if (debug > 1) debug("ESC H at $C")
                        /* right border probably ...*/if (C >= columns) C = columns - 1
                        Tabs[C] = 1
                    }
                    'N' -> onegl = 2
                    'O' -> onegl = 3
                    '=' -> {
                        /*application keypad*/if (debug > 0) debug("ESC =")
                        keypadmode = true
                    }
                    '<' -> vt52mode = false
                    '>' -> {
                        if (debug > 0) debug("ESC >")
                        keypadmode = false
                    }
                    '7' -> {
                        Sc = C
                        Sr = R
                        Sgl = gl
                        Sgr = gr
                        Sa = attributes
                        Sgx = CharArray(4)
                        var i = 0
                        while (i < 4) {
                            Sgx!![i] = gx[i]
                            i++
                        }
                        if (debug > 1) debug("ESC 7")
                    }
                    '8' -> {
                        C = Sc
                        R = Sr
                        gl = Sgl
                        gr = Sgr
                        if (Sgx != null) {
                            var i = 0
                            while (i < 4) {
                                gx[i] = Sgx!![i]
                                i++
                            }
                        }
                        attributes = Sa
                        if (debug > 1) debug("ESC 8")
                    }
                    '(' -> {
                        term_state = TSTATE_SETG0
                        usedcharsets = true
                    }
                    ')' -> {
                        term_state = TSTATE_SETG1
                        usedcharsets = true
                    }
                    '*' -> {
                        term_state = TSTATE_SETG2
                        usedcharsets = true
                    }
                    '+' -> {
                        term_state = TSTATE_SETG3
                        usedcharsets = true
                    }
                    '~' -> {
                        gr = 1.toChar()
                        usedcharsets = true
                    }
                    'n' -> {
                        gl = 2.toChar()
                        usedcharsets = true
                    }
                    '}' -> {
                        gr = 2.toChar()
                        usedcharsets = true
                    }
                    'o' -> {
                        gl = 3.toChar()
                        usedcharsets = true
                    }
                    '|' -> {
                        gr = 3.toChar()
                        usedcharsets = true
                    }
                    'Y' -> term_state = TSTATE_VT52Y
                    '_' -> term_state = TSTATE_TITLE
                    '\\' ->                         // TODO save title
                        term_state = TSTATE_DATA
                    else -> debug("ESC unknown letter: " + c + " (" + c.toInt() + ")")
                }
            }
            TSTATE_VT52X -> {
                C = c.toInt() - 37
                if (C < 0) C = 0 else if (C >= width) C = width - 1
                term_state = TSTATE_VT52Y
            }
            TSTATE_VT52Y -> {
                R = c.toInt() - 37
                if (R < 0) R = 0 else if (R >= height) R = height - 1
                term_state = TSTATE_DATA
            }
            TSTATE_SETG0 -> {
                if (c != '0' && c != 'A' && c != 'B' && c != '<') debug("ESC ( " + c + ": G0 char set?  (" + c.toInt() + ")") else {
                    if (debug > 2) debug("ESC ( : G0 char set  (" + c + " " + c.toInt() + ")")
                    gx[0] = c
                }
                term_state = TSTATE_DATA
            }
            TSTATE_SETG1 -> {
                if (c != '0' && c != 'A' && c != 'B' && c != '<') {
                    debug("ESC ) " + c + " (" + c.toInt() + ") :G1 char set?")
                } else {
                    if (debug > 2) debug("ESC ) :G1 char set  (" + c + " " + c.toInt() + ")")
                    gx[1] = c
                }
                term_state = TSTATE_DATA
            }
            TSTATE_SETG2 -> {
                if (c != '0' && c != 'A' && c != 'B' && c != '<') debug("ESC*:G2 char set?  (" + c.toInt() + ")") else {
                    if (debug > 2) debug("ESC*:G2 char set  (" + c + " " + c.toInt() + ")")
                    gx[2] = c
                }
                term_state = TSTATE_DATA
            }
            TSTATE_SETG3 -> {
                if (c != '0' && c != 'A' && c != 'B' && c != '<') debug("ESC+:G3 char set?  (" + c.toInt() + ")") else {
                    if (debug > 2) debug("ESC+:G3 char set  (" + c + " " + c.toInt() + ")")
                    gx[3] = c
                }
                term_state = TSTATE_DATA
            }
            TSTATE_ESCSQUARE -> {
                when (c) {
                    '8' -> {
                        var i = 0
                        while (i < columns) {
                            var j = 0
                            while (j < rows) {
                                putChar(i, j, 'E', 0)
                                j++
                            }
                            i++
                        }
                    }
                    else -> debug("ESC # $c not supported.")
                }
                term_state = TSTATE_DATA
            }
            TSTATE_DCS -> {
                if (c == '\\' && dcs!![dcs!!.length - 1] == ESC) {
                    handle_dcs(dcs)
                    term_state = TSTATE_DATA
                    break
                }
                dcs = dcs + c
            }
            TSTATE_DCEQ -> {
                term_state = TSTATE_DATA
                when (c) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        DCEvars[DCEvar] = DCEvars[DCEvar] * 10 + c.toInt() - 48
                        term_state = TSTATE_DCEQ
                    }
                    ';' -> {
                        DCEvar++
                        DCEvars[DCEvar] = 0
                        term_state = TSTATE_DCEQ
                    }
                    's' -> {
                        var i = 0
                        while (i <= DCEvar) {
                            when (DCEvars[i]) {
                                9, 1000, 1001, 1002, 1003 -> mouserptSaved = mouserpt
                                else -> debug("ESC [ ? " + DCEvars[0] + " s, unimplemented!")
                            }
                            i++
                        }
                    }
                    'r' -> {
                        if (true || debug > 1) debug("ESC [ ? " + DCEvars[0] + " r")
                        /* DEC Mode reset */
                        var i = 0
                        while (i <= DCEvar) {
                            when (DCEvars[i]) {
                                3 -> setScreenSize(80, height, true)
                                4 -> {
                                }
                                5 -> {
                                }
                                6 -> moveoutsidemargins = true
                                7 -> wraparound = false
                                12 -> {
                                }
                                9, 1000, 1001, 1002, 1003 -> mouserpt = mouserptSaved
                                else -> debug("ESC [ ? " + DCEvars[0] + " r, unimplemented!")
                            }
                            i++
                        }
                    }
                    'h' -> {
                        if (debug > 0) debug("ESC [ ? " + DCEvars[0] + " h")
                        /* DEC Mode set */
                        var i = 0
                        while (i <= DCEvar) {
                            when (DCEvars[i]) {
                                1 -> {
                                    KeyUp[0] = "\u001bOA"
                                    KeyDown[0] = "\u001bOB"
                                    KeyRight[0] = "\u001bOC"
                                    KeyLeft[0] = "\u001bOD"
                                }
                                2 -> vt52mode = false
                                3 -> setScreenSize(132, height, true)
                                6 -> moveoutsidemargins = false
                                7 -> wraparound = true
                                25 -> showCursor(true)
                                9, 1000, 1001, 1002, 1003 -> mouserpt = DCEvars[i]
                                else -> debug("ESC [ ? " + DCEvars[0] + " h, unsupported.")
                            }
                            i++
                        }
                    }
                    'i' -> when (DCEvars[0]) {
                        1 -> if (debug > 1) debug("CSI ? 1 i : Print line containing cursor")
                        4 -> if (debug > 1) debug("CSI ? 4 i : Start passthrough printing")
                        5 -> if (debug > 1) debug("CSI ? 4 i : Stop passthrough printing")
                    }
                    'l' -> {
                        /* DEC Mode reset */if (debug > 0) debug("ESC [ ? " + DCEvars[0] + " l")
                        var i = 0
                        while (i <= DCEvar) {
                            when (DCEvars[i]) {
                                1 -> {
                                    KeyUp[0] = "\u001b[A"
                                    KeyDown[0] = "\u001b[B"
                                    KeyRight[0] = "\u001b[C"
                                    KeyLeft[0] = "\u001b[D"
                                }
                                2 -> vt52mode = true
                                3 -> setScreenSize(80, height, true)
                                6 -> moveoutsidemargins = true
                                7 -> wraparound = false
                                25 -> showCursor(false)
                                9, 1000, 1001, 1002, 1003 -> mouserpt = 0
                                else -> debug("ESC [ ? " + DCEvars[0] + " l, unsupported.")
                            }
                            i++
                        }
                    }
                    'n' -> {
                        if (debug > 0) debug("ESC [ ? " + DCEvars[0] + " n")
                        when (DCEvars[0]) {
                            15 -> {
                                /* printer? no printer. */write("$ESC[?13n", false)
                                debug("ESC[5n")
                            }
                            else -> debug("ESC [ ? " + DCEvars[0] + " n, unsupported.")
                        }
                    }
                    else -> debug("ESC [ ? " + DCEvars[0] + " " + c + ", unsupported.")
                }
            }
            TSTATE_CSI_EX -> {
                term_state = TSTATE_DATA
                when (c) {
                    ESC -> term_state = TSTATE_ESC
                    else -> debug("Unknown character ESC[! character is " + c.toInt())
                }
            }
            TSTATE_CSI_TICKS -> {
                term_state = TSTATE_DATA
                when (c) {
                    'p' -> {
                        debug("Conformance level: " + DCEvars[0] + " (unsupported)," + DCEvars[1])
                        if (DCEvars[0] == 61) {
                            output8bit = false
                            break
                        }
                        output8bit = if (DCEvars[1] == 1) {
                            false
                        } else {
                            true /* 0 or 2 */
                        }
                    }
                    else -> debug("Unknown ESC [...  \"$c")
                }
            }
            TSTATE_CSI_EQUAL -> {
                term_state = TSTATE_DATA
                when (c) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        DCEvars[DCEvar] = DCEvars[DCEvar] * 10 + c.toInt() - 48
                        term_state = TSTATE_CSI_EQUAL
                    }
                    ';' -> {
                        DCEvar++
                        DCEvars[DCEvar] = 0
                        term_state = TSTATE_CSI_EQUAL
                    }
                    'F' -> {
                        val newcolor: Int
                        debug("ESC [ = " + DCEvars[0] + " F")
                        attributes = attributes and COLOR_FG.inv()
                        newcolor = DCEvars[0] and 1 shl 2 or
                                (DCEvars[0] and 2) or
                                (DCEvars[0] and 4 shr 2)
                        attributes = attributes or ((newcolor + 1).toLong() shl COLOR_FG_SHIFT)
                    }
                    'G' -> {
                        val newcolor: Int
                        debug("ESC [ = " + DCEvars[0] + " G")
                        attributes = attributes and COLOR_BG.inv()
                        newcolor = DCEvars[0] and 1 shl 2 or
                                (DCEvars[0] and 2) or
                                (DCEvars[0] and 4 shr 2)
                        attributes = attributes or ((newcolor + 1).toLong() shl COLOR_BG_SHIFT)
                    }
                    else -> {
                        debugStr!!.append("Unknown ESC [ = ")
                        var i = 0
                        while (i <= DCEvar) {
                            debugStr!!.append(DCEvars[i])
                                    .append(',')
                            i++
                        }
                        debugStr!!.append(c)
                        debug(debugStr.toString())
                        debugStr!!.setLength(0)
                    }
                }
            }
            TSTATE_CSI_DOLLAR -> {
                term_state = TSTATE_DATA
                when (c) {
                    '}' -> {
                        debug("Active Status Display now " + DCEvars[0])
                        statusmode = DCEvars[0]
                    }
                    '~' -> debug("Status Line mode now " + DCEvars[0])
                    else -> debug("UNKNOWN Status Display code " + c + ", with Pn=" + DCEvars[0])
                }
            }
            TSTATE_CSI -> {
                term_state = TSTATE_DATA
                when (c) {
                    '"' -> term_state = TSTATE_CSI_TICKS
                    '$' -> term_state = TSTATE_CSI_DOLLAR
                    '=' -> term_state = TSTATE_CSI_EQUAL
                    '!' -> term_state = TSTATE_CSI_EX
                    '?' -> {
                        DCEvar = 0
                        DCEvars[0] = 0
                        term_state = TSTATE_DCEQ
                    }
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        DCEvars[DCEvar] = DCEvars[DCEvar] * 10 + c.toInt() - 48
                        term_state = TSTATE_CSI
                    }
                    ';' -> {
                        DCEvar++
                        DCEvars[DCEvar] = 0
                        term_state = TSTATE_CSI
                    }
                    'c' -> {
                        /* send (ESC[?61c) */
                        var subcode = ""
                        if (terminalID == "de.mud.terminal.vt320") subcode = "63;"
                        if (terminalID == "vt220") subcode = "62;"
                        if (terminalID == "vt100") subcode = "61;"
                        write(ESC.toString() + "[?" + subcode + "1;2c", false)
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " c")
                    }
                    'q' -> if (debug > 1) debug("ESC [ " + DCEvars[0] + " q")
                    'g' -> {
                        when (DCEvars[0]) {
                            3 -> Tabs = ByteArray(width)
                            0 -> Tabs[C] = 0
                        }
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " g")
                    }
                    'h' -> {
                        when (DCEvars[0]) {
                            4 -> insertmode = 1
                            20 -> {
                                debug("Setting CRLF to TRUE")
                                sendcrlf = true
                            }
                            else -> debug("unsupported: ESC [ " + DCEvars[0] + " h")
                        }
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " h")
                    }
                    'i' -> when (DCEvars[0]) {
                        0 -> if (debug > 1) debug("CSI 0 i:  Print Screen, not implemented.")
                        4 -> if (debug > 1) debug("CSI 4 i:  Enable Transparent Printing, not implemented.")
                        5 -> if (debug > 1) debug("CSI 4/5 i:  Disable Transparent Printing, not implemented.")
                        else -> debug("ESC [ " + DCEvars[0] + " i, unimplemented!")
                    }
                    'l' -> when (DCEvars[0]) {
                        4 -> insertmode = 0
                        20 -> {
                            debug("Setting CRLF to FALSE")
                            sendcrlf = false
                        }
                        else -> debug("ESC [ " + DCEvars[0] + " l, unimplemented!")
                    }
                    'A' -> {
                        val limit: Int
                        /* FIXME: xterm only cares about 0 and topmargin */limit = if (R >= topMargin) {
                            topMargin
                        } else 0
                        if (DCEvars[0] == 0) R-- else R -= DCEvars[0]
                        if (R < limit) R = limit
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " A")
                    }
                    'B' ->                         /* cursor down n (1) times */ {
                        val limit: Int
                        limit = if (R <= bottomMargin) {
                            bottomMargin
                        } else rows - 1
                        if (DCEvars[0] == 0) R++ else R += DCEvars[0]
                        if (R > limit) R = limit else {
                            if (debug > 2) debug("Not limited.")
                        }
                        if (debug > 2) debug("to: $R")
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " B (at C=" + C + ")")
                    }
                    'C' -> {
                        if (DCEvars[0] == 0) DCEvars[0] = 1
                        while (DCEvars[0]-- > 0) {
                            C++
                        }
                        if (C >= columns) C = columns - 1
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " C")
                    }
                    'd' -> {
                        R = DCEvars[0] - 1
                        if (R < 0) R = 0 else if (R >= height) R = height - 1
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " d")
                    }
                    'D' -> {
                        if (DCEvars[0] == 0) DCEvars[0] = 1
                        while (DCEvars[0]-- > 0) {
                            C--
                        }
                        if (C < 0) C = 0
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " D")
                    }
                    'r' -> {
                        if (DCEvar > 0) //  Ray:  Any argument is optional
                        {
                            R = DCEvars[1] - 1
                            if (R < 0) R = rows - 1 else if (R >= rows) {
                                R = rows - 1
                            }
                        } else R = rows - 1
                        val bot = R
                        if (R >= DCEvars[0]) {
                            R = DCEvars[0] - 1
                            if (R < 0) R = 0
                        }
                        setMargins(R, bot)
                        _SetCursor(0, 0)
                        if (debug > 1) debug("ESC [" + DCEvars[0] + " ; " + DCEvars[1] + " r")
                    }
                    'G' -> {
                        C = DCEvars[0] - 1
                        if (C < 0) C = 0 else if (C >= width) C = width - 1
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " G")
                    }
                    'H' -> {
                        /* gets 2 arguments */_SetCursor(DCEvars[0] - 1, DCEvars[1] - 1)
                        if (debug > 2) {
                            debug("ESC [ " + DCEvars[0] + ";" + DCEvars[1] + " H, moveoutsidemargins " + moveoutsidemargins)
                            debug("	-> R now $R, C now $C")
                        }
                    }
                    'f' -> {
                        /* gets 2 arguments */R = DCEvars[0] - 1
                        C = DCEvars[1] - 1
                        if (C < 0) C = 0 else if (C >= width) C = width - 1
                        if (R < 0) R = 0 else if (R >= height) R = height - 1
                        if (debug > 2) debug("ESC [ " + DCEvars[0] + ";" + DCEvars[1] + " f")
                    }
                    'S' -> if (DCEvars[0] == 0) insertLine(bottomMargin, SCROLL_UP) else insertLine(bottomMargin, DCEvars[0], SCROLL_UP)
                    'L' -> {
                        /* insert n lines */if (DCEvars[0] == 0) insertLine(R, SCROLL_DOWN) else insertLine(R, DCEvars[0], SCROLL_DOWN)
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + "" + c + " (at R " + R + ")")
                    }
                    'T' -> if (DCEvars[0] == 0) insertLine(topMargin, SCROLL_DOWN) else insertLine(topMargin, DCEvars[0], SCROLL_DOWN)
                    'M' -> {
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + "" + c + " at R=" + R)
                        if (DCEvars[0] == 0) deleteLine(R) else {
                            var i = 0
                            while (i < DCEvars[0]) {
                                deleteLine(R)
                                i++
                            }
                        }
                    }
                    'K' -> {
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " K")
                        when (DCEvars[0]) {
                            6, 0 -> if (C < columns - 1) deleteArea(C, R, columns - C, 1, attributes)
                            1 -> if (C > 0) deleteArea(0, R, C + 1, 1, attributes)
                            2 -> deleteArea(0, R, columns, 1, attributes)
                        }
                    }
                    'J' -> {
                        when (DCEvars[0]) {
                            0 -> {
                                if (R < rows - 1) deleteArea(0, R + 1, columns, rows - R - 1, attributes)
                                if (C < columns - 1) deleteArea(C, R, columns - C, 1, attributes)
                            }
                            1 -> {
                                if (R > 0) deleteArea(0, 0, columns, R, attributes)
                                if (C > 0) deleteArea(0, R, C + 1, 1, attributes) // include up to and including current
                            }
                            2 -> deleteArea(0, 0, columns, rows, attributes)
                        }
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " J")
                    }
                    '@' -> {
                        if (DCEvars[0] == 0) DCEvars[0] = 1
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " @")
                        var i = 0
                        while (i < DCEvars[0]) {
                            insertChar(C, R, ' ', attributes)
                            i++
                        }
                    }
                    'X' -> {
                        var toerase = DCEvars[0]
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " X, C=" + C + ",R=" + R)
                        if (toerase == 0) toerase = 1
                        if (toerase + C > columns) toerase = columns - C
                        deleteArea(C, R, toerase, 1, attributes)
                    }
                    'P' -> {
                        if (debug > 1) debug("ESC [ " + DCEvars[0] + " P, C=" + C + ",R=" + R)
                        if (DCEvars[0] == 0) DCEvars[0] = 1
                        var i = 0
                        while (i < DCEvars[0]) {
                            deleteChar(C, R)
                            i++
                        }
                    }
                    'n' -> when (DCEvars[0]) {
                        5 -> {
                            writeSpecial("$ESC[0n")
                            if (debug > 1) debug("ESC[5n")
                        }
                        6 -> {
                            // DO NOT offset R and C by 1! (checked against /usr/X11R6/bin/resize
                            // FIXME check again.
                            // FIXME: but vttest thinks different???
                            writeSpecial(ESC.toString() + "[" + R + ";" + C + "R")
                            if (debug > 1) debug("ESC[6n")
                        }
                        else -> if (debug > 0) debug("ESC [ " + DCEvars[0] + " n??")
                    }
                    's' -> {
                        Sc = C
                        Sr = R
                        Sa = attributes
                        if (debug > 3) debug("ESC[s")
                    }
                    'u' -> {
                        C = Sc
                        R = Sr
                        attributes = Sa
                        if (debug > 3) debug("ESC[u")
                    }
                    'm' -> {
                        if (debug > 3) debug("ESC [ ")
                        if (DCEvar == 0 && DCEvars[0] == 0) attributes = 0
                        var i = 0
                        while (i <= DCEvar) {
                            when (DCEvars[i]) {
                                0 -> if (DCEvar > 0) {
                                    attributes = if (terminalID == "scoansi") {
                                        attributes and COLOR /* Keeps color. Strange but true. */
                                    } else {
                                        0
                                    }
                                }
                                1 -> {
                                    attributes = attributes or BOLD
                                    attributes = attributes and LOW.inv()
                                }
                                2 ->                                     /* SCO color hack mode */if (terminalID == "scoansi" && DCEvar - i >= 2) {
                                    var ncolor: Int
                                    attributes = attributes and (COLOR or BOLD).inv()
                                    ncolor = DCEvars[i + 1]
                                    if (ncolor and 8 == 8) attributes = attributes or BOLD
                                    ncolor = ncolor and 1 shl 2 or (ncolor and 2) or (ncolor and 4 shr 2)
                                    attributes = attributes or ((ncolor + 1).toLong() shl COLOR_FG_SHIFT)
                                    ncolor = DCEvars[i + 2]
                                    ncolor = ncolor and 1 shl 2 or (ncolor and 2) or (ncolor and 4 shr 2)
                                    attributes = attributes or ((ncolor + 1).toLong() shl COLOR_BG_SHIFT)
                                    i += 2
                                } else {
                                    attributes = attributes or LOW
                                }
                                3 -> attributes = attributes or INVERT
                                4 -> attributes = attributes or UNDERLINE
                                7 -> attributes = attributes or INVERT
                                8 -> attributes = attributes or INVISIBLE
                                5 -> {
                                }
                                10 -> {
                                    gl = 0.toChar()
                                    usedcharsets = true
                                }
                                11, 12 -> {
                                    gl = 1.toChar()
                                    usedcharsets = true
                                }
                                21 -> attributes = attributes and (LOW or BOLD).inv()
                                23 -> attributes = attributes and INVERT.inv()
                                25 -> {
                                }
                                27 -> attributes = attributes and INVERT.inv()
                                28 -> attributes = attributes and INVISIBLE.inv()
                                24 -> attributes = attributes and UNDERLINE.inv()
                                22 -> attributes = attributes and BOLD.inv()
                                30, 31, 32, 33, 34, 35, 36, 37 -> {
                                    attributes = attributes and COLOR_FG.inv()
                                    attributes = attributes or ((DCEvars[i] - 30 + 1).toLong() shl COLOR_FG_SHIFT)
                                }
                                38 -> if (DCEvars[i + 1] == 5) {
                                    attributes = attributes and COLOR_FG.inv()
                                    attributes = attributes or ((DCEvars[i + 2] + 1).toLong() shl COLOR_FG_SHIFT)
                                    i += 2
                                } else if (DCEvars[i + 1] == 2) {
                                    attributes = attributes and COLOR_FG.inv()
                                    val newcolor = DCEvars[i + 2] shl COLOR_RED_SHIFT or (
                                            DCEvars[i + 3] shl COLOR_GREEN_SHIFT) or (
                                            DCEvars[i + 4] shl COLOR_BLUE_SHIFT)
                                    attributes = attributes or ((newcolor + 257).toLong() shl COLOR_FG_SHIFT)
                                    i += 4
                                }
                                39 -> attributes = attributes and COLOR_FG.inv()
                                40, 41, 42, 43, 44, 45, 46, 47 -> {
                                    attributes = attributes and COLOR_BG.inv()
                                    attributes = attributes or ((DCEvars[i] - 40 + 1).toLong() shl COLOR_BG_SHIFT)
                                }
                                48 -> if (DCEvars[i + 1] == 5) {
                                    attributes = attributes and COLOR_BG.inv()
                                    attributes = attributes or ((DCEvars[i + 2] + 1).toLong() shl COLOR_BG_SHIFT)
                                    i += 2
                                } else if (DCEvars[i + 1] == 2) {
                                    attributes = attributes and COLOR_BG.inv()
                                    val newcolor = DCEvars[i + 2] shl COLOR_RED_SHIFT or (
                                            DCEvars[i + 3] shl COLOR_GREEN_SHIFT) or (
                                            DCEvars[i + 4] shl COLOR_BLUE_SHIFT)
                                    attributes = attributes or ((newcolor + 257).toLong() shl COLOR_BG_SHIFT)
                                    i += 4
                                }
                                49 -> attributes = attributes and COLOR_BG.inv()
                                90, 91, 92, 93, 94, 95, 96, 97 -> {
                                    attributes = attributes and COLOR_FG.inv()
                                    attributes = attributes or ((DCEvars[i] - 82 + 1).toLong() shl COLOR_FG_SHIFT)
                                }
                                100, 101, 102, 103, 104, 105, 106, 107 -> {
                                    attributes = attributes and COLOR_BG.inv()
                                    attributes = attributes or ((DCEvars[i] - 92 + 1).toLong() shl COLOR_BG_SHIFT)
                                }
                                else -> {
                                    debugStr!!.append("ESC [ ")
                                            .append(DCEvars[i])
                                            .append(" m unknown...")
                                    debug(debugStr.toString())
                                    debugStr!!.setLength(0)
                                }
                            }
                            if (debug > 3) {
                                debugStr!!.append(DCEvars[i])
                                        .append(';')
                                debug(debugStr.toString())
                                debugStr!!.setLength(0)
                            }
                            i++
                        }
                        if (debug > 3) {
                            debugStr!!.append(" (attributes = ")
                                    .append(attributes)
                                    .append(")m")
                            debug(debugStr.toString())
                            debugStr!!.setLength(0)
                        }
                    }
                    else -> {
                        debugStr!!.append("ESC [ unknown letter: ")
                                .append(c)
                                .append(" (")
                                .append(c.toInt())
                                .append(')')
                        debug(debugStr.toString())
                        debugStr!!.setLength(0)
                    }
                }
            }
            TSTATE_TITLE -> when (c) {
                ESC -> term_state = TSTATE_ESC
                else -> {
                }
            }
            else -> term_state = TSTATE_DATA
        }
        setCursorPosition(C, R)
    }

    /* hard reset the terminal */
    fun reset() {
        gx[0] = 'B'
        gx[1] = 'B'
        gx[2] = 'B'
        gx[3] = 'B'
        gl = 0.toChar() // default GL to G0
        gr = 2.toChar() // default GR to G2
        onegl = -1 // Single shift override

        /* reset tabs */
        var nw = width
        if (nw < 132) nw = 132
        Tabs = ByteArray(nw)
        var i = 0
        while (i < nw) {
            Tabs[i] = 1
            i += 8
        }
        deleteArea(0, 0, width, height, attributes)
        setMargins(0, height)
        R = 0
        C = R
        _SetCursor(0, 0)
        if (display != null) display.resetColors()
        showCursor(true)
        /*FIXME:*/term_state = TSTATE_DATA
    }

    companion object {
        /**
         * The current version id tag.<P>
         * $Id: de.mud.terminal.vt320.java 507 2005-10-25 10:14:52Z marcus $
        </P> */
        const val ID = "\$Id: de.mud.terminal.vt320.java 507 2005-10-25 10:14:52Z marcus $"

        /**
         * the debug level
         */
        private const val debug = 0
        private const val ESC = 27.toChar()
        private const val IND = 132.toChar()
        private const val NEL = 133.toChar()
        private const val RI = 141.toChar()
        private const val SS2 = 142.toChar()
        private const val SS3 = 143.toChar()
        private const val DCS = 144.toChar()
        private const val HTS = 136.toChar()
        private const val CSI = 155.toChar()
        private const val OSC = 157.toChar()
        private const val TSTATE_DATA = 0
        private const val TSTATE_ESC = 1 /* ESC */
        private const val TSTATE_CSI = 2 /* ESC [ */
        private const val TSTATE_DCS = 3 /* ESC P */
        private const val TSTATE_DCEQ = 4 /* ESC [? */
        private const val TSTATE_ESCSQUARE = 5 /* ESC # */
        private const val TSTATE_OSC = 6 /* ESC ] */
        private const val TSTATE_SETG0 = 7 /* ESC (? */
        private const val TSTATE_SETG1 = 8 /* ESC )? */
        private const val TSTATE_SETG2 = 9 /* ESC *? */
        private const val TSTATE_SETG3 = 10 /* ESC +? */
        private const val TSTATE_CSI_DOLLAR = 11 /* ESC [ Pn $ */
        private const val TSTATE_CSI_EX = 12 /* ESC [ ! */
        private const val TSTATE_ESCSPACE = 13 /* ESC <space> */
        private const val TSTATE_VT52X = 14
        private const val TSTATE_VT52Y = 15
        private const val TSTATE_CSI_TICKS = 16
        private const val TSTATE_CSI_EQUAL = 17 /* ESC [ = */
        private const val TSTATE_TITLE = 18 /* xterm title */

        /* Keys we support */
        const val KEY_PAUSE = 1
        const val KEY_F1 = 2
        const val KEY_F2 = 3
        const val KEY_F3 = 4
        const val KEY_F4 = 5
        const val KEY_F5 = 6
        const val KEY_F6 = 7
        const val KEY_F7 = 8
        const val KEY_F8 = 9
        const val KEY_F9 = 10
        const val KEY_F10 = 11
        const val KEY_F11 = 12
        const val KEY_F12 = 13
        const val KEY_UP = 14
        const val KEY_DOWN = 15
        const val KEY_LEFT = 16
        const val KEY_RIGHT = 17
        const val KEY_PAGE_DOWN = 18
        const val KEY_PAGE_UP = 19
        const val KEY_INSERT = 20
        const val KEY_DELETE = 21
        const val KEY_BACK_SPACE = 22
        const val KEY_HOME = 23
        const val KEY_END = 24
        const val KEY_NUM_LOCK = 25
        const val KEY_CAPS_LOCK = 26
        const val KEY_SHIFT = 27
        const val KEY_CONTROL = 28
        const val KEY_ALT = 29
        const val KEY_ENTER = 30
        const val KEY_NUMPAD0 = 31
        const val KEY_NUMPAD1 = 32
        const val KEY_NUMPAD2 = 33
        const val KEY_NUMPAD3 = 34
        const val KEY_NUMPAD4 = 35
        const val KEY_NUMPAD5 = 36
        const val KEY_NUMPAD6 = 37
        const val KEY_NUMPAD7 = 38
        const val KEY_NUMPAD8 = 39
        const val KEY_NUMPAD9 = 40
        const val KEY_DECIMAL = 41
        const val KEY_ADD = 42
        const val KEY_ESCAPE = 43
        const val DELETE_IS_DEL = 0
        const val DELETE_IS_BACKSPACE = 1

        // Map from scoansi linedrawing to DEC _and_ unicode (for the stuff which
        // is not in linedrawing). Got from experimenting with scoadmin.
        private const val scoansi_acs = "Tm7k3x4u?kZl@mYjEnB\u2566DqCtAvM\u2550:\u2551N\u2557I\u2554;\u2557H\u255a0a<\u255d"

        // array to store DEC Special -> Unicode mapping
        //  Unicode   DEC  Unicode name    (DEC name)
        private val DECSPECIAL = charArrayOf(
                '\u0040',  //5f blank
                '\u2666',  //60 black diamond
                '\u2592',  //61 grey square
                '\u2409',  //62 Horizontal tab  (ht) pict. for control
                '\u240c',  //63 Form Feed       (ff) pict. for control
                '\u240d',  //64 Carriage Return (cr) pict. for control
                '\u240a',  //65 Line Feed       (lf) pict. for control
                '\u00ba',  //66 Masculine ordinal indicator
                '\u00b1',  //67 Plus or minus sign
                '\u2424',  //68 New Line        (nl) pict. for control
                '\u240b',  //69 Vertical Tab    (vt) pict. for control
                '\u2518',  //6a Forms light up   and left
                '\u2510',  //6b Forms light down and left
                '\u250c',  //6c Forms light down and right
                '\u2514',  //6d Forms light up   and right
                '\u253c',  //6e Forms light vertical and horizontal
                '\u2594',  //6f Upper 1/8 block                        (Scan 1)
                '\u2580',  //70 Upper 1/2 block                        (Scan 3)
                '\u2500',  //71 Forms light horizontal or ?em dash?    (Scan 5)
                '\u25ac',  //72 \u25ac black rect. or \u2582 lower 1/4 (Scan 7)
                '\u005f',  //73 \u005f underscore  or \u2581 lower 1/8 (Scan 9)
                '\u251c',  //74 Forms light vertical and right
                '\u2524',  //75 Forms light vertical and left
                '\u2534',  //76 Forms light up   and horizontal
                '\u252c',  //77 Forms light down and horizontal
                '\u2502',  //78 vertical bar
                '\u2264',  //79 less than or equal
                '\u2265',  //7a greater than or equal
                '\u00b6',  //7b paragraph
                '\u2260',  //7c not equal
                '\u00a3',  //7d Pound Sign (british)
                '\u00b7' //7e Middle Dot
        )

        /**
         * Replace escape code characters (backslash + identifier) with their
         * respective codes.
         *
         * @param tmp the string to be parsed
         * @return a unescaped string
         */
        fun unEscape(tmp: String): String {
            var idx = 0
            var oldidx = 0
            var cmd: String
            // f.println("unescape("+tmp+")");
            cmd = ""
            while (tmp.indexOf('\\', oldidx).also { idx = it } >= 0 &&
                    ++idx <= tmp.length) {
                cmd += tmp.substring(oldidx, idx - 1)
                if (idx == tmp.length) return cmd
                when (tmp[idx]) {
                    'b' -> cmd += "\b"
                    'e' -> cmd += "\u001b"
                    'n' -> cmd += "\n"
                    'r' -> cmd += "\r"
                    't' -> cmd += "\t"
                    'v' -> cmd += "\u000b"
                    'a' -> cmd += "\u0012"
                    else -> if (tmp[idx] >= '0' && tmp[idx] <= '9') {
                        var i: Int
                        i = idx
                        while (i < tmp.length) {
                            if (tmp[i] < '0' || tmp[i] > '9') break
                            i++
                        }
                        cmd += tmp.substring(idx, i).toInt().toChar()
                        idx = i - 1
                    } else cmd += tmp.substring(idx, ++idx)
                }
                oldidx = ++idx
            }
            if (oldidx <= tmp.length) cmd += tmp.substring(oldidx)
            return cmd
        }

        private val unimap = charArrayOf( //#
                //#    Name:     cp437_DOSLatinUS to Unicode table
                //#    Unicode version: 1.1
                //#    Table version: 1.1
                //#    Table format:  Format A
                //#    Date:          03/31/95
                //#    Authors:       Michel Suignard <michelsu@microsoft.com>
                //#                   Lori Hoerth <lorih@microsoft.com>
                //#    General notes: none
                //#
                //#    Format: Three tab-separated columns
                //#        Column #1 is the cp1255_WinHebrew code (in hex)
                //#        Column #2 is the Unicode (in hex as 0xXXXX)
                //#        Column #3 is the Unicode name (follows a comment sign, '#')
                //#
                //#    The entries are in cp437_DOSLatinUS order
                //#
                0x0000.toChar(),  // #NULL
                0x0001.toChar(),  // #START OF HEADING
                0x0002.toChar(),  // #START OF TEXT
                0x0003.toChar(),  // #END OF TEXT
                0x0004.toChar(),  // #END OF TRANSMISSION
                0x0005.toChar(),  // #ENQUIRY
                0x0006.toChar(),  // #ACKNOWLEDGE
                0x0007.toChar(),  // #BELL
                0x0008.toChar(),  // #BACKSPACE
                0x0009.toChar(),  // #HORIZONTAL TABULATION
                0x000a.toChar(),  // #LINE FEED
                0x000b.toChar(),  // #VERTICAL TABULATION
                0x000c.toChar(),  // #FORM FEED
                0x000d.toChar(),  // #CARRIAGE RETURN
                0x000e.toChar(),  // #SHIFT OUT
                0x000f.toChar(),  // #SHIFT IN
                0x0010.toChar(),  // #DATA LINK ESCAPE
                0x0011.toChar(),  // #DEVICE CONTROL ONE
                0x0012.toChar(),  // #DEVICE CONTROL TWO
                0x0013.toChar(),  // #DEVICE CONTROL THREE
                0x0014.toChar(),  // #DEVICE CONTROL FOUR
                0x0015.toChar(),  // #NEGATIVE ACKNOWLEDGE
                0x0016.toChar(),  // #SYNCHRONOUS IDLE
                0x0017.toChar(),  // #END OF TRANSMISSION BLOCK
                0x0018.toChar(),  // #CANCEL
                0x0019.toChar(),  // #END OF MEDIUM
                0x001a.toChar(),  // #SUBSTITUTE
                0x001b.toChar(),  // #ESCAPE
                0x001c.toChar(),  // #FILE SEPARATOR
                0x001d.toChar(),  // #GROUP SEPARATOR
                0x001e.toChar(),  // #RECORD SEPARATOR
                0x001f.toChar(),  // #UNIT SEPARATOR
                0x0020.toChar(),  // #SPACE
                0x0021.toChar(),  // #EXCLAMATION MARK
                0x0022.toChar(),  // #QUOTATION MARK
                0x0023.toChar(),  // #NUMBER SIGN
                0x0024.toChar(),  // #DOLLAR SIGN
                0x0025.toChar(),  // #PERCENT SIGN
                0x0026.toChar(),  // #AMPERSAND
                0x0027.toChar(),  // #APOSTROPHE
                0x0028.toChar(),  // #LEFT PARENTHESIS
                0x0029.toChar(),  // #RIGHT PARENTHESIS
                0x002a.toChar(),  // #ASTERISK
                0x002b.toChar(),  // #PLUS SIGN
                0x002c.toChar(),  // #COMMA
                0x002d.toChar(),  // #HYPHEN-MINUS
                0x002e.toChar(),  // #FULL STOP
                0x002f.toChar(),  // #SOLIDUS
                0x0030.toChar(),  // #DIGIT ZERO
                0x0031.toChar(),  // #DIGIT ONE
                0x0032.toChar(),  // #DIGIT TWO
                0x0033.toChar(),  // #DIGIT THREE
                0x0034.toChar(),  // #DIGIT FOUR
                0x0035.toChar(),  // #DIGIT FIVE
                0x0036.toChar(),  // #DIGIT SIX
                0x0037.toChar(),  // #DIGIT SEVEN
                0x0038.toChar(),  // #DIGIT EIGHT
                0x0039.toChar(),  // #DIGIT NINE
                0x003a.toChar(),  // #COLON
                0x003b.toChar(),  // #SEMICOLON
                0x003c.toChar(),  // #LESS-THAN SIGN
                0x003d.toChar(),  // #EQUALS SIGN
                0x003e.toChar(),  // #GREATER-THAN SIGN
                0x003f.toChar(),  // #QUESTION MARK
                0x0040.toChar(),  // #COMMERCIAL AT
                0x0041.toChar(),  // #LATIN CAPITAL LETTER A
                0x0042.toChar(),  // #LATIN CAPITAL LETTER B
                0x0043.toChar(),  // #LATIN CAPITAL LETTER C
                0x0044.toChar(),  // #LATIN CAPITAL LETTER D
                0x0045.toChar(),  // #LATIN CAPITAL LETTER E
                0x0046.toChar(),  // #LATIN CAPITAL LETTER F
                0x0047.toChar(),  // #LATIN CAPITAL LETTER G
                0x0048.toChar(),  // #LATIN CAPITAL LETTER H
                0x0049.toChar(),  // #LATIN CAPITAL LETTER I
                0x004a.toChar(),  // #LATIN CAPITAL LETTER J
                0x004b.toChar(),  // #LATIN CAPITAL LETTER K
                0x004c.toChar(),  // #LATIN CAPITAL LETTER L
                0x004d.toChar(),  // #LATIN CAPITAL LETTER M
                0x004e.toChar(),  // #LATIN CAPITAL LETTER N
                0x004f.toChar(),  // #LATIN CAPITAL LETTER O
                0x0050.toChar(),  // #LATIN CAPITAL LETTER P
                0x0051.toChar(),  // #LATIN CAPITAL LETTER Q
                0x0052.toChar(),  // #LATIN CAPITAL LETTER R
                0x0053.toChar(),  // #LATIN CAPITAL LETTER S
                0x0054.toChar(),  // #LATIN CAPITAL LETTER T
                0x0055.toChar(),  // #LATIN CAPITAL LETTER U
                0x0056.toChar(),  // #LATIN CAPITAL LETTER V
                0x0057.toChar(),  // #LATIN CAPITAL LETTER W
                0x0058.toChar(),  // #LATIN CAPITAL LETTER X
                0x0059.toChar(),  // #LATIN CAPITAL LETTER Y
                0x005a.toChar(),  // #LATIN CAPITAL LETTER Z
                0x005b.toChar(),  // #LEFT SQUARE BRACKET
                0x005c.toChar(),  // #REVERSE SOLIDUS
                0x005d.toChar(),  // #RIGHT SQUARE BRACKET
                0x005e.toChar(),  // #CIRCUMFLEX ACCENT
                0x005f.toChar(),  // #LOW LINE
                0x0060.toChar(),  // #GRAVE ACCENT
                0x0061.toChar(),  // #LATIN SMALL LETTER A
                0x0062.toChar(),  // #LATIN SMALL LETTER B
                0x0063.toChar(),  // #LATIN SMALL LETTER C
                0x0064.toChar(),  // #LATIN SMALL LETTER D
                0x0065.toChar(),  // #LATIN SMALL LETTER E
                0x0066.toChar(),  // #LATIN SMALL LETTER F
                0x0067.toChar(),  // #LATIN SMALL LETTER G
                0x0068.toChar(),  // #LATIN SMALL LETTER H
                0x0069.toChar(),  // #LATIN SMALL LETTER I
                0x006a.toChar(),  // #LATIN SMALL LETTER J
                0x006b.toChar(),  // #LATIN SMALL LETTER K
                0x006c.toChar(),  // #LATIN SMALL LETTER L
                0x006d.toChar(),  // #LATIN SMALL LETTER M
                0x006e.toChar(),  // #LATIN SMALL LETTER N
                0x006f.toChar(),  // #LATIN SMALL LETTER O
                0x0070.toChar(),  // #LATIN SMALL LETTER P
                0x0071.toChar(),  // #LATIN SMALL LETTER Q
                0x0072.toChar(),  // #LATIN SMALL LETTER R
                0x0073.toChar(),  // #LATIN SMALL LETTER S
                0x0074.toChar(),  // #LATIN SMALL LETTER T
                0x0075.toChar(),  // #LATIN SMALL LETTER U
                0x0076.toChar(),  // #LATIN SMALL LETTER V
                0x0077.toChar(),  // #LATIN SMALL LETTER W
                0x0078.toChar(),  // #LATIN SMALL LETTER X
                0x0079.toChar(),  // #LATIN SMALL LETTER Y
                0x007a.toChar(),  // #LATIN SMALL LETTER Z
                0x007b.toChar(),  // #LEFT CURLY BRACKET
                0x007c.toChar(),  // #VERTICAL LINE
                0x007d.toChar(),  // #RIGHT CURLY BRACKET
                0x007e.toChar(),  // #TILDE
                0x007f.toChar(),  // #DELETE
                0x00c7.toChar(),  // #LATIN CAPITAL LETTER C WITH CEDILLA
                0x00fc.toChar(),  // #LATIN SMALL LETTER U WITH DIAERESIS
                0x00e9.toChar(),  // #LATIN SMALL LETTER E WITH ACUTE
                0x00e2.toChar(),  // #LATIN SMALL LETTER A WITH CIRCUMFLEX
                0x00e4.toChar(),  // #LATIN SMALL LETTER A WITH DIAERESIS
                0x00e0.toChar(),  // #LATIN SMALL LETTER A WITH GRAVE
                0x00e5.toChar(),  // #LATIN SMALL LETTER A WITH RING ABOVE
                0x00e7.toChar(),  // #LATIN SMALL LETTER C WITH CEDILLA
                0x00ea.toChar(),  // #LATIN SMALL LETTER E WITH CIRCUMFLEX
                0x00eb.toChar(),  // #LATIN SMALL LETTER E WITH DIAERESIS
                0x00e8.toChar(),  // #LATIN SMALL LETTER E WITH GRAVE
                0x00ef.toChar(),  // #LATIN SMALL LETTER I WITH DIAERESIS
                0x00ee.toChar(),  // #LATIN SMALL LETTER I WITH CIRCUMFLEX
                0x00ec.toChar(),  // #LATIN SMALL LETTER I WITH GRAVE
                0x00c4.toChar(),  // #LATIN CAPITAL LETTER A WITH DIAERESIS
                0x00c5.toChar(),  // #LATIN CAPITAL LETTER A WITH RING ABOVE
                0x00c9.toChar(),  // #LATIN CAPITAL LETTER E WITH ACUTE
                0x00e6.toChar(),  // #LATIN SMALL LIGATURE AE
                0x00c6.toChar(),  // #LATIN CAPITAL LIGATURE AE
                0x00f4.toChar(),  // #LATIN SMALL LETTER O WITH CIRCUMFLEX
                0x00f6.toChar(),  // #LATIN SMALL LETTER O WITH DIAERESIS
                0x00f2.toChar(),  // #LATIN SMALL LETTER O WITH GRAVE
                0x00fb.toChar(),  // #LATIN SMALL LETTER U WITH CIRCUMFLEX
                0x00f9.toChar(),  // #LATIN SMALL LETTER U WITH GRAVE
                0x00ff.toChar(),  // #LATIN SMALL LETTER Y WITH DIAERESIS
                0x00d6.toChar(),  // #LATIN CAPITAL LETTER O WITH DIAERESIS
                0x00dc.toChar(),  // #LATIN CAPITAL LETTER U WITH DIAERESIS
                0x00a2.toChar(),  // #CENT SIGN
                0x00a3.toChar(),  // #POUND SIGN
                0x00a5.toChar(),  // #YEN SIGN
                0x20a7.toChar(),  // #PESETA SIGN
                0x0192.toChar(),  // #LATIN SMALL LETTER F WITH HOOK
                0x00e1.toChar(),  // #LATIN SMALL LETTER A WITH ACUTE
                0x00ed.toChar(),  // #LATIN SMALL LETTER I WITH ACUTE
                0x00f3.toChar(),  // #LATIN SMALL LETTER O WITH ACUTE
                0x00fa.toChar(),  // #LATIN SMALL LETTER U WITH ACUTE
                0x00f1.toChar(),  // #LATIN SMALL LETTER N WITH TILDE
                0x00d1.toChar(),  // #LATIN CAPITAL LETTER N WITH TILDE
                0x00aa.toChar(),  // #FEMININE ORDINAL INDICATOR
                0x00ba.toChar(),  // #MASCULINE ORDINAL INDICATOR
                0x00bf.toChar(),  // #INVERTED QUESTION MARK
                0x2310.toChar(),  // #REVERSED NOT SIGN
                0x00ac.toChar(),  // #NOT SIGN
                0x00bd.toChar(),  // #VULGAR FRACTION ONE HALF
                0x00bc.toChar(),  // #VULGAR FRACTION ONE QUARTER
                0x00a1.toChar(),  // #INVERTED EXCLAMATION MARK
                0x00ab.toChar(),  // #LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
                0x00bb.toChar(),  // #RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
                0x2591.toChar(),  // #LIGHT SHADE
                0x2592.toChar(),  // #MEDIUM SHADE
                0x2593.toChar(),  // #DARK SHADE
                0x2502.toChar(),  // #BOX DRAWINGS LIGHT VERTICAL
                0x2524.toChar(),  // #BOX DRAWINGS LIGHT VERTICAL AND LEFT
                0x2561.toChar(),  // #BOX DRAWINGS VERTICAL SINGLE AND LEFT DOUBLE
                0x2562.toChar(),  // #BOX DRAWINGS VERTICAL DOUBLE AND LEFT SINGLE
                0x2556.toChar(),  // #BOX DRAWINGS DOWN DOUBLE AND LEFT SINGLE
                0x2555.toChar(),  // #BOX DRAWINGS DOWN SINGLE AND LEFT DOUBLE
                0x2563.toChar(),  // #BOX DRAWINGS DOUBLE VERTICAL AND LEFT
                0x2551.toChar(),  // #BOX DRAWINGS DOUBLE VERTICAL
                0x2557.toChar(),  // #BOX DRAWINGS DOUBLE DOWN AND LEFT
                0x255d.toChar(),  // #BOX DRAWINGS DOUBLE UP AND LEFT
                0x255c.toChar(),  // #BOX DRAWINGS UP DOUBLE AND LEFT SINGLE
                0x255b.toChar(),  // #BOX DRAWINGS UP SINGLE AND LEFT DOUBLE
                0x2510.toChar(),  // #BOX DRAWINGS LIGHT DOWN AND LEFT
                0x2514.toChar(),  // #BOX DRAWINGS LIGHT UP AND RIGHT
                0x2534.toChar(),  // #BOX DRAWINGS LIGHT UP AND HORIZONTAL
                0x252c.toChar(),  // #BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
                0x251c.toChar(),  // #BOX DRAWINGS LIGHT VERTICAL AND RIGHT
                0x2500.toChar(),  // #BOX DRAWINGS LIGHT HORIZONTAL
                0x253c.toChar(),  // #BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
                0x255e.toChar(),  // #BOX DRAWINGS VERTICAL SINGLE AND RIGHT DOUBLE
                0x255f.toChar(),  // #BOX DRAWINGS VERTICAL DOUBLE AND RIGHT SINGLE
                0x255a.toChar(),  // #BOX DRAWINGS DOUBLE UP AND RIGHT
                0x2554.toChar(),  // #BOX DRAWINGS DOUBLE DOWN AND RIGHT
                0x2569.toChar(),  // #BOX DRAWINGS DOUBLE UP AND HORIZONTAL
                0x2566.toChar(),  // #BOX DRAWINGS DOUBLE DOWN AND HORIZONTAL
                0x2560.toChar(),  // #BOX DRAWINGS DOUBLE VERTICAL AND RIGHT
                0x2550.toChar(),  // #BOX DRAWINGS DOUBLE HORIZONTAL
                0x256c.toChar(),  // #BOX DRAWINGS DOUBLE VERTICAL AND HORIZONTAL
                0x2567.toChar(),  // #BOX DRAWINGS UP SINGLE AND HORIZONTAL DOUBLE
                0x2568.toChar(),  // #BOX DRAWINGS UP DOUBLE AND HORIZONTAL SINGLE
                0x2564.toChar(),  // #BOX DRAWINGS DOWN SINGLE AND HORIZONTAL DOUBLE
                0x2565.toChar(),  // #BOX DRAWINGS DOWN DOUBLE AND HORIZONTAL SINGLE
                0x2559.toChar(),  // #BOX DRAWINGS UP DOUBLE AND RIGHT SINGLE
                0x2558.toChar(),  // #BOX DRAWINGS UP SINGLE AND RIGHT DOUBLE
                0x2552.toChar(),  // #BOX DRAWINGS DOWN SINGLE AND RIGHT DOUBLE
                0x2553.toChar(),  // #BOX DRAWINGS DOWN DOUBLE AND RIGHT SINGLE
                0x256b.toChar(),  // #BOX DRAWINGS VERTICAL DOUBLE AND HORIZONTAL SINGLE
                0x256a.toChar(),  // #BOX DRAWINGS VERTICAL SINGLE AND HORIZONTAL DOUBLE
                0x2518.toChar(),  // #BOX DRAWINGS LIGHT UP AND LEFT
                0x250c.toChar(),  // #BOX DRAWINGS LIGHT DOWN AND RIGHT
                0x2588.toChar(),  // #FULL BLOCK
                0x2584.toChar(),  // #LOWER HALF BLOCK
                0x258c.toChar(),  // #LEFT HALF BLOCK
                0x2590.toChar(),  // #RIGHT HALF BLOCK
                0x2580.toChar(),  // #UPPER HALF BLOCK
                0x03b1.toChar(),  // #GREEK SMALL LETTER ALPHA
                0x00df.toChar(),  // #LATIN SMALL LETTER SHARP S
                0x0393.toChar(),  // #GREEK CAPITAL LETTER GAMMA
                0x03c0.toChar(),  // #GREEK SMALL LETTER PI
                0x03a3.toChar(),  // #GREEK CAPITAL LETTER SIGMA
                0x03c3.toChar(),  // #GREEK SMALL LETTER SIGMA
                0x00b5.toChar(),  // #MICRO SIGN
                0x03c4.toChar(),  // #GREEK SMALL LETTER TAU
                0x03a6.toChar(),  // #GREEK CAPITAL LETTER PHI
                0x0398.toChar(),  // #GREEK CAPITAL LETTER THETA
                0x03a9.toChar(),  // #GREEK CAPITAL LETTER OMEGA
                0x03b4.toChar(),  // #GREEK SMALL LETTER DELTA
                0x221e.toChar(),  // #INFINITY
                0x03c6.toChar(),  // #GREEK SMALL LETTER PHI
                0x03b5.toChar(),  // #GREEK SMALL LETTER EPSILON
                0x2229.toChar(),  // #INTERSECTION
                0x2261.toChar(),  // #IDENTICAL TO
                0x00b1.toChar(),  // #PLUS-MINUS SIGN
                0x2265.toChar(),  // #GREATER-THAN OR EQUAL TO
                0x2264.toChar(),  // #LESS-THAN OR EQUAL TO
                0x2320.toChar(),  // #TOP HALF INTEGRAL
                0x2321.toChar(),  // #BOTTOM HALF INTEGRAL
                0x00f7.toChar(),  // #DIVISION SIGN
                0x2248.toChar(),  // #ALMOST EQUAL TO
                0x00b0.toChar(),  // #DEGREE SIGN
                0x2219.toChar(),  // #BULLET OPERATOR
                0x00b7.toChar(),  // #MIDDLE DOT
                0x221a.toChar(),  // #SQUARE ROOT
                0x207f.toChar(),  // #SUPERSCRIPT LATIN SMALL LETTER N
                0x00b2.toChar(),  // #SUPERSCRIPT TWO
                0x25a0.toChar(),  // #BLACK SQUARE
                0x00a0.toChar())
    }
    /**
     * Create a new de.mud.terminal.vt320 terminal and intialize it with useful settings.
     */
    /**
     * Create a default de.mud.terminal.vt320 terminal with 80 columns and 24 lines.
     */
    init {
        debugStr = StringBuilder()
        setVMS(false)
        setIBMCharset(false)
        setTerminalID("de.mud.terminal.vt320")
        bufferSize = 100
        //setBorder(2, false);
        gx = CharArray(4)
        reset()

        /* top row of numpad */PF1 = "\u001bOP"
        PF2 = "\u001bOQ"
        PF3 = "\u001bOR"
        PF4 = "\u001bOS"

        /* the 3x2 keyblock on PC keyboards */Insert = arrayOfNulls(4)
        Remove = arrayOfNulls(4)
        KeyHome = arrayOfNulls(4)
        KeyEnd = arrayOfNulls(4)
        NextScn = arrayOfNulls(4)
        PrevScn = arrayOfNulls(4)
        Escape = arrayOfNulls(4)
        BackSpace = arrayOfNulls(4)
        TabKey = arrayOfNulls(4)
        Insert[3] = "\u001b[2~"
        Insert[2] = Insert[3]
        Insert[1] = Insert[2]
        Insert[0] = Insert[1]
        Remove[3] = "\u001b[3~"
        Remove[2] = Remove[3]
        Remove[1] = Remove[2]
        Remove[0] = Remove[1]
        PrevScn[3] = "\u001b[5~"
        PrevScn[2] = PrevScn[3]
        PrevScn[1] = PrevScn[2]
        PrevScn[0] = PrevScn[1]
        NextScn[3] = "\u001b[6~"
        NextScn[2] = NextScn[3]
        NextScn[1] = NextScn[2]
        NextScn[0] = NextScn[1]
        KeyHome[3] = "\u001b[H"
        KeyHome[2] = KeyHome[3]
        KeyHome[1] = KeyHome[2]
        KeyHome[0] = KeyHome[1]
        KeyEnd[3] = "\u001b[F"
        KeyEnd[2] = KeyEnd[3]
        KeyEnd[1] = KeyEnd[2]
        KeyEnd[0] = KeyEnd[1]
        Escape[3] = "\u001b"
        Escape[2] = Escape[3]
        Escape[1] = Escape[2]
        Escape[0] = Escape[1]
        if (vms) {
            BackSpace[1] = "" + 10.toChar() //  VMS shift deletes word back
            BackSpace[2] = "\u0018" //  VMS control deletes line back
            BackSpace[3] = "\u007f"
            BackSpace[0] = BackSpace[3] //  VMS other is delete
        } else {
            //BackSpace[0] = BackSpace[1] = BackSpace[2] = BackSpace[3] = "\b";
            // ConnectBot modifications.
            BackSpace[0] = "\b"
            BackSpace[1] = "\u007f"
            BackSpace[2] = "\u001b[3~"
            BackSpace[3] = "\u001b[2~"
        }

        /* some more VT100 keys */Find = "\u001b[1~"
        Select = "\u001b[4~"
        Help = "\u001b[28~"
        Do = "\u001b[29~"
        FunctionKey = arrayOfNulls(21)
        FunctionKey[0] = ""
        FunctionKey[1] = PF1
        FunctionKey[2] = PF2
        FunctionKey[3] = PF3
        FunctionKey[4] = PF4
        /* following are defined differently for vt220 / vt132 ... */FunctionKey[5] = "\u001b[15~"
        FunctionKey[6] = "\u001b[17~"
        FunctionKey[7] = "\u001b[18~"
        FunctionKey[8] = "\u001b[19~"
        FunctionKey[9] = "\u001b[20~"
        FunctionKey[10] = "\u001b[21~"
        FunctionKey[11] = "\u001b[23~"
        FunctionKey[12] = "\u001b[24~"
        FunctionKey[13] = "\u001b[25~"
        FunctionKey[14] = "\u001b[26~"
        FunctionKey[15] = Help
        FunctionKey[16] = Do
        FunctionKey[17] = "\u001b[31~"
        FunctionKey[18] = "\u001b[32~"
        FunctionKey[19] = "\u001b[33~"
        FunctionKey[20] = "\u001b[34~"
        FunctionKeyShift = arrayOfNulls(21)
        FunctionKeyAlt = arrayOfNulls(21)
        FunctionKeyCtrl = arrayOfNulls(21)
        for (i in 0..19) {
            FunctionKeyShift[i] = ""
            FunctionKeyAlt[i] = ""
            FunctionKeyCtrl[i] = ""
        }
        FunctionKeyShift[15] = Find
        FunctionKeyShift[16] = Select
        TabKey[0] = "\u0009"
        TabKey[1] = "\u001bOP\u0009"
        TabKey[3] = ""
        TabKey[2] = TabKey[3]
        KeyUp = arrayOfNulls(4)
        KeyUp[0] = "\u001b[A"
        KeyDown = arrayOfNulls(4)
        KeyDown[0] = "\u001b[B"
        KeyRight = arrayOfNulls(4)
        KeyRight[0] = "\u001b[C"
        KeyLeft = arrayOfNulls(4)
        KeyLeft[0] = "\u001b[D"
        Numpad = arrayOfNulls(10)
        Numpad[0] = "\u001bOp"
        Numpad[1] = "\u001bOq"
        Numpad[2] = "\u001bOr"
        Numpad[3] = "\u001bOs"
        Numpad[4] = "\u001bOt"
        Numpad[5] = "\u001bOu"
        Numpad[6] = "\u001bOv"
        Numpad[7] = "\u001bOw"
        Numpad[8] = "\u001bOx"
        Numpad[9] = "\u001bOy"
        KPMinus = PF4
        KPComma = "\u001bOl"
        KPPeriod = "\u001bOn"
        KPEnter = "\u001bOM"
        NUMPlus = arrayOfNulls(4)
        NUMPlus[0] = "+"
        NUMDot = arrayOfNulls(4)
        NUMDot[0] = "."
    }
}