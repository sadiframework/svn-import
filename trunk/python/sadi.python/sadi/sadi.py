from rdflib import *
import simplejson
import rdflib
import mimeparse
from surf.serializer import to_json
import simplejson as json
import collections

googleAppEngine = False
useTwisted = False
try:
    from twisted.internet import reactor
    from twisted.web import server
    import twisted.web.resource
    from twisted.web.static import File
    useTwisted = True
except:
    try:
        from google.appengine.ext import webapp
        from google.appengine.ext.webapp.util import run_wsgi_app
        sys.path.append(os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                      'gae/lib/python2.5/site-packages/'))
        googleAppEngine = True
    except:
        pass
modPython = False
try:
    from mod_python import apache, publisher
    modPython = True
except:
    modPython = False

from surf import *

from io import StringIO

rdflib.plugin.register('sparql', rdflib.query.Processor,
                       'rdfextras.sparql.processor', 'Processor')
rdflib.plugin.register('sparql', rdflib.query.Result,
                       'rdfextras.sparql.query', 'SPARQLQueryResult')

ns.register(mygrid="http://www.mygrid.org.uk/mygrid-moby-service#")
ns.register(protegedc="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#")


# Install required libraries using easy_install:
# sudo easy_install 'rdflib>=3.0' surf rdfextras surf.rdflib

class DefaultSerializer:
    def __init__(self,inputFormat,outputFormat=None):
        self.inputFormat = inputFormat
        if outputFormat == None:
            self.outputFormat = inputFormat
        else:
            self.outputFormat = outputFormat
    def bindPrefixes(self, graph):
        for n in ns.all().items():
            graph.bind(n[0].lower(), URIRef(n[1]))

    def serialize(self,graph):
        self.bindPrefixes(graph)
        return graph.serialize(format=self.outputFormat)
    def deserialize(self,graph, content):
        if type(content) == str or type(content) == unicode:
            graph.parse(StringIO(unicode(content),newline=None),format=self.inputFormat)
        else:
            graph.parse(content,format=self.inputFormat)

class JSONSerializer:
    def serialize(self,graph):

        def getValue(node):
            if type(node) == BNode:
                return "_:"+str(node)
            else:
                return node.encode('utf-8','ignore')
        def makeObject(o):
            result = {}
            result['value'] = getValue(o)
            if type(o) == URIRef:
                result['type'] = 'uri'
            elif type(o) == BNode:
                result['type'] = 'bnode'
            else:
                result['type'] = 'literal'
                if o.language != None:
                    result['language'] = o.language
                if o.datatype != None:
                    result['datatype'] = str(o.datatype)
            print json.dumps(result)
            return result

        def makeResource():
            return collections.defaultdict(list)
        result = collections.defaultdict(makeResource)
        for stmt in graph:
            result[getValue(stmt[0])][getValue(stmt[1])].append(makeObject(stmt[2]))
        return json.dumps(result)
    
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
        if type(content) != str:
            graph.parse(StringIO(content,newline=None),format=f)
        else:
            graph.parse(content,format=f)

        data = content
        if type(content) == str:
            data = json.load(StringIO(content,newline=None))
        else:
            data = json.load(content)
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

services = {}

def wsgi_register(service):
    services['/'+service.name] = service

def application(environ, start_response):
    requestMethod = environ['REQUEST_METHOD']
    try:
        service = services[environ['PATH_INFO']]
	if requestMethod == 'GET':
            return service.wsgi_get(environ, start_response)
        elif requestMethod == 'POST':
            return service.wsgi_post(environ, start_response)
        else: # Method not supported
            return wsgiMethodNotSupported(environ, start_response)
    except:
        return wsgi404(environ, start_response)

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
            return [ "application/rdf+xml;",self.contentTypes[None]]
        print contentType
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

    def annotateServiceDescription(self, desc):
        pass

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
            
            desc = self.Description("#")

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

            self.annotateServiceDescription(desc)

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

    def wsgi_get(self, environ, start_response):
        modelGraph = self.getServiceDescription()
        acceptType = self.getFormat(environ['HTTP_ACCEPT'])
        response_headers = [
            ('Content-type', acceptType[0]),
            ('Access-Control-Allow-Origin','*')
        ]
        status = '200 OK'
        start_response(status, response_headers)
        return self.serialize(modelGraph,request.getHeader("Accept"))

    def wsgi_post(self, environ, start_response):
        status = '200 OK'
        response_headers = [('Content-type', 'text/plain')]
        start_response(status, response_headers)
        content = request.content.read()
        graph = self.processGraph(content, request.getHeader("Content-Type"))
        acceptType = self.getFormat(request.getHeader("Accept"))
        request.setHeader("Content-Type",acceptType[0])
        request.setHeader('Access-Control-Allow-Origin','*')
        return self.serialize(graph,request.getHeader("Accept"))
    
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
elif useTwisted:
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
            graph = self.processGraph(content, request.getHeader("Content-Type"))
            acceptType = self.getFormat(request.getHeader("Accept"))
            request.setHeader("Content-Type",acceptType[0])
            request.setHeader('Access-Control-Allow-Origin','*')
            return self.serialize(graph,request.getHeader("Accept"))

    Service = TwistedService
