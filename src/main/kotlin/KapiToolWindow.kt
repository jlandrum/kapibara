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
import io.github.rybalkinsd.kohttp.dsl.*
import io.github.rybalkinsd.kohttp.ext.EagerResponse
import io.github.rybalkinsd.kohttp.ext.eager
import io.github.rybalkinsd.kohttp.ext.httpGet
import io.swagger.models.Swagger
import io.swagger.parser.Swagger20Parser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.parser.OpenAPIV3Parser
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import okhttp3.Response
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

        private var activeDocument: Any? = null
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
                webview?.engine?.isJavaScriptEnabled = true
                (webview?.engine?.executeScript("window") as JSObject).also {
                    it.call("ready")
                    it.setMember("JB", JavaBridge)
                }
            }
        }

        private fun updateTheme() =
            webview?.engine?.document?.documentElement?.setAttribute("class", if (UIUtil.isUnderDarcula()) "dark" else "light")


        private fun updateActiveDocument(document: Document) = Platform.runLater {
            val mapper = ObjectMapper()
            fdm.getFile(document)?.let {
                val oap : OpenAPI? = openApi.read(it.path)
                val swg: Swagger? = swagger.read(it.path, emptyList())
                activeDocument = oap ?: swg
                val json = try { mapper.writeValueAsString(activeDocument) } catch (e:Exception) { "{}" } ?: "{}"
                webview?.engine?.executeScript("updateContent($json)")
            }
        }

        object JavaBridge {
            fun log(obj: Any) {
                println(obj)
            }
            public fun executeApiCall(host: String, path: String, method: String) : HttpResponse {
                val url = URL(host+path)
                return HttpResponse(when (method.toLowerCase()) {
                    "get" -> httpGet {
                        this.host = url.host
                        this.path = url.path
                    }
                    "put" -> httpPut {
                        this.host = url.host
                        this.path = url.path
                    }
                    "post" -> httpPost {
                        this.host = url.host
                        this.path = url.path
                    }
                    "delete" -> httpDelete {
                        this.host = url.host
                        this.path = url.path
                    }
                    "head" -> httpHead {
                        this.host = url.host
                        this.path = url.path
                    }
                    "patch" -> httpPatch {
                        this.host = url.host
                        this.path = url.path
                    }
                    else -> null
                }?.eager())
            }
        }

        class HttpResponse(response: EagerResponse?) {
            val body = response?.body ?:""
            val code = response?.code ?: 0
            val message = response?.message ?: ""
        }
    }
}

private fun String.asResource(): URL = KapiToolWindowFactory::class.java.getResource(this)
private fun String.asResourceStream(): InputStream = KapiToolWindowFactory::class.java.getResourceAsStream(this)
