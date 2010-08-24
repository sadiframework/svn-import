#-----------------------------------------------------------------
# SADI::Generators::GenServices
# Author: Martin Senger <martin.senger@gmail.com>,
#         Edward Kawas <edward.kawas@gmail.com>
#
# For copyright and disclaimer see below.
#
# $Id: GenServices.pm,v 1.19 2010-04-16 15:54:59 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::Generators::GenServices;
use SADI::Utils;
use SADI::Service::Instance;
use SADI::Service::UnitTest;
use SADI::Base;
use base qw( SADI::Base );
use FindBin qw( $Bin );
use lib $Bin;
use Template;
use File::Spec;
use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.20 $ =~ /: (\d+)\.(\d+)/;

#-----------------------------------------------------------------
# A list of allowed attribute names. See SADI::Base for details.
#-----------------------------------------------------------------
{
	my %_allowed =
	  ( outdir => undef, definitions => undef, unit_tests_dir => undef );

	sub _accessible {
		my ( $self, $attr ) = @_;
		exists $_allowed{$attr} or $self->SUPER::_accessible($attr);
	}

	sub _attr_prop {
		my ( $self, $attr_name, $prop_name ) = @_;
		my $attr = $_allowed{$attr_name};
		return ref($attr) ? $attr->{$prop_name} : $attr if $attr;
		return $self->SUPER::_attr_prop( $attr_name, $prop_name );
	}
}

#-----------------------------------------------------------------
# init
#-----------------------------------------------------------------
sub init {
	my ($self) = shift;
	$self->SUPER::init();
	$self->outdir( $SADICFG::GENERATORS_OUTDIR
				   || SADI::Utils->find_file( $Bin, 'generated' ) );
	$self->definitions( $SADICFG::GENERATORS_IMPL_DEFINITIONS
						|| SADI::Utils->find_file( $Bin, 'definitions' ) );
	$self->unit_tests_dir(    $SADICFG::UNITTEST_DIR
						   || SADI::Utils->find_file( $Bin, 'unittest' )
						   || undef );
}

#-----------------------------------------------------------------
# generate_base
#-----------------------------------------------------------------
sub generate_base {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		outdir        => $self->outdir,
		service_names => [],

		# other args, with no default values
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{outdir} );
	$LOG->debug(
		"Arguments for generating service bases: " . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	$LOG->("Services will be generated into: '$outdir'")
	  unless $args{outcode};
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};

	# for each name, load the file
	# items in @services is of type SADI::Service::Instance
	my $services = $self->read_services(@names);

	# generate from template
	my $tt = Template->new( ABSOLUTE => 1 );
	my $input = File::Spec->rel2abs(
				  SADI::Utils->find_file(
					  $Bin, 'SADI', 'Generators', 'templates', 'service-base.tt'
				  )
	);
	if ( scalar @$services == 0 ) {
		my $msg = "Didn't find any services for @names!"
		  . "\nPlease make sure that you create a service definition first!";
		$LOG->warn($msg);
		$self->throw($msg);
	}
	foreach my $obj (@$services) {
		my $name = $obj->ServiceName;
		my $module_name =
		  $self->service2module( $obj->Authority, $obj->ServiceName );
		$LOG->debug("\tGenerating base for $name\n");
		if ( $args{outcode} ) {

			# check if the same service is already loaded
			# (it can happen when this subroutine is called several times)
			next if eval '%' . $module_name . '::';
			$tt->process(
						  $input,
						  {
							 obj         => $obj,
							 module_name => $module_name,
						  },
						  $args{outcode}
			) || $LOG->logdie( $tt->error() );
		} else {

			# we cannot easily check whether the same file was already
			# generated - so we don't
			my $outfile =
			  File::Spec->catfile( $outdir, split( /::/, $module_name ) )
			  . '.pm';
			$tt->process(
						  $input,
						  {
							 obj         => $obj,
							 module_name => $module_name,
						  },
						  $outfile
			) || $LOG->logdie( $tt->error() );
		}
	}
}