else:
    Service = ServiceBase

handler = None
if modPython:
    def handler(req):
        req.allow_methods(["GET", "POST"])
        if req.method not in ["GET", "POST"]:
            raise apache.SERVER_RETURN, apache.DECLINED

        # Derive the name of the actual module which will be
        # loaded. In older version of mod_python.publisher
        # you can't actually have a code file name which has
        # an embedded '.' in it except for that used by the
        # extension. This is because the standard Python
        # module import system which is used will think that
        # you are importing a submodule of a package. In
        # this code, because the standard Python module
        # import system isn't used and the actual file is
        # opened directly by name, an embedded '.' besides
        # that used for the extension will technically work.
        
        path,module_name =  os.path.split(req.filename)
    
        # If the request is against a directory, fallback to
        # looking for the 'index' module. This is determined
        # by virtue of the fact that Apache will always add
        # a trailing slash to 'req.filename' when it matches
        # a directory. This will mean that the calculated
        # module name will be empty.

        if not module_name:  
            module_name = 'index'

        # Now need to strip off any special extension which
        # was used to trigger this handler in the first place.
            
        suffixes = ['py']
        suffixes += req.get_addhandler_exts().split()
        if req.extension:
            suffixes.append(req.extension[1:])

        exp = '\\.' + '$|\\.'.join(suffixes) + '$'
        suff_matcher = re.compile(exp)
        module_name = suff_matcher.sub('',module_name)

        # Next need to determine the path for the function
        # which will be called from 'req.path_info'. The
        # leading slash and possibly any trailing slash are
        # eliminated. There would normally be at most one
        # trailing slash as Apache eliminates duplicates
        # from the original URI.

        func_path = ''

        if req.path_info:
            func_path = req.path_info[1:]
            if func_path[-1:] == '/':
                func_path = func_path[:-1]

        # Now determine the actual Python module code file
        # to load. This will first try looking for the file
        # '/path/<module_name>.py'. If this doesn't exist,
        # will try fallback of using the 'index' module,
        # ie., look for '/path/index.py'. In doing this, the
        # 'func_path' gets adjusted so the lead part is what
        # 'module_name' was set to.

        req.filename = path + '/' + module_name + '.py'

        if not publisher.exists(req.filename):
            if publisher.exists(path + '/' + module_name + '.rpy'):
                req.filename = path + '/' + module_name + '.rpy'
            else:
                if func_path:
                    func_path = module_name + '/' + func_path
                else:
                    func_path = module_name

                module_name = 'index' 
                req.filename = path + '/' + module_name + '.py'

            if not publisher.exists(req.filename):
                raise apache.SERVER_RETURN, apache.DECLINED

        # Default to looking for the 'index' function if no
        # function path definition was supplied.

        if not func_path:
            func_path = 'resource'

        # Turn slashes into dots.

        func_path = func_path.replace('/', '.')

        # Normalise req.filename to avoid Win32 issues.

        req.filename = publisher.normpath(req.filename)


        # We use the page cache to load the module
        module = publisher.page_cache[req]

        # does it have an __auth__?
        realm, user, passwd = publisher.process_auth(req, module)

        # resolve the object ('traverse')
        resource = publisher.resolve_object(req, module, func_path, realm, user, passwd)

        if req.method == 'GET':
            modelGraph = resource.getServiceDescription()
            accept = 'application/rdf+xml'
            if 'Accept' in req.headers_in:
                accept = req.headers_in["Accept"]
            acceptType = resource.getFormat(accept)
            req.content_type = acceptType[0]
            req.headers_out['Access-Control-Allow-Origin'] = '*'
            req.write(resource.serialize(modelGraph,req.headers_in['Accept']))
        else:
            content = req.read()
            contentType = "application/rdf+xml"
            if 'Content-Type' in req.headers_in:
                contentType = req.headers_in["Content-Type"]
            graph = resource.processGraph(content, contentType)
            accept = "application/rdf+xml"
            if 'Accept' in req.headers_in:
                accept = req.headers_in["Accept"]
            acceptType = resource.getFormat(accept)
            req.headers_out["Content-Type"] = acceptType[0]
            req.headers_out['Access-Control-Allow-Origin'] = '*'
            req.write(resource.serialize(graph,accept))
        return apache.OK
#    handler = sadiHandler

def publishTwistedService(service, port=8080):
    if not useTwisted:
        raise Exception("Twisted isn't installed in this Python environment, and is needed to run a SADI service through twisted.")
    root = twisted.web.resource.Resource()
    root.putChild(service.name, service)
    site = server.Site(root)
    reactor.listenTCP(port, site)
    reactor.run()

def publishAppEngineService(serviceClass,debug=True):
    application = webapp.WSGIApplication([('/'+serviceClass.name,serviceClass)],
                                         debug=True)
    run_wsgi_app(application)
