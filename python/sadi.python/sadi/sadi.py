from rdflib import *
import rdflib
import mimeparse

googleAppEngine = False
try:
    from twisted.internet import reactor
    from twisted.web import server
    import twisted.web.resource
    from twisted.web.static import File
except:
    googleAppEngine = True
    from google.appengine.ext import webapp
    from google.appengine.ext.webapp.util import run_wsgi_app
    sys.path.append(os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                 'gae/lib/python2.5/site-packages/'))
from surf import *

from StringIO import StringIO


rdflib.plugin.register('sparql', rdflib.query.Processor,
                       'rdfextras.sparql.processor', 'Processor')
rdflib.plugin.register('sparql', rdflib.query.Result,
                       'rdfextras.sparql.query', 'SPARQLQueryResult')

ns.register(myGrid="http://www.mygrid.org.uk/mygrid-moby-service#")
ns.register(protegedc="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#")

# Install required libraries using easy_install:
# sudo easy_install 'rdflib>=3.0' surf rdfextras surf.rdflib

class ServiceBase:
    serviceDescription = None

    comment = None
    serviceDescriptionText = None
    serviceNameText = None
    label = None
    name = None

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

            if "getParameterClass" in dir(self):
                self.parameterClass = self.getParameterClass()
                secondaryParameter = self.Parameter("#params")
                desc.mygrid_hasOperation[0].mygrid_secondaryParameter = secondaryParameter
                secondaryParameter.mygrid_objectType = self.parameterClass
                secondaryParameter.save()

            desc.mygrid_hasOperation[0].mygrid_outputParameter = outputParameter
            desc.mygrid_hasOperation[0].mygrid_inputParameter = inputParameter
            desc.mygrid_hasOperation[0].save()

            desc.save()

        return self.serviceDescription

    def getInstances(self, session, store, graph):
        InputClass = session.get_class(self.getInputClass())
        instances = InputClass.all()
        return instances

    def processGraph(self,content, type):
        inputStore = Store(reader="rdflib", writer="rdflib",
                           rdflib_store='IOMemory')
        inputSession = Session(inputStore)
        inputStore.reader.graph.parse(StringIO(content), type)
        outputStore = Store(reader="rdflib", writer="rdflib",
                            rdflib_store='IOMemory')
        outputSession = Session(outputStore)
        OutputClass = outputSession.get_class(self.getOutputClass())

        instances = self.getInstances(inputSession, inputStore,
                                      inputStore.reader.graph)
        for i in instances:
            o = OutputClass(i.subject)
            self.process(i, o)
        return outputStore.reader.graph

contentTypes = {
    "application/rdf+xml":'xml',
    'text/turtle':'turtle',
    'application/x-turtle':'turtle',
    'text/plain':'nt',
    'text/n3':'n3',
    'text/rdf+n3':'n3',
}

def getFormat(contentType):
    if contentType == None: return [ "application/rdf+xml",'xml']
    type = mimeparse.best_match(contentTypes.keys(),contentType)
    if type != None: return [type,contentTypes[type]]
    else: return [ "application/rdf+xml",'xml']

if googleAppEngine:
    class GAEService(ServiceBase, webapp.RequestHandler):
        def get(self):
            modelGraph = self.getServiceDescription()
            acceptType = getFormat(self.request.headers["Accept"])
            self.response.headers.add_header("Content-Type",
                                             acceptType[0])
            self.response.write(modelGraph.serialize(format=acceptType[1]))
            
        def post(self):
            postType = getFormat(self.request.headers["Content-Type"])[1]
            graph = self.processGraph(content, postType)
            acceptType = getFormat(self.request.headers["Accept"])
            response.headers.add_header("Content-Type",acceptType[0])
            return graph.serialize(format=acceptType[1])
    Service = GAEService
else:
    class TwistedService(ServiceBase, twisted.web.resource.Resource):
        isLeaf=True
        
        def render_GET(self, request):
            modelGraph = self.getServiceDescription()
            acceptType = getFormat(request.getHeader("Accept"))
            
            request.setHeader("Content-Type",acceptType[0])
            return modelGraph.serialize(format=acceptType[1])
        
        def render_POST(self, request):
            content = request.content.read()
            postType = getFormat(request.getHeader("Content-Type"))[1]
            graph = self.processGraph(content, postType)
            acceptType = getFormat(request.getHeader("Accept"))
            request.setHeader("Content-Type",acceptType[0])
            return graph.serialize(format=acceptType[1])

    Service = TwistedService

def publishTwistedService(service, port=8080):
    root = twisted.web.resource.Resource()
    root.putChild(service.name, service)
    site = server.Site(root)
    reactor.listenTCP(port, site)
    reactor.run()

def publishAppEngineService(serviceClass,debug=True):
    application = webapp.WSGIApplication([('/'+serviceClass.name,serviceClass)],
                                         debug=True)
    run_wsgi_app(application)
