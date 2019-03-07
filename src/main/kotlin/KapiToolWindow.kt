import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiFile
import com.intellij.util.ui.UIUtil
import io.swagger.models.Swagger
import io.swagger.parser.Swagger20Parser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.awt.GridLayout
import java.io.InputStream
import java.net.URL
import javax.swing.JPanel
import javax.swing.UIManager

class KapiToolWindowFactory : ToolWindowFactory, TypedHandlerDelegate() {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val kapiWindowView = KapiToolWindowView()
        kapiWindowView.ready(project)
        toolWindow.component.add(kapiWindowView)
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        return Result.CONTINUE
    }

    private class KapiToolWindowView() : JPanel(), DocumentListener, FileEditorManagerListener,
        ChangeListener<Worker.State> {

        private var webview : WebView? = null
        private val fdm = FileDocumentManager.getInstance()
        private val openApi = OpenAPIV3Parser()
        private val swagger = Swagger20Parser()
        private val panel = JFXPanel()

        init {
            add(panel)
            layout = GridLayout(0,1)
        }

        fun ready(project: Project) = Platform.runLater {
            project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
            UIManager.addPropertyChangeListener { if (it.propertyName == "lookAndFeel") Platform.runLater { updateTheme() } }
            Platform.setImplicitExit(false)
            webview = WebView()
            webview?.engine?.let {
                it.isJavaScriptEnabled = true
                it.loadWorker.stateProperty().addListener(this@KapiToolWindowView)
                it.load("/interface.vue.html".asResource().toExternalForm())
                it.isJavaScriptEnabled = true
            }
            panel.scene = Scene(webview)
        }.also {
            FileEditorManager.getInstance(project).selectedTextEditor?.document?.let { updateActiveDocument(it) }
        }

        override fun selectionChanged(event: FileEditorManagerEvent) {
            event.newFile?.let {
                fdm.getDocument(it)?.let { doc -> updateActiveDocument(doc) }
            }
        }

        override fun changed(observable: ObservableValue<out Worker.State>?, oldValue: Worker.State?, newValue: Worker.State?) {
            if (newValue == Worker.State.SUCCEEDED) {
                updateTheme()
                (webview?.engine?.executeScript("window") as JSObject).call("ready")
            }
        }

        private fun updateTheme() =
            webview?.engine?.document?.documentElement?.setAttribute("class", if (UIUtil.isUnderDarcula()) "dark" else "light")


        private fun updateActiveDocument(document: Document) = Platform.runLater {
            val mapper = ObjectMapper()
            fdm.getFile(document)?.let {
                val api : OpenAPI? = openApi.read(it.path)
                val swagger: Swagger? = swagger.read(it.path, emptyList())
                val json = try { mapper.writeValueAsString(api ?: swagger) } catch (e:Exception) { "{}" } ?: "{}"
                webview?.engine?.executeScript("updateContent($json)")
                if (api != null) {
//                    webview?.engine?.loadContent("""
//                        <html>
//                            <center>
//                                <b>${it.name} OpenAPI: ${api.api}</b><br/>
//                                Title: ${api.info.title}<br/>
//                                Description: ${api.info.description}<br/>
//                                Version: ${api.info.version}
//                            </center>
//                        </html>
//                    """.trimIndent())
                } else {
//                    webview?.engine?.loadContent("""
//                        <html>
//                            <center>
//                                <b>This is not a valid OpenAPI document.</b>
//                            </center>
//                        </html>
//                    """.trimIndent())
                }
            }
        }
    }
}

private fun String.asResource(): URL = KapiToolWindowFactory::class.java.getResource(this)
private fun String.asResourceStream(): InputStream = KapiToolWindowFactory::class.java.getResourceAsStream(this)
