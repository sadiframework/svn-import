from rdflib import *
import rdflib

from twisted.internet import reactor
from twisted.web import server
import twisted.web.resource
from twisted.web.static import File

import tempfile
import os
from surf import *

from StringIO import StringIO

import platform

rdflib.plugin.register('sparql', rdflib.query.Processor,
                       'rdfextras.sparql.processor', 'Processor')
rdflib.plugin.register('sparql', rdflib.query.Result,
                       'rdfextras.sparql.query', 'SPARQLQueryResult')

ns.register(myGrid="http://www.mygrid.org.uk/mygrid-moby-service#")
ns.register(protegedc="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#")

# Install required libraries using easy_install:
# sudo easy_install 'rdflib>=3.0' surf rdfextras surf.rdflib

class Service(twisted.web.resource.Resource):
    isLeaf=True
    serviceDescription = None

    comment = None
    serviceDescriptionText = None
    serviceNameText = None
    label = None
    name = None
    port = None

    def getClass(self, identifier):
        return self.descriptionSession.get_class(identifier)

    def getReference(self, uri):
        return URIRef(uri)

    def getServiceDescription(self):
        if self.serviceDescription == None:
            self.descriptionStore = Store(reader="rdflib", writer="rdflib",
                                          rdflib_store='IOMemory')
            self.descriptionSession = Session(self.descriptionStore)
            self.serviceDescription = self.descriptionStore.reader.graph
            self.Description = self.getClass(ns.MYGRID['serviceDescription'])
            self.Organization = self.getClass(ns.MYGRID['organisation'])
            self.Operation = self.getClass(ns.MYGRID['operation'])
            self.Parameter = self.getClass(ns.MYGRID['parameter'])

            self.inputClass = self.getInputClass()
            self.outputClass = self.getOutputClass()
            
            desc = self.Description("")

            if self.label is not None:
                desc.rdfs_label = self.label
            if self.comment is not None:
                desc.rdfs_comment = self.comment
            if self.serviceDescriptionText is not None:
                desc.mygrid_hasServiceDescriptionText = self.serviceDescriptionText
            if self.serviceNameText is not None:
                desc.mygrid_hasServiceNameText = self.serviceNameText
            desc.mygrid_providedBy = self.getOrganization()
            desc.mygrid_providedBy[0].save()
            
            desc.mygrid_hasOperation = self.Operation("#operation")

            outputParameter = self.Parameter("#output")
            desc.mygrid_hasOperation[0].mygrid_outputParameter = outputParameter
            outputParameter.mygrid_objectType = self.outputClass
            outputParameter.save()

            inputParameter = self.Parameter("#input")
            desc.mygrid_hasOperation[0].mygrid_inputParameter = inputParameter
            inputParameter.mygrid_objectType = self.inputClass
            inputParameter.save()

            desc.mygrid_hasOperation[0].mygrid_outputParameter = outputParameter
            desc.mygrid_hasOperation[0].mygrid_inputParameter = inputParameter
            desc.mygrid_hasOperation[0].save()

            desc.save()

        return self.serviceDescription

    def render_GET(self, request):
        modelGraph = self.getServiceDescription()
        request.setHeader("Content-Type","application/rdf+xml")
        return modelGraph.serialize(format="pretty-xml")

    def render_POST(self, request):
        content = request.content.getvalue()
        inputStore = Store(reader="rdflib", writer="rdflib",
                           rdflib_store='IOMemory')
        inputSession = Session(inputStore)
        inputStore.reader.graph.parse(StringIO(content))
        InputClass = inputSession.get_class(self.getInputClass())
        outputStore = Store(reader="rdflib", writer="rdflib",
                            rdflib_store='IOMemory')
        outputSession = Session(outputStore)
        OutputClass = outputSession.get_class(self.getOutputClass())

        instances = InputClass.all()
        for i in instances:
            print i.subject
            o = OutputClass(i.subject)
            self.process(i, o)

        request.setHeader("Content-Type","application/rdf+xml")
        return outputStore.reader.graph.serialize(format="pretty-xml")

def getDefaultBaseURI(port=8080):
    return "http://"+platform.node()+":"+str(port)+"/"

def publishService(service, port=8080):
    root = twisted.web.resource.Resource()
    service.port = port
    root.putChild(service.name, service)
    site = server.Site(root)
    reactor.listenTCP(port, site)
    reactor.run()
