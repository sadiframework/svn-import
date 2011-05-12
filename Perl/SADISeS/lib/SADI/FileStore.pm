#-----------------------------------------------------------------
# SADI::FileStore
# Author: Edward Kawas <edward.kawas@gmail.com>,
#
# For copyright and disclaimer see below.
#
# $Id: FileStore.pm,v 1.11 2010-03-08 19:27:00 ubuntu Exp $
#-----------------------------------------------------------------
package SADI::FileStore;
use SADI::Base;
use base ("SADI::Base");
use FindBin qw( $Bin );
use lib $Bin;
use SADI::Utils;
use Carp;
use File::Spec;
use File::Path;
use Cwd 'abs_path';
use Fcntl qw( :DEFAULT :flock :mode );
use strict;

# add versioning to this module
use vars qw /$VERSION/;
$VERSION = sprintf "%d.%02d", q$Revision: 1.12 $ =~ /: (\d+)\.(\d+)/;

=head1 NAME

SADI::FileStore - A module for (add|get|delet)ing persistent data

=cut

=head1 SYNOPSIS

 use SADI::FileStore;

 # create a new blank SADI FileStore object
 my $data = SADI::FileStore->new ();

 # create a new primed SADI File store object
 $data = SADI::FileStore->new (
     ServiceName => "HelloWorldService",
 );

 # get the service name
 my $name = $data->ServiceName;
 # set the service name
 $data->ServiceName($name);

 # create a unique id
 my $uid = $data->generate_uid();

 # store a persistent file
 $data->add($uid,"my value");

 # get the persistent file
 $data->get($uid);

 # remove the persistent file
 $data->remove($uid);

=cut

=head1 DESCRIPTION
    
A module for creating persistent files (for use with asynchronous services)

=cut

#-----------------------------------------------------------------
# A list of allowed attribute names. See SADI::Base for details.
#-----------------------------------------------------------------

=head1 ACCESSIBLE ATTRIBUTES

Details are in L<SADI::Base>. Here just a list of them (additionally
to the attributes from the parent classes)

=over

=item B<ServiceName>

The name of the service that this FileStore will operate on

=back

=cut

=head1 SUBROUTINES

=head2 add

 # use this method to add persistent data to the store. 
 #   Given a unique identifier, $uid, and the data, $data,  
 #   this sub will store the information for you.
 # Throws an exception if there are problems writing to disk.
 #
 # You can provide any identifier (but make it unique or else 
 # existing data will be over-written). If you want to generate
 # one automatically, use C<generate_uid>.

=cut

=head2 get

 # use this method to get persistent data from the store. 
 #   Given a unique identifier, $uid, this sub will retrieve
 #   the information for you.
 # Throws an exception if there are problems retrieving the data
 # from the disk.

=cut

=head2 remove

 # use this method to remove persistent data from the store. 
 #   Given a unique identifier, $uid, this sub will remove 
 #   the persistent data from the store.
 # Throws an exception if there are problems removing the data
 # from the store.
 #
 # Also, if the service that created this store no longer has
 # data in the store, then the service directory is removed so
 # that no files are left over.

=cut

=head2 generate_uid

 # use this method to generate a unique identifier for use with C<add>. 
 #   Given an optional length for our unique identifier (defaults to 24),  
 #   a unique identifier is generated.
 # using the default length, you have a 1 in 24^6760 chance of generating
 # a duplicate uid. All uids generated are a combo of letters and numbers.

=cut

=head2 clean_store

 # use this method to clean the file store. 
 #   Given an optional age in days of files to clean (defaults to 2.00),  
 #   the file store is checked for files that meet this critia and removed.

=cut

=cut

=head1 AUTHORS, COPYRIGHT, DISCLAIMER

 Edward Kawas  (edward.kawas [at] gmail [dot] com)

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

{
	my %_allowed = (

		# this is the sub dir that we store our files
		ServiceName => {
			type => SADI::Base->STRING,
			post => sub {
				my ($self) = shift;

				# escape the name
				$self->{_escaped_name} =
				  $self->_clean_servicename( $self->ServiceName );
			},
		},

		# here we clean the service name ... just in case
		_escaped_name => { type => SADI::Base->STRING, },
	);

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
}

# add a file to our persistent store
sub add {
	my ( $self, $id, $value ) = @_;
	$self->throw(
			   'Before you can add a data to the store, you need to set ServiceName!\n')
	  unless $self->ServiceName;
	my $async_dir = defined $SADICFG::ASYNC_TMP ? $SADICFG::ASYNC_TMP : File::Spec->tmpdir(); #$SADICFG::ASYNC_TMP || SADI::Utils->find_file( $Bin, 'async' );
	unless ( -e File::Spec->catfile( $async_dir, $self->_escaped_name ) ) {
		unless (
				 File::Path::mkpath(
								 File::Spec->catfile( $async_dir, $self->_escaped_name )
				 )
		  )
		{
			$self->throw("add(): couldn't create directory path: $!");
		}
	}
	my $path = File::Spec->catfile( $async_dir, $self->_escaped_name, $id );
	$LOG->debug("Adding file '$path' to filestore ...");

	# make certain our filehandle goes away when we fall out of scope
	local *FH;
	my $no_follow = eval { O_NOFOLLOW } || 0;
	my $mode = O_WRONLY | $no_follow;

	# kill symlinks when we spot them
	if ( -l $path ) {
		unlink($path)
		  or $self->throw(
				 "add(): '$path' appears to be a symlink and I couldn't remove it: $!");
	}
	$mode = O_RDWR | O_CREAT | O_EXCL unless -e $path;
	sysopen( FH, $path, $mode, 0660 )
	  or $self->throw("add(): couldn't open '$path': $!");

	# sanity check to make certain we're still ok
	if ( -l $path ) {
		$self->throw("add(): '$path' is a symlink, check for malicious processes");
	}

	# prevent race condition
	flock( FH, LOCK_EX )
	  or $self->throw("add(): couldn't lock '$path': $!");
	truncate( FH, 0 ) or $self->throw("add(): couldn't truncate '$path': $!");
	print FH $value;
	close(FH) or $self->throw("add(): couldn't close '$path': $!");
	return 1;
}

