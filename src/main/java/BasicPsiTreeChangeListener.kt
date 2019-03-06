import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener

open class BasicPsiTreeChangeListener : PsiTreeChangeListener {
    override fun beforePropertyChange(event: PsiTreeChangeEvent) {}
    override fun childReplaced(event: PsiTreeChangeEvent) {}
    override fun childrenChanged(event: PsiTreeChangeEvent) {}
    override fun beforeChildAddition(event: PsiTreeChangeEvent) {}
    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {}
    override fun propertyChanged(event: PsiTreeChangeEvent) {}
    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {}
    override fun childMoved(event: PsiTreeChangeEvent) {}
    override fun childRemoved(event: PsiTreeChangeEvent) {}
    override fun beforeChildMovement(event: PsiTreeChangeEvent) {}
    override fun childAdded(event: PsiTreeChangeEvent) {}
    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {}
}
