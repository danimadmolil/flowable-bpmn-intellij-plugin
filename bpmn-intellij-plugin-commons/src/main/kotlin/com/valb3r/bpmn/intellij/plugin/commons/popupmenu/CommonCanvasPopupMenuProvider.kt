
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementChangeEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.snapToGridIfNecessary
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import kotlin.reflect.KClass

private fun <T: WithBpmnId> newShapeElement(project: Project, sceneLocation: Point2D.Float, forObject: T): ShapeElement {
    val templateShape = newElementsFactory(project).newDiagramObject(ShapeElement::class, forObject)

    val bounds = templateShape.rectBounds()
    val snappedLocation = snapToGridIfNecessary(sceneLocation.x, sceneLocation.y)
    return templateShape.copy(
            bounds = BoundsElement(
                    snappedLocation.x,
                    snappedLocation.y,
                    bounds.width,
                    bounds.height
            )
    )
}

private fun <T: WithBpmnId> newShapeElementWithBounds(project: Project, bounds: BoundsElement, forObject: T): ShapeElement {
    val templateShape = newElementsFactory(project).newDiagramObject(ShapeElement::class, forObject)

    return templateShape.copy(
        bounds = bounds
        )
}

class ShapeCreator<T : WithBpmnId> (private val project: Project, private val clazz: KClass<T>, private val sceneLocation: Point2D.Float, private val parent: BpmnElementId): ActionListener {

    override fun actionPerformed(e: ActionEvent?) {
        val newObject = newElementsFactory(project).newBpmnObject(clazz)
        val shape = newShapeElement(project, sceneLocation, newObject)

        updateEventsRegistry(project).addObjectEvent(
                BpmnShapeObjectAddedEvent(WithParentId(parent, newObject), shape, newElementsFactory(project).propertiesOf(newObject))
        )
    }
}

class ShapeChange<T : WithBpmnId>(
    private val project: Project,
    private val clazz: KClass<T>,
    private val elementId: BpmnElementId) : ActionListener {
    override fun actionPerformed(e: ActionEvent?) {
        val newElement = newElementsFactory(project).newBpmnObject(clazz).updateBpmnElemId(elementId)
        val currentElement = currentStateProvider(project).currentState().elementByBpmnId[elementId]!!.element
        if(newElement.javaClass == currentElement.javaClass) return
        val oldPropertyTable = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[elementId]
        val newPropertyTable = newElementsFactory(project).propertiesOf(newElement)
        oldPropertyTable!!.view().forEach { (t, u) ->
            if (null != newPropertyTable[t]) {
                newPropertyTable[t] = u.toMutableList()
            }
        }

        updateEventsRegistry(project).addEvents(
            listOf(
                BpmnElementChangeEvent(
                    elementId,
                    newElement,
                    newPropertyTable,
                    currentStateProvider(project).currentState().elementByBpmnId[elementId]!!.parentIdForXml
                )
            )
        )
    }
}
