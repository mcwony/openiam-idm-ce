package org.openiam.taglib.composition

import grails.artefact.Artefact

@Artefact("TagLibrary")
class CompositionTagLib {
    static namespace = 'ui'

    def out

    Closure composition = { attrs, body ->
        if (!attrs.template) {
            throwTagError("Tag [composition] is missing required attribute [template]")
        }
        Composition composition = new Composition()
        body(composition)
        out << g.render(template: attrs.template, model: composition.defines)
    }

    Closure define = { attrs, body ->
        if (!attrs.composition) {
            throwTagError("Tag [define] is missing required attribute [composition]")
        }
        if (!attrs.name) {
            throwTagError("Tag [define] is missing required attribute [name]")
        }
        attrs.composition.defines.put(attrs.name, body)
    }
}