#-----------------------------------------------------------------
# generate_async_base
#-----------------------------------------------------------------
sub generate_async_base {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		outdir        => $self->outdir,
		service_names => [],

		# other args, with no default values
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{outdir} );
	$LOG->debug(
		"Arguments for generating service bases: " . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	$LOG->debug("Services will be generated into: '$outdir'")
	  unless $args{outcode};
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};

	# for each name, load the file
	# items in @services is of type SADI::Service::Instance
	my $services = $self->read_services(@names);

	# generate from template
	my $tt = Template->new( ABSOLUTE => 1 );
	my $input = File::Spec->rel2abs(
									 SADI::Utils->find_file(
													  $Bin,         'SADI',
													  'Generators', 'templates',
													  'service-base-async.tt'
									 )
	);
	if ( scalar @$services == 0 ) {
		my $msg = "Didn't find any services for @names!"
		  . "\nPlease make sure that you create a service definition first!";
		$LOG->warn($msg);
		$self->throw($msg);
	}
	foreach my $obj (@$services) {
		my $name = $obj->ServiceName;
		my $module_name =
		  $self->service2module( $obj->Authority, $obj->ServiceName );
		$LOG->debug("\tGenerating base for $name\n");
		if ( $args{outcode} ) {

			# check if the same service is already loaded
			# (it can happen when this subroutine is called several times)
			next if eval '%' . $module_name . '::';
			$tt->process(
						  $input,
						  {
							 obj         => $obj,
							 module_name => $module_name,
						  },
						  $args{outcode}
			) || $LOG->logdie( $tt->error() );
		} else {

			# we cannot easily check whether the same file was already
			# generated - so we don't
			my $outfile =
			  File::Spec->catfile( $outdir, split( /::/, $module_name ) )
			  . '.pm';
			$tt->process(
						  $input,
						  {
							 obj         => $obj,
							 module_name => $module_name,
						  },
						  $outfile
			) || $LOG->logdie( $tt->error() );
		}
	}
}

#-----------------------------------------------------------------
# load
#    load (cachedir      => dir,
#          service_names => [..], ... )
#-----------------------------------------------------------------
sub load {
	my ( $self, @args ) = @_;
	my $code = '';
	$self->generate_base( @args, outcode => \$code );
	eval $code;
	$LOG->logdie("$@") if $@;
}

#-----------------------------------------------------------------
# load
#    load (cachedir      => dir,
#          service_names => [..], ... )
#-----------------------------------------------------------------
sub async_load {
	my ( $self, @args ) = @_;
	my $code = '';
	$self->generate_async_base( @args, outcode => \$code );
	eval $code;
	$LOG->logdie("$@") if $@;
}

