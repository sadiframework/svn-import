import sadi
from sadi.serializers import JSONSerializer
from rdflib import *
from example import resource, async_resource
from io import StringIO
import urlparse
import time
import traceback


def no_accept_header_test():
    '''Test to make sure that SADI services will work even if the Accept header isn't included.'''
    c = sadi.setup_test_client(resource)
    resp = c.get('/')
    assert resp.status_code == 200
    g = Graph()
    g.parse(StringIO(unicode(resp.data)),format="xml")
    assert len(g) > 0

def descriptor_test():
    '''Basic test of getting a service description from a SADI service.'''
    c = sadi.setup_test_client(resource)
    resp = c.get('/',headers={'Accept':'*/*'})
    assert resp.status_code == 200
    g = Graph()
    g.parse(StringIO(unicode(resp.data)),format="turtle")
    assert len(g) > 0

def non_allowed_methods_test():
    '''Test of non-allowed methods.'''
    c = sadi.setup_test_client(resource)
    resp = c.put('/',headers={'Accept':'*/*'})
    assert resp.status_code == 405
    resp = c.delete('/',headers={'Accept':'*/*'})
    assert resp.status_code == 405
    resp = c.head('/',headers={'Accept':'*/*'})
    assert resp.status_code == 405

def oddball_accept_descriptor_test():
    '''Test of sending an oddball accept header.'''
    c = sadi.setup_test_client(resource)
    resp = c.get('/',headers={'Accept':'image/png'})
    assert resp.status_code == 200
    g = Graph()
    g.parse(StringIO(unicode(resp.data)),format="xml")
    assert len(g) > 0

# JSON is a special case to parse, so it's not included here.
# We also can't automatically generate RDFa, so that needs its own test too.
supported_mimetypes = [
    ("application/rdf+xml",'xml','xml'),
    ("text/rdf",'xml','xml'),
    ('application/x-www-form-urlencoded','xml','xml'),
    ('text/turtle','turtle','turtle'),
    ('application/x-turtle','turtle','turtle'),
    ('text/plain','nt','nt'),
    ('text/n3','n3','n3'),
]
def descriptor_accept_types_test():
    '''Test of possible Accept mime types.'''
    c = sadi.setup_test_client(resource)
    def fn(mimetype,f,of):
        resp = c.get('/',headers={'Accept':mimetype})
        assert resp.status_code == 200
        g = Graph()
        print resp.data
        g.parse(StringIO(unicode(resp.data)),format=of)
        assert len(g) > 0
    for mimetype, f, of in supported_mimetypes:
        yield fn, mimetype, f, of

def json_descriptor_test():
    '''Basic test of getting a service description in JSON from a SADI service.'''
    c = sadi.setup_test_client(resource)
    resp = c.get('/',headers={'Accept':'application/json'})
    assert resp.status_code == 200
    g = Graph()
    print resp.data
    s = JSONSerializer()
    s.deserialize(g,resp.data)
    assert len(g) > 0
    
def oddball_accept_service_test():
    '''Test of sending an oddball accept header to a SADI service.'''
    testInput = unicode('''<http://tw.rpi.edu/instances/JamesMcCusker> <http://xmlns.com/foaf/0.1/name> "Jim McCusker";
     a <http://sadiframework.org/examples/hello.owl#NamedIndividual>. ''')
    c = sadi.setup_test_client(resource)
    resp = c.post('/',data=testInput, headers={'Content-Type':'text/turtle','Accept':'image/png'})
    assert resp.status_code == 200
    g = Graph()
    print resp.data
    g.parse(StringIO(unicode(resp.data)),format="xml")
    assert len(g) > 0
    
