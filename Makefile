build:
	git submodule init
	git submodule update

	./gradlew build
	cp build/libs/*.jar .

clean:
	./gradlew clean

