	# Load the Module::Install bundled in ./inc/
	use inc::Module::Install;
    
    # constant
    # check for windows
    # http://perldoc.perl.org/perlport.html#DOS-and-Derivatives
    use constant MSWIN => $^O =~ /MSWin32|Windows_NT/i ? 1 : 0;
    
	# Define metadata
	name 'SADI';
	version '1.13';
	license 'new BSD';
	abstract
'This distribution aids in the creation of SADI perl based web services.';
	all_from 'lib/SADI.pm';

	# Specific dependencies
	requires 'Carp'             	=> 0;
	requires 'CGI'              	=> 0;
	requires 'Cwd'                  => 3.33;
	requires 'File::Spec'       	=> 3.33;
	requires 'File::Temp'           => 0.22;
	requires 'File::Path'           => 0;
	requires 'File::HomeDir'    	=> 0.65;
	requires 'File::ShareDir'   	=> 1.00;
	requires 'Config::Simple'       => 4.58;
	requires 'Log::Log4perl'    	=> 1.12;
	requires 'HTTP::Date'			=> 5.81;	
	requires 'Template'         	=> 2.22;
	requires 'Params::Util'     	=> 0.38;
    requires 'Class::Inspector' 	=> 1.17;
    requires 'Unicode::String'  	=> 2.09;
    requires 'IO::String'       	=> 1.08;
    requires 'RDF::Core'            => 0.51;
    requires 'HTTP::Request'        => 0;
    requires 'LWP'                  => 0;
    requires 'OWL2Perl'             => '0.99';
    requires 'URI'                  => 1.40;
    requires 'RDF::Notation3'       => .91;
    requires 'Test::Pod'            => 1.45;

    # windows doesn't need this
	if ( not MSWIN ) {
	    requires 'Want'       =>  0.18;
	    requires 'IO::Prompt' => '0.99.2';
	}

	build_requires 'Test::More' => 0.42;

    # Remove some extra files if they exist
    clean_files( 
        qw{
           allpod.txt
           pod2html.tmp
           pod2htmi.tmp
           pod2htmd.tmp
           t/async
        }
    );
    

	no_index directory        => 'bin/scripts';

    # dont index template files
    no_index directory => 'lib/SADI/Generators/templates';
    
	install_script 'bin/scripts/sadi-install.pl';
	install_script 'bin/scripts/sadi-generate-services.pl';
	install_script 'bin/scripts/sadi-generate-datatypes.pl';
	install_script 'bin/scripts/sadi-testing-service.pl';
	install_script 'bin/scripts/sadi-config-status.pl';
	install_share 'share';

	auto_install;
	WriteAll;