#-----------------------------------------------------------------
# read_services
#    loads the service definition file(s) and creates an array
#    of SADI::Service::Instance objects from the file(s).
#-----------------------------------------------------------------
sub read_services {
	my ( $self, @files ) = @_;
	my @services;
	foreach my $file (@files) {
		$file = SADI::Utils->find_file( $self->definitions, $file );
		my %config;
		eval { Config::Simple->import_from( $file, \%config ); };
		$LOG->warn("Error reading service def from '$file'\n$@") if $@;
		next if $@;

		# replicate these keys without the prefix 'default'
		foreach my $key ( keys %config ) {
			my ($realkey) = ( $key =~ /^$Config::Simple::DEFAULTNS\.(.*)/ );
			if ( $realkey && !exists $config{$realkey} ) {
				$config{$realkey} = $config{$key};
				delete $config{$key};
			}
		}

		# Remove potential whitespaces from the keys (Config::Simple may
		# leave theme there)
		map {
			my $orig_key = $_;
			s/\s//g and $config{$_} = delete $config{$orig_key}
		} keys %config;
		$LOG->debug( $self->toString( \%config ) )
		  if ( $LOG->is_debug );
		do {
			$LOG->warn(
				   "'$file' contains an invalid service def. Please check it.");
			next;
		  } unless (     defined $config{URL}
					 and defined $config{Authority}
					 and defined $config{ServiceName}
					 and defined $config{ServiceType}
					 and defined $config{InputClass}
					 and defined $config{OutputClass}
					 and defined $config{Description}
					 #and defined $config{ServiceURI}
					 #and defined $config{UniqueIdentifier} 
		);
		my $service = SADI::Service::Instance->new;
		map { $service->$_( $config{$_} ) } keys %config;

		# TODO check for unit test information ...
		if ( -e $self->unit_tests_dir and -d $self->unit_tests_dir ) {
			my $test_file = File::Spec->catfile( $self->unit_tests_dir,
												 $service->ServiceName );
			if ( -e $test_file ) {

				# read the file and parse out the unit tests
				my $content;
				{
					local $/ = undef;
					open FILE, "$test_file"
					  or $LOG->warn("Couldn't open file: $!");
					$content = <FILE>;
					close FILE;
				}
				if ( defined $content ) {
					my @tests = split /(\[\s*\S+\s*\][^\[]*)/s, join "",
					  $content;
					foreach my $test (@tests) {
						next if SADI::Utils::trim($test) eq '';
						my $sect;
						my $key;
						my @lines = split "\n", $test;
						foreach my $line (@lines) {
							chomp $line;
							next unless $line;
							next if $line =~ /\s*\#/;    # ignore comments
							next unless $line =~ /\S/; # ignore pure whitespace;
							if ( $line =~ /\[(\w+)\]/ ) {
								$key = $1;
								next;
							}
							my @terms = split( /=/, $line );
							next unless ( $#terms >= 1 );
							$sect->{ SADI::Utils::trim( $terms[0] ) } =
							  SADI::Utils::trim( $terms[1] );
						}

						# populate the unit test
						my $unit_test = new SADI::Service::UnitTest;
						map { $unit_test->$_( $sect->{$_} ) } keys %{$sect};

						# add the test to our service
						$service->add_UnitTest($unit_test) if keys %{$sect};
					}
				}
			}
		}
		push @services, $service;
	}
	return \@services;
}

#-----------------------------------------------------------------
# generate_impl
#-----------------------------------------------------------------
sub generate_impl {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		impl_outdir => (
						 $SADICFG::GENERATORS_IMPL_OUTDIR
						   || SADI::Utils->find_file( $Bin, 'services' )
		),
		impl_prefix   => $SADICFG::GENERATORS_IMPL_PACKAGE_PREFIX,
		service_names => [],
		force_over    => 0,
		static_impl   => 0,
		do_owl2perl   => 1, # try to use owl2perl datatypes by default

		# other args, with no default values
		# authority     => 'authority'
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{impl_outdir} );
	$LOG->debug( "Arguments for generating service implementation:\n"
				 . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};
	my $services = $self->read_services(@names);
	if ( scalar @$services == 0 ) {
		my $msg = "Didn't find any services for @names!"
		  . "\nPlease make sure that you create a service definition first!";
		$LOG->warn($msg);
		$self->throw($msg);
	}

	# generate from template
	my $tt = Template->new( ABSOLUTE => 1 );
	my $input = SADI::Utils->find_file( $Bin, 'SADI', 'Generators', 'templates',
										'service.tt' );
	foreach my $obj (@$services) {
		my $name = $obj->ServiceName;
		my $outputClass = '';
		my (@obj_prop, @dat_prop);
		if ($args{do_owl2perl}) {
			use lib $SADICFG::GENERATORS_OUTDIR;
			# convert the outputclass to a module name
			$outputClass = $self->owlClass2module( $self->uri2package($obj->OutputClass) );
			
			eval "require $outputClass";
			# error: dont put owl2perl in the service skeleton
			if ($@) {
			 $args{do_owl2perl} = 0;
			 $LOG->warn($@);
			}
			if ($args{do_owl2perl}) {
				my @inheritance = &__inheritance($outputClass->new);
				# get the unique fields
				my %seen = ();
				my @unique = grep { ! $seen{$_} ++ } @inheritance;
				
				foreach (@unique) {
				    # fetch names of datatype properties
				    eval {
				    	my @dp = @{$_->__properties->{datatypes}} if defined $_->__properties->{datatypes} ;
				        push @dat_prop, @dp;
				    };
				    $LOG->warn ($@) if $@;
				    # fetch object properties
				    eval {
				    	my @op = @{$_->__properties->{objects}} if defined $_->__properties->{objects};
				        push @obj_prop, @op;
				    };
				    $LOG->warn ($@) if $@;
				}
			}
		}
		$LOG->debug("\tGenerating impl for $name\n");
		my $module_name =
		  $self->service2module( $obj->Authority, $obj->ServiceName );

		#	print SADI::Base->toString (\%input_paths);
		# create implementation specific object
		my $impl =
		  { package => ( $args{impl_prefix} || 'Service' ) . '::' . $name, };
		if ( $args{outcode} ) {
			$tt->process(
						  $input,
						  {
							 base               => $obj,
							 impl               => $impl,
							 static_impl        => $args{static_impl},
							 module_name        => $module_name,
							 is_async           => defined $args{is_async} ? $args{is_async} : 0,
							 do_owl2perl        => $args{do_owl2perl},
							 owl2perl_datatypes => \@dat_prop,  
							 owl2perl_objects   => \@obj_prop,
							 owl2perl_outclass  => $outputClass,
 						  },
						  $args{outcode}
			) || $LOG->logdie( $tt->error() );
		} else {
			my $outfile =
			  File::Spec->catfile( $outdir, split( /::/, $impl->{package} ) )
			  . '.pm';

			# do not overwrite an existing file (there may be already
			# a real implementation code)
			if ( -f $outfile and !$args{force_over} ) {
				$LOG->logwarn( "Implementation '$outfile' already exists. "
						 . "It will *not* be re-generated. Safety reasons.\n" );
				next;
			}
			$tt->process(
						  $input,
						  {
							 base               => $obj,
							 impl               => $impl,
							 static_impl        => $args{static_impl},
							 module_name        => $module_name,
							 is_async           => defined $args{is_async} ? $args{is_async} : 0,
							 do_owl2perl        => $args{do_owl2perl},
                             owl2perl_datatypes => \@dat_prop,  
                             owl2perl_objects   => \@obj_prop,
                             owl2perl_outclass  => $outputClass,
						  },
						  $outfile
			) || $LOG->logdie( $tt->error() );
			$LOG->debug("Created $outfile\n");
		}
	}
}

# extracts all of the parent names from  an OWL2Perl generated datatype
sub __inheritance {
    my $self = $_[0];    
    my $class = ref($self) || $self;
    return unless $class;
    no strict;
    my @parent_classes = @{$class . '::ISA'};

    my %hash;
    my @ordered_inheritance;
    push @ordered_inheritance, $class;
    foreach my $parent_class (@parent_classes) {
        push @ordered_inheritance, $parent_class, ($parent_class eq 'OWL::Data::OWL::Class' ? () : __inheritance($parent_class) );
    }
    return @ordered_inheritance;
}

#-----------------------------------------------------------------
# generate_async_impl
#-----------------------------------------------------------------
#sub generate_async_impl {
#	my ( $self, @args ) = @_;
#	my %args = (    # some default values
#		impl_outdir => (
#						 $SADICFG::GENERATORS_IMPL_OUTDIR
#						   || SADI::Utils->find_file( $Bin, 'services' )
#		),
#		impl_prefix   => $SADICFG::GENERATORS_IMPL_PACKAGE_PREFIX,
#		service_names => [],
#		force_over    => 0,
#		static_impl   => 0,
#
#		# other args, with no default values
#		# authority     => 'authority'
#		# outcode       => ref SCALAR
#		# and the real parameters
#		@args
#	);
#	$self->_check_outcode(%args);
#	my $outdir = File::Spec->rel2abs( $args{impl_outdir} );
#	$LOG->debug( "Arguments for generating async service implementation:\n"
#				 . $self->toString( \%args ) )
#	  if ( $LOG->is_debug );
#	my @names = ();
#	push( @names, @{ $args{service_names} } )
#	  if defined $args{service_names};
#	my $services = $self->read_services(@names);
#	if ( scalar @$services == 0 ) {
#		my $msg = "Didn't find any services for @names!"
#		  . "\nPlease make sure that you create a service definition first!";
#		$LOG->warn($msg);
#		$self->throw($msg);
#	}
#
#	# generate from template
#	my $tt = Template->new( ABSOLUTE => 1 );
#	my $input = SADI::Utils->find_file( $Bin, 'SADI', 'Generators', 'templates',
#										'service-async.tt' );
#	foreach my $obj (@$services) {
#		my $name = $obj->ServiceName;
#		$LOG->debug("\tGenerating impl for $name\n");
#		my $module_name =
#		  $self->service2module( $obj->Authority, $obj->ServiceName );
#
#		#   print SADI::Base->toString (\%input_paths);
#		# create implementation specific object
#		my $impl =
#		  { package => ( $args{impl_prefix} || 'Service' ) . '::' . $name, };
#		if ( $args{outcode} ) {
#			$tt->process(
#						  $input,
#						  {
#							 base        => $obj,
#							 impl        => $impl,
#							 static_impl => $args{static_impl},
#							 module_name => $module_name,
#						  },
#						  $args{outcode}
#			) || $LOG->logdie( $tt->error() );
#		} else {
#			my $outfile =
#			  File::Spec->catfile( $outdir, split( /::/, $impl->{package} ) )
#			  . '.pm';
#
#			# do not overwrite an existing file (there may be already
#			# a real implementation code)
#			if ( -f $outfile and !$args{force_over} ) {
#				$LOG->logwarn( "Implementation '$outfile' already exists. "
#						 . "It will *not* be re-generated. Safety reasons.\n" );
#				next;
#			}
#			$tt->process(
#						  $input,
#						  {
#							 base        => $obj,
#							 impl        => $impl,
#							 static_impl => $args{static_impl},
#							 module_name => $module_name,
#						  },
#						  $outfile
#			) || $LOG->logdie( $tt->error() );
#			$LOG->debug("Created $outfile\n");
#		}
#	}
#}

#-----------------------------------------------------------------
# generate_cgi
#-----------------------------------------------------------------
sub generate_cgi {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		outdir => $SADICFG::GENERATORS_IMPL_OUTDIR
		  || SADI::Utils->find_file( $Bin, 'services' ),
		service_names => [],

		# other args, with no default values
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{outdir} . "/../cgi" );
	$args{outdir} = $outdir;
	$LOG->debug(
		"Arguments for generating cgi services:\n" . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	$LOG->debug("CGI Services will be generated into: '$outdir'")
	  unless $args{outcode};
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};
	my $services = $self->read_services(@names);

	if ( scalar @$services == 0 ) {
		my $msg = "Didn't find any services for @names!"
		  . "\nPlease make sure that you create a service definition first!";
		$LOG->warn($msg);
		$self->throw($msg);
	}

	# generate from template
	my $tt = Template->new( { ABSOLUTE => 1, TRIM => 1 } );
	my $input = File::Spec->rel2abs(
				   SADI::Utils->find_file(
					   $Bin, 'SADI', 'Generators', 'templates', 'service-cgi.tt'
				   )
	);
	foreach my $obj (@$services) {
		my $name = $obj->ServiceName;
		$LOG->debug("\tGenerating cgi script for $name\n");
		my $module_name =
		  $self->service2module( $obj->Authority, $obj->ServiceName );
		$LOG->debug("$name\n");
		if ( $args{outcode} ) {

			# check if the same service is already loaded
			# (it can happen when this subroutine is called several times)
			next if eval '%' . $module_name . '::';
			$tt->process(
						  $input,
						  {
							obj           => $obj,
							generated_dir => $SADICFG::GENERATORS_OUTDIR,
							services_dir  => $SADICFG::GENERATORS_IMPL_OUTDIR,
							home_dir      => $SADICFG::GENERATORS_IMPL_HOME,
						  },
						  $args{outcode}
			) || $LOG->logdie( $tt->error() );
		} else {

			# we cannot easily check whether the same file was already
			# generated - so we don't
			my $outfile =

#File::Spec->catfile( $outdir, split( /\./, $obj->Authority ), $obj->ServiceName );
			  File::Spec->catfile( $outdir, $obj->ServiceName );
			$tt->process(
						  $input,
						  {
							obj           => $obj,
							generated_dir => $SADICFG::GENERATORS_OUTDIR,
							services_dir  => $SADICFG::GENERATORS_IMPL_OUTDIR,
							home_dir      => $SADICFG::GENERATORS_IMPL_HOME,
						  },
						  $outfile
			) || $LOG->logdie( $tt->error() );
			chmod( 0755, $outfile );
			$LOG->debug("\tCGI service created at '$outfile'\n");
		}
	}
}