def service_test():
    '''Basic test of using a SADI service.'''
    testInput = unicode('''<http://tw.rpi.edu/instances/JamesMcCusker> <http://xmlns.com/foaf/0.1/name> "Jim McCusker";
     a <http://sadiframework.org/examples/hello.owl#NamedIndividual>. ''')
    c = sadi.setup_test_client(resource)
    resp = c.post('/',data=testInput, headers={'Content-Type':'text/turtle','Accept':'*/*'})
    assert resp.status_code == 200
    g = Graph()
    print resp.data
    g.parse(StringIO(unicode(resp.data)),format="turtle")
    assert len(g) > 0

def service_unicode_test():
    '''Test of sending and recieving (parsing and serializing) unicode characters.'''
    inputGraph = Graph()
    inputGraph.parse(StringIO(u'<http://example.com/weirdname> <http://xmlns.com/foaf/0.1/name> "a\xac\u1234\u20ac\U00008000"; a <http://sadiframework.org/examples/hello.owl#NamedIndividual>. '),format='turtle')
    c = sadi.setup_test_client(resource)
    def fn(mimetype,f,of):
        resp = c.post('/',data=inputGraph.serialize(format=f),
                      headers={'Content-Type':mimetype,'Accept':mimetype})
        assert resp.status_code == 200
        g = Graph()
        print resp.data
        g.parse(StringIO(unicode(resp.data,'utf-8')),format=of)
        assert len(g) > 0
    for mimetype, f, of in supported_mimetypes:
        yield fn, mimetype, f,of

def service_accept_content_types_test():
    '''Test of accept and content type headers for service invocation.'''
    inputGraph = Graph()
    inputGraph.parse(StringIO(unicode('''<http://tw.rpi.edu/instances/JamesMcCusker> <http://xmlns.com/foaf/0.1/name> "Jim McCusker";
     a <http://sadiframework.org/examples/hello.owl#NamedIndividual>. ''')),format="turtle")
    c = sadi.setup_test_client(resource)
    def fn(mimetype,f,of):
        resp = c.post('/',data=inputGraph.serialize(format=f),
                      headers={'Content-Type':mimetype,'Accept':mimetype})
        assert resp.status_code == 200
        g = Graph()
        print resp.data
        g.parse(StringIO(unicode(resp.data)),format=of)
        assert len(g) > 0
    for mimetype, f, of in supported_mimetypes:
        yield fn, mimetype, f,of

def service_rdfa_content_type_test():
    '''Test of RDFa content type header for service invocation.'''
    i='''<div xmlns="http://www.w3.org/1999/xhtml"
  prefix="
    foaf: http://xmlns.com/foaf/0.1/
    ns1: http://sadiframework.org/examples/hello.owl#
    rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns#
    rdfs: http://www.w3.org/2000/01/rdf-schema#"
  >
  <div typeof="ns1:NamedIndividual" about="http://tw.rpi.edu/instances/JamesMcCusker">
    <div property="foaf:name" content="Jim McCusker"></div>
  </div>
</div>'''
    c = sadi.setup_test_client(resource)
    resp = c.post('/',data=i,
                  headers={'Content-Type':'text/html','Accept':'text/turtle'})
    assert resp.status_code == 200
    g = Graph()
    g.parse(StringIO(unicode(resp.data)),format='turtle')
    print resp.data
    assert len(g) > 0

        
def service_JSON_accept_content_type_test():
    '''Test of JSON accept and content type headers for service invocation.'''
    i = '''{
  "http://tw.rpi.edu/instances/JamesMcCusker": {
    "http://xmlns.com/foaf/0.1/name": [
      {
        "type": "literal", 
        "value": "Jim McCusker"
      }
    ], 
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
      {
        "type": "uri", 
        "value": "http://sadiframework.org/examples/hello.owl#NamedIndividual"
      }
    ]
  }
}'''
    c = sadi.setup_test_client(resource)
    resp = c.post('/',data=i,
                  headers={'Content-Type':'application/json','Accept':'application/json'})
    assert resp.status_code == 200
    g = Graph()
    print resp.data
    s = JSONSerializer()
    s.deserialize(g,resp.data)
    assert len(g) > 0
    

