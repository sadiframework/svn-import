from rdflib import *
import json
import rdflib
import mimeparse
import collections
import email
from io import StringIO, BytesIO
from xml.sax.xmlreader import InputSource
from tidylib import tidy_document

import sys

def setDefaultEncoding():
    currentStdOut = sys.stdout
    currentStdIn = sys.stdin
    currentStdErr = sys.stderr
    
    reload(sys)
    sys.setdefaultencoding('utf-8')
    
    sys.stdout = currentStdOut
    sys.stdin = currentStdIn
    sys.stderr = currentStdErr

setDefaultEncoding()

frir = Namespace("http://purl.org/twc/ontology/frir.owl#")

class CSVSerializer:
    def __init__(self,delimiter=","):
        self.delimiter = delimiter
    def serialize(self,graph):
        return None # Not implemented
    def deserialize(self, graph, content):
        reader = csv.reader(StringIO(content),delimiter=self.delimiter)
        rowNum = 1
        for row in reader:
            rowURI = URIRef('row:'+str(rowNum))
            colNum = 1
            for value in row:
                colURI = URIRef('column:'+str(colNum))
                #if len(value) > 0:
                graph.add((rowURI,colURI,Literal(value)))
                colNum += 1
            rowNum += 1
        return frir.TabularDigest

class DefaultSerializer:
    def __init__(self,inputFormat,outputFormat=None):
        self.inputFormat = inputFormat
        if outputFormat == None:
            self.outputFormat = inputFormat
        else:
            self.outputFormat = outputFormat
    def bindPrefixes(self, graph):
        pass
        #for n in ns.all().items():
        #    graph.bind(n[0].lower(), URIRef(n[1]))

    def serialize(self,graph):
        self.bindPrefixes(graph)
        return graph.serialize(format=self.outputFormat,encoding='utf-8')
    def deserialize(self,graph, content,mimetype):
        if type(content) == str or type(content) == unicode:
            #if self.inputFormat == 'xml':
            #    inputSource = InputSource()
            #    inputSource.setEncoding('utf-8')
            #    inputSource.setCharacterStream(StringIO(content))
            #    graph.parse(inputSource,format=self.inputFormat)
            #else:
            graph.parse(StringIO(content),format=self.inputFormat)
        else:
            graph.parse(content,format=self.inputFormat)
        return frir.RDFGraphDigest

class RDFaSerializer(DefaultSerializer):
    def __init__(self):
        DefaultSerializer.__init__(self,'rdfa','xml')
    def deserialize(self,graph,content,mimetype):
        print "Time to Tidy RDFa!!!"
        document, errors = tidy_document(unicode(content),options={"numeric-entities":1})
        if len(errors) > 0:
            print errors
        DefaultSerializer.deserialize(self,graph,document,mimetype)

class MultipartSerializer:
    def __init__(self,serializers):
        self.serializers = serializers
    def deserialize(self,graph,content,mimetype):
        if type(content) == str or type(content) == unicode:
            msg = email.message_from_string("Content-Type:"+mimetype+"\n"+content)
        else:
            msg = email.message_from_string("Content-Type:"+mimetype+"\n"+content.read())
        named_parts = {}
        unnamed_parts = []
        for part in msg.walk():
            if part.is_multipart():
                continue
            filename = part.get_filename(None)
            if filename:
                named_parts[filename] = part
            else:
                unnamed_parts.append(part)
        rdf = [part for part in unnamed_parts if part.get_content_type() in self.serializers]
        if len(rdf) == 0:
            raise Exception("SADI With Attachments requires one unnamed RDF part.")
        print '\n'.join([str(x) for x in rdf])
        print '\n'.join([str(x) for x in named_parts.values()])
        rdf_content = rdf[0].get_payload()
        print "using this RDF content:"
        print rdf_content
        ser = self.serializers[rdf[0].get_content_type()]
        ser.deserialize(graph, unicode(rdf_content), rdf[0].get_content_type())
        graph.attachments.update(named_parts)
        return frir.RDFGraphDigest
    def serialize(self,graph):
        raise Exception("Multipart serialization is unsupported")
            
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

    def deserialize(self,graph, content,mimetype):
        #if type(content) != str:
        #    graph.parse(StringIO(content,newline=None),format=f)
        #else:
        #    graph.parse(content,format=f)

        data = content
        if type(content) == str or type(content) == unicode:
            data = json.loads(content)
        else:
            data = json.load(content)
        print data
        bnodes = {}
        for s in data.keys():
            subject = self.getResource(s, bnodes)
            for p in data[s].keys():
                predicate = self.getResource(p, bnodes)
                objs = data[s][p]
                for o in objs:
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
                        obj = self.getResource(o['value'],bnodes)
                    graph.add((subject, predicate, obj))
        return frir.RDFGraphDigest
