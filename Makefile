install:
	git submodule init
	git submodule update

	# Get rapidwright files
	wget https://github.com/Xilinx/RapidWright/releases/download/v2018.3.2-beta/rapidwright_data.zip
	rm -rf third_party/rapidwright/data
	unzip rapidwright_data.zip -d third_party/rapidwright
	rm rapidwright_data.zip

	wget https://github.com/Xilinx/RapidWright/releases/download/v2018.3.2-beta/rapidwright_jars.zip
	rm -rf third_party/rapidwright/jars
	unzip rapidwright_jars.zip -d third_party/rapidwright
	rm rapidwright_jars.zip