def curl_turtle_comment_test():
    '''How to test a SADI service using cURL'''
    from wsgiref.simple_server import make_server
    from threading import Thread
    import subprocess

    url = 'http://localhost:9090/'
    headers = {'Content-Type':'text/turtle',
               'Accept':'*/*'}

    httpd = make_server('', 9090, resource)
    thread = Thread(target=lambda:httpd.handle_request())
    thread.daemon = True
    thread.start()
    data = subprocess.check_output('curl -s -H Content-Type:text/turtle -H Accept:text/turtle  -X POST --data-binary @exampleInput.ttl http://localhost:9090/'.split(" "))
    print data
    g = Graph()
    g.parse(StringIO(unicode(data)),format='turtle')
    print "Output graph has", len(g), "triples."
    assert len(g) > 0

def make_relative(uri):
    puri = urlparse.urlsplit(uri)
    result = puri.path
    if len(puri.query) > 0:
        result += "?"+puri.query
    if len(puri.fragment) > 0:
        result += "#"+ puri.fragment
    return result

def wrong_curl_turtle_comment_test():
    '''How NOT to test a SADI service using cURL. Use --data-binary instead of -d, which strips newlines.'''
    from wsgiref.simple_server import make_server
    from threading import Thread
    import subprocess

    url = 'http://localhost:9090/'
    headers = {'Content-Type':'text/turtle',
               'Accept':'*/*'}

    httpd = make_server('', 9090, resource)
    thread = Thread(target=lambda:httpd.handle_request())
    thread.daemon = True
    thread.start()
    data = subprocess.check_output('curl -s -H Content-Type:text/turtle -H Accept:text/turtle  -X POST -d @exampleInput.ttl http://localhost:9090/'.split(" "))
    print data
    g = Graph()
    g.parse(StringIO(unicode(data)),format='turtle')
    print "Output graph has", len(g), "triples."
    assert len(g) == 0

def get_pragmas(response):
    p = response.headers['Pragma']
    p = p.split('\n')
    p = dict([tuple([x.strip() for x in line.split('=')]) for line in p])
    return p

def async_service_test():
    '''Basic test of using an asynchronous SADI service.'''
    testInput = unicode('''<http://tw.rpi.edu/instances/JamesMcCusker> <http://xmlns.com/foaf/0.1/name> "Jim McCusker";
     a <http://sadiframework.org/examples/hello.owl#NamedIndividual>. ''')
    c = sadi.setup_test_client(async_resource)
    resp = c.post('/',data=testInput, headers={'Content-Type':'text/turtle','Accept':'*/*'})
    assert resp.status_code == 202
    g = Graph()
    g.parse(StringIO(unicode(resp.data)),format="turtle")
    jim = URIRef('http://tw.rpi.edu/instances/JamesMcCusker')
    idb = [x for x in g[jim:RDFS.isDefinedBy]]
    assert len(idb) == 1
    idb = idb[0]
    notDone = True
    while notDone:
        idb = make_relative(idb)
        print idb
        resp = c.get(idb,headers={'Accept':'text/turtle'})
        if resp.status_code == 302:
            location = resp.headers['Location']
            try:
                loc = URIRef(location)
                idb = loc
            except:
                assert False
            try:
                pragma = get_pragmas(resp)
                wait = int(pragma['sadi-please-wait'])
                time.sleep(wait/1000.0)
            except:
                print traceback.print_exc()
                assert False
        elif resp.status_code == 200:
            notDone = False
        else:
            raise Exception(str(resp))
    assert resp.status_code == 200
    g = Graph()
    g.parse(StringIO(unicode(resp.data)),format="turtle")
    assert len(g) > 0
    jim = URIRef('http://tw.rpi.edu/instances/JamesMcCusker')
    triples = [x for x in g[jim]]
    assert len(triples) > 0
    print g.serialize(format="turtle")
