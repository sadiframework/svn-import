package Utils::GMOD;

use strict;

=head2 get_db_conf

 Title      : get_db_conf
 Usage      :

              my $dbconf = Utils::GMOD::get_db_conf('default');  

              my $dbname = $db_conf->name;
              my $dbhost = $db_conf->host;
              my $dbport = $db_conf->port;
              my $dbuser = $db_conf->user;
              my $dbpass = $db_conf->password;
 
 Function   : get GMOD database connection parameters (username, password, port, etc.)
              for the given database profile name.
 Returns    : a Bio::GMOD::DB::Config
 Args       : database profile name (usually 'default' or the name of the database in postgres)
                   
=cut

sub get_db_conf
{
    my ($db_profile_name) = @_;

    my $gmod_conf = Bio::GMOD::Config->new();
    my $db_conf = Bio::GMOD::DB::Config->new($gmod_conf, $db_profile_name);

    return $db_conf;
}

1;
