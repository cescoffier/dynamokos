# Introduction #
This page explains how to run the demos.

# Getting the demo #
  1. Download a **-runtime** file from the download section.
  1. Unzip it
  1. Open **two** shells / terminals / consoles and navigate (twice) to  : `$unzip_location/Dynamokos-XXX/runtime` (replace XXX by the version)

# Post 1 : Distributed OSGi Application with Apache CXF DOSGi #

To run the demo of the post 1, execute in a first console:
```
java -Dfelix.config.properties=file:./conf/oracle-dosgi.properties -jar bin/felix.jar cache/oracle-dosgi
```

This launches the Oracle OSGi platform.

Then launch in a second console:
```
java -Dfelix.config.properties=file:./conf/client-dosgi.properties -jar bin/felix.jar cache/client-dosgi
```

This launches the client and the web-part.

Once done, open a browser and go to http://localhost:8080/dynamokos/index.html.

That's it !

# Post 2 : Introducing Dynamic Discovery #
First start the zookeeper server (require Java 6):
```
sh ./zookeeper/bin/zkServer.sh start
```

```
zookeeper\bin\zkServer.cmd start
```

Once started, launch the Oracle platform with:
```
java -Dfelix.config.properties=file:./conf/oracle-dosgi-zookeeper.properties -jar bin/felix.jar cache/oracle-dosgi-zookeeper
```

This launches the Oracle OSGi platform. The Prediction Service will be added to Zookeeper.

Then launch in a second console:
```
java -Dfelix.config.properties=file:./conf/client-dosgi-zookeeper.properties -jar bin/felix.jar cache/client-dosgi-zookeeper
```

This launches the client and the web-part. The Prediction service is discovered thanks to Zookeeper.

Once done, open a browser and go to http://localhost:8080/dynamokos/index.html.

To check the dynamism, try to stop the Oracle bundle from the Oracle platform
```
stop 11
```

(check that the bundle 11 is the Oracle bundle :-))

If you refresh the page... The page is no more there.

Restart the bundle
```
start 11
```

Refresh the page.

That's it !

# Post 3 : Propagating OSGi dynamism in web interfaces #
Relaunch Zookeeper and the server platform as in the previous post.
Then, to launch the pull version, launch:
```
java -Dfelix.config.properties=file:conf/client-dosgi-zookeeper-pull.properties -jar bin/felix.jar cache/client-dosgi-zookeeper-pull
```

To launch the push version, launch:
```
java -Dfelix.config.properties=file:conf/client-dosgi-zookeeper-cometd.properties -jar bin/felix.jar cache/client-dosgi-zookeeper-cometd
```

To see the dynamism impact, {{stop}} and {{start}} the bundle 11 from the server platform.

# Post 4 #
Coming soon