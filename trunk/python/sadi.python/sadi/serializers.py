from rdflib import *
import json
import rdflib
import mimeparse
import collections
from io import StringIO, BytesIO
from xml.sax.xmlreader import InputSource

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
    def deserialize(self,graph, content):
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

    def deserialize(self,graph, content):
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
