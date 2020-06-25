package io.treehouses.remote

import android.util.TypedValue
import androidx.fragment.app.FragmentActivity
import io.treehouses.remote.databinding.*
import io.treehouses.remote.utils.SaveUtils
import me.toptas.fancyshowcase.FancyShowCaseQueue
import me.toptas.fancyshowcase.FancyShowCaseView
import me.toptas.fancyshowcase.FocusShape

object Tutorials {

    fun homeTutorials(bind: ActivityHomeFragmentBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.HOME)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.HOME, false)
        //Put animations here
        fun fancyShowCaseViewBuilderHome(activity: FragmentActivity, title:String, delay:Int):FancyShowCaseView.Builder {
            return FancyShowCaseView.Builder(activity)
                    .title(title)
                    .delay(delay)
                    .enableAutoTextPosition()
        }
        val a = fancyShowCaseViewBuilderHome(activity,"Test Bluetooth Connection to RPI", 750 )
                .focusOn(bind.testConnection)
                .build()

        val b = fancyShowCaseViewBuilderHome(activity,"Configure Network Profiles in the Network Screen to quickly switch between network configurations", 500 )
                .focusOn(bind.networkProfiles)
                .titleSize(18, TypedValue.COMPLEX_UNIT_SP)
                .focusCircleRadiusFactor(1.25)
                .build()
        val queue = FancyShowCaseQueue().add(a).add(b)
        queue.show()
    }

    fun networkTutorials(bind: NewNetworkBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.NETWORK)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.NETWORK, false)
        //Put animations here
    }

    fun systemTutorials(bind: ActivitySystemFragmentBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.SYSTEM)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.SYSTEM, false)
        //Put animations here

    }

    fun terminalTutorials(bind: ActivityTerminalFragmentBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.TERMINAL)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.TERMINAL, false)
        //Put animations here
        fun fancyShowCaseViewBuilderTerminal(activity: FragmentActivity, title:String, delay:Int, focusShape:FocusShape):FancyShowCaseView.Builder {
            return FancyShowCaseView.Builder(activity)
                    .title(title)
                    .delay(delay)
                    .enableAutoTextPosition()
                    .backgroundColor(R.color.focusColor)
                    .focusShape(focusShape)
                    .fitSystemWindows(true)
        }

        val a = fancyShowCaseViewBuilderTerminal(activity,"Enter Commands here to run on Pi Remotely", 750, FocusShape.ROUNDED_RECTANGLE)
                .focusOn(bind.editTextOut)
                .build()

        val b = fancyShowCaseViewBuilderTerminal(activity,"You can Save your Commands here to use them without typing again", 500, FocusShape.ROUNDED_RECTANGLE)
                .focusOn(bind.terminalList)
                .build()

        val c = fancyShowCaseViewBuilderTerminal(activity,"Access Recently used Commands on Successive taps of this button", 500, FocusShape.CIRCLE)
                .focusOn(bind.btnPrevious)
                .build()

        val d = fancyShowCaseViewBuilderTerminal(activity,"Get Information on what Treehouses Commands are Available and how to use them", 500, FocusShape.CIRCLE)
                .focusOn(bind.infoButton)
                .build()

        val queue = FancyShowCaseQueue().add(a).add(b).add(c).add(d)
        queue.show()

    }

    fun servicesOverviewTutorials(bind: ActivityServicesTabFragmentBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.SERVICES_OVERVIEW)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.SERVICES_OVERVIEW, false)
        //Put animations here


    }

    fun servicesDetailsTutorials(bind: ActivityServicesDetailsBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.SERVICES_DETAILS)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.SERVICES_DETAILS, false)
        //Put animations here

    }

    fun tunnelTutorials(bind: ActivityTunnelSshFragmentBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.TUNNEL)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.TUNNEL, false)
        //Put animations here


    }

    fun statusTutorials(bind: ActivityStatusFragmentBinding, activity: FragmentActivity) {
        if (!SaveUtils.getFragmentFirstTime(activity, SaveUtils.Screens.STATUS)) return
        SaveUtils.setFragmentFirstTime(activity, SaveUtils.Screens.STATUS, false)
        //Put animations here


    }


}