#-----------------------------------------------------------------
# generate_async_cgi
#-----------------------------------------------------------------
sub generate_async_cgi {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		outdir => $SADICFG::GENERATORS_IMPL_OUTDIR
		  || SADI::Utils->find_file( $Bin, 'services' ),
		service_names => [],

		# other args, with no default values
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{outdir} . "/../cgi" );
	$args{outdir} = $outdir;
	$LOG->debug(
		"Arguments for generating cgi services:\n" . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	$LOG->debug("CGI Services will be generated into: '$outdir'")
	  unless $args{outcode};
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};
	my $services = $self->read_services(@names);

	if ( scalar @$services == 0 ) {
		my $msg = "Didn't find any services for @names!"
		  . "\nPlease make sure that you create a service definition first!";
		$LOG->warn($msg);
		$self->throw($msg);
	}

	# generate from template
	my $tt = Template->new( { ABSOLUTE => 1, TRIM => 1 } );
	my $input = File::Spec->rel2abs(
			 SADI::Utils->find_file(
				 $Bin, 'SADI', 'Generators', 'templates', 'service-cgi-async.tt'
			 )
	);
	foreach my $obj (@$services) {
		my $name = $obj->ServiceName;
		$LOG->debug("\tGenerating cgi script for $name\n");
		my $module_name =
		  $self->service2module( $obj->Authority, $obj->ServiceName );
		$LOG->debug("$name\n");
		if ( $args{outcode} ) {

			# check if the same service is already loaded
			# (it can happen when this subroutine is called several times)
			next if eval '%' . $module_name . '::';
			$tt->process(
						  $input,
						  {
							obj           => $obj,
							generated_dir => $SADICFG::GENERATORS_OUTDIR,
							services_dir  => $SADICFG::GENERATORS_IMPL_OUTDIR,
							home_dir      => $SADICFG::GENERATORS_IMPL_HOME,
						  },
						  $args{outcode}
			) || $LOG->logdie( $tt->error() );
		} else {

			# we cannot easily check whether the same file was already
			# generated - so we don't
			my $outfile =

#File::Spec->catfile( $outdir, split( /\./, $obj->Authority ), $obj->ServiceName );
			  File::Spec->catfile( $outdir, $obj->ServiceName );
			$tt->process(
						  $input,
						  {
							obj           => $obj,
							generated_dir => $SADICFG::GENERATORS_OUTDIR,
							services_dir  => $SADICFG::GENERATORS_IMPL_OUTDIR,
							home_dir      => $SADICFG::GENERATORS_IMPL_HOME,
						  },
						  $outfile
			) || $LOG->logdie( $tt->error() );
			chmod( 0755, $outfile );
			$LOG->debug("\tCGI service created at '$outfile'\n");
		}
	}
}

