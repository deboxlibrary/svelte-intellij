package dev.blachut.svelte.lang.psi

import com.intellij.lang.javascript.psi.impl.JSFileImpl
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlElementType
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.HtmlUtil
import dev.blachut.svelte.lang.SvelteJSLanguage
import dev.blachut.svelte.lang.parsing.js.SvelteJSScriptContentProvider

class SvelteHtmlFile(viewProvider: FileViewProvider) : JSFileImpl(viewProvider, SvelteJSLanguage.INSTANCE), XmlFile {
    val moduleScript
        get() = document?.children?.find {
            it is XmlTag && HtmlUtil.isScriptTag(it) && it.getAttributeValue(
                "context"
            ) == "module"
        } as XmlTag?

    // By convention instanceScript is placed after module script
    // so it makes sense to resolve last script in case of ambiguity from missing context attribute
    // ambiguous scripts should then be highlighted by appropriate inspection
    val instanceScript
        get() = document?.children?.findLast {
            it is XmlTag && HtmlUtil.isScriptTag(it) && it.getAttributeValue(
                "context"
            ) == null
        } as XmlTag?

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean {
        document ?: return true

        val parentScript = findAncestorScript(place)
        if (parentScript != null && isModuleScript(parentScript)) {
            // place is inside module script, nothing more to process
            return true
        } else if (parentScript != null) {
            // place is inside instance script, process module script if available
            return processScriptDeclarations(processor, state, lastParent, place, moduleScript)
        } else {
            // place is inside template expression, process instance and then module script if available
            return processScriptDeclarations(processor, state, lastParent, place, instanceScript) &&
                processScriptDeclarations(processor, state, lastParent, place, moduleScript)
        }
    }

    private fun processScriptDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement,
        script: PsiElement?
    ): Boolean {
        return SvelteJSScriptContentProvider.getJsEmbeddedContent(script)
            ?.processDeclarations(processor, state, lastParent, place) ?: true
    }

    override fun toString(): String {
        return "SvelteHtmlFile: $name"
    }

    private fun isModuleScript(tag: XmlTag): Boolean {
        return HtmlUtil.isScriptTag(tag) && tag.getAttributeValue("context") == "module"
    }

    override fun getDocument(): XmlDocument? {
        val treeElement: CompositeElement = calcTreeElement()
        val node = treeElement.findChildByType(XmlElementType.HTML_DOCUMENT)
        return if (node != null) node.psi as XmlDocument else null
    }

    override fun getRootTag(): XmlTag? {
        return document?.rootTag
    }

    override fun processElements(processor: PsiElementProcessor<*>?, place: PsiElement?): Boolean {
        val document = document
        return document == null || document.processElements(processor, place)
    }

    override fun getFileResolveScope(): GlobalSearchScope {
        return ProjectScope.getAllScope(project)
    }

    override fun ignoreReferencedElementAccessibility(): Boolean {
        return true
    }
}

private fun findAncestorScript(place: PsiElement): XmlTag? {
    val parentScript = PsiTreeUtil.findFirstContext(place, false) {
        it is XmlTag && HtmlUtil.isScriptTag(it)
    }
    return parentScript as XmlTag?
}
