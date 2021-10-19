package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.core.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.core.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.elemIdToRemove
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.ResizeViewTransform
import java.awt.geom.Point2D

class PhysicalWaypoint(
        elementId: DiagramElementId,
        attachedTo: DiagramElementId?,
        private val parentElementId: DiagramElementId,
        private val parentElementBpmnId: BpmnElementId?,
        private val edge: EdgeWithIdentifiableWaypoints,
        private val physicalPos: Int,
        private val edgePhysicalSize: Int,
        location: Point2D.Float,
        state: () -> RenderState
): CircleAnchorElement(elementId, attachedTo, location, 3.0f, Colors.WAYPOINT_COLOR, state) {

    val owningEdgeId: DiagramElementId
        get() = parentElementId

    override val areaType: AreaType
        get() = AreaType.POINT

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        if (isEdgeBeginOrEnd()) {
            return mutableMapOf()
        }

        val delId = elementId.elemIdToRemove()
        val deleteIconArea = state().ctx.canvas.drawIcon(BoundsElement(x, y - ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state().icons.recycleBin)
        state().ctx.interactionContext.clickCallbacks[delId] = { dest ->
            dest.addEvents(listOf(NewWaypointsEvent(
                    parentElementId,
                    edge.waypoint
                            .filter { it.physical }
                            .filter { it.id != elementId }
                            .toList(),
                    edge.epoch + 1
            )))
        }
        return mutableMapOf(delId to AreaWithZindex(deleteIconArea, areaType, mutableSetOf(), mutableSetOf(),  ICON_Z_INDEX, elementId))
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        val events = mutableListOf<Event>()

        // TODO: Transform
        events += DraggedToEvent(elementId, dx, dy, parentElementId, physicalPos) as Event

        if (null == parentElementBpmnId) {
            return events
        }

        val state = state().currentState
        val currentProps = state.propertyWithElementByPropertyType
        val rootProcessId = state.processId
        if (null != droppedOn && !multipleElementsSelected() && !multipleElementsDragged()) {
            if (edgePhysicalSize - 1 == physicalPos) {
                events += StringValueUpdatedEvent(parentElementBpmnId, PropertyType.TARGET_REF, droppedOn.id)
                handleBpmnIncomingCascade(events, currentProps, droppedOn, rootProcessId, parentElementBpmnId)
            } else if (0 == physicalPos) {
                events += StringValueUpdatedEvent(parentElementBpmnId, PropertyType.SOURCE_REF, droppedOn.id)
                handleBpmnOutgoingCascade(events, currentProps, droppedOn, rootProcessId, parentElementBpmnId)
            }
        }

        return events
    }

    override fun doComputeLocationChangesBasedOnTransformationWithCascade(): MutableList<Event> {
        val transform = state().viewTransform(elementId)
        if (transform !is ResizeViewTransform) {
            return mutableListOf()
        }

        // TODO: Transform
        return mutableListOf(
                DraggedToEvent(
                        elementId,
                        transformedLocation.x - currentLocation.x,
                        transformedLocation.y - currentLocation.y,
                        parentElementId,
                        physicalPos
                )
        )
    }

    override fun ifVisibleNoRenderIf(): Boolean {
        return !isRenderable()
    }

    override fun isRenderable(): Boolean {
        return if ((isEdgeBegin() && isSourceRefAttached()) || (isEdgeEnd() && isTargetRefAttached())) {
            return !multipleElementsSelected()
        } else {
            true
        }
    }

    private fun isEdgeBegin() = 0 == physicalPos
    private fun isEdgeEnd() = edgePhysicalSize - 1 == physicalPos
    private fun isSourceRefAttached() = null != state().currentState.elemPropertiesByStaticElementId[parentElementBpmnId]?.get(PropertyType.SOURCE_REF)?.value
    private fun isTargetRefAttached() = null != state().currentState.elemPropertiesByStaticElementId[parentElementBpmnId]?.get(PropertyType.TARGET_REF)?.value
    private fun isEdgeBeginOrEnd() = edgePhysicalSize - 1 == physicalPos || 0 == physicalPos

    private fun handleBpmnOutgoingCascade(
        events: MutableList<Event>,
        currentProps: Map<PropertyType, Map<BpmnElementId, Property>>,
        droppedOn: BpmnElementId,
        rootProcessId: BpmnElementId,
        parentElementBpmnId: BpmnElementId
    ) {
        events += computeCascadeChangeOfBpmnIncomingOutgoingIndex(currentProps, PropertyType.BPMN_OUTGOING)
        if (droppedOn != rootProcessId) {
            events += StringValueUpdatedEvent(droppedOn, PropertyType.BPMN_INCOMING, parentElementBpmnId.id, propertyIndex = listOf(parentElementBpmnId.id))
        }
    }

    private fun handleBpmnIncomingCascade(
        events: MutableList<Event>,
        currentProps: Map<PropertyType, Map<BpmnElementId, Property>>,
        droppedOn: BpmnElementId,
        rootProcessId: BpmnElementId,
        parentElementBpmnId: BpmnElementId
    ) {
        events += computeCascadeChangeOfBpmnIncomingOutgoingIndex(currentProps, PropertyType.BPMN_INCOMING)
        if (droppedOn != rootProcessId) {
            events += StringValueUpdatedEvent(droppedOn, PropertyType.BPMN_INCOMING, parentElementBpmnId.id, propertyIndex = listOf(parentElementBpmnId.id))
        }
    }

    private fun computeCascadeChangeOfBpmnIncomingOutgoingIndex(props: Map<PropertyType, Map<BpmnElementId, Property>>, propertyType: PropertyType): List<StringValueUpdatedEvent> {
        val defaultIndex = emptyList<StringValueUpdatedEvent>()
        val elementId = parentElementBpmnId ?: return defaultIndex
        return props[propertyType]
            ?.filter { it.value.value == elementId.id }
            ?.map { StringValueUpdatedEvent(it.key, propertyType, "", propertyIndex = it.value.index) } ?: emptyList()
    }
}