package com.github.ljacqu.ijpackagehighlighter.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level persistent settings holding package groups (prefix -> background RGB).
 */
@State(name = "PackageHighlightSettings", storages = [Storage("package-highlighter.xml")])
class PackageHighlightSettings : PersistentStateComponent<PackageHighlightSettings.State?> {
    class PackageGroup {
        @JvmField
        var prefix: String? = ""
        @JvmField
        var rgb: Int = 0xFFFF00 // default yellow background

        // needed for XML deserialization
        constructor()

        constructor(prefix: String?, rgb: Int) {
            this.prefix = prefix
            this.rgb = rgb
        }
    }

    class State {
        var groups: MutableList<PackageGroup?> = ArrayList<PackageGroup?>()
    }

    private val myState = State()

    override fun getState(): State? {
        return myState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean<State>(state, myState)

        // Provide a sensible default if empty (optional)
        if (myState.groups.isEmpty()) {
            myState.groups.add(PackageGroup("java.", 0xFFF2CC)) // soft beige for JDK
            myState.groups.add(PackageGroup("javax.", 0xE2F0D9)) // soft green
            myState.groups.add(PackageGroup("org.springframework.", 0xDDEBF7)) // pale blue
        }
    }

    var groups: MutableList<PackageGroup?>?
        // Convenience getters/setters
        get() = myState.groups
        set(groups) {
            myState.groups =
                if (groups != null) groups else ArrayList<PackageGroup?>()
        }

    companion object {
        @JvmStatic
        val instance: PackageHighlightSettings?
            // Convenience: returns singleton application service
            get() = ServiceManager.getService<PackageHighlightSettings?>(
                PackageHighlightSettings::class.java
            )
    }
}