#-----------------------------------------------------------------
# generate_definition
#-----------------------------------------------------------------
sub generate_definition {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		outdir        => $self->definitions,
		service_names => [],

		# other args, with no default values
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{outdir} );
	$LOG->debug( "Arguments for generating service definitions: "
				 . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	$LOG->debug("Service definitions will be generated into: '$outdir'")
	  unless $args{outcode};
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};

	# generate from template
	my $tt = Template->new( ABSOLUTE => 1 );
	my $input = File::Spec->rel2abs(
									 SADI::Utils->find_file(
													  $Bin,         'SADI',
													  'Generators', 'templates',
													  'service-definition.tt'
									 )
	);
	foreach my $obj (@names) {
		my $name = $obj;
		$LOG->debug("\tGenerating definition for $name\n");
		if ( $args{outcode} ) {
			$tt->process( $input, { name => $name, }, $args{outcode} )
			  || $LOG->logdie( $tt->error() );
		} else {
			my $outfile = File::Spec->catfile( $outdir, $name );
			if ( -f $outfile and !$args{force_over} ) {
				$LOG->logwarn( "Definition '$outfile' already exists. "
						 . "It will *not* be re-generated. Safety reasons.\n" );
				next;
			}
			$tt->process( $input, { name => $name, }, $outfile )
			  || $LOG->logdie( $tt->error() );
		}
	}
}

