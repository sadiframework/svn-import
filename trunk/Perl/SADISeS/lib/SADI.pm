#-----------------------------------------------------------------
# SADI
# Author: Edward Kawas
# For copyright and disclaimer see below.
#
# $Id: SADI.pm,v 1.01 2010-03-09 18:04:43 ubuntu Exp $
#-----------------------------------------------------------------
package SADI;
use strict 'vars';

# add versioning to this module
use vars qw{$VERSION};

BEGIN {
	use vars qw{@ISA @EXPORT @EXPORT_OK};
	$VERSION = sprintf "%d.%02d", q$Revision: 1.06 $ =~ /: (\d+)\.(\d+)/;
	*SADI::VERSION = *VERSION;
}

1;

__END__

=head1 NAME

SADI - Perl extension for the automatic generation of SADI web services

=head2 Upgrading From Versions prior to Version 1.04 to Version 1.04

This new version of SADI uses an updated OWL2Perl module that adds many new items to generated OWL classes, as well as, makes them more memory and cpu efficient. 
In order to use these features to the fullest, you will need to regenerate all of your OWL2Perl generated OWL classes again, using I<sadi-generate-datatypes>.

=head2 Upgrading From Versions prior to 1.03 to Version 1.03

This new version of SADI uses an updated OWL2Perl module that adds property restrictions to generated OWL classes. In order to use these 
features to the fullest, you will need to regenerate all of your OWL2Perl generated OWL classes again, using I<sadi-generate-datatypes>. 

=head2 Upgrading From Version 0.99.4

Version 0.99.5 contains some neat new features and bug fixes! One of the added features is the ability to add unit test for your service.

All that is needed to upgrade to 0.99.5 is for you to re-run the C<sadi-install.pl> script.  

=head2 Upgrading From Version 0.99.2

For those of you upgrading a previous SADISeS installation and using OWL2Perl modules, you will need to regenerate your datatypes.

After careful consideration, it was determined that we could clean up the package names for generated owl entities.

The easiest way to update your services is to do the following (assuming for a second
that your home directory is /home/ubuntu/ and that you are using a *NIX machine):

To update your implementation files, do something like:

C<for i in `find /home/ubuntu/Perl-SADI/services/Service -type f -print`; do perl -pi -e 's|::owl::|::|g' $i; done> 

If you are on windows, try the following (assuming that your home directory is C:\Users\ubuntu\):

C<for /R %i in ("C:\Users\ubuntu\Perl-SADI\cgi\*") do perl -pi -e 's|::owl::|::|g' %i>

Additionally, for those of you that pre-generate your asynchronous service base file (not default operation), you will need to re-generate your service basis.
For each service that is affected, do a C<sadi-generate-service -B your_service_name>. 

=head2 Upgrading From Version 0.99.1

For those of you upgrading a previous SADISeS installation, you will need to
decide if you want to remove the CGI entry scripts for your services and re-generate them.

The reason for this is that the mime type header for SADI services has been changed to
"application/rdf+xml", as per the W3C RDF spec. While this change makes your services
more correct, it doesnt change how your services behave.

The easiest way to update your services is to do the following (assuming for a second
that your home directory is /home/ubuntu/ and that you are using a *NIX machine):

To update your entry scripts, do something like:

C<for i in `find /home/ubuntu/Perl-SADI/cgi -type f -print`; do perl -pi -e 's|text/xml|application/rdf\+xml|g' $i; done> 

If you are on windows, try the following (assuming that your home directory is C:\Users\ubuntu\):

C<for /R %i in ("C:\Users\ubuntu\Perl-SADI\cgi\*") do perl -pi -e 's|text/xml|application/rdf\+xml|g' %i>

=head1 SYNOPSIS

       # to get started, run the install script
       sadi-install.pl

       # for those of you using MS Windows (commands are issued without the ".pl")
       sadi-install

       # generate a service definition file, for example HelloSadiWorld
       sadi-generate-services.pl -D HelloSadiWorld

       # now you have to go and edit the definition file
       # in Perl-SADI/definitions/HelloSadiWorld
       # to point it to the various ontologies that your service uses
       # once you have done that, then...

       # generate a service implementation, based on this edited
       # definitions file.  In this example, you would simply type:

       sadi-generate-services.pl HelloSadiWorld

       # now add your business logic to the module that was created in
       # Perl-SADI/services/Service/HelloSadiWorld.pm

       # finally, make a symbolic link from Perl-SADI/cgi/HelloSadiWorld to
       # your cgi-bin/ directory to deploy your service.

       # assuming that you deployed it, test the service (-g gets the service interface
       # while -e allows you to send input data to the service)
       sadi-testing-service.pl -g http://localhost/cgi-bin/HelloSadiWorld

       # read the POD for more details!

=head1 DESCRIPTION

This is the documentation for Perl SADISeS (SADI Services Support). If you are reading this from the C<perldoc> utility, you may notice that some words are missing or that some phrases are incomplete. In order to view this documentation in the manner intended, please view the html version of this documentation that was installed duing B<make install> or the version on B<CPAN>.

