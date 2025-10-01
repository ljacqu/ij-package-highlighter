package com.github.ljacqu.ijpackagehighlighter.startup

import com.github.ljacqu.ijpackagehighlighter.config.PackageHighlightSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Color
import java.awt.Font

/**
 * Annotates Java type references that appear in method signatures and catch clauses.
 * Applies background highlighting based on package prefix configured in PackageHighlightSettings.
 */
class PackageHighlighter : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiJavaCodeReferenceElement) return
        val ref = element

        if (!isInTargetContext(ref)) return

        val qualifiedName = resolveQualifiedName(ref)
        if (qualifiedName == null) return

        // Get configured mapping (ordered)
        val mapping = buildMappingFromSettings()

        var bgRgb: Int? = null
        for (e in mapping.entries) {
            if (qualifiedName.startsWith(e.key!!)) {
                bgRgb = e.value
                break
            }
        }

        if (bgRgb == null) {
            // nothing matched; do not annotate
            return
        }

        // Build TextAttributes with background color; keep foreground null so it doesn't change text color.
        val bg = Color(bgRgb)
        val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)

        // Create an info annotation and enforce the attributes (background)
        val a = holder.createInfoAnnotation(ref, null)
        a.setEnforcedTextAttributes(attrs)
    }

    private fun buildMappingFromSettings(): MutableMap<String?, Int?> {
        val m: MutableMap<String?, Int?> = LinkedHashMap<String?, Int?>()
        val settings = PackageHighlightSettings.instance
        if (settings != null) {
            val groups = settings.groups
            if (groups != null) {
                for (g in groups) {
                    if (g?.prefix != null && g.prefix?.trim { it <= ' ' }?.isEmpty() == true) {
                        m.put(g.prefix, g.rgb)
                    }
                }
            }
        }
        return m
    }

    private fun resolveQualifiedName(ref: PsiJavaCodeReferenceElement): String? {
        val resolved = ref.resolve()
        if (resolved is PsiClass) {
            return resolved.getQualifiedName()
        }
        // best-effort fallback
        val q = ref.getQualifiedName()
        if (q != null) return q
        // try to synthesize via qualifier
        val qualifier = ref.getQualifier()
        if (qualifier is PsiJavaCodeReferenceElement) {
            val qname = qualifier.getQualifiedName()
            if (qname != null) return qname + "." + ref.getReferenceName()
        }
        return null
    }

    // same context detection logic as before (method return, params, throws list, catch param)
    private fun isInTargetContext(ref: PsiJavaCodeReferenceElement?): Boolean {
        val containingMethod = PsiTreeUtil.getParentOfType<PsiMethod?>(ref, PsiMethod::class.java, false)
        if (containingMethod != null) {
            val returnType = containingMethod.getReturnTypeElement()
            if (returnType != null && PsiTreeUtil.isAncestor(returnType, ref!!, false)) return true

            val param = PsiTreeUtil.getParentOfType<PsiParameter?>(ref, PsiParameter::class.java, false)
            if (param != null && param.getDeclarationScope() === containingMethod) return true

            val throwsList = PsiTreeUtil.getParentOfType<PsiReferenceList?>(ref, PsiReferenceList::class.java, false)
            if (throwsList != null && throwsList.getRole() == PsiReferenceList.Role.THROWS_LIST && throwsList.getParent() === containingMethod) {
                return true
            }
        }

        val catchParam = PsiTreeUtil.getParentOfType<PsiParameter?>(ref, PsiParameter::class.java, false)
        if (catchParam != null && catchParam.getDeclarationScope() is PsiCatchSection) {
            return true
        }

        val refList = PsiTreeUtil.getParentOfType<PsiReferenceList?>(ref, PsiReferenceList::class.java, false)
        if (refList != null && refList.getRole() == PsiReferenceList.Role.THROWS_LIST) {
            return true
        }

        return false
    }
}