#-----------------------------------------------------------------
# generate_unit_test
#-----------------------------------------------------------------
sub generate_unit_test {
	my ( $self, @args ) = @_;
	my %args = (    # some default values
		outdir        => $self->unit_tests_dir,
		service_names => [],

		# other args, with no default values
		# outcode       => ref SCALAR
		# and the real parameters
		@args
	);
	$self->_check_outcode(%args);
	my $outdir = File::Spec->rel2abs( $args{outdir} );
	$LOG->debug( "Arguments for generating service unit tests: "
				 . $self->toString( \%args ) )
	  if ( $LOG->is_debug );
	$LOG->debug("Service unit tests will be generated into: '$outdir'")
	  unless $args{outcode};
	my @names = ();
	push( @names, @{ $args{service_names} } )
	  if defined $args{service_names};

	# make sure that the service impl exists
	my $services = $self->read_services(@names);
	if ( scalar @$services == 0 ) {
		my $msg = "Didn't find any services for @names!"
		  . "\nPlease make sure that you create a service first before generating unit tests for it!";
		$LOG->warn($msg);
		$self->throw($msg);
	}

	# generate from template
	my $tt = Template->new( ABSOLUTE => 1 );
	my $example_path =
	  File::Spec->rel2abs( File::Spec->catfile( $outdir, '..', 'xml' ) );
	my $input = File::Spec->rel2abs(
			 SADI::Utils->find_file(
				 $Bin, 'SADI', 'Generators', 'templates', 'service-unit-test.tt'
			 )
	);
	foreach my $obj (@$services) {
		my $name = $obj->ServiceName;
		my $ex_in =
		  File::Spec->rel2abs(
							File::Spec->catfile( $example_path, "$name.xml" ) );
		my $ex_out =
		  File::Spec->rel2abs(
					 File::Spec->catfile( $example_path, "$name-output.xml" ) );
		$LOG->debug("\tGenerating unit test for $name\n");
		if ( $args{outcode} ) {
			$tt->process(
						  $input,
						  {
							 name   => $name,
							 input  => $ex_in,
							 output => $ex_out,
						  },
						  $args{outcode}
			) || $LOG->logdie( $tt->error() );
		} else {
			my $outfile = File::Spec->catfile( $outdir, $name );
			if ( -f $outfile and !$args{force_over} ) {
				$LOG->logwarn( "Unit test '$outfile' already exists. "
						 . "It will *not* be re-generated. Safety reasons.\n" );
				next;
			}
			$tt->process(
						  $input,
						  {
							 name   => $name,
							 input  => $ex_in,
							 output => $ex_out,
						  },
						  $outfile
			) || $LOG->logdie( $tt->error() );
		}
	}
}

