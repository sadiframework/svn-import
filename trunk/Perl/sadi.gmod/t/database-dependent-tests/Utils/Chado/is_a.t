use Test::Simple tests => 2;

use strict;
use warnings;

use lib 'lib';
use lib 'testlib';

use Utils::Chado;
use Utils::TestDB;

my $dbh = Utils::TestDB::get_test_db();

# GO:0005634 = 'nucleus', GO:0005575 = 'cellular_component'
ok(Utils::Chado::is_a($dbh, 'GO:0005634', 'GO:0005575'), 'term1 is a term2');
# GO:0005634 = 'nucleus', GO:0008150 = 'biological_process'
ok(!Utils::Chado::is_a($dbh, 'GO:0005634', 'GO:0008150'), 'term1 is not a term2');

