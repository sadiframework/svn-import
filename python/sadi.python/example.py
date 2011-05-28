import sadi
from rdflib import *
from surf import *

ns.register(hello="http://sadiframework.org/examples/hello.owl#")

class ExampleService(sadi.Service):
    label = "Hello, world"
    serviceDescriptionText = 'A simple "Hello, World" service that reads a name and attaches a greeting.'
    comment = 'A simple "Hello, World" service that reads a name and attaches a greeting.'
    serviceNameText = "Hello, world (python)"
    name = "example"

    def getOrganization(self):
        result = self.Organization("http://tw.rpi.edu")
        result.mygrid_authoritative = False
        result.protegedc_creator = 'mccusker@gmail.com'
        result.save()
        return result

    def getInputClass(self):
        return ns.HELLO["NamedIndividual"]

    def getOutputClass(self):
        return ns.HELLO["GreetedIndividual"]

    def process(self, input, output):
        output.hello_greeting = "Hello, "+input.foaf_name[0]
        output.save()

resource = ExampleService()

if __name__ == "__main__":
    sadi.publishTwistedService(resource, port=9090)