First of all, it is assumed that you are familiar with SADI. If this assumption is false, please go to the SADI homepage (L<http://sadiframework.org>). 

Hopefully, you have chosen to install this package so that you can create SADI web services. SADI is a novel Web Services framework that dynamically generates RDF graphs by accessing Web Services. Web Service discovery is achieved by registering the type of relationship (i.e. the predicate) that the Web Service creates between its input data and its output data. 

=head2 Package Installation

Installation of this helpful perl package is straightforward!

On *nix machines, install as follows:

=over 4

=item C<perl Makefile.PL>

=item C<make test>

=item C<make install>

=back

On Window machines, install as follows:

=over 4

=item 	C<perl Makefile.PL>

=item 	C<nmake test>

=item 	C<nmake install>

=back

=cut

=head2 SADI Installation

Assuming that you have already installed this package, the very first thing that you should do is run the script B<sadi-install.pl>.

This script will do the following:

=over 4

=item * Check for prerequisite modules

=item * Run you through some configuration for the Perl SADI modules

=item * Create some directories that SADISeS will use (definitions, cgi, services, etc)

=item * Create the logging and service configuration files

=back

Once the installation process is complete, you can create your first service!

=cut

=head2 SADI Services Support in Perl

Perl SADISeS, is a project aiming to help SADI service providers develop their code in Perl.

The basic design principles are the same: to provide libraries that allow a full object-oriented
approach to SADI services(data types and service instances) and shielding service developers 
from all the nitty gritty required to develop SADI services.

=head2 New Features

Some of the new features included in this release are:

=over 4

=item * Support for synchronous SADI web services

=item * Logging support

=item * Support for SADI service testing

=back 

=cut

=head2 Overview

Perl SADI is a generator of Perl code - but this code does not need to be always stored in files, it can be generated on-the-fly every time a service is called. Also, there are up to four things to be generated: objects representing SADI data types, classes establishing bases for services, empty (but working) service implementation modules, and cgi-bin scripts, the entry points to our SADI services.

However, before going into the gory details, let's install Perl SADI, create and call the first service. 

=cut


=head3 Quick Start - Five Steps to the First Service

=over

=item 1. Download the SADI module, and install it.


=item 2. Run the installation script for SADISeS

From the command prompt, enter:

C<sadi-install>

or, 

C<sadi-install.pl>


This works, because part of the installation for the SADISeS module entails the installation of scripts that make SADISeS tasks more simple.

=item 3. Generate a service definition and a service implementation.

You can pick any name for your service:

C<sadi-generate-services -D HelloSadiWorld>

or,

C<sadi-generate-services.pl -D HelloSadiWorld>

This will create a definition for your SADI service (in the E<lt>your-home-directoryE<gt>/Perl-SADI/definitions/ directory).

Next create the implementation for the SADI service by doing:

C<sadi-generate-services HelloSadiWorld>

or,

C<sadi-generate-services.pl HelloSadiWorld>

It creates a Perl module Service::HelloSadiWorld (in E<lt>your-home-directoryE<gt>/Perl-SADI/services/), representing your first SADI Web Service . The service is empty - but is already able to accept SADI input requests and recognize its own data type, and it produces fake output data (of the correct type).

=item 4. Make your service available from your Web Server (this is called deploying). 

The only thing you need to do is to tell your Web Server where there is a starting cgi-bin script. The script was already created during the service generation phase in E<lt>your-home-directoryE<gt>/Perl-SADI/cgi directory. Make a symbolic link from a cgi-bin directory of your Web Server (e.g on some Linux distributions, using Apache Web server, the cgi-bin directory is /usr/lib/cgi-bin). 

For example:

=begin html

<pre>
cd /usr/lib/cgi-bin
sudo ln -s /home/kawas/Perl-SADI/cgi/HelloSadiWorld .
</pre>

=end html

on windows, you can mimic the L<above|"How can I tell apache to execute HelloSadiWorld on Windows without moving the file to cgi-bin?">

If you cannot create Symbolic links, or apache is not allowed to follow them, try this L<workaround|"Cannot Create Symbolic links">

=item 5. Last but not least: call your service. 

There is a testing client that sends only empty data (so it can be used by any service):

=begin html

<pre>sadi-testing-service.pl -e http://localhost/cgi-bin/HelloSadiWorld</pre>

=end html

Of course, you can also send real data, from a local file (for example, from data/my-input.xml):

=begin html

<pre>sadi-testing-service.pl -e http://localhost/cgi-bin/HelloSadiWorld \
     data/my-input.xml
</pre>

=end html

The output (the same with any input data) is not really that exciting:

=begin html

<pre><p>HTTP/1.1 200 OK<br />
  Connection: close<br />
  Date: Tue, 25 Aug 2009 20:45:08 GMT<br />
  Server: Apache/2.2.8 (Ubuntu) PHP/5.2.4-2ubuntu5.3 with Suhosin-Patch<br />
  Vary: Accept-Encoding<br />
  Content-Type: text/xml; charset=ISO-8859-1<br />
  Client-Date: Tue, 25 Aug 2009 20:45:09 GMT<br />
  Client-Peer: 127.0.0.1:80<br />
  Client-Response-Num: 1<br />
  Client-Transfer-Encoding: chunked</p>
<p>&lt;rdf:RDF<br />
  xmlns:a=&quot;http://sadiframework.org/ontologies/predicates.owl#&quot;<br />
  xmlns:rdf=&quot;http://www.w3.org/1999/02/22-rdf-syntax-ns#&quot;<br />
  &gt;<br />
  &lt;rdf:Description rdf:about=&quot;http://someontology.org/datatypes#Output1&quot;&gt;<br />
  &lt;a:somePredicate0&gt;0&lt;/a:somePredicate0&gt;<br />
  &lt;a:somePredicate1&gt;1&lt;/a:somePredicate1&gt;<br />
  &lt;a:somePredicate2&gt;2&lt;/a:somePredicate2&gt;<br />
  &lt;/rdf:Description&gt;<br />
  &lt;/rdf:RDF&gt;</p>
</pre>

=end html

You immediately notice that the returned value does not have an expected "hello" greeting. Well, a fake output is just fake output. You would need to add your own business logic into your service to do something meaningful (such as saying warmly "Hello, SADI world").

Also, the service provider can see few log entries:

=begin html

<pre>
2009/08/26 08:35:28 (254) INFO> [5554] (eval 34):38 - *** REQUEST START ***
REMOTE_ADDR: 127.0.1.1, HTTP_USER_AGENT: libwww-perl/5.829, CONTENT_LENGTH: 228, CONTENT_TYPE: application/x-www-form-urlencoded
2009/08/26 08:35:28 (278) INFO> [5554] (eval 34):84 - *** RESPONSE READY ***
</pre>

=end html

The number in square brackets is a process ID - it helps to find which response belongs to which request when a site gets more to many requests in the same time. And in parenthesis, there is the number of milliseconds since the program has started. More about the log format in L<logging|"Logging">.

=back

=cut

=head3 Motivation

SADISeS in Perl was created to allow people to enter the SADI world with little to no barriers. Web services are complicated enough without SADI! Why should SADI add to the complications?

=over

=item * Fully object-oriented approach,

 based on objects created on-the-fly (or pre-generated) from service definition files and OWL classes.

=item * More centralized 

(and therefore easier changeable if/when needed) support for service configuration, for request logging, and even for protocol binding (for example, in your service implementation code there are no visible links to any HTTP protocol).

In other words, the services written with the Perl SADI as their back-end are more unified and less prone to be changed when their environment changes. They should differ really only in their business logic (what they do, and not how they communicate with the rest of the world). 

=back

=cut

=head2 Bits and Pieces

=cut

=head3 Requirements

The other modules needed are (all available from the CPAN):

=over

=item * CGI

=item * Log::Log4perl 
	- a wonderful port of the famous log4j Java logging system.

=item * Config::Simple
     - for a simple service configuration

=item * IO::Stringy
    - a.k.a. IO::Scalar

=item * Unicode::String

=item * File::HomeDir

=item * File::ShareDir

=item * File::Spec

=item * FindBin

=item * IO::Prompt - try to install version 0.99.2 and not 0.99.4

=item * Want 
    - for IO::Prompt ... they require it but don't include it!

=item * HTTP::Date 
    - for developing asynchronous moby services I<Optional>

=item *
    Template - for creating RDF, service and definition templates 

=item * Params::Util

=item * Class::Inspector

=item * IO::String

=item * RDF::Core 
    - for parsing the RDF

=back

=cut

=head3 Installation

The installation script is (as well as the other Perl SADI scripts) installed at module install time and is available from any command prompt. You should run it the first time, and you can run it anytime later again. The files that are already created are not overwritten - unless you want it to be (using the B<-F> option).

C<sadi-install.pl> or C<sadi-install> on the MS Windows platform

This is an example of a typical conversation and output of the first installation:

=begin html

<pre>

ubuntu@ubuntu:~/Perl-SADI$ sadi-install.pl
Welcome, SADIiers. Preparing stage for Perl SADI ...
------------------------------------------------------
OK. Module Carp is installed.
OK. Module CGI is installed.
OK. Module File::Spec is installed.
OK. Module Config::Simple is installed.
OK. Module File::HomeDir is installed.
OK. Module File::ShareDir is installed.
OK. Module Log::Log4perl is installed.
OK. Module HTTP::Date is installed.
OK. Module Template is installed.
OK. Module Params::Util is installed.
OK. Module Class::Inspector is installed.
OK. Module Unicode::String is installed.
OK. Module IO::String is installed.
OK. Module RDF::Core is installed.
OK. Module IO::Prompt is installed.

Installing in /home/ubuntu/Perl-SADI

Created install directory &apos;/home/ubuntu/Perl-SADI&apos;.
Created install directory &apos;/home/ubuntu/Perl-SADI/cgi&apos;.
Created sample-resources directory &apos;/home/ubuntu/Perl-SADI/sample-resources&apos;.
Created service defintions directory &apos;/home/ubuntu/Perl-SADI/definitions&apos;.
Created service defintions directory &apos;/home/ubuntu/Perl-SADI/unittest&apos;.
Created service defintions directory &apos;/home/ubuntu/Perl-SADI/xml&apos;.

Log properties file created: &apos;/home/ubuntu/Perl-SADI/log4perl.properties&apos;.

Configuration file created: &apos;/home/ubuntu/Perl-SADI/sadi-services.cfg&apos;.

Done.

</pre>

=end html

All these things can be done manually, at any time. Installation script just makes it easier for the first comers. Here is what the installation does:

    * It checks if all needed third-party Perl modules are available. 
	Since you got this far, they most likely are! It does not help with 
	installing them, however. Perl has a CPAN mechanism in place to do 
	that. The required modules are listed in requirements. Installation
	stops if some module is not available.

	* It creates a directory called 'Perl-SADI' in your user directory.
	Perl SADI will stop working if you move this directory because it 
	contains vital configuration information inside it.
	
	* It creates sub directories in the 'Perl-SADI' directory for places
	to store your service definitions, generated code, example service
	input, etc.

    * It creates two empty log files Perl-SADI/services.log and
	Perl-SADI/parser.log - unless they already exist. In any case,
	it changes their permissions to allow everybody to write to them.
	This helps later, when the same log files are written to by a Web
	Server. The purpose of these two files is described in logging.

    * It creates a Perl-SADI/log4perl.properties file from a distributed
	template, and updates their locations to reflect your local
	installation. Again, more about this file in logging.

    * Finally, it creates a configuration file Perl-SADI/sadi-services.cfg 
	(unless it already exists). See more about how to further configure Perl 
	SADI in configuration.

If you wish to install from scratch (the same way it was done the first time), start it by using a force option:

C<sadi-install.pl -F>

In this mode, it overwrites the files I<sadi-services.cfg>, I<services.log>, I<parser.log> and I<log4perl.properties>.

There is a little extra functionality going on behind the scenes: If the configuration file C<sadi-services.cfg> exists when you start the installation script, its values are used instead of default ones. It may be useful in cases when you plan to put all Perl SADI directories somewhere else (typically and for example, if your Web Server does not support symbolic links that can point to the current directories). In such cases, edit your sadi-services.cfg, put the new locations inside it, and run C<sadi-install.pl> again. 

=cut

=head3 What Perl SADI Really Does

Perl Moses generates Perl code. Actually, up to four pieces of the code:

=cut

=head4 Perl Datatypes Representing OWL classes for SADI services

SADISeS allows for easier implementation of SADI services by allowing you to use automatically generated PERL modules representing OWL classes in your service implementation.

The Generated perl modules that represent OWL classes have their own constructors, contain getter and setters for their datatype properties, and even have range checking for object properties. While the generator isn't perfect, it will do the job in most cases; making your service provision much more simple.

=cut

=head4 Perl Modules Representing Bases of Service Implementations

Each Perl SADI service implementation can benefit by inheriting some basic functionality from its base. These bases contain the code specific for the given service.

The service base takes care about:

    * Logging request/response.
    * Allowing to run a service locally, outside of the HTTP environment (good for early testing).
    * Catching and reporting exceptions if the input is wrong or incomplete.

You can see its code by running (for example):

C<sadi-generate-services.pl -sb HelloSadiWorld>

Again, the services bases can be generated and loaded on-the-fly, or pre-generated in the files.

=cut

=head4 Perl Modules Representing Empty Service Implementations

This is your playground! What is generated is only an empty service implementation - and you are supposed to add the meat - whatever your service is expected to do.

Well, it is not that empty, after all.

First, because it inherits from its base, it already knows how to do all the features listed in the paragraph above:

=begin html

<pre>
#-----------------------------------------------------------------
# Service name: HelloSadiWorld
# Authority:    helloworld.com
# Created:      26-Aug-2009 07:52:01 PDT
# Contact:      myaddress@organization.org
# Description:
#               the usual hello world service
#-----------------------------------------------------------------

package Service::HelloSadiWorld;

use FindBin qw( $Bin );
use lib $Bin;

#-----------------------------------------------------------------
# This is a mandatory section - but you can still choose one of
# the two options (keep one and commented out the other):
#-----------------------------------------------------------------
use SADI::Base;
# --- (1) this option loads dynamically everything
BEGIN {
    use SADI::Generators::GenServices;
    new SADI::Generators::GenServices-&gt;load(
         service_names =&gt; [&#39;HelloSadiWorld&#39;]);
}

# --- (2) this option uses pre-generated module
#  You can generate the module by calling a script:
#    moses-generate-services -b helloworld.com HelloSadiWorld
#  then comment out the whole option above, and uncomment
#  the following line (and make sure that Perl can find it):
#use com::helloworld::HelloSadiWorldBase;

# (this to stay here with any of the options above)
use vars qw( @ISA );
@ISA = qw( com::helloworld::HelloSadiWorldBase );
use strict;
</pre>

=end html

Second, it has the code that reads the input, using methods specific for this service. It does not do anything with the input, but the code shows you what methods you can use and how:

=begin html

<pre>
my @inputs = @$values;
# iterate over each input
foreach my $input (@inputs) {
     # NOTE: this fills in the log file
     $LOG-&gt;info (&quot;Input data (&quot;
        . $input-&gt;getURI ? $input-&gt;getURI : &quot;no_uri&quot;
        . &quot;)&quot;
        . $input-&gt;getLocalValue ? &quot;:\n&quot; . $input-&gt;getLocalValue : &quot;&quot;
        .&quot;&quot;) if defined $input;
     # do something with $input ... (sorry, can&#39;t help with that)

}
</pre>

=end html

And finally, it produces a fake output (not related to the input at all). Which is good because you can call the service immediately, without writing a single line of code, and because you see what methods can be used to create the real output:

=begin html

<pre>
# fill in the output nodes - this is what you need to do!
foreach my $output (0..2) {
	# for example ...
    $core-&gt;addOutputData(
        node =&gt; $core-&gt;Signature-&gt;OutputClass,
        value =&gt; &quot;$output&quot;,
    	predicate =&gt; &quot;http://sadiframework.org/ontologies/predicates.owl#somePredicate$output&quot;
	);
}
</pre>

=end html

The service implementations are definitely not generated on-the-fly. They must be pre-generated into a file (because you have to edit them, don't you?). 

Again, the C<sadi-generate-services.pl> script will do it. More in L<scripts|"Scripts">.

=cut

=head4 Perl CGI scripts representing entry points to SADI services

The files generated into the Perl-SADI/cgi directory are the gateway for the world to use your SADI services. These scripts are the ones that you will be deploying to the apache cgi-bin directory and I<usually> do not need changing! 

=cut

=head3 Scripts

Scripts

The scripts are small programs that generate pieces and that let you test things.

They share some basic features:

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* They are automatically installed with the perl module.

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* They can be started from anywhere.

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* They all are Perl programs, expecting Perl executable in /usr/bin/perl. If your perl is elsewhere, start them as:

E<nbsp>E<nbsp>E<nbsp>E<nbsp>E<nbsp>E<nbsp>E<nbsp>E<nbsp>perl -w E<lt>script-nameE<gt>

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* They all recognize an option -h, giving a short help. They also have options -v (verbose) and -d (debug) for setting the level of logging. 

Here they are in the alphabetic order:

=head4 sadi-config-status.pl

This script does not do much but gives you overview of your configuration and installation. You can run it to find how Perl SADI will behave when used. For example:

=begin html

<pre>
Perl-SADI VERSION: 0.96

Configuration
-------------
Default configuration file: sadi-services.cfg
Environment variable SADI_CFG_DIR is not set
Successfully read configuration files:
        sadi-services.cfg
All configuration parameters:
        generators.impl.definitions =&gt; /home/ubuntu/Perl-SADI/definitions
        generators.impl.home =&gt; /home/ubuntu/Perl-SADI
        generators.impl.outdir =&gt; /home/ubuntu/Perl-SADI/services
        generators.impl.package.prefix =&gt; Service
        generators.outdir =&gt; /home/ubuntu/Perl-SADI/generated
        log.config =&gt; /home/ubuntu/Perl-SADI/log4perl.properties
All imported names (equivalent to parameters above):
        $SADICFG::GENERATORS_IMPL_DEFINITIONS
        $SADICFG::GENERATORS_IMPL_HOME
        $SADICFG::GENERATORS_IMPL_OUTDIR
        $SADICFG::GENERATORS_IMPL_PACKAGE_PREFIX
        $SADICFG::GENERATORS_OUTDIR
        $SADICFG::LOG_CONFIG
        $SADICFG::LOG_FILE
        $SADICFG::LOG_LEVEL
        $SADICFG::LOG_PATTERN
Logging
-------
Logger name (use it in the configuration file): services
Available appenders (log destinations):
        Log: /home/ubuntu/Perl-SADI/services.log
        Screen: stderr
Logging level FATAL: true
Logging level ERROR: true
Logging level WARN:  true
Logging level INFO:  true
Logging level DEBUG: false

Logging configuration file
        Name: /home/ubuntu/Perl-SADI/log4perl.properties
        Contents:
log4perl.logger.services = INFO, Screen, Log
#log4perl.logger.services = DEBUG, Screen, Log

log4perl.appender.Screen = Log::Log4perl::Appender::Screen
log4perl.appender.Screen.stderr = 1
log4perl.appender.Screen.Threshold = FATAL
log4perl.appender.Screen.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.Screen.layout.ConversionPattern = %d (%r) %p&gt; [%x] %F{1}:%L - %m%n

log4perl.appender.Log = Log::Log4perl::Appender::File
log4perl.appender.Log.filename = /home/ubuntu/Perl-SADI/services.log
log4perl.appender.Log.mode = append
log4perl.appender.Log.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.Log.layout.ConversionPattern = %d (%r) %p&gt; [%x] %F{1}:%L - %m%n

# for parser debugging
log4perl.logger.parser = INFO, ParserLog
#log4perl.logger.parser = DEBUG, ParserLog

log4perl.appender.ParserLog = Log::Log4perl::Appender::File
log4perl.appender.ParserLog.filename = /home/ubuntu/Perl-SADI/parser.log
log4perl.appender.ParserLog.mode = append
log4perl.appender.ParserLog.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.ParserLog.layout.ConversionPattern = %d (%r) %p&gt; [%x] %F{1}:%L - %m%n



Testing log messages (some may go only to a logfile):
2009/08/26 08:38:37 (50) FATAL&gt; [[undef]] sadi-config-status.pl:106 - Missing Dunkin' Donuts
</pre>

=end html

=cut

=head4 sadi-generate-datatypes.pl

This script is really important for those of you wishing to use PERL modules representing your OWL classes.

Basically, this script reads your OWL file and processes all of the OWL classes and properties specified in that file. Once the processing is complete, PERL modules representing those classes and properties are either written to disk or displayed on screen.

One obvious question is "B<where are these perl objects generated>"?

You can always determine this after generation by looking in the log file - the message has the INFO level which means it is almost always logged. But, if you want to know in advance here are the rules:

=over

=item If there is a generators.outdir parameter in the configuration file, it is used. It defines the directory where OWL classes and properties are created.

=item Otherwise, program is trying to find an existing directory named 'generated' anywhere in the @INC (a set of directories used by Perl to locate its modules).

=item If it fails, it creates a new directory 'generated' in the "current" directory.

=back

You can use option I<-s> to get the generated result directly on the screen (in that case no file is created).

To generate OWL classes and properties from a remote url, you would use the B<-u> option. This option sets the base URI for the OWL file to be the URL to the OWL file (if one didn't already exist).

If the file you would like to generate classes and properties from resides on your disk, then you would use not specify any option. If the local file doesn't specify a base URI for the ontology, you can do this with the B<-b> flag. When the script executes, it will prompt you for a URI.

More often than not, your ontology will contain import statements. If you wish to follow them, you need to set the B<-i> flag. This flag tells the script to process import statements, so make sure that you have an internet connection if the referenced files are located somewhere remote.  

Like the other SADI scripts included in this distribution, you can access the script help page by running the script with the B<-h> flag.

For more information on the perl OWL modules that were generated, read the perldoc for the class or property of interest!

=cut

=head4 sadi-generate-services.pl

This is the most important script. You may use only the C<sadi-install.pl> and this one - and you will get all what you need. It generates services - all pieces belonging to services (except data types).

Usually, you generate code for one or only several services. Before you can generate an implementation for your service, you need to generate and edit a definition file for it.

C<sadi-generate-services.pl -D HelloSadiWorld> will generate a definition file for the service and will place it into Perl-SADI/definitions. Once created, all you need to do is modify the various values as you see fit.

An example of a definition file is listed below

=begin html

# make sure to escape the chars:
#    #,= with a \, for instance \#
# new lines in a property value are not supported yet!

# dont change the service name!
# if you find that you need to modify
# the service name, regenerate a new file
# and remove this one!
ServiceName = HelloSadiWorld

# modify the values below as you see fit.
ServiceType = http://someontology.org/services/sometype
InputClass = http://someontology.org/datatypes\#Input1
OutputClass = http://someontology.org/datatypes\#Output1
Description = An implementation of the &apos;HelloSadiWorld&apos; service
UniqueIdentifier = urn:lsid:myservices:HelloSadiWorld
Authority = authority.for.HelloSadiWorld
Authoritative = 1
Provider = myaddress@organization.org
ServiceURI = http://localhost/cgi-bin/HelloSadiWorld
URL = http://localhost/cgi-bin/HelloSadiWorld
SignatureURL = http://localhost/cgi-bin/HelloSadiWorld

=end html

Follow up the generation of the Service definition with a call to C<sadi-generate-services.pl> to generate your skeleton files!

C<sadi-generate-services.pl HelloSadiWorld>

There are several L<configurables options|"Configuration"> to influence the result:

C<generators.impl.outdir> dictates where the code is to be generated.

C<generators.impl.package.prefix> tells what package name should be used (the package name always ends with the service name as was used during definition generation). Default is Service.

With options, you can generated other Perl SADI pieces:

=begin html

<pre>

   Option <strong>-D</strong> generate a definition file for the service that you can fill in.
   This is the first thing that you need to generate before generating a service implementation!<p/>

   Option <strong>-b</strong> generates <a href="#perl_modules_representing_bases_of_service_implementations">service bases</a>,

   Option <strong>-S</strong> generates both <a href="#perl_modules_representing_bases_of_service_implementations">service bases</a> 
   and service implementations. This also influences how the service base will be used at run-time:  
        if it is already generated (with the -S option) there is no need to do it again in the run-time - therefore, the service implementation 
   is generated slightly differently - with an option "use the base, rather than load the base" enabled.<p/>

   Option <strong>-A</strong> generates both a <a href="#perl_modules_representing_bases_of_service_implementations">service implementation</a> 
     as well as an asynchronous module. This is not supported yet!<p/>
</pre>

=end html

You can use option I<-s> to get the generated result directly on the screen (in that case no file is created).

=cut

=head4 sadi-install.pl

This script is used for L<installation|"Installation">.

=cut

=head4 sadi-testing-service.pl

A script for the testing of your SADI web service. It does not give you the comfort that you can get from other SADI clients - but it is well suited for immediate testing.

It calls a SADI service in one of the two modes (actually the two modes are completely separated to the point that this script could be two scripts):

=over

=item * Calling the service before it is deployed (known) to a Web Server. This mode is useful for debugging. It sends SADI RDF/XML input to a service, but without using HTTP protocol. Of course, the service can be called only locally in this mode.

=item * Calling the service for real, using the Web Server, its cgi-bin script and the HTTP protocol.

=back

In both modes, the script can send an input RDF/XML file to the service - but if the input file is not given, an empty input is created and sent to the service. Which is not particularly useful, but still it can help with some preliminary testing.

When calling the service locally, you may use the following options/parameters:

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* A mandatory package name - a full package name of the called service.

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* Option -l location can be used to specify a directory where is the called service stored. Default is Perl-SADI/services.

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* Options -v and -d make also sense in this mode (but not in the other one).

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* An optional input file name. 

C<sadi-testing-service.pl -d Service::HelloSadiWorld>

The output of this call was already shown in this L<documentation|"Quick Start - Five Steps to the First Service">. Therefore, just look what debug messages were logged (notice the -d option used):

=begin html

<pre>
    2009/08/26 09:17:32 (218) INFO&gt; [6283] (eval 45):38 - *** REQUEST START ***
    2009/08/26 09:17:32 (218) DEBUG&gt; [6283] (eval 45):46 - Input raw data:
    &lt;?xml version=&quot;1.0&quot;?&gt;
    &lt;rdf:RDF
      xmlns:b=&quot;http://www.w3.org/2000/01/rdf-schema#&quot;
      xmlns:a=&quot;http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#&quot;
      xmlns:rdf=&quot;http://www.w3.org/1999/02/22-rdf-syntax-ns#&quot;&gt;
    &lt;/rdf:RDF&gt;
    2009/08/26 09:17:32 (227) INFO&gt; [6283] (eval 45):85 - *** RESPONSE READY ***
</pre>

=end html

The full mode has the following options/parameters:

One of:

=over

=item * E<nbsp>E<nbsp>E<nbsp>E<nbsp>* A SADI service endpoint -e endpoint defining where is the service located, or

=item * E<nbsp>E<nbsp>E<nbsp>E<nbsp>* A SADI service endpoint -g endpoint defining where is the service located.

=back

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* An optional input file name. This method only makes sense with the -e option. As the B<-g> option is used to get the service interface.

=begin html

<pre>
    sadi-testing-service.pl \
           -e http://localhost/cgi-bin/HelloSADIWorld
</pre>

=end html

=cut

=cut

=head3 Configuration

Configuration means to avoid hard-coding local-specific things (such as file paths) into the code itself but hard-coding them in a separate file, a file that is not shared with other (CVS) users.

Perl SADI stores configuration in a file named I<moby-services.cfg>. The file name is hard-coded (and cannot be changed without changing the I<SADI::Config> module), but its location can be set using an environment variable I<SADI_CFG_DIR>. Perl SADI looks for its configuration place in the following places, in this order:

   1. In the "current" directory (which is not that well defined when used from a Web Server).
   2. In the directory given by SADI_CFG_DIR environment variable.
   3. In the directory <your-user-dir>/Perl-SADI/.
   4. In one of the @INC directories (directories where Perl looks for its modules). 

Therefore, the best place is to keep the configuration file together where the installation script puts it anyway.

The Perl SADI internally uses C<Config::Simple> CPAN module, but wraps it into its own SADI::Config. This allows expansion later, or even changing the underlying configuration system. The Config::Simple is simple (thus the name, and thus we selected it) but has few drawbacks that may be worth to work on later.

The file format is as defined by the C<Config::Simple>. It can be actually of several formats. The most common is the one distributed in the sadi-services.cfg.template. This is an example of a configuration file:

=begin html

<pre>
[generators]
outdir = /home/ubuntu/Perl-SADI/generated
impl.outdir = /home/ubuntu/Perl-SADI/services
impl.package.prefix = Service
impl.definitions    = /home/ubuntu/Perl-SADI/definitions
impl.home = /home/ubuntu/Perl-SADI

[log]
config = /home/ubuntu/Perl-SADI/log4perl.properties
#file = /home/ubuntu/Perl-SADI/services.log
#level = info
#pattern = &quot;%d (%r) %p&gt; [%x] %F{1}:%L - %m%n&quot;
</pre>

=end html

The names of the configuration parameters are created by concatenating the "section" name (the one in the square brackets) and the name itself. For example, the logger configuration is specified by the parameter I<log.config>. Parameters that are outside of any section has just their name, or they can be referred to as from the I<default> section. For example, these two names are equivalent: I<default.cachedir> and I<cachedir>.

Blank lines are ignored, comments lines start with a hash (#), and boolean properties B<must> have a value ('true' or 'false').

Obviously, important is to know what can be configured, and how. This document on various places already mentioned several configuration options. Here is their list (for more explanations about their purpose you may visit an appropriate section of this document):  

B<generators.outdir> - Directory where to generate data types and service bases. The default value for data types is 'generated', for service bases is 'services'. 

B<generators.impl.outdir> - Directory where to generate service implementations. Default is 'Perl-SADI/services'. 

B<generators.impl.package.prefix> - A beginning of the package name of the generated service implementations. Default is 'Service'. For example, a service Mabuhay will be represented by a Perl module I<Service::Mabuhay>. 

B<generators.impl.definitions> - Directory where SADI web service definition files are kept.

B<generators.impl.home> - The users Perl-SADI directory. Default is 'Perl-SADI'.

B<log.config> - A full file name with the Log4perl properties. No default. If this parameter is given but the file is not existing or not readable, Perl SADI complains on STDERR (which may end up in the Web Server I<error.log> file). 

B<log.file> - A full file name of a log file (where the log messages will be written to). No default. If the value is 'stderr' (case-insensitive) the messages will go to the STDERR. It is not clear what happens when it is used together with the above I<log.config>. 

B<log.level> - A log level. Default is ERROR. 

B<log.pattern> - A format of the log messages. Default is I<'%d (%r) %pE<gt> [%x] %F{1}:%L - %m%n'>.

The parameters just described are used by Perl SADI modules - but the configuration system is here also for your own services. You can invent any not-yet-taken name, and add your own parameter. In order not to clash with the future Perl Moses parameters, it is recommended to prefix your configuration properties with the service name. For example, the HelloSadiWorld service needs to read a file with "hellos" in many languages, so it defines:

=begin html

<pre>
[HelloSadiWorld]
resource.file = /home/ubuntu/Perl-SADI/samples-resources/HelloSadiWorld.file
</pre>

=end html

=head4 How to use configuration in your service implementation?

All configuration parameters are imported to a Perl namespace SADICFG. The imported names are changed to all-uppercase and dots are replaces by underscores. You can see this change if you run the config-status.cfg:

=begin html

<pre>
$SADICFG::GENERATORS_IMPL_DEFINITIONS
$SADICFG::GENERATORS_IMPL_HOME
$SADICFG::GENERATORS_IMPL_OUTDIR
$SADICFG::GENERATORS_IMPL_PACKAGE_PREFIX
$SADICFG::GENERATORS_OUTDIR
$SADICFG::LOG_CONFIG
$SADICFG::LOG_FILE
$SADICFG::LOG_LEVEL
$SADICFG::LOG_PATTERN
$SADICFG::HELLOSADIWORLD_RESOURCE_FILE
</pre>

=end html

In your program, you can use the imported names. For example, here is how the HelloSadiWorld service opens its resource file:

=begin html

<pre>
open HELLO, $SADICFG::HELLOSADIWORLD_RESOURCE_FILE
   or $self->throw ('HelloSadiWorld resource file not found.');
</pre>

=end html

You can also change or add parameters during the run-time. For example, to overwrite existing parameters because you want to create everything in a separate place, in a temporary directory, and within the 'Testing' package. Because the generators read from the configuration files:

=begin html

<pre>
my $outdir = File::Spec->catfile ($tmpdir, 'generated-services');
SADI::Config->param ('generators.impl.outdir', $outdir);
SADI::Config->param ('generators.impl.package.prefix', 'Testing');
unshift (@INC, $SADICFG::GENERATORS_IMPL_OUTDIR);
my $generator = new SADI::Generators::GenServices;
</pre>

=end html

More about how to communicate pragmatically with the configuration can be (hopefully) find in the L<Perl Modules Documentation|"Perl Modules Documentation">.

=cut

=cut

=head3 Logging

The logging system is based on a splendid Perl module Log::Log4perl, a Perl port of the widely popular log4j logging package. The Log4perl is well documented (here is its POD documentation L<http://search.cpan.org/~mschilli/Log-Log4perl-1.06/lib/Log/Log4perl.pm>).

How does it work in Perl SADI?

The logging is available from the moment when Perl SADI knows about the SADI::Base module. All generated service implementations inherit from this class, so all of them have immediate access to the logging system. By default, the SADI::Base creates a logger in a variable $LOG. Which means that in your service implementation you can log events in five different log levels:

=begin html

<pre>
$LOG->debug ("Deep in my mind, I have an idea...");
$LOG->info  ("What a nice day by a keyboard.");
$LOG->warn  ("However, the level of sugar is decreasing!");
$LOG->error ("Tim Hortons'' Donuts");
$LOG->fatal ('...and we are out of their splendid coffee!');
</pre>

=end html

The logger name is "services". (The name is used in the logging configuration file - see below).

You can create your own logger. Which may be good if you wish to have, for example, a different logging level for a particular service, or for a part of it! Here is what you need to do:

=begin html

<pre>
use Log::Log4perl qw(get_logger :levels);
my $mylog = get_logger ('my_log_name');
</pre>

=end html

Then use the name "my_log_name" in the configuration to set its own properties. Which brings also us to the logging configuration.

The logging configuration can be done in three ways:

=over

=item * Do nothing.

=item * Edit log4perl.properties file.

=item * Edit logging configuration options in sadi-services.cfg. 

=back

If Perl SADI cannot find a I<log4perl.properties> file, and if there are no logging options in I<sadi-services.cfg>, it assumes some defaults (check them in I<SADI::Base>, in its BEGIN section, if you need-to-know).

The better way is to use I<log4perl.properties> file. The file name can be actually different - it is specified by an option log.config in the sadi-services.cfg configuration file. This is what Perl SADI installation creates there (of course, using your own path):

=begin html

<pre>
[log]
config = /home/ubuntu/Perl-SADI/log4perl.properties
</pre>

=end html

The I<log4perl.properties> is created (in the installation time) from the I<log4perl.properties.template>, by putting there your specific paths to log files. The log4perl (or log4j) documentation explains all details - here is just a brief example what is in the file and what it means:

=begin html

<pre>
log4perl.logger.services = INFO, Screen, Log
&nbsp; 
log4perl.appender.Screen = Log::Log4perl::Appender::Screen
log4perl.appender.Screen.stderr = 1
log4perl.appender.Screen.Threshold = FATAL
log4perl.appender.Screen.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.Screen.layout.ConversionPattern = %d (%r) %p> [%x] %F{1}:%L - %m%n
&nbsp;
log4perl.appender.Log = Log::Log4perl::Appender::File
log4perl.appender.Log.filename = /home/ubuntu/Perl-SADI/services.log
log4perl.appender.Log.mode = append
log4perl.appender.Log.layout = Log::Log4perl::Layout::PatternLayout
log4perl.appender.Log.layout.ConversionPattern = %d (%r) %p> [%x] %F{1}:%L - %m%n
</pre>

=end html


It says: Log only INFO (and above) levels (so no DEBUG messages are logged) on the screen (meaning on the STDERR) and to a file. But because of the "screen appender" has defined a Threshold FATAL - the screen (STDERR) will get only FATAL messages. There is no threshold in the "file appender" so the file gets all the INFO, WARN, ERROR and FATAL messages. In both cases the format of the messages is defined by the "ConversionPattern".

Note that printing to STDERR means that the message will go to the error.log file of your Web Server.

To change the log level to DEBUG, replace INFO by DEBUG in the first line.

The message format (unless you change the Perl SADI default way) means:

=begin html

<pre>
%d                  (%r ) %p   > [%x   ] %F{1}               :%L - %m      %n
2009/08/28 11:38:07 (504) FATAL> [26849] HelloSadiWorld:63 - Go away!
1                   2     3      4       5                    6    7       8
</pre>

=end html

Where:

=begin html

<ul>
  <li> <font size="-1"><b>1</b></font> (%d) - Current date
in yyyy/MM/dd hh:mm:ss format
  </li><li> <font size="-1"><b>2</b></font> (%r) - Number of
milliseconds elapsed from program start to logging
   event
  </li><li> <font size="-1"><b>3</b></font> (%p) - Level
(priority) of the logging event
  </li><li> <font size="-1"><b>4</b></font> (%x) - Process ID
(kind of a <em>user session</em>)
  </li><li> <font size="-1"><b>5</b></font> (%F) - File where
the logging event occurred (unfortunately, it is not always that
useful - when it happens in an <em>eval</em> block - which often
does).
  </li><li> <font size="-1"><b>6</b></font> (%L) - Line number
within the file where the log statement was issued
  </li><li> <font size="-1"><b>7</b></font> (%m) - The message
to be logged
  </li><li> <font size="-1"><b>8</b></font> (%n) - Newline

</li></ul>

=end html

The last option how to specify logging properties is to set few configuration options in the sadi-service.cfg file. It was already mentioned that there is an option log.config that points to a full log property file. If this option exists, no other logging configuration options are considered. But if you comment it out, you can set the basics in the following options:

=begin html

<pre>
[log]
#config = /home/ubuntu/Perl-SADI/log4perl.properties
file = /home/ubuntu/Perl-SADI/services.log
level = info
pattern = "%d (%r) %p> [%x] %F{1}:%L - %m%n"
</pre>

=end html

Where log.file defines a log file, I<log.level> specifies what events will be logged (the one mentioned here and above), and the I<log.pattern> creates the format of the log events.

This is meant for a fast change in the logging system (perhaps during the testing phase).

There are definitely more features in the Log4perl system to be explored:

For example, in the mod_perl mode it would be interesting to use the "Automatic reloading of changed configuration files". In this mode, C<Log::Log4perl> will examine the configuration file every defined number of seconds for changes via the file's last modification time-stamp. If the file has been updated, it will be reloaded and replace the current logging configuration.

Or, one can explore additional log appenders (you will need to install additional Perl modules for that) allowing, for example, to rotate automatically log files when they reach a given size. See the Log4perl documentation for details. 

=cut

=head3 Deploying

Deploying means to make your SADI web service visible via your Web Server.

By contrast to the deployment of the Java-based services, Perl services are invoked by a cgi-bin script directly from a Web Server - there is no other container, such as Tomcat in the Java world. Which makes life a whole bunch easier. Well, only slightly, because soon you will start to think about using mod_perl module, and it may make things complicated.

TO BE DONE
The Perl SADI was not tested in the mod_perl environment, at all. But it should be. I wonder if anybody can explore this a bit.
In order to deploy a Perl SADI service, you need:

=begin html

<ul>

  <li> To run a Web Server, such as Apache. It has a directory where
its cgi-bin scripts are located (e.g. <tt>/usr/lib/cgi-bin</tt>). You
may configure your Web Server to allow to have other cgi-bin script
elsewhere, or to link them by symbolic links. An example how to create
a symbolic link is in <a href="#4._Make_your_service_available_from_your_Web_Server_(this_is_called_deploying).">5 steps to your first service</a>.

  </li><li> Then you need the cgi-bin script itself. Its name does not
matter (except that it will become a part of your services
URL (aka endpoint)). The Perl SADI service generation script creates a 
script with the name that you give it in <tt>/Perl-SADI/cgi</tt> directory.
Notice that it contains some hard-coded paths - that comes from the sadi-services.cfg file.
Feel free to change them manually, or remove the file and run <tt>sadi-install.pl</tt> again to 
re-create it. Here is a whole script (for HelloSadiWorld):
</li>
</ul>
<pre>

#!/usr/bin/perl -w
# This is a CGI-BIN script, invoked by a web server when an HTTP
# POST comes in, dispatching requests to the appropriate module
# (SADI web service).
#
# It includes some hard-coded paths - they were added during the
# generate service call.
#
# $Id: SADI.pm,v 1.36 2010-03-09 18:04:43 ubuntu Exp $
# Contact: Edward Kawas &lt;edward.kawas@gmail.com&gt;
# ---------------------------------------------------------------

#-----------------------------------------------------------------
# Authority:    helloworld.com
# Service name: HelloSadiWorld
# Generated:    26-Aug-2009 10:15:16 PDT
# Contact: Edward Kawas &lt;edward.kawas@gmail.com&gt;
#-----------------------------------------------------------------

use strict;


# --- during service generation ---
# leave at the top!
use lib &#39;/home/ubuntu/Perl-SADI/generated&#39;;
use lib &#39;/home/ubuntu/Perl-SADI/services&#39;;
use lib &#39;/home/ubuntu/Perl-SADI&#39;;

use CGI;
use CGI::Carp qw(fatalsToBrowser);

use SADI::Service::Instance;
use SADI::RDF::Core;
use SADI::Generators::GenServices;

# limit the max size of a post - change it as you see fit
$CGI::POST_MAX=1024 * 1024 * 10;  # max 10MB posts

# here we require the service module and add it to ISA hierarchy
use base &#39;Service::HelloSadiWorld&#39;;

# get the cgi variable
my $q = new CGI;

# if this is a GET, send the service interface
if ($ENV{REQUEST_METHOD} eq &#39;GET&#39;) {
    # send the signature for this service
        # instantiate a new SADI::RDF::Core object
        my $core = SADI::RDF::Core-&gt;new;
        # set the Instance for $core
        $core-&gt;Signature(__PACKAGE__-&gt;get_service_signature(&#39;HelloSadiWorld&#39;));
        print $q-&gt;header(-type=&gt;&#39;text/xml&#39;);
    print $core-&gt;getServiceInterface();

} else {
    # call the service
    # get the data from the &#39;data&#39; parameter
    my $data = $q-&gt;param(&#39;data&#39;) || $q-&gt;param(&#39;POSTDATA&#39;) || &quot;&quot;;
    # call the service
    my $x =  __PACKAGE__-&gt;HelloSadiWorld($data);
    # print the results
    print $q-&gt;header(-type=&gt;&#39;text/xml&#39;);
    print $x;
}

__END__

</pre>

=end html

These 2 things (a Web Server knowing about your cgi-bin scripts and a cgi-bin script knowing about Perl SADI code) are all you need to have your service deployed. 

=cut

=head2 Perl Modules Documentation

After reading so far you may still wonder: Okay, but what should I do in my implementation to gain all the benefits generated for me by Perl SADI? This section will try to answer it - but notice that some particular activities were already explained in corresponding sections about L<logging|"Logging"> and L<configuration|"Configuration">.

=head4 How to write a service implementation

First of all, you need to have a service implementation, at least its starting (empty) phase. Generate it, using the C<sadi-generated-services> script. Depending on how you generate it (without any option, or using an -S option) generator enables one of the following options (not that it matters to your business logic code):

=begin html

<pre>
#-----------------------------------------------------------------
# This is a mandatory section - but you can still choose one of
# the two options (keep one and commented out the other):
#-----------------------------------------------------------------
use SADI::Base;
# --- (1) this option loads dynamically everything
BEGIN {
    use SADI::Generators::GenServices;
    new SADI::Generators::GenServices-&gt;load(
         service_names =&gt; [&#39;HelloSadiWorld&#39;]);
}

# --- (2) this option uses pre-generated module
#  You can generate the module by calling a script:
#    moses-generate-services -b helloworld.com HelloSadiWorld
#  then comment out the whole option above, and uncomment
#  the following line (and make sure that Perl can find it):
#use com::helloworld::HelloSadiWorldBase;
</pre>

=end html


Secondly, you need to understand when and how your implementation code is called:

Your service implementation has to implement method process_it that is called for every incoming SADI web service request. The C<SADI::Service::ServiceBase> has details about this method (what parameters it gets, how to deal with exceptions, etc.).

In the beginning of the generated process_it method is the code that tells you what methods are available for reading inputs, and at the end of the same method is the code showing how to fill the response. Feel free to remove the code, extend it, fill it, turn it upside-down, whatever. This is, after all, your implementation. And Perl SADI generator is clever enough not to overwrite the code once is generated. (It is not clever enough, however, to notice that it could be overwritten because you have not touched it yet.)

Perhaps the best way how to close this section is to show a full implementation of (so often mentioned) service HelloSadiWorld:

=begin html

<pre>
#-----------------------------------------------------------------
# process_it
#    This method is called for every client request.
#    Input data are in $values.
#    You will need to add output via $core-&gt;addOutputData()
#        this method adds nodes to the final output.
#        param keys: node, value, predicate
#-----------------------------------------------------------------
sub process_it {
    my ($self, $values, $core) = @_;
    # empty data, then return
    return unless $values;

    my @inputs = @$values;
    # iterate over each input
    foreach my $input (@inputs) {
        # NOTE: this fills in the log file
        $LOG-&gt;info (&quot;Input data (&quot;
                . $input-&gt;getURI ? $input-&gt;getURI : &quot;no_uri&quot;
                . &quot;)&quot;
                . $input-&gt;getLocalValue ? &quot;:\n&quot; . $input-&gt;getLocalValue : &quot;&quot;
                .&quot;&quot;)
                if defined $input;
        # do something with $input ... (sorry, can&#39;t help with that)

        }

        # fill in the output nodes - this is what you need to do!
        foreach my $output (0..2) {
            # for example ...
                $core-&gt;addOutputData(
                        node =&gt; $core-&gt;Signature-&gt;OutputClass,
                value =&gt; &quot;$output&quot;,
                    predicate =&gt; &quot;http://sadiframework.org/ontologies/predicates.owl#somePredicate$output&quot;
                );
        }
}
</pre>

=end html

When you go through the code above you notice how to do basic things that almost every service has to do. Which are:

Reading input data:

The possible methods were already pre-generated for you so you know what methods to use. But you should always check if the data are really there (the clients can send you rubbish, of course).

The question is what to do if input (or anything else) is not complete or valid. This brings us to...

Reporting exceptions:

=begin html

<pre>
    One option is to throw an exception:
&nbsp;
    open HELLO, $SADICFG::HELLOSADIWORLD_RESOURCE_FILE
         or $self->throw ('HELLO SADI WORLD resource file not found.');
</pre>

=end html

This immediately stops the processing of the input request.

Another, less drastic, option is to record an exception (and, usually, return):

=begin html

<pre>
    TO BE DONE - not sure of how to report errors in SADI
</pre>

=end html

Creating output data:

=begin html

&nbsp;&nbsp;&nbsp;&nbsp;Again, methods for creating response were pre-generated, so you have hints how to use them.
<p/>
<pre>
foreach my $output (0..2) {
    # for example ...
    $core-&gt;addOutputData(
        node =&gt; $core-&gt;Signature-&gt;OutputClass,
        value =&gt; &quot;$output&quot;,
        predicate =&gt; &quot;http://sadiframework.org/ontologies/predicates.owl#somePredicate$output&quot;
    );
}
</pre>

=end html

For more information on how to use the $core variable, refer to C<SADI::RDF::Core>!

=cut

=cut

=head2 FAQ

=head3 How can I tell apache to execute HelloSadiWorld on Windows without moving the file to cgi-bin?

=begin html

<p>This can be done using the following steps <em><strong>(Please make sure to back up the file first!)</strong></em>:</p>
<ol>
  <li> Open the file httpd.conf and search for following text:<br />
  <em>ScriptAlias /cgi-bin/</em></li>
  <li>Underneath this text, enter something like the following (replace Eddie with your username):<br /> 
  <em>ScriptAlias /services/ &quot;C:/Documents/Eddie/Perl-SADI/&quot;</em></li>
  <li> Just below this, after the <em>&lt;/IfModule&gt;</em> line, add the following text (replace Eddie with your username and the directory with your directory):
    <pre>
      &lt;Directory &quot;C:/Documents/Eddie/Perl-SADI&quot;&gt;
         AllowOverride None
         Options +ExecCGI
         Order allow,deny
         Allow from all
      &lt;/Directory&gt;
    </pre>
  </li>
  <li>Save the file and restart apache.  </li>
  <li>The very last thing to do is to open up the file <em><strong>HelloSadiWorld</strong></em> and change the header <br />
  &nbsp;&nbsp;<strong><em>&nbsp;#!/usr/bin/perl -w </em></strong><br />
  to <br />
  &nbsp;&nbsp;&nbsp;<strong><em>#!C:/path/to/perl/bin/perl.exe -w</em></strong></li>
  <li>Now anytime you read about http://localhost/cgi-bin/HelloSadiWorld, replace it with http://localhost/services/HelloSadiWorld </li>
</ol>

=end html

=cut

=head3 How Can Apache Follow Symbolic links?

Add the following to the end of your httpd.conf file:

=begin html

<pre>
    &lt;Directory &quot;/path/to/cgi-bin&quot; &gt;
	    Options +FollowSymLinks
	&lt;/Directory&gt;
</pre>

=end html

Make sure to change I</path/to/cgi-bin> to be the real path to your cgi-bin directory!

=cut

=head3 Cannot Create Symbolic links

If you cannot create symbolic links, another tested alternative would be to copy your file B<HelloSadiWorld> to the I<cgi-bin> directory of your web server.

Once the file has been copied, change the ownership of the file to the web servers' user/group. Also, make sure that the path (and its parents) to all of the directories in the 'use lib ...' are readable by your web server. 

That's all there is to it! Now when you test your services, remember that your file may no longer be called HelloSadiWorld, but something else that you named it!

=cut

=head3 When I run the install script, IO::Prompt complains ...

This could mean that the package C<IO::Prompt> is not installed properly.

What version do you have?

C<perl -MIO::Prompt -e'print "$IO::Prompt::VERSION\n";'>

We have tested version 0.99.2 on both *nix machines and windows. Please make sure that you have  that version. If you do not, please remove the one that you have (the cpan module B<CPAN Plus> is very useful here) and install version 0.99.2! Version 0.99.4 doesnt seem to work too well and produces numerous warnings in our module. Other versions have yet to be tested.

=cut

=cut

=head2 Missing Features

There will be always bugs waiting to be fixed. Please let us know about them.

And there are features (and known) bugs that should or could be implemented (or fixed). Here are those I am aware of (B = bug, N = not yet implemented, F = potential future feature):

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* (N) Parsing the input and output owl classes and creating perl module stubs for them so that you can use them in your code and not have to manipulate RDF by yourself.

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* (N) Documentation of the Perl Modules is unfinished, and links to it are not yet included in this document. This is an important part of the documentation because it expands hints how to write your own service implementation.

E<nbsp>E<nbsp>E<nbsp>E<nbsp>* (N) The generated service implementation could have a better Perl documentation, listing all available methods for inputs and outputs (the methods are already shown in the code, but having them also in the POD would help).

An up-to-date list of the recent changes is in the Changes file included with the distribution. 

=cut

=head2 Acknowledgement

The main perl developers (whom please direct your suggestions and reports to) are Edward Kawas and Mark Wilkinson.

However, there would be no SADISes without CardioSHARE - CardioSHARE is generously supported by the Heart and Stroke Foundation of B.C. and Yukon, the Canadian Institutes of Health Research, and Microsoft Research.

Lots of code was written by Martin Senger for the Perl MoSeS module and used in this module. 

=cut

=head1 EXPORT

None by default.


=head1 SEE ALSO

For information on SADI, please see 

=begin html

<a href="http://sadiframework.org/">http://sadiframework.org/</a>

=end html

Please visit the SADI website at L<http://sadiframework.org>!

=cut

=head1 AUTHORS

=begin html

<a href="mailto:&#101;&#100;&#119;&#97;&#114;&#100;&#46;&#107;&#97;&#119;&#97;&#115;+&#115;&#97;&#100;&#105;&#115;&#101;&#115;&#64;&#103;&#109;&#97;&#105;&#108;&#46;&#99;&#111;&#109;">Edward Kawas</a>

=end html

=cut

=head1 COPYRIGHT

Copyright (c) 2009, Mark Wilkinson
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the University of British Columbia nor the names of 
      its contributors may be used to endorse or promote products derived from 
      this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.

=cut