# remove a file from our persistent store
sub remove {
	my ( $self, $id ) = @_;

	# make sure that we have a primed FileStore first
	$self->throw(
			'Before you can remove data from the store, you need to set ServiceName!\n')
	  unless $self->ServiceName;
	my $async_dir = defined $SADICFG::ASYNC_TMP ? $SADICFG::ASYNC_TMP : File::Spec->tmpdir(); #$SADICFG::ASYNC_TMP || SADI::Utils->find_file( $Bin, 'async' );
	my $path = File::Spec->catfile( $async_dir, $self->_escaped_name, $id );

	# remove the file if the path exists ...
	if ( -e $path ) {

		# path exists
		unlink($path) or $self->throw("Couldn't remove the file '$path'!\n");
	}

	# check if the directory is empty now ...
	if ( $self->_is_dir_empty( File::Spec->catfile( $async_dir, $self->_escaped_name ) )
	  )
	{
		File::Path::rmtree( File::Spec->catfile( $async_dir, $self->_escaped_name ) );
	}
	return 1;
}

# get a file from our persistent store
sub get {
	my ( $self, $id ) = @_;

	# make sure that we have a primed store first
	$self->throw(
		  'Before you can retrieve data from the store, you need to set ServiceName!\n')
	  unless $self->ServiceName;
	my $async_dir = defined $SADICFG::ASYNC_TMP ? $SADICFG::ASYNC_TMP : File::Spec->tmpdir(); #$SADICFG::ASYNC_TMP || SADI::Utils->find_file( $Bin, 'async' );
	my $path = File::Spec->catfile( $async_dir, $self->_escaped_name, $id );
	return undef unless -e $path;

	# make certain our filehandle goes away when we fall out of scope
	local *FH;
	if ( -l $path ) {
		unlink($path)
		  or $self->throw(
			   "get(): '$path' appears to be a symlink and I couldn't remove it: $!\n");
	}
	my $no_follow = eval { O_NOFOLLOW } || 0;
	sysopen( FH, $path, O_RDONLY | $no_follow )
	  || return $self->set_error("retrieve(): couldn't open '$path': $!");
	flock( FH, LOCK_SH ) or $self->throw("get(): couldn't lock '$path': $!");
	my $rv = "";
	while (<FH>) {
		$rv .= $_;
	}
	close(FH);
	return $rv;
}

# TODO when should we do this? should we just allow the service provider to call this method? Should we automatically do it?
sub clean_store {
	my ( $self, $age ) = @_;
	return unless $self->ServiceName;
	$age = 2.00 unless defined $age;
	$age = 2.00 unless $age =~ m/^\s*(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?\s*$/;
	my $async_dir = defined $SADICFG::ASYNC_TMP ? $SADICFG::ASYNC_TMP : File::Spec->tmpdir(); # $SADICFG::ASYNC_TMP || SADI::Utils->find_file( $Bin, 'async' );
	for my $eachFile (
				glob( File::Spec->catfile( $async_dir, $self->_escaped_name ) . '/*' ) )
	{
		if ( -d $eachFile ) {
			$LOG->warn(
"Encountered a directory in the file store where a file was expected! Offender: '$eachFile'\n"
			);
		} else {

			# remove the following file if the age is greater than wanted ...
			$LOG->debug("Removing '$eachFile' from store\n") if $LOG->is_debug() and -A $eachFile >= $age;
			unlink $eachFile if -A $eachFile >= $age;
		}
	}
}

sub _clean_servicename {
	my ( $self, $name ) = @_;
	$name =~ s/\W/_/g;
	return ( $name =~ /^\d/ ? "_$name" : $name );
}

sub _is_dir_empty {
	my ( $self, $path ) = @_;
	return 1 unless $path;
	# catch cases where the path doesnt exist and therefore is empty
	eval {$path = abs_path($path)};
	return 1 if $@;
	
	opendir (my $dfh, $path);
	while ( readdir ($dfh) ) {
		next unless defined $_;
		next if ( $_ =~ m/^\./ );
		closedir ($dfh);
		return 0;
	}
	closedir ($dfh);
	return 1;
}

sub generate_uid {
	my ( $self, $length ) = @_;
	my $uid = "";

	# if no length, then we give a default of 24
	$length = 24 unless defined $length;

	# if length isnt numeric, then we give a default of 24
	$length = 24 unless $length =~ m/^\s*\d+\s*$/;
	while ( length($uid) != $length ) {
		my $j = chr( int( rand(127) ) );
		if ( $j =~ /[a-zA-Z0-9]/ ) {
			$uid .= $j;
		}
	}
	return $uid;
}
1;
__END__
