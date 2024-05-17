build:
	git submodule init
	git submodule update

	cd RapidWright; ./gradlew compileJava

