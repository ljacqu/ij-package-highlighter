package com.github.ljacqu.ijpackagehighlighter.config

import com.github.ljacqu.ijpackagehighlighter.config.PackageHighlightSettings.Companion.instance
import com.github.ljacqu.ijpackagehighlighter.config.PackageHighlightSettings.PackageGroup
import com.intellij.openapi.options.SearchableConfigurable
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * Application-level settings page to manage package groups and their background colors.
 * Simple UI: list of groups on left, edit fields on right, Add/Remove buttons below.
 */
class PackageHighlightConfigurable : SearchableConfigurable {
    private val root: JPanel
    private val groupList: JList<PackageHighlightSettings.PackageGroup?>
    private val listModel: DefaultListModel<PackageHighlightSettings.PackageGroup?>
    private val prefixField: JTextField
    private val colorButton: JButton
    private val addButton: JButton
    private val removeButton: JButton

    // Keep track of currently selected group's color
    private var currentColor: Color? = Color.YELLOW

    init {
        listModel = DefaultListModel<PackageHighlightSettings.PackageGroup?>()
        groupList = JList<PackageHighlightSettings.PackageGroup?>(listModel)
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        prefixField = JTextField()
        colorButton = JButton("Pick Color")
        addButton = JButton("Add")
        removeButton = JButton("Remove")

        // build root panel
        root = JPanel(BorderLayout(8, 8))
        val left = JPanel(BorderLayout())
        left.add(JScrollPane(groupList), BorderLayout.CENTER)

        val leftButtons = JPanel(FlowLayout(FlowLayout.LEFT))
        leftButtons.add(addButton)
        leftButtons.add(removeButton)
        left.add(leftButtons, BorderLayout.SOUTH)

        val right = JPanel()
        right.setLayout(BoxLayout(right, BoxLayout.Y_AXIS))
        right.add(JLabel("Package prefix (e.g. java., org.springframework.)"))
        right.add(prefixField)
        right.add(Box.createVerticalStrut(8))
        right.add(JLabel("Background color"))
        right.add(colorButton)
        right.add(Box.createVerticalGlue())

        root.add(left, BorderLayout.CENTER)
        root.add(right, BorderLayout.EAST)

        // listeners
        addButton.addActionListener(ActionListener { e: ActionEvent? -> this.onAdd(e) })
        removeButton.addActionListener(ActionListener { e: ActionEvent? -> onRemove() })
        colorButton.addActionListener(ActionListener { e: ActionEvent? -> onPickColor() })
        groupList.addListSelectionListener(ListSelectionListener { e: ListSelectionEvent? -> onSelect() })

        prefixField.getDocument().addDocumentListener(object : DocumentListener {
            private fun update() {
                applyPrefixToSelected()
            }

            override fun insertUpdate(e: DocumentEvent?) {
                update()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                update()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                update()
            }
        })
    }

    private fun onAdd(e: ActionEvent?) {
        val g = PackageHighlightSettings.PackageGroup("new.prefix.", 0xFFFF00)
        listModel.addElement(g)
        groupList.setSelectedValue(g, true)
    }

    private fun onRemove() {
        val idx = groupList.getSelectedIndex()
        if (idx >= 0) listModel.remove(idx)
    }

    private fun onPickColor() {
        val chosen = JColorChooser.showDialog(root, "Choose background color", currentColor)
        if (chosen != null) {
            currentColor = chosen
            // update selected group's rgb and refresh list
            val sel = groupList.getSelectedValue()
            if (sel != null) {
                sel.rgb = chosen.getRGB() and 0xFFFFFF // store 24-bit RGB
                groupList.repaint()
            }
        }
    }

    private fun onSelect() {
        val sel = groupList.getSelectedValue()
        if (sel != null) {
            prefixField.setText(sel.prefix)
            currentColor = Color(sel.rgb)
        } else {
            prefixField.setText("")
        }
    }

    private fun applyPrefixToSelected() {
        val sel = groupList.getSelectedValue()
        if (sel != null) {
            sel.prefix = prefixField.getText()
            groupList.repaint()
        }
    }

    override fun getId(): String {
        return "settings.packageHighlight"
    }

    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "Package-based Type Highlighter"
    }

    override fun createComponent(): JComponent? {
        // load current settings into UI
        listModel.clear()
        val settings = instance
        if (settings != null) {
            for (g in settings.groups!!) {
                listModel.addElement(g)
            }
        }
        if (!listModel.isEmpty()) groupList.setSelectedIndex(0)
        return root
    }

    override fun isModified(): Boolean {
        // compare UI state with persisted settings
        val settings = instance
        if (settings == null) return false
        val current: MutableList<PackageHighlightSettings.PackageGroup> =
            ArrayList<PackageHighlightSettings.PackageGroup>()
        for (i in 0..<listModel.getSize()) current.add(listModel.get(i)!!)
        val persisted: MutableList<PackageGroup?>? = settings.groups
        if (current.size != persisted!!.size) return true
        for (i in current.indices) {
            val a = current.get(i)
            val b = persisted.get(i)
            if (a.prefix != b?.prefix || a.rgb != b?.rgb) return true
        }
        return false
    }

    override fun apply() {
        val settings = instance
        if (settings != null) {
            val newList: MutableList<PackageHighlightSettings.PackageGroup?> =
                ArrayList<PackageHighlightSettings.PackageGroup?>()
            for (i in 0..<listModel.size()) newList.add(listModel.get(i))
            settings.groups = newList
        }
    }

    override fun reset() {
        // reload from persisted
        listModel.clear()
        val settings = instance
        if (settings != null) {
            for (g in settings.groups!!) listModel.addElement(g)
        }
    }

    override fun disposeUIResources() {
    }
}