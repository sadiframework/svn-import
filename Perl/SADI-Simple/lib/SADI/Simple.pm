package SADI::Simple;

1;
__END__

=head1 NAME

SADI::Simple - Module for creating Perl SADI services.

=head1 SYNOPSIS

    #!/usr/bin/perl

    #-----------------------------------------------------------------
    # HelloWorld.pl -- A SADI service that attaches a greeting property 
    #                  to a named individual.
    #-----------------------------------------------------------------

    package HelloWorld;

    use strict;
    use warnings;

    #-----------------------------------------------------------------
    # CGI HANDLER PART
    #-----------------------------------------------------------------

    use Log::Log4perl qw(:easy);
    use base 'SADI::Simple::AsyncService';  # or 'SADI::Simple::SyncService'

    Log::Log4perl->easy_init($ERROR);

    my $config = {

        ServiceName => 'HelloWorld',
        Authority => 'sadiframework.org', 
        InputClass => 'http://sadiframework.org/examples/hello.owl#NamedIndividual',
        OutputClass => 'http://sadiframework.org/examples/hello.owl#GreetedIndividual',   
        Description => 'A \'Hello, World!\' service',
        Provider => 'myaddress@organization.org',
        URL => 'http://organization.org/cgi-bin/HelloWorldAsync', # only required for asynchronous services

    };

    my $service = HelloWorld->new(%$config);
    $service->handle_cgi_request;

    #-----------------------------------------------------------------
    # SERVICE IMPLEMENTATION PART
    #-----------------------------------------------------------------

    use RDF::Trine::Node::Resource;
    use RDF::Trine::Node::Literal;
    use RDF::Trine::Statement;

    =head2 process_it

     Function: implements the business logic of a SADI service
     Args    : $inputs - ref to an array of RDF::Trine::Node::Resource
               $input_model - an RDF::Trine::Model containing the input RDF data
               $output_model - an RDF::Trine::Model containing the output RDF data
     Returns : nothing (service output is stored in $output_model)

    =cut

    sub process_it {

        my ($self, $inputs, $input_model, $output_model) = @_;

        my $log = Log::Log4perl->get_logger('HelloWorld');

        foreach my $input (@$inputs) {
            
            $log->info(sprintf('processing input %s', $input->uri));

            my $name_property = RDF::Trine::Node::Resource->new('http://xmlns.com/foaf/0.1/name');
            my ($name) = $input_model->objects($input, $name_property);

            if (!$name || !$name->is_literal) {
                $log->warn(sprintf('skipping input %s, doesn\'t have a name property with a literal value', $input->uri));
                next;
            }

            my $greeting_property = RDF::Trine::Node::Resource->new('http://sadiframework.org/examples/hello.owl#greeting');
            my $greeting = sprintf("Hello, '%s'!", $name->value);
            my $greeting_literal = RDF::Trine::Node::Literal->new($greeting);
            
            my $statement = RDF::Trine::Statement->new($input, $greeting_property, $greeting_literal);
            $output_model->add_statement($statement);

        }

    }

=head1 DESCRIPTION

This module provides classes for implementing SADI services in Perl. SADI
(Semantic Automated Discovery and Integration) is a standard for implementing Web 
services that natively consume and generate RDF.  

Key points of SADI standard: 

=over

=item * 

A SADI service consumes a single RDF document as input and generates a single RDF document as output.
The input RDF document may contain multiple input instances (i.e. graphs) representing separate
invocations of the service.

=item * 

A SADI service is invoked by an HTTP POST to the service URL, using an RDF document as the POSTDATA.

=item * 

The structure of the input/output instances for a SADI service are described using OWL. 
The service provider publishes one input OWL class and one output OWL class which describe the 
structure of an input instance and an output instance, respectively.

=item * 

Metadata for a SADI service is retrieved by an HTTP GET on the service URL.  This metadata includes the
URIs of the input and output OWL classes, as well as other information such as the service name, service
description, etc.

=back

The main strengths of SADI are:

=over

=item * 

No framework-specific messaging formats or ontologies are required for using SADI.

=item * 

SADI supports processing multiple inputs in a single request, i.e. batch processing.

=item * 

SADI supports long-running services, i.e. asynchronous services.

=back

For more information about the SADI standard, see L<http://sadiframework.org>.

=head1 SYNCHRONOUS SERVICES VS ASYNCHRONOUS SERVICES

Service providers may implement their SADI services as a subclass of either:

=over

=item C<SADI::Simple::SyncService>

A service that subclasses C<SADI::Simple::SyncService> is a 
synchronous service, which means that the service completes its computation before
returning any response to the caller. 

=item C<SADI::Simple::AsyncService>

A service that subclasses C<SADI::Simple::AsyncService> is a
an asynchronous service, which means that the service returns an immediate "ask me later"
response to a request, and must be polled for the results.

=back

In general, asynchronous services are a better choice as they can run for an arbitarily long
time. The main advantage of synchronous services is that there is less back-and-forth messaging
and so they are potentially more efficient for services that perform trivial operations.