#-----------------------------------------------------------------
# _check_outcode
#    throws an exception if %args has an 'outcode' of a wrong type
#-----------------------------------------------------------------
sub _check_outcode {
	my ( $self, %args ) = @_;
	$self->throw("Parameter 'outcode' should be a reference to a SCALAR.")
	  if $args{outcode} and ref( $args{outcode} ) ne 'SCALAR';
}
1;
__END__

=head1 NAME

SADI::Generators::GenServices - generator of SADI services

=head1 SYNOPSIS

 use SADI::Generators::GenServices;

=head1 DESCRIPTION

A module required for generating SADI service skeleton code.

=head1 AUTHORS, COPYRIGHT, DISCLAIMER

 Martin Senger (martin.senger [at] gmail [dot] com)
 Edward Kawas (edward.kawas [at] gmail [dot] com)

Copyright (c) 2009 Martin Senger, Edward Kawas. All Rights Reserved.

This module is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

This software is provided "as is" without warranty of any kind.

=head1 ACCESSIBLE ATTRIBUTES

Details are in L<SADI::Base>. Here just a list of them:

=over

=item B<outdir>

A directory where to create generated code.

=item B<definitions>

A directory containing service definitions.

=back

=head1 SUBROUTINES

=over

=item async_load

=item load

=item generate_base

=item generate_impl

=item generate_cgi

=item generate_definition

=item generate_async_base

=item generate_async_impl

=item generate_async_cgi

=back

=cut

