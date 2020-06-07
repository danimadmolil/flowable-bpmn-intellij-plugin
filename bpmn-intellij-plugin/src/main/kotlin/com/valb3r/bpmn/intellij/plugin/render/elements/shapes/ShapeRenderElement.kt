package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.State
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorBottom
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorTop
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.Event
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class ShapeRenderElement(
        private val shape: ShapeElement,
        elemState: State,
        state: CurrentState,
        parent: BaseRenderElement?,
        private val childrenElems: Set<BaseRenderElement>
): BaseRenderElement(elemState, state, parent) {

    override val children: Set<BaseRenderElement>
        get() = anchors(state).toList().toSet() + childrenElems

    override fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val elem = state.elementByDiagramId[shape.id]
        val props = state.elemPropertiesByStaticElementId[elem]
        val name = props?.get(PropertyType.NAME)?.value as String?

        return doRender(
                ctx,
                ShapeCtx(
                        shape.id,
                        elem,
                        currentRect(ctx.canvas.camera),
                        props,
                        name
                )
        )
    }

    abstract fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex>

    override fun doDragToWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?) {
        TODO("Not yet implemented")
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        val rect = currentRect(camera)
        val halfWidth = rect.width / 2.0f
        val halfHeight = rect.height / 2.0f

        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy),

                Point2D.Float(cx - halfWidth, cy),
                Point2D.Float(cx + halfWidth, cy),
                Point2D.Float(cx, cy - halfHeight),
                Point2D.Float(cx, cy + halfHeight),

                Point2D.Float(cx - halfWidth / 2.0f, cy - halfHeight),
                Point2D.Float(cx + halfWidth / 2.0f, cy - halfHeight),
                Point2D.Float(cx - halfWidth / 2.0f, cy + halfHeight),
                Point2D.Float(cx + halfWidth / 2.0f, cy + halfHeight),

                Point2D.Float(cx - halfWidth, cy - halfHeight / 2.0f),
                Point2D.Float(cx - halfWidth, cy + halfHeight / 2.0f),
                Point2D.Float(cx + halfWidth, cy - halfHeight / 2.0f),
                Point2D.Float(cx + halfWidth, cy + halfHeight / 2.0f)
        )
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        val rect = currentRect(camera)
        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy)
        )
    }

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        val anchors = anchors(state)
        return Rectangle2D.Float(
                anchors.first.location.x,
                anchors.first.location.y,
                anchors.second.location.x,
                anchors.second.location.y
        )
    }

    private fun anchors(state: CurrentState): Pair<ShapeResizeAnchorTop, ShapeResizeAnchorBottom> {
        return Pair(
                ShapeResizeAnchorTop(DiagramElementId("TOP:" + shape.id.id), Point2D.Float(shape.bounds().first.x, shape.bounds().first.y), State.NOT_SELECTED, state, this),
                ShapeResizeAnchorBottom(DiagramElementId("BOTTOM:" + shape.id.id), Point2D.Float(shape.bounds().second.x, shape.bounds().second.y), State.NOT_SELECTED, state, this)
        )
    }
}