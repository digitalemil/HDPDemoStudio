HDPAppStudio
============

Making HDP Demos easy


Now supporting HDP 2.2.0-2041 (2.2 Preview sandbox does not work!)

Default mode is binary delivery now. This means HDPAppStudio is an Ambari View on it's own. 
Having said that property-files are still ok. In fact the Ambari view takes your input and creates a property-file from them.

To build HDP AppStudio run the following commands on a 2.2 Sandbox or cluster
mvn clean compile assembly:single
cd StormTopology
mvn clean compile assembly:single
cd ..
./createpkg.sh

This will produce a tar-ball under dist.

Copy the tar-ball dist/HDPAppStudio-bin-2.2.0.0-2041.tar on the a fresh Sandbox
and 
tar xf HDPAppStudio-bin-2.2.0.0-2041.tar
./install.sh

Afterwards find your HDPAppStudio View in Ambari and create your application there. 
 
Alternatively you start a fresh sandbox, logon and do:
git clone https://github.com/digitalemil/HDPAppStudio.git
cd HDPAppStudio
sh ./install.sh
and follow the output



