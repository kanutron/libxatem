#!/bin/sh

#!/bin/sh
PKG="com.xetrix.xmpp."
CLS="XatemTester"
CMD="java -Djavax.net.debug=none -Djava.net.debug=none -jar $CLS.jar jmroca@xetrix.com dev talk.google.com 5222 2"
CMD="java -Djavax.net.debug=none -jar $CLS.jar kanutron dev chat.facebook.com 5222 2"
CMD="java -Djavax.net.debug=none -jar $CLS.jar kanutron@jabber.org dev jabber.org 5222 0"
SRC=com/xetrix/xmpp

export CLASSPATH=./lib/jargs.jar:./lib/xpp.jar:./lib/jzlib.jar:$CLASSPATH

echo "cleaning..."
rm -f $CLS.jar
find -iname *.class -exec rm {} \;

echo "building $PKG$CLS..."
javac -Xlint:unchecked $SRC/*.java || exit 1

echo "creating JAR for $PKG$CLS..."
jar cfm $CLS.jar Manifest.txt $SRC/*.class $SRC/util/*.class $SRC/client/*.class lib/*.jar

echo -e "running $PKG$CLS..."
echo -e "$CMD\n\n\n"
exec $CMD
