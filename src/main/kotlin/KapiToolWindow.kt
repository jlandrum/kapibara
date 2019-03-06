import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.layout.CCFlags.*
import com.intellij.ui.layout.CCFlags.grow
import com.intellij.ui.layout.panel
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.web.WebView
import java.awt.Color
import java.awt.Paint
import java.awt.font.TextAttribute
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class KapiToolWindowFactory : ToolWindowFactory, TypedHandlerDelegate() {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val kapiWindowView = KapiToolWindowView(FileEditorManager.getInstance(project).selectedTextEditor?.document)
        kapiWindowView.ready(project)
        toolWindow.component.add(kapiWindowView)
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        return Result.CONTINUE
    }

    private class KapiToolWindowView(activeDoc: Document?) : JPanel(), DocumentListener, FileEditorManagerListener {
        private var webview : WebView? = null
        private val fdm = FileDocumentManager.getInstance()
        private val openApi = OpenAPIV3Parser()
        private val panel = JFXPanel()

        init {
            activeDoc?.let { updateActiveDocument(it) }
            add(panel)
        }

        fun ready(project: Project) = Platform.runLater {
            project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
            Platform.setImplicitExit(true)
            webview = WebView()
            //panel.scene = Scene(webview)
            panel.background = Color.GREEN
        }

        override fun selectionChanged(event: FileEditorManagerEvent) {
            event.newFile?.let {
                fdm.getDocument(it)?.let { doc -> updateActiveDocument(doc) }
            }
        }

        private fun updateActiveDocument(document: Document) {
            fdm.getFile(document)?.let {
                val api : OpenAPI? = openApi.read(it.path)
                if (api != null) {
                    webview?.engine?.loadContent("""
                        <html>
                            <center>
                                <b>${it.name} OpenAPI: ${api.openapi}</b><br/>
                                Title: ${api.info.title}<br/>
                                Description: ${api.info.description}<br/>
                                Version: ${api.info.version}
                            </center>
                        </html>
                    """.trimIndent())
                } else {
                    webview?.engine?.loadContent("""
                        <html>
                            <center>
                                <b>This is not a valid OpenAPI document.</b>
                            </center>
                        </html>
                    """.trimIndent())

                }
            }
        }
    }
}