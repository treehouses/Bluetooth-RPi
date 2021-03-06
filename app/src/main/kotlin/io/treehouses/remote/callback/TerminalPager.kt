package io.treehouses.remote.callback

import android.view.LayoutInflater
import android.view.animation.Animation
import io.treehouses.remote.ssh.terminal.TerminalViewPager
import io.treehouses.remote.ssh.terminal.TerminalManager
import android.os.Handler

interface TerminalPager {
    fun handleData()
    fun getPager(): TerminalViewPager
    fun getInflater(): LayoutInflater
    fun getManager(): TerminalManager?
    fun getAnimation(): Animation?
    fun getHandler(): Handler
}