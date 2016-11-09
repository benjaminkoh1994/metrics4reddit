# Metrics4Reddit

This is the metrics4reddit reddit script.

This software was written as an experiment in harvesting intersting data from Reddit.  I'm still not sure where this is going.  I may get some deep learning or natual language processing involved to see what I can get these libraries to tell me about how people interact with various communities on Reddit.

Because I am standing on the shoulders of others I am publishing my code minus things like the appid, secret etcetera required to really run it.

In order to run this you need to provide a file named metrics4reddit.properties on the classpath.  This file must contain two properties:

```
#the appId assigned by reddit
metrics4reddit.reddit.appId=<<replace with appId obtained from reddit>>
#the secret assigned by reddit
metrics4reddit.reddit.secret=<<replace with secret obtained from reddit>>
```


These properties must be populated by values obtained from registering a script for development purposes here:
https://www.reddit.com/prefs/apps/

You will find a button at the bottom of the apps page labled "Create Another App".  Just click on it and register as a developer script.  Find the appId and secret in the registration and plug them into your metrics4reddit.properties file. 

This software uses the follwing packages:
JRAW:    MIT License [JRAW Github](https://github.com/thatJavaNerd/JRAW)  
log4j:   Apache License 2.0 [log4j Home](http://logging.apache.org/log4j/2.x/)  
slf4j:   MIT License [slf4j Home](http://www.slf4j.org/)  
guava:   Apache License 2.0 [Guava Github](https://github.com/google/guava)  
jackson: Apache License 2.0 [Jackson Github](https://github.com/FasterXML/jackson)  
okhttp:  Apache License 2.0 [Okhttp Github](http://square.github.io/okhttp/)  
okio:    Apache License 2.0 [Okio Github](https://github.com/square/okio)

