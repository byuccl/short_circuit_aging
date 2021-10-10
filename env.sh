DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
export RAPIDWRIGHT_PATH=$DIR/../third_party/rapidwright


# Add rapidwright stuff to CLASSPATH
export CLASSPATH=$RAPIDWRIGHT_PATH/build/classes/java/main:$(echo $RAPIDWRIGHT_PATH/jars/*.jar | tr ' ' ':')

# Add shorty stuff to CLASSPATH
export CLASSPATH=$CLASSPATH:$DIR/build/classes/java/main