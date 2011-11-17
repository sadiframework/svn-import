from rdflib import *
import simplejson
import rdflib
import mimeparse
from surf.serializer import to_json
import simplejson as json

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

ns.register(mygrid="http://www.mygrid.org.uk/mygrid-moby-service#")
ns.register(protegedc="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#")


# Install required libraries using easy_install:
# sudo easy_install 'rdflib>=3.0' surf rdfextras surf.rdflib

class DefaultSerializer:
    def __init__(self,format):
        self.format = format
    def bindPrefixes(self, graph):
        for n in ns.all().items():
            graph.bind(n[0].lower(), URIRef(n[1]))

    def serialize(self,graph):
        self.bindPrefixes(graph)
        return graph.serialize(format=self.format)
    def deserialize(self,graph, content):
        f = self.format
        if f == 'turtle':
            f = 'n3'
        graph.parse(StringIO(content),format=f)

class JSONSerializer:
    def serialize(self,graph):
        return to_json(modelGraph)
    
    def getResource(self, r, bnodes):
        result = None
        if r.startswith("_:"):
            if r in bnodes:
                result = bnodes[r]
            else:
                result = BNode()
                bnodes[r] = result
        else:
            result = URIRef(r)
        return result

    def deserialize(self,graph, content):
        if json.loads(content):
            data = json.load(StringIO(content))
            bnodes = {}
            for s in data.keys():
                subject = self.getResource(s, bnodes)
                for p in data[s].keys():
                    predicate = self.getResource(p, bnodes)
                    o = data[s][p]
                    obj = None
                    if o['type'] == 'literal':
                        datatype = None
                        if 'datatype' in o:
                            datatype = URIRef(o['datatype'])
                        lang = None
                        if 'lang' in o:
                            lang = o['lang']
                        value = o['value']
                        obj = Literal(value, lang, datatype)
                    else:
                        obj = self.getResource(o['value'])
                    graph.add(subject, predicate, obj)

class ServiceBase:
    serviceDescription = None

    comment = None
    serviceDescriptionText = None
    serviceNameText = None
    label = None
    name = None

    def __init__(self):
        self.contentTypes = {
            None:DefaultSerializer('xml'),
            "application/rdf+xml":DefaultSerializer('xml'),
            'text/turtle':DefaultSerializer('turtle'),
            'application/x-turtle':DefaultSerializer('turtle'),
            'text/plain':DefaultSerializer('nt'),
            'text/n3':DefaultSerializer('n3'),
            'text/rdf+n3':DefaultSerializer('n3'),
            'application/json':JSONSerializer(),
            }


    def getFormat(self, contentType):
        if contentType == None: return [ "application/rdf+xml;",self.contentTypes[None]]
        type = mimeparse.best_match([x for x in self.contentTypes.keys() if x != None],
                                    contentType)
        if type != None: return [type,self.contentTypes[type]]
        else: return [ "application/rdf+xml",serializeXML]

    def serialize(self, graph, accept):
        format = self.getFormat(accept)
        return format[0],format[1].serialize(graph)

    def deserialize(self, graph, content, mimetype):
        format = self.getFormat(mimetype)
        format[1].deserialize(graph,content)

    def serialize(self, graph, accept):
        format = self.getFormat(accept)
        return format[1].serialize(graph)

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
        self.deserialize(inputStore.reader.graph, content, type)
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


if googleAppEngine:
    class GAEService(ServiceBase, webapp.RequestHandler):
        def __init__(self):
            ServiceBase.__init__(self)

        def get(self):
            modelGraph = self.getServiceDescription()
            output = self.serialize(modelGraph, self.request.headers["Accept"])
            self.response.headers.add_header("Content-Type",
                                             output)
            self.response.write(output[1])
            
        def post(self):
            postType = self.getFormat(self.request.headers["Content-Type"])[1]
            graph = self.processGraph(content, postType)
            acceptType = self.getFormat(self.request.headers["Accept"])
            response.headers.add_header("Content-Type",acceptType[0])
            if acceptType[1] == 'json':
                return to_json(modelGraph)
            else: return graph.serialize(format=acceptType[1])
    Service = GAEService
else:
    class TwistedService(ServiceBase, twisted.web.resource.Resource):
        isLeaf=True
        
        def __init__(self):
            ServiceBase.__init__(self)

        def render_GET(self, request):
            modelGraph = self.getServiceDescription()
            acceptType = self.getFormat(request.getHeader("Accept"))
            request.setHeader("Content-Type",acceptType[0])
            request.setHeader('Access-Control-Allow-Origin','*')
            return self.serialize(modelGraph,request.getHeader("Accept"))
            #if acceptType[1] == 'json':
            #    return to_json(modelGraph)
            #else: return selfmodelGraph.serialize(format=acceptType[1])
        
        def render_POST(self, request):
            content = request.content.read()
            print content
            postType = self.getFormat(request.getHeader("Content-Type"))[1]
            graph = self.processGraph(content, postType)
            acceptType = self.getFormat(request.getHeader("Accept"))
            request.setHeader("Content-Type",acceptType[0])
            request.setHeader('Access-Control-Allow-Origin','*')
            return self.serialize(graph,request.getHeader("Accept"))
            #print acceptType
            #if acceptType[1] == 'json':
            #    result =  to_json(graph)
            #    print "JSON output"
            #    print result
            #    return result
            #else: return graph.serialize(format=acceptType[1])

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
