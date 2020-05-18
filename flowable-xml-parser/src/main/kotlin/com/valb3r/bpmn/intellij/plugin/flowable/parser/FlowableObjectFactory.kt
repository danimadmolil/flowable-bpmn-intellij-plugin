package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType
import java.util.*
import kotlin.reflect.KClass

class FlowableObjectFactory: BpmnObjectFactory {

    private val mapper = ObjectMapper()

    override fun <T : WithBpmnId> newBpmnObject(clazz: KClass<T>): T {
        val result: WithBpmnId = when(clazz) {
            BpmnStartEvent::class -> BpmnStartEvent(BpmnElementId(UUID.randomUUID().toString()), null, null)
            BpmnUserTask::class -> BpmnUserTask(BpmnElementId(UUID.randomUUID().toString()), null, null, null, null, null, null, null, null, null, null, null)
            BpmnScriptTask::class -> BpmnScriptTask(BpmnElementId(UUID.randomUUID().toString()), null, null, null, null, null, null, null)
            BpmnServiceTask::class -> BpmnServiceTask(BpmnElementId(UUID.randomUUID().toString()), null, null, null, null, null, null, null, null, null)
            BpmnBusinessRuleTask::class -> BpmnBusinessRuleTask(BpmnElementId(UUID.randomUUID().toString()), null, null, null, null, null, null, null, null)
            BpmnCallActivity::class -> BpmnCallActivity(BpmnElementId(UUID.randomUUID().toString()), null, null, null, "", null, null, null, null)
            BpmnExclusiveGateway::class -> BpmnExclusiveGateway(BpmnElementId(UUID.randomUUID().toString()), null, null, null)
            BpmnEndEvent::class -> BpmnEndEvent(BpmnElementId(UUID.randomUUID().toString()), null, null)
            else -> throw IllegalArgumentException("Can't create class: " + clazz.qualifiedName)
        }

        return result as T
    }

    override fun <T : WithDiagramId> newDiagramObject(clazz: KClass<T>, forBpmnObject: WithBpmnId): T {
        val result: WithDiagramId = when(clazz) {
            EdgeElement::class -> EdgeElement(DiagramElementId(UUID.randomUUID().toString()), forBpmnObject.id, null)
            ShapeElement::class -> ShapeElement(DiagramElementId(UUID.randomUUID().toString()), forBpmnObject.id, bounds(forBpmnObject))
            else -> throw IllegalArgumentException("Can't create class: " + clazz.qualifiedName)
        }

        return result as T
    }

    override fun <T : WithBpmnId> newOutgoingSequence(obj: T): BpmnSequenceFlow {
        return when(obj) {
            is BpmnExclusiveGateway -> BpmnSequenceFlow(BpmnElementId(UUID.randomUUID().toString()), null, null, obj.id.id, "", ConditionExpression("tFormalExpression", ""))
            else -> BpmnSequenceFlow(BpmnElementId(UUID.randomUUID().toString()), null, null, obj.id.id, "", null)
        }
    }

    override fun <T : WithBpmnId> propertiesOf(obj: T): Map<PropertyType, Property> {
        return when(obj) {
            is BpmnStartEvent -> fillForStartEvent(obj)
            is BpmnEndEvent -> fillForEndEvent(obj)
            is BpmnCallActivity -> fillForCallActivity(obj)
            is BpmnUserTask -> fillForUserTask(obj)
            is BpmnScriptTask -> fillForScriptTask(obj)
            is BpmnServiceTask -> fillForServiceTask(obj)
            is BpmnBusinessRuleTask -> fillForBusinessRuleTask(obj)
            is BpmnSequenceFlow -> fillForSequenceFlow(obj)
            is BpmnExclusiveGateway -> fillForExclusiveGateway(obj)
            else -> throw IllegalArgumentException("Can't parse properties of: ${obj.javaClass}")
        }
    }

    private fun fillForStartEvent(activity: BpmnStartEvent): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun fillForEndEvent(activity: BpmnEndEvent): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun fillForCallActivity(activity: BpmnCallActivity): Map<PropertyType, Property> {
        val properties = processDtoToPropertyMap(activity)
        // TODO: handle extension elements
        return properties
    }

    private fun fillForServiceTask(activity: BpmnServiceTask): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun fillForScriptTask(activity: BpmnScriptTask): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun fillForBusinessRuleTask(activity: BpmnBusinessRuleTask): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun fillForUserTask(activity: BpmnUserTask): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun fillForSequenceFlow(activity: BpmnSequenceFlow): Map<PropertyType, Property> {
        if (null != activity.conditionExpression && activity.conditionExpression!!.type != "tFormalExpression") {
            throw IllegalArgumentException("Unknown type: ${activity.conditionExpression!!.type}")
        }

        return processDtoToPropertyMap(activity)
    }

    private fun fillForExclusiveGateway(activity: BpmnExclusiveGateway): Map<PropertyType, Property> {
        return processDtoToPropertyMap(activity)
    }

    private fun processDtoToPropertyMap(dto: Any): MutableMap<PropertyType, Property> {
        val result: MutableMap<PropertyType, Property> = mutableMapOf()
        val propertyTree = mapper.valueToTree<JsonNode>(dto)

        for (type in PropertyType.values()) {
            if (type.path.contains(".")) {
                tryParseNestedValue(type, propertyTree, result)
                continue
            }

            propertyTree[type.path]?.apply {
                parseValue(result, type)
            }
        }

        return result
    }

    private fun tryParseNestedValue(type: PropertyType, propertyTree: JsonNode, result: MutableMap<PropertyType, Property>) {
        val split = type.path.split(".", limit = 2)
        val targetId = split[0]
        propertyTree[targetId]?.apply {
            if (split[1].contains(".")) {
                tryParseNestedValue(type, this, result)
            }

            this[split[1]]?.parseValue(result, type)
        }
    }

    private fun JsonNode.parseValue(result: MutableMap<PropertyType, Property>, type: PropertyType) {
        result[type] = when (type.valueType) {
            PropertyValueType.STRING, PropertyValueType.CLASS, PropertyValueType.EXPRESSION -> if (this.isNull) Property(null) else Property(this.asText())
            PropertyValueType.BOOLEAN -> Property(this.asBoolean())
        }
    }

    private fun bounds(forBpmnObject: WithBpmnId): BoundsElement {
        return when(forBpmnObject) {
            is BpmnStartEvent, is BpmnEndEvent -> BoundsElement(0.0f, 0.0f, 30.0f, 30.0f)
            is BpmnExclusiveGateway -> BoundsElement(0.0f, 0.0f, 40.0f, 40.0f)
            else -> BoundsElement(0.0f, 0.0f, 100.0f, 80.0f)
        }
    }
}