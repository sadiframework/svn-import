import sadi
from rdflib import *
from time import sleep


hello=Namespace("http://sadiframework.org/examples/hello.owl#")
foaf=Namespace("http://xmlns.com/foaf/0.1/")

class ExampleAsyncService(sadi.Service):
    label = "Hello, world"
    serviceDescriptionText = 'A simple asynchronous "Hello, World" service that reads a name and attaches a greeting.'
    comment = 'A simple asynchronous "Hello, World" service that reads a name and attaches a greeting.'
    serviceNameText = "Hello, world (async python)"
    name = "example"

    def getOrganization(self):
        result = self.Organization()
        result.add(RDFS.label,Literal("Tetherless World Constellation, RPI"))
        result.add(sadi.mygrid.authoritative, Literal(False))
        result.add(sadi.dc.creator, URIRef('mailto:mccusker@gmail.com'))
        return result

    def getInputClass(self):
        return hello.NamedIndividual

    def getOutputClass(self):
        return hello.GreetedIndividual

    def async_process(self, i,o):
        # This is just to make it actually take a bit of time.
        sleep(7)
        print input
        o.set(hello.greeting, Literal("Hello, "+i.value(foaf.name).value))

async_resource = ExampleAsyncService()

class ExampleService(sadi.Service):
    label = "Hello, world"
    serviceDescriptionText = 'A simple "Hello, World" service that reads a name and attaches a greeting.'
    comment = 'A simple "Hello, World" service that reads a name and attaches a greeting.'
    serviceNameText = "Hello, world (python)"
    name = "example"

    def getOrganization(self):
        result = self.Organization()
        result.add(RDFS.label,Literal("Tetherless World Constellation, RPI"))
        result.add(sadi.mygrid.authoritative, Literal(False))
        result.add(sadi.dc.creator, URIRef('mailto:mccusker@gmail.com'))
        return result

    def getInputClass(self):
        return hello.NamedIndividual

    def getOutputClass(self):
        return hello.GreetedIndividual

    def process(self, input, output):
        print input
        output.set(hello.greeting, Literal("Hello, "+input.value(foaf.name).value))

resource = ExampleService()

if __name__ == "__main__":
    sadi.serve(resource, port=9090)
