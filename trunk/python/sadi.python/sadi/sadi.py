from rdflib import *
from rdflib.resource import *
import rdflib
import mimeparse
import collections
import sys
from webob import Request

from serializers import *

from io import StringIO

rdflib.plugin.register('sparql', rdflib.query.Processor,
                       'rdfextras.sparql.processor', 'Processor')
rdflib.plugin.register('sparql', rdflib.query.Result,
                       'rdfextras.sparql.query', 'SPARQLQueryResult')

dc = Namespace("http://purl.org/dc/terms/")
mygrid = Namespace("http://www.mygrid.org.uk/mygrid-moby-service#")

# Install required libraries using easy_install:
# sudo easy_install 'rdflib>=3.0' surf rdfextras surf.rdflib

class OntClass(Resource):
    def __init__(self,graph, identifier):
        if isinstance(identifier, basestring):
            identifier = URIRef(identifier)
        Resource.__init__(self,graph,identifier)
    def __call__(self,identifier=None):
        if isinstance(identifier, basestring):
            identifier = URIRef(identifier)
        if identifier == None:
            identifier = BNode()
        result = Resource(self.graph,identifier)
        result.add(RDF.type,self.identifier)
        return result
    def all(self):
        for x in self.graph.subjects(RDF.type,self.identifier):
            yield Resource(self.graph,x)

class Service:
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
            "text/rdf":DefaultSerializer('xml'),
            'application/x-www-form-urlencoded':DefaultSerializer('xml'),
            'text/turtle':DefaultSerializer('n3','turtle'),
            'application/x-turtle':DefaultSerializer('n3','turtle'),
            'text/plain':DefaultSerializer('nt'),
            'text/n3':DefaultSerializer('n3'),
            'text/html':DefaultSerializer('rdfa','xml'),
            'application/json':JSONSerializer(),
            }


    def getFormat(self, contentType):
        if contentType == None:
            return [ "application/rdf+xml",self.contentTypes[None]]
        type = mimeparse.best_match(["application/rdf+xml"]+[x for x in self.contentTypes.keys() if x != None],
                                    contentType)
        if type == '' or type == None: 
            return ["application/rdf+xml",DefaultSerializer('xml')]
        else:
            return [type,self.contentTypes[type]]

    def deserialize(self, graph, content, mimetype):
        f = self.getFormat(mimetype)
        f[1].deserialize(graph,content)

    def serialize(self, graph, accept):
        f = self.getFormat(accept)
        return f[1].serialize(graph)

    def annotateServiceDescription(self, desc):
        pass

    def getServiceDescription(self):
        if self.serviceDescription == None:
            self.serviceDescription = Graph()
            self.Description = OntClass(self.serviceDescription,mygrid.serviceDescription)
            self.Organization = OntClass(self.serviceDescription,mygrid.organisation)
            self.Operation = OntClass(self.serviceDescription,mygrid.operation)
            self.Parameter = OntClass(self.serviceDescription,mygrid.parameter)

            self.inputClass = self.getInputClass()
            self.outputClass = self.getOutputClass()
            
            desc = self.Description("#")

            if self.label is not None:
                desc.add(RDFS.label, Literal(self.label))
            if self.comment is not None:
                desc.add(RDFS.comment, Literal(self.comment))
            if self.serviceDescriptionText is not None:
                desc.add(mygrid.hasServiceDescriptionText, Literal(self.serviceDescriptionText))
            if self.serviceNameText is not None:
                desc.add(mygrid.hasServiceNameText, Literal(self.serviceNameText))
            desc.add(mygrid.providedBy, self.getOrganization())
            
            desc.add(mygrid.hasOperation, self.Operation("#operation"))

            outputParameter = self.Parameter("#output")
            desc.value(mygrid.hasOperation).add(mygrid.outputParameter, outputParameter)
            outputParameter.add(mygrid.objectType, self.outputClass)

            inputParameter = self.Parameter("#input")
            desc.value(mygrid.hasOperation).add(mygrid.inputParameter, inputParameter)
            inputParameter.add(mygrid.objectType, self.inputClass)

            if "getParameterClass" in dir(self):
                self.parameterClass = self.getParameterClass()
                secondaryParameter = self.Parameter("#params")
                desc.value(mygrid.hasOperation).add(mygrid.secondaryParameter, secondaryParameter)
                secondaryParameter.add(mygrid.objectType, self.parameterClass)

            operation = desc.value(mygrid.hasOperation)
            operation.add(mygrid.outputParameter, outputParameter)
            operation.add(mygrid.inputParameter, inputParameter)

            self.annotateServiceDescription(desc)

        return self.serviceDescription

    def getInstances(self, graph):
        InputClass = OntClass(graph,self.getInputClass())
        instances = InputClass.all()
        return instances

    def processGraph(self,content, type):
        inputGraph = Graph()
        self.deserialize(inputGraph, content, type)
        outputGraph = Graph()
        OutputClass = OntClass(outputGraph,self.getOutputClass())

        instances = self.getInstances(inputGraph)
        for i in instances:
            o = OutputClass(i.identifier)
            self.process(i, o)
        return outputGraph

    def GET(self, environ, start_response):
        modelGraph = self.getServiceDescription()
        acceptType = self.getFormat(environ.get('HTTP_ACCEPT'))
        response_headers = [
            ('Content-type', acceptType[0]+'; charset=utf-8'),
            ('Access-Control-Allow-Origin','*')
        ]
        status = '200 OK'
        start_response(status, response_headers)
        return [self.serialize(modelGraph,acceptType[0])]

    def POST(self, environ, start_response):
        request = Request(environ,'utf-8')
        status = '200 OK'
        acceptType = self.getFormat(request.headers.get('Accept'))
        response_headers = [
            ('Content-type', acceptType[0]+'; charset=utf-8'),
            ('Access-Control-Allow-Origin','*')
        ]
        start_response(status, response_headers)
        content = unicode(request.body,'utf-8')
        graph = self.processGraph(content, request.headers['Content-Type'])
        return [self.serialize(graph,acceptType[0])]

    def __call__(self,environ,start_response):
        method = environ['REQUEST_METHOD']
        if method == 'GET':
            return self.GET(environ,start_response)
        if method == 'POST':
            return self.POST(environ,start_response)
        status = '405 Method Not Allowed'
        response_headers = [('Content-type', 'text/plain')]
        start_response(status, response_headers)
        return ['Error 405: Method Not Allowed']

def setup_test_client(app):
    from werkzeug.test import Client
    from werkzeug.wrappers import BaseResponse
    c = Client(app,BaseResponse)
    return c
    
def serve(resource,port):
    from wsgiref.simple_server import make_server

    httpd = make_server('', port, resource)
    print "Serving HTTP on port",port,"..."

    # Respond to requests until process is killed
    httpd.serve_forever()
