package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnCollapsedSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.ProcessBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

class SubProcess: BpmnMappable<BpmnSubProcess>, ProcessBody() {

    @JacksonXmlProperty(isAttribute = true) var id: String? = null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) var exclusive: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) var triggeredByEvent: Boolean? = null
    @JsonIgnore var hasExternalDiagram: Boolean = false
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>? = null

    override fun toElement(): BpmnSubProcess {
        return Mappers.getMapper(SubProcessMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class SubProcessMapping { // Default methods of Kotlin interfaces are not understood by MapStruct

        fun mapNonCollapsed(input: List<SubProcess>?) : List<BpmnSubProcess>? {
            return input?.filter { !it.hasExternalDiagram }?.map { convertToDto(it) }
        }

        fun mapCollapsed(input: List<SubProcess>?) : List<BpmnCollapsedSubprocess>? {
            return input?.filter { it.hasExternalDiagram }?.map { convertToCollapsedDto(it) }
        }

        @Mapping(target = "transactionalSubprocess", constant = "false")
        abstract fun convertToDto(input: SubProcess) : BpmnSubProcess

        @Mapping(target = "transactionalSubprocess", constant = "false")
        abstract fun convertToCollapsedDto(input: SubProcess) : BpmnCollapsedSubprocess
    }
}